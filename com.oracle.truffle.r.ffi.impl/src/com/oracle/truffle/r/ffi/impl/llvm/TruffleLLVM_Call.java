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

import static com.oracle.truffle.r.runtime.context.FastROptions.TraceNativeCalls;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_CallFactory.TruffleLLVM_InvokeCallNodeGen;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DLL.LLVM_Handle;
import com.oracle.truffle.r.ffi.impl.llvm.upcalls.BytesToNativeCharArrayCall;
import com.oracle.truffle.r.ffi.impl.llvm.upcalls.CharSXPToNativeArrayCall;
import com.oracle.truffle.r.ffi.impl.upcalls.Callbacks;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.FFIMaterializeNode;
import com.oracle.truffle.r.runtime.ffi.FFIToNativeMirrorNode;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.FFIWrap.FFIDownCallWrap;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIVariables;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

public final class TruffleLLVM_Call implements CallRFFI {

    public TruffleLLVM_Call() {
        if (RContext.getInstance().getOption(TraceNativeCalls)) {
            System.out.println("WARNING: The TraceNativeCalls option was discontinued!\n" +
                            "You can rerun FastR with --log.R." + RLogger.LOGGER_RFFI + ".level=FINE --log.file=<yourfile>.\n" +
                            "NOTE that stdout is problematic for embedded mode, when using this logger, also always specify a log file");
        }
    }

    static final class ContextStateImpl implements RContext.ContextState {
        final TruffleLLVM_UpCallsRFFIImpl upCallsRFFIImpl = new TruffleLLVM_UpCallsRFFIImpl();
        private RContext context;
        private boolean initVarsDone;
        private TruffleObject setCallbacksAddress;
        private TruffleObject callbacks;

        @CompilationFinal public Object doubleArrayType;
        @CompilationFinal public Object intArrayType;
        @CompilationFinal public Object longArrayType;
        @CompilationFinal public Object byteArrayType;

