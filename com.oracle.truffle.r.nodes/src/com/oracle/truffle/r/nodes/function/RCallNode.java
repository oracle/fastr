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
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
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

/**
 * This class denotes a call site to a function,e.g.:
 *
 * <pre>
 * f(a, b, c)
 * </pre>
 * <p>
 * To avoid repeated work on each call, {@link CachedCallNode} class together with
 * {@link UninitializedCallNode} forms the basis for the polymorphic inline cache (PIC) used to
 * cache argument nodes on the call site of a function call.
 * </p>
 * There are two problems we're facing which are the reason for the caching:
 * <ol>
 * <li>on some call sites the function may change between executions</li>
 * <li>for call sites taking "..." as an argument the actual argument list passed into the function
 * may change each call (note that this is totally independent of the problem which function is to
 * be called)</li>
 * </ol>
 * <p>
 * Problem 1 can be tackled by a the use of an {@link IndirectCallNode} instead of a
 * {@link DirectCallNode} which is not as performant as the latter but has no further disadvantages.
 * But as the function changed its formal parameters changed, too, so a re-match has to be done as
 * well, which involves the creation of nodes and thus must happen on the {@link SlowPath}.<br/>
 * Problem 2 however is not that easy, too: It is solved by reading the values associated with "..."
 * (which are Promises) and wrapping them in newly created {@link RNode}s. These nodes get inserted
 * into the arguments list ({@link CallArgumentsNode#executeFlatten(VirtualFrame)}) - which needs to
 * be matched against the formal parameters again, as theses arguments may carry names as well which
 * may have an impact on argument order. As matching involves node creation, it has to happen on the
 * {@link SlowPath}.
 * </p>
 * To cache those node creations, two interwoven PICs are implemented. The cache is constructed
 * using the following classes:
 *
 * <pre>
 *  U = {@link UninitializedCallNode}: Forms the uninitialized end of the function PIC
 *  D = {@link DispatchedCallNode}: Function fixed, no varargs
 *  G = {@link GenericCallNode}: Function fixed, no varargs (generic case)
 * 
 *  UV = {@link UninitializedCallNode} with varargs,
 *  UVC = {@link UninitializedVarArgsCacheCallNode} with varargs, for varargs cache
 *  DV = {@link DispatchedVarArgsCallNode}: Function fixed, with cached varargs
 *  DGV = {@link DispatchedGenericVarArgsCallNode}: Function fixed, with arbitrary varargs (generic case)
 *  GV = {@link GenericVarArgsCallNode}: Function arbitrary, with arbitrary varargs (generic case)
 * 
 * (RB = {@link RBuiltinNode}: individual functions that are builtins are represented by this node
 * which is not aware of caching)
 * </pre>
 *
 * As the function's identity is the primary id for this cache and as the property
 * "takes varargs as argument" is static for each call site, we effectively end up with two separate
 * cache structures. Some examples of each are depicted below:
 *
 * <pre>
 * non varargs, max depth:
 * |
 * D-D-D-U
 * 
 * no varargs, generic (if max depth is exceeded):
 * |
 * G
 * 
 * varargs:
 * |
 * DV-DV-UV
 *    |
 *    DV
 *    |
 *    UVC
 * 
 * varargs, max varargs depth exceeded:
 * |
 * DV-DGV-UV
 * 
 * varargs, max function depth exceeded:
 * |
 * GV
 * </pre>
 * <p>
 * In the diagrams above every horizontal connection "-" is in fact established by a separate node:
 * {@link CachedCallNode}, which encapsulates the check for functions identity. So the 1. example in
 * fact looks like this:
 *
 * <pre>
 * |
 * C-C-C-U
 * | | |
 * D D D
 * </pre>
 *
 * This is useful as it avoids repetition and allows child nodes to be more concise. It is also
 * needed as there are {@link RCallNode} implementations that are not aware of caching, e.g.
 * {@link RBuiltinNode}, which may be part of the cache. Note that varargs cache nodes (
 * {@link DispatchedVarArgsCallNode}, the nodes connected by "|") handle the check for varargs value
 * identity by itself.
 * </p>
 *
 * As the two problems of changing arguments and changing functions are totally independent as
 * described above, it would be possible to use two totally separate caching infrastructures. The
 * reason for the current choice is the expectation that R users will try to call same functions
 * with the same arguments, and there will very rarely be the case that the same arguments get
 * passed via "..." to the same functions.
 */
public abstract class RCallNode extends RNode {

