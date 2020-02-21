/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.data.model.RAbstractVector.ENABLE_COMPLETE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.NodeCloneable;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;

/**
 * Variant of the {@link NACheck} specialized for determining whether a vector data that is written
 * values from some "inputs" should be switched to "incomplete" (
 * {@link VectorDataLibrary#isComplete(Object)} is {@code false}).
 *
 * This check is doing noting if all "inputs" it ever saw were "complete". Once it sees "incomplete"
 * input, it starts checking individual values in the {@code check} methods and once it sees actual
 * {@code NA} value it again turns off any checks and {@link #needsResettingCompleteFlag()} will
 * always return {@code true} from then on.
 *
 * Instances of this class should be stored in AST as (compilation) final field or cached parameters
 * of specializations.
 */
public final class InputNACheck extends NodeCloneable {
    // no need to check: we have only seen "complete" "input" vectors so far
    private static final byte STATE_DISABLED_CHECK_NEVER_SEEN_NA = 0;
    // no need to check the input: all the destination vectors were incomplete already
    private static final byte STATE_DISABLED_CHECK_INCOMPLETE_DEST = 1;
    // check: we saw some "incomplete" "input" vectors, but no actual NA value was ever passed to
    // one of the check methods
    private static final byte STATE_ENABLED_CHECK = 2;
    // we already saw NA value, all bets are off: no need to check NAs anymore
    private static final byte STATE_DISABLED_CHECK_SEEN_NA = 3;

    /**
     * Singleton instance of check that is degraded to no checks and
     * {@link #needsResettingCompleteFlag} returning {@code true}.
     */
    public static final InputNACheck SEEN_NA = new InputNACheck(STATE_DISABLED_CHECK_SEEN_NA);

    public static InputNACheck fromNACheck(NACheck naCheck) {
        if (!ENABLE_COMPLETE) {
            return new InputNACheck(STATE_DISABLED_CHECK_SEEN_NA);
        } else if (naCheck.isEnabled()) {
            return new InputNACheck(STATE_DISABLED_CHECK_NEVER_SEEN_NA);
        } else if (naCheck.neverSeenNA()) {
            return new InputNACheck(STATE_ENABLED_CHECK);
        } else {
            return new InputNACheck(STATE_DISABLED_CHECK_SEEN_NA);
        }
    }

    public static InputNACheck create() {
        return new InputNACheck(ENABLE_COMPLETE ? STATE_DISABLED_CHECK_NEVER_SEEN_NA : STATE_DISABLED_CHECK_SEEN_NA);
    }

    public static InputNACheck getUncached() {
        return SEEN_NA;
    }

    @CompilationFinal private byte state;

    private InputNACheck(byte state) {
        this.state = state;
    }

    public InputNACheck enable(boolean destinationIsComplete, boolean inputIsComplete) {
        // So far we've seen only incomplete destination vectors, now we saw first complete one,
        // now it makes sense to check if the input does not contain NAs
        boolean firstCompleteDest = state == STATE_DISABLED_CHECK_INCOMPLETE_DEST && destinationIsComplete;
        // So far all the inputs were complete, so no need to check the elements, now we need to
        boolean firstIncompleteInput = state == STATE_DISABLED_CHECK_NEVER_SEEN_NA && !inputIsComplete;
        if (firstCompleteDest || firstIncompleteInput) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // TODO: to be figured out, which is better:
            // Maybe we can also check the length of the input and do the checks of
            // individual elements only if we've seen only small vectors...
            // state = STATE_ENABLED_CHECK; -- checks every element to avoid reporting hasSeenNA at
            // the end if possible
            state = STATE_DISABLED_CHECK_SEEN_NA; // -- just gives up, but does not pollute the loop
                                                  // body with NA check
        }
        return this;
    }

    /**
     * Fallback to the worst-case: no checks are performed and {@link #needsResettingCompleteFlag()}
     * always returns {@code true}.
     */
    public void disableChecks() {
        if (state != STATE_DISABLED_CHECK_SEEN_NA) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            state = STATE_DISABLED_CHECK_SEEN_NA;
        }
    }

    public boolean needsResettingCompleteFlag() {
        // no need to reset the flag in case when the destination already had complete == false
        return state == STATE_DISABLED_CHECK_SEEN_NA;
    }

    public void check(int value) {
        if (state == STATE_ENABLED_CHECK) {
            if (RRuntime.isNA(value)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = STATE_DISABLED_CHECK_SEEN_NA;
            }
        }
    }

    public void check(double value) {
        if (state == STATE_ENABLED_CHECK) {
            if (RRuntime.isNA(value)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = STATE_DISABLED_CHECK_SEEN_NA;
            }
        }
    }

    public void check(String value) {
        if (state == STATE_ENABLED_CHECK) {
            if (RRuntime.isNA(value)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = STATE_DISABLED_CHECK_SEEN_NA;
            }
        }
    }

    public void check(RComplex value) {
        if (state == STATE_ENABLED_CHECK) {
            if (RRuntime.isNA(value)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = STATE_DISABLED_CHECK_SEEN_NA;
            }
        }
    }

    // assertion checks for: NA input value => NA check must have been enabled

    public void assertInputValue(int value) {
        assert !RRuntime.isNA(value) || state != STATE_DISABLED_CHECK_NEVER_SEEN_NA;
    }

    public void assertInputValue(double value) {
        assert !RRuntime.isNA(value) || state != STATE_DISABLED_CHECK_NEVER_SEEN_NA;
    }

    public void assertInputValue(byte value) {
        assert !RRuntime.isNA(value) || state != STATE_DISABLED_CHECK_NEVER_SEEN_NA;
    }

    public void assertInputValue(String value) {
        assert !RRuntime.isNA(value) || state != STATE_DISABLED_CHECK_NEVER_SEEN_NA;
    }

    public void assertInputValue(RComplex value) {
        assert !RRuntime.isNA(value) || state != STATE_DISABLED_CHECK_NEVER_SEEN_NA;
    }
}
