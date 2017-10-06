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
package com.oracle.truffle.r.ffi.impl.common;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;

/**
 * Mostly support for tracing R FFI up/down calls. Currently tracing of the arguments to calls is
 * limited. The type of the argument is printed as is the value for types with simple (short)
 * values. Potentially complex types, e.g, {@link RPairList} do not have their values printed.
 *
 * Embedded mode requires special treatment. Experimentally the primary embedding app, RStudio, sets
 * a very constrained environment (i.e. does not propagate environment variables set in the shell
 * that launches RStudio.) and sets the cwd to "/", which is not writeable.
 */
public class RFFIUtils {
    /**
     * Set this to {@code true} when it is not possible to set {@link FastROptions}.
     */
    private static boolean alwaysTrace;
    /**
     * Is set by initialization and caches whether we are tracing.
     */
    @CompilationFinal public static boolean traceEnabled;

    public static boolean traceInitialized;

    /**
     * Always trace to a file because stdout is problematic for embedded mode.
     */
    private static final String TRACEFILE = "fastr_trace_nativecalls.log";
    private static FileOutputStream traceStream;
    /**
     * Records the call depth. TBD: make context specific
     */
    private static int depth;

    /**
     * Handles the initialization of the RFFI downcalls/upcall tracing implementation.
     */
    public static synchronized void initializeTracing() {
        if (!traceInitialized) {
            traceInitialized = true;
            traceEnabled = alwaysTrace || FastROptions.TraceNativeCalls.getBooleanValue();
            if (traceEnabled) {
                if (traceStream == null) {
                    initTraceStream();
                }
            }
        }
    }

    private static void initTraceStream() {
        Path tracePath = Utils.getLogPath(TRACEFILE);
        try {
            traceStream = new FileOutputStream(tracePath.toString());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    private enum CallMode {
        UP("U"),
        UP_RETURN("UR"),
        DOWN("D"),
        DOWN_RETURN("DR");

        private final String printName;

        CallMode(String printName) {
            this.printName = printName;
        }
    }

    @TruffleBoundary
    public static void traceUpCall(String name, List<Object> args) {
        traceCall(CallMode.UP, name, depth, args.toArray());
    }

    @TruffleBoundary
    public static void traceUpCallReturn(String name, Object result) {
        traceCall(CallMode.UP_RETURN, name, depth, result);
    }

    @TruffleBoundary
    public static void traceDownCall(String name, Object... args) {
        traceCall(CallMode.DOWN, name, ++depth, args);
    }

    @TruffleBoundary
    public static void traceDownCallReturn(String name, Object result) {
        traceCall(CallMode.DOWN_RETURN, name, depth--, result);
    }

    public static boolean traceEnabled() {
        return traceEnabled;
    }

    private static void traceCall(CallMode mode, String name, int depthValue, Object... args) {
        if (traceEnabled) {
            StringBuilder sb = new StringBuilder();
            sb.append("CallRFFI[");
            sb.append(mode.printName);
            sb.append(':');
            sb.append(depthValue);
            sb.append(']');
            sb.append(name);
            sb.append('(');
            printArgs(sb, args);
            sb.append(')');
            try {
                traceStream.write(sb.toString().getBytes());
                traceStream.write('\n');
                traceStream.flush();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    private static void printArgs(StringBuilder sb, Object[] args) {
        boolean first = true;
        for (Object arg : args) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(arg == null ? "" : arg.getClass().getSimpleName());
            if (arg instanceof RSymbol) {
                RSymbol symbol = (RSymbol) arg;
                sb.append("(\"" + symbol.getName() + "\")");
            }
            if (!(arg instanceof RTypedValue)) {
                sb.append("(" + arg + ")");
            }
        }
    }

    // Error handling
    static RuntimeException unimplemented() {
        CompilerDirectives.transferToInterpreter();
        return unimplemented("");
    }

    static RuntimeException unimplemented(String message) {
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.unimplemented(message);
    }

    static void guarantee(boolean condition) {
        guarantee(condition, "");
    }

    static void guarantee(boolean condition, String message) {
        if (!condition) {
            CompilerDirectives.transferToInterpreter();
            unimplemented(message);
        }
    }

    public static <T> T guaranteeInstanceOf(Object x, Class<T> clazz) {
        if (x == null) {
            CompilerDirectives.transferToInterpreter();
            unimplemented("unexpected type: null instead of " + clazz.getSimpleName());
        } else if (!clazz.isInstance(x)) {
            CompilerDirectives.transferToInterpreter();
            unimplemented("unexpected type: " + x + " is " + x.getClass().getSimpleName() + " instead of " + clazz.getSimpleName());
        }
        return clazz.cast(x);
    }
}
