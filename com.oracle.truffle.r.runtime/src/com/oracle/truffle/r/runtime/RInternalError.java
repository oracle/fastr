/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * This class is intended to be used for internal errors that do not correspond to R errors.
 */
public final class RInternalError extends Error {

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
    public static void reportError(Throwable t) {
        if (FastROptions.PrintErrorStacktracesToFile.getBooleanValue() || FastROptions.PrintErrorStacktraces.getBooleanValue()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(out));
            String verboseStackTrace;
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
            if (FastROptions.PrintErrorStacktracesToFile.getBooleanValue()) {
                try (BufferedWriter writer = Files.newBufferedWriter(FileSystems.getDefault().getPath(REnvVars.rHome(), "fastr_errors.log"), StandardCharsets.UTF_8, StandardOpenOption.APPEND,
                                StandardOpenOption.CREATE)) {
                    writer.append(new Date().toString()).append('\n');
                    writer.append(out.toString()).append('\n');
                    writer.append(verboseStackTrace).append("\n\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
