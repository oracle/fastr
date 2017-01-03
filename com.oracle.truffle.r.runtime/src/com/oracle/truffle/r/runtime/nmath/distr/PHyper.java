/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1999-2014, The R Core Team
 * Copyright (c) 2004, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 * Copyright (C) 2004 Morten Welinder
 *
 * Current implementation based on posting
 * From: Morten Welinder <terra@gnome.org>
 * Cc: R-bugs@biostat.ku.dk
 * Subject: [Rd] phyper accuracy and efficiency (PR#6772)
 * Date: Thu, 15 Apr 2004 18:06:37 +0200 (CEST)
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_EPSILON;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;

public final class PHyper implements Function4_2 {
    private final DHyper dhyper = new DHyper();

    @Override
    public double evaluate(double xIn, double nrIn, double nbIn, double nIn, boolean lowerTailIn, boolean logP) {
        /* Sample of n balls from nr red and nb black ones; x are red */
        if (Double.isNaN(xIn) || Double.isNaN(nrIn) || Double.isNaN(nbIn) || Double.isNaN(nIn)) {
            return xIn + nrIn + nbIn + nIn;
        }

        double x = Math.floor(xIn + 1e-7);
        double nr = RMath.forceint(nrIn);
        double nb = RMath.forceint(nbIn);
        double n = RMath.forceint(nIn);

        if (nr < 0 || nb < 0 || !Double.isFinite(nr + nb) || n < 0 || n > nr + nb) {
            return RMathError.defaultError();
        }

        boolean lowerTail = lowerTailIn;
        if (x * (nr + nb) > n * nr) {
            /* Swap tails. */
            double oldNB = nb;
            nb = nr;
            nr = oldNB;
            x = n - x - 1;
            lowerTail = !lowerTail;
        }

        if (x < 0) {
            return DPQ.rdt0(lowerTail, logP);
        }
        if (x >= nr || x >= n) {
            return DPQ.rdt1(lowerTail, logP);
        }

        double d = dhyper.evaluate(x, nr, nb, n, logP);
        double pd = pdhyper(x, nr, nb, n, logP);

        return logP ? DPQ.rdtlog(d + pd, lowerTail, logP) : DPQ.rdlval(d * pd, lowerTail);
    }

    static double pdhyper(double xIn, double nr, double nb, double n, boolean logP) {
        /*
         * Calculate
         *
         * phyper (x, nr, nb, n, true, false) [log] ---------------------------------- dhyper (x,
         * nr, nb, n, false)
         *
         * without actually calling phyper. This assumes that
         *
         * x * (nr + nb) <= n * nr
         *
         */
        /* LDOUBLE */double sum = 0;
        /* LDOUBLE */double term = 1;

        double x = xIn;
        while (x > 0 && term >= DBL_EPSILON * sum) {
            term *= x * (nb - n + x) / (n + 1 - x) / (nr + 1 - x);
            sum += term;
            x--;
        }

        double ss = sum;
        return logP ? RMath.log1p(ss) : 1 + ss;
    }
}
