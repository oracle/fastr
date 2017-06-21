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
package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.RFFIUtils;
import com.oracle.truffle.r.ffi.impl.common.TruffleUnwrap;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_CallFactory.TruffleLLVM_InvokeCallNodeGen;
import com.oracle.truffle.r.ffi.impl.llvm.upcalls.BytesToNativeCharArrayCall;
import com.oracle.truffle.r.ffi.impl.llvm.upcalls.CharSXPToNativeArrayCall;
import com.oracle.truffle.r.ffi.impl.upcalls.Callbacks;
import com.oracle.truffle.r.ffi.impl.upcalls.UpCallsRFFI;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIVariables;

final class TruffleLLVM_Call implements CallRFFI {
    private static UpCallsRFFI upCallsRFFI;
    private static TruffleLLVM_UpCallsRFFIImpl upCallsRFFIImpl;

    TruffleLLVM_Call() {
        upCallsRFFIImpl = new TruffleLLVM_UpCallsRFFIImpl();
        upCallsRFFI = RFFIUtils.initialize(upCallsRFFIImpl);
    }

    static class ContextStateImpl implements RContext.ContextState {
        private RContext context;
        private boolean initDone;

        @Override
        public ContextState initialize(RContext contextA) {
            this.context = contextA;
            RFFIFactory.getRFFI().getCallRFFI();
            if (!initDone) {
                initVariables(context);
                initCallbacks(context, upCallsRFFI);
                initDone = true;
            }
            return this;
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

    private static void initVariables(RContext context) {
        // must have parsed the variables module in libR
        for (INIT_VAR_FUN initVarFun : INIT_VAR_FUN.values()) {
            initVarFun.symbolHandle = new SymbolHandle(context.getEnv().importSymbol("@" + initVarFun.funName));
        }
        Node executeNode = Message.createExecute(2).createNode();
        RFFIVariables[] variables = RFFIVariables.initialize();
        boolean isNullSetting = RContext.getRForeignAccessFactory().setIsNull(false);
        try {
            for (int i = 0; i < variables.length; i++) {
                RFFIVariables var = variables[i];
                Object value = var.getValue();
                if (value == null) {
                    continue;
                }
                try {
                    if (value instanceof Double) {
                        ForeignAccess.sendExecute(executeNode, INIT_VAR_FUN.DOUBLE.symbolHandle.asTruffleObject(), i, value);
                    } else if (value instanceof Integer) {
                        ForeignAccess.sendExecute(executeNode, INIT_VAR_FUN.INT.symbolHandle.asTruffleObject(), i, value);
                    } else if (value instanceof TruffleObject) {
                        ForeignAccess.sendExecute(executeNode, INIT_VAR_FUN.OBJ.symbolHandle.asTruffleObject(), i, value);
                    }
                } catch (Throwable t) {
                    throw RInternalError.shouldNotReachHere(t);
                }
            }
        } finally {
            RContext.getRForeignAccessFactory().setIsNull(isNullSetting);
        }
    }

    private static void initCallbacks(RContext context, UpCallsRFFI upCallsImpl) {
        Node executeNode = Message.createExecute(2).createNode();
        SymbolHandle symbolHandle = new SymbolHandle(context.getEnv().importSymbol("@" + "Rinternals_addCallback"));

        try {
            // standard callbacks
            Callbacks[] callbacks = Callbacks.values();
            Callbacks.createCalls(upCallsImpl);
            for (Callbacks callback : callbacks) {
                ForeignAccess.sendExecute(executeNode, symbolHandle.asTruffleObject(), callback.ordinal(), callback.call);
            }
            // llvm specific callbacks
            ForeignAccess.sendExecute(executeNode, symbolHandle.asTruffleObject(), callbacks.length, new BytesToNativeCharArrayCall(upCallsRFFIImpl));
            ForeignAccess.sendExecute(executeNode, symbolHandle.asTruffleObject(), callbacks.length + 1, new CharSXPToNativeArrayCall(upCallsRFFIImpl));
        } catch (Throwable t) {
            throw RInternalError.shouldNotReachHere(t);
        }
    }

    @ImportStatic({Message.class})
    public abstract static class TruffleLLVM_InvokeCallNode extends InvokeCallNode {
        private final boolean isVoid;

        protected TruffleLLVM_InvokeCallNode(boolean isVoid) {
            this.isVoid = isVoid;
        }

        @Specialization(guards = {"cachedNativeCallInfo.name.equals(nativeCallInfo.name)", "args.length == cachedArgCount"})
        protected Object invokeCallCached(NativeCallInfo nativeCallInfo, Object[] args,
                        @SuppressWarnings("unused") @Cached("nativeCallInfo") NativeCallInfo cachedNativeCallInfo,
                        @SuppressWarnings("unused") @Cached("argCount(args)") int cachedArgCount,
                        @Cached("createMessageNode(args)") Node cachedMessageNode) {
            return doInvoke(cachedMessageNode, nativeCallInfo, args);
        }

        @Specialization(replaces = "invokeCallCached")
        protected Object invokeCallNormal(NativeCallInfo nativeCallInfo, Object[] args) {
            return doInvoke(Message.createExecute(args.length).createNode(), nativeCallInfo, args);
        }

        private Object doInvoke(Node messageNode, NativeCallInfo nativeCallInfo, Object[] args) {
            boolean isNullSetting = RContext.getRForeignAccessFactory().setIsNull(false);
            try {
                Object result = ForeignAccess.sendExecute(messageNode, nativeCallInfo.address.asTruffleObject(), args);
                if (!isVoid) {
                    result = TruffleUnwrap.unwrap(result);
                }
                return result;
            } catch (InteropException t) {
                throw RInternalError.shouldNotReachHere(t);
            } finally {
                RContext.getRForeignAccessFactory().setIsNull(isNullSetting);
            }
        }

        public int argCount(Object[] args) {
            return args.length;
        }

        public Node createMessageNode(Object[] args) {
            return Message.createExecute(args.length).createNode();
        }

    }

    private static class TruffleLLVM_InvokeVoidCallNode extends InvokeVoidCallNode {
        @Child private TruffleLLVM_InvokeCallNode invokeCallNode = TruffleLLVM_InvokeCallNodeGen.create(true);

        @Override
        public synchronized void execute(NativeCallInfo nativeCallInfo, Object[] args) {
            invokeCallNode.execute(nativeCallInfo, args);
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
}
