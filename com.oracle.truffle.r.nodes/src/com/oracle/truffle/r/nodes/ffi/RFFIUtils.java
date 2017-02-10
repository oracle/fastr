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
package com.oracle.truffle.r.nodes.ffi;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.ffi.UpCallsRFFI;

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
    /**
     * Set this to {@code true} when it is not possible to set {@link FastROptions}.
     */
    private static boolean alwaysTrace;
    /**
     * Is set by initialization and caches whether we are tracing.
     */
    private static boolean traceEnabled;

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
     * Handles the initialization of the RFFI downcalls/upcall implementation.
     *
     * @param upCallsRFFIImpl the concrete, implementation-specific variant of {@link UpCallsRFFI}.
     * @return if tracing is enabled an instance of {@link TracingUpCallsRFFIImpl} that wraps
     *         {@code upCallsRFFIImpl} else {@code upCallsRFFIImpl}.
     */
    public static UpCallsRFFI initialize(UpCallsRFFI upCallsRFFIImpl) {
        UpCallsRFFI returnUpCallsRFFIImpl = upCallsRFFIImpl;
        traceEnabled = alwaysTrace || FastROptions.TraceNativeCalls.getBooleanValue();
        if (traceEnabled) {
            if (traceStream == null) {
                initTraceStream();
            }
            returnUpCallsRFFIImpl = new TracingUpCallsRFFIImpl(upCallsRFFIImpl);
        }
        FFIUpCallRootNode.register();
        return returnUpCallsRFFIImpl;
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

    /**
     * Upcalled from native when tracing to get FD of the {@link #traceStream}. Allows the same fd
     * to be used on both sides of the JNI boundary.
     */
    @SuppressWarnings("unused")
    private static FileDescriptor getTraceFileDescriptor() {
        try {
            if (traceStream == null) {
                // Happens if native has tracing enabled and Java does not
                initTraceStream();
            }
            return traceStream.getFD();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
            return null;
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

    public static void traceUpCall(String name, Object... args) {
        traceCall(CallMode.UP, name, depth, args);
    }

    public static void traceUpCallReturn(String name, Object result) {
        traceCall(CallMode.UP_RETURN, name, depth, result);
    }

    public static void traceDownCall(String name, Object... args) {
        traceCall(CallMode.DOWN, name, ++depth, args);
    }

    public static void traceDownCallReturn(String name, Object result) {
        traceCall(CallMode.DOWN_RETURN, name, depth--, result);
    }

    public static boolean traceEnabled() {
        return traceEnabled;
    }

    private static void traceCall(CallMode mode, String name, int depthValue, Object... args) {
        assert initialized;
        if (traceEnabled) {
            StringBuffer sb = new StringBuffer();
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

    // Error handling
    static RuntimeException unimplemented() {
        return unimplemented("");
    }

    static RuntimeException unimplemented(String message) {
        throw RInternalError.unimplemented(message);
    }

    static void guarantee(boolean condition) {
        guarantee(condition, "");
    }

    static void guarantee(boolean condition, String message) {
        if (!condition) {
            unimplemented(message);
        }
    }

    public static <T> T guaranteeInstanceOf(Object x, Class<T> clazz) {
        if (x == null) {
            guarantee(false, "unexpected type: null instead of " + clazz.getSimpleName());
        } else if (!clazz.isInstance(x)) {
            guarantee(false, "unexpected type: " + x + " is " + x.getClass().getSimpleName() + " instead of " + clazz.getSimpleName());
        }
        return clazz.cast(x);
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
