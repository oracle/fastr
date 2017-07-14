/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.DoCallNodeGen.DoCallInternalNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctions.Get;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = ".fastr.do.call", visibility = CUSTOM, kind = RBuiltinKind.INTERNAL, parameterNames = {"what", "args", "quote", "envir"}, behavior = COMPLEX)
public abstract class DoCall extends RBuiltinNode.Arg4 implements InternalRSyntaxNodeChildren {

    static {
        Casts casts = new Casts(DoCall.class);
        casts.arg("what").defaultError(Message.MUST_BE_STRING_OR_FUNCTION, "what").mustBe(instanceOf(RFunction.class).or(stringValue()));
        casts.arg("args").mustBe(RAbstractListVector.class, Message.SECOND_ARGUMENT_LIST);
        casts.arg("quote").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        casts.arg("envir").mustNotBeMissing().mustBe(REnvironment.class, Message.MUST_BE_ENVIRON, "envir");
    }

    protected static Get createGet() {
        return GetNodeGen.create();
    }

    // Note: quote tells us if symbols in args list should be interpreted as symbols (quote=TRUE) or
    // they should be interpreted as read of given symbol from the given environment.

    @Specialization
    protected Object doCall(VirtualFrame frame, RAbstractStringVector what, RList argsAsList, boolean quote, REnvironment env,
                    @Cached("create()") DoCallInternal internal,
                    @Cached("createGet()") Get getNode) {
        if (what.getLength() != 1) {
            throw error(RError.Message.MUST_BE_STRING_OR_FUNCTION, "what");
        }
        // Note: if the function is in fact a promise, we are evaluating it here slightly earlier
        // than GNU R. It should not a be a problem.
        RFunction func = (RFunction) getNode.execute(frame, what.getDataAt(0), env, RType.Function.getName(), true);
        return doCall(frame, func, argsAsList, quote, env, internal);
    }

    @Specialization
    protected Object doCall(VirtualFrame virtualFrame, RFunction func, RList argsAsList, boolean quote, REnvironment env,
                    @Cached("create()") DoCallInternal internal) {
        return internal.execute(virtualFrame, func, argsAsList, quote, env);
    }

    protected abstract static class DoCallInternal extends Node {
        @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
        @Child private SetVisibilityNode setVisibilityNode = SetVisibilityNode.create();

        public static DoCallInternal create() {
            return DoCallInternalNodeGen.create();
        }

        public abstract Object execute(VirtualFrame virtualFrame, RFunction func, RList argsAsList, boolean quote, REnvironment env);

        protected final ArgumentsSignature getArgsNames(RList argsAsList) {
            ArgumentsSignature signature = ArgumentsSignature.fromNamesAttribute(getNamesNode.getNames(argsAsList));
            return signature == null ? ArgumentsSignature.empty(argsAsList.getLength()) : signature;
        }

        /**
         * Fast version that works only for simple cases. It does not explicitly create the AST and
         * evaluate it, but instead it directly implements what the execution of such AST would do.
         */
        @Specialization(guards = "isSimple(func, argsAsList)")
        public Object doSimple(VirtualFrame virtualFrame, RFunction func, RList argsAsList, boolean quote, REnvironment env,
                        @Cached("create()") ShareObjectNode shareObjectNode,
                        @Cached("createBinaryProfile()") ConditionProfile quoteProfile,
                        @Cached("create()") BranchProfile containsRSymbolProfile,
                        @Cached("createClassProfile()") ValueProfile frameAccessProfile) {
            Object[] argValuesData = argsAsList.getDataWithoutCopying();
            Object[] argValues = argValuesData;
            MaterializedFrame envFrame = env.getFrame(frameAccessProfile).materialize();
            if (quoteProfile.profile(!quote)) {
                argValues = Arrays.copyOf(argValuesData, argValuesData.length);
                for (int i = 0; i < argValues.length; i++) {
                    Object arg = argValues[i];
                    if (arg instanceof RSymbol) {
                        containsRSymbolProfile.enter();
                        RSymbol symbol = (RSymbol) arg;
                        if (symbol.getName().isEmpty()) {
                            argValues[i] = REmpty.instance;
                        } else {
                            argValues[i] = createLookupPromise(envFrame, symbol);
                        }
                    }
                }
            }
            for (int i = 0; i < argValues.length; i++) {
                shareObjectNode.execute(argValues[i]);
            }
            ArgumentsSignature signature = getArgsNames(argsAsList);
            RCaller caller = RCaller.create(virtualFrame, RCallerHelper.createFromArguments(func, new RArgsValuesAndNames(argValues, signature)));
            try {
                Object resultValue = RContext.getEngine().evalFunction(func, envFrame, caller, false, signature, argValues);
                setVisibilityNode.execute(virtualFrame, getVisibility(envFrame));
                return resultValue;
            } finally {
                for (int i = 0; i < argValues.length; i++) {
                    ShareObjectNode.unshare(argValues[i]);
                }
            }
        }

