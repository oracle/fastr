/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2007, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgammafn;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_1_SQRT_2PI;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_LN2;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_2;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RMathError.MLError;

public class PTukey implements Function4_2 {
    private static final int nlegq = 16;
    private static final int ihalfq = 8;
    private static final int nleg = 12;
    private static final int ihalf = 6;
    private static final double eps1 = -30.0;
    private static final double eps2 = 1.0e-14;
    private static final double dhaf = 100.0;
    private static final double dquar = 800.0;
    private static final double deigh = 5000.0;
    private static final double dlarg = 25000.0;
    private static final double ulen1 = 1.0;
    private static final double ulen2 = 0.5;
    private static final double ulen3 = 0.25;
    private static final double ulen4 = 0.125;
    private static final double[] xlegq = {
                    0.989400934991649932596154173450,
                    0.944575023073232576077988415535,
                    0.865631202387831743880467897712,
                    0.755404408355003033895101194847,
                    0.617876244402643748446671764049,
                    0.458016777657227386342419442984,
                    0.281603550779258913230460501460,
                    0.950125098376374401853193354250e-1
    };
    private static final double[] alegq = {
                    0.271524594117540948517805724560e-1,
                    0.622535239386478928628438369944e-1,
                    0.951585116824927848099251076022e-1,
                    0.124628971255533872052476282192,
                    0.149595988816576732081501730547,
                    0.169156519395002538189312079030,
                    0.182603415044923588866763667969,
                    0.189450610455068496285396723208
    };

    private final Pnorm pnorm = new Pnorm();

    @Override
    public double evaluate(double q, double rr, double cc, double df, boolean lowerTail, boolean logP) {
        if (Double.isNaN(q) || Double.isNaN(rr) || Double.isNaN(cc) || Double.isNaN(df)) {
            return RMathError.defaultError();
        }

        if (q <= 0) {
            return DPQ.rdt0(lowerTail, logP);
        }

        /* df must be > 1 */
        /* there must be at least two values */

        if (df < 2 || rr < 1 || cc < 2) {
            return RMathError.defaultError();
        }

        if (!Double.isFinite(q)) {
            return DPQ.rdt1(lowerTail, logP);
        }

        if (df > dlarg) {
            return DPQ.rdtval(wprob(q, rr, cc), lowerTail, logP);
        }

        /* calculate leading constant */

        double f2 = df * 0.5;
        /* lgammafn(u) = Math.log(gamma(u)) */
        double f2lf = ((f2 * Math.log(df)) - (df * M_LN2)) - lgammafn(f2);
        double f21 = f2 - 1.0;

        /* integral is divided into unit, half-unit, quarter-unit, or */
        /* eighth-unit length intervals depending on the value of the */
        /* degrees of freedom. */

        double ff4 = df * 0.25;
        double ulen = getULen(df);
        f2lf += Math.log(ulen);

        /* integrate over each subinterval */
        double ans = 0.0;

        double otsum = 0.0;
        for (int i = 1; i <= 50; i++) {
            otsum = 0.0;

            /* legendre quadrature with order = nlegq */
            /* nodes (stored in xlegq) are symmetric around zero. */

            double twa1 = (2 * i - 1) * ulen;

            for (int jj = 1; jj <= nlegq; jj++) {
                int j;
                double t1;
                if (ihalfq < jj) {
                    j = jj - ihalfq - 1;
                    t1 = (f2lf + (f21 * Math.log(twa1 + (xlegq[j] * ulen)))) - (((xlegq[j] * ulen) + twa1) * ff4);
                } else {
                    j = jj - 1;
                    t1 = (f2lf + (f21 * Math.log(twa1 - (xlegq[j] * ulen)))) + (((xlegq[j] * ulen) - twa1) * ff4);

                }

                /* if Math.exp(t1) < 9e-14, then doesn't contribute to integral */
                if (t1 >= eps1) {
                    double qsqz;
                    if (ihalfq < jj) {
                        qsqz = q * Math.sqrt(((xlegq[j] * ulen) + twa1) * 0.5);
                    } else {
                        qsqz = q * Math.sqrt(((-(xlegq[j] * ulen)) + twa1) * 0.5);
                    }

                    /* call wprob to find integral of range portion */

                    double wprb = wprob(qsqz, rr, cc);
                    double rotsum = (wprb * alegq[j]) * Math.exp(t1);
                    otsum += rotsum;
                }
                /* end legendre integral for interval i */
                /* L200: */
            }

            /*
             * if integral for interval i < 1e-14, then stop. However, in order to avoid small area
             * under left tail, at least 1 / ulen intervals are calculated.
             */
            if (i * ulen >= 1.0 && otsum <= eps2) {
                break;
            }

            /* end of interval i */
            /* L330: */

            ans += otsum;
        }

        if (otsum > eps2) { /* not converged */
            RMathError.error(MLError.PRECISION, "ptukey");
        }
        if (ans > 1.) {
            ans = 1.;
        }
        return DPQ.rdtval(ans, lowerTail, logP);
    }

