/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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
