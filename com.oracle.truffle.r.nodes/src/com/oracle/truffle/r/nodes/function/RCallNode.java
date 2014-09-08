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
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
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

    public static RCallNode createStaticCall(SourceSection src, String function, CallArgumentsNode arguments) {
        return RCallNode.createCall(src, ReadVariableNode.create(function, RRuntime.TYPE_FUNCTION, false), arguments);
    }

    /**
     * Creates a call to a resolved {@link RBuiltinKind#INTERNAL} that will be used to replace the
     * original call.
     *
     * @param frame The frame to create the inlined builtins in
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
        current.assignSourceSection(src);
        return current.createCacheNode(frame, function);
    }

    /**
     * Creates a modified call in which the first argument if replaced by {@code arg1}. This is, for
     * example, to support {@code HiddenInternalFunctions.MakeLazy}.
     */
    @SlowPath
    public static RCallNode createCloneReplacingFirstArg(RCallNode call, ConstantNode arg1) {
        assert call instanceof UninitializedCallNode;
        UninitializedCallNode callClone = NodeUtil.cloneNode((UninitializedCallNode) call);
        CallArgumentsNode args = callClone.args;
        RNode[] argNodes = args.getArguments();
        argNodes[0].replace(arg1);
        return callClone;
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

        @Child RNode functionNode;

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

        @Child private RootCallNode nextNode;
        @Child private RCallNode currentNode;
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

    public static final class UninitializedCallNode extends RootCallNode {

        @Child private CallArgumentsNode args;
        private final int depth;

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
            this.assignSourceSection(copy.getSourceSection());
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
                cachedNode.assignSourceSection(getSourceSection());
                current.onCreate();
                this.replace(cachedNode);
                return cachedNode;
            } else {
                RootCallNode topMost = (RootCallNode) getTopNode();
                RCallNode generic = args.containsVarArgsSymbol() ? new GenericVarArgsCallNode(topMost.functionNode, args) : new GenericCallNode(topMost.functionNode, args);
                generic.assignSourceSection(getSourceSection());
                generic = topMost.replace(generic);
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

        public CallArgumentsNode getClonedArgs() {
            return NodeUtil.cloneNode(args);
        }

        protected RCallNode createCacheNode(VirtualFrame frame, RFunction function) {
            CallArgumentsNode clonedArgs = getClonedArgs();
            SourceSection callSrc = getSourceSection();
            SourceSection argsSrc = args.getEncapsulatingSourceSection();

            RCallNode callNode = null;
            // Check implementation: If written in Java, handle differently!
            if (function.isBuiltin()) {
                RootCallTarget callTarget = function.getTarget();
                RBuiltinRootNode root = findBuiltinRootNode(callTarget);
                if (root != null) {
                    // We inline the given arguments here, as builtins are executed inside the same
                    // frame as they are called.
                    InlinedArguments inlinedArgs = ArgumentMatcher.matchArgumentsInlined(frame, function, clonedArgs, callSrc, argsSrc);
                    callNode = root.inline(inlinedArgs);
                }
            } else {
                // Now we need to distinguish: Do supplied arguments vary between calls?
                if (clonedArgs.containsVarArgsSymbol()) {
                    // Yes, maybe.
                    callNode = new DispatchedVarArgsCallNode(function, clonedArgs);
                } else {
                    // Nope! (peeewh)
                    MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(frame, function, clonedArgs, callSrc, argsSrc);
                    callNode = new DispatchedCallNode(function, matchedArgs);
                }
            }

            callNode.assignSourceSection(callSrc);
            return callNode;
        }
    }

    /**
     * A {@link RCallNode} for calls to fixed {@link RFunction}s with fixed arguments (no varargs).
     */
    private static class DispatchedCallNode extends RCallNode {

        @Child private DirectCallNode call;
        @CompilationFinal private MatchedArguments matchedArgs;

        private final RFunction function;

        DispatchedCallNode(RFunction function, MatchedArguments matchedArgs) {
            this.matchedArgs = matchedArgs;
            this.function = function;
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction evaluatedFunction) {
            Object[] argsObject = RArguments.create(function, getSourceSection(), matchedArgs.doExecuteArray(frame), matchedArgs.getNames());
            return call.call(frame, argsObject);
        }
    }

    /**
     * A {@link RCallNode} for calls to fixed {@link RFunction}s which have varargs in their
     * {@link FormalArguments}. Varargs have to be matched again every call!
     */
    private static class DispatchedVarArgsCallNode extends RCallNode {

        @Child private CallArgumentsNode suppliedArgs;    // Is not executed!
        @Child private DirectCallNode call;

        private final RFunction function;
        @CompilationFinal private VarArgsSignature varArgsSignature = null;
        @CompilationFinal private MatchedArguments reorderedArgs = null;

        DispatchedVarArgsCallNode(RFunction function, CallArgumentsNode suppliedArgs) {
            this.suppliedArgs = suppliedArgs;
            this.function = function;
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction evaluatedFunction) {
            // As this function takes "..." as argument, it's argument may change every call. Thus
            // we check whether the signature changed - only then rematching is necessary!
            VarArgsSignature newVarArgsSignature = suppliedArgs.createSignature(frame);
            if (reorderedArgs == null || newVarArgsSignature.isNotEqualTo(varArgsSignature)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                UnrolledVariadicArguments argsValuesAndNames = suppliedArgs.executeFlatten(frame);
                // ...to match them against the chosen function's formal arguments
                varArgsSignature = newVarArgsSignature;
                reorderedArgs = ArgumentMatcher.matchArguments(frame, evaluatedFunction, argsValuesAndNames, getSourceSection(), getEncapsulatingSourceSection());
            }

            Object[] argsObject = RArguments.create(function, getSourceSection(), reorderedArgs.doExecuteArray(frame), reorderedArgs.getNames());
            return call.call(frame, argsObject);
        }
    }

    /**
     * {@link RootCallNode} in case there is no fixed {@link RFunction} but an expression which
     * first has to be evaluated.
     */
    private static final class GenericCallNode extends RootCallNode {

        @Child private CallArgumentsNode suppliedArgs;
        @Child private IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();

        @CompilationFinal private RFunction lastFunction = null;
        @CompilationFinal private MatchedArguments reorderedArgs = null;

        GenericCallNode(RNode functionNode, CallArgumentsNode suppliedArgs) {
            super(functionNode);
            this.suppliedArgs = suppliedArgs;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction) {
            // Function may change every call. If it does, rematching is necessary
            if (currentFunction != lastFunction || reorderedArgs == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                UnrolledVariadicArguments argsValuesAndNames = suppliedArgs.executeFlatten(frame);
                // Match them against the resolved function's formal arguments
                lastFunction = currentFunction;
                reorderedArgs = ArgumentMatcher.matchArguments(frame, currentFunction, argsValuesAndNames, getSourceSection(), getEncapsulatingSourceSection());
            }

            Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), reorderedArgs.doExecuteArray(frame), reorderedArgs.getNames());
            return indirectCall.call(frame, currentFunction.getTarget(), argsObject);
        }
    }

    /**
     * {@link RootCallNode} in case there is no fixed {@link RFunction} but an expression which
     * first has to be evaluated, which also take one or more "..." arguments.
     */
    private static final class GenericVarArgsCallNode extends RootCallNode {

        @Child private CallArgumentsNode suppliedArgs;
        @Child private IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();

        @CompilationFinal private RFunction lastFunction = null;
        @CompilationFinal private VarArgsSignature varArgsSignature = null;
        @CompilationFinal private MatchedArguments reorderedArgs = null;

        GenericVarArgsCallNode(RNode functionNode, CallArgumentsNode suppliedArgs) {
            super(functionNode);
            this.suppliedArgs = suppliedArgs;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction) {
            // Function may change every call, plus each takes one or more "...": Check if rematch
            // is necessary...
            VarArgsSignature newVarArgsSignature = suppliedArgs.createSignature(frame);
            if (currentFunction != lastFunction || reorderedArgs == null || newVarArgsSignature.isNotEqualTo(varArgsSignature)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                // ...yes!
                UnrolledVariadicArguments argsValuesAndNames = suppliedArgs.executeFlatten(frame);
                lastFunction = currentFunction;
                varArgsSignature = newVarArgsSignature;
                // Match them against the chosen function's formal arguments
                reorderedArgs = ArgumentMatcher.matchArguments(frame, currentFunction, argsValuesAndNames, getSourceSection(), getEncapsulatingSourceSection());
            }

            Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), reorderedArgs.doExecuteArray(frame), reorderedArgs.getNames());
            return indirectCall.call(frame, currentFunction.getTarget(), argsObject);
        }
    }
}
