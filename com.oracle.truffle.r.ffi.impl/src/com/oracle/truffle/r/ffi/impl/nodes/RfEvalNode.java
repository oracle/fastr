/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import static com.oracle.truffle.r.runtime.RError.Message.ARGUMENT_NOT_ENVIRONMENT;
import static com.oracle.truffle.r.runtime.RError.Message.ARGUMENT_NOT_FUNCTION;
import static com.oracle.truffle.r.runtime.RError.Message.UNKNOWN_OBJECT;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.ffi.impl.nodes.RfEvalNodeGen.CachedCallInfoEvalNodeGen;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.nodes.function.opt.eval.AbstractCallInfoEvalNode;
import com.oracle.truffle.r.nodes.function.opt.eval.ArgValueSupplierNode;
import com.oracle.truffle.r.nodes.function.opt.eval.CallInfo;
import com.oracle.truffle.r.nodes.function.opt.eval.CallInfo.EvalMode;
import com.oracle.truffle.r.nodes.function.opt.eval.CallInfoEvalRootNode.FastPathDirectCallerNode;
import com.oracle.truffle.r.nodes.function.opt.eval.CallInfoEvalRootNode.SlowPathDirectCallerNode;
import com.oracle.truffle.r.nodes.function.opt.eval.CallInfoNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;

public abstract class RfEvalNode extends FFIUpCallNode.Arg2 {

    @Child private PromiseHelperNode promiseHelper;
    private final ConditionProfile envIsNullProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile nullFrameProfile = BranchProfile.create();
    private final BranchProfile tryToFindEnvCallerProfile = BranchProfile.create();
    private final BranchProfile globalEnvProfile = BranchProfile.create();

    protected RfEvalNode() {
    }

    public static RfEvalNode create() {
        return RfEvalNodeGen.create();
    }

    private RCaller createCall(REnvironment env, RContext rCtx) {
        Frame frame = getCurrentRFrameFastPath(rCtx);
        if (frame == null) {
            nullFrameProfile.enter();
            // Is it necessary?
            // Warning: Utils.getActualCurrentFrame causes deopt
            Utils.getActualCurrentFrame();
        }
        final MaterializedFrame envFrame = env.getFrame();
        RCaller originalCaller = RArguments.getCall(envFrame);
        if (!RCaller.isValidCaller(originalCaller) && env instanceof REnvironment.NewEnv) {
            tryToFindEnvCallerProfile.enter();
            // Try to find the valid original caller stored in the original frame of a
            // VirtualEvalFrame that is the same as envFrame
            RCaller validOrigCaller = RCallerHelper.tryToFindEnvCaller(envFrame);
            if (validOrigCaller != null) {
                originalCaller = validOrigCaller;
            }
        }
        RCaller currentCaller = RArguments.getCall(frame);
        if (env == REnvironment.globalEnv(rCtx)) {
            globalEnvProfile.enter();
            return RCaller.createForPromise(originalCaller, currentCaller);
        }
        return RCaller.createForPromise(originalCaller, env, currentCaller);
    }

    @Specialization
    Object handlePromise(RPromise expr, @SuppressWarnings("unused") RNull nulLEnv,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        return getPromiseHelper().visibleEvaluate(getCurrentRFrame(ctxRef.get()), expr);
    }

    @Specialization
    Object handlePromise(RPromise expr, REnvironment env, @Cached("createBinaryProfile()") ConditionProfile isEvaluatedProfile) {
        Object value = expr.getRawValue();
        if (isEvaluatedProfile.profile(value != null)) {
            return value;
        }

        return handlePromise(expr, env);
    }

    @TruffleBoundary
    Object handlePromise(RPromise expr, REnvironment env) {
        return getPromiseHelper().visibleEvaluate(env.getFrame(), expr);
    }

    @Specialization
    Object handleExpression(RExpression expr, Object envArg,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        RContext rCtx = ctxRef.get();
        REnvironment env = getEnv(envArg, rCtx);
        RCaller call = createCall(env, rCtx);
        return evaluateExpression(expr, ctxRef, env, call);
    }

