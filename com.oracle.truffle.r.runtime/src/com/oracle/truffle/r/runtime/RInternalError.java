/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * This class is intended to be used for internal errors that do not correspond to R errors.
 */
public final class RInternalError extends Error implements TruffleException {

    private static final long serialVersionUID = 80698622974155216L;

    private static boolean initializing = false;

    private final String verboseStackTrace;

    public RInternalError(String message, Object... args) {
        super(String.format(message, args));
        verboseStackTrace = createVerboseStackTrace();
    }

    public RInternalError(Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
        verboseStackTrace = createVerboseStackTrace();
    }

    /**
     * Constructor that does not use {@code String.format} so that the message may contain
     * formatting instructions.
     */
    public RInternalError(Throwable cause, String message) {
        super(message, cause);
        verboseStackTrace = createVerboseStackTrace();
    }

    public String getVerboseStackTrace() {
        return verboseStackTrace;
    }

    public static void guarantee(boolean condition, String message) {
        if (!condition) {
            CompilerDirectives.transferToInterpreter();
            throw shouldNotReachHere("failed guarantee: " + message);
        }
    }

    public static void guarantee(boolean condition) {
        if (!condition) {
            CompilerDirectives.transferToInterpreter();
            throw shouldNotReachHere("failed guarantee");
        }
    }

    public static <T> T guaranteeNonNull(T value) {
        if (value == null) {
            CompilerDirectives.transferToInterpreter();
            throw shouldNotReachHere("should not be null");
        }
        return value;
    }

    public static RuntimeException unimplemented() {
        CompilerDirectives.transferToInterpreter();
        throw new RInternalError("not implemented");
    }

    public static RuntimeException unimplemented(String message) {
        CompilerDirectives.transferToInterpreter();
        throw new RInternalError("not implemented: %s", message);
    }

    public static RuntimeException shouldNotReachHere() {
        CompilerDirectives.transferToInterpreter();
        throw new RInternalError("should not reach here");
    }

    public static RuntimeException shouldNotReachHere(Throwable cause) {
        CompilerDirectives.transferToInterpreter();
        throw new RInternalError(cause, "should not reach here");
    }

    public static RuntimeException shouldNotReachHere(String message) {
        CompilerDirectives.transferToInterpreter();
        throw new RInternalError("should not reach here: %s", message);
    }

    public static RuntimeException shouldNotReachHere(Throwable cause, String message) {
        CompilerDirectives.transferToInterpreter();
        throw new RInternalError(cause, "should not reach here: %s", message);
    }

    static String createVerboseStackTrace() {
        if (FastROptions.PrintErrorStacktracesToFile.getBooleanValue() || FastROptions.PrintErrorStacktraces.getBooleanValue()) {
            if (!initializing) {
                initializing = true;
                try {
                    return Utils.createStackTrace(true);
                } catch (Throwable t) {
                    StringWriter str = new StringWriter();
                    t.printStackTrace(new PrintWriter(str));
                    return str.toString();
                } finally {
                    initializing = false;
                }
            } else {
                return "<exception during stack introspection>";
            }
        } else {
            return "";
        }
    }

    @TruffleBoundary
    public static void reportErrorAndConsoleLog(Throwable throwable, int contextId) {
        reportError(throwable, contextId);
    }

    @TruffleBoundary
    public static void reportError(Throwable throwable) {
        reportError(throwable, 0);
    }

    private static void reportError(Throwable throwable, int contextId) {
        try {
            Throwable t = throwable;
            if (FastROptions.PrintErrorStacktracesToFile.getBooleanValue() || FastROptions.PrintErrorStacktraces.getBooleanValue()) {

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                t.printStackTrace(new PrintStream(out));
                String verboseStackTrace;
                if (t.getCause() != null && t instanceof IOException) {
                    t = t.getCause();
                }
                if (t instanceof RInternalError) {
                    verboseStackTrace = ((RInternalError) t).getVerboseStackTrace();
                } else if (t instanceof RError) {
                    verboseStackTrace = ((RError) t).getVerboseStackTrace();
                } else {
                    verboseStackTrace = "";
                }
                if (FastROptions.PrintErrorStacktraces.getBooleanValue()) {
                    System.err.println(out.toString());
                    System.err.println(verboseStackTrace);
                }
                String message = t instanceof RInternalError && t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : "internal error: " + t.getClass().getSimpleName();
                if (FastROptions.PrintErrorStacktracesToFile.getBooleanValue()) {
                    String suffix = contextId == 0 ? "" : "-" + Integer.toString(contextId);
                    Path logfile = Utils.getLogPath("fastr_errors.log" + suffix);
                    try (BufferedWriter writer = Files.newBufferedWriter(logfile, StandardCharsets.UTF_8, StandardOpenOption.APPEND,
                                    StandardOpenOption.CREATE)) {
                        writer.append(new Date().toString()).append('\n');
                        writer.append(out.toString()).append('\n');
                        writer.append(verboseStackTrace).append("\n\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.err.println(message + " (see fastr_errors.log" + suffix + ")");
                    if (RContext.isEmbedded()) {
                        Utils.rSuicide("FastR internal error");
                    }
                }
                if (!FastROptions.PrintErrorStacktraces.getBooleanValue() && !FastROptions.PrintErrorStacktracesToFile.getBooleanValue()) {
                    System.err.println(message);
                }
            }
        } catch (Throwable t) {
            System.err.println("error while reporting internal error:");
            t.printStackTrace();
        }
    }

    @Override
    public Node getLocation() {
        return null;
    }

    @Override
    public boolean isInternalError() {
        return true;
    }
}
