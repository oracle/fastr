/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "crossprod", kind = INTERNAL, parameterNames = {"x", "y"}, behavior = PURE)
public abstract class Crossprod extends RBuiltinNode {

    @Child private MatMult matMult = MatMultNodeGen.create(/* promoteDimNames: */ false);
    @Child private Transpose transpose;

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").mustBe(numericValue().or(complexValue()), RError.ROOTNODE, RError.Message.NUMERIC_COMPLEX_MATRIX_VECTOR);
        casts.arg("y").defaultError(RError.ROOTNODE, RError.Message.NUMERIC_COMPLEX_MATRIX_VECTOR).allowNull().mustBe(numericValue().or(complexValue()));
    }

    private Object matMult(Object op1, Object op2) {
        return matMult.executeObject(op1, op2);
    }

    private Object transpose(RAbstractVector value) {
        if (transpose == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            transpose = insert(TransposeNodeGen.create());
        }
        return transpose.execute(value);
    }

    @Specialization(guards = {"x.isMatrix()", "y.isMatrix()"})
    protected RDoubleVector crossprod(RAbstractDoubleVector x, RAbstractDoubleVector y) {
        int xRows = x.getDimensions()[0];
        int xCols = x.getDimensions()[1];
        int yRows = y.getDimensions()[0];
        int yCols = y.getDimensions()[1];
        return matMult.doubleMatrixMultiply(x, y, xCols, xRows, yRows, yCols, xRows, 1, 1, yRows, false);
    }

    private static RDoubleVector mirror(RDoubleVector result) {
        /*
         * Mirroring the result is not only good for performance, but it is also required to produce
         * the same result as GNUR.
         */
        assert result.isMatrix() && result.getDimensions()[0] == result.getDimensions()[1];
        int size = result.getDimensions()[0];
        double[] data = result.getDataWithoutCopying();
        for (int row = 0; row < size; row++) {
            int destIndex = row * size + row + 1;
            int sourceIndex = (row + 1) * size + row;
            for (int col = row + 1; col < size; col++) {
                data[destIndex] = data[sourceIndex];
                destIndex++;
                sourceIndex += size;
            }
        }
        return result;
    }

    @Specialization
    protected Object crossprod(RAbstractVector x, RAbstractVector y) {
        return matMult(transpose(x), y);
    }

    @Specialization(guards = "x.isMatrix()")
    protected RDoubleVector crossprodDoubleMatrix(RAbstractDoubleVector x, @SuppressWarnings("unused") RNull y) {
        int xRows = x.getDimensions()[0];
        int xCols = x.getDimensions()[1];
        return mirror(matMult.doubleMatrixMultiply(x, x, xCols, xRows, xRows, xCols, xRows, 1, 1, xRows, true));
    }

    @Specialization
    protected Object crossprod(RAbstractVector x, @SuppressWarnings("unused") RNull y) {
        return matMult(transpose(x), x);
    }
}
