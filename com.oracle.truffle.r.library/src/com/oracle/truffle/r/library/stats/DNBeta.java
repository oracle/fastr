/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-12, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.GammaFunctions.dpoisRaw;

import com.oracle.truffle.r.library.stats.StatsFunctions.Function4_1;

public final class DNBeta implements Function4_1 {
    private static final double eps = 1.e-15;
    private final DBeta dbeta = new DBeta();

    @Override
    public double evaluate(double x, double a, double b, double ncp, boolean giveLog) {
        if (Double.isNaN(x) || Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(ncp)) {
            return x + a + b + ncp;
        }
        if (ncp < 0 || a <= 0 || b <= 0 || !Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(ncp)) {
            return RMathError.defaultError();
        }

        if (x < 0 || x > 1) {
            return DPQ.rd0(giveLog);
        }
        if (ncp == 0) {
            return dbeta.evaluate(x, a, b, giveLog);
        }

        /* New algorithm, starting with *largest* term : */
        double ncp2 = 0.5 * ncp;
        double dx2 = ncp2 * x;
        double d = (dx2 - a - 1) / 2;
        double capD = d * d + dx2 * (a + b) - a;
        int kMax;
        if (capD <= 0) {
            kMax = 0;
        } else {
            capD = Math.ceil(d + Math.sqrt(capD));
            kMax = (capD > 0) ? (int) capD : 0;
        }

        /* The starting "middle term" --- first look at it's log scale: */
        double term = dbeta.evaluate(x, a + kMax, b, /* log = */ true);
        /* LDOUBLE */double pK = dpoisRaw(kMax, ncp2, true);
        if (x == 0. || !Double.isFinite(term) || !Double.isFinite(pK)) {
            /* if term = +Inf */
            return DPQ.rdexp(pK + term, giveLog);
        }

        /*
         * Now if s_k := pK * t_k {here = Math.exp(pK + term)} would underflow, we should rather
         * scale everything and re-scale at the end:
         */

        pK += term; /*
                     * = Math.log(pK) + Math.log(t_k) == Math.log(s_k) -- used at end to rescale
                     */
        /* mid = 1 = the rescaled value, instead of mid = Math.exp(pK); */

        /* Now sum from the inside out */
        /* LDOUBLE */double sum = term = 1. /* = mid term */;
        /* middle to the left */
        int k = kMax;
        while (k > 0 && term > sum * eps) {
            k--;
            /* LDOUBLE */double q = /* 1 / r_k = */ (k + 1) * (k + a) / (k + a + b) / dx2;
            term *= q;
            sum += term;
        }
        /* middle to the right */
        term = 1.;
        k = kMax;
        do {
            /* LDOUBLE */double q = /* r_{old k} = */ dx2 * (k + a + b) / (k + a) / (k + 1);
            k++;
            term *= q;
            sum += term;
        } while (term > sum * eps);

        // #ifdef HAVE_LONG_DOUBLE
        // return DPQ.rdMath.exp((double)(pK + logl(sum)));
        // #else
        return DPQ.rdexp(pK + Math.log(sum), giveLog);
    }
}
