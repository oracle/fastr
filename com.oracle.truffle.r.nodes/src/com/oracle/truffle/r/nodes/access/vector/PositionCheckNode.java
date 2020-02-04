/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.vector.PositionCheckNodeFactory.Mat2indsubNodeGen;
import com.oracle.truffle.r.nodes.access.vector.PositionsCheckNode.PositionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

import java.util.Arrays;

/**
 * Handles casting of a position to either integer vector, logical vector or missing, which can than
 * be handled by {@link WriteIndexedVectorNode}.
 *
 * There is a PositionCheckNode for each position object given as an argument to any subsetting or
 * subscripting operator, eg.:
 * <ul>
 * <li>For m[i, j], where m is matrix and i,j are numerics or character vectors, there are two
 * position objects, hence number of positions is 2.</li>
 * <li>For a[m], where a is array or matrix and m is matrix, there is one position object m.</li>
 * </ul>
 *
 * This node delegates to {@link PositionCastNode} that casts to integer, or logical, or missing, or
 * string vector and then this node handles further error reporting, position value transformation
 * (e.g., names to integer indexes) and normalization.
 * 
 * The subclasses implement logic specific to subset/subscript in the abstract {@code execute}
 * method, so in practice via {@code Specialization}s called from the generated {@code execute}
 * method.
 *
 * Note one special exception: {@link PositionCastNode} does not cast {@code double}s if the mode is
 * subset, and instead the double positions are handled in the {@link PositionCheckSubsetNode}.
 */
abstract class PositionCheckNode extends RBaseNode {

    private final Class<?> positionClass;
    private final int positionsIndex;
    protected final int numPositions;
    protected final boolean replace;
    protected final RType containerType;
    private final ElementAccessMode mode;
    private final VectorLengthProfile positionLengthProfile = VectorLengthProfile.create();
    private final ValueProfile positionClassProfile = ValueProfile.createClassProfile();
    protected final BranchProfile error = BranchProfile.create();
    @Child private PositionCastNode castNode;
    @Child private PositionCharacterLookupNode characterLookup;

    // Fields specific for subsetting by matrix.
    private final boolean hasMatrixPosition;
    @Child private GetDimAttributeNode getDimsNode;
    private final ConditionProfile vectorNullDimensionsProfile;
    @Child private Matrix2IndexCache matrix2IndexCache;

    PositionCheckNode(ElementAccessMode mode, RType containerType, Object positionValue, int positionIndex, int numPositions, boolean exact, boolean replace) {
        this.positionClass = positionValue.getClass();
        this.positionsIndex = positionIndex;
        this.numPositions = numPositions;
        this.mode = mode;
        this.replace = replace;
        this.containerType = containerType;
        this.castNode = PositionCastNode.create(mode, replace);
        if (positionValue instanceof String || positionValue instanceof RAbstractStringVector) {
            boolean useNAForNotFound = !replace && isListLike(containerType) && mode.isSubscript();
            characterLookup = new PositionCharacterLookupNode(mode, numPositions, positionIndex, useNAForNotFound, exact);
        }
        if (mode.isSubset()) {
            this.getDimsNode = GetDimAttributeNode.create();
            this.vectorNullDimensionsProfile = ConditionProfile.createBinaryProfile();
            this.hasMatrixPosition = isMatrix(positionValue);
            if (this.hasMatrixPosition) {
                assert positionIndex == 0;
                assert numPositions == 1;
                this.matrix2IndexCache = new Matrix2IndexCache(exact);
            }
        } else {
            this.getDimsNode = null;
            this.hasMatrixPosition = false;
            this.vectorNullDimensionsProfile = null;
            this.matrix2IndexCache = null;
        }
    }

    protected static boolean isListLike(RType type) {
        switch (type) {
            case Language:
            case Expression:
            case PairList:
            case List:
                return true;
        }
        return false;
    }

    private boolean isMatrix(Object position) {
        if (position instanceof RAbstractVector) {
            return getDimsNode.isMatrix((RAbstractVector) position);
        } else {
            return false;
        }
    }

