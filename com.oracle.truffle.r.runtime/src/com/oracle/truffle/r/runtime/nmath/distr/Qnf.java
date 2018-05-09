/*
 * Copyright (c) 2006-8, The R Core Team
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

public final class Qnf implements Function4_2 {
    private final QNChisq qnchisq = new QNChisq();
    private final QNBeta qnbeta = new QNBeta();

    @Override
    public double evaluate(double p, double df1, double df2, double ncp, boolean lowerTail, boolean logP) {
        if (Double.isNaN(p) || Double.isNaN(df1) || Double.isNaN(df2) || Double.isNaN(ncp)) {
            return p + df1 + df2 + ncp;
        }
        if (df1 <= 0. || df2 <= 0. || ncp < 0 || !Double.isFinite(ncp) || !Double.isFinite(df1) && !Double.isFinite(df2)) {
            return RMathError.defaultError();
        }

        try {
            DPQ.rqp01boundaries(p, 0, Double.POSITIVE_INFINITY, lowerTail, logP);
        } catch (EarlyReturn e) {
            return e.result;
        }

        if (df2 > 1e8) {
            /* avoid problems with +Inf and loss of accuracy */
            return qnchisq.evaluate(p, df1, ncp, lowerTail, logP) / df1;
        }

        double y = qnbeta.evaluate(p, df1 / 2., df2 / 2., ncp, lowerTail, logP);
        return y / (1 - y) * (df2 / df1);
    }
}
