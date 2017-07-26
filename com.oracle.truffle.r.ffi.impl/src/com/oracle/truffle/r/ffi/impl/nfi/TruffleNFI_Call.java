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

import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.traceDownCall;
import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.traceDownCallReturn;
import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.traceEnabled;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.RFFIUtils;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_CallFactory.TruffleNFI_InvokeCallNodeGen;
import com.oracle.truffle.r.ffi.impl.upcalls.Callbacks;
import com.oracle.truffle.r.ffi.impl.upcalls.UpCallsRFFI;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIVariables;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

public class TruffleNFI_Call implements CallRFFI {

    private enum INIT_VAR_FUN {
        OBJ("(env, sint32, object) : void"),
        DOUBLE("(sint32, double): void"),
        STRING("(sint32, string): void"),
        INT("(sint32, sint32) : void");

        private final String funName;
        private TruffleObject initFunction;
        private final String signature;

        INIT_VAR_FUN(String signature) {
            this.signature = signature;
            funName = "Call_initvar_" + name().toLowerCase();
        }
    }

    /**
     * Nesting of native calls is rare but can happen and the cleanup needs to be per call.
     */
    private static int callDepth;

    public TruffleNFI_Call() {
        initialize();
        TruffleNFI_PkgInit.initialize();
    }

    private static void initVariables() {
        Node bind = Message.createInvoke(1).createNode();
        for (INIT_VAR_FUN initVarFun : INIT_VAR_FUN.values()) {
            SymbolHandle symbolHandle = DLL.findSymbol(initVarFun.funName, null); // libR
            try {
                initVarFun.initFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", initVarFun.signature);
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere(t);
            }
        }
        Node executeNode = Message.createExecute(2).createNode();
        RFFIVariables[] variables = RFFIVariables.initialize();
        boolean isNullSetting = RContext.getRForeignAccessFactory().setIsNull(false);
        try {
            for (int i = 0; i < variables.length; i++) {
                RFFIVariables var = variables[i];
                Object value = var.getValue();
                if (value == null || var.alwaysUpCall) {
                    continue;
                }
                try {
                    if (value instanceof Double) {
                        ForeignAccess.sendExecute(executeNode, INIT_VAR_FUN.DOUBLE.initFunction, i, value);
                    } else if (value instanceof Integer) {
                        ForeignAccess.sendExecute(executeNode, INIT_VAR_FUN.INT.initFunction, i, value);
                    } else if (value instanceof String) {
                        ForeignAccess.sendExecute(executeNode, INIT_VAR_FUN.STRING.initFunction, i, value);
                    } else {
                        ForeignAccess.sendExecute(executeNode, INIT_VAR_FUN.OBJ.initFunction, i, value);
                    }
                } catch (Throwable t) {
                    throw RInternalError.shouldNotReachHere(t);
                }
            }
        } finally {
            RContext.getRForeignAccessFactory().setIsNull(isNullSetting);
        }
    }

    private static void initCallbacks(UpCallsRFFI upCallsImpl) {
        Node bind = Message.createInvoke(1).createNode();
        Node executeNode = Message.createExecute(1).createNode();
        SymbolHandle symbolHandle = DLL.findSymbol("Rinternals_addCallback", null);

        try {
            Callbacks.createCalls(upCallsImpl);
            for (Callbacks callback : Callbacks.values()) {
                String addCallbackSignature = String.format("(env, sint32, %s): void", callback.nfiSignature);
                TruffleObject addCallbackFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", addCallbackSignature);
                ForeignAccess.sendExecute(executeNode, addCallbackFunction, callback.ordinal(), callback.call);
            }
        } catch (Throwable t) {
            throw RInternalError.shouldNotReachHere(t);
        }
    }

    private enum ReturnArray {
        INTEGER_CREATE("([sint32], sint32): uint64", 2),
        DOUBLE_CREATE("([double], sint32): uint64", 2),
        BYTE_CREATE("([uint8], sint32, sint32): uint64", 3),
        INTEGER_EXISTING("(uint64): void", 1),
        DOUBLE_EXISTING("(uint64): void", 1),
        BYTE_EXISTING("(uint64): void", 1),
        FREE("(uint64): void", 1);

        private final String signature;
        private final String funName;
        private TruffleObject function;
        private final Node executeNode;

