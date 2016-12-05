/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

public final class RNbinomMu implements RandFunction2_Double {
    private final RGamma rgamma = new RGamma();

    @Override
    public double evaluate(double size, double mu, RandomNumberProvider rand) {
        if (!Double.isFinite(mu) || Double.isNaN(size) || size <= 0 || mu < 0) {
            return StatsUtil.mlError();
        }
        if (!Double.isFinite(size)) {
            size = Double.MAX_VALUE / 2.;
        }
        return (mu == 0) ? 0 : RPois.rpois(rgamma.evaluate(size, mu / size, rand), rand);
    }
}
