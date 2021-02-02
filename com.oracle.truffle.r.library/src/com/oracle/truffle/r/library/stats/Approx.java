/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public abstract class Approx extends RExternalBuiltinNode.Arg7 {
    private static final NACheck naCheck = NACheck.create();

    public static Approx create() {
        return ApproxNodeGen.create();
    }

    static {
        Casts casts = new Casts(Approx.class);
        casts.arg(0).mustBe(doubleValue());
        casts.arg(1).mustBe(doubleValue());
        casts.arg(2).mustBe(missingValue().not()).mapIf(nullValue(), emptyDoubleVector()).asDoubleVector();
        casts.arg(3).asIntegerVector().findFirst();
        casts.arg(4).asDoubleVector().findFirst();
        casts.arg(5).asDoubleVector().findFirst();
        casts.arg(6).asDoubleVector().findFirst();
    }

    @Specialization(limit = "getVectorAccessCacheSize()")
    protected RDoubleVector approx(RDoubleVector x, RDoubleVector y, RDoubleVector v, int method, double yl, double yr, double f,
                    @CachedLibrary("x.getData()") VectorDataLibrary xLib,
                    @CachedLibrary("y.getData()") VectorDataLibrary yLib,
                    @CachedLibrary("v.getData()") VectorDataLibrary vLib) {
        int nx = x.getLength();
        int nout = v.getLength();
        double[] yout = new double[nout];
        ApprMeth apprMeth = new ApprMeth();

        apprMeth.f2 = f;
        apprMeth.f1 = 1 - f;
        apprMeth.kind = method;
        apprMeth.ylow = yl;
        apprMeth.yhigh = yr;
        naCheck.enable(true);

        Object xData = x.getData();
        RandomAccessIterator xIter = xLib.randomAccessIterator(xData);
        Object yData = y.getData();
        RandomAccessIterator yIter = yLib.randomAccessIterator(yData);
        Object vData = v.getData();
        SeqIterator vIter = vLib.iterator(vData);

        int i = 0;
        while (vLib.next(vData, vIter)) {
            double xouti = vLib.getNextDouble(vData, vIter);
            yout[i] = RRuntime.isNAorNaN(xouti) ? xouti : approx1(xouti, xLib, xData, xIter, yLib, yData, yIter, nx, apprMeth);
            naCheck.check(yout[i]);
            i++;
        }
        return RDataFactory.createDoubleVector(yout, naCheck.neverSeenNA());
    }

    private static class ApprMeth {
        double ylow;
        double yhigh;
        double f1;
        double f2;
        int kind;
    }

    private static double approx1(double v, VectorDataLibrary xLib, Object xData, RandomAccessIterator xIter,
                    VectorDataLibrary yLib, Object yData, RandomAccessIterator yIter, int n, ApprMeth apprMeth) {
        /* Approximate y(v), given (x,y)[i], i = 0,..,n-1 */

        if (n == 0) {
            return RRuntime.DOUBLE_NA;
        }

        int i = 0;
        int j = n - 1;
        /* handle out-of-domain points */
        if (v < xLib.getDouble(xData, xIter, i)) {
            return apprMeth.ylow;
        }
        if (v > xLib.getDouble(xData, xIter, j)) {
            return apprMeth.yhigh;
        }

        /* find the correct interval by bisection */
        while (i < j - 1) { /* x.getDataAt(i) <= v <= x.getDataAt(j) */
            int ij = (i + j) / 2;
            /* i+1 <= ij <= j-1 */
            if (v < xLib.getDouble(xData, xIter, ij)) {
                j = ij;
            } else {
                i = ij;
            }
            /* still i < j */
        }
        /* provably have i == j-1 */

        /* interpolation */

        double xJ = xLib.getDouble(xData, xIter, j);
        double yJ = yLib.getDouble(yData, yIter, j);
        if (v == xJ) {
            return yJ;
        }
        double xI = xLib.getDouble(xData, xIter, i);
        double yI = yLib.getDouble(yData, yIter, i);
        if (v == xI) {
            return yI;
        }
        /* impossible: if(x.getDataAt(j) == x.getDataAt(i)) return y.getDataAt(i); */

        if (apprMeth.kind == 1) { /* linear */
            return yI + (yJ - yI) * ((v - xI) / (xJ - xI));
        } else { /* 2 : constant */
            return (apprMeth.f1 != 0.0 ? yI * apprMeth.f1 : 0.0) + (apprMeth.f2 != 0.0 ? yJ * apprMeth.f2 : 0.0);
        }
    }/* approx1() */

}
