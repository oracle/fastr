/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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
