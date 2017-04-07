/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode.isMatrix;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.binary.BinaryMapArithmeticFunctionNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "%*%", kind = PRIMITIVE, parameterNames = {"", ""}, behavior = PURE)
public abstract class MatMult extends RBuiltinNode {

    private static final int BLOCK_SIZE = 64;

    @Child private BinaryMapArithmeticFunctionNode mult = new BinaryMapArithmeticFunctionNode(BinaryArithmetic.MULTIPLY.createOperation());
    @Child private BinaryMapArithmeticFunctionNode add = new BinaryMapArithmeticFunctionNode(BinaryArithmetic.ADD.createOperation());
    private final boolean promoteDimNames;

    private final LoopConditionProfile mainLoopProfile = LoopConditionProfile.createCountingProfile();
    private final LoopConditionProfile remainingLoopProfile = LoopConditionProfile.createCountingProfile();

    private final ConditionProfile notOneRow = ConditionProfile.createBinaryProfile();
    private final ConditionProfile notOneColumn = ConditionProfile.createBinaryProfile();

    private final ConditionProfile noDimAttributes = ConditionProfile.createBinaryProfile();

    @Child protected GetDimAttributeNode getADimsNode = GetDimAttributeNode.create();
    @Child protected GetDimAttributeNode getBDimsNode = GetDimAttributeNode.create();
    @Child protected SetDimAttributeNode setDimsNode = SetDimAttributeNode.create();

    @Child private SetDimNamesAttributeNode setDimNamesNode = SetDimNamesAttributeNode.create();
    @Child private GetDimNamesAttributeNode getADimNamesNode = GetDimNamesAttributeNode.create();
    @Child private GetDimNamesAttributeNode getBDimNamesNode = GetDimNamesAttributeNode.create();

    protected abstract Object executeObject(Object a, Object b);

    private final NACheck na;

    static {
        Casts.noCasts(MatMult.class);
    }

    public MatMult(boolean promoteDimNames) {
        this.promoteDimNames = promoteDimNames;
        this.na = NACheck.create();
    }

    public static MatMult create() {
        return MatMultNodeGen.create(true);
    }

    @Specialization(guards = "bothZeroDim(a, b)")
    protected RDoubleVector both0Dim(RAbstractDoubleVector a, RAbstractDoubleVector b) {
        int r = getBDimsNode.getDimensions(b)[1];
        int c = getADimsNode.getDimensions(a)[0];
        RDoubleVector result = RDataFactory.createDoubleVector(r * c);
        setDimsNode.setDimensions(result, new int[]{r, c});
        return result;
    }

    @Specialization(guards = "hasZeroDim(a, getADimsNode)")
    protected RAbstractVector left0Dim(RAbstractVector a, RAbstractVector b) {
        int[] aDim = getADimsNode.getDimensions(a);
        int[] dim = aDim[0] == 0 ? new int[]{0, getBDimsNode.getDimensions(b)[1]} : new int[]{getBDimsNode.getDimensions(b)[0], 0};
        return a.copyWithNewDimensions(dim);
    }

