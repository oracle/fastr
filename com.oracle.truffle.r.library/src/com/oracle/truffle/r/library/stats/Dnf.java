/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2006, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 *  AUTHOR
 *    Peter Ruckdeschel, peter.ruckdeschel@uni-bayreuth.de.
 *    April 13, 2006.
 *
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.GammaFunctions.dgamma;

import com.oracle.truffle.r.library.stats.StatsFunctions.Function4_1;

public class Dnf implements Function4_1 {
    private final DNChisq dnchisq = new DNChisq();
    private final DNBeta dnbeta = new DNBeta();

    @Override
    public double evaluate(double x, double df1, double df2, double ncp, boolean giveLog) {
        if (Double.isNaN(x) || Double.isNaN(df1) || Double.isNaN(df2) || Double.isNaN(ncp)) {
            return x + df2 + df1 + ncp;
        }

        /*
         * want to compare dnf(ncp=0) behavior with df() one, hence *NOT* : if (ncp == 0) return
         * df(x, df1, df2, giveLog);
         */

        if (df1 <= 0. || df2 <= 0. || ncp < 0) {
            return RMathError.defaultError();
        }
        if (x < 0.) {
            return DPQ.rd0(giveLog);
        }
        if (!Double.isFinite(ncp)) {
            /* ncp = +Inf -- GnuR: fix me?: in some cases, limit exists */
            return RMathError.defaultError();
        }

        /*
         * This is not correct for df1 == 2, ncp > 0 - and seems unneeded: if (x == 0.) { return(df1
         * > 2 ? DPQ.rd0(log_p) : (df1 == 2 ? DPQ.rd1 : Double.POSITIVE_INFINITY)); }
         */
        if (!Double.isFinite(df1) && !Double.isFinite(df2)) {
            /* both +Inf */
            /* PR: not sure about this (taken from ncp==0) -- GnuR fix me ? */
            if (x == 1.) {
                return Double.POSITIVE_INFINITY;
            } else {
                return DPQ.rd0(giveLog);
            }
        }
        if (!Double.isFinite(df2)) {
            /* i.e. = +Inf */
            return df1 * dnchisq.evaluate(x * df1, df1, ncp, giveLog);
        }
        /* == dngamma(x, df1/2, 2./df1, ncp, giveLog) -- but that does not exist */
        if (df1 > 1e14 && ncp < 1e7) {
            /* includes df1 == +Inf: code below is inaccurate there */
            double f = 1 + ncp / df1; /* assumes ncp << df1 [ignores 2*ncp^(1/2)/df1*x term] */
            double z = dgamma(1. / x / f, df2 / 2, 2. / df2, giveLog);
            return giveLog ? z - 2 * Math.log(x) - Math.log(f) : z / (x * x) / f;
        }

        double y = (df1 / df2) * x;
        double z = dnbeta.evaluate(y / (1 + y), df1 / 2., df2 / 2., ncp, giveLog);
        return giveLog ? z + Math.log(df1) - Math.log(df2) - 2 * RMath.log1p(y) : z * (df1 / df2) / (1 + y) / (1 + y);
    }
}
