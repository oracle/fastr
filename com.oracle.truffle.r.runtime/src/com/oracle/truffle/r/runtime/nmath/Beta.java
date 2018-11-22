/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2014, The R Core Team
 * Copyright (c) 2018, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime.nmath;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.gammafn;
import static com.oracle.truffle.r.runtime.nmath.LBeta.lbeta;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.ML_POSINF;

public final class Beta implements Function2_1 {

    public static final Beta INSTANCE = new Beta();
    private static final double xmax = GammaFunctions.gfn_xmax;

    @Override
    public double evaluate(double a, double b, @SuppressWarnings("unused") boolean x) {
        return beta(a, b);
    }

    public static double beta(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return a + b;
        }
        if (a < 0 || b < 0) {
            return RMathError.defaultError(); // ML_ERR_return_NAN
        } else if (a == 0 || b == 0) {
            return ML_POSINF;
        } else if (!RRuntime.isFinite(a) || !RRuntime.isFinite(b)) {
            return 0;
        }

        if (a + b < xmax) { /* ~= 171.61 for IEEE */
            // return gammafn(a) * gammafn(b) / gammafn(a+b);
            /*
             * All the terms are positive, and all can be large for large or small arguments. They
             * are never much less than one. gammafn(x) can still overflow for x ~ 1e-308, but the
             * result would too.
             */
            return (1 / gammafn(a + b)) * gammafn(a) * gammafn(b);
        } else {
            double val = lbeta(a, b);
            // underflow to 0 is not harmful per se; exp(-999) also gives no warning
            return Math.exp(val);
        }
    }
}
