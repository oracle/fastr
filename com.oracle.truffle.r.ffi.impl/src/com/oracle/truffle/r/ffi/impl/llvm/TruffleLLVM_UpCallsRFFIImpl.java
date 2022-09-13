/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.ffi.impl.common.JavaUpCallsRFFIImpl;
import com.oracle.truffle.r.ffi.impl.common.RFFIUtils;
import com.oracle.truffle.r.ffi.impl.upcalls.Callbacks;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.FFIWrap.FFIDownCallWrap;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RObjectDataPtr;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

/**
 * (Incomplete) Variant of {@link JavaUpCallsRFFIImpl} for Truffle LLVM.
 *
 */
public class TruffleLLVM_UpCallsRFFIImpl extends JavaUpCallsRFFIImpl {

    public TruffleObject setSymbolHandle;

    @Override
    public RFFIFactory.Type getRFFIType() {
        return RFFIFactory.Type.LLVM;
    }

    public Object charSXPToNativeCharArray(Object x) {
        CharSXPWrapper chars = RFFIUtils.guaranteeInstanceOf(x, CharSXPWrapper.class);
        return new NativeCharArray(chars.getContents().getBytes());
    }

    public Object bytesToNativeCharArray(byte[] bytes) {
        return new NativeCharArray(bytes);
    }

    @Override
    public HandleUpCallExceptionNode createHandleUpCallExceptionNode() {
        return new HandleLLVMUpCallExceptionNode();
    }

    @Override
    public HandleUpCallExceptionNode getUncachedHandleUpCallExceptionNode() {
        return new HandleLLVMUpCallExceptionNode();
    }

    // Checkstyle: stop method name check

    @Override
    public Object Rf_mkCharLenCE(Object obj, int len, int encoding) {
        if (obj instanceof RObjectDataPtr) {
            RBaseObject vector = ((RObjectDataPtr) obj).getVector();
            if (vector instanceof RAbstractContainer) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                Object vectorData = ((RAbstractContainer) vector).getData();
                byte[] byteArray = VectorDataLibrary.getFactory().getUncached().getReadonlyRawData(vectorData);
                assert byteArray[byteArray.length - 1] == '\0' : "Cannot make CHARSXP from bytes not terminated by \0";
                return CharSXPWrapper.create(new String(byteArray, 0, byteArray.length - 1));
            } else {
                assert vector instanceof CharSXPWrapper;
                return vector;
            }
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
        byte[] sbytes = REnvVars.rHome(RContext.getInstance()).getBytes();
        return new NativeCharArray(sbytes);
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
    protected void setSymbol(DLLInfo dllInfo, int nstOrd, Object routines, int index) {
        FFIDownCallWrap ffiWrap = new FFIDownCallWrap();
        try {
            InteropLibrary.getFactory().getUncached().execute(setSymbolHandle, ffiWrap.wrapUncached(dllInfo), nstOrd, routines, index);
        } catch (Exception ex) {
            throw RInternalError.shouldNotReachHere(ex);
        } finally {
            ffiWrap.close();
        }
    }

    @Override
    public Object R_alloc(int n, int size) {
        throw RInternalError.unimplemented("R_alloc for LLVM");
    }
}