    public boolean isIgnoreDimension() {
        return positionClass == RMissing.class;
    }

    public Class<?> getPositionClass() {
        return positionClass;
    }

    public final boolean isSupported(Object position) {
        if (mode.isSubscript()) {
            return position.getClass() == positionClass;
        } else {
            if (hasMatrixPosition && isMatrix(position)) {
                return true;
            } else if (hasMatrixPosition && !isMatrix(position) ||
                            (!hasMatrixPosition && isMatrix(position))) {
                return false;
            } else if (!hasMatrixPosition && !isMatrix(position)) {
                return position.getClass() == positionClass;
            } else {
                return false;
            }
        }
    }

    public static PositionCheckNode createNode(ElementAccessMode mode, RType containerType, Object position, int positionIndex, int numPositions, boolean exact, boolean replace, boolean recursive) {
        if (mode.isSubset()) {
            return PositionCheckSubsetNodeGen.create(mode, containerType, position, positionIndex, numPositions, exact, replace);
        } else {
            return PositionCheckSubscriptNodeGen.create(mode, containerType, position, positionIndex, numPositions, exact, replace, recursive);
        }
    }

    protected boolean isMultiDimension() {
        return numPositions > 1;
    }

    public final Object execute(PositionProfile profile, RAbstractContainer vector, int[] vectorDimensions, int vectorLength, Object position) {
        Object castPosition = castNode.execute(positionClass.cast(position));

        if (mode.isSubscript() && isMissing()) {
            if (!isListLike(containerType)) {
                throw error(Message.SUBSCRIPT_BOUNDS);
            }
        }

        int dimensionLength;
        if (numPositions == 1) {
            dimensionLength = vectorLength;
        } else {
            assert vectorDimensions != null;
            assert vectorDimensions.length == numPositions;
            dimensionLength = vectorDimensions[positionsIndex];
        }

        boolean positionCasted = false;
        if (hasMatrixPosition) {
            assert mode.isSubset();
            final int[] vectorDim = getDimsNode.getDimensions(vector);
            if (vectorNullDimensionsProfile.profile(vectorDim != null)) {
                assert castPosition instanceof RAbstractVector;
                Object newCastPosition = handleIndexingArrayByMatrix(vector, vectorDim, (RAbstractVector) castPosition);
                if (newCastPosition != null) {
                    castPosition = newCastPosition;
                    positionCasted = true;
                }
            }
        }

        if (!positionCasted && characterLookup != null) {
            castPosition = characterLookup.execute(vector, (RAbstractStringVector) castPosition, dimensionLength);
        }

        RBaseObject positionVector = (RBaseObject) profilePosition(castPosition);

        int positionLength;
        if (positionVector instanceof RMissing) {
            positionLength = -1;
        } else {
            positionLength = positionLengthProfile.profile(((RAbstractVector) positionVector).getLength());
        }

        assert isValidCastedType(positionVector) : "result type of a position cast node must be integer or logical";

        return execute(profile, dimensionLength, positionVector, positionLength);
    }

    private Object handleIndexingArrayByMatrix(RAbstractContainer vector, int[] vectorDim, RAbstractVector matrixPosition) {
        assert vectorDim.length >= 2;
        int[] posDim = getDimsNode.getDimensions(matrixPosition);
        assert posDim != null;
        assert posDim.length == 2;

        if (posDim[1] == vectorDim.length) {
            if (matrixPosition instanceof RIntVector || matrixPosition instanceof RAbstractDoubleVector) {
                return matrix2IndexCache.mat2indsub.execute(vectorDim, matrixPosition, posDim);
            } else if (matrixPosition instanceof RAbstractStringVector) {
                return matrix2IndexCache.strmat2indsub.execute(vector, vectorDim, (RAbstractStringVector) matrixPosition, posDim);
            }
        }
        return null;
    }

    private final ValueProfile castedValue = ValueProfile.createClassProfile();

