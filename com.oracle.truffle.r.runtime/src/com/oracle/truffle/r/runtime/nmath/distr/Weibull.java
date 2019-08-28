/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
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

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.distr.WeibullFactory.RWeibullNodeGen;

public final class Weibull {
    private Weibull() {
        // only static members
    }

    public static final class QWeibull implements Function3_2 {
        public static QWeibull create() {
            return new QWeibull();
        }

        public static QWeibull getUncached() {
            return new QWeibull();
        }

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

        public static PWeibull create() {
            return new PWeibull();
        }

        public static PWeibull getUncached() {
            return new PWeibull();
        }

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

        public static DWeibull create() {
            return new DWeibull();
        }

        public static DWeibull getUncached() {
            return new DWeibull();
        }

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

    @GenerateUncached
    public abstract static class RWeibull extends RandFunction2_Double {
        @Specialization
        public double exec(double shape, double scale, RandomNumberProvider rand) {
            if (!Double.isFinite(shape) || !Double.isFinite(scale) || shape <= 0. || scale <= 0.) {
                return scale == 0. ? 0. : RMathError.defaultError();
            }

            return scale * Math.pow(-Math.log(rand.unifRand()), 1.0 / shape);
        }

        public static RWeibull create() {
            return RWeibullNodeGen.create();
        }

        public static RWeibull getUncached() {
            return RWeibullNodeGen.getUncached();
        }
    }
}
