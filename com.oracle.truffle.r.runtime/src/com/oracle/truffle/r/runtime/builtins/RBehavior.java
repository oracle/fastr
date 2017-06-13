/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.builtins;

import com.oracle.truffle.r.runtime.context.RContext;

/**
 * The different ways in which an {@link RBuiltin} interacts with its parameters and the rest of the
 * system.
 */
public enum RBehavior {
    /**
     * This builtin always returns the same result or raises the same error if it is called with the
     * same arguments. It cannot depend on any external state, on the frame or on IO input.
     */
    PURE_ARITHMETIC(true), // the length of the result is the maximum of all inputs
    PURE_SUMMARY(true), // the length of the result is exactly one
    PURE_SUBSET(true), // the length of the result depends on the inputs
    PURE_SUBSCRIPT(true), // the length of the result is exactly one
    PURE(true), // unknown result length
    /**
     * This builtin performs IO operations.
     */
    IO,
    /**
     * This builtin reads from the frame, but does not depend on any other state. It will return the
     * same result or raise the same error if called with the same arguments, as long as the frame
     * is not changed.
     */
    READS_FRAME,
    /**
     * This builtin modifies the frame, changing values or other state that is stored in the frame.
     * It also depends on the frame, similar to {@link #READS_FRAME}.
     */
    MODIFIES_FRAME,
    /**
     * This builtin reads from the global state ({@link RContext}, etc.), but not the frame. It will
     * return the same result or raise the same error if called with the same arguments, as long as
     * the global state is not changed.
     */
    READS_STATE,
    /**
     * This builtin modifies the global state ({@link RContext}, etc.), but not the frame. It also
     * depends on the global state, similar to {@link #READS_STATE}.
     */
    MODIFIES_STATE,
    /**
     * This builtin has arbitrary effects on the global state, on IO components, the frame, and the
     * AST itself. It also depends on the global state, IO and the frame.
     */
    COMPLEX;

    private final boolean pure;

    RBehavior() {
        this(false);
    }

    RBehavior(boolean pure) {
        this.pure = pure;
    }

    public boolean isPure() {
        return pure;
    }
}
