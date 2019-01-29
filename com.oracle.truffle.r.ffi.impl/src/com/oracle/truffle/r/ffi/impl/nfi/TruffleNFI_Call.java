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

import static com.oracle.truffle.r.runtime.ffi.RFFILog.logDownCall;
import static com.oracle.truffle.r.runtime.ffi.RFFILog.logDownCallReturn;
import static com.oracle.truffle.r.runtime.ffi.RFFILog.logEnabled;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_CallFactory.TruffleNFI_InvokeCallNodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.FFIWrapNode;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

public class TruffleNFI_Call implements CallRFFI {

    private static String getSignatureForArity(int arity) {
        CompilerAsserts.neverPartOfCompilation();
        StringBuilder str = new StringBuilder(10 * (arity + 2)).append("(");
        for (int i = 0; i < arity + 1; i++) {
            str.append(i > 0 ? ", " : "");
            str.append("pointer");
        }
        return str.append("): pointer").toString();
    }

    private abstract static class NodeAdapter extends Node {
        @Child private Node bindNode = Message.INVOKE.createNode();

        @TruffleBoundary
        protected TruffleObject getFunction(String name, String signature) {
            DLL.SymbolHandle symbolHandle = DLL.findSymbol(name, TruffleNFI_Context.getInstance().getRLibDLLInfo());
            try {
                return (TruffleObject) ForeignAccess.sendInvoke(bindNode, symbolHandle.asTruffleObject(), "bind", signature);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    @ImportStatic(FFIWrapNode.class)
    public abstract static class TruffleNFI_InvokeCallNode extends NodeAdapter implements InvokeCallNode {

        @TruffleBoundary
        protected TruffleObject getFunction(int arity) {
            return getFunction("dot_call" + arity, getSignatureForArity(arity));
        }

        @Specialization(guards = {"args.length == cachedArgsLength", "nativeCallInfo.address.asTruffleObject() == cachedAddress"})
        protected Object invokeCallCached(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("args.length") int cachedArgsLength,
                        @Cached("create(cachedArgsLength)") FFIWrapNode[] ffiWrapNodes,
                        @Cached("create()") FFIUnwrapNode unwrap,
                        @Cached("createExecute()") Node executeNode,
                        @Cached("nativeCallInfo.address.asTruffleObject()") TruffleObject cachedAddress,
                        @Cached("getFunction(cachedArgsLength)") TruffleObject cachedFunction) {
            Object result = null;
            Object[] realArgs = new Object[cachedArgsLength + 1];
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args, ffiWrapNodes);
            try {
                System.arraycopy(args, 0, realArgs, 1, cachedArgsLength);
                realArgs[0] = cachedAddress;
                result = ForeignAccess.sendExecute(executeNode, cachedFunction, realArgs);
                return unwrap.execute(result);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                assert realArgs != null; // to keep the values alive
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        @Specialization(limit = "99", guards = "args.length == cachedArgsLength")
        protected Object invokeCallCachedLength(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("args.length") int cachedArgsLength,
                        @Cached("create(cachedArgsLength)") FFIWrapNode[] ffiWrapNodes,
                        @Cached("create()") FFIUnwrapNode unwrap,
                        @Cached("createExecute()") Node executeNode) {
            Object result = null;
            Object[] realArgs = new Object[cachedArgsLength + 1];
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args, ffiWrapNodes);
            try {
                System.arraycopy(args, 0, realArgs, 1, cachedArgsLength);
                realArgs[0] = nativeCallInfo.address.asTruffleObject();
                result = ForeignAccess.sendExecute(executeNode, getFunction(cachedArgsLength), realArgs);
                return unwrap.execute(result);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                assert realArgs != null; // to keep the values alive
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        public static Node createExecute() {
            return Message.EXECUTE.createNode();
        }
    }

    private static class TruffleNFI_InvokeVoidCallNode extends NodeAdapter implements InvokeVoidCallNode {
        private static final String CallVoid1Sig = "(pointer, pointer): void";
        private static final String CallVoid0Sig = "(pointer): void";

        @Child private Node execute0Node = Message.EXECUTE.createNode();
        @Child private Node execute1Node = Message.EXECUTE.createNode();
        @Children private final FFIWrapNode[] ffiWrapNodes0 = FFIWrapNode.create(0);
        @Children private final FFIWrapNode[] ffiWrapNodes1 = FFIWrapNode.create(1);

        @Override
        public void execute(NativeCallInfo nativeCallInfo, Object[] args) {
            boolean isNullSetting = true;
            try {
                switch (args.length) {
                    case 0:
                        isNullSetting = prepareCall(nativeCallInfo.name, args, ffiWrapNodes0);
                        TruffleObject callVoid0Function = getFunction("dot_call_void0", CallVoid0Sig);
                        ForeignAccess.sendExecute(execute0Node, callVoid0Function, nativeCallInfo.address.asTruffleObject());
                        break;
                    case 1:
                        isNullSetting = prepareCall(nativeCallInfo.name, args, ffiWrapNodes1);
                        TruffleObject callVoid1Function = getFunction("dot_call_void1", CallVoid1Sig);
                        ForeignAccess.sendExecute(execute1Node, callVoid1Function, nativeCallInfo.address.asTruffleObject(), args[0]);
                        break;
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                prepareReturn(nativeCallInfo.name, null, isNullSetting);
            }
        }
    }

    @ExplodeLoop
    private static boolean prepareCall(String name, Object[] args, FFIWrapNode[] ffiWrapNodes) {
        CompilerAsserts.compilationConstant(ffiWrapNodes.length);
        if (logEnabled()) {
            logDownCall(name, args);
        }
        for (int i = 0; i < ffiWrapNodes.length; i++) {
            args[i] = ffiWrapNodes[i].execute(args[i]);
        }
        boolean isNullSetting = RContext.getRForeignAccessFactory().setIsNull(false);
        return isNullSetting;
    }

    private static void prepareReturn(String name, Object result, boolean isNullSetting) {
        RContext.getRForeignAccessFactory().setIsNull(isNullSetting);
        if (logEnabled()) {
            logDownCallReturn(name, result);
        }
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
