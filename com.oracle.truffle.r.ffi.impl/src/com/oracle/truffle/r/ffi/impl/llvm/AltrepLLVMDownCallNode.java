package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
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
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory.DownCallNode;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;

public abstract class AltrepLLVMDownCallNode extends DownCallNode {
    @Child private LLVMDownCallNode llvmDownCallNode;
    // TODO: Use descriptorHandle instead of descriptor
    private final AltRepClassDescriptor descriptor;

    public static AltrepLLVMDownCallNode create(AltRepClassDescriptor descriptor) {
        return AltrepLLVMDownCallNodeGen.create(descriptor);
    }

    public AltrepLLVMDownCallNode(AltRepClassDescriptor descriptor) {
        DownCallNode downCallNode = TruffleLLVM_DownCallNodeFactory.INSTANCE.createDownCallNode();
        assert downCallNode instanceof LLVMDownCallNode;
        this.llvmDownCallNode = (LLVMDownCallNode) downCallNode;
        this.descriptor = descriptor;
    }

    @Specialization
    protected Object doCall(NativeFunction f, Object[] args,
                            @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        return doCallImpl(f, args, ctxRef);
    }

    @ExplodeLoop
    @Override
    protected Object beforeCall(@SuppressWarnings("unused") NativeFunction nativeFunction, TruffleObject f, Object[] args) {
        llvmDownCallNode.beforeCall(nativeFunction, f, args);
        CompilerAsserts.compilationConstant(args.length);
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof RBaseObject) {
                args[i] = wrapInNativeMirror((RBaseObject) args[i]);
            }
        }
        return 0;
    }

    @Override
    protected void afterCall(Object before, NativeFunction f, TruffleObject t, Object[] args) {

    }

    @Override
    protected TruffleObject createTarget(ContextReference<RContext> ctxRef, NativeFunction f) {
        // TODO: Get descriptor from ctxRef via descriptorHandle
        if (f == NativeFunction.AltInteger_Is_sorted) {
            assert descriptor instanceof AltIntegerClassDescriptor;
            Object isSortedMethod = ((AltIntegerClassDescriptor) descriptor).getIsSortedMethod();
            assert isSortedMethod != null;
            assert isSortedMethod instanceof TruffleObject;
            return (TruffleObject) isSortedMethod;
        }
        throw RInternalError.unimplemented();
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
