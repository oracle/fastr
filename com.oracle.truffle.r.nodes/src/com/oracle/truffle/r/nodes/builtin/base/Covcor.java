/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

/*
 * Logic derived from GNU-R, library/stats/src/cov.c
 */
public class Covcor {

    public static NACheck check = new NACheck();

    public static RDoubleVector cor(RDoubleVector x, RDoubleVector y, boolean iskendall, SourceSection source) {
        return corcov(x, y, iskendall, true, source);
    }

    public static RDoubleVector cov(RDoubleVector x, RDoubleVector y, boolean iskendall, SourceSection source) {
        return corcov(x, y, iskendall, false, source);
    }

    @com.oracle.truffle.api.CompilerDirectives.SlowPath
    private static RDoubleVector corcov(RDoubleVector x, RDoubleVector y, boolean iskendall, boolean cor, SourceSection source) {
        boolean ansmat;
        boolean naFail;
        boolean sd0;
        boolean emptyErr;
        int n;
        int ncx;
        int ncy;

        ansmat = isMatrix(x);
        if (ansmat) {
            n = nrows(x);
            ncx = ncols(x);
        } else {
            n = length(x);
            ncx = 1;
        }

        if (isMatrix(y)) {
            if (nrows(y) != n) {
                error("incompatible dimensions");
            }
            ncy = ncols(y);
            ansmat = true;
        } else {
            if (length(y) != n) {
                error("incompatible dimensions");
            }
            ncy = 1;
        }

        /* "default: complete" (easier for -Wall) */
        naFail = false;
        emptyErr = true;

        // case 1: /* use all : no NAs */
        naFail = true;

        if (emptyErr && lENGTH(x) == 0) {
            error("'x' is empty");
        }

        double[] answerData = new double[ncx * ncy];

        // else if (!pair) { /* all | complete */
        double[] xm = new double[ncx];
        double[] ym = new double[ncy];
        RIntVector ind = RDataFactory.createIntVector(n);

        complete2(n, ncx, ncy, x, y, ind, naFail);
        sd0 = covComplete2(n, ncx, ncy, x, y, xm, ym, ind, answerData, cor, iskendall);

        if (sd0) { /* only in cor() */
            RError.warning(source, RError.SD_ZERO);
        }

        RDoubleVector ans = null;
        if (isMatrix(x)) {
            ans = RDataFactory.createDoubleVector(answerData, true, new int[]{ncx, ncy});
        } else {
            ans = RDataFactory.createDoubleVector(answerData, false);
        }
        return ans;
    }

    private static int ncols(RDoubleVector x) {
        assert isMatrix(x);
        return x.getDimensions()[1];
    }

    private static int nrows(RDoubleVector x) {
        assert isMatrix(x);
        return x.getDimensions()[0];
    }

    private static void complete2(int n, int ncx, int ncy, RDoubleVector x, RDoubleVector y, RIntVector ind, boolean naFail) {
        int i;
        int j;
        for (i = 0; i < n; i++) {
            ind.updateDataAt(i, 1, check);
        }
        for (j = 0; j < ncx; j++) {
            // z = &x[j * n];
            for (i = 0; i < n; i++) {
                if (Double.isNaN(x.getDataAt(j * n + i))) {
                    if (naFail) {
                        error("missing observations in cov/cor");
                    } else {
                        ind.updateDataAt(i, 0, check);
                    }
                }
            }
        }

        for (j = 0; j < ncy; j++) {
            // z = &y[j * n];
            for (i = 0; i < n; i++) {
                if (Double.isNaN(y.getDataAt(j * n + i))) {
                    if (naFail) {
                        error("missing observations in cov/cor");
                    } else {
                        ind.updateDataAt(i, 0, check);
                    }
                }
            }
        }
    }