    @Specialization(guards = "hasZeroDim(b, getBDimsNode)")
    protected RAbstractVector right0Dim(RAbstractVector a, RAbstractVector b) {
        int[] bDim = getBDimsNode.getDimensions(b);
        int[] dim = bDim[0] == 0 ? new int[]{0, getADimsNode.getDimensions(a)[1]} : new int[]{getADimsNode.getDimensions(a)[0], 0};
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
            throw error(RError.Message.NON_CONFORMABLE_ARGS);
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

        RDoubleVector resultVec = RDataFactory.createDoubleVector(result, complete, new int[]{aRows, bCols});
        RList aDimNames = getADimNamesNode.getDimNames(a);
        RList bDimNames = getBDimNamesNode.getDimNames(b);
        if (!promoteDimNames || noDimAttributes.profile(aDimNames == null && bDimNames == null)) {
            return resultVec;
        }

        Object dimName1 = RNull.instance;
        if (aDimNames != null && aDimNames.getLength() > 0) {
            dimName1 = aDimNames.getDataAt(0);
        }
        Object dimName2 = RNull.instance;
        if (bDimNames != null && bDimNames.getLength() > 1) {
            dimName2 = bDimNames.getDataAt(1);
        }
        setDimNamesNode.setDimNames(resultVec, RDataFactory.createList(new Object[]{dimName1, dimName2}));
        return resultVec;
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

    @Specialization(guards = {"a.getClass() == aClass", "b.getClass() == bClass"})
    protected RDoubleVector multiplyDouble(RAbstractDoubleVector a, RAbstractDoubleVector b,
                    @Cached("a.getClass()") Class<? extends RAbstractDoubleVector> aClass,
                    @Cached("b.getClass()") Class<? extends RAbstractDoubleVector> bClass,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile lengthEquals) {
        return doubleMultiply(aClass.cast(a), bClass.cast(b), aIsMatrix, bIsMatrix, lengthEquals);
    }

    @Specialization(replaces = "multiplyDouble")
    protected RDoubleVector multiply(RAbstractDoubleVector a, RAbstractDoubleVector b,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile lengthEquals) {
        return doubleMultiply(a, b, aIsMatrix, bIsMatrix, lengthEquals);
    }

    private RDoubleVector doubleMultiply(RAbstractDoubleVector a, RAbstractDoubleVector b, ConditionProfile aIsMatrix, ConditionProfile bIsMatrix, ConditionProfile lengthEquals) {
        int[] aDimensions = getADimsNode.getDimensions(a);
        int[] bDimensions = getBDimsNode.getDimensions(b);
        if (aIsMatrix.profile(isMatrix(aDimensions))) {
            if (bIsMatrix.profile(isMatrix(bDimensions))) {
                return doubleMatrixMultiply(a, b, aDimensions[0], aDimensions[1], bDimensions[0], bDimensions[1]);
            } else {
                int aRows = aDimensions[0];
                int aCols = aDimensions[1];
                int bRows;
                int bCols;
                if (lengthEquals.profile(aCols == b.getLength())) {
                    bRows = b.getLength();
                    bCols = 1;
                } else {
                    bRows = 1;
                    bCols = b.getLength();
                }
                return doubleMatrixMultiply(a, b, aRows, aCols, bRows, bCols);
            }
        } else {
            if (bIsMatrix.profile(isMatrix(bDimensions))) {
                int bRows = bDimensions[0];
                int bCols = bDimensions[1];
                int aRows;
                int aCols;
                if (lengthEquals.profile(bRows == a.getLength())) {
                    aRows = 1;
                    aCols = a.getLength();
                } else {
                    aRows = a.getLength();
                    aCols = 1;
                }
                return doubleMatrixMultiply(a, b, aRows, aCols, bRows, bCols);
            } else {
                if (a.getLength() != b.getLength()) {
                    throw error(RError.Message.NON_CONFORMABLE_ARGS);
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
        }
    }

    // complex-complex

    @Specialization(guards = {"a.getClass() == aClass", "b.getClass() == bClass"})
    protected RComplexVector multiplyComplex(RAbstractComplexVector a, RAbstractComplexVector b,
                    @Cached("a.getClass()") Class<? extends RAbstractComplexVector> aClass,
                    @Cached("b.getClass()") Class<? extends RAbstractComplexVector> bClass,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix) {
        return complexMultiply(aClass.cast(a), bClass.cast(b), aIsMatrix, bIsMatrix);
    }

    @Specialization(replaces = "multiplyComplex")
    protected RComplexVector multiply(RAbstractComplexVector a, RAbstractComplexVector b,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix) {
        return complexMultiply(a, b, aIsMatrix, bIsMatrix);
    }

    private RComplexVector complexMultiply(RAbstractComplexVector a, RAbstractComplexVector b, ConditionProfile aIsMatrix, ConditionProfile bIsMatrix) {
        int[] aDimensions = getADimsNode.getDimensions(a);
        int[] bDimensions = getBDimsNode.getDimensions(b);
        if (aIsMatrix.profile(isMatrix(aDimensions))) {
            if (bIsMatrix.profile(isMatrix(bDimensions))) {
                final int aCols = aDimensions[1];
                final int bRows = bDimensions[0];
                if (aCols != bRows) {
                    throw error(RError.Message.NON_CONFORMABLE_ARGS);
                }
                final int aRows = aDimensions[0];
                final int bCols = bDimensions[1];
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
            } else {
                final int aCols = aDimensions[1];
                final int aRows = aDimensions[0];
                if (aCols != 1 && aCols != b.getLength()) {
                    throw error(RError.Message.NON_CONFORMABLE_ARGS);
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
                        result[(row << 1) + 1] = x.getImaginaryPart();
                    }
                    return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{aRows, 1});
                } else {
                    double[] result = new double[aRows * b.getLength() << 1];
                    for (int row = 0; row < aRows; row++) {
                        for (int k = 0; k < b.getLength(); k++) {
                            RComplex x = mult.applyComplex(a.getDataAt(row), b.getDataAt(k));
                            na.check(x);
                            result[(k * aRows + row) << 1] = x.getRealPart();
                            result[((k * aRows + row) << 1) + 1] = x.getRealPart();
                        }
                    }
                    return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{aRows, b.getLength()});
                }
            }
        } else {
            if (bIsMatrix.profile(isMatrix(bDimensions))) {
                final int bRows = bDimensions[0];
                final int bCols = bDimensions[1];
                if (bRows != 1 && bRows != a.getLength()) {
                    throw error(RError.Message.NON_CONFORMABLE_ARGS);
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
                        result[(k << 1) + 1] = x.getImaginaryPart();
                    }
                    return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{1, bCols});
                } else {
                    double[] result = new double[(bCols * a.getLength()) << 1];
                    for (int row = 0; row < a.getLength(); row++) {
                        for (int k = 0; k < bCols; k++) {
                            RComplex x = mult.applyComplex(a.getDataAt(row), b.getDataAt(k));
                            na.check(x);
                            result[(k * a.getLength() + row) << 1] = x.getRealPart();
                            result[((k * a.getLength() + row) << 1) + 1] = x.getImaginaryPart();
                        }
                    }
                    return RDataFactory.createComplexVector(result, na.neverSeenNA(), new int[]{a.getLength(), bCols});
                }
            } else {
                if (a.getLength() != b.getLength()) {
                    throw error(RError.Message.NON_CONFORMABLE_ARGS);
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
        }
    }

