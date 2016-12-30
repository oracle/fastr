/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000-2015, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
/*
 *  Algorithm AS 275 Appl.Statist. (1992), vol.41, no.2
 *  original  (C) 1992       Royal Statistical Society
 *
 *  Computes the noncentral chi-squared distribution function with
 *  positive real degrees of freedom df and nonnegative noncentrality
 *  parameter ncp.  pnchisq_raw is based on
 *
 *    Ding, C. G. (1992)
 *    Algorithm AS275: Computing the non-central chi-squared
 *    distribution function. Appl.Statist., 41, 478-482.
 */

package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.GammaFunctions.lgamma;
import static com.oracle.truffle.r.library.stats.GammaFunctions.lgammafn;
import static com.oracle.truffle.r.library.stats.MathConstants.DBL_EPSILON;
import static com.oracle.truffle.r.library.stats.MathConstants.DBL_MIN_EXP;
import static com.oracle.truffle.r.library.stats.MathConstants.M_LN10;
import static com.oracle.truffle.r.library.stats.MathConstants.M_LN2;
import static com.oracle.truffle.r.library.stats.MathConstants.M_LN_SQRT_2PI;
import static com.oracle.truffle.r.library.stats.MathConstants.logspaceAdd;

import com.oracle.truffle.r.library.stats.Chisq.PChisq;
import com.oracle.truffle.r.library.stats.RMathError.MLError;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function3_2;
import com.oracle.truffle.r.runtime.RError.Message;

public class PNChisq implements Function3_2 {
    private static final double _dbl_min_exp = M_LN2 * DBL_MIN_EXP;
    private final PChisq pchisq = new PChisq();

    @Override
    public double evaluate(double x, double df, double ncp, boolean lowerTail, boolean logP) {
        double ans;
        if (Double.isNaN(x) || Double.isNaN(df) || Double.isNaN(ncp)) {
            return x + df + ncp;
        }
        if (!Double.isFinite(df) || !Double.isFinite(ncp)) {
            return RMathError.defaultError();
        }

        if (df < 0. || ncp < 0.) {
            return RMathError.defaultError();
        }

        ans = pnchisqRaw(x, df, ncp, 1e-12, 8 * DBL_EPSILON, 1000000, lowerTail, logP);
        if (ncp >= 80) {
            if (lowerTail) {
                ans = RMath.fmin2(ans, DPQ.rd1(logP)); /* e.g., pchisq(555, 1.01, ncp = 80) */
            } else { /* !lower_tail */
                /* since we computed the other tail cancellation is likely */
                if (ans < (logP ? (-10. * M_LN10) : 1e-10)) {
                    RMathError.error(MLError.PRECISION, "pnchisq");
                }
                if (!logP) {
                    ans = RMath.fmax2(ans, 0.0);
                } /* Precaution PR#7099 */
            }
        }
        if (!logP || ans < -1e-8) {
            return ans;
        } else { // log_p && ans > -1e-8
            // prob. = Math.exp(ans) is near one: we can do better using the other tail
            debugPrintf("   pnchisq_raw(*, log_p): ans=%g => 2nd call, other tail\n", ans);
            // GNUR fix me: (sum,sum2) will be the same (=> return them as well and reuse here ?)
            ans = pnchisqRaw(x, df, ncp, 1e-12, 8 * DBL_EPSILON, 1000000, !lowerTail, false);
            return RMath.log1p(-ans);
        }
    }

