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
package com.oracle.truffle.r.ffi.impl.nfi;

import static com.oracle.truffle.r.runtime.ffi.RFFILog.logDownCall;
import static com.oracle.truffle.r.runtime.ffi.RFFILog.logEnabled;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.nfi.api.SignatureLibrary;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_CallFactory.TruffleNFI_InvokeCallNodeGen;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_CallFactory.TruffleNFI_InvokeVoidCallNodeGen;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.FFIMaterializeNode;
import com.oracle.truffle.r.runtime.ffi.FFIToNativeMirrorNode;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.FFIWrap.FFIDownCallWrap;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

public class TruffleNFI_Call implements CallRFFI {

    abstract static class NodeAdapter extends Node {
        @Child private SignatureLibrary signatures = SignatureLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());

        static Object getSignatureForArity(int arity) {
            CompilerAsserts.neverPartOfCompilation();
            StringBuilder str = new StringBuilder(10 * (arity + 2)).append("(");
            for (int i = 0; i < arity + 1; i++) {
                str.append(i > 0 ? ", " : "");
                str.append("pointer");
            }
            String signature = str.append("): pointer").toString();
            return TruffleNFI_Context.parseSignature(signature);
        }

        @TruffleBoundary
        protected TruffleObject getFunction(String name, Object signature) {
            DLL.SymbolHandle symbolHandle = DLL.findSymbol(name, TruffleNFI_Context.getInstance().getRLibDLLInfo());
            return (TruffleObject) signatures.bind(signature, symbolHandle.asTruffleObject());
        }
    }

    @ImportStatic(DSLConfig.class)
    public abstract static class TruffleNFI_InvokeCallNode extends NodeAdapter implements InvokeCallNode {

        @TruffleBoundary
        protected TruffleObject getFunction(int arity) {
            return getFunction(arity, getSignatureForArity(arity));
        }

        @TruffleBoundary
        protected TruffleObject getFunction(int arity, Object signature) {
            return getFunction("dot_call" + arity, signature);
        }

        @Specialization(guards = {"args.length == cachedArgsLength", "nativeCallInfo.address.asTruffleObject() == cachedAddress"})
        protected Object invokeCallCached(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("args.length") int cachedArgsLength,
                        @Cached("createMaterializeNodess(cachedArgsLength)") FFIMaterializeNode[] ffiMaterializeNode,
                        @Cached("createWrapperNodes(cachedArgsLength)") FFIToNativeMirrorNode[] ffiWrapperNodes,
                        @Cached("create()") FFIUnwrapNode unwrap,
                        @Cached("nativeCallInfo.address.asTruffleObject()") TruffleObject cachedAddress,
                        @Cached("getFunction(cachedArgsLength)") TruffleObject cachedFunction,
                        @CachedLibrary("cachedFunction") InteropLibrary interop) {
            return doInvoke(nativeCallInfo, cachedAddress, cachedFunction, args, cachedArgsLength, ffiMaterializeNode, ffiWrapperNodes, interop, unwrap);
        }

        @Specialization(limit = "99", guards = "args.length == cachedArgsLength")
        protected Object invokeCallCachedLength(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("args.length") int cachedArgsLength,
                        @Cached("createMaterializeNodess(cachedArgsLength)") FFIMaterializeNode[] ffiMaterializeNode,
                        @Cached("createWrapperNodes(cachedArgsLength)") FFIToNativeMirrorNode[] ffiToNativeMirrorNodes,
                        @Cached("create()") FFIUnwrapNode unwrap,
                        @Cached("getSignatureForArity(cachedArgsLength)") Object cachedSignature,
                        @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary interop) {
            return doInvoke(nativeCallInfo, nativeCallInfo.address.asTruffleObject(), getFunction(cachedArgsLength, cachedSignature), args, cachedArgsLength, ffiMaterializeNode,
                            ffiToNativeMirrorNodes, interop, unwrap);
        }

        private static Object doInvoke(NativeCallInfo nativeCallInfo, TruffleObject address, TruffleObject function, Object[] args, int cachedArgsLength, FFIMaterializeNode[] ffiMaterializeNode,
                        FFIToNativeMirrorNode[] ffiToNativeMirrorNodes, InteropLibrary interop, FFIUnwrapNode unwrap) throws RuntimeException {
            Object result;
            FFIDownCallWrap ffiWrap = new FFIDownCallWrap(args.length);
            try {
                logCall(nativeCallInfo.name, args);
                Object[] wrappedArgs = ffiWrap.wrapAll(args, ffiMaterializeNode, ffiToNativeMirrorNodes);
                Object[] realArgs = new Object[cachedArgsLength + 1];
                realArgs[0] = address;
                System.arraycopy(wrappedArgs, 0, realArgs, 1, cachedArgsLength);
                result = interop.execute(function, realArgs);
                return unwrap.execute(result);
            } catch (Exception ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                ffiWrap.close();
            }
        }

        protected static FFIMaterializeNode[] createMaterializeNodess(int count) {
            return FFIMaterializeNode.create(count);
        }

        protected static FFIToNativeMirrorNode[] createWrapperNodes(int count) {
            return FFIToNativeMirrorNode.create(count);
        }
    }

    @ImportStatic(DSLConfig.class)
    public abstract static class TruffleNFI_InvokeVoidCallNode extends NodeAdapter implements InvokeVoidCallNode {
        protected static final String CallVoid1Sig = "(pointer, pointer): void";
        protected static final String CallVoid0Sig = "(pointer): void";

        @Specialization
        public void invokeVoidCallCached(NativeCallInfo nativeCallInfo, Object[] args,
                        @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary execute0Interop,
                        @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary execute1Interop,
                        @Cached("createMaterializeNodes(1)") FFIMaterializeNode[] ffiMaterialize1,
                        @Cached("createWrapperNodes(1)") FFIToNativeMirrorNode[] ffiWrapper1,
                        @Cached("getCallVoid0Function()") TruffleObject callVoid0Function,
                        @Cached("getCallVoid1Function()") TruffleObject callVoid1Function) {
            FFIDownCallWrap ffiWrap = new FFIDownCallWrap(args.length);
            try {
                Object[] wrappedArgs;
                switch (args.length) {
                    case 0:
                        logCall(nativeCallInfo.name, args);
                        execute0Interop.execute(callVoid0Function, nativeCallInfo.address.asTruffleObject());
                        break;
                    case 1:
                        logCall(nativeCallInfo.name, args);
                        wrappedArgs = ffiWrap.wrapAll(args, ffiMaterialize1, ffiWrapper1);
                        execute1Interop.execute(callVoid1Function, nativeCallInfo.address.asTruffleObject(), wrappedArgs[0]);
                        break;
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            } catch (Exception ex) {
                throw RInternalError.shouldNotReachHere(ex);
            } finally {
                ffiWrap.close();
            }
        }

        protected TruffleObject getCallVoid0Function() {
            Object callVoid0Sig = TruffleNFI_Context.parseSignature(CallVoid0Sig);
            return getFunction("dot_call_void0", callVoid0Sig);
        }

        protected TruffleObject getCallVoid1Function() {
            Object callVoid1Sig = TruffleNFI_Context.parseSignature(CallVoid1Sig);
            return getFunction("dot_call_void1", callVoid1Sig);
        }

        protected static FFIMaterializeNode[] createMaterializeNodes(int count) {
            return FFIMaterializeNode.create(count);
        }

        protected static FFIToNativeMirrorNode[] createWrapperNodes(int count) {
            return FFIToNativeMirrorNode.create(count);
        }
    }

    private static void logCall(String name, Object[] args) {
        if (logEnabled()) {
            logDownCall(name, args);
        }
    }

    @Override
    public InvokeCallNode createInvokeCallNode() {
        return TruffleNFI_InvokeCallNodeGen.create();
    }

    @Override
    public InvokeVoidCallNode createInvokeVoidCallNode() {
        return TruffleNFI_InvokeVoidCallNodeGen.create();
    }

}