        ReturnArray(String signature, int numArgs) {
            this.signature = signature;
            this.funName = "return_" + name();
            this.executeNode = Message.createExecute(numArgs).createNode();
        }
    }

    private static void initReturnArray() {
        Node bind = Message.createInvoke(1).createNode();
        for (ReturnArray returnArrayFun : ReturnArray.values()) {
            SymbolHandle symbolHandle = DLL.findSymbol(returnArrayFun.funName, null); // libR
            try {
                returnArrayFun.function = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", returnArrayFun.signature);
            } catch (InteropException t) {
                throw RInternalError.shouldNotReachHere(t);
            }
        }
    }

    // TODO Nodify?
    static long returnArrayCreate(Object array, boolean isString) {
        try {
            if (array instanceof int[]) {
                return (long) ForeignAccess.sendExecute(ReturnArray.INTEGER_CREATE.executeNode, ReturnArray.INTEGER_CREATE.function, JavaInterop.asTruffleObject(array), ((int[]) array).length);
            } else if (array instanceof double[]) {
                return (long) ForeignAccess.sendExecute(ReturnArray.DOUBLE_CREATE.executeNode, ReturnArray.DOUBLE_CREATE.function, JavaInterop.asTruffleObject(array), ((double[]) array).length);
            } else if (array instanceof byte[]) {
                return (long) ForeignAccess.sendExecute(ReturnArray.BYTE_CREATE.executeNode, ReturnArray.BYTE_CREATE.function, JavaInterop.asTruffleObject(array), ((byte[]) array).length,
                                isString ? 1 : 0);
            } else {
                throw RInternalError.shouldNotReachHere();
            }
        } catch (InteropException t) {
            throw RInternalError.shouldNotReachHere(t);
        }
    }

    static void returnArrayExisting(SEXPTYPE type, long address) {
        try {
            switch (type) {
                case INTSXP:
                case LGLSXP:
                    ForeignAccess.sendExecute(ReturnArray.INTEGER_EXISTING.executeNode, ReturnArray.INTEGER_EXISTING.function, address);
                    break;
                case REALSXP:
                    ForeignAccess.sendExecute(ReturnArray.DOUBLE_EXISTING.executeNode, ReturnArray.DOUBLE_EXISTING.function, address);
                    break;
                case CHARSXP:
                case RAWSXP:
                    ForeignAccess.sendExecute(ReturnArray.BYTE_EXISTING.executeNode, ReturnArray.BYTE_EXISTING.function, address);
                    break;
                default:
                    throw RInternalError.shouldNotReachHere();

            }
        } catch (InteropException t) {
            throw RInternalError.shouldNotReachHere(t);
        }
    }

    static void freeArray(long address) {
        Node executeNode = Message.createExecute(1).createNode();
        try {
            ForeignAccess.sendExecute(executeNode, ReturnArray.FREE.function, address);
        } catch (InteropException t) {
            throw RInternalError.shouldNotReachHere(t);
        }
    }

    private static void initialize() {
        RFFIUtils.initializeTracing();
        if (traceEnabled()) {
            traceDownCall("initialize");
        }
        try {
            initCallbacks(new TruffleNFI_UpCallsRFFIImpl());
            initVariables();
            initReturnArray();
        } finally {
            if (traceEnabled()) {
                traceDownCallReturn("initialize", null);
            }
        }
    }

    public abstract static class TruffleNFI_InvokeCallNode extends Node implements InvokeCallNode {
        @Child private Node bindNode = Message.createInvoke(1).createNode();

