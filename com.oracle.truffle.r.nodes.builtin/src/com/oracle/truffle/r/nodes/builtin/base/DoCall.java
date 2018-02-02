/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.returnIf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;

import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.DoCallNodeGen.DoCallInternalNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctions.Get;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.nodes.function.visibility.GetVisibilityNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.ClosureCache;
import com.oracle.truffle.r.runtime.data.ClosureCache.RNodeClosureCache;
import com.oracle.truffle.r.runtime.data.ClosureCache.SymbolClosureCache;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
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
        @Child private GetNamesAttributeNode getNamesNode;
        @Child private SetVisibilityNode setVisibilityNode;
        private final ValueProfile frameAccessProfile = ValueProfile.createClassProfile();
        private final RNodeClosureCache languagesClosureCache = new RNodeClosureCache();
        private final SymbolClosureCache symbolsClosureCache = new SymbolClosureCache();

        public static DoCallInternal create() {
            return DoCallInternalNodeGen.create();
        }

        public abstract Object execute(VirtualFrame virtualFrame, RFunction func, RList argsAsList, boolean quote, REnvironment env);

        protected FrameDescriptor getFrameDescriptor(REnvironment env) {
            return env.getFrame(frameAccessProfile).getFrameDescriptor();
        }

        /**
         * Because the underlying AST in {@link RExplicitCallNode} may cache frame slots, i.e.
         * expect the {@link FrameDescriptor} to never change, we're caching this AST and also
         * {@link GetVisibilityNode} for each {@link FrameDescriptor} we encounter.
         */
        @Specialization(guards = {"getFrameDescriptor(env) == fd"}, limit = "20")
        public Object doFastPath(VirtualFrame virtualFrame, RFunction func, RList argsAsList, boolean quote, REnvironment env,
                        @Cached("getFrameDescriptor(env)") @SuppressWarnings("unused") FrameDescriptor fd,
                        @Cached("create()") RExplicitCallNode explicitCallNode,
                        @Cached("create()") GetVisibilityNode getVisibilityNode,
                        @Cached("createBinaryProfile()") ConditionProfile quoteProfile,
                        @Cached("create()") BranchProfile containsRSymbolProfile) {
            MaterializedFrame promiseFrame = env.getFrame(frameAccessProfile).materialize();
            RArgsValuesAndNames args = getArguments(promiseFrame, quote, quoteProfile, containsRSymbolProfile, argsAsList);
            RCaller caller = getExplicitCaller(virtualFrame, promiseFrame, func, args);
            MaterializedFrame evalFrame = getEvalFrame(virtualFrame, promiseFrame);

            Object resultValue = explicitCallNode.execute(evalFrame, func, args, caller);
            setVisibility(virtualFrame, getVisibilityNode.execute(evalFrame));
            return resultValue;
        }

        /**
         * Slow-path version avoids the problem by creating {@link RExplicitCallNode} for every call
         * again and again and putting it behind truffle boundary to avoid deoptimization.
         */
        @Specialization(replaces = "doFastPath")
        public Object doSlowPath(VirtualFrame virtualFrame, RFunction func, RList argsAsList, boolean quote, REnvironment env,
                        @Cached("create()") SlowPathExplicitCall slowPathExplicitCall,
                        @Cached("createBinaryProfile()") ConditionProfile quoteProfile,
                        @Cached("create()") BranchProfile containsRSymbolProfile) {
            MaterializedFrame promiseFrame = env.getFrame(frameAccessProfile).materialize();
            RArgsValuesAndNames args = getArguments(promiseFrame, quote, quoteProfile, containsRSymbolProfile, argsAsList);
            RCaller caller = getExplicitCaller(virtualFrame, promiseFrame, func, args);
            MaterializedFrame evalFrame = getEvalFrame(virtualFrame, promiseFrame);

            Object resultValue = slowPathExplicitCall.execute(evalFrame, caller, func, args);
            setVisibility(virtualFrame, getVisibilitySlowPath(evalFrame));
            return resultValue;
        }

        /**
         * The contract is that the function call will be evaluated in the given environment, but at
         * the same time some primitives expect to see {@code do.call(foo, ...)} as the caller, so
         * we create a frame the fakes caller, but otherwise delegates to the frame backing the
         * explicitly given environment.
         */
        private MaterializedFrame getEvalFrame(VirtualFrame virtualFrame, MaterializedFrame envFrame) {
            return VirtualEvalFrame.create(envFrame, RArguments.getFunction(virtualFrame), RArguments.getCall(virtualFrame));
        }

        /**
         * If the call leads to actual call via
         * {@link com.oracle.truffle.r.nodes.function.call.CallRFunctionNode}, which creates new
         * frame and new set of arguments for it, then for this new arguments we explicitly provide
         * a caller that looks like the function was called from the explicitly given environment
         * (it will be its parent call), but at the same time its depth is one above the do.call
         * function that actually invoked it.
         *
         * @see RCaller
         * @see RArguments
         */
        private RCaller getExplicitCaller(VirtualFrame virtualFrame, MaterializedFrame envFrame, RFunction func, RArgsValuesAndNames args) {
            Supplier<RSyntaxElement> callerSyntax = RCallerHelper.createFromArguments(func, args);
            return RCaller.create(RArguments.getDepth(virtualFrame) + 1, RArguments.getCall(envFrame), callerSyntax);
        }

        private RArgsValuesAndNames getArguments(MaterializedFrame promiseFrame, boolean quote, ConditionProfile quoteProfile,
                        BranchProfile containsRSymbolProfile, RList argsAsList) {
            Object[] argValues = argsAsList.getDataCopy();
            if (quoteProfile.profile(!quote)) {
                for (int i = 0; i < argValues.length; i++) {
                    Object arg = argValues[i];
                    if (arg instanceof RSymbol) {
                        containsRSymbolProfile.enter();
                        RSymbol symbol = (RSymbol) arg;
                        if (symbol.getName().isEmpty()) {
                            argValues[i] = REmpty.instance;
                        } else {
                            argValues[i] = createLookupPromise(promiseFrame, symbol.getName());
                        }
                    } else if (arg instanceof RLanguage) {
                        argValues[i] = createLanguagePromise(promiseFrame, (RLanguage) arg);
                    }
                }
            }
            ArgumentsSignature signature = getArgsNames(argsAsList);
            return new RArgsValuesAndNames(argValues, signature);
        }

        @TruffleBoundary
        private RPromise createLanguagePromise(MaterializedFrame promiseFrame, RLanguage arg) {
            Closure closure = languagesClosureCache.getOrCreatePromiseClosure(arg.getRep());
            return RDataFactory.createPromise(PromiseState.Default, closure, promiseFrame);
        }

        @TruffleBoundary
        private RPromise createLookupPromise(MaterializedFrame callerFrame, String name) {
            Closure closure = symbolsClosureCache.getOrCreatePromiseClosure(name);
            return RDataFactory.createPromise(PromiseState.Supplied, closure, callerFrame);
        }

        private void setVisibility(VirtualFrame frame, boolean value) {
            if (setVisibilityNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setVisibilityNode = insert(SetVisibilityNode.create());
            }
            setVisibilityNode.execute(frame, value);
        }

        private ArgumentsSignature getArgsNames(RList argsAsList) {
            if (getNamesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNamesNode = insert(GetNamesAttributeNode.create());
            }
            ArgumentsSignature signature = ArgumentsSignature.fromNamesAttribute(getNamesNode.getNames(argsAsList));
            return signature == null ? ArgumentsSignature.empty(argsAsList.getLength()) : signature;
        }

        @TruffleBoundary
        private static boolean getVisibilitySlowPath(MaterializedFrame envFrame) {
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
    }

    static class SlowPathExplicitCall extends TruffleBoundaryNode {
        @Child private RExplicitCallNode slowPathCallNode;

        public static SlowPathExplicitCall create() {
            return new SlowPathExplicitCall();
        }

        public Object execute(VirtualFrame evalFrame, RCaller caller, RFunction func, RArgsValuesAndNames args) {
            slowPathCallNode = insert(RExplicitCallNode.create());
            return slowPathCallNode.execute(evalFrame, func, args, caller);
        }
    }
}
