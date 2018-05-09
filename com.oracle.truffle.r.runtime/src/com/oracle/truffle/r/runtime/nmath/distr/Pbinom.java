/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2007, The R Foundation
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
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;

public final class Pbinom implements Function3_2 {

    private final BranchProfile nanProfile = BranchProfile.create();

    @Override
    public double evaluate(double initialQ, double initialSize, double prob, boolean lowerTail, boolean logP) {
        double q = initialQ;
        double size = initialSize;
        if (Double.isNaN(q) || Double.isNaN(size) || Double.isNaN(prob)) {
            nanProfile.enter();
            return q + size + prob;
        }
        if (!Double.isFinite(size) || !Double.isFinite(prob)) {
            nanProfile.enter();
            return Double.NaN;
        }

        if (DPQ.nonint(size)) {
            nanProfile.enter();
            DPQ.nointCheckWarning(size, "n");
            return RMathError.defaultError();
        }
        size = RMath.forceint(size);
        /* PR#8560: n=0 is a valid value */
        if (size < 0 || prob < 0 || prob > 1) {
            nanProfile.enter();
            return Double.NaN;
        }

        if (q < 0) {
            return DPQ.rdt0(lowerTail, logP);
        }
        q = Math.floor(q + 1e-7);
        if (size <= q) {
            return DPQ.rdt1(lowerTail, logP);
        }
        return Pbeta.pbeta(prob, q + 1, size - q, !lowerTail, logP, nanProfile);
    }
}
