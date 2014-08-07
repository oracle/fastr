/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ReadVariableNode.BuiltinFunctionVariableNode;
import com.oracle.truffle.r.nodes.access.ReadVariableNodeFactory.BuiltinFunctionVariableNodeFactory;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class RCallNode extends RNode {

    private static final int INLINE_CACHE_SIZE = 4;

    protected RCallNode() {
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new AssertionError();
    }

    protected void onCreate() {
        // intended for subclasses to be used to implement special inlining semantics.
    }

    public abstract Object execute(VirtualFrame frame, RFunction function);

    public int executeInteger(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectInteger(execute(frame, function));
    }

    public double executeDouble(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectDouble(execute(frame, function));
    }

    public static RCallNode createStaticCall(String function, CallArgumentsNode arguments) {
        return RCallNode.createCall(ReadVariableNode.create(function, RRuntime.TYPE_FUNCTION, false), arguments);
    }

    public static RCallNode createStaticCall(SourceSection src, String function, CallArgumentsNode arguments) {
        RCallNode cn = createStaticCall(function, arguments);
        cn.assignSourceSection(src);
        return cn;
    }

    /**
     * Creates a call to a resolved {@link RBuiltinKind#INTERNAL} that will be used to replace the
     * original call.
     *
     * @param frame TODO
     * @param src source section to use (from original call)
     * @param internalCallArg the {@link UninitializedCallNode} corresponding to the argument to the
     *            {code .Internal}.
     * @param function the resolved {@link RFunction}.
     * @param symbol The name of the function
     */
    public static RCallNode createInternalCall(VirtualFrame frame, SourceSection src, RCallNode internalCallArg, RFunction function, Symbol symbol) {
        BuiltinFunctionVariableNode functionNode = BuiltinFunctionVariableNodeFactory.create(function, symbol);
        assert internalCallArg instanceof UninitializedCallNode;
        UninitializedCallNode current = new UninitializedCallNode(functionNode, ((UninitializedCallNode) internalCallArg).args);
        RCallNode result = current.createCacheNode(frame, function);
        result.assignSourceSection(src);
        return result;
    }

    /**
     * Creates a modified call in which the first argument if replaced by {@code arg1}. This is, for
     * example, to support {@code HiddenInternalFunctions.MakeLazy}.
     */
    public static RCallNode createCloneReplacingFirstArg(RCallNode call, ConstantNode arg1) {
        assert call instanceof UninitializedCallNode;
        UninitializedCallNode callClone = NodeUtil.cloneNode((UninitializedCallNode) call);
        CallArgumentsNode args = callClone.args;
        RNode[] argNodes = args.getArguments();
        argNodes[0].replace(arg1);
        return callClone;
    }

    public static RCallNode createCall(RNode function, CallArgumentsNode arguments) {
        return new UninitializedCallNode(function, arguments);
    }

    public static RCallNode createCall(SourceSection src, RNode function, CallArgumentsNode arguments) {
        RCallNode cn = new UninitializedCallNode(function, arguments);
        cn.assignSourceSection(src);
        return cn;
    }

    private static RBuiltinRootNode findBuiltinRootNode(RootCallTarget callTarget) {
        RootNode root = callTarget.getRootNode();
        if (root instanceof RBuiltinRootNode) {
            return (RBuiltinRootNode) root;
        }
        return null;
    }

    protected <T extends Node> T replaceChild(T oldChild, T newChild) {
        if (oldChild == null) {
            return insert(newChild);
        } else {
            return oldChild.replace(newChild);
        }
    }

    public abstract static class RootCallNode extends RCallNode {

        @Child protected RNode functionNode;

        public RootCallNode(RNode function) {
            this.functionNode = function;
        }

        public RNode getFunctionNode() {
            return functionNode;
        }

        private RFunction executeFunctionNode(VirtualFrame frame) {
            try {
                return functionNode.executeFunction(frame);
            } catch (UnexpectedResultException e) {
                // TODO unsupported yet
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public final Object execute(VirtualFrame frame) {
            return execute(frame, executeFunctionNode(frame));
        }

        @Override
        public final int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
            return executeInteger(frame, executeFunctionNode(frame));
        }

        @Override
        public final double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
            return executeDouble(frame, executeFunctionNode(frame));
        }

    }

    public static final class CachedCallNode extends RootCallNode {

        @Child protected RootCallNode nextNode;
        @Child protected RCallNode currentNode;
        private final RFunction function;
        private final CallTarget target;

        public CachedCallNode(RNode function, RCallNode current, RootCallNode next, RFunction cachedFunction) {
            super(function);
            this.currentNode = current;
            this.nextNode = next;
            this.function = cachedFunction;
            this.target = cachedFunction.getTarget();
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction f) {
            if (this.function == f) {
                return currentNode.execute(frame, this.function);
            }
            return nextNode.execute(frame, f);
        }

        @Override
        public int executeInteger(VirtualFrame frame, RFunction f) throws UnexpectedResultException {
            if (this.function == f) {
                return currentNode.executeInteger(frame, this.function);
            }
            return nextNode.executeInteger(frame, f);
        }

        @Override
        public double executeDouble(VirtualFrame frame, RFunction f) throws UnexpectedResultException {
            if (this.function == f) {
                return currentNode.executeDouble(frame, this.function);
            }
            return nextNode.executeDouble(frame, f);
        }

        CallTarget getCallTarget() {
            return target;
        }
    }

    private static final class UninitializedCallNode extends RootCallNode {

        @Child protected CallArgumentsNode args;
        protected final int depth;

        protected UninitializedCallNode(RNode function, CallArgumentsNode args) {
            this(function, args, 0);
        }

        private UninitializedCallNode(RNode function, CallArgumentsNode args, int depth) {
            super(function);
            this.args = args;
            this.depth = depth;
        }

        protected UninitializedCallNode(UninitializedCallNode copy) {
            this(copy, copy.depth + 1);
        }

        private UninitializedCallNode(UninitializedCallNode copy, int depth) {
            super(null);
            this.args = copy.args;
            this.depth = depth;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(frame, function).execute(frame, function);
        }

        private RCallNode specialize(VirtualFrame frame, RFunction function) {
            CompilerAsserts.neverPartOfCompilation();

            if (depth < INLINE_CACHE_SIZE) {
                final RCallNode current = createCacheNode(frame, function);
                final RootCallNode cachedNode = new CachedCallNode(this.functionNode, current, new UninitializedCallNode(this), function);
                current.onCreate();
                this.replace(cachedNode);
                return cachedNode;
            } else {
                RootCallNode topMost = (RootCallNode) getTopNode();
                GenericCallNode generic = topMost.replace(new GenericCallNode(topMost.functionNode, args));
                generic.onCreate();
                return generic;
            }
        }

        protected Node getTopNode() {
            Node parentNode = this;
            for (int i = 0; i < depth; i++) {
                parentNode = parentNode.getParent();
            }
            return parentNode;
        }

        protected RCallNode createCacheNode(VirtualFrame frame, RFunction function) {
            // Check implementation: If written in Java, handle differently!
            if (function.isBuiltin()) {
                RootCallTarget callTarget = function.getTarget();
                RBuiltinRootNode root = findBuiltinRootNode(callTarget);
                if (root != null) {
                    // We inline the given arguments here, as builtins are executed inside the same
                    // frame as they are called.
                    InlinedArguments inlinedArgs = ArgumentMatcher.matchArgumentsInlined(frame, function, args, getEncapsulatingSourceSection());
                    // TODO Set proper parent <-> child relations for arguments!!
                    return root.inline(inlinedArgs);
                }
            }

            // Now we need to distinguish: Do supplied arguments vary between calls?
            boolean hasVarArgsInvolved = args.containsVarArgsSymbol() || ((RRootNode) function.getTarget().getRootNode()).getFormalArguments().hasVarArgs();
            if (hasVarArgsInvolved) {
                // Yes, maybe.
                return new DispatchedVarArgsCallNode(function, args);
            } else {
                // Nope! (peeewh)
                MatchedArgumentsNode matchedArgs = ArgumentMatcher.matchArguments(frame, function, args, getEncapsulatingSourceSection());
                return new DispatchedCallNode(function, matchedArgs);
            }
        }
    }

    /**
     * A {@link RCallNode} for calls to fixed {@link RFunction}s with fixed arguments (no varargs).
     */
    private static class DispatchedCallNode extends RCallNode {

        @Child protected MatchedArgumentsNode matchedArgs;
        @Child protected DirectCallNode call;

        protected final RFunction function;

        DispatchedCallNode(RFunction function, MatchedArgumentsNode matchedArgs) {
            this.matchedArgs = matchedArgs;
            this.function = function;
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction evaluatedFunction) {
            Object[] argsObject = RArguments.create(function, matchedArgs.executeArray(frame));
            return call.call(frame, argsObject);
        }
    }

    /**
     * A {@link RCallNode} for calls to fixed {@link RFunction}s which have varargs in their
     * {@link FormalArguments}. Varargs have to be matched again every call!
     */
    private static class DispatchedVarArgsCallNode extends RCallNode {

        @Child protected CallArgumentsNode suppliedArgs;    // Is not executed!
        @Child protected DirectCallNode call;
        @Child protected MatchedArgumentsNode matchedArgs;

        protected final RFunction function;

        /**
         * Remembers last function and last arguments rearrange signature.
         */
        private final ArgumentMatcher matcher;

        /**
         * Used to speculate on non-changing argument order.
         */
        private final BranchProfile argsChangedProfile = new BranchProfile();

        DispatchedVarArgsCallNode(RFunction function, CallArgumentsNode suppliedArgs) {
            this.suppliedArgs = suppliedArgs;
            this.function = function;
            this.matcher = new ArgumentMatcher();
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction evaluatedFunction) {
            // Needs to be created every time as function (and thus its arguments)
            // may change each call
            if (matchedArgs == null || matcher.argsNeedRematch(frame, function, suppliedArgs, this)) {
                argsChangedProfile.enter();

                // Create new MatchedArgumentsNode
                MatchedArgumentsNode newMatchedArgs = ArgumentMatcher.matchArguments(frame, function, suppliedArgs, getEncapsulatingSourceSection());
                replaceChild(matchedArgs, newMatchedArgs);
                matchedArgs = newMatchedArgs;
            }

            Object[] argsObject = RArguments.create(function, matchedArgs.executeArray(frame));
            return call.call(frame, argsObject);
        }
    }

    /**
     * {@link RootCallNode} in case there is no fixed {@link RFunction} but an expression which
     * first has to be evaluated.
     */
    private static final class GenericCallNode extends RootCallNode {

        @Child protected CallArgumentsNode suppliedArgs;
        @Child protected IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();
        @Child protected MatchedArgumentsNode matchedArgs;

        /**
         * Stores the function and arguments used for the last call and checks whether
         * {@link ArgumentMatcher#argsNeedRematch(VirtualFrame, RFunction, CallArgumentsNode, RNode)}
         * .
         */
        private final ArgumentMatcher matcher;

        /**
         * Used to speculate on non-changing argument order.
         */
        private final BranchProfile argsOrFunctionChangedProfile = new BranchProfile();

        GenericCallNode(RNode functionNode, CallArgumentsNode suppliedArgs) {
            super(functionNode);
            this.suppliedArgs = suppliedArgs;
            this.matcher = new ArgumentMatcher();
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            // Needs to be created every time as function (and thus its arguments)
            // may change each call
            if (matchedArgs == null || matcher.argsNeedRematch(frame, function, suppliedArgs, this)) {
                argsOrFunctionChangedProfile.enter();

                // Create new MatchedArgumentsNode
                MatchedArgumentsNode newMatchedArgs = ArgumentMatcher.matchArguments(frame, function, suppliedArgs, getEncapsulatingSourceSection());
                replaceChild(matchedArgs, newMatchedArgs);
                matchedArgs = newMatchedArgs;
            }

            Object[] argsObject = RArguments.create(function, matchedArgs.executeArray(frame));
            return indirectCall.call(frame, function.getTarget(), argsObject);
        }
    }
}
