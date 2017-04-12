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
package com.oracle.truffle.r.runtime.data;

import java.util.HashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;

/**
 * Denotes an R {@code promise}.
 */
@ValueType
public class RPromise implements RTypedValue {

    private static final int DEFAULT_BIT = 0x1;
    private static final int FULL_PROMISE_BIT = 0x2;
    private static final int EAGER_BIT = 0x4;
    private static final int EXPLICIT_BIT = 0x8;
    private static final int UNDER_EVALUATION_BIT = 0x10;
    private static final int UNDER_EVALUATION_MASK = 0x0f;

    /**
     * This enum encodes the source, optimization and current state of a promise.
     */
    public enum PromiseState {
        /**
         * This promise is created for an argument that has been supplied to the function call and
         * thus has to be evaluated inside the caller frame.
         */
        Supplied(FULL_PROMISE_BIT),
        /**
         * This promise is created for an argument that was 'missing' at the function call and thus
         * contains it's default value and has to be evaluated inside the _callee_ frame.
         */
        Default(DEFAULT_BIT | FULL_PROMISE_BIT),
        /**
         * A supplied promise that was optimized to eagerly evaluate its value.
         */
        EagerSupplied(EAGER_BIT),
        /**
         * A default promise that was optimized to eagerly evaluate its value.
         */
        EagerDefault(EAGER_BIT | DEFAULT_BIT),
        /**
         * This promise was created to wrap around a parameter value that is a promise itself.
         */
        Promised(0),
        /**
         * This promise is not a function argument at all. (Created by 'delayedAssign', for
         * example).
         */
        Explicit(EXPLICIT_BIT | FULL_PROMISE_BIT),
        /**
         * This promise is currently being evaluated. This necessary to avoid cyclic evaluation, and
         * can by checked via {@link #isUnderEvaluation()}.
         */
        UnderEvaluation(UNDER_EVALUATION_BIT);

        private final int bits;

        PromiseState(int bits) {
            this.bits = bits;
        }

        public static boolean isDefaultOpt(int state) {
            return (state & FULL_PROMISE_BIT) != 0;
        }

        public static boolean isEager(int state) {
            return (state & EAGER_BIT) != 0;
        }

        public static boolean isExplicit(int state) {
            return (state & EXPLICIT_BIT) != 0;
        }
    }

    private int state;

    /**
     * @see #getFrame()
     * @see EagerPromise#materialize()
     */
    @CompilationFinal protected MaterializedFrame execFrame;

    /**
     * May not be <code>null</code>.
     */
    private final Closure closure;

    /**
     * When {@code null} the promise has not been evaluated.
     */
    private Object value = null;

    /**
     * This creates a new tuple (expr, env, closure, value=null), which may later be evaluated.
     */
    RPromise(PromiseState state, MaterializedFrame execFrame, Closure closure) {
        this.state = state.bits;
        this.execFrame = execFrame;
        this.closure = closure;
    }

    /**
     * This creates a new tuple (expr, null, null, value), which is already evaluated.
     */
    RPromise(PromiseState state, Closure closure, Object value) {
        assert value != null;
        this.state = state.bits;
        this.closure = closure;
        this.value = value;
        // Not needed as already evaluated:
        this.execFrame = null;
    }

    @Override
    public RType getRType() {
        return RType.Promise;
    }

    public final int getState() {
        return state;
    }

    public final void setState(int state) {
        assert !isEvaluated();
        this.state = state;
    }

    /**
     * @return {@link #closure}
     */
    public final Closure getClosure() {
        return closure;
    }

    public final RBaseNode getRep() {
        return closure.getExpr();
    }

    public final boolean isDefaultArgument() {
        return (state & DEFAULT_BIT) != 0;
    }

    public final boolean isNullFrame() {
        assert !isEvaluated();
        return execFrame == null;
    }

    /**
     * @return The {@link #value} of the promise - this must only be called for evaluated promises.
     */
    public final Object getValue() {
        assert isEvaluated();
        return value;
    }

    /**
     * @return The raw {@link #value}, which may be {@code null} if the promise was not evaluated
     *         yet.
     */
    public final Object getRawValue() {
        return value;
    }

    /**
     * Used in case the {@link RPromise} is evaluated outside.
     *
     * @param newValue
     */
    public final void setValue(Object newValue) {
        assert !isEvaluated();
        assert newValue != null;
        this.value = newValue;
    }

    /**
     * Returns {@code true} if this promise has been evaluated?
     */
    public final boolean isEvaluated() {
        return value != null;
    }