    private static final int FUNCTION_INLINE_CACHE_SIZE = 4;
    private static final int VARARGS_INLINE_CACHE_SIZE = 4;
    private static final String GENERIC_VARARGS_WARNING = "Warning: RCallNode varargs generic case!";
    private static final String GENERIC_FUNCTION_WARNING = "Warning: RCallNode function generic case!";

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

    public static RCallNode createStaticCall(SourceSection src, RFunction function, CallArgumentsNode arguments) {
        Symbol symbol = Symbol.create(function.getName());
        return RCallNode.createCall(src, BuiltinFunctionVariableNodeFactory.create(function, symbol), arguments);
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

    /**
     * Base class for classes that denote a call site/a call to a function.
     */
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

    /**
     * [C]
     *
     * @see RCallNode
     */
    public static final class CachedCallNode extends RootCallNode {

        @Child private RootCallNode nextNode;
        @Child private RCallNode currentNode;
        private final RFunction cachedFunction;

        public CachedCallNode(RNode function, RCallNode current, RootCallNode next, RFunction cachedFunction) {
            super(function);
            this.currentNode = current;
            this.nextNode = next;
            this.cachedFunction = cachedFunction;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction f) {
            if (cachedFunction == f) {
                return currentNode.execute(frame, cachedFunction);
            }
            return nextNode.execute(frame, f);
        }

        @Override
        public int executeInteger(VirtualFrame frame, RFunction f) throws UnexpectedResultException {
            if (cachedFunction == f) {
                return currentNode.executeInteger(frame, cachedFunction);
            }
            return nextNode.executeInteger(frame, f);
        }

        @Override
        public double executeDouble(VirtualFrame frame, RFunction f) throws UnexpectedResultException {
            if (cachedFunction == f) {
                return currentNode.executeDouble(frame, cachedFunction);
            }
            return nextNode.executeDouble(frame, f);
        }
    }

    /**
     * [U]/[UV]
     *
     * @see RCallNode
     */
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

        private UninitializedCallNode(UninitializedCallNode org, int depth) {
            super(null);
            this.args = org.args;
            this.depth = depth;
            this.assignSourceSection(org.getSourceSection());
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(frame, function).execute(frame, function);
        }

        @SlowPath
        private RCallNode specialize(VirtualFrame frame, RFunction function) {
            CompilerAsserts.neverPartOfCompilation();

            if (depth < FUNCTION_INLINE_CACHE_SIZE) {
                final RCallNode current = createCacheNode(frame, function);
                final RootCallNode cachedNode = new CachedCallNode(this.functionNode, current, new UninitializedCallNode(this), function);
                current.onCreate();
                this.replace(cachedNode);
                return cachedNode;
            } else {
                RootCallNode topMost = (RootCallNode) getTopNode();
                // 2 possible cases: G (Multiple functions, no varargs) or GV (Multiple function
                // arbitrary varargs)
                RCallNode generic = args.containsVarArgsSymbol() ? new GenericVarArgsCallNode(topMost.functionNode, args) : new GenericCallNode(topMost.functionNode, args);
                generic = topMost.replace(generic, GENERIC_FUNCTION_WARNING);
                generic.onCreate();
                return generic;
            }
        }

        @SlowPath
        protected RCallNode createCacheNode(VirtualFrame frame, RFunction function) {
            CompilerAsserts.neverPartOfCompilation();

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
                    InlinedArguments inlinedArgs = ArgumentMatcher.matchArgumentsInlined(frame, function, clonedArgs, callSrc, argsSrc, !root.matchArguments());
                    callNode = root.inline(inlinedArgs);
                }
            } else {
                // Now we need to distinguish: Do supplied arguments vary between calls?
                if (clonedArgs.containsVarArgsSymbol()) {
                    // Yes, maybe.
                    VarArgsCacheCallNode nextNode = new UninitializedVarArgsCacheCallNode(clonedArgs);
                    VarArgsSignature varArgsSignature = clonedArgs.createSignature(frame);
                    callNode = DispatchedVarArgsCallNode.create(frame, clonedArgs, nextNode, callSrc, function, varArgsSignature, true);
                } else {
                    // Nope! (peeewh)
                    MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(frame, function, clonedArgs, callSrc, argsSrc);
                    callNode = new DispatchedCallNode(function, matchedArgs);
                }
            }

