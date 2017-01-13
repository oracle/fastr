/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1998-2015, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 *  based on AS243 (C) 1989 Royal Statistical Society
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgammafn;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_EPSILON;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_MIN_EXP;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_LN2;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_LN_SQRT_PI;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_SQRT_2dPI;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RMathError.MLError;
import com.oracle.truffle.r.runtime.nmath.TOMS708;

public class Pnt implements Function3_2 {
    private static final int itrmax = 1000;
    private static final double errmax = 1.e-12;

    private final Pt pt = new Pt();
    private final Pnorm pnorm = new Pnorm();
    private final Pbeta pbeta = new Pbeta();

    @Override
    public double evaluate(double t, double df, double ncp, boolean lowerTail, boolean logP) {
        if (df <= 0.0) {
            return RMathError.defaultError();
        }
        if (ncp == 0.0) {
            return pt.evaluate(t, df, lowerTail, logP);
        }

        if (!Double.isFinite(t)) {
            return t < 0 ? DPQ.rdt0(lowerTail, logP) : DPQ.rdt1(lowerTail, logP);
        }

        boolean negdel;
        double tt;
        double del;
        if (t >= 0.) {
            negdel = false;
            tt = t;
            del = ncp;
        } else {
            /*
             * We deal quickly with left tail if extreme, since pt(q, df, ncp) <= pt(0, df, ncp) =
             * \Phi(-ncp)
             */
            if (ncp > 40 && (!logP || !lowerTail)) {
                return DPQ.rdt0(lowerTail, logP);
            }
            negdel = true;
            tt = -t;
            del = -ncp;
        }

        /* LDOUBLE */double s;
        if (df > 4e5 || del * del > 2 * M_LN2 * (-DBL_MIN_EXP)) {
            /*-- 2nd part: if del > 37.62, then p=0 below
              GnuR fix me: test should depend on `df', `tt' AND `del' ! */
            /* Approx. from Abramowitz & Stegun 26.7.10 (p.949) */
            s = 1. / (4. * df);
            double pnormSigma = Math.sqrt(1. + tt * tt * 2. * s);
            return pnorm.evaluate(tt * (1. - s), del, pnormSigma, lowerTail != negdel, logP);
        }

        /* initialize twin series */
        /* Guenther, J. (1978). Statist. Computn. Simuln. vol.6, 199. */

        double x = t * t;
        double rxb = df / (x + df); /* := (1 - x) {x below} -- but more accurately */
        x = x / (x + df); /* in [0,1) */
        debugPrintf("pnt(t=%7g, df=%7g, ncp=%7g) ==> x= %10g:", t, df, ncp, x);
        if (x <= 0. || Double.isNaN(x)) {
            return finish(0., del, negdel, lowerTail, logP);
        }

        /* LDOUBLE */double tnc;
        /* else x > 0. <==> t != 0 */
        double lambda = del * del;
        /* LDOUBLE */double p = .5 * Math.exp(-.5 * lambda);
        debugPrintf("\t p=%10Lg\n", p);

        if (p == 0.) { /* underflow! */
            /// GnuR note: really use an other algorithm for this case
            RMathError.error(MLError.UNDERFLOW, "pnt");
            RMathError.error(MLError.RANGE, "pnt"); /* |ncp| too large */
            return DPQ.rdt0(lowerTail, logP);
        }

        debugPrintf("it  1e5*(godd,   geven)|          p           q           s        pnt(*)     errbd\n");

        /* LDOUBLE */double q = M_SQRT_2dPI * p * del;
        s = .5 - p;
        /* s = 0.5 - p = 0.5*(1 - Math.exp(-.5 L)) = -0.5*expm1(-.5 L)) */
        if (s < 1e-7) {
            s = -0.5 * RMath.expm1(-0.5 * lambda);
        }
        double a = .5;
        double b = .5 * df;
        /*
         * rxb = (1 - x) ^ b [ ~= 1 - b*x for tiny x --> see 'xeven' below] where '(1 - x)' =: rxb
         * {accurately!} above
         */
        rxb = Math.pow(rxb, b);
        double albeta = M_LN_SQRT_PI + lgammafn(b) - lgammafn(.5 + b);
        /* LDOUBLE */double xodd = pbeta.evaluate(x, a, b, /* lower */true, /* logP */false);
        /* LDOUBLE */double godd = 2. * rxb * Math.exp(a * Math.log(x) - albeta);
        tnc = b * x;
        /* LDOUBLE */double xeven = (tnc < DBL_EPSILON) ? tnc : 1. - rxb;
        /* LDOUBLE */double geven = tnc * rxb;
        tnc = p * xodd + q * xeven;

        /* repeat until convergence or iteration limit */
        for (int it = 1; it <= itrmax; it++) {
            a += 1.;
            xodd -= godd;
            xeven -= geven;
            godd *= x * (a + b - 1.) / a;
            geven *= x * (a + b - .5) / (a + .5);
            p *= lambda / (2 * it);
            q *= lambda / (2 * it + 1);
            tnc += p * xodd + q * xeven;
            s -= p;
            /* R 2.4.0 added test for rounding error here. */
            if (s < -1.e-10) { /* happens e.g. for (t,df,ncp)=(40,10,38.5), after 799 it. */
                RMathError.error(MLError.PRECISION, "pnt");
                debugPrintf("s = %#14.7Lg < 0 !!! ---> non-convergence!!\n", s);
                return finish(tnc, del, negdel, lowerTail, logP);
            }
            if (s <= 0 && it > 1) {
                return finish(tnc, del, negdel, lowerTail, logP);
            }
            double errbd = 2. * s * (xodd - godd);
            debugPrintf("%3d %.4g %.4g|%.4Lg %.4g %.4g %.10g %.4g\n",
                            it, 1e5 * godd, 1e5 * geven, p, q, s, tnc, errbd);
            if (TOMS708.fabs(errbd) < errmax) {
                return finish(tnc, del, negdel, lowerTail, logP); /* convergence */
            }
        }
        /* non-convergence: */
        RMathError.error(MLError.NOCONV, "pnt");
        return finish(tnc, del, negdel, lowerTail, logP);
    }

    private double finish(double tncIn, double del, boolean negdel, boolean lowerTailIn, boolean logP) {
        /* LDOUBLE */double tnc = tncIn + pnorm.evaluate(-del, 0., 1., /* lower */true, /* logP */false);

        boolean lowerTail = lowerTailIn != negdel; /* xor */
        if (tnc > 1 - 1e-10 && lowerTail) {
            RMathError.error(MLError.PRECISION, "pnt{final}");
        }
        return DPQ.rdtval(RMath.fmin2(tnc, 1.) /* Precaution */, lowerTail, logP);
    }

    @SuppressWarnings("unused")
    private void debugPrintf(String fmt, Object... args) {
        // System.out.printf(fmt + "\n", args);
    }
}
