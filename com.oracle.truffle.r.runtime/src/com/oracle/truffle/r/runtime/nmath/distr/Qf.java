/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2015, The R Core Team
 * Copyright (c) 2005, The R Foundation
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

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.distr.Chisq.QChisq;

public final class Qf implements Function3_2 {

    public static Qf create() {
        return new Qf();
    }

    public static Qf getUncached() {
        return new Qf();
    }

    private final QBeta qbeta = new QBeta();
    private final QChisq qchisq = new QChisq();

    @Override
    public double evaluate(double p, double df1, double df2, boolean lowerTail, boolean logP) {

        if (Double.isNaN(p) || Double.isNaN(df1) || Double.isNaN(df2)) {
            return p + df1 + df2;
        }

        if (df1 <= 0. || df2 <= 0.) {
            return RMathError.defaultError();
        }

        try {
            DPQ.rqp01boundaries(p, 0, Double.POSITIVE_INFINITY, lowerTail, logP);
        } catch (EarlyReturn e) {
            return e.result;
        }

        /*
         * fudge the extreme DF cases -- qbeta doesn't do this well. But we still need to fudge the
         * infinite ones.
         */

        if (df1 <= df2 && df2 > 4e5) {
            if (!Double.isFinite(df1)) { /* df1 == df2 == Inf : */
                return 1.;
            } else {
                return qchisq.evaluate(p, df1, lowerTail, logP) / df1;
            }
        }
        if (df1 > 4e5) { /* and so df2 < df1 */
            return df2 / qchisq.evaluate(p, df2, !lowerTail, logP);
        }

        // FIXME: (1/qb - 1) = (1 - qb)/qb; if we know qb ~= 1, should use other tail
        double newP = (1. / qbeta.evaluate(p, df2 / 2, df1 / 2, !lowerTail, logP) - 1.) * (df2 / df1);
        return RMath.mlValid(newP) ? newP : Double.NaN;
    }
}