    /**
     * @return {@link #execFrame}. This might be <code>null</code>! Materialize before.
     *
     * @see #execFrame
     */
    public final MaterializedFrame getFrame() {
        assert !isEvaluated();
        return execFrame;
    }

    /**
     * @param frame
     * @return Whether the given {@link RPromise} is in its origin context and thus can be resolved
     *         directly inside the AST.
     */
    public final boolean isInOriginFrame(VirtualFrame frame) {
        assert !isEvaluated();
        if (isDefaultArgument() && isNullFrame()) {
            return true;
        }

        if (frame == null) {
            return false;
        }
        return frame == execFrame;
    }

    /**
     * @return The state of the {@code underEvaluation} flag.
     */
    public final boolean isUnderEvaluation() {
        assert !isEvaluated();
        return (state & UNDER_EVALUATION_BIT) != 0;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "[" + state + ", " + execFrame + ", expr=" + getRep() + ", " + value + "]";
    }

    /**
     * This is a {@link RPromise} implementation that performs two optimizations:
     * <ul>
     * <li>1. It does not carry a {@link MaterializedFrame} ({@link RPromise#execFrame}) but knows
     * how to retrieve the correct one if needed</li>
     * <li>2. It carries a pre-evaluated value of the symbol-expression it is supposed to evaluate
     * on first read</li>
     * </ul>
     * The 1. optimization is only possible if the {@link EagerPromise} does not leave the stack it
     * was created in, e.g. by the means of "sys.frame", "function" or similar. If it needs to be
     * present for any reason, {@link #materialize()} is called.<br/>
     * The 2. optimization is only possible as long it can be guaranteed that the symbol it was
     * originally read from has not been altered in the mean time. If this cannot be guaranteed for
     * any reason, a Promise gets {@link #deoptimize()} (which includes {@link #materialize()}ion).
     */
    public static final class EagerPromise extends RPromise {
        private final Object eagerValue;

        private final Assumption notChangedNonLocally;
        private final RCaller targetFrame;
        private final EagerFeedback feedback;
        private final int wrapIndex;

        /**
         * Set to <code>true</code> by {@link #deoptimize()}. If this is true, the
         * {@link RPromise#execFrame} is guaranteed to be set.
         */
        private boolean deoptimized = false;

        EagerPromise(PromiseState state, Closure closure, Object eagerValue, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback, int wrapIndex) {
            super(state, (MaterializedFrame) null, closure);
            assert state != PromiseState.Explicit;
            this.eagerValue = eagerValue;
            this.notChangedNonLocally = notChangedNonLocally;
            this.targetFrame = targetFrame;
            this.feedback = feedback;
            this.wrapIndex = wrapIndex;
        }

        /**
         * @return Whether the promise has been deoptimized before
         */
        public boolean deoptimize() {
            if (!deoptimized && !isEvaluated() && feedback != null) {
                deoptimized = true;
                notifyFailure();
                materialize();
                return false;
            }
            return true;
        }

        @TruffleBoundary
        public void materialize() {
            if (execFrame == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.execFrame = Utils.getStackFrame(FrameAccess.MATERIALIZE, targetFrame).materialize();
            }
        }

        public Object getEagerValue() {
            return eagerValue;
        }

        public boolean isDeoptimized() {
            return deoptimized;
        }

        public Assumption getIsValidAssumption() {
            return notChangedNonLocally;
        }

        public boolean isValid() {
            return notChangedNonLocally.isValid();
        }

        public void notifyFailure() {
            assert feedback != null : "eager promises without feedback should never fail";
            feedback.onFailure(this);
        }

        public int wrapIndex() {
            return wrapIndex;
        }
    }

    /**
     * Used to allow feedback on {@link EagerPromise} evaluation.
     */
    public interface EagerFeedback {

        /**
         * Whenever an optimized {@link EagerPromise} has been deoptimized, or it's assumption did
         * not hold until evaluation.
         *
         * @param promise
         */
        void onFailure(RPromise promise);
    }

    /**
     * A factory which produces instances of {@link RPromise}.
     */
    public static final class RPromiseFactory {
        private final Closure exprClosure;
        private final PromiseState state;

        /**
         * Create the promise with a representation that allows evaluation later in the "current"
         * frame. The frame may need to be set if the promise is passed as an argument to another
         * function.
         */
        public static RPromiseFactory create(PromiseState state, Closure suppliedClosure) {
            return new RPromiseFactory(state, suppliedClosure);
        }

