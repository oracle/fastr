/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2006-2015, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_EPSILON;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_MAX;
import static com.oracle.truffle.r.runtime.nmath.distr.Qnorm.qnorm;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.TOMS708;

public final class Qnt implements Function3_2 {
    private static final double accu = 1e-13;
    private static final double Eps = 1e-11; /* must be > accu */

    private final Pnt pnt = new Pnt();
    private final Qt qt = new Qt();

    @Override
    public double evaluate(double pIn, double df, double ncp, boolean lowerTail, boolean logP) {
        if (Double.isNaN(pIn) || Double.isNaN(df) || Double.isNaN(ncp)) {
            return pIn + df + ncp;
        }
        if (df <= 0.0) {
            return RMathError.defaultError();
        }

        if (ncp == 0.0 && df >= 1.0) {
            return qt.evaluate(pIn, df, lowerTail, logP);
        }

        try {
            DPQ.rqp01boundaries(pIn, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, lowerTail, logP);
        } catch (EarlyReturn e) {
            return e.result;
        }

        if (!Double.isFinite(df)) {
            // df = Inf ==> limit N(ncp,1)
            return qnorm(pIn, ncp, 1., lowerTail, logP);
        }

        double p = DPQ.rdtqiv(pIn, lowerTail, logP);

        /*
         * Invert pnt(.) : 1. finding an upper and lower bound
         */
        if (p > 1 - DBL_EPSILON) {
            return Double.POSITIVE_INFINITY;
        }
        double pp = RMath.fmin2(1 - DBL_EPSILON, p * (1 + Eps));
        double ux = RMath.fmax2(1., ncp);
        while (ux < DBL_MAX && pnt.evaluate(ux, df, ncp, true, false) < pp) {
            ux *= 2;
        }
        pp = p * (1 - Eps);
        double lx = RMath.fmin2(-1., -ncp);
        while (lx > -DBL_MAX && pnt.evaluate(lx, df, ncp, true, false) > pp) {
            lx *= 2;
        }

        /* 2. interval (lx,ux) halving : */
        double nx;
        do {
            nx = 0.5 * (lx + ux); // could be zero
            if (pnt.evaluate(nx, df, ncp, true, false) > p) {
                ux = nx;
            } else {
                lx = nx;
            }
        } while ((ux - lx) > accu * RMath.fmax2(TOMS708.fabs(lx), TOMS708.fabs(ux)));

        return 0.5 * (lx + ux);
    }
}
