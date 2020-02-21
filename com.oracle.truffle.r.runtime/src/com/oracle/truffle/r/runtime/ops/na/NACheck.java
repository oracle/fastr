/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RRuntime.isNA;
import static com.oracle.truffle.r.runtime.data.model.RAbstractVector.ENABLE_COMPLETE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

/**
 * Serves as a FastR specific Truffle profile, i.e. it uses {@link CompilerDirectives Truffle
 * compiler directives} to communicate to the compiler that certain code can be omitted from the
 * compilation. Instances of {@link NACheck} should be fields of AST Nodes (this includes creation
 * via {@link com.oracle.truffle.api.dsl.Cached} annotation).
 *
 * Main use-case of {@link NACheck} is to save checks for {@code NA} values inside a loop if we know
 * that we are reading those values from a vector that does not contain any {@code NA} value, which
 * can be determined via {@link RAbstractContainer#isComplete()}. In the following example:
 *
 * <pre>
 * naCheck.enable(vector);
 * for (int i = 0; i < vector.getLength(); i++) {
 *     if (naCheck.check(vector.getDataAt(i)) { ... }
 * }
 * </pre>
 *
 * The {@code if} can be completely eliminated from the loop if all the vectors that were seen
 * during the runtime returned {@code true} from {@link RAbstractContainer#isComplete()}.
 *
 * Common pattern is to use {@link #neverSeenNA()} as a value for the {@code complete} flag of a new
 * vector if whether it contains {@NA} values or not depends on the vector(s) for which we
 * {@link #enable(RAbstractContainer)} the check. Note that in such case the vector may be marked as
 * incomplete even if the current vector for which we enabled the check happens to be complete,
 * because some previous vector seen during runtime wasn't complete and once enabled {@link NACheck}
 * is never "disabled" and stays enabled forever. Marking a vector without any {@code NA}s as
 * incomplete is OK as "incompleteness" gives no guarantees about {@code NA}s in the vector, only
 * completeness does. Example:
 *
 * <pre>
 * naCheck.enable(vector);
 * int[] result = new int[vector.getLength()];
 * for (int i = 0; i < vector.getLength(); i++) {
 *     if (naCheck.check(vector.getDataAt(i)) { result[i] = RRuntime.INT_NA; }
 *     else { result[i] = vector.getDataAt(i) + 1; }
 * }
 * return RDataFactory.createIntVector(result, naCheck.neverSeenNA());
 * </pre>
 *
 * The {@link NACheck} also contains facility for {@code NaN} checks. The trick is that {@code NA}
 * is of the of possible values representing {@code NaN}, so if it is necessary to check for both,
 * the patten is follows:
 *
 * <pre>
 * if (naCheck.checkNAorNan(value)) {
 *     if (naCheck.check(value)) { ...is NA... }
 *     else { ...is NaN... }
 * }
 * </pre>
 *
 * The {@code if} will not be removed from the compilation, because completeness doesn't tell us if
 * the "source" vector contains {@code NaN}s, but {@link NACheck} will make sure the code inside the
 * {@code if} will be replaced with {@code deopt} if we have never seen any {@code NaN}s during the
 * runtime so far.
 */
public final class NACheck {

    private final BranchProfile conversionOverflowReached = BranchProfile.create();

    /**
     * The {@link NACheck} can be in 3 states. {@link #NO_CHECK} means that no incomplete
     * vector/value was ever passed to none of the {@code enable} functions and so {@code check}
     * functions will be no-ops in the compiled code.
     */
    private static final int NO_CHECK = 0;

    /**
     * First time an incomplete vector/value is passed to one of the {@code enable} methods, we
     * change the state to {@link #CHECK_DEOPT}, but only actually deoptimize once one of the
     * {@code check} methods is called.
     */
    private static final int CHECK_DEOPT = 1;

    /**
     * Once one of the {@code check} methods is called and state is {@link #CHECK_DEOPT}, then the
     * {@code check} method calls {@link CompilerDirectives#transferToInterpreterAndInvalidate()}
     * and changes state to {@link #CHECK}.
     */
    private static final int CHECK = 2;

    @CompilationFinal private int state;
    @CompilationFinal private boolean seenNaN;

    private NACheck() {
        // private constructor
    }

    private static final NACheck ENABLED;

    static {
        ENABLED = new NACheck();
        ENABLED.state = CHECK;
        ENABLED.seenNaN = true;
    }

    public static NACheck getEnabled() {
        return ENABLED;
    }

    public static NACheck create() {
        if (ENABLE_COMPLETE) {
            return new NACheck();
        } else {
            // enabled check if always checking NA and always says that it has seen NA/NaN
            return ENABLED;
        }
    }

    public void enable(boolean value) {
        if (state == NO_CHECK && value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            state = CHECK_DEOPT;
        }
    }

    public void enable(byte logical) {
        if (state == NO_CHECK) {
            enable(RRuntime.isNA(logical));
        }
    }

    public void enable(int value) {
        if (state == NO_CHECK) {
            enable(RRuntime.isNA(value));
        }
    }

    public void enable(double value) {
        if (state == NO_CHECK) {
            enable(RRuntime.isNA(value));
        }
    }

    public void enable(RComplex value) {
        if (state == NO_CHECK) {
            enable(value.isNA());
        }
    }

    // XXX TODO: this should be replaced by enable(XXXLibrary library, RAbstractContainer value)
    public void enable(RAbstractContainer value) {
        if (state == NO_CHECK) {
            enable(!value.isComplete());
        }
    }

    public void enable(AbstractContainerLibrary library, RAbstractContainer value) {
        if (state == NO_CHECK) {
            enable(!library.isComplete(value));
        }
    }

