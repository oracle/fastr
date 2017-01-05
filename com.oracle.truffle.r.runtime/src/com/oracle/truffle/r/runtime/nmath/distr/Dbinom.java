/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000-2014, The R Core Team
 * Copyright (c) 2008, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 * credited as additional author in the original file:
 *   Catherine Loader, catherine@research.bell-labs.com.
 *   October 23, 2000.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathConstants;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.RMath;

// transcribed from dbinom.c

public final class Dbinom implements Function3_1 {

    private final BranchProfile nanProfile = BranchProfile.create();

    public static double dbinomRaw(double x, double n, double p, double q, boolean giveLog) {

        if (p == 0) {
            return ((x == 0) ? DPQ.rd1(giveLog) : DPQ.rd0(giveLog));
        }
        if (q == 0) {
            return ((x == n) ? DPQ.rd1(giveLog) : DPQ.rd0(giveLog));
        }

        if (x == 0) {
            if (n == 0) {
                return DPQ.rd1(giveLog);
            }
            double lc = (p < 0.1) ? -RMath.bd0(n, n * q) - n * p : n * Math.log(q);
            return DPQ.rdexp(lc, giveLog);
        }
        if (x == n) {
            double lc = (q < 0.1) ? -RMath.bd0(n, n * p) - n * q : n * Math.log(p);
            return DPQ.rdexp(lc, giveLog);
        }
        if (x < 0 || x > n) {
            return DPQ.rd0(giveLog);
        }

        /*
         * n*p or n*q can underflow to zero if n and p or q are small. This used to occur in dbeta,
         * and gives NaN as from R 2.3.0.
         */
        double lc = RMath.stirlerr(n) - RMath.stirlerr(x) - RMath.stirlerr(n - x) - RMath.bd0(x, n * p) - RMath.bd0(n - x, n * q);

        /* f = (M_2PI*x*(n-x))/n; could overflow or underflow */
        /*
         * Upto R 2.7.1: lf = log(M_2PI) + log(x) + log(n-x) - log(n); -- following is much better
         * for x << n :
         */
        double lf = MathConstants.M_LN_2PI + Math.log(x) + Math.log1p(-x / n);

        return DPQ.rdexp(lc - 0.5 * lf, giveLog);
    }

    @Override
    public double evaluate(double x, double n, double p, boolean giveLog) {
        /* NaNs propagated correctly */
        if (Double.isNaN(x) || Double.isNaN(n) || Double.isNaN(p)) {
            nanProfile.enter();
            return x + n + p;
        }

        if (p < 0 || p > 1 || n < 0 || DPQ.nonint(n)) {
            nanProfile.enter();
            return Double.NaN;
        }

        try {
            DPQ.nonintCheck(x, giveLog);
        } catch (EarlyReturn e) {
            return e.result;
        }

        if (x < 0 || !Double.isFinite(x)) {
            return DPQ.rd0(giveLog);
        }

        return dbinomRaw(RMath.forceint(x), RMath.forceint(n), p, 1 - p, giveLog);
    }
}
