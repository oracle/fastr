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
package com.oracle.truffle.r.runtime.ffi.jni;

import static com.oracle.truffle.r.runtime.ffi.RFFIUtils.traceDownCall;
import static com.oracle.truffle.r.runtime.ffi.RFFIUtils.traceDownCallReturn;
import static com.oracle.truffle.r.runtime.ffi.RFFIUtils.traceEnabled;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLException;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.LibPaths;
import com.oracle.truffle.r.runtime.ffi.RFFIUtils;
import com.oracle.truffle.r.runtime.ffi.RFFIVariables;

/**
 * The only variety in the signatures for {@code .Call} is the number of arguments. GnuR supports a
 * maximum number of args (64). This implementation passes up to 9 arguments explicitly; beyond 9
 * they are passed as an array and the JNI code has to call back to get the args (not very
 * efficient).
 *
 * The JNI layer is not (currently) MT safe, so all calls are single threaded.
 */
public class JNI_Call implements CallRFFI {

    protected JNI_Call() {
        loadLibrary();
    }

    private static final boolean ForceRTLDGlobal = false;

    /**
     * Load the {@code libR} library. N.B. this library defines some non-JNI global symbols that are
     * referenced by C code in R packages. Unfortunately, {@link System#load(String)} uses
     * {@code RTLD_LOCAL} with {@code dlopen}, so we have to load the library manually and set
     * {@code RTLD_GLOBAL}. However, a {@code dlopen} does not hook the JNI functions into the JVM,
     * so we have to do an additional {@code System.load} to achieve that.
     *
     * Before we do that we must load {@code libjniboot} because the implementation of
     * {@link DLLRFFI#dlopen} is called by {@link DLL#load} which uses JNI!
     */
    @TruffleBoundary
    private static void loadLibrary() {
        String libjnibootPath = LibPaths.getBuiltinLibPath("jniboot");
        System.load(libjnibootPath);

        String librffiPath = LibPaths.getBuiltinLibPath("R");
        try {
            DLL.load(librffiPath, ForceRTLDGlobal, false);
        } catch (DLLException ex) {
            throw new RInternalError(ex, "error while loading " + librffiPath);
        }
        System.load(librffiPath);
        RFFIUtils.initialize();
        if (traceEnabled()) {
            traceDownCall("initialize");
        }
        try {
            initialize(RFFIVariables.values());
        } finally {
            if (traceEnabled()) {
                traceDownCallReturn("initialize", null);
            }

        }
    }

    @Override
    @TruffleBoundary
    public synchronized Object invokeCall(SymbolHandle handleArg, String name, Object[] args) {
        long address = handleArg.asAddress();
        Object result = null;
        if (traceEnabled()) {
            traceDownCall(name, args);
        }
        try {
            switch (args.length) {
            // @formatter:off
            case 0: result = call0(address); break;
            case 1: result = call1(address, args[0]); break;
            case 2: result = call2(address, args[0], args[1]); break;
            case 3: result = call3(address, args[0], args[1], args[2]); break;
            case 4: result = call4(address, args[0], args[1], args[2], args[3]); break;
            case 5: result = call5(address, args[0], args[1], args[2], args[3], args[4]); break;
            case 6: result = call6(address, args[0], args[1], args[2], args[3], args[4], args[5]); break;
            case 7: result = call7(address, args[0], args[1], args[2], args[3], args[4], args[5], args[6]); break;
            case 8: result = call8(address, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]); break;
            case 9: result = call9(address, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]); break;
            default:
                result = call(address, args); break;
                // @formatter:on
            }
            return result;
        } finally {
            if (traceEnabled()) {
                traceDownCallReturn(name, result);
            }
        }
    }

    private static native void initialize(RFFIVariables[] variables);

    private static native void nativeSetTempDir(String tempDir);

    private static native void nativeSetInteractive(boolean interactive);

    private static native Object call(long address, Object[] args);

    private static native Object call0(long address);

    private static native Object call1(long address, Object arg1);

    private static native Object call2(long address, Object arg1, Object arg2);

    private static native Object call3(long address, Object arg1, Object arg2, Object arg3);

    private static native Object call4(long address, Object arg1, Object arg2, Object arg3, Object arg4);

    private static native Object call5(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    private static native Object call6(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    private static native Object call7(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    private static native Object call8(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8);

    private static native Object call9(long address, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9);

    @Override
    @TruffleBoundary
    public synchronized void invokeVoidCall(SymbolHandle handle, String name, Object[] args) {
        if (traceEnabled()) {
            traceDownCall(name, args);
        }
        long address = handle.asAddress();
        try {
            switch (args.length) {
                case 0:
                    callVoid0(address);
                    break;
                case 1:
                    callVoid1(address, args[0]);
                    break;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        } finally {
            if (traceEnabled()) {
                traceDownCallReturn(name, null);
            }
        }
    }

    private static native void callVoid0(long address);

    private static native void callVoid1(long address, Object arg1);

    @Override
    public synchronized void setTempDir(String tempDir) {
        if (traceEnabled()) {
            traceDownCall("setTempDir", tempDir);
        }
        RFFIVariables.setTempDir(tempDir);
        nativeSetTempDir(tempDir);
        if (traceEnabled()) {
            traceDownCallReturn("setTempDir", null);
        }
    }

    @Override
    public synchronized void setInteractive(boolean interactive) {
        if (traceEnabled()) {
            traceDownCall("setInteractive", interactive);
        }
        nativeSetInteractive(interactive);
        if (traceEnabled()) {
            traceDownCallReturn("setInteractive", null);
        }
    }

}
