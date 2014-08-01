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
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;
import com.oracle.truffle.r.nodes.access.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.access.ArrayPositionCastFactory.OperatorConverterNodeFactory;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "op", type = RNode.class), @NodeChild(value = "vector", type = RNode.class),
                @NodeChild(value = "operand", type = OperatorConverterNode.class, executeWith = {"vector", "op"})})
public abstract class ArrayPositionCast extends RNode {

    public abstract Object executeArg(VirtualFrame frame, Object op, Object vector, Object operand);

    abstract RNode getVector();

    private final int dimension;

    private final int numDimensions;

    private final boolean assignment;

    private final boolean isSubset;

    protected ArrayPositionCast(int dimension, int numDimensions, boolean assignment, boolean isSubset) {
        this.dimension = dimension;
        this.numDimensions = numDimensions;
        this.assignment = assignment;
        this.isSubset = isSubset;
    }

    protected ArrayPositionCast(ArrayPositionCast other) {
        this.dimension = other.dimension;
        this.numDimensions = other.numDimensions;
        this.assignment = other.assignment;
        this.isSubset = other.isSubset;
    }

    protected static void verifyDimensions(VirtualFrame frame, RAbstractContainer container, int dimension, int numDimensions, boolean assignment, boolean isSubset, SourceSection sourceSection) {
        if ((container.getDimensions() == null && (dimension != 0 || numDimensions > 1)) || (container.getDimensions() != null && dimension >= container.getDimensions().length)) {
            if (assignment) {
                if (isSubset) {
                    if (numDimensions == 2) {
                        throw RError.error(frame, sourceSection, RError.Message.INCORRECT_SUBSCRIPTS_MATRIX);
                    } else {
                        throw RError.error(frame, sourceSection, RError.Message.INCORRECT_SUBSCRIPTS);
                    }
                } else {
                    throw RError.error(frame, sourceSection, RError.Message.IMPROPER_SUBSCRIPT);
                }
            } else {
                throw RError.error(frame, sourceSection, RError.Message.INCORRECT_DIMENSIONS);
            }
        }
    }

    @Specialization(order = 1)
    public RIntVector doMissingVector(Object op, RNull vector, RAbstractIntVector operand) {
        return operand.materialize();
    }

    @Specialization(order = 2)
    public Object doFuncOp(Object op, RFunction vector, Object operand) {
        return operand;
    }

