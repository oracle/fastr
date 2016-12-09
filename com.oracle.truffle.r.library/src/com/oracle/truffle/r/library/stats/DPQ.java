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

/**
 * Contains macros transcribed from dpq.h. Naming convention is all lowercase and remove underscores
 * from GnuR name. Some macros change control flow by explicitly returning, this is handled by
 * throwing {@link EarlyReturn} exception that encapsulates the desired return value.
 */
public final class DPQ {
    private DPQ() {
        // only static methods
    }

    public static final class EarlyReturn extends ControlFlowException {
        private static final long serialVersionUID = 1182697355931636213L;
        public final double result;

        private EarlyReturn(double result) {
            this.result = result;
        }
    }

    // R >= 3.1.0: # define R_nonint(x) (fabs((x) - R_forceint(x)) > 1e-7)
    // Note: if true should be followed by "return d0(logP)", consider using nointCheck instead
    public static boolean nonint(double x) {
        return Math.abs(x - Math.round(x)) > 1e-7 * Math.max(1., Math.abs(x));
    }

    // R_D__0
    public static double rd0(boolean logP) {
        return logP ? Double.NEGATIVE_INFINITY : 0.;
    }

    // R_D__1
    public static double rd1(boolean logP) {
        return logP ? 0. : 1.;
    }

    // R_DT_0
    public static double rdt0(boolean lowerTail, boolean logP) {
        return lowerTail ? rd0(logP) : rd1(logP);
    }

    // R_D_log
    public static double rdlog(double p, boolean logP) {
        return logP ? p : Math.log(p);
    }

    public static double rdtlog(double p, boolean lowerTail, boolean logp) {
        return lowerTail ? rdlog(p, logp) : rdlexp(p, logp);
    }

    // R_DT_1
    public static double rdt1(boolean lowerTail, boolean logP) {
        return lowerTail ? rd1(logP) : rd0(logP);
    }

    // R_D_Lval
    public static double rdlval(double p, boolean lowerTail) {
        return lowerTail ? p : 0.5 - p + 0.5;
    }

    public static double rdcval(double p, boolean lowerTail) {
        return lowerTail ? 0.5 - p + 0.5 : p; /* 1 - p */
    }

    // R_D_val
    public static double rdval(double x, boolean logP) {
        return logP ? Math.log(x) : x; /* x in pF(x,..) */
    }

    public static double rdexp(double x, boolean logP) {
        return logP ? x : Math.exp(x); /* exp(x) */
    }

    // R_D_LExp
    public static double rdlexp(double x, boolean logP) {
        return (logP ? rlog1exp(x) : RMath.log1p(-x));
    }

    public static double rdfexp(double f, double x, boolean giveLog) {
        return giveLog ? -0.5 * Math.log(f) + x : Math.exp(x) / Math.sqrt(f);
    }

    // R_Log1_Exp
    public static double rlog1exp(double x) {
        return ((x) > -M_LN2 ? Math.log(-RMath.expm1(x)) : RMath.log1p(-Math.exp(x)));
    }

    // R_DT_Clog
    public static double rdtclog(double p, boolean lowerTail, boolean logP) {
        return lowerTail ? rdlexp(p, logP) : rdlog(p, logP);
    }

    // R_DT_qIv
    public static double rdtqiv(double p, boolean lowerTail, boolean logP) {
        return logP ? lowerTail ? Math.exp(p) : -Math.expm1(p) : rdlval(p, lowerTail);
    }

    // R_DT_CIv
    public static double rdtciv(double p, boolean lowerTail, boolean logP) {
        return logP ? lowerTail ? -Math.expm1(p) : Math.exp(p) : rdcval(p, lowerTail);
    }

    // R_Q_P01_boundaries
    public static void rqp01boundaries(double p, double left, double right, boolean lowerTail, boolean logP) throws EarlyReturn {
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

    // R_Q_P01_check
    public static void rqp01check(double p, boolean logP) throws EarlyReturn {
        if ((logP && p > 0) || (!logP && (p < 0 || p > 1))) {
            throw new EarlyReturn(RMath.mlError());
        }
    }

    // Unimplemented macros:
    //
    // #define R_DT_exp(x) R_D_exp(R_D_Lval(x)) /* exp(x) */
    // #define R_DT_Cexp(x) R_D_exp(R_D_Cval(x)) /* exp(1 - x) */
    //
    // #define R_DT_log(p) (lower_tail? R_D_log(p) : R_D_LExp(p))/* log(p) in qF */
    // #define R_DT_Clog(p) (lower_tail? R_D_LExp(p): R_D_log(p))/* log(1-p) in qF*/
    // #define R_DT_Log(p) (lower_tail? (p) : R_Log1_Exp(p))

    // FastR helpers:

    public static void nointCheckWarning(double x, String varName) {
        RError.warning(RError.SHOW_CALLER, Message.NON_INTEGER_N, varName, x);
    }

    public static void nonintCheck(double x, boolean giveLog) throws EarlyReturn {
        if (nonint(x)) {
            nointCheckWarning(x, "x");
            throw new EarlyReturn(rd0(giveLog));
        }
    }
}
