/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.jnr;

import java.nio.file.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RPlatform.OSInfo;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ffi.*;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;
import com.oracle.truffle.r.runtime.ops.na.*;

/**
 * The only variety in the signatures for {@code .Call} is the number of arguments. GnuR supports a
 * maximum number of args. We could perhaps be clever using {@ode jnr-invoke} but for simple
 * functions a straight JNI interface is adequate.
 */
public class CallRFFIWithJNI implements CallRFFI {

    CallRFFIWithJNI() {
        loadLibrary();
    }

    @SlowPath
    private static void loadLibrary() {
        String rHome = REnvVars.rHome();
        String packageName = "com.oracle.truffle.r.native";
        OSInfo osInfo = RPlatform.getOSInfo();
        Path path = FileSystems.getDefault().getPath(rHome, packageName, "fficall", "jni", "lib", "libcall." + osInfo.libExt);
        System.load(path.toString());
    }

    /**
     * Helper function that handles {@link Integer} and {@link RIntVector} "vectors".
     *
     * @return value at logical index 0
     */
    public static int getIntDataAtZero(Object x) {
        if (x instanceof Integer) {
            return ((Integer) x).intValue();
        } else if (x instanceof RIntVector) {
            return ((RIntVector) x).getDataAt(0);
        } else {
            assert false;
            return 0;
        }
    }

    private static final NACheck elementNACheck = NACheck.create();

    /**
     * Helper function for updating arrays.
     */
    public static void updateIntDataAt(RIntVector x, int index, int value) {
        elementNACheck.enable(x);
        x.updateDataAt(index, value, elementNACheck);
    }

    // @formatter:off
    public Object invokeCall(SymbolInfo symbolInfo, Object[] args) throws Throwable {
        switch (args.length) {
            case 0: return call0(symbolInfo.getAddress());
            case 1: return call1(symbolInfo.getAddress(), args[0]);
            case 2: return call2(symbolInfo.getAddress(), args[0], args[1]);
            case 3: return call3(symbolInfo.getAddress(), args[0], args[1], args[2]);
            case 4: return call4(symbolInfo.getAddress(), args[0], args[1], args[2], args[3]);
            case 5: return call5(symbolInfo.getAddress(), args[0], args[1], args[2], args[3], args[4]);
            case 6: return call6(symbolInfo.getAddress(), args[0], args[1], args[2], args[3], args[4], args[5]);
            case 7: return call7(symbolInfo.getAddress(), args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            case 8: return call8(symbolInfo.getAddress(), args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
            case 9: return call9(symbolInfo.getAddress(), args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
            default:
                assert false;
                return null;
        }
    }

    public Object invokeExternal(SymbolInfo symbolInfo, Object[] args) throws Throwable {
        assert false;
        return null;
    }

    private static native Object call(long address, Object[] args);
    private static native Object call0(long address);
    private static native Object call1(long address, Object arg1);
    private static native Object call2(long address, Object arg1, Object arg2);
    private static native Object call3(long address, Object arg1, Object arg2,  Object arg3);
    private static native Object call4(long address, Object arg1, Object arg2,  Object arg3,  Object arg4);
    private static native Object call5(long address, Object arg1, Object arg2,  Object arg3,  Object arg4,  Object arg5);
    private static native Object call6(long address, Object arg1, Object arg2,  Object arg3,  Object arg4,  Object arg5,  Object arg6);
    private static native Object call7(long address, Object arg1, Object arg2,  Object arg3,  Object arg4,  Object arg5,  Object arg6,  Object arg7);
    private static native Object call8(long address, Object arg1, Object arg2,  Object arg3,  Object arg4,  Object arg5,  Object arg6,  Object arg7,  Object arg8);
    private static native Object call9(long address, Object arg1, Object arg2,  Object arg3,  Object arg4,  Object arg5,  Object arg6,  Object arg7,  Object arg8,  Object arg9);

}
