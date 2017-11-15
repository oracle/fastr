/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995--2015, The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;

public abstract class DoubleCentre extends RExternalBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(DoubleCentre.class);
        casts.arg(0).mustNotBeNull(RError.Message.MACRO_CAN_BE_APPLIED_TO, "REAL()", "numeric", "NULL").mustNotBeMissing().asDoubleVector();
    }

    @Specialization(guards = {"aAccess.supports(a)", "reuse.supports(a)"})
    protected RAbstractDoubleVector doubleCentre(RAbstractDoubleVector a,
                    @Cached("a.access()") VectorAccess aAccess,
                    @Cached("createNonShared(a)") VectorReuse reuse,
                    @Cached("create()") GetDimAttributeNode getDimNode) {
        int n = getDimNode.nrows(a);

        try (RandomIterator aIter = aAccess.randomAccess(a)) {
            RAbstractDoubleVector result = reuse.getResult(a);
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
    protected RAbstractDoubleVector doubleCentreGeneric(RAbstractDoubleVector a,
                    @Cached("createNonSharedGeneric()") VectorReuse reuse,
                    @Cached("create()") GetDimAttributeNode getDimNode) {
        return doubleCentre(a, a.slowPathAccess(), reuse, getDimNode);
    }
}
