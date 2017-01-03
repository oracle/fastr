/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2006-8, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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
