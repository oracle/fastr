/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2009, The R Core Team
 * Copyright (c) 2003--2009, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.M_LN_SQRT_2PI;
import static com.oracle.truffle.r.library.stats.RMath.forceint;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction3_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;

public final class RHyper extends RandFunction3_Double {
    private static final double[] al = {
                    0.0, /* ln(0!)=ln(1) */
                    0.0, /* ln(1!)=ln(1) */
                    0.69314718055994530941723212145817, /* ln(2) */
                    1.79175946922805500081247735838070, /* ln(6) */
                    3.17805383034794561964694160129705, /* ln(24) */
                    4.78749174278204599424770093452324,
                    6.57925121201010099506017829290394,
                    8.52516136106541430016553103634712
                    /*
                     * 10.60460290274525022841722740072165, approx. value below = 10.6046028788027;
                     * rel.error = 2.26 10^{-9}
                     *
                     * FIXME: Use constants and if(n > ..) decisions from ./stirlerr.c ----- will be
                     * even *faster* for n > 500 (or so)
                     */
    };

    // afc(i) := ln( i! ) [Math.logarithm of the factorial i]
    private static double afc(int i) {
        // If (i > 7), use Stirling's approximation, otherwise use table lookup.
        if (i < 0) {
            RError.warning(RError.SHOW_CALLER, Message.GENERIC, String.format("RHyper.java: afc(i), i=%d < 0 -- SHOULD NOT HAPPEN!\n", i));
            return -1;
        }
        if (i <= 7) {
            return al[i];
        }
        // else i >= 8 :
        double di = i;
        double i2 = di * di;
        return (di + 0.5) * Math.log(di) - di + M_LN_SQRT_2PI +
                        (0.0833333333333333 - 0.00277777777777778 / i2) / di;
    }

    /* These should become 'thread_local globals' : */
    private static int ks = -1;
    private static int n1s = -1;
    private static int n2s = -1;
    private static int m;
    private static int minjx;
    private static int maxjx;
    private static int k;
    private static int n1;
    private static int n2; // <- not allowing larger integer par
    private static double tn;

    // II :
    private static double w;
    // III:
    // Checkstyle: stop
    private static double a;
    private static double xl;
    private static double xr;
    private static double lamdl;
    private static double lamdr;
    private static double p1;
    private static double p2;
    private static double p3;
    // // Checkstyle: resume

    private static final double scale = 1e25; // scaling factor against (early) underflow
    private static final double con = 57.5646273248511421;

    private static final double deltal = 0.0078;
    private static final double deltau = 0.0034;

    private final Rbinom rbinom = new Rbinom();

