/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2005-6 Morten Welinder <terra@gnome.org>
 * Copyright (C) 2005-10 The R Foundation
 * Copyright (C) 2006-2015 The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.pgammaRaw;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.RMathError;

public final class PGamma implements Function3_2 {
    @Override
    public double evaluate(double xIn, double alph, double scale, boolean lowerTail, boolean logP) {
        if (Double.isNaN(xIn) || Double.isNaN(alph) || Double.isNaN(scale)) {
            return xIn + alph + scale;
        }
        if (alph < 0 || scale < 0) {
            return RMathError.defaultError();
        }

        double x = xIn / scale;
        if (Double.isNaN(x)) {
            return x;
        }
        if (alph == 0.) {
            return x <= 0 ? DPQ.rdt0(lowerTail, logP) : DPQ.rdt1(lowerTail, logP);
        }
        return pgammaRaw(x, alph, lowerTail, logP);
    }
}
