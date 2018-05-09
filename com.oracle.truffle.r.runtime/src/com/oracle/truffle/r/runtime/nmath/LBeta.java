/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
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

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.gammafn;
import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgamma;
import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgammacor;
import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgammafn;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_LN_SQRT_2PI;

public final class LBeta {
    public static double lbeta(double a, double b) {
        double corr;
        double p;
        double q;

        if (Double.isNaN(a) || Double.isNaN(b)) {
            return a + b;
        }

        p = q = a;
        if (b < p) {
            p = b;
            /* := min(a,b) */ }
        if (b > q) {
            q = b;
            /* := max(a,b) */ }

        /* both arguments must be >= 0 */
        if (p < 0) {
            return RMathError.defaultError();
        } else if (p == 0) {
            return Double.POSITIVE_INFINITY;
        } else if (!Double.isFinite(q)) { /* q == +Inf */
            return Double.NEGATIVE_INFINITY;
        }

        if (p >= 10) {
            /* p and q are big. */
            corr = lgammacor(p) + lgammacor(q) - lgammacor(p + q);
            return Math.log(q) * -0.5 + M_LN_SQRT_2PI + corr + (p - 0.5) * Math.log(p / (p + q)) + q * Math.log1p(-p / (p + q));
        } else if (q >= 10) {
            /* p is small, but q is big. */
            corr = lgammacor(q) - lgammacor(p + q);
            return lgammafn(p) + corr + p - p * Math.log(p + q) + (q - 0.5) * Math.log1p(-p / (p + q));
        } else {
            /* p and q are small: p <= q < 10. */
            /* R change for very small args */
            if (p < 1e-306) {
                return lgamma(p) + (lgamma(q) - lgamma(p + q));
            } else {
                return Math.log(gammafn(p) * (gammafn(q) / gammafn(p + q)));
            }
        }
    }
}