    private static boolean covComplete2(int n, int ncx, int ncy, RDoubleVector x, RDoubleVector y, double[] xm, double[] ym, RIntVector indInput, double[] ans, boolean cor, boolean kendall) {
        int n1 = -1;
        int nobs;
        boolean isSd0 = false;

        /* total number of complete observations */
        nobs = 0;
        for (int k = 0; k < n; k++) {
            if (indInput.getDataAt(k) != 0) {
                nobs++;
            }
        }
        if (nobs <= 1) { /* too many missing */
            for (int i = 0; i < ans.length; i++) {
                ans[i] = RRuntime.DOUBLE_NA;
            }
            return isSd0;
        }

        RIntVector ind = indInput;
        if (nobs == ind.getLength()) {
            // No values of ind are zeroed.
            ind = null;
        }

        if (!kendall) {
            mean(x, xm, ind, n, ncx, nobs);
            mean(y, ym, ind, n, ncy, nobs);
            n1 = nobs - 1;
        }
        for (int i = 0; i < ncx; i++) {
            if (!kendall) {
                double xxm = xm[i];
                for (int j = 0; j < ncy; j++) {
                    double yym = ym[j];
                    double sum = 0;
                    for (int k = 0; k < n; k++) {
                        if (ind == null || ind.getDataAt(k) != 0) {
                            sum += (x.getDataAt(i * n + k) - xxm) * (y.getDataAt(j * n + k) - yym);
                        }
                    }
                    ans[i + j * ncx] = (sum / n1);
                }
            } else { /* Kendall's tau */
                throw new UnsupportedOperationException("kendall's unsupported");
            }
        }

        if (cor) {
            covsdev(x, xm, ind, n, ncx, n1, kendall); /* -> xm[.] */
            covsdev(y, ym, ind, n, ncy, n1, kendall); /* -> ym[.] */

            for (int i = 0; i < ncx; i++) {
                for (int j = 0; j < ncy; j++) {
                    double divisor = (xm[i] * ym[j]);
                    if (divisor == 0.0) {
                        isSd0 = true;
                        ans[i + j * ncx] = RRuntime.DOUBLE_NA;
                    } else {
                        double value = ans[i + j * ncx] / divisor;
                        if (value > 1) {
                            value = 1;
                        }
                        ans[i + j * ncx] = value;
                    }
                }
            }
        }
        return isSd0;
    }

    private static void covsdev(RDoubleVector vector, double[] vectorM, RIntVector ind, int n, int len, int n1, boolean kendall) {
        for (int i = 0; i < len; i++) {
            double sum = 0;
            if (!kendall) {
                double xxm = vectorM[i];
                for (int k = 0; k < n; k++) {
                    if (ind == null || ind.getDataAt(k) != 0) {
                        double value = vector.getDataAt(i * n + k);
                        sum += (value - xxm) * (value - xxm);
                    }
                }
                sum /= n1;
            } else { /* Kendall's tau */
                throw new UnsupportedOperationException("kenall's unsupported");
            }
            vectorM[i] = Math.sqrt(sum);
        }
    }

    private static void mean(RDoubleVector vector, double[] vectorM, RIntVector ind, int n, int len, int nobs) {
        /* variable means */
        for (int i = 0; i < len; i++) {
            double sum = 0.0;
            for (int k = 0; k < n; k++) {
                if (ind == null || ind.getDataAt(k) != 0) {
                    sum += vector.getDataAt(i * n + k);
                }
            }
            double tmp = sum / nobs;
            if (!Double.isInfinite(tmp)) {
                sum = 0.0;
                for (int k = 0; k < n; k++) {
                    if (ind == null || ind.getDataAt(k) != 0) {
                        sum += (vector.getDataAt(i * n + k) - tmp);
                    }
                }
                tmp = tmp + sum / nobs;
            }
            vectorM[i] = tmp;
        }
    }

    private static boolean isMatrix(RAbstractVector vector) {
        return RRuntime.isMatrix(vector);
    }

    private static int lENGTH(RAbstractVector v) {
        return length(v);
    }

    private static int length(RAbstractVector v) {
        return v.getLength();
    }

    private static void error(String string) {
        throw new UnsupportedOperationException("error: " + string);
    }

}