        protected static boolean isSimple(RFunction function, RList args) {
            RBuiltinDescriptor builtin = function.getRBuiltin();
            if (builtin != null && builtin.getDispatch() != RDispatch.DEFAULT) {
                return false;
            }
            for (int i = 0; i < args.getLength(); i++) {
                if (args.getDataAt(i) instanceof RLanguage) {
                    // Note: language is tricky because of formulae, which are language that is
                    // really not meant to be evaluated again in a different frame than the one were
                    // the were evaluated for the first time. The solution should be to clone the
                    // language's rep and get its nodes in uninitilized state, but that does not
                    // work for some reason.
                    return false;
                }
            }
            return true;
        }

        @TruffleBoundary
        private static RPromise createLookupPromise(MaterializedFrame callerFrame, RSymbol symbol) {
            Closure closure = RPromise.Closure.create(RContext.getASTBuilder().lookup(RSyntaxNode.SOURCE_UNAVAILABLE, symbol.getName(), false).asRNode());
            return RDataFactory.createPromise(PromiseState.Supplied, closure, callerFrame);
        }

        @TruffleBoundary
        private static boolean getVisibility(MaterializedFrame envFrame) {
            FrameSlot envVisibilitySlot = FrameSlotChangeMonitor.findOrAddFrameSlot(envFrame.getFrameDescriptor(), RFrameSlot.Visibility, FrameSlotKind.Boolean);
            if (envVisibilitySlot != null) {
                try {
                    return envFrame.getBoolean(envVisibilitySlot);
                } catch (FrameSlotTypeException e) {
                    throw RInternalError.shouldNotReachHere();
                }
            }
            return false;
        }

        @Specialization(guards = "!isSimple(func, argsAsList)")
        public Object doGeneric(VirtualFrame virtualFrame, RFunction func, RList argsAsList, boolean quote, REnvironment env) {
            CallResult result = doCallGeneric(func, argsAsList.getDataWithoutCopying(), getArgsNames(argsAsList), quote, RArguments.getCall(virtualFrame), env);
            setVisibilityNode.execute(virtualFrame, result.visibility);
            return result.value;
        }

        @TruffleBoundary
        private static CallResult doCallGeneric(RFunction function, Object[] argValues, ArgumentsSignature argsSignature, boolean quote, RCaller call, REnvironment env) {
            RSyntaxNode[] argsConstants = new RSyntaxNode[argValues.length];
            for (int i = 0; i < argValues.length; i++) {
                if (!quote && argValues[i] instanceof RLanguage) {
                    argsConstants[i] = ((RLanguage) argValues[i]).getRep().asRSyntaxNode();
                } else if (!quote && argValues[i] instanceof RSymbol) {
                    RSymbol symbol = (RSymbol) argValues[i];
                    if (symbol.isMissing()) {
                        argsConstants[i] = ConstantNode.create(REmpty.instance);
                    } else {
                        argsConstants[i] = ReadVariableNode.create(((RSymbol) argValues[i]).getName());
                    }
                } else {
                    argsConstants[i] = ConstantNode.create(argValues[i]);
                }
            }
            for (int i = 0; i < argValues.length; i++) {
                ShareObjectNode.share(argValues[i]);
            }
            RLanguage lang = RDataFactory.createLanguage(RASTUtils.createCall(ConstantNode.create(function), true, argsSignature, argsConstants).asRNode());
            try {
                Object resultValue = RContext.getEngine().eval(lang, env, call);
                MaterializedFrame envFrame = env.getFrame();
                FrameSlot envVisibilitySlot = FrameSlotChangeMonitor.findOrAddFrameSlot(envFrame.getFrameDescriptor(), RFrameSlot.Visibility, FrameSlotKind.Boolean);
                boolean resultVisibility = false;
                if (envVisibilitySlot != null) {
                    resultVisibility = envFrame.getBoolean(envVisibilitySlot);
                }
                return new CallResult(resultValue, resultVisibility);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            } finally {
                for (int i = 0; i < argValues.length; i++) {
                    ShareObjectNode.unshare(argValues[i]);
                }
            }
        }

        private static final class CallResult {
            public final Object value;
            public final boolean visibility;

            CallResult(Object value, boolean visibility) {
                this.value = value;
                this.visibility = visibility;
            }
        }
    }
}
