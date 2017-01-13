/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class Weibull {
    private Weibull() {
        // only static members
    }

    public static final class QWeibull implements Function3_2 {
        @Override
        public double evaluate(double p, double shape, double scale, boolean lowerTail, boolean logP) {
            if (Double.isNaN(p) || Double.isNaN(shape) || Double.isNaN(scale)) {
                return p + shape + scale;
            }

            if (shape <= 0 || scale <= 0) {
                return RMathError.defaultError();
            }

            try {
                DPQ.rqp01boundaries(p, 0, Double.POSITIVE_INFINITY, lowerTail, logP);
            } catch (EarlyReturn e) {
                return e.result;
            }

            return scale * Math.pow(-DPQ.rdtclog(p, lowerTail, logP), 1. / shape);
        }
    }

    public static final class PWeibull implements Function3_2 {
        @Override
        public double evaluate(double x, double shape, double scale, boolean lowerTail, boolean logP) {
            if (Double.isNaN(x) || Double.isNaN(shape) || Double.isNaN(scale)) {
                return x + shape + scale;
            }

            if (shape <= 0 || scale <= 0) {
                return RMathError.defaultError();
            }

            if (x <= 0) {
                return DPQ.rdt0(lowerTail, logP);
            }
            double result = -Math.pow(x / scale, shape);
            return lowerTail ? (logP ? DPQ.rlog1exp(result) : -RMath.expm1(result)) : DPQ.rdexp(result, logP);
        }
    }

    public static final class DWeibull implements Function3_1 {
        @Override
        public double evaluate(double x, double shape, double scale, boolean giveLog) {
            if (Double.isNaN(x) || Double.isNaN(shape) || Double.isNaN(scale)) {
                return x + shape + scale;
            }
            if (shape <= 0 || scale <= 0) {
                return RMathError.defaultError();
            }

            if (x < 0) {
                return DPQ.rd0(giveLog);
            }
            if (!Double.isFinite(x)) {
                return DPQ.rd0(giveLog);
            }
            /* need to handle x == 0 separately */
            if (x == 0 && shape < 1) {
                return Double.POSITIVE_INFINITY;
            }
            double tmp1 = Math.pow(x / scale, shape - 1);
            double tmp2 = tmp1 * (x / scale);
            /* These are incorrect if tmp1 == 0 */
            return giveLog ? -tmp2 + Math.log(shape * tmp1 / scale) : shape * tmp1 * Math.exp(-tmp2) / scale;
        }
    }

    public static final class RWeibull extends RandFunction2_Double {
        @Override
        public double execute(double shape, double scale, RandomNumberProvider rand) {
            if (!Double.isFinite(shape) || !Double.isFinite(scale) || shape <= 0. || scale <= 0.) {
                return scale == 0. ? 0. : RMathError.defaultError();
            }

            return scale * Math.pow(-Math.log(rand.unifRand()), 1.0 / shape);

        }
    }
}
