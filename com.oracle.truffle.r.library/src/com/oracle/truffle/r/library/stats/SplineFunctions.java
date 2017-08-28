/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998--2012  The R Core Team
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.RError.Message.NA_INTRODUCED_COERCION;
import static com.oracle.truffle.r.runtime.RRuntime.INT_NA;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.nmath.RMath;

/**
 * Internal functions for the spline function in the stats package. These are originally called
 * through the .Call mechanism, but are implemented as substitutes in FastR.
 *
 * The code is derived from GNU R, file {@code splines.c}.
 */
public class SplineFunctions {

    public abstract static class SplineCoef extends RExternalBuiltinNode.Arg3 {
        static {
            Casts casts = new Casts(SplineCoef.class);
            casts.arg(0).mapNull(constant(INT_NA)).mapIf(numericValue(),
                            chain(asIntegerVector()).with(findFirst().integerElement(INT_NA)).end(),
                            chain(asIntegerVector()).with(findFirst().integerElement(INT_NA)).with(
                                            Predef.shouldBe(integerValue(), NA_INTRODUCED_COERCION)).end());
            casts.arg(1).mustNotBeMissing().asDoubleVector();
            casts.arg(2).mustNotBeMissing().asDoubleVector();
        }

        @Specialization
        @TruffleBoundary
        protected Object splineCoef(int method, RAbstractDoubleVector x, RAbstractDoubleVector y) {
            return splineCoefImpl(method, x.materialize(), y.materialize());
        }

        @Specialization
        @TruffleBoundary
        protected Object splineCoef(int method, RAbstractDoubleVector x, @SuppressWarnings("unused") RNull y) {
            return splineCoefImpl(method, x.materialize(), RDataFactory.createDoubleVector(0));
        }

        @Specialization
        @TruffleBoundary
        protected Object splineCoef(int method, @SuppressWarnings("unused") RNull x, RAbstractDoubleVector y) {
            return splineCoefImpl(method, RDataFactory.createDoubleVector(0), y.materialize());
        }

        @Specialization
        @TruffleBoundary
        protected Object splineCoef(int method, @SuppressWarnings("unused") RNull x, @SuppressWarnings("unused") RNull y) {
            return splineCoefImpl(method, RDataFactory.createDoubleVector(0), RDataFactory.createDoubleVector(0));
        }

        private RList splineCoefImpl(int method, RDoubleVector x, RDoubleVector y) {
            int n = x.getLength();
            if (y.getLength() != n) {
                throw error(RError.Message.INPUTS_DIFFERENT_LENGTHS);
            }

            double[] b = new double[n];
            double[] c = new double[n];
            double[] d = new double[n];

            SplineFunctions.splineCoef(method, n, x.getReadonlyData(), y.getReadonlyData(), b, c, d);

            final boolean complete = x.isComplete() && y.isComplete();
            RDoubleVector bv = RDataFactory.createDoubleVector(b, complete);
            RDoubleVector cv = RDataFactory.createDoubleVector(c, complete);
            RDoubleVector dv = RDataFactory.createDoubleVector(d, complete);
            Object[] resultData = new Object[]{method, n, x, y, bv, cv, dv};
            RStringVector resultNames = RDataFactory.createStringVector(new String[]{"method", "n", "x", "y", "b", "c", "d"}, RDataFactory.COMPLETE_VECTOR);
            return RDataFactory.createList(resultData, resultNames);
        }
    }

