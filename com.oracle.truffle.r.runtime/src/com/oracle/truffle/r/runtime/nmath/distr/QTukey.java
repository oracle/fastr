/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000--2005, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 *  based in part on AS70 (C) 1974 Royal Statistical Society
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.r.runtime.nmath.DPQ;
import com.oracle.truffle.r.runtime.nmath.DPQ.EarlyReturn;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_2;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.RMathError;
import com.oracle.truffle.r.runtime.nmath.RMathError.MLError;
import com.oracle.truffle.r.runtime.nmath.TOMS708;

/*
 *  Copenhaver, Margaret Diponzio & Holland, Burt S.
 *  Multiple comparisons of simple effects in
 *  the two-way analysis of variance with fixed effects.
 *  Journal of Statistical Computation and Simulation,
 *  Vol.30, pp.1-15, 1988.
 *
 *  Uses the secant method to find critical values.
 *
 *  p = confidence level (1 - alpha)
 *  rr = no. of rows or groups
 *  cc = no. of columns or treatments
 *  df = degrees of freedom of error term
 *
 *  ir(1) = error flag = 1 if wprob probability > 1
 *  ir(2) = error flag = 1 if ptukey probability > 1
 *  ir(3) = error flag = 1 if convergence not reached in 50 iterations
 *       = 2 if df < 2
 *
 *  qtukey = returned critical value
 *
 *  If the difference between successive iterates is less than eps,
 *  the search is terminated
 */

public final class QTukey implements Function4_2 {
    private static final double eps = 0.0001;
    private static final int maxiter = 50;

    private final PTukey ptukey = new PTukey();

    @Override
    public double evaluate(double pIn, double rr, double cc, double df, boolean lowerTail, boolean logP) {
        if (Double.isNaN(pIn) || Double.isNaN(rr) || Double.isNaN(cc) || Double.isNaN(df)) {
            RMathError.error(MLError.DOMAIN, "qtukey");
            return pIn + rr + cc + df;
        }

        /* df must be > 1 ; there must be at least two values */
        if (df < 2 || rr < 1 || cc < 2) {
            return RMathError.defaultError();
        }

        try {
            DPQ.rqp01boundaries(pIn, 0, Double.POSITIVE_INFINITY, lowerTail, logP);
        } catch (EarlyReturn e) {
            return e.result;
        }

        double p = DPQ.rdtqiv(pIn, lowerTail, logP); /* lowerTail,non-log "p" */

        /* Initial value */

        double x0 = qinv(p, cc, df);

        /* Find prob(value < x0) */

        double valx0 = ptukey.evaluate(x0, rr, cc, df, /* LOWER */true, /* LOG_P */false) - p;

        /* Find the second iterate and prob(value < x1). */
        /* If the first iterate has probability value */
        /* exceeding p then second iterate is 1 less than */
        /* first iterate; otherwise it is 1 greater. */

        double x1 = valx0 > 0.0 ? RMath.fmax2(0.0, x0 - 1.0) : x0 + 1.0;
        double valx1 = ptukey.evaluate(x1, rr, cc, df, /* LOWER */true, /* LOG_P */false) - p;

        /* Find new iterate */

        double ans = 0.;
        for (int iter = 1; iter < maxiter; iter++) {
            ans = x1 - ((valx1 * (x1 - x0)) / (valx1 - valx0));
            valx0 = valx1;

            /* New iterate must be >= 0 */

            x0 = x1;
            if (ans < 0.0) {
                ans = 0.0;
                valx1 = -p;
            }
            /* Find prob(value < new iterate) */

            valx1 = ptukey.evaluate(ans, rr, cc, df, /* LOWER */true, /* LOG_P */false) - p;
            x1 = ans;

            /* If the difference between two successive */
            /* iterates is less than eps, stop */
            double xabs = TOMS708.fabs(x1 - x0);
            if (xabs < eps) {
                return ans;
            }
        }

        /* The process did not converge in 'maxiter' iterations */
        RMathError.error(MLError.NOCONV, "qtukey");
        return ans;

    }

    private static final double p0 = 0.322232421088;
    private static final double q0 = 0.993484626060e-01;
    private static final double p1 = -1.0;
    private static final double q1 = 0.588581570495;
    private static final double p2 = -0.342242088547;
    private static final double q2 = 0.531103462366;
    private static final double p3 = -0.204231210125;
    private static final double q3 = 0.103537752850;
    private static final double p4 = -0.453642210148e-04;
    private static final double q4 = 0.38560700634e-02;
    private static final double c1 = 0.8832;
    private static final double c2 = 0.2368;
    private static final double c3 = 1.214;
    private static final double c4 = 1.208;
    private static final double c5 = 1.4142;
    private static final double vmax = 120.0;

    static double qinv(double p, double c, double v) {
        double ps = 0.5 - 0.5 * p;
        double yi = Math.sqrt(Math.log(1.0 / (ps * ps)));
        double t = yi + ((((yi * p4 + p3) * yi + p2) * yi + p1) * yi + p0) / ((((yi * q4 + q3) * yi + q2) * yi + q1) * yi + q0);
        if (v < vmax) {
            t += (t * t * t + t) / v / 4.0;
        }
        double q = c1 - c2 * t;
        if (v < vmax) {
            q += -c3 / v + c4 * t / v;
        }
        return t * (q * Math.log(c - 1.0) + c5);
    }
}