    private Object profilePosition(Object positionVector) {
        return castedValue.profile(positionVector);
    }

    private static boolean isValidCastedType(RBaseObject positionVector) {
        RType type = positionVector.getRType();
        return type == RType.Integer || type == RType.Logical || type == RType.Double || type == RType.Null;
    }

    public abstract Object execute(PositionProfile statistics, int dimensionLength, Object position, int positionLength);

    /*
     * Transcribed from GnuR subscript.c/mat2indsub
     *
     * Special Matrix Subscripting: Handles the case x[i] where x is an n-way array and i is a
     * matrix with n columns. This code returns a vector containing the subscripts to be extracted
     * when x is regarded as unravelled.
     *
     * Negative indices are not allowed.
     *
     * A zero/NA anywhere in a row will cause a zero/NA in the same position in the result.
     */
    public abstract static class Mat2indsubNode extends RBaseNode {

        public abstract RAbstractVector execute(int[] vectorDimensions, RAbstractVector pos, int[] positionDimensions);

        private final BranchProfile error = BranchProfile.create();
        private final BranchProfile na = BranchProfile.create();

        @Specialization
        protected RIntVector doInt(int[] vectorDimensions, RIntVector intPos, int[] positionDimensions) {
            int numberOfPositions = positionDimensions[0];
            int[] flatIndexes = new int[numberOfPositions];

            for (int i = 0; i < numberOfPositions; i++) {
                flatIndexes[i] = 1;
            }
            for (int positionIdx = 0; positionIdx < numberOfPositions; positionIdx++) {
                int tdim = 1;
                for (int dimIdx = 0; dimIdx < vectorDimensions.length; dimIdx++) {

                    int indexIntoVector = intPos.getDataAt(positionIdx + dimIdx * numberOfPositions); // intPos[positionIdx,dimIdx]
                    if (indexIntoVector == RRuntime.INT_NA || indexIntoVector == 0) {
                        na.enter();
                        flatIndexes[positionIdx] = indexIntoVector;
                        break;
                    }
                    if (indexIntoVector < 0) {
                        error.enter();
                        throw error(RError.Message.GENERIC, "negative values are not allowed in a matrix subscript");
                    }
                    int dimLength = vectorDimensions[dimIdx];
                    if (indexIntoVector > dimLength) {
                        error.enter();
                        throw error(RError.Message.SUBSCRIPT_BOUNDS);
                    }
                    flatIndexes[positionIdx] += (indexIntoVector - 1) * tdim;
                    tdim *= dimLength;
                }
            }
            return RDataFactory.createIntVector(flatIndexes, intPos.isComplete());
        }

        @Specialization
        protected RAbstractDoubleVector doDouble(int[] vectorDimensions, RAbstractDoubleVector doublePos, int[] positionDimensions) {
            int numberOfPositions = positionDimensions[0];
            double[] flatIndexes = new double[numberOfPositions];

            for (int i = 0; i < numberOfPositions; i++) {
                flatIndexes[i] = 1;
            }
            for (int positionIdx = 0; positionIdx < numberOfPositions; positionIdx++) {
                int tdim = 1;
                for (int dimIdx = 0; dimIdx < vectorDimensions.length; dimIdx++) {
                    double indexIntoVector = doublePos.getDataAt(positionIdx + dimIdx * numberOfPositions);
                    if (RRuntime.isNAorNaN(indexIntoVector) || indexIntoVector == 0) {
                        na.enter();
                        flatIndexes[positionIdx] = indexIntoVector;
                        break;
                    }
                    if (indexIntoVector < 0) {
                        error.enter();
                        throw error(RError.Message.GENERIC, "negative values are not allowed in a matrix subscript");
                    }
                    int dimLength = vectorDimensions[dimIdx];
                    if (indexIntoVector > dimLength) {
                        error.enter();
                        throw error(RError.Message.SUBSCRIPT_BOUNDS);
                    }
                    flatIndexes[positionIdx] += (indexIntoVector - 1) * tdim;
                    tdim *= dimLength;
                }
            }
            return RDataFactory.createDoubleVector(flatIndexes, doublePos.isComplete());
        }
    }

