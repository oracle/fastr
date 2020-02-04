/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Denotes an R {@code promise}.
 */
@ValueType
@ExportLibrary(InteropLibrary.class)
public class RPromise extends RBaseObject {

    private static final int DEFAULT_BIT = 0x1;
    private static final int FULL_PROMISE_BIT = 0x2;
    private static final int EAGER_BIT = 0x4;
    private static final int EXPLICIT_BIT = 0x8;
    private static final int UNDER_EVALUATION_BIT = 0x10;
    private static final int UNDER_EVALUATION_MASK = 0x0f;

    private static final String MEMBER_VALUE = "value";
    private static final String MEMBER_IS_EVALUATED = "isEvaluated";
    private static final String MEMBER_EXPR = "expression";

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
         * This promise is currently being evaluated. This is necessary to avoid cyclic evaluation,
         * and can by checked via {@link #isUnderEvaluation()}.
         */
        UnderEvaluation(UNDER_EVALUATION_BIT);

        private final int bits;

        PromiseState(int bits) {
            this.bits = bits;
        }

        public static boolean isFullPromise(int state) {
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
     * This creates a new tuple (expr, env, null, value), which is already evaluated. Note: we still
     * need to keep the execution environment, because it can be accessed via R API {@code PRENV},
     * and the environment can indeed be used for tidy evaluation.
     */
    RPromise(PromiseState state, Closure closure, Object value, MaterializedFrame execFrame) {
        assert value != null;
        this.state = state.bits;
        this.closure = closure;
        this.value = value;
        this.execFrame = execFrame;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return RDataFactory.createStringVector(new String[]{MEMBER_VALUE, MEMBER_IS_EVALUATED, MEMBER_EXPR}, true);
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return MEMBER_EXPR.equals(member) || MEMBER_VALUE.equals(member) || MEMBER_IS_EVALUATED.equals(member);
    }

    @ExportMessage
    Object readMember(String member) throws UnknownIdentifierException {
        if (MEMBER_EXPR.equals(member)) {
            Closure cl = getClosure();
            RSyntaxNode sn = cl.getExpr().asRSyntaxNode();
            if (sn instanceof RSyntaxLookup) {
                return RDataFactory.createSymbol(((RSyntaxLookup) sn).getIdentifier());
            } else if (sn instanceof RSyntaxConstant) {
                return ((RSyntaxConstant) sn).getValue();
            } else {
                return RDataFactory.createLanguage(cl);
            }
        }
        if (MEMBER_IS_EVALUATED.equals(member)) {
            return RRuntime.asLogical(isEvaluated());
        }
        if (MEMBER_VALUE.equals(member)) {
            // only read value if evaluated
            if (isEvaluated()) {
                return getValue();
            }
            return RNull.instance;
        }
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    boolean isMemberModifiable(String member) {
        return member.equals(MEMBER_IS_EVALUATED);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isMemberInsertable(@SuppressWarnings("unused") String member) {
        return false;
    }

    @ExportMessage
    void writeMember(String member, Object newvalue) throws UnknownIdentifierException, UnsupportedTypeException {
        if (MEMBER_IS_EVALUATED.equals(member)) {
            if (!(newvalue instanceof Boolean)) {
                throw UnsupportedTypeException.create(new Object[]{newvalue});
            }

            boolean newVal = (boolean) newvalue;
            if (!isEvaluated() && newVal) {
                RContext.getRRuntimeASTAccess().forcePromise(this);
            } else if (isEvaluated() && !newVal) {
                resetValue();
            }
        } else {
            throw UnknownIdentifierException.create(member);
        }
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
     * Discards any previously evaluated value if this is not an eager promise. This is for
     * debugging purposes only!
     */
    public final void resetValue() {
        // Only non-eager promises can be reset.
        if (!PromiseState.isEager(state)) {
            value = null;
        }
    }

    /**
     * Used in case the {@link RPromise} is evaluated outside.
     *
     * @param newValue
     */
    public final void setValue(Object newValue) {
        assert !isEvaluated();
        assert newValue != null;
        assert !(newValue instanceof RPromise);
        this.value = newValue;
    }

    /**
     * Allows in-place update of the value.
     */
    public final void updateValue(Object newValue) {
        assert newValue != null;
        assert !(newValue instanceof RPromise);
        assert value != null;
        this.value = newValue;
    }

    /**
     * Promises to constants can be optimized, which means that they only hold the value.
     */
    public boolean isOptimized() {
        return value != null && this.closure.getExpr() instanceof RSyntaxConstant;
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

//@formatter:off
    /**
     * This is a {@link RPromise} implementation that performs two optimizations:
     * <ul>
     * <li>1. It does not carry a {@link MaterializedFrame} when in the compiler,
     * ({@link RPromise#execFrame}) but it knows how to retrieve the correct one if needed (by
     * traversing the Truffle frames and using the {@link EagerPromise#targetFrame} r-caller as the
     * anchor)</li>
     * <li>2. It carries a pre-evaluated value of the symbol-expression it is supposed to evaluate
     * on first read</li>
     * </ul>
     * The 1. optimization is only possible if the {@link EagerPromise} does not leave the stack it
     * was created in, e.g. by means of "sys.frame", "function" or similar. If it needs to be
     * present for any reason, {@link #materialize()} is called (see
     * {@code PromiseHelperNode.PromiseDeoptimizeFrameNode}).<br/>
     * For symbol-lookup promises, the 2. optimization is only possible as long as it can be
     * guaranteed that the symbol it was originally read from has not been altered in the mean time.
     * If this cannot be guaranteed for any reason, this promise gets {@link #deoptimize()
     * deoptimized} (which includes {@link #materialize() materialization}). (See
     * {@code OptVariablePromiseBaseNode})<br/>
     * For forced promises, the 2. optimization is only possible as long as either the promise
     * expression has no side-effects (see {@code OptForcedEagerPromiseNode}).
     */
//@formatter:on
    public static final class EagerPromise extends RPromise {
        private final Object eagerValue;

        private final Assumption notChangedNonLocally;
        private final RCaller targetFrame;
        private final EagerFeedback feedback;

        /**
         * Index of the argument for which the promise was created for. {@code -1} for promises
         * created for default argument values.
         */
        private final int wrapIndex;

        /**
         * Set to <code>true</code> by {@link #deoptimize()}. If this is true, the
         * {@link RPromise#execFrame} is guaranteed to be set.
         */
        private boolean deoptimized = false;

        EagerPromise(PromiseState state, Closure closure, Object eagerValue, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback, int wrapIndex, MaterializedFrame execFrame) {
            super(state, execFrame, closure);
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
                materialize();
                return false;
            }
            return true;
        }

        @TruffleBoundary
        public void materialize() {
            if (execFrame == null) {
                this.execFrame = Utils.getStackFrame(FrameAccess.MATERIALIZE, targetFrame).materialize();
                if (feedback != null) {
                    notifyFailure();
                }
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
        public RPromise createEagerSuppliedPromise(Object eagerValue, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback, int wrapIndex, MaterializedFrame execFrame) {
            return RDataFactory.createEagerPromise(state == PromiseState.Default ? PromiseState.EagerDefault : PromiseState.EagerSupplied, exprClosure, eagerValue, notChangedNonLocally, targetFrame,
                            feedback, wrapIndex, execFrame);
        }

        public RPromise createPromisedPromise(RPromise promisedPromise, Assumption notChangedNonLocally, RCaller targetFrame, EagerFeedback feedback, MaterializedFrame execFrame) {
            assert state == PromiseState.Supplied;
            return RDataFactory.createPromisedPromise(exprClosure, promisedPromise, notChangedNonLocally, targetFrame, feedback, execFrame);
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

    public void setUnderEvaluation() {
        assert (state & UNDER_EVALUATION_BIT) == 0;
        state |= UNDER_EVALUATION_BIT;
    }

    public void resetUnderEvaluation() {
        assert (state & UNDER_EVALUATION_BIT) != 0;
        state &= UNDER_EVALUATION_MASK;
    }
}
