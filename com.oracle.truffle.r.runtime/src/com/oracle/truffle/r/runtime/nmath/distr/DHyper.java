/*
 * Copyright (c) 2000-2014, The R Core Team
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates
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

    public static DHyper create() {
        return new DHyper();
    }

    public static DHyper getUncached() {
        return new DHyper();
    }

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