    @TruffleBoundary
    private static Object evaluateExpression(RExpression expr, ContextReference<RContext> ctxRef, REnvironment env, RCaller call) {
        return ctxRef.get().getThisEngine().eval(expr, env, call);
    }

    /**
     * Evaluates the function call defined in {@code FunctionInfo} in the fast path.
     */
    @ReportPolymorphism
    abstract static class CachedCallInfoEvalNode extends AbstractCallInfoEvalNode {

        static CachedCallInfoEvalNode create() {
            return CachedCallInfoEvalNodeGen.create();
        }

        abstract Object execute(Object downcallFrame, CallInfo callInfo, RPairList expr);

        @Specialization(limit = "CACHE_SIZE", guards = {"cachedCallInfo.isCompatible(callInfo, otherInfoClassProfile)"})
        Object evalFastPath(Object downcallFrame, CallInfo callInfo, RPairList expr,
                        @CachedContext(TruffleRLanguage.class) RContext ctx,
                        @SuppressWarnings("unused") @Cached("createClassProfile()") ValueProfile otherInfoClassProfile,
                        @Cached("createClassProfile()") ValueProfile downcallFrameClassProfile,
                        @SuppressWarnings("unused") @Cached("callInfo.getCachedCallInfo()") CallInfo.CachedCallInfo cachedCallInfo,
                        @Cached("new()") FastPathDirectCallerNode callNode,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib,
                        @Cached("new()") PromiseHelperNode promiseHelper,
                        @Cached("createArgValueSupplierNodes(callInfo.argsLen, true)") ArgValueSupplierNode[] argValSupplierNodes) {
            MaterializedFrame materializedFrame = downcallFrameClassProfile.profile((MaterializedFrame) downcallFrame);
            MaterializedFrame promiseFrame = frameProfile.profile(callInfo.env.getFrame(frameAccessProfile)).materialize();

            RCaller promiseCaller = RCallerHelper.getPromiseCallerForExplicitCaller(ctx, materializedFrame,
                            promiseFrame, callInfo.env);
            MaterializedFrame evalFrame = getEvalFrame(materializedFrame, promiseFrame, promiseCaller);
            RArgsValuesAndNames args = callInfo.prepareArgumentsExploded(materializedFrame, evalFrame, plLib, promiseHelper, argValSupplierNodes);
            RCaller caller = RCallerHelper.getExplicitCaller(materializedFrame, expr, promiseCaller);

            Object resultValue = callNode.execute(evalFrame, callInfo.function, args, caller,
                            materializedFrame);
            // setVisibility ???
            return resultValue;
        }

        @Specialization(replaces = "evalFastPath", guards = "callInfo.argsLen <= MAX_ARITY")
        Object evalSlowPath(Object downcallFrame, CallInfo callInfo, RPairList expr,
                        @CachedContext(TruffleRLanguage.class) RContext ctx,
                        @Cached("createClassProfile()") ValueProfile downcallFrameClassProfile,
                        @Cached("new()") SlowPathDirectCallerNode slowPathCallNode,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib,
                        @Cached("new()") PromiseHelperNode promiseHelper,
                        @Cached("createGenericArgValueSupplierNodes(MAX_ARITY)") ArgValueSupplierNode[] argValSupplierNodes) {
            MaterializedFrame materializedFrame = downcallFrameClassProfile.profile((MaterializedFrame) downcallFrame);
            MaterializedFrame promiseFrame = frameProfile.profile(callInfo.env.getFrame(frameAccessProfile)).materialize();
            RCaller promiseCaller = RCallerHelper.getPromiseCallerForExplicitCaller(ctx, materializedFrame, promiseFrame, callInfo.env);
            MaterializedFrame evalFrame = getEvalFrame(materializedFrame, promiseFrame, promiseCaller);
            RArgsValuesAndNames args = callInfo.prepareArgumentsExploded(materializedFrame, evalFrame, plLib, promiseHelper, argValSupplierNodes);

            RCaller caller = RCallerHelper.getExplicitCaller(materializedFrame, expr, promiseCaller);

            Object resultValue = slowPathCallNode.execute(evalFrame, materializedFrame, caller, callInfo.function, args);
            // setVisibility ???
            return resultValue;
        }

