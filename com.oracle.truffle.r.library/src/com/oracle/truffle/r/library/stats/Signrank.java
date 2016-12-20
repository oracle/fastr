/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1999--2014, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.forceint;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction1_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

public final class Signrank {
    private Signrank() {
        // only static members
    }

    public static final class RSignrank extends RandFunction1_Double {
        @Override
        public double execute(double n, RandomNumberProvider rand) {
            int i;
            int k;
            double r;

            if (Double.isNaN(n)) {
                return n;
            }
            if (Double.isInfinite(n)) {
                // In GnuR these "results" seem to be generated due to the behaviour of R_forceint,
                // and the "(int) n" cast, which ends up casting +/-infinity to integer...
                return n < 0 ? RMath.mlError() : 0;
            }

            n = forceint(n);
            if (n < 0) {
                return RMath.mlError();
            }

            if (n == 0) {
                return 0;
            }
            r = 0.0;
            k = (int) n;
            for (i = 0; i < k; i++) {
                r += (i + 1) * Math.floor(rand.unifRand() + 0.5);
            }
            return r;
        }
    }
}
