/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2013, The R Core Team
 * Copyright (c) 2003-2013, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.DBL_EPSILON;
import static com.oracle.truffle.r.library.stats.MathConstants.DBL_MANT_DIG;
import static com.oracle.truffle.r.library.stats.MathConstants.DBL_MIN;
import static com.oracle.truffle.r.library.stats.MathConstants.M_1_PI;
import static com.oracle.truffle.r.library.stats.MathConstants.M_LN2;
import static com.oracle.truffle.r.library.stats.MathConstants.M_PI;
import static com.oracle.truffle.r.library.stats.MathConstants.M_PI_2;
import static com.oracle.truffle.r.library.stats.MathConstants.M_SQRT2;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_2;

public class Qt implements Function2_2 {
    private static final double eps = 1.e-12;
    private static final double accu = 1e-13;
    private static final double Eps = 1e-11; /* must be > accu */

    private final Qnorm qnorm = new Qnorm();
    private final Dt dt = new Dt();
    private final Pt pt = new Pt();

    @Override
    public double evaluate(double p, double ndf, boolean lowerTail, boolean logP) {

        if (Double.isNaN(p) || Double.isNaN(ndf)) {
            return p + ndf;
        }

        try {
            DPQ.rqp01boundaries(p, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, lowerTail, logP);
        } catch (EarlyReturn earlyReturn) {
            return earlyReturn.result;
        }

        if (ndf <= 0) {
            return RMathError.defaultError();
        }

        if (ndf < 1) { /* based on qnt */

            int iter = 0;

            p = DPQ.rdtqiv(p, lowerTail, logP);

            /*
             * Invert pt(.) : 1. finding an upper and lower bound
             */
            if (p > 1 - DBL_EPSILON) {
                return Double.POSITIVE_INFINITY;
            }
            double pp = RMath.fmin2(1 - DBL_EPSILON, p * (1 + Eps));
            double ux;
            double lx;
            ux = 1.;
            while (ux < Double.MAX_VALUE && pt.evaluate(ux, ndf, true, false) < pp) {
                ux *= 2;
            }
            pp = p * (1 - Eps);
            lx = -1.;
            while (lx > -Double.MAX_VALUE && pt.evaluate(lx, ndf, true, false) > pp) {
                lx *= 2;
            }

            /*
             * 2. interval (lx,ux) halving regula falsi failed on qt(0.1, 0.1)
             */
            double nx;
            do {
                nx = 0.5 * (lx + ux);
                if (pt.evaluate(nx, ndf, true, false) > p) {
                    ux = nx;
                } else {
                    lx = nx;
                }
            } while ((ux - lx) / Math.abs(nx) > accu && ++iter < 1000);

            if (iter >= 1000) {
                return RMathError.defaultError();
            }

            return 0.5 * (lx + ux);
        }

        if (ndf > 1e20) {
            return qnorm.evaluate(p, 0., 1., lowerTail, logP);
        }

        double capP = DPQ.rdqiv(p, logP); /* if Math.exp(p) underflows, we fix below */

        boolean neg = (!lowerTail || capP < 0.5) && (lowerTail || capP > 0.5);
        boolean isNegLower = (lowerTail == neg); /* both true or false == !xor */
        if (neg) {
            capP = 2 * (logP ? (lowerTail ? capP : -RMath.expm1(p)) : DPQ.rdlval(p, lowerTail));
        } else {
            capP = 2 * (logP ? (lowerTail ? -RMath.expm1(p) : capP) : DPQ.rdcval(p, lowerTail));
        }
        /* 0 <= P <= 1 ; P = 2*min(P', 1 - P') in all cases */

        double q;
        if (Math.abs(ndf - 2) < eps) { /* df ~= 2 */
            if (capP > DBL_MIN) {
                if (3 * capP < DBL_EPSILON) { /* P ~= 0 */
                    q = 1 / Math.sqrt(capP);
                } else if (capP > 0.9) { /* P ~= 1 */
                    q = (1 - capP) * Math.sqrt(2 / (capP * (2 - capP)));
                } else { /* eps/3 <= P <= 0.9 */
                    q = Math.sqrt(2 / (capP * (2 - capP)) - 2);
                }
            } else { /* P << 1, q = 1/Math.sqrt(P) = ... */
                if (logP) {
                    q = isNegLower ? Math.exp(-p / 2) / M_SQRT2 : 1 / Math.sqrt(-RMath.expm1(p));
                } else {
                    q = Double.POSITIVE_INFINITY;
                }
            }
        } else if (ndf < 1 + eps) { /* df ~= 1 (df < 1 excluded above): Cauchy */
            if (capP == 1.) {
                q = 0;
            } else if (capP > 0) {
                // some versions of tanpi give Inf, some NaN
                q = 1 / RMath.tanpi(capP / 2.); /* == - tan((P+1) * M_PI_2) -- suffers for P ~= 0 */
            } else { /* P = 0, but maybe = 2*Math.exp(p) ! */
                if (logP) { /* 1/tan(e) ~ 1/e */
                    q = isNegLower ? M_1_PI * Math.exp(-p) : -1. / (M_PI * RMath.expm1(p));
                } else {
                    q = Double.POSITIVE_INFINITY;
                }
            }
        } else { /*-- usual case;  including, e.g.,  df = 1.1 */
            double x = 0.;
            double y = 0;
            double logP2 = 0.;
            double a = 1 / (ndf - 0.5);
            double b = 48 / (a * a);
            double c = ((20700 * a / b - 98) * a - 16) * a + 96.36;
            double d = ((94.5 / (b + c) - 3) / b + 1) * Math.sqrt(a * M_PI_2) * ndf;

            boolean pOk1 = capP > DBL_MIN || !logP;
            boolean pOk = pOk1;
            if (pOk1) {
                y = Math.pow(d * capP, 2.0 / ndf);
                pOk = (y >= DBL_EPSILON);
            }
            if (!pOk) { // log.p && P very.small || (d*P)^(2/df) =: y < eps_c
                logP2 = isNegLower ? DPQ.rdlog(p, logP) : DPQ.rdlexp(p, logP); /*
                                                                                * == Math.log(P / 2)
                                                                                */
                x = (Math.log(d) + M_LN2 + logP2) / ndf;
                y = Math.exp(2 * x);
            }

            if ((ndf < 2.1 && capP > 0.5) || y > 0.05 + a) { /* P > P0(df) */
                /* Asymptotic inverse expansion about normal */
                if (pOk) {
                    x = qnorm.evaluate(0.5 * capP, 0., 1., /* lower_tail */true, /* log_p */false);
                } else { /* log_p && P underflowed */
                    x = qnorm.evaluate(logP2, 0., 1., lowerTail, /* log_p */ true);
                }

                y = x * x;
                if (ndf < 5) {
                    c += 0.3 * (ndf - 4.5) * (x + 0.6);
                }
                c = (((0.05 * d * x - 5) * x - 7) * x - 2) * x + b + c;
                y = (((((0.4 * y + 6.3) * y + 36) * y + 94.5) / c - y - 3) / b + 1) * x;
                y = RMath.expm1(a * y * y);
                q = Math.sqrt(ndf * y);
            } else if (!pOk && x < -M_LN2 * DBL_MANT_DIG) { /* 0.5* Math.log(DBL_EPSILON) */
                /* y above might have underflown */
                q = Math.sqrt(ndf) * Math.exp(-x);
            } else { /* re-use 'y' from above */
                y = ((1 / (((ndf + 6) / (ndf * y) - 0.089 * d - 0.822) * (ndf + 2) * 3) + 0.5 / (ndf + 4)) * y - 1) * (ndf + 1) / (ndf + 2) + 1 / y;
                q = Math.sqrt(ndf * y);
            }

            /*
             * Now apply 2-term Taylor expansion improvement (1-term = Newton): as by Hill (1981)
             * [ref.above]
             */

            /*
             * FIXME: This can be far from optimal when log_p = true but is still needed, e.g. for
             * qt(-2, df=1.01, log=true). Probably also improvable when lower_tail = false
             */

            if (pOk1) {
                int it = 0;
                while (it++ < 10 && (y = dt.evaluate(q, ndf, false)) > 0 &&
                                Double.isFinite(x = (pt.evaluate(q, ndf, false, false) - capP / 2) / y) &&
                                Math.abs(x) > 1e-14 * Math.abs(q)) {
                    /*
                     * Newton (=Taylor 1 term): q += x; Taylor 2-term :
                     */
                    q += x * (1. + x * q * (ndf + 1) / (2 * (q * q + ndf)));
                }
            }
        }
        if (neg) {
            q = -q;
        }
        return q;
    }
}
