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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.SetDataAt;
import com.oracle.truffle.r.runtime.data.nodes.VectorReadAccess;

public abstract class DoubleCentre extends RExternalBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(DoubleCentre.class);
        casts.arg(0).mustBe(missingValue().not()).mustBe(nullValue().not(), RError.Message.MACRO_CAN_BE_APPLIED_TO, "REAL()", "numeric", "NULL").asDoubleVector();
    }

    @Specialization
    protected RDoubleVector doubleCentre(RAbstractDoubleVector aVecAbs,
                    @Cached("create()") VectorReadAccess.Double aAccess,
                    @Cached("create()") SetDataAt.Double aSetter,
                    @Cached("create()") GetDimAttributeNode getDimNode) {
        RDoubleVector aVec = aVecAbs.materialize();
        int n = getDimNode.nrows(aVec);
        Object aStore = aAccess.getDataStore(aVec);
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += aAccess.getDataAt(aVec, aStore, i + j * n);
            }
            sum /= n;
            for (int j = 0; j < n; j++) {
                double val = aAccess.getDataAt(aVec, aStore, i + j * n);
                aSetter.setDataAt(aVec, aStore, i + j * n, val - sum);
            }
        }
        for (int j = 0; j < n; j++) {
            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += aAccess.getDataAt(aVec, aStore, i + j * n);
            }
            sum /= n;
            for (int i = 0; i < n; i++) {
                double val = aAccess.getDataAt(aVec, aStore, i + j * n);
                aSetter.setDataAt(aVec, aStore, i + j * n, val - sum);
            }
        }
        return aVec;
    }
}