        @Specialization(guards = "args.length == 0")
        protected Object invokeCall0(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            Object result = null;
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                nativeCallInfo.address.asTruffleObject(), "bind", "(): object");
                result = ForeignAccess.sendExecute(executeNode, callFunction);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        @Specialization(guards = "args.length == 1")
        protected Object invokeCall1(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            Object result = null;
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                nativeCallInfo.address.asTruffleObject(), "bind", "(object): object");
                result = ForeignAccess.sendExecute(executeNode, callFunction, args[0]);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        @Specialization(guards = "args.length == 2")
        protected Object invokeCall2(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            Object result = null;
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                nativeCallInfo.address.asTruffleObject(), "bind", "(object, object): object");
                result = ForeignAccess.sendExecute(executeNode, callFunction, args[0], args[1]);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        @Specialization(guards = "args.length == 3")
        protected Object invokeCall3(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            Object result = null;
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                nativeCallInfo.address.asTruffleObject(), "bind", "(object, object, object): object");
                result = ForeignAccess.sendExecute(executeNode, callFunction, args[0], args[1], args[2]);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        @Specialization(guards = "args.length == 4")
        protected Object invokeCall4(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            Object result = null;
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                nativeCallInfo.address.asTruffleObject(), "bind", "(object, object, object, object): object");
                result = ForeignAccess.sendExecute(executeNode, callFunction, args[0], args[1], args[2],
                                args[3]);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        @Specialization(guards = "args.length == 5")
        protected Object invokeCall5(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            Object result = null;
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                nativeCallInfo.address.asTruffleObject(), "bind", "(object, object, object, object, object): object");
                result = ForeignAccess.sendExecute(executeNode, callFunction, args[0], args[1],
                                args[2], args[3], args[4]);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        @Specialization(guards = "args.length == 6")
        protected Object invokeCall6(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            Object result = null;
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                nativeCallInfo.address.asTruffleObject(), "bind", "(object, object, object, object, object, object): object");
                result = ForeignAccess.sendExecute(executeNode, callFunction, args[0], args[1],
                                args[2], args[3], args[4], args[5]);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        @Specialization(guards = "args.length == 7")
        protected Object invokeCall7(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            Object result = null;
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                nativeCallInfo.address.asTruffleObject(), "bind", "(object, object, object, object, object, object, object): object");
                result = ForeignAccess.sendExecute(executeNode, callFunction, args[0], args[1],
                                args[2], args[3], args[4], args[5],
                                args[6]);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        @Specialization(guards = "args.length == 8")
        protected Object invokeCall8(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            Object result = null;
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                nativeCallInfo.address.asTruffleObject(), "bind", "(object, object, object, object, object, object, object, object): object");
                result = ForeignAccess.sendExecute(executeNode, callFunction, args[0], args[1],
                                args[2], args[3], args[4], args[5],
                                args[6], args[7]);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        public static Node createExecute(int n) {
            return Message.createExecute(n).createNode();
        }
    }

    private static class TruffleNFI_InvokeVoidCallNode extends Node implements InvokeVoidCallNode {
        private static final String CallVoid1Sig = "(object): void";
        private static final String CallVoid0Sig = "(): void";
        @Child private Node bindNode = Message.createInvoke(1).createNode();
        @Child private Node execute0Node = Message.createExecute(0).createNode();
        @Child private Node execute1Node = Message.createExecute(1).createNode();

        @Override
        public void execute(NativeCallInfo nativeCallInfo, Object[] args) {
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args);
            try {
                switch (args.length) {
                    case 0:
                        TruffleObject callVoid0Function = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                        nativeCallInfo.address.asTruffleObject(), "bind", CallVoid0Sig);
                        ForeignAccess.sendExecute(execute0Node, callVoid0Function);
                        break;
                    case 1:
                        TruffleObject callVoid1Function = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                        nativeCallInfo.address.asTruffleObject(), "bind", CallVoid1Sig);
                        ForeignAccess.sendExecute(execute1Node, callVoid1Function, args[0]);
                        break;
                }
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, null, isNullSetting);
            }
        }
    }

    private static boolean prepareCall(String name, Object[] args) {
        if (traceEnabled()) {
            traceDownCall(name, args);
        }
        boolean isNullSetting = RContext.getRForeignAccessFactory().setIsNull(false);
        TruffleNFI_NativeArray.callEnter(callDepth);
        callDepth++;
        return isNullSetting;
    }

    private static void prepareReturn(String name, Object result, boolean isNullSetting) {
        if (traceEnabled()) {
            traceDownCallReturn(name, result);
        }
        TruffleNFI_NativeArray.callEnter(callDepth);
        RContext.getRForeignAccessFactory().setIsNull(isNullSetting);
        callDepth--;
    }

    @Override
    public InvokeCallNode createInvokeCallNode() {
        return TruffleNFI_InvokeCallNodeGen.create();
    }

    @Override
    public InvokeVoidCallNode createInvokeVoidCallNode() {
        return new TruffleNFI_InvokeVoidCallNode();
    }
}
