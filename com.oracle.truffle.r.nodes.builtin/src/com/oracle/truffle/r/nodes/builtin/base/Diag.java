/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "diag", kind = INTERNAL, parameterNames = {"x", "nrow", "ncol"}, behavior = PURE)
public abstract class Diag extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("nrow").asIntegerVector().findFirst().mustBe(notIntNA(), Message.INVALID_LARGE_NA_VALUE, "nrow").mustBe(gte0(), Message.INVALID_NEGATIVE_VALUE, "nrow");

        casts.arg("ncol").asIntegerVector().findFirst().mustBe(notIntNA(), Message.INVALID_LARGE_NA_VALUE, "ncol").mustBe(gte0(), Message.INVALID_NEGATIVE_VALUE, "ncol");
    }

    private static int checkX(RAbstractVector x, int nrow, int ncol) {
        int mn = (nrow < ncol) ? nrow : ncol;
        if (mn > 0 && x.getLength() == 0) {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.SHOW_CALLER2, Message.POSITIVE_LENGTH, "x");
        }
        return mn;
    }

    @Specialization
    protected Object diag(@SuppressWarnings("unused") RNull x, int nrow, int ncol) {
        if (nrow == 0 && ncol == 0) {
            return RDataFactory.createDoubleVector(new double[]{}, true, new int[]{0, 0});
        } else {
            throw RError.error(RError.SHOW_CALLER2, Message.X_NUMERIC);
        }
    }

    @Specialization
    protected Object diag(RAbstractComplexVector x, int nrow, int ncol) {
        int mn = checkX(x, nrow, ncol);

        double[] data = new double[nrow * ncol * 2];
        int nx = x.getLength();
        for (int j = 0; j < mn; j++) {
            RComplex value = x.getDataAt(j % nx);
            int index = j * (nrow + 1) * 2;
            data[index] = value.getRealPart();
            data[index + 1] = value.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(data, x.isComplete(), new int[]{nrow, ncol});
    }

    @Specialization(guards = "!isRAbstractComplexVector(x)")
    protected Object diag(RAbstractVector x, int nrow, int ncol, //
                    @Cached("create()") CastDoubleNode cast) {
        int mn = checkX(x, nrow, ncol);

        RAbstractDoubleVector source = (RAbstractDoubleVector) cast.execute(x);
        double[] data = new double[nrow * ncol];
        int nx = source.getLength();
        for (int j = 0; j < mn; j++) {
            data[j * (nrow + 1)] = source.getDataAt(j % nx);
        }
        return RDataFactory.createDoubleVector(data, source.isComplete(), new int[]{nrow, ncol});
    }
}
