/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2015, The R Foundation
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */

// TODO: fix copyright
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq.RChisq;

@GenerateUncached
public abstract class RNchisq extends RandFunction2_Double {

    @Specialization
    public double exec(double df, double lambda, RandomNumberProvider rand,
                    @Cached() RGamma rgamma,
                    @Cached() RChisq rchisq) {
        if (Double.isNaN(df) || !Double.isFinite(lambda) || df < 0. || lambda < 0.) {
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

    public static RNchisq create() {
        return RNchisqNodeGen.create();
    }

    public static RNchisq getUncached() {
        return RNchisqNodeGen.getUncached();
    }
}
