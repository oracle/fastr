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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild("vector"), @NodeChild("firstPosition"), @NodeChild("secondPosition")})
public abstract class AccessMatrixNode extends RNode {

    private final NACheck elementNACheck = NACheck.create();
    private final NACheck namesNACheck = NACheck.create();

    @CompilationFinal private boolean isSubset;

    public void setSubset(boolean s) {
        isSubset = s;
    }

    @CompilationFinal private boolean everSeenZeroPosition;

    abstract RNode getVector();

    @CreateCast({"vector"})
    public RNode createCastVector(RNode child) {
        return CastToVectorNodeFactory.create(child, false, false);
    }

    @CreateCast({"firstPosition"})
    public RNode createCastFirstPosition(RNode child) {
        return ArrayPositionCastFactory.create(0, getVector(), child);
    }

    @CreateCast({"secondPosition"})
    public RNode createCastSecondPosition(RNode child) {
        return ArrayPositionCastFactory.create(1, getVector(), child);
    }

    // matrix is NULL

    @Specialization(order = 0)
    public RNull access(RNull vector, @SuppressWarnings("unused") Object firstPosition, @SuppressWarnings("unused") Object secondPosition) {
        return vector;
    }

    // no indexes

    @Specialization(order = 10)
    @SuppressWarnings("unused")
    public RAbstractVector access(RAbstractVector vector, RMissing firstPosition, RMissing secondPosition) {
        return vector;
    }

    // one of the indexes is 0 (empty vector but with different dimensions)

    private static void resetDim(RVector vector, int[] dimensions, int dim) {
        assert dimensions != null && dimensions.length >= 2;
        dimensions[dim] = 0;
        vector.setDimensions(dimensions);
    }

    private static void modifyDim(RVector vector, int[] dimensions, int dimToModify, int dimValue, int dimToReset) {
        dimensions[dimToModify] = dimValue;
        resetDim(vector, dimensions, dimToReset);
    }

    @Specialization(order = 20, guards = "bothPositionsZero")
    public RAbstractVector accessZeroZero(RAbstractVector vector, @SuppressWarnings("unused") int firstPosition, @SuppressWarnings("unused") int secondPosition) {
        RVector v = vector.materialize();
        RVector resultVector = v.createEmptySameType(0, true);
        resultVector.setDimensions(new int[2]);
        return resultVector;
    }

    @Specialization(order = 30, guards = "secondPositionZero")
    public RAbstractVector accessMissingZero(RAbstractVector vector, @SuppressWarnings("unused") RMissing firstPosition, @SuppressWarnings("unused") int secondPosition) {
        RVector v = vector.materialize();
        RVector resultVector = v.createEmptySameType(0, true);
        resetDim(resultVector, vector.getDimensions(), 1);
        return resultVector;
    }

    @Specialization(order = 31, guards = "secondPositionZero")
    public RAbstractVector accessPositiveZero(RAbstractVector vector, @SuppressWarnings("unused") int firstPosition, @SuppressWarnings("unused") int secondPosition) {
        RVector v = vector.materialize();
        return v.createEmptySameType(0, true);
    }

    @Specialization(order = 32, guards = "secondPositionZero")
    public RAbstractVector accessNegativeZero(RAbstractVector vector, RIntVector firstPosition, @SuppressWarnings("unused") int secondPosition) {
        RVector v = vector.materialize();
        RVector resultVector = v.createEmptySameType(0, true);
        modifyDim(resultVector, vector.getDimensions(), 0, firstPosition.getLength(), 1);
        return resultVector;
    }

    @Specialization(order = 40, guards = "firstPositionZero")
    public RAbstractVector accessZeroMissing(RAbstractVector vector, @SuppressWarnings("unused") int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        RVector v = vector.materialize();
        RVector resultVector = v.createEmptySameType(0, true);
        resetDim(resultVector, vector.getDimensions(), 0);
        return resultVector;
    }

