/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Denotes an R {@code promise}.
 */
@ValueType
public class RPromise extends RLanguageRep {

    /**
     * The policy used to evaluate a promise.
     */
    public enum EvalPolicy {
        /**
         * This policy is use for arguments that are inlined (RBuiltinNode.inline) for the use in
         * builtins that are built-into FastR and thus written in Java.<br/>
         * Promises with this policy are evaluated every time they are executed inside the caller
         * (!) frame regardless of their {@link PromiseType}(!!) and return their values immediately
         * (thus are no real promises).
         */
        INLINED,

        /**
         * This promise is an actual promise! It's value won't get evaluated until it's read.<br/>
         * Promises with this policy are evaluated when forced/used the first time. Evaluation
         * happens inside the caller frame if {@link PromiseType#ARG_SUPPLIED} or inside callee
         * frame if {@link PromiseType#ARG_DEFAULT}.
         */
        PROMISED;
    }

    /**
     * Different to GNU R, FastR has no additional binding information (a "origin" where the binding
     * is coming from). This enum is meant to substitute this information.
     */
    public enum PromiseType {
        /**
         * This promise is created for an argument that has been supplied to the function call and
         * thus has to be evaluated inside the caller frame.
         */
        ARG_SUPPLIED,

        /**
         * This promise is created for an argument that was 'missing' at the function call and thus
         * contains it's default value and has to be evaluated inside the _callee_ frame.
         */
        ARG_DEFAULT,

        /**
         * This promise is not a function argument at all. (Created by 'delayedAssign', for
         * example).
         */
        NO_ARG;
    }

    private enum OptType {
        DEFAULT,
        EAGER,
        VARARG,
        PROMISED
    }

    public static final String CLOSURE_WRAPPER_NAME = new String("<promise>");

    /**
     * @see EvalPolicy
     */
    protected final EvalPolicy evalPolicy;

    /**
     * @see PromiseType
     */
    protected final PromiseType type;

    protected final OptType optType;

    /**
     * @see #getFrame()
     * @see #materialize(PromiseProfile)
     */
    protected MaterializedFrame execFrame;

    /**
     * Might not be <code>null</code>.
     */
    private final Closure closure;

    /**
     * When {@code null} the promise has not been evaluated.
     */
    protected Object value = null;

    /**
     * A flag to indicate the promise has been evaluated.
     */
    protected boolean isEvaluated = false;

    /**
     * A flag which is necessary to avoid cyclic evaluation. Manipulated by
     * {@link #setUnderEvaluation(boolean)} and can by checked via
     * {@link #isUnderEvaluation(PromiseProfile)}.
     */
    private boolean underEvaluation = false;

    /**
     * This creates a new tuple (isEvaluated=false, expr, env, closure, value=null), which may later
     * be evaluated.
     *
     * @param evalPolicy {@link EvalPolicy}
     * @param type {@link #type}
     * @param optType {@link #optType}
     * @param execFrame {@link #execFrame}
     * @param closure {@link #getClosure()}
     */
    private RPromise(EvalPolicy evalPolicy, PromiseType type, OptType optType, MaterializedFrame execFrame, Closure closure) {
        super(closure.getExpr());
        this.evalPolicy = evalPolicy;
        this.type = type;
        this.optType = optType;
        this.execFrame = execFrame;
        this.closure = closure;
    }

    /**
     * This creates a new tuple (isEvaluated=true, expr, null, null, value), which is already
     * evaluated. Meant to be called via {@link RPromiseFactory#createArgEvaluated(Object)} only!
     *
     *
     * @param evalPolicy {@link EvalPolicy}
     * @param type {@link #type}
     * @param optType {@link #optType}
     * @param expr {@link #getRep()}
     * @param value {@link #value}
     */
    private RPromise(EvalPolicy evalPolicy, PromiseType type, OptType optType, Object expr, Object value) {
        super(expr);
        this.evalPolicy = evalPolicy;
        this.type = type;
        this.optType = optType;
        this.value = value;
        this.isEvaluated = true;
        // Not needed as already evaluated:
        this.execFrame = null;
        this.closure = null;

    }

