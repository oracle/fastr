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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_2;
import com.oracle.truffle.r.library.stats.TOMS708.Bratio;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;

// transcribed from pbeta.c

public final class Pbeta implements Function3_2 {

    private final BranchProfile naProfile = BranchProfile.create();

    @Override
    public double evaluate(double x, double a, double b, boolean lowerTail, boolean logP) {
        return pbeta(x, a, b, lowerTail, logP, naProfile);
    }

    @TruffleBoundary
    private static double pbetaRaw(double x, double a, double b, boolean lowerTail, boolean logProb) {
        // treat limit cases correctly here:
        if (a == 0 || b == 0 || !Double.isFinite(a) || !Double.isFinite(b)) {
            // NB: 0 < x < 1 :
            if (a == 0 && b == 0) {
                // point mass 1/2 at each of {0,1} :
                return (logProb ? -MathConstants.M_LN2 : 0.5);
            }
            if (a == 0 || a / b == 0) {
                // point mass 1 at 0 ==> P(X <= x) = 1, all x > 0
                return DPQ.rdt1(lowerTail, logProb);
            }
            if (b == 0 || b / a == 0) {
                // point mass 1 at 1 ==> P(X <= x) = 0, all x < 1
                return DPQ.rdt0(lowerTail, logProb);
            }

            // else, remaining case: a = b = Inf : point mass 1 at 1/2
            if (x < 0.5) {
                return DPQ.rdt0(lowerTail, logProb);
            }
            // else, x >= 0.5 :
            return DPQ.rdt1(lowerTail, logProb);
        }
        // Now: 0 < a < Inf; 0 < b < Inf

        double x1 = 0.5 - x + 0.5;
        // ====
        Bratio bratio = Bratio.bratio(a, b, x, x1, logProb); /* -> ./toms708.c */
        // ====
        /* ierr = 8 is about inaccuracy in extreme cases */
        if (bratio.ierr != 0 && (bratio.ierr != 8 || logProb)) {
            doWarning(x, a, b, bratio.ierr);
        }
        return lowerTail ? bratio.w : bratio.w1;
    }/* pbeta_raw() */

    @TruffleBoundary
    private static void doWarning(double x, double a, double b, int ierr) {
        RError.warning(RError.SHOW_CALLER, Message.GENERIC, String.format("pbeta_raw(%g, a=%g, b=%g, ..) -> bratio() gave error code %d", x, a, b, ierr));
    }

    static double pbeta(double x, double a, double b, boolean lowerTail, boolean logP, BranchProfile nanProfile) {
        if (Double.isNaN(x) || Double.isNaN(a) || Double.isNaN(b)) {
            nanProfile.enter();
            return x + a + b;
        }

        if (a < 0 || b < 0) {
            nanProfile.enter();
            return Double.NaN;
        }
        // allowing a==0 and b==0 <==> treat as one- or two-point mass

        if (x <= 0) {
            return DPQ.rdt0(lowerTail, logP);
        }
        if (x >= 1) {
            return DPQ.rdt1(lowerTail, logP);
        }

        return pbetaRaw(x, a, b, lowerTail, logP);
    }
}
