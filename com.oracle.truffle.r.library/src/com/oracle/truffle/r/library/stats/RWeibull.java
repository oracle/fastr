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

public final class RWeibull extends RandFunction2_Double {
    @Override
    public double execute(double shape, double scale, RandomNumberProvider rand) {
        if (!Double.isFinite(shape) || !Double.isFinite(scale) || shape <= 0. || scale <= 0.) {
            return scale == 0. ? 0. : RMath.mlError();
        }

        return scale * Math.pow(-Math.log(rand.unifRand()), 1.0 / shape);

    }
}
