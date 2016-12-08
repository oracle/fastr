/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
// Acknowledgement from GnuR header:
// Author: Catherine Loader, catherine@research.bell-labs.com, October 23, 2000.
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.GammaFunctions.dpoisRaw;
import static com.oracle.truffle.r.library.stats.MathConstants.forceint;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_1;

public final class DPois implements Function2_1 {
    @Override
    public double evaluate(double x, double lambda, boolean giveLog) {
        if (Double.isNaN(x) || Double.isNaN(lambda)) {
            return x + lambda;
        }
        if (lambda < 0) {
            return RMath.mlError();
        }

        try {
            DPQ.nonintCheck(x, giveLog);
        } catch (EarlyReturn e) {
            return e.result;
        }
        if (x < 0 || !Double.isFinite(x)) {
            return DPQ.rd0(giveLog);
        }

        return dpoisRaw(forceint(x), lambda, giveLog);
    }
}
