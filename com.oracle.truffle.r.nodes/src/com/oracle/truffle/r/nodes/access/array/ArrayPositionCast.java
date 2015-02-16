/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.array;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCastNodeGen.ContainerDimNamesGetNodeGen;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCastNodeGen.OperatorConverterNodeGen;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.ops.na.*;

abstract class ArrayPositionsCastBase extends RNode {

    protected final int dimension;
    protected final int numDimensions;
    protected final boolean assignment;
    protected final boolean isSubset;

    private final BranchProfile errorProfile = BranchProfile.create();

    protected ArrayPositionsCastBase(int dimension, int numDimensions, boolean assignment, boolean isSubset) {
        this.dimension = dimension;
        this.numDimensions = numDimensions;
        this.assignment = assignment;
        this.isSubset = isSubset;
    }

    protected ArrayPositionsCastBase(ArrayPositionsCastBase other) {
        this.dimension = other.dimension;
        this.numDimensions = other.numDimensions;
        this.assignment = other.assignment;
        this.isSubset = other.isSubset;
    }

    private final ConditionProfile nameConditionProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile naValueMet = BranchProfile.create();
    private final BranchProfile intVectorMet = BranchProfile.create();

    private final ConditionProfile dataFrameProfile = ConditionProfile.createBinaryProfile();

    protected int[] getDimensions(RAbstractContainer container) {
        if (dataFrameProfile.profile(container.getElementClass() == RDataFrame.class)) {
            // this largely reproduces code from ShortRowNames
            Object rowNames = container.getRowNames();
            if (nameConditionProfile.profile(rowNames == RNull.instance)) {
                return new int[]{0, container.getLength()};
            } else {
                return new int[]{Math.abs(calculateN((RAbstractVector) rowNames)), container.getLength()};
            }
        } else {
            return container.getDimensions();
        }
    }

    private int calculateN(RAbstractVector rowNames) {
        if (rowNames.getElementClass() == RInt.class && rowNames.getLength() == 2) {
            RAbstractIntVector rowNamesIntVector = (RAbstractIntVector) rowNames;
            intVectorMet.enter();
            if (RRuntime.isNA(rowNamesIntVector.getDataAt(0))) {
                naValueMet.enter();
                return rowNamesIntVector.getDataAt(1);
            }
        }
        return rowNames.getLength();
    }

    protected void verifyDimensions(int[] dimensions) {
        if ((dimensions == null && (dimension != 0 || numDimensions > 1)) || (dimensions != null && dimension >= dimensions.length)) {
            errorProfile.enter();
            if (assignment) {
                if (isSubset) {
                    if (numDimensions == 2) {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_SUBSCRIPTS_MATRIX);
                    } else {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_SUBSCRIPTS);
                    }
                } else {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.IMPROPER_SUBSCRIPT);
                }
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_DIMENSIONS);
            }
        }
    }
}

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
public abstract class ArrayPositionCast extends ArrayPositionsCastBase {

    public abstract Object executeArg(VirtualFrame frame, Object container, Object operand);

    protected abstract RNode getVector();

    private final ConditionProfile primitiveProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile emptyOpProfile = ConditionProfile.createBinaryProfile();

    protected ArrayPositionCast(int dimension, int numDimensions, boolean assignment, boolean isSubset) {
        super(dimension, numDimensions, assignment, isSubset);
    }

    protected ArrayPositionCast(ArrayPositionCast other) {
        super(other);
    }

    @Specialization
    protected RIntVector doMissingVector(RNull vector, RAbstractIntVector operand) {
        return operand.materialize();
    }

    @Specialization
    protected Object doFuncOp(RFunction vector, Object operand) {
        return operand;
    }

    @Specialization
    protected Object doEnvOp(REnvironment vector, Object operand) {
        return operand;
    }

