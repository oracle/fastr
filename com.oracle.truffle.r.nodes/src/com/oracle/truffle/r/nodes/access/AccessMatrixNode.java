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
package com.oracle.truffle.r.nodes.access;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.AccessVectorNodeFactory.AccessVectorVectorCastFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild("vector"), @NodeChild("firstPosition"), @NodeChild("secondPosition")})
public abstract class AccessMatrixNode extends RNode {

    private final NACheck firstPositionNACheck = NACheck.create();
    private final NACheck secondPositionNACheck = NACheck.create();
    private final NACheck elementNACheck = NACheck.create();

    @CompilationFinal private boolean isSubset;

    public void setSubset(boolean s) {
        isSubset = s;
    }

    @CompilationFinal private boolean everSeenZeroPosition;

    @CreateCast({"vector"})
    public RNode createCastVector(RNode child) {
        return AccessVectorVectorCastFactory.create(child);
    }

    @CreateCast({"firstPosition"})
    public RNode createCastFirstPosition(RNode child) {
        return VectorPositionCastFactory.create(firstPositionNACheck, child);
    }

    @CreateCast({"secondPosition"})
    public RNode createCastSecondPosition(RNode child) {
        return VectorPositionCastFactory.create(secondPositionNACheck, child);
    }

    // Vector is NULL

    @Specialization(order = 0)
    public RNull access(RNull vector, @SuppressWarnings("unused") Object firstPosition, @SuppressWarnings("unused") Object secondPosition) {
        return vector;
    }

    // Int matrix access

    @Specialization(order = 200, guards = "bothInBounds")
    public int access(RIntVector vector, int firstPosition, int secondPosition) {
        return accessInBoundsIntMatrix(vector, firstPosition, secondPosition);
    }

    @Specialization(order = 201)
    @SuppressWarnings("unused")
    public RIntVector access(RIntVector vector, RMissing firstPosition, RMissing secondPosition) {
        return vector;
    }

    @SuppressWarnings("unused")
    protected static boolean arg2isZero(Object arg0, Object arg1, int value) {
        return value == 0;
    }

    @Specialization(order = 220, guards = "arg2isZero")
    public RIntVector accessMissing0(RIntVector vector, @SuppressWarnings("unused") RMissing firstPosition, @SuppressWarnings("unused") int secondPosition) {
        return createVectorWithZeroDim(vector, 1);
    }

    @SuppressWarnings("unchecked")
    private static <T extends RVector> T createVectorWithZeroDim(T vector, int dim) {
        int[] dimensions = vector.getDimensions();
        assert dimensions != null && dimensions.length >= 2;
        dimensions[dim] = 0;
        return (T) vector.createEmptySameType(0, true).copyWithNewDimensions(dimensions);
    }

    @SuppressWarnings("unused")
    protected static boolean arg2Negative(Object arg0, Object arg1, int value) {
        return value < 0;
    }

