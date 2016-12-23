/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.GammaFunctions.dgamma;
import static com.oracle.truffle.r.library.stats.GammaFunctions.pgamma;
import static com.oracle.truffle.r.library.stats.GammaFunctions.qgamma;

import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandFunction1_Double;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_1;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_2;

public final class Chisq {
    private Chisq() {
        // only static members
    }

    public static final class PChisq implements Function2_2 {
        @Override
        public double evaluate(double x, double df, boolean lowerTail, boolean logP) {
            return pgamma(x, df / 2., 2., lowerTail, logP);
        }
    }

    public static final class DChisq implements Function2_1 {
        @Override
        public double evaluate(double x, double df, boolean giveLog) {
            return dgamma(x, df / 2., 2., giveLog);
        }
    }

    public static final class QChisq implements Function2_2 {
        @Override
        public double evaluate(double p, double df, boolean lowerTail, boolean logP) {
            return qgamma(p, 0.5 * df, 2.0, lowerTail, logP);
        }
    }

    public static final class RChisq extends RandFunction1_Double {
        public static double rchisq(double df, RandomNumberProvider rand) {
            if (!Double.isFinite(df) || df < 0.0) {
                return RMath.mlError();
            }
            return new RGamma().execute(df / 2.0, 2.0, rand);
        }

        @Override
        public double execute(double a, RandomNumberProvider rand) {
            return rchisq(a, rand);
        }
    }
}
