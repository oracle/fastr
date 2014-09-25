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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Denotes an R {@code promise}.
 */
@ValueType
public final class RPromise extends RLanguageRep {

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

    /**
     * The {@link REnvironment} {@link #getRep()} should be evaluated in. For promises associated
     * with environments (frames) that are not top-level. May be <code>null</code>.
     */
    protected REnvironment env;

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
     * This creates a new tuple (env, expr), which may later be evaluated.
     *
     * @param evalPolicy {@link EvalPolicy}
     * @param env {@link #env}
     * @param closure {@link #getClosure()}
     */
    private RPromise(EvalPolicy evalPolicy, PromiseType type, REnvironment env, Closure closure) {
        super(closure.getExpr());
        this.evalPolicy = evalPolicy;
        this.type = type;
        this.env = env;
        this.closure = closure;
    }

    /**
     * @param evalPolicy {@link EvalPolicy}
     * @param env {@link #env}
     * @param closure {@link #getClosure()}
     * @return see {@link #RPromise(EvalPolicy, PromiseType, REnvironment, Closure)}
     */
    public static RPromise create(EvalPolicy evalPolicy, PromiseType type, REnvironment env, Closure closure) {
        assert closure != null;
        assert closure.getExpr() != null;
        return new RPromise(evalPolicy, type, env, closure);
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
        private final ConditionProfile isNullEnvProfile = ConditionProfile.createBinaryProfile();

        private final ConditionProfile isInlinedProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isDefaultProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isFrameForEnvProfile = ConditionProfile.createBinaryProfile();
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

    public boolean isNullEnv(PromiseProfile profile) {
        return profile.isNullEnvProfile.profile(env == null);
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
            if (!isNullEnv(profile) && !isInOriginFrame(frame, profile)) {
                SourceSection callSrc = frame != null ? RArguments.getCallSourceSection(frame) : null;
                newValue = doEvalArgument(callSrc);
            } else {
                assert isInOriginFrame(frame, profile);
                newValue = doEvalArgument(frame);
            }

            setValue(newValue);
        } finally {
            underEvaluation = false;
        }
        return value;
    }

    /**
     * Used in case the {@link RPromise} is evaluated outside
     *
     * @param newValue
     */
    public void setValue(Object newValue) {
        this.value = newValue;
        this.isEvaluated = true;
        this.env = null; // REnvironment and associated frame are no longer needed after execution

        // TODO Does this apply to other values, too?
        if (newValue instanceof RVector) {
            // set NAMED = 2
            ((RVector) newValue).makeShared();
        }
    }

    @SlowPath
    protected Object doEvalArgument(SourceSection callSrc) {
        Object result = null;
        assert env != null;
        try {
            result = RContext.getEngine().evalPromise(this, callSrc);
        } catch (RError e) {
            result = e;
            throw e;
        }
        return result;
    }

    protected Object doEvalArgument(VirtualFrame frame) {
        Object result = null;
        try {
            result = RContext.getEngine().evalPromise(this, frame);
        } catch (RError e) {
            result = e;
            throw e;
        }
        return result;
    }

    /**
     * This method is necessary, as we have to create {@link RPromise}s before the actual function
     * call, but the callee frame and environment get created _after_ the call happened. This update
     * has to take place in AccessArgumentNode, just before arguments get stuffed into the fresh
     * environment for the function. Whether a {@link RPromise} needs one is determined by
     * {@link #needsCalleeFrame(PromiseProfile)}!
     *
     * @param newEnv The REnvironment this promise is to be evaluated in
     */
    public void updateEnv(REnvironment newEnv, PromiseProfile profile) {
        assert type == PromiseType.ARG_DEFAULT;
        if (isNullEnv(profile) && !isEvaluated(profile)) {
            env = newEnv;
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
     * {@link #updateEnv(REnvironment,PromiseProfile)}!
     *
     * @return Whether this promise needs a callee environment set (see
     *         {@link #updateEnv(REnvironment,PromiseProfile)})
     */
    public boolean needsCalleeFrame(PromiseProfile profile) {
        return !isInlined(profile) && isDefault(profile) && isNullEnv(profile) && !isEvaluated(profile);
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

        if (isDefault(profile) && isNullEnv(profile)) {
            return true;
        }

        if (frame == null) {
            return false;
        }
        return profile.isFrameForEnvProfile.profile(REnvironment.isFrameForEnv(frame, env));
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
     * @return {@link #env}
     */
    public REnvironment getEnv() {
        return env;
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
        return "[" + evalPolicy + ", " + type + ", " + env + ", expr=" + getRep() + ", " + value + ", " + isEvaluated + "]";
    }

    /**
     * A factory which produces instances of {@link RPromise}.
     *
     * @see RPromiseFactory#createPromise(REnvironment)
     * @see RPromiseFactory#createPromiseDefault()
     * @see RPromiseFactory#createPromiseArgEvaluated(Object)
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
        public RPromise createPromise(REnvironment env) {
            return RPromise.create(evalPolicy, type, env, exprClosure);
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
        public RPromise createPromiseArgEvaluated(Object argumentValue) {
            RPromise result = new RPromise(evalPolicy, type, null, null);
            result.value = argumentValue;
            result.isEvaluated = true;
            return result;
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
