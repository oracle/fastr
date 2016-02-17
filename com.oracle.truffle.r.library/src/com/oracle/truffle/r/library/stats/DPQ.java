/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000--2014, The R Core Team
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

// transcribed from dpq.h

public final class DPQ {
    private DPQ() {
        // private
    }

    // R >= 3.1.0: # define R_nonint(x) (fabs((x) - R_forceint(x)) > 1e-7)
    public static boolean nonint(double x) {
        return (Math.abs((x) - Math.round(x)) > 1e-7 * Math.max(1., Math.abs(x)));
    }

    // R_D__0
    public static double d0(boolean logP) {
        return (logP ? Double.NEGATIVE_INFINITY : 0.);
    }

    // R_D__1
    public static double d1(boolean logP) {
        return (logP ? 0. : 1.);
    }

    // R_DT_0
    public static double dt0(boolean logP, boolean lowerTail) {
        return (lowerTail ? d0(logP) : d1(logP));
    }

    // R_DT_1
    public static double dt1(boolean logP, boolean lowerTail) {
        return (lowerTail ? d1(logP) : d0(logP));
    }

    /* Use 0.5 - p + 0.5 to perhaps gain 1 bit of accuracy */

    // R_D_Lval
    public static double dLval(boolean lowerTail, double p) {
        return (lowerTail ? (p) : (0.5 - (p) + 0.5));
    }

    // #define R_D_Cval(p) (lower_tail ? (0.5 - (p) + 0.5) : (p)) /* 1 - p */
    //
    // #define R_D_val(x) (log_p ? log(x) : (x)) /* x in pF(x,..) */
    // #define R_D_qIv(p) (log_p ? exp(p) : (p)) /* p in qF(p,..) */
    // R_D_exp
    public static double dExp(double x, boolean logP) {
        return logP ? (x) : Math.exp(x); /* exp(x) */
    }

    // #define R_D_log(p) (log_p ? (p) : log(p)) /* log(p) */
    // #define R_D_Clog(p) (log_p ? log1p(-(p)) : (0.5 - (p) + 0.5)) /* [log](1-p) */
    //
    // // log(1 - exp(x)) in more stable form than log1p(- R_D_qIv(x)) :
    // #define R_Log1_Exp(x) ((x) > -M_LN2 ? log(-expm1(x)) : log1p(-exp(x)))
    //
    // #define R_D_LExp(x) (log_p ? R_Log1_Exp(x) : log1p(-x))
    //
    // #define R_DT_val(x) (lower_tail ? R_D_val(x) : R_D_Clog(x))
    // #define R_DT_Cval(x) (lower_tail ? R_D_Clog(x) : R_D_val(x))
    //
    // R_DT_qIv
    public static double dtQIv(boolean logP, boolean lowerTail, double p) {
        return (logP ? (lowerTail ? Math.exp(p) : -Math.expm1(p)) : dLval(lowerTail, p));
    }
}
