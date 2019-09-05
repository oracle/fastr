/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nfi;

import java.util.concurrent.atomic.AtomicReference;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.ffi.impl.common.JavaUpCallsRFFIImpl;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.FFIWrap;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.UnsafeAdapter;

public class TruffleNFI_UpCallsRFFIImpl extends JavaUpCallsRFFIImpl {

    private AtomicReference<CallTarget> setSymbolCallTarget = new AtomicReference<>();

    @Override
    public RFFIFactory.Type getRFFIType() {
        return RFFIFactory.Type.NFI;
    }

    @Override
    @TruffleBoundary
    public Object Rf_mkCharLenCE(Object bytes, int len, int encoding) {
        // "bytes" is actually a Long unboxed from a NativePointer
        // TODO: handle encoding properly
        long address;
        try {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            if (!interop.isPointer(bytes)) {
                interop.toNative(bytes);
            }
            address = interop.asPointer(bytes);
        } catch (UnsupportedMessageException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
        return CharSXPWrapper.create(TruffleNFI_Utils.getString(address, len));
    }

    @Override
    public Object R_alloc(int n, int size) {
        long result = UnsafeAdapter.UNSAFE.allocateMemory(n * size);
        getContext().transientAllocations.peek().add(result);
        return result;
    }

    @Override
    @TruffleBoundary
    public Object getCCallable(String pkgName, String functionName) {
        CEntry result = DLLInfo.lookupCEntry(pkgName, functionName);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, RError.Message.UNKNOWN_OBJECT, functionName);
        }
        return result.address.value;
    }

    @Override
    @TruffleBoundary
    protected void setSymbol(DLLInfo dllInfo, int nstOrd, Object routines, int index) {
        setSymbolCallTarget.compareAndSet(null, Truffle.getRuntime().createCallTarget(new SetSymbolRootNode()));
        setSymbolCallTarget.get().call(dllInfo, nstOrd, routines, index);
    }

    private class SetSymbolRootNode extends RootNode {
        @CompilationFinal TruffleObject setSymFun;
        @Child private InteropLibrary interop;

        SetSymbolRootNode() {
            super(null);
            setSymFun = lookupSetSymbol();
            interop = InteropLibrary.getFactory().create(setSymFun);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            FFIWrap ffiWrap = new FFIWrap();
            try {
                Object[] args = frame.getArguments();
                // first arg is DLLInfo - have to wrapUncached it for native
                args[0] = ffiWrap.wrapUncached(args[0]);
                interop.execute(setSymFun, args);
                return RNull.instance;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                // FFIwrap holds the materialized values,
                // we have to keep them alive until the call returns
                CompilerDirectives.materialize(ffiWrap);
            }
        }
    }

    @TruffleBoundary
    private static TruffleObject lookupSetSymbol() {
        return TruffleNFI_Context.getInstance().lookupNativeFunction(NativeFunction.Rdynload_setSymbol);
    }

    @Override
    public HandleUpCallExceptionNode createHandleUpCallExceptionNode() {
        return HandleNFIUpCallExceptionNodeGen.create();
    }

    @Override
    public HandleUpCallExceptionNode getUncachedHandleUpCallExceptionNode() {
        return HandleNFIUpCallExceptionNodeGen.getUncached();
    }

    private static TruffleNFI_Context getContext() {
        return RContext.getInstance().getStateRFFI(TruffleNFI_Context.class);
    }

}
