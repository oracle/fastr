/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_1;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_2;

public final class LogNormal {
    private LogNormal() {
        // only static members
    }

    public static final class RLNorm implements RandFunction2_Double {
        private final Rnorm rnorm = new Rnorm();

        @Override
        public double evaluate(double meanlog, double sdlog, RandomNumberProvider rand) {
            if (Double.isNaN(meanlog) || !Double.isFinite(sdlog) || sdlog < 0.) {
                return RMath.mlError();
            }
            return Math.exp(rnorm.evaluate(meanlog, sdlog, rand));
        }
    }

    public static final class DLNorm implements Function3_1 {
        @Override
        public double evaluate(double x, double meanlog, double sdlog, boolean giveLog) {
            if (Double.isNaN(x) || Double.isNaN(meanlog) || Double.isNaN(sdlog)) {
                return x + meanlog + sdlog;
            }
            if (sdlog <= 0) {
                if (sdlog < 0) {
                    return RMath.mlError();
                }
                // sdlog == 0 :
                return (Math.log(x) == meanlog) ? Double.POSITIVE_INFINITY : DPQ.rd0(giveLog);
            }
            if (x <= 0) {
                return DPQ.rd0(giveLog);
            }

            double y = (Math.log(x) - meanlog) / sdlog;
            return (giveLog ? -(MathConstants.M_LN_SQRT_2PI + 0.5 * y * y + Math.log(x * sdlog)) : MathConstants.M_1_SQRT_2PI * Math.exp(-0.5 * y * y) / (x * sdlog));
            /* M_1_SQRT_2PI = 1 / Math.sqrt(2 * pi) */
        }
    }

    public static final class QLNorm implements Function3_2 {
        private final Qnorm qnorm = new Qnorm();

        @Override
        public double evaluate(double p, double meanlog, double sdlog, boolean lowerTail, boolean logP) {
            if (Double.isNaN(p) || Double.isNaN(meanlog) || Double.isNaN(sdlog)) {
                return p + meanlog + sdlog;
            }
            try {
                DPQ.rqp01boundaries(p, 0, Double.POSITIVE_INFINITY, lowerTail, logP);
            } catch (EarlyReturn e) {
                return e.result;
            }
            return Math.exp(qnorm.evaluate(p, meanlog, sdlog, lowerTail, logP));
        }
    }

    public static final class PLNorm implements Function3_2 {
        private final Pnorm pnorm = new Pnorm();

        @Override
        public double evaluate(double x, double meanlog, double sdlog, boolean lowerTail, boolean logP) {
            if (Double.isNaN(x) || Double.isNaN(meanlog) || Double.isNaN(sdlog)) {
                return x + meanlog + sdlog;
            }
            if (sdlog < 0) {
                return RMath.mlError();
            }
            if (x > 0) {
                return pnorm.evaluate(Math.log(x), meanlog, sdlog, lowerTail, logP);
            }
            return DPQ.rdt0(lowerTail, logP);
        }
    }
}
