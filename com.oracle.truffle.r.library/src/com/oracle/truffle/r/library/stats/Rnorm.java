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

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

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
