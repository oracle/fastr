/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1999--2014, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.runtime.RError.Message.CALLOC_COULD_NOT_ALLOCATE_INF;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction2_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;

public final class Wilcox {
    private Wilcox() {
        // only static members
    }

    public static final class RWilcox extends RandFunction2_Double {
        @Override
        public double execute(double m, double n, RandomNumberProvider rand) {
            int i;
            int j;
            int k;
            double r;

            /* NaNs propagated correctly */
            if (Double.isNaN(m) || Double.isNaN(n)) {
                return (m + n);
            }
            if (!Double.isFinite(m) || !Double.isFinite(n)) {
                // GnuR does not check this and tries to allocate the memory, we do check this, but
                // fail with the same error message for compatibility reasons.
                throw RError.error(RError.SHOW_CALLER, CALLOC_COULD_NOT_ALLOCATE_INF);
            }

            m = Math.round(m);
            n = Math.round(n);
            if ((m < 0) || (n < 0)) {
                // TODO: for some reason the macro in GNUR here returns NA instead of NaN...
                // return StatsUtil.mlError();
                return RRuntime.DOUBLE_NA;
            }

            if ((m == 0) || (n == 0)) {
                return (0);
            }

            r = 0.0;
            k = (int) (m + n);

            int[] x;
            try {
                x = new int[k];
            } catch (OutOfMemoryError ex) {
                // GnuR seems to be reporting the same number regardless of 'k'
                throw RError.error(RError.SHOW_CALLER, CALLOC_COULD_NOT_ALLOCATE_INF);
            }
            for (i = 0; i < k; i++) {
                x[i] = i;
            }
            for (i = 0; i < n; i++) {
                j = (int) Math.floor(k * rand.unifRand());
                r += x[j];
                x[j] = x[--k];
            }
            return (r - n * (n - 1) / 2);
        }
    }
}