    @Specialization
    protected Object doMissingVector(VirtualFrame frame, RAbstractContainer container, RMissing operand) {
        int[] dimensions = getDimensions(container);
        verifyDimensions(dimensions);
        int[] data = new int[numDimensions == 1 ? container.getLength() : dimensions[dimension]];
        if (data.length > 0) {
            for (int i = 0; i < data.length; i++) {
                data[i] = i + 1;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        } else {
            return 0;
        }
    }

    @Specialization
    protected RNull doNull(RAbstractContainer container, RNull operand) {
        // this is a special case - RNull can only appear to represent the x[[NA]] case which has to
        // return null and not a null vector
        return operand;
    }

    @Specialization
    protected RStringVector doStringVector(RList vector, RStringVector operand) {
        // recursive access to the list
        return operand;
    }

    @Specialization
    protected RList doList(RAbstractContainer container, RList operand) {
        return operand;
    }

    @Specialization
    protected RComplex doCompled(RAbstractContainer container, RComplex operand) {
        return operand;
    }

    @Specialization
    protected RRaw doRaw(RAbstractContainer container, RRaw operand) {
        return operand;
    }

    @Specialization
    protected Object doIntVector(RAbstractContainer container, RAbstractIntVector operand) {
        if (primitiveProfile.profile(operand.getLength() == 1 && operand.getNames() == null)) {
            return operand.getDataAtAsObject(0);
        } else if (emptyOpProfile.profile(operand.getLength() == 0)) {
            return 0;
        } else {
            return operand.materialize();
        }
    }

    protected static boolean sizeOneOp(RAbstractContainer container, RAbstractIntVector operand) {
        return operand.getLength() == 1;
    }

    protected boolean operandHasNames(RAbstractContainer container, RAbstractIntVector operand) {
        return operand.getNames() != null;
    }

    protected boolean numDimensionsOne() {
        return numDimensions == 1;
    }

    protected boolean isAssignment() {
        return assignment;
    }

    protected boolean emptyOperand(RAbstractContainer container, RAbstractIntVector operand) {
        return operand.getLength() == 0;
    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class), @NodeChild(value = "exact", type = RNode.class)})
    public abstract static class OperatorConverterNode extends ArrayPositionsCastBase {

        public abstract Object executeConvert(VirtualFrame frame, Object vector, Object operand, Object exact);

        @Child private OperatorConverterNode operatorConvertRecursive;
        @Child private CastIntegerNode castInteger;
        @Child private CastLogicalNode castLogical;
        @Child private CastToVectorNode castVector;
        @Child ContainerDimNamesGet dimNamesGetter;

        private final NACheck naCheck = NACheck.create();

        private final ConditionProfile nullDimensionsProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile tooManyDimensions = BranchProfile.create();
        private final ConditionProfile indNAProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile dimSizeOneProfile = ConditionProfile.createBinaryProfile();

        private final BranchProfile error = BranchProfile.create();
        private final BranchProfile outOfBoundsPositive = BranchProfile.create();
        private final BranchProfile outOfBoundsNegative = BranchProfile.create();
        private final BranchProfile nonNegative = BranchProfile.create();
        private final BranchProfile negative = BranchProfile.create();
        private final ConditionProfile elementsCountProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile listProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile emptyOperandProfile = ConditionProfile.createBinaryProfile();

        private final BranchProfile opLengthOne = BranchProfile.create();
        private final BranchProfile opLong = BranchProfile.create();

        private final ConditionProfile namesProfile = ConditionProfile.createBinaryProfile();

        private final ConditionProfile findExactProfile = ConditionProfile.createBinaryProfile();

        protected OperatorConverterNode(int dimension, int numDimensions, boolean assignment, boolean isSubset) {
            super(dimension, numDimensions, assignment, isSubset);
        }

        protected OperatorConverterNode(OperatorConverterNode other) {
            super(other);
        }

        private void initConvertCast() {
            if (operatorConvertRecursive == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                operatorConvertRecursive = insert(OperatorConverterNodeGen.create(this.dimension, this.numDimensions, this.assignment, this.isSubset, null, null, null));
            }
        }

        private RList getContainerDimNames(VirtualFrame frame, RAbstractContainer value) {
            if (dimNamesGetter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dimNamesGetter = insert(ContainerDimNamesGetNodeGen.create(null));
            }
            return dimNamesGetter.execute(frame, value);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractContainer container, int operand, Object exact) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, container, operand, exact);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractContainer container, double operand, Object exact) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, container, operand, exact);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractContainer container, byte operand, Object exact) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, container, operand, exact);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractContainer container, Object operand, Object exact) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, container, operand, exact);
        }

        private void initIntCast() {
            if (castInteger == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castInteger = insert(CastIntegerNodeGen.create(null, true, false, false));
            }
        }

        private Object castInteger(VirtualFrame frame, double operand) {
            initIntCast();
            return castInteger.executeCast(frame, operand);
        }

        private Object castInteger(VirtualFrame frame, byte operand) {
            initIntCast();
            return castInteger.executeCast(frame, operand);
        }

        private Object castInteger(VirtualFrame frame, Object operand) {
            initIntCast();
            return castInteger.executeCast(frame, operand);
        }

        private Object castLogical(VirtualFrame frame, Object operand) {
            if (castLogical == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castLogical = insert(CastLogicalNodeGen.create(null, false, false, false));
            }
            return castLogical.executeCast(frame, operand);
        }

        private Object castVector(VirtualFrame frame, Object operand) {
            if (castVector == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castVector = insert(CastToVectorNodeGen.create(null, false, false, false, false));
            }
            return castVector.executeCast(frame, operand);
        }

        @Specialization
        protected Object doFactor(VirtualFrame frame, RAbstractContainer container, RFactor factor, Object exact) {
            return convertOperatorRecursive(frame, container, factor.getVector(), exact);
        }

        @Specialization
        protected RList doList(RAbstractContainer container, RList operand, Object exact) {
            return operand;
        }

        @Specialization
        protected RMissing doFuncOp(RAbstractContainer container, RFunction operand, Object exact) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "closure");
        }

        @Specialization
        protected Object doMissingDimLengthOne(VirtualFrame frame, RAbstractContainer container, RMissing operand, Object exact) {
            if (!isSubset) {
                if (assignment) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.MISSING_SUBSCRIPT);
                } else {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "symbol");
                }
            }
            if (dimSizeOneProfile.profile(getDimensionSize(frame, container) == 1)) {
                return 1;
            } else {
                return operand;
            }
        }

        @Specialization
        protected int doNull(RAbstractContainer container, RNull operand, Object exact) {
            if (isSubset) {
                return 0;
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
            }
        }

        @Specialization
        protected Object doFuncOp(RFunction vector, Object operand, Object exact) {
            return operand;
        }

        @Specialization
        protected Object doEnvOp(REnvironment vector, Object operand, Object exact) {
            return operand;
        }

        @Specialization
        protected int doFactor(RNull vector, RFactor operand, Object exact) {
            return 0;
        }

        @Specialization(guards = "isAssignment")
        protected RIntVector doStringNullVecAssignment(RNull vector, String operand, Object exact) {
            RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, !RRuntime.isNA(operand));
            return RDataFactory.createIntVector(new int[]{1}, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization(guards = "!isAssignment")
        protected int doStringOneDimAssignment(RNull vector, String operand, Object exact) {
            return RRuntime.INT_NA;
        }

        @TruffleBoundary
        @Specialization
        protected Object doStringVectorOneDimAssignment(RNull vector, RAbstractStringVector operand, Object exact) {
            if (assignment && numDimensions == 1 && isSubset && operand.getLength() > 1) {
                // we need to get rid of duplicates but retain all NAs
                int[] data = new int[operand.getLength()];
                int initialPos = 0;
                for (int i = 0; i < data.length; i++) {
                    // TODO: this is slow - is it important to make it faster?
                    initialPos = eliminateDuplicate(operand, data, initialPos, i);
                }
                return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR, operand.materialize());
            } else {
                return RRuntime.INT_NA;
            }
        }

        @Specialization(guards = "!opString")
        protected Object doMissingVectorOp(VirtualFrame frame, RNull vector, RAbstractVector operand, Object exact) {
            return castInteger(frame, operand);
        }

        @Specialization(guards = "indNA")
        protected Object doIntNA(RList vector, int operand, Object exact) {
            return numDimensions != 1 || isSubset ? operand : RNull.instance;
        }

        @Specialization(guards = {"indNA", "!isVectorList"})
        protected int doIntNA(RAbstractContainer container, int operand, Object exact) {
            if (isSubset) {
                return operand;
            } else if (assignment) {
                // let assignment handle it as it depends on the value
                return RRuntime.INT_NA;
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
            }
        }

        @Specialization(guards = "!indNA")
        protected Object doInt(VirtualFrame frame, RAbstractContainer container, int operand, Object exact) {
            int dimSize = getDimensionSize(frame, container);

            if (operand > dimSize) {
                outOfBoundsPositive.enter();
                if (numDimensions != 1 || (!isSubset && !assignment)) {
                    error.enter();
                    throw RError.error(assignment ? getEncapsulatingSourceSection() : null, RError.Message.SUBSCRIPT_BOUNDS);
                } else {
                    return assignment ? operand : RRuntime.INT_NA;
                }
            } else if (operand < 0 && -operand > dimSize) {
                outOfBoundsNegative.enter();
                if (dimSizeOneProfile.profile(dimSize == 1)) {
                    // e.g. c(7)[-2] vs c(7)[[-2]]
                    return isSubset ? /* only one element to be picked */1 : /* ultimately an error */operand;
                } else {
                    // e.g. c(7, 42)[-7] vs c(, 427)[[-7]]
                    return isSubset ? RMissing.instance : operand;
                }
            } else if (operand < 0) {
                negative.enter();
                if (dimSizeOneProfile.profile(dimSize == 1)) {
                    // it's negative, but not out of bounds and dimension has length one - result is
                    // no dimensions left
                    return 0;
                } else {
                    return doIntNegativeMultiDim(frame, container, operand);
                }
            } else {
                nonNegative.enter();
                return operand;
            }
        }

        protected Object doIntNegativeMultiDim(VirtualFrame frame, RAbstractContainer container, int operand) {
            if (elementsCountProfile.profile(isSubset || container.getLength() <= 2)) {
                // it's negative, but not out of bounds - pick all indexes apart from the negative
                // one
                int dimLength = numDimensions == 1 ? container.getLength() : getDimensions(container)[dimension];
                int[] positions = new int[dimLength - 1];
                int ind = 0;
                for (int i = 1; i <= dimLength; i++) {
                    if (i != -operand) {
                        positions[ind++] = i;
                    }
                }
                return RDataFactory.createIntVector(positions, RDataFactory.COMPLETE_VECTOR);
            } else {
                // too many elements chosen - ultimately an error
                return operand;
            }
        }

        @Specialization(guards = "!isNegative")
        protected Object doDouble(VirtualFrame frame, RAbstractContainer container, double operand, Object exact) {
            return convertOperatorRecursive(frame, container, castInteger(frame, operand), exact);
        }

        @Specialization(guards = "isNegative")
        protected Object doDoubleNegative(VirtualFrame frame, RAbstractContainer container, double operand, Object exact) {
            // returns object as it may return either int or RIntVector due to conversion
            return convertOperatorRecursive(frame, container, castInteger(frame, Math.abs(operand) > container.getLength() ? operand - 1 : operand), exact);
            // The check for the operand size in relation to the vector length is done to maintain
            // compatibility with GNU R, which (oddly) does not seem to apply as.integer semantics
            // to a negative argument here. For instance, { x <- c(1,2,3); x[-3.1] } will give the
            // answer 1 2 3 even though as.integer would turn -3.1 into -3, which should lead to
            // removal of the third element.
        }

        @Specialization(guards = {"indNA", "numDimensionsOne", "!isSubset"})
        protected RNull doLogicalDimLengthOne(RList vector, byte operand, Object exact) {
            return RNull.instance;
        }

        @Specialization(guards = {"indNA", "numDimensionsOne", "!isSubset", "!isVectorList"})
        protected int doLogicalDimLengthOne(RAbstractContainer container, byte operand, Object exact) {
            return RRuntime.INT_NA;
        }

        @Specialization(guards = "indNA")
        protected Object doLogicalNA(VirtualFrame frame, RAbstractContainer container, byte operand, Object exact) {
            if (isSubset && !assignment) {
                int dimLength = numDimensions == 1 ? (container.getLength() == 0 ? 1 : container.getLength()) : getDimensions(container)[dimension];
                int[] data = new int[dimLength];
                Arrays.fill(data, RRuntime.INT_NA);
                return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
            } else {
                return RRuntime.INT_NA;
            }
        }

        @Specialization(guards = {"indTrue", "isSubset"})
        protected RIntVector doLogicalIndTrue(VirtualFrame frame, RAbstractContainer container, byte operand, Object exact) {
            int dimLength = numDimensions == 1 ? container.getLength() : getDimensions(container)[dimension];
            int[] data = new int[dimLength];
            for (int i = 0; i < dimLength; i++) {
                data[i] = i + 1;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(guards = {"indFalse", "isSubset"})
        protected int doLogicalIndFalse(RAbstractContainer container, byte operand, Object exact) {
            return 0;
        }

        @Specialization(guards = {"!indNA", "!isSubset"})
        protected int doLogical(RAbstractContainer container, byte operand, Object exact) {
            return RRuntime.logical2intNoCheck(operand);
        }

        @Specialization
        protected RComplex doComplex(RAbstractContainer container, RComplex operand, Object exact) {
            return operand;
        }

        @Specialization
        protected RRaw doRaw(RAbstractContainer container, RRaw operand, Object exact) {
            return operand;
        }

        private int findPosition(RAbstractContainer container, Object namesObj, String operand, boolean findExact) {
            if (namesObj != RNull.instance) {
                RStringVector names = (RStringVector) namesObj;
                if (findExactProfile.profile(findExact)) {
                    for (int j = 0; j < names.getLength(); j++) {
                        if (operand.equals(names.getDataAt(j))) {
                            return j + 1;
                        }
                    }
                } else {
                    int res = -1;
                    for (int j = 0; j < names.getLength(); j++) {
                        if (names.getDataAt(j).startsWith(operand)) {
                            if (res == -1) {
                                res = j + 1;
                            } else {
                                // multiple matches (as if there was no match)
                                res = -1;
                                break;
                            }
                        }
                    }
                    if (res != -1) {
                        return res;
                    }

                }
            }
            if (numDimensions == 1) {
                return RRuntime.INT_NA;
            } else {
                error.enter();
                throw RError.error(isSubset ? null : getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
            }
        }

        private static int getSingleNamesPos(RAbstractContainer container, RStringVector names, String operand) {
            int position = -1;
            for (int j = 0; j < names.getLength(); j++) {
                if (operand.equals(names.getDataAt(j))) {
                    position = j + 1;
                    break;
                }
            }
            if (position == -1) {
                position = container.getLength() + 1;
            }
            return position;
        }

        private static RIntVector findPositionWithNames(RAbstractContainer container, RStringVector names, String operand) {
            RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, !RRuntime.isNA(operand));
            int position = -1;
            if (names != null) {
                position = getSingleNamesPos(container, names, operand);
            } else {
                position = container.getLength() + 1;
            }
            return RDataFactory.createIntVector(new int[]{position}, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        private static RIntVector findPositionWithDimNames(RAbstractContainer container, Object namesObj, String operand) {
            RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, !RRuntime.isNA(operand));
            int position = -1;
            if (namesObj != RNull.instance) {
                RStringVector names = (RStringVector) namesObj;
                position = getSingleNamesPos(container, names, operand);
            } else {
                position = container.getLength() + 1;
            }
            return RDataFactory.createIntVector(new int[]{position}, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization(guards = "indNA")
        protected Object doStringNA(VirtualFrame frame, RAbstractContainer container, String operand, Object exact) {
            if (numDimensions == 1) {
                if (assignment) {
                    RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, RDataFactory.INCOMPLETE_VECTOR);
                    return RDataFactory.createIntVector(new int[]{container.getLength() + 1}, RDataFactory.COMPLETE_VECTOR, resNames);
                } else {
                    return convertOperatorRecursive(frame, container, RRuntime.INT_NA, exact);
                }
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
            }
        }

        @TruffleBoundary
        private Object doStringOneDim(RAbstractContainer container, String operand, boolean exact) {
            // single-dimension access
            if (assignment) {
                // with assignment, container's names can be set by the operand
                return findPositionWithNames(container, container.getNames(), operand);
            } else if (container.getNames() != null) {
                // with vector read, we need names to even try finding container components
                int result = findPosition(container, container.getNames(), operand, exact);

                if (container instanceof RList) {
                    // container is a list
                    if (!isSubset && RRuntime.isNA(result)) {
                        return RNull.instance;
                    }
                }
                return result;
            } else {
                // container has no names
                if (isSubset) {
                    return RRuntime.INT_NA;
                } else {
                    if (container instanceof RList) {
                        // container is a list
                        return RNull.instance;
                    } else {
                        error.enter();
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
                    }
                }
            }
        }

        @TruffleBoundary
        private Object doStringMultiDim(RAbstractContainer container, String operand, RList dimNames, boolean exact) {
            if (dimNames != null) {
                if (assignment) {
                    return findPositionWithDimNames(container, dimNames.getDataAt(dimension), operand);
                } else {
                    return findPosition(container, dimNames.getDataAt(dimension), operand, exact);
                }
            } else {
                error.enter();
                if (isSubset || container instanceof RList) {
                    throw RError.error(RError.Message.NO_ARRAY_DIMNAMES);
                } else {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
                }

            }
        }

        @Specialization(guards = "!indNA")
        protected Object doString(VirtualFrame frame, RAbstractContainer container, String operand, Object exact) {
            boolean findExact = true;
            RAbstractLogicalVector exactVec = (RAbstractLogicalVector) castLogical(frame, castVector(frame, exact));
            if (exactVec.getLength() > 0 && exactVec.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
                findExact = false;
            }
            if (numDimensions == 1) {
                return doStringOneDim(container, operand, findExact);
            } else {
                return doStringMultiDim(container, operand, getContainerDimNames(frame, container), findExact);
            }
        }

        @Specialization
        protected Object doIntVector(VirtualFrame frame, RAbstractContainer container, RAbstractIntVector operand, Object exact) {
            if (operand.getLength() == 1) {
                opLengthOne.enter();
                return convertOperatorRecursive(frame, container, operand.getDataAt(0), exact);
            }
            if (isSubset) {
                if (emptyOperandProfile.profile(operand.getLength() == 0)) {
                    return 0;
                } else {
                    return transformIntoPositive(frame, container, operand);
                }
            } else {
                if (emptyOperandProfile.profile(operand.getLength() == 0)) {
                    if (listProfile.profile(container instanceof RList)) {
                        // container is a list
                        error.enter();
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
                    } else {
                        return 0;
                    }
                } else {
                    // no transformation - if it's a list, then it's handled during recursive
                    // access, if it's not then it's an error dependent on the value
                    return operand;
                }
            }

        }

        @Specialization
        protected Object doDouble(VirtualFrame frame, RAbstractContainer container, RAbstractDoubleVector operand, Object exact) {
            return convertOperatorRecursive(frame, container, castInteger(frame, operand), exact);
        }

        @Specialization
        protected Object doLogical(VirtualFrame frame, RAbstractContainer container, RAbstractLogicalVector operand, Object exact) {
            if (emptyOperandProfile.profile(operand.getLength() == 0)) {
                return convertOperatorRecursive(frame, container, castInteger(frame, operand), exact);
            } else if (operand.getLength() == 1) {
                opLengthOne.enter();
                return convertOperatorRecursive(frame, container, operand.getDataAt(0), exact);
            } else {
                opLong.enter();
                if (isSubset) {
                    if ((operand.getLength() > getDimensionSize(frame, container)) && numDimensions != 1) {
                        error.enter();
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.LOGICAL_SUBSCRIPT_LONG);
                    }
                    return doLogicalVectorInternal(frame, container, operand);

                } else {
                    return convertOperatorRecursive(frame, container, castInteger(frame, operand), exact);
                }
            }
        }

        private static int[] eliminateZeros(RAbstractContainer container, int[] positions, int zeroCount) {
            int positionsLength = positions.length;
            int[] data = new int[positionsLength - zeroCount];
            int ind = 0;
            for (int i = 0; i < positionsLength; i++) {
                int pos = positions[i];
                if (pos != 0) {
                    data[ind++] = pos;
                }
            }
            return data;
        }

        protected RIntVector doLogicalVectorInternal(VirtualFrame frame, RAbstractContainer container, RAbstractLogicalVector operand) {
            int dimLength = numDimensions == 1 ? container.getLength() : getDimensions(container)[dimension];
            int resultLength = Math.max(operand.getLength(), dimLength);
            int logicalVectorLength = operand.getLength();
            int logicalVectorInd = 0;
            int[] data = new int[resultLength];
            naCheck.enable(operand);
            int timesSeenFalse = 0;
            int timesSeenNA = 0;
            int i = 0;
            for (; i < resultLength; i++, logicalVectorInd = Utils.incMod(logicalVectorInd, logicalVectorLength)) {
                byte b = operand.getDataAt(logicalVectorInd);
                if (naCheck.check(b)) {
                    timesSeenNA++;
                    if (i < container.getLength() || !assignment || numDimensions != 1) {
                        data[i] = RRuntime.INT_NA;
                    } else {
                        // all such positions must be filled with NAs (preserved negative index)
                        data[i] = -(i + 1);
                    }
                } else if (b == RRuntime.LOGICAL_TRUE) {
                    if (i >= dimLength && !assignment) {
                        data[i] = RRuntime.INT_NA;
                    } else {
                        data[i] = i + 1;
                    }
                } else if (b == RRuntime.LOGICAL_FALSE) {
                    timesSeenFalse++;
                    data[i] = 0;
                }
            }
            if (timesSeenFalse > 0) {
                // remove 0s (used to be FALSE) and resize the vector
                return RDataFactory.createIntVector(eliminateZeros(container, data, timesSeenFalse), naCheck.neverSeenNA() && i < container.getLength());
            } else {
                return RDataFactory.createIntVector(data, naCheck.neverSeenNA() && i < container.getLength());
            }
        }

        private Object handleInvalidVecType(VirtualFrame frame, RAbstractContainer container, RAbstractVector operand, String typeName, Object exact) {
            if (operand.getLength() == 1) {
                opLengthOne.enter();
                return convertOperatorRecursive(frame, container, operand.getDataAtAsObject(0), exact);
            } else {
                error.enter();
                if (operand.getLength() == 0 && !isSubset) {
                    if (container instanceof RList) {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
                    } else {
                        return 0;
                    }
                } else if (isSubset || operand.getLength() == 2) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, typeName);
                } else {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
                }
            }
        }

        @Specialization
        protected Object doComplex(VirtualFrame frame, RAbstractContainer container, RAbstractComplexVector operand, Object exact) {
            return handleInvalidVecType(frame, container, operand, "complex", exact);
        }

        @Specialization
        protected Object doRawx(VirtualFrame frame, RAbstractContainer container, RAbstractRawVector operand, Object exact) {
            return handleInvalidVecType(frame, container, operand, "raw", exact);
        }

        private RIntVector findPositions(VirtualFrame frame, RAbstractContainer container, Object namesObj, RAbstractStringVector operand, boolean retainNames) {
            int[] data = new int[operand.getLength()];
            boolean hasSeenNA = false;
            int namesLength = namesObj == RNull.instance ? 0 : ((RStringVector) namesObj).getLength();
            for (int i = 0; i < data.length; i++) {
                String positionName = operand.getDataAt(i);
                int j = 0;
                for (; j < namesLength; j++) {
                    if (positionName.equals(((RStringVector) namesObj).getDataAt(j))) {
                        data[i] = j + 1;
                        break;
                    }
                }
                if (j == namesLength) {
                    if (numDimensions == 1) {
                        data[i] = RRuntime.INT_NA;
                        hasSeenNA = true;
                    } else {
                        error.enter();
                        throw RError.error(RError.Message.SUBSCRIPT_BOUNDS);
                    }
                }
            }
            return RDataFactory.createIntVector(data, !hasSeenNA);
        }

        private static int eliminateDuplicate(RAbstractStringVector operand, int[] data, int initialPos, int currentElementPos) {
            int position = initialPos;
            String name = operand.getDataAt(currentElementPos);
            if (name == RRuntime.STRING_NA || name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE)) {
                // duplicate NAs and empty strings are not eliminated
                data[currentElementPos] = (position++) + 1;
            } else {
                int j = 0;
                for (; j < currentElementPos; j++) {
                    String prevName = operand.getDataAt(j);
                    if (name.equals(prevName)) {
                        data[currentElementPos] = data[j];
                        break;
                    }
                }
                if (j == currentElementPos) {
                    data[currentElementPos] = (position++) + 1;
                }
            }
            return position;
        }

        private static int getNamesPos(RStringVector names, RAbstractStringVector operand, int[] data, int initialPos, int i) {
            int newInitialPos = initialPos;
            String positionName = operand.getDataAt(i);
            int j = 0;
            for (; j < names.getLength(); j++) {
                if (positionName.equals(names.getDataAt(j))) {
                    data[i] = j + 1;
                    break;
                }
            }
            if (j == names.getLength()) {
                // TODO: this is slow - is it important to make it faster?
                newInitialPos = eliminateDuplicate(operand, data, newInitialPos, i);
            }
            return newInitialPos;
        }

        private static RIntVector findPositionsWithNames(VirtualFrame frame, RAbstractContainer container, RStringVector names, RAbstractStringVector operand, boolean retainNames) {
            RStringVector resNames = operand.materialize();
            int initialPos = container.getLength();
            int[] data = new int[operand.getLength()];
            for (int i = 0; i < data.length; i++) {
                if (names != null) {
                    initialPos = getNamesPos(names, operand, data, initialPos, i);
                } else {
                    // TODO: this is slow - is it important to make it faster?
                    initialPos = eliminateDuplicate(operand, data, initialPos, i);
                }
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        private static RIntVector findPositionsWithDimNames(VirtualFrame frame, RAbstractContainer container, Object namesObj, RAbstractStringVector operand, boolean retainNames) {
            RStringVector resNames = operand.materialize();
            int initialPos = container.getLength();
            int[] data = new int[operand.getLength()];
            for (int i = 0; i < data.length; i++) {
                if (namesObj != RNull.instance) {
                    RStringVector names = (RStringVector) namesObj;
                    initialPos = getNamesPos(names, operand, data, initialPos, i);
                } else {
                    // TODO: this is slow - is it important to make it faster?
                    initialPos = eliminateDuplicate(operand, data, initialPos, i);
                }
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization
        protected Object doString(VirtualFrame frame, RAbstractContainer container, RAbstractStringVector operand, Object exact) {
            if (emptyOperandProfile.profile(operand.getLength() == 0)) {
                return convertOperatorRecursive(frame, container, castInteger(frame, operand), exact);
            } else if (operand.getLength() == 1) {
                opLengthOne.enter();
                return convertOperatorRecursive(frame, container, operand.getDataAt(0), exact);
            } else {
                opLong.enter();
                if (isSubset) {
                    if (numDimensions == 1) {
                        if (assignment) {
                            return findPositionsWithNames(frame, container, container.getNames(), operand, assignment);
                        } else if (namesProfile.profile(container.getNames() != null)) {
                            return findPositions(frame, container, container.getNames(), operand, assignment);
                        } else {
                            int[] data = new int[operand.getLength()];
                            Arrays.fill(data, RRuntime.INT_NA);
                            return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
                        }
                    } else {
                        if (namesProfile.profile(getContainerDimNames(frame, container) != null)) {
                            if (assignment) {
                                return findPositionsWithDimNames(frame, container, getContainerDimNames(frame, container).getDataAt(dimension), operand, assignment);
                            } else {
                                return findPositions(frame, container, getContainerDimNames(frame, container).getDataAt(dimension), operand, assignment);
                            }
                        } else {
                            error.enter();
                            throw RError.error(RError.Message.SUBSCRIPT_BOUNDS);
                        }
                    }
                } else {
                    if (numDimensions != 1 || container.getElementClass() != Object.class) {
                        error.enter();
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
                    }

                    // for recursive access
                    return operand;
                }
            }
        }

        private final NACheck positionNACheck = NACheck.create();

        private int[] eliminateZeros(RAbstractContainer container, RAbstractIntVector positions, int zeroCount) {
            int positionsLength = positions.getLength();
            int[] data = new int[positionsLength - zeroCount];
            int ind = 0;
            int i = 0;
            for (; i < positionsLength; i++) {
                int pos = positions.getDataAt(i);
                if (pos > container.getLength()) {
                    if (assignment) {
                        data[ind++] = pos;
                    } else {
                        data[ind++] = RRuntime.INT_NA;

                    }
                } else if (pos != 0) {
                    data[ind++] = pos;
                }
            }
            return data;
        }

        private final BranchProfile seenPositive = BranchProfile.create();
        private final BranchProfile seenZero = BranchProfile.create();
        private final BranchProfile seenNegative = BranchProfile.create();
        private final BranchProfile seenNA = BranchProfile.create();

        private RAbstractIntVector transformIntoPositive(VirtualFrame frame, RAbstractContainer container, RAbstractIntVector positions) {
            boolean hasSeenPositive = false;
            boolean hasSeenZero = false;
            boolean hasSeenNegative = false;
            boolean hasSeenNA = false;
            int zeroCount = 0;
            positionNACheck.enable(positions);
            int positionsLength = positions.getLength();
            int dimLength = numDimensions == 1 ? container.getLength() : getDimensions(container)[dimension];
            boolean outOfBounds = false;
            for (int i = 0; i < positionsLength; ++i) {
                int pos = positions.getDataAt(i);
                if (positionNACheck.check(pos)) {
                    seenNA.enter();
                    hasSeenNA = true;
                } else if (pos > 0) {
                    if (numDimensions != 1 && pos > dimLength) {
                        error.enter();
                        throw RError.error(RError.Message.SUBSCRIPT_BOUNDS);
                    }
                    if (numDimensions == 1 && pos > container.getLength()) {
                        if (isSubset) {
                            outOfBounds = true;
                        } else {
                            error.enter();
                            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
                        }
                    }
                    seenPositive.enter();
                    hasSeenPositive = true;
                } else if (pos == 0) {
                    if (!isSubset) {
                        error.enter();
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
                    }
                    seenZero.enter();
                    hasSeenZero = true;
                    zeroCount++;
                } else {
                    seenNegative.enter();
                    hasSeenNegative = true;
                }
            }
            if (hasSeenPositive || hasSeenNA) {
                if (hasSeenNegative) {
                    error.enter();
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.ONLY_0_MIXED);
                } else if (hasSeenZero || (outOfBounds && numDimensions == 1)) {
                    // eliminate 0-s and handle out-of-bounds for single-subscript accesses
                    int[] data = eliminateZeros(container, positions, zeroCount);
                    return RDataFactory.createIntVector(data, positionNACheck.neverSeenNA() && !outOfBounds);
                } else {
                    if (assignment && numDimensions == 1 && positions.getNames() != null) {
                        // in this case, positions having the "names" attribute is considered a
                        // special case needed for handling assignments using string indexes (which
                        // update "names" attribute of the updated vector)
                        RIntVector resPositions = (RIntVector) positions.copy();
                        resPositions.setNames(null);
                        return resPositions;
                    } else {
                        // fast path (most common expected behavior)
                        return positions;
                    }
                }
            } else if (hasSeenNegative) {
                if (hasSeenNA) {
                    error.enter();
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.ONLY_0_MIXED);
                }
                boolean[] excludedPositions = new boolean[dimLength];
                int allPositionsNum = dimLength;
                for (int i = 0; i < positionsLength; i++) {
                    int pos = -positions.getDataAt(i);
                    if (pos > 0 && pos <= dimLength && !excludedPositions[pos - 1]) {
                        allPositionsNum--;
                        excludedPositions[pos - 1] = true;
                    }
                }
                if (allPositionsNum == 0) {
                    return RDataFactory.createIntVector(new int[]{0}, RDataFactory.COMPLETE_VECTOR);
                } else {
                    int[] data = new int[allPositionsNum];
                    int ind = 0;
                    for (int i = 0; i < dimLength; i++) {
                        if (!excludedPositions[i]) {
                            data[ind++] = i + 1;
                        }
                    }
                    return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
                }
            } else {
                // all zeros
                return RDataFactory.createIntVector(1);
            }
        }

        protected boolean opString(RNull container, RAbstractVector operand) {
            return operand.getElementClass() == RString.class;
        }

        private int getDimensionSize(VirtualFrame frame, RAbstractContainer container) {
            int[] dimensions = getDimensions(container);
            verifyDimensions(dimensions);
            return numDimensions == 1 ? container.getLength() : dimensions[dimension];
        }

        protected boolean isVectorList(RAbstractContainer container) {
            return container instanceof RList;
        }

        protected static boolean indNA(RAbstractContainer container, int operand) {
            return RRuntime.isNA(operand);
        }

        protected static boolean indNA(RAbstractContainer container, byte operand) {
            return RRuntime.isNA(operand);
        }

        protected static boolean indNA(RAbstractContainer container, String operand) {
            return RRuntime.isNA(operand);
        }

        protected static boolean indTrue(RAbstractContainer container, byte operand) {
            return operand == RRuntime.LOGICAL_TRUE;
        }

        protected static boolean indFalse(RAbstractContainer container, byte operand) {
            return operand == RRuntime.LOGICAL_FALSE;
        }

        protected boolean isNegative(RAbstractContainer container, double operand) {
            return operand < 0;
        }

        protected boolean numDimensionsOne() {
            return numDimensions == 1;
        }

        protected boolean isSubset() {
            return isSubset;
        }

        protected boolean isAssignment() {
            return assignment;
        }

        protected boolean operandHasNames(RNull vector, RAbstractIntVector operand) {
            return operand.getNames() != null;
        }
    }

    // TODO: this should not be necessary once data frame access operators are implemented in R
    // which likely makes potential refactoring of this code redundant
    @NodeChild(value = "op")
    protected abstract static class ContainerDimNamesGet extends RNode {

        @Child ContainerRowNamesGet rowNamesGetter;
        @Child private CastStringNode castString;

        public abstract RList execute(VirtualFrame frame, RAbstractContainer container);

        private Object getContainerRowNames(VirtualFrame frame, RAbstractContainer value) {
            if (rowNamesGetter == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                rowNamesGetter = insert(ContainerRowNamesGetNodeGen.create(null));
            }
            return rowNamesGetter.execute(frame, value);
        }

        private Object castString(VirtualFrame frame, Object operand) {
            if (castString == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castString = insert(CastStringNodeGen.create(null, false, true, false, false));
            }
            return castString.executeCast(frame, operand);
        }

        @Specialization(guards = "!isDataFrame")
        RList getDim(RAbstractContainer container) {
            return container.getDimNames();
        }

        @Specialization(guards = "isDataFrame")
        RList getDimDataFrame(VirtualFrame frame, RAbstractContainer container) {
            return RDataFactory.createList(new Object[]{castString(frame, getContainerRowNames(frame, container)), container.getNames()});
        }

        protected boolean isDataFrame(RAbstractContainer container) {
            return container.getElementClass() == RDataFrame.class;
        }

    }

}
