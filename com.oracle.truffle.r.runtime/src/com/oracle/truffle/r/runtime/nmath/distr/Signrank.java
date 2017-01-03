/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1999--2014, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.RError.Message.GENERIC;
import static com.oracle.truffle.r.runtime.nmath.RMath.forceint;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction1_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class Signrank {
    private Signrank() {
        // only static members
    }

    /**
     * Holds cache for the dynamic algorithm that wilcox uses.
     */
    public static final class SignrankData {
        private static final ThreadLocal<double[]> data = new ThreadLocal<>();

        @TruffleBoundary
        static double[] getData(int n) {
            double[] result = data.get();
            int u = n * (n + 1) / 2;
            int c = (u / 2);

            if (result != null && (result.length <= c)) {
                result = null;
            }
            if (result == null) {
                try {
                    result = new double[c + 1];
                } catch (OutOfMemoryError e) {
                    throw RError.error(RError.SHOW_CALLER, GENERIC, "signrank allocation error");
                }
                data.set(result);
            }
            return result;
        }

        public static void freeData() {
            data.set(null);
        }
    }

    public static final class RSignrank extends RandFunction1_Double {
        @Override
        public double execute(double nIn, RandomNumberProvider rand) {
            if (Double.isNaN(nIn)) {
                return nIn;
            }
            if (Double.isInfinite(nIn)) {
                // In GnuR these "results" seem to be generated due to the behaviour of R_forceint,
                // and the "(int) n" cast, which ends up casting +/-infinity to integer...
                return nIn < 0 ? RMathError.defaultError() : 0;
            }

            double n = forceint(nIn);
            if (n < 0) {
                return RMathError.defaultError();
            }

            if (n == 0) {
                return 0;
            }
            double r = 0.0;
            int k = (int) n;
            for (int i = 0; i < k; i++) {
                r += (i + 1) * Math.floor(rand.unifRand() + 0.5);
            }
            return r;
        }
    }
}
