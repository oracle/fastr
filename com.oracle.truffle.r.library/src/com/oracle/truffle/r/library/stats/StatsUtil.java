/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2012, The R Core Team
 * Copyright (c) 2004, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * Auxiliary functions and constants from GNU R. These are originally found in the source files
 * mentioned in the code.
 */
public class StatsUtil {

    /**
     * corresponds to macro {@code ML_ERR_return_NAN} in GnuR.
     */
    public static double mlError() {
        return Double.NaN;
    }

    public static final double DBLEPSILON = 2.2204460492503131e-16;

    @TruffleBoundary
    private static void fail(String message) {
        throw new RuntimeException(message);
    }

    // Constants and auxiliary functions originate from dpq.h and Rmath.h.

    public static final double M_LN2 = 0.693147180559945309417232121458;

    public static final double M_PI = 3.141592653589793238462643383280;

    public static final double M_2PI = 6.283185307179586476925286766559;

    public static final double M_1_SQRT_2PI = 0.398942280401432677939946059934;

    public static final double M_SQRT_32 = 5.656854249492380195206754896838;

    public static final double M_LOG10_2 = 0.301029995663981195213738894724;

    public static final double DBL_MANT_DIG = 53;

    public static final int DBL_MAX_EXP = 1024;

    public static final int DBL_MIN_EXP = -1021;

    public static double rdtlog(double p, boolean lowerTail, boolean logp) {
        return lowerTail ? rdlog(p, logp) : rdlexp(p, logp);
    }

    public static double rdlog(double p, boolean logp) {
        return logp ? p : Math.log(p);
    }

    public static double rdlexp(double x, boolean logp) {
        return logp ? rlog1exp(x) : log1p(-x);
    }

    public static double rlog1exp(double x) {
        return x > -M_LN2 ? Math.log(-expm1(x)) : log1p(-Math.exp(x));
    }

    public static double rdtclog(double p, boolean lowerTail, boolean logp) {
        return lowerTail ? rdlexp(p, logp) : rdlog(p, logp);
    }

    public static double rdtqiv(double p, boolean lowerTail, boolean logp) {
        return logp ? (lowerTail ? Math.exp(p) : -expm1(p)) : rdlval(p, lowerTail);
    }

    public static double rdtciv(double p, boolean lowerTail, boolean logp) {
        return logp ? (lowerTail ? -expm1(p) : Math.exp(p)) : rdcval(p, lowerTail);
    }

    public static double rdlval(double p, boolean lowerTail) {
        return lowerTail ? p : (0.5 - (p) + 0.5);
    }

    public static double rdcval(double p, boolean lowerTail) {
        return lowerTail ? (0.5 - p + 0.5) : p;
    }

    public static double rd0(boolean logp) {
        return logp ? Double.NEGATIVE_INFINITY : 0.;
    }

    public static double rd1(boolean logp) {
        return logp ? 0. : 1.;
    }

    public static double rdt0(boolean lowerTail, boolean logp) {
        return lowerTail ? rd0(logp) : rd1(logp);
    }

    public static double rdt1(boolean lowerTail, boolean logp) {
        return lowerTail ? rd1(logp) : rd0(logp);
    }

    public static boolean rqp01check(double p, boolean logp) {
        return (logp && p > 0) || (!logp && (p < 0 || p > 1));
    }

    public static double rdexp(double x, boolean logp) {
        return logp ? x : Math.exp(x);
    }

    public static double rdfexp(double f, double x, boolean giveLog) {
        return giveLog ? -0.5 * Math.log(f) + x : Math.exp(x) / Math.sqrt(f);
    }

