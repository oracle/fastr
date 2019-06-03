/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.mixed;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_Call;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_CallFactory.TruffleMixed_InvokeCallNodeGen;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_CallFactory.TruffleMixed_InvokeVoidCallNodeGen;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_DLL.MixedLLVM_Handle;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_Call;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI.LibHandle;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public class TruffleMixed_Call implements CallRFFI {

    private final TruffleLLVM_Call llvmCallRFFI = new TruffleLLVM_Call();
    private final TruffleNFI_Call nfiCallRFFI = new TruffleNFI_Call();

    @Override
    public InvokeCallNode createInvokeCallNode() {
        return TruffleMixed_InvokeCallNodeGen.create(this);
    }

    @Override
    public InvokeVoidCallNode createInvokeVoidCallNode() {
        return TruffleMixed_InvokeVoidCallNodeGen.create(this);
    }

    abstract static class TruffleMixed_InvokeCallNodeBase extends Node {

        protected static boolean isLLVMInvocation(NativeCallInfo nativeCallInfo) {
            return nativeCallInfo.dllInfo == null || nativeCallInfo.dllInfo.handle.getRFFIType() == RFFIFactory.Type.LLVM;
        }

        protected static boolean isNFIInvocation(NativeCallInfo nativeCallInfo) {
            return nativeCallInfo.dllInfo != null && nativeCallInfo.dllInfo.handle.getRFFIType() == RFFIFactory.Type.NFI;
        }

    }

    abstract static class TruffleMixed_InvokeCallNode extends TruffleMixed_InvokeCallNodeBase implements InvokeCallNode {

        private final TruffleMixed_Call callRffi;

        TruffleMixed_InvokeCallNode(TruffleMixed_Call callRffi) {
            this.callRffi = callRffi;
        }

        protected InvokeCallNode createLLVMInvocationNode() {
            return callRffi.llvmCallRFFI.createInvokeCallNode();
        }

        protected InvokeCallNode createNFIInvocationNode() {
            return callRffi.nfiCallRFFI.createInvokeCallNode();
        }

        @Specialization(guards = "isLLVMInvocation(nativeCallInfo)")
        protected Object handleLLVMInvocation(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createLLVMInvocationNode()") InvokeCallNode delegNode) {
            return delegNode.execute(nativeCallInfo, args);
        }

        @Specialization(guards = "isNFIInvocation(nativeCallInfo)")
        protected Object handleNFIInvocation(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createNFIInvocationNode()") InvokeCallNode delegNode) {
            return delegNode.execute(nativeCallInfo, args);
        }
    }

    abstract static class TruffleMixed_InvokeVoidCallNode extends TruffleMixed_InvokeCallNodeBase implements InvokeVoidCallNode {

        private final TruffleMixed_Call callRffi;

        TruffleMixed_InvokeVoidCallNode(TruffleMixed_Call callRffi) {
            this.callRffi = callRffi;
        }

        protected InvokeVoidCallNode createLLVMInvocationNode() {
            return callRffi.llvmCallRFFI.createInvokeVoidCallNode();
        }

        protected InvokeVoidCallNode createNFIInvocationNode() {
            return callRffi.nfiCallRFFI.createInvokeVoidCallNode();
        }

        static boolean isInitLibSymbol(NativeCallInfo nativeCallInfo) {
            return nativeCallInfo.name.startsWith(DLL.R_INIT_PREFIX);
        }

        @Specialization(guards = "isNFIInvocation(nativeCallInfo)")
        protected void handleNFIInvocation(VirtualFrame frame, NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createNFIInvocationNode()") InvokeVoidCallNode delegNode) {
            delegNode.execute(frame, nativeCallInfo, args);
        }

        @Specialization(guards = {"isLLVMInvocation(nativeCallInfo)", "!isInitLibSymbol(nativeCallInfo)"})
        protected void handleLLVMInvocation(VirtualFrame frame, NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createLLVMInvocationNode()") InvokeVoidCallNode delegNode) {
            delegNode.execute(frame, nativeCallInfo, args);
        }

        @Specialization(guards = {"isLLVMInvocation(nativeCallInfo)", "isInitLibSymbol(nativeCallInfo)"})
        protected void handleLLVMNFIInitLib(VirtualFrame frame, NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("createLLVMInvocationNode()") InvokeVoidCallNode llvmDelegNode,
                        @Cached("createNFIInvocationNode()") InvokeVoidCallNode nfiDelegNode) {
            RList symbols = (RList) nativeCallInfo.address.value;
            llvmDelegNode.dispatch(frame, new NativeCallInfo(nativeCallInfo.name, (SymbolHandle) symbols.getDataAt(0), nativeCallInfo.dllInfo), args);
            LibHandle nfiLibHandle = ((MixedLLVM_Handle) nativeCallInfo.dllInfo.handle).nfiLibHandle;
            if (nfiLibHandle != null) {
                DLLInfo nfiDllInfo = nativeCallInfo.dllInfo.replaceHandle(nfiLibHandle);
                nfiDelegNode.dispatch(frame, new NativeCallInfo(nativeCallInfo.name, (SymbolHandle) symbols.getDataAt(1), nfiDllInfo), new Object[]{nfiDllInfo});
            }
        }
    }
}
