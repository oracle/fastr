/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.DBL_EPSILON;
import static com.oracle.truffle.r.library.stats.MathConstants.forceint;
import static com.oracle.truffle.r.library.stats.RMath.fmax2;
import static com.oracle.truffle.r.library.stats.RMath.fmin2;
import static com.oracle.truffle.r.library.stats.RMath.lfastchoose;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;

public final class QHyper {
    public static double qhyper(double pIn, double nrIn, double nbIn, double nIn, boolean lowerTail, boolean logP) {
        /* This is basically the same code as ./phyper.c *used* to be --> FIXME! */
        if (Double.isNaN(pIn) || Double.isNaN(nrIn) || Double.isNaN(nbIn) || Double.isNaN(nIn)) {
            return pIn + nrIn + nbIn + nIn;
        }
        if (!Double.isFinite(pIn) || !Double.isFinite(nrIn) || !Double.isFinite(nbIn) || !Double.isFinite(nIn)) {
            return RMath.mlError();
        }

        double nr = forceint(nrIn);
        double nb = forceint(nbIn);
        double capN = nr + nb;
        double n = forceint(nIn);
        if (nr < 0 || nb < 0 || n < 0 || n > capN) {
            return RMath.mlError();
        }

        /*
         * Goal: Find xr (= #{red balls in sample}) such that phyper(xr, NR,NB, n) >= p > phyper(xr
         * - 1, NR,NB, n)
         */

        double xstart = fmax2(0, n - nb);
        double xend = fmin2(n, nr);

        double p = pIn;
        try {
            DPQ.rqp01boundaries(p, xstart, xend, lowerTail, logP);
        } catch (EarlyReturn ex) {
            return ex.result;
        }

        double xr = xstart;
        double xb = n - xr; /* always ( = #{black balls in sample} ) */

        boolean smallN = (capN < 1000); /* won't have underflow in product below */
        /*
         * if N is small, term := product.ratio( bin.coef ); otherwise work with its Math.logarithm
         * to protect against underflow
         */
        double term = lfastchoose(nr, xr) + lfastchoose(nb, xb) - lfastchoose(capN, n);
        if (smallN) {
            term = Math.exp(term);
        }
        nr -= xr;
        nb -= xb;

        if (!lowerTail || logP) {
            p = DPQ.rdtqiv(p, lowerTail, logP);
        }
        p *= 1 - 1000 * DBL_EPSILON; /* was 64, but failed on FreeBSD sometimes */
        double sum = smallN ? term : Math.exp(term);

        while (sum < p && xr < xend) {
            xr++;
            nb++;
            if (smallN) {
                term *= (nr / xr) * (xb / nb);
            } else {
                term += Math.log((nr / xr) * (xb / nb));
            }
            sum += smallN ? term : Math.exp(term);
            xb--;
            nr--;
        }
        return xr;
    }

}
