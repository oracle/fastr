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
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_C.LLVMFunctionObjectGetter;
import com.oracle.truffle.r.ffi.impl.mixed.TruffleMixed_CFactory.MixedFunctionObjectGetterNodeGen;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_C.NFIFunctionObjectGetter;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.InvokeCNode;
import com.oracle.truffle.r.runtime.ffi.InvokeCNode.FunctionObjectGetter;
import com.oracle.truffle.r.runtime.ffi.InvokeCNodeGen;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public class TruffleMixed_C implements CRFFI {

    abstract static class MixedFunctionObjectGetter extends FunctionObjectGetter {

        protected static boolean isLLVMInvocation(NativeCallInfo nativeCallInfo) {
            return nativeCallInfo.dllInfo.handle.getRFFIType() == RFFIFactory.Type.LLVM;
        }

        protected static boolean isNFIInvocation(NativeCallInfo nativeCallInfo) {
            return nativeCallInfo.dllInfo.handle.getRFFIType() == RFFIFactory.Type.NFI;
        }

        @SuppressWarnings("static-method")
        @Specialization(guards = "isLLVMInvocation(nativeCallInfo)")
        protected TruffleObject handleLLVMInvocation(TruffleObject address, int arity, NativeCallInfo nativeCallInfo,
                        @Cached("new()") LLVMFunctionObjectGetter deleg) {
            return deleg.execute(address, arity, nativeCallInfo);
        }

        @SuppressWarnings("static-method")
        @Specialization(guards = "isNFIInvocation(nativeCallInfo)")
        protected TruffleObject handleNFIInvocation(TruffleObject address, int arity, NativeCallInfo nativeCallInfo,
                        @Cached("new()") NFIFunctionObjectGetter deleg) {
            return deleg.execute(address, arity, nativeCallInfo);
        }

    }

    @Override
    public InvokeCNode createInvokeCNode() {
        return InvokeCNodeGen.create(MixedFunctionObjectGetterNodeGen.create());
    }
}
