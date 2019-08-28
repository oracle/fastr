/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq.RChisq;

@GenerateUncached
public abstract class Rf extends RandFunction2_Double {
    @Specialization
    public double exec(double n1, double n2, RandomNumberProvider rand,
                    @Cached() RChisq rchisq) {
        if (Double.isNaN(n1) || Double.isNaN(n2) || n1 <= 0. || n2 <= 0.) {
            return RMathError.defaultError();
        }

        double v1;
        double v2;
        v1 = Double.isFinite(n1) ? (rchisq.execute(n1, rand) / n1) : 1;
        v2 = Double.isFinite(n2) ? (rchisq.execute(n2, rand) / n2) : 1;
        return v1 / v2;
    }

    public static Rf create() {
        return RfNodeGen.create();
    }

    public static Rf getUncached() {
        return RfNodeGen.getUncached();
    }
}