    @Specialization(order = 221, guards = "arg2Negative")
    public RIntVector accessRemove(RIntVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        final int resultNcols = ncols - 1;
        final int removeCol = -secondPosition;

        // result has one column less (as secondPosition is negative: remove that column)
        int[] result = new int[nrows * resultNcols];
        // dimensions degenerate to vector if there is only one column remaining
        int[] resultDim = resultNcols == 1 ? null : new int[]{nrows, resultNcols};

        int w = 0; // write index
        elementNACheck.enable(true);

        // copy up to removed column
        for (int r = 0; r < nrows * (removeCol - 1); ++r, ++w) {
            result[w] = vector.getDataAt(r);
            elementNACheck.check(result[w]);
        }

        // copy after removed column
        for (int r = nrows * removeCol; r < vector.getLength(); ++r, ++w) {
            result[w] = vector.getDataAt(r);
            elementNACheck.check(result[w]);
        }

        return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), resultDim);
    }

    @Specialization(order = 222, guards = "!arg2isZero")
    public RIntVector access(RIntVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int[] result = new int[nrows];
        elementNACheck.enable(true);
        for (int i = 0; i < nrows; ++i) {
            result[i] = vector.getDataAt((secondPosition - 1) * nrows + i);
            elementNACheck.check(result[i]);
        }
        return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    protected boolean arg2hasNegative(Object arg0, int arg1, RIntVector arg2) {
        for (int i = 0; i < arg2.getLength(); ++i) {
            if (arg2.getDataAt(i) < 0) {
                return true;
            }
        }
        return false;
    }

    @Specialization(order = 223, guards = "arg2hasNegative")
    public RIntVector accessRemove(RIntVector vector, int firstPosition, RIntVector secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];

        // assemble the columns to be removed (increasing order)
        // (ignore duplicates and nonexistent colums; the first 0 in the array terminates)
        int[] removeCols = new int[secondPosition.getLength()];
        int colsToRemove = 0;
        for (int i = 0; i < secondPosition.getLength(); ++i) {
            int removeCol = -secondPosition.getDataAt(i);
            if (removeCol > 0 && removeCol <= ncols) {
                // duplicate?
                boolean duplicate = false;
                for (int j = 0; j < removeCols.length; ++j) {
                    if (removeCols[j] == 0) {
                        break;
                    }
                    if (removeCols[j] == removeCol) {
                        duplicate = true;
                        break;
                    }
                }
                if (duplicate) {
                    continue;
                }
                // insert
                ++colsToRemove;
                for (int j = 0; j < removeCols.length; ++j) {
                    if (removeCols[j] == 0) {
                        removeCols[j] = removeCol;
                        break;
                    }
                    if (removeCols[j] < removeCol) {
                        for (int k = j + 1; k < removeCols.length; ++k) {
                            boolean was0 = removeCols[k] == 0;
                            removeCols[k] = removeCols[k - 1];
                            if (was0) {
                                break;
                            }
                        }
                        removeCols[j] = removeCol;
                        break;
                    }
                }
            }
        }

        final int resultNcols = ncols - colsToRemove;

        // result has less columns
        int[] result = new int[resultNcols];

        int r = firstPosition - 1; // read index (starts at given row, column 0)
        int w = 0; // write index
        elementNACheck.enable(true);

        int removeCol = 0;
        for (int rc : removeCols) {
            if (rc == 0) {
                break;
            }

            removeCol = rc;

            // copy up to next removed column
            while (r < nrows * (removeCol - 1)) {
                result[w] = vector.getDataAt(r);
                elementNACheck.check(result[w]);
                r += nrows;
                ++w;
            }

            r += nrows;
        }

        // copy after last removed column
        while (r < vector.getLength()) {
            result[w] = vector.getDataAt(r);
            elementNACheck.check(result[w]);
            r += nrows;
            ++w;
        }

        return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA());
    }

    @Specialization(order = 224)
    public RIntVector access(RIntVector vector, int firstPosition, RIntVector secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int[] result = new int[secondPosition.getLength()];
        elementNACheck.enable(true);
        for (int i = 0; i < result.length; ++i) {
            result[i] = vector.getDataAt(nrows * (secondPosition.getDataAt(i) - 1) + firstPosition - 1);
            elementNACheck.check(result[i]);
        }
        return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    protected static boolean arg1isZero(Object arg0, int value) {
        return value == 0;
    }

    @Specialization(order = 230, guards = "arg1isZero")
    public RIntVector access0Missing(RIntVector vector, @SuppressWarnings("unused") int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        return createVectorWithZeroDim(vector, 0);
    }

    @Specialization(order = 231, guards = "!arg1isZero")
    public RIntVector access(RIntVector vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        int[] result = new int[ncols];
        elementNACheck.enable(true);
        for (int i = 0; i < ncols; ++i) {
            result[i] = vector.getDataAt(i * nrows + (firstPosition - 1));
            elementNACheck.check(result[i]);
        }
        return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA());
    }

    // Double matrix access

    @Specialization(order = 300, guards = "bothInBounds")
    public double access(RDoubleVector vector, int firstPosition, int secondPosition) {
        return accessInBoundsDoubleMatrix(vector, firstPosition, secondPosition);
    }

    @Specialization(order = 301)
    @SuppressWarnings("unused")
    public RDoubleVector access(RDoubleVector vector, RMissing firstPosition, RMissing secondPosition) {
        return vector;
    }

    @Specialization(order = 320, guards = "arg2isZero")
    public RDoubleVector accessMissing0(RDoubleVector vector, @SuppressWarnings("unused") RMissing firstPosition, @SuppressWarnings("unused") int secondPosition) {
        return createVectorWithZeroDim(vector, 1);
    }

    @Specialization(order = 321, guards = "!arg2isZero")
    public RDoubleVector access(RDoubleVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        double[] result = new double[nrows];
        elementNACheck.enable(true);
        for (int i = 0; i < nrows; ++i) {
            result[i] = vector.getDataAt((secondPosition - 1) * nrows + i);
            elementNACheck.check(result[i]);
        }
        return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA());
    }

    @Specialization(order = 330, guards = "arg1isZero")
    public RDoubleVector access0Missing(RDoubleVector vector, @SuppressWarnings("unused") int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        return createVectorWithZeroDim(vector, 0);
    }

    @Specialization(order = 331, guards = "!arg1isZero")
    public RDoubleVector access(RDoubleVector vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        double[] result = new double[ncols];
        elementNACheck.enable(vector);
        for (int i = 0; i < ncols; ++i) {
            result[i] = vector.getDataAt(i * nrows + (firstPosition - 1));
            elementNACheck.check(result[i]);
        }
        return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA());
    }

    @Specialization(order = 350)
    public RDoubleVector access(RDoubleVector vector, RIntSequence firstPosition, RIntSequence secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        double[] result = new double[firstPosition.getLength() * secondPosition.getLength()];
        elementNACheck.enable(vector);
        for (int j = 0, q = secondPosition.getStart(); j < secondPosition.getLength(); j++, q += secondPosition.getStride()) {
            for (int i = 0, p = firstPosition.getStart(); i < firstPosition.getLength(); i++, p += firstPosition.getStride()) {
                double value = vector.getDataAt((q - 1) * nrows + (p - 1));
                result[j * firstPosition.getLength() + i] = value;
                elementNACheck.check(value);
            }
        }
        return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), new int[]{firstPosition.getLength(), secondPosition.getLength()});
    }

    @Specialization(order = 380)
    public RDoubleVector access(RDoubleVector vector, RMissing firstPosition, RIntVector secondPosition) {
        int secondLength = secondPosition.getLength();
        secondPositionNACheck.enable(secondPosition);
        if (secondLength == 1) {
            if (secondPositionNACheck.check(secondPosition.getDataAt(0))) {
                // returns a vector of the same dimensions, but filled with NA
                double[] data = new double[vector.getLength()];
                Arrays.fill(data, RRuntime.DOUBLE_NA);
                return RDataFactory.createDoubleVector(data, false, vector.getDimensions());
            } else {
                return access(vector, firstPosition, secondPosition.getDataAt(0));
            }
        } else {
            assert vector.isMatrix() && vector.getDimensions().length == 2;
            final int nrows = vector.getDimensions()[0];
            int selectedCols = secondLength;
            for (int i = 0; i < secondLength; i++) {
                if (secondPosition.getDataAt(i) == 0) {
                    --selectedCols;
                    if (!everSeenZeroPosition) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        everSeenZeroPosition = true;
                    }
                }
            }
            final int resultLength = nrows * selectedCols;
            double[] result = new double[resultLength];
            elementNACheck.enable(vector);
            int resultcol = 0;
            for (int j = 0; j < secondLength; ++j) {
                int position = secondPosition.getDataAt(j);
                if (everSeenZeroPosition && position == 0) {
                    continue;
                }
                for (int i = 0; i < nrows; ++i) {
                    double value;
                    if (secondPositionNACheck.check(position)) {
                        value = RRuntime.DOUBLE_NA;
                    } else {
                        value = vector.getDataAt((position - 1) * nrows + i);
                        elementNACheck.check(value);
                    }
                    result[resultcol * nrows + i] = value;
                }
                ++resultcol;
            }
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA() && secondPositionNACheck.neverSeenNA(), new int[]{nrows, selectedCols});
        }
    }

    @Specialization(order = 381)
    public RDoubleVector access(RDoubleVector vector, RIntVector firstPosition, RMissing secondPosition) {
        if (firstPosition.getLength() == 1 && firstPosition.getDataAt(0) != RRuntime.INT_NA) {
            return access(vector, firstPosition.getDataAt(0), secondPosition);
        } else {
            throw Utils.nyi();
        }
    }

    // access String Matrix

    @Specialization(order = 400, guards = "bothInBounds")
    public String access(RStringVector vector, int firstPosition, int secondPosition) {
        return accessInBoundsStringMatrix(vector, firstPosition, secondPosition);
    }

    @Specialization(order = 401)
    @SuppressWarnings("unused")
    public RStringVector access(RStringVector vector, RMissing firstPosition, RMissing secondPosition) {
        return vector;
    }

    @Specialization(order = 420, guards = "arg2isZero")
    public RStringVector accessMissing0(RStringVector vector, @SuppressWarnings("unused") RMissing firstPosition, @SuppressWarnings("unused") int secondPosition) {
        return createVectorWithZeroDim(vector, 1);
    }

    @Specialization(order = 421, guards = "!arg2isZero")
    public RStringVector access(RStringVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        String[] result = new String[nrows];
        elementNACheck.enable(vector);
        for (int i = 0; i < nrows; ++i) {
            result[i] = vector.getDataAt((secondPosition - 1) * nrows + i);
            elementNACheck.check(result[i]);
        }
        return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA());
    }

    @Specialization(order = 440, guards = "arg1isZero")
    public RStringVector access0Missing(RStringVector vector, @SuppressWarnings("unused") int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        return createVectorWithZeroDim(vector, 0);
    }

    @Specialization(order = 431, guards = "!arg1isZero")
    public RStringVector access(RStringVector vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        String[] result = new String[ncols];
        elementNACheck.enable(vector);
        for (int i = 0; i < ncols; ++i) {
            result[i] = vector.getDataAt(i * nrows + (firstPosition - 1));
            elementNACheck.check(result[i]);
        }
        return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA());
    }

    @Specialization(order = 432)
    public RStringVector access(RStringVector vector, int firstPosition, RIntVector secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        String[] result = new String[secondPosition.getLength()];
        elementNACheck.enable(vector);
        for (int i = 0; i < result.length; ++i) {
            result[i] = vector.getDataAt(nrows * (secondPosition.getDataAt(i) - 1) + firstPosition - 1);
            elementNACheck.check(result[i]);
        }
        return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA());
    }

    // helper functions

    private static int accessInBoundsIntMatrix(RIntVector vector, int firstPosition, int secondPosition) {
        return vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition));
    }

    private static double accessInBoundsDoubleMatrix(RDoubleVector vector, int firstPosition, int secondPosition) {
        return vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition));
    }

    private static String accessInBoundsStringMatrix(RStringVector vector, int firstPosition, int secondPosition) {
        return vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition));
    }

    protected boolean bothInBounds(RVector vector, int firstPosition, int secondPosition) {
        return vector.isInBounds(firstPosition, secondPosition);
    }

    public static AccessMatrixNode create(RNode vector, RNode firstPosition, RNode secondPosition) {
        return AccessMatrixNodeFactory.create(vector, firstPosition, secondPosition);
    }

    @NodeChild("operand")
    public abstract static class AccessVectorVectorCast extends RNode {

        @Specialization
        public RLogicalVector test(byte operand) {
            return RDataFactory.createLogicalVectorFromScalar(operand);
        }

        @Specialization
        public RIntVector doInt(int operand) {
            return RDataFactory.createIntVectorFromScalar(operand);
        }

        @Specialization
        public RDoubleVector doDouble(double operand) {
            return RDataFactory.createDoubleVectorFromScalar(operand);
        }

        @Specialization
        public RComplexVector doComplex(RComplex operand) {
            return RDataFactory.createComplexVectorFromScalar(operand);
        }

        @Specialization
        public RDoubleVector doDoubleSequence(RDoubleSequence operand) {
            return (RDoubleVector) operand.createVector();
        }

        @Specialization
        public RIntVector doIntVector(RIntSequence operand) {
            return (RIntVector) operand.createVector();
        }

        @Specialization
        public RIntVector doIntVector(RIntVector operand) {
            return operand;
        }

        @Specialization
        public RDoubleVector doDoubleVector(RDoubleVector operand) {
            return operand;
        }

        @Specialization
        public RComplexVector doComplexVector(RComplexVector operand) {
            return operand;
        }

        @Specialization
        public RLogicalVector doLogicalVector(RLogicalVector operand) {
            return operand;
        }

        @Specialization
        public RStringVector doStringVector(RStringVector operand) {
            return operand;
        }
    }
}
