/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.JavaUpCallsRFFIImpl;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.UnsafeAdapter;

public class TruffleNFI_UpCallsRFFIImpl extends JavaUpCallsRFFIImpl {

    private final Node asPointer = Message.AS_POINTER.createNode();

    @Override
    @TruffleBoundary
    public Object Rf_mkCharLenCE(Object bytes, int len, int encoding) {
        // "bytes" is actually a Long unboxed from a NativePointer
        // TODO: handle encoding properly
        long address;
        try {
            address = ForeignAccess.sendAsPointer(asPointer, (TruffleObject) bytes);
        } catch (UnsupportedMessageException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
        return CharSXPWrapper.create(TruffleNFI_Utils.getString(address, len));
    }

    @Override
    public Object R_alloc(int n, int size) {
        long result = UnsafeAdapter.UNSAFE.allocateMemory(n * size);
        getContext().transientAllocations.add(result);
        return result;
    }

    @Override
    @TruffleBoundary
    public Object getCCallable(String pkgName, String functionName) {
        DLLInfo lib = DLL.safeFindLibrary(pkgName);
        CEntry result = lib.lookupCEntry(functionName);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, RError.Message.UNKNOWN_OBJECT, functionName);
        }
        return result.address.value;
    }

    @Override
    @TruffleBoundary
    protected DotSymbol setSymbol(DLLInfo dllInfo, int nstOrd, Object routines, int index) {
        Node executeNode = Message.createExecute(4).createNode();
        try {
            return (DotSymbol) FFIUnwrapNode.unwrap(
                            ForeignAccess.sendExecute(executeNode, TruffleNFI_Context.getInstance().lookupNativeFunction(NativeFunction.Rdynload_setSymbol), dllInfo, nstOrd, routines, index));
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    private static TruffleNFI_Context getContext() {
        return (TruffleNFI_Context) RContext.getInstance().getStateRFFI();
    }

}
