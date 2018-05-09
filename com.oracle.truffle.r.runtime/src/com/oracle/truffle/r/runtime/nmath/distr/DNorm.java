/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */

package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathConstants;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.RMathError;

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
            return RMathError.defaultError();
        } else if (sigma <= 0) {
            if (sigma < 0) {
                return RMathError.defaultError();
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
            return -(MathConstants.M_LN_SQRT_2PI + 0.5 * x * x + Math.log(sigma));
        }
        return MathConstants.M_1_SQRT_2PI * Math.exp(-0.5 * x * x) / sigma;
    }
}
