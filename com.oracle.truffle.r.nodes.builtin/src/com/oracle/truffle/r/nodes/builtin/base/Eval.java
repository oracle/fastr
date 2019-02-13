/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.mustBe;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.EnvironmentNodes.RList2EnvNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.EvalNodeGen.EvalEnvCastNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.FrameFunctions.SysFrame;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctions.Get;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;

/**
 * Contains the {@code eval} {@code .Internal} implementation.
 */
@RBuiltin(name = "eval", visibility = CUSTOM, kind = INTERNAL, parameterNames = {"expr", "envir", "enclos"}, behavior = COMPLEX)
public abstract class Eval extends RBuiltinNode.Arg3 {

    /**
     * Profiling for catching {@link ReturnException}s.
     */
    private final ConditionProfile returnTopLevelProfile = ConditionProfile.createBinaryProfile();

    /**
     * Eval takes two arguments that specify the environment where the expression should be
     * evaluated: 'envir', 'enclos'. These arguments are pre-processed by the means of default
     * values in the R stub function, but there is still several combinations of their possible
     * values that may make it into the internal code. This node handles these. See the
     * documentation of eval for more details.
     */
    abstract static class EvalEnvCast extends RBaseNode {

        @Child private RList2EnvNode rList2EnvNode;

        public static EvalEnvCast create() {
            return EvalEnvCastNodeGen.create();
        }

        public abstract REnvironment execute(VirtualFrame frame, Object env, Object enclos);

        @Specialization
        protected REnvironment cast(@SuppressWarnings("unused") RNull env, @SuppressWarnings("unused") RNull enclos) {
            return REnvironment.baseEnv();
        }

        @Specialization
        protected REnvironment cast(REnvironment env, @SuppressWarnings("unused") RNull enclos) {
            return env;
        }

        @Specialization
        protected REnvironment cast(REnvironment env, @SuppressWarnings("unused") REnvironment enclos) {
            // from the doc: enclos is only relevant when envir is list or pairlist
            return env;
        }

        @Specialization
        protected REnvironment cast(@SuppressWarnings("unused") RNull env, REnvironment enclos) {
            // seems not to be documented, but GnuR works this way
            return enclos;
        }

        @Specialization
        protected REnvironment cast(RList list, REnvironment enclos) {
            lazyCreateRList2EnvNode();
            return rList2EnvNode.execute(list, null, null, enclos);
        }

        @Specialization(guards = "!list.isLanguage()")
        protected REnvironment cast(RPairList list, REnvironment enclos) {
            lazyCreateRList2EnvNode();
            return rList2EnvNode.execute(list.toRList(), null, null, enclos);
        }

        @Specialization
        protected REnvironment cast(RList list, @SuppressWarnings("unused") RNull enclos) {
            lazyCreateRList2EnvNode();

            // This can happen when envir is a list and enclos is explicitly set to NULL
            return rList2EnvNode.execute(list, null, null, REnvironment.baseEnv());
        }

        @Specialization(guards = "!list.isLanguage()")
        protected REnvironment cast(RPairList list, @SuppressWarnings("unused") RNull enclos) {
            lazyCreateRList2EnvNode();

            // This can happen when envir is a pairlist and enclos is explicitly set to NULL
            return rList2EnvNode.execute(list.toRList(), null, null, REnvironment.baseEnv());
        }

        private void lazyCreateRList2EnvNode() {
            if (rList2EnvNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                rList2EnvNode = insert(RList2EnvNode.create());
            }
        }

        @Specialization
        protected REnvironment cast(VirtualFrame frame, int envir, @SuppressWarnings("unused") Object enclos,
                        @Cached("createSysFrame()") SysFrame sysFrameNode) {
            return sysFrameNode.executeInt(frame, envir);
        }

        protected static SysFrame createSysFrame() {
            // SysFrame.create(skipDotInternal=true) because we are invoking SysFrame directly and
            // normally SysFrame skips its .Internal frame
            return SysFrame.create(true);
        }
    }

    @Child private EvalEnvCast envCast = EvalEnvCastNodeGen.create();
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    static {
        Casts casts = new Casts(Eval.class);
        casts.arg("envir").allowNull().mustBe(instanceOf(REnvironment.class).or(instanceOf(RList.class)).or(instanceOf(RPairList.class)).or(numericValue())).mapIf(numericValue(),
                        chain(asIntegerVector()).with(mustBe(singleElement())).with(findFirst().integerElement()).end());
        casts.arg("enclos").allowNull().mustBe(REnvironment.class);
    }

