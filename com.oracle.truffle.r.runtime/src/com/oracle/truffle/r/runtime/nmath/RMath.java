/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2012, The R Core Team
 * Copyright (c) 2004, The R Foundation
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath;

import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_LN_SQRT_2PI;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.nmath.RMathError.MLError;

/**
 * Encapsulates functions to be found in Rmath.h or in nmath directory in GnuR except for random
 * distribution related functions, which usually have their own files, and other well defined groups
 * of functions/macros referenced below.
 *
 * @see DPQ
 * @see RMathError
 * @see MathConstants
 */
public final class RMath {

    public static boolean mlValid(double d) {
        return !Double.isNaN(d);
    }

    /**
     * Implementation of {@code R_forceint}, which is not equal to {@code Math.round}, because it
     * returns {@code double} and so it can handle values that do not fit into long.
     */
    public static double forceint(double x) {
        // Note: in GnuR this is alias for nearbyint
        if (Double.isNaN(x)) {
            return 0;
        }
        return Math.floor(x + 0.5);
    }

    /**
     * Implementation of C routine {@code round}, which is not equal to {@code Math.round}, because
     * it returns {@code double} and so it can handle values that do not fit into long.
     */
    public static double round(double x) {
        return forceint(x);
    }

    public static double fsign(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return x + y;
        }
        return ((y >= 0) ? TOMS708.fabs(x) : -TOMS708.fabs(x));
    }

    private static double fmod(double a, double b) {
        double q = a / b;
        if (b != 0) {
            double tmp = a - Math.floor(q) * b;
            if (RRuntime.isFinite(q) && Math.abs(q) > 1 / RRuntime.EPSILON) {
                // TODO support warning here
                throw new UnsupportedOperationException();
            }
            return tmp - Math.floor(tmp / b) * b;
        } else {
            return Double.NaN;
        }
    }

    public static double tanpi(double x) {
        if (Double.isNaN(x)) {
            return x;
        }
        if (!Double.isFinite(x)) {
            return RMathError.defaultError();
        }

        double x2 = fmod(x, 1.); // tan(pi(x + k)) == tan(pi x) for all integer k
        // map (-1,1) --> (-1/2, 1/2] :
        if (x2 <= -0.5) {
            x2++;
        } else if (x2 > 0.5) {
            x2--;
        }
        return (x2 == 0.) ? 0. : ((x2 == 0.5) ? Double.NaN : Math.tan(MathConstants.M_PI * x2));
    }

    //
    // GNUR from fmin2.c and fmax2
    //

    public static double fmax2(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return x + y;
        }
        return (x < y) ? y : x;
    }

    public static double fmin2(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return x + y;
        }
        return (x < y) ? x : y;
    }

    //
    // GNUR from expm1.c
    //

    public static double expm1(double x) {
        double y;
        double a = Math.abs(x);

        if (a < MathConstants.DBL_EPSILON) {
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
        y -= (1 + y) * (RMath.log1p(y) - x);
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
            if (Math.abs(x) < .5 * MathConstants.DBL_EPSILON) {
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
            RMathError.error(MLError.PRECISION, "log1p");
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
            return RMathError.defaultError();
        }

        if (x < -1.1 || x > 1.1) {
            return RMathError.defaultError();
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

    //
    // stirlerr
    //

    private static final double S0 = 0.083333333333333333333; /* 1/12 */
    private static final double S1 = 0.00277777777777777777778; /* 1/360 */
    private static final double S2 = 0.00079365079365079365079365; /* 1/1260 */
    private static final double S3 = 0.000595238095238095238095238; /* 1/1680 */
    private static final double S4 = 0.0008417508417508417508417508; /* 1/1188 */

    /*
     * error for 0, 0.5, 1.0, 1.5, ..., 14.5, 15.0.
     */
    @CompilationFinal private static final double[] sferr_halves = new double[]{
                    0.0, /* n=0 - wrong, place holder only */
                    0.1534264097200273452913848, /* 0.5 */
                    0.0810614667953272582196702, /* 1.0 */
                    0.0548141210519176538961390, /* 1.5 */
                    0.0413406959554092940938221, /* 2.0 */
                    0.03316287351993628748511048, /* 2.5 */
                    0.02767792568499833914878929, /* 3.0 */
                    0.02374616365629749597132920, /* 3.5 */
                    0.02079067210376509311152277, /* 4.0 */
                    0.01848845053267318523077934, /* 4.5 */
                    0.01664469118982119216319487, /* 5.0 */
                    0.01513497322191737887351255, /* 5.5 */
                    0.01387612882307074799874573, /* 6.0 */
                    0.01281046524292022692424986, /* 6.5 */
                    0.01189670994589177009505572, /* 7.0 */
                    0.01110455975820691732662991, /* 7.5 */
                    0.010411265261972096497478567, /* 8.0 */
                    0.009799416126158803298389475, /* 8.5 */
                    0.009255462182712732917728637, /* 9.0 */
                    0.008768700134139385462952823, /* 9.5 */
                    0.008330563433362871256469318, /* 10.0 */
                    0.007934114564314020547248100, /* 10.5 */
                    0.007573675487951840794972024, /* 11.0 */
                    0.007244554301320383179543912, /* 11.5 */
                    0.006942840107209529865664152, /* 12.0 */
                    0.006665247032707682442354394, /* 12.5 */
                    0.006408994188004207068439631, /* 13.0 */
                    0.006171712263039457647532867, /* 13.5 */
                    0.005951370112758847735624416, /* 14.0 */
                    0.005746216513010115682023589, /* 14.5 */
                    0.005554733551962801371038690 /* 15.0 */
    };

    public static double stirlerr(double n) {
        double nn;

        if (n <= 15.0) {
            nn = n + n;
            if (nn == (int) nn) {
                return (sferr_halves[(int) nn]);
            }
            return (GammaFunctions.lgammafn(n + 1.) - (n + 0.5) * Math.log(n) + n - M_LN_SQRT_2PI);
        }

        nn = n * n;
        if (n > 500) {
            return ((S0 - S1 / nn) / n);
        }
        if (n > 80) {
            return ((S0 - (S1 - S2 / nn) / nn) / n);
        }
        if (n > 35) {
            return ((S0 - (S1 - (S2 - S3 / nn) / nn) / nn) / n);
        }
        /* 15 < n <= 35 : */
        return ((S0 - (S1 - (S2 - (S3 - S4 / nn) / nn) / nn) / nn) / n);
    }

    //
    // bd0
    //

    /**
     * Evaluates the "deviance part". Transcribed from GnuR.
     */
    public static double bd0(double x, double np) {
        double ej;
        double s;
        double s1;
        double v;
        int j;

        if (!RRuntime.isFinite(x) || !RRuntime.isFinite(np) || np == 0.0) {
            return RMathError.defaultError();
        }

        if (Math.abs(x - np) < 0.1 * (x + np)) {
            v = (x - np) / (x + np);
            s = (x - np) * v; /* s using v -- change by MM */
            ej = 2 * x * v;
            v = v * v;
            for (j = 1;; j++) { /* Taylor series */
                ej *= v;
                s1 = s + ej / ((j << 1) + 1);
                if (s1 == s) { /* last term was effectively 0 */
                    return s1;
                }
                s = s1;
            }
        }
        /* else: | x - np | is not too small */
        return x * Math.log(x / np) + np - x;
    }
}
