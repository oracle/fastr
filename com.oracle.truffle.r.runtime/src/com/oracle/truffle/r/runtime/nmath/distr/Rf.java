/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2008, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandFunction2_Double;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;

public final class Rf extends RandFunction2_Double {
    @Override
    public double execute(double n1, double n2, RandomNumberProvider rand) {
        if (Double.isNaN(n1) || Double.isNaN(n2) || n1 <= 0. || n2 <= 0.) {
            return RMathError.defaultError();
        }

        double v1;
        double v2;
        v1 = Double.isFinite(n1) ? (Chisq.RChisq.rchisq(n1, rand) / n1) : 1;
        v2 = Double.isFinite(n2) ? (Chisq.RChisq.rchisq(n2, rand) / n2) : 1;
        return v1 / v2;
    }
}
