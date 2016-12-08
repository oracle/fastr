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

import static com.oracle.truffle.r.library.stats.MathConstants.M_LN2;

import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;

// transcribed from dpq.h

public final class DPQ {
    private DPQ() {
        // private
    }

    // R >= 3.1.0: # define R_nonint(x) (fabs((x) - R_forceint(x)) > 1e-7)
    // Note: if true should be followed by "return d0(logP)"
    // Consider using dNointCheck instead
    public static boolean nonint(double x) {
        return Math.abs(x - Math.round(x)) > 1e-7 * Math.max(1., Math.abs(x));
    }

    // R_D__0
    public static double d0(boolean logP) {
        return logP ? Double.NEGATIVE_INFINITY : 0.;
    }

    // R_D__1
    public static double d1(boolean logP) {
        return logP ? 0. : 1.;
    }

    // R_DT_0
    public static double dt0(boolean logP, boolean lowerTail) {
        return lowerTail ? d0(logP) : d1(logP);
    }

    // R_D_log
    public static double dLog(double p, boolean logP) {
        return logP ? p : Math.log(p);
    }

    // R_DT_1
    public static double dt1(boolean logP, boolean lowerTail) {
        return lowerTail ? d1(logP) : d0(logP);
    }

    /* Use 0.5 - p + 0.5 to perhaps gain 1 bit of accuracy */

    // R_D_Lval
    public static double dLval(boolean lowerTail, double p) {
        return lowerTail ? p : 0.5 - p + 0.5;
    }

    public static double dCval(double p, boolean lowerTail) {
        return lowerTail ? 0.5 - p + 0.5 : p; /* 1 - p */
    }

    //
    public static double dVal(double x, boolean logP) {
        return logP ? Math.log(x) : x; /* x in pF(x,..) */
    }

    public static double dExp(double x, boolean logP) {
        return logP ? x : Math.exp(x); /* exp(x) */
    }

    /* log(1-exp(x)): R_D_LExp(x) == (log1p(- R_D_qIv(x))) but even more stable: */
    // #define R_D_LExp(x) (log_p ? R_Log1_Exp(x) : log1p(-x))
    public static double dLExp(double x, boolean logP) {
        return (logP ? log1Exp(x, logP) : StatsUtil.log1p(-x));
    }

    // #define R_Log1_Exp(x) ((x) > -M_LN2 ? log(-expm1(x)) : log1p(-exp(x)))
    public static double log1Exp(double x, boolean logP) {
        return ((x) > -M_LN2 ? Math.log(-StatsUtil.expm1(x)) : StatsUtil.log1p(-Math.exp(x)));
    }

    // #define R_D_log(p) (log_p ? (p) : log(p)) /* log(p) */
    public static double dClog(double p, boolean logP) {
        return logP ? StatsUtil.log1p(-p) : 0.5 - p + 0.5; /* [log](1-p) */
    }

    // #define R_DT_Clog(p) (lower_tail? R_D_LExp(p): R_D_log(p))/* log(1-p) in qF*/
    public static double dtCLog(double p, boolean lowerTail, boolean logP) {
        return lowerTail ? dLExp(p, logP) : dLog(p, logP);
    }

    //
    // // log(1 - exp(x)) in more stable form than log1p(- R_D_qIv(x)) :
    // #define R_Log1_Exp(x) ((x) > -M_LN2 ? log(-expm1(x)) : log1p(-exp(x)))
    //
    // #define R_D_LExp(x) (log_p ? R_Log1_Exp(x) : log1p(-x))
    //
    // #define R_DT_val(x) (lower_tail ? R_D_val(x) : R_D_Clog(x))
    public static double dtCval(double x, boolean lowerTail, boolean logP) {
        return lowerTail ? dClog(x, logP) : dVal(x, logP);
    }

    //
    // R_DT_qIv
    public static double dtQIv(double p, boolean lowerTail, boolean logP) {
        return logP ? lowerTail ? Math.exp(p) : -Math.expm1(p) : dLval(lowerTail, p);
    }

    // /*#define R_DT_CIv(p) R_D_Cval(R_D_qIv(p)) * 1 - p in qF */

    public static double dtCIv(double p, boolean lowerTail, boolean logP) {
        return logP ? lowerTail ? -Math.expm1(p) : Math.exp(p) : dCval(p, lowerTail);
    }

    //
    // #define R_DT_exp(x) R_D_exp(R_D_Lval(x)) /* exp(x) */
    // #define R_DT_Cexp(x) R_D_exp(R_D_Cval(x)) /* exp(1 - x) */
    //
    // #define R_DT_log(p) (lower_tail? R_D_log(p) : R_D_LExp(p))/* log(p) in qF */
    // #define R_DT_Clog(p) (lower_tail? R_D_LExp(p): R_D_log(p))/* log(1-p) in qF*/
    // #define R_DT_Log(p) (lower_tail? (p) : R_Log1_Exp(p))

    public static final class EarlyReturn extends ControlFlowException {
        private static final long serialVersionUID = 1182697355931636213L;
        public final double result;

        private EarlyReturn(double result) {
            this.result = result;
        }
    }

    /*
     * Do the boundaries exactly for q*() functions : Often _LEFT_ = ML_NEGINF , and very often
     * _RIGHT_ = ML_POSINF;
     *
     * R_Q_P01_boundaries(p, _LEFT_, _RIGHT_) :<==>
     *
     * R_Q_P01_check(p); if (p == R_DT_0) return _LEFT_ ; if (p == R_DT_1) return _RIGHT_;
     *
     * the following implementation should be more efficient (less tests):
     */
    public static void qP01Boundaries(double p, double left, double right, boolean lowerTail, boolean logP) throws EarlyReturn {
        if (logP) {
            if (p > 0) {
                throw new EarlyReturn(Double.NaN);
            }
            if (p == 0) {
                /* upper bound */
                throw new EarlyReturn(lowerTail ? right : left);
            }
            if (p == Double.NEGATIVE_INFINITY) {
                throw new EarlyReturn(lowerTail ? left : right);
            }
        } else { /* !log_p */
            if (p < 0 || p > 1) {
                throw new EarlyReturn(Double.NaN);
            }
            if (p == 0) {
                throw new EarlyReturn(lowerTail ? left : right);
            }
            if (p == 1) {
                throw new EarlyReturn(lowerTail ? right : left);
            }
        }
    }

    // #define R_Q_P01_check(p) \
    // if ((log_p && p > 0) || \
    // (!log_p && (p < 0 || p > 1)) ) \
    // ML_ERR_return_NAN
    public static void qQP01Check(double p, boolean logP) throws EarlyReturn {
        if ((logP && p > 0) || (!logP && (p < 0 || p > 1))) {
            throw new EarlyReturn(StatsUtil.mlError());
        }
    }

    /* [neg]ative or [non int]eger : */
    public static boolean dNegInonint(double x) {
        return x < 0 || nonint(x);
    }

    // FastR helpers:

    public static void nointCheckWarning(double x, String varName) {
        RError.warning(RError.SHOW_CALLER, Message.NON_INTEGER_N, varName, x);
    }

    public static void dNonintCheck(double x, boolean giveLog) throws EarlyReturn {
        if (nonint(x)) {
            nointCheckWarning(x, "x");
            throw new EarlyReturn(d0(giveLog));
        }
    }
}
