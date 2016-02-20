/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2013, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RRuntime;

// transcribed from pnorm.c

public final class Pnorm implements StatsFunctions.Function3_2 {

    private final BranchProfile nanProfile = BranchProfile.create();

    @Override
    public double evaluate(double x, double mu, double sigma, boolean lowerTail, boolean logP) {
        /*
         * Note: The structure of these checks has been carefully thought through. For example, if x
         * == mu and sigma == 0, we get the correct answer 1.
         */
        if (Double.isNaN(x) || Double.isNaN(mu) || Double.isNaN(sigma)) {
            nanProfile.enter();
            return x + mu + sigma;
        }
        if (!Double.isFinite(x) && mu == x) {
            nanProfile.enter();
            return Double.NaN; /* x-mu is NaN */
        }
        if (sigma <= 0) {
            if (sigma < 0) {
                nanProfile.enter();
                return Double.NaN;
            }
            /* sigma = 0 : */
            return (x < mu) ? DPQ.d0(logP) : DPQ.d1(logP);
        }
        double p = (x - mu) / sigma;
        if (!Double.isFinite(p)) {
            return (x < mu) ? DPQ.d0(logP) : DPQ.d1(logP);
        }

        PnormBoth pnormBoth = new PnormBoth(p);
        pnormBoth.pnormBoth(p, !lowerTail, logP);

        return (lowerTail ? pnormBoth.cum : pnormBoth.ccum);
    }

    private static final class PnormBoth {
        private double cum;
        private double ccum;

        PnormBoth(double cum) {
            this.cum = cum;
        }

        private static final double SIXTEN = 16; /* Cutoff allowing exact "*" and "/" */

        private void doDel(double x, double originalX, double temp, boolean logP, boolean lower, boolean upper) {
            double xsq = ((long) (x * SIXTEN)) / SIXTEN;
            double del = (x - xsq) * (x + xsq);
            if (logP) {
                cum = (-xsq * xsq * 0.5) + (-del * 0.5) + Math.log(temp);
                if ((lower && originalX > 0.) || (upper && originalX <= 0.)) {
                    ccum = Math.log1p(-Math.exp(-xsq * xsq * 0.5) * Math.exp(-del * 0.5) * temp);
                }
            } else {
                cum = Math.exp(-xsq * xsq * 0.5) * Math.exp(-del * 0.5) * temp;
                ccum = 1.0 - cum;
            }
        }

        private void swapTail(double x, boolean lower) {
            if (x > 0.) { /* swap ccum <--> cum */
                double temp = cum;
                if (lower) {
                    cum = ccum;
                }
                ccum = temp;
            }
        }

