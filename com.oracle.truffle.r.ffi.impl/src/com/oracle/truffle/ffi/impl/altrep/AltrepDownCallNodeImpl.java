package com.oracle.truffle.ffi.impl.altrep;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_Context;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_Context;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.altrep.AltrepDownCall;
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
    public abstract Object execute(AltrepDownCall altrepDowncall, boolean unwrapFlag, Object[] args);

    public static AltrepDownCallNodeImpl create() {
        return AltrepDownCallNodeImplNodeGen.create();
    }

    public static AltrepDownCallNodeImpl getUncached() {
        throw RInternalError.unimplemented("getUncached");
    }

    @Specialization(guards = "cachedLength == args.length", limit = "3")
    public Object doIt(AltrepDownCall altrepDowncallIn, boolean unwrapFlag, Object[] args,
                       @Cached("args.length") int cachedLength,
                       @CachedLibrary("altrepDowncallIn.method") InteropLibrary methodInterop,
                       @Cached(value = "createUnwrapNode(unwrapFlag)", uncached = "createUncachedUnwrapNode()") FFIUnwrapNode unwrapNode,
                       @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                       @Cached("createMaterialized(args.length)") FFIMaterializeNode[] materializeNodes,
                       @Cached("createToNatives(args.length)") FFIToNativeMirrorNode[] toNativeNodes,
                       @Cached("createBinaryProfile()") ConditionProfile isLLVMProfile,
                       @Cached BranchProfile unwrapResultProfile,
                       @Cached("createIdentityProfile()") ValueProfile identityProfile) {
        CompilerAsserts.partialEvaluationConstant(unwrapFlag);
        AltrepDownCall altrepDowncall = identityProfile.profile(altrepDowncallIn);

        assert methodInterop.isExecutable(altrepDowncall.method);
        RContext ctx = ctxRef.get();

        if (isLLVMProfile.profile(altrepDowncall.rffiType == Type.LLVM)) {
            ctx.getRFFI(TruffleLLVM_Context.class).beforeDowncall(null, altrepDowncall.rffiType);
        } else {
            ctx.getRFFI(TruffleNFI_Context.class).beforeDowncall(null, altrepDowncall.rffiType);
        }

        Object ret;
        try (FFIDownCallWrap ffiWrap = new FFIDownCallWrap(cachedLength)) {
            Object[] wrappedArgs = ffiWrap.wrap(args, materializeNodes, toNativeNodes);
            ret = methodInterop.execute(altrepDowncall.method, wrappedArgs);
            if (unwrapFlag) {
                unwrapResultProfile.enter();
                ret = unwrapNode.execute(ret);
            }
        } catch (Exception ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }

        if (isLLVMProfile.profile(altrepDowncall.rffiType == Type.LLVM)) {
            ctx.getRFFI(TruffleLLVM_Context.class).afterDowncall(null, altrepDowncall.rffiType);
        } else {
            ctx.getRFFI(TruffleNFI_Context.class).afterDowncall(null, altrepDowncall.rffiType);
        }

        return ret;
    }

    @Specialization(guards = "cachedLength == args.length", replaces = "doIt")
    public Object doItWithDispatchedMethodInterop(
                        AltrepDownCall altrepDowncall, boolean unwrapFlag, Object[] args,
                        @Cached("args.length") int cachedLength,
                        @CachedLibrary(limit = "3") InteropLibrary methodInterop,
                        @Cached(value = "createUnwrapNode(unwrapFlag)", uncached = "createUncachedUnwrapNode()") FFIUnwrapNode unwrapNode,
                        @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                        @Cached(value = "createMaterialized(args.length)", allowUncached = true) FFIMaterializeNode[] materializeNodes,
                        @Cached(value = "createToNatives(args.length)", allowUncached = true) FFIToNativeMirrorNode[] toNativeNodes,
                        @Cached("createBinaryProfile()") ConditionProfile isLLVMProfile,
                        @Cached BranchProfile unwrapResultProfile,
                        @Cached("createIdentityProfile()") ValueProfile identityProfile) {
        return doIt(altrepDowncall, unwrapFlag, args, cachedLength, methodInterop, unwrapNode, ctxRef, materializeNodes,
                toNativeNodes, isLLVMProfile, unwrapResultProfile, identityProfile);
    }

    // TODO: Implement some uncached specialization?

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

    protected static FFIMaterializeNode[] createMaterialized(int length) {
        return FFIMaterializeNode.create(length);
    }

    protected static FFIToNativeMirrorNode[] createToNatives(int length) {
        return FFIToNativeMirrorNode.create(length);
    }
}
