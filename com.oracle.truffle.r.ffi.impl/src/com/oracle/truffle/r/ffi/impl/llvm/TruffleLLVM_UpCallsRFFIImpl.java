/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.ffi.impl.common.JavaUpCallsRFFIImpl;
import com.oracle.truffle.r.ffi.impl.common.RFFIUtils;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DLL.LLVM_Handle;
import com.oracle.truffle.r.ffi.impl.upcalls.Callbacks;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RScalar;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNodeGen;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.VectorRFFIWrapper;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

/**
 * (Incomplete) Variant of {@link JavaUpCallsRFFIImpl} for Truffle LLVM.
 *
 */
public class TruffleLLVM_UpCallsRFFIImpl extends JavaUpCallsRFFIImpl {

    private TruffleObject setSymbolHandle;

    void initialize() {
        LLVM_Handle rdllInfo = (LLVM_Handle) DLL.getRdllInfo().handle;
        try {
            setSymbolHandle = (TruffleObject) rdllInfo.parsedIRs[0].lookup("Rdynload_setSymbol");
        } catch (Exception e) {
            RInternalError.shouldNotReachHere(e);
        }
    }

    @Override
    public RFFIFactory.Type getRFFIType() {
        return RFFIFactory.Type.LLVM;
    }

    public Object charSXPToNativeCharArray(Object x) {
        CharSXPWrapper chars = RFFIUtils.guaranteeInstanceOf(x, CharSXPWrapper.class);
        return new NativeCharArray(chars.getContents().getBytes());
    }

    public Object bytesToNativeCharArray(byte[] bytes) {
        Object result = new NativeCharArray(bytes);
        return result;
    }

    @Override
    public HandleUpCallExceptionNode createHandleUpCallExceptionNode() {
        return new HandleLLVMUpCallExceptionNode();
    }

    // Checkstyle: stop method name check

    @Override
    public Object Rf_mkCharLenCE(Object obj, int len, int encoding) {
        if (obj instanceof VectorRFFIWrapper) {
            Object wrappedCharSXP = ((VectorRFFIWrapper) obj).getVector();
            assert wrappedCharSXP instanceof CharSXPWrapper;
            return wrappedCharSXP;
        } else if (obj instanceof NativeCharArray) {
            byte[] bytes = ((NativeCharArray) obj).getValue();
            return super.Rf_mkCharLenCE(bytes, bytes.length, encoding);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    @Override
    @TruffleBoundary
    public Object R_Home() {
        byte[] sbytes = REnvVars.rHome().getBytes();
        return new NativeCharArray(sbytes);
    }

    @Override
    public Object Rf_findVar(Object symbolArg, Object envArg) {
        Object v = super.Rf_findVar(symbolArg, envArg);
        if (v instanceof RTypedValue) {
            return v;
        } else {
            return wrapPrimitive(v);
        }
    }

    private static RScalar wrapPrimitive(Object x) {
        if (x instanceof Double) {
            return RDouble.valueOf((double) x);
        } else if (x instanceof Integer) {
            return RInteger.valueOf((int) x);
        } else if (x instanceof Byte) {
            return RLogical.valueOf((byte) x);
        } else if (x instanceof String) {
            return RString.valueOf((String) x);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    public Object getCallback(int index) {
        return Callbacks.values()[index].call;
    }

    @Override
    public Object getCCallable(String pkgName, String functionName) {
        CEntry result = DLLInfo.lookupCEntry(pkgName, functionName);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, RError.Message.UNKNOWN_OBJECT, functionName);
        }
        return result.address.asTruffleObject();
    }

    @Override
    protected Object setSymbol(DLLInfo dllInfo, int nstOrd, Object routines, int index) {
        try {
            return FFIUnwrapNodeGen.getUncached().execute(InteropLibrary.getFactory().getUncached().execute(setSymbolHandle, dllInfo, nstOrd, routines, index));
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Override
    public Object R_alloc(int n, int size) {
        throw RInternalError.unimplemented("R_alloc for LLVM");
    }
}