    @Specialization(order = 3)
    public RIntVector doMissingVector(VirtualFrame frame, Object op, RAbstractContainer container, RMissing operand) {
        verifyDimensions(frame, container, dimension, numDimensions, assignment, isSubset, getEncapsulatingSourceSection());
        int[] data = new int[numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension]];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1;
        }

        return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 4)
    public RNull doNullSubset(Object op, RAbstractContainer container, RNull operand) {
        // this is a special case - RNull can only appear to represent the x[[NA]] case which has to
        // return null and not a null vector
        return operand;
    }

    @Specialization(order = 5)
    public RStringVector doStringVector(Object op, RList vector, RStringVector operand) {
        // recursive access to the list
        return operand;
    }

    @Specialization(order = 6)
    public RList doList(Object op, RAbstractContainer container, RList operand) {
        return operand;
    }

    @Specialization(order = 7)
    public RComplex doList(Object op, RAbstractContainer container, RComplex operand) {
        return operand;
    }

    @Specialization(order = 8)
    public RRaw doList(Object op, RAbstractContainer container, RRaw operand) {
        return operand;
    }

    @Specialization(order = 16, guards = {"sizeOneOp", "numDimensionsOne", "!operandHasNames"})
    public int doIntVectorSizeOne(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        int val = operand.getDataAt(0);
        return val;
    }

    @Specialization(order = 17, guards = {"sizeOneOp", "numDimensionsOne", "operandHasNames"})
    public RIntVector doIntVectorSizeOneNames(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        assert operand.getDataAt(0) != 0;
        return operand.materialize();
    }

    @Specialization(order = 21, guards = {"sizeOneOp", "!numDimensionsOne"})
    public RIntVector doIntVectorSizeOneMultiDim(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        return operand.materialize();
    }

    @Specialization(order = 22, guards = {"!emptyOperand", "!sizeOneOp", "!numDimensionsOne"})
    public RIntVector doIntVectorMultiDim(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        return operand.materialize();
    }

    @Specialization(order = 23, guards = {"!emptyOperand", "!sizeOneOp", "numDimensionsOne"})
    public RIntVector doIntVectorOneDim(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        return operand.materialize();
    }

    @Specialization(order = 24, guards = "emptyOperand")
    public int doIntVectorZero(Object op, RAbstractContainer container, RAbstractIntVector operand) {
        return 0;
    }

    public static boolean sizeOneOp(Object op, RAbstractContainer container, RAbstractIntVector operand) {
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
    public abstract static class OperatorConverterNode extends RNode {

        public abstract Object executeConvert(VirtualFrame frame, Object vector, Object operand);

        private final int dimension;
        private final int numDimensions;
        private final boolean assignment;
        private final boolean isSubset;

        @Child private OperatorConverterNode operatorConvertRecursive;
        @Child private CastIntegerNode castInteger;
        private final NACheck naCheck = NACheck.create();

        protected OperatorConverterNode(int dimension, int numDimensions, boolean assignment, boolean isSubset) {
            this.dimension = dimension;
            this.numDimensions = numDimensions;
            this.assignment = assignment;
            this.isSubset = isSubset;
        }

        protected OperatorConverterNode(OperatorConverterNode other) {
            this.dimension = other.dimension;
            this.numDimensions = other.numDimensions;
            this.assignment = other.assignment;
            this.isSubset = other.isSubset;
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

        @Specialization(order = 0)
        public RList doList(RAbstractContainer container, RList operand) {
            return operand;
        }

        @Specialization(order = 1)
        public RMissing doFuncOp(VirtualFrame frame, RAbstractContainer container, RFunction operand) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "closure");
        }

        @Specialization(order = 6, guards = "dimLengthOne")
        public int doMissingDimLengthOne(VirtualFrame frame, RAbstractContainer container, RMissing operand) {
            if (!isSubset) {
                if (assignment) {
                    throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.MISSING_SUBSCRIPT);
                } else {
                    throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "symbol");
                }
            }
            return 1;
        }

        @Specialization(order = 7, guards = "!dimLengthOne")
        public RMissing doMissing(VirtualFrame frame, RAbstractContainer container, RMissing operand) {
            if (!isSubset) {
                if (assignment) {
                    throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.MISSING_SUBSCRIPT);
                } else {
                    throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "symbol");
                }
            }
            return operand;
        }

        @Specialization(order = 8)
        public int doNull(VirtualFrame frame, RAbstractContainer container, RNull operand) {
            if (isSubset) {
                return 0;
            } else {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
            }
        }

        @Specialization(order = 9, guards = {"indNA", "isSubset", "numDimensionsOne"})
        public int doIntNASubset(RList vector, int operand) {
            return operand;
        }

        @Specialization(order = 10, guards = {"indNA", "!isSubset", "numDimensionsOne"})
        public RNull doIntNA(RList vector, int operand) {
            return RNull.instance;
        }

        @Specialization(order = 11, guards = {"indNA", "!numDimensionsOne"})
        public int doIntNAMultiDim(RList vector, int operand) {
            return operand;
        }

        @Specialization(order = 12, guards = {"indNA", "isSubset", "!isVectorList"})
        public int doIntNASubset(RAbstractContainer container, int operand) {
            return operand;
        }

        @Specialization(order = 13, guards = {"indNA", "!isSubset", "!isVectorList"})
        public int doIntNA(VirtualFrame frame, RAbstractContainer container, int operand) {
            if (!assignment) {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
            } else {
                // let assignment handle it as it depends on the value
                return RRuntime.INT_NA;
            }
        }

        @Specialization(order = 14, guards = {"!indNA", "!isSubset", "!isNegative"})
        public int doIntNegative(RList vector, int operand) {
            return operand;
        }

        @Specialization(order = 15, guards = {"!indNA", "!isSubset", "outOfBoundsNegative"})
        public int doIntOutOfBoundsNegative(RList vector, int operand) {
            return operand;
        }

        @Specialization(order = 16, guards = {"!indNA", "outOfBounds", "numDimensionsOne"})
        public int doIntOutOfBoundsOneDim(VirtualFrame frame, RAbstractContainer container, int operand) {
            if (assignment) {
                return operand;
            } else {
                if (isSubset) {
                    return RRuntime.INT_NA;
                } else {
                    throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
                }
            }
        }

        @Specialization(order = 17, guards = {"!indNA", "outOfBounds", "!numDimensionsOne"})
        public int doIntOutOfBounds(VirtualFrame frame, RAbstractContainer container, int operand) {
            throw RError.error(frame, assignment ? getEncapsulatingSourceSection() : null, RError.Message.SUBSCRIPT_BOUNDS);
        }

        @Specialization(order = 18, guards = {"!indNA", "outOfBoundsNegative", "dimLengthOne", "isSubset"})
        public int doIntOutOfBoundsNegativeOneElementSubset(RAbstractContainer container, int operand) {
            // there is only one element to be picked
            return 1;
        }

        @Specialization(order = 19, guards = {"!indNA", "outOfBoundsNegative", "dimLengthOne", "!isSubset"})
        public int doIntOutOfBoundsNegativeOneElementAccess(RAbstractContainer container, int operand) {
            return operand;
        }

        @Specialization(order = 20, guards = {"!indNA", "outOfBoundsNegative", "!dimLengthOne", "isSubset"})
        public RMissing doIntOutOfBoundsNegativeSubset(RAbstractContainer container, int operand) {
            // all indexes - result is the same as with missing index
            return RMissing.instance;
        }

        @Specialization(order = 22, guards = {"!indNA", "outOfBoundsNegative", "!dimLengthOne", "!isSubset"})
        public int doIntOutOfBoundsNegativeAccess(RAbstractContainer container, int operand) {
            return operand;
        }

        @Specialization(order = 23, guards = {"!indNA", "!outOfBounds", "!isNegative"})
        public int doInt(RAbstractContainer container, int operand) {
            return operand;
        }

        @Specialization(order = 24, guards = {"!indNA", "isNegative", "!outOfBoundsNegative", "dimLengthOne"})
        public int doIntNegativeNoDimLeft(RAbstractContainer container, int operand) {
            // it's negative, but not out of bounds and dimension has length one - result is no
            // dimensions left
            return 0;
        }

        @Specialization(order = 25, guards = {"isSubset", "!indNA", "isNegative", "!outOfBoundsNegative", "!dimLengthOne", "!vecLengthTwo"})
        public RIntVector doIntNegativeSubset(RAbstractContainer container, int operand) {
            // it's negative, but not out of bounds - pick all indexes apart from the negative one
            int dimLength = numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension];
            int[] positions = new int[dimLength - 1];
            int ind = 0;
            for (int i = 1; i <= dimLength; i++) {
                if (i != -operand) {
                    positions[ind++] = i;
                }
            }
            return RDataFactory.createIntVector(positions, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(order = 26, guards = {"!isSubset", "!indNA", "isNegative", "!outOfBoundsNegative", "!dimLengthOne", "!vecLengthTwo"})
        public int doIntNegative(RAbstractContainer container, int operand) {
            return operand;
        }

        @Specialization(order = 27, guards = {"opNegOne", "vecLengthTwo"})
        public int doIntNegOne(RAbstractContainer container, int operand) {
            return 2;
        }

        @Specialization(order = 28, guards = {"opNegTwo", "vecLengthTwo"})
        public int doIntNegTow(RAbstractContainer container, int operand) {
            return 1;
        }

        @Specialization(order = 30, guards = "!isNegative")
        public Object doDouble(VirtualFrame frame, RAbstractContainer container, double operand) {
            return convertOperatorRecursive(frame, container, castInteger(frame, operand));
        }

        @Specialization(order = 31, guards = "isNegative")
        public Object doDoubleNegative(VirtualFrame frame, RAbstractContainer container, double operand) {
            // returns object as it may return either int or RIntVector due to conversion
            return convertOperatorRecursive(frame, container, castInteger(frame, operand));
        }

        @Specialization(order = 32, guards = {"indNA", "numDimensionsOne", "!isSubset"})
        public RNull doLogicalDimLengthOne(RList vector, byte operand) {
            return RNull.instance;
        }

        @Specialization(order = 33, guards = {"indNA", "numDimensionsOne", "!isSubset", "!isVectorList"})
        public int doLogicalDimLengthOne(RAbstractContainer container, byte operand) {
            return RRuntime.INT_NA;
        }

        @Specialization(order = 34, guards = {"indNA", "numDimensionsOne", "isSubset", "isAssignment"})
        public int doLogicalNASubsetDimOneAssignment(RAbstractContainer container, byte operand) {
            return RRuntime.INT_NA;
        }

        @Specialization(order = 35, guards = {"indNA", "numDimensionsOne", "isSubset", "!isAssignment"})
        public RIntVector doLogicalNASubsetDimOne(RAbstractContainer container, byte operand) {
            int dimLength = numDimensions == 1 ? (container.getLength() == 0 ? 1 : container.getLength()) : container.getDimensions()[dimension];
            int[] data = new int[dimLength];
            Arrays.fill(data, RRuntime.INT_NA);
            return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
        }

        @Specialization(order = 36, guards = {"indNA", "!numDimensionsOne", "isSubset", "dimLengthOne"})
        public int doLogicalDimLengthOneSubset(VirtualFrame frame, RAbstractContainer container, byte operand) {
            return (int) castInteger(frame, operand);
        }

        @Specialization(order = 37, guards = {"indNA", "!numDimensionsOne", "isSubset", "!dimLengthOne", "isAssignment"})
        public int doLogicalNASubsetAssignment(RAbstractContainer container, byte operand) {
            return RRuntime.INT_NA;
        }

        @Specialization(order = 38, guards = {"indNA", "!numDimensionsOne", "isSubset", "!dimLengthOne", "!isAssignment"})
        public RIntVector doLogicalNASubset(RAbstractContainer container, byte operand) {
            int dimLength = numDimensions == 1 ? (container.getLength() == 0 ? 1 : container.getLength()) : container.getDimensions()[dimension];
            int[] data = new int[dimLength];
            Arrays.fill(data, RRuntime.INT_NA);
            return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
        }

        @Specialization(order = 39, guards = {"indNA", "!numDimensionsOne", "!isSubset"})
        public int doLogicalNA(RAbstractContainer container, byte operand) {
            return RRuntime.INT_NA;
        }

        @Specialization(order = 40, guards = {"indTrue", "isSubset"})
        public RIntVector doLogicalIndTrue(RAbstractContainer container, byte operand) {
            int dimLength = numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension];
            int[] data = new int[dimLength];
            for (int i = 0; i < dimLength; i++) {
                data[i] = i + 1;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(order = 41, guards = {"!indTrue", "!indNA", "isSubset"})
        public int doLogicalIndFalse(VirtualFrame frame, RAbstractContainer container, byte operand) {
            return 0;
        }

        @Specialization(order = 42, guards = {"!indNA", "!isSubset"})
        public int doLogical(VirtualFrame frame, RAbstractContainer container, byte operand) {
            return (int) castInteger(frame, operand);
        }

        @Specialization(order = 44)
        public RComplex doComplexValLengthZero(RAbstractContainer container, RComplex operand) {
            return operand;
        }

        @Specialization(order = 45)
        public RRaw doRaw(RAbstractContainer container, RRaw operand) {
            return operand;
        }

        private int findPosition(VirtualFrame frame, RAbstractContainer container, Object namesObj, String operand) {
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
                throw RError.error(frame, isSubset ? null : getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
            }
        }

        private static RIntVector findPositionWithNames(RAbstractContainer container, RStringVector names, String operand) {
            RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, !RRuntime.isNA(operand));
            int position = -1;
            if (names != null) {
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

        @Specialization(order = 48, guards = {"indNA", "!numDimensionsOne"})
        public Object doStringNA(VirtualFrame frame, RAbstractContainer container, String operand) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
        }

        @Specialization(order = 49, guards = {"indNA", "isAssignment", "numDimensionsOne"})
        public RIntVector doStringNANumDimsOneAssignment(VirtualFrame frame, RAbstractContainer container, String operand) {
            RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createIntVector(new int[]{container.getLength() + 1}, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization(order = 50, guards = {"indNA", "!isAssignment", "numDimensionsOne"})
        public Object doStringNANumDimsOne(VirtualFrame frame, RAbstractContainer container, String operand) {
            return convertOperatorRecursive(frame, container, RRuntime.INT_NA);
        }

        @Specialization(order = 51, guards = {"hasNames", "isAssignment", "numDimensionsOne"})
        public RIntVector doStringOneDimNamesAssignment(RAbstractContainer container, String operand) {
            RStringVector names = (RStringVector) container.getNames();
            return findPositionWithNames(container, names, operand);
        }

        @Specialization(order = 52, guards = {"isSubset", "hasNames", "!isAssignment", "numDimensionsOne"})
        public Object doStringOneDimNamesSubset(VirtualFrame frame, RList vector, String operand) {
            RStringVector names = (RStringVector) vector.getNames();
            return findPosition(frame, vector, names, operand);
        }

        @Specialization(order = 53, guards = {"!isSubset", "hasNames", "!isAssignment", "numDimensionsOne"})
        public Object doStringOneDimNames(VirtualFrame frame, RList vector, String operand) {
            // we need to return either an int or null - is there a prettier way to handle this?
            RStringVector names = (RStringVector) vector.getNames();
            int result = findPosition(frame, vector, names, operand);
            if (RRuntime.isNA(result)) {
                return RNull.instance;
            } else {
                return result;
            }
        }

        @Specialization(order = 54, guards = {"hasNames", "!isAssignment", "numDimensionsOne"})
        public int doStringOneDimNames(VirtualFrame frame, RAbstractContainer container, String operand) {
            RStringVector names = (RStringVector) container.getNames();
            return findPosition(frame, container, names, operand);
        }

        @Specialization(order = 55, guards = {"!hasNames", "isAssignment", "numDimensionsOne"})
        public RIntVector doStringOneDimAssignment(RAbstractContainer container, String operand) {
            return findPositionWithNames(container, null, operand);
        }

        @Specialization(order = 56, guards = {"isAssignment", "numDimensionsOne"})
        public RIntVector doStringOneDimAssignment(RNull vector, String operand) {
            RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, !RRuntime.isNA(operand));
            return RDataFactory.createIntVector(new int[]{1}, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization(order = 57, guards = {"hasDimNames", "!numDimensionsOne"})
        public int doString(VirtualFrame frame, RAbstractContainer container, String operand) {
            RList dimNames = container.getDimNames();
            Object names = dimNames.getDataAt(dimension);
            return findPosition(frame, container, names, operand);
        }

        @Specialization(order = 58, guards = "isSubset")
        public int doStringNoNamesSubset(VirtualFrame frame, RList vector, String operand) {
            if (numDimensions == 1) {
                return RRuntime.INT_NA;
            } else {
                throw RError.error(frame, RError.Message.NO_ARRAY_DIMNAMES);
            }
        }

        @Specialization(order = 59, guards = "!isSubset")
        public RNull doStringNoNames(VirtualFrame frame, RList vector, String operand) {
            if (numDimensions == 1) {
                return RNull.instance;
            } else {
                throw RError.error(frame, RError.Message.NO_ARRAY_DIMNAMES);
            }
        }

        @Specialization(order = 60)
        public int doStringNoNames(VirtualFrame frame, RAbstractContainer container, String operand) {
            if (isSubset) {
                if (numDimensions == 1) {
                    return RRuntime.INT_NA;
                } else {
                    throw RError.error(frame, RError.Message.NO_ARRAY_DIMNAMES);
                }
            } else {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
            }
        }

        @Specialization(order = 65, guards = {"!isSubset", "!opLengthZero", "!opLengthOne"})
        public RAbstractIntVector doIntVectorOp(RAbstractContainer container, RAbstractIntVector operand) {
            // no transformation - if it's a list, then it's handled during recursive access, if
            // it's not then it's an error dependent on the value
            return operand;
        }

        @Specialization(order = 66, guards = {"!isSubset", "opLengthZero"})
        public RAbstractIntVector doIntVectorOp(VirtualFrame frame, RList vector, RAbstractVector operand) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }

        @Specialization(order = 67, guards = {"!isSubset", "!opLengthZero", "!opLengthOne"})
        public RAbstractIntVector doIntVectorOp(VirtualFrame frame, RList vector, RAbstractDoubleVector operand) {
            return (RIntVector) castInteger(frame, operand);
        }

        @Specialization(order = 68, guards = {"!isSubset", "!opLengthZero", "!opLengthOne"})
        public RAbstractIntVector doIntVectorOp(VirtualFrame frame, RList vector, RAbstractLogicalVector operand) {
            return (RIntVector) castInteger(frame, operand);
        }

        @Specialization(order = 70, guards = {"!isSubset", "opLengthZero"})
        public int doIntEmptyOp(VirtualFrame frame, RAbstractContainer container, RAbstractVector operand) {
            return 0;
        }

        @Specialization(order = 100, guards = "opLengthOne")
        public Object doIntVectorOpLengthOne(VirtualFrame frame, RAbstractContainer container, RAbstractIntVector operand) {
            return convertOperatorRecursive(frame, container, operand.getDataAt(0));
        }

        @Specialization(order = 101, guards = {"isSubset", "!opLengthOne", "!opLengthZero"})
        public RAbstractIntVector doIntVectorOpSubset(VirtualFrame frame, RAbstractContainer container, RAbstractIntVector operand) {
            return transformIntoPositive(frame, container, operand);
        }

        @Specialization(order = 103, guards = {"isSubset", "opLengthZero"})
        public int doIntVectorFewManySelected(RAbstractContainer container, RAbstractIntVector operand) {
            return 0;
        }

        @Specialization(order = 120, guards = "opLengthOne")
        public Object doDoubleVectorOpLengthOne(VirtualFrame frame, RAbstractContainer container, RAbstractDoubleVector operand) {
            return convertOperatorRecursive(frame, container, operand.getDataAt(0));
        }

        @Specialization(order = 121, guards = "!opLengthOne")
        public Object doDoubleVector(VirtualFrame frame, RAbstractContainer container, RAbstractDoubleVector operand) {
            return convertOperatorRecursive(frame, container, castInteger(frame, operand));
        }

        @Specialization(order = 135, guards = "opLengthOne")
        public Object doLogicalVectorOpLengthOne(VirtualFrame frame, RAbstractContainer container, RAbstractLogicalVector operand) {
            return convertOperatorRecursive(frame, container, operand.getDataAt(0));
        }

        @Specialization(order = 136, guards = {"outOfBounds", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doLogicalVectorOutOfBounds(VirtualFrame frame, RAbstractContainer container, RAbstractLogicalVector operand) {
            throw RError.error(frame, isSubset ? null : getEncapsulatingSourceSection(), RError.Message.LOGICAL_SUBSCRIPT_LONG);
        }

        @Specialization(order = 137, guards = {"outOfBounds", "!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doLogicalVectorOutOfBoundsTooManySelected(VirtualFrame frame, RAbstractContainer container, RAbstractLogicalVector operand) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
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

        @Specialization(order = 138, guards = {"!outOfBounds", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doLogicalVector(RAbstractContainer container, RAbstractLogicalVector operand) {
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

        @Specialization(order = 139, guards = {"!outOfBounds", "!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doLogicalVectorTooManySelected(VirtualFrame frame, RAbstractContainer container, RAbstractLogicalVector operand) {
            if (operand.getLength() == 2 && operand.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
            } else {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
            }
        }

        @Specialization(order = 140, guards = {"isSubset", "opLengthZero"})
        public int doDoubleVectorTooFewSelected(RAbstractContainer container, RAbstractLogicalVector operand) {
            return 0;
        }

        @Specialization(order = 150, guards = "opLengthOne")
        public Object doComplexVectorOpLengthOne(VirtualFrame frame, RAbstractContainer container, RAbstractComplexVector operand) {
            return convertOperatorRecursive(frame, container, operand.getDataAt(0));
        }

        @Specialization(order = 151, guards = {"isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doComplexVectorSubset(VirtualFrame frame, RAbstractContainer container, RAbstractComplexVector operand) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
        }

        @Specialization(order = 152, guards = {"!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doComplexVector(VirtualFrame frame, RAbstractContainer container, RAbstractComplexVector operand) {
            if (operand.getLength() == 2) {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
            } else {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
            }
        }

        @Specialization(order = 153, guards = {"isSubset", "opLengthZero"})
        public RIntVector doComplexVectoTooFewSelectedSubset(VirtualFrame frame, RAbstractContainer container, RAbstractComplexVector operand) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
        }

        @Specialization(order = 160, guards = "opLengthOne")
        public Object doRawVectorOpLengthOne(VirtualFrame frame, RAbstractContainer container, RAbstractRawVector operand) {
            return convertOperatorRecursive(frame, container, operand.getDataAt(0));
        }

        @Specialization(order = 161, guards = {"isSubset", "!opLengthOne", "!opLengthZero"})
        public RAbstractIntVector doRawVectorSubset(VirtualFrame frame, RAbstractContainer container, RAbstractRawVector operand) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
        }

        @Specialization(order = 162, guards = {"!isSubset", "!opLengthOne", "!opLengthZero"})
        public RAbstractIntVector doRawVector(VirtualFrame frame, RAbstractContainer container, RAbstractRawVector operand) {
            if (operand.getLength() == 2) {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
            } else {
                throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
            }
        }

        @Specialization(order = 163, guards = {"isSubset", "opLengthZero"})
        public RIntVector doRawVectorTooFewSelectedSubset(VirtualFrame frame, RAbstractContainer container, RAbstractRawVector operand) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
        }

        private RIntVector findPositions(VirtualFrame frame, RAbstractContainer container, RStringVector names, RAbstractStringVector operand, boolean retainNames) {
            int[] data = new int[operand.getLength()];
            boolean seenNA = false;
            for (int i = 0; i < data.length; i++) {
                String positionName = operand.getDataAt(i);
                int j = 0;
                for (; j < names.getLength(); j++) {
                    if (positionName.equals(names.getDataAt(j))) {
                        data[i] = j + 1;
                        break;
                    }
                }
                if (j == names.getLength()) {
                    if (numDimensions == 1) {
                        data[i] = RRuntime.INT_NA;
                        seenNA = true;
                    } else {
                        throw RError.error(frame, RError.Message.SUBSCRIPT_BOUNDS);
                    }
                }
            }
            return RDataFactory.createIntVector(data, !seenNA);
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

        private static RIntVector findPositionsWithNames(RAbstractContainer container, RStringVector names, RAbstractStringVector operand, boolean retainNames) {
            RStringVector resNames = operand.materialize();
            int initialPos = container.getLength();
            int[] data = new int[operand.getLength()];
            for (int i = 0; i < data.length; i++) {
                if (names != null) {
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

        @Specialization(order = 170, guards = "opLengthOne")
        public Object doStringlVectorOpLengthOne(VirtualFrame frame, RAbstractContainer container, RAbstractStringVector operand) {
            return convertOperatorRecursive(frame, container, operand.getDataAt(0));
        }

        @Specialization(order = 171, guards = {"hasNames", "isAssignment", "numDimensionsOne", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorOneDimNamesAssignment(RAbstractContainer container, RAbstractStringVector operand) {
            RStringVector names = (RStringVector) container.getNames();
            return findPositionsWithNames(container, names, operand, assignment);
        }

        @Specialization(order = 172, guards = {"hasNames", "!isAssignment", "numDimensionsOne", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorOneDimNames(VirtualFrame frame, RAbstractContainer container, RAbstractStringVector operand) {
            RStringVector names = (RStringVector) container.getNames();
            return findPositions(frame, container, names, operand, assignment);
        }

        @Specialization(order = 173, guards = {"!hasNames", "isAssignment", "numDimensionsOne", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorOneDimAssignment(RAbstractContainer container, RAbstractStringVector operand) {
            return findPositionsWithNames(container, null, operand, assignment);
        }

        @Specialization(order = 174, guards = {"isAssignment", "numDimensionsOne", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorOneDimAssignment(VirtualFrame frame, RNull vector, RAbstractStringVector operand) {
            // we need to get rid of duplicates but retain all NAs
            int[] data = new int[operand.getLength()];
            int initialPos = 0;
            for (int i = 0; i < data.length; i++) {
                // TODO: this is slow - is it important to make it faster?
                initialPos = eliminateDuplicate(operand, data, initialPos, i);
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR, operand.materialize());
        }

        @Specialization(order = 175, guards = {"hasDimNames", "!numDimensionsOne", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVector(VirtualFrame frame, RAbstractContainer container, RAbstractStringVector operand) {
            RList dimNames = container.getDimNames();
            RStringVector names = (RStringVector) dimNames.getDataAt(dimension);
            return findPositions(frame, container, names, operand, false);
        }

        @Specialization(order = 176, guards = {"!isSubset", "!opLengthOne", "!opLengthZero", "numDimensionsOne"})
        public RAbstractStringVector doStringVectorTooManySelected(RList vector, RAbstractStringVector operand) {
            // for recursive access
            return operand;
        }

        @Specialization(order = 177, guards = {"!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorTooManySelected(VirtualFrame frame, RAbstractContainer container, RAbstractStringVector operand) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        }

        @Specialization(order = 178, guards = {"isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorNoDimNames(VirtualFrame frame, RAbstractContainer container, RAbstractStringVector operand) {
            if (numDimensions == 1) {
                int[] data = new int[operand.getLength()];
                Arrays.fill(data, RRuntime.INT_NA);
                return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
            } else {
                throw RError.error(frame, RError.Message.SUBSCRIPT_BOUNDS);
            }
        }

        @Specialization(order = 179, guards = "opLengthZero")
        public int doStringVectorTooFewSelected(RAbstractContainer container, RAbstractStringVector operand) {
            return 0;
        }

        @Specialization(order = 200, guards = {"numDimensionsOne", "operandHasNames", "!opLengthOne", "!opLengthZero"})
        public RAbstractIntVector doMissingVector(RNull vector, RAbstractIntVector operand) {
            RIntVector resPositions = (RIntVector) operand.copy();
            resPositions.setNames(null);
            return resPositions;
        }

        @Specialization(order = 201, guards = {"numDimensionsOne", "operandHasNames", "opLengthZero"})
        public Object doMissingVectorOpLengthZero(VirtualFrame frame, RNull vector, RAbstractIntVector operand) {
            return castInteger(frame, operand);
        }

        @Specialization(order = 202, guards = {"numDimensionsOne", "operandHasNames", "opLengthOne"})
        public Object doMissingVectorOpLengthOne(VirtualFrame frame, RNull vector, RAbstractIntVector operand) {
            return castInteger(frame, operand);
        }

        @Specialization(order = 203, guards = {"numDimensionsOne", "!operandHasNames"})
        public Object doMissingVectorNoNames(VirtualFrame frame, RNull vector, RAbstractIntVector operand) {
            return castInteger(frame, operand);
        }

        @Specialization(order = 204, guards = "!numDimensionsOne")
        public Object doMissingVectorDimGreaterThanOne(VirtualFrame frame, RNull vector, RAbstractIntVector operand) {
            return castInteger(frame, operand);
        }

        @Specialization(order = 210)
        public Object doMissingVector(VirtualFrame frame, RNull vector, RAbstractDoubleVector operand) {
            return castInteger(frame, operand);
        }

        @Specialization(order = 211)
        public Object doMissingVector(VirtualFrame frame, RNull vector, RAbstractLogicalVector operand) {
            return castInteger(frame, operand);
        }

        @Specialization(order = 212)
        public Object doMissingVector(VirtualFrame frame, RNull vector, RAbstractComplexVector operand) {
            return castInteger(frame, operand);
        }

        @Specialization(order = 213)
        public Object doMissingVector(VirtualFrame frame, RNull vector, RAbstractRawVector operand) {
            return castInteger(frame, operand);
        }

        @Specialization(order = 300)
        public Object doFuncOp(RFunction vector, Object operand) {
            return operand;
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

        private AssumedValue<Boolean> hasSeenPositive = new AssumedValue<>("hasSeenPositive", false);
        private AssumedValue<Boolean> hasSeenZero = new AssumedValue<>("hasSeenZero", false);
        private AssumedValue<Boolean> hasSeenNegative = new AssumedValue<>("hasSeenNegative", false);
        private AssumedValue<Boolean> hasSeenNA = new AssumedValue<>("hasSeenNA", false);

        private RAbstractIntVector transformIntoPositive(VirtualFrame frame, RAbstractContainer container, RAbstractIntVector positions) {
            int zeroCount = 0;
            positionNACheck.enable(positions);
            int positionsLength = positions.getLength();
            int dimLength = numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension];
            boolean outOfBounds = false;
            for (int i = 0; i < positionsLength; ++i) {
                int pos = positions.getDataAt(i);
                if (positionNACheck.check(pos)) {
                    if (!hasSeenNA.get()) {
                        hasSeenNA.set(true);
                    }
                } else if (pos > 0) {
                    if (numDimensions != 1 && pos > dimLength) {
                        throw RError.error(frame, RError.Message.SUBSCRIPT_BOUNDS);
                    }
                    if (numDimensions == 1 && pos > container.getLength()) {
                        if (isSubset) {
                            outOfBounds = true;
                        } else {
                            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
                        }
                    }
                    if (!hasSeenPositive.get()) {
                        hasSeenPositive.set(true);
                    }
                } else if (pos == 0) {
                    if (!isSubset) {
                        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
                    }
                    if (!hasSeenZero.get()) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenZero.set(true);
                    }
                    zeroCount++;
                } else {
                    if (!hasSeenNegative.get()) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenNegative.set(true);
                    }
                }
            }
            if (hasSeenPositive.get() || hasSeenNA.get()) {
                if (hasSeenNegative.get()) {
                    throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ONLY_0_MIXED);
                } else if (hasSeenZero.get() || (outOfBounds && numDimensions == 1)) {
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
            } else if (hasSeenNegative.get()) {
                if (hasSeenNA.get()) {
                    throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ONLY_0_MIXED);
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

        protected boolean vecLengthTwo(RAbstractContainer container, int operand) {
            return container.getLength() == 2;
        }

        protected boolean opNegOne(RAbstractContainer container, int operand) {
            return operand == -1;
        }

        protected boolean opNegTwo(RAbstractContainer container, int operand) {
            return operand == -2;
        }

        protected boolean valLenZero(RAbstractContainer container, Object operand, RAbstractVector value) {
            return value.getLength() == 0;
        }

        protected boolean valLenOne(RAbstractContainer container, Object operand, RAbstractVector value) {
            return value.getLength() == 1;
        }

        private int getDimensionSize(VirtualFrame frame, RAbstractContainer container) {
            verifyDimensions(frame, container, dimension, numDimensions, assignment, isSubset, getEncapsulatingSourceSection());
            return numDimensions == 1 ? container.getLength() : container.getDimensions()[dimension];
        }

        protected boolean isVectorList(RAbstractContainer container) {
            return container.getElementClass() == Object.class;
        }

        protected boolean dimLengthOne(VirtualFrame frame, RAbstractContainer container, byte operand) {
            return getDimensionSize(frame, container) == 1;
        }

        protected boolean dimLengthOne(VirtualFrame frame, RAbstractContainer container, int operand) {
            return getDimensionSize(frame, container) == 1;
        }

        protected boolean dimLengthOne(VirtualFrame frame, RAbstractContainer container, RMissing operand) {
            return getDimensionSize(frame, container) == 1;
        }

        protected boolean dimLengthOne(VirtualFrame frame, RAbstractContainer container, RAbstractVector operand) {
            return getDimensionSize(frame, container) == 1;
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

        protected boolean outOfBoundsNegative(VirtualFrame frame, RAbstractContainer container, int operand) {
            return operand < 0 && -operand > getDimensionSize(frame, container);
        }

        protected boolean outOfBounds(VirtualFrame frame, RAbstractContainer container, RAbstractLogicalVector operand) {
            // out of bounds if multi dimensional access with a logical vector that's too long (in
            // single-dimensional case it's OK)
            return (operand.getLength() > getDimensionSize(frame, container)) && numDimensions != 1;
        }

        protected boolean outOfBounds(VirtualFrame frame, RAbstractContainer container, int operand) {
            return operand > getDimensionSize(frame, container);
        }

        protected boolean numDimensionsOne() {
            return numDimensions == 1;
        }

        protected boolean isNegative(RAbstractContainer container, int operand) {
            return operand < 0;
        }

        protected boolean isNegative(RAbstractContainer container, double operand) {
            return operand < 0;
        }

        protected boolean hasDimNames(RAbstractContainer container, String operand) {
            return container.getDimNames() != null;
        }

        protected boolean hasNames(RAbstractContainer container, String operand) {
            return container.getNames() != RNull.instance;
        }

        protected boolean hasDimNames(RAbstractContainer container, RAbstractStringVector operand) {
            return container.getDimNames() != null;
        }

        protected boolean hasNames(RAbstractContainer container, RAbstractStringVector operand) {
            return container.getNames() != RNull.instance;
        }

        protected boolean opLengthTwo(RAbstractContainer container, RList operand) {
            return operand.getLength() == 2;
        }

        protected boolean opLengthOne(RAbstractContainer container, RAbstractVector operand) {
            return operand.getLength() == 1;
        }

        protected boolean opLengthOne(RNull vector, RAbstractVector operand) {
            return operand.getLength() == 1;
        }

        protected boolean opLengthZero(RList vector, RAbstractVector operand) {
            return operand.getLength() == 0;
        }

        protected boolean opLengthZero(RAbstractContainer container, RAbstractVector operand) {
            return operand.getLength() == 0;
        }

        protected boolean opLengthZero(RNull vector, RAbstractVector operand) {
            return operand.getLength() == 0;
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
