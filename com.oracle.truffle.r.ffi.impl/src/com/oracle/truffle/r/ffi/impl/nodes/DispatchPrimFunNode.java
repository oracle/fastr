package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;

public abstract class DispatchPrimFunNode extends FFIUpCallNode.Arg4 {

    public static DispatchPrimFunNode create() {
        return DispatchPrimFunNodeGen.create();
    }

    public static DispatchPrimFunNode getUncached() {
        return DispatchPrimFunNodeGen.getUncached();
    }

    static RRuntimeASTAccess.ExplicitFunctionCall createFunctionCallNode() {
        return RContext.getRRuntimeASTAccess().createExplicitFunctionCall();
    }

    static RRuntimeASTAccess.ExplicitFunctionCall createSlowPathFunctionCallNode() {
        return RContext.getRRuntimeASTAccess().createSlowPathExplicitFunctionCall();
    }

    static MaterializedFrame getCurrentRFrame(RContext ctxRef) {
        RFFIContext context = ctxRef.getStateRFFI();
        return context.rffiContextState.currentDowncallFrame;
    }

    static FrameDescriptor getCurrentRFrameDescriptor(RContext ctxRef) {
        return getCurrentRFrame(ctxRef).getFrameDescriptor();
    }

    @Specialization(guards = "getCurrentRFrameDescriptor(ctxRef.get()) == cachedFrameDesc", limit = "3")
    @ExplodeLoop
    static Object dispatchCached(@SuppressWarnings("unused") Object call, RFunction function, RPairList args, @SuppressWarnings("unused") Object rho,
                    @Cached("createFunctionCallNode()") RRuntimeASTAccess.ExplicitFunctionCall callNode,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef,
                    @SuppressWarnings("unused") @Cached("getCurrentRFrameDescriptor(ctxRef.get())") FrameDescriptor cachedFrameDesc) {
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
            argsAndNames = new RArgsValuesAndNames(argsWrapped, argSig);
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

    @Specialization(replaces = "dispatchCached")
    static Object dispatchGeneric(@SuppressWarnings("unused") Object call, RFunction function, RPairList args, @SuppressWarnings("unused") Object rho,
                    @Cached("createSlowPathFunctionCallNode()") RRuntimeASTAccess.ExplicitFunctionCall callNode,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        return dispatchCached(call, function, args, rho, callNode, ctxRef, null);
    }

}
