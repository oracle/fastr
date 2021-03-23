/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates
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

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;

// transcribed from arithmetic.c

public final class Arithmetic {
    private Arithmetic() {
        // private
    }

    private static final double q_1_eps = 1 / RRuntime.EPSILON;

    /* Keep myfmod() and myfloor() in step */
    public static double myfmod(double x1, double x2) {
        if (x2 == 0.0) {
            return Double.NaN;
        }
        if (Math.abs(x2) > q_1_eps && Double.isFinite(x1) && Math.abs(x1) <= Math.abs(x2)) {
            return (Math.abs(x1) == Math.abs(x2)) ? 0
                            : ((x1 < 0 && x2 > 0) ||
                                            (x2 < 0 && x1 > 0))
                                                            ? x1 + x2  // differing signs
                                                            : x1;      // "same" signs (incl. 0)
        }
        double q = x1 / x2;
        if (Double.isFinite(q) && (Math.abs(q) > q_1_eps)) {
            RError.warning(RError.SHOW_CALLER, Message.GENERIC, "probable complete loss of accuracy in modulus");
        }
        double tmp = x1 - Math.floor(q) * x2;
        return (tmp - Math.floor(tmp / x2) * x2);
    }

    // R_pow
    public static double pow(double x, double y) {
        /* = x ^ y */
        /*
         * squaring is the most common of the specially handled cases so check for it first.
         */
        if (y == 2.0) {
            return x * x;
        }
        if (x == 1. || y == 0.) {
            return 1.;
        }
        if (x == 0.) {
            if (y > 0.) {
                return 0.;
            } else if (y < 0) {
                return Double.POSITIVE_INFINITY;
            } else {
                return y; /* NA or NaN, we assert */
            }
        }
        if (Double.isFinite(x) && Double.isFinite(y)) {
            /*
             * There was a special case for y == 0.5 here, but gcc 4.3.0 -g -O2 mis-compiled it.
             * Showed up with 100^0.5 as 3.162278, example(pbirthday) failed.
             */
            return Math.pow(x, y);
        }
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return x + y;
        }
        if (!Double.isFinite(x)) {
            if (x > 0) {
                /* Inf ^ y */
                return (y < 0.) ? 0. : Double.POSITIVE_INFINITY;
            } else { /* (-Inf) ^ y */
                if (Double.isFinite(y) && y == Math.floor(y)) {
                    /* (-Inf) ^ n */
                    return (y < 0.) ? 0. : (myfmod(y, 2.) != 0 ? x : -x);
                }
            }
        }
        if (!Double.isFinite(y)) {
            if (x >= 0) {
                if (y > 0) {
                    /* y == +Inf */
                    return (x >= 1) ? Double.POSITIVE_INFINITY : 0.;
                } else {
                    /* y == -Inf */
                    return (x < 1) ? Double.POSITIVE_INFINITY : 0.;
                }
            }
        }
        return Double.NaN; // all other cases: (-Inf)^{+-Inf, non-int}; (neg)^{+-Inf}
    }

    // R_POW
    private static double pow2(double x, double y) {
        /* handle x ^ 2 inline */
        return y == 2.0 ? x * x : pow(x, y);
    }

    // R_pow_di
    public static double powDi(double initialX, int initialN) {
        double x = initialX;
        int n = initialN;
        double xn = 1.0;

        if (Double.isNaN(x)) {
            return x;
        }
        if (n == RRuntime.INT_NA) {
            return RRuntime.DOUBLE_NA;
        }

        if (n != 0) {
            if (!Double.isFinite(x)) {
                return pow2(x, n);
            }

            boolean isNeg = (n < 0);
            if (isNeg) {
                n = -n;
            }
            for (;;) {
                // Note: the octal literal is intentional
                if ((n & 01) != 0) {
                    xn *= x;
                }
                if ((n >>= 1) != 0) {
                    x *= x;
                } else {
                    break;
                }
            }
            if (isNeg) {
                xn = 1. / xn;
            }
        }
        return xn;
    }
}
