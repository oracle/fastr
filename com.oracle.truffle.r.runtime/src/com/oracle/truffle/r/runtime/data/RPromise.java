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
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
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

    /**
     * @see EvalPolicy
     */
    protected final EvalPolicy evalPolicy;

    /**
     * The {@link REnvironment} {@link #exprRep} should be evaluated in. For promises associated
     * with environments (frames) that are not top-level. May be <code>null</code>.
     */
    protected final REnvironment env;

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
    RPromise(EvalPolicy evalPolicy, REnvironment env, Object expr) {
        this.evalPolicy = evalPolicy;
        this.env = env;
        this.exprRep = new RLanguageRep(expr);
    }

    /**
     * @param evalPolicy {@link EvalPolicy}
     * @param env {@link #env}
     * @param expr {@link #exprRep}
     * @return see {@link #RPromise(EvalPolicy, REnvironment, Object)}
     */
    public static RPromise create(EvalPolicy evalPolicy, REnvironment env, Object expr) {
        return new RPromise(evalPolicy, env, expr);
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
        value = doEvalArgument();

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

    protected Object doEvalDefaultArgument(VirtualFrame frame) {
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
     * @return Whether this {@link RPromise} is 'missing' (see {@link RMissing} for details). As
     *         this is argument, it cannot be missing!
     */
    public boolean isMissingBitSet() {
        return false;
    }

    /**
     * TODO Gero, add comment!
     */
    public static class RPromiseArg extends RPromise {

        /**
         * The representation of the default argument. May NOT be <code>null</code>, 'no default
         * argument' is denoted by a constant wrapping {@link RMissing#instance}.
         */
        private final RLanguageRep defaultRep;

        /**
         * Whether there was a argument supplied or not. If this is set to <code>false</code>,
         * {@link #exprRep} contains <code>null</code>!
         */
        private final boolean missingBit;

        /**
         * Create the promise with a representation that allows evaluation later in a given frame.
         *
         * @param evalPolicy {@link EvalPolicy}
         * @param env {@link #env}
         * @param argumentRep {@link #getArgumentRep()}
         * @param defaultRep {@link #defaultRep}
         * @param missingBit {@link #missingBit}
         */
        RPromiseArg(EvalPolicy evalPolicy, REnvironment env, Object argumentRep, Object defaultRep, boolean missingBit) {
            super(evalPolicy, env, argumentRep);
            this.defaultRep = new RLanguageRep(defaultRep);
            this.missingBit = missingBit;
        }

        /**
         * @param evalPolicy
         * @param env
         * @param rep
         * @param defaultRep
         * @return see {@link #RPromiseArg(EvalPolicy, REnvironment, Object, Object, boolean)}, with
         *         {@link #missingBit} = <code>rep == null</code>
         */
        public static RPromiseArg create(EvalPolicy evalPolicy, REnvironment env, Object rep, Object defaultRep) {
            return new RPromiseArg(evalPolicy, env, rep, defaultRep, rep == null);
        }

        @Override
        public Object evaluate(VirtualFrame frame) {
            if (isEvaluated) {
                return value;
            }

            // If there has been an argument supplied: Evaluate it!
            Object obj = null;
            if (!missingBit) {
                // Evaluate argument
                obj = doEvalArgument();
            }

            // If there EITHER has been no argument supplied OR its value is missing: Evaluate the
            // default value.
            // (The later case may happen, as passing a missing argument into a function call does
            // _not_ count as evaluation, and forwards the missing state in the form of
            // RMissing_Arg/RMissing.instance).
            if (missingBit || RMissing.isMissing(frame, obj)) {
                // Evaluate default value
                obj = doEvalDefaultArgument(frame);

// // Check if we just evaluated a missing argument
// if (RMissing.isMissing(obj)) {
// // TODO ARGUMENT_MISSING! formal arg name!
// String name = "<no var name>"; // ((Node) defRep).
// throw RError.error(RError.Message.ARGUMENT_MISSING, name);
// }
            }

            // Done. Set values and invalidate, as promise is evaluate-once.
            isEvaluated = true;
            value = obj;
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return obj;
        }

        /**
         * @return {@link #missingBit}
         */
        @Override
        public boolean isMissingBitSet() {
            return missingBit;
        }

        /**
         * @return The argument expr. May contain <code>null</code> if {@link #isMissingBitSet()}!
         */
        public RLanguageRep getArgumentRep() {
            return exprRep;
        }

        /**
         * @return {@link #defaultRep}
         */
        public RLanguageRep getDefaultRep() {
            return defaultRep;
        }

        @SlowPath
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("[ ");
            b.append(evalPolicy).append(", ");
            b.append(value).append(", ");
            b.append(missingBit).append(", ");
            b.append(getArgumentRep().getRep()).append(", ");
            b.append(env).append(", ");
            b.append(defaultRep.getRep()).append(" ]");
            return b.toString();
        }
    }

    /**
     * A {@link RPromise} implementation that already has its argument value evaluated.
     *
     * @see #RPromise(EvalPolicy, Object, Object)
     */
    public static class RPromiseArgEvaluated extends RPromiseArg {
        private final Object argumentValue;

        /**
         * @param evalPolicy {@link EvalPolicy}
         * @param argumentValue The already evaluated value of the supplied argument.
         *            <code>RMissing.instance</code> denotes 'argument not supplied', aka.
         *            'missing'.
         * @param defaultRep
         */
        public RPromiseArgEvaluated(EvalPolicy evalPolicy, Object argumentValue, Object defaultRep) {
            super(evalPolicy, null, null, defaultRep, argumentValue == RMissing.instance);
            this.argumentValue = argumentValue;
        }

        @Override
        protected Object doEvalArgument() {
            return argumentValue;
        }

        @SlowPath
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("[");
            b.append(getEvalPolicy()).append(", ");
            b.append(getValue()).append(", ");
            b.append(isMissingBitSet()).append(", ");
            b.append(argumentValue).append(", ");
            b.append(getDefaultRep().getRep()).append(" ]");
            return b.toString();
        }
    }

    /**
     * A factory which produces instances of {@link RPromiseArg}.
     *
     * @see RPromiseArgFactory#createPromiseArg()
     * @see RPromiseArgFactory#createPromiseArgEvaluated(Object)
     */
    public static final class RPromiseArgFactory {
        private REnvironment env;
        private final Object argument;
        private final Object defaultArg;
        private final EvalPolicy evalPolicy;

        public static RPromiseArgFactory create(REnvironment env, Object rep, Object defaultRep) {
            return new RPromiseArgFactory(EvalPolicy.PROMISED, env, rep, defaultRep);
        }

        /**
         * Create the promise with a representation that allows evaluation later in the "current"
         * frame. The frame may need to be set if the promise is passed as an argument to another
         * function.
         */
        public static RPromiseArgFactory create(EvalPolicy evalPolicy, REnvironment env, Object argument, Object defaultArg) {
            return new RPromiseArgFactory(evalPolicy, env, argument, defaultArg);
        }

        private RPromiseArgFactory(EvalPolicy evalPolicy, REnvironment env, Object argument, Object defaultArg) {
            this.evalPolicy = evalPolicy;
            this.env = env;
            this.argument = argument;
            this.defaultArg = defaultArg;
        }

        /**
         * @return A {@link RPromise} from the given parameters
         */
        public RPromiseArg createPromiseArg() {
            return RPromiseArg.create(evalPolicy, env, argument, defaultArg);
        }

        /**
         * @param argumentValue The already evaluated argument value
         * @return A {@link RPromise} whose supplied argument has already been evaluated
         * @see RPromiseArgEvaluated
         */
        public RPromise createPromiseArgEvaluated(Object argumentValue) {
            return new RPromiseArgEvaluated(evalPolicy, argumentValue, defaultArg);
        }

        public REnvironment getEnv() {
            return env;
        }

        public Object getArgument() {
            return argument;
        }

        public Object getDefaultArg() {
            return defaultArg;
        }

        public EvalPolicy getEvalPolicy() {
            return evalPolicy;
        }
    }
}
