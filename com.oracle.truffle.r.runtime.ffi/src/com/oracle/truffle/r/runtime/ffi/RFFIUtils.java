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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;

public class RFFIUtils {
    /**
     * Set this to {@code true} when it is not possible to set {@link FastROptions}.
     */
    private static boolean alwaysTrace;

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

    /**
     * Places in /tmp because in embedded mode can't trust that cwd is writeable. Also, tag with
     * time in event of multiple concurrent instances.
     */
    private static final String tracePathPrefix = "/tmp/fastr_trace_nativecalls.log-";
    private static PrintWriter traceWriter;

    private static void initialize() {
        if (traceWriter == null) {
            String tracePath = tracePathPrefix + Long.toString(System.currentTimeMillis());
            try {
                traceWriter = new PrintWriter(new FileWriter(tracePath));
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
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

    private static void traceCall(CallMode mode, String name, Object... args) {
        if (alwaysTrace || FastROptions.TraceNativeCalls.getBooleanValue()) {
            initialize();
            StringBuffer sb = new StringBuffer();
            sb.append("CallRFFI[");
            sb.append(mode.printName);
            sb.append(']');
            sb.append(name);
            sb.append('(');
            printArgs(sb, args);
            sb.append(')');
            traceWriter.println(sb.toString());
            traceWriter.flush();
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
            if (arg instanceof RPairList) {
                sb.append("[");
                printArgs(sb, ((RPairList) arg).toRList().getDataCopy());
                sb.append("]");
            }
            if (arg instanceof RSymbol) {
                RSymbol symbol = (RSymbol) arg;
                sb.append("\"" + symbol.getName() + "\")");
            }
            if (!(arg instanceof RTypedValue)) {
                sb.append("(" + arg + ")");
            }
        }
    }
}
