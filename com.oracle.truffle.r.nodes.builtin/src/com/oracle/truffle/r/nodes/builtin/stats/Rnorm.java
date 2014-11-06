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
package com.oracle.truffle.r.nodes.builtin.stats;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;
import static com.oracle.truffle.r.nodes.builtin.stats.StatsUtil.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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
@RBuiltin(name = "rnorm", kind = SUBSTITUTE, parameterNames = {"n", "mean", "sd"})
// TODO INTERNAL
public abstract class Rnorm extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{null, ConstantNode.create(0d), ConstantNode.create(1d)};
    }

    @Specialization
    @TruffleBoundary
    protected RDoubleVector rnorm(int n, double mean, double standardd) {
        controlVisibility();
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = generateNorm(mean, standardd);
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    @TruffleBoundary
    protected RDoubleVector rnorm(int n, int mean, int standardd) {
        controlVisibility();
        return rnorm(n, (double) mean, (double) standardd);
    }

    @Specialization
    @TruffleBoundary
    protected RDoubleVector rnorm(double n, double mean, double standardd) {
        controlVisibility();
        return rnorm((int) n, mean, standardd);
    }

    // from GNUR: rnorm.c
    private static double generateNorm(double mean, double standardd) {
        return mean + standardd * normRand();
    }

    // from GNUR: snorm.c
    private static double normRand() {
        double u1;

        // case INVERSION:
        double big = 134217728; /* 2^27 */
        /* unif_rand() alone is not of high enough precision */
        u1 = RRNG.unifRand();
        u1 = (int) (big * u1) + RRNG.unifRand();
        return qnorm5(u1 / big, 0.0, 1.0, true, false);
    }

    // from GNUR: qnorm.c
    public static double qnorm5(double p, double mu, double sigma, boolean lowerTail, boolean logP) {
        double pU;
        double q;
        double r;
        double val;

        // R_Q_P01_boundaries(p, ML_NEGINF, ML_POSINF);

        // if(sigma < 0) { ML_ERR_return_NAN; }
        if (sigma == 0) {
            return mu;
        }

        pU = rdtqiv(p, lowerTail, logP); /* real lower_tail prob. p */
        q = pU - 0.5;

        if (Math.abs(q) <= .425) { /* 0.075 <= p <= 0.925 */
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
                r = rdtciv(p, lowerTail, logP); /* 1-p */
            } else {
                r = pU; /* = R_DT_Iv(p) ^= p */
            }
            r = Math.sqrt(-((logP && ((lowerTail && q <= 0) || (!lowerTail && q > 0))) ? p : /* else */Math.log(r)));
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

}
