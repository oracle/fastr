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

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction1_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

public final class RChisq implements RandFunction1_Double {
    public static double rchisq(double df, RandomNumberProvider rand) {
        if (!Double.isFinite(df) || df < 0.0) {
            return StatsUtil.mlError();
        }
        return new RGamma().evaluate(df / 2.0, 2.0, rand);
    }

    @Override
    public double evaluate(double a, RandomNumberProvider rand) {
        return rchisq(a, rand);
    }
}