        void pnormBoth(double x, boolean iTail, boolean logP) {
            /*
             * i_tail in {0,1,2} means: "lower", "upper", or "both" : if(lower) return *cum := P[X
             * <= x] if(upper) return *ccum := P[X > x] = 1 - P[X <= x]
             */
            double[] a = {2.2352520354606839287, 161.02823106855587881, 1067.6894854603709582, 18154.981253343561249, 0.065682337918207449113};
            double[] b = {47.20258190468824187, 976.09855173777669322, 10260.932208618978205, 45507.789335026729956};
            double[] c = {0.39894151208813466764, 8.8831497943883759412, 93.506656132177855979, 597.27027639480026226, 2494.5375852903726711, 6848.1904505362823326, 11602.651437647350124,
                            9842.7148383839780218, 1.0765576773720192317e-8};
            double[] d = {22.266688044328115691, 235.38790178262499861, 1519.377599407554805, 6485.558298266760755, 18615.571640885098091, 34900.952721145977266, 38912.003286093271411,
                            19685.429676859990727};
            double[] p = {0.21589853405795699, 0.1274011611602473639, 0.022235277870649807, 0.001421619193227893466, 2.9112874951168792e-5, 0.02307344176494017303};
            double[] q = {1.28426009614491121, 0.468238212480865118, 0.0659881378689285515, 0.00378239633202758244, 7.29751555083966205e-5};

            // #ifdef NO_DENORMS
            // double min = DBL_MIN;
            // #endif

            if (Double.isNaN(x)) {
                cum = ccum = x;
                return;
            }

            /* Consider changing these : */
            double eps = RRuntime.EPSILON * 0.5;

            /* i_tail in {0,1,2} =^= {lower, upper, both} */
            boolean lower = !iTail;
            boolean upper = iTail;

            double y = Math.abs(x);
            if (y <= 0.67448975) { /* qnorm(3/4) = .6744.... -- earlier had 0.66291 */
                double xnum;
                double xden;
                if (y > eps) {
                    double xsq = x * x;
                    xnum = a[4] * xsq;
                    xden = xsq;
                    for (int i = 0; i < 3; ++i) {
                        xnum = (xnum + a[i]) * xsq;
                        xden = (xden + b[i]) * xsq;
                    }
                } else {
                    xnum = xden = 0.0;
                }

                double temp = x * (xnum + a[3]) / (xden + b[3]);
                if (lower) {
                    cum = 0.5 + temp;
                }
                if (upper) {
                    ccum = 0.5 - temp;
                }
                if (logP) {
                    if (lower) {
                        cum = Math.log(cum);
                    }
                    if (upper) {
                        ccum = Math.log(ccum);
                    }
                }
            } else if (y <= MathConstants.M_SQRT_32) {

                /* Evaluate pnorm for 0.674.. = qnorm(3/4) < |x| <= sqrt(32) ~= 5.657 */

                double xnum = c[8] * y;
                double xden = y;
                for (int i = 0; i < 7; ++i) {
                    xnum = (xnum + c[i]) * y;
                    xden = (xden + d[i]) * y;
                }
                double temp = (xnum + c[7]) / (xden + d[7]);

                doDel(y, x, temp, logP, lower, upper);
                swapTail(x, lower);

                /*
                 * else |x| > sqrt(32) = 5.657 : the next two case differentiations were really for
                 * lower=T, log=F Particularly *not* for log_p !
                 * 
                 * Cody had (-37.5193 < x && x < 8.2924) ; R originally had y < 50
                 * 
                 * Note that we do want symmetry(0), lower/upper -> hence use y
                 */
            } else if ((logP && y < 1e170) /* avoid underflow below */
                            /*
                             * ^^^^^ MM FIXME: can speedup for log_p and much larger |x| ! Then,
                             * make use of Abramowitz & Stegun, 26.2.13, something like
                             * 
                             * xsq = x*x;
                             * 
                             * if(xsq * DBL_EPSILON < 1.) del = (1. - (1. - 5./(xsq+6.)) / (xsq+4.))
                             * / (xsq+2.); else del = 0.;cum = -.5*xsq - M_LN_SQRT_2PI - log(x) +
                             * log1p(-del);ccum = log1p(-exp(*cum)); /.* ~ log(1) = 0 *./
                             * 
                             * swap_tail;
                             * 
                             * [Yes, but xsq might be infinite.]
                             */
                            || (lower && -37.5193 < x && x < 8.2924)
                            || (upper && -8.2924 < x && x < 37.5193)) {

                /* Evaluate pnorm for x in (-37.5, -5.657) union (5.657, 37.5) */
                double xsq = 1.0 / (x * x); /* (1./x)*(1./x) might be better */
                double xnum = p[5] * xsq;
                double xden = xsq;
                for (int i = 0; i < 4; ++i) {
                    xnum = (xnum + p[i]) * xsq;
                    xden = (xden + q[i]) * xsq;
                }
                double temp = xsq * (xnum + p[4]) / (xden + q[4]);
                temp = (MathConstants.M_1_SQRT_2PI - temp) / y;

                doDel(x, x, temp, logP, lower, upper);
                swapTail(x, lower);
            } else { /* large x such that probs are 0 or 1 */
                if (x > 0) {
                    cum = DPQ.d1(logP);
                    ccum = DPQ.d0(logP);
                } else {
                    cum = DPQ.d0(logP);
                    ccum = DPQ.d1(logP);
                }
            }

            // #ifdef NO_DENORMS
            // /* do not return "denormalized" -- we do in R */
            // if(log_p) {
            // if(*cum > -min) *cum = -0.;
            // if(*ccum > -min)*ccum = -0.;
            // }
            // else {
            // if(*cum < min) *cum = 0.;
            // if(*ccum < min) *ccum = 0.;
            // }
            // #endif
            return;
        }
    }
}
