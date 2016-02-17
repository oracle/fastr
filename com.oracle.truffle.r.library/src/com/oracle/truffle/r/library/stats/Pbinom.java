/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2004, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;

public abstract class Pbinom extends RExternalBuiltinNode.Arg5 {

    static double pbinom(double initialX, double initialN, double p, boolean lowerTail, boolean logP) {
        double x = initialX;
        double n = initialN;
        if (Double.isNaN(x) || Double.isNaN(n) || Double.isNaN(p)) {
            return x + n + p;
        }
        if (!Double.isFinite(n) || !Double.isFinite(p)) {
            return Double.NaN;
        }

        if (DPQ.nonint(n)) {
            RError.warning(RError.SHOW_CALLER, RError.Message.GENERIC, String.format("non-integer n = %f", n));
            return Double.NaN;
        }
        n = Math.round(n);
        /* PR#8560: n=0 is a valid value */
        if (n < 0 || p < 0 || p > 1) {
            return Double.NaN;
        }

        if (x < 0) {
            return DPQ.dt0(logP, lowerTail);
        }
        x = Math.floor(x + 1e-7);
        if (n <= x) {
            return DPQ.dt1(logP, lowerTail);
        }
        return Pbeta.pbeta(p, x + 1, n - x, !lowerTail, logP);
    }
}
