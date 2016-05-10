/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.binary.BinaryMapArithmeticFunctionNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "%*%", kind = PRIMITIVE, parameterNames = {"", ""})
public abstract class MatMult extends RBuiltinNode {

    private static final int BLOCK_SIZE = 64;

    @Child private BinaryMapArithmeticFunctionNode mult = new BinaryMapArithmeticFunctionNode(BinaryArithmetic.MULTIPLY.create());
    @Child private BinaryMapArithmeticFunctionNode add = new BinaryMapArithmeticFunctionNode(BinaryArithmetic.ADD.create());

    private final BranchProfile errorProfile = BranchProfile.create();
    private final LoopConditionProfile mainLoopProfile = LoopConditionProfile.createCountingProfile();
    private final LoopConditionProfile remainingLoopProfile = LoopConditionProfile.createCountingProfile();

    private final ConditionProfile notOneRow = ConditionProfile.createBinaryProfile();
    private final ConditionProfile notOneColumn = ConditionProfile.createBinaryProfile();

    protected abstract Object executeObject(Object a, Object b);

    private final NACheck na;

    public MatMult() {
        this.na = NACheck.create();
    }

    @Specialization(guards = "bothZeroDim(a, b)")
    protected RDoubleVector both0Dim(RAbstractDoubleVector a, RAbstractDoubleVector b) {
        int r = b.getDimensions()[1];
        int c = a.getDimensions()[0];
        RDoubleVector result = RDataFactory.createDoubleVector(r * c);
        result.setDimensions(new int[]{r, c});
        return result;
    }

    @Specialization(guards = "hasZeroDim(a)")
    protected RAbstractVector left0Dim(RAbstractVector a, RAbstractVector b) {
        int[] dim = a.getDimensions()[0] == 0 ? new int[]{0, b.getDimensions()[1]} : new int[]{b.getDimensions()[0], 0};
        return a.copyWithNewDimensions(dim);
    }

    @Specialization(guards = "hasZeroDim(b)")
    protected RAbstractVector right0Dim(RAbstractVector a, RAbstractVector b) {
        int[] dim = b.getDimensions()[0] == 0 ? new int[]{0, a.getDimensions()[1]} : new int[]{a.getDimensions()[0], 0};
        return b.copyWithNewDimensions(dim);
    }

    // double-double

    private static void multiplyBlock(double[] a, double[] b, int aRows, double[] result, int row, int col, int k, int aRowStride, int aColStride, int bRowStride, int bColStride, int remainingCols,
                    int remainingRows, int remainingK, LoopConditionProfile loopProfile) {
        for (int innerCol = 0; innerCol < remainingCols; innerCol++) {
            for (int innerRow = 0; innerRow < remainingRows; innerRow++) {
                int bIndex = (col + innerCol) * bColStride + k * bRowStride;
                int aIndex = k * aColStride + (row + innerRow) * aRowStride;
                loopProfile.profileCounted(remainingK);
                double x = 0.0;
                for (int innerK = 0; loopProfile.inject(innerK < remainingK); innerK++) {
                    x += a[aIndex] * b[bIndex];
                    aIndex += aColStride;
                    bIndex += bRowStride;
                }
                result[(col + innerCol) * aRows + row + innerRow] += x;
            }
        }
    }

    private final ConditionProfile bigProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile incompleteProfile = BranchProfile.create();
    @CompilationFinal private boolean seenLargeMatrix;

    @Specialization(guards = "matmat(a, b)")
    protected RDoubleVector matmatmult(RAbstractDoubleVector a, RAbstractDoubleVector b) {
        int[] aDimensions = a.getDimensions();
        int[] bDimensions = b.getDimensions();
        return doubleMatrixMultiply(a, b, aDimensions[0], aDimensions[1], bDimensions[0], bDimensions[1]);
    }

    private RDoubleVector doubleMatrixMultiply(RAbstractDoubleVector a, RAbstractDoubleVector b, int aRows, int aCols, int bRows, int bCols) {
        return doubleMatrixMultiply(a, b, aRows, aCols, bRows, bCols, 1, aRows, 1, bRows, false);
    }