    public static double fmax2(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return x + y;
        }
        return (x < y) ? y : x;
    }

    //
    // GNUR from expm1.c
    //

    public static double expm1(double x) {
        double y;
        double a = Math.abs(x);

        if (a < DBLEPSILON) {
            return x;
        }
        if (a > 0.697) {
            return Math.exp(x) - 1; /* negligible cancellation */
        }

        if (a > 1e-8) {
            y = Math.exp(x) - 1;
        } else {
            /* Taylor expansion, more accurate in this range */
            y = (x / 2 + 1) * x;
        }
        /* Newton step for solving log(1 + y) = x for y : */
        /* WARNING: does not work for y ~ -1: bug in 1.5.0 */
        y -= (1 + y) * (log1p(y) - x);
        return y;
    }

    //
    // GNUR from log1p.c
    //

    @CompilationFinal(dimensions = 1) private static final double[] alnrcs = {+.10378693562743769800686267719098e+1, -.13364301504908918098766041553133e+0, +.19408249135520563357926199374750e-1,
                    -.30107551127535777690376537776592e-2, +.48694614797154850090456366509137e-3, -.81054881893175356066809943008622e-4, +.13778847799559524782938251496059e-4,
                    -.23802210894358970251369992914935e-5, +.41640416213865183476391859901989e-6, -.73595828378075994984266837031998e-7, +.13117611876241674949152294345011e-7,
                    -.23546709317742425136696092330175e-8, +.42522773276034997775638052962567e-9, -.77190894134840796826108107493300e-10, +.14075746481359069909215356472191e-10,
                    -.25769072058024680627537078627584e-11, +.47342406666294421849154395005938e-12, -.87249012674742641745301263292675e-13, +.16124614902740551465739833119115e-13,
                    -.29875652015665773006710792416815e-14, +.55480701209082887983041321697279e-15, -.10324619158271569595141333961932e-15, +.19250239203049851177878503244868e-16,
                    -.35955073465265150011189707844266e-17, +.67264542537876857892194574226773e-18, -.12602624168735219252082425637546e-18, +.23644884408606210044916158955519e-19,
                    -.44419377050807936898878389179733e-20, +.83546594464034259016241293994666e-21, -.15731559416479562574899253521066e-21, +.29653128740247422686154369706666e-22,
                    -.55949583481815947292156013226666e-23, +.10566354268835681048187284138666e-23, -.19972483680670204548314999466666e-24, +.37782977818839361421049855999999e-25,
                    -.71531586889081740345038165333333e-26, +.13552488463674213646502024533333e-26, -.25694673048487567430079829333333e-27, +.48747756066216949076459519999999e-28,
                    -.92542112530849715321132373333333e-29, +.17578597841760239233269760000000e-29, -.33410026677731010351377066666666e-30, +.63533936180236187354180266666666e-31};

    public static double log1p(double x) {
        /*
         * series for log1p on the interval -.375 to .375 with weighted error 6.35e-32 log weighted
         * error 31.20 significant figures required 30.93 decimal places required 32.01
         */

        int nlnrel = 22;
        double xmin = -0.999999985;

        if (x == 0.) {
            return 0.;
        } /* speed */
        if (x == -1) {
            return (Double.NEGATIVE_INFINITY);
        }
        if (x < -1) {
            return Double.NaN;
        }

        if (Math.abs(x) <= .375) {
            /*
             * Improve on speed (only); again give result accurate to IEEE double precision:
             */
            if (Math.abs(x) < .5 * DBLEPSILON) {
                return x;
            }

            if ((0 < x && x < 1e-8) || (-1e-9 < x && x < 0)) {
                return x * (1 - .5 * x);
            }
            /* else */
            return x * (1 - x * chebyshevEval(x / .375, alnrcs, nlnrel));
        }
        /* else */
        if (x < xmin) {
            /* answer less than half precision because x too near -1 */
            fail("ERROR: ML_ERROR(ME_PRECISION, \"log1p\")");
        }
        return Math.log(1 + x);
    }

    //
    // chebyshev.c
    //

    public static double chebyshevEval(double x, double[] a, final int n) {
        double b0;
        double b1;
        double b2;
        double twox;
        int i;

        if (n < 1 || n > 1000) {
            return Double.NaN; // ML_ERR_return_NAN;
        }

        if (x < -1.1 || x > 1.1) {
            return Double.NaN; // ML_ERR_return_NAN;
        }

        twox = x * 2;
        b2 = b1 = 0;
        b0 = 0;
        for (i = 1; i <= n; i++) {
            b2 = b1;
            b1 = b0;
            b0 = twox * b1 - b2 + a[n - i];
        }
        return (b0 - b2) * 0.5;
    }
}
