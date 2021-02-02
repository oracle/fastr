/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notIntNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;

@RBuiltin(name = "diag", kind = INTERNAL, parameterNames = {"x", "nrow", "ncol"}, behavior = PURE)
public abstract class Diag extends RBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(Diag.class);
        casts.arg("x").mustNotBeMissing().returnIf(nullValue()).mapIf(complexValue().not().and(logicalValue().not()), asDoubleVector());
        casts.arg("nrow").asIntegerVector().findFirst().mustBe(notIntNA(), Message.INVALID_LARGE_NA_VALUE, "nrow").mustBe(gte0(), Message.INVALID_NEGATIVE_VALUE, "nrow");
        casts.arg("ncol").asIntegerVector().findFirst().mustBe(notIntNA(), Message.INVALID_LARGE_NA_VALUE, "ncol").mustBe(gte0(), Message.INVALID_NEGATIVE_VALUE, "ncol");
    }

    private int checkX(RAbstractVector x, int nrow, int ncol) {
        int mn = (nrow < ncol) ? nrow : ncol;
        if (mn > 0 && x.getLength() == 0) {
            throw error(Message.POSITIVE_LENGTH, "x");
        }
        return mn;
    }

    @Specialization
    protected Object diag(@SuppressWarnings("unused") RNull x, int nrow, int ncol) {
        if (nrow == 0 || ncol == 0) {
            return RDataFactory.createDoubleVector(new double[]{}, true, new int[]{nrow, ncol});
        } else {
            throw error(Message.X_NUMERIC);
        }
    }

    @Specialization
    protected Object diag(double x, int nrow, int ncol) {
        int mn = (nrow < ncol) ? nrow : ncol;

        double[] data = new double[nrow * ncol];
        for (int j = 0; j < mn; j++) {
            data[j * (nrow + 1)] = x;
        }
        return RDataFactory.createDoubleVector(data, !RRuntime.isNA(x), new int[]{nrow, ncol});
    }

    @Specialization(limit = "getGenericDataLibraryCacheSize()")
    protected RAbstractVector diagCached(RAbstractVector x, int nrow, int ncol,
                    @Cached SetDimAttributeNode setDimAttributeNode,
                    @CachedLibrary("x") AbstractContainerLibrary xContainerLib,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary(limit = "getGenericDataLibraryCacheSize()") VectorDataLibrary resultDataLib) {
        int mn = checkX(x, nrow, ncol);
        Object xData = x.getData();
        SeqIterator xIt = xDataLib.iterator(xData);
        RAbstractVector result = xContainerLib.createEmptySameType(x, nrow * ncol, false);
        Object resultData = result.getData();
        RandomAccessWriteIterator resultIt = resultDataLib.randomAccessWriteIterator(resultData);
        try {
            int resultIndex = 0;
            for (int j = 0; j < mn; j++) {
                xDataLib.nextWithWrap(xData, xIt);
                resultDataLib.transferNextToRandom(resultData, resultIt, resultIndex, xDataLib, xIt, xData);
                resultIndex += nrow + 1;
            }
        } finally {
            resultDataLib.commitRandomAccessWriteIterator(resultData, resultIt, xDataLib.getNACheck(xData).neverSeenNA());
        }
        setDimAttributeNode.setDimensions(result, new int[]{nrow, ncol});
        return result;
    }
}
