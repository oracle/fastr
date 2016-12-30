/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2008, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function4_2;

public class Pnf implements Function4_2 {
    private final PNChisq pnchisq = new PNChisq();
    private final PNBeta pnbeta = new PNBeta();

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
        return pnbeta.pnbeta2(y / (1. + y), 1. / (1. + y), df1 / 2., df2 / 2.,
                        ncp, lowerTail, logP);
    }
}
