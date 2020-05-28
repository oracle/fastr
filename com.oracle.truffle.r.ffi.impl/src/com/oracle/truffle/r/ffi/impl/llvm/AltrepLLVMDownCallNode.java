package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_DownCallNodeFactory.LLVMDownCallNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeMirror;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRepClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;

@GenerateUncached
public abstract class AltrepLLVMDownCallNode extends LLVMDownCallNode {

    public static AltrepLLVMDownCallNode create() {
        return AltrepLLVMDownCallNodeGen.create();
    }

    public AltrepLLVMDownCallNode() {
    }

    @Specialization
    @Override
    protected Object doCall(Frame frame, NativeFunction func, Object[] args,
                                  @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        assert isAltrep(args[0]);
        AltRepClassDescriptor descriptorFromArgs = getDescriptorFromAltrepObj((RBaseObject) args[0]);

        // FIXME: Ugly hack - save the descriptor inside altrep context
        ctxRef.get().altRepContext.saveDescriptor(descriptorFromArgs);

        return doCallImpl(frame, func, args, ctxRef);
    }

    private static boolean isAltrep(Object object) {
        return AltrepUtilities.isAltrep(object);
    }

    private static AltRepClassDescriptor getDescriptorFromAltrepObj(RBaseObject altrepObject) {
        return AltrepUtilities.getDescriptorFromAltrepObj(altrepObject);
    }

    @ExplodeLoop
    @Override
    protected Object beforeCall(Frame frame, @SuppressWarnings("unused") NativeFunction nativeFunction, TruffleObject f, Object[] args) {
        Object savedDownCallFrame = super.beforeCall(frame, nativeFunction, f, args);
        CompilerAsserts.partialEvaluationConstant(args.length);
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof RBaseObject) {
                args[i] = wrapInNativeMirror((RBaseObject) args[i]);
            }
        }
        return savedDownCallFrame;
    }

    @Override
    protected void afterCall(Frame frame, Object before, NativeFunction f, TruffleObject t, Object[] args) {
        super.afterCall(frame, before, f, t, args);
    }

    @Override
    protected TruffleObject createTarget(ContextReference<RContext> ctxRef, NativeFunction f) {
        CompilerAsserts.partialEvaluationConstant(f);
        AltRepClassDescriptor descriptor = ctxRef.get().altRepContext.loadDescriptor();
        switch (f) {
            case AltInteger_Dataptr:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object dataptrMethod = ((AltIntegerClassDescriptor) descriptor).getDataptrDownCall().method;
                assert dataptrMethod instanceof TruffleObject;
                return (TruffleObject) dataptrMethod;
            case AltInteger_Is_sorted:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object isSortedMethod = ((AltIntegerClassDescriptor) descriptor).getIsSortedDownCall().method;
                assert isSortedMethod instanceof TruffleObject;
                return (TruffleObject) isSortedMethod;
            case AltInteger_Elt:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object eltMethod = ((AltIntegerClassDescriptor) descriptor).getEltDownCall().method;
                assert eltMethod instanceof TruffleObject;
                return (TruffleObject) eltMethod;
            case AltInteger_Max:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object maxMethod = ((AltIntegerClassDescriptor) descriptor).getMaxDownCall().method;
                assert maxMethod instanceof TruffleObject;
                return (TruffleObject) maxMethod;
            case AltInteger_Min:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object minMethod = ((AltIntegerClassDescriptor) descriptor).getMinDownCall().method;
                assert minMethod instanceof TruffleObject;
                return (TruffleObject) minMethod;
            case AltInteger_Sum:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object sumMethod = ((AltIntegerClassDescriptor) descriptor).getSumDownCall().method;
                assert sumMethod instanceof TruffleObject;
                return (TruffleObject) sumMethod;
            case AltInteger_No_NA:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object noNaMethod = ((AltIntegerClassDescriptor) descriptor).getNoNADownCall().method;
                assert noNaMethod instanceof TruffleObject;
                return (TruffleObject) noNaMethod;
            case AltInteger_Get_region:
                assert descriptor instanceof AltIntegerClassDescriptor;
                Object getRegionMethod = ((AltIntegerClassDescriptor) descriptor).getGetRegionDownCall().method;
                assert getRegionMethod instanceof TruffleObject;
                return (TruffleObject) getRegionMethod;
            default:
                throw RInternalError.unimplemented();
        }
    }

    private static NativeMirror wrapInNativeMirror(RBaseObject object) {
        NativeMirror mirror = object.getNativeMirror();
        if (mirror != null) {
            return mirror;
        } else {
            return NativeDataAccess.createNativeMirror(object);
        }
    }
}
