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
package com.oracle.truffle.r.runtime.ops.na;

import static com.oracle.truffle.r.runtime.RRuntime.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;

public final class NACheck implements RDataCheckClosure {

    private static final int NO_CHECK = 0;
    private static final int CHECK_DEOPT = 1;
    private static final int CHECK = 2;

    @CompilerDirectives.CompilationFinal int state;
    @CompilerDirectives.CompilationFinal boolean conversionOverflowReached;
    @CompilerDirectives.CompilationFinal boolean seenNaN;

    public static NACheck create() {
        return new NACheck();
    }

    public void enable(boolean value) {
        if (state == NO_CHECK && value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            state = CHECK_DEOPT;
        }
    }

    public void enable(byte logical) {
        enable(RRuntime.isNA(logical));
    }

    public void enable(int value) {
        enable(RRuntime.isNA(value));
    }

    public void enable(double value) {
        enable(RRuntime.isNA(value));
    }

    public void enable(RComplex value) {
        enable(value.isNA());
    }

    public void enable(RAbstractVector value) {
        enable(!value.isComplete());
    }

    public void enable(String operand) {
        enable(RRuntime.isNA(operand));
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

    public int convertLogicalToInt(byte value) {
        if (check(value)) {
            return RRuntime.INT_NA;
        }
        return value;
    }

    public RComplex convertLogicalToComplex(byte value) {
        if (check(value)) {
            return RRuntime.createComplexNA();
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
        if (checkNAorNaN(value)) {
            // Special case here NaN does not enable the NA check.
            this.enable(true);
            return RRuntime.createComplexNA();
        }
        return RDataFactory.createComplex(value, 0);
    }

    public RComplex convertIntToComplex(int value) {
        if (check(value)) {
            return RRuntime.createComplexNA();
        }
        return RDataFactory.createComplex(value, 0);
    }

    public boolean neverSeenNA() {
        return state != CHECK;
    }

    public boolean hasNeverBeenTrue() {
        return neverSeenNA();
    }

    public double convertLogicalToDouble(byte value) {
        if (check(value)) {
            return RRuntime.DOUBLE_NA;
        }
        return value;
    }

    @SuppressWarnings("static-method")
    public String convertLogicalToString(byte right) {
        return RRuntime.logicalToString(right);
    }

    @SuppressWarnings("static-method")
    public String convertIntToString(int right) {
        return RRuntime.intToString(right, false);
    }

    @SlowPath
    @SuppressWarnings("static-method")
    public double convertStringToDouble(String value) {
        if (value.startsWith("0x") || value.startsWith("0X")) {
            try {
                return Long.valueOf(value.substring(2), 16);
            } catch (NumberFormatException ex) {
                return RRuntime.DOUBLE_NA;
            }
        } else {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                return RRuntime.DOUBLE_NA;
            }
        }
    }

    @SuppressWarnings("static-method")
    public RComplex convertStringToComplex(String value) {
        try {
            int startIdx = 0;
            char firstChar = value.charAt(0);
            boolean negativeReal = firstChar == '-';
            if (firstChar == '+' || negativeReal) {
                startIdx++;
            }

            int plusIdx = value.indexOf("+", startIdx);
            int minusIdx = value.indexOf("-", startIdx);
            int iIdx = value.indexOf("i", startIdx);
            int signIdx = getSignIdx(plusIdx, minusIdx);
            boolean negativeImaginary = minusIdx > 0;

            double realPart = Double.parseDouble(value.substring(startIdx, signIdx));
            double imaginaryPart = Double.parseDouble(value.substring(signIdx + 1, iIdx));

            return RDataFactory.createComplex(realPart * (negativeReal ? -1 : 1), imaginaryPart * (negativeImaginary ? -1 : 1));
        } catch (NumberFormatException ex) {
            return RRuntime.createComplexNA();
        }
    }

    private static int getSignIdx(int plusIdx, int minusIdx) throws NumberFormatException {
        if (plusIdx < 0) {
            if (minusIdx < 0) {
                throw new NumberFormatException();
            }
            return minusIdx;
        } else {
            if (minusIdx < 0) {
                return plusIdx;
            }
            throw new NumberFormatException();
        }
    }

    public int convertStringToInt(String value) {
        if (check(value)) {
            return RRuntime.INT_NA;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return RRuntime.INT_NA;
        }
    }

    public String convertDoubleToString(double value) {
        if (check(value)) {
            return "NA";
        }
        return RRuntime.doubleToString(value);
    }

    public String convertComplexToString(RComplex value) {
        if (check(value)) {
            return "NA";
        }
        return value.toString();
    }

    public double convertComplexToDouble(RComplex value) {
        if (check(value)) {
            return RRuntime.DOUBLE_NA;
        }
        // TODO Output conversion warning
        return value.getRealPart();
    }

    public byte convertComplexToLogical(RComplex value) {
        if (check(value)) {
            return RRuntime.LOGICAL_NA;
        }
        // TODO Output conversion warning
        return ((int) value.getRealPart()) == 0 ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_TRUE;
    }

    public int convertComplexToInt(RContext context, RComplex right) {
        return convertComplexToInt(context, right, true);
    }

    @SuppressWarnings("static-method")
    public int convertComplexToInt(RContext context, RComplex right, boolean warning) {
        if (warning) {
            context.setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return (int) right.getRealPart();
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

    public int convertDoubleToInt(RContext context, double value) {
        if (checkNAorNaN(value)) {
            return RRuntime.INT_NA;
        }
        int result = (int) value;
        if (result == Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            if (!this.conversionOverflowReached) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.conversionOverflowReached = true;
            }
            context.setEvalWarning(RError.NA_INTRODUCED_COERCION);
            return RRuntime.INT_NA;
        }
        return result;
    }

    public int[] convertDoubleVectorToIntData(RContext context, RDoubleVector vector) {
        int length = vector.getLength();
        int[] result = new int[length];
        boolean warning = false;
        for (int i = 0; i < length; i++) {
            double value = vector.getDataAt(i);
            if (checkNAorNaN(value)) {
                result[i] = RRuntime.INT_NA;
            } else {
                int intValue = (int) value;
                if (intValue == Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                    if (!this.conversionOverflowReached) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        this.conversionOverflowReached = true;
                    }
                    warning = true;
                    intValue = RRuntime.INT_NA;
                }
                result[i] = intValue;
            }
            if (warning) {
                context.setEvalWarning(RError.NA_INTRODUCED_COERCION);
            }
        }
        return result;
    }

    @SuppressWarnings("static-method")
    public byte convertIntToLogical(int value) {
        return RRuntime.int2logical(value);
    }

    @SuppressWarnings("static-method")
    public byte convertDoubleToLogical(double value) {
        return RRuntime.double2logical(value);
    }

    @SuppressWarnings("static-method")
    public byte convertStringToLogical(RContext context, String value) {
        return RRuntime.string2logical(context, value);
    }

}
