/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000-2014, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 *  AUTHOR
 *    Catherine Loader, catherine@research.bell-labs.com.
 *    October 23, 2000.
 *
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_1;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;

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

        double ix = RMath.forceint(x);
        double ir = RMath.forceint(r);
        double ib = RMath.forceint(b);
        double in = RMath.forceint(n);

        if (in < ix || ir < ix || in - ix > ib) {
            return DPQ.rd0(giveLog);
        }
        if (in == 0) {
            return (ix == 0) ? DPQ.rd1(giveLog) : DPQ.rd0(giveLog);
        }

        double p = in / (ir + ib);
        double q = (ir + ib - in) / (ir + ib);

        double p1 = Dbinom.dbinomRaw(ix, ir, p, q, giveLog);
        double p2 = Dbinom.dbinomRaw(in - ix, ib, p, q, giveLog);
        double p3 = Dbinom.dbinomRaw(in, ir + ib, p, q, giveLog);

        return (giveLog) ? p1 + p2 - p3 : p1 * p2 / p3;
    }
}
