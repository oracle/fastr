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

import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public final class Dqrcf extends RExternalBuiltinNode {
    @Child private RApplRFFI.RApplRFFINode rApplRFFINode = RFFIFactory.getRFFI().getRApplRFFI().createRApplRFFINode();

    private static final String E = RRuntime.NAMES_ATTR_EMPTY_VALUE;
    private static final RStringVector DQRCF_NAMES = RDataFactory.createStringVector(new String[]{E, E, E, E, E, E, "coef", "info"}, RDataFactory.COMPLETE_VECTOR);

    static {
        Casts.noCasts(Dqrcf.class);
    }

    @Override
    public RList call(RArgsValuesAndNames args) {
        Object[] argValues = args.getArguments();
        try {
            RAbstractDoubleVector xVec = (RAbstractDoubleVector) argValues[0];
            int n = argValues[1] instanceof Integer ? (int) argValues[1] : ((RAbstractIntVector) argValues[1]).getDataAt(0);
            RAbstractIntVector k = (RAbstractIntVector) argValues[2];
            RAbstractDoubleVector qrauxVec = (RAbstractDoubleVector) argValues[3];
            RAbstractDoubleVector yVec = (RAbstractDoubleVector) argValues[4];
            int ny = argValues[5] instanceof Integer ? (int) argValues[5] : ((RAbstractIntVector) argValues[5]).getDataAt(0);
            RAbstractDoubleVector bVec = (RAbstractDoubleVector) argValues[6];
            RAbstractIntVector infoVec = (RAbstractIntVector) argValues[7];
            double[] x = xVec.materialize().getDataTemp();
            double[] qraux = qrauxVec.materialize().getDataTemp();
            double[] y = yVec.materialize().getDataTemp();
            double[] b = bVec.materialize().getDataTemp();
            int[] info = infoVec.materialize().getDataTemp();
            rApplRFFINode.dqrcf(x, n, k.getDataAt(0), qraux, y, ny, b, info);
            RDoubleVector coef = RDataFactory.createDoubleVector(b, RDataFactory.COMPLETE_VECTOR);
            coef.copyAttributesFrom(bVec);
            // @formatter:off
            Object[] data = new Object[]{
                        RDataFactory.createDoubleVector(x, RDataFactory.COMPLETE_VECTOR),
                        argValues[1],
                        k.copy(),
                        RDataFactory.createDoubleVector(qraux, RDataFactory.COMPLETE_VECTOR),
                        RDataFactory.createDoubleVector(y, RDataFactory.COMPLETE_VECTOR),
                        argValues[5],
                        coef,
                        RDataFactory.createIntVector(info, RDataFactory.COMPLETE_VECTOR),
            };
            // @formatter:on
            return RDataFactory.createList(data, DQRCF_NAMES);

        } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INCORRECT_ARG, "dqrcf");
        }
    }
}
