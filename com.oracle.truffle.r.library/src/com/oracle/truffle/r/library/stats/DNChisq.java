/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-15, The R Core Team
 * Copyright (c) 2004-15, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.GammaFunctions.dpoisRaw;

import com.oracle.truffle.r.library.stats.Chisq.DChisq;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_1;

public final class DNChisq implements Function3_1 {
    private static final double eps = 5e-15;
    private final DChisq dchisq = new DChisq();

    @Override
    public double evaluate(double x, double df, double ncp, boolean giveLog) {
        if (Double.isNaN(x) || Double.isNaN(df) || Double.isNaN(ncp)) {
            return x + df + ncp;
        }

        if (!Double.isFinite(df) || !Double.isFinite(ncp) || ncp < 0 || df < 0) {
            return RMathError.defaultError();
        }

        if (x < 0) {
            return DPQ.rd0(giveLog);
        }
        if (x == 0 && df < 2.) {
            return Double.POSITIVE_INFINITY;
        }
        if (ncp == 0) {
            return (df > 0) ? dchisq.evaluate(x, df, giveLog) : DPQ.rd0(giveLog);
        }
        if (x == Double.POSITIVE_INFINITY) {
            return DPQ.rd0(giveLog);
        }

        double ncp2 = 0.5 * ncp;

        /* find max element of sum */
        double imax = Math.ceil((-(2 + df) + Math.sqrt((2 - df) * (2 - df) + 4 * ncp * x)) / 4);
        double mid;
        double dfmid = 0;   // Note: not initialized in GnuR
        if (imax < 0) {
            imax = 0;
        }
        if (Double.isFinite(imax)) {
            dfmid = df + 2 * imax;
            mid = dpoisRaw(imax, ncp2, false) * dchisq.evaluate(x, dfmid, false);
        } else {
            /* imax = Inf */
            mid = 0;
        }

        if (mid == 0) {
            /*
             * underflow to 0 -- maybe numerically correct; maybe can be more accurate, particularly
             * when giveLog = true
             */
            /*
             * Use central-chisq approximation formula when appropriate; ((FIXME: the optimal cutoff
             * also depends on (x,df); use always here? ))
             */
            if (giveLog || ncp > 1000.) {
                /* = "1/(1+b)" Abramowitz & St. */
                double nl = df + ncp;
                double ic = nl / (nl + ncp);
                return dchisq.evaluate(x * ic, nl * ic, giveLog);
            } else {
                return DPQ.rd0(giveLog);
            }
        }

        /* errorbound := term * q / (1-q) now subsumed in while() / if () below: */

        /* upper tail */
        /* LDOUBLE */double sum = mid;
        /* LDOUBLE */double term = mid;
        double df2 = dfmid;
        double i = imax;
        double x2 = x * ncp2;
        double q;
        do {
            i++;
            q = x2 / i / df2;
            df2 += 2;
            term *= q;
            sum += term;
        } while (q >= 1 || term * q > (1 - q) * eps || term > 1e-10 * sum);
        /* lower tail */
        term = mid;
        df2 = dfmid;
        i = imax;
        while (i != 0) {
            df2 -= 2;
            q = i * df2 / x2;
            i--;
            term *= q;
            sum += term;
            if (q < 1 && term * q <= (1 - q) * eps) {
                break;
            }
        }
        return DPQ.rdval(sum, giveLog);
    }
}
