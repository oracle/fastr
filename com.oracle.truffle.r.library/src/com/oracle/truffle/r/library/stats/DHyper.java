/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000-2014, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 *  AUTHOR
 *    Catherine Loader, catherine@research.bell-labs.com.
 *    October 23, 2000.
 *
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.Dbinom.dbinomRaw;

import com.oracle.truffle.r.library.stats.DPQ.EarlyReturn;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function4_1;

public final class DHyper implements Function4_1 {
    @Override
    public double evaluate(double x, double r, double b, double n, boolean giveLog) {
        if (Double.isNaN(x) || Double.isNaN(r) || Double.isNaN(b) || Double.isNaN(n)) {
            return x + r + b + n;
        }

        if (DPQ.rdneginonint(r) || DPQ.rdneginonint(b) || DPQ.rdneginonint(n) || n > r + b) {
            return RMathError.defaultError();
        }
        if (x < 0) {
            return DPQ.rd0(giveLog);
        }

        try {
            DPQ.nonintCheck(x, giveLog); // incl warning
        } catch (EarlyReturn e) {
            return e.result;
        }

        x = RMath.forceint(x);
        r = RMath.forceint(r);
        b = RMath.forceint(b);
        n = RMath.forceint(n);

        if (n < x || r < x || n - x > b) {
            return DPQ.rd0(giveLog);
        }
        if (n == 0) {
            return (x == 0) ? DPQ.rd1(giveLog) : DPQ.rd0(giveLog);
        }

        double p = n / (r + b);
        double q = (r + b - n) / (r + b);

        double p1 = dbinomRaw(x, r, p, q, giveLog);
        double p2 = dbinomRaw(n - x, b, p, q, giveLog);
        double p3 = dbinomRaw(n, r + b, p, q, giveLog);

        return (giveLog) ? p1 + p2 - p3 : p1 * p2 / p3;
    }
}
