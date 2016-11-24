/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2012, The R Core Team
 * Copyright (c) 2004, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.library.stats.StatsUtil.DBLEPSILON;
import static com.oracle.truffle.r.library.stats.StatsUtil.DBL_MANT_DIG;
import static com.oracle.truffle.r.library.stats.StatsUtil.DBL_MAX_EXP;
import static com.oracle.truffle.r.library.stats.StatsUtil.DBL_MIN_EXP;
import static com.oracle.truffle.r.library.stats.StatsUtil.M_LOG10_2;
import static com.oracle.truffle.r.library.stats.StatsUtil.M_PI;
import static com.oracle.truffle.r.library.stats.StatsUtil.fmax2;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.stats.GammaFunctions;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.BaseGammaFunctionsFactory.DpsiFnCalcNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public class BaseGammaFunctions {

    @RBuiltin(name = "gamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class Gamma extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RDoubleVector lgamma(@SuppressWarnings("unused") RAbstractDoubleVector x) {
            throw RError.nyi(this, "gamma");
        }
    }

    @RBuiltin(name = "trigamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class TriGamma extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RDoubleVector trigamma(@SuppressWarnings("unused") RAbstractDoubleVector x) {
            throw RError.nyi(this, "trigamma");
        }
    }

    @RBuiltin(name = "lgamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class Lgamma extends RBuiltinNode {

        private final NACheck naValCheck = NACheck.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").defaultError(RError.Message.NON_NUMERIC_MATH).mustBe(complexValue().not(), RError.Message.UNIMPLEMENTED_COMPLEX_FUN).mustBe(numericValue()).asDoubleVector();
        }

        @Specialization
        protected RDoubleVector lgamma(RAbstractDoubleVector x) {
            naValCheck.enable(true);
            double[] result = new double[x.getLength()];
            for (int i = 0; i < x.getLength(); i++) {
                double xv = x.getDataAt(i);
                result[i] = GammaFunctions.lgammafn(xv);
                naValCheck.check(result[i]);
            }
            return RDataFactory.createDoubleVector(result, naValCheck.neverSeenNA());
        }

        @Specialization
        protected RDoubleVector lgamma(RAbstractIntVector x) {
            return lgamma(RClosures.createIntToDoubleVector(x));
        }

        @Specialization
        protected RDoubleVector lgamma(RAbstractLogicalVector x) {
            return lgamma(RClosures.createLogicalToDoubleVector(x));
        }

    }

    @RBuiltin(name = "digamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class DiGamma extends RBuiltinNode {

        @Child private DpsiFnCalc dpsiFnCalc;

        private final NACheck naValCheck = NACheck.create();

        private double dpsiFnCalc(double x, int n, int kode, double ans) {
            if (dpsiFnCalc == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dpsiFnCalc = insert(DpsiFnCalcNodeGen.create(null, null, null, null));
            }
            return dpsiFnCalc.executeDouble(x, n, kode, ans);
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("x").defaultError(RError.Message.NON_NUMERIC_MATH).mustBe(complexValue().not(), RError.Message.UNIMPLEMENTED_COMPLEX_FUN).mustBe(numericValue()).asDoubleVector();
        }

        @Specialization
        protected RDoubleVector digamma(RAbstractDoubleVector x) {
            naValCheck.enable(x);
            double[] result = new double[x.getLength()];
            boolean warnNaN = false;
            for (int i = 0; i < x.getLength(); i++) {
                double xv = x.getDataAt(i);
                if (naValCheck.check(xv)) {
                    result[i] = xv;
                } else {
                    double val = dpsiFnCalc(xv, 0, 1, 0);
                    if (Double.isNaN(val)) {
                        result[i] = val;
                        warnNaN = true;
                    } else {
                        result[i] = -val;
                    }
                }
            }
            if (warnNaN) {
                RError.warning(this, RError.Message.NAN_PRODUCED);
            }
            return RDataFactory.createDoubleVector(result, naValCheck.neverSeenNA());
        }

        @Specialization
        protected RDoubleVector digamma(RAbstractIntVector x) {
            return digamma(RClosures.createIntToDoubleVector(x));
        }

        @Specialization
        protected RDoubleVector digamma(RAbstractLogicalVector x) {
            return digamma(RClosures.createLogicalToDoubleVector(x));
        }

    }

    @NodeChildren({@NodeChild(value = "x"), @NodeChild(value = "n"), @NodeChild(value = "kode"), @NodeChild(value = "ans")})
    protected abstract static class DpsiFnCalc extends RNode {

        // the following is transcribed from polygamma.c

        public abstract double executeDouble(double x, int n, int kode, double ans);

        @Child private DpsiFnCalc dpsiFnCalc;

        @CompilationFinal private static final double[] bvalues = new double[]{1.00000000000000000e+00, -5.00000000000000000e-01, 1.66666666666666667e-01, -3.33333333333333333e-02,
                        2.38095238095238095e-02, -3.33333333333333333e-02, 7.57575757575757576e-02, -2.53113553113553114e-01, 1.16666666666666667e+00, -7.09215686274509804e+00,
                        5.49711779448621554e+01, -5.29124242424242424e+02, 6.19212318840579710e+03, -8.65802531135531136e+04, 1.42551716666666667e+06, -2.72982310678160920e+07,
                        6.01580873900642368e+08, -1.51163157670921569e+10, 4.29614643061166667e+11, -1.37116552050883328e+13, 4.88332318973593167e+14, -1.92965793419400681e+16};

        private static final int n_max = 100;
        // the following is actually a parameter in the original code, but it's always 1 and must be
        // as the original code treats the "ans" value of type double as an array, which is legal
        // only if a the first element of the array is accessed at all times
        private static final int m = 1;

        private double dpsiFnCalc(double x, int n, int kode, double ans) {
            if (dpsiFnCalc == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dpsiFnCalc = insert(DpsiFnCalcNodeGen.create(null, null, null, null));
            }
            return dpsiFnCalc.executeDouble(x, n, kode, ans);
        }

        // TODO: it's recursive - turn into AST recursion
        @Specialization
        double dpsifn(double xOld, int n, int kode, double ansOld) {

            double x = xOld;
            double ans = ansOld;

            int mm;
            int mx;
            int nn;
            int np;
            int nx;
            int fn;
            double arg;
            double den;
            double elim;
            double eps;
            double fln;
            double rln;
            double r1m4;
            double r1m5;
            double s;
            double slope;
            double t;
            double tk;
            double tt;
            double t1;
            double t2;
            double wdtol;
            double xdmln;
            double xdmy;
            double xinc;
            double xln = 0.0;
            double xm;
            double xmin;
            double yint;
            double[] trm = new double[23];
            double[] trmr = new double[n_max + 1];

            // non-zero ierr always results in generating a NaN
            // mVal.ierr = 0;
            if (n < 0 || kode < 1 || kode > 2 || m < 1) {
                return Double.NaN;
            }
            if (x <= 0.) {
                /*
                 * use Abramowitz & Stegun 6.4.7 "Reflection Formula" psi(k, x) = (-1)^k psi(k, 1-x)
                 * - pi^{n+1} (d/dx)^n cot(x)
                 */
                if (x == Math.round(x)) {
                    /* non-positive integer : +Inf or NaN depends on n */
                    // for(j=0; j < m; j++) /* k = j + n : */
                    // ans[j] = ((j+n) % 2) ? ML_POSINF : ML_NAN;
                    // m is always 1
                    ans = (n % 2) != 0 ? Double.POSITIVE_INFINITY : Double.NaN;
                    return ans;
                }
                /* This could cancel badly */
                ans = dpsiFnCalc(1. - x, n, /* kode = */1, ans);
                /*
                 * ans[j] == (-1)^(k+1) / gamma(k+1) * psi(k, 1 - x) for j = 0:(m-1) , k = n + j
                 */

                /* Cheat for now: only work for m = 1, n in {0,1,2,3} : */
                if (m > 1 || n > 3) { /* doesn't happen for digamma() .. pentagamma() */
                    /* not yet implemented */
                    // non-zero ierr always results in generating a NaN
                    // mVal.ierr = 4;
                    return Double.NaN;
                }
                x *= M_PI; /* pi * x */
                if (n == 0) {
                    tt = Math.cos(x) / Math.sin(x);
                } else if (n == 1) {
                    tt = -1 / Math.pow(Math.sin(x), 2);
                } else if (n == 2) {
                    tt = 2 * Math.cos(x) / Math.pow(Math.sin(x), 3);
                } else if (n == 3) {
                    tt = -2 * (2 * Math.pow(Math.cos(x), 2) + 1.) / Math.pow(Math.sin(x), 4);
                } else { /* can not happen! */
                    tt = RRuntime.DOUBLE_NA;
                }
                /* end cheat */

                s = (n % 2) != 0 ? -1. : 1.; /* s = (-1)^n */
                /*
                 * t := pi^(n+1) * d_n(x) / gamma(n+1) , where d_n(x) := (d/dx)^n cot(x)
                 */
                t1 = t2 = s = 1.;
                for (int k = 0, j = k - n; j < m; k++, j++, s = -s) {
                    /* k == n+j , s = (-1)^k */
                    t1 *= M_PI; /* t1 == pi^(k+1) */
                    if (k >= 2) {
                        t2 *= k; /* t2 == k! == gamma(k+1) */
                    }
                    if (j >= 0) { /* by cheat above, tt === d_k(x) */
                        // j must always be 0
                        assert j == 0;
                        // ans[j] = s*(ans[j] + t1/t2 * tt);
                        ans = s * (ans + t1 / t2 * tt);
                    }
                }
                if (n == 0 && kode == 2) { /* unused from R, but "wrong": xln === 0 : */
                    // ans[0] += xln;
                    ans += xln;
                }
                return ans;
            } /* x <= 0 */

            /* else : x > 0 */
            // nz not used
            // mVal.nz = 0;
            xln = Math.log(x);
            if (kode == 1 /* && m == 1 */) { /* the R case --- for very large x: */
                double lrg = 1 / (2. * DBLEPSILON);
                if (n == 0 && x * xln > lrg) {
                    // ans[0] = -xln;
                    ans = -xln;
                    return ans;
                } else if (n >= 1 && x > n * lrg) {
                    // ans[0] = exp(-n * xln)/n; /* == x^-n / n == 1/(n * x^n) */
                    ans = Math.exp(-n * xln) / n;
                    return ans;
                }
            }
            mm = m;
            // nx = imin2(-Rf_i1mach(15), Rf_i1mach(16));/* = 1021 */
            nx = Math.min(-DBL_MIN_EXP, DBL_MAX_EXP);
            assert (nx == 1021);
            r1m5 = M_LOG10_2; // Rf_d1mach(5);
            r1m4 = DBLEPSILON * 0.5; // Rf_d1mach(4) * 0.5;
            wdtol = fmax2(r1m4, 0.5e-18); /* 1.11e-16 */

            /* elim = approximate exponential over and underflow limit */
            elim = 2.302 * (nx * r1m5 - 3.0); /* = 700.6174... */
            for (;;) {
                nn = n + mm - 1;
                fn = nn;
                t = (fn + 1) * xln;

                /* overflow and underflow test for small and large x */

                if (Math.abs(t) > elim) {
                    if (t <= 0.0) {
                        // nz not used
                        // mVal.nz = 0;
                        // non-zero ierr always results in generating a NaN
                        // mVal.ierr = 2;
                        return Double.NaN;
                    }
                } else {
                    if (x < wdtol) {
                        // ans[0] = R_pow_di(x, -n-1);
                        ans = Math.pow(x, -n - 1);
                        if (mm != 1) {
                            // for(k = 1; k < mm ; k++)
                            // ans[k] = ans[k-1] / x;
                            assert mm < 2;
                            // int the original code, ans should not be accessed beyond the 0th
                            // index
                        }
                        if (n == 0 && kode == 2) {
                            // ans[0] += xln;
                            ans += xln;
                        }
                        return ans;
                    }

                    /* compute xmin and the number of terms of the series, fln+1 */

                    rln = r1m5 * DBL_MANT_DIG; // Rf_i1mach(14);
                    rln = Math.min(rln, 18.06);
                    fln = Math.max(rln, 3.0) - 3.0;
                    yint = 3.50 + 0.40 * fln;
                    slope = 0.21 + fln * (0.0006038 * fln + 0.008677);
                    xm = yint + slope * fn;
                    mx = (int) xm + 1;
                    xmin = mx;
                    if (n != 0) {
                        xm = -2.302 * rln - Math.min(0.0, xln);
                        arg = xm / n;
                        arg = Math.min(0.0, arg);
                        eps = Math.exp(arg);
                        xm = 1.0 - eps;
                        if (Math.abs(arg) < 1.0e-3) {
                            xm = -arg;
                        }
                        fln = x * xm / eps;
                        xm = xmin - x;
                        if (xm > 7.0 && fln < 15.0) {
                            break;
                        }
                    }
                    xdmy = x;
                    xdmln = xln;
                    xinc = 0.0;
                    if (x < xmin) {
                        nx = (int) x;
                        xinc = xmin - nx;
                        xdmy = x + xinc;
                        xdmln = Math.log(xdmy);
                    }

                    /* generate w(n+mm-1, x) by the asymptotic expansion */

                    t = fn * xdmln;
                    t1 = xdmln + xdmln;
                    t2 = t + xdmln;
                    tk = Math.max(Math.abs(t), fmax2(Math.abs(t1), Math.abs(t2)));
                    if (tk <= elim) { /* for all but large x */
                        return l10(t, tk, xdmy, xdmln, x, nn, nx, wdtol, fn, trm, trmr, xinc, mm, kode, ans);
                    }
                }
                // nz not used
                // mVal.nz++; /* underflow */
                mm--;
                // ans[mm] = 0.;
                assert mm == 0;
                ans = 0.;
                if (mm == 0) {
                    return ans;
                }
            } /* end{for()} */
            nn = (int) fln + 1;
            np = n + 1;
            t1 = (n + 1) * xln;
            t = Math.exp(-t1);
            s = t;
            den = x;
            for (int i = 1; i <= nn; i++) {
                den += 1.;
                trm[i] = Math.pow(den, -np);
                s += trm[i];
            }
            // ans[0] = s;
            ans = s;
            if (n == 0 && kode == 2) {
                // ans[0] = s + xln;
                ans = s + xln;
            }

            if (mm != 1) { /* generate higher derivatives, j > n */
                assert false;
                // tol = wdtol / 5.0;
                // for(j = 1; j < mm; j++) {
                // t /= x;
                // s = t;
                // tols = t * tol;
                // den = x;
                // for(i=1; i <= nn; i++) {
                // den += 1.;
                // trm[i] /= den;
                // s += trm[i];
                // if (trm[i] < tols) {
                // break;
                // }
                // }
                // ans[j] = s;
                // }
            }
            return ans;

        }

        private static double l10(double oldT, double oldTk, double xdmy, double xdmln, double x, double nn, double oldNx, double wdtol, double oldFn, double[] trm, double[] trmr, double xinc,
                        double mm, int kode, double ansOld) {
            double t = oldT;
            double tk = oldTk;
            double nx = oldNx;
            double fn = oldFn;
            double ans = ansOld;

            double tss = Math.exp(-t);
            double tt = 0.5 / xdmy;
            double t1 = tt;
            double tst = wdtol * tt;
            if (nn != 0) {
                t1 = tt + 1.0 / fn;
            }
            double rxsq = 1.0 / (xdmy * xdmy);
            double ta = 0.5 * rxsq;
            t = (fn + 1) * ta;
            double s = t * bvalues[2];
            if (Math.abs(s) >= tst) {
                tk = 2.0;
                for (int k = 4; k <= 22; k++) {
                    t = t * ((tk + fn + 1) / (tk + 1.0)) * ((tk + fn) / (tk + 2.0)) * rxsq;
                    trm[k] = t * bvalues[k - 1];
                    if (Math.abs(trm[k]) < tst) {
                        break;
                    }
                    s += trm[k];
                    tk += 2.;
                }
            }
            s = (s + t1) * tss;
            if (xinc != 0.0) {

                /* backward recur from xdmy to x */

                nx = (int) xinc;
                double np = nn + 1;
                if (nx > n_max) {
                    // nz not used
                    // mVal.nz = 0;
                    // non-zero ierr always results in generating a NaN
                    // mVal.ierr = 3;
                    return Double.NaN;
                } else {
                    if (nn == 0) {
                        return l20(xdmln, xdmy, x, s, nx, kode, ans);
                    }
                    double xm = xinc - 1.0;
                    double fx = x + xm;

                    /* this loop should not be changed. fx is accurate when x is small */
                    for (int i = 1; i <= nx; i++) {
                        trmr[i] = Math.pow(fx, -np);
                        s += trmr[i];
                        xm -= 1.;
                        fx = x + xm;
                    }
                }
            }
            // ans[mm-1] = s;
            assert (mm - 1) == 0;
            ans = s;
            if (fn == 0) {
                return l30(xdmln, xdmy, x, s, kode, ans);
            }

            /* generate lower derivatives, j < n+mm-1 */

            for (int j = 2; j <= mm; j++) {
                fn--;
                tss *= xdmy;
                t1 = tt;
                if (fn != 0) {
                    t1 = tt + 1.0 / fn;
                }
                t = (fn + 1) * ta;
                s = t * bvalues[2];
                if (Math.abs(s) >= tst) {
                    tk = 4 + fn;
                    for (int k = 4; k <= 22; k++) {
                        trm[k] = trm[k] * (fn + 1) / tk;
                        if (Math.abs(trm[k]) < tst) {
                            break;
                        }
                        s += trm[k];
                        tk += 2.;
                    }
                }
                s = (s + t1) * tss;
                if (xinc != 0.0) {
                    if (fn == 0) {
                        return l20(xdmln, xdmy, x, s, nx, kode, ans);
                    }
                    double xm = xinc - 1.0;
                    double fx = x + xm;
                    for (int i = 1; i <= nx; i++) {
                        trmr[i] = trmr[i] * fx;
                        s += trmr[i];
                        xm -= 1.;
                        fx = x + xm;
                    }
                }
                // ans[mm - j] = s;
                assert (mm - j) == 0;
                ans = s;
                if (fn == 0) {
                    return l30(xdmln, xdmy, x, s, kode, ans);
                }
            }
            return ans;

        }

        private static double l20(double xdmln, double xdmy, double x, double oldS, double nx, int kode, double ans) {
            double s = oldS;
            for (int i = 1; i <= nx; i++) {
                s += 1. / (x + (nx - i)); /* avoid disastrous cancellation, PR#13714 */
            }

            return l30(xdmln, xdmy, x, s, kode, ans);
        }

        private static double l30(double xdmln, double xdmy, double x, double s, int kode, double ansOld) {
            double ans = ansOld;
            if (kode != 2) { /* always */
                // ans[0] = s - xdmln;
                ans = s - xdmln;
            } else if (xdmy != x) {
                double xq;
                xq = xdmy / x;
                // ans[0] = s - log(xq);
                ans = s - Math.log(xq);
            }
            return ans;
        }
    }
}
