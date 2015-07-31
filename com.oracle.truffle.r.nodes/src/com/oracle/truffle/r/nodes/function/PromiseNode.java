/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.function.opt.EagerEvalHelper.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.function.opt.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.env.*;

/**
 * These {@link RNode} implementations are used as a factory-nodes for {@link RPromise}s.<br/>
 * All these classes are created during/after argument matching and get cached afterwards, so they
 * get (and need to get) called every repeated call to a function with the same arguments.
 *
 * TODO Certain subclasses used to override {@code deparse}. Since a {@link PromiseNode} is not a
 * {@link RSyntaxNode} this is no longer possible. So we probably need wrappers.
 */
public abstract class PromiseNode extends RNode {
    /**
     * The {@link RPromiseFactory} which holds all information necessary to construct a proper
     * {@link RPromise} for every case that might occur.
     */
    protected final RPromiseFactory factory;

    /**
     * @param factory {@link #factory}
     */
    protected PromiseNode(RPromiseFactory factory) {
        this.factory = factory;
    }

    /**
     * Returns the {@link RSyntaxNode} that is the expression associated with the promise.
     */
    public abstract RSyntaxNode getPromiseExpr();

    public static RNode createInlined(RNode expression, Object defaultValue, boolean unwrap) {
        CompilerAsserts.neverPartOfCompilation();
        RNode clonedExpression = NodeUtil.cloneNode(expression);
        RNode pn = clonedExpression instanceof ConstantNode ? clonedExpression : new InlinedSuppliedArgumentNode(clonedExpression, defaultValue, unwrap);
        return pn;
    }

    /**
     * @param src The {@link SourceSection} of the argument for debugging purposes
     * @param factory {@link #factory}
     * @return Depending on {@link RPromiseFactory#getType()}, the proper {@link PromiseNode}
     *         implementation
     *
     *         TODO remove SourceSection arg
     */
    @TruffleBoundary
    public static RNode create(SourceSection src, RPromiseFactory factory, boolean noOpt) {
        assert factory.getType() != PromiseType.NO_ARG;

        RNode pn = null;
        // For ARG_DEFAULT, expr == defaultExpr!
        RNode arg = (RNode) factory.getExpr();
        RNode expr = arg;
        if (arg instanceof WrapArgumentNode) {
            expr = ((WrapArgumentNode) arg).getOperand();
        }
        if (isOptimizableConstant(expr)) {
            // As Constants don't care where they are evaluated, we don't need to
            // distinguish between ARG_DEFAULT and ARG_SUPPLIED
            pn = new OptConstantPromiseNode(factory.getType(), (ConstantNode) expr);
        } else

        if (factory.getType() == PromiseType.ARG_SUPPLIED) {
            if (isVararg(expr)) {
                pn = new VarargPromiseNode(factory, (VarArgNode) expr);
            } else

            if (!isVararg(expr) && !noOpt && isOptimizableVariable(expr)) {
                pn = new OptVariableSuppliedPromiseNode(factory, (ReadVariableNode) expr, arg == expr ? ArgumentStatePush.INVALID_INDEX : ((WrapArgumentNode) arg).getIndex());
            }

// if (isOptimizableExpression(expr)) {
// System.err.println(" >>> SUP " + src.getCode());
// }
        }

        if (pn == null) {
            pn = new PromisedNode(factory, src);
        }

        return pn;
    }

    /**
     * This method checks whether to apply optimizations to RNodes created for single "..."
     * elements.
     *
     * @param expr
     * @return Whether the given {@link RNode} is a {@link VarArgNode}
     */
    private static boolean isVararg(RNode expr) {
        return expr instanceof VarArgNode;
    }

    /**
     * @return Creates a {@link VarArgNode} for the given
     */
    public static VarArgNode createVarArg(int varArgIndex, SourceSection src) {
        return new VarArgNode(varArgIndex, src);
    }

    /**
     * A {@link PromiseNode} for supplied arguments.
     */
    private static final class PromisedNode extends PromiseNode {
        private final SourceSection src;