    /**
     * This creates a new tuple (isEvaluated=false, expr, null, null, value=null). Meant to be
     * called via {@link VarargPromise#VarargPromise(PromiseType, RPromise, Closure)} only!
     *
     * @param evalPolicy {@link EvalPolicy}
     * @param type {@link #type}
     * @param expr {@link #getRep()}
     */
    private RPromise(EvalPolicy evalPolicy, PromiseType type, OptType optType, Object expr) {
        super(expr);
        this.evalPolicy = evalPolicy;
        this.type = type;
        this.optType = optType;
        // Not needed as already evaluated:
        this.execFrame = null;
        this.closure = null;

    }

    /**
     * @param evalPolicy {@link EvalPolicy}
     * @param closure {@link #getClosure()}
     * @return see {@link #RPromise(EvalPolicy, PromiseType, OptType, MaterializedFrame, Closure)}
     */
    public static RPromise create(EvalPolicy evalPolicy, PromiseType type, MaterializedFrame execFrame, Closure closure) {
        assert closure != null;
        assert closure.getExpr() != null;
        return new RPromise(evalPolicy, type, OptType.DEFAULT, execFrame, closure);
    }

    /**
     * This class contains a profile of a specific promise evaluation site, i.e., a specific point
     * in the AST where promises are inspected.
     *
     * This is useful to keep the amount of code included in Truffle compilation for each promise
     * operation to a minimum.
     */
    public static final class PromiseProfile {
        private final ConditionProfile isEvaluatedProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile underEvaluationProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isFrameEnvProfile = ConditionProfile.createBinaryProfile();

        private final ConditionProfile isInlinedProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isDefaultProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isFrameForEnvProfile = ConditionProfile.createBinaryProfile();
        private final ValueProfile optTypeProfile = ValueProfile.createIdentityProfile();

        private final ValueProfile valueProfile = ValueProfile.createClassProfile();

        // Eager
        private final ConditionProfile isDeoptimizedProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile fallbackProfile = BranchProfile.create();
    }

    public final boolean isInlined(PromiseProfile profile) {
        return profile.isInlinedProfile.profile(evalPolicy == EvalPolicy.INLINED);
    }

    /**
     * @return Whether this promise is of {@link #type} {@link PromiseType#ARG_DEFAULT}.
     */
    public final boolean isDefault(PromiseProfile profile) {
        return profile.isDefaultProfile.profile(type == PromiseType.ARG_DEFAULT);
    }

    public final boolean isNonArgument() {
        return type == PromiseType.NO_ARG;
    }

    public final boolean isNullFrame(PromiseProfile profile) {
        return profile.isFrameEnvProfile.profile(execFrame == null);
    }

    public boolean isEvaluated(PromiseProfile profile) {
        return profile.isEvaluatedProfile.profile(isEvaluated);
    }

    /**
     * Evaluates this promise. If it has already been evaluated ({@link #isEvaluated()}),
     * {@link #getValue()} is returned.
     *
     * @param frame The {@link VirtualFrame} in which the evaluation of this promise is forced
     * @return The value this promise resolves to
     */
    public final Object evaluate(VirtualFrame frame, PromiseProfile profile) {
        SourceSection callSrc = null;
        if (frame != null) {
            callSrc = RArguments.getCallSourceSection(frame);
        }
        return doEvaluate(frame, profile, callSrc);
    }

    protected final Object doEvaluate(VirtualFrame frame, PromiseProfile profile, SourceSection callSrc) {
        CompilerAsserts.compilationConstant(profile);
        if (isEvaluated(profile)) {
            return value;
        }

        // Check for dependency cycle
        if (isUnderEvaluation(profile)) {
            // SourceSection callSrc = RArguments.getCallSourceSection(frame);
            throw RError.error(callSrc, RError.Message.PROMISE_CYCLE);
        }

        Object newValue;
        try {
            underEvaluation = true;

            newValue = generateValue(frame, profile, callSrc);

            setValue(newValue, profile);
        } finally {
            underEvaluation = false;
        }
        return newValue;
    }

    /**
     * Used in case the {@link RPromise} is evaluated outside.
     *
     * @param newValue
     */
    public final void setValue(Object newValue, PromiseProfile profile) {
        Object profiledValue = profile.valueProfile.profile(newValue);
        this.value = profiledValue;
        this.isEvaluated = true;

        // TODO Does this apply to other values, too?
        if (profiledValue instanceof RShareable) {
            // set NAMED = 2
            ((RShareable) profiledValue).makeShared();
        }
    }

