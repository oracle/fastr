/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates
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
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

@GenerateUncached
public abstract class Rnorm extends RandFunction2_Double {

    @Specialization
    public double exec(double muIn, double sigmaIn, RandomNumberProvider rand,
                    @Cached("createBinaryProfile()") ConditionProfile zeroSigmaProfile,
                    @Cached("createEqualityProfile()") PrimitiveValueProfile sigmaValueProfile,
                    @Cached("createEqualityProfile()") PrimitiveValueProfile muValueProfile,
                    @Cached() BranchProfile errorProfile) {
        double sigma = sigmaValueProfile.profile(sigmaIn);
        double mu = muValueProfile.profile(muIn);
        if (Double.isNaN(mu) || !Double.isFinite(sigma) || sigma < 0.) {
            errorProfile.enter();
            return RMathError.defaultError();
        }
        if (zeroSigmaProfile.profile(sigma == 0. || !Double.isFinite(mu))) {
            return mu; /* includes mu = +/- Inf with finite sigma */
        } else {
            return mu + sigma * rand.normRand();
        }
    }

    public static Rnorm create() {
        return RnormNodeGen.create();
    }

    public static Rnorm getUncached() {
        return RnormNodeGen.getUncached();
    }
}
