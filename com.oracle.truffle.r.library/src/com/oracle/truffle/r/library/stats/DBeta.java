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

import static com.oracle.truffle.r.library.stats.LBeta.lbeta;

import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_1;

public class DBeta implements Function3_1 {
    @Override
    public double evaluate(double x, double a, double b, boolean log) {
        /* NaNs propagated correctly */
        if (Double.isNaN(x) || Double.isNaN(a) || Double.isNaN(b)) {
            return x + a + b;
        }

        if (a < 0 || b < 0) {
            return RMath.mlError();
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
