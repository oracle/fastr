/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public abstract class Dqrqy extends RExternalBuiltinNode.Arg7 {
    @Child private RApplRFFI.DqrqyNode dqrqyNode = RFFIFactory.getRFFI().getRApplRFFI().createDqrqyNode();

    static {
        Casts casts = new Casts(Dqrqy.class);
        casts.arg(0, "x").mustBe(doubleValue()).asDoubleVector();
        casts.arg(1, "nx").mustBe(integerValue()).asIntegerVector().findFirst();
        casts.arg(2, "k").mustBe(integerValue()).asIntegerVector().findFirst();
        casts.arg(3, "qraux").mustBe(doubleValue()).asDoubleVector();
        casts.arg(4, "y").mustBe(doubleValue()).asDoubleVector();
        casts.arg(5, "ny").mustBe(integerValue()).asIntegerVector().findFirst();
        casts.arg(6, "b").mustBe(doubleValue()).asDoubleVector();
    }

    @Specialization
    public RList dqrcf(RAbstractDoubleVector xVec, int nx, int k, RAbstractDoubleVector qrauxVec, RAbstractDoubleVector yVec, int ny, RAbstractDoubleVector bVec) {
        try {
            double[] x = xVec.materialize().getDataTemp();
            double[] qraux = qrauxVec.materialize().getDataTemp();
            double[] y = yVec.materialize().getDataTemp();
            double[] b = bVec.materialize().getDataTemp();
            dqrqyNode.execute(x, nx, k, qraux, y, ny, b);
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
            };
            // @formatter:on
            return RDataFactory.createList(data);

        } catch (ClassCastException | ArrayIndexOutOfBoundsException ex) {
            throw error(RError.Message.INCORRECT_ARG, "dqrqy");
        }
    }
}
