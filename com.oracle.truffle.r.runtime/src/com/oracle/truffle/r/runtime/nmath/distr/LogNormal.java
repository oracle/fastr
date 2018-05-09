/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
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

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathConstants;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class LogNormal {
    private LogNormal() {
        // only static members
    }

    public static final class RLNorm extends RandFunction2_Double {
        private final Rnorm rnorm = new Rnorm();

        @Override
        public double execute(double meanlog, double sdlog, RandomNumberProvider rand) {
            if (Double.isNaN(meanlog) || !Double.isFinite(sdlog) || sdlog < 0.) {
                return RMathError.defaultError();
            }
            return Math.exp(rnorm.execute(meanlog, sdlog, rand));
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
                    return RMathError.defaultError();
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
                return RMathError.defaultError();
            }
            if (x > 0) {
                return pnorm.evaluate(Math.log(x), meanlog, sdlog, lowerTail, logP);
            }
            return DPQ.rdt0(lowerTail, logP);
        }
    }
}
