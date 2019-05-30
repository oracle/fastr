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
package com.oracle.truffle.r.ffi.impl.llvm;

import java.nio.file.FileSystems;
import java.util.EnumMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DLL.LLVM_Handle;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;
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
public final class TruffleLLVM_Context extends RFFIContext {

    private final TruffleLLVM_DLL.ContextStateImpl dllState = new TruffleLLVM_DLL.ContextStateImpl();
    final TruffleLLVM_Call.ContextStateImpl callState = new TruffleLLVM_Call.ContextStateImpl();

    public TruffleLLVM_Context() {
        this(new RFFIContextState());
    }

    public TruffleLLVM_Context(RFFIContextState rffiContextState) {
        super(rffiContextState, new TruffleLLVM_C(), new BaseRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE, TruffleLLVM_DownCallNodeFactory.INSTANCE), new TruffleLLVM_Call(), new TruffleLLVM_DLL(),
                        new TruffleLLVM_UserRng(),
                        new ZipRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE), new PCRERFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE),
                        new LapackRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE), new StatsRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE),
                        new ToolsRFFI(), new REmbedRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE), new MiscRFFI(TruffleLLVM_DownCallNodeFactory.INSTANCE));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends RFFIContext> C as(Class<C> rffiCtxClass) {
        assert rffiCtxClass == TruffleLLVM_Context.class;
        return (C) this;
    }

    static TruffleLLVM_Context getContextState() {
        return RContext.getInstance().getStateRFFI(TruffleLLVM_Context.class);
    }

    static TruffleLLVM_Context getContextState(RContext context) {
        return context.getStateRFFI(TruffleLLVM_Context.class);
    }

    @Override
    public ContextState initialize(RContext context) {

        // Load the f2c runtime library
        String libf2cPath = FileSystems.getDefault().getPath(REnvVars.rHome(), "lib", "libf2c.so").toString();
        DLLRFFI.DLOpenRootNode.create(context).call(libf2cPath, false, false);

        // Load dependencies that don't automatically get loaded:
        TruffleLLVM_Lapack.load();

        if (context.isInitial()) {
            String librffiPath = LibPaths.getBuiltinLibPath("R");
            loadLibR(context, librffiPath);
        }

        dllState.initialize(context);
        callState.initialize(context);
        return this;
    }

    @Override
    public void initializeVariables(RContext context) {
        super.initializeVariables(context);
        callState.initializeVariables();
    }

    @Override
    public void beforeDispose(RContext context) {
        dllState.beforeDispose(context);
        callState.beforeDispose(context);
    }

    private final EnumMap<NativeFunction, TruffleObject> nativeFunctions = new EnumMap<>(NativeFunction.class);

    @Override
    public TruffleObject lookupNativeFunction(NativeFunction function) {
        CompilerAsserts.neverPartOfCompilation();
        if (!nativeFunctions.containsKey(function)) {
            Object lookupObject;
            if (Utils.identityEquals(function.getLibrary(), NativeFunction.baseLibrary())) {
                lookupObject = ((LLVM_Handle) DLL.getRdllInfo().handle).handle;
            } else if (Utils.identityEquals(function.getLibrary(), NativeFunction.anyLibrary())) {
                DLLInfo dllInfo = DLL.findLibraryContainingSymbol(RContext.getInstance(), function.getCallName());
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
            } catch (UnsupportedMessageException|UnknownIdentifierException e) {
                RInternalError.shouldNotReachHere();
            }
            nativeFunctions.put(function, target);
        }
        return nativeFunctions.get(function);
    }
}
