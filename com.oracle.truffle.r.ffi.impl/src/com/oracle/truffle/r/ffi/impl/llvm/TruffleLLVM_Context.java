/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.ffi.impl.altrep.AltrepDownCallNodeFactoryImpl;
import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DLL.LLVM_Handle;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.AfterDownCallProfiles;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNodeGen;
import com.oracle.truffle.r.runtime.ffi.FFIWrap;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.PCRE2RFFI;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;

/**
 * A facade for the context state for the Truffle LLVM factory. Delegates to the various
 * module-specific pieces of state.
 */
public class TruffleLLVM_Context extends RFFIContext {

    final TruffleLLVM_Call.ContextStateImpl callState = new TruffleLLVM_Call.ContextStateImpl();

    public TruffleLLVM_Context() {
        this(new RFFIContextState());
    }

    public TruffleLLVM_Context(RFFIContextState rffiContextState) {
        super(rffiContextState, new TruffleLLVM_C(), new BaseRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE, TruffleLLVM_DownCallNodeFactory.INSTANCE),
                        new AltrepRFFI(AltrepDownCallNodeFactoryImpl.INSTANCE),
                        new TruffleLLVM_Call(), new TruffleLLVM_DLL(),
                        new TruffleLLVM_UserRng(),
                        new ZipRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE),
                        new PCRE2RFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE),
                        new LapackRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE),
                        new StatsRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE), new ToolsRFFI(), new REmbedRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE),
                        new MiscRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends RFFIContext> C as(Class<C> rffiCtxClass) {
        assert rffiCtxClass == TruffleLLVM_Context.class;
        return (C) this;
    }

    static TruffleLLVM_Context getContextState(RContext context) {
        return context.getStateRFFI(TruffleLLVM_Context.class);
    }

    protected void addLibRToDLLContextState(RContext context, DLLInfo libR) {
        context.stateDLL.addLibR(libR);
    }

    @Override
    public ContextState initialize(RContext context) {
        // For now, the LLVM context is only used in the mixed mode, where we first load NFI
        // NFI loads libR, which links with libRlapack and libRblas, therefore these native
        // libraries are also loaded for us.
        // Note that tricky thing w.r.t. libRlapack/blas is that they depend on "xerbla_", which is
        // defined in libR itself. The native loader can probably deal with this dependency cycle,
        // while Sulong cannot at the moment.
        // We build the libR.sol on purpose without liking it to libRblas and libRlapack, so that
        // Sulong is not trying to load these here.
        if (context.isInitial()) {
            TruffleLLVM_DLL.dlOpen(context, LibPaths.getBuiltinLibPath(context, "f2c"));
            String librffiPath = LibPaths.getBuiltinLibPath(context, "R");
            DLLInfo libR = DLL.loadLibR(context, librffiPath, path -> TruffleLLVM_DLL.dlOpen(context, path));
            addLibRToDLLContextState(context, libR);
        }
        callState.initialize(context);
        return this;
    }

    @Override
    public void initializeVariables(RContext context) {
        super.initializeVariables(context);
        callState.initializeVariables();
    }

    @Override
    public Object getSulongArrayType(Object arrayElem) {
        if (arrayElem instanceof Double) {
            return callState.doubleArrayType;
        } else if (arrayElem instanceof Integer) {
            return callState.intArrayType;
        } else if (arrayElem instanceof Byte) {
            return callState.byteArrayType;
        } else if (arrayElem instanceof Long) {
            return callState.longArrayType;
        }
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere("No array type for " + arrayElem);
    }

    @Override
    public Object callNativeFunction(Object nativeFunc, Type nativeFuncType, String signature, Object[] args, boolean[] whichArgToWrap) {
        assert nativeFuncType == Type.LLVM;
        InteropLibrary interop = InteropLibrary.getUncached();
        assert interop.isExecutable(nativeFunc);
        Object before = beforeDowncall(null, Type.LLVM);
        FFIWrap.FFIDownCallWrap ffiWrap = new FFIWrap.FFIDownCallWrap(args.length);
        Object[] wrappedArgs = ffiWrap.wrapSomeUncached(args, whichArgToWrap);
        Object ret;
        try {
            ret = interop.execute(nativeFunc, wrappedArgs);
        } catch(InteropException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
        ret = FFIUnwrapNodeGen.getUncached().execute(ret);
        afterDowncall(before, Type.LLVM, AfterDownCallProfiles.getUncached());
        return ret;
    }

    @Override
    public void beforeDispose(RContext context) {
        callState.beforeDispose(context);
    }

    @CompilationFinal(dimensions = 1) private final TruffleObject[] nativeFunctions = new TruffleObject[NativeFunction.values().length];

    @Override
    public TruffleObject lookupNativeFunction(NativeFunction function, RContext ctx) {
        int index = function.ordinal();
        if (nativeFunctions[index] == null) {
            // one-off event:
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Object lookupObject;
            if (Utils.identityEquals(function.getLibrary(), NativeFunction.baseLibrary())) {
                lookupObject = ((LLVM_Handle) DLL.getRdllInfo().handle).handle;
            } else if (Utils.identityEquals(function.getLibrary(), NativeFunction.anyLibrary())) {
                DLLInfo dllInfo = DLL.findLibraryContainingSymbol(ctx, function.getCallName());
                if (dllInfo == null) {
                    throw RInternalError.shouldNotReachHere("Could not find library containing symbol " + function.getCallName());
                }
                lookupObject = ((LLVM_Handle) dllInfo.handle).handle;
            } else {
                DLLInfo dllInfo = DLL.findLibrary(function.getLibrary());
                if (dllInfo == null) {
                    throw RInternalError.shouldNotReachHere("Could not find library  " + function.getLibrary());
                }
                lookupObject = ((LLVM_Handle) dllInfo.handle).handle;
            }
            TruffleObject target = null;
            try {
                target = (TruffleObject) InteropLibrary.getFactory().getUncached().readMember(lookupObject, function.getCallName());
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            nativeFunctions[index] = target;
        }
        return nativeFunctions[index];
    }

    @Override
    public Type getDefaultRFFIType() {
        return Type.LLVM;
    }
}
