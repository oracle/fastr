/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2012, The R Core Team
 * Copyright (c) 2004, The R Foundation
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
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

// transcribed from Rmath.h

public final class MathConstants {
    private MathConstants() {
        // private
    }

    /* ----- The following constants and entry points are part of the R API ---- */

    /* 30 Decimal-place constants */
    /* Computed with bc -l (scale=32; proper round) */

    /* SVID & X/Open Constants */
    /* Names from Solaris math.h */

    // e
    public static final double M_E = 2.718281828459045235360287471353;
    // log2(e)
    public static final double M_LOG2E = 1.442695040888963407359924681002;
    // log10(e)
    public static final double M_LOG10E = 0.434294481903251827651128918917;
    // ln(2)
    public static final double M_LN2 = 0.693147180559945309417232121458;
    // ln(10)
    public static final double M_LN10 = 2.302585092994045684017991454684;
    // pi
    public static final double M_PI = 3.141592653589793238462643383280;
    // 2*pi
    public static final double M_2PI = 6.283185307179586476925286766559;
    // pi/2
    public static final double M_PI_2 = 1.570796326794896619231321691640;
    // pi/4
    public static final double M_PI_4 = 0.785398163397448309615660845820;
    // 1/pi
    public static final double M_1_PI = 0.318309886183790671537767526745;
    // 2/pi
    public static final double M_2_PI = 0.636619772367581343075535053490;
    // 2/sqrt(pi)
    public static final double M_2_SQRTPI = 1.128379167095512573896158903122;
    // sqrt(2)
    public static final double M_SQRT2 = 1.414213562373095048801688724210;
    // 1/sqrt(2)
    public static final double M_SQRT1_2 = 0.707106781186547524400844362105;

    /* R-Specific Constants from dpq.h and Rmath.h and others */
    // sqrt(3)
    public static final double M_SQRT_3 = 1.732050807568877293527446341506;
    // sqrt(32)
    public static final double M_SQRT_32 = 5.656854249492380195206754896838;
    // log10(2)
    public static final double M_LOG10_2 = 0.301029995663981195213738894724;
    // sqrt(pi)
    public static final double M_SQRT_PI = 1.772453850905516027298167483341;
    // 1/sqrt(2pi)
    public static final double M_1_SQRT_2PI = 0.398942280401432677939946059934;
    // sqrt(2/pi)
    public static final double M_SQRT_2dPI = 0.797884560802865355879892119869;
    // log(2*pi)
    public static final double M_LN_2PI = 1.837877066409345483560659472811;
    // log(sqrt(pi)) == log(pi)/2
    public static final double M_LN_SQRT_PI = 0.572364942924700087071713675677;
    // log(sqrt(2*pi)) == log(2*pi)/2
    public static final double M_LN_SQRT_2PI = 0.918938533204672741780329736406;
    // log(sqrt(pi/2)) == log(pi/2)/2
    public static final double M_LN_SQRT_PId2 = 0.225791352644727432363097614947;

    public static final double DBL_MANT_DIG = 53;

    public static final int DBL_MAX_EXP = 1024;

    public static final int DBL_MIN_EXP = -1021;

    public static final double DBL_EPSILON = Math.ulp(1.0);

    /* Max.expon. of 10 (=308.2547) */
    public static final int MAX10E = (int) (DBL_MAX_EXP * M_LOG10_2);

    public static final long LONG_MAX = Long.MAX_VALUE;

    public static final double ML_NAN = Double.NaN;

    public static final double ML_NEGINF = Double.NEGATIVE_INFINITY;

    public static final double ML_POSINF = Double.POSITIVE_INFINITY;

    // Different to Double.MIN_VALUE!
    public static final double DBL_MIN = Double.MIN_NORMAL;

    public static final double DBL_MAX = Double.MAX_VALUE;

    static final double INV_SQRT_2_PI = .398942280401433; /* == 1/sqrt(2*pi); */

    /**
     * Compute the log of a sum from logs of terms, i.e.,
     *
     * log (exp (logx) + exp (logy))
     *
     * without causing overflows and without throwing away large handfuls of accuracy.
     */
    // logspace_add
    public static double logspaceAdd(double logx, double logy) {
        return Math.max(logx, logy) + Math.log1p(Math.exp(-Math.abs(logx - logy)));
    }

    /**
     * Compute the log of a difference from logs of terms, i.e.,
     *
     * log (exp (logx) - exp (logy))
     *
     * without causing overflows and without throwing away large handfuls of accuracy.
     */
    public static double logspaceSub(double logx, double logy) {
        return logx + log1Exp(logy - logx);
    }

    private static double log1Exp(double x) {
        return (x) > -M_LN2 ? Math.log(-Math.expm1(x)) : Math.log1p(-Math.exp(x));
    }

}
