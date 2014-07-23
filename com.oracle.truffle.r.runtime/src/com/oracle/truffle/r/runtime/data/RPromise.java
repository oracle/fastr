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
     * {@link EvalPolicy#RAW}<br/>
     * {@link EvalPolicy#PROMISED}<br/>
     * {@link EvalPolicy#STRICT}<br/>
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
    private final EvalPolicy evalPolicy;

    /**
     * The {@link REnvironment} {@link #argumentRep} should be evaluated in. For promises associated
     * with environments (frames) that are not top-level. May be <code>null</code>.
     */
    private REnvironment env;

    /**
     * The representation of the supplied argument. May contain <code>null</code> if
     * <code>{@link #missingBit} == true</code>!
     */
    private final RLanguageRep argumentRep;

    /**
     * The representation of the default argument. May NOT be <code>null</code>, 'no default
     * argument' is denoted by a constant wrapping {@link RMissing#instance}.
     */
    private final RLanguageRep defaultRep;

    /**
     * Whether there was a argument supplied or not. If this is set to <code>false</code>,
     * {@link #argumentRep} contains <code>null</code>!
     */
    private final boolean missingBit;

    /**
     * When {@code null} the promise has not been evaluated.
     */
    private Object value = null;

    /**
     * A flag use by {@link #evaluate(VirtualFrame)} to
     */
    @CompilationFinal private boolean isEvaluated = false;

    /**
     * Create the promise with a representation that allows evaluation later in a given frame.
     *
     * @param evalPolicy {@link EvalPolicy}
     * @param env {@link #env}
     * @param argumentRep {@link #argumentRep}
     * @param defaultRep {@link #defaultRep}
     * @param missingBit {@link #missingBit}
     */
    RPromise(EvalPolicy evalPolicy, REnvironment env, Object argumentRep, Object defaultRep, boolean missingBit) {
        this.evalPolicy = evalPolicy;
        this.env = env;
        this.argumentRep = new RLanguageRep(argumentRep);
        this.defaultRep = new RLanguageRep(defaultRep);
        this.missingBit = missingBit;
    }

    /**
     * @param evalPolicy
     * @param env
     * @param rep
     * @param defaultRep
     * @return see {@link #RPromise(EvalPolicy, REnvironment, Object, Object, boolean)}, with
     *         {@link #missingBit} = <code>rep == null</code>
     */
    public static RPromise create(EvalPolicy evalPolicy, REnvironment env, Object rep, Object defaultRep) {
        return new RPromise(evalPolicy, env, rep, defaultRep, rep == null);
    }

    /**
     * Evaluates this promise. If it has already been evaluated ({@link #isEvaluated()}),
     * {@link #getValue()} is returned.
     *
     * @param frame The {@link VirtualFrame} which is use for evaluation of {@link #defaultRep} if
     *            necessary
     * @return The value this promise resolves to
     */
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
        if (missingBit || RMissing.isMissing(obj)) {
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
     * @return {@link #missingBit}
     */
    public boolean isMissingBitSet() {
        return missingBit;
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
     * @return {@link #argumentRep}
     */
    public RLanguageRep getArgumentRep() {
        return argumentRep;
    }

    /**
     * @return {@link #argumentRep}
     */
    public Object getRep() {
        return argumentRep.getRep();
    }

    /**
     * @return {@link #defaultRep}
     */
    public RLanguageRep getDefaultRep() {
        return defaultRep;
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

    @SlowPath
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("[ ");
        b.append(evalPolicy).append(", ");
        b.append(value).append(", ");
        b.append(missingBit).append(", ");
        b.append(argumentRep.getRep()).append(", ");
        b.append(env).append(", ");
        b.append(defaultRep.getRep()).append(" ]");
        return b.toString();
    }

    /**
     * A {@link RPromise} implementation that already has its argument value evaluated
     *
     * @see #RPromise(EvalPolicy, Object, Object)
     */
    public static class RPromiseArgEvaluated extends RPromise {
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
     * A factory which produces instances of {@link RPromise}.
     *
     * @see RPromiseFactory#createPromise()
     * @see RPromiseFactory#createPromiseArgEvaluated(Object)
     */
    public static class RPromiseFactory {
        private REnvironment env;
        private final Object argument;
        private final Object defaultArg;
        private final EvalPolicy evalPolicy;

        public static RPromiseFactory create(REnvironment env, Object rep, Object defaultRep) {
            return new RPromiseFactory(EvalPolicy.PROMISED, env, rep, defaultRep);
        }

        /**
         * Create the promise with a representation that allows evaluation later in the "current"
         * frame. The frame may need to be set if the promise is passed as an argument to another
         * function.
         */
        public static RPromiseFactory create(EvalPolicy evalPolicy, REnvironment env, Object argument, Object defaultArg) {
            return new RPromiseFactory(evalPolicy, env, argument, defaultArg);
        }

        private RPromiseFactory(EvalPolicy evalPolicy, REnvironment env, Object argument, Object defaultArg) {
            this.evalPolicy = evalPolicy;
            this.env = env;
            this.argument = argument;
            this.defaultArg = defaultArg;
        }

        /**
         * @return A {@link RPromise} from the given parameters
         */
        public RPromise createPromise() {
            return RPromise.create(evalPolicy, env, argument, defaultArg);
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