    /**
     * Performs matrix multiplication, generating the appropriate error if the input matrices are
     * not of compatible size.
     *
     * @param a the first input matrix
     * @param b the second input matrix
     * @param aRows the number of rows in the first input matrix
     * @param aCols the number of columns in the first input matrix
     * @param bRows the number of rows in the second input matrix
     * @param bCols the number of columns in the second input matrix
     * @param aRowStride distance between elements in row X and X+1
     * @param aColStride distance between elements in column X and X+1
     * @param bRowStride distance between elements in row X and X+1
     * @param bColStride distance between elements in column X and X+1
     * @param mirrored true if only the upper right triangle of the result needs to be calculated
     * @return the result vector
     */
    public RDoubleVector doubleMatrixMultiply(RAbstractDoubleVector a, RAbstractDoubleVector b, int aRows, int aCols, int bRows, int bCols, int aRowStride, int aColStride, int bRowStride,
                    int bColStride, boolean mirrored) {
        if (aCols != bRows) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        double[] dataA = a.materialize().getDataWithoutCopying();
        double[] dataB = b.materialize().getDataWithoutCopying();
        double[] result = new double[aRows * bCols];

        if (!seenLargeMatrix && (aRows > BLOCK_SIZE || aCols > BLOCK_SIZE || bRows > BLOCK_SIZE || bCols > BLOCK_SIZE)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            seenLargeMatrix = true;
        }
        if (seenLargeMatrix) {
            for (int row = 0; row < aRows; row += BLOCK_SIZE) {
                for (int col = mirrored ? row : 0; col < bCols; col += BLOCK_SIZE) {
                    for (int k = 0; k < aCols; k += BLOCK_SIZE) {
                        int remainingCols = Math.min(BLOCK_SIZE, bCols - col);
                        int remainingRows = Math.min(BLOCK_SIZE, aRows - row);
                        int remainingK = BLOCK_SIZE;
                        if (k + BLOCK_SIZE > aCols) {
                            remainingK = aCols - k;
                        }
                        if (bigProfile.profile(remainingCols == BLOCK_SIZE && remainingRows == BLOCK_SIZE && remainingK == BLOCK_SIZE)) {
                            multiplyBlock(dataA, dataB, aRows, result, row, col, k, aRowStride, aColStride, bRowStride, bColStride, BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE, mainLoopProfile);
                        } else {
                            multiplyBlock(dataA, dataB, aRows, result, row, col, k, aRowStride, aColStride, bRowStride, bColStride, remainingCols, remainingRows, remainingK, remainingLoopProfile);
                        }
                    }
                }
            }
        } else {
            multiplyBlock(dataA, dataB, aRows, result, 0, 0, 0, aRowStride, aColStride, bRowStride, bColStride, bCols, aRows, aCols, remainingLoopProfile);
        }
        // NAs are checked in bulk here, because doing so during multiplication is too costly
        boolean complete = true;
        if (!b.isComplete()) {
            incompleteProfile.enter();
            fixNAColumns(dataB, aRows, aCols, bCols, bRowStride, bColStride, result);
            complete = false;
        }
        if (!complete || !a.isComplete()) {
            /*
             * In case b is not complete, NaN rows need to be restored because the NaN in a takes
             * precedence over the NA in b.
             */
            incompleteProfile.enter();
            fixNARows(dataA, aRows, bRows, bCols, aRowStride, aColStride, result);
            complete = false;
        }
        return RDataFactory.createDoubleVector(result, complete, new int[]{aRows, bCols});
    }

