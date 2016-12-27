/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.MathConstants.M_1_SQRT_2PI;
import static com.oracle.truffle.r.library.stats.MathConstants.M_LN_SQRT_2PI;

import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_1;

public final class DNorm implements Function3_1 {
    @Override
    public double evaluate(double xa, double mu, double sigma, boolean giveLog) {
        double x = xa;
        if (Double.isNaN(x) || Double.isNaN(mu) || Double.isNaN(sigma)) {
            return x + mu + sigma;
        }

        if (!Double.isFinite(sigma)) {
            return DPQ.rd0(giveLog);
        } else if (!Double.isFinite(x) && mu == x) {
            return RMath.mlError();
        } else if (sigma <= 0) {
            if (sigma < 0) {
                return RMath.mlError();
            }
            return (x == mu) ? Double.POSITIVE_INFINITY : DPQ.rd0(giveLog);
        }

        x = (x - mu) / sigma;
        x = Math.abs(x);
        if (x >= 2 * Math.sqrt(Double.MAX_VALUE)) {
            return DPQ.rd0(giveLog);
        }

        if (!Double.isFinite(x)) {
            return DPQ.rd0(giveLog);
        }

        if (giveLog) {
            return -(M_LN_SQRT_2PI + 0.5 * x * x + Math.log(sigma));
        }
        return M_1_SQRT_2PI * Math.exp(-0.5 * x * x) / sigma;
    }
}
