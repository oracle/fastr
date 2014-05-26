/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "%*%", kind = PRIMITIVE)
public abstract class MatMult extends RBuiltinNode {

    @Child protected BinaryArithmeticNode mult = BinaryArithmeticNode.create(BinaryArithmetic.MULTIPLY);
    @Child protected BinaryArithmeticNode add = BinaryArithmeticNode.create(BinaryArithmetic.ADD);

    protected final NACheck na;

    public MatMult() {
        this.na = NACheck.create();
    }

    public MatMult(MatMult prev) {
        this.na = prev.na;
    }

    @Specialization(order = 1, guards = "bothZeroDim")
    public RDoubleVector both0Dim(RDoubleVector a, RDoubleVector b) {
        controlVisibility();
        int r = b.getDimensions()[1];
        int c = a.getDimensions()[0];
        RDoubleVector result = RDataFactory.createDoubleVector(r * c);
        result.setDimensions(new int[]{r, c});
        return result;
    }

    @Specialization(order = 2, guards = "leftHasZeroDim")
    public RAbstractVector left0Dim(RAbstractVector a, RAbstractVector b) {
        controlVisibility();
        int[] dim = a.getDimensions()[0] == 0 ? new int[]{0, b.getDimensions()[1]} : new int[]{b.getDimensions()[0], 0};
        return a.copyWithNewDimensions(dim);
    }

    @Specialization(order = 3, guards = "rightHasZeroDim")
    public RAbstractVector right0Dim(RAbstractVector a, RAbstractVector b) {
        controlVisibility();
        int[] dim = b.getDimensions()[0] == 0 ? new int[]{0, a.getDimensions()[1]} : new int[]{a.getDimensions()[0], 0};
        return b.copyWithNewDimensions(dim);
    }

    // double-double

    @Specialization(order = 10, guards = "matmat")
    public RDoubleVector matmatmult(RDoubleVector a, RDoubleVector b) {
        controlVisibility();
        final int aCols = a.getDimensions()[1];
        final int bRows = b.getDimensions()[0];
        assert aCols == bRows;
        final int aRows = a.getDimensions()[0];
        final int bCols = b.getDimensions()[1];
        double[] result = new double[aRows * bCols];
        for (int row = 0; row < aRows; ++row) {
            for (int col = 0; col < bCols; ++col) {
                double x = 0.0;
                na.enable(x);
                for (int k = 0; k < aCols; ++k) {
                    x = add.doDoubleDouble(x, mult.doDoubleDouble(a.getDataAt(k * aRows + row), b.getDataAt(col * bRows + k)));
                    na.check(x);
                }
                result[col * aRows + row] = x;
            }
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA(), new int[]{aRows, bCols});
    }

    @Specialization(order = 11, guards = "vecvec")
    public RDoubleVector vecvecmult(RDoubleVector a, RDoubleVector b) {
        controlVisibility();
        assert a.getLength() == b.getLength();
        double result = 0.0;
        na.enable(result);
        for (int k = 0; k < a.getLength(); ++k) {
            result = add.doDoubleDouble(result, mult.doDoubleDouble(a.getDataAt(k), b.getDataAt(k)));
            na.check(result);
        }
        return RDataFactory.createDoubleVector(new double[]{result}, na.neverSeenNA(), new int[]{1, 1});
    }

    @Specialization(order = 12, guards = "matvec")
    public RDoubleVector matvecmult(RDoubleVector a, RDoubleVector b) {
        controlVisibility();
        final int aCols = a.getDimensions()[1];
        assert aCols == b.getLength();
        final int aRows = a.getDimensions()[0];
        double[] result = new double[aRows];
        for (int row = 0; row < aRows; ++row) {
            double x = 0.0;
            na.enable(x);
            for (int k = 0; k < aCols; ++k) {
                x = add.doDoubleDouble(x, mult.doDoubleDouble(a.getDataAt(k * aRows + row), b.getDataAt(k)));
                na.check(x);
            }
            result[row] = x;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA(), new int[]{aRows, 1});
    }

    @Specialization(order = 13, guards = "vecmat")
    public RDoubleVector vecmatmult(RDoubleVector a, RDoubleVector b) {
        // convert a to a matrix with one column, perform matrix multiplication
        int[] dims;
        if (b.getDimensions()[0] == 1) {
            dims = new int[]{a.getLength(), 1};
        } else {
            dims = new int[]{1, a.getLength()};
        }
        RDoubleVector amat = a.copyWithNewDimensions(dims);
        return matmatmult(amat, b);
    }

