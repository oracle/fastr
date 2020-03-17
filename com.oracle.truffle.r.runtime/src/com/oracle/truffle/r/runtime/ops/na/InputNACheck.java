/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ops.na;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.NodeCloneable;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;

/**
 * Variant of the {@link NACheck} specialized for determining whether a vector data that has written
 * values from some "inputs" should be switched to "incomplete" (
 * {@link VectorDataLibrary#isComplete(Object)} is {@code false}).
 *
 * {@link #getUncached()} is intended for use with Truffle DSL: it returns {@link InputNACheck} that
 * checks nothing, but always returns {@code false} from {@link #neverSeenNA()}.
 *
 * Use {@link #create()} and then optionally {@link #enable(boolean, boolean)} for
 * {@link InputNACheck} that optimizes the checks when possible.
 *
 * Use {@link #disableChecksNeverSeenNA()} to turn the object into a dummy instance configured to do
 * nothing and always return {@code true} from {@link #neverSeenNA()}, which is useful for
 * situations, where the API requires {@link InputNACheck}, but the user wants it to be a no-op and
 * wants to do the {@code NA} checking by other means.
 *
 * Instances of this class should be stored in AST as (compilation) final field or cached parameters
 * of specializations.
 */
public final class InputNACheck extends NodeCloneable {
    // Initial state when "enable" was not called yet,
    // checking NAs is preventive measure for cases when the user forgets to call "enable"
    private static final byte STATE_INITIAL_ENABLED_CHECK = 0;
    // Nothing is checked, neverSeenNA() returns true
    private static final byte STATE_NO_CHECK_NEVER_SEEN_NA = 1;
    // Checks individual values, neverSeenNA() returns true,
    // but any NA value transforms the state to STATE_DISABLED_CHECK_SEEN_NA
    private static final byte STATE_ENABLED_CHECK_NEVER_SEEN_NA = 2;
    // We already saw NA value, all bets are off: no need to check NAs anymore
    private static final byte STATE_DISABLED_CHECK_SEEN_NA = 3;
    // Special state used to denote "no-op" InputNACheck instance
    private static final byte STATE_DISABLED_CHECK_NEVER_SEEN_NA = 4;

    @CompilationFinal private byte state;

    private InputNACheck(byte state) {
        this.state = state;
    }

    public static InputNACheck getUncached() {
        return new InputNACheck(STATE_DISABLED_CHECK_SEEN_NA);
    }

    public static InputNACheck create() {
        return new InputNACheck(STATE_INITIAL_ENABLED_CHECK);
    }

    public void disableChecksNeverSeenNA() {

    }

    /**
     * Communicates the completeness of the destination vector (where we are writing to) and the
     * source of the data. If the completeness of the source (input) is not known, use {@code false}
     * .
     */
    public void enable(boolean destinationIsComplete, boolean sourceIsComplete) {
        if (state == STATE_INITIAL_ENABLED_CHECK) {
            if (!destinationIsComplete) {
                // no need for NA checks, complete flag of destination is false anyway
                setState(STATE_NO_CHECK_NEVER_SEEN_NA);
            } else if (!sourceIsComplete) {
                // source may contain NAs, we need to check
                setState(STATE_ENABLED_CHECK_NEVER_SEEN_NA);
            } else {
                // sourceIsComplete == true, no NAs in source, we're fine
                setState(STATE_NO_CHECK_NEVER_SEEN_NA);
            }
        } else if (state == STATE_NO_CHECK_NEVER_SEEN_NA) {
            if (destinationIsComplete) {
                // destination is complete, now it matters: we need to know if we need to update the
                // complete flag
                // source is not complete: we need to check the individual incoming values
                if (!sourceIsComplete) {
                    setState(STATE_ENABLED_CHECK_NEVER_SEEN_NA);
                }
            }
        }
    }

    public boolean neverSeenNA() {
        return state == STATE_NO_CHECK_NEVER_SEEN_NA ||
                        state == STATE_ENABLED_CHECK_NEVER_SEEN_NA ||
                        state == STATE_DISABLED_CHECK_NEVER_SEEN_NA;
    }

    public byte check(byte value) {
        if (isCheckEnabled()) {
            if (RRuntime.isNA(value)) {
                setState(STATE_DISABLED_CHECK_SEEN_NA);
            }
        }
        return value;
    }

    public int check(int value) {
        if (isCheckEnabled()) {
            if (RRuntime.isNA(value)) {
                setState(STATE_DISABLED_CHECK_SEEN_NA);
            }
        }
        return value;
    }

    public double check(double value) {
        if (isCheckEnabled()) {
            if (RRuntime.isNA(value)) {
                setState(STATE_DISABLED_CHECK_SEEN_NA);
            }
        }
        return value;
    }

    public String check(String value) {
        if (isCheckEnabled()) {
            if (RRuntime.isNA(value)) {
                setState(STATE_DISABLED_CHECK_SEEN_NA);
            }
        }
        return value;
    }

    public RComplex check(RComplex value) {
        if (isCheckEnabled()) {
            if (RRuntime.isNA(value)) {
                setState(STATE_DISABLED_CHECK_SEEN_NA);
            }
        }
        return value;
    }

    private boolean isCheckEnabled() {
        return state == STATE_ENABLED_CHECK_NEVER_SEEN_NA || state == STATE_INITIAL_ENABLED_CHECK;
    }

    private void setState(byte newState) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        state = newState;
    }
}
