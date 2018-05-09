/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
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
/*
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
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
/*
 * Copyright (C) 1998       Ross Ihaka
 * Copyright (C) 2000-12    The R Core Team
 * Copyright (C) 2004--2005 The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.RMath.forceint;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction1_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class Geom {
    private Geom() {
        // only static members
    }

    public static final class QGeom implements Function2_2 {
        @Override
        public double evaluate(double p, double prob, boolean lowerTail, boolean logP) {
            if (prob <= 0 || prob > 1) {
                return RMathError.defaultError();
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
                return RMathError.defaultError();
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

    public static final class PGeom implements Function2_2 {
        @Override
        public double evaluate(double xIn, double p, boolean lowerTail, boolean logP) {
            if (Double.isNaN(xIn) || Double.isNaN(p)) {
                return xIn + p;
            }
            if (p <= 0 || p > 1) {
                return RMathError.defaultError();
            }

            if (xIn < 0.) {
                return DPQ.rdt0(lowerTail, logP);
            }
            if (!Double.isFinite(xIn)) {
                return DPQ.rdt1(lowerTail, logP);
            }
            double x = Math.floor(xIn + 1e-7);

            if (p == 1.) { /* we cannot assume IEEE */
                x = lowerTail ? 1 : 0;
                return logP ? Math.log(x) : x;
            }
            x = RMath.log1p(-p) * (x + 1);
            if (logP) {
                return DPQ.rdtclog(x, lowerTail, logP);
            } else {
                return lowerTail ? -RMath.expm1(x) : Math.exp(x);
            }
        }
    }

    public static final class RGeom extends RandFunction1_Double {
        @Override
        public double execute(double p, RandomNumberProvider rand) {
            if (!Double.isFinite(p) || p <= 0 || p > 1) {
                return RMathError.defaultError();
            }
            return RPois.rpois(rand.expRand() * ((1 - p) / p), rand);
        }
    }
}