    double pnchisqRaw(double x, double f, double theta /* = ncp */,
                    double errmax, double reltol, int itrmax,
                    boolean lowerTail, boolean logP) {
        if (x <= 0.) {
            if (x == 0. && f == 0.) {
                final double minusLambda = (-0.5 * theta);
                return lowerTail ? DPQ.rdexp(minusLambda, logP) : (logP ? DPQ.rlog1exp(minusLambda) : -RMath.expm1(minusLambda));
            }
            /* x < 0 or {x==0, f > 0} */
            return DPQ.rdt0(lowerTail, logP);
        }
        if (!Double.isFinite(x)) {
            return DPQ.rdt1(lowerTail, logP);
        }

        if (theta < 80) {
            /* use 110 for Inf, as ppois(110, 80/2, lower.tail=false) is 2e-20 */
            return smallTheta(x, f, theta, lowerTail, logP);
        }

        // else: theta == ncp >= 80 --------------------------------------------
        debugPrintf("pnchisq(x=%g, f=%g, theta=%g >= 80): ", x, f, theta);

        // Series expansion ------- FIXME: log_p=true, lower_tail=false only applied at end

        double lam = .5 * theta;
        boolean lamSml = (-lam < _dbl_min_exp);
        double lLam = -1;
        /* LDOUBLE */double lu = -1;
        /* LDOUBLE */double u;
        if (lamSml) {
            u = 0;
            lu = -lam; /* == ln(u) */
            lLam = Math.log(lam);
        } else {
            u = Math.exp(-lam);
        }

        /* evaluate the first term */
        /* LDOUBLE */double v = u;
        double x2 = .5 * x;
        double f2 = .5 * f;
        double fx2n = f - x;

        debugPrintf("-- v=Math.exp(-th/2)=%g, x/2= %g, f/2= %g\n", v, x2, f2);

        /* LDOUBLE */double lt;
        /* LDOUBLE */double t;
        if (f2 * DBL_EPSILON > 0.125 && /* very large f and x ~= f: probably needs */
                        MathWrapper.abs(t = x2 - f2) < /* another algorithm anyway */
                        Math.sqrt(DBL_EPSILON) * f2) {
            /* evade cancellation error */
            /* t = Math.exp((1 - t)*(2 - t/(f2 + 1))) / Math.sqrt(2*M_PI*(f2 + 1)); */
            lt = (1 - t) * (2 - t / (f2 + 1)) - M_LN_SQRT_2PI - 0.5 * Math.log(f2 + 1);
            debugPrintf(" (case I) ==> ");
        } else {
            /* Usual case 2: careful not to overflow .. : */
            lt = f2 * Math.log(x2) - x2 - lgammafn(f2 + 1);
        }
        debugPrintf(" lt= %g", lt);

        boolean tSml = (lt < _dbl_min_exp);
        double lX = -1;
        double term;
        /* LDOUBLE */double ans;
        if (tSml) {
            debugPrintf(" is very small\n");
            if (x > f + theta + 5 * Math.sqrt(2 * (f + 2 * theta))) {
                /* x > E[X] + 5* sigma(X) */
                return DPQ.rdt1(lowerTail, logP); /*
                                                   * GNUR fix me: could be more accurate than 0.
                                                   */
            } /* else */
            lX = Math.log(x);
            ans = term = 0.;
            t = 0;
        } else {
            t = MathWrapper.exp(lt);
            debugPrintf(", t=Math.exp(lt)= %g\n", t);
            ans = term = (v * t);
        }

        int n;
        double f2n;
        boolean isIt;
        double bound;
        for (n = 1, f2n = f + 2., fx2n += 2.;; n++, f2n += 2, fx2n += 2) {
            debugPrintf("\n _OL_: n=%d", n);
            /*
             * f2n === f + 2*n fx2n === f - x + 2*n > 0 <==> (f+2n) > x
             */
            if (fx2n > 0) {
                /* find the error bound and check for convergence */
                bound = t * x / fx2n;
                debugPrintf("\n L10: n=%d; term= %g; bound= %g", n, term, bound);
                boolean isR = isIt = false;
                boolean isB;
                /* convergence only if BOTH absolute and relative error < 'bnd' */
                if (((isB = (bound <= errmax)) &&
                                (isR = (term <= reltol * ans))) || (isIt = (n > itrmax))) {
                    debugPrintf("BREAK n=%d %s; bound= %g %s, rel.err= %g %s\n",
                                    n, (isIt ? "> itrmax" : ""),
                                    bound, (isB ? "<= errmax" : ""),
                                    term / ans, (isR ? "<= reltol" : ""));
                    break; /* out completely */
                }

            }

            /* evaluate the next term of the */
            /* expansion and then the partial sum */

            if (lamSml) {
                lu += lLam - Math.log(n); /* u = u* lam / n */
                if (lu >= _dbl_min_exp) {
                    /* no underflow anymore ==> change regime */
                    debugPrintf(" n=%d; nomore underflow in u = Math.exp(lu) ==> change\n",
                                    n);
                    v = u = MathWrapper.exp(lu); /* the first non-0 'u' */
                    lamSml = false;
                }
            } else {
                u *= lam / n;
                v += u;
            }
            if (tSml) {
                lt += lX - Math.log(f2n); /* t <- t * (x / f2n) */
                if (lt >= _dbl_min_exp) {
                    /* no underflow anymore ==> change regime */
                    debugPrintf("  n=%d; nomore underflow in t = Math.exp(lt) ==> change\n", n);
                    t = MathWrapper.exp(lt); /* the first non-0 't' */
                    tSml = false;
                }
            } else {
                t *= x / f2n;
            }
            if (!lamSml && !tSml) {
                term = v * t;
                ans += term;
            }

        } /* for(n ...) */

        if (isIt) {
            RMathError.warning(Message.PCHISQ_NOT_CONVERGED_WARNING, x, itrmax);
        }

        debugPrintf("\n == L_End: n=%d; term= %g; bound=%g\n", n, term, bound);
        return DPQ.rdtval(ans, lowerTail, logP);
    }

