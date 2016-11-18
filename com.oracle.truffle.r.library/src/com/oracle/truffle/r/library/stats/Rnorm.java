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
import com.oracle.truffle.r.runtime.rng.RRNG;

/**
 * TODO GnuR checks/updates {@code .Random.seed} across this call. TODO Honor min/max.
 */
public final class Rnorm implements RandFunction2_Double {

    private static final double BIG = 134217728;

    public double evaluate(double mu, double sigma) {
        // TODO: GnuR invokes norm_rand to get "rand"
        double u1 = (int) (BIG * RRNG.unifRand()) + RRNG.unifRand();
        double rand = Random2.qnorm5(u1 / BIG, 0.0, 1.0, true, false);
        return rand * sigma + mu;
    }
}
