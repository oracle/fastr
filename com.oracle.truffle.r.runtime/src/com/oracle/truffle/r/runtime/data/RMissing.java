/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * See singleton {@link #instance}.
 */
public final class RMissing extends RScalar {

    /**
     * This object denotes a missing argument in a function call.
     */
    public static final RMissing instance = new RMissing();

    private RMissing() {
    }

    /**
     * This function determines, of an arguments value - given as 'value' - is missing. An argument
     * is missing, when it has not been provided to the current function call, OR if the value that
     * has been provided once was a missing argument. (cp. R language definition and Internals 1.5.1
     * Missingness).
     *
     * @param frame The frame where to eventually evaluate the promise in
     * @param value The value that should be examined
     * @return <code>true</code> iff this value is 'missing' in the definition of R
     */
    public static boolean isMissing(VirtualFrame frame, Object value) {
        if (value == instance) {
            return true;
        }

        // This might be a promise...
        if (value instanceof RPromise) {
            return isMissing(frame, (RPromise) value);
        }
        return false;
    }

    /**
     * @param frame The frame where to eventually evaluate the promise in
     * @param promise The {@link RPromise} that might evaluate to be 'missing'
     * @return See {@link #isMissing(VirtualFrame, Object)}
     */
    public static boolean isMissing(VirtualFrame frame, RPromise promise) {
        // Is missing bit set?
        if (promise.isMissing()) {
            return true;
        }

        // We also have to evaluate and check Promise value!
        return promise.evaluate(frame) == instance;
    }

    @Override
    public String toString() {
        return "missing";
    }
}