    private double smallTheta(double x, double f, double theta, boolean lowerTail, boolean logP) {
        // Have pgamma(x,s) < x^s / Gamma(s+1) (< and ~= for small x)
        // ==> pchisq(x, f) = pgamma(x, f/2, 2) = pgamma(x/2, f/2)
        // < (x/2)^(f/2) / Gamma(f/2+1) < eps
        // <==> f/2 * Math.log(x/2) - Math.log(Gamma(f/2+1)) < Math.log(eps) ( ~= -708.3964 )
        // <==> Math.log(x/2) < 2/f*(Math.log(Gamma(f/2+1)) + Math.log(eps))
        // <==> Math.log(x) < Math.log(2) + 2/f*(Math.log(Gamma(f/2+1)) + Math.log(eps))
        if (lowerTail && f > 0. && Math.log(x) < M_LN2 + 2 / f * (lgamma(f / 2. + 1) + _dbl_min_exp)) {
            // all pchisq(x, f+2*i, lower_tail, false), i=0,...,110 would underflow to 0.
            // ==> work in log scale
            double lambda = 0.5 * theta;
            double sum;
            double sum2;
            double pr = -lambda;
            sum = sum2 = Double.NEGATIVE_INFINITY;
            /* we need to renormalize here: the result could be very close to 1 */
            int i;
            for (i = 0; i < 110; pr += Math.log(lambda) - Math.log(++i)) {
                sum2 = logspaceAdd(sum2, pr);
                sum = logspaceAdd(sum, pr + pchisq.evaluate(x, f + 2 * i, lowerTail, true));
                if (sum2 >= -1e-15) {
                    /* <=> EXP(sum2) >= 1-1e-15 */ break;
                }
            }
            /* LDOUBLE */double ans = sum - sum2;
            debugPrintf("pnchisq(x=%g, f=%g, th.=%g); th. < 80, logspace: i=%d, ans=(sum=%g)-(sum2=%g)\n",
                            x, f, theta, i, sum, sum2);
            return logP ? ans : MathWrapper.exp(ans);
        } else {
            /* LDOUBLE */double lambda = 0.5 * theta;
            /* LDOUBLE */double sum = 0;
            /* LDOUBLE */double sum2 = 0;
            /* LDOUBLE */double pr = Math.exp(-lambda); // does this need a feature test?
            /* we need to renormalize here: the result could be very close to 1 */
            int i;
            for (i = 0; i < 110; pr *= lambda / ++i) {
                // pr == Math.exp(-lambda) lambda^i / i! == dpois(i, lambda)
                sum2 += pr;
                // pchisq(*, i, *) is strictly decreasing to 0 for lower_tail=true
                // and strictly increasing to 1 for lower_tail=false
                sum += pr * pchisq.evaluate(x, f + 2 * i, lowerTail, false);
                if (sum2 >= 1 - 1e-15) {
                    break;
                }
            }
            /* LDOUBLE */double ans = sum / sum2;
            debugPrintf("pnchisq(x=%g, f=%g, theta=%g); theta < 80: i=%d, sum=%g, sum2=%g\n",
                            x, f, theta, i, sum, sum2);
            return logP ? MathWrapper.log(ans) : ans;
        }
    }

    private void debugPrintf(@SuppressWarnings("unused") String fmt, @SuppressWarnings("unused") Object... args) {
        // System.out.printf(fmt + "\n", args);
    }

    /**
     * For easier switch to {@code Decimal} if necessary.
     */
    private static final class MathWrapper {
        public static double exp(double x) {
            return Math.exp(x);
        }

        public static double log(double x) {
            return Math.log(x);
        }

        public static double abs(double x) {
            return Math.abs(x);
        }
    }
}
