/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

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

    @Specialization(guards = "xAccess.supports(x)")
    protected RAbstractVector diagCached(RAbstractVector x, int nrow, int ncol,
                    @Cached("x.access()") VectorAccess xAccess,
                    @Cached("createNew(xAccess.getType())") VectorAccess resultAccess,
                    @Cached("create()") VectorFactory factory) {
        int mn = checkX(x, nrow, ncol);

        try (SequentialIterator xIter = xAccess.access(x)) {
            RAbstractVector result = factory.createUninitializedVector(xAccess.getType(), nrow * ncol, new int[]{nrow, ncol}, null, null);
            try (RandomIterator resultIter = resultAccess.randomAccess(result)) {
                int resultIndex = 0;
                for (int j = 0; j < mn; j++) {
                    xAccess.nextWithWrap(xIter);
                    resultAccess.setFromSameType(resultIter, resultIndex, xAccess, xIter);
                    resultIndex += nrow + 1;
                }
            }
            result.setComplete(x.isComplete());
            return result;
        }
    }

    @Specialization(replaces = "diagCached")
    protected RAbstractVector diagGeneric(RAbstractVector x, int nrow, int ncol,
                    @Cached("create()") VectorFactory factory) {
        return diagCached(x, nrow, ncol, x.slowPathAccess(), VectorAccess.createSlowPathNew(x.getRType()), factory);
    }
}
