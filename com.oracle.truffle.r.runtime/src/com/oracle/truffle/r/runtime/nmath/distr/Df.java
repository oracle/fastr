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

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.dgamma;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.RMathError;

public final class Df implements Function3_1 {
    @Override
    public double evaluate(double x, double m, double n, boolean giveLog) {
        double p;
        double q;
        double f;
        double dens;

        if (Double.isNaN(x) || Double.isNaN(m) || Double.isNaN(n)) {
            return x + m + n;
        }

        if (m <= 0 || n <= 0) {
            return RMathError.defaultError();
        }
        if (x < 0.) {
            return DPQ.rd0(giveLog);
        }
        if (x == 0.) {
            return m > 2 ? DPQ.rd0(giveLog) : (m == 2 ? DPQ.rd1(giveLog) : Double.POSITIVE_INFINITY);
        }
        if (!Double.isFinite(m) && !Double.isFinite(n)) { /* both +Inf */
            if (x == 1.) {
                return Double.POSITIVE_INFINITY;
            } else {
                return DPQ.rd0(giveLog);
            }
        }
        if (!Double.isFinite(n)) {
            /* must be +Inf by now */
            return dgamma(x, m / 2, 2. / m, giveLog);
        }
        if (m > 1e14) { /* includes +Inf: code below is inaccurate there */
            dens = dgamma(1. / x, n / 2, 2. / n, giveLog);
            return giveLog ? dens - 2 * Math.log(x) : dens / (x * x);
        }

        f = 1. / (n + x * m);
        q = n * f;
        p = x * m * f;

        if (m >= 2) {
            f = m * q / 2;
            dens = Dbinom.dbinomRaw((m - 2) / 2, (m + n - 2) / 2, p, q, giveLog);
        } else {
            f = m * m * q / (2 * p * (m + n));
            dens = Dbinom.dbinomRaw(m / 2, (m + n) / 2, p, q, giveLog);
        }
        return (giveLog ? Math.log(f) + dens : f * dens);
    }
}
