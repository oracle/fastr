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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_CallFactory.TruffleNFI_InvokeCallNodeGen;
import com.oracle.truffle.r.runtime.DSLConfig;
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
        @Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());

        @TruffleBoundary
        protected TruffleObject getFunction(String name, String signature) {
            DLL.SymbolHandle symbolHandle = DLL.findSymbol(name, TruffleNFI_Context.getInstance().getRLibDLLInfo());
            try {
                return (TruffleObject) interop.invokeMember(symbolHandle.asTruffleObject(), "bind", signature);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    @ImportStatic(DSLConfig.class)
    public abstract static class TruffleNFI_InvokeCallNode extends NodeAdapter implements InvokeCallNode {

        @TruffleBoundary
        protected TruffleObject getFunction(int arity) {
            return getFunction("dot_call" + arity, getSignatureForArity(arity));
        }

        @Specialization(guards = {"args.length == cachedArgsLength", "nativeCallInfo.address.asTruffleObject() == cachedAddress"})
        protected Object invokeCallCached(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("args.length") int cachedArgsLength,
                        @Cached("createWrappers(cachedArgsLength)") FFIWrapNode[] ffiWrapNodes,
                        @Cached("create()") FFIUnwrapNode unwrap,
                        @Cached("nativeCallInfo.address.asTruffleObject()") TruffleObject cachedAddress,
                        @Cached("getFunction(cachedArgsLength)") TruffleObject cachedFunction,
                        @CachedLibrary("cachedFunction") InteropLibrary interop) {
            Object result = null;
            Object[] realArgs = new Object[cachedArgsLength + 1];
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args, ffiWrapNodes);
            try {
                System.arraycopy(args, 0, realArgs, 1, cachedArgsLength);
                realArgs[0] = cachedAddress;
                result = interop.execute(cachedFunction, realArgs);
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
                        @Cached("createWrappers(cachedArgsLength)") FFIWrapNode[] ffiWrapNodes,
                        @Cached("create()") FFIUnwrapNode unwrap,
                        @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary interop) {
            Object result = null;
            Object[] realArgs = new Object[cachedArgsLength + 1];
            boolean isNullSetting = prepareCall(nativeCallInfo.name, args, ffiWrapNodes);
            try {
                System.arraycopy(args, 0, realArgs, 1, cachedArgsLength);
                realArgs[0] = nativeCallInfo.address.asTruffleObject();
                result = interop.execute(getFunction(cachedArgsLength), realArgs);
                return unwrap.execute(result);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                assert realArgs != null; // to keep the values alive
                prepareReturn(nativeCallInfo.name, result, isNullSetting);
            }
        }

        protected static FFIWrapNode[] createWrappers(int count) {
            return FFIWrapNode.create(count);
        }
    }

    private static class TruffleNFI_InvokeVoidCallNode extends NodeAdapter implements InvokeVoidCallNode {
        private static final String CallVoid1Sig = "(pointer, pointer): void";
        private static final String CallVoid0Sig = "(pointer): void";

        @Child private InteropLibrary execute0Interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());
        @Child private InteropLibrary execute1Interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());
        @Children private final FFIWrapNode[] ffiWrapNodes0 = FFIWrapNode.create(0);
        @Children private final FFIWrapNode[] ffiWrapNodes1 = FFIWrapNode.create(1);

        @Override
        public void execute(VirtualFrame frame, NativeCallInfo nativeCallInfo, Object[] args) {
            boolean isNullSetting = true;
            try {
                switch (args.length) {
                    case 0:
                        isNullSetting = prepareCall(nativeCallInfo.name, args, ffiWrapNodes0);
                        TruffleObject callVoid0Function = getFunction("dot_call_void0", CallVoid0Sig);
                        execute0Interop.execute(callVoid0Function, nativeCallInfo.address.asTruffleObject());
                        break;
                    case 1:
                        isNullSetting = prepareCall(nativeCallInfo.name, args, ffiWrapNodes1);
                        TruffleObject callVoid1Function = getFunction("dot_call_void1", CallVoid1Sig);
                        execute1Interop.execute(callVoid1Function, nativeCallInfo.address.asTruffleObject(), args[0]);
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