    // complex-complex

    @Specialization(order = 20, guards = "matmat")
    public RComplexVector matmatmult(RComplexVector a, RComplexVector b) {
        controlVisibility();
        final int aCols = a.getDimensions()[1];
        final int bRows = b.getDimensions()[0];
        assert aCols == bRows;
        final int aRows = a.getDimensions()[0];
        final int bCols = b.getDimensions()[1];
        double[] result = new double[(aRows * bCols) << 1];
        for (int row = 0; row < aRows; ++row) {
            for (int col = 0; col < bCols; ++col) {
                RComplex x = RDataFactory.createComplexZero();
                na.enable(x);
                for (int k = 0; k < aCols; ++k) {
                    x = add.doComplexComplex(x, mult.doComplexComplex(a.getDataAt(k * aRows + row), b.getDataAt(col * bRows + k)));
                    na.check(x);
                }
                final int index = 2 * (col * aRows + row);
                result[index] = x.getRealPart();
                result[index + 1] = x.getImaginaryPart();
            }
        }
        return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{aRows, bCols});
    }

    @Specialization(order = 21, guards = "vecvec")
    public RComplexVector vecvecmult(RComplexVector a, RComplexVector b) {
        controlVisibility();
        assert a.getLength() == b.getLength();
        RComplex result = RDataFactory.createComplexZero();
        na.enable(result);
        for (int k = 0; k < a.getLength(); ++k) {
            result = add.doComplexComplex(result, mult.doComplexComplex(a.getDataAt(k), b.getDataAt(k)));
            na.check(result);
        }
        return RDataFactory.createComplexVector(new double[]{result.getRealPart(), result.getImaginaryPart()}, na.neverSeenNA(), new int[]{1, 1});
    }

    @Specialization(order = 22, guards = "matvec")
    public RComplexVector matvecmult(RComplexVector a, RComplexVector b) {
        controlVisibility();
        final int aCols = a.getDimensions()[1];
        assert aCols == b.getLength();
        final int aRows = a.getDimensions()[0];
        double[] result = new double[aRows << 1];
        for (int row = 0; row < aRows; ++row) {
            RComplex x = RDataFactory.createComplexZero();
            na.enable(x);
            for (int k = 0; k < aCols; ++k) {
                x = add.doComplexComplex(x, mult.doComplexComplex(a.getDataAt(k * aRows + row), b.getDataAt(k)));
                na.check(x);
            }
            result[2 * row] = x.getRealPart();
            result[2 * row + 1] = x.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{aRows, 1});
    }

    @Specialization(order = 23, guards = "vecmat")
    public RComplexVector vecmatmult(RComplexVector a, RComplexVector b) {
        controlVisibility();
        final int bRows = b.getDimensions()[0];
        assert a.getLength() == bRows;
        final int bCols = b.getDimensions()[1];
        double[] result = new double[bCols << 1];
        for (int col = 0; col < bCols; ++col) {
            RComplex x = RDataFactory.createComplexZero();
            na.enable(x);
            for (int k = 0; k < bRows; ++k) {
                x = add.doComplexComplex(x, mult.doComplexComplex(a.getDataAt(k), b.getDataAt(col * bRows + k)));
                na.check(x);
            }
            result[2 * col] = x.getRealPart();
            result[2 * col + 1] = x.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{1, bCols});
    }

    // guards

    protected static boolean matmat(RVector a, RVector b) {
        return a.isMatrix() && b.isMatrix();
    }

    protected static boolean vecvec(RVector a, RVector b) {
        return !a.isMatrix() && !b.isMatrix();
    }

    protected static boolean matvec(RVector a, RVector b) {
        return a.isMatrix() && !b.isMatrix();
    }

    protected static boolean vecmat(RVector a, RVector b) {
        return !a.isMatrix() && b.isMatrix();
    }

    protected static boolean leftHasZeroDim(RAbstractVector a, @SuppressWarnings("unused") RAbstractVector b) {
        return hasZeroDim(a);
    }

    protected static boolean rightHasZeroDim(@SuppressWarnings("unused") RAbstractVector a, RAbstractVector b) {
        return hasZeroDim(b);
    }

    protected static boolean bothZeroDim(RAbstractVector a, RAbstractVector b) {
        return hasZeroDim(a) && hasZeroDim(b);
    }

    protected static boolean hasZeroDim(RAbstractVector v) {
        if (!v.hasDimensions()) {
            return false;
        }
        for (int d : v.getDimensions()) {
            if (d == 0) {
                return true;
            }
        }
        return false;
    }

}
