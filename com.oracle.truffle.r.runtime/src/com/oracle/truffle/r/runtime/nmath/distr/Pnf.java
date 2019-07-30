/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2008, The R Core Team
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

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_2;
import com.oracle.truffle.r.runtime.nmath.RMathError;

public final class Pnf implements Function4_2 {

    public static Pnf create() {
        return new Pnf();
    }

    public static Pnf getUncached() {
        return new Pnf();
    }

    private final PNChisq pnchisq = new PNChisq();

    @Override
    public double evaluate(double x, double df1, double df2, double ncp, boolean lowerTail, boolean logP) {
        double y;
        if (Double.isNaN(x) || Double.isNaN(df1) || Double.isNaN(df2) || Double.isNaN(ncp)) {
            return x + df2 + df1 + ncp;
        }
        if (df1 <= 0. || df2 <= 0. || ncp < 0) {
            return RMathError.defaultError();
        }
        if (!Double.isFinite(ncp)) {

            return RMathError.defaultError();
        }
        if (!Double.isFinite(df1) && !Double.isFinite(df2)) {
            /* both +Inf */
            return RMathError.defaultError();
        }

        try {
            DPQ.rpbounds01(x, 0., Double.POSITIVE_INFINITY, lowerTail, logP);
        } catch (EarlyReturn e) {
            return e.result;
        }

        if (df2 > 1e8) {
            /* avoid problems with +Inf and loss of accuracy */
            return pnchisq.evaluate(x * df1, df1, ncp, lowerTail, logP);
        }

        y = (df1 / df2) * x;
        return PNBeta.pnbeta2(y / (1. + y), 1. / (1. + y), df1 / 2., df2 / 2., ncp, lowerTail, logP);
    }
}
