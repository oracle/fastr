/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.ffi.impl.nodes.RfEvalNodeGen.FunctionEvalNodeGen;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.nodes.function.RFunctionEvalNodes.FunctionEvalCallNode;
import com.oracle.truffle.r.nodes.function.RFunctionEvalNodes.FunctionInfo;
import com.oracle.truffle.r.nodes.function.RFunctionEvalNodes.FunctionInfoNode;
import com.oracle.truffle.r.nodes.function.RFunctionEvalNodes.SlowPathFunctionEvalCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.RContext;
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

    protected RfEvalNode() {
    }

    public static RfEvalNode create() {
        return RfEvalNodeGen.create();
    }

    @TruffleBoundary
    private static RCaller createCall(REnvironment env) {
        Frame frame = getCurrentRFrameFastPath();
        if (frame == null) {
            // Is it necessary?
            // Warning: Utils.getActualCurrentFrame causes deopt
            Utils.getActualCurrentFrame();
        }
        final MaterializedFrame envFrame = env.getFrame();
        RCaller originalCaller = RArguments.getCall(envFrame);
        if (!RCaller.isValidCaller(originalCaller) && env instanceof REnvironment.NewEnv) {
            // Try to find the valid original caller stored in the original frame of a
            // VirtualEvalFrame that is the same as envFrame
            RCaller validOrigCaller = Utils.iterateRFrames(FrameAccess.READ_ONLY, (f) -> {
                if (f instanceof VirtualEvalFrame && ((VirtualEvalFrame) f).getOriginalFrame() == envFrame) {
                    return RArguments.getCall(f);
                } else {
                    return null;
                }
            });
            if (validOrigCaller != null) {
                originalCaller = validOrigCaller;
            }
        }
        RCaller currentCaller = RArguments.getCall(frame);
        if (env == REnvironment.globalEnv(RContext.getInstance())) {
            return RCaller.createForPromise(originalCaller, currentCaller);
        } else {
            return RCaller.createForPromise(originalCaller, env, currentCaller);
        }
    }

    @Specialization
    Object handlePromise(RPromise expr, @SuppressWarnings("unused") RNull nulLEnv) {
        return getPromiseHelper().visibleEvaluate(getCurrentRFrame(), expr);
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
    @TruffleBoundary
    Object handleExpression(RExpression expr, Object envArg) {
        REnvironment env = getEnv(envArg);
        return RContext.getEngine().eval(expr, env, createCall(env));
    }

    /**
     * Evaluates the function call defined in {@code FunctionInfo} in the fast path.
     */
    abstract static class FunctionEvalNode extends Node {

        protected static final int CACHE_SIZE = DSLConfig.getCacheSize(100);

        private final ValueProfile frameProfile = ValueProfile.createClassProfile();
        private final ValueProfile frameAccessProfile = ValueProfile.createClassProfile();

        abstract Object execute(Object downcallFrame, FunctionInfo functionInfo);

        @Specialization(limit = "CACHE_SIZE", guards = {"functionInfo.env.getFrame().getFrameDescriptor() == cachedDesc"})
        Object evalFastPath(Object downcallFrame, FunctionInfo functionInfo,
                        @SuppressWarnings("unused") @Cached("functionInfo.env.getFrame().getFrameDescriptor()") FrameDescriptor cachedDesc,
                        @Cached("new()") FunctionEvalCallNode callNode,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib,
                        @Cached("create()") BranchProfile symbolArgProfile,
                        @Cached("create()") BranchProfile pairListArgProfile,
                        @Cached("create()") BranchProfile namedArgsProfile) {
            MaterializedFrame materializedFrame = (MaterializedFrame) downcallFrame;
            MaterializedFrame promiseFrame = frameProfile.profile(functionInfo.env.getFrame(frameAccessProfile)).materialize();
            MaterializedFrame evalFrame = getEvalFrame(materializedFrame, promiseFrame);
            RArgsValuesAndNames args = functionInfo.prepareArguments(evalFrame, symbolArgProfile, pairListArgProfile, namedArgsProfile, plLib);
            RCaller caller = RCallerHelper.getExplicitCaller(materializedFrame, promiseFrame, functionInfo.env, null, functionInfo.function, args);
            RArguments.setCall(evalFrame, caller);

            Object resultValue = callNode.execute(evalFrame, functionInfo.function, args, caller, materializedFrame);
            // setVisibility ???
            return resultValue;
        }

        @Specialization(replaces = "evalFastPath")
        Object evalSlowPath(Object downcallFrame, FunctionInfo functionInfo,
                        @Cached("new()") SlowPathFunctionEvalCallNode slowPathCallNode,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib,
                        @Cached("create()") BranchProfile symbolArgProfile,
                        @Cached("create()") BranchProfile pairListArgProfile,
                        @Cached("create()") BranchProfile namedArgsProfile) {
            MaterializedFrame materializedFrame = (MaterializedFrame) downcallFrame;
            MaterializedFrame promiseFrame = frameProfile.profile(functionInfo.env.getFrame(frameAccessProfile)).materialize();
            MaterializedFrame evalFrame = getEvalFrame(materializedFrame, promiseFrame);
            RArgsValuesAndNames args = functionInfo.prepareArguments(evalFrame, symbolArgProfile, pairListArgProfile, namedArgsProfile, plLib);
            RCaller caller = RCallerHelper.getExplicitCaller(materializedFrame, promiseFrame, functionInfo.env, null, functionInfo.function, args);
            RArguments.setCall(evalFrame, caller);

            Object resultValue = slowPathCallNode.execute(evalFrame, materializedFrame, caller, functionInfo.function, args);
            // setVisibility ???
            return resultValue;
        }

        private static MaterializedFrame getEvalFrame(VirtualFrame currentFrame, MaterializedFrame envFrame) {
            return VirtualEvalFrame.create(envFrame, RArguments.getFunction(currentFrame), currentFrame, null);
        }

    }

    @Child private FunctionEvalNode functionEvalNode;

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
                    @Cached("create()") FunctionInfoNode funInfoNode) {
        MaterializedFrame downcallFrame = getCurrentRFrameFastPath();
        if (downcallFrame == null) {
            noDowncallFrameFunProfile.enter();
            return handleLanguageDefault(expr, envArg);
        }

        REnvironment env = getEnv(envArg);
        FunctionInfo functionInfo = funInfoNode.execute(expr, env);
        if (functionInfo == null) {
            nullFunProfile.enter();
            return handleLanguageDefault(expr, envArg);
        }

        if (functionEvalNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            functionEvalNode = insert(FunctionEvalNodeGen.create());
        }

        try {
            return functionEvalNode.execute(downcallFrame, functionInfo);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private static MaterializedFrame getCurrentRFrameFastPath() {
        RFFIContext context = RContext.getInstance().getStateRFFI();
        return context.rffiContextState.currentDowncallFrame;
    }

    @TruffleBoundary
    private static MaterializedFrame getCurrentRFrameSlowPath() {
        return Utils.getActualCurrentFrame().materialize();
    }

    private static MaterializedFrame getCurrentRFrame() {
        MaterializedFrame frame = getCurrentRFrameFastPath();
        return frame != null ? frame : getCurrentRFrameSlowPath();
    }

    @TruffleBoundary
    private Object handleLanguageDefault(RPairList expr, Object envArg) {
        REnvironment env = getEnv(envArg);
        return RContext.getEngine().eval(expr, env, createCall(env));
    }

    @Specialization
    @TruffleBoundary
    Object handleSymbol(RSymbol expr, Object envArg,
                    @Cached("createClassProfile()") ValueProfile accessProfile) {
        Object result = ReadVariableNode.lookupAny(expr.getName(), getEnv(envArg).getFrame(accessProfile), false);
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
                    @Cached("createBinaryProfile()") ConditionProfile noArgsProfile) {
        REnvironment env = getEnv(envArg);
        Object car = l.car();
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

        Object args = l.cdr();
        if (noArgsProfile.profile(args == RNull.instance)) {
            return evalFunction(f, env, null);
        } else {
            RList argsList = ((RPairList) args).toRList();
            return evalFunction(f, env, ArgumentsSignature.fromNamesAttribute(argsList.getNames()), argsList.getDataTemp());
        }
    }

    @TruffleBoundary
    private static Object evalFunction(RFunction f, REnvironment env, ArgumentsSignature argsNames, Object... args) {
        return RContext.getEngine().evalFunction(f, env == REnvironment.globalEnv() ? null : env.getFrame(), createCall(env), true, argsNames, args);
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

    private REnvironment getEnv(Object envArg) {
        if (envIsNullProfile.profile(envArg == RNull.instance)) {
            return REnvironment.globalEnv(RContext.getInstance());
        } else if (envArg instanceof REnvironment) {
            REnvironment env = (REnvironment) envArg;
            if (env == REnvironment.emptyEnv()) {
                return RContext.getInstance().stateREnvironment.getEmptyDummy();
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
