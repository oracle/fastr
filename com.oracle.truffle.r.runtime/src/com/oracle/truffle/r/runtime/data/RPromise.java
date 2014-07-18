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
public class RPromise extends RLanguageRep {

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

    /**
     * For promises associated with environments (frames) that are not top-level.
     */
    private REnvironment env;

    /**
     * When {@code null} the promise has not been evaluated.
     */
    private Object value = null;
    private boolean argIsEvaluated = false;
    // TODO @CompilationFinal + invalidate??
    private boolean promiseIsEvaluated = false;

    private final RLanguageRep defaultRep;

    private final EvalPolicy evalPolicy;

    /**
     * Create the promise with a representation that allows evaluation later in the "current" frame.
     * The frame may need to be set if the promise is passed as an argument to another function.
     */
    public RPromise(Object rep, REnvironment env) {
        this(rep, null, env, EvalPolicy.PROMISED);
    }

    /**
     * Create the promise with a representation that allows evaluation later in the "current" frame.
     * The frame may need to be set if the promise is passed as an argument to another function.
     */
    public RPromise(Object rep, Object defaultRep, EvalPolicy evalPolicy) {
        this(rep, defaultRep, null, evalPolicy);
    }

    /**
     * Create the promise with a representation that allows evaluation later in a given frame.
     */
    public RPromise(Object rep, Object defaultRep, REnvironment env, EvalPolicy evalPolicy) {
        super(rep);
        this.env = env;
        this.defaultRep = new RLanguageRep(defaultRep);
        this.evalPolicy = evalPolicy;
    }

    public REnvironment getEnv() {
        return env;
    }

    /**
     * TODO Gero, add comment!
     *
     * @param value
     */
    public void setRawValue(Object value) {
        this.value = value;
        this.argIsEvaluated = true;
    }

    public Object getRawValue() {
        return value;
    }

    public Object evaluate(VirtualFrame argFrame, VirtualFrame defFrame) {
        return doGetValue(argFrame, defFrame);
    }

    private Object doGetValue(VirtualFrame argFrame, VirtualFrame defFrame) {
        if (promiseIsEvaluated) {
            return value;
        }

        if (!argIsEvaluated) {
            value = doEvalValue(argFrame);
            argIsEvaluated = true;
        }

        if (isSymbolMissing(value)) {
            value = doEvalValue(defFrame);
        }
        promiseIsEvaluated = true;

        return value;
    }

    /**
     * @param frame
     * @return TODO Gero, add comment!
     */
    private Object doEvalValue(VirtualFrame frame) {
        Object result = null;
        try {
            if (frame == null) {
                assert env != null;
                result = RContext.getEngine().evalPromise(this);
            } else {
                result = RContext.getEngine().evalPromise(this, frame);
            }
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

    /**
     * Returns {@code true} if this promise has been evaluated?
     */
    public boolean isEvaluated() {
        return promiseIsEvaluated;
    }

    public boolean isArgumentEvaluated() {
        return argIsEvaluated;
    }

    public EvalPolicy getEvalPolicy() {
        return evalPolicy;
    }

    public RLanguageRep getDefaultRep() {
        return defaultRep;
    }
}
