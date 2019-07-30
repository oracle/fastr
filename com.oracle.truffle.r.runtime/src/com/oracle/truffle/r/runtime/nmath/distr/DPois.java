/*
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
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
// Acknowledgement from GnuR header:
// Author: Catherine Loader, catherine@research.bell-labs.com, October 23, 2000.
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.DPQ.rd0;
import static com.oracle.truffle.r.runtime.nmath.DPQ.rd1;
import static com.oracle.truffle.r.runtime.nmath.DPQ.rdexp;
import static com.oracle.truffle.r.runtime.nmath.DPQ.rdfexp;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_MIN;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.M_2PI;
import static com.oracle.truffle.r.runtime.nmath.RMath.forceint;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.GammaFunctions;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;

public final class DPois implements Function2_1 {

    public static DPois create() {
        return new DPois();
    }

    public static DPois getUncached() {
        return new DPois();
    }

    @Override
    public double evaluate(double x, double lambda, boolean giveLog) {
        if (Double.isNaN(x) || Double.isNaN(lambda)) {
            return x + lambda;
        }
        if (lambda < 0) {
            return RMathError.defaultError();
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

    public static double dpoisRaw(double x, double lambda, boolean giveLog) {
        /*
         * x >= 0 ; integer for dpois(), but not e.g. for pgamma()! lambda >= 0
         */
        if (lambda == 0) {
            return (x == 0) ? rd1(giveLog) : rd0(giveLog);
        }
        if (!RRuntime.isFinite(lambda)) {
            // including for the case where x = lambda = +Inf
            return rd0(giveLog);
        }
        if (x < 0) {
            return rd0(giveLog);
        }
        if (x <= lambda * DBL_MIN) {
            if (!RRuntime.isFinite(x)) {
                // lambda < x = +Inf
                return rd0(giveLog);
            }
            return (rdexp(-lambda, giveLog));
        }
        if (lambda < x * DBL_MIN) {
            return (rdexp(-lambda + x * Math.log(lambda) - GammaFunctions.lgammafn(x + 1), giveLog));
        }
        return rdfexp(M_2PI * x, -RMath.stirlerr(x) - RMath.bd0(x, lambda), giveLog);
    }
}
