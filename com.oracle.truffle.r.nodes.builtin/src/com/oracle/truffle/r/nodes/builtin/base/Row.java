/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

@RBuiltin(name = "row", kind = INTERNAL, parameterNames = {"dims"}, behavior = PURE)
public abstract class Row extends RBuiltinNode {

    static {
        Casts casts = new Casts(Row.class);
        casts.arg("dims").defaultError(SHOW_CALLER, RError.Message.MATRIX_LIKE_REQUIRED, "row").mustBe(integerValue()).asIntegerVector().mustBe(size(2));
    }

    @Specialization
    protected RIntVector col(RAbstractIntVector x) {
        int nrows = x.getDataAt(0);
        int ncols = x.getDataAt(1);
        int[] result = new int[nrows * ncols];
        for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
                result[i + j * nrows] = i + 1;
            }
        }
        return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR, new int[]{nrows, ncols});
    }
}