    // rhyper(NR, NB, n) -- NR 'red', NB 'blue', n drawn, how many are 'red'
    @Override
    @TruffleBoundary
    public double execute(double nn1in, double nn2in, double kkin, RandomNumberProvider rand) {
        /* extern double afc(int); */

        int ix; // return value (coerced to double at the very end)

        /* check parameter validity */

        if (!Double.isFinite(nn1in) || !Double.isFinite(nn2in) || !Double.isFinite(kkin)) {
            return RMathError.defaultError();
        }

        double nn1int = forceint(nn1in);
        double nn2int = forceint(nn2in);
        double kkint = forceint(kkin);

        if (nn1int < 0 || nn2int < 0 || kkint < 0 || kkint > nn1int + nn2int) {
            return RMathError.defaultError();
        }
        if (nn1int >= Integer.MAX_VALUE || nn2int >= Integer.MAX_VALUE || kkint >= Integer.MAX_VALUE) {
            /*
             * large n -- evade integer overflow (and inappropriate algorithms) --------
             */
            // FIXME: Much faster to give rbinom() approx when appropriate; -> see Kuensch(1989)
            // Johnson, Kotz,.. p.258 (top) mention the *four* different binomial approximations
            if (kkint == 1.) { // Bernoulli
                return rbinom.execute(kkint, nn1int / (nn1int + nn2int), rand);
            }
            // Slow, but safe: return F^{-1}(U) where F(.) = phyper(.) and U ~ U[0,1]
            return QHyper.qhyper(rand.unifRand(), nn1int, nn2int, kkint, false, false);
        }
        int nn1 = (int) nn1int;
        int nn2 = (int) nn2int;
        int kk = (int) kkint;

        /* if new parameter values, initialize */
        boolean setup1;
        boolean setup2;
        if (nn1 != n1s || nn2 != n2s) {
            setup1 = true;
            setup2 = true;
        } else if (kk != ks) {
            setup1 = false;
            setup2 = true;
        } else {
            setup1 = false;
            setup2 = false;
        }
        if (setup1) {
            n1s = nn1;
            n2s = nn2;
            tn = nn1 + nn2;
            if (nn1 <= nn2) {
                n1 = nn1;
                n2 = nn2;
            } else {
                n1 = nn2;
                n2 = nn1;
            }
        }
        if (setup2) {
            ks = kk;
            if (kk + kk >= tn) {
                k = (int) (tn - kk);
            } else {
                k = kk;
            }
        }
        if (setup1 || setup2) {
            m = (int) ((k + 1.) * (n1 + 1.) / (tn + 2.));
            minjx = Math.max(0, k - n2);
            maxjx = Math.min(n1, k);
        }
        /* generate random variate --- Three basic cases */

        if (minjx == maxjx) { /*
                               * I: degenerate distribution ----------------
                               */
            ix = maxjx;

        } else if (m - minjx < 10) { // II: (Scaled) algorithm HIN
                                     // (inverse transformation)
                                     // ----
            // 25*Math.log(10) = Math.log(scale) { <==> Math.exp(con) == scale }
            if (setup1 || setup2) {
                double lw; // Math.log(w); w = Math.exp(lw) * scale = Math.exp(lw +
                           // Math.log(scale)) = Math.exp(lw + con)
                if (k < n2) {
                    lw = afc(n2) + afc(n1 + n2 - k) - afc(n2 - k) - afc(n1 + n2);
                } else {
                    lw = afc(n1) + afc(k) - afc(k - n2) - afc(n1 + n2);
                }
                w = Math.exp(lw + con);
            }
            L10: while (true) {
                double p = w;
                ix = minjx;
                double u = rand.unifRand() * scale;
                while (u > p) {
                    u -= p;
                    p *= ((double) n1 - ix) * (k - ix);
                    ix++;
                    p = p / ix / (n2 - k + ix);
                    if (ix > maxjx) {
                        continue L10;
                    }
                    // FIXME if(p == 0.) we also "have lost" => goto L10
                }
                break L10;
            }
        } else { /* III : H2PE Algorithm --------------------------------------- */

            double u;
            double v;

            double s;
            if (setup1 || setup2) {
                s = Math.sqrt((tn - k) * k * n1 * n2 / (tn - 1) / tn / tn);

                /* remark: d is defined in reference without int. */
                /* the truncation centers the cell boundaries at 0.5 */

                double d = (int) (1.5 * s) + .5;
                xl = m - d + .5;
                xr = m + d + .5;
                a = afc(m) + afc(n1 - m) + afc(k - m) + afc(n2 - k + m);
                double kl = Math.exp(a - afc((int) (xl)) - afc((int) (n1 - xl)) - afc((int) (k - xl)) - afc((int) (n2 - k + xl)));
                double kr = Math.exp(a - afc((int) (xr - 1)) - afc((int) (n1 - xr + 1)) - afc((int) (k - xr + 1)) - afc((int) (n2 - k + xr - 1)));
                lamdl = -Math.log(xl * (n2 - k + xl) / (n1 - xl + 1) / (k - xl + 1));
                lamdr = -Math.log((n1 - xr + 1) * (k - xr + 1) / xr / (n2 - k + xr));
                p1 = d + d;
                p2 = p1 + kl / lamdl;
                p3 = p2 + kr / lamdr;
            }
            int nUv = 0;
            L30: while (true) {

                u = rand.unifRand() * p3;
                v = rand.unifRand();
                nUv++;
                if (nUv >= 10000) {
                    RError.warning(RError.SHOW_CALLER, Message.GENERIC, String.format("rhyper() branch III: giving up after %d rejections", nUv));
                    return RMathError.defaultError();
                }

                if (u < p1) { /* rectangular region */
                    ix = (int) (xl + u);
                } else if (u <= p2) { /* left tail */
                    ix = (int) (xl + Math.log(v) / lamdl);
                    if (ix < minjx) {
                        continue L30;
                    }
                    v = v * (u - p1) * lamdl;
                } else { /* right tail */
                    ix = (int) (xr - Math.log(v) / lamdr);
                    if (ix > maxjx) {
                        continue L30;
                    }
                    v = v * (u - p2) * lamdr;
                }

                /* acceptance/rejection test */
                boolean reject = true;

                if (m < 100 || ix <= 50) {
                    /* Math.explicit evaluation */
                    /*
                     * The original algorithm (and TOMS 668) have f = f * i * (n2 - k + i) / (n1 -
                     * i) / (k - i); in the (m > ix) case, but the definition of the recurrence
                     * relation on p134 shows that the +1 is needed.
                     */
                    int i;
                    double f = 1.0;
                    if (m < ix) {
                        for (i = m + 1; i <= ix; i++) {
                            f = f * (n1 - i + 1) * (k - i + 1) / (n2 - k + i) / i;
                        }
                    } else if (m > ix) {
                        for (i = ix + 1; i <= m; i++) {
                            f = f * i * (n2 - k + i) / (n1 - i + 1) / (k - i + 1);
                        }
                    }
                    if (v <= f) {
                        reject = false;
                    }
                } else {
                    // Checkstyle: stop
                    double e, g, r, t, y;
                    double de, dg, dr, ds, dt, gl, gu, nk, nm, ub;
                    double xk, xm, xn, y1, ym, yn, yk, alv;
                    // Checkstyle: resume

                    /* squeeze using upper and lower bounds */
                    y = ix;
                    y1 = y + 1.0;
                    ym = y - m;
                    yn = n1 - y + 1.0;
                    yk = k - y + 1.0;
                    nk = n2 - k + y1;
                    r = -ym / y1;
                    s = ym / yn;
                    t = ym / yk;
                    e = -ym / nk;
                    g = yn * yk / (y1 * nk) - 1.0;
                    dg = 1.0;
                    if (g < 0.0) {
                        dg = 1.0 + g;
                    }
                    gu = g * (1.0 + g * (-0.5 + g / 3.0));
                    gl = gu - .25 * (g * g * g * g) / dg;
                    xm = m + 0.5;
                    xn = n1 - m + 0.5;
                    xk = k - m + 0.5;
                    nm = n2 - k + xm;
                    ub = y * gu - m * gl + deltau + xm * r * (1. + r * (-0.5 + r / 3.0)) + xn * s * (1. + s * (-0.5 + s / 3.0)) + xk * t * (1. + t * (-0.5 + t / 3.0)) +
                                    nm * e * (1. + e * (-0.5 + e / 3.0));
                    /* test against upper bound */
                    alv = Math.log(v);
                    if (alv > ub) {
                        reject = true;
                    } else {
                        /* test against lower bound */
                        dr = xm * (r * r * r * r);
                        if (r < 0.0) {
                            dr /= (1.0 + r);
                        }
                        ds = xn * (s * s * s * s);
                        if (s < 0.0) {
                            ds /= (1.0 + s);
                        }
                        dt = xk * (t * t * t * t);
                        if (t < 0.0) {
                            dt /= (1.0 + t);
                        }
                        de = nm * (e * e * e * e);
                        if (e < 0.0) {
                            de /= (1.0 + e);
                        }
                        if (alv < ub - 0.25 * (dr + ds + dt + de) + (y + m) * (gl - gu) - deltal) {
                            reject = false;
                        } else {
                            /*
                             * * Stirling's formula to machine accuracy
                             */
                            reject = !(alv <= (a - afc(ix) - afc(n1 - ix) - afc(k - ix) - afc(n2 - k + ix)));
                        }
                    }
                } // else
                if (!reject) {
                    break L30; // e.g. if (reject) goto L30;
                }
            }
        }

        /* return appropriate variate */
        double ix1 = ix;
        if ((double) kk + (double) kk >= tn) {
            if ((double) nn1 > (double) nn2) {
                ix1 = (double) kk - (double) nn2 + ix1;
            } else {
                ix1 = nn1 - ix1;
            }
        } else {
            if ((double) nn1 > (double) nn2) {
                ix1 = kk - ix1;
            }
        }
        return ix1;
    }
}