        @Override
        public ContextState initialize(RContext contextA) {
            this.context = contextA;
            RFFIFactory.getCallRFFI();
            initCallbacks();

            try {
                InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                LLVM_Handle rdllInfo = (LLVM_Handle) contextA.stateDLL.getLibR().handle;
                doubleArrayType = findAndExecute(interop, rdllInfo.handle, "get_double_array_sulong_type");
                intArrayType = findAndExecute(interop, rdllInfo.handle, "get_i32_array_sulong_type");
                longArrayType = findAndExecute(interop, rdllInfo.handle, "get_i64_array_sulong_type");
                byteArrayType = findAndExecute(interop, rdllInfo.handle, "get_byte_array_sulong_type");
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
            return this;
        }

        private static Object findAndExecute(InteropLibrary interop, Object handle, String name) throws InteropException {
            return interop.execute(interop.readMember(handle, name));
        }

        public void initializeVariables() {
            if (!initVarsDone) {
                initVariables(context);
                initVarsDone = true;
            }
        }

        private void initCallbacks() {
            try {
                Callbacks.createCalls(upCallsRFFIImpl);

                TruffleObject[] callbacksArray = new TruffleObject[Callbacks.values().length + 2];
                for (Callbacks callback : Callbacks.values()) {
                    callbacksArray[callback.ordinal()] = callback.call;
                }
                callbacksArray[Callbacks.values().length] = new BytesToNativeCharArrayCall(upCallsRFFIImpl);
                callbacksArray[Callbacks.values().length + 1] = new CharSXPToNativeArrayCall(upCallsRFFIImpl);

                callbacks = (TruffleObject) context.getEnv().asGuestValue(callbacksArray);

                InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                LLVM_Handle rdllInfo = (LLVM_Handle) context.stateDLL.getLibR().handle;
                Object setClbkAddr = interop.readMember(rdllInfo.handle, "Rinternals_setCallbacksAddress");
                SymbolHandle setClbkAddrSymbolHandle = new SymbolHandle(setClbkAddr);
                setCallbacksAddress = setClbkAddrSymbolHandle.asTruffleObject();
                // Initialize the callbacks global variable
                interop.execute(setCallbacksAddress, context.getEnv().asGuestValue(new TruffleObject[0]));
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }

    }

    private enum INIT_VAR_FUN {
        DOUBLE,
        INT,
        STRING,
        OBJ;

        private final String funName;
        private SymbolHandle symbolHandle;

        INIT_VAR_FUN() {
            funName = "Call_initvar_" + name().toLowerCase();
        }

    }

    public static void initVariables(RContext context) {
        // must have parsed the variables module in libR
        InteropLibrary interop = InteropLibrary.getFactory().getUncached();
        LLVM_Handle rdllInfo = (LLVM_Handle) context.stateDLL.getLibR().handle;
        for (INIT_VAR_FUN initVarFun : INIT_VAR_FUN.values()) {
            try {
                initVarFun.symbolHandle = new SymbolHandle(interop.readMember(rdllInfo.handle, initVarFun.funName));
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
        RFFIVariables[] variables = RFFIVariables.initialize(context);
        for (int i = 0; i < variables.length; i++) {
            RFFIVariables var = variables[i];
            Object value = var.getValue();
            if (value == null) {
                continue;
            }
            try {
                if (value instanceof Double) {
                    interop.execute(INIT_VAR_FUN.DOUBLE.symbolHandle.asTruffleObject(), i, value);
                } else if (value instanceof Integer) {
                    interop.execute(INIT_VAR_FUN.INT.symbolHandle.asTruffleObject(), i, value);
                } else if (value instanceof String) {
                    interop.execute(INIT_VAR_FUN.STRING.symbolHandle.asTruffleObject(), i, new NativeCharArray((String) value));
                } else if (value instanceof TruffleObject) {
                    FFIDownCallWrap ffiWrap = new FFIDownCallWrap();
                    try {
                        interop.execute(INIT_VAR_FUN.OBJ.symbolHandle.asTruffleObject(), i, ffiWrap.wrapUncached(value));
                    } catch (Exception ex) {
                        throw RInternalError.shouldNotReachHere(ex);
                    } finally {
                        ffiWrap.close();
                    }
                }
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    @ImportStatic(DSLConfig.class)
    abstract static class TruffleLLVM_InvokeCallNode extends Node implements InvokeCallNode {

        @Child private FFIUnwrapNode unwrap;
        @Child private PushCallbacksNode pushCallbacks = new PushCallbacksNode();
        @Child private PopCallbacksNode popCallbacks = new PopCallbacksNode();
        private final boolean isVoid;

        protected TruffleLLVM_InvokeCallNode(boolean isVoid) {
            this.isVoid = isVoid;
            this.unwrap = isVoid ? null : FFIUnwrapNode.create();
        }

        protected static FFIMaterializeNode[] createMaterializeNodes(int length) {
            FFIMaterializeNode[] result = new FFIMaterializeNode[length];
            for (int i = 0; i < length; i++) {
                result[i] = FFIMaterializeNode.create();
            }
            return result;
        }

        protected static FFIToNativeMirrorNode[] createWrapperNodes(int length) {
            FFIToNativeMirrorNode[] result = new FFIToNativeMirrorNode[length];
            for (int i = 0; i < length; i++) {
                result[i] = FFIToNativeMirrorNode.create();
            }
            return result;
        }

        @Override
        public Object dispatch(VirtualFrame frame, NativeCallInfo nativeCallInfo, Object[] args) {
            TruffleLLVM_Context rffiCtx = TruffleLLVM_Context.getContextState(RContext.getInstance(this));
            pushCallbacks.execute(rffiCtx.callState.setCallbacksAddress, rffiCtx.callState.callbacks);
            try {
                return InvokeCallNode.super.dispatch(frame, nativeCallInfo, args);
            } finally {
                popCallbacks.execute();
            }
        }

        @Specialization(guards = {"args.length == cachedArgCount", "cachedNativeCallInfo.name.equals(nativeCallInfo.name)"})
        protected Object invokeCallCached(@SuppressWarnings("unused") NativeCallInfo nativeCallInfo, Object[] args,
                        @SuppressWarnings("unused") @Cached("nativeCallInfo") NativeCallInfo cachedNativeCallInfo,
                        @SuppressWarnings("unused") @Cached("argCount(args)") int cachedArgCount,
                        @Cached("createMaterializeNodes(cachedArgCount)") FFIMaterializeNode[] ffiMaterializeNodes,
                        @Cached("createWrapperNodes(cachedArgCount)") FFIToNativeMirrorNode[] ffiWrapperNodes,
                        @Cached("nativeCallInfo.address.asTruffleObject()") TruffleObject truffleObject,
                        @CachedLibrary("truffleObject") InteropLibrary interop) {
            return doInvoke(interop, truffleObject, args, ffiMaterializeNodes, ffiWrapperNodes);
        }

        @Specialization(limit = "99", guards = "args.length == cachedArgCount")
        @TruffleBoundary
        protected Object invokeCallNormal(NativeCallInfo nativeCallInfo, Object[] args,
                        @SuppressWarnings("unused") @Cached("argCount(args)") int cachedArgCount,
                        @Cached("createMaterializeNodes(cachedArgCount)") FFIMaterializeNode[] ffiMaterializeNodes,
                        @Cached("createWrapperNodes(cachedArgCount)") FFIToNativeMirrorNode[] ffiToNativeMirrorNodes,
                        @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary interop) {
            return doInvoke(interop, nativeCallInfo.address.asTruffleObject(), args, ffiMaterializeNodes, ffiToNativeMirrorNodes);
        }

        private Object doInvoke(InteropLibrary interop, TruffleObject truffleObject, Object[] args, FFIMaterializeNode[] ffiMaterializeNodes, FFIToNativeMirrorNode[] ffiToNativeMirrorNodes) {
            FFIDownCallWrap ffiWrap;
            Object[] wrappedArgs;
            if (ffiToNativeMirrorNodes != null) {
                ffiWrap = new FFIDownCallWrap(args.length);
                wrappedArgs = ffiWrap.wrapAll(args, ffiMaterializeNodes, ffiToNativeMirrorNodes);
            } else {
                ffiWrap = null;
                wrappedArgs = args;
            }
            try {
                Object result = interop.execute(truffleObject, wrappedArgs);
                if (!isVoid) {
                    result = unwrap.execute(result);
                }
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } catch (Exception ex) {
                if (ex.getCause() instanceof RError) {
                    // Sulong wraps an error having occurred in an upcall, so let's propagate the
                    // wrapped one instead of the Sulong one.
                    throw (RError) ex.getCause();
                } else {
                    throw ex;
                }
            } finally {
                // FFIwrap holds the materialized values,
                // we have to keep them alive until the call returns
                CompilerDirectives.materialize(ffiWrap);
            }
        }

        public int argCount(Object[] args) {
            return args.length;
        }
    }

    private static final class TruffleLLVM_InvokeVoidCallNode extends Node implements InvokeVoidCallNode {
        @Child private TruffleLLVM_InvokeCallNode invokeCallNode = TruffleLLVM_InvokeCallNodeGen.create(true);

        @Override
        public void execute(VirtualFrame frame, NativeCallInfo nativeCallInfo, Object[] args) {
            invokeCallNode.dispatch(frame, nativeCallInfo, args);
        }
    }

    /**
     * Upcalled from Rinternal et al.
     */
    @SuppressWarnings("static-method")
    public void unimplemented(String function) {
        throw RInternalError.unimplemented("RFFI function: '" + function + "' not implemented");
    }

    @Override
    public InvokeCallNode createInvokeCallNode() {
        return TruffleLLVM_InvokeCallNodeGen.create(false);
    }

    @Override
    public InvokeVoidCallNode createInvokeVoidCallNode() {
        return new TruffleLLVM_InvokeVoidCallNode();
    }

    public static final class PushCallbacksNode extends Node {
        @Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());

        public void execute(TruffleObject setCallbacksAddress, TruffleObject callbacks) {
            try {
                interop.execute(setCallbacksAddress, callbacks);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    public static final class PopCallbacksNode extends Node {
        public void execute() {
        }
    }

}
