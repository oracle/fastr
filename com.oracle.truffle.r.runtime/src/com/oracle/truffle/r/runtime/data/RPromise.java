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
         * This policy is use for arguments that are inlined for the use in builtins that are
         * built-into FastR, written in Java and thus evaluated differently then builtins
         * implemented in R.<br/>
         * Promises with this policy are evaluated every time they are executed.
         */
        RAW,

        /**
         * This promise is an actual promise! It's value won't get evaluated until it's read.
         */
        PROMISED,

        /**
         * This is used for maintaining old, strict argument evaluation semantics. Arguments are
         * fully evaluated before executing the actual function.
         */
        STRICT;
    }

    public enum PromiseType {
        ARG_SUPPLIED,
        ARG_DEFAULT,
        NO_ARG;
    }

    /**
     * @see EvalPolicy
     */
    protected final EvalPolicy evalPolicy;

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
        if (env != null && frame != RArguments.getEnvironment(frame)) {
            if (exprRep.getRep() == null) {
                System.out.print("as");
            }
            value = doEvalArgument();
        } else {
            assert type == PromiseType.ARG_DEFAULT;
            value = doEvalArgument(frame);
        }

        isEvaluated = true;
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
     * @return {@link #evalPolicy}
     */
    public EvalPolicy getEvalPolicy() {
        return evalPolicy;
    }

    /**
     * @return TODO Gero, add comment!
     */
    public PromiseType getType() {
        return type;
    }

    /**
     * @return {@link #env}
     */
    public REnvironment getEnv() {
        return env;
    }

    /**
     * TODO Gero, add comment!
     *
     * @param newEnv
     */
    public void updateEnv(REnvironment newEnv) {
        assert type == PromiseType.ARG_DEFAULT;
        if (env == null) {
            env = newEnv;
        }
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
     * @return Whether this {@link RPromise} is 'missing' (see {@link RMissing} for details). As
     *         this is argument, it cannot be missing!
     */
    public boolean isMissing() {
        return type == PromiseType.ARG_DEFAULT;
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
