/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public final class Dqrdc2 extends RExternalBuiltinNode {
    @Child private RApplRFFI.RApplRFFINode rApplRFFINode = RFFIFactory.getRFFI().getRApplRFFI().rApplRFFINode();

    private static final String E = RRuntime.NAMES_ATTR_EMPTY_VALUE;
    private static final RStringVector DQRDC2_NAMES = RDataFactory.createStringVector(new String[]{"qr", E, E, E, E, "rank", "qraux", "pivot", E}, RDataFactory.COMPLETE_VECTOR);

    @Override
    public RList call(RArgsValuesAndNames args) {
        Object[] argValues = args.getArguments();
        try {
            RAbstractDoubleVector xVec = (RAbstractDoubleVector) argValues[0];
            int ldx = argValues[1] instanceof Integer ? (int) argValues[1] : ((RAbstractIntVector) argValues[1]).getDataAt(0);
            int n = argValues[2] instanceof Integer ? (int) argValues[2] : ((RAbstractIntVector) argValues[2]).getDataAt(0);
            int p = argValues[3] instanceof Integer ? (int) argValues[3] : ((RAbstractIntVector) argValues[3]).getDataAt(0);
            double tol = argValues[4] instanceof Double ? (double) argValues[4] : ((RAbstractDoubleVector) argValues[4]).getDataAt(0);
            RAbstractIntVector rankVec = (RAbstractIntVector) argValues[5];
            RAbstractDoubleVector qrauxVec = (RAbstractDoubleVector) argValues[6];
            RAbstractIntVector pivotVec = (RAbstractIntVector) argValues[7];
            RAbstractDoubleVector workVec = (RAbstractDoubleVector) argValues[8];
            double[] x = xVec.materialize().getDataTemp();
            int[] rank = rankVec.materialize().getDataTemp();
            double[] qraux = qrauxVec.materialize().getDataTemp();
            int[] pivot = pivotVec.materialize().getDataTemp();
            rApplRFFINode.dqrdc2(x, ldx, n, p, tol, rank, qraux, pivot, workVec.materialize().getDataCopy());
            // @formatter:off
            Object[] data = new Object[]{
                        RDataFactory.createDoubleVector(x, RDataFactory.COMPLETE_VECTOR, xVec.getDimensions()),
                        argValues[1], argValues[2], argValues[3], argValues[4],
                        RDataFactory.createIntVector(rank, RDataFactory.COMPLETE_VECTOR),
                        RDataFactory.createDoubleVector(qraux, RDataFactory.COMPLETE_VECTOR),
                        RDataFactory.createIntVector(pivot, RDataFactory.COMPLETE_VECTOR),
                        argValues[8]
            };
            // @formatter:on
            return RDataFactory.createList(data, DQRDC2_NAMES);
        } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INCORRECT_ARG, "dqrdc2");
        }
    }
}
