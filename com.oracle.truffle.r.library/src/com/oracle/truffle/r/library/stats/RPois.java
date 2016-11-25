/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.M_1_SQRT_2PI;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

public final class RPois {

    private static final double a0 = -0.5;
    private static final double a1 = 0.3333333;
    private static final double a2 = -0.2500068;
    private static final double a3 = 0.2000118;
    private static final double a4 = -0.1661269;
    private static final double a5 = 0.1421878;
    private static final double a6 = -0.1384794;
    private static final double a7 = 0.1250060;

    private static final double one_7 = 0.1428571428571428571;
    private static final double one_12 = 0.0833333333333333333;
    private static final double one_24 = 0.0416666666666666667;

    /* Factorial Table (0:9)! */
    private static final double[] fact = new double[]{
                    1., 1., 2., 6., 24., 120., 720., 5040., 40320., 362880.
    };

    public static double rpois(double mu, RandomNumberProvider rand) {

        /* These are static --- persistent between calls for same mu : */
        // TODO: state variables
        int l = 0;
        int m = 0;
        double b1;
        double b2;
        double c = 0;
        double c0 = 0;
        double c1 = 0;
        double c2 = 0;
        double c3 = 0;
        double[] pp = new double[36];
        double p0 = 0;
        double p = 0;
        double q = 0;
        double s = 0;
        double d = 0;
        double omega = 0;
        double bigL = 0; /* integer "w/o overflow" */
        double muprev = 0.;
        double muprev2 = 0.; /* , muold = 0. */

        /* Local Vars [initialize some for -Wall]: */
        double del;
        double difmuk = 0.;
        double e = 0.;
        double fk = 0.;
        double fx;
        double fy;
        double g;
        double px;
        double py;
        double t = 0;
        double u = 0.;
        double v;
        double x;
        double pois = -1.;
        int k;
        int kflag = 0;
        boolean bigMu;
        boolean newBigMu = false;

        if (!Double.isFinite(mu) || mu < 0) {
            return StatsUtil.mlError();
        }

        if (mu <= 0.) {
            return 0.;
        }

        bigMu = mu >= 10.;
        if (bigMu) {
            newBigMu = false;
        }

        if (!(bigMu && mu == muprev)) { /* maybe compute new persistent par.s */

            if (bigMu) {
                newBigMu = true;
                /*
                 * Case A. (recalculation of s,d,l because mu has changed): The poisson
                 * probabilities pk exceed the discrete normal probabilities fk whenever k >= m(mu).
                 */
                muprev = mu;
                s = Math.sqrt(mu);
                d = 6. * mu * mu;
                bigL = Math.floor(mu - 1.1484);
                /* = an upper bound to m(mu) for all mu >= 10. */
            } else { /* Small mu ( < 10) -- not using normal approx. */

                /* Case B. (start new table and calculate p0 if necessary) */

                /* muprev = 0.;-* such that next time, mu != muprev .. */
                if (mu != muprev) {
                    muprev = mu;
                    m = Math.max(1, (int) mu);
                    l = 0; /* pp[] is already ok up to pp[l] */
                    q = p0 = p = Math.exp(-mu);
                }

                while (true) {
                    /* Step U. uniform sample for inversion method */
                    u = rand.unifRand();
                    if (u <= p0) {
                        return 0.;
                    }

                    /*
                     * Step T. table comparison until the end pp[l] of the pp-table of cumulative
                     * poisson probabilities (0.458 > ~= pp[9](= 0.45792971447) for mu=10 )
                     */
                    if (l != 0) {
                        for (k = (u <= 0.458) ? 1 : Math.min(l, m); k <= l; k++) {
                            if (u <= pp[k]) {
                                return (double) k;
                            }
                        }
                        if (l == 35) { /* u > pp[35] */
                            continue;
                        }
                    }
                    /*
                     * Step C. creation of new poisson probabilities p[l..] and their cumulatives q
                     * =: pp[k]
                     */
                    l++;
                    for (k = l; k <= 35; k++) {
                        p *= mu / k;
                        q += p;
                        pp[k] = q;
                        if (u <= q) {
                            l = k;
                            return (double) k;
                        }
                    }
                    l = 35;
                } /* end(repeat) */
            } /* mu < 10 */

        } /* end {initialize persistent vars} */

        /* Only if mu >= 10 : ----------------------- */

        /* Step N. normal sample */
        g = mu + s * rand.unifRand(); /* norm_rand() ~ N(0,1), standard normal */

        if (g >= 0.) {
            pois = Math.floor(g);
            /* Step I. immediate acceptance if pois is large enough */
            if (pois >= bigL) {
                return pois;
            }
            /* Step S. squeeze acceptance */
            fk = pois;
            difmuk = mu - fk;
            u = rand.unifRand(); /* ~ U(0,1) - sample */
            if (d * u >= difmuk * difmuk * difmuk) {
                return pois;
            }
        }

        /*
         * Step P. preparations for steps Q and H. (recalculations of parameters if necessary)
         */

        if (newBigMu || mu != muprev2) {
            /*
             * Careful! muprev2 is not always == muprev because one might have exited in step I or S
             */
            muprev2 = mu;
            omega = M_1_SQRT_2PI / s;
            /*
             * The quantities b1, b2, c3, c2, c1, c0 are for the Hermite approximations to the
             * discrete normal probabilities fk.
             */

            b1 = one_24 / mu;
            b2 = 0.3 * b1 * b1;
            c3 = one_7 * b1 * b2;
            c2 = b2 - 15. * c3;
            c1 = b1 - 6. * b2 + 45. * c3;
            c0 = 1. - b1 + 3. * b2 - 15. * c3;
            c = 0.1069 / mu; /* guarantees majorization by the 'hat'-function. */
        }

        boolean gotoStepF = false;
        if (g >= 0.) {
            /* 'Subroutine' F is called (kflag=0 for correct return) */
            kflag = 0;
            gotoStepF = true;
            // goto Step_F;
        }

        while (true) {
            if (!gotoStepF) {
                /* Step E. Exponential Sample */
                e = rand.expRand(); /* ~ Exp(1) (standard exponential) */

                /*
                 * sample t from the laplace 'hat' (if t <= -0.6744 then pk < fk for all mu >= 10.)
                 */
                u = 2 * rand.unifRand() - 1.;
                t = 1.8 + StatsUtil.fsign(e, u);
            }

            if (t > -0.6744 || gotoStepF) {
                if (!gotoStepF) {
                    pois = Math.floor(mu + s * t);
                    fk = pois;
                    difmuk = mu - fk;

                    /* 'subroutine' F is called (kflag=1 for correct return) */
                    kflag = 1;
                }

                // Step_F: /* 'subroutine' F : calculation of px,py,fx,fy. */
                gotoStepF = false;

                if (pois < 10) { /* use factorials from table fact[] */
                    px = -mu;
                    py = Math.pow(mu, pois) / fact[(int) pois];
                } else {
                    /*
                     * Case pois >= 10 uses polynomial approximation a0-a7 for accuracy when
                     * advisable
                     */
                    del = one_12 / fk;
                    del = del * (1. - 4.8 * del * del);
                    v = difmuk / fk;
                    if (TOMS708.fabs(v) <= 0.25) {
                        px = fk * v * v * (((((((a7 * v + a6) * v + a5) * v + a4) *
                                        v + a3) * v + a2) * v + a1) * v + a0) - del;
                    } else { /* |v| > 1/4 */
                        px = fk * Math.log(1. + v) - difmuk - del;
                    }
                    py = M_1_SQRT_2PI / Math.sqrt(fk);
                }
                x = (0.5 - difmuk) / s;
                x *= x; /* x^2 */
                fx = -0.5 * x;
                fy = omega * (((c3 * x + c2) * x + c1) * x + c0);
                if (kflag > 0) {
                    /* Step H. Hat acceptance (E is repeated on rejection) */
                    if (c * TOMS708.fabs(u) <= py * Math.exp(px + e) - fy * Math.exp(fx + e)) {
                        break;
                    }
                } else {
                    /* Step Q. Quotient acceptance (rare case) */
                    if (fy - u * fy <= py * Math.exp(px - fx)) {
                        break;
                    }
                }
            } /* t > -.67.. */
        }
        return pois;
    }
}
