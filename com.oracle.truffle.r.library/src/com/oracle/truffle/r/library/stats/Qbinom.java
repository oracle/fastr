/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2009, The R Core Team
 * Copyright (c) 2003--2009, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;

// transcribed from qbinom.c

public final class Qbinom implements StatsFunctions.Function3_2 {

    private static final class Search {
        private double z;

        Search(double z) {
            this.z = z;
        }

        double doSearch(double initialY, double p, double n, double pr, double incr, Pbinom pbinom1, Pbinom pbinom2) {
            double y = initialY;
            if (z >= p) {
                /* search to the left */
                for (;;) {
                    double newz;
                    if (y == 0 || (newz = pbinom1.evaluate(y - incr, n, pr, true, false)) < p) {
                        return y;
                    }
                    y = Math.max(0, y - incr);
                    z = newz;
                }
            } else { /* search to the right */
                for (;;) {
                    y = Math.min(y + incr, n);
                    if (y == n || (z = pbinom2.evaluate(y, n, pr, true, false)) >= p) {
                        return y;
                    }
                }
            }
        }
    }

    private final BranchProfile nanProfile = BranchProfile.create();
    private final ConditionProfile smallNProfile = ConditionProfile.createBinaryProfile();
    private final Pbinom pbinom = new Pbinom();
    private final Pbinom pbinomSearch1 = new Pbinom();
    private final Pbinom pbinomSearch2 = new Pbinom();

    public double evaluate(double initialP, double n, double pr, boolean lowerTail, boolean logProb) {
        double p = initialP;

        if (Double.isNaN(p) || Double.isNaN(n) || Double.isNaN(pr)) {
            nanProfile.enter();
            return p + n + pr;
        }

        if (!Double.isFinite(n) || !Double.isFinite(pr)) {
            nanProfile.enter();
            return Double.NaN;
        }
        /* if logP is true, p = -Inf is a legitimate value */
        if (!Double.isFinite(p) && !logProb) {
            nanProfile.enter();
            return Double.NaN;
        }

        if (n != Math.floor(n + 0.5)) {
            nanProfile.enter();
            return Double.NaN;
        }
        if (pr < 0 || pr > 1 || n < 0) {
            nanProfile.enter();
            return Double.NaN;
        }

        if (logProb) {
            if (p > 0) {
                nanProfile.enter();
                return Double.NaN;
            }
            if (p == 0) {
                return lowerTail ? n : (double) 0;
            }
            if (p == Double.NEGATIVE_INFINITY) {
                return lowerTail ? (double) 0 : n;
            }
        } else { /* !logP */
            if (p < 0 || p > 1) {
                nanProfile.enter();
                return Double.NaN;
            }
            if (p == 0) {
                return lowerTail ? (double) 0 : n;
            }
            if (p == 1) {
                return lowerTail ? n : (double) 0;
            }
        }

        if (pr == 0 || n == 0) {
            return 0;
        }

        double q = 1 - pr;
        if (q == 0) {
            /* covers the full range of the distribution */
            return n;
        }
        double mu = n * pr;
        double sigma = Math.sqrt(n * pr * q);
        double gamma = (q - pr) / sigma;

        /*
         * Note : "same" code in qpois.c, qbinom.c, qnbinom.c -- FIXME: This is far from optimal
         * [cancellation for p ~= 1, etc]:
         */
        if (!lowerTail || logProb) {
            p = DPQ.dtQIv(p, lowerTail, logProb); /* need check again (cancellation!): */
            if (p == 0) {
                return 0;
            }
            if (p == 1) {
                return n;
            }
        }
        /* temporary hack --- FIXME --- */
        if (p + 1.01 * RRuntime.EPSILON >= 1.) {
            return n;
        }

        /* y := approx.value (Cornish-Fisher expansion) : */

        double z = Random2.qnorm5(p, 0., 1., /* lowerTail */true, /* logP */false);
        double y = Math.floor(mu + sigma * (z + gamma * (z * z - 1) / 6) + 0.5);

        if (y > n) {
            /* way off */
            y = n;
        }

        z = pbinom.evaluate(y, n, pr, /* lowerTail */true, /* logP */false);

        /* fuzz to ensure left continuity: */
        p *= 1 - 64 * RRuntime.EPSILON;

        Search search = new Search(z);

        if (smallNProfile.profile(n < 1e5)) {
            return search.doSearch(y, p, n, pr, 1, pbinomSearch1, pbinomSearch2);
        }
        /* Otherwise be a bit cleverer in the search */
        double incr = Math.floor(n * 0.001);
        double oldincr;
        do {
            oldincr = incr;
            y = search.doSearch(y, p, n, pr, incr, pbinomSearch1, pbinomSearch2);
            incr = Math.max(1, Math.floor(incr / 100));
        } while (oldincr > 1 && incr > n * 1e-15);
        return y;
    }
}
