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

import com.oracle.truffle.api.dsl.Specialization;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public abstract class Dqrcf extends RExternalBuiltinNode.Arg8 {
    @Child private RApplRFFI.RApplRFFINode rApplRFFINode = RFFIFactory.getRFFI().getRApplRFFI().createRApplRFFINode();

    private static final String E = RRuntime.NAMES_ATTR_EMPTY_VALUE;
    private static final RStringVector DQRCF_NAMES = RDataFactory.createStringVector(new String[]{E, E, E, E, E, E, "coef", "info"}, RDataFactory.COMPLETE_VECTOR);

    static {
        Casts casts = new Casts(Dqrcf.class);
        casts.arg(0).mustBe(doubleValue()).asDoubleVector();
        casts.arg(1).mustBe(integerValue()).asIntegerVector().findFirst();
        casts.arg(2).mustBe(integerValue()).asIntegerVector().findFirst();
        casts.arg(3).mustBe(doubleValue()).asDoubleVector();
        casts.arg(4).mustBe(doubleValue()).asDoubleVector();
        casts.arg(5).mustBe(integerValue()).asIntegerVector().findFirst();
        casts.arg(6).mustBe(doubleValue()).asDoubleVector();
        casts.arg(7).mustBe(integerValue()).asIntegerVector();
    }

    @Specialization
    public RList dqrcf(RAbstractDoubleVector xVec, int nx, int k, RAbstractDoubleVector qrauxVec, RAbstractDoubleVector yVec, int ny, RAbstractDoubleVector bVec, RAbstractIntVector infoVec) {
        try {
            double[] x = xVec.materialize().getDataTemp();
            double[] qraux = qrauxVec.materialize().getDataTemp();
            double[] y = yVec.materialize().getDataTemp();
            double[] b = bVec.materialize().getDataTemp();
            int[] info = infoVec.materialize().getDataTemp();
            rApplRFFINode.dqrcf(x, nx, k, qraux, y, ny, b, info);
            RDoubleVector coef = RDataFactory.createDoubleVector(b, RDataFactory.COMPLETE_VECTOR);
            coef.copyAttributesFrom(bVec);
            // @formatter:off
            Object[] data = new Object[]{
                        RDataFactory.createDoubleVector(x, RDataFactory.COMPLETE_VECTOR),
                        nx,
                        k,
                        RDataFactory.createDoubleVector(qraux, RDataFactory.COMPLETE_VECTOR),
                        RDataFactory.createDoubleVector(y, RDataFactory.COMPLETE_VECTOR),
                        ny,
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