    public static final class StringMatrixToIndexesNode extends RBaseNode {
        @Child private SearchFirstStringNode searchNode;
        @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();

        StringMatrixToIndexesNode(boolean exact) {
            searchNode = SearchFirstStringNode.createNode(exact, false);
        }

        public RAbstractVector execute(RAbstractContainer vector, int[] vectorDimensions, RAbstractStringVector matrixStringPos, int[] positionDimensions) {
            final RList dimNames = getDimNamesNode.getDimNames(vector);
            final int numDimensions = vectorDimensions.length;
            assert numDimensions >= 2;

            if (dimNames == null) {
                throw error(Message.NO_ARRAY_DIMNAMES);
            }
            return indexArrayDimNamesByMatrix(dimNames, vectorDimensions, matrixStringPos, positionDimensions);
        }

        private RAbstractVector indexArrayDimNamesByMatrix(RList dimNames, int[] vectorDimensions, RAbstractStringVector matrixStringPos, int[] positionDimensions) {
            final int numberOfPositions = positionDimensions[0];
            int[] flatIndexes = new int[numberOfPositions];
            Arrays.fill(flatIndexes, 1);
            boolean flatIndexesContainsNA = false;

            for (int positionIdx = 0; positionIdx < numberOfPositions; positionIdx++) {
                int tdim = 1;
                for (int dimIdx = 0; dimIdx < vectorDimensions.length; dimIdx++) {
                    Object namesForCurrentDimObj = dimNames.getDataAt(dimIdx);
                    if (RRuntime.isNull(namesForCurrentDimObj)) {
                        // TODO: Throw something more informative
                        throw error(Message.SUBSCRIPT_BOUNDS);
                    }
                    assert namesForCurrentDimObj instanceof RAbstractStringVector;
                    RAbstractStringVector namesForCurrentDim = (RAbstractStringVector) namesForCurrentDimObj;

                    String nameToFind = matrixStringPos.getDataAt(positionIdx + dimIdx * numberOfPositions);
                    if (RRuntime.isNA(nameToFind)) {
                        flatIndexes[positionIdx] = RRuntime.INT_NA;
                        flatIndexesContainsNA = true;
                        break;
                    }
                    RStringVector nameToFindVector = RDataFactory.createStringVectorFromScalar(nameToFind);
                    int notFoundStartIndex = namesForCurrentDim.getLength();
                    RIntVector foundIndexes = searchNode.apply(namesForCurrentDim, nameToFindVector, notFoundStartIndex, null);

                    assert foundIndexes.getLength() <= 1;
                    int foundIndex = foundIndexes.getDataAt(0);
                    if (foundIndex <= namesForCurrentDim.getLength()) {
                        flatIndexes[positionIdx] += (foundIndex - 1) * tdim;
                    } else {
                        throw error(Message.SUBSCRIPT_BOUNDS);
                    }

                    int dimLength = vectorDimensions[dimIdx];
                    tdim *= dimLength;
                }
            }
            return RDataFactory.createIntVector(flatIndexes, !flatIndexesContainsNA);
        }
    }

    private static final class Matrix2IndexCache extends Node {
        @Child private Mat2indsubNode mat2indsub;
        @Child private StringMatrixToIndexesNode strmat2indsub;

        Matrix2IndexCache(boolean exact) {
            this.mat2indsub = Mat2indsubNodeGen.create();
            this.strmat2indsub = new StringMatrixToIndexesNode(exact);
        }
    }

    public boolean isEmptyPosition(Object position) {
        if (positionClass == REmpty.class) {
            return false;
        }
        Object castPosition = positionClassProfile.profile(position);
        return castPosition instanceof RAbstractContainer && ((RAbstractContainer) castPosition).getLength() == 0;
    }

    public boolean isMissing() {
        return positionClass == RMissing.class || positionClass == REmpty.class || positionClass == RSymbol.class;
    }
}