        private PromisedNode(RPromiseFactory factory, SourceSection src) {
            super(factory);
            this.src = src;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            MaterializedFrame execFrame = factory.getType() == PromiseType.ARG_SUPPLIED ? frame.materialize() : null;
            return factory.createPromise(execFrame);
        }

        @Override
        public SourceSection getEncapsulatingSourceSection() {
            return src;
        }

        @Override
        public RSyntaxNode getPromiseExpr() {
            return (RSyntaxNode) factory.getExpr();
        }
    }

    /**
     * TODO Expand!
     */
    private static final class OptVariableSuppliedPromiseNode extends OptVariablePromiseBaseNode {

        public OptVariableSuppliedPromiseNode(RPromiseFactory factory, ReadVariableNode rvn, int wrapIndex) {
            super(factory, rvn, wrapIndex);
        }

        @Override
        protected RNode createFallback() {
            return new PromisedNode(factory, originalRvn.getSourceSection());
        }

// @TruffleBoundary
        public void onSuccess(RPromise promise) {
// System.err.println("Opt SUCCESS: " + promise.getOptType());
        }

// @TruffleBoundary
        public void onFailure(RPromise promise) {
// System.err.println("Opt FAILURE: " + promise.getOptType());
            rewriteToFallback();
        }
    }

    /**
     * TODO Expand!
     */
    public static final class VarargPromiseNode extends PromiseNode {
        @Child private VarArgNode varargNode;

        public VarargPromiseNode(RPromiseFactory factory, VarArgNode varargNode) {
            super(factory);
            this.varargNode = varargNode;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // At this point we simply circumvent VarArgNode (by directly passing the contained
            // Promise); BUT we have to respect the RPromise! Thus we use a RPromise class which
            // knows that it contains a RPromise..
            return factory.createVarargPromise(varargNode.executeNonEvaluated(frame));
        }

        public RSyntaxNode substitute(REnvironment env) {
            // TODO Since VarargPromiseNode is not an RSyntaxNode this is suspicious
            return varargNode.substitute(env);
        }

        public VarArgNode getVarArgNode() {
            return varargNode;
        }

        @Override
        public SourceSection getEncapsulatingSourceSection() {
            return varargNode.getEncapsulatingSourceSection();
        }

        @Override
        public RSyntaxNode getPromiseExpr() {
            return (RSyntaxNode) factory.getExpr();
        }
    }

    /**
     * This class is meant for supplied arguments (which have to be evaluated in the caller frame)
     * which are supposed to be evaluated inline: This means we can simply evaluate it here, and not
     * create a promise.
     */
    private static final class InlinedSuppliedArgumentNode extends RNode {
        @Child private RNode expression;
        @Child private RNode defaultExpressionCache;
        private final Object defaultValue;
        private final boolean unwrap;

        @Child private PromiseHelperNode promiseHelper;

        private final BranchProfile isVarArgProfile = BranchProfile.create();
        private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();

        public InlinedSuppliedArgumentNode(RNode expression, Object defaultValue, boolean unwrap) {
            // TODO assert RSyntaxNode?
            this.expression = expression;
            this.defaultValue = defaultValue;
            this.unwrap = unwrap;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // builtin.inline: We do re-evaluation every execute inside the caller frame, as we
            // know that the evaluation of default values has no side effects (has to be assured by
            // builtin implementations)
            Object obj;
            if (unwrap && expression instanceof WrapArgumentNode) {
                obj = ((WrapArgumentNode) expression).getOperand().execute(frame);
            } else {
                obj = expression.execute(frame);
            }
            if (obj == RMissing.instance) {
                if (defaultExpressionCache == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    defaultExpressionCache = insert(ConstantNode.create(defaultValue));
                }
                return defaultExpressionCache.execute(frame);
            } else if (obj instanceof RArgsValuesAndNames) {
                isVarArgProfile.enter();
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                }
                return checkEvaluateArgs(frame, (RArgsValuesAndNames) obj);
            } else {
                if (isPromiseProfile.profile(obj instanceof RPromise)) {
                    if (promiseHelper == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        promiseHelper = insert(new PromiseHelperNode());
                    }
                    return promiseHelper.evaluate(frame, (RPromise) obj);
                } else {
                    return obj;
                }
            }
        }

