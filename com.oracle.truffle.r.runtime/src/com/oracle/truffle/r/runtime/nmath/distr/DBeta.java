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

import static com.oracle.truffle.r.runtime.nmath.LBeta.lbeta;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.RMathError;

public final class DBeta implements Function3_1 {

    public static DBeta create() {
        return new DBeta();
    }

    public static DBeta getUncached() {
        return new DBeta();
    }

    @Override
    public double evaluate(double x, double a, double b, boolean log) {
        /* NaNs propagated correctly */
        if (Double.isNaN(x) || Double.isNaN(a) || Double.isNaN(b)) {
            return x + a + b;
        }

        if (a < 0 || b < 0) {
            return RMathError.defaultError();
        }
        if (x < 0 || x > 1) {
            return (DPQ.rd0(log));
        }

        // limit cases for (a,b), leading to point masses
        if (a == 0 || b == 0 || !Double.isFinite(a) || !Double.isFinite(b)) {
            if (a == 0 && b == 0) { // point mass 1/2 at each of {0,1} :
                if (x == 0 || x == 1) {
                    return Double.POSITIVE_INFINITY;
                } else {
                    return DPQ.rd0(log);
                }
            }
            if (a == 0 || a / b == 0) { // point mass 1 at 0
                if (x == 0) {
                    return Double.POSITIVE_INFINITY;
                } else {
                    return DPQ.rd0(log);
                }
            }
            if (b == 0 || b / a == 0) { // point mass 1 at 1
                if (x == 1) {
                    return Double.POSITIVE_INFINITY;
                } else {
                    return DPQ.rd0(log);
                }
            }
            // else, remaining case: a = b = Inf : point mass 1 at 1/2
            if (x == 0.5) {
                return Double.POSITIVE_INFINITY;
            } else {
                return DPQ.rd0(log);
            }
        }

        if (x == 0) {
            if (a > 1) {
                return DPQ.rd0(log);
            }
            if (a < 1) {
                return Double.POSITIVE_INFINITY;
            }
            /* a == 1 : */
            return DPQ.rdval(b, log);
        }
        if (x == 1) {
            if (b > 1) {
                return DPQ.rd0(log);
            }
            if (b < 1) {
                return Double.POSITIVE_INFINITY;
            }
            /* b == 1 : */
            return (DPQ.rdval(a, log));
        }

        double lval;
        if (a <= 2 || b <= 2) {
            lval = (a - 1) * Math.log(x) + (b - 1) * Math.log1p(-x) - lbeta(a, b);
        } else {
            lval = Math.log(a + b - 1) + Dbinom.dbinomRaw(a - 1, a + b - 2, x, 1 - x, true);
        }

        return DPQ.rdexp(lval, log);
    }
}
