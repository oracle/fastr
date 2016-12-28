/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2015, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

// TODO: fix copyright
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

public final class RNchisq extends RandFunction2_Double {
    private final RGamma rgamma = new RGamma();

    @Override
    public double execute(double df, double lambda, RandomNumberProvider rand) {
        if (!Double.isFinite(df) || !Double.isFinite(lambda) || df < 0. || lambda < 0.) {
            return RMath.mlError();
        }

        if (lambda == 0.) {
            return (df == 0.) ? 0. : rgamma.execute(df / 2., 2., rand);
        } else {
            double r = RPois.rpois(lambda / 2., rand);
            if (r > 0.) {
                r = Chisq.RChisq.rchisq(2. * r, rand);
            }
            if (df > 0.) {
                r += rgamma.execute(df / 2., 2., rand);
            }
            return r;
        }
    }
}
