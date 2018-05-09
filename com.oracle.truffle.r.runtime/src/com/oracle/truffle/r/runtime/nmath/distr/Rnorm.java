/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
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

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class Rnorm extends RandFunction2_Double {
    private final BranchProfile errorProfile = BranchProfile.create();
    private final ConditionProfile zeroSigmaProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile sigmaValueProfile = ValueProfile.createEqualityProfile();
    private final ValueProfile muValueProfile = ValueProfile.createEqualityProfile();

    @Override
    public double execute(double muIn, double sigmaIn, RandomNumberProvider rand) {
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
}
