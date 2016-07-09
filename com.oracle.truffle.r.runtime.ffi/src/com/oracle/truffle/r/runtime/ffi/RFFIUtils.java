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
package com.oracle.truffle.r.runtime.ffi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
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
 *
 */
public class RFFIUtils {
    private static boolean initialized;
    /**
     * Set this to {@code true} when it is not possible to set {@link FastROptions}.
     */
    private static boolean alwaysTrace;
    /**
     * Is set by initialization and caches whether we are tracing.
     */
    private static boolean traceEnabled;

    /**
     * In embedded mode can't trust that cwd is writeable, so output placed in /tmp. Also, tag with
     * time in event of multiple concurrent instances (which happens with RStudio).
     */
    private static final String tracePathPrefix = "/tmp/fastr_trace_nativecalls.log-";
    private static PrintStream traceStream;

    private static void initialize() {
        if (!initialized) {
            traceEnabled = alwaysTrace || FastROptions.TraceNativeCalls.getBooleanValue();
            if (traceEnabled) {
                if (RContext.isEmbedded()) {
                    if (traceStream == null) {
                        String tracePath = tracePathPrefix + Long.toString(System.currentTimeMillis());
                        try {
                            traceStream = new PrintStream(new FileOutputStream(tracePath));
                        } catch (IOException ex) {
                            System.err.println(ex.getMessage());
                            System.exit(1);
                        }
                    }
                } else {
                    traceStream = System.out;
                }
            }
            initialized = true;
        }
    }

    private enum CallMode {
        UP("Up"),
        UP_RETURN("UpReturn"),
        DOWN("Down"),
        DOWN_RETURN("DownReturn");

        private final String printName;

        CallMode(String printName) {
            this.printName = printName;
        }
    }

    public static void traceUpCall(String name, Object... args) {
        traceCall(CallMode.UP, name, args);
    }

    public static void traceUpCallReturn(String name, Object... args) {
        traceCall(CallMode.UP_RETURN, name, args);
    }

    public static void traceDownCall(String name, Object... args) {
        traceCall(CallMode.DOWN, name, args);
    }

    public static boolean traceEnabled() {
        return traceEnabled;
    }

    private static void traceCall(CallMode mode, String name, Object... args) {
        initialize();
        if (traceEnabled) {
            StringBuffer sb = new StringBuffer();
            sb.append("CallRFFI[");
            sb.append(mode.printName);
            sb.append(']');
            sb.append(name);
            sb.append('(');
            printArgs(sb, args);
            sb.append(')');
            traceStream.println(sb.toString());
            traceStream.flush();
        }
    }

    private static void printArgs(StringBuffer sb, Object[] args) {
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

    // Miscellaneous support functions

    public static byte[] wrapChar(char v) {
        return new byte[]{(byte) v};
    }

    public static int[] wrapInt(int v) {
        return new int[]{v};
    }

    public static double[] wrapDouble(double v) {
        return new double[]{v};
    }

    @TruffleBoundary
    public static IOException ioex(String errMsg) throws IOException {
        throw new IOException(errMsg);
    }

}
