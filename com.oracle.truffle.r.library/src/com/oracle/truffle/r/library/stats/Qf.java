/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2015, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.library.stats.Chisq.QChisq;
import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_2;

public final class Qf implements Function3_2 {
    private final QBeta qbeta = new QBeta();
    private final QChisq qchisq = new QChisq();

    @Override
    public double evaluate(double p, double df1, double df2, boolean lowerTail, boolean logP) {

        if (Double.isNaN(p) || Double.isNaN(df1) || Double.isNaN(df2)) {
            return p + df1 + df2;
        }

        if (df1 <= 0. || df2 <= 0.) {
            return RMath.mlError();
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
        p = (1. / qbeta.evaluate(p, df2 / 2, df1 / 2, !lowerTail, logP) - 1.) * (df2 / df1);
        return RMath.mlValid(p) ? p : Double.NaN;
    }
}
