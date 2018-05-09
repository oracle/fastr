/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000, The R Core Team
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

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.pgamma;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.RMathError;

public final class PPois implements Function2_2 {
    @Override
    public double evaluate(double x, double lambda, boolean lowerTail, boolean logP) {
        if (Double.isNaN(x) || Double.isNaN(lambda) || lambda < 0.) {
            return RMathError.defaultError();
        }
        if (x < 0) {
            return DPQ.rdt0(lowerTail, logP);
        }
        if (lambda == 0.) {
            return DPQ.rdt1(lowerTail, logP);
        }
        if (!Double.isFinite(x)) {
            return DPQ.rdt1(lowerTail, logP);
        }
        double floorX = Math.floor(x + 1e-7);

        return pgamma(lambda, floorX + 1, 1., !lowerTail, logP);
    }
}
