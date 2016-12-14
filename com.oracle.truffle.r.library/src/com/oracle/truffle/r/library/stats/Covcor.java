/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.eq;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.NO_CALLER;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/*
 * Logic derived from GNU-R, library/stats/src/cov.c
 */
public abstract class Covcor extends RExternalBuiltinNode.Arg4 {

    private final boolean isCor;

    public Covcor(boolean isCor) {
        this.isCor = isCor;
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(0).mustNotBeNull(SHOW_CALLER, Message.IS_NULL, "x").asDoubleVector();
        casts.arg(1).allowNull().asDoubleVector();
        casts.arg(2).asIntegerVector().findFirst().mustBe(eq(4), this, Message.NYI, "covcor: other method than 4 not implemented.");
        casts.arg(3).asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    public Object call(RAbstractDoubleVector x, @SuppressWarnings("unused") RNull y, int method, boolean iskendall) {
        return corcov(x.materialize(), null, method, iskendall, this);
    }

    @Specialization
    public Object call(RAbstractDoubleVector x, RAbstractDoubleVector y, int method, boolean iskendall) {
        return corcov(x.materialize(), y.materialize(), method, iskendall, this);
    }

    private final NACheck check = NACheck.create();

    private final ConditionProfile noNAXProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noNAYProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile xCompleteProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile yCompleteProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile bothZeroProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile tooManyMissing = BranchProfile.create();
    private final BranchProfile naInRes = BranchProfile.create();
    private final BranchProfile error = BranchProfile.create();
    private final BranchProfile warning = BranchProfile.create();

    @Child private GetDimAttributeNode getDimsNode = GetDimAttributeNode.create();

    private final LoopConditionProfile loopLength = LoopConditionProfile.createCountingProfile();

    public RDoubleVector corcov(RDoubleVector x, RDoubleVector y, @SuppressWarnings("unused") int method, boolean iskendall, RBaseNode invokingNode) throws RError {
        boolean ansmat;
        boolean naFail;
        boolean everything;
        boolean sd0;
        boolean emptyErr;
        int n;
        int ncx;
        int ncy;

        ansmat = x.isMatrix();
        if (ansmat) {
            n = nrows(x);
            ncx = ncols(x);
        } else {
            n = x.getLength();
            ncx = 1;
        }

        if (y == null) {
            ncy = ncx;
        } else if (y.isMatrix()) {
            if (nrows(y) != n) {
                error.enter();
                error("incompatible dimensions");
            }
            ncy = ncols(y);
            ansmat = true;
        } else {
            if (y.getLength() != n) {
                error.enter();
                error("incompatible dimensions");
            }
            ncy = 1;
        }

        // TODO adopt full use semantics

        /* "default: complete" (easier for -Wall) */
        naFail = false;
        everything = false;
        emptyErr = true;

        // case 4: /* "everything": NAs are propagated */
        everything = true;
        emptyErr = false;

        if (emptyErr && x.getLength() == 0) {
            error.enter();
            error("'x' is empty");
        }

        double[] answerData = new double[ncx * ncy];

        double[] xm = new double[ncx];
        if (y == null) {
            if (everything) {
                sd0 = covNA1(n, ncx, x, xm, answerData, isCor, iskendall);
            } else {
                RIntVector ind = RDataFactory.createIntVector(n);
                complete1(n, ncx, x, ind, naFail);
                sd0 = covComplete1(n, ncx, x, xm, ind, answerData, isCor, iskendall);
            }
        } else {
            double[] ym = new double[ncy];
            if (everything) {
                sd0 = covNA2(n, ncx, ncy, x, y, xm, ym, answerData, isCor, iskendall);
            } else {
                RIntVector ind = RDataFactory.createIntVector(n);
                complete2(n, ncx, ncy, x, y, ind, naFail);
                sd0 = covComplete2(n, ncx, ncy, x, y, xm, ym, ind, answerData, isCor, iskendall);
            }
        }

        if (sd0) { /* only in cor() */
            warning.enter();
            RError.warning(invokingNode, RError.Message.SD_ZERO);
        }

        boolean seenNA = false;
        for (int i = 0; i < answerData.length; i++) {
            if (RRuntime.isNA(answerData[i])) {
                naInRes.enter();
                seenNA = true;
                break;
            }
        }

        RDoubleVector ans = null;
        if (x.isMatrix()) {
            ans = RDataFactory.createDoubleVector(answerData, !seenNA, new int[]{ncx, ncy});
        } else {
            ans = RDataFactory.createDoubleVector(answerData, !seenNA);
        }
        return ans;
    }

    private int ncols(RDoubleVector x) {
        assert x.isMatrix();
        return getDimsNode.getDimensions(x)[1];
    }

    private int nrows(RDoubleVector x) {
        assert x.isMatrix();
        return getDimsNode.getDimensions(x)[0];
    }

    private void complete1(int n, int ncx, RDoubleVector x, RIntVector ind, boolean naFail) {
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
    }

    private void complete2(int n, int ncx, int ncy, RDoubleVector x, RDoubleVector y, RIntVector ind, boolean naFail) {
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

    private static boolean covComplete1(int n, int ncx, RDoubleVector x, double[] xm, RIntVector indInput, double[] ans, boolean cor, boolean kendall) {
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
            n1 = nobs - 1;
        }
        for (int i = 0; i < ncx; i++) {
            if (!kendall) {
                double xxm = xm[i];
                for (int j = 0; j <= i; j++) {
                    double yym = xm[j];
                    double sum = 0.0;
                    for (int k = 0; k < n; k++) {
                        if (ind == null || ind.getDataAt(k) != 0) {
                            sum += (x.getDataAt(i * n + k) - xxm) * (x.getDataAt(j * n + k) - yym);
                        }
                    }
                    double r = sum / n1;
                    ans[i + j * ncx] = r;
                    ans[j + i * ncx] = r;
                }
            } else { /* Kendall's tau */
                throw new UnsupportedOperationException("kendall's unsupported");
            }
        }

        if (cor) {
            for (int i = 0; i < ncx; i++) {
                xm[i] = Math.sqrt(ans[i + i * ncx]);
            }
            for (int i = 0; i < ncx; i++) {
                for (int j = 0; j < i; j++) {
                    if (xm[i] == 0 || xm[j] == 0) {
                        isSd0 = true;
                        ans[i + j * ncx] = RRuntime.DOUBLE_NA;
                        ans[j + i * ncx] = RRuntime.DOUBLE_NA;
                    } else {
                        double sum = ans[i + j * ncx] / (xm[i] * xm[j]);
                        if (sum > 1.0) {
                            sum = 1.0;
                        }
                        ans[i + j * ncx] = sum;
                        ans[j + i * ncx] = sum;
                    }
                }
                ans[i + i * ncx] = 1.0;
            }
        }

        return isSd0;
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
                throw new UnsupportedOperationException("kendall's unsupported");
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

    private static boolean[] findNAs(int n, int nc, RDoubleVector v) {
        boolean[] hasNA = new boolean[nc];
        double[] data = v.getDataWithoutCopying();
        for (int j = 0; j < nc; j++) {
            for (int i = 0; i < n; i++) {
                if (Double.isNaN(data[j * n + i])) {
                    hasNA[j] = true;
                    break;
                }
            }
        }
        return hasNA;
    }

    private boolean covNA1(int n, int ncx, RDoubleVector x, double[] xm, double[] ans, boolean cor, boolean iskendall) {
        double sum;
        double xxm;
        double yym;
        int n1 = -1;
        boolean sd0 = false;

        double[] xData = x.getDataWithoutCopying();
        boolean[] hasNAx = findNAs(n, ncx, x);

        if (n <= 1) { /* too many missing */
            tooManyMissing.enter();
            Arrays.fill(ans, RRuntime.DOUBLE_NA);
            return sd0;
        }

        if (!iskendall) {
            if (xCompleteProfile.profile(x.isComplete())) {
                meanNoNA(n, ncx, xData, xm, hasNAx);
            } else {
                mean(n, ncx, xData, xm, hasNAx);
            }
            n1 = n - 1;
        }

        for (int i = 0; i < ncx; i++) {
            double[] temp = new double[n];
            if (noNAXProfile.profile(!hasNAx[i])) {
                if (!iskendall) {
                    xxm = xm[i];
                    for (int j = 0; j <= i; j++) {
                        double r;
                        if (noNAXProfile.profile(!hasNAx[j])) {
                            yym = xm[j];
                            if (checkNAs(xxm, yym)) {
                                r = RRuntime.DOUBLE_NA;
                            } else {
                                sum = 0.0;
                                loopLength.profileCounted(n);
                                for (int k = 0; loopLength.inject(k < n); k++) {
                                    double u = xData[i * n + k];
                                    double v = xData[j * n + k];
                                    temp[k] = (u - xxm) * (v - yym);
                                }
                                for (int k = 0; loopLength.inject(k < n); k++) {
                                    sum += temp[k];
                                }
                                r = checkNAs(sum) ? RRuntime.DOUBLE_NA : sum / n1;
                            }
                        } else {
                            r = RRuntime.DOUBLE_NA;
                        }
                        ans[j + i * ncx] = r;
                        ans[i + j * ncx] = r;
                    }
                } else { /* Kendall's tau */
                    throw new UnsupportedOperationException("kendall's unsupported");
                }
            } else {
                for (int j = 0; j <= i; j++) {
                    ans[j + i * ncx] = RRuntime.DOUBLE_NA;
                    ans[i + j * ncx] = RRuntime.DOUBLE_NA;
                }
            }
        }

        if (cor) {
            for (int i = 0; i < ncx; i++) {
                if (noNAXProfile.profile(!hasNAx[i])) {
                    double u = ans[i + i * ncx];
                    xm[i] = checkNAs(u) ? RRuntime.DOUBLE_NA : Math.sqrt(u);
                }
            }
            for (int i = 0; i < ncx; i++) {
                if (noNAXProfile.profile(!hasNAx[i])) {
                    for (int j = 0; j < i; j++) {
                        if (bothZeroProfile.profile(xm[i] == 0 || xm[j] == 0)) {
                            sd0 = true;
                            ans[j + i * ncx] = RRuntime.DOUBLE_NA;
                            ans[i + j * ncx] = RRuntime.DOUBLE_NA;
                        } else {
                            double u = ans[i + j * ncx];
                            double v = xm[i];
                            double w = xm[j];
                            sum = checkNAs(u, v, w) ? RRuntime.DOUBLE_NA : u / (v * w);
                            if (sum > 1.0) {
                                sum = 1.0;
                            }
                            ans[j + i * ncx] = sum;
                            ans[i + j * ncx] = sum;
                        }
                    }
                }
                ans[i + i * ncx] = 1.0;
            }
        }

        return sd0;
    }

    private static void meanNoNA(int n, int ncx, double[] x, double[] xm, boolean[] hasNA) {
        double sum;
        double tmp;
        /* variable means (has_na) */
        for (int i = 0; i < ncx; i++) {
            if (hasNA[i]) {
                tmp = RRuntime.DOUBLE_NA;
            } else {
                sum = 0.0;
                for (int k = 0; k < n; k++) {
                    double u = x[i * n + k];
                    sum += u;
                }
                tmp = sum / n;
                if (RRuntime.isFinite(tmp)) {
                    sum = 0.0;
                    for (int k = 0; k < n; k++) {
                        double u = x[i * n + k];
                        sum += u - tmp;
                    }
                    tmp += sum / n;
                }
            }
            xm[i] = tmp;
        }
    }

    private void mean(int n, int ncx, double[] x, double[] xm, boolean[] hasNA) {
        double sum;
        double tmp;
        /* variable means (has_na) */
        for (int i = 0; i < ncx; i++) {
            if (hasNA[i]) {
                tmp = RRuntime.DOUBLE_NA;
            } else {
                sum = 0.0;
                for (int k = 0; k < n; k++) {
                    double u = x[i * n + k];
                    if (checkNAs(u)) {
                        sum = RRuntime.DOUBLE_NA;
                        break;
                    }
                    sum += u;
                }
                tmp = checkNAs(sum) ? RRuntime.DOUBLE_NA : sum / n;
                if (RRuntime.isFinite(tmp)) {
                    sum = 0.0;
                    for (int k = 0; k < n; k++) {
                        double u = x[i * n + k];
                        if (checkNAs(u)) {
                            sum = RRuntime.DOUBLE_NA;
                            break;
                        }
                        sum += u - tmp;
                    }
                    if (checkNAs(sum)) {
                        tmp = RRuntime.DOUBLE_NA;
                    } else {
                        tmp += sum / n;
                    }
                }
            }
            xm[i] = tmp;
        }
    }

    private boolean covNA2(int n, int ncx, int ncy, RDoubleVector x, RDoubleVector y, double[] xm, double[] ym, double[] ans, boolean cor, boolean iskendall) {
        double sum;
        double xxm;
        double yym;
        int n1 = -1;
        boolean sd0 = false;

        double[] xData = x.getDataWithoutCopying();
        double[] yData = y.getDataWithoutCopying();
        boolean[] hasNAx = findNAs(n, ncx, x);
        boolean[] hasNAy = findNAs(n, ncy, y);

        if (n <= 1) { /* too many missing */
            tooManyMissing.enter();
            for (int i = 0; i < ncx; i++) {
                for (int j = 0; j < ncy; j++) {
                    ans[i + j * ncx] = RRuntime.DOUBLE_NA;
                }
            }
            return sd0;
        }

        if (!iskendall) {
            if (xCompleteProfile.profile(x.isComplete())) {
                meanNoNA(n, ncx, xData, xm, hasNAx);
            } else {
                mean(n, ncx, xData, xm, hasNAx);
            }
            if (yCompleteProfile.profile(y.isComplete())) {
                meanNoNA(n, ncy, yData, ym, hasNAy);
            } else {
                mean(n, ncy, yData, ym, hasNAy);
            }
            n1 = n - 1;
        }

        for (int i = 0; i < ncx; i++) {
            if (noNAXProfile.profile(!hasNAx[i])) {
                if (!iskendall) {
                    xxm = xm[i];
                    for (int j = 0; j < ncy; j++) {
                        double r;
                        if (noNAYProfile.profile(!hasNAy[j])) {
                            yym = ym[j];
                            if (checkNAs(xxm, yym)) {
                                r = RRuntime.DOUBLE_NA;
                            } else {
                                sum = 0.0;
                                for (int k = 0; k < n; k++) {
                                    double u = xData[i * n + k];
                                    double v = yData[j * n + k];
                                    sum += (u - xxm) * (v - yym);
                                }
                                r = checkNAs(sum) ? RRuntime.DOUBLE_NA : sum / n1;
                            }
                        } else {
                            r = RRuntime.DOUBLE_NA;
                        }
                        ans[i + j * ncx] = r;
                    }
                } else { /* Kendall's tau */
                    throw new UnsupportedOperationException("kendall's unsupported");
                }
            } else {
                for (int j = 0; j < ncy; j++) {
                    ans[i + j * ncx] = RRuntime.DOUBLE_NA;
                }
            }
        }

        if (cor) {
            covsdev(n, n1, ncx, x, hasNAx, xm, iskendall);
            covsdev(n, n1, ncy, y, hasNAy, ym, iskendall);

            for (int i = 0; i < ncx; i++) {
                if (noNAXProfile.profile(!hasNAx[i])) {
                    for (int j = 0; j < ncy; j++) {
                        if (noNAYProfile.profile(!hasNAy[j])) {
                            if (xm[i] == 0.0 || ym[j] == 0.0) {
                                sd0 = true;
                                ans[i + j * ncx] = RRuntime.DOUBLE_NA;
                            } else {
                                double u = xm[i];
                                double v = ym[j];
                                if (checkNAs(u, v)) {
                                    ans[i + j * ncx] = RRuntime.DOUBLE_NA;
                                } else {
                                    ans[i + j * ncx] /= u * v;
                                }
                                if (ans[i + j * ncx] > 1.0) {
                                    ans[i + j * ncx] = 1.0;
                                }
                            }
                        }
                    }
                }
            }
        }

        return sd0;
    }

    private void covsdev(int n, int n1, int ncx, RDoubleVector x, boolean[] hasNA, double[] xm, boolean iskendall) {
        for (int i = 0; i < ncx; i++) {
            if (!hasNA[i]) { /* Var(X[j]) */
                double sum = 0.0;
                if (!iskendall) {
                    double xxm = xm[i];
                    if (checkNAs(xxm)) {
                        sum = RRuntime.DOUBLE_NA;
                    } else {
                        for (int k = 0; k < n; k++) {
                            double u = x.getDataAt(i * n + k);
                            double v = x.getDataAt(i * n + k);
                            if (checkNAs(u, v)) {
                                sum = RRuntime.DOUBLE_NA;
                                break;
                            }
                            sum += (u - xxm) * (v - xxm);
                        }
                    }
                    if (!checkNAs(sum)) {
                        sum /= n1;
                    }
                } else { /* Kendall's tau */
                    throw new UnsupportedOperationException("kendall's unsupported");
                }
                xm[i] = checkNAs(sum) ? RRuntime.DOUBLE_NA : Math.sqrt(sum);
            }
        }
    }

    private static void error(String message) {
        RError.error(NO_CALLER, Message.GENERIC, message);
    }

    private boolean checkNAs(double... xs) {
        for (double x : xs) {
            check.enable(x);
            if (check.check(x)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkNAs(double x) {
        check.enable(x);
        return check.check(x);
    }
}
