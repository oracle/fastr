/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2012, The R Core Team
 * Copyright (c) 2003-2008, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class RMultinom {
    private RMultinom() {
        // only static method rmultinom
    }

    /**
     * Returns true if no element of the vector rN got assigned value NA, i.e. is stayed complete if
     * it was before. GnuR doc: `Return' vector rN[1:K] {K := length(prob)} where rN[j] ~ Bin(n,
     * prob[j]) , sum_j rN[j] == n, sum_j prob[j] == 1.
     */
    @TruffleBoundary
    public static boolean rmultinom(int nIn, double[] prob, int maxK, int[] rN, int rnStartIdx, RandomNumberProvider rand, Rbinom rbinom) {
        /*
         * This calculation is sensitive to exact values, so we try to ensure that the calculations
         * are as accurate as possible so different platforms are more likely to give the same
         * result.
         */

        int n = nIn;
        if (RRuntime.isNA(maxK) || maxK < 1 || RRuntime.isNA(n) || n < 0) {
            if (rN.length > rnStartIdx) {
                rN[rnStartIdx] = RRuntime.INT_NA;
            }
            return false;
        }

        /*
         * Note: prob[K] is only used here for checking sum_k prob[k] = 1 ; Could make loop one
         * shorter and drop that check !
         */
        /* LDOUBLE */double pTot = 0.;
        for (int k = 0; k < maxK; k++) {
            double pp = prob[k];
            if (!Double.isFinite(pp) || pp < 0. || pp > 1.) {
                rN[rnStartIdx + k] = RRuntime.INT_NA;
                return false;
            }
            pTot += pp;
            rN[rnStartIdx + k] = 0;
        }

        /* LDOUBLE */double probSum = Math.abs(pTot - 1);
        if (probSum > 1e-7) {
            throw RError.error(SHOW_CALLER, Message.GENERIC, String.format("rbinom: probability sum should be 1, but is %g", pTot));
        }
        if (n == 0) {
            return true;
        }
        if (maxK == 1 && pTot == 0.) {
            return true; /* trivial border case: do as rbinom */
        }

        /* Generate the first K-1 obs. via binomials */
        for (int k = 0; k < maxK - 1; k++) {
            /* (p_tot, n) are for "remaining binomial" */
            /* LDOUBLE */double probK = prob[k];
            if (probK != 0.) {
                double pp = probK / pTot;
                // System.out.printf("[%d] %.17f\n", k + 1, pp);
                rN[rnStartIdx + k] = ((pp < 1.) ? (int) rbinom.execute(n, pp, rand) :
                /* >= 1; > 1 happens because of rounding */
                                n);
                n -= rN[rnStartIdx + k];
            } else {
                rN[rnStartIdx + k] = 0;
            }
            if (n <= 0) {
                /* we have all */
                return true;
            }
            /* i.e. = sum(prob[(k+1):K]) */
            pTot -= probK;
        }

        rN[rnStartIdx + maxK - 1] = n;
        return true;
    }
}