    private static void fixNARows(double[] dataA, int aRows, int aCols, int bCols, int aRowStride, int aColStride, double[] result) {
        // NA's in a cause the whole row to be NA in the result
        outer: for (int row = 0; row < aRows; row++) {
            boolean hasNaN = false;
            for (int col = 0; col < aCols; col++) {
                double value = dataA[col * aColStride + row * aRowStride];
                if (RRuntime.isNA(value)) {
                    for (int innerCol = 0; innerCol < bCols; innerCol++) {
                        result[innerCol * aRows + row] = RRuntime.DOUBLE_NA;
                    }
                    continue outer;
                } else if (Double.isNaN(value)) {
                    hasNaN = true;
                }
            }
            if (hasNaN) {
                for (int innerCol = 0; innerCol < bCols; innerCol++) {
                    result[innerCol * aRows + row] = Double.NaN;
                }
            }
        }
    }

    private static void fixNAColumns(double[] dataB, int aRows, int bRows, int bCols, int bRowStride, int bColStride, double[] result) {
        // NA's in b cause the whole column to be NA in the result
        outer: for (int col = 0; col < bCols; col++) {
            for (int row = 0; row < bRows; row++) {
                if (RRuntime.isNA(dataB[col * bColStride + row * bRowStride])) {
                    for (int innerRow = 0; innerRow < aRows; innerRow++) {
                        result[col * aRows + innerRow] = RRuntime.DOUBLE_NA;
                    }
                    continue outer;
                }
            }
        }
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RDoubleVector vecvecmult(RAbstractDoubleVector a, RAbstractDoubleVector b) {
        if (a.getLength() != b.getLength()) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        double result = 0.0;
        na.enable(a);
        na.enable(b);
        for (int k = 0; k < a.getLength(); k++) {
            double aValue = a.getDataAt(k);
            double bValue = b.getDataAt(k);
            if (na.check(aValue) || na.check(bValue)) {
                return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, false, new int[]{1, 1});
            }
            result = add.applyDouble(result, mult.applyDouble(aValue, bValue));
        }
        return RDataFactory.createDoubleVector(new double[]{result}, true, new int[]{1, 1});
    }

