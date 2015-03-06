/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "crossprod", kind = INTERNAL, parameterNames = {"x", "y"})
public abstract class Crossprod extends RBuiltinNode {

    @Child private MatMult matMult;
    @Child private Transpose transpose;

    private void ensureMatMult() {
        if (matMult == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            matMult = insert(MatMultFactory.create(new RNode[2], getBuiltin(), getSuppliedSignature()));
        }
    }

    private Object matMult(VirtualFrame frame, Object op1, Object op2) {
        ensureMatMult();
        return matMult.executeObject(frame, op1, op2);
    }

    private Object transpose(VirtualFrame frame, Object value) {
        if (transpose == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            transpose = insert(TransposeFactory.create(new RNode[1], getBuiltin(), getSuppliedSignature()));
        }
        return transpose.execute(frame, value);
    }

    @Specialization(guards = {"isMatrix(x)", "isMatrix(y)"})
    protected RDoubleVector crossprod(RAbstractDoubleVector x, RAbstractDoubleVector y) {
        controlVisibility();
        ensureMatMult();
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
    protected Object crossprod(VirtualFrame frame, RAbstractVector x, RAbstractVector y) {
        controlVisibility();
        return matMult(frame, transpose(frame, x), y);
    }

    @Specialization(guards = "isMatrix(x)")
    protected Object crossprodDoubleMatrix(RAbstractDoubleVector x, @SuppressWarnings("unused") RNull y) {
        controlVisibility();
        ensureMatMult();
        int xRows = x.getDimensions()[0];
        int xCols = x.getDimensions()[1];
        return mirror(matMult.doubleMatrixMultiply(x, x, xCols, xRows, xRows, xCols, xRows, 1, 1, xRows, true));
    }

    @Specialization
    protected Object crossprod(VirtualFrame frame, RAbstractVector x, @SuppressWarnings("unused") RNull y) {
        controlVisibility();
        return matMult(frame, transpose(frame, x), x);
    }

    protected static boolean isMatrix(RAbstractVector v) {
        return v.isMatrix();
    }
}
