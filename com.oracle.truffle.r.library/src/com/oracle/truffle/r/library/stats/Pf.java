/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2007, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.StatsUtil.*;

import com.oracle.truffle.api.profiles.BranchProfile;

// transcribed from nmath/pf.c
public final class Pf implements StatsFunctions.Function3_2 {

    private final BranchProfile nanProfile = BranchProfile.create();

    @Override
    public double evaluate(double x, double df1, double df2, boolean lowerTail, boolean logP) {
// if (Double.isNaN(x) || Double.isNaN(df1) || Double.isNaN(df2)) {
// return x + df2 + df1;
// }

        if (df1 <= 0 || df2 <= 0) {
            // TODO ML_ERR_return_NAN
            return Double.NaN;
        }

        // expansion of R_P_bounds_01(x, 0., ML_POSINF);
        if (x <= 0) {
            return rdt0(lowerTail, logP);
        }
        if (x >= Double.POSITIVE_INFINITY) {
            return rdt1(lowerTail, logP);
        }

        if (df2 == Double.POSITIVE_INFINITY) {
            if (df1 == Double.POSITIVE_INFINITY) {
                if (x < 1) {
                    return rdt0(lowerTail, logP);
                }
                if (x == 1) {
                    return logP ? -M_LN2 : 0.5;
                }
                if (x > 1) {
                    return rdt1(lowerTail, logP);
                }
            }

            return GammaFunctions.pgamma(x * df1, df1 / 2, 2, lowerTail, logP);
        }

        if (df1 == Double.POSITIVE_INFINITY) {
            return GammaFunctions.pgamma(df2 / x, df2 / 2, 2, !lowerTail, logP);
        }

        double ret;
        if (df1 * x > df2) {
            ret = Pbeta.pbeta(df2 / (df2 + df1 * x), df2 / 2, df1 / 2, !lowerTail, logP, nanProfile);
        } else {
            ret = Pbeta.pbeta(df1 * x / (df2 + df1 * x), df1 / 2, df2 / 2, lowerTail, logP, nanProfile);
        }

        return ret;

    }
}
