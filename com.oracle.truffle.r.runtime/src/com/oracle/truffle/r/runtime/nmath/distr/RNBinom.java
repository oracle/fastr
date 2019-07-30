/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2016, The R Core Team
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_MAX;

import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class RNBinom {
    private RNBinom() {
        // only static members
    }

    @GenerateUncached
    public abstract static class RNBinomFunc extends RandFunction2_Double {

        @Specialization
        public double execute(double size, double prob, RandomNumberProvider rand,
                        @Cached() RPois rpois,
                        @Cached() RGamma rgamma) {
            if (!Double.isFinite(prob) || Double.isNaN(size) || size <= 0 || prob <= 0 || prob > 1) {
                /* prob = 1 is ok, PR#1218 */
                return RMathError.defaultError();
            }
            return (prob == 1) ? 0 : rpois.execute(rgamma.execute(fixupSize(size), (1 - prob) / prob, rand), rand);
        }

        public static RNBinomFunc create() {
            return RNBinomFactory.RNBinomFuncNodeGen.create();
        }

        public static RNBinomFunc getUncached() {
            return RNBinomFactory.RNBinomFuncNodeGen.getUncached();
        }
    }

    @GenerateUncached
    public abstract static class RNBinomMu extends RandFunction2_Double {
        @Specialization
        public double exec(double size, double mu, RandomNumberProvider rand,
                        @Cached() RPois rpois,
                        @Cached() RGamma rgamma) {
            if (!Double.isFinite(mu) || Double.isNaN(size) || size <= 0 || mu < 0) {
                return RMathError.defaultError();
            }
            double fixedSize = fixupSize(size);
            return (mu == 0) ? 0 : rpois.execute(rgamma.execute(fixedSize, mu / fixedSize, rand), rand);
        }

        public static RNBinomMu create() {
            return RNBinomFactory.RNBinomMuNodeGen.create();
        }

        public static RNBinomMu getUncached() {
            return RNBinomFactory.RNBinomMuNodeGen.getUncached();
        }
    }

    private static double fixupSize(double size) {
        // 'DBL_MAX/2' to prevent rgamma() returning Inf
        return !Double.isFinite(size) ? DBL_MAX / 2. : size;
    }
}