        private RPromiseFactory(PromiseState state, Closure suppliedClosure) {
            assert state == PromiseState.Default || state == PromiseState.Supplied;
            this.state = state;
            this.exprClosure = suppliedClosure;
        }

        /**
         * @return A {@link RPromise} from the given parameters
         */
        public RPromise createPromise(MaterializedFrame frame) {
            return RDataFactory.createPromise(state, exprClosure, frame);
        }

        /**
         * @param eagerValue The eagerly evaluated value
         * @param notChangedNonLocally The {@link Assumption} that eagerValue is still valid
         * @param feedback The {@link EagerFeedback} to notify whether the {@link Assumption} hold
         *            until evaluation
         * @return An {@link EagerPromise}
         */
        public RPromise createEagerSuppliedPromise(Object eagerValue, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback, int wrapIndex) {
            return RDataFactory.createEagerPromise(state == PromiseState.Default ? PromiseState.EagerDefault : PromiseState.EagerSupplied, exprClosure, eagerValue, notChangedNonLocally, targetFrame,
                            feedback, wrapIndex);
        }

        public RPromise createPromisedPromise(RPromise promisedPromise, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback) {
            assert state == PromiseState.Supplied;
            return RDataFactory.createPromisedPromise(exprClosure, promisedPromise, notChangedNonLocally, targetFrame, feedback);
        }

        public Object getExpr() {
            if (exprClosure == null) {
                return null;
            }
            return exprClosure.getExpr();
        }

        public PromiseState getState() {
            return state;
        }
    }

    public static final class Closure {
        private HashMap<FrameDescriptor, RootCallTarget> callTargets;

        public static final String CLOSURE_WRAPPER_NAME = new String("<promise>");

        private final RBaseNode expr;
        private final String symbol;
        private final String stringConstant;

        private Closure(RBaseNode expr) {
            this.expr = expr;
            if (expr.asRSyntaxNode() instanceof RSyntaxLookup) {
                this.symbol = ((RSyntaxLookup) expr.asRSyntaxNode()).getIdentifier().intern();
            } else {
                this.symbol = null;
            }
            if (expr.asRSyntaxNode() instanceof RSyntaxConstant) {
                Object constant = ((RSyntaxConstant) expr.asRSyntaxNode()).getValue();
                if (constant instanceof String) {
                    this.stringConstant = (String) constant;
                } else if (constant instanceof RAbstractStringVector && ((RAbstractStringVector) constant).getLength() == 1) {
                    this.stringConstant = ((RAbstractStringVector) constant).getDataAt(0);
                } else {
                    this.stringConstant = null;
                }
            } else {
                this.stringConstant = null;
            }
        }

        public static Closure create(RBaseNode expr) {
            return new Closure(expr);
        }

        /**
         * Evaluates an {@link com.oracle.truffle.r.runtime.data.RPromise.Closure} in {@code frame}.
         */
        public Object eval(MaterializedFrame frame) {
            CompilerAsserts.neverPartOfCompilation();

            // Create lazily, as it is not needed at all for INLINED promises!
            if (callTargets == null) {
                callTargets = new HashMap<>();
            }
            FrameDescriptor desc = frame.getFrameDescriptor();
            RootCallTarget callTarget = callTargets.get(desc);

            if (callTarget == null) {
                // clone for additional call targets
                callTarget = generateCallTarget((RNode) (callTargets.isEmpty() ? expr : RContext.getASTBuilder().process(expr.asRSyntaxNode())));
                callTargets.put(desc, callTarget);
            }
            return callTarget.call(frame);
        }

        private static RootCallTarget generateCallTarget(RNode expr) {
            return RContext.getEngine().makePromiseCallTarget(expr, CLOSURE_WRAPPER_NAME + System.identityHashCode(expr));
        }

        public RBaseNode getExpr() {
            return expr;
        }

        public String asSymbol() {
            return symbol;
        }

        public String asStringConstant() {
            return stringConstant;
        }
    }

    @Override
    public int getTypedValueInfo() {
        return 0;
    }

    @Override
    public void setTypedValueInfo(int value) {
        // This gets called from RSerialize, just ignore (for now)
    }

    @Override
    public boolean isS4() {
        return false;
    }

    public void setUnderEvaluation() {
        assert (state & UNDER_EVALUATION_BIT) == 0;
        state |= UNDER_EVALUATION_BIT;
    }

    public void resetUnderEvaluation() {
        assert (state & UNDER_EVALUATION_BIT) != 0;
        state &= UNDER_EVALUATION_MASK;
    }
}
