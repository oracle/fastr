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

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RTypedValue;

public class RFFIUtils {
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

    public static void traceCall(String name, Object... args) {
        if (FastROptions.TraceNativeCalls.getBooleanValue()) {
            System.out.print("CallRFFI " + name + ": ");
            printArgs(args);
            System.out.println();
        }
    }

    private static void printArgs(Object[] args) {
        for (Object arg : args) {
            System.out.print(" ");
            System.out.print(arg == null ? "" : arg.getClass().getSimpleName());
            if (arg instanceof RPairList) {
                System.out.print("[");
                printArgs(((RPairList) arg).toRList().getDataCopy());
                System.out.print("]");
            }
            if (!(arg instanceof RTypedValue)) {
                System.out.print("(" + arg + ")");
            }
        }
    }
}