    @Specialization(order = 41, guards = "firstPositionZero")
    public RAbstractVector accessZeroPositive(RAbstractVector vector, @SuppressWarnings("unused") int firstPosition, @SuppressWarnings("unused") int secondPosition) {
        RVector v = vector.materialize();
        return v.createEmptySameType(0, true);
    }

    @Specialization(order = 42, guards = "firstPositionZero")
    public RAbstractVector accessZeroNegative(RAbstractVector vector, @SuppressWarnings("unused") int firstPosition, RIntVector secondPosition) {
        RVector v = vector.materialize();
        RVector resultVector = v.createEmptySameType(0, true);
        modifyDim(resultVector, vector.getDimensions(), 1, secondPosition.getLength(), 0);
        return resultVector;
    }

    // Int matrix access

    @Specialization(order = 100, guards = "bothInBounds")
    public int access(RIntVector vector, int firstPosition, int secondPosition) {
        return vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition));
    }

    @Specialization(order = 110)
    public RIntVector access(RIntVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int[] result = new int[nrows];
        int columnBase = (secondPosition - 1) * nrows;
        elementNACheck.enable(true);
        // pick all elements from a chosen column
        for (int i = 0; i < nrows; i++) {
            int rowOffset = i;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(0)).copy());
        }
    }

    private void updateDimName(String[] data, int dataInd, RStringVector names, int namesInd) {
        if (names != null) {
            data[dataInd] = names.getDataAt(namesInd);
            namesNACheck.check(data[dataInd]);
        }
    }

    @Specialization(order = 120)
    public RIntVector access(RIntVector vector, RIntVector firstPosition, int secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int[] result = new int[firstPosition.getLength()];
        int columnBase = (secondPosition - 1) * nrows;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(0);
            namesData = new String[firstPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        // pick selected elements from a chosen column
        for (int i = 0; i < result.length; i++) {
            int rowOffset = firstPosition.getDataAt(i) - 1;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 130)
    public RIntVector access(RIntVector vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        int[] result = new int[ncols];
        int rowOffset = firstPosition - 1;
        elementNACheck.enable(!vector.isComplete());
        for (int i = 0; i < ncols; i++) {
            int columnBase = i * nrows;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(1)).copy());
        }
    }

    @Specialization(order = 140)
    public RIntVector access(RIntVector vector, int firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int[] result = new int[secondPosition.getLength()];
        int rowOffset = firstPosition - 1;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(1);
            namesData = new String[secondPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        for (int i = 0; i < result.length; ++i) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 150)
    public RIntVector access(RIntVector vector, @SuppressWarnings("unused") RMissing firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = nrows;
        assert resRowNum > 1; // due to how ArrayPositionCast works
        int[] result = new int[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = j;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }
        if (dimnames == null) {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RIntVector resultVector = RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 151)
    public RIntVector access(RIntVector vector, RIntVector firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = vector.getDimensions()[1];
        assert resColNum > 1; // due to how ArrayPositionCast works
        int resRowNum = firstPosition.getLength();
        int[] result = new int[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * i;
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RIntVector resultVector = RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 152)
    public RIntVector access(RIntVector vector, RIntVector firstPosition, RIntVector secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = firstPosition.getLength();
        int[] result = new int[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }
        if (dimnames == null) {
            return RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RIntVector resultVector = RDataFactory.createIntVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    // Double matrix access

    @Specialization(order = 200, guards = "bothInBounds")
    public double access(RDoubleVector vector, int firstPosition, int secondPosition) {
        return vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition));
    }

    @Specialization(order = 210)
    public RDoubleVector access(RDoubleVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        double[] result = new double[nrows];
        int columnBase = (secondPosition - 1) * nrows;
        elementNACheck.enable(true);
        // pick all elements from a chosen column
        for (int i = 0; i < nrows; i++) {
            int rowOffset = i;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(0)).copy());
        }
    }

    @Specialization(order = 220)
    public RDoubleVector access(RDoubleVector vector, RIntVector firstPosition, int secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        double[] result = new double[firstPosition.getLength()];
        int columnBase = (secondPosition - 1) * nrows;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(0);
            namesData = new String[firstPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        // pick selected elements from a chosen column
        for (int i = 0; i < result.length; i++) {
            int rowOffset = firstPosition.getDataAt(i) - 1;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 230)
    public RDoubleVector access(RDoubleVector vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        double[] result = new double[ncols];
        int rowOffset = firstPosition - 1;
        elementNACheck.enable(!vector.isComplete());
        for (int i = 0; i < ncols; i++) {
            int columnBase = i * nrows;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(1)).copy());
        }
    }

    @Specialization(order = 240)
    public RDoubleVector access(RDoubleVector vector, int firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        double[] result = new double[secondPosition.getLength()];
        int rowOffset = firstPosition - 1;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(1);
            namesData = new String[secondPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        for (int i = 0; i < result.length; ++i) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 250)
    public RDoubleVector access(RDoubleVector vector, @SuppressWarnings("unused") RMissing firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = nrows;
        double[] result = new double[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = j;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }
        if (dimnames == null) {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RDoubleVector resultVector = RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 251)
    public RDoubleVector access(RDoubleVector vector, RIntVector firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = vector.getDimensions()[1];
        int resRowNum = firstPosition.getLength();
        double[] result = new double[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * i;
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RDoubleVector resultVector = RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 252)
    public RDoubleVector access(RDoubleVector vector, RIntVector firstPosition, RIntVector secondPosition) {
        assert firstPosition.getLength() > 1;
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = firstPosition.getLength();
        double[] result = new double[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RDoubleVector resultVector = RDataFactory.createDoubleVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    // logical matrix access

    @Specialization(order = 300, guards = "bothInBounds")
    public byte access(RLogicalVector vector, int firstPosition, int secondPosition) {
        return vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition));
    }

    @Specialization(order = 310)
    public RLogicalVector access(RLogicalVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        byte[] result = new byte[nrows];
        int columnBase = (secondPosition - 1) * nrows;
        elementNACheck.enable(true);
        // pick all elements from a chosen column
        for (int i = 0; i < nrows; i++) {
            int rowOffset = i;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(0)).copy());
        }
    }

    @Specialization(order = 320)
    public RLogicalVector access(RLogicalVector vector, RIntVector firstPosition, int secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        byte[] result = new byte[firstPosition.getLength()];
        int columnBase = (secondPosition - 1) * nrows;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(0);
            namesData = new String[firstPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        // pick selected elements from a chosen column
        for (int i = 0; i < result.length; i++) {
            int rowOffset = firstPosition.getDataAt(i) - 1;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 330)
    public RLogicalVector access(RLogicalVector vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        byte[] result = new byte[ncols];
        int rowOffset = firstPosition - 1;
        elementNACheck.enable(!vector.isComplete());
        for (int i = 0; i < ncols; i++) {
            int columnBase = i * nrows;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(1)).copy());
        }
    }

    @Specialization(order = 340)
    public RLogicalVector access(RLogicalVector vector, int firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        byte[] result = new byte[secondPosition.getLength()];
        int rowOffset = firstPosition - 1;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(1);
            namesData = new String[secondPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        for (int i = 0; i < result.length; ++i) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 350)
    public RLogicalVector access(RLogicalVector vector, @SuppressWarnings("unused") RMissing firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = nrows;
        byte[] result = new byte[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = j;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }
        if (dimnames == null) {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RLogicalVector resultVector = RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 351)
    public RLogicalVector access(RLogicalVector vector, RIntVector firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = vector.getDimensions()[1];
        int resRowNum = firstPosition.getLength();
        byte[] result = new byte[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * i;
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RLogicalVector resultVector = RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 352)
    public RLogicalVector access(RLogicalVector vector, RIntVector firstPosition, RIntVector secondPosition) {
        assert firstPosition.getLength() > 1;
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = firstPosition.getLength();
        byte[] result = new byte[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RLogicalVector resultVector = RDataFactory.createLogicalVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    // String matrix acccess

    @Specialization(order = 400, guards = "bothInBounds")
    public String access(RStringVector vector, int firstPosition, int secondPosition) {
        return vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition));
    }

    @Specialization(order = 410)
    public RStringVector access(RStringVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        String[] result = new String[nrows];
        int columnBase = (secondPosition - 1) * nrows;
        elementNACheck.enable(true);
        // pick all elements from a chosen column
        for (int i = 0; i < nrows; i++) {
            int rowOffset = i;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(0)).copy());
        }
    }

    @Specialization(order = 420)
    public RStringVector access(RStringVector vector, RIntVector firstPosition, int secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        String[] result = new String[firstPosition.getLength()];
        int columnBase = (secondPosition - 1) * nrows;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(0);
            namesData = new String[firstPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        // pick selected elements from a chosen column
        for (int i = 0; i < result.length; i++) {
            int rowOffset = firstPosition.getDataAt(i) - 1;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 430)
    public RStringVector access(RStringVector vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        String[] result = new String[ncols];
        int rowOffset = firstPosition - 1;
        elementNACheck.enable(!vector.isComplete());
        for (int i = 0; i < ncols; i++) {
            int columnBase = i * nrows;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(1)).copy());
        }
    }

    @Specialization(order = 440)
    public RStringVector access(RStringVector vector, int firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        String[] result = new String[secondPosition.getLength()];
        int rowOffset = firstPosition - 1;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(1);
            namesData = new String[secondPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        for (int i = 0; i < result.length; ++i) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            result[i] = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(result[i]);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 450)
    public RStringVector access(RStringVector vector, @SuppressWarnings("unused") RMissing firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = nrows;
        String[] result = new String[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = j;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }
        if (dimnames == null) {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RStringVector resultVector = RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 451)
    public RStringVector access(RStringVector vector, RIntVector firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = vector.getDimensions()[1];
        int resRowNum = firstPosition.getLength();
        String[] result = new String[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * i;
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RStringVector resultVector = RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 452)
    public RStringVector access(RStringVector vector, RIntVector firstPosition, RIntVector secondPosition) {
        assert firstPosition.getLength() > 1;
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = firstPosition.getLength();
        String[] result = new String[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(result[ind]);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RStringVector resultVector = RDataFactory.createStringVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    // RComplex matrix access

    @Specialization(order = 500, guards = "bothInBounds")
    public RComplex access(RComplexVector vector, int firstPosition, int secondPosition) {
        return vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition));
    }

    @Specialization(order = 510)
    public RComplexVector access(RComplexVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        double[] result = new double[nrows << 1];
        int columnBase = (secondPosition - 1) * nrows;
        elementNACheck.enable(true);
        // pick all elements from a chosen column
        int ind = 0;
        for (int i = 0; i < nrows; i++) {
            int rowOffset = i;
            RComplex c = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(c);
            result[ind++] = c.getRealPart();
            result[ind++] = c.getImaginaryPart();
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(0)).copy());
        }
    }

    @Specialization(order = 520)
    public RComplexVector access(RComplexVector vector, RIntVector firstPosition, int secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        double[] result = new double[firstPosition.getLength() << 1];
        int columnBase = (secondPosition - 1) * nrows;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(0);
            namesData = new String[firstPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        // pick selected elements from a chosen column
        int ind = 0;
        for (int i = 0; i < firstPosition.getLength(); i++) {
            int rowOffset = firstPosition.getDataAt(i) - 1;
            RComplex c = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(c);
            updateDimName(namesData, i, names, columnBase + rowOffset);
            result[ind++] = c.getRealPart();
            result[ind++] = c.getImaginaryPart();
        }
        if (dimnames == null) {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 530)
    public RComplexVector access(RComplexVector vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        double[] result = new double[ncols << 1];
        int rowOffset = firstPosition - 1;
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < ncols; i++) {
            int columnBase = i * nrows;
            RComplex c = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(c);
            result[ind++] = c.getRealPart();
            result[ind++] = c.getImaginaryPart();
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), ((RStringVector) dimnames.getDataAt(1)).copy());
        }
    }

    @Specialization(order = 540)
    public RComplexVector access(RComplexVector vector, int firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        double[] result = new double[secondPosition.getLength() << 1];
        int rowOffset = firstPosition - 1;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(1);
            namesData = new String[secondPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < secondPosition.getLength(); ++i) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            RComplex c = vector.getDataAt(columnBase + rowOffset);
            elementNACheck.check(c);
            updateDimName(namesData, i, names, columnBase + rowOffset);
            result[ind++] = c.getRealPart();
            result[ind++] = c.getImaginaryPart();
        }
        if (dimnames == null) {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA());
        } else {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 550)
    public RComplexVector access(RComplexVector vector, @SuppressWarnings("unused") RMissing firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = nrows;
        double[] result = new double[(resColNum * resRowNum) << 1];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = j;
                RComplex c = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(c);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                result[ind++] = c.getRealPart();
                result[ind++] = c.getImaginaryPart();
            }
        }

        if (dimnames == null) {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RComplexVector resultVector = RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 551)
    public RComplexVector access(RComplexVector vector, RIntVector firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = vector.getDimensions()[1];
        int resRowNum = firstPosition.getLength();
        double[] result = new double[(resColNum * resRowNum) << 1];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * i;
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                RComplex c = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(c);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                result[ind++] = c.getRealPart();
                result[ind++] = c.getImaginaryPart();
            }
        }

        if (dimnames == null) {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RComplexVector resultVector = RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 552)
    public RComplexVector access(RComplexVector vector, RIntVector firstPosition, RIntVector secondPosition) {
        assert firstPosition.getLength() > 1;
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = firstPosition.getLength();
        double[] result = new double[(resColNum * resRowNum) << 1];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        elementNACheck.enable(!vector.isComplete());
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                RComplex c = vector.getDataAt(columnBase + rowOffset);
                elementNACheck.check(c);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                result[ind++] = c.getRealPart();
                result[ind++] = c.getImaginaryPart();
            }
        }

        if (dimnames == null) {
            return RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
        } else {
            RComplexVector resultVector = RDataFactory.createComplexVector(result, elementNACheck.neverSeenNA(), new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    // RRaw matrix access

    @Specialization(order = 600, guards = "bothInBounds")
    public RRaw access(RRawVector vector, int firstPosition, int secondPosition) {
        return vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition));
    }

    @Specialization(order = 610)
    public RRawVector access(RRawVector vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        byte[] result = new byte[nrows];
        int columnBase = (secondPosition - 1) * nrows;
        // pick all elements from a chosen column
        for (int i = 0; i < nrows; i++) {
            int rowOffset = i;
            result[i] = vector.getDataAt(columnBase + rowOffset).getValue();
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createRawVector(result);
        } else {
            return RDataFactory.createRawVector(result, ((RStringVector) dimnames.getDataAt(0)).copy());
        }
    }

    @Specialization(order = 620)
    public RRawVector access(RRawVector vector, RIntVector firstPosition, int secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        byte[] result = new byte[firstPosition.getLength()];
        int columnBase = (secondPosition - 1) * nrows;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(0);
            namesData = new String[firstPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        // pick selected elements from a chosen column
        for (int i = 0; i < result.length; i++) {
            int rowOffset = firstPosition.getDataAt(i) - 1;
            result[i] = vector.getDataAt(columnBase + rowOffset).getValue();
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createRawVector(result);
        } else {
            return RDataFactory.createRawVector(result, RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 630)
    public RRawVector access(RRawVector vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        byte[] result = new byte[ncols];
        int rowOffset = firstPosition - 1;
        for (int i = 0; i < ncols; i++) {
            int columnBase = i * nrows;
            result[i] = vector.getDataAt(columnBase + rowOffset).getValue();
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createRawVector(result);
        } else {
            return RDataFactory.createRawVector(result, ((RStringVector) dimnames.getDataAt(1)).copy());
        }
    }

    @Specialization(order = 640)
    public RRawVector access(RRawVector vector, int firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        byte[] result = new byte[secondPosition.getLength()];
        int rowOffset = firstPosition - 1;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(1);
            namesData = new String[secondPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        for (int i = 0; i < result.length; ++i) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            result[i] = vector.getDataAt(columnBase + rowOffset).getValue();
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createRawVector(result);
        } else {
            return RDataFactory.createRawVector(result, RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 650)
    public RRawVector access(RRawVector vector, @SuppressWarnings("unused") RMissing firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = nrows;
        byte[] result = new byte[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = j;
                result[ind] = vector.getDataAt(columnBase + rowOffset).getValue();
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }
        if (dimnames == null) {
            return RDataFactory.createRawVector(result, new int[]{resRowNum, resColNum});
        } else {
            RRawVector resultVector = RDataFactory.createRawVector(result, new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 651)
    public RRawVector access(RRawVector vector, RIntVector firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = vector.getDimensions()[1];
        int resRowNum = firstPosition.getLength();
        byte[] result = new byte[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * i;
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset).getValue();
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createRawVector(result, new int[]{resRowNum, resColNum});
        } else {
            RRawVector resultVector = RDataFactory.createRawVector(result, new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 652)
    public RRawVector access(RRawVector vector, RIntVector firstPosition, RIntVector secondPosition) {
        assert firstPosition.getLength() > 1;
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = firstPosition.getLength();
        byte[] result = new byte[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset).getValue();
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createRawVector(result, new int[]{resRowNum, resColNum});
        } else {
            RRawVector resultVector = RDataFactory.createRawVector(result, new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    // list matrix access

    @Specialization(order = 700, guards = "bothInBounds")
    public RList access(RList vector, int firstPosition, int secondPosition) {
        return RDataFactory.createList(new Object[]{vector.getDataAt(vector.convertToIndex(firstPosition, secondPosition))});
    }

    @Specialization(order = 710)
    public RList access(RList vector, @SuppressWarnings("unused") RMissing firstPosition, int secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        Object[] result = new Object[nrows];
        int columnBase = (secondPosition - 1) * nrows;
        // pick all elements from a chosen column
        for (int i = 0; i < nrows; i++) {
            int rowOffset = i;
            result[i] = vector.getDataAt(columnBase + rowOffset);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createList(result);
        } else {
            return RDataFactory.createList(result, ((RStringVector) dimnames.getDataAt(0)).copy());
        }
    }

    @Specialization(order = 720)
    public RList access(RList vector, RIntVector firstPosition, int secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        Object[] result = new Object[firstPosition.getLength()];
        int columnBase = (secondPosition - 1) * nrows;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(0);
            namesData = new String[firstPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        // pick selected elements from a chosen column
        for (int i = 0; i < result.length; i++) {
            int rowOffset = firstPosition.getDataAt(i) - 1;
            result[i] = vector.getDataAt(columnBase + rowOffset);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createList(result);
        } else {
            return RDataFactory.createList(result, RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 730)
    public RList access(RList vector, int firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        final int ncols = vector.getDimensions()[1];
        Object[] result = new Object[ncols];
        int rowOffset = firstPosition - 1;
        for (int i = 0; i < ncols; i++) {
            int columnBase = i * nrows;
            result[i] = vector.getDataAt(columnBase + rowOffset);
        }
        RList dimnames = vector.getDimNames();
        if (dimnames == null) {
            return RDataFactory.createList(result);
        } else {
            return RDataFactory.createList(result, ((RStringVector) dimnames.getDataAt(1)).copy());
        }
    }

    @Specialization(order = 740)
    public RList access(RList vector, int firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        Object[] result = new Object[secondPosition.getLength()];
        int rowOffset = firstPosition - 1;
        RList dimnames = vector.getDimNames();
        String[] namesData = null;
        RStringVector names = null;
        if (dimnames != null) {
            names = (RStringVector) dimnames.getDataAt(1);
            namesData = new String[secondPosition.getLength()];
            namesNACheck.enable(!names.isComplete());
        }
        for (int i = 0; i < result.length; ++i) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            result[i] = vector.getDataAt(columnBase + rowOffset);
            updateDimName(namesData, i, names, columnBase + rowOffset);
        }
        if (dimnames == null) {
            return RDataFactory.createList(result);
        } else {
            return RDataFactory.createList(result, RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA()));
        }
    }

    @Specialization(order = 750)
    public RList access(RList vector, @SuppressWarnings("unused") RMissing firstPosition, RIntVector secondPosition) {
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = nrows;
        Object[] result = new Object[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = j;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }
        if (dimnames == null) {
            return RDataFactory.createList(result, new int[]{resRowNum, resColNum});
        } else {
            RList resultVector = RDataFactory.createList(result, new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 751)
    public RList access(RList vector, RIntVector firstPosition, @SuppressWarnings("unused") RMissing secondPosition) {
        assert firstPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = vector.getDimensions()[1];
        int resRowNum = firstPosition.getLength();
        Object[] result = new Object[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * i;
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createList(result, new int[]{resRowNum, resColNum});
        } else {
            RList resultVector = RDataFactory.createList(result, new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    @Specialization(order = 752)
    public RList access(RList vector, RIntVector firstPosition, RIntVector secondPosition) {
        assert firstPosition.getLength() > 1;
        assert secondPosition.getLength() > 1;
        assert vector.isMatrix() && vector.getDimensions().length == 2;
        final int nrows = vector.getDimensions()[0];
        int resColNum = secondPosition.getLength();
        int resRowNum = firstPosition.getLength();
        Object[] result = new Object[resColNum * resRowNum];
        RList dimnames = vector.getDimNames();
        String[] rowNamesData = null;
        String[] colNamesData = null;
        RStringVector rowNames = null;
        RStringVector colNames = null;
        if (dimnames != null) {
            rowNames = (RStringVector) dimnames.getDataAt(0);
            colNames = (RStringVector) dimnames.getDataAt(1);
            rowNamesData = new String[resRowNum];
            colNamesData = new String[resColNum];
            namesNACheck.enable(!rowNames.isComplete() || !colNames.isComplete());
        }
        int ind = 0;
        for (int i = 0; i < resColNum; i++) {
            int columnBase = nrows * (secondPosition.getDataAt(i) - 1);
            updateDimName(colNamesData, i, colNames, columnBase);
            for (int j = 0; j < resRowNum; j++) {
                int rowOffset = firstPosition.getDataAt(j) - 1;
                result[ind] = vector.getDataAt(columnBase + rowOffset);
                updateDimName(rowNamesData, j, rowNames, rowOffset);
                ind++;
            }
        }

        if (dimnames == null) {
            return RDataFactory.createList(result, new int[]{resRowNum, resColNum});
        } else {
            RList resultVector = RDataFactory.createList(result, new int[]{resRowNum, resColNum});
            resultVector.setDimNames(RDataFactory.createList(new Object[]{rowNamesData, colNamesData}));
            return resultVector;
        }
    }

    // helper functions

    protected boolean bothInBounds(RVector vector, int firstPosition, int secondPosition) {
        return vector.isInBounds(firstPosition, secondPosition);
    }

    @SuppressWarnings("unused")
    protected static boolean bothPositionsZero(RAbstractVector vector, int firstPosition, int secondPosition) {
        return firstPosition == 0 && secondPosition == 0;
    }

    @SuppressWarnings("unused")
    protected static boolean secondPositionZero(RAbstractVector vector, Object firstPosition, int secondPosition) {
        return secondPosition == 0;
    }

    @SuppressWarnings("unused")
    protected static boolean secondPositionZero(RAbstractVector vector, int firstPosition, int secondPosition) {
        return secondPosition == 0;
    }

    @SuppressWarnings("unused")
    protected static boolean firstPositionZero(RAbstractVector vector, int firstPosition) {
        return firstPosition == 0;
    }

    public static AccessMatrixNode create(RNode vector, RNode firstPosition, RNode secondPosition) {
        return AccessMatrixNodeFactory.create(vector, firstPosition, secondPosition);
    }
}
