/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.runtime.nmath;

import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_MIN;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_LOG10_2;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;

public final class Mach {
    private Mach() {
        // only static members
    }

    // Rf_d1mach transcribed from d1mach.c
    public static double d1mach(int i) {
        switch (i) {
            case 1:
                return DBL_MIN;
            case 2:
                return Double.MAX_VALUE;
            case 3:
                // FLT_RADIX ^ - DBL_MANT_DIG for IEEE: = 2^-53 = 1.110223e-16 = .5*DBL_EPSILON
                return 0.5 * RRuntime.EPSILON;
            case 4:
                // FLT_RADIX ^ (1- DBL_MANT_DIG) = for IEEE: = 2^-52 = DBL_EPSILON
                return RRuntime.EPSILON;
            case 5:
                return M_LOG10_2;
            default:
                return 0.0;
        }
    }

    // Rf_i1mach transcribed from i1mach.c
    public static int i1mach(int i) {
        switch (i) {
            case 1:
                return 5;
            case 2:
                return 6;
            case 3:
                return 0;
            case 4:
                return 0;

            // case 5: return CHAR_BIT * sizeof(int);
            // case 6: return sizeof(int)/sizeof(char);

            case 7:
                return 2;
            // case 8: return CHAR_BIT * sizeof(int) - 1;
            case 9:
                return Integer.MAX_VALUE;

            // case 10: return FLT_RADIX;
            //
            // case 11: return FLT_MANT_DIG;
            // case 12: return FLT_MIN_EXP;
            // case 13: return FLT_MAX_EXP;
            //
            // case 14: return DBL_MANT_DIG;
            case 15:
                return Double.MIN_EXPONENT;
            case 16:
                return Double.MAX_EXPONENT;

            default:
                throw RInternalError.shouldNotReachHere();
                // return 0;
        }
    }
}
