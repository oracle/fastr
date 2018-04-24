/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2014, The R Core Team
 * Copyright (c) 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath;

import com.oracle.truffle.r.runtime.RRuntime;
import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.gammafn;
import static com.oracle.truffle.r.runtime.nmath.LBeta.lbeta;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.ML_POSINF;

public class Beta {

    private static final double xmax = GammaFunctions.gfn_xmax;

    public static double beta(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return a + b;
        }
        if (a < 0 || b < 0) {
            return RMathError.defaultError(); // ML_ERR_return_NAN
        } else if (a == 0 || b == 0) {
            return ML_POSINF;
        } else if (!RRuntime.isFinite(a) || !RRuntime.isFinite(b)) {
            return 0;
        }

        if (a + b < xmax) { /* ~= 171.61 for IEEE */
            // return gammafn(a) * gammafn(b) / gammafn(a+b);
            /*
             * All the terms are positive, and all can be large for large or small arguments. They
             * are never much less than one. gammafn(x) can still overflow for x ~ 1e-308, but the
             * result would too.
             */
            return (1 / gammafn(a + b)) * gammafn(a) * gammafn(b);
        } else {
            double val = lbeta(a, b);
            // underflow to 0 is not harmful per se; exp(-999) also gives no warning
            return Math.exp(val);
        }
    }

}
