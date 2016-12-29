/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 *  Copyright (C) 1995, 1996    Robert Gentleman and Ross Ihaka
 *  Copyright (C) 2000          The R Core Team
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_1;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_2;

public final class Logis {
    private Logis() {
        // only static members
    }

    public static final class DLogis implements Function3_1 {
        @Override
        public double evaluate(double x, double location, double scale, boolean giveLog) {
            if (Double.isNaN(x) || Double.isNaN(location) || Double.isNaN(scale)) {
                return x + location + scale;
            }
            if (scale <= 0.0) {
                return RMathError.defaultError();
            }

            x = TOMS708.fabs((x - location) / scale);
            double e = Math.exp(-x);
            double f = 1.0 + e;
            return giveLog ? -(x + Math.log(scale * f * f)) : e / (scale * f * f);
        }
    }

    public static final class QLogis implements Function3_2 {
        @Override
        public double evaluate(double p, double location, double scale, boolean lowerTail, boolean logP) {
            if (Double.isNaN(p) || Double.isNaN(location) || Double.isNaN(scale)) {
                return p + location + scale;
            }

            try {
                DPQ.rqp01boundaries(p, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, lowerTail, logP);
            } catch (EarlyReturn e) {
                return e.result;
            }

            if (scale < 0.) {
                return RMathError.defaultError();
            }
            if (scale == 0.) {
                return location;
            }

            /* p := logit(p) = Math.log( p / (1-p) ) : */
            if (logP) {
                if (lowerTail) {
                    p = p - DPQ.rlog1exp(p);
                } else {
                    p = DPQ.rlog1exp(p) - p;
                }
            } else {
                p = Math.log(lowerTail ? (p / (1. - p)) : ((1. - p) / p));
            }

            return location + scale * p;
        }
    }

    public static final class PLogis implements Function3_2 {
        @Override
        public double evaluate(double x, double location, double scale, boolean lowerTail, boolean logP) {
            if (Double.isNaN(x) || Double.isNaN(location) || Double.isNaN(scale)) {
                return x + location + scale;
            }
            if (scale <= 0.0) {
                return RMathError.defaultError();
            }

            x = (x - location) / scale;
            if (Double.isNaN(x)) {
                return RMathError.defaultError();
            }

            try {
                DPQ.rpboundsinf01(x, lowerTail, logP);
            } catch (EarlyReturn earlyReturn) {
                return earlyReturn.result;
            }

            if (logP) {
                // Math.log(1 / (1 + Math.exp( +- x ))) = -Math.log(1 + Math.exp( +- x))
                return -log1pexp(lowerTail ? -x : x);
            } else {
                return 1 / (1 + Math.exp(lowerTail ? -x : x));
            }
        }

        private static double log1pexp(double x) {
            if (x <= 18.) {
                return RMath.log1p(Math.exp(x));
            }
            if (x > 33.3) {
                return x;
            }
            // else: 18.0 < x <= 33.3 :
            return x + Math.exp(-x);
        }
    }

    public static final class RLogis extends RandFunction2_Double {
        @Override
        public double execute(double location, double scale, RandomNumberProvider rand) {
            if (Double.isNaN(location) || !Double.isFinite(scale)) {
                return RMathError.defaultError();
            }

            if (scale == 0. || !Double.isFinite(location)) {
                return location;
            } else {
                double u = rand.unifRand();
                return location + scale * Math.log(u / (1. - u));
            }
        }
    }
}