        @Specialization(replaces = "evalFastPath")
        Object evalSlowPath(Object downcallFrame, CallInfo callInfo, RPairList expr,
                        @CachedContext(TruffleRLanguage.class) RContext ctx,
                        @Cached("createClassProfile()") ValueProfile downcallFrameClassProfile,
                        @Cached("new()") SlowPathDirectCallerNode slowPathCallNode,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib,
                        @Cached("new()") PromiseHelperNode promiseHelper,
                        @Cached("create(false)") ArgValueSupplierNode argValSupplierNode) {
            MaterializedFrame materializedFrame = downcallFrameClassProfile.profile((MaterializedFrame) downcallFrame);
            MaterializedFrame promiseFrame = frameProfile.profile(callInfo.env.getFrame(frameAccessProfile)).materialize();
            RCaller promiseCaller = RCallerHelper.getPromiseCallerForExplicitCaller(ctx, materializedFrame, promiseFrame, callInfo.env);
            MaterializedFrame evalFrame = getEvalFrame(materializedFrame, promiseFrame, promiseCaller);
            RArgsValuesAndNames args = callInfo.prepareArguments(materializedFrame, evalFrame, plLib, promiseHelper, argValSupplierNode);

            RCaller caller = RCallerHelper.getExplicitCaller(materializedFrame, expr, promiseCaller);

            Object resultValue = slowPathCallNode.execute(evalFrame, materializedFrame, caller, callInfo.function, args);
            // setVisibility ???
            return resultValue;
        }

        private static MaterializedFrame getEvalFrame(VirtualFrame currentFrame, MaterializedFrame envFrame, RCaller promiseCaller) {
            return VirtualEvalFrame.create(envFrame, RArguments.getFunction(currentFrame), currentFrame, promiseCaller);
        }

    }

    @Child private CachedCallInfoEvalNode cachedCallInfoEvalNode;

    /**
     * This specialization attempts to retrieve a function call from the expression and evaluate it
     * in the fast path. Otherwise it falls back into the default slow path expression evaluation.
     * It uses {@code FunctionInfoNode} to retrieve the function call from the expression and
     * {@code FunctionEvalNode} to evaluate the function call in the fast path.
     */
    @Specialization(guards = "expr.isLanguage()")
    Object handleLanguage(RPairList expr, Object envArg,
                    @Cached("create()") BranchProfile noDowncallFrameFunProfile,
                    @Cached("create()") BranchProfile nullFunProfile,
                    @Cached("create()") CallInfoNode cachedCallInfoNode,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        RContext rContext = ctxRef.get();
        MaterializedFrame downcallFrame = getCurrentRFrameFastPath(rContext);
        if (downcallFrame == null) {
            noDowncallFrameFunProfile.enter();
            return handleLanguageDefault(expr, envArg, rContext);
        }

        REnvironment env = getEnv(envArg, rContext);
        CallInfo cachedCallInfo = cachedCallInfoNode.execute(expr, env);
        if (cachedCallInfo == null || cachedCallInfo.evalMode == EvalMode.SLOW) {
            nullFunProfile.enter();
            return handleLanguageDefault(expr, envArg, rContext);
        }

        if (cachedCallInfoEvalNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cachedCallInfoEvalNode = insert(CachedCallInfoEvalNodeGen.create());
        }

        try {
            return cachedCallInfoEvalNode.execute(downcallFrame, cachedCallInfo, expr);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private static MaterializedFrame getCurrentRFrameFastPath(RContext rCtx) {
        RFFIContext context = rCtx.getStateRFFI();
        return context.rffiContextState.currentDowncallFrame;
    }

    @TruffleBoundary
    private static MaterializedFrame getCurrentRFrameSlowPath() {
        return Utils.getActualCurrentFrame().materialize();
    }

    private static MaterializedFrame getCurrentRFrame(RContext rCtx) {
        MaterializedFrame frame = getCurrentRFrameFastPath(rCtx);
        return frame != null ? frame : getCurrentRFrameSlowPath();
    }

    private Object handleLanguageDefault(RPairList expr, Object envArg, RContext rCtx) {
        REnvironment env = getEnv(envArg, rCtx);
        RCaller call = createCall(env, rCtx);
        return evaluateLanguage(expr, rCtx, env, call);
    }

    @TruffleBoundary
    private static Object evaluateLanguage(RPairList expr, RContext rCtx, REnvironment env, RCaller call) {
        return rCtx.getThisEngine().eval(expr, env, call);
    }

    @Specialization
    @TruffleBoundary
    Object handleSymbol(RSymbol expr, Object envArg,
                    @Cached("createClassProfile()") ValueProfile accessProfile,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        Object result = ReadVariableNode.lookupAny(expr.getName(), getEnv(envArg, ctxRef.get()).getFrame(accessProfile), false);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, UNKNOWN_OBJECT, expr.getName());
        }
        if (result instanceof ActiveBinding) {
            result = ((ActiveBinding) result).readValue();
        }
        return result;
    }

