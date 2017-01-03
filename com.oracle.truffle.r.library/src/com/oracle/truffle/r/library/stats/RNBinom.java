/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2016, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.DBL_MAX;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

public final class RNBinom {
    private RNBinom() {
        // only static members
    }

    public static final class RNBinomFunc extends RandFunction2_Double {
        @Child private RPois rpois = new RPois();
        @Child private RGamma rgamma = new RGamma();

        @Override
        public double execute(double size, double prob, RandomNumberProvider rand) {
            if (!Double.isFinite(prob) || Double.isNaN(size) || size <= 0 || prob <= 0 || prob > 1) {
                /* prob = 1 is ok, PR#1218 */
                return RMathError.defaultError();
            }
            return (prob == 1) ? 0 : rpois.execute(rgamma.execute(fixupSize(size), (1 - prob) / prob, rand), rand);
        }
    }

    public static final class RNBinomMu extends RandFunction2_Double {
        @Child private RPois rpois = new RPois();
        @Child private RGamma rgamma = new RGamma();

        @Override
        public double execute(double size, double mu, RandomNumberProvider rand) {
            if (!Double.isFinite(mu) || Double.isNaN(size) || size <= 0 || mu < 0) {
                return RMathError.defaultError();
            }
            double fixedSize = fixupSize(size);
            return (mu == 0) ? 0 : rpois.execute(rgamma.execute(fixedSize, mu / fixedSize, rand), rand);
        }
    }

    private static double fixupSize(double size) {
        // 'DBL_MAX/2' to prevent rgamma() returning Inf
        return !Double.isFinite(size) ? DBL_MAX / 2. : size;
    }
}
