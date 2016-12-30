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

import static com.oracle.truffle.r.library.stats.TOMS708.fabs;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

public final class RGamma extends RandFunction2_Double {
    private static final double sqrt32 = 5.656854;
    private static final double exp_m1 = 0.36787944117144232159; /* exp(-1) = 1/e */

    /*
     * Coefficients q[k] - for q0 = sum(q[k]*a^(-k)) Coefficients a[k] - for q =
     * q0+(t*t/2)*sum(a[k]*v^k) Coefficients e[k] - for exp(q)-1 = sum(e[k]*q^k)
     */
    private static final double q1 = 0.04166669;
    private static final double q2 = 0.02083148;
    private static final double q3 = 0.00801191;
    private static final double q4 = 0.00144121;
    private static final double q5 = -7.388e-5;
    private static final double q6 = 2.4511e-4;
    private static final double q7 = 2.424e-4;

    private static final double a1 = 0.3333333;
    private static final double a2 = -0.250003;
    private static final double a3 = 0.2000062;
    private static final double a4 = -0.1662921;
    private static final double a5 = 0.1423657;
    private static final double a6 = -0.1367177;
    private static final double a7 = 0.1233795;

    @Override
    public double execute(double a, double scale, RandomNumberProvider rand) {

        // TODO: state variables
        double aa = Double.NaN;
        double aaa = Double.NaN;
        double s = 0;
        double s2 = 0;
        double d = 0; /* no. 1 (step 1) */
        double q0 = 0;
        double b = 0;
        double si = 0;
        double c = 0; /* no. 2 (step 4) */

        double e;
        double p;
        double q;
        double r;
        double t;
        double u;
        double v;
        double w;
        double x;
        double retVal;

        if (Double.isNaN(a) || Double.isNaN(scale)) {
            return RMathError.defaultError();
        }
        if (a <= 0.0 || scale <= 0.0) {
            if (scale == 0. || a == 0.) {
                return 0.;
            }
            return RMathError.defaultError();
        }
        if (!Double.isFinite(a) || !Double.isFinite(scale)) {
            return Double.POSITIVE_INFINITY;
        }

        if (a < 1.) { /* GS algorithm for parameters a < 1 */
            e = 1.0 + exp_m1 * a;
            while (true) {
                p = e * rand.unifRand();
                if (p >= 1.0) {
                    x = -Math.log((e - p) / a);
                    if (rand.expRand() >= (1.0 - a) * Math.log(x)) {
                        break;
                    }
                } else {
                    x = Math.exp(Math.log(p) / a);
                    if (rand.expRand() >= x) {
                        break;
                    }
                }
            }
            return scale * x;
        }

        /* --- a >= 1 : GD algorithm --- */

        /* Step 1: Recalculations of s2, s, d if a has changed */
        if (a != aa) {
            aa = a;
            s2 = a - 0.5;
            s = Math.sqrt(s2);
            d = sqrt32 - s * 12.0;
        }
        /*
         * Step 2: t = standard normal deviate, x = (s,1/2) -normal deviate.
         */

        /* immediate acceptance (i) */
        t = rand.normRand();
        x = s + 0.5 * t;
        retVal = x * x;
        if (t >= 0.0) {
            return scale * retVal;
        }

        /* Step 3: u = 0,1 - uniform sample. squeeze acceptance (s) */
        u = rand.unifRand();
        if (d * u <= Math.pow(t, 3)) {
            return scale * retVal;
        }

        /* Step 4: recalculations of q0, b, si, c if necessary */

        if (a != aaa) {
            aaa = a;
            r = 1.0 / a;
            q0 = ((((((q7 * r + q6) * r + q5) * r + q4) * r + q3) * r + q2) * r + q1) * r;

            /* Approximation depending on size of parameter a */
            /* The constants in the expressions for b, si and c */
            /* were established by numerical experiments */

            if (a <= 3.686) {
                b = 0.463 + s + 0.178 * s2;
                si = 1.235;
                c = 0.195 / s - 0.079 + 0.16 * s;
            } else if (a <= 13.022) {
                b = 1.654 + 0.0076 * s2;
                si = 1.68 / s + 0.275;
                c = 0.062 / s + 0.024;
            } else {
                b = 1.77;
                si = 0.75;
                c = 0.1515 / s;
            }
        }
        /* Step 5: no quotient test if x not positive */

        if (x > 0.0) {
            /* Step 6: calculation of v and quotient q */
            v = t / (s + s);
            if (fabs(v) <= 0.25) {
                q = q0 + 0.5 * t * t * ((((((a7 * v + a6) * v + a5) * v + a4) * v + a3) * v + a2) * v + a1) * v;
            } else {
                q = q0 - s * t + 0.25 * t * t + (s2 + s2) * Math.log(1.0 + v);
            }

            /* Step 7: quotient acceptance (q) */
            if (Math.log(1.0 - u) <= q) {
                return scale * retVal;
            }
        }

        while (true) {
            /*
             * Step 8: e = standard exponential deviate u = 0,1 -uniform deviate t = (b,si)-double
             * exponential (laplace) sample
             */
            e = rand.expRand();
            u = rand.unifRand();
            u = u + u - 1.0;
            if (u < 0.0) {
                t = b - si * e;
            } else {
                t = b + si * e;
            }
            /* Step 9: rejection if t < tau(1) = -0.71874483771719 */
            if (t >= -0.71874483771719) {
                /* Step 10: calculation of v and quotient q */
                v = t / (s + s);
                if (fabs(v) <= 0.25) {
                    q = q0 + 0.5 * t * t *
                                    ((((((a7 * v + a6) * v + a5) * v + a4) * v + a3) * v + a2) * v + a1) * v;
                } else {
                    q = q0 - s * t + 0.25 * t * t + (s2 + s2) * Math.log(1.0 + v);
                }
                /* Step 11: hat acceptance (h) */
                /* (if q not positive go to step 8) */
                if (q > 0.0) {
                    w = RMath.expm1(q);
                    /* ^^^^^ original code had approximation with rel.err < 2e-7 */
                    /* if t is rejected sample again at step 8 */
                    if (c * fabs(u) <= w * Math.exp(e - 0.5 * t * t)) {
                        break;
                    }
                }
            }
        } /* repeat .. until `t' is accepted */
        x = s + 0.5 * t;
        return scale * x * x;
    }
}
