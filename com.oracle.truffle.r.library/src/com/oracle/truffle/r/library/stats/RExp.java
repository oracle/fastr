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

public final class RExp implements RandFunction1_Double {
    @Override
    public double evaluate(double scale, RandomNumberProvider rand) {
        if (!Double.isFinite(scale) || scale <= 0.0) {
            return scale == 0. ? 0. : StatsUtil.mlError();
        }
        return scale * rand.expRand();
    }
}