    /**
     * This method allows subclasses to override the evaluation method easily while maintaining
     * {@link #isEvaluated()} and {@link #underEvaluation} semantics.
     *
     * @param frame The {@link VirtualFrame} of the environment the Promise is forced in
     * @param profile
     * @return The value this Promise represents
     */
    protected final Object generateValue(VirtualFrame frame, PromiseProfile profile, SourceSection callSrc) {
        OptType profiledOptType = profile.optTypeProfile.profile(optType);
        if (profiledOptType == OptType.DEFAULT) {
            return defaultGenerateValue(frame, profile, callSrc);
        } else if (profiledOptType == OptType.PROMISED || profiledOptType == OptType.EAGER) {
            EagerPromise eager = (EagerPromise) this;
            return eager.eagerGenerateValue(frame, profile, callSrc);
        } else if (profiledOptType == OptType.VARARG) {
            VarargPromise var = (VarargPromise) this;
            return var.varargGenerateValue(profile, callSrc);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    protected final Object defaultGenerateValue(VirtualFrame frame, PromiseProfile profile, SourceSection callSrc) {
        // Evaluate this promise's value!
        // Performance: We can use frame directly
        if (!isNullFrame(profile) && !isInOriginFrame(frame, profile)) {
// SourceSection callSrc = frame != null ? RArguments.getCallSourceSection(frame) : null;
            return doEvalArgument(callSrc);
        } else {
            assert isInOriginFrame(frame, profile);
            return doEvalArgument(frame.materialize());
        }
    }

    @TruffleBoundary
    protected final Object doEvalArgument(SourceSection callSrc) {
        assert execFrame != null;
        return RContext.getEngine().evalPromise(this, callSrc);
    }

    @TruffleBoundary
    protected final Object doEvalArgument(MaterializedFrame frame) {
        return RContext.getEngine().evalPromise(this, frame);
    }

    /**
     * This method should be called whenever a {@link RPromise} may not be executed in it's
     * optimized versions anymore, because the assumption, that the Promise is executed in the same
     * stack it has been created in, does not hold anymore. This might be the case in the following
     * situations:
     * <ul>
     * <li>Promise leaves stack via substitute, assign or delayedAssign</li>
     * <li>Promise leaves stack via a frame that is either:
     * <ul>
     * <li>returned</li>
     * <li>super-assigned</li>
     * </ul>
     * </li>
     * <li>Promise leaves stack via a function that contains a frame (see above)</li>
     * <li>Promise leaves stack via frame or function that is passed to assign or delayedAssign</li>
     * </ul>
     *
     * @return <code>true</code> if this was deoptimized before
     */
    public boolean deoptimize(PromiseProfile profile) {
        OptType profiledOptType = profile.optTypeProfile.profile(optType);
        if (profiledOptType == OptType.EAGER || profiledOptType == OptType.PROMISED) {
            EagerPromise eager = (EagerPromise) this;
            return eager.deoptimizeEager(profile);
        } else {
            // Nothing to do here; already the generic and slow RPromise
            return true;
        }
    }

    /**
     * Guarantees, that all {@link RPromise}s in frame are {@link #deoptimize(PromiseProfile)}d and
     * thus are safe to leave it's stack-branch.
     *
     * @param frame The frame to check for {@link RPromise}s to {@link #deoptimize(PromiseProfile)}
     * @return Whether there was at least on {@link RPromise} which needed to be
     *         {@link #deoptimize(PromiseProfile)}d.
     */
    @TruffleBoundary
    // Deoptimize because of frame slot access
    public static boolean deoptimizeFrame(MaterializedFrame frame, PromiseProfile profile) {
        boolean deoptOne = false;
        for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
            // We're only interested in RPromises
            if (slot.getKind() != FrameSlotKind.Object) {
                continue;
            }

            // Try to read it...
            try {
                Object value = frame.getObject(slot);

                // If it's a promise, deoptimize it!
                if (value instanceof RPromise) {
                    deoptOne |= ((RPromise) value).deoptimize(profile);
                }
            } catch (FrameSlotTypeException err) {
                // Should not happen after former check on FrameSlotKind!
                throw RInternalError.shouldNotReachHere();
            }
        }
        return deoptOne;
    }

    /**
     * Materializes {@link #execFrame}. After execution, it is guaranteed to be != <code>null</code>
     *
     * @return Whether it was materialized before
     * @see #execFrame
     * @see #getFrame()
     */
    public boolean materialize(PromiseProfile profile) {
        OptType profiledOptType = profile.optTypeProfile.profile(optType);
        if (profiledOptType == OptType.EAGER || profiledOptType == OptType.PROMISED) {
            EagerPromise eager = (EagerPromise) this;
            return eager.materializeEager();
        } else {
            // Nothing to do here; already the generic and slow RPromise
            return true;
        }
    }

    /**
     * @param obj
     * @param profile
     * @return If obj is a {@link RPromise}, it is evaluated and its result returned
     */
    public static Object checkEvaluate(VirtualFrame frame, Object obj, PromiseProfile profile) {
        if (obj instanceof RPromise) {
            return ((RPromise) obj).evaluate(frame, profile);
        }
        return obj;
    }

    /**
     * @param frame
     * @param profile
     * @return Whether the given {@link RPromise} is in its origin context and thus can be resolved
     *         directly inside the AST.
     */
    public final boolean isInOriginFrame(VirtualFrame frame, PromiseProfile profile) {
        if (isInlined(profile)) {
            return true;
        }

        if (isDefault(profile) && isNullFrame(profile)) {
            return true;
        }

        if (frame == null) {
            return false;
        }
        return profile.isFrameForEnvProfile.profile(frame == execFrame);
    }

    /**
     * @return The representation of expression (a RNode). May contain <code>null</code> if no expr
     *         is provided!
     */
    @Override
    public final Object getRep() {
        return super.getRep();
    }

    /**
     * @return {@link #closure}
     */
    public final Closure getClosure() {
        return closure;
    }

    /**
     * @return {@link #execFrame}. This might be <code>null</code> if
     *         {@link #isEagerPromise(PromiseProfile)} == <code>true</code>!!! Materialize with
     *         {@link #materialize(PromiseProfile)}.
     *
     * @see #materialize(PromiseProfile)
     * @see #execFrame
     */
    public final MaterializedFrame getFrame() {
        return execFrame;
    }

    /**
     * @return The raw {@link #value}.
     */
    public final Object getValue() {
        assert isEvaluated;
        return value;
    }

    /**
     * Returns {@code true} if this promise has been evaluated?
     */
    public final boolean isEvaluated() {
        return isEvaluated;
    }

    public final OptType getOptType() {
        return optType;
    }

    /**
     * @return Whether this is a eagerly evaluated Promise
     */
    public boolean isEagerPromise(PromiseProfile profile) {
        OptType optTypeProfiled = profile.optTypeProfile.profile(optType);
        return optTypeProfiled == OptType.EAGER || optTypeProfiled == OptType.PROMISED || optTypeProfiled == OptType.VARARG;
    }

    /**
     * Used to manipulate {@link #underEvaluation}.
     *
     * @param underEvaluation The new value to set
     */
    public void setUnderEvaluation(boolean underEvaluation) {
        this.underEvaluation = underEvaluation;
    }

    /**
     * @return The state of the {@link #underEvaluation} flag.
     */
    public boolean isUnderEvaluation(PromiseProfile profile) {
        return profile.underEvaluationProfile.profile(underEvaluation);
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return "[" + evalPolicy + ", " + type + ", " + optType + ", " + execFrame + ", expr=" + getRep() + ", " + value + ", " + isEvaluated + "]";
    }

    // TODO @ValueType annotation necessary here?
    private static class EagerPromise extends RPromise {
        protected final Object eagerValue;

        private final Assumption assumption;
        private final int frameId;
        private final EagerFeedback feedback;

        /**
         * Set to <code>true</code> by {@link #deoptimize(PromiseProfile)}. If this is true, the
         * {@link RPromise#execFrame} is guaranteed to be set.
         */
        private boolean deoptimized = false;

        private EagerPromise(PromiseType type, OptType optType, Closure closure, Object eagerValue, Assumption assumption, int nFrameId, EagerFeedback feedback) {
            super(EvalPolicy.PROMISED, type, optType, (MaterializedFrame) null, closure);
            assert type != PromiseType.NO_ARG;
            this.eagerValue = eagerValue;
            this.assumption = assumption;
            this.frameId = nFrameId;
            this.feedback = feedback;
        }

        protected Object eagerGenerateValue(VirtualFrame frame, PromiseProfile profile, SourceSection callSrc) {
            if (profile.isDeoptimizedProfile.profile(deoptimized)) {
                // execFrame already materialized, feedback already given. Now we're a
                // plain'n'simple RPromise
                return super.defaultGenerateValue(frame, profile, callSrc);
            } else if (assumption.isValid()) {
                feedback.onSuccess(this);

                return genEagerValue(profile, callSrc);
            } else {
                profile.fallbackProfile.enter();
                feedback.onFailure(this);

                // Fallback: eager evaluation failed, now take the slow path
                materialize(profile);

                // Call
                return super.defaultGenerateValue(frame, profile, callSrc);
            }
        }

        private Object genEagerValue(PromiseProfile profile, SourceSection callSrc) {
            OptType profieldOptType = profile.optTypeProfile.profile(optType);
            if (profieldOptType == OptType.EAGER) {
                return eagerValue;  // eagerGenEagerValue(frame, profile);
            } else if (profieldOptType == OptType.PROMISED) {
                PromisedPromise p = (PromisedPromise) this;
                return p.promisedGenEagerValue(profile, callSrc);
            } else {
                throw RInternalError.shouldNotReachHere();
            }
        }

        public boolean deoptimizeEager(PromiseProfile profile) {
            if (!deoptimized) {
                deoptimized = true;
                feedback.onFailure(this);
                materialize(profile);
                return false;
            }
            return true;
        }

        public boolean materializeEager() {
            if (execFrame == null) {
                this.execFrame = (MaterializedFrame) Utils.getStackFrame(FrameAccess.MATERIALIZE, frameId);
                return false;
            }
            return true;
        }
    }

    /**
     * A {@link RPromise} implementation that knows that it holds a Promise itself (that it has to
     * respect by evaluating it on {@link #evaluate(VirtualFrame, PromiseProfile)}).
     */
    private static final class VarargPromise extends RPromise {
        private final RPromise vararg;

        private VarargPromise(PromiseType type, RPromise vararg, Closure exprClosure) {
            super(EvalPolicy.PROMISED, type, OptType.VARARG, exprClosure.getExpr());
            this.vararg = vararg;
        }

        @TruffleBoundary
        protected Object varargGenerateValue(PromiseProfile profile, SourceSection callSrc) {
            return vararg.doEvaluate((VirtualFrame) null, profile, callSrc);
        }
    }

    /**
     * TODO Gero comment!
     */
    private static final class PromisedPromise extends EagerPromise {

        public PromisedPromise(PromiseType type, Closure closure, RPromise eagerValue, Assumption assumption, int nFrameId, EagerFeedback feedback) {
            super(type, OptType.PROMISED, closure, eagerValue, assumption, nFrameId, feedback);
        }

        @TruffleBoundary
        protected Object promisedGenEagerValue(PromiseProfile profile, SourceSection callSrc) {
            RPromise promisedPromise = (RPromise) eagerValue;
            return promisedPromise.doEvaluate((VirtualFrame) null, profile, callSrc);
        }
    }

    public interface EagerFeedback {
        void onSuccess(RPromise promise);

        void onFailure(RPromise promise);
    }

    /**
     * A factory which produces instances of {@link RPromise}.
     *
     * @see RPromiseFactory#createPromise(MaterializedFrame)
     * @see RPromiseFactory#createPromiseDefault()
     * @see RPromiseFactory#createArgEvaluated(Object)
     */
    public static final class RPromiseFactory {
        private final Closure exprClosure;
        private final Closure defaultClosure;
        private final EvalPolicy evalPolicy;
        private final PromiseType type;

        public static RPromiseFactory create(PromiseType type, Closure rep, Closure defaultExpr) {
            return new RPromiseFactory(EvalPolicy.PROMISED, type, rep, defaultExpr);
        }

        /**
         * Create the promise with a representation that allows evaluation later in the "current"
         * frame. The frame may need to be set if the promise is passed as an argument to another
         * function.
         */
        public static RPromiseFactory create(EvalPolicy evalPolicy, PromiseType type, Closure suppliedClosure, Closure defaultClosure) {
            return new RPromiseFactory(evalPolicy, type, suppliedClosure, defaultClosure);
        }

        private RPromiseFactory(EvalPolicy evalPolicy, PromiseType type, Closure suppliedClosure, Closure defaultClosure) {
            this.evalPolicy = evalPolicy;
            this.type = type;
            this.exprClosure = suppliedClosure;
            this.defaultClosure = defaultClosure;
        }

        /**
         * @return A {@link RPromise} from the given parameters
         */
        public RPromise createPromise(MaterializedFrame frame) {
            return RPromise.create(evalPolicy, type, frame, exprClosure);
        }

        /**
         * Uses this {@link RPromiseFactory} to not create a {@link RPromise} of type
         * {@link PromiseType#ARG_SUPPLIED}, but one of type {@link PromiseType#ARG_DEFAULT}
         * instead!
         *
         * @return A {@link RPromise} with {@link PromiseType#ARG_DEFAULT} and no
         *         {@link REnvironment} set!
         */
        public RPromise createPromiseDefault() {
            return RPromise.create(evalPolicy, PromiseType.ARG_DEFAULT, null, defaultClosure);
        }

        /**
         * @param argumentValue The already evaluated value of the argument.
         *            <code>RMissing.instance</code> denotes 'argument not supplied', aka.
         *            'missing'.
         * @return A {@link RPromise} whose argument has already been evaluated
         */
        public RPromise createArgEvaluated(Object argumentValue) {
            return new RPromise(evalPolicy, type, OptType.DEFAULT, exprClosure.getExpr(), argumentValue);
        }

        /**
         * @param eagerValue The eagerly evaluated value
         * @param assumption The {@link Assumption} that eagerValue is still valid
         * @param feedback The {@link EagerFeedback} to notify whether the {@link Assumption} hold
         *            until evaluation
         * @return An {@link EagerPromise}
         */
        public RPromise createEagerSuppliedPromise(Object eagerValue, Assumption assumption, int nFrameId, EagerFeedback feedback) {
            return new EagerPromise(type, OptType.EAGER, exprClosure, eagerValue, assumption, nFrameId, feedback);
        }

        public RPromise createPromisedPromise(RPromise promisedPromise, Assumption assumption, int nFrameId, EagerFeedback feedback) {
            return new PromisedPromise(type, exprClosure, promisedPromise, assumption, nFrameId, feedback);
        }

        /**
         * @param eagerValue The eagerly evaluated value
         * @param assumption The {@link Assumption} that eagerValue is still valid
         * @param feedback The {@link EagerFeedback} to notify whether the {@link Assumption} hold
         *            until evaluation
         * @return An {@link EagerPromise}
         */
        public RPromise createEagerDefaultPromise(Object eagerValue, Assumption assumption, int nFrameId, EagerFeedback feedback) {
            return new EagerPromise(type, OptType.EAGER, defaultClosure, eagerValue, assumption, nFrameId, feedback);
        }

        public RPromise createVarargPromise(RPromise promisedVararg) {
            return new VarargPromise(type, promisedVararg, exprClosure);
        }

        public Object getExpr() {
            if (exprClosure == null) {
                return null;
            }
            return exprClosure.getExpr();
        }

        public Object getDefaultExpr() {
            if (defaultClosure == null) {
                return null;
            }
            return defaultClosure.getExpr();
        }

        public EvalPolicy getEvalPolicy() {
            return evalPolicy;
        }

        public PromiseType getType() {
            return type;
        }
    }

    public static final class Closure {
        private RootCallTarget callTarget;
        private final Object expr;

        private Closure(Object expr) {
            this.expr = expr;
        }

        public static Closure create(Object expr) {
            return new Closure(expr);
        }

        public RootCallTarget getCallTarget() {
            if (callTarget == null) {
                // Create lazily, as it is not needed at all for INLINED promises!
                callTarget = generateCallTarget(expr);
            }
            return callTarget;
        }

        @TruffleBoundary
        private static RootCallTarget generateCallTarget(Object expr) {
            return RContext.getEngine().makeCallTarget(expr, CLOSURE_WRAPPER_NAME);
        }

        public Object getExpr() {
            return expr;
        }
    }
}
