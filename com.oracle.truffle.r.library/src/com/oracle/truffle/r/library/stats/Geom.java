/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 * Copyright (C) 1998       Ross Ihaka
 * Copyright (C) 2000-12    The R Core Team
 * Copyright (C) 2004--2005 The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.forceint;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction1_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_1;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_2;

public class Geom {
    public static final class QGeom implements Function2_2 {
        @Override
        public double evaluate(double p, double prob, boolean lowerTail, boolean logP) {
            if (prob <= 0 || prob > 1) {
                return RMath.mlError();
            }

            try {
                DPQ.rqp01boundaries(p, 0, Double.POSITIVE_INFINITY, lowerTail, logP);
            } catch (EarlyReturn e) {
                return e.result;
            }

            if (Double.isNaN(p) || Double.isNaN(prob)) {
                return p + prob;
            }

            if (prob == 1) {
                return 0;
            }
            /* add a fuzz to ensure left continuity, but value must be >= 0 */
            return RMath.fmax2(0, Math.ceil(DPQ.rdtclog(p, lowerTail, logP) / RMath.log1p(-prob) - 1 - 1e-12));
        }

    }

    public static final class DGeom implements Function2_1 {
        @Override
        public double evaluate(double x, double p, boolean giveLog) {
            if (Double.isNaN(x) || Double.isNaN(p)) {
                return x + p;
            }
            if (p <= 0 || p > 1) {
                return RMath.mlError();
            }

            try {
                DPQ.nonintCheck(x, giveLog);
            } catch (EarlyReturn e) {
                return e.result;
            }

            if (x < 0 || !Double.isFinite(x) || p == 0) {
                return DPQ.rd0(giveLog);
            }
            /* prob = (1-p)^x, stable for small p */
            double prob = Dbinom.dbinomRaw(0., forceint(x), p, 1 - p, giveLog);
            return ((giveLog) ? Math.log(p) + prob : p * prob);
        }
    }

    public static final class RGeom implements RandFunction1_Double {
        @Override
        public double evaluate(double p, RandomNumberProvider rand) {
            if (!Double.isFinite(p) || p <= 0 || p > 1) {
                return RMath.mlError();
            }
            return RPois.rpois(rand.expRand() * ((1 - p) / p), rand);
        }
    }
}