    // int-int

    @Specialization(guards = {"a.getClass() == aClass", "b.getClass() == bClass"})
    protected RIntVector multiplyInt(RAbstractIntVector a, RAbstractIntVector b,
                    @Cached("a.getClass()") Class<? extends RAbstractIntVector> aClass,
                    @Cached("b.getClass()") Class<? extends RAbstractIntVector> bClass,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix) {
        return intMultiply(aClass.cast(a), bClass.cast(b), aIsMatrix, bIsMatrix);
    }

    @Specialization(replaces = "multiplyInt")
    protected RIntVector multiply(RAbstractIntVector a, RAbstractIntVector b,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix) {
        return intMultiply(a, b, aIsMatrix, bIsMatrix);
    }

    private RIntVector intMultiply(RAbstractIntVector a, RAbstractIntVector b, ConditionProfile aIsMatrix, ConditionProfile bIsMatrix) {
        int[] aDimensions = getADimsNode.getDimensions(a);
        int[] bDimensions = getBDimsNode.getDimensions(b);
        if (aIsMatrix.profile(isMatrix(aDimensions))) {
            if (bIsMatrix.profile(isMatrix(bDimensions))) {
                int aCols = aDimensions[1];
                int bRows = bDimensions[0];
                if (aCols != bRows) {
                    throw error(RError.Message.NON_CONFORMABLE_ARGS);
                }
                int aRows = aDimensions[0];
                int bCols = bDimensions[1];
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
            } else {
                final int aCols = aDimensions[1];
                final int aRows = aDimensions[0];
                if (aCols != 1 && aCols != b.getLength()) {
                    throw error(RError.Message.NON_CONFORMABLE_ARGS);
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
        } else {
            if (bIsMatrix.profile(isMatrix(bDimensions))) {
                final int bCols = bDimensions[1];
                final int bRows = bDimensions[0];
                if (bRows != 1 && bRows != a.getLength()) {
                    throw error(RError.Message.NON_CONFORMABLE_ARGS);
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
            } else {
                if (a.getLength() != b.getLength()) {
                    throw error(RError.Message.NON_CONFORMABLE_ARGS);
                }
                int result = 0;
                na.enable(result);
                for (int k = 0; k < a.getLength(); k++) {
                    result = add.applyInteger(result, mult.applyInteger(a.getDataAt(k), b.getDataAt(k)));
                    na.check(result);
                }
                return RDataFactory.createIntVector(new int[]{result}, na.neverSeenNA(), new int[]{1, 1});
            }
        }
    }

    // logical-logical

    @Specialization
    protected RIntVector multiply(RAbstractLogicalVector aOriginal, RAbstractLogicalVector bOriginal,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix) {
        return intMultiply((RAbstractIntVector) aOriginal.castSafe(RType.Integer, isNAProfile), (RAbstractIntVector) bOriginal.castSafe(RType.Integer, isNAProfile), aIsMatrix, bIsMatrix);
    }

    // to int

    @Specialization
    protected RIntVector multiply(RAbstractLogicalVector aOriginal, RAbstractIntVector b,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix) {
        return intMultiply((RAbstractIntVector) aOriginal.castSafe(RType.Integer, isNAProfile), b, aIsMatrix, bIsMatrix);
    }

    @Specialization
    protected RIntVector multiply(RAbstractIntVector a, RAbstractLogicalVector bOriginal,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix) {
        return intMultiply(a, (RAbstractIntVector) bOriginal.castSafe(RType.Integer, isNAProfile), aIsMatrix, bIsMatrix);
    }

    // to double

    @Specialization(guards = {"!isRAbstractComplexVector(a)"})
    protected RDoubleVector multiply(RAbstractAtomicVector a, RAbstractDoubleVector b,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile lengthEquals,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return doubleMultiply((RAbstractDoubleVector) a.castSafe(RType.Double, isNAProfile), b, aIsMatrix, bIsMatrix, lengthEquals);
    }

    @Specialization(guards = {"!isRAbstractComplexVector(b)"})
    protected RDoubleVector multiply(RAbstractDoubleVector a, RAbstractAtomicVector b,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile lengthEquals,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return doubleMultiply(a, (RAbstractDoubleVector) b.castSafe(RType.Double, isNAProfile), aIsMatrix, bIsMatrix, lengthEquals);
    }

    // to complex

    @Specialization
    protected RComplexVector multiply(RAbstractAtomicVector a, RAbstractComplexVector b,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return complexMultiply((RAbstractComplexVector) a.castSafe(RType.Complex, isNAProfile), b, aIsMatrix, bIsMatrix);
    }

    @Specialization
    protected RComplexVector multiply(RAbstractComplexVector a, RAbstractAtomicVector b,
                    @Cached("createBinaryProfile()") ConditionProfile aIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile bIsMatrix,
                    @Cached("createBinaryProfile()") ConditionProfile isNAProfile) {
        return complexMultiply(a, (RAbstractComplexVector) b.castSafe(RType.Complex, isNAProfile), aIsMatrix, bIsMatrix);
    }

    // errors

    @Fallback
    @TruffleBoundary
    protected RDoubleVector doRaw(@SuppressWarnings("unused") Object a, @SuppressWarnings("unused") Object b) {
        throw error(RError.Message.NUMERIC_COMPLEX_MATRIX_VECTOR);
    }

    // guards

    protected boolean bothZeroDim(RAbstractVector a, RAbstractVector b) {
        return hasZeroDim(a, getADimsNode) && hasZeroDim(b, getBDimsNode);
    }

    protected boolean hasZeroDim(RAbstractVector v, GetDimAttributeNode getDimsNode) {
        int[] dims = getDimsNode.getDimensions(v);
        if (dims == null || dims.length == 0) {
            return false;
        }
        for (int d : dims) {
            if (d == 0) {
                return true;
            }
        }
        return false;
    }
}
