/*
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates
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
/*
 *  AUTHOR
 *    Claus Ekstrøm, ekstrom@dina.kvl.dk
 *    July 15, 2003.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgammafn;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_EPSILON;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_LN_SQRT_PI;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.TOMS708;

public final class Dnt implements Function3_1 {

    public static Dnt create() {
        return new Dnt();
    }

    public static Dnt getUncached() {
        return new Dnt();
    }

    private final Dt dt = new Dt();
    private final DNorm dnorm = new DNorm();
    private final Pnt pnt = new Pnt();

    @Override
    public double evaluate(double x, double df, double ncp, boolean giveLog) {
        if (Double.isNaN(x) || Double.isNaN(df)) {
            return x + df;
        }

        /* If non-positive df then error */
        if (df <= 0.0) {
            return RMathError.defaultError();
        }

        if (ncp == 0.0) {
            return dt.evaluate(x, df, giveLog);
        }

        /* If x is infinite then return 0 */
        if (!Double.isFinite(x)) {
            return DPQ.rd0(giveLog);
        }

        /*
         * If infinite df then the density is identical to a normal distribution with mean = ncp.
         * However, the formula loses a lot of accuracy around df=1e9
         */
        if (!Double.isFinite(df) || df > 1e8) {
            return dnorm.evaluate(x, ncp, 1., giveLog);
        }

        /* Do calculations on log scale to stabilize */

        /* Consider two cases: x ~= 0 or not */
        double u;
        if (TOMS708.fabs(x) > Math.sqrt(df * DBL_EPSILON)) {
            u = Math.log(df) - Math.log(TOMS708.fabs(x)) +
                            Math.log(TOMS708.fabs(pnt.evaluate(x * Math.sqrt((df + 2) / df), df + 2, ncp, true, false) -
                                            pnt.evaluate(x, df, ncp, true, false)));
            /* GnuR fix me: the above still suffers from cancellation (but not horribly) */
        } else { /* x ~= 0 : -> same value as for x = 0 */
            u = lgammafn((df + 1) / 2) - lgammafn(df / 2) - (M_LN_SQRT_PI + .5 * (Math.log(df) + ncp * ncp));
        }

        return (giveLog ? u : Math.exp(u));
    }
}
