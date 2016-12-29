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

public class QPois implements Function2_2 {
    private final Qnorm qnorm = new Qnorm();
    private final PPois ppois = new PPois();

    @Override
    public double evaluate(double p, double lambda, boolean lowerTail, boolean logP) {
        if (Double.isNaN(p) || Double.isNaN(lambda)) {
            return p + lambda;
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
            DPQ.rqp01boundaries(p, 0, Double.POSITIVE_INFINITY, lowerTail, logP);
        } catch (EarlyReturn e) {
            return e.result;
        }

        double mu = lambda;
        double sigma = Math.sqrt(lambda);
        /* gamma = sigma; PR#8058 should be kurtosis which is mu^-0.5 */
        double gamma = 1.0 / sigma;

        /*
         * Note : "same" code in qpois.c, qbinom.c, qnbinom.c -- FIXME: This is far from optimal
         * [cancellation for p ~= 1, etc]:
         */
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
        double y = Math.round(mu + sigma * (z + gamma * (z * z - 1) / 6));

        z = ppois.evaluate(y, lambda, /* lower_tail */true, /* log_p */false);

        /* fuzz to ensure left continuity; 1 - 1e-7 may lose too much : */
        p *= 1 - 64 * DBL_EPSILON;

        /* If the mean is not too large a simple search is OK */
        if (lambda < 1e5) {
            return search(y, z, p, lambda, 1).y;
        } else {
            /* Otherwise be a bit cleverer in the search */
            double incr = Math.floor(y * 0.001);
            double oldincr;
            do {
                oldincr = incr;
                SearchResult searchResult = search(y, z, p, lambda, incr);
                y = searchResult.y;
                z = searchResult.z;
                incr = RMath.fmax2(1, Math.floor(incr / 100));
            } while (oldincr > 1 && incr > lambda * 1e-15);
            return y;
        }
    }

    private SearchResult search(double yIn, double zIn, double p, double lambda, double incr) {
        if (zIn >= p) {
            /* search to the left */
            double y = yIn;
            for (;;) {
                double z = zIn;
                if (y == 0 || (z = ppois.evaluate(y - incr, lambda, /* l._t. */true, /* log_p */false)) < p) {
                    return new SearchResult(y, z);
                }
                y = RMath.fmax2(0, y - incr);
            }
        } else { /* search to the right */
            double y = yIn;
            for (;;) {
                y = y + incr;
                double z = zIn;
                if ((z = ppois.evaluate(y, lambda, /* l._t. */true, /* log_p */false)) >= p) {
                    return new SearchResult(y, z);
                }
            }
        }
    }

    private static final class SearchResult {
        final double y;
        final double z;

        SearchResult(double y, double z) {
            this.y = y;
            this.z = z;
        }
    }
}
