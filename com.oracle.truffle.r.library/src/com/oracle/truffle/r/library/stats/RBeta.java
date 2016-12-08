/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.DBL_MAX_EXP;
import static com.oracle.truffle.r.library.stats.MathConstants.M_LN2;
import static com.oracle.truffle.r.library.stats.RMath.fmax2;
import static com.oracle.truffle.r.library.stats.RMath.fmin2;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;

public final class RBeta implements RandFunction2_Double {

    private static final double expmax = (DBL_MAX_EXP * M_LN2); /* = log(DBL_MAX) */

    @Override
    public double evaluate(double aa, double bb, RandomNumberProvider rand) {
        if (Double.isNaN(aa) || Double.isNaN(bb) || aa < 0. || bb < 0.) {
            return RMath.mlError();
        }
        if (!Double.isFinite(aa) && !Double.isFinite(bb)) { // a = b = Inf : all mass at 1/2
            return 0.5;
        }
        if (aa == 0. && bb == 0.) { // point mass 1/2 at each of {0,1} :
            return (rand.unifRand() < 0.5) ? 0. : 1.;
        }
        // now, at least one of a, b is finite and positive
        if (!Double.isFinite(aa) || bb == 0.) {
            return 1.0;
        }
        if (!Double.isFinite(bb) || aa == 0.) {
            return 0.0;
        }

        double a;
        double b;
        double r;
        double s;
        double t;
        double u1;
        double u2;
        double v = 0;
        double w = 0;
        double y;
        double z;

        // TODO: state variables
        double beta = 0;
        double gamma = 1;
        double delta;
        double k1 = 0;
        double k2 = 0;
        double olda = -1.0;
        double oldb = -1.0;

        /* Test if we need new "initializing" */
        boolean qsame = (olda == aa) && (oldb == bb);
        if (!qsame) {
            olda = aa;
            oldb = bb;
        }

        a = fmin2(aa, bb);
        b = fmax2(aa, bb); /* a <= b */
        double alpha = a + b;

        if (a <= 1.0) { /* --- Algorithm BC --- */
            /* changed notation, now also a <= b (was reversed) */
            if (!qsame) { /* initialize */
                beta = 1.0 / a;
                delta = 1.0 + b - a;
                k1 = delta * (0.0138889 + 0.0416667 * a) / (b * beta - 0.777778);
                k2 = 0.25 + (0.5 + 0.25 / delta) * a;
            }
            /* FIXME: "do { } while()", but not trivially because of "continue"s: */
            for (;;) {
                u1 = rand.unifRand();
                u2 = rand.unifRand();
                if (u1 < 0.5) {
                    y = u1 * u2;
                    z = u1 * y;
                    if (0.25 * u2 + z - y >= k1) {
                        continue;
                    }
                } else {
                    z = u1 * u1 * u2;
                    if (z <= 0.25) {
                        v = beta * Math.log(u1 / (1.0 - u1));
                        w = wFromU1Bet(b, v, w);
                        break;
                    }
                    if (z >= k2) {
                        continue;
                    }
                }

                v = beta * Math.log(u1 / (1.0 - u1));
                w = wFromU1Bet(b, v, w);

                if (alpha * (Math.log(alpha / (a + w)) + v) - 1.3862944 >= Math.log(z)) {
                    break;
                }
            }
            return (aa == a) ? a / (a + w) : w / (a + w);

        } else { /* Algorithm BB */

            if (!qsame) { /* initialize */
                beta = Math.sqrt((alpha - 2.0) / (2.0 * a * b - alpha));
                gamma = a + 1.0 / beta;
            }
            do {
                u1 = rand.unifRand();
                u2 = rand.unifRand();

                v = beta * Math.log(u1 / (1.0 - u1));
                w = wFromU1Bet(a, v, w);

                z = u1 * u1 * u2;
                r = gamma * v - 1.3862944;
                s = a + r - w;
                if (s + 2.609438 >= 5.0 * z) {
                    break;
                }
                t = Math.log(z);
                if (s > t) {
                    break;
                }
            } while (r + alpha * Math.log(alpha / (b + w)) < t);

            return (aa != a) ? b / (b + w) : w / (b + w);
        }
    }

    private static double wFromU1Bet(double aa, double v, double w) {
        if (v <= expmax) {
            w = aa * Math.exp(v);
            if (!Double.isFinite(w)) {
                w = Double.MAX_VALUE;
            }
        } else {
            w = Double.MAX_VALUE;
        }
        return w;
    }

}
