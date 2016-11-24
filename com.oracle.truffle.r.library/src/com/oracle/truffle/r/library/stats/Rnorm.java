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
import com.oracle.truffle.r.runtime.rng.RandomNumberNode;

public final class Rnorm implements RandFunction2_Double {

    private static final double BIG = 134217728;

    @Override
    public void init(int length, RandomNumberNode randNode) {
        RRNG.getRNGState();
    }

    @Override
    public void finish() {
        RRNG.putRNGState();
    }

    @Override
    public double evaluate(int index, double mu, double sigma, double random, RandomNumberNode randomNode) {
        // TODO: GnuR invokes norm_rand to get "rand"
        double u1 = (int) (BIG * randomNode.executeSingleDouble()) + randomNode.executeSingleDouble();
        double rand = Random2.qnorm5(u1 / BIG, 0.0, 1.0, true, false);
        return rand * sigma + mu;
    }
}
