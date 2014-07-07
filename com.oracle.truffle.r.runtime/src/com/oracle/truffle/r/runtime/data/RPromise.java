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
     * For promises associated with environments (frames) that are not top-level.
     */
    private REnvironment env;
    /**
     * When {@code null} the promise has not been evaluated.
     */
    private Object value;

    /**
     * Create the promise with a representation that allows evaluation later in the "current" frame.
     * The frame may need to be set if the promise is passed as an argument to another function.
     */
    RPromise(Object rep) {
        this(rep, null);
    }

    /**
     * Create the promise with a representation that allows evaluation later in a given frame.
     */
    RPromise(Object rep, REnvironment env) {
        super(rep);
        this.env = env;
    }

    public REnvironment getEnv() {
        return env;
    }

    public Object force() {
        return getValue();
    }

    /**
     * Get the value of the promise, evaluating it if necessary in the associated environment. A
     * promise is evaluate-once.
     */
    public Object getValue() {
        if (value == null) {
            assert env != null;
            try {
                value = RContext.getEngine().evalPromise(this);
            } catch (RError e) {
                value = e;
                throw e;
            }
        }
        return value;
    }

    /**
     * Get the value of the promise, evaluating it if necessary in the given {@link VirtualFrame}. A
     * promise is evaluate-once.
     */
    public Object getValue(VirtualFrame frame) {
        if (value == null) {
            try {
                value = RContext.getEngine().evalPromise(this, frame);
            } catch (RError e) {
                value = e;
                throw e;
            }
        }
        return value;
    }

    /**
     * Returns {@code true} if this promise has been evaluated?
     */
    public boolean hasValue() {
        return value != null;
    }
}
