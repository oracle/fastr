/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000, The R Core Team
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
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction1_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class Exp {
    private Exp() {
        // only static members
    }

    public static final class DExp implements Function2_1 {
        @Override
        public double evaluate(double x, double scale, boolean giveLog) {
            /* NaNs propagated correctly */
            if (Double.isNaN(x) || Double.isNaN(scale)) {
                return x + scale;
            }

            if (scale <= 0.0) {
                return RMathError.defaultError();
            }

            if (x < 0.) {
                return DPQ.rd0(giveLog);
            }
            return (giveLog ? (-x / scale) - Math.log(scale) : Math.exp(-x / scale) / scale);
        }
    }

    public static final class RExp extends RandFunction1_Double {
        @Override
        public double execute(double scale, RandomNumberProvider rand) {
            if (!Double.isFinite(scale) || scale <= 0.0) {
                return scale == 0. ? 0. : RMathError.defaultError();
            }
            return scale * rand.expRand();
        }
    }

    public static final class PExp implements Function2_2 {
        @Override
        public double evaluate(double xIn, double scale, boolean lowerTail, boolean logP) {
            if (Double.isNaN(xIn) || Double.isNaN(scale)) {
                return xIn + scale;
            }
            if (scale < 0) {
                return RMathError.defaultError();
            }

            if (xIn <= 0.) {
                return DPQ.rdt0(lowerTail, logP);
            }

            /* same as weibull( shape = 1): */
            double x = -(xIn / scale);
            return lowerTail ? (logP ? DPQ.rlog1exp(x) : -RMath.expm1(x)) : DPQ.rdexp(x, logP);
        }
    }

    public static final class QExp implements Function2_2 {
        @Override
        public double evaluate(double p, double scale, boolean lowerTail, boolean logP) {
            if (Double.isNaN(p) || Double.isNaN(scale)) {
                return p + scale;
            }

            if (scale < 0) {
                return RMathError.defaultError();
            }

            try {
                DPQ.rqp01check(p, logP);
            } catch (EarlyReturn e) {
                return e.result;
            }

            if (p == DPQ.rdt0(lowerTail, logP)) {
                return 0;
            }

            return -scale * DPQ.rdtclog(p, lowerTail, logP);
        }
    }
}
