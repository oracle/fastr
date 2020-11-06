/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime;

import static com.oracle.truffle.r.runtime.context.FastROptions.PrintErrorStacktraces;
import static com.oracle.truffle.r.runtime.context.FastROptions.PrintErrorStacktracesToFile;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Date;

import org.graalvm.options.OptionKey;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * This class is intended to be used for internal errors that do not correspond to R errors.
 */
public final class RInternalError extends Error {

    private static final String FASTR_ERRORS_LOG = "fastr_errors";

    private static final long serialVersionUID = 80698622974155216L;

    private static boolean initializing = false;

    private final String verboseStackTrace;

    public RInternalError(String message, Object... args) {
        super(Utils.stringFormat(message, args));
        verboseStackTrace = createVerboseStackTrace();
    }

    public RInternalError(Throwable cause, String message, Object... args) {
        super(Utils.stringFormat(message, args), cause);
        CompilerAsserts.neverPartOfCompilation();
        verboseStackTrace = createVerboseStackTrace();
    }

    /**
     * Constructor that does not use {@code String.format} so that the message may contain
     * formatting instructions.
     */
    public RInternalError(Throwable cause, String message) {
        super(message, cause);
        CompilerAsserts.neverPartOfCompilation();
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

    public static RuntimeException unimplemented(String format, Object... args) {
        CompilerDirectives.transferToInterpreter();
        throw new RInternalError("not implemented: %s", String.format(format, args));
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
        RContext ctx = RContext.getInstance();
        if (ctx.getOption(PrintErrorStacktracesToFile) || ctx.getOption(PrintErrorStacktraces)) {
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
        reportErrorDefault(throwable, null, contextId);
    }

    @TruffleBoundary
    public static void reportError(Throwable t) {
        reportErrorDefault(t, null, 0);
    }

    @TruffleBoundary
    public static void reportError(Throwable t, RContext ctx) {
        reportErrorDefault(t, ctx, 0);
    }

    @TruffleBoundary
    private static void reportErrorDefault(Throwable t, RContext ctx, int contextId) {
        RContext context = ctx != null ? ctx : RContext.getInstance();
        boolean detailedMessage = context.getOption(PrintErrorStacktraces) || context.getOption(PrintErrorStacktracesToFile);

        String errMsg = ".";
        if (detailedMessage) {
            errMsg = ": \"";
            errMsg += t instanceof RInternalError && t.getMessage() != null && !t.getMessage().isEmpty() ? t.getMessage() : t.getClass().getSimpleName();
            errMsg += "\"";
        }
        reportError(errMsg, t, context, contextId);
    }

    private static void reportError(String errMsg, Throwable throwable, RContext ctx, int contextId) {
        try {
            Throwable t = throwable;
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

            boolean printErrorStacktraces = getOption(PrintErrorStacktraces, ctx);
            boolean printErrorStacktracesToFile = getOption(PrintErrorStacktracesToFile, ctx);
            if (printErrorStacktraces) {
                System.err.println(out.toString());
                System.err.println(verboseStackTrace);
            }

            String message = "An internal error occurred" + errMsg + "\nPlease report an issue at https://github.com/oracle/fastr including the commands";
            // if ctx == null and we can't determine if stactrace should be logged, or not,
            // then at least print to file
            if (printErrorStacktracesToFile) {
                TruffleFile logfile = Utils.getLogPath(ctx, getLogFileName(contextId));
                if (logfile != null) {
                    message += " and the error log file '" + logfile + "'.";
                    try (BufferedWriter writer = logfile.newBufferedWriter(StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
                        writer.append(new Date().toString()).append('\n');
                        writer.append(out.toString()).append('\n');
                        writer.append(verboseStackTrace).append("\n\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    message += ". Cannot write error log file (tried current working directory, user home directory, FastR home directory).";
                }
                System.err.println(message);
                if (RContext.isEmbedded()) {
                    RSuicide.rSuicide("FastR internal error");
                }
            } else {
                message += ". You can rerun FastR with --R.PrintErrorStacktracesToFile=true" +
                                " to turn on internal errors logging. Please attach the log file to the issue if possible.";
            }
            if (!printErrorStacktraces && !printErrorStacktracesToFile) {
                System.err.println(message);
            }
        } catch (ExitException | ThreadDeath t) {
            throw t;
        } catch (Throwable t) {
            System.err.println("error while reporting internal error:");
            t.printStackTrace();
        }
    }

    private static boolean getOption(OptionKey<Boolean> key, RContext ctx) {
        if (ctx != null) {
            return ctx.getOption(key);
        } else {
            return (boolean) FastROptions.getEnvValue(FastROptions.getName(key), false);
        }
    }

    private static String getLogFileName(int contextId) {
        return contextId == 0 ? FASTR_ERRORS_LOG : FASTR_ERRORS_LOG + "-" + contextId;
    }
}
