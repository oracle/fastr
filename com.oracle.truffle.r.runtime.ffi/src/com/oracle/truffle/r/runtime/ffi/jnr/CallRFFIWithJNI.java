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
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RPlatform.OSInfo;
import com.oracle.truffle.r.runtime.ffi.*;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLException;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;

/**
 * The only variety in the signatures for {@code .Call} is the number of arguments. GnuR supports a
 * maximum number of args (64). This implementation passes up to 9 arguments explicitly; beyond 9
 * they are passed as an array and the JNI code has to call back to get the args (not very
 * efficient).
 */
public class CallRFFIWithJNI implements CallRFFI {

    CallRFFIWithJNI() {
        loadLibrary();
    }

    private static boolean FORCE_RTLD_GLOBAL = false;

    /**
     * Load the {@code librfficall} library. N.B. this library defines some non-JNI global symbols
     * that are referenced by C code in R packages. Unfortunately, {@link System#load(String)} uses
     * {@code RTLD_LOCAL} with {@code dlopen}, so we have to load the library manually and set
     * {@code RTLD_GLOBAL}. However, a {@code dlopen} does not hook the JNI functions into the JVM,
     * so we have to do an additional {@code System.load} to achieve that.
     */
    @SlowPath
    private static void loadLibrary() {
        String rHome = REnvVars.rHome();
        String packageName = "com.oracle.truffle.r.native";
        OSInfo osInfo = RPlatform.getOSInfo();
        Path path = FileSystems.getDefault().getPath(rHome, packageName, "builtinlibs", "lib", osInfo.osSubDir, "librfficall." + osInfo.libExt);
        try {
            DLL.load(path.toString(), FORCE_RTLD_GLOBAL, false);
        } catch (DLLException ex) {
            throw RError.error((SourceSection) null, ex);
        }
        System.load(path.toString());
        initialize();
    }

    // @formatter:off
    public Object invokeCall(SymbolInfo symbolInfo, Object[] args) throws Throwable {
        long address = symbolInfo.getAddress();
        switch (args.length) {
            case 0: return call0(address);
            case 1: return call1(address, args[0]);
            case 2: return call2(address, args[0], args[1]);
            case 3: return call3(address, args[0], args[1], args[2]);
            case 4: return call4(address, args[0], args[1], args[2], args[3]);
            case 5: return call5(address, args[0], args[1], args[2], args[3], args[4]);
            case 6: return call6(address, args[0], args[1], args[2], args[3], args[4], args[5]);
            case 7: return call7(address, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            case 8: return call8(address, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
            case 9: return call9(address, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
            default:
                return call(address, args);
        }
    }

    public Object invokeExternal(SymbolInfo symbolInfo, Object[] args) throws Throwable {
        assert false;
        return null;
    }

    private static native void initialize();
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
