/*
 * Copyright (c) 1995--2015, The R Core Team
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;

public abstract class DoubleCentre extends RExternalBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(DoubleCentre.class);
        casts.arg(0).mustNotBeNull(RError.Message.MACRO_CAN_BE_APPLIED_TO, "REAL()", "numeric", "NULL").mustNotBeMissing().asDoubleVector();
    }

    @Specialization(guards = {"aAccess.supports(a)", "reuse.supports(a)"})
    protected RDoubleVector doubleCentre(RDoubleVector a,
                    @Cached("a.access()") VectorAccess aAccess,
                    @Cached("createNonShared(a)") VectorReuse reuse,
                    @Cached("create()") GetDimAttributeNode getDimNode) {
        int n = getDimNode.nrows(a);
        if (!getDimNode.isSquareMatrix(a)) {
            // Note: otherwise array index out of bounds
            throw error(Message.MUST_BE_SQUARE_MATRIX, "x");
        }
        try (RandomIterator aIter = aAccess.randomAccess(a)) {
            RDoubleVector result = reuse.getResult(a);
            VectorAccess resultAccess = reuse.access(result);
            try (RandomIterator resultIter = resultAccess.randomAccess(result)) {
                for (int i = 0; i < n; i++) {
                    double sum = 0;
                    for (int j = 0; j < n; j++) {
                        sum += aAccess.getDouble(aIter, i + j * n);
                    }
                    sum /= n;
                    for (int j = 0; j < n; j++) {
                        resultAccess.setDouble(resultIter, i + j * n, aAccess.getDouble(aIter, i + j * n) - sum);
                    }
                }
                for (int j = 0; j < n; j++) {
                    double sum = 0;
                    for (int i = 0; i < n; i++) {
                        sum += resultAccess.getDouble(aIter, i + j * n);
                    }
                    sum /= n;
                    for (int i = 0; i < n; i++) {
                        resultAccess.setDouble(resultIter, i + j * n, resultAccess.getDouble(aIter, i + j * n) - sum);
                    }
                }
            }
            return result;
        }
    }

    @Specialization(replaces = "doubleCentre")
    protected RDoubleVector doubleCentreGeneric(RDoubleVector a,
                    @Cached("createNonSharedGeneric()") VectorReuse reuse,
                    @Cached("create()") GetDimAttributeNode getDimNode) {
        return doubleCentre(a, a.slowPathAccess(), reuse, getDimNode);
    }
}
