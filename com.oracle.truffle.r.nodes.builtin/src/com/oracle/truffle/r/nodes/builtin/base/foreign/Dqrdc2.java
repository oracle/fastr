/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public abstract class Dqrdc2 extends RExternalBuiltinNode.Arg9 {
    @Child private RApplRFFI.Dqrdc2Node dqrdc2Node = RFFIFactory.getRFFI().getRApplRFFI().createDqrdc2Node();

    private static final String E = RRuntime.NAMES_ATTR_EMPTY_VALUE;
    private static final RStringVector DQRDC2_NAMES = RDataFactory.createStringVector(new String[]{"qr", E, E, E, E, "rank", "qraux", "pivot", E}, RDataFactory.COMPLETE_VECTOR);

    public static Dqrdc2 create() {
        return Dqrdc2NodeGen.create();
    }

    @Child private GetDimAttributeNode getDimNode = GetDimAttributeNode.create();

    static {
        Casts casts = new Casts(Dqrdc2.class);
        casts.arg(0, "xVec").mustBe(doubleValue()).asDoubleVector();
        casts.arg(1, "ldx").mustBe(integerValue()).asIntegerVector().findFirst();
        casts.arg(2, "n").mustBe(integerValue()).asIntegerVector().findFirst();
        casts.arg(3, "p").mustBe(integerValue()).asIntegerVector().findFirst();
        casts.arg(4, "tol").mustBe(doubleValue()).asDoubleVector().findFirst();
        casts.arg(5, "rankVec").mustBe(integerValue()).asIntegerVector();
        casts.arg(6, "qrauxVec").mustBe(doubleValue()).asDoubleVector();
        casts.arg(7, "pivotVec").mustBe(integerValue()).asIntegerVector();
        casts.arg(8, "workVec").mustBe(doubleValue()).asDoubleVector();
    }

    @Specialization
    public RList dqrdc2(RAbstractDoubleVector xVec, int ldx, int n, int p, double tol, RAbstractIntVector rankVec, RAbstractDoubleVector qrauxVec, RAbstractIntVector pivotVec,
                    RAbstractDoubleVector workVec) {
        double[] x = xVec.materialize().getDataTemp();
        int[] rank = rankVec.materialize().getDataTemp();
        double[] qraux = qrauxVec.materialize().getDataTemp();
        int[] pivot = pivotVec.materialize().getDataTemp();
        dqrdc2Node.execute(x, ldx, n, p, tol, rank, qraux, pivot, workVec.materialize().getDataCopy());
        // @formatter:off
        Object[] data = new Object[]{
                    RDataFactory.createDoubleVector(x, RDataFactory.COMPLETE_VECTOR, getDimNode.getDimensions(xVec)),
                    ldx, n, p, tol,
                    RDataFactory.createIntVector(rank, RDataFactory.COMPLETE_VECTOR),
                    RDataFactory.createDoubleVector(qraux, RDataFactory.COMPLETE_VECTOR),
                    RDataFactory.createIntVector(pivot, RDataFactory.COMPLETE_VECTOR),
                    workVec
        };
        // @formatter:on
        return RDataFactory.createList(data, DQRDC2_NAMES);
    }
}