            callNode.assignSourceSection(callSrc);
            return callNode;
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
    }

    /**
     * [D] A {@link RCallNode} for calls to fixed {@link RFunction}s with fixed arguments (no
     * varargs).
     *
     * @see RCallNode
     */
    private static final class DispatchedCallNode extends RCallNode {

        @Child private DirectCallNode call;
        private final MatchedArguments matchedArgs;

        private final RFunction cachedFunction;

        DispatchedCallNode(RFunction function, MatchedArguments matchedArgs) {
            this.matchedArgs = matchedArgs;
            this.cachedFunction = function;
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction) {
            assert cachedFunction == currentFunction;

            Object[] argsObject = RArguments.create(cachedFunction, getSourceSection(), matchedArgs.doExecuteArray(frame), matchedArgs.getNames());
            return call.call(frame, argsObject);
        }
    }

    /**
     * [G] A {@link RCallNode} in case there is no fixed {@link RFunction} (and no varargs).
     *
     * @see RCallNode
     */
    private static final class GenericCallNode extends RootCallNode {

        @Child private IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();
        @Child private CallArgumentsNode args;

        private RFunction lastFunction = null;
        private MatchedArguments lastMatchedArgs = null;

        GenericCallNode(RNode functionNode, CallArgumentsNode args) {
            super(functionNode);
            this.args = args;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction) {
            if (lastFunction == currentFunction && lastMatchedArgs != null) {
                // Same function: No re-match needed
                Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), lastMatchedArgs.doExecuteArray(frame), lastMatchedArgs.getNames());
                return indirectCall.call(frame, currentFunction.getTarget(), argsObject);
            }

            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(frame, currentFunction, args, getSourceSection(), args.getEncapsulatingSourceSection());
            this.lastMatchedArgs = matchedArgs;
            this.lastFunction = currentFunction;

            Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), matchedArgs.doExecuteArray(frame), matchedArgs.getNames());
            return indirectCall.call(frame, currentFunction.getTarget(), argsObject);
        }
    }

    /*
     * Varargs cache classes follow
     */

    /**
     * Base class for the varargs cache [UVC] and [DV]
     *
     * @see RCallNode
     */
    private abstract static class VarArgsCacheCallNode extends RCallNode {

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            return execute(frame, function, null);
        }

        protected abstract Object execute(VirtualFrame frame, RFunction function, VarArgsSignature varArgsSignature);
    }

    /**
     * [UVC] The uninitialized end of the varargs cache
     *
     * @see RCallNode
     */
    public static final class UninitializedVarArgsCacheCallNode extends VarArgsCacheCallNode {
        @Child private CallArgumentsNode args;
        private int depth = 1;  // varargs cached is started with a [DV] DispatchedVarArgsCallNode

        public UninitializedVarArgsCacheCallNode(CallArgumentsNode args) {
            this.args = args;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function, VarArgsSignature varArgsSignature) {
            return specializeAndExecute(frame, function, varArgsSignature);
        }

        private Object specializeAndExecute(VirtualFrame frame, RFunction function, VarArgsSignature varArgsSignature) {
            CompilerAsserts.neverPartOfCompilation();

            if (depth < VARARGS_INLINE_CACHE_SIZE) {
                // Extend cache
                this.depth += 1;
                CallArgumentsNode clonedArgs = NodeUtil.cloneNode(args);
                DispatchedVarArgsCallNode newCallNode = DispatchedVarArgsCallNode.create(frame, clonedArgs, this, getSourceSection(), function, varArgsSignature, false);
                return replace(newCallNode).execute(frame, function, varArgsSignature);
            } else {
                // Add stable generic case
                DispatchedGenericVarArgsCallNode dgv = new DispatchedGenericVarArgsCallNode(function, args);
                RCallNode topMostVarargsCache = getTopMost();
                return topMostVarargsCache.replace(dgv, GENERIC_VARARGS_WARNING).execute(frame, function);
            }
        }

        private RCallNode getTopMost() {
            Node parent = this;
            for (int i = 0; i < depth; i--) {
                parent = parent.getParent();
            }
            return (RCallNode) parent;
        }
    }

    /**
     * [DV] A {@link RCallNode} for calls to cached {@link RFunction}s with cached varargs.
     *
     * @see RCallNode
     */
    private static final class DispatchedVarArgsCallNode extends VarArgsCacheCallNode {
        @Child private DirectCallNode call;
        @Child private CallArgumentsNode args;
        @Child private VarArgsCacheCallNode next;

        private final RFunction cachedFunction;
        private final VarArgsSignature cachedSignature;
        private final MatchedArguments matchedArgs;

        /**
         * Whether this [DV] node is the root of the varargs sub-cache (cmp. {@link RCallNode})
         */
        private final boolean isVarArgsRoot;

        /**
         * Used to profile on constant {@link #isVarArgsRoot}
         */
        private final ConditionProfile isRootProfile = ConditionProfile.createBinaryProfile();

        protected DispatchedVarArgsCallNode(CallArgumentsNode args, VarArgsCacheCallNode next, RFunction function, VarArgsSignature varArgsSignature, MatchedArguments matchedArgs,
                        boolean isVarArgsRoot) {
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
            this.args = args;
            this.next = next;
            this.cachedFunction = function;
            this.cachedSignature = varArgsSignature;
            this.matchedArgs = matchedArgs;
            this.isVarArgsRoot = isVarArgsRoot;
        }

        protected static DispatchedVarArgsCallNode create(VirtualFrame frame, CallArgumentsNode args, VarArgsCacheCallNode next, SourceSection callSrc, RFunction function,
                        VarArgsSignature varArgsSignature, boolean isVarArgsRoot) {
            UnrolledVariadicArguments unrolledArguments = args.executeFlatten(frame);
            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(frame, function, unrolledArguments, callSrc, args.getEncapsulatingSourceSection());
            return new DispatchedVarArgsCallNode(args, next, function, varArgsSignature, matchedArgs, isVarArgsRoot);
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction, VarArgsSignature varArgsSignature) {
            assert currentFunction == cachedFunction;

            // If this is the root of the varargs sub-cache: The signature needs to be created
            // once
            VarArgsSignature currentSignature;
            if (isRootProfile.profile(isVarArgsRoot)) {
                currentSignature = args.createSignature(frame);
            } else {
                currentSignature = varArgsSignature;
            }

            // If the signature does not match: delegate to next node!
            if (cachedSignature.isNotEqualTo(currentSignature)) {
                return next.execute(frame, currentFunction, currentSignature);
            }

            // Our cached function and matched arguments do match, simply execute!
            Object[] argsObject = RArguments.create(cachedFunction, getSourceSection(), matchedArgs.doExecuteArray(frame), matchedArgs.getNames());
            return call.call(frame, argsObject);
        }
    }

    /**
     * [DGV] {@link RCallNode} in case there is a cached {@link RFunction} with cached varargs that
     * exceeded the cache size {@link RCallNode#VARARGS_INLINE_CACHE_SIZE}.}
     *
     * @see RCallNode
     */
    private static final class DispatchedGenericVarArgsCallNode extends RCallNode {

        @Child private DirectCallNode call;
        @Child private CallArgumentsNode suppliedArgs;

        private final RFunction function;

        DispatchedGenericVarArgsCallNode(RFunction function, CallArgumentsNode suppliedArgs) {
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
            this.suppliedArgs = suppliedArgs;
            this.function = function;
        }

        @Override
        @SlowPath
        public Object execute(VirtualFrame frame, RFunction currentFunction) {
            CompilerAsserts.neverPartOfCompilation();
            assert function == currentFunction;

            // Arguments may change every call: Flatt'n'Match on SlowPath! :-/
            UnrolledVariadicArguments argsValuesAndNames = suppliedArgs.executeFlatten(frame);
            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(frame, currentFunction, argsValuesAndNames, getSourceSection(), getEncapsulatingSourceSection());

            Object[] argsObject = RArguments.create(function, getSourceSection(), matchedArgs.doExecuteArray(frame), matchedArgs.getNames());
            return call.call(frame, argsObject);
        }
    }

    /**
     * [GV] {@link RootCallNode} in case there is no fixed {@link RFunction} AND varargs...
     *
     * @see RCallNode
     */
    private static final class GenericVarArgsCallNode extends RootCallNode {

        @Child private CallArgumentsNode suppliedArgs;
        @Child private IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();

        GenericVarArgsCallNode(RNode functionNode, CallArgumentsNode suppliedArgs) {
            super(functionNode);
            this.suppliedArgs = suppliedArgs;
        }

        @Override
        @SlowPath
        public Object execute(VirtualFrame frame, RFunction currentFunction) {
            CompilerAsserts.neverPartOfCompilation();

            // Function and arguments may change every call: Flatt'n'Match on SlowPath! :-/
            UnrolledVariadicArguments argsValuesAndNames = suppliedArgs.executeFlatten(frame);
            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(frame, currentFunction, argsValuesAndNames, getSourceSection(), getEncapsulatingSourceSection());

            Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), matchedArgs.doExecuteArray(frame), matchedArgs.getNames());
            return indirectCall.call(frame, currentFunction.getTarget(), argsObject);
        }
    }
}
