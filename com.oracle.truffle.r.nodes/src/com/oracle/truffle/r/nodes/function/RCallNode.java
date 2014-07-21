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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
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

    private abstract static class RootCallNode extends RCallNode {

        @Child protected RNode functionNode;

        public RootCallNode(RNode function) {
            this.functionNode = function;
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
            if (function.isBuiltin()) {
                RootCallTarget callTarget = function.getTarget();
                RBuiltinRootNode root = findBuiltinRootNode(callTarget);
                if (root != null) {
                    // TODO Inline only feasible if it's guaranteed that default values of builtins
                    // have NO side effects?!?!?!
                    UnevaluatedArguments unevaluatedArgs = ArgumentMatcher.matchArgumentsUnevaluated(function, frame, args, getEncapsulatingSourceSection());
                    // TODO Set proper parent <-> child relations for arguments!!
                    return root.inline(unevaluatedArgs);
                }
            }

            // Now we need to distinguish: Do supplied arguments vary between calls?
            boolean hasVarArgsSupplied = args.hasVarArgs();
            if (hasVarArgsSupplied) {
                // Yes, maybe.
                return new DispatchedVarArgsCallNode(function, args);
            } else {
                // Nope! (peeewh)
                MatchedArgumentsNode matchedArgs = ArgumentMatcher.matchArguments(function, frame, args, getEncapsulatingSourceSection());
                return new DispatchedCallNode(function, matchedArgs);
            }
        }
    }

    /**
     * A {@link RCallNode} for calls to fixed {@link RFunction}s with fixed arguments (no varargs)
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

        @Child protected CallArgumentsNode suppliedArgs;
        @Child protected DirectCallNode call;
        @Child protected MatchedArgumentsNode matchedArgs;

        protected final RFunction function;

        DispatchedVarArgsCallNode(RFunction function, CallArgumentsNode suppliedArgs) {
            this.suppliedArgs = suppliedArgs;
            this.function = function;
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction evaluatedFunction) {
            // Needs to be created every time as function (and thus its arguments)
            // may change each call
            MatchedArgumentsNode newMatchedArgs = ArgumentMatcher.matchArguments(function, frame, suppliedArgs, getEncapsulatingSourceSection());
            replaceChild(matchedArgs, newMatchedArgs);
            matchedArgs = newMatchedArgs;

            Object[] argsObject = RArguments.create(function, matchedArgs.executeArray(frame));
            return call.call(frame, argsObject);
        }
    }

    /**
     * {@link RootCallNode} in case there is no fixed {@link RFunction} but an expression which
     * first has to be evaluated
     */
    private static final class GenericCallNode extends RootCallNode {

        @Child protected CallArgumentsNode suppliedArgs;
        @Child protected IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();
        @Child protected MatchedArgumentsNode matchedArgs;

        GenericCallNode(RNode functionNode, CallArgumentsNode suppliedArgs) {
            super(functionNode);
            this.suppliedArgs = suppliedArgs;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            // Needs to be created every time as function (and thus its arguments)
            // may change each call
            MatchedArgumentsNode newMatchedArgs = ArgumentMatcher.matchArguments(function, frame, suppliedArgs, getEncapsulatingSourceSection());
            replaceChild(matchedArgs, newMatchedArgs);
            matchedArgs = newMatchedArgs;

            Object[] argsObject = RArguments.create(function, matchedArgs.executeArray(frame));
            return indirectCall.call(frame, function.getTarget(), argsObject);
        }
    }
}
