/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;

import static com.oracle.truffle.r.library.stats.MathConstants.*;

/*
 * transcribed from toms708.c - as the original file contains no copyright header, we assume that it is copyright R code and R foundation.
 */

public class TOMS708 {

    @SuppressWarnings("unused")
    private static void debugPrintf(String format, Object... args) {
        // System.out.print(String.format(format, args));
    }

    private static void emitWarning(String format, Object... args) {
        RError.warning(RError.SHOW_CALLER, Message.GENERIC, String.format(format, args));
    }

    // R_Log1_Exp
    public static double log1Exp(double x) {
        return ((x) > -M_LN2 ? log(-rexpm1(x)) : log1p(-exp(x)));
    }

    private static double sin(double v) {
        return Math.sin(v);
    }

    private static double cos(double v) {
        return Math.cos(v);
    }

    private static double log(double v) {
        return Math.log(v);
    }

    private static double sqrt(double v) {
        return Math.sqrt(v);
    }

    private static double log1p(double v) {
        return Math.log1p(v);
    }

    private static double exp(double v) {
        return Math.exp(v);
    }

    private static double fabs(double v) {
        return Math.abs(v);
    }

    private static double pow(double a, double b) {
        return Math.pow(a, b);
    }

    private static double min(double a, double b) {
        return Math.min(a, b);
    }

    private static double max(double a, double b) {
        return Math.max(a, b);
    }

    private static final double ML_NEGINF = Double.NEGATIVE_INFINITY;
    private static final int INT_MAX = Integer.MAX_VALUE;
    private static final double DBL_MIN = Double.MIN_NORMAL;
    private static final double INV_SQRT_2_PI = .398942280401433; /* == 1/sqrt(2*pi); */

    public static final class MathException extends RuntimeException {
        private static final long serialVersionUID = -4745984791703065276L;

        private final int code;

