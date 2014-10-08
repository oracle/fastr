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
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
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

    public static final String CLOSURE_WRAPPER_NAME = new String("<promise>");

    /**
     * @see EvalPolicy
     */
    protected final EvalPolicy evalPolicy;

    /**
     * @see PromiseType
     */
    protected final PromiseType type;

    protected MaterializedFrame execFrame;

    /**
     * Might not be <code>null</code>
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
     * @param closure {@link #getClosure()}
     */
    private RPromise(EvalPolicy evalPolicy, PromiseType type, MaterializedFrame execFrame, Closure closure) {
        super(closure.getExpr());
        this.evalPolicy = evalPolicy;
        this.type = type;
        this.execFrame = execFrame;
        this.closure = closure;
    }

    /**
     * This creates a new tuple (isEvaluated=true, expr, null, null, value), which is already
     * evaluated. Meant to be called via {@link RPromiseFactory#createSuppliedArgEvaluated(Object)}
     * only!
     *
     * @param evalPolicy {@link EvalPolicy}
     * @param expr
     */
    private RPromise(EvalPolicy evalPolicy, PromiseType type, Object expr, Object value) {
        super(expr);
        this.evalPolicy = evalPolicy;
        this.type = type;
        this.value = value;
        this.isEvaluated = true;
        // Not needed as already evaluated:
        this.execFrame = null;
        this.closure = null;

    }

    /**
     * @param evalPolicy {@link EvalPolicy}
     * @param closure {@link #getClosure()}
     * @return see {@link #RPromise(EvalPolicy, PromiseType, MaterializedFrame, Closure)}
     */
    public static RPromise create(EvalPolicy evalPolicy, PromiseType type, MaterializedFrame execFrame, Closure closure) {
        assert closure != null;
        assert closure.getExpr() != null;
        return new RPromise(evalPolicy, type, execFrame, closure);
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
        private final ConditionProfile isEagerPromise = ConditionProfile.createBinaryProfile();
    }

    public boolean isInlined(PromiseProfile profile) {
        return profile.isInlinedProfile.profile(evalPolicy == EvalPolicy.INLINED);
    }

    /**
     * @return Whether this promise is of {@link #type} {@link PromiseType#ARG_DEFAULT}.
     */
    public boolean isDefault(PromiseProfile profile) {
        return profile.isDefaultProfile.profile(type == PromiseType.ARG_DEFAULT);
    }

    public boolean isNonArgument() {
        return type == PromiseType.NO_ARG;
    }

    public boolean isNullFrame(PromiseProfile profile) {
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
    public Object evaluate(VirtualFrame frame, PromiseProfile profile) {
        CompilerAsserts.compilationConstant(profile);
        if (isEvaluated(profile)) {
            return value;
        }

        // Check for dependency cycle
        if (isUnderEvaluation(profile)) {
            SourceSection callSrc = RArguments.getCallSourceSection(frame);
            throw RError.error(callSrc, RError.Message.PROMISE_CYCLE);
        }

        Object newValue;
        try {
            underEvaluation = true;

            // Evaluate this promise's value!
            // Performance: We can use frame directly
            if (!isNullFrame(profile) && !isInOriginFrame(frame, profile)) {
                SourceSection callSrc = frame != null ? RArguments.getCallSourceSection(frame) : null;
                newValue = doEvalArgument(callSrc);
            } else {
                assert isInOriginFrame(frame, profile);
                newValue = doEvalArgument(frame.materialize());
            }

            setValue(newValue);
        } finally {
            underEvaluation = false;
        }
        return newValue;
    }

    /**
     * Used in case the {@link RPromise} is evaluated outside
     *
     * @param newValue
     */
    public void setValue(Object newValue) {
        this.value = newValue;
        this.isEvaluated = true;
        this.execFrame = null; // the frame is no longer needed after execution

        // TODO Does this apply to other values, too?
        if (newValue instanceof RShareable) {
            // set NAMED = 2
            ((RShareable) newValue).makeShared();
        }
    }

    @SlowPath
    protected Object doEvalArgument(SourceSection callSrc) {
        assert execFrame != null;
        return RContext.getEngine().evalPromise(this, callSrc);
    }

    protected Object doEvalArgument(MaterializedFrame frame) {
        return RContext.getEngine().evalPromise(this, frame);
    }

    /**
     * This method is necessary, as we have to create {@link RPromise}s before the actual function
     * call, but the callee frame and environment get created _after_ the call happened. This update
     * has to take place in AccessArgumentNode, just before arguments get stuffed into the fresh
     * environment for the function. Whether a {@link RPromise} needs one is determined by
     * {@link #needsCalleeFrame(PromiseProfile)}!
     *
     * @param newFrame The REnvironment this promise is to be evaluated in
     */
    public void updateFrame(MaterializedFrame newFrame, PromiseProfile profile) {
        assert type == PromiseType.ARG_DEFAULT;
        if (isNullFrame(profile) && !isEvaluated(profile)) {
            execFrame = newFrame;
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
     * Only to be called from AccessArgumentNode, and in combination with
     * {@link #updateFrame(MaterializedFrame,PromiseProfile)}!
     *
     * @return Whether this promise needs a callee environment set (see
     *         {@link #updateFrame(MaterializedFrame,PromiseProfile)})
     */
    public boolean needsCalleeFrame(PromiseProfile profile) {
        return !isInlined(profile) && isDefault(profile) && isNullFrame(profile) && !isEvaluated(profile);
    }

    /**
     * @param frame
     * @param profile
     * @return Whether the given {@link RPromise} is in its origin context and thus can be resolved
     *         directly inside the AST.
     */
    public boolean isInOriginFrame(VirtualFrame frame, PromiseProfile profile) {
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
    public Object getRep() {
        return super.getRep();
    }

    /**
     * @return {@link #closure}
     */
    public Closure getClosure() {
        return closure;
    }

    /**
     * @return {@link #execFrame}
     */
    public MaterializedFrame getFrame() {
        return execFrame;
    }

    /**
     * @return The raw {@link #value}.
     */
    public Object getValue() {
        assert isEvaluated;
        return value;
    }

    /**
     * Returns {@code true} if this promise has been evaluated?
     */
    public boolean isEvaluated() {
        return isEvaluated;
    }

    /**
     * @return Whether this is a eagerly evaluated Promise
     */
    public boolean isEagerPromise(PromiseProfile profile) {
        return profile.isEagerPromise.profile(false);
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
    @SlowPath
    public String toString() {
        return "[" + evalPolicy + ", " + type + ", " + execFrame + ", expr=" + getRep() + ", " + value + ", " + isEvaluated + "]";
    }

    // TODO @ValueType annotation necessary here?
    private static final class EagerPromise extends RPromise {
        private final Object eagerValue;

        private final Assumption assumption;
        // TODO sth to resolve the correct REnvironment! Maybe envprovider useful here?
        private final EagerFeedback feedback;

        private EagerPromise(PromiseType type, Closure closure, Object eagerValue, Assumption assumption, EagerFeedback feedback) {
            super(EvalPolicy.PROMISED, type, null, closure);
            assert type != PromiseType.NO_ARG;
            this.eagerValue = eagerValue;
            this.assumption = assumption;
            this.feedback = feedback;
        }

        @Override
        public Object evaluate(VirtualFrame frame, PromiseProfile profile) {
            if (profile.isEvaluatedProfile.profile(isEvaluated)) {
                return value;
            }

            // Check if assumption is still true
            if (assumption.isValid()) {
                // If yes: return value and notify success!
                feedback.notifySuccess();
                setValue(eagerValue);
            } else {
                // Fallback: eager evaluation failed,
                feedback.notifyFailure();
                this.execFrame = null; // TODO Magically produce Frame!

                // Call
                super.evaluate(null, profile);
            }
            return value;
        }

        @Override
        public boolean needsCalleeFrame(PromiseProfile profile) {
            // In case it is default: We certainly do not need the frame
            return false;
        }

        @Override
        public boolean isEagerPromise(PromiseProfile profile) {
            return profile.isEagerPromise.profile(true);
        }
    }

    public interface EagerFeedback {
        void notifySuccess();

        void notifyFailure();
    }

    /**
     * A factory which produces instances of {@link RPromise}.
     *
     * @see RPromiseFactory#createPromise(MaterializedFrame)
     * @see RPromiseFactory#createPromiseDefault()
     * @see RPromiseFactory#createSuppliedArgEvaluated(Object)
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
            assert type == PromiseType.ARG_SUPPLIED;
            return RPromise.create(evalPolicy, PromiseType.ARG_DEFAULT, null, defaultClosure);
        }

        /**
         * @param argumentValue The already evaluated value of the supplied argument.
         *            <code>RMissing.instance</code> denotes 'argument not supplied', aka.
         *            'missing'.
         * @return A {@link RPromise} whose supplied argument has already been evaluated
         */
        public RPromise createSuppliedArgEvaluated(Object argumentValue) {
            return new RPromise(evalPolicy, type, exprClosure.getExpr(), argumentValue);
        }

        /**
         * @param argumentValue The already evaluated value of the default argument. Only applicable
         *            to eager evaluated constants!!
         * @return A {@link RPromise} whose default argument has already been evaluated
         */
        public RPromise createDefaultArgEvaluated(Object argumentValue) {
            return new RPromise(evalPolicy, type, defaultClosure.getExpr(), argumentValue);
        }

        /**
         * @param eagerValue The eagerly evaluated value
         * @param assumption The {@link Assumption} that eagerValue is still valid
         * @param feedback The {@link EagerFeedback} to notify whether the {@link Assumption} hold
         *            until evaluation
         * @return An {@link EagerPromise}
         */
        public RPromise createEagerSuppliedPromise(Object eagerValue, Assumption assumption, EagerFeedback feedback) {
            return new EagerPromise(type, exprClosure, eagerValue, assumption, feedback);
        }

        /**
         * @param feedback The {@link EagerFeedback} to notify whether the {@link Assumption} hold
         *            until evaluation
         * @return An {@link EagerPromise} (which needs to be fully fixed after the call!)
         */
        public RPromise createEagerDefaultPromise(EagerFeedback feedback) {
            // TODO Does this work??
            return new EagerPromise(type, defaultClosure, null, null, feedback);
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

        @SlowPath
        private static RootCallTarget generateCallTarget(Object expr) {
            return RContext.getEngine().makeCallTarget(expr, CLOSURE_WRAPPER_NAME);
        }

        public Object getExpr() {
            return expr;
        }
    }
}