    @Specialization(guards = "matvec(a, b)")
    protected RDoubleVector matvecmult(RAbstractDoubleVector a, RAbstractDoubleVector b) {
        int aRows = a.getDimensions()[0];
        int aCols = a.getDimensions()[1];
        int bRows;
        int bCols;
        if (aCols == b.getLength()) {
            bRows = b.getLength();
            bCols = 1;
        } else {
            bRows = 1;
            bCols = b.getLength();
        }
        return doubleMatrixMultiply(a, b, aRows, aCols, bRows, bCols);
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RDoubleVector vecmatmult(RAbstractDoubleVector a, RAbstractDoubleVector b) {
        int bRows = b.getDimensions()[0];
        int bCols = b.getDimensions()[1];
        int aRows;
        int aCols;
        if (bRows == a.getLength()) {
            aRows = 1;
            aCols = a.getLength();
        } else {
            aRows = a.getLength();
            aCols = 1;
        }
        return doubleMatrixMultiply(a, b, aRows, aCols, bRows, bCols);
    }

    // complex-complex

    @Specialization(guards = "matmat(a, b)")
    protected RComplexVector matmatmult(RAbstractComplexVector a, RAbstractComplexVector b) {
        final int aCols = a.getDimensions()[1];
        final int bRows = b.getDimensions()[0];
        if (aCols != bRows) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        final int aRows = a.getDimensions()[0];
        final int bCols = b.getDimensions()[1];
        double[] result = new double[(aRows * bCols) << 1];
        na.enable(a);
        na.enable(b);
        for (int row = 0; row < aRows; row++) {
            for (int col = 0; col < bCols; col++) {
                RComplex x = RDataFactory.createComplexZero();
                for (int k = 0; k < aCols; k++) {
                    x = add.applyComplex(x, mult.applyComplex(a.getDataAt(k * aRows + row), b.getDataAt(col * bRows + k)));
                    na.check(x);
                }
                final int index = 2 * (col * aRows + row);
                result[index] = x.getRealPart();
                result[index + 1] = x.getImaginaryPart();
            }
        }
        return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{aRows, bCols});
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RComplexVector vecvecmult(RAbstractComplexVector a, RAbstractComplexVector b) {
        if (a.getLength() != b.getLength()) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        RComplex result = RDataFactory.createComplexZero();
        na.enable(a);
        na.enable(b);
        for (int k = 0; k < a.getLength(); k++) {
            result = add.applyComplex(result, mult.applyComplex(a.getDataAt(k), b.getDataAt(k)));
            na.check(result);
        }
        return RDataFactory.createComplexVector(new double[]{result.getRealPart(), result.getImaginaryPart()}, na.neverSeenNA(), new int[]{1, 1});
    }

    @Specialization(guards = "matvec(a, b)")
    protected RComplexVector matvecmult(RAbstractComplexVector a, RAbstractComplexVector b) {
        final int aCols = a.getDimensions()[1];
        final int aRows = a.getDimensions()[0];
        if (aCols != 1 && aCols != b.getLength()) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        na.enable(a);
        na.enable(b);
        if (notOneColumn.profile(aCols != 1)) {
            double[] result = new double[aRows << 1];
            for (int row = 0; row < aRows; row++) {
                RComplex x = RDataFactory.createComplexZero();
                for (int k = 0; k < b.getLength(); k++) {
                    x = add.applyComplex(x, mult.applyComplex(a.getDataAt(k * aRows + row), b.getDataAt(k)));
                    na.check(x);
                }
                result[row << 1] = x.getRealPart();
                result[row << 1 + 1] = x.getImaginaryPart();
            }
            return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{aRows, 1});
        } else {
            double[] result = new double[aRows * b.getLength() << 1];
            for (int row = 0; row < aRows; row++) {
                for (int k = 0; k < b.getLength(); k++) {
                    RComplex x = mult.applyComplex(a.getDataAt(row), b.getDataAt(k));
                    na.check(x);
                    result[(k * aRows + row) << 1] = x.getRealPart();
                    result[(k * aRows + row) << 1 + 1] = x.getRealPart();
                }
            }
            return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{aRows, b.getLength()});
        }
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RComplexVector vecmatmult(RAbstractComplexVector a, RAbstractComplexVector b) {
        final int bRows = b.getDimensions()[0];
        final int bCols = b.getDimensions()[1];
        if (bRows != 1 && bRows != a.getLength()) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        na.enable(a);
        na.enable(b);
        if (notOneRow.profile(bRows != 1)) {
            double[] result = new double[bCols << 1];
            for (int k = 0; k < bCols; k++) {
                RComplex x = RDataFactory.createComplexZero();
                for (int row = 0; row < a.getLength(); row++) {
                    x = add.applyComplex(x, mult.applyComplex(a.getDataAt(row), b.getDataAt(k * a.getLength() + row)));
                    na.check(x);
                }
                result[k << 1] = x.getRealPart();
                result[k << 1 + 1] = x.getImaginaryPart();
            }
            return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{1, bCols});
        } else {
            double[] result = new double[(bCols * a.getLength()) << 1];
            for (int row = 0; row < a.getLength(); row++) {
                for (int k = 0; k < bCols; k++) {
                    RComplex x = mult.applyComplex(a.getDataAt(row), b.getDataAt(k));
                    na.check(x);
                    result[(k * a.getLength() + row) << 1] = x.getRealPart();
                    result[(k * a.getLength() + row) << 1 + 1] = x.getImaginaryPart();
                }
            }
            return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{a.getLength(), bCols});
        }
    }

    // int-int

    @Specialization(guards = "matmat(a, b)")
    protected RIntVector matmatmult(RAbstractIntVector a, RAbstractIntVector b) {
        final int aCols = a.getDimensions()[1];
        final int bRows = b.getDimensions()[0];
        if (aCols != bRows) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        final int aRows = a.getDimensions()[0];
        final int bCols = b.getDimensions()[1];
        int[] result = new int[aRows * bCols];
        na.enable(a);
        na.enable(b);
        for (int row = 0; row < aRows; row++) {
            for (int col = 0; col < bCols; col++) {
                int x = 0;
                for (int k = 0; k < aCols; k++) {
                    x = add.applyInteger(x, mult.applyInteger(a.getDataAt(k * aRows + row), b.getDataAt(col * bRows + k)));
                    na.check(x);
                }
                result[col * aRows + row] = x;
            }
        }
        return RDataFactory.createIntVector(result, na.neverSeenNA(), new int[]{aRows, bCols});
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RIntVector vecvecmult(RAbstractIntVector a, RAbstractIntVector b) {
        if (a.getLength() != b.getLength()) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        int result = 0;
        na.enable(result);
        for (int k = 0; k < a.getLength(); k++) {
            result = add.applyInteger(result, mult.applyInteger(a.getDataAt(k), b.getDataAt(k)));
            na.check(result);
        }
        return RDataFactory.createIntVector(new int[]{result}, na.neverSeenNA(), new int[]{1, 1});
    }

    @Specialization(guards = "matvec(a, b)")
    protected RIntVector matvecmult(RAbstractIntVector a, RAbstractIntVector b) {
        final int aCols = a.getDimensions()[1];
        final int aRows = a.getDimensions()[0];
        if (aCols != 1 && aCols != b.getLength()) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        na.enable(a);
        na.enable(b);
        if (notOneColumn.profile(aCols != 1)) {
            int[] result = new int[aRows];
            for (int row = 0; row < aRows; row++) {
                int x = 0;
                for (int k = 0; k < b.getLength(); k++) {
                    x = add.applyInteger(x, mult.applyInteger(a.getDataAt(k * aRows + row), b.getDataAt(k)));
                    na.check(x);
                }
                result[row] = x;
            }
            return RDataFactory.createIntVector(result, na.neverSeenNA(), new int[]{aRows, 1});
        } else {
            int[] result = new int[aRows * b.getLength()];
            for (int row = 0; row < aRows; row++) {
                for (int k = 0; k < b.getLength(); k++) {
                    int x = mult.applyInteger(a.getDataAt(row), b.getDataAt(k));
                    na.check(x);
                    result[k * aRows + row] = x;
                }
            }
            return RDataFactory.createIntVector(result, na.neverSeenNA(), new int[]{aRows, b.getLength()});
        }
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RIntVector vecmatmult(RAbstractIntVector a, RAbstractIntVector b) {
        final int bCols = b.getDimensions()[1];
        final int bRows = b.getDimensions()[0];
        if (bRows != 1 && bRows != a.getLength()) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.NON_CONFORMABLE_ARGS);
        }
        na.enable(a);
        na.enable(b);
        if (notOneRow.profile(bRows != 1)) {
            int[] result = new int[bCols];
            for (int k = 0; k < bCols; k++) {
                int x = 0;
                for (int row = 0; row < a.getLength(); row++) {
                    x = add.applyInteger(x, mult.applyInteger(a.getDataAt(row), b.getDataAt(k * a.getLength() + row)));
                    na.check(x);
                }
                result[k] = x;
            }
            return RDataFactory.createIntVector(result, na.neverSeenNA(), new int[]{1, bCols});
        } else {
            int[] result = new int[bCols * a.getLength()];
            for (int row = 0; row < a.getLength(); row++) {
                for (int k = 0; k < bCols; k++) {
                    int x = mult.applyInteger(a.getDataAt(row), b.getDataAt(k));
                    na.check(x);
                    result[k * a.getLength() + row] = x;
                }
            }
            return RDataFactory.createIntVector(result, na.neverSeenNA(), new int[]{a.getLength(), bCols});
        }
    }

    // logical-logical

    @Specialization(guards = "matmat(a, b)")
    protected RIntVector matmatmult(RAbstractLogicalVector a, RAbstractLogicalVector b) {
        return matmatmult(RClosures.createLogicalToIntVector(a), RClosures.createLogicalToIntVector(b));
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RIntVector vecvecmult(RAbstractLogicalVector a, RAbstractLogicalVector b) {
        return vecvecmult(RClosures.createLogicalToIntVector(a), RClosures.createLogicalToIntVector(b));
    }

    @Specialization(guards = "matvec(a, b)")
    protected RIntVector matvecmult(RAbstractLogicalVector a, RAbstractLogicalVector b) {
        return matvecmult(RClosures.createLogicalToIntVector(a), RClosures.createLogicalToIntVector(b));
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RIntVector vecmatmult(RAbstractLogicalVector a, RAbstractLogicalVector b) {
        return vecmatmult(RClosures.createLogicalToIntVector(a), RClosures.createLogicalToIntVector(b));
    }

    // to int

    @Specialization(guards = "matmat(a, b)")
    protected RIntVector matmatmult(RAbstractLogicalVector a, RAbstractIntVector b) {
        return matmatmult(RClosures.createLogicalToIntVector(a), b);
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RIntVector vecvecmult(RAbstractLogicalVector a, RAbstractIntVector b) {
        return vecvecmult(RClosures.createLogicalToIntVector(a), b);
    }

    @Specialization(guards = "matvec(a, b)")
    protected RIntVector matvecmult(RAbstractLogicalVector a, RAbstractIntVector b) {
        return matvecmult(RClosures.createLogicalToIntVector(a), b);
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RIntVector vecmatmult(RAbstractLogicalVector a, RAbstractIntVector b) {
        return vecmatmult(RClosures.createLogicalToIntVector(a), b);
    }

    @Specialization(guards = "matmat(a, b)")
    protected RIntVector matmatmult(RAbstractIntVector a, RAbstractLogicalVector b) {
        return matmatmult(a, RClosures.createLogicalToIntVector(b));
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RIntVector vecvecmult(RAbstractIntVector a, RAbstractLogicalVector b) {
        return vecvecmult(a, RClosures.createLogicalToIntVector(b));
    }

    @Specialization(guards = "matvec(a, b)")
    protected RIntVector matvecmult(RAbstractIntVector a, RAbstractLogicalVector b) {
        return matvecmult(a, RClosures.createLogicalToIntVector(b));
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RIntVector vecmatmult(RAbstractIntVector a, RAbstractLogicalVector b) {
        return vecmatmult(a, RClosures.createLogicalToIntVector(b));
    }

    // to complex

    @Specialization(guards = "matmat(a, b)")
    protected RComplexVector matmatmult(RAbstractIntVector a, RAbstractComplexVector b) {
        return matmatmult(RClosures.createIntToComplexVector(a), b);
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RComplexVector vecvecmult(RAbstractIntVector a, RAbstractComplexVector b) {
        return vecvecmult(RClosures.createIntToComplexVector(a), b);
    }

    @Specialization(guards = "matvec(a, b)")
    protected RComplexVector matvecmult(RAbstractIntVector a, RAbstractComplexVector b) {
        return matvecmult(RClosures.createIntToComplexVector(a), b);
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RComplexVector vecmatmult(RAbstractIntVector a, RAbstractComplexVector b) {
        return vecmatmult(RClosures.createIntToComplexVector(a), b);
    }

    @Specialization(guards = "matmat(a, b)")
    protected RComplexVector matmatmult(RAbstractComplexVector a, RAbstractIntVector b) {
        return matmatmult(a, RClosures.createIntToComplexVector(b));
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RComplexVector vecvecmult(RAbstractComplexVector a, RAbstractIntVector b) {
        return vecvecmult(a, RClosures.createIntToComplexVector(b));
    }

    @Specialization(guards = "matvec(a, b)")
    protected RComplexVector matvecmult(RAbstractComplexVector a, RAbstractIntVector b) {
        return matvecmult(a, RClosures.createIntToComplexVector(b));
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RComplexVector vecmatmult(RAbstractComplexVector a, RAbstractIntVector b) {
        return vecmatmult(a, RClosures.createIntToComplexVector(b));
    }

    @Specialization(guards = "matmat(a, b)")
    protected RComplexVector matmatmult(RAbstractLogicalVector a, RAbstractComplexVector b) {
        return matmatmult(RClosures.createLogicalToComplexVector(a), b);
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RComplexVector vecvecmult(RAbstractLogicalVector a, RAbstractComplexVector b) {
        return vecvecmult(RClosures.createLogicalToComplexVector(a), b);
    }

    @Specialization(guards = "matvec(a, b)")
    protected RComplexVector matvecmult(RAbstractLogicalVector a, RAbstractComplexVector b) {
        return matvecmult(RClosures.createLogicalToComplexVector(a), b);
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RComplexVector vecmatmult(RAbstractLogicalVector a, RAbstractComplexVector b) {
        return vecmatmult(RClosures.createLogicalToComplexVector(a), b);
    }

    @Specialization(guards = "matmat(a, b)")
    protected RComplexVector matmatmult(RAbstractComplexVector a, RAbstractLogicalVector b) {
        return matmatmult(a, RClosures.createLogicalToComplexVector(b));
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RComplexVector vecvecmult(RAbstractComplexVector a, RAbstractLogicalVector b) {
        return vecvecmult(a, RClosures.createLogicalToComplexVector(b));
    }

    @Specialization(guards = "matvec(a, b)")
    protected RComplexVector matvecmult(RAbstractComplexVector a, RAbstractLogicalVector b) {
        return matvecmult(a, RClosures.createLogicalToComplexVector(b));
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RComplexVector vecmatmult(RAbstractComplexVector a, RAbstractLogicalVector b) {
        return vecmatmult(a, RClosures.createLogicalToComplexVector(b));
    }

    @Specialization(guards = "matmat(a, b)")
    protected RComplexVector matmatmult(RAbstractDoubleVector a, RAbstractComplexVector b) {
        return matmatmult(RClosures.createDoubleToComplexVector(a), b);
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RComplexVector vecvecmult(RAbstractDoubleVector a, RAbstractComplexVector b) {
        return vecvecmult(RClosures.createDoubleToComplexVector(a), b);
    }

    @Specialization(guards = "matvec(a, b)")
    protected RComplexVector matvecmult(RAbstractDoubleVector a, RAbstractComplexVector b) {
        return matvecmult(RClosures.createDoubleToComplexVector(a), b);
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RComplexVector vecmatmult(RAbstractDoubleVector a, RAbstractComplexVector b) {
        return vecmatmult(RClosures.createDoubleToComplexVector(a), b);
    }

    @Specialization(guards = "matmat(a, b)")
    protected RComplexVector matmatmult(RAbstractComplexVector a, RAbstractDoubleVector b) {
        return matmatmult(a, RClosures.createDoubleToComplexVector(b));
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RComplexVector vecvecmult(RAbstractComplexVector a, RAbstractDoubleVector b) {
        return vecvecmult(a, RClosures.createDoubleToComplexVector(b));
    }

    @Specialization(guards = "matvec(a, b)")
    protected RComplexVector matvecmult(RAbstractComplexVector a, RAbstractDoubleVector b) {
        return matvecmult(a, RClosures.createDoubleToComplexVector(b));
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RComplexVector vecmatmult(RAbstractComplexVector a, RAbstractDoubleVector b) {
        return vecmatmult(a, RClosures.createDoubleToComplexVector(b));
    }

    // to double

    @Specialization(guards = "matmat(a, b)")
    protected RDoubleVector matmatmult(RAbstractIntVector a, RAbstractDoubleVector b) {
        return matmatmult(RClosures.createIntToDoubleVector(a), b);
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RDoubleVector vecvecmult(RAbstractIntVector a, RAbstractDoubleVector b) {
        return vecvecmult(RClosures.createIntToDoubleVector(a), b);
    }

    @Specialization(guards = "matvec(a, b)")
    protected RDoubleVector matvecmult(RAbstractIntVector a, RAbstractDoubleVector b) {
        return matvecmult(RClosures.createIntToDoubleVector(a), b);
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RDoubleVector vecmatmult(RAbstractIntVector a, RAbstractDoubleVector b) {
        return vecmatmult(RClosures.createIntToDoubleVector(a), b);
    }

    @Specialization(guards = "matmat(a, b)")
    protected RDoubleVector matmatmult(RAbstractDoubleVector a, RAbstractIntVector b) {
        return matmatmult(a, RClosures.createIntToDoubleVector(b));
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RDoubleVector vecvecmult(RAbstractDoubleVector a, RAbstractIntVector b) {
        return vecvecmult(a, RClosures.createIntToDoubleVector(b));
    }

    @Specialization(guards = "matvec(a, b)")
    protected RDoubleVector matvecmult(RAbstractDoubleVector a, RAbstractIntVector b) {
        return matvecmult(a, RClosures.createIntToDoubleVector(b));
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RDoubleVector vecmatmult(RAbstractDoubleVector a, RAbstractIntVector b) {
        return vecmatmult(a, RClosures.createIntToDoubleVector(b));
    }

    @Specialization(guards = "matmat(a, b)")
    protected RDoubleVector matmatmult(RAbstractLogicalVector a, RAbstractDoubleVector b) {
        return matmatmult(RClosures.createLogicalToDoubleVector(a), b);
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RDoubleVector vecvecmult(RAbstractLogicalVector a, RAbstractDoubleVector b) {
        return vecvecmult(RClosures.createLogicalToDoubleVector(a), b);
    }

    @Specialization(guards = "matvec(a, b)")
    protected RDoubleVector matvecmult(RAbstractLogicalVector a, RAbstractDoubleVector b) {
        return matvecmult(RClosures.createLogicalToDoubleVector(a), b);
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RDoubleVector vecmatmult(RAbstractLogicalVector a, RAbstractDoubleVector b) {
        return vecmatmult(RClosures.createLogicalToDoubleVector(a), b);
    }

    @Specialization(guards = "matmat(a, b)")
    protected RDoubleVector matmatmult(RAbstractDoubleVector a, RAbstractLogicalVector b) {
        return matmatmult(a, RClosures.createLogicalToDoubleVector(b));
    }

    @Specialization(guards = "vecvec(a, b)")
    protected RDoubleVector vecvecmult(RAbstractDoubleVector a, RAbstractLogicalVector b) {
        return vecvecmult(a, RClosures.createLogicalToDoubleVector(b));
    }

    @Specialization(guards = "matvec(a, b)")
    protected RDoubleVector matvecmult(RAbstractDoubleVector a, RAbstractLogicalVector b) {
        return matvecmult(a, RClosures.createLogicalToDoubleVector(b));
    }

    @Specialization(guards = "vecmat(a, b)")
    protected RDoubleVector vecmatmult(RAbstractDoubleVector a, RAbstractLogicalVector b) {
        return vecmatmult(a, RClosures.createLogicalToDoubleVector(b));
    }

    // errors

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector doRaw(RAbstractRawVector a, Object b) {
        throw RError.error(this, RError.Message.NUMERIC_COMPLEX_MATRIX_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector doRaw(Object a, RAbstractRawVector b) {
        throw RError.error(this, RError.Message.NUMERIC_COMPLEX_MATRIX_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector doString(RAbstractStringVector a, Object b) {
        throw RError.error(this, RError.Message.NUMERIC_COMPLEX_MATRIX_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector doString(Object a, RAbstractStringVector b) {
        throw RError.error(this, RError.Message.NUMERIC_COMPLEX_MATRIX_VECTOR);
    }

    // guards

    protected static boolean matmat(RAbstractVector a, RAbstractVector b) {
        return a.isMatrix() && b.isMatrix();
    }

    protected static boolean vecvec(RAbstractVector a, RAbstractVector b) {
        return !a.isMatrix() && !b.isMatrix();
    }

    protected static boolean matvec(RAbstractVector a, RAbstractVector b) {
        return a.isMatrix() && !b.isMatrix();
    }

    protected static boolean vecmat(RAbstractVector a, RAbstractVector b) {
        return !a.isMatrix() && b.isMatrix();
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