    public abstract static class SplineEval extends RExternalBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(SplineEval.class);
            casts.arg(0, "xout").mustBe(doubleValue()).asDoubleVector();
            casts.arg(1, "z").mustBe(instanceOf(RList.class));
        }

        @Specialization
        @TruffleBoundary
        protected Object splineEval(RAbstractDoubleVector xout, RList z) {
            // This is called with the result of SplineCoef, so it is surely an RList
            return SplineFunctions.splineEval(xout.materialize(), z);
        }
    }

    private static void splineCoef(int method, int n, double[] x, double[] y, double[] b, double[] c, double[] d) {
        switch (method) {
            case 1:
                periodicSpline(n, x, y, b, c, d);
                break;
            case 2:
                naturalSpline(n, x, y, b, c, d);
                break;
            case 3:
                fmmSpline(n, x, y, b, c, d);
                break;
        }
    }

    /*
     * Periodic Spline --------------- The end conditions here match spline (and its derivatives) at
     * x[1] and x[n].
     *
     * Note: There is an explicit check that the user has supplied data with y[1] equal to y[n].
     */
    private static void periodicSpline(int n, double[] x, double[] y, double[] b, double[] c, double[] d) {
        double s;
        int i;
        int nm2;

        double[] e = new double[n];

        if (n < 2 || y[0] != y[n - 1]) {
            return;
        }

        if (n == 2) {
            b[0] = 0.0;
            b[1] = 0.0;
            c[0] = 0.0;
            c[1] = 0.0;
            d[0] = 0.0;
            d[1] = 0.0;
            return;
        } else if (n == 3) {
            double r = -(y[0] - y[1]) * (x[0] - 2 * x[1] + x[2]) / (x[2] - x[1]) / (x[1] - x[0]);
            b[0] = r;
            b[1] = r;
            b[2] = r;
            c[0] = -3 * (y[0] - y[1]) / (x[2] - x[1]) / (x[1] - x[0]);
            c[1] = -c[0];
            c[2] = c[0];
            d[0] = -2 * c[0] / 3 / (x[1] - x[0]);
            d[1] = -d[0] * (x[1] - x[0]) / (x[2] - x[1]);
            d[2] = d[0];
            return;
        }

        /* else --------- n >= 4 --------- */
        nm2 = n - 2;

        /* Set up the matrix system */
        /* A = diagonal B = off-diagonal C = rhs */

        double[] mA = b;
        double[] mB = d;
        double[] mC = c;

        mB[0] = x[1] - x[0];
        mB[nm2] = x[n - 1] - x[nm2];
        mA[0] = 2.0 * (mB[0] + mB[nm2]);
        mC[0] = (y[1] - y[0]) / mB[0] - (y[n - 1] - y[nm2]) / mB[nm2];

        for (i = 1; i < n - 1; i++) {
            mB[i] = x[i + 1] - x[i];
            mA[i] = 2.0 * (mB[i] + mB[i - 1]);
            mC[i] = (y[i + 1] - y[i]) / mB[i] - (y[i] - y[i - 1]) / mB[i - 1];
        }

        /* Choleski decomposition */

        double[] mL = b;
        double[] mM = d;
        double[] mE = e;

        mL[0] = Math.sqrt(mA[0]);
        mE[0] = (x[n - 1] - x[nm2]) / mL[0];
        s = 0.0;
        for (i = 0; i <= nm2 - 2; i++) {
            mM[i] = mB[i] / mL[i];
            if (i != 0) {
                mE[i] = -mE[i - 1] * mM[i - 1] / mL[i];
            }
            mL[i + 1] = Math.sqrt(mA[i + 1] - mM[i] * mM[i]);
            s = s + mE[i] * mE[i];
        }
        mM[nm2 - 1] = (mB[nm2 - 1] - mE[nm2 - 2] * mM[nm2 - 2]) / mL[nm2 - 1];
        mL[nm2] = Math.sqrt(mA[nm2] - mM[nm2 - 1] * mM[nm2 - 1] - s);

        /* Forward Elimination */

        double[] mY = c;
        double[] mD = c;

        mY[0] = mD[0] / mL[0];
        s = 0.0;
        for (i = 1; i <= nm2 - 1; i++) {
            mY[i] = (mD[i] - mM[i - 1] * mY[i - 1]) / mL[i];
            s = s + mE[i - 1] * mY[i - 1];
        }
        mY[nm2] = (mD[nm2] - mM[nm2 - 1] * mY[nm2 - 1] - s) / mL[nm2];

        double[] mX = c;

        mX[nm2] = mY[nm2] / mL[nm2];
        mX[nm2 - 1] = (mY[nm2 - 1] - mM[nm2 - 1] * mX[nm2]) / mL[nm2 - 1];
        for (i = nm2 - 2; i >= 0; i--) {
            mX[i] = (mY[i] - mM[i] * mX[i + 1] - mE[i] * mX[nm2]) / mL[i];
        }

        /* Wrap around */

        mX[n - 1] = mX[0];

        /* Compute polynomial coefficients */

        for (i = 0; i <= nm2; i++) {
            s = x[i + 1] - x[i];
            b[i] = (y[i + 1] - y[i]) / s - s * (c[i + 1] + 2.0 * c[i]);
            d[i] = (c[i + 1] - c[i]) / s;
            c[i] = 3.0 * c[i];
        }
        b[n - 1] = b[0];
        c[n - 1] = c[0];
        d[n - 1] = d[0];
        return;
    }

    /*
     * Natural Splines --------------- Here the end-conditions are determined by setting the second
     * derivative of the spline at the end-points to equal to zero.
     *
     * There are n-2 unknowns (y[i]'' at x[2], ..., x[n-1]) and n-2 equations to determine them.
     * Either Choleski or Gaussian elimination could be used.
     */
    private static void naturalSpline(int n, double[] x, double[] y, double[] b, double[] c, double[] d) {
        int nm2;
        int i;
        double t;

        if (n < 2) {
            return;
        }

        if (n < 3) {
            t = (y[1] - y[0]);
            b[0] = t / (x[1] - x[0]);
            b[1] = b[0];
            c[0] = 0.0;
            c[1] = 0.0;
            d[0] = 0.0;
            d[1] = 0.0;
            return;
        }

        nm2 = n - 2;

        /* Set up the tridiagonal system */
        /* b = diagonal, d = offdiagonal, c = right hand side */

        d[0] = x[1] - x[0];
        c[1] = (y[1] - y[0]) / d[0];
        for (i = 1; i < n - 1; i++) {
            d[i] = x[i + 1] - x[i];
            b[i] = 2.0 * (d[i - 1] + d[i]);
            c[i + 1] = (y[i + 1] - y[i]) / d[i];
            c[i] = c[i + 1] - c[i];
        }

        /* Gaussian elimination */

        for (i = 2; i < n - 1; i++) {
            t = d[i - 1] / b[i - 1];
            b[i] = b[i] - t * d[i - 1];
            c[i] = c[i] - t * c[i - 1];
        }

        /* Backward substitution */

        c[nm2] = c[nm2] / b[nm2];
        for (i = n - 3; i > 0; i--) {
            c[i] = (c[i] - d[i] * c[i + 1]) / b[i];
        }

        /* End conditions */

        c[0] = c[n - 1] = 0.0;

        /* Get cubic coefficients */

        b[0] = (y[1] - y[0]) / d[0] - d[i] * c[1];
        c[0] = 0.0;
        d[0] = c[1] / d[0];
        b[n - 1] = (y[n - 1] - y[nm2]) / d[nm2] + d[nm2] * c[nm2];
        for (i = 1; i < n - 1; i++) {
            b[i] = (y[i + 1] - y[i]) / d[i] - d[i] * (c[i + 1] + 2.0 * c[i]);
            d[i] = (c[i + 1] - c[i]) / d[i];
            c[i] = 3.0 * c[i];
        }
        c[n - 1] = 0.0;
        d[n - 1] = 0.0;

        return;
    }

    /*
     * Splines a la Forsythe Malcolm and Moler --------------------------------------- In this case
     * the end-conditions are determined by fitting cubic polynomials to the first and last 4 points
     * and matching the third derivitives of the spline at the end-points to the third derivatives
     * of these cubics at the end-points.
     */
    private static void fmmSpline(int n, double[] x, double[] y, double[] b, double[] c, double[] d) {
        int nm2;
        int i;
        double t;

        if (n < 2) {
            return;
        }

        if (n < 3) {
            t = (y[1] - y[0]);
            b[0] = t / (x[1] - x[0]);
            b[1] = b[0];
            c[0] = 0.0;
            c[1] = 0.0;
            d[0] = 0.0;
            d[1] = 0.0;
            return;
        }

        nm2 = n - 2;

        /* Set up tridiagonal system */
        /* b = diagonal, d = offdiagonal, c = right hand side */

        d[0] = x[1] - x[0];
        c[1] = (y[1] - y[0]) / d[0]; /* = +/- Inf for x[1]=x[2] -- problem? */
        for (i = 1; i < n - 1; i++) {
            d[i] = x[i + 1] - x[i];
            b[i] = 2.0 * (d[i - 1] + d[i]);
            c[i + 1] = (y[i + 1] - y[i]) / d[i];
            c[i] = c[i + 1] - c[i];
        }

        /* End conditions. */
        /* Third derivatives at x[0] and x[n-1] obtained */
        /* from divided differences */

        b[0] = -d[0];
        b[n - 1] = -d[nm2];
        c[0] = 0.0;
        c[n - 1] = 0.0;
        if (n > 3) {
            c[0] = c[2] / (x[3] - x[1]) - c[1] / (x[2] - x[0]);
            c[n - 1] = c[nm2] / (x[n - 1] - x[n - 3]) - c[n - 3] / (x[nm2] - x[n - 4]);
            c[0] = c[0] * d[0] * d[0] / (x[3] - x[0]);
            c[n - 1] = -c[n - 1] * d[nm2] * d[nm2] / (x[n - 1] - x[n - 4]);
        }

        /* Gaussian elimination */

        for (i = 1; i <= n - 1; i++) {
            t = d[i - 1] / b[i - 1];
            b[i] = b[i] - t * d[i - 1];
            c[i] = c[i] - t * c[i - 1];
        }

        /* Backward substitution */

        c[n - 1] = c[n - 1] / b[n - 1];
        for (i = nm2; i >= 0; i--) {
            c[i] = (c[i] - d[i] * c[i + 1]) / b[i];
        }

        /* c[i] is now the sigma[i-1] of the text */
        /* Compute polynomial coefficients */

        b[n - 1] = (y[n - 1] - y[n - 2]) / d[n - 2] + d[n - 2] * (c[n - 2] + 2.0 * c[n - 1]);
        for (i = 0; i <= nm2; i++) {
            b[i] = (y[i + 1] - y[i]) / d[i] - d[i] * (c[i + 1] + 2.0 * c[i]);
            d[i] = (c[i + 1] - c[i]) / d[i];
            c[i] = 3.0 * c[i];
        }
        c[n - 1] = 3.0 * c[n - 1];
        d[n - 1] = d[nm2];
        return;
    }

    private static RDoubleVector splineEval(RDoubleVector xout, RList z) {
        int nu = xout.getLength();
        double[] yout = new double[nu];
        int method = (int) z.getDataAt(z.getElementIndexByName("method"));
        int nx = (int) z.getDataAt(z.getElementIndexByName("n"));
        RDoubleVector x = (RDoubleVector) z.getDataAt(z.getElementIndexByName("x"));
        RDoubleVector y = (RDoubleVector) z.getDataAt(z.getElementIndexByName("y"));
        RDoubleVector b = (RDoubleVector) z.getDataAt(z.getElementIndexByName("b"));
        RDoubleVector c = (RDoubleVector) z.getDataAt(z.getElementIndexByName("c"));
        RDoubleVector d = (RDoubleVector) z.getDataAt(z.getElementIndexByName("d"));

        splineEval(method, nu, xout.getReadonlyData(), yout, nx, x.getReadonlyData(), y.getReadonlyData(), b.getReadonlyData(), c.getReadonlyData(),
                        d.getReadonlyData());
        return RDataFactory.createDoubleVector(yout, xout.isComplete() && x.isComplete() && y.isComplete());
    }

    private static void splineEval(int method, int nu, double[] u, double[] v, int n, double[] x, double[] y, double[] b, double[] c, double[] d) {
        /*
         * Evaluate v[l] := spline(u[l], ...), l = 1,..,nu, i.e. 0:(nu-1) Nodes x[i], coef (y[i];
         * b[i],c[i],d[i]); i = 1,..,n , i.e. 0:(*n-1)
         */
        final int nm1 = n - 1;
        int i;
        int j;
        int k;
        int l;
        double ul;
        double dx;
        double tmp;

        if (method == 1 && n > 1) { /* periodic */
            dx = x[nm1] - x[0];
            for (l = 0; l < nu; l++) {
                v[l] = RMath.fmod(u[l] - x[0], dx);
                if (v[l] < 0.0) {
                    v[l] += dx;
                }
                v[l] += x[0];
            }
        } else {
            for (l = 0; l < nu; l++) {
                v[l] = u[l];
            }
        }

        for (l = 0, i = 0; l < nu; l++) {
            ul = v[l];
            if (ul < x[i] || (i < nm1 && x[i + 1] < ul)) {
                /* reset i such that x[i] <= ul <= x[i+1] : */
                i = 0;
                j = n;
                do {
                    k = (i + j) / 2;
                    if (ul < x[k]) {
                        j = k;
                    } else {
                        i = k;
                    }
                } while (j > i + 1);
            }
            dx = ul - x[i];
            /* for natural splines extrapolate linearly left */
            tmp = (method == 2 && ul < x[0]) ? 0.0 : d[i];

            v[l] = y[i] + dx * (b[i] + dx * (c[i] + dx * tmp));
        }
    }
}
