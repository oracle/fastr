/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2003-2007, The R Core Team
 * Copyright (c) 2003-2007, The R Foundation
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.runtime.nmath;

import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgammafn;
import static com.oracle.truffle.r.runtime.nmath.GammaFunctions.lgammafnSign;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError.Message;

// transcribed from nmath/choose.c

public class Choose {
    private static final int K_SMALL_MAX = 30;

    public static double lfastchoose(double n, double k) {
        return -Math.log(n + 1.) - LBeta.lbeta(n - k + 1., k + 1.);
    }

    @TruffleBoundary
    public static double choose(double n, double ka) {
        double k = ka;
        if (Double.isNaN(n) || Double.isNaN(ka)) {
            return n + k;
        }
        if (TOMS708.fabs(k - RMath.forceint(k)) > 1e-7) {
            RMathError.warning(Message.CHOOSE_ROUNDING_WARNING, k, RMath.forceint(k));
        }

        if (k < K_SMALL_MAX) {
            // symmetry: this e.g. turns (n=3,k=3) into (n=3,k=0)
            int intN = (int) n;
            if (n == intN && intN - k < k && intN >= 0) {
                k = intN - k;
            }

            if (k < 0) {
                return 0;
            } else if (k == 0) {
                return 1;
            }

            double result = n;
            for (int i = 2; i <= k; i++) {
                result *= (n - i + 1) / i;
            }
            return isInt(n) ? RMath.forceint(result) : result;
        }
        if (n < 0) {
            double factor = isOdd(k) ? -1. : 1.;
            return factor * choose(-n + k - 1, k);
        }
        if (isInt(n)) {
            double nInt = RMath.forceint(n);
            if (nInt < k) {
                return 0;
            } else if (nInt - k < K_SMALL_MAX) {
                return choose(nInt, nInt - k);
            }
            return RMath.forceint(Math.exp(lfastchoose(n, k)));
        }
        // else non-integer n >= 0
        if (n < k - 1) {
            int[] sChoose = new int[1];
            double result = lfastchoose2(n, k, sChoose);
            return sChoose[0] * Math.exp(result);
        }
        return Math.exp(lfastchoose(n, k));
    }

    @TruffleBoundary
    public static double lchoose(double n, double kIn) {
        double k = RMath.forceint(kIn);
        /* NaNs propagated correctly */
        if (Double.isNaN(n) || Double.isNaN(k)) {
            return n + k;
        }

        if (TOMS708.fabs(k - kIn) > 1e-7) {
            RMathError.warning(Message.CHOOSE_ROUNDING_WARNING, kIn, k);
        }
        if (k < 2) {
            if (k < 0) {
                return Double.NEGATIVE_INFINITY;
            }
            if (k == 0) {
                return 0.;
            }
            /* else: k == 1 */
            return Math.log(TOMS708.fabs(n));
        }
        /* else: k >= 2 */
        if (n < 0) {
            return lchoose(-n + k - 1, k);
        } else if (isInt(n)) {
            double nInt = RMath.forceint(n);
            if (nInt < k) {
                return Double.NEGATIVE_INFINITY;
            }
            /* k <= n : */
            if (nInt - k < 2) {
                return lchoose(nInt, nInt - k);
            } /* <- Symmetry */
            /* else: n >= k+2 */
            return lfastchoose(nInt, k);
        }
        /* else non-integer n >= 0 : */
        if (n < k - 1) {
            int[] s = new int[1];
            return lfastchoose2(n, k, s);
        }
        return lfastchoose(n, k);
    }

    /*
     * mathematically the same: less stable typically, but useful if n-k+1 < 0 :
     */
    private static double lfastchoose2(double n, double k, int[] sChoose) {
        double r;
        r = lgammafnSign(n - k + 1., sChoose);
        return lgammafn(n + 1.) - lgammafn(k + 1.) - r;
    }

    private static boolean isOdd(double x) {
        return x != 2 * Math.floor(x / 2.);
    }

    private static boolean isInt(double x) {
        return !DPQ.nonint(x);
    }
}
