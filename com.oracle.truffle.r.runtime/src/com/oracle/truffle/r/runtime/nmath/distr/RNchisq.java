/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2015, The R Foundation
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

// TODO: fix copyright
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq.RChisq;

public final class RNchisq extends RandFunction2_Double {
    @Child private RGamma rgamma = new RGamma();
    @Child private RChisq rchisq = new RChisq();

    @Override
    public double execute(double df, double lambda, RandomNumberProvider rand) {
        if (!Double.isFinite(df) || !Double.isFinite(lambda) || df < 0. || lambda < 0.) {
            return RMathError.defaultError();
        }

        if (lambda == 0.) {
            return (df == 0.) ? 0. : rgamma.execute(df / 2., 2., rand);
        } else {
            double r = RPois.rpois(lambda / 2., rand);
            if (r > 0.) {
                r = rchisq.execute(2. * r, rand);
            }
            if (df > 0.) {
                r += rgamma.execute(df / 2., 2., rand);
            }
            return r;
        }
    }
}
