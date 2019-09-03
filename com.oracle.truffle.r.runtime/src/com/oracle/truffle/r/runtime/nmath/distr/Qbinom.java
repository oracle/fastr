/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2009, The R Core Team
 * Copyright (c) 2003--2009, The R Foundation
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;

// transcribed from qbinom.c

public final class Qbinom implements Function3_2 {

    public static Qbinom create() {
        return new Qbinom();
    }

    public static Qbinom getUncached() {
        return new Qbinom();
    }

    private final BranchProfile nanProfile = BranchProfile.create();
    private final ConditionProfile smallNProfile = ConditionProfile.createBinaryProfile();
    private final Pbinom pbinom = new Pbinom();

    @Override
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
            p = DPQ.rdtqiv(p, lowerTail, logProb); /* need check again (cancellation!): */
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

        double z = Qnorm.qnorm(p, 0., 1., /* lowerTail */true, /* logP */false);
        double y = Math.floor(mu + sigma * (z + gamma * (z * z - 1) / 6) + 0.5);

        if (y > n) {
            /* way off */
            y = n;
        }

        /* fuzz to ensure left continuity: */
        p *= 1 - 64 * RRuntime.EPSILON;

        QuantileSearch search = new QuantileSearch(n, (quantile, lt, lp) -> pbinom.evaluate(quantile, n, pr, lt, lp), true);
        if (smallNProfile.profile(n < 1e5)) {
            return search.simpleSearch(y, p, 1);
        } else {
            return search.iterativeSearch(y, p, Math.floor(n * 0.001), 1e-15, 100);
        }
    }
}
