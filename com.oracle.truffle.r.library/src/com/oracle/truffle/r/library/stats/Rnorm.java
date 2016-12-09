/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

public final class Rnorm implements RandFunction2_Double {
    @Override
    public double evaluate(double mu, double sigma, RandomNumberProvider rand) {
        if (Double.isNaN(mu) || !Double.isFinite(sigma) || sigma < 0.) {
            return RMath.mlError();
        }
        if (sigma == 0. || !Double.isFinite(mu)) {
            return mu; /* includes mu = +/- Inf with finite sigma */
        } else {
            return mu + sigma * rand.normRand();
        }
    }
}