    public void enable(VectorDataLibrary library, Object vectorData) {
        if (state == NO_CHECK) {
            enable(!library.isComplete(vectorData));
        }
    }

    public void enable(VectorDataLibrary library, RIntVector vector) {
        if (state == NO_CHECK) {
            enable(!library.isComplete(vector.getData()));
        }
    }

    public void enable(String operand) {
        if (state == NO_CHECK) {
            enable(RRuntime.isNA(operand));
        }
    }

    public boolean check(double value) {
        if (state != NO_CHECK && isNA(value)) {
            if (state == CHECK_DEOPT) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = CHECK;
            }
            return true;
        }
        return false;
    }

    public boolean check(RComplex value) {
        if (state != NO_CHECK && value.isNA()) {
            if (state == CHECK_DEOPT) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = CHECK;
            }
            return true;
        }
        return false;
    }

    public boolean check(double real, double imag) {
        if (state != NO_CHECK && RRuntime.isNA(real, imag)) {
            if (state == CHECK_DEOPT) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = CHECK;
            }
            return true;
        }
        return false;
    }

    public void seenNA() {
        if (state != CHECK) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            state = CHECK;
        }
    }

    public boolean check(int value) {
        if (state != NO_CHECK && isNA(value)) {
            if (state == CHECK_DEOPT) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = CHECK;
            }
            return true;
        }
        return false;
    }

    public boolean check(String value) {
        if (state != NO_CHECK && isNA(value)) {
            if (state == CHECK_DEOPT) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = CHECK;
            }
            return true;
        }
        return false;
    }

    public boolean check(byte value) {
        if (state != NO_CHECK && isNA(value)) {
            if (state == CHECK_DEOPT) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = CHECK;
            }
            return true;
        }
        return false;
    }

    public boolean checkListElement(Object value) {
        assert value != null;
        if (state != NO_CHECK && value == RNull.instance) {
            if (state == CHECK_DEOPT) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                state = CHECK;
            }
            return true;
        }
        return false;
    }

    public int convertLogicalToInt(byte value) {
        if (check(value)) {
            return RRuntime.INT_NA;
        }
        return value;
    }

    public RComplex convertLogicalToComplex(byte value) {
        if (check(value)) {
            return RComplex.createNA();
        }
        return RDataFactory.createComplex(value, 0);
    }

    public double convertIntToDouble(int value) {
        if (check(value)) {
            return RRuntime.DOUBLE_NA;
        }
        return value;
    }

    public RComplex convertDoubleToComplex(double value) {
        if (check(value)) {
            return RComplex.createNA();
        }
        return RDataFactory.createComplex(value, 0);
    }

    public RComplex convertIntToComplex(int value) {
        if (check(value)) {
            return RComplex.createNA();
        }
        return RDataFactory.createComplex(value, 0);
    }

    public boolean isEnabled() {
        return state != NO_CHECK;
    }

    public boolean neverSeenNA() {
        // need to check for both NA and NaN (the latter used for double to int
        // conversions)
        return state != CHECK && !seenNaN;
    }

    public boolean neverSeenNAOrNaN() {
        return neverSeenNA() && seenNaN;
    }

    public boolean hasNeverBeenTrue() {
        return neverSeenNA();
    }

    public double convertLogicalToDouble(byte value) {
        if (check(value)) {
            return RRuntime.DOUBLE_NA;
        }
        return RRuntime.logical2doubleNoCheck(value);
    }

    public double convertStringToDouble(String value) {
        if (check(value)) {
            return RRuntime.DOUBLE_NA;
        }
        double result = RRuntime.string2doubleNoCheck(value);
        check(result); // can be NA
        return result;
    }

    public RComplex convertStringToComplex(String value) {
        if (check(value)) {
            return RComplex.createNA();
        }
        RComplex result = RRuntime.string2complexNoCheck(value);
        check(result); // can be NA
        return result;
    }

    public String convertDoubleToString(double value) {
        if (check(value)) {
            return RRuntime.STRING_NA;
        }
        return RContext.getRRuntimeASTAccess().encodeDouble(value);
    }

    public String convertComplexToString(RComplex value) {
        if (check(value)) {
            return RRuntime.STRING_NA;
        }
        return RContext.getRRuntimeASTAccess().encodeComplex(value);
    }

    public byte convertComplexToLogical(RComplex value) {
        if (check(value)) {
            return RRuntime.LOGICAL_NA;
        }
        return RRuntime.complex2logicalNoCheck(value);
    }

    public boolean checkNAorNaN(double value) {
        if (Double.isNaN(value)) {
            if (!this.seenNaN) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.seenNaN = true;
            }
            return true;
        }
        return false;
    }

    public int convertDoubleToInt(double value) {
        if (checkNAorNaN(value)) {
            return RRuntime.INT_NA;
        }
        int result = (int) value;
        if (result == Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            conversionOverflowReached.enter();
            check(RRuntime.INT_NA); // na encountered
            return RRuntime.INT_NA;
        }
        return result;
    }

    public byte convertIntToLogical(int value) {
        if (check(value)) {
            return RRuntime.LOGICAL_NA;
        }
        return RRuntime.int2logicalNoCheck(value);
    }

    public byte convertDoubleToLogical(double value) {
        if (check(value)) {
            return RRuntime.LOGICAL_NA;
        }
        return RRuntime.double2logicalNoCheck(value);
    }

    public byte convertStringToLogical(String value) {
        if (check(value)) {
            return RRuntime.LOGICAL_NA;
        }
        return RRuntime.string2logicalNoCheck(value);
    }
}
