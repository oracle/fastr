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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;

/**
 * Denotes an R {@code promise}. It extends {@link RLanguageRep} with a (lazily) evaluated value.
 */
@com.oracle.truffle.api.CompilerDirectives.ValueType
public class RPromise {

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
        RAW,

        /**
         * This promise is an actual promise! It's value won't get evaluated until it's read.<br/>
         * Promises with this policy are evaluated when forced/used the first time. Evaluation
         * happens inside the caller frame if {@link PromiseType#ARG_SUPPLIED} or inside callee
         * frame if {@link PromiseType#ARG_DEFAULT}.
         */
        PROMISED,

        /**
         * This is used for maintaining old, strict argument evaluation semantics. Arguments are
         * fully evaluated before executing the actual function.<br/>
         * Evaluation happens inside the caller frame if {@link PromiseType#ARG_SUPPLIED} or inside
         * callee frame if {@link PromiseType#ARG_DEFAULT}.
         */
        STRICT;
    }

    /**
     * Different to GNU R, FastR has no additional binding information (a "origin" where the binding
     * is coming from). This enum is meant to substitute this information.
     */
    public enum PromiseType {
        /**
         * This promise is created for an argument that has been supplied to the function call and
         * thus has to be evaluated inside the caller frame
         */
        ARG_SUPPLIED,

        /**
         * This promise is created for an argument that was 'missing' at the function call and thus
         * contains it's default value and has to be evaluated inside the _callee_ frame
         */
        ARG_DEFAULT,

        /**
         * This promise is not a function argument at all. (Created by 'delayedAssign', for
         * example).
         */
        NO_ARG;
    }

    /**
     * @see EvalPolicy
     */
    protected final EvalPolicy evalPolicy;

    /**
     * @see PromiseType
     */
    protected final PromiseType type;

    /**
     * The {@link REnvironment} {@link #exprRep} should be evaluated in. For promises associated
     * with environments (frames) that are not top-level. May be <code>null</code>.
     */
    protected REnvironment env;

    /**
     * The representation of the supplied argument. May contain <code>null</code> if no expr is
     * provided!
     */
    protected final RLanguageRep exprRep;

    /**
     * When {@code null} the promise has not been evaluated.
     */
    protected Object value = null;

    /**
     * A flag to indicate the promise has been evaluated.
     */
    @CompilationFinal protected boolean isEvaluated = false;

    /**
     * A flag which is necessary to avoid cyclic evaluation. Manipulated by
     * {@link #setUnderEvaluation(boolean)} and can by checked via {@link #isUnderEvaluation()}.
     */
    private boolean underEvaluation = false;

    /**
     * This creates a new tuple (env, expr), which may later be evaluated.
     *
     * @param evalPolicy {@link EvalPolicy}
     * @param env {@link #env}
     * @param expr {@link #exprRep}
     */
    RPromise(EvalPolicy evalPolicy, PromiseType type, REnvironment env, Object expr) {
        this.evalPolicy = evalPolicy;
        this.type = type;
        this.env = env;
        this.exprRep = new RLanguageRep(expr);
    }

    /**
     * @param evalPolicy {@link EvalPolicy}
     * @param env {@link #env}
     * @param expr {@link #exprRep}
     * @return see {@link #RPromise(EvalPolicy, PromiseType, REnvironment, Object)}
     */
    public static RPromise create(EvalPolicy evalPolicy, PromiseType type, REnvironment env, Object expr) {
        return new RPromise(evalPolicy, type, env, expr);
    }

    /**
     * Evaluates this promise. If it has already been evaluated ({@link #isEvaluated()}),
     * {@link #getValue()} is returned.
     *
     * @param frame The {@link VirtualFrame} in which the evaluation of this promise is forced
     * @return The value this promise resolves to
     */
    public Object evaluate(VirtualFrame frame) {
        if (isEvaluated) {
            return value;
        }

        // Evaluate this promises value!
        // TODO Performance: We can use frame directly if we are sure that it matches the on in env!
        if (env != null && env != RArguments.getEnvironment(frame)) {
            value = doEvalArgument();
        } else {
            assert type == PromiseType.ARG_DEFAULT;
            value = doEvalArgument(frame);
        }

        isEvaluated = true;
        env = null; // REnvironment and associated frame are no longer needed after execution
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return value;
    }

    protected Object doEvalArgument() {
        Object result = null;
        assert env != null;
        try {
            result = RContext.getEngine().evalPromise(this);
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
     * {@link #needsCalleeFrame()}!
     *
     * @param newEnv The REnvironment this promise is to be evaluated in
     */
    public void updateEnv(REnvironment newEnv) {
        assert type == PromiseType.ARG_DEFAULT;
        if (env == null && !isEvaluated) {
            env = newEnv;
        }
    }

    /**
     * Only to be called from AccessArgumentNode, and in combination with
     * {@link #updateEnv(REnvironment)}!
     *
     * @return Whether this promise needs a callee environment set (see
     *         {@link #updateEnv(REnvironment)})
     */
    public boolean needsCalleeFrame() {
        return evalPolicy != EvalPolicy.STRICT && type == PromiseType.ARG_DEFAULT && env == null && !isEvaluated;
    }

    /**
     * @return {@link #evalPolicy}
     */
    public EvalPolicy getEvalPolicy() {
        return evalPolicy;
    }

    /**
     * @return {@link #type}
     */
    public PromiseType getType() {
        return type;
    }

    /**
     * @return Whether this promise is of {@link #type} {@link PromiseType#ARG_DEFAULT}.
     */
    public boolean isDefaulted() {
        return type == PromiseType.ARG_DEFAULT;
    }

    /**
     * @return {@link #env}
     */
    public REnvironment getEnv() {
        return env;
    }

    /**
     * @return {@link #exprRep}
     */
    public RLanguageRep getExprRep() {
        return exprRep;
    }

    /**
     * @return {@link #exprRep}
     */
    public Object getRep() {
        return exprRep.getRep();
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
    public boolean isUnderEvaluation() {
        return underEvaluation;
    }

    @Override
    public String toString() {
        return "[" + evalPolicy + ", " + type + ", " + env + ", expr=" + exprRep.getRep() + ", " + value + ", " + isEvaluated + "]";
    }

    /**
     * A {@link RPromise} implementation that already has its argument value evaluated.
     *
     * @see #RPromise(EvalPolicy, PromiseType, Object)
     */
    public static class RPromiseArgEvaluated extends RPromise {

        /**
         * @param evalPolicy {@link EvalPolicy}
         * @param argumentValue The already evaluated value of the supplied argument.
         *            <code>RMissing.instance</code> denotes 'argument not supplied', aka.
         *            'missing'.
         */
        public RPromiseArgEvaluated(EvalPolicy evalPolicy, PromiseType type, Object argumentValue) {
            super(evalPolicy, type, null, null);
            this.value = argumentValue;
            this.isEvaluated = true;
        }
    }

    /**
     * A factory which produces instances of {@link RPromise}.
     *
     * @see RPromiseFactory#createPromise(REnvironment)
     * @see RPromiseFactory#createPromiseDefault()
     * @see RPromiseFactory#createPromiseArgEvaluated(Object)
     */
    public static final class RPromiseFactory {
        private final Object expr;
        private final Object defaultExpr;
        private final EvalPolicy evalPolicy;
        private final PromiseType type;

        public static RPromiseFactory create(PromiseType type, Object rep, Object defaultExpr) {
            return new RPromiseFactory(EvalPolicy.PROMISED, type, rep, defaultExpr);
        }

        /**
         * Create the promise with a representation that allows evaluation later in the "current"
         * frame. The frame may need to be set if the promise is passed as an argument to another
         * function.
         */
        public static RPromiseFactory create(EvalPolicy evalPolicy, PromiseType type, Object argument, Object defaultExpr) {
            return new RPromiseFactory(evalPolicy, type, argument, defaultExpr);
        }

        private RPromiseFactory(EvalPolicy evalPolicy, PromiseType type, Object expr, Object defaultExpr) {
            this.evalPolicy = evalPolicy;
            this.type = type;
            this.expr = expr;
            this.defaultExpr = defaultExpr;
        }

        /**
         * @return A {@link RPromise} from the given parameters
         */
        public RPromise createPromise(REnvironment env) {
            return RPromise.create(evalPolicy, type, env, expr);
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
            return RPromise.create(evalPolicy, PromiseType.ARG_DEFAULT, null, defaultExpr);
        }

        /**
         * @param argumentValue The already evaluated argument value
         * @return A {@link RPromise} whose supplied argument has already been evaluated
         * @see RPromiseArgEvaluated
         */
        public RPromise createPromiseArgEvaluated(Object argumentValue) {
            return new RPromiseArgEvaluated(evalPolicy, type, argumentValue);
        }

        public Object getExpr() {
            return expr;
        }

        public Object getDefaultExpr() {
            return defaultExpr;
        }

        public EvalPolicy getEvalPolicy() {
            return evalPolicy;
        }

        public PromiseType getType() {
            return type;
        }
    }
}
