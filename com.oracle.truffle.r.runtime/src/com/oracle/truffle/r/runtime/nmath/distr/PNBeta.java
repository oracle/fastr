/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000-2013, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgammafn;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RMathError.MLError;
import com.oracle.truffle.r.runtime.nmath.TOMS708.Bratio;

public final class PNBeta implements Function4_2 {
    @Override
    public double evaluate(double x, double a, double b, double ncp, boolean lowerTail, boolean logP) {
        if (Double.isNaN(x) || Double.isNaN(a) || Double.isNaN(b) || Double.isNaN(ncp)) {
            return x + a + b + ncp;
        }
        try {
            DPQ.rpbounds01(x, 0., 1., lowerTail, logP);
        } catch (EarlyReturn e) {
            return e.result;
        }
        return pnbeta2(x, 1 - x, a, b, ncp, lowerTail, logP);
    }

    double pnbeta2(double x, double oX, double a, double b, double ncp, boolean lowerTail, boolean logP) {
        /* LDOUBLE */
        double ans = pnbetaRaw(x, oX, a, b, ncp);

        /* return DPQ.rdtval(ans), but we want to warn about cancellation here */
        if (lowerTail) {
            // #ifdef HAVE_LONG_DOUBLE
            // return (double) (logP ? logl(ans) : ans);
            // #else
            return logP ? Math.log(ans) : ans;
        } else {
            if (ans > 1. - 1e-10) {
                RMathError.error(MLError.PRECISION, "pnbeta");
            }
            if (ans > 1.0) {
                ans = 1.0;
            } /* Precaution */
            // #if defined(HAVE_LONG_DOUBLE) && defined(HAVE_LOG1PL)
            // return (double) (logP ? log1pl(-ans) : (1. - ans));
            // #else
            /* include standalone case */
            return (logP ? RMath.log1p(-ans) : (1. - ans));
        }

    }

    /*
     * GnuR: change errmax and itrmax if desired; original (AS 226, R84) had (errmax; itrmax) =
     * (1e-6; 100)
     */
    private static final double errmax = 1.0e-9;
    private static final int itrmax = 10000; /*
                                              * GnuR: 100 is not enough for pf(ncp=200) see PR#11277
                                              */

    double pnbetaRaw(double x, double oX, double a, double b, double ncp) {
        /* oX == 1 - x but maybe more accurate */
        if (ncp < 0. || a <= 0. || b <= 0.) {
            return RMathError.defaultError();
        }

        if (x < 0. || oX > 1. || (x == 0. && oX == 1.)) {
            return 0.;
        }
        if (x > 1. || oX < 0. || (x == 1. && oX == 0.)) {
            return 1.;
        }

        double c = ncp / 2.;

        /* initialize the series */
        double x0 = Math.floor(RMath.fmax2(c - 7. * Math.sqrt(c), 0.));
        double a0 = a + x0;
        double lbeta = lgammafn(a0) + lgammafn(b) - lgammafn(a0 + b);

        /* temp = pbeta_raw(x, a0, b, true, false), but using (x, oX): */
        double temp = Bratio.bratio(a0, b, x, oX, false).w;

        /* LDOUBLE */double gx = Math.exp(a0 * Math.log(x) + b * (x < .5 ? RMath.log1p(-x) : Math.log(oX)) - lbeta - Math.log(a0));
        /* LDOUBLE */double q;
        if (a0 > a) {
            q = Math.exp(-c + x0 * Math.log(c) - lgammafn(x0 + 1.));
        } else {
            q = Math.exp(-c);
        }

        /* LDOUBLE */double sumq = 1. - q;
        /* LDOUBLE */double ans = q * temp;
        /* LDOUBLE */double ax;

        /* recurse over subsequent terms until convergence is achieved */
        double j = Math.floor(x0); // x0 could be billions, and is in package EnvStats
        double errbd;
        do {
            j++;
            temp -= gx;
            gx *= x * (a + b + j - 1.) / (a + j);
            q *= c / j;
            sumq -= q;
            ax = temp * q;
            ans += ax;
            errbd = ((temp - gx) * sumq);
        } while (errbd > errmax && j < itrmax + x0);

        if (errbd > errmax) {
            RMathError.error(MLError.PRECISION, "pnbeta");
        }
        if (j >= itrmax + x0) {
            RMathError.error(MLError.NOCONV, "pnbeta");
        }
        return ans;
    }
}
