/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2006, The R Core Team
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;

// transcribed from pbeta.c

public abstract class Pbeta extends RExternalBuiltinNode.Arg5 {

    private static double pbetaRaw(double x, double a, double b, boolean lowerTail, boolean logP) {
        // treat limit cases correctly here:
        if (a == 0 || b == 0 || !Double.isFinite(a) || !Double.isFinite(b)) {
            // NB: 0 < x < 1 :
            if (a == 0 && b == 0) {
                // point mass 1/2 at each of {0,1} :
                return (logP ? -MathConstants.M_LN2 : 0.5);
            }
            if (a == 0 || a / b == 0) {
                // point mass 1 at 0 ==> P(X <= x) = 1, all x > 0
                return DPQ.dt1(logP, lowerTail);
            }
            if (b == 0 || b / a == 0) {
                // point mass 1 at 1 ==> P(X <= x) = 0, all x < 1
                return DPQ.dt0(logP, lowerTail);
            }

            // else, remaining case: a = b = Inf : point mass 1 at 1/2
            if (x < 0.5) {
                return DPQ.dt0(logP, lowerTail);
            }
            // else, x >= 0.5 :
            return DPQ.dt1(logP, lowerTail);
        }
        // Now: 0 < a < Inf; 0 < b < Inf

        double x1 = 0.5 - x + 0.5;
        // ====
        TOMS708.Bratio bratio = new TOMS708.Bratio();
        bratio.bratio(a, b, x, x1, logP); /* -> ./toms708.c */
        // ====
        /* ierr = 8 is about inaccuracy in extreme cases */
        if (bratio.ierr != 0 && (bratio.ierr != 8 || logP)) {
            RError.warning(RError.SHOW_CALLER, Message.GENERIC, String.format("pbeta_raw(%g, a=%g, b=%g, ..) -> bratio() gave error code %d",
                            x, a, b, bratio.ierr));
        }
        return lowerTail ? bratio.w : bratio.w1;
    }/* pbeta_raw() */

    static double pbeta(double x, double a, double b, boolean lowerTail, boolean logP) {
        if (Double.isNaN(x) || Double.isNaN(a) || Double.isNaN(b)) {
            return x + a + b;
        }

        if (a < 0 || b < 0) {
            return Double.NaN;
        }
        // allowing a==0 and b==0 <==> treat as one- or two-point mass

        if (x <= 0) {
            return DPQ.dt0(logP, lowerTail);
        }
        if (x >= 1) {
            return DPQ.dt1(logP, lowerTail);
        }

        return pbetaRaw(x, a, b, lowerTail, logP);
    }

}
