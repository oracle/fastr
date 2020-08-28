/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.altrep;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_Context;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_Context;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.altrep.AltrepMethodDescriptor;
import com.oracle.truffle.r.runtime.ffi.AfterDownCallProfiles;
import com.oracle.truffle.r.runtime.ffi.FFIMaterializeNode;
import com.oracle.truffle.r.runtime.ffi.FFIToNativeMirrorNode;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNodeGen;
import com.oracle.truffle.r.runtime.ffi.FFIWrap.FFIDownCallWrap;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type;
import com.oracle.truffle.r.runtime.nodes.altrep.AltrepDownCallNode;

@GenerateUncached
public abstract class AltrepDownCallNodeImpl extends AltrepDownCallNode {
    @Override
    public abstract Object execute(AltrepMethodDescriptor altrepDowncall, boolean unwrapResult, boolean[] wrapArguments, Object[] args);

    public static AltrepDownCallNodeImpl create() {
        return AltrepDownCallNodeImplNodeGen.create();
    }

    public static AltrepDownCallNodeImpl getUncached() {
        return AltrepDownCallNodeImplNodeGen.getUncached();
    }

    @Specialization(limit = "3")
    public Object doIt(AltrepMethodDescriptor altrepDowncallIn, boolean unwrapResult, boolean[] wrapArguments, Object[] args,
                    @CachedLibrary("altrepDowncallIn.method") InteropLibrary methodInterop,
                    @Cached(value = "createUnwrapNode(unwrapResult)", uncached = "createUncachedUnwrapNode()") FFIUnwrapNode unwrapNode,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                    @Cached(value = "createMaterialized(wrapArguments)", allowUncached = true) FFIMaterializeNode[] materializeNodes,
                    @Cached(value = "createToNatives(wrapArguments)", allowUncached = true) FFIToNativeMirrorNode[] toNativeNodes,
                    @Cached("createBinaryProfile()") ConditionProfile isLLVMProfile,
                    @Cached BranchProfile unwrapResultProfile,
                    @Cached("createIdentityProfile()") ValueProfile identityProfile,
                    @Cached AfterDownCallProfiles afterDownCallProfiles) {
        CompilerAsserts.partialEvaluationConstant(unwrapResult);
        CompilerAsserts.partialEvaluationConstant(args.length);
        AltrepMethodDescriptor altrepMethodDescriptor = identityProfile.profile(altrepDowncallIn);

        assert methodInterop.isExecutable(altrepMethodDescriptor.method);
        RContext ctx = ctxRef.get();

        Object before = null;
        if (isLLVMProfile.profile(altrepMethodDescriptor.rffiType == Type.LLVM)) {
            before = ctx.getRFFI(TruffleLLVM_Context.class).beforeDowncall(null, altrepMethodDescriptor.rffiType);
        } else {
            before = ctx.getRFFI(TruffleNFI_Context.class).beforeDowncall(null, altrepMethodDescriptor.rffiType);
        }

        Object ret;
        try (FFIDownCallWrap ffiWrap = new FFIDownCallWrap(args.length)) {
            Object[] wrappedArgs = ffiWrap.wrapSome(args, materializeNodes, toNativeNodes, wrapArguments);
            ret = methodInterop.execute(altrepMethodDescriptor.method, wrappedArgs);
            if (unwrapResult) {
                unwrapResultProfile.enter();
                ret = unwrapNode.execute(ret);
            }
        } catch (Exception ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }

        if (isLLVMProfile.profile(altrepMethodDescriptor.rffiType == Type.LLVM)) {
            ctx.getRFFI(TruffleLLVM_Context.class).afterDowncall(before, altrepMethodDescriptor.rffiType, afterDownCallProfiles);
        } else {
            ctx.getRFFI(TruffleNFI_Context.class).afterDowncall(before, altrepMethodDescriptor.rffiType, afterDownCallProfiles);
        }

        return ret;
    }

    protected static FFIUnwrapNode createUnwrapNode(boolean unwrapFlag) {
        if (unwrapFlag) {
            return FFIUnwrapNode.create();
        } else {
            return null;
        }
    }

    protected static FFIUnwrapNode createUncachedUnwrapNode() {
        return FFIUnwrapNodeGen.getUncached();
    }

    @ExplodeLoop
    protected static FFIMaterializeNode[] createMaterialized(boolean[] wrapArguments) {
        return FFIMaterializeNode.create(wrapArguments.length);
    }

    @ExplodeLoop
    protected static FFIToNativeMirrorNode[] createToNatives(boolean[] wrapArguments) {
        return FFIToNativeMirrorNode.create(wrapArguments.length);
    }
}
