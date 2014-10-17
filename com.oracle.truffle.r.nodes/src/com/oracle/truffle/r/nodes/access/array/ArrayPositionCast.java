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
package com.oracle.truffle.r.nodes.access.array;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCastFactory.OperatorConverterNodeFactory;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
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

    protected void verifyDimensions(RAbstractContainer container) {
        if ((container.getDimensions() == null && (dimension != 0 || numDimensions > 1)) || (container.getDimensions() != null && dimension >= container.getDimensions().length)) {
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
@NodeChildren({@NodeChild(value = "op", type = RNode.class), @NodeChild(value = "vector", type = RNode.class),
                @NodeChild(value = "operand", type = OperatorConverterNode.class, executeWith = {"vector", "op"})})
public abstract class ArrayPositionCast extends ArrayPositionsCastBase {

    public abstract Object executeArg(VirtualFrame frame, Object op, Object vector, Object operand);

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
    protected RIntVector doMissingVector(Object op, RNull vector, RAbstractIntVector operand) {
        return operand.materialize();
    }

    @Specialization
    protected Object doFuncOp(Object op, RFunction vector, Object operand) {
        return operand;
    }

    @Specialization
    protected RIntVector doMissingVector(Object op, RAbstractContainer container, RMissing operand) {
        verifyDimensions(container);
        int[] data = new int[numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension]];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1;
        }

        return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RNull doNull(Object op, RAbstractContainer container, RNull operand) {
        // this is a special case - RNull can only appear to represent the x[[NA]] case which has to
        // return null and not a null vector
        return operand;
    }

    @Specialization
    protected RStringVector doStringVector(Object op, RList vector, RStringVector operand) {
        // recursive access to the list
        return operand;
    }

    @Specialization
    protected RList doList(Object op, RAbstractContainer container, RList operand) {
        return operand;
    }

    @Specialization
    protected RComplex doCompled(Object op, RAbstractContainer container, RComplex operand) {
        return operand;
    }

    @Specialization
    protected RRaw doRaw(Object op, RAbstractContainer container, RRaw operand) {
        return operand;
    }

    @Specialization
    protected Object doIntVector(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        if (primitiveProfile.profile(operand.getLength() == 1 && operand.getNames() == RNull.instance)) {
            return operand.getDataAtAsObject(0);
        } else if (emptyOpProfile.profile(operand.getLength() == 0)) {
            return 0;
        } else {
            return operand.materialize();
        }
    }

    protected static boolean sizeOneOp(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        return operand.getLength() == 1;
    }

    protected boolean operandHasNames(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        return operand.getNames() != RNull.instance;
    }

    protected boolean numDimensionsOne() {
        return numDimensions == 1;
    }

    protected boolean isAssignment() {
        return assignment;
    }

    protected boolean emptyOperand(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        return operand.getLength() == 0;
    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
    public abstract static class OperatorConverterNode extends ArrayPositionsCastBase {

        public abstract Object executeConvert(VirtualFrame frame, Object vector, Object operand);

        @Child private OperatorConverterNode operatorConvertRecursive;
        @Child private CastIntegerNode castInteger;
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

        protected OperatorConverterNode(int dimension, int numDimensions, boolean assignment, boolean isSubset) {
            super(dimension, numDimensions, assignment, isSubset);
        }

        protected OperatorConverterNode(OperatorConverterNode other) {
            super(other);
        }

        private void initConvertCast() {
            if (operatorConvertRecursive == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                operatorConvertRecursive = insert(OperatorConverterNodeFactory.create(this.dimension, this.numDimensions, this.assignment, this.isSubset, null, null));
            }
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractContainer container, int operand) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, container, operand);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractContainer container, double operand) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, container, operand);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractContainer container, byte operand) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, container, operand);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractContainer container, Object operand) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, container, operand);
        }

        private void initIntCast() {
            if (castInteger == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castInteger = insert(CastIntegerNodeFactory.create(null, true, false, false));
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

        @Specialization
        protected RList doList(RAbstractContainer container, RList operand) {
            return operand;
        }

        @Specialization
        protected RMissing doFuncOp(RAbstractContainer container, RFunction operand) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "closure");
        }

        @Specialization
        protected Object doMissingDimLengthOne(RAbstractContainer container, RMissing operand) {
            if (!isSubset) {
                if (assignment) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.MISSING_SUBSCRIPT);
                } else {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "symbol");
                }
            }
            if (dimSizeOneProfile.profile(getDimensionSize(container) == 1)) {
                return 1;
            } else {
                return operand;
            }
        }

        @Specialization
        protected int doNull(RAbstractContainer container, RNull operand) {
            if (isSubset) {
                return 0;
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
            }
        }

        @Specialization
        protected Object doFuncOp(RFunction vector, Object operand) {
            return operand;
        }

        @Specialization(guards = "isAssignment")
        protected RIntVector doStringNullVecAssignment(RNull vector, String operand) {
            RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, !RRuntime.isNA(operand));
            return RDataFactory.createIntVector(new int[]{1}, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization(guards = "!isAssignment")
        protected int doStringOneDimAssignment(RNull vector, String operand) {
            return RRuntime.INT_NA;
        }

        @SlowPath
        @Specialization
        protected Object doStringVectorOneDimAssignment(VirtualFrame frame, RNull vector, RAbstractStringVector operand) {
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
        protected Object doMissingVectorOp(VirtualFrame frame, RNull vector, RAbstractVector operand) {
            return castInteger(frame, operand);
        }

        @Specialization(guards = "indNA")
        protected Object doIntNA(RList vector, int operand) {
            return numDimensions != 1 || isSubset ? operand : RNull.instance;
        }

        @Specialization(guards = {"indNA", "!isVectorList"})
        protected int doIntNA(RAbstractContainer container, int operand) {
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
        protected Object doInt(RAbstractContainer container, int operand) {
            int dimSize = getDimensionSize(container);

            if (operand > dimSize) {
                outOfBoundsPositive.enter();
                if (numDimensions != 1 || (!isSubset && !assignment)) {
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
                    return doIntNegativeMultiDim(container, operand);
                }
            } else {
                nonNegative.enter();
                return operand;
            }
        }

        protected Object doIntNegativeMultiDim(RAbstractContainer container, int operand) {
            if (elementsCountProfile.profile(isSubset || container.getLength() <= 2)) {
                // it's negative, but not out of bounds - pick all indexes apart from the negative
                // one
                int dimLength = numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension];
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
        protected Object doDouble(VirtualFrame frame, RAbstractContainer container, double operand) {
            return convertOperatorRecursive(frame, container, castInteger(frame, operand));
        }

        @Specialization(guards = "isNegative")
        protected Object doDoubleNegative(VirtualFrame frame, RAbstractContainer container, double operand) {
            // returns object as it may return either int or RIntVector due to conversion
            return convertOperatorRecursive(frame, container, castInteger(frame, Math.abs(operand) > container.getLength() ? operand - 1 : operand));
            // The check for the operand size in relation to the vector length is done to maintain
            // compatibility with GNU R, which (oddly) does not seem to apply as.integer semantics
            // to a negative argument here. For instance, { x <- c(1,2,3); x[-3.1] } will give the
            // answer 1 2 3 even though as.integer would turn -3.1 into -3, which should lead to
            // removal of the third element.
        }

        @Specialization(guards = {"indNA", "numDimensionsOne", "!isSubset"})
        protected RNull doLogicalDimLengthOne(RList vector, byte operand) {
            return RNull.instance;
        }

        @Specialization(guards = {"indNA", "numDimensionsOne", "!isSubset", "!isVectorList"})
        protected int doLogicalDimLengthOne(RAbstractContainer container, byte operand) {
            return RRuntime.INT_NA;
        }

        @Specialization(guards = "indNA")
        protected Object doLogicalNA(VirtualFrame frame, RAbstractContainer container, byte operand) {
            if (isSubset && !assignment) {
                int dimLength = numDimensions == 1 ? (container.getLength() == 0 ? 1 : container.getLength()) : container.getDimensions()[dimension];
                int[] data = new int[dimLength];
                Arrays.fill(data, RRuntime.INT_NA);
                return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
            } else {
                return RRuntime.INT_NA;
            }
        }

        @Specialization(guards = {"indTrue", "isSubset"})
        protected RIntVector doLogicalIndTrue(RAbstractContainer container, byte operand) {
            int dimLength = numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension];
            int[] data = new int[dimLength];
            for (int i = 0; i < dimLength; i++) {
                data[i] = i + 1;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(guards = {"indFalse", "isSubset"})
        protected int doLogicalIndFalse(RAbstractContainer container, byte operand) {
            return 0;
        }

        @Specialization(guards = {"!indNA", "!isSubset"})
        protected int doLogical(RAbstractContainer container, byte operand) {
            return RRuntime.logical2intNoCheck(operand);
        }

        @Specialization
        protected RComplex doComplex(RAbstractContainer container, RComplex operand) {
            return operand;
        }

        @Specialization
        protected RRaw doRaw(RAbstractContainer container, RRaw operand) {
            return operand;
        }

        private int findPosition(RAbstractContainer container, Object namesObj, String operand) {
            if (namesObj != RNull.instance) {
                RStringVector names = (RStringVector) namesObj;
                for (int j = 0; j < names.getLength(); j++) {
                    if (operand.equals(names.getDataAt(j))) {
                        return j + 1;
                    }
                }
            }
            if (numDimensions == 1) {
                return RRuntime.INT_NA;
            } else {
                throw RError.error(isSubset ? null : getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
            }
        }

        private static RIntVector findPositionWithNames(RAbstractContainer container, Object namesObj, String operand) {
            RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, !RRuntime.isNA(operand));
            int position = -1;
            if (namesObj != RNull.instance) {
                RStringVector names = (RStringVector) namesObj;
                for (int j = 0; j < names.getLength(); j++) {
                    if (operand.equals(names.getDataAt(j))) {
                        position = j + 1;
                        break;
                    }
                }
                if (position == -1) {
                    position = container.getLength() + 1;
                }
            } else {
                position = container.getLength() + 1;
            }
            return RDataFactory.createIntVector(new int[]{position}, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization(guards = "indNA")
        protected Object doStringNA(VirtualFrame frame, RAbstractContainer container, String operand) {
            if (numDimensions == 1) {
                if (assignment) {
                    RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, RDataFactory.INCOMPLETE_VECTOR);
                    return RDataFactory.createIntVector(new int[]{container.getLength() + 1}, RDataFactory.COMPLETE_VECTOR, resNames);
                } else {
                    return convertOperatorRecursive(frame, container, RRuntime.INT_NA);
                }
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
            }
        }

        @SlowPath
        // retrieving vector components by name is likely OK on the slow path
        @Specialization(guards = "!indNA")
        protected Object doString(RAbstractContainer container, String operand) {
            if (numDimensions == 1) {
                // single-dimension access
                if (assignment) {
                    // with assignment, container's names can be set by the operand
                    return findPositionWithNames(container, container.getNames(), operand);
                } else if (container.getNames() != RNull.instance) {
                    // with vector read, we need names to even try finding container components
                    int result = findPosition(container, container.getNames(), operand);

                    if (container.getElementClass() == Object.class) {
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
                        if (container.getElementClass() == Object.class) {
                            // container is a list
                            return RNull.instance;
                        } else {
                            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
                        }
                    }
                }
            } else {
                // multi-dimension access
                if (container.getDimNames() != null) {
                    if (assignment) {
                        return findPositionWithNames(container, container.getDimNames().getDataAt(dimension), operand);
                    } else
                        return findPosition(container, container.getDimNames().getDataAt(dimension), operand);
                } else {
                    if (isSubset || container.getElementClass() == Object.class) {
                        throw RError.error(RError.Message.NO_ARRAY_DIMNAMES);
                    } else {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
                    }

                }
            }
        }

        @Specialization
        protected Object doIntVector(VirtualFrame frame, RAbstractContainer container, RAbstractIntVector operand) {
            if (operand.getLength() == 1) {
                opLengthOne.enter();
                return convertOperatorRecursive(frame, container, operand.getDataAt(0));
            }
            if (isSubset) {
                if (emptyOperandProfile.profile(operand.getLength() == 0)) {
                    return 0;
                } else {
                    return transformIntoPositive(container, operand);
                }
            } else {
                if (emptyOperandProfile.profile(operand.getLength() == 0)) {
                    if (listProfile.profile(container.getElementClass() == Object.class)) {
                        // container is a list
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
        protected Object doDouble(VirtualFrame frame, RAbstractContainer container, RAbstractDoubleVector operand) {
            return convertOperatorRecursive(frame, container, castInteger(frame, operand));
        }

        @Specialization
        protected Object doLogical(VirtualFrame frame, RAbstractContainer container, RAbstractLogicalVector operand) {
            if (emptyOperandProfile.profile(operand.getLength() == 0)) {
                return convertOperatorRecursive(frame, container, castInteger(frame, operand));
            } else if (operand.getLength() == 1) {
                opLengthOne.enter();
                return convertOperatorRecursive(frame, container, operand.getDataAt(0));
            } else {
                opLong.enter();
                if (isSubset) {
                    if ((operand.getLength() > getDimensionSize(container)) && numDimensions != 1) {
                        error.enter();
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.LOGICAL_SUBSCRIPT_LONG);
                    }
                    return doLogicalVectorInternal(container, operand);

                } else {
                    return convertOperatorRecursive(frame, container, castInteger(frame, operand));
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

        protected RIntVector doLogicalVectorInternal(RAbstractContainer container, RAbstractLogicalVector operand) {
            int resultLength = numDimensions == 1 ? Math.max(operand.getLength(), container.getLength()) : container.getDimensions()[dimension];
            int logicalVectorLength = operand.getLength();
            int logicalVectorInd = 0;
            int[] data = new int[resultLength];
            naCheck.enable(!operand.isComplete());
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
                    if (i >= container.getLength() && !assignment) {
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

        private Object handleInvalidVecType(VirtualFrame frame, RAbstractContainer container, RAbstractVector operand, String typeName) {
            if (operand.getLength() == 1) {
                opLengthOne.enter();
                return convertOperatorRecursive(frame, container, operand.getDataAtAsObject(0));
            } else {
                error.enter();
                if (operand.getLength() == 0 && !isSubset) {
                    if (container.getElementClass() == Object.class) {
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
        protected Object doComplex(VirtualFrame frame, RAbstractContainer container, RAbstractComplexVector operand) {
            return handleInvalidVecType(frame, container, operand, "complex");
        }

        @Specialization
        protected Object doRawx(VirtualFrame frame, RAbstractContainer container, RAbstractRawVector operand) {
            return handleInvalidVecType(frame, container, operand, "raw");
        }

        private RIntVector findPositions(RAbstractContainer container, Object namesObj, RAbstractStringVector operand, boolean retainNames) {
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

        private static RIntVector findPositionsWithNames(RAbstractContainer container, Object namesObj, RAbstractStringVector operand, boolean retainNames) {
            RStringVector resNames = operand.materialize();
            int initialPos = container.getLength();
            int[] data = new int[operand.getLength()];
            for (int i = 0; i < data.length; i++) {
                if (namesObj != RNull.instance) {
                    RStringVector names = (RStringVector) namesObj;
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
                        initialPos = eliminateDuplicate(operand, data, initialPos, i);
                    }
                } else {
                    // TODO: this is slow - is it important to make it faster?
                    initialPos = eliminateDuplicate(operand, data, initialPos, i);
                    // data[i] = (initialPos++) + 1;
                }
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization
        protected Object doString(VirtualFrame frame, RAbstractContainer container, RAbstractStringVector operand) {
            if (emptyOperandProfile.profile(operand.getLength() == 0)) {
                return convertOperatorRecursive(frame, container, castInteger(frame, operand));
            } else if (operand.getLength() == 1) {
                opLengthOne.enter();
                return convertOperatorRecursive(frame, container, operand.getDataAt(0));
            } else {
                opLong.enter();
                if (isSubset) {
                    if (numDimensions == 1) {
                        if (assignment) {
                            return findPositionsWithNames(container, container.getNames(), operand, assignment);
                        } else if (namesProfile.profile(container.getNames() != RNull.instance)) {
                            return findPositions(container, container.getNames(), operand, assignment);
                        } else {
                            int[] data = new int[operand.getLength()];
                            Arrays.fill(data, RRuntime.INT_NA);
                            return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
                        }
                    } else {
                        if (namesProfile.profile(container.getDimNames() != null)) {
                            if (assignment) {
                                return findPositionsWithNames(container, container.getDimNames().getDataAt(dimension), operand, assignment);
                            } else {
                                return findPositions(container, container.getDimNames().getDataAt(dimension), operand, assignment);
                            }
                        } else {
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

        private RAbstractIntVector transformIntoPositive(RAbstractContainer container, RAbstractIntVector positions) {
            boolean hasSeenPositive = false;
            boolean hasSeenZero = false;
            boolean hasSeenNegative = false;
            boolean hasSeenNA = false;
            int zeroCount = 0;
            positionNACheck.enable(positions);
            int positionsLength = positions.getLength();
            int dimLength = numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension];
            boolean outOfBounds = false;
            for (int i = 0; i < positionsLength; ++i) {
                int pos = positions.getDataAt(i);
                if (positionNACheck.check(pos)) {
                    seenNA.enter();
                    hasSeenNA = true;
                } else if (pos > 0) {
                    if (numDimensions != 1 && pos > dimLength) {
                        throw RError.error(RError.Message.SUBSCRIPT_BOUNDS);
                    }
                    if (numDimensions == 1 && pos > container.getLength()) {
                        if (isSubset) {
                            outOfBounds = true;
                        } else {
                            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
                        }
                    }
                    seenPositive.enter();
                    hasSeenPositive = true;
                } else if (pos == 0) {
                    if (!isSubset) {
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

        private int getDimensionSize(RAbstractContainer container) {
            verifyDimensions(container);
            return numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension];
        }

        protected boolean isVectorList(RAbstractContainer container) {
            return container.getElementClass() == Object.class;
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
            return operand.getNames() != RNull.instance;
        }

    }

}
