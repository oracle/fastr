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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_CallFactory.ToNativeNodeGen;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_CallFactory.TruffleLLVM_InvokeCallNodeGen;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DLL.LLVM_Handle;
import com.oracle.truffle.r.ffi.impl.llvm.upcalls.BytesToNativeCharArrayCall;
import com.oracle.truffle.r.ffi.impl.llvm.upcalls.CharSXPToNativeArrayCall;
import com.oracle.truffle.r.ffi.impl.upcalls.Callbacks;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import static com.oracle.truffle.r.runtime.context.FastROptions.TraceNativeCalls;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RScalar;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIVariables;

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

        @Override
        public ContextState initialize(RContext contextA) {
            this.context = contextA;
            RFFIFactory.getCallRFFI();
            initCallbacks();
            return this;
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
                LLVM_Handle rdllInfo = (LLVM_Handle) DLL.getRdllInfo().handle;
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
        LLVM_Handle rdllInfo = (LLVM_Handle) DLL.getRdllInfo().handle;
        for (INIT_VAR_FUN initVarFun : INIT_VAR_FUN.values()) {
            try {
                initVarFun.symbolHandle = new SymbolHandle(interop.readMember(rdllInfo.handle, initVarFun.funName));
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
        RFFIVariables[] variables = RFFIVariables.initialize(context);
        boolean isNullSetting = RNull.setIsNull(context, false);
        try {
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
                    } else if (value instanceof TruffleObject) {
                        interop.execute(INIT_VAR_FUN.OBJ.symbolHandle.asTruffleObject(), i, value);
                    }
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        } finally {
            RNull.setIsNull(context, isNullSetting);
        }
    }

    abstract static class ToNativeNode extends Node {

        public abstract Object execute(Object value);

        @Specialization
        protected static Object convert(int value) {
            return RDataFactory.createIntVectorFromScalar(value);
        }

        @Specialization
        protected static Object convert(double value) {
            return RDataFactory.createDoubleVectorFromScalar(value);
        }

        @Specialization
        protected static Object convert(RAbstractVector value) {
            return value;
        }

        @Specialization
        protected static Object convert(RScalar value) {
            return value;
        }

        @Specialization
        protected static Object convert(byte value) {
            return RDataFactory.createLogicalVectorFromScalar(value);
        }

        @Specialization
        protected static Object convert(String value) {
            return RDataFactory.createStringVectorFromScalar(value);
        }

        @Fallback
        protected static Object convert(Object value) {
            return value;
        }
    }

    abstract static class TruffleLLVM_InvokeCallNode extends Node implements InvokeCallNode {

        @Child private FFIUnwrapNode unwrap;
        @Child private PushCallbacksNode pushCallbacks = new PushCallbacksNode();
        @Child private PopCallbacksNode popCallbacks = new PopCallbacksNode();
        private final boolean isVoid;

        protected TruffleLLVM_InvokeCallNode(boolean isVoid) {
            this.isVoid = isVoid;
            this.unwrap = isVoid ? null : FFIUnwrapNode.create();
        }

        protected static ToNativeNode[] createConvertNodes(int length) {
            ToNativeNode[] result = new ToNativeNode[length];
            for (int i = 0; i < length; i++) {
                result[i] = ToNativeNodeGen.create();
            }
            return result;
        }

        @Override
        public Object dispatch(VirtualFrame frame, NativeCallInfo nativeCallInfo, Object[] args) {
            TruffleLLVM_Context rffiCtx = TruffleLLVM_Context.getContextState();
            pushCallbacks.execute(rffiCtx.callState.setCallbacksAddress, rffiCtx.callState.callbacks);
            try {
                return InvokeCallNode.super.dispatch(frame, nativeCallInfo, args);
            } finally {
                popCallbacks.execute();
            }
        }

        @Specialization(guards = {"cachedNativeCallInfo.name.equals(nativeCallInfo.name)", "args.length == cachedArgCount"})
        protected Object invokeCallCached(@SuppressWarnings("unused") NativeCallInfo nativeCallInfo, Object[] args,
                        @SuppressWarnings("unused") @Cached("nativeCallInfo") NativeCallInfo cachedNativeCallInfo,
                        @SuppressWarnings("unused") @Cached("argCount(args)") int cachedArgCount,
                        @Cached("createConvertNodes(cachedArgCount)") ToNativeNode[] convert,
                        @Cached("nativeCallInfo.address.asTruffleObject()") TruffleObject truffleObject,
                        @CachedLibrary("truffleObject") InteropLibrary interop) {
            return doInvoke(interop, truffleObject, args, convert);
        }

        @Specialization(replaces = "invokeCallCached")
        @TruffleBoundary
        protected Object invokeCallNormal(NativeCallInfo nativeCallInfo, Object[] args) {
            return doInvoke(InteropLibrary.getFactory().getUncached(), nativeCallInfo.address.asTruffleObject(), args, null);
        }

        @ExplodeLoop
        private Object doInvoke(InteropLibrary interop, TruffleObject truffleObject, Object[] args, ToNativeNode[] convert) {
            RContext ctx = RContext.getInstance();
            boolean isNullSetting = RNull.setIsNull(ctx, false);
            try {
                if (convert != null) {
                    for (int i = 0; i < convert.length; i++) {
                        args[i] = convert[i].execute(args[i]);
                    }
                }
                Object result = interop.execute(truffleObject, args);
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
                RNull.setIsNull(ctx, isNullSetting);
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
     *
     * @param function
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
