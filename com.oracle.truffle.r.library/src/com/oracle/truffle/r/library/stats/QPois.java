/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2014, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.DBL_EPSILON;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_2;

public final class QPois implements Function2_2 {
    private final Qnorm qnorm = new Qnorm();
    private final PPois ppois = new PPois();

    @Override
    public double evaluate(double pIn, double lambda, boolean lowerTail, boolean logP) {
        if (Double.isNaN(pIn) || Double.isNaN(lambda)) {
            return pIn + lambda;
        }
        if (!Double.isFinite(lambda)) {
            return RMathError.defaultError();
        }
        if (lambda < 0) {
            return RMathError.defaultError();
        }
        if (lambda == 0) {
            return 0;
        }

        try {
            DPQ.rqp01boundaries(pIn, 0, Double.POSITIVE_INFINITY, lowerTail, logP);
        } catch (EarlyReturn e) {
            return e.result;
        }

        double sigma = Math.sqrt(lambda);
        /* gamma = sigma; PR#8058 should be kurtosis which is mu^-0.5 */
        double gamma = 1.0 / sigma;

        /*
         * Note : "same" code in qpois.c, qbinom.c, qnbinom.c -- FIXME: This is far from optimal
         * [cancellation for p ~= 1, etc]:
         */
        double p = pIn;
        if (!lowerTail || logP) {
            p = DPQ.rdtqiv(p, lowerTail, logP); /* need check again (cancellation!): */
            if (p == 0.) {
                return 0;
            }
            if (p == 1.) {
                return Double.POSITIVE_INFINITY;
            }
        }
        /* temporary hack --- FIXME --- */
        if (p + 1.01 * DBL_EPSILON >= 1.) {
            return Double.POSITIVE_INFINITY;
        }

        /* y := approx.value (Cornish-Fisher expansion) : */
        double z = qnorm.evaluate(p, 0., 1., /* lower_tail */true, /* log_p */false);
        // #ifdef HAVE_NEARBYINT
        // y = nearbyint(mu + sigma * (z + gamma * (z*z - 1) / 6));
        // #else
        double y = RMath.round(lambda + sigma * (z + gamma * (z * z - 1) / 6));

        /* fuzz to ensure left continuity; 1 - 1e-7 may lose too much : */
        p *= 1 - 64 * DBL_EPSILON;

        QuantileSearch search = new QuantileSearch((quantile, lt, lp) -> ppois.evaluate(quantile, lambda, lt, lp));
        if (lambda < 1e5) {
            /* If the mean is not too large a simple search is OK */
            return search.simpleSearch(y, p, 1);
        } else {
            /* Otherwise be a bit cleverer in the search */
            return search.iterativeSearch(y, p);
        }
    }
}
