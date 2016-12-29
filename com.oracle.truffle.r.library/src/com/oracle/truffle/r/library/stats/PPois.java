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

import static com.oracle.truffle.r.library.stats.GammaFunctions.pgamma;

import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_2;

public class PPois implements Function2_2 {
    @Override
    public double evaluate(double x, double lambda, boolean lowerTail, boolean logP) {
        if (Double.isNaN(x) || Double.isNaN(lambda) || lambda < 0.) {
            return RMathError.defaultError();
        }
        if (x < 0) {
            return DPQ.rdt0(lowerTail, logP);
        }
        if (lambda == 0.) {
            return DPQ.rdt1(lowerTail, logP);
        }
        if (!Double.isFinite(x)) {
            return DPQ.rdt1(lowerTail, logP);
        }
        x = Math.floor(x + 1e-7);

        return pgamma(lambda, x + 1, 1., !lowerTail, logP);
    }
}