    @Specialization(guards = "expr.isLanguage()")
    protected Object doEval(VirtualFrame frame, RPairList expr, Object envir, Object enclos) {
        REnvironment environment = envCast.execute(frame, envir, enclos);
        RCaller call = RArguments.getCall(frame);
        RCaller rCaller = getCaller(frame, call.isValidCaller() ? () -> call.getSyntaxNode() : null);
        try {
            RFunction evalFun = getFunctionArgument();
            return RContext.getEngine().eval(expr, environment, frame.materialize(), rCaller, evalFun);
        } catch (ReturnException ret) {
            if (returnTopLevelProfile.profile(ret.getTarget() == rCaller)) {
                return ret.getResult();
            } else {
                throw ret;
            }
        } finally {
            visibility.executeAfterCall(frame, rCaller);
        }
    }

    @Specialization
    protected Object doEval(VirtualFrame frame, RExpression expr, Object envir, Object enclos) {
        REnvironment environment = envCast.execute(frame, envir, enclos);
        // TODO: how the call should look like for an expression? Block statement?
        RCaller call = RArguments.getCall(frame);
        RCaller rCaller = getCaller(frame, call.isValidCaller() ? () -> call.getSyntaxNode() : null);
        try {
            RFunction evalFun = getFunctionArgument();
            return RContext.getEngine().eval(expr, environment, frame.materialize(), rCaller, evalFun);
        } catch (ReturnException ret) {
            if (returnTopLevelProfile.profile(ret.getTarget() == rCaller)) {
                return ret.getResult();
            } else {
                throw ret;
            }
        } finally {
            visibility.executeAfterCall(frame, rCaller);
        }
    }

    /**
     * This follows GNU-R. If you asks for sys.function, of the 'eval' frame, you get
     * ".Primitive('eval')", which can be invoked.
     */
    private static RFunction getFunctionArgument() {
        return RContext.getInstance().lookupBuiltin("eval");
    }

    protected static Get createGet() {
        return GetNodeGen.create();
    }

    @Specialization(guards = "!isVariadicSymbol(expr)")
    protected Object doEval(VirtualFrame frame, RSymbol expr, Object envir, Object enclos,
                    @Cached("createGet()") Get get) {
        REnvironment environment = envCast.execute(frame, envir, enclos);
        try {
            // no need to do the full eval for symbols: just do the lookup
            return get.execute(frame, expr.getName(), environment, RType.Any.getName(), true);
        } finally {
            visibility.execute(frame, true);
        }
    }

    protected static PromiseCheckHelperNode createPromiseHelper() {
        return new PromiseCheckHelperNode();
    }

    @Specialization(guards = "isVariadicSymbol(expr)")
    protected Object doEvalVariadic(VirtualFrame frame, RSymbol expr, Object envir, Object enclos,
                    @Cached("createPromiseHelper()") PromiseCheckHelperNode promiseHelper) {
        REnvironment environment = envCast.execute(frame, envir, enclos);
        try {
            int index = getVariadicIndex(expr);
            Object args = ReadVariableNode.lookupAny(ArgumentsSignature.VARARG_NAME, environment.getFrame(), false);
            if (args == null) {
                throw error(RError.Message.NO_DOT_DOT, index + 1);
            }
            RArgsValuesAndNames argsValuesAndNames = (RArgsValuesAndNames) args;
            if (argsValuesAndNames.isEmpty()) {
                throw error(RError.Message.NO_LIST_FOR_CDR);
            }
            if (argsValuesAndNames.getLength() <= index) {
                throw error(RError.Message.DOT_DOT_SHORT, index + 1);
            }
            Object ret = argsValuesAndNames.getArgument(index);
            return ret == null ? RMissing.instance : promiseHelper.checkEvaluate(frame, ret);
        } finally {
            visibility.execute(frame, true);
        }
    }

    protected static boolean isVariadicSymbol(RSymbol sym) {
        String x = sym.getName();
        // TODO: variadic symbols can have two digits up to ".99"
        if (!Utils.identityEquals(x, ArgumentsSignature.VARARG_NAME) && x.length() > 2 && x.charAt(0) == '.' && x.charAt(1) == '.') {
            for (int i = 2; i < x.length(); i++) {
                if (!Character.isDigit(x.charAt(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static int getVariadicIndex(RSymbol sym) {
        String x = sym.getName();
        return Integer.parseInt(x.substring(2, x.length())) - 1;
    }

    @Fallback
    protected Object doEval(VirtualFrame frame, Object expr, Object envir, Object enclos) {
        // just return value
        envCast.execute(frame, envir, enclos);
        visibility.execute(frame, true);
        return expr;
    }

    private RCaller getCaller(VirtualFrame frame, Supplier<RSyntaxElement> call) {
        return call != null ? RCaller.create(frame, call) : RCaller.create(frame, getOriginalCall());
    }
}