    @Specialization(guards = "!l.isLanguage()")
    Object handlePairList(RPairList l, Object envArg,
                    @Cached("createBinaryProfile()") ConditionProfile isPromiseProfile,
                    @Cached("createBinaryProfile()") ConditionProfile noArgsProfile,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib,
                    @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
        REnvironment env = getEnv(envArg, ctxRef.get());
        Object car = plLib.car(l);
        RFunction f = null;
        if (isPromiseProfile.profile(car instanceof RPromise)) {
            car = getPromiseHelper().visibleEvaluate(null, (RPromise) car);
        }

        if (car instanceof RFunction) {
            f = (RFunction) car;
        } else if (car instanceof RSymbol) {
            f = ReadVariableNode.lookupFunction(((RSymbol) car).getName(), env.getFrame());
        }

        if (f == null) {
            throw RError.error(RError.NO_CALLER, ARGUMENT_NOT_FUNCTION);
        }

        Object args = plLib.cdr(l);
        if (noArgsProfile.profile(args == RNull.instance)) {
            return evalFunction(f, env, null, ctxRef.get(), nullFrameProfile, tryToFindEnvCallerProfile, globalEnvProfile);
        } else {
            RList argsList = ((RPairList) args).toRList();
            return evalFunction(f, env, ArgumentsSignature.fromNamesAttribute(argsList.getNames()), ctxRef.get(), nullFrameProfile, tryToFindEnvCallerProfile, globalEnvProfile,
                            argsList.getDataTemp());
        }
    }

    private Object evalFunction(RFunction f, REnvironment env, ArgumentsSignature argsNames, RContext rCtx, Object... args) {
        RCaller call = createCall(env, rCtx);
        return evaluateFunction(f, env, argsNames, rCtx, call, args);
    }

    @TruffleBoundary
    private static Object evaluateFunction(RFunction f, REnvironment env, ArgumentsSignature argsNames, RContext rCtx, RCaller call, Object... args) {
        MaterializedFrame frame = env == REnvironment.globalEnv() ? null : env.getFrame();
        return rCtx.getThisEngine().evalFunction(f, frame, call, true, argsNames, args);
    }

    @Fallback
    Object handleOthers(Object expr, Object env) {
        if (env instanceof REnvironment) {
            return expr;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.NO_CALLER, ARGUMENT_NOT_ENVIRONMENT);
        }
    }

    private REnvironment getEnv(Object envArg, RContext rCtx) {
        if (envIsNullProfile.profile(envArg == RNull.instance)) {
            return REnvironment.globalEnv(rCtx);
        } else if (envArg instanceof REnvironment) {
            REnvironment env = (REnvironment) envArg;
            if (env == REnvironment.emptyEnv()) {
                return rCtx.stateREnvironment.getEmptyDummy();
            }
            return env;
        }
        CompilerDirectives.transferToInterpreter();
        throw RError.error(RError.NO_CALLER, ARGUMENT_NOT_ENVIRONMENT);
    }

    private PromiseHelperNode getPromiseHelper() {
        if (promiseHelper == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseHelper = insert(new PromiseHelperNode());
        }
        return promiseHelper;
    }
}
