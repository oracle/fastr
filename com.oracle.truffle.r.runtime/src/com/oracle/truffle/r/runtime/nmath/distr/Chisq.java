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

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.dgamma;
import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.pgamma;
import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.qgamma;

import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction1_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class Chisq {
    private Chisq() {
        // only static members
    }

    public static final class PChisq implements Function2_2 {
        @Override
        public double evaluate(double x, double df, boolean lowerTail, boolean logP) {
            return pgamma(x, df / 2., 2., lowerTail, logP);
        }
    }

    public static final class DChisq implements Function2_1 {
        @Override
        public double evaluate(double x, double df, boolean giveLog) {
            return dgamma(x, df / 2., 2., giveLog);
        }
    }

    public static final class QChisq implements Function2_2 {
        @Override
        public double evaluate(double p, double df, boolean lowerTail, boolean logP) {
            return qgamma(p, 0.5 * df, 2.0, lowerTail, logP);
        }
    }

    public static final class RChisq extends RandFunction1_Double {
        @Child private RGamma rGamma = new RGamma();

        @Override
        public double execute(double df, RandomNumberProvider rand) {
            if (!Double.isFinite(df) || df < 0.0) {
                return RMathError.defaultError();
            }
            return rGamma.execute(df / 2.0, 2.0, rand);
        }
    }
}
