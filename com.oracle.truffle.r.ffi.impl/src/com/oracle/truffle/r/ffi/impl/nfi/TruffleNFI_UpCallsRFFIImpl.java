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
package com.oracle.truffle.r.ffi.impl.nfi;

import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.guaranteeInstanceOf;

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.JavaUpCallsRFFIImpl;
import com.oracle.truffle.r.ffi.impl.common.RFFIUtils;
import com.oracle.truffle.r.ffi.impl.interop.UnsafeAdapter;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_C.StringWrapper;
import com.oracle.truffle.r.ffi.impl.upcalls.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

import sun.misc.Unsafe;

public class TruffleNFI_UpCallsRFFIImpl extends JavaUpCallsRFFIImpl {

    private static final String SETSYMBOL_SIGNATURE = "(pointer, sint32, pointer, sint32): pointer";
    private static TruffleObject setSymbolFunction;

    public TruffleNFI_UpCallsRFFIImpl() {
        Node bind = Message.createInvoke(1).createNode();
        SymbolHandle symbolHandle = DLL.findSymbol("Rdynload_setSymbol", null);
        try {
            setSymbolFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", SETSYMBOL_SIGNATURE);
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    private static Node asPointer = Message.AS_POINTER.createNode();

    @Override
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

    @MessageResolution(receiverType = VectorWrapper.class)
    public static class VectorWrapperMR {

        @Resolve(message = "IS_POINTER")
        public abstract static class IntVectorWrapperNativeIsPointerNode extends Node {
            protected Object access(@SuppressWarnings("unused") VectorWrapper receiver) {
                return true;
            }
        }

        @Resolve(message = "AS_POINTER")
        public abstract static class IntVectorWrapperNativeAsPointerNode extends Node {
            protected long access(VectorWrapper receiver) {
                RVector<?> v = receiver.vector;
                if (v instanceof RIntVector) {
                    return ((RIntVector) v).allocateNativeContents();
                } else if (v instanceof RLogicalVector) {
                    return ((RLogicalVector) v).allocateNativeContents();
                } else {
                    throw RInternalError.shouldNotReachHere();
                }
            }
        }

        @CanResolve
        public abstract static class VectorWrapperCheck extends Node {
            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof VectorWrapper;
            }
        }
    }

    public static final class VectorWrapper implements TruffleObject {

        private final RVector<?> vector;

        public VectorWrapper(RVector<?> vector) {
            this.vector = vector;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return VectorWrapperMRForeign.ACCESS;
        }
    }

    @Override
    public Object INTEGER(Object x) {
        // also handles LOGICAL
        assert x instanceof RIntVector || x instanceof RLogicalVector;
        return new VectorWrapper(guaranteeInstanceOf(x, RVector.class));
    }

    @Override
    public Object LOGICAL(Object x) {
        return new VectorWrapper(guaranteeInstanceOf(x, RLogicalVector.class));
    }

    @Override
    public Object REAL(Object x) {
        long arrayAddress = TruffleNFI_NativeArray.findArray(x);
        if (arrayAddress == 0) {
            System.out.println("getting REAL contents");
            Object array = super.REAL(x);
            arrayAddress = TruffleNFI_NativeArray.recordArray(x, array, SEXPTYPE.REALSXP);
        } else {
            TruffleNFI_Call.returnArrayExisting(SEXPTYPE.REALSXP, arrayAddress);
        }
        return x;

    }

    @Override
    public Object RAW(Object x) {
        long arrayAddress = TruffleNFI_NativeArray.findArray(x);
        if (arrayAddress == 0) {
            System.out.println("getting RAW contents");
            Object array = super.RAW(x);
            arrayAddress = TruffleNFI_NativeArray.recordArray(x, array, SEXPTYPE.RAWSXP);
        } else {
            TruffleNFI_Call.returnArrayExisting(SEXPTYPE.RAWSXP, arrayAddress);
        }
        return x;
    }

    @Override
    public Object R_CHAR(Object x) {
        long arrayAddress = TruffleNFI_NativeArray.findArray(x);
        if (arrayAddress == 0) {
            System.out.println("getting R_CHAR contents");
            CharSXPWrapper charSXP = (CharSXPWrapper) x;
            Object array = charSXP.getContents().getBytes();
            arrayAddress = TruffleNFI_NativeArray.recordArray(x, array, SEXPTYPE.CHARSXP);
        } else {
            TruffleNFI_Call.returnArrayExisting(SEXPTYPE.CHARSXP, arrayAddress);
        }
        return x;
    }

    @Override
    @TruffleBoundary
    public Object getCCallable(String pkgName, String functionName) {
        DLLInfo lib = DLL.safeFindLibrary(pkgName);
        CEntry result = lib.lookupCEntry(functionName);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, RError.Message.UNKNOWN_OBJECT, functionName);
        }
        return result.address.asAddress();
    }

    @Override
    protected DotSymbol setSymbol(DLLInfo dllInfo, int nstOrd, Object routines, int index) {
        Node executeNode = Message.createExecute(4).createNode();
        try {
            return (DotSymbol) FFIUnwrapNode.unwrap(ForeignAccess.sendExecute(executeNode, setSymbolFunction, dllInfo, nstOrd, routines, index));
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }
}
