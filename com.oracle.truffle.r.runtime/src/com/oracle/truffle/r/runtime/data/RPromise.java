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
        RAW,

        /**
         * {@link RPromise} are created for every argument. If function is a builtin, its argument
         * semantics are maintained!
         */
        PROMISED,

        /**
         *
         */
        STRICT;
    }

    private final EvalPolicy evalPolicy;
    /**
     * For promises associated with environments (frames) that are not top-level.
     */
    private REnvironment env;
    private final RLanguageRep argumentRep;
    private final RLanguageRep defaultRep;

    /**
     * When {@code null} the promise has not been evaluated.
     */
    private Object value = null;
    private boolean isEvaluated = false;

    /**
     * Create the promise with a representation that allows evaluation later in a given frame.
     */
    RPromise(EvalPolicy evalPolicy, REnvironment env, Object rep, Object defaultRep) {
        this.evalPolicy = evalPolicy;
        this.env = env;
        this.argumentRep = new RLanguageRep(rep);
        this.defaultRep = new RLanguageRep(defaultRep);
    }

    public Object evaluate(VirtualFrame frame) {
        if (isEvaluated) {
            return value;
        }

        Object obj = doEvalArgument();
        if (isSymbolMissing(obj)) {
            obj = doEvalDefaultArgument(frame);
        }

        isEvaluated = true;
        value = obj;
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

    public static boolean isSymbolMissing(Object obj) {
        // TODO Missing!
        return obj == RMissing.instance;
    }

    public EvalPolicy getEvalPolicy() {
        return evalPolicy;
    }

    public REnvironment getEnv() {
        return env;
    }

    public RLanguageRep getArgumentRep() {
        return argumentRep;
    }

    public Object getRep() {
        return argumentRep.getRep();
    }

    public RLanguageRep getDefaultRep() {
        return defaultRep;
    }

    /**
     * Returns {@code true} if this promise has been evaluated?
     */
    public boolean isEvaluated() {
        return isEvaluated;
    }

    static class RPromiseArgEvaluated extends RPromise {
        private final Object argumentValue;

        RPromiseArgEvaluated(EvalPolicy evalPolicy, Object argumentValue, Object defaultRep) {
            super(evalPolicy, null, null, defaultRep);
            this.argumentValue = argumentValue;
        }

        @Override
        protected Object doEvalArgument() {
            return argumentValue;
        }
    }

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

        public RPromise createPromise() {
            return new RPromise(evalPolicy, env, argument, defaultArg);
        }

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