    private static double getULen(double df) {
        if (df <= dhaf) {
            return ulen1;
        } else if (df <= dquar) {
            return ulen2;
        } else if (df <= deigh) {
            return ulen3;
        }
        return ulen4;
    }

    private static final double C1 = -30.;
    private static final double C2 = -50.;
    private static final double C3 = 60.;
    private static final double bb = 8.;
    private static final double wlar = 3.;
    private static final double wincr1 = 2.;
    private static final double wincr2 = 3.;
    private static final double[] xleg = {
                    0.981560634246719250690549090149,
                    0.904117256370474856678465866119,
                    0.769902674194304687036893833213,
                    0.587317954286617447296702418941,
                    0.367831498998180193752691536644,
                    0.125233408511468915472441369464
    };
    private static final double[] aleg = {
                    0.047175336386511827194615961485,
                    0.106939325995318430960254718194,
                    0.160078328543346226334652529543,
                    0.203167426723065921749064455810,
                    0.233492536538354808760849898925,
                    0.249147045813402785000562436043
    };

    private double wprob(double w, double rr, double cc) {
        // double a, ac, prW, b, binc, c, cc1,
        // pminus, pplus, qexpo, qsqz, rinsum, wi, wincr, xx;
        // LDOUBLE blb, bub, einsum, elsum;
        // int j, jj;

        double qsqz = w * 0.5;

        /* if w >= 16 then the integral lower bound (occurs for c=20) */
        /* is 0.99999999999995 so return a value of 1. */

        if (qsqz >= bb) {
            return 1.0;
        }

        /* find (f(w/2) - 1) ^ cc */
        /* (first term in integral of hartley's form). */

        double prW = 2 * pnorm.evaluate(qsqz, 0., 1., true, false) - 1.; /* erf(qsqz / M_SQRT2) */
        /* if prW ^ cc < 2e-22 then set prW = 0 */
        if (prW >= Math.exp(C2 / cc)) {
            prW = Math.pow(prW, cc);
        } else {
            prW = 0.0;
        }

        /* if w is large then the second component of the */
        /* integral is small, so fewer intervals are needed. */

        double wincr = w > wlar ? wincr1 : wincr2;

        /* find the integral of second term of hartley's form */
        /* for the integral of the range for equal-length */
        /* intervals using legendre quadrature. limits of */
        /* integration are from (w/2, 8). two or three */
        /* equal-length intervals are used. */

        /* blb and bub are lower and upper limits of integration. */

        /* LDOUBLE */double blb = qsqz;
        double binc = (bb - qsqz) / wincr;
        /* LDOUBLE */double bub = blb + binc;
        /* LDOUBLE */double einsum = 0.0;

        /* integrate over each interval */

        double cc1 = cc - 1.0;
        for (double wi = 1; wi <= wincr; wi++) {
            /* LDOUBLE */double elsum = 0.0;
            double a = 0.5 * (bub + blb);

            /* legendre quadrature with order = nleg */

            double b = 0.5 * (bub - blb);

            for (int jj = 1; jj <= nleg; jj++) {
                double xx;
                int j;
                if (ihalf < jj) {
                    j = (nleg - jj) + 1;
                    xx = xleg[j - 1];
                } else {
                    j = jj;
                    xx = -xleg[j - 1];
                }
                double c = b * xx;
                double ac = a + c;

                /* if Math.exp(-qexpo/2) < 9e-14, */
                /* then doesn't contribute to integral */

                double qexpo = ac * ac;
                if (qexpo > C3) {
                    break;
                }

                double pplus = 2 * pnorm.evaluate(ac, 0., 1., true, false);
                double pminus = 2 * pnorm.evaluate(ac, w, 1., true, false);

                /* if rinsum ^ (cc-1) < 9e-14, */
                /* then doesn't contribute to integral */

                double rinsum = (pplus * 0.5) - (pminus * 0.5);
                if (rinsum >= Math.exp(C1 / cc1)) {
                    rinsum = (aleg[j - 1] * Math.exp(-(0.5 * qexpo))) * Math.pow(rinsum, cc1);
                    elsum += rinsum;
                }
            }
            elsum *= (((2.0 * b) * cc) * M_1_SQRT_2PI);
            einsum += elsum;
            blb = bub;
            bub += binc;
        }

        /* if prW ^ rr < 9e-14, then return 0 */
        prW += einsum;
        if (prW <= Math.exp(C1 / rr)) {
            return 0.;
        }

        prW = Math.pow(prW, rr);
        if (prW >= 1.) {
            /* 1 was iMax was eps */
            return 1.;
        }

        return prW;
    }
}
