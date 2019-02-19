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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.ffi.impl.nodes.RfEvalNodeGen.FunctionEvalNodeGen;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.nodes.function.call.SlowPathExplicitCall;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

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
        Frame frame = getCurrentRFrame();
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
    @TruffleBoundary
    Object handlePromise(RPromise expr, @SuppressWarnings("unused") RNull nulLEnv) {
        return getPromiseHelper().visibleEvaluate(Utils.getActualCurrentFrame().materialize(), expr);
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

    static final class FunctionInfo {
        final RFunction function;
        final RArgsValuesAndNames args;

        FunctionInfo(RFunction function, RArgsValuesAndNames args) {
            this.function = function;
            this.args = args;
        }

        static FunctionInfo fromPairList(RFunction fun, RPairList argList, REnvironment env) {
            int len = 0;
            boolean named = false;
            if (argList != null) {
                for (RPairList item : argList) {
                    named = named || !item.isNullTag();
                    len++;
                }
            }
            Object[] args = new Object[len];
            String[] names = named ? new String[len] : null;
            int i = 0;
            if (argList != null) {
                for (RPairList plt : argList) {
                    Object a = plt.car();

                    if (a instanceof RSymbol) {
                        RSyntaxNode lookupSyntaxNode = createLookupNode(a);
                        Closure closure = Closure.createPromiseClosure(lookupSyntaxNode.asRNode());
                        args[i] = RDataFactory.createPromise(PromiseState.Supplied, closure, env.getFrame());
                    } else if (a instanceof RPairList) {
                        RPairList aPL = (RPairList) a;
                        aPL = RDataFactory.createPairList(aPL.car(), aPL.cdr(), aPL.getTag(), SEXPTYPE.LANGSXP);
                        createPromise(env, args, i, aPL);
                    } else {
                        args[i] = a;
                    }

                    if (named) {
                        Object ptag = plt.getTag();
                        if (RPairList.isNull(ptag)) {
                            names[i] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                        } else if (ptag instanceof RSymbol) {
                            names[i] = ((RSymbol) ptag).getName();
                        } else {
                            names[i] = RRuntime.asString(ptag);
                            assert names[i] != null : "unexpected type of tag in RPairList";
                        }
                    }
                    i++;
                }
            }

            return new FunctionInfo(fun, new RArgsValuesAndNames(args, named ? ArgumentsSignature.get(names) : ArgumentsSignature.empty(len)));
        }

        private static void createPromise(REnvironment env, Object[] args, int i, RPairList aPL) {
            args[i] = RDataFactory.createPromise(PromiseState.Supplied, aPL.getClosure(), env.getFrame());
        }

        @TruffleBoundary
        private static RSyntaxNode createLookupNode(Object a) {
            RSyntaxNode lookupSyntaxNode = RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, ((RSymbol) a).getName(), false);
            return lookupSyntaxNode;
        }

        static FunctionInfo get(RPairList expr, REnvironment env) {
            Object p = expr.car();
            if (p instanceof RFunction) {
                Object argList = expr.cdr();
                return FunctionInfo.fromPairList((RFunction) p, argList instanceof RPairList ? (RPairList) argList : null, env);
            } else if (p instanceof RSymbol && tryCatch == ((RSymbol) p).getName()) {
                Object pp = expr.cdr();
                if (pp instanceof RPairList) {
                    pp = ((RPairList) pp).car();
                    if (pp instanceof RPairList) {
                        p = ((RPairList) pp).car();
                        if (p instanceof RSymbol && evalq == ((RSymbol) p).getName()) {
                            pp = ((RPairList) pp).cdr();
                            if (pp instanceof RPairList) {
                                pp = ((RPairList) pp).car();
                                if (pp instanceof RPairList) {
                                    p = ((RPairList) pp).car();
                                    if (p instanceof RFunction) {
                                        Object argList = ((RPairList) pp).cdr();
                                        return FunctionInfo.fromPairList((RFunction) p, argList instanceof RPairList ? (RPairList) argList : null, env);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

    }

    private static final String tryCatch = "tryCatch";
    private static final String evalq = "evalq";

    abstract static class FunctionEvalNode extends Node {

        protected static final int CACHE_SIZE = DSLConfig.getCacheSize(100);

        private final ValueProfile frameProfile = ValueProfile.createClassProfile();
        private final ValueProfile frameAccessProfile = ValueProfile.createClassProfile();

        abstract Object execute(Object downcallFrame, FunctionInfo functionInfo, REnvironment env);

        @Specialization(limit = "CACHE_SIZE", guards = {"env.getFrame().getFrameDescriptor() == cachedDesc"})
        Object evalFastPath(Object downcallFrame, FunctionInfo functionInfo, REnvironment env,
                        @SuppressWarnings("unused") @Cached("env.getFrame().getFrameDescriptor()") FrameDescriptor cachedDesc,
                        @Cached("create()") RExplicitCallNode explicitCallNode) {
            MaterializedFrame materializedFrame = (MaterializedFrame) downcallFrame;
            MaterializedFrame promiseFrame = frameProfile.profile(env.getFrame(frameAccessProfile)).materialize();
            RCaller caller = RCallerHelper.getExplicitCaller(materializedFrame, promiseFrame, env, null, functionInfo.function, functionInfo.args);
            MaterializedFrame evalFrame = getEvalFrame(materializedFrame, promiseFrame);

            Object resultValue = explicitCallNode.execute(evalFrame, functionInfo.function, functionInfo.args, caller, materializedFrame);
            // setVisibility(virtualFrame, getVisibilityNode.execute(evalFrame));
            return resultValue;
        }

        @Specialization(replaces = "evalFastPath")
        Object evalSlowPath(Object downcallFrame, FunctionInfo functionInfo, REnvironment env,
                        @Cached("create()") SlowPathExplicitCall slowPathExplicitCall) {
            MaterializedFrame materializedFrame = (MaterializedFrame) downcallFrame;
            MaterializedFrame promiseFrame = frameProfile.profile(env.getFrame(frameAccessProfile)).materialize();
            RCaller caller = RCallerHelper.getExplicitCaller(materializedFrame, promiseFrame, env, null, functionInfo.function, functionInfo.args);
            MaterializedFrame evalFrame = getEvalFrame(materializedFrame, promiseFrame);

            Object resultValue = slowPathExplicitCall.execute(evalFrame, materializedFrame, caller, functionInfo.function, functionInfo.args);
            // setVisibility(virtualFrame, getVisibilityNode.execute(evalFrame));
            return resultValue;
        }

        private static MaterializedFrame getEvalFrame(VirtualFrame currentFrame, MaterializedFrame envFrame) {
            return VirtualEvalFrame.create(envFrame, RArguments.getFunction(currentFrame), currentFrame, RArguments.getCall(currentFrame));
        }

    }

    @Child private FunctionEvalNode functionEvalNode;

    @Specialization(guards = "expr.isLanguage()")
    Object handleLanguage(RPairList expr, Object envArg,
                    @Cached("create()") BranchProfile noDowncallFrameFunProfile,
                    @Cached("create()") BranchProfile nullFunProfile) {
        MaterializedFrame downcallFrame = getCurrentRFrame();
        if (downcallFrame == null) {
            noDowncallFrameFunProfile.enter();
            return handleLanguageDefault(expr, envArg);
        }

        REnvironment env = getEnv(envArg);
        FunctionInfo functionInfo = FunctionInfo.get(expr, env);
        if (functionInfo == null) {
            nullFunProfile.enter();
            return handleLanguageDefault(expr, envArg);
        }

        if (functionEvalNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            functionEvalNode = insert(FunctionEvalNodeGen.create());
        }

        return functionEvalNode.execute(downcallFrame, functionInfo, env);
    }

    private static MaterializedFrame getCurrentRFrame() {
        RFFIContext context = RContext.getInstance().getStateRFFI();
        return context.rffiContextState.downcallFrameStack.get(context.rffiContextState.downcallFrameStack.size() - 1);
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
