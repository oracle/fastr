/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
// Acknowledgement from GnuR header:
// Author: Catherine Loader, catherine@research.bell-labs.com, October 23, 2000.
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.DPQ.rd0;
import static com.oracle.truffle.r.library.stats.DPQ.rd1;
import static com.oracle.truffle.r.library.stats.DPQ.rdexp;
import static com.oracle.truffle.r.library.stats.DPQ.rdfexp;
import static com.oracle.truffle.r.library.stats.MathConstants.DBL_MIN;
import static com.oracle.truffle.r.library.stats.MathConstants.M_2PI;
import static com.oracle.truffle.r.library.stats.RMath.forceint;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_1;
import com.oracle.truffle.r.runtime.RRuntime;

public final class DPois implements Function2_1 {
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
            return rd0(giveLog);
        }
        if (x < 0) {
            return rd0(giveLog);
        }
        if (x <= lambda * DBL_MIN) {
            return (rdexp(-lambda, giveLog));
        }
        if (lambda < x * DBL_MIN) {
            return (rdexp(-lambda + x * Math.log(lambda) - GammaFunctions.lgammafn(x + 1), giveLog));
        }
        return rdfexp(M_2PI * x, -RMath.stirlerr(x) - RMath.bd0(x, lambda), giveLog);
    }
}
