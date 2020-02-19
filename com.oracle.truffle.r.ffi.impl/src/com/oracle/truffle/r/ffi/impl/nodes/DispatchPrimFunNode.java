package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RForeignObjectWrapper;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNodeGen;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class DispatchPrimFunNode extends RBaseNode {

    public static DispatchPrimFunNode create() {
        return DispatchPrimFunNodeGen.create();
    }

    public static DispatchPrimFunNode getUncached() {
        return DispatchPrimFunNodeGen.getUncached();
    }

    public abstract Object executeObject(Object call, Object op, Object args, Object rho);

    static RRuntimeASTAccess.ExplicitFunctionCall createFunctionCallNode() {
        return RContext.getRRuntimeASTAccess().createExplicitFunctionCall();
    }

    static MaterializedFrame getCurrentRFrame(RContext ctxRef) {
        RFFIContext context = ctxRef.getStateRFFI();
        return context.rffiContextState.currentDowncallFrame;
    }

    static FrameDescriptor getCurrentRFrameDescriptor(RContext ctxRef) {
        return getCurrentRFrame(ctxRef).getFrameDescriptor();
    }

    private static final int MAX_UNWRAP_NODES = 5;

    private static final FFIUnwrapNode[] emptyUnwrapNodes = new FFIUnwrapNode[0];

    static FFIUnwrapNode[] createUnwrapNodes() {
        FFIUnwrapNode[] nodes = new FFIUnwrapNode[MAX_UNWRAP_NODES];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = FFIUnwrapNodeGen.create();
        }
        return nodes;
    }

    @Specialization(guards = "getCurrentRFrameDescriptor(ctxRef.get()) == cachedFrameDesc", limit = "3")
    @ExplodeLoop
    static Object dispatchCached(@SuppressWarnings("unused") Object call, RFunction function, RPairList args, @SuppressWarnings("unused") Object rho,
                    @Cached("createFunctionCallNode()") RRuntimeASTAccess.ExplicitFunctionCall callNode,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                    @SuppressWarnings("unused") @Cached("getCurrentRFrameDescriptor(ctxRef.get())") FrameDescriptor cachedFrameDesc,
                    @Cached("createUnwrapNodes()") FFIUnwrapNode[] unwrapNodes,
                    @Cached BranchProfile argsCountBelowBoundsProfile,
                    @Cached BranchProfile argsCountOutOfBoundsProfile) {
        RList argsList = args.toRList();
        RArgsValuesAndNames argsAndNames;
        if (argsList.getLength() == 0) {
            argsAndNames = RArgsValuesAndNames.EMPTY;
        } else {
            ArgumentsSignature argSig = ArgumentsSignature.fromNamesAttribute(argsList.getNames());
            if (argSig == null) {
                argSig = ArgumentsSignature.empty(argsList.getLength());
            }
            Object[] argsWrapped = (Object[]) argsList.getDataTemp();
            Object[] argsUnwrapped = new Object[argsWrapped.length];
            for (int i = 0; i < MAX_UNWRAP_NODES; i++) {
                if (i >= argsWrapped.length) {
                    argsCountBelowBoundsProfile.enter();
                    break;
                }

                Object arg = argsWrapped[i];
                if (arg instanceof RForeignObjectWrapper) {
                    argsUnwrapped[i] = ((RForeignObjectWrapper) arg).getDelegate();
                } else if (arg instanceof RTruffleObject) {
                    argsUnwrapped[i] = arg;
                } else if (arg instanceof Double || arg instanceof Integer || arg instanceof Byte) {
                    argsUnwrapped[i] = arg;
                } else {
                    argsUnwrapped[i] = unwrapNodes[i].execute(arg);
                }
            }

            if (argsWrapped.length >= MAX_UNWRAP_NODES) {
                argsCountOutOfBoundsProfile.enter();
                unwrapArgsGeneric(argsWrapped, argsUnwrapped);
            }

            argsAndNames = new RArgsValuesAndNames(argsUnwrapped, argSig);
        }

        boolean primFunBeingDispatchedSaved = ctxRef.get().getStateRFFI().rffiContextState.primFunBeingDispatched;
        ctxRef.get().getStateRFFI().rffiContextState.primFunBeingDispatched = true;
        try {
            return callNode.call(getCurrentRFrame(ctxRef.get()), function, argsAndNames);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (ReturnException e) {
            return e.getResult();
        } finally {
            ctxRef.get().getStateRFFI().rffiContextState.primFunBeingDispatched = primFunBeingDispatchedSaved;
        }
    }

    private static void unwrapArgsGeneric(Object[] argsWrapped, Object[] argsUnwrapped) {
        for (int i = MAX_UNWRAP_NODES; i < argsWrapped.length; i++) {
            Object arg = argsWrapped[i];
            if (arg instanceof RForeignObjectWrapper) {
                argsUnwrapped[i] = ((RForeignObjectWrapper) arg).getDelegate();
            } else if (arg instanceof RTruffleObject) {
                argsUnwrapped[i] = arg;
            } else if (arg instanceof Double || arg instanceof Integer || arg instanceof Byte) {
                argsUnwrapped[i] = arg;
            } else {
                argsUnwrapped[i] = FFIUnwrapNodeGen.getUncached().execute(arg);
            }
        }
    }

    @Specialization(replaces = "dispatchCached")
    static Object dispatchGeneric(@SuppressWarnings("unused") Object call, RFunction function, RPairList args, @SuppressWarnings("unused") Object rho,
                    @Cached("createFunctionCallNode()") RRuntimeASTAccess.ExplicitFunctionCall callNode,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        return dispatchCached(call, function, args, rho, callNode, ctxRef, null, emptyUnwrapNodes, BranchProfile.getUncached(), BranchProfile.getUncached());
    }

}
