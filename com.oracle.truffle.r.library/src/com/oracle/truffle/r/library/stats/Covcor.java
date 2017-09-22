/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctionsFactory.SetDimNamesAttributeNodeGen;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.unary.IsFactorNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.nmath.RMath;

/*
 * Logic derived from GNU-R, library/stats/src/cov.c
 */
public abstract class Covcor extends RExternalBuiltinNode.Arg4 {
    // Checkstyle: stop method name check

    private static RuntimeException error(String message) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(RError.SHOW_CALLER, Message.GENERIC, message);
    }

    private static double ANS(double[] ans, int ncx, int i, int j) {
        return ans[i + j * ncx];
    }

    private static void ANS(double[] ans, int ncx, int i, int j, double value) {
        ans[i + j * ncx] = value;
    }

    private static double CLAMP(double X) {
        return (X >= 1 ? 1 : (X <= -1 ? -1 : X));
    }

    private static boolean ISNAN(double v) {
        return Double.isNaN(v);
    }

    /*
     * Note that "if (kendall)" and "if (cor)" are used inside a double for() loop; which makes the
     * code better readable -- and is hopefully dealt with by a smartly optimizing compiler
     */

    /**
     * Compute Cov(xx[], yy[]) or Cor(.,.) with n = length(xx)
     */
    private static void COV_PAIRWISE_BODY(double[] ans, int n, int ncx, int i, int j, double[] x, double[] y, int xx, int yy, boolean[] sd_0, boolean cor, boolean kendall) {
        double xmean = 0, ymean = 0;
        int nobs = 0;
        if (!kendall) {
            for (int k = 0; k < n; k++) {
                if (!(ISNAN(x[xx + k]) || ISNAN(y[yy + k]))) {
                    nobs++;
                    xmean += x[xx + k];
                    ymean += y[yy + k];
                }
            }
        } else /* kendall */
            for (int k = 0; k < n; k++) {
                if (!(ISNAN(x[xx + k]) || ISNAN(y[yy + k]))) {
                    nobs++;
                }
            }

        if (nobs >= 2) {
            int n1 = -1;
            double xsd = 0, ysd = 0, sum = 0;
            if (!kendall) {
                xmean /= nobs;
                ymean /= nobs;
                n1 = nobs - 1;
            }
            for (int k = 0; k < n; k++) {
                if (!(ISNAN(x[xx + k]) || ISNAN(y[yy + k]))) {
                    if (!kendall) {
                        double xm = x[xx + k] - xmean;
                        double ym = y[yy + k] - ymean;

                        sum += xm * ym;
                        if (cor) {
                            xsd += xm * xm;
                            ysd += ym * ym;
                        }
                    }

                    else { /* Kendall's tau */
                        for (n1 = 0; n1 < k; n1++) {
                            if (!(ISNAN(x[xx + n1]) || ISNAN(y[yy + n1]))) {
                                double xm = RMath.sign(x[xx + k] - x[xx + n1]);
                                double ym = RMath.sign(y[yy + k] - y[yy + n1]);

                                sum += xm * ym;
                                if (cor) {
                                    xsd += xm * xm;
                                    ysd += ym * ym;
                                }
                            }
                        }
                    }
                }
            }
            if (cor) {
                if (xsd == 0 || ysd == 0) {
                    sd_0[0] = true;
                    sum = RRuntime.DOUBLE_NA;
                } else {
                    if (!kendall) {
                        xsd /= n1;
                        ysd /= n1;
                        sum /= n1;
                    }
                    sum /= (Math.sqrt(xsd) * Math.sqrt(ysd));
                    sum = CLAMP(sum);
                }
            } else if (!kendall) {
                sum /= n1;
            }

            ANS(ans, ncx, i, j, sum);
        } else {
            ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
        }
    }

    private static void cov_pairwise1(int n, int ncx, double[] x, double[] ans, boolean[] sd_0, boolean cor, boolean kendall) {
        for (int i = 0; i < ncx; i++) {
            int xx = i * n;
            for (int j = 0; j <= i; j++) {
                int yy = j * n;

                COV_PAIRWISE_BODY(ans, n, ncx, i, j, x, x, xx, yy, sd_0, cor, kendall);

                ANS(ans, ncx, j, i, ANS(ans, ncx, i, j));
            }
        }
    }

    private static void cov_pairwise2(int n, int ncx, int ncy, double[] x, double[] y, double[] ans, boolean[] sd_0, boolean cor, boolean kendall) {
        for (int i = 0; i < ncx; i++) {
            int xx = i * n;
            for (int j = 0; j < ncy; j++) {
                int yy = j * n;

                COV_PAIRWISE_BODY(ans, n, ncx, i, j, x, y, xx, yy, sd_0, cor, kendall);
            }
        }
    }

    /*
     * method = "complete" or "all.obs" (only difference: na_fail): -------- -------
     */

    /* This uses two passes for better accuracy */
    private static void MEAN(int n, int ncx, double[] x, double[] xm, boolean[] ind, int nobs) {
        /* variable means */
        for (int i = 0; i < ncx; i++) {
            int xx = i * n;
            double sum = 0;
            for (int k = 0; k < n; k++) {
                if (ind[k]) {
                    sum += x[xx + k];
                }
            }
            double tmp = sum / nobs;
            if (Double.isFinite(tmp)) {
                sum = 0;
                for (int k = 0; k < n; k++) {
                    if (ind[k]) {
                        sum += (x[xx + k] - tmp);
                    }
                }
                tmp = tmp + sum / nobs;
            }
            xm[i] = tmp;
        }
    }

    /* This uses two passes for better accuracy */
    private static void MEAN_(int n, int ncx, double[] x, double[] xm, boolean[] has_na) {
        /* variable means (has_na) */
        for (int i = 0; i < ncx; i++) {
            double tmp;
            if (has_na[i]) {
                tmp = RRuntime.DOUBLE_NA;
            } else {
                int xx = i * n;
                double sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += x[xx + k];
                }
                tmp = sum / n;
                if (Double.isFinite(tmp)) {
                    sum = 0;
                    for (int k = 0; k < n; k++) {
                        sum += (x[xx + k] - tmp);
                    }
                    tmp = tmp + sum / n;
                }
            }
            xm[i] = tmp;
        }
    }

    private static void cov_complete1(int n, int ncx, double[] x, double[] xm, boolean[] ind, double[] ans, boolean[] sd_0, boolean cor, boolean kendall) {
        int n1 = -1;

        /* total number of complete observations */
        int nobs = 0;
        for (int k = 0; k < n; k++) {
            if (ind[k]) {
                nobs++;
            }
        }
        if (nobs <= 1) {/* too many missing */
            for (int i = 0; i < ncx; i++) {
                for (int j = 0; j < ncx; j++) {
                    ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                }
            }
            return;
        }

        if (!kendall) {
            MEAN(n, ncx, x, xm, ind, nobs); /* -> xm[] */
            n1 = nobs - 1;
        }
        for (int i = 0; i < ncx; i++) {
            int xx = i * n;

            if (!kendall) {
                double xxm = xm[i];
                for (int j = 0; j <= i; j++) {
                    int yy = j * n;
                    double yym = xm[j];
                    double sum = 0;
                    for (int k = 0; k < n; k++) {
                        if (ind[k]) {
                            sum += (x[xx + k] - xxm) * (x[yy + k] - yym);
                        }
                    }
                    double result = sum / n1;
                    ANS(ans, ncx, j, i, result);
                    ANS(ans, ncx, i, j, result);
                }
            } else { /* Kendall's tau */
                for (int j = 0; j <= i; j++) {
                    int yy = j * n;
                    double sum = 0;
                    for (int k = 0; k < n; k++) {
                        if (ind[k]) {
                            for (n1 = 0; n1 < n; n1++) {
                                if (ind[n1]) {
                                    sum += RMath.sign(x[xx + k] - x[xx + n1]) * RMath.sign(x[yy + k] - x[yy + n1]);
                                }
                            }
                        }
                    }
                    ANS(ans, ncx, j, i, sum);
                    ANS(ans, ncx, i, j, sum);
                }
            }
        }

        if (cor) {
            for (int i = 0; i < ncx; i++) {
                xm[i] = Math.sqrt(ANS(ans, ncx, i, i));
            }
            for (int i = 0; i < ncx; i++) {
                for (int j = 0; j < i; j++) {
                    double result;
                    if (xm[i] == 0 || xm[j] == 0) {
                        sd_0[0] = true;
                        result = RRuntime.DOUBLE_NA;
                    } else {
                        double current = ANS(ans, ncx, i, j);
                        if (RRuntime.isNA(current)) {
                            result = RRuntime.DOUBLE_NA;
                        } else {
                            result = CLAMP(current / (xm[i] * xm[j]));
                        }
                    }
                    ANS(ans, ncx, j, i, result);
                    ANS(ans, ncx, i, j, result);
                }
                ANS(ans, ncx, i, i, 1);
            }
        }
    }

    private static void cov_na_1(int n, int ncx, double[] x, double[] xm, boolean[] has_na, double[] ans, boolean[] sd_0, boolean cor, boolean kendall) {
        int n1 = -1;
        if (n <= 1) { /* too many missing */
            for (int i = 0; i < ncx; i++) {
                for (int j = 0; j < ncx; j++) {
                    ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                }
            }
            return;
        }

        if (!kendall) {
            MEAN_(n, ncx, x, xm, has_na);/* -> xm[] */
            n1 = n - 1;
        }
        for (int i = 0; i < ncx; i++) {
            if (has_na[i]) {
                for (int j = 0; j <= i; j++) {
                    ANS(ans, ncx, j, i, RRuntime.DOUBLE_NA);
                    ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                }
            } else {
                int xx = i * n;

                if (!kendall) {
                    double xxm = xm[i];
                    for (int j = 0; j <= i; j++) {
                        if (has_na[j]) {
                            ANS(ans, ncx, j, i, RRuntime.DOUBLE_NA);
                            ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                        } else {
                            int yy = j * n;
                            double yym = xm[j];
                            double sum = 0;
                            for (int k = 0; k < n; k++) {
                                sum += (x[xx + k] - xxm) * (x[yy + k] - yym);
                            }
                            double result = sum / n1;
                            ANS(ans, ncx, j, i, result);
                            ANS(ans, ncx, i, j, result);
                        }
                    }
                } else { /* Kendall's tau */
                    for (int j = 0; j <= i; j++) {
                        if (has_na[j]) {
                            ANS(ans, ncx, j, i, RRuntime.DOUBLE_NA);
                            ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                        } else {
                            int yy = j * n;
                            double sum = 0;
                            for (int k = 0; k < n; k++) {
                                for (n1 = 0; n1 < n; n1++) {
                                    sum += RMath.sign(x[xx + k] - x[xx + n1]) * RMath.sign(x[yy + k] - x[yy + n1]);
                                }
                            }
                            ANS(ans, ncx, j, i, sum);
                            ANS(ans, ncx, i, j, sum);
                        }
                    }
                }
            }
        }

        if (cor) {
            for (int i = 0; i < ncx; i++) {
                if (!has_na[i]) {
                    xm[i] = Math.sqrt(ANS(ans, ncx, i, i));
                }
            }
            for (int i = 0; i < ncx; i++) {
                if (!has_na[i]) {
                    for (int j = 0; j < i; j++) {
                        double result;
                        if (xm[i] == 0 || xm[j] == 0) {
                            sd_0[0] = true;
                            result = RRuntime.DOUBLE_NA;
                        } else {
                            double current = ANS(ans, ncx, i, j);
                            if (RRuntime.isNA(current)) {
                                result = RRuntime.DOUBLE_NA;
                            } else {
                                result = CLAMP(current / (xm[i] * xm[j]));
                            }
                        }
                        ANS(ans, ncx, j, i, result);
                        ANS(ans, ncx, i, j, result);
                    }
                }
                ANS(ans, ncx, i, i, 1);
            }
        }
    }

    private static void COV_SDEV1(int n, int n1, int nc, double[] array, double[] m, boolean[] ind, boolean kendall) {
        for (int i = 0; i < nc; i++) { /* Var(X[i]) */
            int xx = i * n;
            double sum = 0;
            if (!kendall) {
                double xxm = m[i];
                for (int k = 0; k < n; k++) {
                    if (ind[k]) {
                        sum += (array[xx + k] - xxm) * (array[xx + k] - xxm);
                    }
                }
                sum /= n1;
            } else { /* Kendall's tau */
                for (int k = 0; k < n; k++) {
                    if (ind[k]) {
                        for (int n1_ = 0; n1_ < n; n1_++) {
                            if (ind[n1_] && array[xx + k] != array[xx + n1_]) {
                                sum++; /* = sign(. - .)^2 */
                            }
                        }
                    }
                }
            }
            m[i] = Math.sqrt(sum);
        }
    }

    private static void cov_complete2(int n, int ncx, int ncy, double[] x, double[] y, double[] xm, double[] ym, boolean[] ind, double[] ans, boolean[] sd_0, boolean cor, boolean kendall) {
        int n1 = -1;

        /* total number of complete observations */
        int nobs = 0;
        for (int k = 0; k < n; k++) {
            if (ind[k]) {
                nobs++;
            }
        }
        if (nobs <= 1) {/* too many missing */
            for (int i = 0; i < ncx; i++) {
                for (int j = 0; j < ncy; j++) {
                    ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                }
            }
            return;
        }

        if (!kendall) {
            MEAN(n, ncx, x, xm, ind, nobs);/* -> xm[] */
            MEAN(n, ncy, y, ym, ind, nobs);/* -> ym[] */
            n1 = nobs - 1;
        }
        for (int i = 0; i < ncx; i++) {
            int xx = i * n;
            if (!kendall) {
                double xxm = xm[i];
                for (int j = 0; j < ncy; j++) {
                    int yy = j * n;
                    double yym = ym[j];
                    double sum = 0;
                    for (int k = 0; k < n; k++) {
                        if (ind[k]) {
                            sum += (x[xx + k] - xxm) * (y[yy + k] - yym);
                        }
                    }
                    ANS(ans, ncx, i, j, sum / n1);
                }
            } else { /* Kendall's tau */
                for (int j = 0; j < ncy; j++) {
                    int yy = j * n;
                    double sum = 0;
                    for (int k = 0; k < n; k++) {
                        if (ind[k]) {
                            for (n1 = 0; n1 < n; n1++) {
                                if (ind[n1]) {
                                    sum += RMath.sign(x[xx + k] - x[xx + n1]) * RMath.sign(y[yy + k] - y[yy + n1]);
                                }
                            }
                        }
                    }
                    ANS(ans, ncx, i, j, sum);
                }
            }
        }

        if (cor) {

            COV_SDEV1(n, n1, ncx, x, xm, ind, kendall); /* -> xm[.] */
            COV_SDEV1(n, n1, ncy, y, ym, ind, kendall); /* -> ym[.] */

            for (int i = 0; i < ncx; i++) {
                for (int j = 0; j < ncy; j++) {
                    double result;
                    if (xm[i] == 0 || ym[j] == 0) {
                        sd_0[0] = true;
                        result = RRuntime.DOUBLE_NA;
                    } else {
                        double current = ANS(ans, ncx, i, j);
                        if (RRuntime.isNA(current)) {
                            result = RRuntime.DOUBLE_NA;
                        } else {
                            result = CLAMP(current / (xm[i] * ym[j]));
                        }
                    }
                    ANS(ans, ncx, i, j, result);
                }
            }
        }
    }

    private static void COV_SDEV2(int n, int n1, int nc, double[] array, double[] m, boolean[] has_na, boolean kendall) {
        for (int i = 0; i < nc; i++) {
            if (!has_na[i]) { /* Var(X[j]) */
                int xx = i * n;
                double sum = 0;
                if (!kendall) {
                    double xxm = m[i];
                    for (int k = 0; k < n; k++) {
                        sum += (array[xx + k] - xxm) * (array[xx + k] - xxm);
                    }
                    sum /= n1;
                } else { /* Kendall's tau */
                    for (int k = 0; k < n; k++) {
                        for (int n1_ = 0; n1_ < n; n1_++) {
                            if (array[xx + k] != array[xx + n1_]) {
                                sum++; /* = sign(. - .)^2 */
                            }
                        }
                    }
                }
                m[i] = Math.sqrt(sum);
            }
        }
    }

    private static void cov_na_2(int n, int ncx, int ncy, double[] x, double[] y, double[] xm, double[] ym, boolean[] has_na_x, boolean[] has_na_y, double[] ans, boolean[] sd_0, boolean cor,
                    boolean kendall) {
        int n1 = -1;
        if (n <= 1) {/* too many missing */
            for (int i = 0; i < ncx; i++) {
                for (int j = 0; j < ncy; j++) {
                    ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                }
            }
            return;
        }

        if (!kendall) {
            MEAN_(n, ncx, x, xm, has_na_x);/* -> xm[] */
            MEAN_(n, ncy, y, ym, has_na_y);/* -> ym[] */
            n1 = n - 1;
        }
        for (int i = 0; i < ncx; i++) {
            if (has_na_x[i]) {
                for (int j = 0; j < ncy; j++) {
                    ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                }
            } else {
                int xx = i * n;
                if (!kendall) {
                    double xxm = xm[i];
                    for (int j = 0; j < ncy; j++) {
                        if (has_na_y[j]) {
                            ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                        } else {
                            int yy = j * n;
                            double yym = ym[j];
                            double sum = 0;
                            for (int k = 0; k < n; k++) {
                                sum += (x[xx + k] - xxm) * (y[yy + k] - yym);
                            }
                            ANS(ans, ncx, i, j, sum / n1);
                        }
                    }
                } else { /* Kendall's tau */
                    for (int j = 0; j < ncy; j++) {
                        if (has_na_y[j]) {
                            ANS(ans, ncx, i, j, RRuntime.DOUBLE_NA);
                        } else {
                            int yy = j * n;
                            double sum = 0;
                            for (int k = 0; k < n; k++) {
                                for (n1 = 0; n1 < n; n1++) {
                                    sum += RMath.sign(x[xx + k] - x[xx + n1]) * RMath.sign(y[yy + k] - y[yy + n1]);
                                }
                            }
                            ANS(ans, ncx, i, j, sum);
                        }
                    }
                }
            }
        }

        if (cor) {

            COV_SDEV2(n, n1, ncx, x, xm, has_na_x, kendall); /* -> xm[.] */
            COV_SDEV2(n, n1, ncy, y, ym, has_na_y, kendall); /* -> ym[.] */

            for (int i = 0; i < ncx; i++) {
                if (!has_na_x[i]) {
                    for (int j = 0; j < ncy; j++) {
                        if (!has_na_y[j]) {
                            double result;
                            if (xm[i] == 0 || ym[j] == 0) {
                                sd_0[0] = true;
                                result = RRuntime.DOUBLE_NA;
                            } else {
                                double current = ANS(ans, ncx, i, j);
                                if (RRuntime.isNA(current)) {
                                    result = RRuntime.DOUBLE_NA;
                                } else {
                                    result = CLAMP(current / (xm[i] * ym[j]));
                                }
                            }
                            ANS(ans, ncx, i, j, result);
                        }
                    }
                }
            }
        }
    }

    /*
     * complete[12]() returns indicator vector ind[] of complete.cases(), or --------------
     * if(na_fail) signals error if any NA/NaN is encountered
     */

    /*
     * This might look slightly inefficient, but it is designed to optimise paging in virtual memory
     * systems ... (or at least that's my story, and I'm sticking to it.)
     */
    private static void NA_LOOP(int n, int z, double[] x, boolean[] ind, boolean na_fail) {
        for (int i = 0; i < n; i++) {
            if (ISNAN(x[z + i])) {
                if (na_fail) {
                    error("missing observations in cov/cor");
                } else {
                    ind[i] = false;
                }
            }
        }
    }

    private static void complete1(int n, int ncx, double[] x, boolean[] ind, boolean na_fail) {
        for (int i = 0; i < n; i++) {
            ind[i] = true;
        }
        for (int j = 0; j < ncx; j++) {
            int z = j * n;
            NA_LOOP(n, z, x, ind, na_fail);
        }
    }

    static void complete2(int n, int ncx, int ncy, double[] x, double[] y, boolean[] ind, boolean na_fail) {
        complete1(n, ncx, x, ind, na_fail);

        for (int j = 0; j < ncy; j++) {
            int z = j * n;
            NA_LOOP(n, z, y, ind, na_fail);
        }
    }

    static void find_na_1(int n, int ncx, double[] x, boolean[] has_na) {
        for (int j = 0; j < ncx; j++) {
            int z = j * n;
            has_na[j] = false;
            for (int i = 0; i < n; i++) {
                if (ISNAN(x[z + i])) {
                    has_na[j] = true;
                    break;
                }
            }
        }
    }

    static void find_na_2(int n, int ncx, int ncy, double[] x, double[] y, boolean[] has_na_x, boolean[] has_na_y) {
        find_na_1(n, ncx, x, has_na_x);
        find_na_1(n, ncy, y, has_na_y);
    }

    /*
     * co[vr](x, y, use = { 1, 2, 3, 4, 5 } "all.obs", "complete.obs", "pairwise.complete",
     * "everything", "na.or.complete" kendall = TRUE/FALSE)
     */
    public RDoubleVector corcov(RDoubleVector x, RDoubleVector y, int method, boolean kendall) throws RError {
        int n, ncx, ncy;

        /* Arg.1: x */
        if (isFactorX.executeIsFactor(x)) {
            error("'x' is a factor");
            // maybe only warning: "Calling var(x) on a factor x is deprecated and will become an
            // error.\n Use something like 'all(duplicated(x)[-1L])' to test for a constant vector."
        }
        /* length check of x -- only if(empty_err) --> below */
        int[] xDims = getDimsXNode.getDimensions(x);
        boolean ansmat = matrixProfile.profile(GetDimAttributeNode.isMatrix(xDims));
        if ((ansmat)) {
            n = xDims[0];
            ncx = xDims[1];
        } else {
            n = x.getLength();
            ncx = 1;
        }
        /* Arg.2: y */
        if (y == null) {/* y = x : var() */
            ncy = ncx;
        } else {
            if (isFactorY.executeIsFactor(y)) {
                error("'y' is a factor");
                // maybe only warning: "Calling var(x) on a factor x is deprecated and will become
                // an error.\n Use something like 'all(duplicated(x)[-1L])' to test for a constant
                // vector."
            }
            int[] yDims = getDimsYNode.getDimensions(y);
            if (GetDimAttributeNode.isMatrix(yDims)) {
                if (yDims[0] != n) {
                    error("incompatible dimensions");
                }
                ncy = yDims[1];
                ansmat = true;
            } else {
                if (y.getLength() != n) {
                    error("incompatible dimensions");
                }
                ncy = 1;
            }
        }

        /* "default: complete" */
        boolean na_fail = false;
        boolean everything = false;
        boolean empty_err = true;
        boolean pair = false;
        switch (method) {
            case 1: /* use all : no NAs */
                na_fail = true;
                break;
            case 2: /* complete */
                /* did na.omit in R */
                if (x.getLength() == 0) {
                    error("no complete element pairs");
                }
                break;
            case 3: /* pairwise.complete */
                pair = true;
                break;
            case 4: /* "everything": NAs are propagated */
                everything = true;
                empty_err = false;
                break;
            case 5: /* "na.or.complete": NAs are propagated */
                empty_err = false;
                break;
            default:
                error("invalid 'use' (computational method)");
        }
        if (empty_err && x.getLength() == 0) {
            error("'x' is empty");
        }

        double[] xData = getReadonlyDataNode.execute(x);
        double[] ans = new double[ncx * ncy];
        boolean[] sd_0 = new boolean[1];

        evaluate(y, kendall, isCor, n, ncx, ncy, na_fail, everything, empty_err, pair, xData, ans, sd_0);

        if (sd_0[0]) { /* only in cor() */
            warning(RError.Message.SD_ZERO);
        }

        boolean seenNA = false;
        for (int i = 0; i < ans.length; i++) {
            if (RRuntime.isNA(ans[i])) {
                naInRes.enter();
                seenNA = true;
                break;
            }
        }

        if (ansmat) { /* set dimnames() when applicable */
            RList newDimNames = null;
            if (y == null) {
                RList dimNames = getDimsNamesXNode.getDimNames(x);
                if (dimNames != null) {
                    Object names = dimNames.getDataAt(1);
                    if (names != RNull.instance) {
                        newDimNames = RDataFactory.createList(new Object[]{names, names});
                    }
                }
            } else {
                RList dimNamesX = getDimsNamesXNode.getDimNames(x);
                RList dimNamesY = getDimsNamesYNode.getDimNames(y);
                Object namesX = dimNamesX.getLength() >= 2 ? dimNamesX.getDataAt(1) : RNull.instance;
                Object namesY = dimNamesY.getLength() >= 2 ? dimNamesY.getDataAt(1) : RNull.instance;
                if (namesX != RNull.instance || namesY != RNull.instance) {
                    newDimNames = RDataFactory.createList(new Object[]{namesX, namesY});
                }
            }
            RDoubleVector result = RDataFactory.createDoubleVector(ans, !seenNA, new int[]{ncx, ncy});
            if (newDimNames != null) {
                setDimNamesNode.setDimNames(result, newDimNames);
            }
            return result;
        } else {
            return RDataFactory.createDoubleVector(ans, !seenNA);
        }
    }

    @TruffleBoundary
    private static void evaluate(RDoubleVector y, boolean kendall, boolean cor, int n, int ncx, int ncy, boolean na_fail, boolean everything, boolean empty_err, boolean pair, double[] xData,
                    double[] ans, boolean[] sd_0) {
        if (y == null) {
            if (everything) { /* NA's are propagated */
                double[] xm = new double[ncx];
                boolean[] ind = new boolean[ncx];
                find_na_1(n, ncx, xData, /* --> has_na[] = */ ind);
                cov_na_1(n, ncx, xData, xm, ind, ans, sd_0, cor, kendall);
            } else if (!pair) { /* all | complete "var" */
                double[] xm = new double[ncx];
                boolean[] ind = new boolean[n];
                complete1(n, ncx, xData, ind, na_fail);
                cov_complete1(n, ncx, xData, xm, ind, ans, sd_0, cor, kendall);
                if (empty_err) {
                    boolean indany = false;
                    for (int i = 0; i < n; i++) {
                        if (ind[i]) {
                            indany = true;
                            break;
                        }
                    }
                    if (!indany) {
                        error("no complete element pairs");
                    }
                }
            } else { /* pairwise "var" */
                cov_pairwise1(n, ncx, xData, ans, sd_0, cor, kendall);
            }
        } else { /* Co[vr] (x, y) */
            double[] yData = y.getReadonlyData();
            if (everything) {
                double[] xm = new double[ncx];
                double[] ym = new double[ncy];
                boolean[] ind = new boolean[ncx];
                boolean[] has_na_y = new boolean[ncy];
                find_na_2(n, ncx, ncy, xData, yData, ind, has_na_y);
                cov_na_2(n, ncx, ncy, xData, yData, xm, ym, ind, has_na_y, ans, sd_0, cor, kendall);
            } else if (!pair) { /* all | complete */
                double[] xm = new double[ncx];
                double[] ym = new double[ncy];
                boolean[] ind = new boolean[n];
                complete2(n, ncx, ncy, xData, yData, ind, na_fail);
                cov_complete2(n, ncx, ncy, xData, yData, xm, ym, ind, ans, sd_0, cor, kendall);
                if (empty_err) {
                    boolean indany = false;
                    for (int i = 0; i < n; i++) {
                        if (ind[i]) {
                            indany = true;
                            break;
                        }
                    }
                    if (!indany) {
                        error("no complete element pairs");
                    }
                }
            } else { /* pairwise */
                cov_pairwise2(n, ncx, ncy, xData, yData, ans, sd_0, cor, kendall);
            }
        }
    }

    private final boolean isCor;

    public Covcor(boolean isCor) {
        this.isCor = isCor;
    }

    static {
        Casts casts = new Casts(Covcor.class);
        casts.arg(0).mustNotBeMissing().mustBe(nullValue().not(), Message.IS_NULL, "x").asDoubleVector();
        casts.arg(1).mustNotBeMissing().asDoubleVector();
        casts.arg(2).asIntegerVector().findFirst();
        casts.arg(3).asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    public Object call(RAbstractDoubleVector x, @SuppressWarnings("unused") RNull y, int method, boolean iskendall) {
        return corcov(x.materialize(), null, method, iskendall);
    }

    @Specialization
    public Object call(RAbstractDoubleVector x, RAbstractDoubleVector y, int method, boolean iskendall) {
        return corcov(x.materialize(), y.materialize(), method, iskendall);
    }

    private final BranchProfile naInRes = BranchProfile.create();
    private final ConditionProfile matrixProfile = ConditionProfile.createBinaryProfile();

    @Child private GetReadonlyData.Double getReadonlyDataNode = GetReadonlyData.Double.create();
    @Child private GetDimAttributeNode getDimsXNode = GetDimAttributeNode.create();
    @Child private GetDimAttributeNode getDimsYNode = GetDimAttributeNode.create();
    @Child private GetDimNamesAttributeNode getDimsNamesXNode = GetDimNamesAttributeNode.create();
    @Child private GetDimNamesAttributeNode getDimsNamesYNode = GetDimNamesAttributeNode.create();
    @Child private SetDimNamesAttributeNode setDimNamesNode = SetDimNamesAttributeNodeGen.create();
    @Child private IsFactorNode isFactorX = new IsFactorNode();
    @Child private IsFactorNode isFactorY = new IsFactorNode();
}
