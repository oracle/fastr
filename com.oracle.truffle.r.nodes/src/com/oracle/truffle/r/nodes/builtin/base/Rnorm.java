/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.rng.*;

/*
 * Logic derived from GNU-R, see inline comments.
 */
@RBuiltin("rnorm")
public abstract class Rnorm extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"n", "mean", "sd"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{null, ConstantNode.create(0d), ConstantNode.create(1d)};
    }

    @Specialization
    public RDoubleVector rnorm(int n, double mean, double standardd) {
        controlVisibility();
        RRNG.Generator rng = RRNG.get();
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = generateNorm(mean, standardd, rng);
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    public RDoubleVector rnorm(int n, int mean, int standardd) {
        controlVisibility();
        return rnorm(n, (double) mean, (double) standardd);
    }

    @Specialization
    public RDoubleVector rnorm(double n, double mean, double standardd) {
        controlVisibility();
        return rnorm((int) n, mean, standardd);
    }

    // from GNUR: rnorm.c
    private static double generateNorm(double mean, double standardd, RRNG.Generator rng) {
        return mean + standardd * normRand(rng);
    }

    // from GNUR: snorm.c
    private static double normRand(RRNG.Generator rng) {
        double u1;

        // case INVERSION:
        double big = 134217728; /* 2^27 */
        /* unif_rand() alone is not of high enough precision */
        u1 = rng.genrandDouble();
        u1 = (int) (big * u1) + rng.genrandDouble();
        return qnorm5(u1 / big, 0.0, 1.0, 1, 0);
    }

    // from GNUR: qnorm.c
    private static double qnorm5(double p, double mu, double sigma, int lowerTail, int logP) {
        double pU;
        double q;
        double r;
        double val;

        // R_Q_P01_boundaries(p, ML_NEGINF, ML_POSINF);

        // if(sigma < 0) { ML_ERR_return_NAN; }
        if (sigma == 0) {
            return mu;
        }

        pU = rDTqIv(p, logP, lowerTail); /* real lower_tail prob. p */
        q = pU - 0.5;

        if (fabs(q) <= .425) { /* 0.075 <= p <= 0.925 */
            r = .180625 - q * q;
            val = q *
                            (((((((r * 2509.0809287301226727 + 33430.575583588128105) * r + 67265.770927008700853) * r + 45921.953931549871457) * r + 13731.693765509461125) * r + 1971.5909503065514427) *
                                            r + 133.14166789178437745) *
                                            r + 3.387132872796366608) /
                            (((((((r * 5226.495278852854561 + 28729.085735721942674) * r + 39307.89580009271061) * r + 21213.794301586595867) * r + 5394.1960214247511077) * r + 687.1870074920579083) *
                                            r + 42.313330701600911252) *
                                            r + 1.);
        } else { /* closer than 0.075 from {0,1} boundary */

            /* r = min(p, 1-p) < 0.075 */
            if (q > 0) {
                r = rDTCIv(p, logP, lowerTail); /* 1-p */
            } else {
                r = pU; /* = R_DT_Iv(p) ^= p */
            }
            r = Math.sqrt(-((logP != 0 && ((lowerTail != 0 && q <= 0) || (lowerTail == 0 && q > 0))) ? p : /* else */Math.log(r)));
            /* r = sqrt(-log(r)) <==> min(p, 1-p) = exp( - r^2 ) */

            if (r <= 5.) { /* <==> min(p,1-p) >= exp(-25) ~= 1.3888e-11 */
                r += -1.6;
                val = (((((((r * 7.7454501427834140764e-4 + .0227238449892691845833) * r + .24178072517745061177) * r + 1.27045825245236838258) * r + 3.64784832476320460504) * r + 5.7694972214606914055) *
                                r + 4.6303378461565452959) *
                                r + 1.42343711074968357734) /
                                (((((((r * 1.05075007164441684324e-9 + 5.475938084995344946e-4) * r + .0151986665636164571966) * r + .14810397642748007459) * r + .68976733498510000455) * r + 1.6763848301838038494) *
                                                r + 2.05319162663775882187) *
                                                r + 1.);
            } else { /* very close to 0 or 1 */
                r += -5.;
                val = (((((((r * 2.01033439929228813265e-7 + 2.71155556874348757815e-5) * r + .0012426609473880784386) * r + .026532189526576123093) * r + .29656057182850489123) * r + 1.7848265399172913358) *
                                r + 5.4637849111641143699) *
                                r + 6.6579046435011037772) /
                                (((((((r * 2.04426310338993978564e-15 + 1.4215117583164458887e-7) * r + 1.8463183175100546818e-5) * r + 7.868691311456132591e-4) * r + .0148753612908506148525) * r + .13692988092273580531) *
                                                r + .59983220655588793769) *
                                                r + 1.);
            }

            if (q < 0.0) {
                val = -val;
            }
            /* return (q >= 0.)? r : -r ; */
        }
        return mu + sigma * val;
    }

    private static double rDTCIv(double p, double logP, double lowerTail) {
        return (logP != 0 ? (lowerTail != 0 ? -expm1(p) : Math.exp(p)) : rDCval(p, lowerTail));
    }

    private static double rDCval(double p, double lowerTail) {
        return (lowerTail != 0 ? (0.5 - (p) + 0.5) : (p));
    }

    private static double fabs(double d) {
        return Math.abs(d); // TODO is this correct?
    }

    private static double rDTqIv(double p, double logP, double lowerTail) {
        return logP != 0 ? (lowerTail != 0 ? Math.exp(p) : -expm1(p)) : rDLval(p, lowerTail);
    }

    private static double rDLval(double p, double lowerTail) {
        return (lowerTail != 0 ? (p) : (0.5 - (p) + 0.5));
    }

    public static final double DBLEPSILON = 1E-9;

    // GNUR from expm1.c
    private static double expm1(double x) {
        double y;
        double a = fabs(x);

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

    // GNUR from log1p.c
    private static final double[] alnrcs = {+.10378693562743769800686267719098e+1, -.13364301504908918098766041553133e+0, +.19408249135520563357926199374750e-1, -.30107551127535777690376537776592e-2,
                    +.48694614797154850090456366509137e-3, -.81054881893175356066809943008622e-4, +.13778847799559524782938251496059e-4, -.23802210894358970251369992914935e-5,
                    +.41640416213865183476391859901989e-6, -.73595828378075994984266837031998e-7, +.13117611876241674949152294345011e-7, -.23546709317742425136696092330175e-8,
                    +.42522773276034997775638052962567e-9, -.77190894134840796826108107493300e-10, +.14075746481359069909215356472191e-10, -.25769072058024680627537078627584e-11,
                    +.47342406666294421849154395005938e-12, -.87249012674742641745301263292675e-13, +.16124614902740551465739833119115e-13, -.29875652015665773006710792416815e-14,
                    +.55480701209082887983041321697279e-15, -.10324619158271569595141333961932e-15, +.19250239203049851177878503244868e-16, -.35955073465265150011189707844266e-17,
                    +.67264542537876857892194574226773e-18, -.12602624168735219252082425637546e-18, +.23644884408606210044916158955519e-19, -.44419377050807936898878389179733e-20,
                    +.83546594464034259016241293994666e-21, -.15731559416479562574899253521066e-21, +.29653128740247422686154369706666e-22, -.55949583481815947292156013226666e-23,
                    +.10566354268835681048187284138666e-23, -.19972483680670204548314999466666e-24, +.37782977818839361421049855999999e-25, -.71531586889081740345038165333333e-26,
                    +.13552488463674213646502024533333e-26, -.25694673048487567430079829333333e-27, +.48747756066216949076459519999999e-28, -.92542112530849715321132373333333e-29,
                    +.17578597841760239233269760000000e-29, -.33410026677731010351377066666666e-30, +.63533936180236187354180266666666e-31};

    // GNUR from log1p.c
    private static double log1p(double x) {
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

        if (fabs(x) <= .375) {
            /*
             * Improve on speed (only); again give result accurate to IEEE double precision:
             */
            if (fabs(x) < .5 * DBLEPSILON) {
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
            throw new RuntimeException("ERROR: ML_ERROR(ME_PRECISION, \"log1p\")");
        }
        return Math.log(1 + x);
    }

    private static double chebyshevEval(double x, double[] a, final int n) {
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
