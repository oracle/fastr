/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2014,  The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_LOGICAL;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

/**
 * Note: invoked from merge.data.frame.
 */
@RBuiltin(name = "merge", kind = INTERNAL, parameterNames = {"xinds", "yinds", "all.x", "all.y"}, behavior = PURE)
public abstract class Merge extends RBuiltinNode {

    static {
        Casts casts = new Casts(Merge.class);
        addIntegerCast(casts, "xinds");
        addIntegerCast(casts, "yinds");
        addLogicalCast(casts, "all.x");
        addLogicalCast(casts, "all.y");
    }

    private static void addIntegerCast(Casts casts, String name) {
        casts.arg(name).mustBe(integerValue()).asIntegerVector().mustBe(notEmpty());
    }

    private static void addLogicalCast(Casts casts, String name) {
        casts.arg(name).defaultError(INVALID_LOGICAL, "all.x").mustBe(numericValue()).asLogicalVector().findFirst().notNA().map(toBoolean());
    }

    private static void isortWithIndex(int[] x, int[] indx, int n) {
        int i;
        int j;
        int h;
        int iv;
        int v;

        for (h = 1; h <= n / 9; h = 3 * h + 1) {
        }
        for (; h > 0; h /= 3) {
            for (i = h; i < n; i++) {
                v = x[i];
                iv = indx[i];
                j = i;
                while (j >= h && x[j - h] > v) {
                    x[j] = x[j - h];
                    indx[j] = indx[j - h];
                    j -= h;
                }
                x[j] = v;
                indx[j] = iv;
            }
        }
    }

    @Specialization
    RList merge(RAbstractIntVector xIndsAbstract, RAbstractIntVector yIndsAbstract, boolean allX, boolean allY) {
        RIntVector xInds = xIndsAbstract.materialize();
        RIntVector yInds = yIndsAbstract.materialize();

        /* 0. sort the indices */
        int nx = xInds.getLength();
        int ny = yInds.getLength();
        int[] ix = new int[nx];
        int[] iy = new int[ny];
        for (int i = 0; i < nx; i++) {
            ix[i] = i + 1;
        }
        for (int i = 0; i < ny; i++) {
            iy[i] = i + 1;
        }
        int[] xIndsData = xInds.getDataWithoutCopying();
        int[] yIndsData = yInds.getDataWithoutCopying();
        isortWithIndex(xIndsData, ix, nx);
        isortWithIndex(yIndsData, iy, ny);

        /* 1. determine result sizes */
        int nxLone = 0;
        int nyLone = 0;
        int l;
        for (l = 0; l < nx; l++) {
            if (xIndsData[l] > 0) {
                break;
            }
        }
        nxLone = l;

        for (l = 0; l < ny; l++) {
            if (yIndsData[l] > 0) {
                break;
            }
        }
        nyLone = l;

        int nnx;
        int nny;
        double dnans = 0;
        int j = nyLone;
        for (int i = nxLone; i < nx; i = nnx, j = nny) {
            int tmp = xIndsData[i];
            for (nnx = i; nnx < nx; nnx++) {
                if (xIndsData[nnx] != tmp) {
                    break;
                }
            }
            // the next is not in theory necessary, since we have the common values only
            for (; j < ny; j++) {
                if (yIndsData[j] >= tmp) {
                    break;
                }
            }
            for (nny = j; nny < ny; nny++) {
                if (yIndsData[nny] != tmp) {
                    break;
                }
            }
            dnans += ((double) (nnx - i)) * (nny - j);
        }
        if (dnans > RRuntime.INT_MAX_VALUE) {
            throw RError.error(this, RError.Message.GENERIC, "number of rows in the result exceeds maximum vector length");
        }
        int nans = (int) dnans;

        /* 2. allocate and store result components */

        int[] ansXData = new int[nans];
        int[] ansYData = new int[nans];

        RList ans = RDataFactory.createList(new Object[4], RDataFactory.createStringVector(new String[]{"xi", "yi", "x.alone", "y.alone"}, RDataFactory.COMPLETE_VECTOR));
        ans.updateDataAt(0, RDataFactory.createIntVector(ansXData, RDataFactory.COMPLETE_VECTOR), null);
        ans.updateDataAt(1, RDataFactory.createIntVector(ansYData, RDataFactory.COMPLETE_VECTOR), null);

        if (allX) {
            int[] xLoneData = new int[nxLone];
            ans.updateDataAt(2, RDataFactory.createIntVector(xLoneData, RDataFactory.COMPLETE_VECTOR), null);
            for (int i = 0, ll = 0; i < nxLone; i++) {
                xLoneData[ll++] = ix[i];
            }
        } else {
            ans.updateDataAt(2, RNull.instance, null);
        }

        if (allY) {
            int[] yLoneData = new int[nyLone];
            ans.updateDataAt(3, RDataFactory.createIntVector(yLoneData, RDataFactory.COMPLETE_VECTOR), null);
            for (int i = 0, ll = 0; i < nyLone; i++) {
                yLoneData[ll++] = iy[i];
            }
        } else {
            ans.updateDataAt(3, RNull.instance, null);
        }

        j = nyLone;
        for (int i = nxLone, k = 0; i < nx; i = nnx, j = nny) {
            int tmp = xIndsData[i];
            for (nnx = i; nnx < nx; nnx++) {
                if (xIndsData[nnx] != tmp) {
                    break;
                }
            }
            for (; j < ny; j++) {
                if (yIndsData[j] >= tmp) {
                    break;
                }
            }
            for (nny = j; nny < ny; nny++) {
                if (yIndsData[nny] != tmp) {
                    break;
                }
            }
            for (int i0 = i; i0 < nnx; i0++) {
                for (int j0 = j; j0 < nny; j0++) {
                    ansXData[k] = ix[i0];
                    ansYData[k++] = iy[j0];
                }
            }
        }

        return ans;
    }
}
