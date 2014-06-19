/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "crossprod", kind = INTERNAL)
public abstract class Crossprod extends RBuiltinNode {

    // TODO: this is supposed to be slightly faster than t(x) %*% y but for now it should suffice

    @Child MatMult matMult;
    @Child Transpose transpose;
    protected final NACheck na = NACheck.create();

    private Object matMult(VirtualFrame frame, Object op1, Object op2) {
        if (matMult == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            matMult = insert(MatMultFactory.create(new RNode[1], getBuiltin()));
        }
        return matMult.executeObject(frame, op1, op2);
    }

    private Object transpose(VirtualFrame frame, Object value) {
        if (transpose == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            transpose = insert(TransposeFactory.create(new RNode[1], getBuiltin()));
        }
        return transpose.execute(frame, value);
    }

    @Specialization
    public Object crossprod(VirtualFrame frame, RAbstractVector a, RAbstractVector b) {
        controlVisibility();
        return matMult(frame, transpose(frame, a), b);
    }

    @Specialization(order = 1, guards = "!matdouble")
    public Object crossprod(VirtualFrame frame, RAbstractVector b, @SuppressWarnings("unused") RNull a) {
        controlVisibility();
        return matMult(frame, transpose(frame, b), b);
    }

    @Specialization(order = 2, guards = "matdouble")
    public Object crossprodDoubleMatrix(RAbstractDoubleVector a, @SuppressWarnings("unused") RNull b) {
        controlVisibility();
        final int aCols = a.getDimensions()[1];
        final int bRows = a.getDimensions()[0];
        assert aCols == bRows;
        final int aRows = aCols;
        final int bCols = bRows;
        double[] result = new double[aRows * bCols];
        na.enable(a);
        for (int row = 0; row < aRows; ++row) {
            for (int col = 0; col < bCols; ++col) {
                double x = 0.0;
                for (int k = 0; k < aCols; ++k) {
                    double op1 = a.getDataAt(row * bRows + k);
                    double op2 = a.getDataAt(col * bRows + k);
                    if (na.checkNAorNaN(op1)) {
                        x = op1;
                        break;
                    } else if (na.check(op2)) {
                        x = op2;
                        break;
                    } else {
                        double mul = op1 * op2;
                        x = x + mul;
                    }
                }
                result[col * aRows + row] = x;
            }
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA(), new int[]{aRows, bCols});

    }

    protected static boolean matdouble(RAbstractVector vector1) {
        return vector1.getElementClass() == RDouble.class && vector1.isMatrix();
    }

}
