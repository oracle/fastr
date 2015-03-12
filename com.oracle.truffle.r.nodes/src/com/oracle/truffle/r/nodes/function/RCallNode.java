/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.MatchedArguments.MatchedArgumentsNode;
import com.oracle.truffle.r.nodes.runtime.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.Func;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

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
 * well, which involves the creation of nodes and thus must happen on the {@link TruffleBoundary}.<br/>
 * Problem 2 is not that easy, too: It is solved by reading the values associated with "..." (which
 * are Promises) and wrapping them in newly created {@link RNode}s. These nodes get inserted into
 * the arguments list ({@link CallArgumentsNode#executeFlatten(VirtualFrame)}) - which needs to be
 * be matched against the formal parameters again, as theses arguments may carry names as well which
 * may have an impact on argument order. As matching involves node creation, it has to happen on the
 * {@link TruffleBoundary}.
 * </p>
 * To avoid repeated node creations as much as possible by caching two interwoven PICs are
 * implemented. The caches are constructed using the following classes:
 *
 * <pre>
 *  U = {@link UninitializedCallNode}: Forms the uninitialized end of the function PIC
 *  D = {@link DispatchedCallNode}: Function fixed, no varargs
 *  G = {@link GenericCallNode}: Function arbitrary, no varargs (generic case)
 * 
 *  UV = {@link UninitializedCallNode} with varargs,
 *  UVC = {@link UninitializedVarArgsCacheCallNode} with varargs, for varargs cache
 *  DV = {@link DispatchedVarArgsCallNode}: Function fixed, with cached varargs
 *  DGV = {@link DispatchedGenericVarArgsCallNode}: Function fixed, with arbitrary varargs (generic case)
 *  GV = {@link GenericVarArgsCallNode}: Function arbitrary, with arbitrary varargs (generic case)
 * 
 * (RB = {@link RBuiltinNode}: individual functions that are builtins are represented by this node
 * which is not aware of caching). Due to {@link CachedCallNode} (see below) this is transparent to
 * the cache and just behaves like a D/DGV)
 * </pre>
 *
 * As the property "takes varargs as argument" is static for each call site, we effectively end up
 * with two separate cache structures. Some examples of each are depicted below:
 *
 * <pre>
 * non varargs, max depth:
 * |
 * D-D-D-U
 * 
 * no varargs, generic (if max depth is exceeded):
 * |
 * D-D-D-D-G
 * 
 * varargs:
 * |
 * DV-DV-UV         <- function call target identity level cache
 *    |
 *    DV
 *    |
 *    UVC           <- varargs signature level cache
 * 
 * varargs, max varargs depth exceeded:
 * |
 * DV-DV-UV
 *    |
 *    DV
 *    |
 *    DV
 *    |
 *    DV
 *    |
 *    DGV
 * 
 * varargs, max function depth exceeded:
 * |
 * DV-DV-DV-DV-GV
 * </pre>
 * <p>
 * In the diagrams above every horizontal connection "-" is in fact established by a separate node:
 * {@link CachedCallNode}, which encapsulates the check for function call target identity. So the 1.
 * example in fact looks like this:
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

    protected RCallNode() {
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new AssertionError();
    }

    public abstract Object execute(VirtualFrame frame, RFunction function);

    public abstract RNode getFunctionNode();

    public abstract CallArgumentsNode getArgumentsNode();

    /**
     * A temporary hack to allow promises in {@link PromiseNode.VarArgNode} instances to be reset.
     * Overridden in the appropriate subclasses.
     */
    public MatchedArgumentsNode getMatchedArgumentsNode() {
        return null;
    }

    @Override
    public boolean isSyntax() {
        return true;
    }

    @Override
    public void deparse(State state) {
        Object fname = RASTUtils.findFunctionName(this, false);
        if (fname instanceof RSymbol) {
            String sfname = ((RSymbol) fname).getName();
            if (sfname.equals(":::") || sfname.equals("::")) {
                // special infix, could be a:::b() or a:::b
                RNode fn = getFunctionNode().unwrap();
                RCallNode colonCall;
                RNode[] argValues;
                if (fn instanceof RCallNode) {
                    colonCall = (RCallNode) fn;
                    argValues = colonCall.getArgumentsNode().getArguments();
                } else {
                    colonCall = this;
                    argValues = getArgumentsNode().getArguments();
                }
                argValues[0].deparse(state);
                state.append(sfname);
                argValues[1].deparse(state);
                if (fn instanceof RCallNode) {
                    getArgumentsNode().deparse(state);
                }
                return;
            }
        }
        Func func = RASTDeparse.isInfixOperator(fname);
        if (func != null) {
            RASTDeparse.deparseInfixOperator(state, this, func);
        } else {
            getFunctionNode().deparse(state);
            getArgumentsNode().deparse(state);
        }
    }

    @Override
    public RNode substitute(REnvironment env) {
        RNode functionSub = getFunctionNode().substitute(env);
        CallArgumentsNode argsSub = (CallArgumentsNode) getArgumentsNode().substitute(env);
        // TODO check type of functionSub
        return RASTUtils.createCall(functionSub, argsSub);
    }

    public int executeInteger(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        return RTypesGen.expectInteger(execute(frame, function));
    }

    public double executeDouble(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        return RTypesGen.expectDouble(execute(frame, function));
    }

    public static RCallNode createStaticCall(SourceSection src, String function, CallArgumentsNode arguments, ASTNode parserNode) {
        return RCallNode.createCall(src, ReadVariableNode.createFunctionLookup(function, true), arguments, parserNode);
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
     * @param name The name of the function
     */
    public static RCallNode createInternalCall(VirtualFrame frame, SourceSection src, RCallNode internalCallArg, RFunction function, String name) {
        CompilerDirectives.transferToInterpreter();
        assert internalCallArg instanceof UninitializedCallNode;
        return UninitializedCallNode.createCacheNode(frame, function, ((UninitializedCallNode) internalCallArg).args, src);
    }

    /**
     * Creates a modified call in which the first N arguments are replaced by
     * {@code replacementArgs}. This is, for example, to support
     * {@code HiddenInternalFunctions.MakeLazy}, and condition handling.
     */
    @TruffleBoundary
    public static RCallNode createCloneReplacingArgs(RCallNode call, RNode... replacementArgs) {
        assert call instanceof UninitializedCallNode;
        UninitializedCallNode callClone = NodeUtil.cloneNode((UninitializedCallNode) call);
        CallArgumentsNode args = callClone.args;
        RNode[] argNodes = args.getArguments();
        for (int i = 0; i < replacementArgs.length; i++) {
            argNodes[i].replace(replacementArgs[i]);
        }
        return callClone;
    }

    public static RCallNode createCall(SourceSection src, RNode function, CallArgumentsNode arguments, ASTNode parserNode) {
        RCallNode cn = new UninitializedCallNode(function, arguments, parserNode);
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

    protected RCallNode getParentCallNode() {
        RNode parent = (RNode) unwrapParent();
        if (!(parent instanceof RCallNode)) {
            throw RInternalError.shouldNotReachHere();
        }
        return (RCallNode) parent;
    }

    /**
     * Base class for classes that are on the top of the cache hierarchy.
     *
     * @see LeafCallNode
     */
    public abstract static class RootCallNode extends RCallNode {

        @Child protected RNode functionNode;
        @Child protected CallArgumentsNode args;
        @Child private PromiseHelperNode promiseHelper;
        private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile errorProfile = BranchProfile.create();

        public RootCallNode(RNode function, CallArgumentsNode args) {
            this.functionNode = function;
            this.args = args;
        }

        private RFunction executeFunctionNode(VirtualFrame frame) {
            /**
             * Expressions such as "pkg:::f" can result in (delayedAssign) promises that must be
             * explicitly resolved.
             */
            Object value = functionNode.execute(frame);
            if (isPromiseProfile.profile(value instanceof RPromise)) {
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                }
                value = promiseHelper.evaluate(frame, (RPromise) value);
            }
            if (value instanceof RFunction) {
                return (RFunction) value;
            } else {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.APPLY_NON_FUNCTION);
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

        @Override
        public RNode getFunctionNode() {
            if (functionNode != null) {
                // Only the very 1. RootCallNode on the top level contains the one-and-only function
                // node
                return functionNode;
            }
            return getParentCallNode().getFunctionNode();
        }

        @Override
        public CallArgumentsNode getArgumentsNode() {
            return args;
        }
    }

    /**
     * This is the counterpart of {@link RootCallNode}: While that is the base class for all
     * top-level nodes (C, G, U and GV), this is the base class for all nodes not on the top level
     * of the PIC (and thus don't need all information, but can rely on their parents to have them).
     *
     * @see RootCallNode
     */
    public abstract static class LeafCallNode extends RCallNode {

        @Override
        public RNode getFunctionNode() {
            return getParentCallNode().getFunctionNode();
        }

        @Override
        public final CallArgumentsNode getArgumentsNode() {
            return getParentCallNode().getArgumentsNode();
        }
    }

    /**
     * [C] Extracts the check for function call target identity away from the individual cache
     * nodes.
     *
     * @see RCallNode
     */
    public static final class CachedCallNode extends RootCallNode {

        @Child private RootCallNode nextNode;
        @Child private RCallNode currentNode;
        private final CallTarget cachedCallTarget;

        public CachedCallNode(RNode function, RCallNode current, RootCallNode next, RFunction cachedFunction) {
            super(function, null);  // Relies on the getArguments redirect below
            this.currentNode = current;
            this.nextNode = next;
            this.cachedCallTarget = cachedFunction.getTarget();
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction f) {
            if (cachedCallTarget == f.getTarget()) {
                return currentNode.execute(frame, f);
            }
            return nextNode.execute(frame, f);
        }

        @Override
        public int executeInteger(VirtualFrame frame, RFunction f) throws UnexpectedResultException {
            if (cachedCallTarget == f.getTarget()) {
                return currentNode.executeInteger(frame, f);
            }
            return nextNode.executeInteger(frame, f);
        }

        @Override
        public double executeDouble(VirtualFrame frame, RFunction f) throws UnexpectedResultException {
            if (cachedCallTarget == f.getTarget()) {
                return currentNode.executeDouble(frame, f);
            }
            return nextNode.executeDouble(frame, f);
        }

        @Override
        public CallArgumentsNode getArgumentsNode() {
            // This relies on the fact that the top level of the cache consists only of Cs
            // (maintained by UninitializedCallNode.specialize), where it's head is one of U/UV or
            // G/GV - which are all RootCallNodes, and as such hold their own CallArgumentsNode.
            return nextNode.getArgumentsNode();
        }

        @Override
        public MatchedArgumentsNode getMatchedArgumentsNode() {
            return currentNode.getMatchedArgumentsNode();
        }
    }

    /**
     * [U]/[UV] Forms the uninitialized end of the function PIC.
     *
     * @see RCallNode
     */
    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    public static final class UninitializedCallNode extends RootCallNode {

        private int depth;
        private final ASTNode parserNode;

        protected UninitializedCallNode(RNode function, CallArgumentsNode args, ASTNode parserNode) {
            super(function, args);
            this.depth = 0;
            this.parserNode = parserNode;
        }

        protected UninitializedCallNode(UninitializedCallNode copy) {
            super(null, copy.args);
            this.parserNode = copy.parserNode;
            this.depth = copy.depth + 1;
            this.assignSourceSection(copy.getSourceSection());
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize(frame, function).execute(frame, function);
        }

        private RCallNode specialize(VirtualFrame frame, RFunction function) {
            RootCallNode next = createNextNode();
            RCallNode current = createCacheNode(frame, function, next instanceof UninitializedLazyCallNode);
            RootCallNode cachedNode = new CachedCallNode(this.functionNode, current, next, function);
            this.replace(cachedNode);
            return cachedNode;
        }

        private RootCallNode createNextNode() {
            if (depth + 1 < FUNCTION_INLINE_CACHE_SIZE) {
                if (parserNode != null) {
                    return new UninitializedLazyCallNode(depth, parserNode);
                } else {
                    return new UninitializedCallNode(this);
                }
            } else {
                // 2 possible cases: G (Multiple functions, no varargs) or GV (Multiple function
                // arbitrary varargs)
                CallArgumentsNode clonedArgs = getClonedArgs();
                return args.containsVarArgsSymbol() ? new GenericVarArgsCallNode(functionNode, clonedArgs) : new GenericCallNode(functionNode, clonedArgs);
            }
        }

        private static RCallNode createCacheNode(VirtualFrame frame, RFunction function, CallArgumentsNode args, SourceSection callSrc) {
            CompilerDirectives.transferToInterpreter();
            SourceSection argsSrc = args.getEncapsulatingSourceSection();
            for (String name : args.getSignature()) {
                if (name != null && name.isEmpty()) {
                    throw RError.error(RError.Message.ZERO_LENGTH_VARIABLE);
                }
            }

            RCallNode callNode = null;
            // Check implementation: If written in Java, handle differently!
            if (function.isBuiltin()) {
                RootCallTarget callTarget = function.getTarget();
                RBuiltinRootNode root = findBuiltinRootNode(callTarget);
                if (root != null) {
                    // We inline the given arguments here, as builtins are executed inside the same
                    // frame as they are called.
                    InlinedArguments inlinedArgs = ArgumentMatcher.matchArgumentsInlined(function, args, callSrc, argsSrc);
                    callNode = root.inline(inlinedArgs);
                }
            } else {
                // Now we need to distinguish: Do supplied arguments vary between calls?
                if (args.containsVarArgsSymbol()) {
                    // Yes, maybe.
                    VarArgsCacheCallNode nextNode = new UninitializedVarArgsCacheCallNode(args);
                    VarArgsSignature varArgsSignature = args.createSignature(frame);
                    callNode = DispatchedVarArgsCallNode.create(frame, args, nextNode, callSrc, function, varArgsSignature, true);
                } else {
                    // Nope! (peeewh)
                    MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(function, args, callSrc, argsSrc, false);
                    callNode = new DispatchedCallNode(function, matchedArgs);
                }
            }

            callNode.assignSourceSection(callSrc);
            return callNode;
        }

        private RCallNode createCacheNode(VirtualFrame frame, RFunction function, boolean reuseArgs) {
            CompilerDirectives.transferToInterpreter();
            return createCacheNode(frame, function, reuseArgs ? args : getClonedArgs(), getSourceSection());
        }

        public CallArgumentsNode getClonedArgs() {
            return NodeUtil.cloneNode(args);
        }

    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    public static final class UninitializedLazyCallNode extends RootCallNode {

        private final int depth;
        private final ASTNode parserNode;

        protected UninitializedLazyCallNode(int depth, ASTNode parserNode) {
            super(null, null);
            this.depth = depth;
            this.parserNode = parserNode;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return specialize().execute(frame, function);
        }

        private RCallNode specialize() {
            UninitializedCallNode callNode = (UninitializedCallNode) parserNode.accept(new RTruffleVisitor());
            callNode.depth = depth;
            return replace(callNode);
        }

        @Override
        public CallArgumentsNode getArgumentsNode() {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            UninitializedCallNode callNode = (UninitializedCallNode) specialize();
            return callNode.getArgumentsNode();
        }
    }

    /**
     * [D] A {@link RCallNode} for calls to fixed {@link RFunction}s with fixed arguments (no
     * varargs).
     *
     * @see RCallNode
     */
    private static final class DispatchedCallNode extends LeafCallNode {

        @Child private DirectCallNode call;
        @Child private MatchedArgumentsNode matchedArgs;

        private final boolean needsCallerFrame;
        @CompilationFinal private boolean needsSplitting;

        DispatchedCallNode(RFunction function, MatchedArguments matchedArgs) {
            this.matchedArgs = matchedArgs.createNode();
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
            this.needsCallerFrame = function.containsDispatch();
            this.needsSplitting = function.containsDispatch();
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction) {
            if (needsSplitting) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                needsSplitting = false;
                call.cloneCallTarget();
            }
            MaterializedFrame callerFrame = needsCallerFrame ? frame.materialize() : null;
            Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), callerFrame, RArguments.getDepth(frame) + 1, matchedArgs.executeArray(frame), matchedArgs.getSignature());
            return call.call(frame, argsObject);
        }

        @Override
        public MatchedArgumentsNode getMatchedArgumentsNode() {
            return matchedArgs;
        }
    }

    /**
     * [G] A {@link RCallNode} in case there is no fixed {@link RFunction} (and no varargs).
     *
     * @see RCallNode
     */
    private static final class GenericCallNode extends RootCallNode {

        @Child private IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();

        private CallTarget lastCallTarget = null;
        private MatchedArguments lastMatchedArgs = null;

        GenericCallNode(RNode functionNode, CallArgumentsNode args) {
            super(functionNode, args);
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction) {
            CompilerDirectives.transferToInterpreter();

            if (lastCallTarget == currentFunction.getTarget() && lastMatchedArgs != null) {
                // poor man's caching succeeded - same function: no re-match needed
                Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), null, RArguments.getDepth(frame) + 1, lastMatchedArgs.doExecuteArray(frame),
                                lastMatchedArgs.getSignature());
                return indirectCall.call(frame, currentFunction.getTarget(), argsObject);
            }

            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(currentFunction, args, getSourceSection(), args.getEncapsulatingSourceSection(), true);
            this.lastMatchedArgs = matchedArgs;
            this.lastCallTarget = currentFunction.getTarget();

            Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), null, RArguments.getDepth(frame) + 1, matchedArgs.doExecuteArray(frame), matchedArgs.getSignature());
            return indirectCall.call(frame, currentFunction.getTarget(), argsObject);
        }
    }

    /*
     * Varargs cache classes follow
     */

    /**
     * Base class for the varargs cache [UVC] and [DV].
     *
     * @see RCallNode
     */
    private abstract static class VarArgsCacheCallNode extends LeafCallNode {

        @Override
        public Object execute(VirtualFrame frame, RFunction function) {
            return execute(frame, function, null);
        }

        protected abstract Object execute(VirtualFrame frame, RFunction function, VarArgsSignature varArgsSignature);
    }

    /**
     * [UVC] The uninitialized end of the varargs cache.
     *
     * @see RCallNode
     */
    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    public static final class UninitializedVarArgsCacheCallNode extends VarArgsCacheCallNode {
        @Child private CallArgumentsNode args;
        private int depth = 1;  // varargs cached is started with a [DV] DispatchedVarArgsCallNode

        public UninitializedVarArgsCacheCallNode(CallArgumentsNode args) {
            this.args = args;
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction function, VarArgsSignature varArgsSignature) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            // Extend cache
            this.depth += 1;
            CallArgumentsNode clonedArgs = NodeUtil.cloneNode(args);
            VarArgsCacheCallNode next = createNextNode(function);
            DispatchedVarArgsCallNode newCallNode = DispatchedVarArgsCallNode.create(frame, clonedArgs, next, getSourceSection(), function, varArgsSignature, false);
            return replace(newCallNode).execute(frame, function, varArgsSignature);
        }

        private VarArgsCacheCallNode createNextNode(RFunction function) {
            if (depth < VARARGS_INLINE_CACHE_SIZE) {
                return this;
            } else {
                CallArgumentsNode clonedArgs = NodeUtil.cloneNode(args);
                return new DispatchedGenericVarArgsCallNode(function, clonedArgs);
            }
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
        @Child private MatchedArgumentsNode matchedArgs;

        private final VarArgsSignature cachedSignature;
        private final boolean needsCallerFrame;
        @CompilationFinal private boolean needsSplitting;

        /**
         * Whether this [DV] node is the root of the varargs sub-cache (cmp. {@link RCallNode})
         */
        private final boolean isVarArgsRoot;

        /**
         * Used to profile on constant {@link #isVarArgsRoot}.
         */
        private final ConditionProfile isRootProfile = ConditionProfile.createBinaryProfile();

        protected DispatchedVarArgsCallNode(CallArgumentsNode args, VarArgsCacheCallNode next, RFunction function, VarArgsSignature varArgsSignature, MatchedArguments matchedArgs,
                        boolean isVarArgsRoot) {
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
            this.args = args;
            this.next = next;
            this.cachedSignature = varArgsSignature;
            this.matchedArgs = matchedArgs.createNode();
            this.isVarArgsRoot = isVarArgsRoot;
            this.needsCallerFrame = function.containsDispatch();
            /*
             * this is a simple heuristic - methods that need a caller frame should have call site -
             * specific versions
             */
            this.needsSplitting = function.containsDispatch();
        }

        protected static DispatchedVarArgsCallNode create(VirtualFrame frame, CallArgumentsNode args, VarArgsCacheCallNode next, SourceSection callSrc, RFunction function,
                        VarArgsSignature varArgsSignature, boolean isVarArgsRoot) {
            UnrolledVariadicArguments unrolledArguments = args.executeFlatten(frame);
            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(function, unrolledArguments, callSrc, args.getEncapsulatingSourceSection(), false);
            return new DispatchedVarArgsCallNode(args, next, function, varArgsSignature, matchedArgs, isVarArgsRoot);
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction, VarArgsSignature varArgsSignature) {
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

            if (needsSplitting) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                needsSplitting = false;
                call.cloneCallTarget();
            }
            MaterializedFrame callerFrame = needsCallerFrame ? frame.materialize() : null;
            Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), callerFrame, RArguments.getDepth(frame) + 1, matchedArgs.executeArray(frame), matchedArgs.getSignature());
            return call.call(frame, argsObject);
        }

        @Override
        public MatchedArgumentsNode getMatchedArgumentsNode() {
            return matchedArgs;
        }
    }

    /**
     * [DGV] {@link RCallNode} in case there is a cached {@link RFunction} with cached varargs that
     * exceeded the cache size {@link RCallNode#VARARGS_INLINE_CACHE_SIZE} .
     *
     * @see RCallNode
     */
    private static final class DispatchedGenericVarArgsCallNode extends VarArgsCacheCallNode {

        @Child private DirectCallNode call;
        @Child private CallArgumentsNode suppliedArgs;

        @CompilationFinal private boolean needsCallerFrame;

        DispatchedGenericVarArgsCallNode(RFunction function, CallArgumentsNode suppliedArgs) {
            this.call = Truffle.getRuntime().createDirectCallNode(function.getTarget());
            this.suppliedArgs = suppliedArgs;
        }

        @Override
        protected Object execute(VirtualFrame frame, RFunction currentFunction, VarArgsSignature varArgsSignature) {
            CompilerDirectives.transferToInterpreter();

            // Arguments may change every call: Flatt'n'Match on SlowPath! :-/
            UnrolledVariadicArguments argsValuesAndNames = suppliedArgs.executeFlatten(frame);
            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(currentFunction, argsValuesAndNames, getSourceSection(), getEncapsulatingSourceSection(), true);

            if (!needsCallerFrame && currentFunction.containsDispatch()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                needsCallerFrame = true;
            }
            MaterializedFrame callerFrame = needsCallerFrame ? frame.materialize() : null;
            Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), callerFrame, RArguments.getDepth(frame) + 1, matchedArgs.doExecuteArray(frame), matchedArgs.getSignature());
            return call.call(frame, argsObject);
        }
    }

    /**
     * [GV] {@link RootCallNode} in case there is no fixed {@link RFunction} AND varargs...
     *
     * @see RCallNode
     */
    private static final class GenericVarArgsCallNode extends RootCallNode {

        @Child private IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();

        GenericVarArgsCallNode(RNode functionNode, CallArgumentsNode args) {
            super(functionNode, args);
        }

        @Override
        public Object execute(VirtualFrame frame, RFunction currentFunction) {
            CompilerDirectives.transferToInterpreter();

            // Function and arguments may change every call: Flatt'n'Match on SlowPath! :-/
            UnrolledVariadicArguments argsValuesAndNames = args.executeFlatten(frame);
            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(currentFunction, argsValuesAndNames, getSourceSection(), getEncapsulatingSourceSection(), true);

            Object[] argsObject = RArguments.create(currentFunction, getSourceSection(), null, RArguments.getDepth(frame) + 1, matchedArgs.doExecuteArray(frame), matchedArgs.getSignature());
            return indirectCall.call(frame, currentFunction.getTarget(), argsObject);
        }
    }
}
