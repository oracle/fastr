package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DownCallNodeFactory.LLVMDownCallNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeMirror;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RIntVectorData;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRepClassDescriptor;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.altrep.RAltStringVector;
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory.DownCallNode;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;

public abstract class AltrepLLVMDownCallNode extends DownCallNode {
    @Child private LLVMDownCallNode llvmDownCallNode;
    // TODO: Use descriptorHandle instead of descriptor
    // TODO: Add CompilationFinal
    private AltRepClassDescriptor descriptor;
    @CompilationFinal(dimensions = 1) private ConditionProfile[] hasMirrorProfiles;

    public static AltrepLLVMDownCallNode create() {
        return AltrepLLVMDownCallNodeGen.create();
    }

    public AltrepLLVMDownCallNode() {
        DownCallNode downCallNode = TruffleLLVM_DownCallNodeFactory.INSTANCE.createDownCallNode();
        assert downCallNode instanceof LLVMDownCallNode;
        this.llvmDownCallNode = (LLVMDownCallNode) downCallNode;
    }

    @Specialization
    protected Object doCall(NativeFunction f, Object[] args,
                            @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        if (hasMirrorProfiles == null) {
            hasMirrorProfiles = createHasMirrorProfiles(args.length);
        }
        assert isAltrep(args[0]);
        AltRepClassDescriptor descriptorFromArgs = getDescriptorFromAltrepObj((RBaseObject) args[0]);
        if (descriptor != descriptorFromArgs) {
            descriptor = descriptorFromArgs;
        }

        return doCallImpl(f, args, ctxRef);
    }

    private static boolean isAltrep(Object object) {
        return object instanceof RBaseObject && ((RBaseObject) object).isAltRep();
    }

    private static AltRepClassDescriptor getDescriptorFromAltrepObj(RBaseObject altrepObject) {
        assert altrepObject.isAltRep();

        if (altrepObject instanceof RIntVector) {
            RIntVectorData data = ((RIntVector) altrepObject).getData();
            assert data instanceof RAltIntVectorData;
            return ((RAltIntVectorData) data).getDescriptor();
        } else if (altrepObject instanceof RAltStringVector) {
            return ((RAltStringVector) altrepObject).getDescriptor();
        } else {
            throw RInternalError.unimplemented();
        }
    }

    @ExplodeLoop
    @Override
    protected Object beforeCall(@SuppressWarnings("unused") NativeFunction nativeFunction, TruffleObject f, Object[] args) {
        llvmDownCallNode.beforeCall(nativeFunction, f, args);
        CompilerAsserts.partialEvaluationConstant(args.length);
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof RBaseObject) {
                args[i] = wrapInNativeMirror((RBaseObject) args[i], hasMirrorProfiles[i]);
            }
        }

        // TODO: Add RContext.getRFFIContext.beforeDownCall, and afterDownCall
        return 0;
    }

    @Override
    protected void afterCall(Object before, NativeFunction f, TruffleObject t, Object[] args) {

    }

    @Override
    protected TruffleObject createTarget(ContextReference<RContext> ctxRef, NativeFunction f) {
        // TODO: Get descriptor from ctxRef via descriptorHandle
        CompilerAsserts.partialEvaluationConstant(f);
        switch (f) {
            case AltInteger_Is_sorted:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object isSortedMethod = ((AltIntegerClassDescriptor) descriptor).getIsSortedMethod();
                assert isSortedMethod instanceof TruffleObject;
                return (TruffleObject) isSortedMethod;
            case AltInteger_Elt:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object eltMethod = ((AltIntegerClassDescriptor) descriptor).getEltMethod();
                assert eltMethod instanceof TruffleObject;
                return (TruffleObject) eltMethod;
            case AltInteger_Max:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object maxMethod = ((AltIntegerClassDescriptor) descriptor).getMaxMethod();
                assert maxMethod instanceof TruffleObject;
                return (TruffleObject) maxMethod;
            case AltInteger_Min:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object minMethod = ((AltIntegerClassDescriptor) descriptor).getMinMethod();
                assert minMethod instanceof TruffleObject;
                return (TruffleObject) minMethod;
            case AltInteger_Sum:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object sumMethod = ((AltIntegerClassDescriptor) descriptor).getSumMethod();
                assert sumMethod instanceof TruffleObject;
                return (TruffleObject) sumMethod;
            case AltInteger_No_NA:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object noNaMethod = ((AltIntegerClassDescriptor) descriptor).getNoNAMethod();
                assert noNaMethod instanceof TruffleObject;
                return (TruffleObject) noNaMethod;
            case AltInteger_Get_region:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object getRegionMethod = ((AltIntegerClassDescriptor) descriptor).getGetRegionMethod();
                assert getRegionMethod instanceof TruffleObject;
                return (TruffleObject) getRegionMethod;
            default:
                throw RInternalError.unimplemented();
        }
    }

    @ExplodeLoop
    protected static ConditionProfile[] createHasMirrorProfiles(int count) {
        ConditionProfile[] res = new ConditionProfile[count];
        for (int i = 0; i < count; i++) {
            res[i] = ConditionProfile.createBinaryProfile();
        }
        return res;
    }

    private static NativeMirror wrapInNativeMirror(RBaseObject object, ConditionProfile hasMirrorProfile) {
        NativeMirror mirror = object.getNativeMirror();
        if (hasMirrorProfile.profile(mirror != null)) {
            return mirror;
        } else {
            return NativeDataAccess.createNativeMirror(object);
        }
    }
}