        public RArgsValuesAndNames checkEvaluateArgs(VirtualFrame frame, RArgsValuesAndNames args) {
            Object[] values = args.getArguments();
            Object[] newValues = new Object[values.length];
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (isPromiseProfile.profile(value instanceof RPromise)) {
                    value = promiseHelper.evaluate(frame, (RPromise) value);
                }
                newValues[i] = value;
            }
            return new RArgsValuesAndNames(newValues, args.getSignature());
        }

        @Override
        public SourceSection getEncapsulatingSourceSection() {
            return expression.getSourceSection();
        }

    }

    /**
     * This {@link RNode} is used to evaluate the expression given in a {@link RPromise} formerly
     * wrapped into a "..." after unrolling.<br/>
     * In a certain sense this is the class corresponding class for GNU R's PROMSXP (AST equivalent
     * of RPromise, only needed for varargs in FastR TODO Move to separate package together with
     * other varargs classes) N.B. This implements {@link RSyntaxNode} because it can be stored as
     * an argument in a {@link RCallNode} where the arguments are statically typed as
     * {@link RSyntaxNode}.
     */
    public static final class VarArgNode extends RNode implements RSyntaxNode {

        @Child private FrameSlotNode varArgsSlotNode;
        @Child private PromiseHelperNode promiseHelper;

        private final int index;

        private VarArgNode(int index, SourceSection src) {
            this.index = index;
            if (src == null) {
                throw RInternalError.shouldNotReachHere();
            }
            assignSourceSection(src);
        }

        public RArgsValuesAndNames getVarargsAndNames(VirtualFrame frame) {
            if (varArgsSlotNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                varArgsSlotNode = insert(FrameSlotNode.create(ArgumentsSignature.VARARG_NAME));
            }
            RArgsValuesAndNames varArgsAndNames;
            try {
                varArgsAndNames = (RArgsValuesAndNames) frame.getObject(varArgsSlotNode.executeFrameSlot(frame));
            } catch (FrameSlotTypeException | ClassCastException e) {
                throw RInternalError.shouldNotReachHere("'...' should always be represented by RArgsValuesAndNames");
            }
            return varArgsAndNames;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            RPromise promise = executeNonEvaluated(frame);
            if (promise.isEvaluated()) {
                return promise.getValue();
            }
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            return promiseHelper.evaluate(frame, promise);
        }

        public RPromise executeNonEvaluated(VirtualFrame frame) {
            return (RPromise) getVarargsAndNames(frame).getArgument(index);
        }

        public RSyntaxNode substitute(REnvironment env) {
            Object obj = ((RArgsValuesAndNames) env.get("...")).getArgument(index);
            return obj instanceof RPromise ? (RSyntaxNode) ((RPromise) obj).getRep() : ConstantNode.create(obj);
        }

        public int getIndex() {
            return index;
        }

        @Override
        public void deparse(RDeparse.State state) {
            state.append(getSourceSection().getCode());
        }

    }

    @TruffleBoundary
    public static RNode createVarArgsInlined(RNode[] nodes, ArgumentsSignature signature) {
        return new InlineVarArgsNode(nodes, signature);
    }

    @TruffleBoundary
    public static RNode createVarArgs(RNode[] nodes, ArgumentsSignature signature, ClosureCache closureCache) {
        return new VarArgsPromiseNode(nodes, signature, closureCache);
    }

    /**
     * This class is used for wrapping arguments into "..." ({@link RArgsValuesAndNames}).
     */
    public static final class VarArgsPromiseNode extends RNode {
        @CompilationFinal private final Closure[] closures;
        private final ArgumentsSignature signature;
        protected final ClosureCache closureCache;

        public VarArgsPromiseNode(RNode[] nodes, ArgumentsSignature signature, ClosureCache closureCache) {
            this.closures = new Closure[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                this.closures[i] = closureCache.getOrCreateClosure(nodes[i]);
            }
            this.signature = signature;
            this.closureCache = closureCache;
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            Object[] promises = new Object[closures.length];
            for (int i = 0; i < closures.length; i++) {
                promises[i] = RDataFactory.createPromise(PromiseType.ARG_SUPPLIED, frame.materialize(), closures[i]);
            }
            return new RArgsValuesAndNames(promises, signature);
        }

        public Closure[] getClosures() {
            return closures;
        }

        public ArgumentsSignature getSignature() {
            return signature;
        }
    }

    /**
     * The inlined counterpart of {@link VarArgsPromiseNode}: This gets a bit more complicated, as
     * "..." might include values from an outer "...", which might resolve to an empty argument
     * list.
     */
    public static final class InlineVarArgsNode extends RNode {
        @Children private final RNode[] varargs;
        protected final ArgumentsSignature signature;

        @Child private PromiseCheckHelperNode promiseCheckHelper = new PromiseCheckHelperNode();
        private final ConditionProfile argsValueAndNamesProfile = ConditionProfile.createBinaryProfile();

        public InlineVarArgsNode(RNode[] nodes, ArgumentsSignature signature) {
            this.varargs = nodes;
            this.signature = signature;
            assert varargs.length == signature.getLength();
        }

        public RNode[] getVarargs() {
            return varargs;
        }

        public ArgumentsSignature getSignature() {
            return signature;
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            if (varargs.length == 0) {
                // No need to create an extra object, already have one
                return RArgsValuesAndNames.EMPTY;
            }
            Object[] evaluatedArgs = new Object[varargs.length];
            String[] evaluatedNames = null;
            int index = 0;
            for (int i = 0; i < varargs.length; i++) {
                Object argValue = varargs[i].execute(frame);
                if (argsValueAndNamesProfile.profile(argValue instanceof RArgsValuesAndNames)) {
                    if (evaluatedNames == null) {
                        evaluatedNames = initializeNames(i);
                    }
                    // this can happen if ... is simply passed around (in particular when the call
                    // chain contains two functions with just the ... argument)
                    RArgsValuesAndNames argsValuesAndNames = (RArgsValuesAndNames) argValue;
                    int newLength = evaluatedArgs.length + argsValuesAndNames.getLength() - 1;
                    if (newLength == 0) {
                        // Corner case: "f <- function(...) g(...); g <- function(...)"
                        // In this case, "..." gets evaluated, and its only content is "...", which
                        // itself is missing. Result: Both disappear!
                        return RArgsValuesAndNames.EMPTY;
                    }
                    evaluatedArgs = Utils.resizeArray(evaluatedArgs, newLength);
                    evaluatedNames = Utils.resizeArray(evaluatedNames, newLength);
                    Object[] varargValues = argsValuesAndNames.getArguments();
                    index = copyVarArgs(frame, evaluatedArgs, evaluatedNames, index, argsValuesAndNames, varargValues);
                } else {
                    if (evaluatedNames != null) {
                        evaluatedNames[index] = signature.getName(i);
                    }
                    evaluatedArgs[index++] = promiseCheckHelper.checkEvaluate(frame, argValue);
                }
            }
            ArgumentsSignature actualSignature = argsValueAndNamesProfile.profile(evaluatedNames != null) ? ArgumentsSignature.get(evaluatedNames) : signature;
            return new RArgsValuesAndNames(evaluatedArgs, actualSignature);
        }

        private String[] initializeNames(int i) {
            String[] evaluatedNames;
            evaluatedNames = new String[i];
            for (int j = 0; j < i; j++) {
                evaluatedNames[j] = signature.getName(j);
            }
            return evaluatedNames;
        }

        private int copyVarArgs(VirtualFrame frame, Object[] evaluatedArgs, String[] evaluatedNames, int startIndex, RArgsValuesAndNames argsValuesAndNames, Object[] varargValues) {
            int index = startIndex;
            for (int j = 0; j < argsValuesAndNames.getLength(); j++) {
                evaluatedArgs[index] = promiseCheckHelper.checkEvaluate(frame, varargValues[j]);
                evaluatedNames[index] = argsValuesAndNames.getSignature().getName(j);
                index++;
            }
            return index;
        }
    }
}