        public MathException(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    public static final class Bratio {
        public double w;
        public double w1;
        public int ierr;

        private Bratio(double w, double w1, int ierr) {
            this.w = w;
            this.w1 = w1;
            this.ierr = ierr;
        }

        private enum States {
            Start,
            L131,
            L140,
            L_w_bpser,
            L_w1_bpser,
            L_end_from_w1_log,
            L_end_from_w1,
            L_end_from_w,
            L_bfrac,
        }

        private static Bratio result(double w, double w1, int ierr) {
            return new Bratio(w, w1, ierr);
        }

        public static Bratio bratio(double a, double b, double x, double y, boolean logP) {
            /*
             * -----------------------------------------------------------------------
             * 
             * Evaluation of the Incomplete Beta function I_x(a,b)
             * 
             * --------------------
             * 
             * It is assumed that a and b are nonnegative, and that x <= 1 and y = 1 - x. Bratio
             * assigns w and w1 the values
             * 
             * w = I_x(a,b) w1 = 1 - I_x(a,b)
             * 
             * ierr is a variable that reports the status of the results. If no input errors are
             * detected then ierr is set to 0 and w and w1 are computed. otherwise, if an error is
             * detected, then w and w1 are assigned the value 0 and ierr is set to one of the
             * following values ...
             * 
             * ierr = 1 if a or b is negative ierr = 2 if a = b = 0 ierr = 3 if x < 0 or x > 1 ierr
             * = 4 if y < 0 or y > 1 ierr = 5 if x + y != 1 ierr = 6 if x = a = 0 ierr = 7 if y = b
             * = 0 ierr = 8 "error" in bgrat()
             * 
             * -------------------- Written by Alfred H. Morris, Jr. Naval Surface Warfare Center
             * Dahlgren, Virginia Revised ... Nov 1991
             * -----------------------------------------------------------------------
             */

            double w;
            double w1;
            int ierr = 0;

            boolean doSwap = false;
            int n = 0;
            double z = 0;
            double a0 = 0;
            double b0 = 0;
            double x0 = 0;
            double y0 = 0;
            double eps = 0;
            double lambda = 0;

            /*
             * eps is a machine dependent constant: the smallest floating point number for which 1.0
             * + eps > 1.0
             */
            eps = RRuntime.EPSILON; /* == DBL_EPSILON (in R, Rmath) */

            /* ----------------------------------------------------------------------- */
            w = DPQ.d0(logP);
            w1 = DPQ.d0(logP);

            if (a < 0.0 || b < 0.0) {
                return result(w, w1, 1);
            }
            if (a == 0.0 && b == 0.0) {
                return result(w, w1, 2);
            }
            if (x < 0.0 || x > 1.0) {
                return result(w, w1, 3);
            }
            if (y < 0.0 || y > 1.0) {
                return result(w, w1, 4);
            }

            /* check that 'y == 1 - x' : */
            z = x + y - 0.5 - 0.5;

            if (Math.abs(z) > eps * 3.0) {
                return result(w, w1, 5);
            }
            debugPrintf("bratio(a=%f, b=%f, x=%9f, y=%9f, .., log_p=%b): ", a, b, x, y, logP);

            if (x == 0.0) {
                if (a == 0.0) {
                    return result(w, w1, 6);
                }
                // else:
                w = DPQ.d0(logP);
                w1 = DPQ.d1(logP);
                return result(w, w1, 0);
            }
            if (y == 0.0) {
                if (b == 0.0) {
                    return result(w, w1, 7);
                }
                // else:
                w = DPQ.d1(logP);
                w1 = DPQ.d0(logP);
                return result(w, w1, 0);
            }

            if (a == 0.0) {
                // else:
                w = DPQ.d1(logP);
                w1 = DPQ.d0(logP);
                return result(w, w1, 0);
            }
            if (b == 0.0) {
                w = DPQ.d0(logP);
                w1 = DPQ.d1(logP);
                return result(w, w1, 0);
            }

            eps = Math.max(eps, 1e-15);
            boolean aLtB = a < b;
            if (/* max(a,b) */(aLtB ? b : a) < eps * .001) {
                /* procedure for a and b < 0.001 * eps */
                // L230: -- result *independent* of x (!)
                // *w = a/(a+b) and w1 = b/(a+b) :
                if (logP) {
                    if (aLtB) {
                        w = Math.log1p(-a / (a + b)); // notably if a << b
                        w1 = Math.log(a / (a + b));
                    } else { // b <= a
                        w = Math.log(b / (a + b));
                        w1 = Math.log1p(-b / (a + b));
                    }
                } else {
                    w = b / (a + b);
                    w1 = a / (a + b);
                }
                debugPrintf("a & b very small -> simple ratios (%f,%f)\n", w, w1);
                return result(w, w1, 0);
            }

            States state = States.Start;
            Bgrat bgrat = new Bgrat();
            L_end: while (true) {
                while (true) {
                    switch (state) {
                        case Start:

                            if (Math.min(a, b) <= 1.) { /*------------------------ a <= 1  or  b <= 1 ---- */

                                doSwap = (x > 0.5);
                                if (doSwap) {
                                    a0 = b;
                                    x0 = y;
                                    b0 = a;
                                    y0 = x;
                                } else {
                                    a0 = a;
                                    x0 = x;
                                    b0 = b;
                                    y0 = y;
                                }
                                /* now have x0 <= 1/2 <= y0 (still x0+y0 == 1) */
                                debugPrintf(" min(a,b) <= 1, do_swap=%b;", doSwap);

                                if (b0 < Math.min(eps, eps * a0)) { /* L80: */
                                    w = fpser(a0, b0, x0, eps, logP);
                                    w1 = logP ? log1Exp(w) : 0.5 - w + 0.5;
                                    debugPrintf("  b0 small -> w := fpser(*) = %.15f\n", w);
                                    break L_end;
                                }

                                if (a0 < Math.min(eps, eps * b0) && b0 * x0 <= 1.0) { /* L90: */
                                    w1 = apser(a0, b0, x0, eps);
                                    debugPrintf("  a0 small -> w1 := apser(*) = %.15fg\n", w1);
                                    state = States.L_end_from_w1;
                                    continue;
                                }

                                boolean didBup = false;
                                if (Math.max(a0, b0) > 1.0) {
                                    /* L20: min(a,b) <= 1 < max(a,b) */
                                    debugPrintf("\n L20:  min(a,b) <= 1 < max(a,b); ");
                                    if (b0 <= 1.0) {
                                        state = States.L_w_bpser;
                                        continue;
                                    }

                                    if (x0 >= 0.29) {
                                        /* was 0.3, PR#13786 */
                                        state = States.L_w1_bpser;
                                        continue;
                                    }

                                    if (x0 < 0.1 && Math.pow(x0 * b0, a0) <= 0.7) {
                                        state = States.L_w_bpser;
                                        continue;
                                    }

                                    if (b0 > 15.0) {
                                        w1 = 0.;
                                        state = States.L131;
                                    }
                                } else { /* a, b <= 1 */
                                    debugPrintf("\n      both a,b <= 1; ");
                                    if (a0 >= Math.min(0.2, b0)) {
                                        state = States.L_w_bpser;
                                        continue;
                                    }

                                    if (Math.pow(x0, a0) <= 0.9) {
                                        state = States.L_w_bpser;
                                        continue;
                                    }

                                    if (x0 >= 0.3) {
                                        state = States.L_w1_bpser;
                                        continue;
                                    }
                                }
                                if (state != States.L131) {
                                    n = 20; /* goto L130; */
                                    w1 = bup(b0, a0, y0, x0, n, eps, false);
                                    debugPrintf("  ... n=20 and *w1 := bup(*) = %.15f; ", w1);
                                    didBup = true;

                                    b0 += n;
                                }
                                debugPrintf(" L131: bgrat(*, w1=%.15f) ", w1);
                                bgrat.w = w1;
                                bgrat.bgrat(b0, a0, y0, x0, 15 * eps, false);
                                w1 = bgrat.w;

                                if (w1 == 0 || (0 < w1 && w1 < 1e-310)) { // w1=0 or very close:
                                    // "almost surely" from underflow, try more: [2013-03-04]
                                    // FIXME: it is even better to do this in bgrat *directly* at
                                    // least for the case
                                    // !did_bup, i.e., where *w1 = (0 or -Inf) on entry
                                    if (didBup) { // re-do that part on log scale:
                                        w1 = bup(b0 - n, a0, y0, x0, n, eps, true);
                                    } else {
                                        w1 = Double.NEGATIVE_INFINITY; // = 0 on log-scale
                                    }
                                    bgrat.w = w1;
                                    bgrat.bgrat(b0, a0, y0, x0, 15 * eps, false);
                                    w1 = bgrat.w;
                                    if (bgrat.ierr != 0) {
                                        ierr = 8;
                                    }
                                    state = States.L_end_from_w1_log;
                                    continue;
                                }
                                // else
                                if (bgrat.ierr != 0) {
                                    ierr = 8;
                                }
                                if (w1 < 0) {
                                    RError.warning(RError.SHOW_CALLER, Message.GENERIC, String.format("bratio(a=%f, b=%f, x=%f): bgrat() -> w1 = %f", a, b, x, w1));
                                }
                                state = States.L_end_from_w1;
                                continue;
                            } else { /*
                                      * L30: -------------------- both a, b > 1 {a0 > 1 & b0 > 1}
                                      * ---
                                      */

                                if (a > b) {
                                    lambda = (a + b) * y - b;
                                } else {
                                    lambda = a - (a + b) * x;
                                }

                                doSwap = (lambda < 0.0);
                                if (doSwap) {
                                    lambda = -lambda;
                                    a0 = b;
                                    x0 = y;
                                    b0 = a;
                                    y0 = x;
                                } else {
                                    a0 = a;
                                    x0 = x;
                                    b0 = b;
                                    y0 = y;
                                }

                                debugPrintf("  L30:  both  a, b > 1; |lambda| = %f, do_swap = %b\n", lambda, doSwap);

                                if (b0 < 40.0) {
                                    debugPrintf("  b0 < 40;");
                                    if (b0 * x0 <= 0.7 || (logP && lambda > 650.)) {
                                        state = States.L_w_bpser;
                                        continue;
                                    } else {
                                        state = States.L140;
                                        continue;
                                    }
                                } else if (a0 > b0) { /* ---- a0 > b0 >= 40 ---- */
                                    debugPrintf("  a0 > b0 >= 40;");
                                    if (b0 <= 100.0 || lambda > b0 * 0.03) {
                                        state = States.L_bfrac;
                                        continue;
                                    }
                                } else if (a0 <= 100.0) {
                                    debugPrintf("  a0 <= 100; a0 <= b0 >= 40;");
                                    state = States.L_bfrac;
                                    continue;
                                } else if (lambda > a0 * 0.03) {
                                    debugPrintf("  b0 >= a0 > 100; lambda > a0 * 0.03 ");
                                    state = States.L_bfrac;
                                    continue;
                                }

                                /* else if none of the above L180: */
                                w = basym(a0, b0, lambda, eps * 100.0, logP);
                                w1 = logP ? log1Exp(w) : 0.5 - w + 0.5;
                                debugPrintf("  b0 >= a0 > 100; lambda <= a0 * 0.03: *w:= basym(*) =%.15f\n", w);
                                break L_end;

                            } /* else: a, b > 1 */

                            /* EVALUATION OF THE APPROPRIATE ALGORITHM */

                        case L_w_bpser: // was L100
                            w = bpser(a0, b0, x0, eps, logP);
                            w1 = logP ? log1Exp(w) : 0.5 - w + 0.5;
                            debugPrintf(" L_w_bpser: *w := bpser(*) = %.1fg\n", w);
                            break L_end;

                        case L_w1_bpser:  // was L110
                            w1 = bpser(b0, a0, y0, eps, logP);
                            w = logP ? log1Exp(w1) : 0.5 - w1 + 0.5;
                            debugPrintf(" L_w1_bpser: *w1 := bpser(*) = %.15f\n", w1);
                            break L_end;

                        case L_bfrac:
                            w = bfrac(a0, b0, x0, y0, lambda, eps * 15.0, logP);
                            w1 = logP ? log1Exp(w) : 0.5 - w + 0.5;
                            debugPrintf(" L_bfrac: *w := bfrac(*) = %f\n", w);
                            break L_end;

                        case L140:
                            /* b0 := fractional_part( b0 ) in (0, 1] */
                            n = (int) b0;
                            b0 -= n;
                            if (b0 == 0.) {
                                --n;
                                b0 = 1.;
                            }

                            w = bup(b0, a0, y0, x0, n, eps, false);

                            debugPrintf(" L140: *w := bup(b0=%g,..) = %.15f; ", b0, w);
                            if (w < DBL_MIN && logP) { /* do not believe it; try bpser() : */
                                /* revert: */b0 += n;
                                /* which is only valid if b0 <= 1 || b0*x0 <= 0.7 */
                                state = States.L_w_bpser;
                                continue;
                            }
                            if (x0 <= 0.7) {
                                /*
                                 * log_p : TODO: w = bup(.) + bpser(.) -- not so easy to use
                                 * log-scale
                                 */
                                w += bpser(a0, b0, x0, eps, /* log_p = */false);
                                debugPrintf(" x0 <= 0.7: *w := *w + bpser(*) = %.15f\n", w);
                                state = States.L_end_from_w;
                                continue;
                            }
                            /* L150: */
                            if (a0 <= 15.0) {
                                n = 20;
                                w += bup(a0, b0, x0, y0, n, eps, false);
                                debugPrintf("\n a0 <= 15: *w := *w + bup(*) = %.15f;", w);
                                a0 += n;
                            }
                            debugPrintf(" bgrat(*, w=%.15f) ", w);
                            bgrat.w = w;
                            bgrat.bgrat(a0, b0, x0, y0, 15 * eps, false);
                            w = bgrat.w;
                            if (bgrat.ierr != 0) {
                                ierr = 8;
                            }
                            state = States.L_end_from_w;
                            continue;

                            /* TERMINATION OF THE PROCEDURE */

                        case L_end_from_w:
                            if (logP) {
                                w1 = Math.log1p(-w);
                                w = Math.log(w);
                            } else {
                                w1 = 0.5 - w + 0.5;
                            }
                            break L_end;

                        case L_end_from_w1:
                            if (logP) {
                                w = Math.log1p(-w1);
                                w1 = Math.log(w1);
                            } else {
                                w = 0.5 - w1 + 0.5;
                            }
                            break L_end;

                        case L_end_from_w1_log:
                            // *w1 = log(w1) already; w = 1 - w1 ==> log(w) = log(1 - w1) = log(1 -
                            // exp(*w1))
                            if (logP) {
                                w = log1Exp(w1);
                            } else {
                                w = /* 1 - exp(*w1) */-Math.expm1(w1);
                                w1 = Math.exp(w1);
                            }
                            break L_end;

                        default:
                            throw RInternalError.shouldNotReachHere("state: " + state);
                    }/* bratio */
                }
                // unreachable:
                // break;
            }
            // L_end:
            if (doSwap) { /* swap */
                double t = w;
                w = w1;
                w1 = t;
            }
            return result(w, w1, ierr);
        }
    }

    private static final class Bgrat {
        private static final int n_terms_bgrat = 30;

        private int ierr;
        private double w;

        void bgrat(double a, double b, double x, double y, double eps, boolean logW) {
            /*
             * ----------------------------------------------------------------------- Asymptotic
             * Expansion for I_x(a,b) when a is larger than b. Compute w := w + I_x(a,b) It is
             * assumed a >= 15 and b <= 1. eps is the tolerance used. ierr is a variable that
             * reports the status of the results.
             * 
             * if(log_w), *w itself must be in log-space; compute w := w + I_x(a,b) but return *w =
             * log(w): *w := log(exp(*w) + I_x(a,b)) = logspace_add(*w, log( I_x(a,b) ))
             * -----------------------------------------------------------------------
             */

            double[] c = new double[n_terms_bgrat];
            double[] d = new double[n_terms_bgrat];
            double bm1 = b - 0.5 - 0.5;
            double nu = a + bm1 * 0.5;
            // nu = a + (b-1)/2 =: T, in (9.1) of Didonato & Morris(1992), p.362
            double lnx = (y > 0.375) ? Math.log(x) : alnrel(-y);
            double z = -nu * lnx;
            // z =: u in (9.1) of D.&M.(1992)

            if (b * z == 0.0) { // should not happen, but does, e.g.,
                // for pbeta(1e-320, 1e-5, 0.5) i.e., _subnormal_ x,
                // Warning ... bgrat(a=20.5, b=1e-05, x=1, y=9.99989e-321): ..
                RError.warning(RError.SHOW_CALLER, Message.GENERIC, String.format(
                                "bgrat(a=%f, b=%f, x=%f, y=%f): b*z == 0 underflow, hence inaccurate pbeta()",
                                a, b, x, y));
                /* L_Error: THE EXPANSION CANNOT BE COMPUTED */
                ierr = 1;
                return;
            }

            /* COMPUTATION OF THE EXPANSION */
            /*
             * r1 = b * (gam1(b) + 1.0) * exp(b * log(z)),// = b/gamma(b+1) z^b = z^b / gamma(b) set
             * r := exp(-z) * z^b / gamma(b) ; gam1(b) = 1/gamma(b+1) - 1 , b in [-1/2, 3/2]
             */
            // exp(a*lnx) underflows for large (a * lnx); e.g. large a ==> using log_r := log(r):
            // r = r1 * exp(a * lnx) * exp(bm1 * 0.5 * lnx);
            // log(r)=log(b) + log1p(gam1(b)) + b * log(z) + (a * lnx) + (bm1 * 0.5 * lnx),
            double logR = Math.log(b) + log1p(gam1(b)) + b * Math.log(z) + nu * lnx;
            // FIXME work with log_u = log(u) also when log_p=FALSE (??)
            // u is 'factored out' from the expansion {and multiplied back, at the end}:
            double logU = logR - (algdiv(b, a) + b * Math.log(nu)); // algdiv(b,a) =
            // log(gamma(a)/gamma(a+b))
            /* u = (log_p) ? log_r - u : exp(log_r-u); // =: M in (9.2) of {reference above} */
            /* u = algdiv(b, a) + b * log(nu);// algdiv(b,a) = log(gamma(a)/gamma(a+b)) */
            // u = (log_p) ? log_u : exp(log_u); // =: M in (9.2) of {reference above}
            double u = Math.exp(logU);

            if (logU == Double.NEGATIVE_INFINITY) {
                /* L_Error: THE EXPANSION CANNOT BE COMPUTED */ierr = 2;
                return;
            }

            boolean u0 = (u == 0.); // underflow --> do work with log(u) == log_u !
            double l = // := *w/u .. but with care: such that it also works when u underflows to 0:
            logW ? ((w == Double.NEGATIVE_INFINITY) ? 0. : Math.exp(w - logU)) : ((w == 0.) ? 0. : Math.exp(Math.log(w) - logU));

            debugPrintf(" bgrat(a=%f, b=%f, x=%f, *)\n -> u=%f, l='w/u'=%f, ", a, b, x, u, l);

            double qR = gratR(b, z, logR, eps); // = q/r of former grat1(b,z, r, &p, &q)
            double v = 0.25 / (nu * nu);
            double t2 = lnx * 0.25 * lnx;
            double j = qR;
            double sum = j;
            double t = 1.0;
            double cn = 1.0;
            double n2 = 0.;
            for (int n = 1; n <= n_terms_bgrat; ++n) {
                double bp2n = b + n2;
                j = (bp2n * (bp2n + 1.0) * j + (z + bp2n + 1.0) * t) * v;
                n2 += 2.;
                t *= t2;
                cn /= n2 * (n2 + 1.);
                int nm1 = n - 1;
                c[nm1] = cn;
                double s = 0.0;
                if (n > 1) {
                    double coef = b - n;
                    for (int i = 1; i <= nm1; ++i) {
                        s += coef * c[i - 1] * d[nm1 - i];
                        coef += b;
                    }
                }
                d[nm1] = bm1 * cn + s / n;
                double dj = d[nm1] * j;
                sum += dj;
                if (sum <= 0.0) {
                    /* L_Error: THE EXPANSION CANNOT BE COMPUTED */ierr = 3;
                    return;
                }
                if (Math.abs(dj) <= eps * (sum + l)) {
                    break;
                } else if (n == n_terms_bgrat) {
                    // never? ; please notify R-core if seen:
                    RError.warning(RError.SHOW_CALLER, Message.GENERIC, String.format("bgrat(a=%f, b=%f, x=%f,..): did *not* converge; dj=%f, rel.err=%f\n", a, b, x, dj, Math.abs(dj) / (sum + l)));
                }
            }

            /* ADD THE RESULTS TO W */
            ierr = 0;
            if (logW) {
                // *w is in log space already:
                w = logspaceAdd(w, logU + Math.log(sum));
            } else {
                w += (u0 ? Math.exp(logU + Math.log(sum)) : u * sum);
            }
            return;
        } /* bgrat */
    }

    private static double fpser(double a, double b, double x, double eps, boolean logP) {
        /*
         * ----------------------------------------------------------------------- *
         * 
         * EVALUATION OF I (A,B) X
         * 
         * FOR B < MIN(EPS, EPS*A) AND X <= 0.5
         * 
         * -----------------------------------------------------------------------
         */

        double ans;

        /* SET ans := x^a : */
        if (logP) {
            ans = a * log(x);
        } else if (a > eps * 0.001) {
            double t = a * log(x);
            if (t < exparg(1)) { /* exp(t) would underflow */
                return 0.0;
            }
            ans = exp(t);
        } else {
            ans = 1.;
        }

        /* NOTE THAT 1/B(A,B) = B */

        if (logP) {
            ans += log(b) - log(a);
        } else {
            ans *= b / a;
        }

        double tol = eps / a;
        double an = a + 1.0;
        double t = x;
        double s = t / an;
        double c;
        do {
            an += 1.0;
            t = x * t;
            c = t / an;
            s += c;
        } while (fabs(c) > tol);

        if (logP) {
            ans += log1p(a * s);
        } else {
            ans *= a * s + 1.0;
        }
        return ans;
    } /* fpser */

    static double apser(double a, double b, double x, double eps) {
        /*
         * ----------------------------------------------------------------------- apser() yields
         * the incomplete beta ratio I_{1-x}(b,a) for a <= min(eps,eps*b), b*x <= 1, and x <= 0.5,
         * i.e., a is very small. Use only if above inequalities are satisfied.
         * -----------------------------------------------------------------------
         */

        double g = .577215664901533;

        double bx = b * x;

        double t = x - bx;
        double c;
        if (b * eps <= 0.02) {
            c = log(x) + psi(b) + g + t;
        } else {
            c = log(bx) + g + t;
        }

        double tol = eps * 5.0 * fabs(c);
        double j = 1.;
        double s = 0.;
        double aj;
        do {
            j += 1.0;
            t *= x - bx / j;
            aj = t / j;
            s += aj;
        } while (fabs(aj) > tol);

        return -a * (c + s);
    } /* apser */

    static double bpser(double a, double b, double x, double eps, boolean logP) {
        /*
         * ----------------------------------------------------------------------- Power SERies
         * expansion for evaluating I_x(a,b) when b <= 1 or b*x <= 0.7. eps is the tolerance used.
         * -----------------------------------------------------------------------
         */

        if (x == 0.) {
            return DPQ.d0(logP);
        }
        /* ----------------------------------------------------------------------- */
        /* compute the factor x^a/(a*Beta(a,b)) */
        /* ----------------------------------------------------------------------- */
        double ans;
        double a0 = min(a, b);
        if (a0 >= 1.0) { /* ------ 1 <= a0 <= b0 ------ */
            double z = a * log(x) - betaln(a, b);
            ans = logP ? z - log(a) : exp(z) / a;
        } else {
            double b0 = max(a, b);

            if (b0 < 8.0) {

                if (b0 <= 1.0) { /* ------ a0 < 1 and b0 <= 1 ------ */

                    if (logP) {
                        ans = a * log(x);
                    } else {
                        ans = pow(x, a);
                        if (ans == 0.) {
                            /* once underflow, always underflow .. */
                            return ans;
                        }
                    }
                    double apb = a + b;
                    double z;
                    if (apb > 1.0) {
                        double u = a + b - 1.;
                        z = (gam1(u) + 1.0) / apb;
                    } else {
                        z = gam1(apb) + 1.0;
                    }
                    double c = (gam1(a) + 1.0) * (gam1(b) + 1.0) / z;

                    if (logP) {
                        /* FIXME ? -- improve quite a bit for c ~= 1 */
                        ans += log(c * (b / apb));
                    } else {
                        ans *= c * (b / apb);
                    }

                } else { /* ------ a0 < 1 < b0 < 8 ------ */

                    double u = gamln1(a0);
                    int m = (int) (b0 - 1.0);
                    if (m >= 1) {
                        double c = 1.0;
                        for (int i = 1; i <= m; ++i) {
                            b0 += -1.0;
                            c *= b0 / (a0 + b0);
                        }
                        u += log(c);
                    }

                    double z = a * log(x) - u;
                    b0 += -1.0; // => b0 in (0, 7)
                    double apb = a0 + b0;
                    double t;
                    if (apb > 1.0) {
                        u = a0 + b0 - 1.;
                        t = (gam1(u) + 1.0) / apb;
                    } else {
                        t = gam1(apb) + 1.0;
                    }

                    if (logP) {
                        /* FIXME? potential for improving log(t) */
                        ans = z + log(a0 / a) + log1p(gam1(b0)) - log(t);
                    } else {
                        ans = exp(z) * (a0 / a) * (gam1(b0) + 1.0) / t;
                    }
                }

            } else { /* ------ a0 < 1 < 8 <= b0 ------ */

                double u = gamln1(a0) + algdiv(a0, b0);
                double z = a * log(x) - u;

                if (logP) {
                    ans = z + log(a0 / a);
                } else {
                    ans = a0 / a * exp(z);
                }
            }
        }
        debugPrintf(" bpser(a=%f, b=%f, x=%f, log=%b): prelim.ans = %.14f;\n", a, b, x, logP, ans);
        if (ans == DPQ.d0(logP) || (!logP && a <= eps * 0.1)) {
            return ans;
        }

        /* ----------------------------------------------------------------------- */
        /* COMPUTE THE SERIES */
        /* ----------------------------------------------------------------------- */
        double sum = 0.;
        double n = 0.;
        double c = 1.;
        double tol = eps / a;

        double w;
        do {
            n += 1.;
            c *= (0.5 - b / n + 0.5) * x;
            w = c / (a + n);
            sum += w;
        } while (n < 1e7 && fabs(w) > tol);
        if (fabs(w) > tol) { // the series did not converge (in time)
            // warn only when the result seems to matter:
            if ((logP && !(a * sum > -1. && fabs(log1p(a * sum)) < eps * fabs(ans))) || (!logP && fabs(a * sum + 1) != 1.)) {
                emitWarning(" bpser(a=%f, b=%f, x=%f,...) did not converge (n=1e7, |w|/tol=%f > 1; A=%f)", a, b, x, fabs(w) / tol, ans);
            }
        }
        debugPrintf("  -> n=%d iterations, |w|=%f %s %f=tol:=eps/a ==> a*sum=%f\n", (int) n, fabs(w), (fabs(w) > tol) ? ">!!>" : "<=", tol, a * sum);
        if (logP) {
            if (a * sum > -1.0) {
                ans += log1p(a * sum);
            } else {
                ans = ML_NEGINF;
            }
        } else {
            ans *= a * sum + 1.0;
        }
        return ans;
    } /* bpser */

    static double bup(double a, double b, double x, double y, int n, double eps, boolean giveLog) {
        /* ----------------------------------------------------------------------- */
        /* EVALUATION OF I_x(A,B) - I_x(A+N,B) WHERE N IS A POSITIVE INT. */
        /* EPS IS THE TOLERANCE USED. */
        /* ----------------------------------------------------------------------- */

        // Obtain the scaling factor exp(-mu) and exp(mu)*(x^a * y^b / beta(a,b))/a

        double apb = a + b;
        double ap1 = a + 1.0;
        int mu;
        double d;
        if (n > 1 && a >= 1. && apb >= ap1 * 1.1) {
            mu = (int) fabs(exparg(1));
            int k = (int) exparg(0);
            if (mu > k) {
                mu = k;
            }
            double t = mu;
            d = exp(-t);
        } else {
            mu = 0;
            d = 1.0;
        }

        /* L10: */
        double retVal = giveLog ? brcmp1(mu, a, b, x, y, true) - log(a) : brcmp1(mu, a, b, x, y, false) / a;
        if (n == 1 || (giveLog && retVal == ML_NEGINF) || (!giveLog && retVal == 0.)) {
            return retVal;
        }

        int nm1 = n - 1;
        double w = d;

        /* LET K BE THE INDEX OF THE MAXIMUM TERM */

        boolean doL40 = false;
        int k = 0;
        do {
            if (b <= 1.0) {
                doL40 = true;
                break;
            }
            if (y > 1e-4) {
                double r = (b - 1.0) * x / y - a;
                if (r < 1.0) {
                    doL40 = true;
                    break;
                }
                k = nm1;
                double t = nm1;
                if (r < t) {
                    k = (int) r;
                }
            } else {
                k = nm1;
            }

            /* ADD THE INCREASING TERMS OF THE SERIES */

            /* L30: */
            for (int i = 1; i <= k; ++i) {
                double l = i - 1;
                d = (apb + l) / (ap1 + l) * x * d;
                w += d;
                /* L31: */
            }
            if (k != nm1) {
                doL40 = true;
                break;
            }
        } while (false);
        /* ADD THE REMAINING TERMS OF THE SERIES */
        if (doL40) {
            for (int i = k + 1; i <= nm1; ++i) {
                double l = i - 1;
                d = (apb + l) / (ap1 + l) * x * d;
                w += d;
                if (d <= eps * w) {
                    /* relativ convergence (eps) */
                    break;
                }
            }
        }

        // L50: TERMINATE THE PROCEDURE
        if (giveLog) {
            retVal += log(w);
        } else {
            retVal *= w;
        }

        return retVal;
    } /* bup */

    static double bfrac(double a, double b, double x, double y, double lambda, double eps, boolean logP) {
        /*
         * ----------------------------------------------------------------------- Continued
         * fraction expansion for I_x(a,b) when a, b > 1. It is assumed that lambda = (a + b)*y - b.
         * -----------------------------------------------------------------------
         */

        double brc = brcomp(a, b, x, y, logP);

        if (!logP && brc == 0.) {
            /* already underflowed to 0 */
            return 0.;
        }

        double c = lambda + 1.0;
        double c0 = b / a;
        double c1 = 1.0 / a + 1.0;
        double yp1 = y + 1.0;

        double n = 0.0;
        double p = 1.0;
        double s = a + 1.0;
        double an = 0.0;
        double bn = 1.0;
        double anp1 = 1.0;
        double bnp1 = c / c1;
        double r = c1 / c;

        /* CONTINUED FRACTION CALCULATION */

        do {
            n += 1.0;
            double t = n / a;
            double w = n * (b - n) * x;
            double e = a / s;
            double alpha = p * (p + c0) * e * e * (w * x);
            e = (t + 1.0) / (c1 + t + t);
            double beta = n + w / s + e * (c + n * yp1);
            p = t + 1.0;
            s += 2.0;

            /* update an, bn, anp1, and bnp1 */

            t = alpha * an + beta * anp1;
            an = anp1;
            anp1 = t;
            t = alpha * bn + beta * bnp1;
            bn = bnp1;
            bnp1 = t;

            double r0 = r;
            r = anp1 / bnp1;
            if (fabs(r - r0) <= eps * r) {
                break;
            }

            /* rescale an, bn, anp1, and bnp1 */

            an /= bnp1;
            bn /= bnp1;
            anp1 = r;
            bnp1 = 1.0;
        } while (true);

        return (logP ? brc + log(r) : brc * r);
    } /* bfrac */

    static double brcomp(double a, double b, double x, double y, boolean logP) {
        /*
         * ----------------------------------------------------------------------- Evaluation of x^a
         * * y^b / Beta(a,b) -----------------------------------------------------------------------
         */

        /* R has M_1_SQRT_2PI , and M_LN_SQRT_2PI = ln(sqrt(2*pi)) = 0.918938.. */

        if (x == 0.0 || y == 0.0) {
            return DPQ.d0(logP);
        }
        double a0 = min(a, b);
        if (a0 < 8.0) {
            double lnx;
            double lny;
            if (x <= .375) {
                lnx = log(x);
                lny = alnrel(-x);
            } else {
                if (y > .375) {
                    lnx = log(x);
                    lny = log(y);
                } else {
                    lnx = alnrel(-y);
                    lny = log(y);
                }
            }

            double z = a * lnx + b * lny;
            if (a0 >= 1.) {
                z -= betaln(a, b);
                return DPQ.dExp(z, logP);
            }

            /* ----------------------------------------------------------------------- */
            /* PROCEDURE FOR a < 1 OR b < 1 */
            /* ----------------------------------------------------------------------- */

            double b0 = max(a, b);
            if (b0 >= 8.0) { /* L80: */
                double u = gamln1(a0) + algdiv(a0, b0);

                return (logP ? log(a0) + (z - u) : a0 * exp(z - u));
            }
            /* else : */

            if (b0 <= 1.0) { /* algorithm for max(a,b) = b0 <= 1 */

                double eZ = DPQ.dExp(z, logP);

                if (!logP && eZ == 0.0) {
                    /* exp() underflow */
                    return 0.;
                }

                double apb = a + b;
                if (apb > 1.0) {
                    double u = a + b - 1.;
                    z = (gam1(u) + 1.0) / apb;
                } else {
                    z = gam1(apb) + 1.0;
                }

                double c = (gam1(a) + 1.0) * (gam1(b) + 1.0) / z;
                /* FIXME? log(a0*c)= log(a0)+ log(c) and that is improvable */
                return (logP ? eZ + log(a0 * c) - log1p(a0 / b0) : eZ * (a0 * c) / (a0 / b0 + 1.0));
            }

            /* else : ALGORITHM FOR 1 < b0 < 8 */

            double u = gamln1(a0);
            int n = (int) (b0 - 1.0);
            if (n >= 1) {
                double c = 1.0;
                for (int i = 1; i <= n; ++i) {
                    b0 += -1.0;
                    c *= b0 / (a0 + b0);
                }
                u = log(c) + u;
            }
            z -= u;
            b0 += -1.0;
            double apb = a0 + b0;
            double t;
            if (apb > 1.0) {
                u = a0 + b0 - 1.;
                t = (gam1(u) + 1.0) / apb;
            } else {
                t = gam1(apb) + 1.0;
            }

            return (logP ? log(a0) + z + log1p(gam1(b0)) - log(t) : a0 * exp(z) * (gam1(b0) + 1.0) / t);

        } else {
            /* ----------------------------------------------------------------------- */
            /* PROCEDURE FOR A >= 8 AND B >= 8 */
            /* ----------------------------------------------------------------------- */
            double h;
            double x0;
            double y0;
            double lambda;
            if (a <= b) {
                h = a / b;
                x0 = h / (h + 1.0);
                y0 = 1.0 / (h + 1.0);
                lambda = a - (a + b) * x;
            } else {
                h = b / a;
                x0 = 1.0 / (h + 1.0);
                y0 = h / (h + 1.0);
                lambda = (a + b) * y - b;
            }

            double e = -lambda / a;
            double u;
            if (fabs(e) > .6) {
                u = e - log(x / x0);
            } else {
                u = rlog1(e);
            }

            e = lambda / b;
            double v;
            if (fabs(e) <= .6) {
                v = rlog1(e);
            } else {
                v = e - log(y / y0);
            }

            double z = logP ? -(a * u + b * v) : exp(-(a * u + b * v));

            return (logP ? -M_LN_SQRT_2PI + .5 * log(b * x0) + z - bcorr(a, b) : INV_SQRT_2_PI * sqrt(b * x0) * z * exp(-bcorr(a, b)));
        }
    } /* brcomp */

    // called only once from bup(), as r = brcmp1(mu, a, b, x, y, false) / a;
    // -----
    static double brcmp1(int mu, double a, double b, double x, double y, boolean giveLog) {
        /*
         * ----------------------------------------------------------------------- Evaluation of
         * exp(mu) * x^a * y^b / beta(a,b)
         * -----------------------------------------------------------------------
         */

        /* R has M_1_SQRT_2PI */

        /* Local variables */

        double a0 = min(a, b);
        if (a0 < 8.0) {
            double lnx;
            double lny;
            if (x <= .375) {
                lnx = log(x);
                lny = alnrel(-x);
            } else if (y > .375) {
                // L11:
                lnx = log(x);
                lny = log(y);
            } else {
                lnx = alnrel(-y);
                lny = log(y);
            }

            // L20:
            double z = a * lnx + b * lny;
            if (a0 >= 1.0) {
                z -= betaln(a, b);
                return esum(mu, z, giveLog);
            }
            // else :
            /* ----------------------------------------------------------------------- */
            /* PROCEDURE FOR A < 1 OR B < 1 */
            /* ----------------------------------------------------------------------- */
            // L30:
            double b0 = max(a, b);
            if (b0 >= 8.0) {
                /* L80: ALGORITHM FOR b0 >= 8 */
                double u = gamln1(a0) + algdiv(a0, b0);
                debugPrintf(" brcmp1(mu,a,b,*): a0 < 1, b0 >= 8;  z=%.15f\n", z);
                return giveLog ? log(a0) + esum(mu, z - u, true) : a0 * esum(mu, z - u, false);

            } else if (b0 <= 1.0) {
                // a0 < 1, b0 <= 1
                double ans = esum(mu, z, giveLog);
                if (ans == (giveLog ? ML_NEGINF : 0.)) {
                    return ans;
                }

                double apb = a + b;
                if (apb > 1.0) {
                    // L40:
                    double u = a + b - 1.;
                    z = (gam1(u) + 1.0) / apb;
                } else {
                    z = gam1(apb) + 1.0;
                }
                // L50:
                double c = giveLog ? log1p(gam1(a)) + log1p(gam1(b)) - log(z) : (gam1(a) + 1.0) * (gam1(b) + 1.0) / z;
                debugPrintf(" brcmp1(mu,a,b,*): a0 < 1, b0 <= 1;  c=%.15f\n", c);
                return giveLog ? ans + log(a0) + c - log1p(a0 / b0) : ans * (a0 * c) / (a0 / b0 + 1.0);
            }
            // else: algorithm for a0 < 1 < b0 < 8
            // L60:
            double u = gamln1(a0);
            int n = (int) (b0 - 1.0);
            if (n >= 1) {
                double c = 1.0;
                for (int i = 1; i <= n; ++i) {
                    b0 += -1.0;
                    c *= b0 / (a0 + b0);
                    /* L61: */
                }
                u += log(c); // TODO?: log(c) = log( prod(...) ) = sum( log(...) )
            }
            // L70:
            z -= u;
            b0 += -1.0;
            double apb = a0 + b0;
            double t;
            if (apb > 1.) {
                // L71:
                t = (gam1(apb - 1.) + 1.0) / apb;
            } else {
                t = gam1(apb) + 1.0;
            }
            debugPrintf(" brcmp1(mu,a,b,*): a0 < 1 < b0 < 8;  t=%.15f\n", t);
            // L72:
            return giveLog ? log(a0) + esum(mu, z, true) + log1p(gam1(b0)) - log(t) : a0 * esum(mu, z, false) * (gam1(b0) + 1.0) / t;

        } else {

            /* ----------------------------------------------------------------------- */
            /* PROCEDURE FOR A >= 8 AND B >= 8 */
            /* ----------------------------------------------------------------------- */
            // L100:
            double h;
            double x0;
            double y0;
            double lambda;
            if (a > b) {
                // L101:
                h = b / a;
                x0 = 1.0 / (h + 1.0); // => lx0 := log(x0) = 0 - log1p(h)
                y0 = h / (h + 1.0);
                lambda = (a + b) * y - b;
            } else {
                h = a / b;
                x0 = h / (h + 1.0);  // => lx0 := log(x0) = - log1p(1/h)
                y0 = 1.0 / (h + 1.0);
                lambda = a - (a + b) * x;
            }
            double lx0 = -log1p(b / a); // in both cases

            debugPrintf(" brcmp1(mu,a,b,*): a,b >= 8;  x0=%.15f, lx0=log(x0)=%.15f\n", x0, lx0);
            // L110:
            double e = -lambda / a;
            double u;
            if (fabs(e) > 0.6) {
                // L111:
                u = e - log(x / x0);
            } else {
                u = rlog1(e);
            }

            // L120:
            e = lambda / b;
            double v;
            if (fabs(e) > 0.6) {
                // L121:
                v = e - log(y / y0);
            } else {
                v = rlog1(e);
            }

            // L130:
            double z = esum(mu, -(a * u + b * v), giveLog);
            return giveLog ? log(INV_SQRT_2_PI) + (log(b) + lx0) / 2. + z - bcorr(a, b) : INV_SQRT_2_PI * sqrt(b * x0) * z * exp(-bcorr(a, b));
        }

    } /* brcmp1 */

    // called only from bgrat() , as q_r = grat_r(b, z, log_r, eps) :
    static double gratR(double a, double x, double logR, double eps) {
        /*
         * ----------------------------------------------------------------------- Scaled complement
         * of incomplete gamma ratio function grat_r(a,x,r) := Q(a,x) / r where Q(a,x) = pgamma(x,a,
         * lower.tail=false) and r = e^(-x)* x^a / Gamma(a) == exp(log_r)
         * 
         * It is assumed that a <= 1. eps is the tolerance to be used.
         * -----------------------------------------------------------------------
         */

        if (a * x == 0.0) { /* L130: */
            if (x <= a) {
                /* L100: */
                return exp(-logR);
            } else {
                /* L110: */
                return 0.;
            }
        } else if (a == 0.5) { // e.g. when called from pt()
            /* L120: */
            if (x < 0.25) {
                double p = erf(sqrt(x));
                debugPrintf(" grat_r(a=%f, x=%f ..)): a=1/2 --> p=erf__(.)= %f\n", a, x, p);
                return (0.5 - p + 0.5) * exp(-logR);

            } else { // 2013-02-27: improvement for "large" x: direct computation of q/r:
                double sx = sqrt(x);
                double qR = erfc1(1, sx) / sx * M_SQRT_PI;
                debugPrintf(" grat_r(a=%f, x=%f ..)): a=1/2 --> q_r=erfc1(..)/r= %f\n", a, x, qR);
                return qR;
            }
        } else if (x < 1.1) { /* L10: Taylor series for P(a,x)/x^a */

            double an = 3.;
            double c = x;
            double sum = x / (a + 3.0);
            double tol = eps * 0.1 / (a + 1.0);
            double t;
            do {
                an += 1.;
                c *= -(x / an);
                t = c / (a + an);
                sum += t;
            } while (fabs(t) > tol);

            debugPrintf(" grat_r(a=%f, x=%f, log_r=%f): sum=%f; Taylor w/ %.0f terms", a, x, logR, sum, an - 3.);
            double j = a * x * ((sum / 6. - 0.5 / (a + 2.)) * x + 1. / (a + 1.));
            double z = a * log(x);
            double h = gam1(a);
            double g = h + 1.0;

            if ((x >= 0.25 && (a < x / 2.59)) || (z > -0.13394)) {
                // L40:
                double l = rexpm1(z);
                double q = ((l + 0.5 + 0.5) * j - l) * g - h;
                if (q <= 0.0) {
                    debugPrintf(" => q_r= 0.\n");
                    /* L110: */
                    return 0.;
                } else {
                    debugPrintf(" => q_r=%.15f\n", q * exp(-logR));
                    return q * exp(-logR);
                }

            } else {
                double p = exp(z) * g * (0.5 - j + 0.5);
                debugPrintf(" => q_r=%.15f\n", (0.5 - p + 0.5) * exp(-logR));
                return /* q/r = */(0.5 - p + 0.5) * exp(-logR);
            }

        } else {
            /* L50: ---- (x >= 1.1) ---- Continued Fraction Expansion */

            double a2n1 = 1.0;
            double a2n = 1.0;
            double b2n1 = x;
            double b2n = x + (1.0 - a);
            double c = 1.;
            double am0;
            double an0;

            do {
                a2n1 = x * a2n + c * a2n1;
                b2n1 = x * b2n + c * b2n1;
                am0 = a2n1 / b2n1;
                c += 1.;
                double cA = c - a;
                a2n = a2n1 + cA * a2n;
                b2n = b2n1 + cA * b2n;
                an0 = a2n / b2n;
            } while (fabs(an0 - am0) >= eps * an0);

            debugPrintf(" grat_r(a=%f, x=%f, log_r=%f): Cont.frac. %.0f terms => q_r=%.15f\n", a, x, logR, c - 1., an0);
            return /* q/r = (r * an0)/r = */an0;
        }
    } /* grat_r */

    private static final int num_IT = 20;

    static double basym(double a, double b, double lambda, double eps, boolean logP) {
        /* ----------------------------------------------------------------------- */
        /* ASYMPTOTIC EXPANSION FOR I_x(A,B) FOR LARGE A AND B. */
        /* LAMBDA = (A + B)*Y - B AND EPS IS THE TOLERANCE USED. */
        /* IT IS ASSUMED THAT LAMBDA IS NONNEGATIVE AND THAT */
        /* A AND B ARE GREATER THAN OR EQUAL TO 15. */
        /* ----------------------------------------------------------------------- */

        /* ------------------------ */
        /* ****** NUM IS THE MAXIMUM VALUE THAT N CAN TAKE IN THE DO LOOP */
        /* ENDING AT STATEMENT 50. IT IS REQUIRED THAT NUM BE EVEN. */
        /* THE ARRAYS A0, B0, C, D HAVE DIMENSION NUM + 1. */

        double e0 = 1.12837916709551; // e0 == 2/sqrt(pi)
        double e1 = .353553390593274; // e1 == 2^(-3/2)
        double lnE0 = 0.120782237635245; // == ln(e0)

        double[] a0 = new double[num_IT + 1];
        double[] b0 = new double[num_IT + 1];
        double[] c = new double[num_IT + 1];
        double[] d = new double[num_IT + 1];

        double f = a * rlog1(-lambda / a) + b * rlog1(lambda / b);
        double t;
        if (logP) {
            t = -f;
        } else {
            t = exp(-f);
            if (t == 0.0) {
                return 0; /* once underflow, always underflow .. */
            }
        }
        double z0 = sqrt(f);
        double z = z0 / e1 * 0.5;
        double z2 = f + f;

        double h;
        double r0;
        double r1;
        double w0;

        if (a < b) {
            h = a / b;
            r0 = 1.0 / (h + 1.0);
            r1 = (b - a) / b;
            w0 = 1.0 / sqrt(a * (h + 1.0));
        } else {
            h = b / a;
            r0 = 1.0 / (h + 1.0);
            r1 = (b - a) / a;
            w0 = 1.0 / sqrt(b * (h + 1.0));
        }

        a0[0] = r1 * .66666666666666663;
        c[0] = a0[0] * -0.5;
        d[0] = -c[0];
        double j0 = 0.5 / e0 * erfc1(1, z0);
        double j1 = e1;
        double sum = j0 + d[0] * w0 * j1;

        double s = 1.0;
        double h2 = h * h;
        double hn = 1.0;
        double w = w0;
        double znm1 = z;
        double zn = z2;
        for (int n = 2; n <= num_IT; n += 2) {
            hn *= h2;
            a0[n - 1] = r0 * 2.0 * (h * hn + 1.0) / (n + 2.0);
            int np1 = n + 1;
            s += hn;
            a0[np1 - 1] = r1 * 2.0 * s / (n + 3.0);

            for (int i = n; i <= np1; ++i) {
                double r = (i + 1.0) * -0.5;
                b0[0] = r * a0[0];
                for (int m = 2; m <= i; ++m) {
                    double bsum = 0.0;
                    for (int j = 1; j <= m - 1; ++j) {
                        int mmj = m - j;
                        bsum += (j * r - mmj) * a0[j - 1] * b0[mmj - 1];
                    }
                    b0[m - 1] = r * a0[m - 1] + bsum / m;
                }
                c[i - 1] = b0[i - 1] / (i + 1.0);

                double dsum = 0.0;
                for (int j = 1; j <= i - 1; ++j) {
                    dsum += d[i - j - 1] * c[j - 1];
                }
                d[i - 1] = -(dsum + c[i - 1]);
            }

            j0 = e1 * znm1 + (n - 1.0) * j0;
            j1 = e1 * zn + n * j1;
            znm1 = z2 * znm1;
            zn = z2 * zn;
            w *= w0;
            double t0 = d[n - 1] * w * j0;
            w *= w0;
            double t1 = d[np1 - 1] * w * j1;
            sum += t0 + t1;
            if (fabs(t0) + fabs(t1) <= eps * sum) {
                break;
            }
        }

        if (logP) {
            return lnE0 + t - bcorr(a, b) + log(sum);
        } else {
            double u = exp(-bcorr(a, b));
            return e0 * t * u * sum;
        }

    } /* basym_ */

    static double exparg(int l) {
        /* -------------------------------------------------------------------- */
        /*
         * IF L = 0 THEN EXPARG(L) = THE LARGEST POSITIVE W FOR WHICH EXP(W) CAN BE COMPUTED. ==>
         * exparg(0) = 709.7827 nowadays.
         */

        /*
         * IF L IS NONZERO THEN EXPARG(L) = THE LARGEST NEGATIVE W FOR WHICH THE COMPUTED VALUE OF
         * EXP(W) IS NONZERO. ==> exparg(1) = -708.3964 nowadays.
         */

        /* Note... only an approximate value for exparg(L) is needed. */
        /* -------------------------------------------------------------------- */

        double lnb = .69314718055995;
        int m = (l == 0) ? MathInit.i1mach(16) : MathInit.i1mach(15) - 1;

        return m * lnb * .99999;
    } /* exparg */

    static double esum(int mu, double x, boolean giveLog) {
        /* ----------------------------------------------------------------------- */
        /* EVALUATION OF EXP(MU + X) */
        /* ----------------------------------------------------------------------- */

        if (giveLog) {
            return x + mu;
        }

        // else :
        double w;
        if (x > 0.0) { /* L10: */
            if (mu > 0) {
                return exp(mu) * exp(x);
            }
            w = mu + x;
            if (w < 0.0) {
                return exp(mu) * exp(x);
            }
        } else { /* x <= 0 */
            if (mu < 0) {
                return exp(mu) * exp(x);
            }
            w = mu + x;
            if (w > 0.0) {
                return exp(mu) * exp(x);
            }
        }
        return exp(w);
    } /* esum */

    private static double rexpm1(double x) {
        /* ----------------------------------------------------------------------- */
        /* EVALUATION OF THE FUNCTION EXP(X) - 1 */
        /* ----------------------------------------------------------------------- */

        double p1 = 9.14041914819518e-10;
        double p2 = .0238082361044469;
        double q1 = -.499999999085958;
        double q2 = .107141568980644;
        double q3 = -.0119041179760821;
        double q4 = 5.95130811860248e-4;

        if (fabs(x) <= 0.15) {
            return x * (((p2 * x + p1) * x + 1.0) / ((((q4 * x + q3) * x + q2) * x + q1) * x + 1.0));
        } else { /* |x| > 0.15 : */
            double w = exp(x);
            if (x > 0.0) {
                return w * (0.5 - 1.0 / w + 0.5);
            } else {
                return w - 0.5 - 0.5;
            }
        }

    } /* rexpm1 */

    static double alnrel(double a) {
        /*
         * ----------------------------------------------------------------------- Evaluation of the
         * function ln(1 + a)
         * -----------------------------------------------------------------------
         */

        if (fabs(a) > 0.375) {
            return log(1. + a);
        }
        // else : |a| <= 0.375
        double p1 = -1.29418923021993;
        double p2 = .405303492862024;
        double p3 = -.0178874546012214;
        double q1 = -1.62752256355323;
        double q2 = .747811014037616;
        double q3 = -.0845104217945565;
        double t = a / (a + 2.0);
        double t2 = t * t;
        double w = (((p3 * t2 + p2) * t2 + p1) * t2 + 1.) / (((q3 * t2 + q2) * t2 + q1) * t2 + 1.);
        return t * 2.0 * w;

    } /* alnrel */

    static double rlog1(double x) {
        /*
         * ----------------------------------------------------------------------- Evaluation of the
         * function x - ln(1 + x)
         * -----------------------------------------------------------------------
         */

        double a = .0566749439387324;
        double b = .0456512608815524;
        double p0 = .333333333333333;
        double p1 = -.224696413112536;
        double p2 = .00620886815375787;
        double q1 = -1.27408923933623;
        double q2 = .354508718369557;

        if (x < -0.39 || x > 0.57) { /* direct evaluation */
            double w = x + 0.5 + 0.5;
            return x - log(w);
        }
        double h;
        double w1;
        /* else */
        if (x < -0.18) { /* L10: */
            h = x + .3;
            h /= .7;
            w1 = a - h * .3;
        } else if (x > 0.18) { /* L20: */
            h = x * .75 - .25;
            w1 = b + h / 3.0;
        } else { /* Argument Reduction */
            h = x;
            w1 = 0.0;
        }

        /* L30: Series Expansion */

        double r = h / (h + 2.0);
        double t = r * r;
        double w = ((p2 * t + p1) * t + p0) / ((q2 * t + q1) * t + 1.0);
        return t * 2.0 * (1.0 / (1.0 - r) - r * w) + w1;

    } /* rlog1 */

    static double erf(double x) {
        /*
         * ----------------------------------------------------------------------- EVALUATION OF THE
         * REAL ERROR FUNCTION
         * -----------------------------------------------------------------------
         */

        /* Initialized data */

        double c = .564189583547756;
        double[] a = {7.7105849500132e-5, -.00133733772997339, .0323076579225834, .0479137145607681, .128379167095513};
        double[] b = {.00301048631703895, .0538971687740286, .375795757275549};
        double[] p = {-1.36864857382717e-7, .564195517478974, 7.21175825088309, 43.1622272220567, 152.98928504694, 339.320816734344, 451.918953711873, 300.459261020162};
        double[] q = {1., 12.7827273196294, 77.0001529352295, 277.585444743988, 638.980264465631, 931.35409485061, 790.950925327898, 300.459260956983};
        double[] r = {2.10144126479064, 26.2370141675169, 21.3688200555087, 4.6580782871847, .282094791773523};
        double[] s = {94.153775055546, 187.11481179959, 99.0191814623914, 18.0124575948747};

        /* System generated locals */

        double ax = fabs(x);
        if (ax <= 0.5) {
            double t = x * x;
            double top = (((a[0] * t + a[1]) * t + a[2]) * t + a[3]) * t + a[4] + 1.0;
            double bot = ((b[0] * t + b[1]) * t + b[2]) * t + 1.0;

            return x * (top / bot);
        }
        /* else: ax > 0.5 */

        if (ax <= 4.) { /* ax in (0.5, 4] */
            double top = ((((((p[0] * ax + p[1]) * ax + p[2]) * ax + p[3]) * ax + p[4]) * ax + p[5]) * ax + p[6]) * ax + p[7];
            double bot = ((((((q[0] * ax + q[1]) * ax + q[2]) * ax + q[3]) * ax + q[4]) * ax + q[5]) * ax + q[6]) * ax + q[7];
            double retVal = 0.5 - exp(-x * x) * top / bot + 0.5;
            if (x < 0.0) {
                retVal = -retVal;
            }
            return retVal;
        }

        /* else: ax > 4 */

        if (ax >= 5.8) {
            return x > 0 ? 1 : -1;
        }
        double x2 = x * x;
        double t = 1.0 / x2;
        double top = (((r[0] * t + r[1]) * t + r[2]) * t + r[3]) * t + r[4];
        double bot = (((s[0] * t + s[1]) * t + s[2]) * t + s[3]) * t + 1.0;
        t = (c - top / (x2 * bot)) / ax;
        double retVal = 0.5 - exp(-x2) * t + 0.5;
        if (x < 0.0) {
            retVal = -retVal;
        }
        return retVal;

    } /* erf */

    static double erfc1(int ind, double x) {
        /* ----------------------------------------------------------------------- */
        /* EVALUATION OF THE COMPLEMENTARY ERROR FUNCTION */

        /* ERFC1(IND,X) = ERFC(X) IF IND = 0 */
        /* ERFC1(IND,X) = EXP(X*X)*ERFC(X) OTHERWISE */
        /* ----------------------------------------------------------------------- */

        /* Initialized data */

        double c = .564189583547756;
        double[] a = {7.7105849500132e-5, -.00133733772997339, .0323076579225834, .0479137145607681, .128379167095513};
        double[] b = {.00301048631703895, .0538971687740286, .375795757275549};
        double[] p = {-1.36864857382717e-7, .564195517478974, 7.21175825088309, 43.1622272220567, 152.98928504694, 339.320816734344, 451.918953711873, 300.459261020162};
        double[] q = {1., 12.7827273196294, 77.0001529352295, 277.585444743988, 638.980264465631, 931.35409485061, 790.950925327898, 300.459260956983};
        double[] r = {2.10144126479064, 26.2370141675169, 21.3688200555087, 4.6580782871847, .282094791773523};
        double[] s = {94.153775055546, 187.11481179959, 99.0191814623914, 18.0124575948747};

        double ax = fabs(x);
        // |X| <= 0.5 */
        if (ax <= 0.5) {
            double t = x * x;
            double top = (((a[0] * t + a[1]) * t + a[2]) * t + a[3]) * t + a[4] + 1.0;
            double bot = ((b[0] * t + b[1]) * t + b[2]) * t + 1.0;
            double retVal = 0.5 - x * (top / bot) + 0.5;
            if (ind != 0) {
                retVal = exp(t) * retVal;
            }
            return retVal;
        }
        double retVal;
        // else (L10:): 0.5 < |X| <= 4
        if (ax <= 4.0) {
            double top = ((((((p[0] * ax + p[1]) * ax + p[2]) * ax + p[3]) * ax + p[4]) * ax + p[5]) * ax + p[6]) * ax + p[7];
            double bot = ((((((q[0] * ax + q[1]) * ax + q[2]) * ax + q[3]) * ax + q[4]) * ax + q[5]) * ax + q[6]) * ax + q[7];
            retVal = top / bot;
        } else { // |X| > 4
            // L20:
            if (x <= -5.6) {
                // L50: LIMIT VALUE FOR "LARGE" NEGATIVE X
                retVal = 2.0;
                if (ind != 0) {
                    retVal = exp(x * x) * 2.0;
                }
                return retVal;
            }
            if (ind == 0 && (x > 100.0 || x * x > -exparg(1))) {
                // LIMIT VALUE FOR LARGE POSITIVE X WHEN IND = 0
                // L60:
                return 0.0;
            }

            // L30:
            double t = 1. / (x * x);
            double top = (((r[0] * t + r[1]) * t + r[2]) * t + r[3]) * t + r[4];
            double bot = (((s[0] * t + s[1]) * t + s[2]) * t + s[3]) * t + 1.0;
            retVal = (c - t * top / bot) / ax;
        }

        // L40: FINAL ASSEMBLY
        if (ind != 0) {
            if (x < 0.0) {
                retVal = exp(x * x) * 2.0 - retVal;
            }
        } else {
            // L41: ind == 0 :
            double w = x * x;
            double t = w;
            double e = w - t;
            retVal = (0.5 - e + 0.5) * exp(-t) * retVal;
            if (x < 0.0) {
                retVal = 2.0 - retVal;
            }
        }
        return retVal;

    } /* erfc1 */

    static double gam1(double a) {
        /* ------------------------------------------------------------------ */
        /* COMPUTATION OF 1/GAMMA(A+1) - 1 FOR -0.5 <= A <= 1.5 */
        /* ------------------------------------------------------------------ */

        double t = a;
        double d = a - 0.5;
        // t := if(a > 1/2) a-1 else a
        if (d > 0.0) {
            t = d - 0.5;
        }
        if (t < 0.0) { /* L30: */
            double[] r = {-.422784335098468, -.771330383816272, -.244757765222226, .118378989872749, 9.30357293360349e-4, -.0118290993445146, .00223047661158249, 2.66505979058923e-4,
                            -1.32674909766242e-4};
            double s1 = .273076135303957;
            double s2 = .0559398236957378;

            double top = (((((((r[8] * t + r[7]) * t + r[6]) * t + r[5]) * t + r[4]) * t + r[3]) * t + r[2]) * t + r[1]) * t + r[0];
            double bot = (s2 * t + s1) * t + 1.0;
            double w = top / bot;
            debugPrintf("  gam1(a = %.15f): t < 0: w=%.15f\n", a, w);
            if (d > 0.0) {
                return t * w / a;
            } else {
                return a * (w + 0.5 + 0.5);
            }
        } else if (t == 0) { // L10: a in {0, 1}
            return 0.;

        } else { /* t > 0; L20: */
            double[] p = {.577215664901533, -.409078193005776, -.230975380857675, .0597275330452234, .0076696818164949, -.00514889771323592, 5.89597428611429e-4};
            double[] q = {1., .427569613095214, .158451672430138, .0261132021441447, .00423244297896961};

            double top = (((((p[6] * t + p[5]) * t + p[4]) * t + p[3]) * t + p[2]) * t + p[1]) * t + p[0];
            double bot = (((q[4] * t + q[3]) * t + q[2]) * t + q[1]) * t + 1.0;
            double w = top / bot;
            debugPrintf("  gam1(a = %.15f): t > 0: (is a < 1.5 ?)  w=%.15f\n", a, w);
            if (d > 0.0) { /* L21: */
                return t / a * (w - 0.5 - 0.5);
            } else {
                return a * w;
            }
        }
    } /* gam1 */

    static double gamln1(double a) {
        /* ----------------------------------------------------------------------- */
        /* EVALUATION OF LN(GAMMA(1 + A)) FOR -0.2 <= A <= 1.25 */
        /* ----------------------------------------------------------------------- */

        double w;
        if (a < 0.6) {
            double p0 = .577215664901533;
            double p1 = .844203922187225;
            double p2 = -.168860593646662;
            double p3 = -.780427615533591;
            double p4 = -.402055799310489;
            double p5 = -.0673562214325671;
            double p6 = -.00271935708322958;
            double q1 = 2.88743195473681;
            double q2 = 3.12755088914843;
            double q3 = 1.56875193295039;
            double q4 = .361951990101499;
            double q5 = .0325038868253937;
            double q6 = 6.67465618796164e-4;
            w = ((((((p6 * a + p5) * a + p4) * a + p3) * a + p2) * a + p1) * a + p0) / ((((((q6 * a + q5) * a + q4) * a + q3) * a + q2) * a + q1) * a + 1.);
            return -(a) * w;
        } else { /* 0.6 <= a <= 1.25 */
            double r0 = .422784335098467;
            double r1 = .848044614534529;
            double r2 = .565221050691933;
            double r3 = .156513060486551;
            double r4 = .017050248402265;
            double r5 = 4.97958207639485e-4;
            double s1 = 1.24313399877507;
            double s2 = .548042109832463;
            double s3 = .10155218743983;
            double s4 = .00713309612391;
            double s5 = 1.16165475989616e-4;
            double x = a - 0.5 - 0.5;
            w = (((((r5 * x + r4) * x + r3) * x + r2) * x + r1) * x + r0) / (((((s5 * x + s4) * x + s3) * x + s2) * x + s1) * x + 1.0);
            return x * w;
        }
    } /* gamln1 */

    static double psi(double initialX) {
        double x = initialX;
        /*
         * ---------------------------------------------------------------------
         * 
         * Evaluation of the Digamma function psi(x)
         * 
         * -----------
         * 
         * Psi(xx) is assigned the value 0 when the digamma function cannot be computed.
         * 
         * The main computation involves evaluation of rational Chebyshev approximations published
         * in Math. Comp. 27, 123-127(1973) by Cody, Strecok and Thacher.
         */

        /* --------------------------------------------------------------------- */
        /* Psi was written at Argonne National Laboratory for the FUNPACK */
        /* package of special function subroutines. Psi was modified by */
        /* A.H. Morris (NSWC). */
        /* --------------------------------------------------------------------- */

        double piov4 = .785398163397448; /* == pi / 4 */
        /* dx0 = zero of psi() to extended precision : */
        double dx0 = 1.461632144968362341262659542325721325;

        /* --------------------------------------------------------------------- */
        /* COEFFICIENTS FOR RATIONAL APPROXIMATION OF */
        /* PSI(X) / (X - X0), 0.5 <= X <= 3.0 */
        double[] p1 = {.0089538502298197, 4.77762828042627, 142.441585084029, 1186.45200713425, 3633.51846806499, 4138.10161269013, 1305.60269827897};
        double[] q1 = {44.8452573429826, 520.752771467162, 2210.0079924783, 3641.27349079381, 1908.310765963, 6.91091682714533e-6};
        /* --------------------------------------------------------------------- */

        /* --------------------------------------------------------------------- */
        /* COEFFICIENTS FOR RATIONAL APPROXIMATION OF */
        /* PSI(X) - LN(X) + 1 / (2*X), X > 3.0 */

        double[] p2 = {-2.12940445131011, -7.01677227766759, -4.48616543918019, -.648157123766197};
        double[] q2 = {32.2703493791143, 89.2920700481861, 54.6117738103215, 7.77788548522962};
        /* --------------------------------------------------------------------- */

        /* MACHINE DEPENDENT CONSTANTS ... */

        /* --------------------------------------------------------------------- */
        /*
         * XMAX1 = THE SMALLEST POSITIVE FLOATING POINT CONSTANT WITH ENTIRELY INT REPRESENTATION.
         * ALSO USED AS NEGATIVE OF LOWER BOUND ON ACCEPTABLE NEGATIVE ARGUMENTS AND AS THE POSITIVE
         * ARGUMENT BEYOND WHICH PSI MAY BE REPRESENTED AS LOG(X). Originally: xmax1 =
         * amin1(ipmpar(3), 1./spmpar(1))
         */
        double xmax1 = INT_MAX;
        double d2 = 0.5 / MathInit.d1mach(3); /* = 0.5 / (0.5 * DBL_EPS) = 1/DBL_EPSILON = 2^52 */
        if (xmax1 > d2) {
            xmax1 = d2;
        }

        /* --------------------------------------------------------------------- */
        /* XSMALL = ABSOLUTE ARGUMENT BELOW WHICH PI*COTAN(PI*X) */
        /* MAY BE REPRESENTED BY 1/X. */
        double xsmall = 1e-9;
        /* --------------------------------------------------------------------- */
        double aug = 0.0;
        if (x < 0.5) {
            /* --------------------------------------------------------------------- */
            /* X < 0.5, USE REFLECTION FORMULA */
            /* PSI(1-X) = PSI(X) + PI * COTAN(PI*X) */
            /* --------------------------------------------------------------------- */
            if (fabs(x) <= xsmall) {

                if (x == 0.0) {
                    // goto L_err;
                    return 0.;
                }
                /* --------------------------------------------------------------------- */
                /* 0 < |X| <= XSMALL. USE 1/X AS A SUBSTITUTE */
                /* FOR PI*COTAN(PI*X) */
                /* --------------------------------------------------------------------- */
                aug = -1.0 / x;
            } else { /* |x| > xsmall */
                /* --------------------------------------------------------------------- */
                /* REDUCTION OF ARGUMENT FOR COTAN */
                /* --------------------------------------------------------------------- */
                /* L100: */
                double w = -x;
                double sgn = piov4;
                if (w <= 0.0) {
                    w = -w;
                    sgn = -sgn;
                }
                /* --------------------------------------------------------------------- */
                /* MAKE AN ERROR EXIT IF |X| >= XMAX1 */
                /* --------------------------------------------------------------------- */
                if (w >= xmax1) {
                    // goto L_err;
                    return 0.;
                }
                int nq = (int) w;
                w -= nq;
                nq = (int) (w * 4.0);
                w = (w - nq * 0.25) * 4.0;
                /* --------------------------------------------------------------------- */
                /* W IS NOW RELATED TO THE FRACTIONAL PART OF 4.0 * X. */
                /* ADJUST ARGUMENT TO CORRESPOND TO VALUES IN FIRST */
                /* QUADRANT AND DETERMINE SIGN */
                /* --------------------------------------------------------------------- */
                int n = nq / 2;
                if (n + n != nq) {
                    w = 1.0 - w;
                }
                double z = piov4 * w;
                int m = n / 2;
                if (m + m != n) {
                    sgn = -sgn;
                }
                /* --------------------------------------------------------------------- */
                /* DETERMINE FINAL VALUE FOR -PI*COTAN(PI*X) */
                /* --------------------------------------------------------------------- */
                n = (nq + 1) / 2;
                m = n / 2;
                m += m;
                if (m == n) {
                    /* --------------------------------------------------------------------- */
                    /* CHECK FOR SINGULARITY */
                    /* --------------------------------------------------------------------- */
                    if (z == 0.0) {
                        // goto L_err;
                        return 0.;
                    }
                    /* --------------------------------------------------------------------- */
                    /* USE COS/SIN AS A SUBSTITUTE FOR COTAN, AND */
                    /* SIN/COS AS A SUBSTITUTE FOR TAN */
                    /* --------------------------------------------------------------------- */
                    aug = sgn * (cos(z) / sin(z) * 4.0);

                } else { /* L140: */
                    aug = sgn * (sin(z) / cos(z) * 4.0);
                }
            }

            x = 1.0 - x;

        }
        /* L200: */
        if (x <= 3.0) {
            /* --------------------------------------------------------------------- */
            /* 0.5 <= X <= 3.0 */
            /* --------------------------------------------------------------------- */
            double den = x;
            double upper = p1[0] * x;

            for (int i = 1; i <= 5; ++i) {
                den = (den + q1[i - 1]) * x;
                upper = (upper + p1[i]) * x;
            }

            den = (upper + p1[6]) / (den + q1[5]);
            double xmx0 = x - dx0;
            return den * xmx0 + aug;
        }

        /* --------------------------------------------------------------------- */
        /* IF X >= XMAX1, PSI = LN(X) */
        /* --------------------------------------------------------------------- */
        if (x < xmax1) {
            /* --------------------------------------------------------------------- */
            /* 3.0 < X < XMAX1 */
            /* --------------------------------------------------------------------- */
            double w = 1.0 / (x * x);
            double den = w;
            double upper = p2[0] * w;

            for (int i = 1; i <= 3; ++i) {
                den = (den + q2[i - 1]) * w;
                upper = (upper + p2[i]) * w;
            }

            aug = upper / (den + q2[3]) - 0.5 / x + aug;
        }
        return aug + log(x);

        /* --------------------------------------------------------------------- */
        /* ERROR RETURN */
        /* --------------------------------------------------------------------- */
        // L_err:
        // return 0.;
    } /* psi */

    static double betaln(double a0, double b0) {
        /*
         * ----------------------------------------------------------------------- Evaluation of the
         * logarithm of the beta function ln(beta(a0,b0))
         * -----------------------------------------------------------------------
         */

        double e = .918938533204673; // e == 0.5*LN(2*PI)

        double a = min(a0, b0);
        double b = max(a0, b0);

        if (a < 8.0) {
            if (a < 1.0) {
                /* ----------------------------------------------------------------------- */
                // A < 1
                /* ----------------------------------------------------------------------- */
                if (b < 8.0) {
                    return gamln(a) + (gamln(b) - gamln(a + b));
                } else {
                    return gamln(a) + algdiv(a, b);
                }
            }
            /* else */
            /* ----------------------------------------------------------------------- */
            // 1 <= A < 8
            /* ----------------------------------------------------------------------- */
            double w = 0.0;
            boolean doL40 = false;
            if (a < 2.0) {
                if (b <= 2.0) {
                    return gamln(a) + gamln(b) - gsumln(a, b);
                }
                /* else */

                w = 0.0;
                if (b < 8.0) {
                    doL40 = true;
                } else {
                    return gamln(a) + algdiv(a, b);
                }
            }
            // else L30: REDUCTION OF A WHEN B <= 1000

            if (doL40 || b <= 1e3) {
                int n;
                if (!doL40) {
                    n = (int) (a - 1.0);
                    w = 1.0;
                    for (int i = 1; i <= n; ++i) {
                        a += -1.0;
                        double h = a / b;
                        w *= h / (h + 1.0);
                    }
                    w = log(w);

                    if (b >= 8.0) {
                        return w + gamln(a) + algdiv(a, b);
                    }
                }
                // else
                // L40:
                // reduction of B when B < 8
                n = (int) (b - 1.0);
                double z = 1.0;
                for (int i = 1; i <= n; ++i) {
                    b += -1.0;
                    z *= b / (a + b);
                }
                return w + log(z) + (gamln(a) + (gamln(b) - gsumln(a, b)));
            } else { // L50: reduction of A when B > 1000
                int n = (int) (a - 1.0);
                w = 1.0;
                for (int i = 1; i <= n; ++i) {
                    a += -1.0;
                    w *= a / (a / b + 1.0);
                }
                return log(w) - n * log(b) + (gamln(a) + algdiv(a, b));
            }

        } else {
            /* ----------------------------------------------------------------------- */
            // L60: A >= 8
            /* ----------------------------------------------------------------------- */

            double w = bcorr(a, b);
            double h = a / b;
            double u = -(a - 0.5) * log(h / (h + 1.0));
            double v = b * alnrel(h);
            if (u > v) {
                return log(b) * -0.5 + e + w - v - u;
            } else {
                return log(b) * -0.5 + e + w - u - v;
            }
        }

    } /* betaln */

    static double gsumln(double a, double b) {
        /* ----------------------------------------------------------------------- */
        /* EVALUATION OF THE FUNCTION LN(GAMMA(A + B)) */
        /* FOR 1 <= A <= 2 AND 1 <= B <= 2 */
        /* ----------------------------------------------------------------------- */

        double x = a + b - 2.; // in [0, 2]

        if (x <= 0.25) {
            return gamln1(x + 1.0);
        }

        /* else */
        if (x <= 1.25) {
            return gamln1(x) + alnrel(x);
        }
        /* else x > 1.25 : */
        return gamln1(x - 1.0) + log(x * (x + 1.0));
    } /* gsumln */

    static double bcorr(double a0, double b0) {
        /* ----------------------------------------------------------------------- */

        /* EVALUATION OF DEL(A0) + DEL(B0) - DEL(A0 + B0) WHERE */
        /* LN(GAMMA(A)) = (A - 0.5)*LN(A) - A + 0.5*LN(2*PI) + DEL(A). */
        /* IT IS ASSUMED THAT A0 >= 8 AND B0 >= 8. */

        /* ----------------------------------------------------------------------- */
        /* Initialized data */

        double c0 = .0833333333333333;
        double c1 = -.00277777777760991;
        double c2 = 7.9365066682539e-4;
        double c3 = -5.9520293135187e-4;
        double c4 = 8.37308034031215e-4;
        double c5 = -.00165322962780713;

        /* ------------------------ */
        double a = min(a0, b0);
        double b = max(a0, b0);

        double h = a / b;
        double c = h / (h + 1.0);
        double x = 1.0 / (h + 1.0);
        double x2 = x * x;

        /* SET SN = (1 - X^N)/(1 - X) */

        double s3 = x + x2 + 1.0;
        double s5 = x + x2 * s3 + 1.0;
        double s7 = x + x2 * s5 + 1.0;
        double s9 = x + x2 * s7 + 1.0;
        double s11 = x + x2 * s9 + 1.0;

        /* SET W = DEL(B) - DEL(A + B) */

        /* Computing 2nd power */
        double r1 = 1.0 / b;
        double t = r1 * r1;
        double w = ((((c5 * s11 * t + c4 * s9) * t + c3 * s7) * t + c2 * s5) * t + c1 * s3) * t + c0;
        w *= c / b;

        /* COMPUTE DEL(A) + W */

        /* Computing 2nd power */
        r1 = 1.0 / a;
        t = r1 * r1;
        return (((((c5 * t + c4) * t + c3) * t + c2) * t + c1) * t + c0) / a + w;
    } /* bcorr */

    static double algdiv(double a, double b) {
        /* ----------------------------------------------------------------------- */

        /* COMPUTATION OF LN(GAMMA(B)/GAMMA(A+B)) WHEN B >= 8 */

        /* -------- */

        /* IN THIS ALGORITHM, DEL(X) IS THE FUNCTION DEFINED BY */
        /* LN(GAMMA(X)) = (X - 0.5)*LN(X) - X + 0.5*LN(2*PI) + DEL(X). */

        /* ----------------------------------------------------------------------- */

        /* Initialized data */

        double c0 = .0833333333333333;
        double c1 = -.00277777777760991;
        double c2 = 7.9365066682539e-4;
        double c3 = -5.9520293135187e-4;
        double c4 = 8.37308034031215e-4;
        double c5 = -.00165322962780713;

        double h;
        double c;
        double x;
        double d;

        /* ------------------------ */
        if (a > b) {
            h = b / a;
            c = 1.0 / (h + 1.0);
            x = h / (h + 1.0);
            d = a + (b - 0.5);
        } else {
            h = a / b;
            c = h / (h + 1.0);
            x = 1.0 / (h + 1.0);
            d = b + (a - 0.5);
        }

        /* Set s<n> = (1 - x^n)/(1 - x) : */

        double x2 = x * x;
        double s3 = x + x2 + 1.0;
        double s5 = x + x2 * s3 + 1.0;
        double s7 = x + x2 * s5 + 1.0;
        double s9 = x + x2 * s7 + 1.0;
        double s11 = x + x2 * s9 + 1.0;

        /* w := Del(b) - Del(a + b) */

        double t = 1. / (b * b);
        double w = ((((c5 * s11 * t + c4 * s9) * t + c3 * s7) * t + c2 * s5) * t + c1 * s3) * t + c0;
        w *= c / b;

        /* COMBINE THE RESULTS */

        double u = d * alnrel(a / b);
        double v = a * (log(b) - 1.0);
        if (u > v) {
            return w - v - u;
        } else {
            return w - u - v;
        }
    } /* algdiv */

    static double gamln(double a) {
        /*
         * ----------------------------------------------------------------------- Evaluation of
         * ln(gamma(a)) for positive a
         * -----------------------------------------------------------------------
         */
        /* Written by Alfred H. Morris */
        /* Naval Surface Warfare Center */
        /* Dahlgren, Virginia */
        /* ----------------------------------------------------------------------- */

        double d = .418938533204673; // d == 0.5*(LN(2*PI) - 1)

        double c0 = .0833333333333333;
        double c1 = -.00277777777760991;
        double c2 = 7.9365066682539e-4;
        double c3 = -5.9520293135187e-4;
        double c4 = 8.37308034031215e-4;
        double c5 = -.00165322962780713;

        if (a <= 0.8) {
            return gamln1(a) - log(a); /* ln(G(a+1)) - ln(a) == ln(G(a+1)/a) = ln(G(a)) */
        } else if (a <= 2.25) {
            return gamln1(a - 0.5 - 0.5);
        } else if (a < 10.0) {
            int n = (int) (a - 1.25);
            double t = a;
            double w = 1.0;
            for (int i = 1; i <= n; ++i) {
                t += -1.0;
                w *= t;
            }
            return gamln1(t - 1.) + log(w);
        } else { /* a >= 10 */
            double t = 1. / (a * a);
            double w = (((((c5 * t + c4) * t + c3) * t + c2) * t + c1) * t + c0) / a;
            return d + w + (a - 0.5) * (log(a) - 1.0);
        }
    } /* gamln */

}
