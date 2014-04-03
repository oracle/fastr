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
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;
import com.oracle.truffle.r.nodes.access.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.access.ArrayPositionCastFactory.OperatorConverterNodeFactory;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "op", type = RNode.class), @NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "newValue", type = RNode.class),
                @NodeChild(value = "operand", type = OperatorConverterNode.class, executeWith = {"vector", "op", "newValue"})})
public abstract class ArrayPositionCast extends RNode {

    public abstract Object executeArg(VirtualFrame frame, Object op, Object vector, Object value, Object operand);

    abstract RNode getVector();

    abstract RNode getNewValue();

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

    protected static void verifyDimensions(RAbstractVector vector, int dimension, int numDimensions, boolean assignment, boolean isSubset, SourceSection sourceSection) {
        if ((vector.getDimensions() == null && (dimension != 0 || numDimensions > 1)) || (vector.getDimensions() != null && dimension >= vector.getDimensions().length)) {
            if (assignment) {
                if (isSubset) {
                    if (numDimensions == 2) {
                        throw RError.getIncorrectSubscriptsMatrix(sourceSection);
                    } else {
                        throw RError.getIncorrectSubscripts(sourceSection);
                    }
                } else {
                    throw RError.getImproperSubscript(sourceSection);
                }
            } else {
                throw RError.getIncorrectDimensions(sourceSection);
            }
        }
    }

    @Specialization(order = 1)
    public Object doMissingVector(Object op, RNull vector, Object value, Object operand) {
        return operand;
    }

    @Specialization(order = 2)
    public Object doFuncOp(Object op, RFunction vector, Object value, Object operand) {
        return operand;
    }

    @Specialization(order = 3)
    public RIntVector doMissingVector(Object op, RAbstractVector vector, Object value, RMissing operand) {
        verifyDimensions(vector, dimension, numDimensions, assignment, isSubset, getEncapsulatingSourceSection());
        int[] data = new int[numDimensions == 1 ? vector.getLength() : vector.getDimensions()[dimension]];
        for (int i = 0; i < data.length; i++) {
            data[i] = i + 1;
        }

        return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 4)
    public RNull doNullSubset(Object op, RAbstractVector vector, Object value, RNull operand) {
        // this is a special case - RNull can only appear to represent the x[[NA]] case which has to
        // return null and not a null vector
        return operand;
    }

    @Specialization(order = 5)
    public RStringVector doStringVector(Object op, RList vector, Object value, RStringVector operand) {
        // recursive access to the list
        return operand;
    }

    @Specialization(order = 14, guards = {"sizeOneVector", "numDimensionsOne", "operandHasNames", "isAssignment"})
    public int doIntVectorSizeOneOpNames(Object op, RAbstractVector vector, RNull value, RAbstractIntVector operand) {
        assert operand.getDataAt(0) != 0;
        return operand.getDataAt(0);
    }

    @Specialization(order = 15, guards = {"sizeOneVector", "numDimensionsOne", "operandHasNames", "isAssignment"})
    public RIntVector doIntVectorSizeOneOpNames(Object op, RAbstractVector vector, Object value, RAbstractIntVector operand) {
        assert operand.getDataAt(0) != 0;
        return operand.materialize();
    }

    @Specialization(order = 16, guards = {"sizeOneVector", "numDimensionsOne", "!operandHasNames"})
    public int doIntVectorSizeOne(Object op, RAbstractVector vector, Object value, RAbstractIntVector operand) {
        int val = operand.getDataAt(0);
        return val;
    }

    @Specialization(order = 20, guards = "emptyOperand")
    public int doIntVectorZero(Object op, RAbstractVector vector, Object value, RAbstractIntVector operand) {
        return 0;
    }

    @Specialization(order = 21)
    public RIntVector doIntVector(Object op, RAbstractVector vector, Object value, RAbstractIntVector operand) {
        return operand.materialize();
    }

    public static boolean sizeOneVector(Object op, RAbstractVector vector, Object value, RAbstractIntVector operand) {
        return operand.getLength() == 1;
    }

    protected boolean operandHasNames(Object op, RAbstractVector vector, Object value, RAbstractIntVector operand) {
        return operand.getNames() != RNull.instance;
    }

    protected boolean numDimensionsOne() {
        return numDimensions == 1;
    }

    protected boolean isAssignment() {
        return assignment;
    }

    protected boolean emptyOperand(Object op, RAbstractVector vector, Object value, RAbstractIntVector operand) {
        return operand.getLength() == 0;
    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class), @NodeChild(value = "newValue", type = RNode.class)})
    public abstract static class OperatorConverterNode extends RNode {

        public abstract Object executeConvert(VirtualFrame frame, Object vector, Object operand, Object value);

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
                operatorConvertRecursive = insert(OperatorConverterNodeFactory.create(this.dimension, this.numDimensions, this.assignment, this.isSubset, null, null, null));
            }
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractVector vector, int operand, Object value) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, vector, operand, value);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractVector vector, double operand, Object value) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, vector, operand, value);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractVector vector, byte operand, Object value) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, vector, operand, value);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractVector vector, Object operand, Object value) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, vector, operand, value);
        }

        private void initIntCast() {
            if (castInteger == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                castInteger = insert(CastIntegerNodeFactory.create(null, true, false));
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

        @Specialization(order = 1)
        public RMissing doFuncOp(RAbstractVector vector, RFunction operand, Object value) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "closure");
        }

        @Specialization(order = 2, guards = {"!isSubset", "emptyValue"})
        public RMissing doIntOutOfBoundsNegative(RAbstractVector vector, int operand, RAbstractVector value) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }

        @Specialization(order = 3, guards = {"!isSubset", "emptyValue"})
        public RMissing doIntOutOfBoundsNegative(RAbstractVector vector, double operand, RAbstractVector value) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }

        @Specialization(order = 4, guards = {"!isSubset", "emptyValue"})
        public RMissing doIntOutOfBoundsNegative(RAbstractVector vector, byte operand, RAbstractVector value) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }

        @Specialization(order = 5, guards = {"!isSubset", "emptyValue"})
        public RMissing doIntOutOfBoundsNegative(RAbstractVector vector, Object operand, RAbstractVector value) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }

        @Specialization(order = 6, guards = "dimLengthOne")
        public int doMissingDimLengthOne(RAbstractVector vector, RMissing operand, Object value) {
            if (!isSubset) {
                if (assignment) {
                    throw RError.getMissingSubscript(getEncapsulatingSourceSection());
                } else {
                    throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "symbol");
                }
            }
            return 1;
        }

        @Specialization(order = 7, guards = "!dimLengthOne")
        public RMissing doMissing(RAbstractVector vector, RMissing operand, Object value) {
            if (!isSubset) {
                if (assignment) {
                    throw RError.getMissingSubscript(getEncapsulatingSourceSection());
                } else {
                    throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "symbol");
                }
            }
            return operand;
        }

        @Specialization(order = 8)
        public int doNull(RAbstractVector vector, RNull operand, Object value) {
            if (isSubset) {
                return 0;
            } else {
                throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
            }
        }

        @Specialization(order = 9, guards = {"indNA", "isSubset"})
        public int doIntNASubset(RAbstractVector vector, int operand, Object value) {
            return operand;
        }

        @Specialization(order = 10, guards = {"indNA", "!isSubset"})
        public RNull doIntNA(RList vector, int operand, Object value) {
            return RNull.instance;
        }

        @Specialization(order = 11, guards = {"indNA", "!isSubset"})
        public int doIntNA(RAbstractVector vector, int operand, Object value) {
            if (!assignment) {
                throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
            } else {
                // let assignment handle it as it depends on the value
                return RRuntime.INT_NA;
            }
        }

        @Specialization(order = 12, guards = {"outOfBounds", "numDimensionsOne"})
        public int doIntOutOfBoundsOneDim(RAbstractVector vector, int operand, Object value) {
            if (assignment) {
                return operand;
            } else {
                if (isSubset) {
                    return RRuntime.INT_NA;
                } else {
                    throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
                }
            }
        }

        @Specialization(order = 13, guards = {"outOfBounds", "!numDimensionsOne"})
        public int doIntOutOfBounds(RAbstractVector vector, int operand, Object value) {
            throw RError.getSubscriptBounds(assignment ? getEncapsulatingSourceSection() : null);
        }

        @Specialization(order = 14, guards = {"outOfBoundsNegative", "dimLengthOne", "isSubset"})
        public int doIntOutOfBoundsNegativeOneElementSubset(RAbstractVector vector, int operand, Object value) {
            // there is only one element to be picked
            return 1;
        }

        @Specialization(order = 15, guards = {"outOfBoundsNegative", "dimLengthOne", "!isSubset"})
        public int doIntOutOfBoundsNegativeOneElement(RAbstractVector vector, int operand, Object value) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }

        @Specialization(order = 16, guards = {"outOfBoundsNegative", "!dimLengthOne", "isSubset"})
        public RMissing doIntOutOfBoundsNegativeIsSubset(RAbstractVector vector, int operand, Object value) {
            // all indexes - result is the same as with missing index
            return RMissing.instance;
        }

        @Specialization(order = 17, guards = {"outOfBoundsNegative", "!dimLengthOne", "!isSubset"})
        public RMissing doIntOutOfBoundsNegative(RAbstractVector vector, int operand, Object value) {
            throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
        }

        @Specialization(order = 18, guards = {"!outOfBounds", "!isNegative"})
        public int doInt(RAbstractVector vector, int operand, Object value) {
            return operand;
        }

        @Specialization(order = 19, guards = {"isNegative", "dimLengthOne"})
        public int doIntNegativeNoDimLeft(RAbstractVector vector, int operand, Object value) {
            // it's negative, but not out of bounds and dimension has length one - result is no
            // dimensions left
            return 0;
        }

        @Specialization(order = 20, guards = "isNegative")
        public RIntVector doIntNegative(RAbstractVector vector, int operand, Object value) {
            // it's negative, but not out of bounds - pick all indexes apart from the negative one
            int dimLength = numDimensions == 1 ? vector.getLength() : vector.getDimensions()[dimension];
            if (!isSubset && (dimLength - 1) > 1) {
                throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
            }
            int[] positions = new int[dimLength - 1];
            int ind = 0;
            for (int i = 1; i <= dimLength; i++) {
                if (i != -operand) {
                    positions[ind++] = i;
                }
            }
            return RDataFactory.createIntVector(positions, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(order = 21, guards = "!isNegative")
        public Object doDouble(VirtualFrame frame, RAbstractVector vector, double operand, Object value) {
            return convertOperatorRecursive(frame, vector, castInteger(frame, operand), value);
        }

        @Specialization(order = 22, guards = "isNegative")
        public Object doDoubleNegative(VirtualFrame frame, RAbstractVector vector, double operand, Object value) {
            // returns object as it may return either int or RIntVector due to conversion
            return convertOperatorRecursive(frame, vector, castInteger(frame, operand), value);
        }

        @Specialization(order = 25, guards = {"indNA", "!isSubset"})
        public RNull doLogicalDimLengthOne(RList vector, byte operand, Object value) {
            return RNull.instance;
        }

        @Specialization(order = 26, guards = {"indNA", "!isSubset"})
        public int doLogicalDimLengthOne(RAbstractVector vector, byte operand, Object value) {
            if (!assignment) {
                throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
            } else {
                // let assignment handle it as it depends on the value
                return RRuntime.INT_NA;
            }
        }

        @Specialization(order = 27, guards = {"dimLengthOne", "indNA", "isSubset"})
        public int doLogicalDimLengthOneSubset(VirtualFrame frame, RAbstractVector vector, byte operand, Object value) {
            return (int) castInteger(frame, operand);
        }

        @Specialization(order = 28, guards = {"!dimLengthOne", "indNA", "isSubset", "isAssignment"})
        public int doLogicalNASubsetAssignment(RAbstractVector vector, byte operand, Object value) {
            return RRuntime.INT_NA;
        }

        @Specialization(order = 29, guards = {"!dimLengthOne", "indNA", "isSubset", "!isAssignment"})
        public RIntVector doLogicalNASubset(RAbstractVector vector, byte operand, Object value) {
            int dimLength = numDimensions == 1 ? (vector.getLength() == 0 ? 1 : vector.getLength()) : vector.getDimensions()[dimension];
            int[] data = new int[dimLength];
            Arrays.fill(data, RRuntime.INT_NA);
            return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
        }

        @Specialization(order = 30, guards = {"isSubset", "indTrue"})
        public RIntVector doLogicalIndTrue(RAbstractVector vector, byte operand, Object value) {
            int dimLength = numDimensions == 1 ? vector.getLength() : vector.getDimensions()[dimension];
            int[] data = new int[dimLength];
            for (int i = 0; i < dimLength; i++) {
                data[i] = i + 1;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(order = 31)
        public int doLogical(VirtualFrame frame, RAbstractVector vector, byte operand, Object value) {
            return (int) castInteger(frame, operand);
        }

        protected boolean valLenZero(RAbstractVector vector, Object operand, RAbstractVector value) {
            return value.getLength() == 0;
        }

        protected boolean valLenOne(RAbstractVector vector, Object operand, RAbstractVector value) {
            return value.getLength() == 1;
        }

        @Specialization(order = 32, guards = {"!isSubset", "valLenZero"})
        public int doComplexValLengthZero(RAbstractVector vector, RComplex operand, RAbstractVector value) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }

        @Specialization(order = 33, guards = {"!isSubset", "!valLenZero", "!valLenOne"})
        public int doComplexVal(RAbstractVector vector, RComplex operand, RAbstractVector value) {
            throw RError.getMoreElementsSupplied(getEncapsulatingSourceSection());
        }

        @Specialization(order = 34)
        public int doComplexValLengthOne(RAbstractVector vector, RComplex operand, Object value) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "complex");
        }

        @Specialization(order = 35, guards = {"!isSubset", "valLenZero"})
        public int doComplexValLengthZero(RAbstractVector vector, RRaw operand, RAbstractVector value) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }

        @Specialization(order = 36, guards = {"!isSubset", "!valLenZero", "!valLenOne"})
        public int doComplexVal(RAbstractVector vector, RRaw operand, RAbstractVector value) {
            throw RError.getMoreElementsSupplied(getEncapsulatingSourceSection());
        }

        @Specialization(order = 37)
        public int doRaw(RAbstractVector vector, RRaw operand, Object value) {
            throw RError.getInvalidSubscriptType(isSubset ? null : getEncapsulatingSourceSection(), "raw");
        }

        private int findPosition(RAbstractVector vector, RStringVector names, String operand) {
            for (int j = 0; j < names.getLength(); j++) {
                if (operand.equals(names.getDataAt(j))) {
                    return j + 1;
                }
            }
            if (numDimensions == 1) {
                return RRuntime.INT_NA;
            } else {
                throw RError.getSubscriptBounds(isSubset ? null : getEncapsulatingSourceSection());
            }
        }

        private static RIntVector findPositionWithNames(RAbstractVector vector, RStringVector names, String operand) {
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
                    position = vector.getLength() + 1;
                }
            } else {
                position = vector.getLength() + 1;
            }
            return RDataFactory.createIntVector(new int[]{position}, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization(order = 40, guards = {"indNA", "!numDimensionsOne"})
        public Object doStringNA(RAbstractVector vector, String operand, Object value) {
            throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
        }

        @Specialization(order = 41, guards = {"indNA", "numDimensionsOne"})
        public Object doStringNANumDimsOne(VirtualFrame frame, RAbstractVector vector, String operand, Object value) {
            return convertOperatorRecursive(frame, vector, RRuntime.INT_NA, value);
        }

        @Specialization(order = 42, guards = {"hasNames", "isAssignment", "numDimensionsOne"})
        public RIntVector doStringOneDimNamesAssignment(RAbstractVector vector, String operand, Object value) {
            RStringVector names = (RStringVector) vector.getNames();
            return findPositionWithNames(vector, names, operand);
        }

        @Specialization(order = 43, guards = {"isSubset", "hasNames", "!isAssignment", "numDimensionsOne"})
        public Object doStringOneDimNamesSubset(RList vector, String operand, Object value) {
            RStringVector names = (RStringVector) vector.getNames();
            return findPosition(vector, names, operand);
        }

        @Specialization(order = 44, guards = {"!isSubset", "hasNames", "!isAssignment", "numDimensionsOne"})
        public Object doStringOneDimNames(RList vector, String operand, Object value) {
            // we need to return either an int or null - is there a prettier way to handle this?
            RStringVector names = (RStringVector) vector.getNames();
            int result = findPosition(vector, names, operand);
            if (RRuntime.isNA(result)) {
                return RNull.instance;
            } else {
                return result;
            }
        }

        @Specialization(order = 45, guards = {"hasNames", "!isAssignment", "numDimensionsOne"})
        public int doStringOneDimNames(RAbstractVector vector, String operand, Object value) {
            RStringVector names = (RStringVector) vector.getNames();
            return findPosition(vector, names, operand);
        }

        @Specialization(order = 46, guards = {"!hasNames", "isAssignment", "numDimensionsOne"})
        public RIntVector doStringOneDimAssignment(RAbstractVector vector, String operand, Object value) {
            return findPositionWithNames(vector, null, operand);
        }

        @Specialization(order = 47, guards = {"isAssignment", "numDimensionsOne"})
        public RIntVector doStringOneDimAssignment(RNull vector, String operand, Object value) {
            RStringVector resNames = RDataFactory.createStringVector(new String[]{operand}, !RRuntime.isNA(operand));
            return RDataFactory.createIntVector(new int[]{1}, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization(order = 48, guards = {"hasDimNames", "!numDimensionsOne"})
        public int doString(RAbstractVector vector, String operand, Object value) {
            RList dimNames = vector.getDimNames();
            RStringVector names = (RStringVector) dimNames.getDataAt(dimension);
            return findPosition(vector, names, operand);
        }

        @Specialization(order = 49, guards = "isSubset")
        public int doStringNoNamesSubset(RList vector, String operand, Object value) {
            if (numDimensions == 1) {
                return RRuntime.INT_NA;
            } else {
                throw RError.getNoArrayDimnames(null);
            }
        }

        @Specialization(order = 50, guards = "!isSubset")
        public RNull doStringNoNames(RList vector, String operand, Object value) {
            if (numDimensions == 1) {
                return RNull.instance;
            } else {
                throw RError.getNoArrayDimnames(null);
            }
        }

        @Specialization(order = 51)
        public int doStringNoNames(RAbstractVector vector, String operand, Object value) {
            if (isSubset) {
                if (numDimensions == 1) {
                    return RRuntime.INT_NA;
                } else {
                    throw RError.getNoArrayDimnames(null);
                }
            } else {
                throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
            }
        }

        @Specialization(order = 52, guards = {"!isSubset", "opLengthZero", "valLenZero"})
        public int doListValLengthZeroOpLengthZero(RAbstractVector vector, RList operand, RAbstractVector value) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }

        @Specialization(order = 53, guards = {"!isSubset", "opLengthOne", "valLenZero"})
        public int doListValLengthZero(RAbstractVector vector, RList operand, RAbstractVector value) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }

        @Specialization(order = 54, guards = {"!isSubset", "opLengthZero", "valLenOne"})
        public int doListValLengthOneOpLengthZero(RAbstractVector vector, RList operand, RAbstractVector value) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }

        @Specialization(order = 55, guards = {"!isSubset", "opLengthZero", "!valLenZero", "!valLenOne"})
        public int doListOpLengthZero(RAbstractVector vector, RList operand, RAbstractVector value) {
            throw RError.getMoreElementsSupplied(getEncapsulatingSourceSection());
        }

        @Specialization(order = 56, guards = {"!isSubset", "opLengthOne", "!valLenZero", "!valLenOne"})
        public int doList(RAbstractVector vector, RList operand, RAbstractVector value) {
            throw RError.getMoreElementsSupplied(getEncapsulatingSourceSection());
        }

        @Specialization(order = 57, guards = {"!isSubset", "opLengthZero"})
        public RIntVector doListOutOpLengthZero(RAbstractVector vector, RList operand, Object value) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }

        @Specialization(order = 58, guards = {"!isSubset", "!opLengthTwo", "!opLengthOne", "!opLengthZero", "isAssignment"})
        public RIntVector doListOutOfBoundsTooManySelectedAssignment(RAbstractVector vector, RList operand, Object value) {
            throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
        }

        @Specialization(order = 59, guards = {"!isSubset", "!opLengthOne", "!opLengthZero", "!isAssignment"})
        public RIntVector doListOutOfBoundsTooManySelected(RAbstractVector vector, RList operand, Object value) {
            throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
        }

        @Specialization(order = 60)
        public int doListOpLengthOne(RAbstractVector vector, RList operand, Object value) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "list");
        }

        @Specialization(order = 65, guards = {"!isSubset", "opLengthZero"})
        public RAbstractIntVector doIntVectorOp(RList vector, RAbstractVector operand, Object value) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }

        @Specialization(order = 66, guards = {"!isSubset", "!opLengthZero", "!opLengthOne"})
        public RAbstractIntVector doIntVectorOp(RList vector, RAbstractIntVector operand, Object value) {
            return operand;
        }

        @Specialization(order = 67, guards = {"!isSubset", "!opLengthZero", "!opLengthOne"})
        public RAbstractIntVector doIntVectorOp(VirtualFrame frame, RList vector, RAbstractDoubleVector operand, Object value) {
            return (RIntVector) castInteger(frame, operand);
        }

        @Specialization(order = 68, guards = {"!isSubset", "!opLengthZero", "!opLengthOne"})
        public RAbstractIntVector doIntVectorOp(VirtualFrame frame, RList vector, RAbstractLogicalVector operand, Object value) {
            return (RIntVector) castInteger(frame, operand);
        }

        @Specialization(order = 70, guards = {"!isSubset", "opLengthZero"})
        public int doIntEmptyOp(VirtualFrame frame, RAbstractVector vector, RAbstractVector operand, Object value) {
            return 0;
        }

        @Specialization(order = 100, guards = "opLengthOne")
        public Object doIntVectorOpLengthOne(VirtualFrame frame, RAbstractVector vector, RAbstractIntVector operand, Object value) {
            return convertOperatorRecursive(frame, vector, operand.getDataAt(0), value);
        }

        @Specialization(order = 101, guards = {"isSubset", "!opLengthOne", "!opLengthZero"})
        public RAbstractIntVector doIntVectorOp(RAbstractVector vector, RAbstractIntVector operand, Object value) {
            return transformIntoPositive(vector, operand);
        }

        @Specialization(order = 102, guards = {"!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doIntVectorTooManySelected(RAbstractVector vector, RAbstractIntVector operand, Object value) {
            if (operand.getLength() == 2 && operand.getDataAt(0) == 0.0) {
                throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
            } else {
                throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
            }
        }

        @Specialization(order = 103, guards = {"isSubset", "opLengthZero"})
        public int doIntVectorFewManySelected(RAbstractVector vector, RAbstractIntVector operand, Object value) {
            return 0;
        }

        @Specialization(order = 120, guards = "opLengthOne")
        public Object doDoubleVectorOpLengthOne(VirtualFrame frame, RAbstractVector vector, RAbstractDoubleVector operand, Object value) {
            return convertOperatorRecursive(frame, vector, operand.getDataAt(0), value);
        }

        @Specialization(order = 121, guards = {"isSubset", "!opLengthOne", "!opLengthZero"})
        public RAbstractIntVector doDoubleVector(VirtualFrame frame, RAbstractVector vector, RAbstractDoubleVector operand, Object value) {
            RIntVector resultVector = (RIntVector) castInteger(frame, operand);
            return transformIntoPositive(vector, resultVector);
        }

        @Specialization(order = 122, guards = {"!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doDoubleVectorTooManySelected(RAbstractVector vector, RAbstractDoubleVector operand, Object value) {
            if (operand.getLength() == 2 && operand.getDataAt(0) == 0.0) {
                throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
            } else {
                throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
            }
        }

        @Specialization(order = 123, guards = {"isSubset", "opLengthZero"})
        public int doDoubleVectorTooFewSelected(RAbstractVector vector, RAbstractDoubleVector operand, Object value) {
            return 0;
        }

        @Specialization(order = 135, guards = "opLengthOne")
        public Object doLogicalVectorOpLengthOne(VirtualFrame frame, RAbstractVector vector, RAbstractLogicalVector operand, Object value) {
            return convertOperatorRecursive(frame, vector, operand.getDataAt(0), value);
        }

        @Specialization(order = 136, guards = {"outOfBounds", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doLogicalVectorOutOfBounds(RAbstractVector vector, RAbstractLogicalVector operand, Object value) {
            throw RError.getLogicalSubscriptLong(isSubset ? null : getEncapsulatingSourceSection());
        }

        @Specialization(order = 137, guards = {"outOfBounds", "!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doLogicalVectorOutOfBoundsTooManySelected(RAbstractVector vector, RAbstractLogicalVector operand, Object value) {
            throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
        }

        private static int[] eliminateZeros(RAbstractVector vector, int[] positions, int zeroCount) {
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
        public RIntVector doLogicalVector(RAbstractVector vector, RAbstractLogicalVector operand, Object value) {
            int resultLength = numDimensions == 1 ? Math.max(operand.getLength(), vector.getLength()) : vector.getDimensions()[dimension];
            int logicalVectorLength = operand.getLength();
            int logicalVectorInd = 0;
            int[] data = new int[resultLength];
            naCheck.enable(!operand.isComplete());
            int timesSeenFalse = 0;
            int i = 0;
            for (; i < resultLength; i++, logicalVectorInd = Utils.incMod(logicalVectorInd, logicalVectorLength)) {
                byte b = operand.getDataAt(logicalVectorInd);
                if (naCheck.check(b)) {
                    data[i] = RRuntime.INT_NA;
                } else if (b == RRuntime.LOGICAL_TRUE) {
                    if (i >= vector.getLength()) {
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
                return RDataFactory.createIntVector(eliminateZeros(vector, data, timesSeenFalse), naCheck.neverSeenNA() && i < vector.getLength());
            } else {
                return RDataFactory.createIntVector(data, naCheck.neverSeenNA() && i < vector.getLength());
            }
        }

        @Specialization(order = 139, guards = {"!outOfBounds", "!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doLogicalVectorTooManySelected(RAbstractVector vector, RAbstractLogicalVector operand, Object value) {
            if (operand.getLength() == 2 && operand.getDataAt(0) == RRuntime.LOGICAL_FALSE) {
                throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
            } else {
                throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
            }
        }

        @Specialization(order = 140, guards = {"isSubset", "opLengthZero"})
        public int doDoubleVectorTooFewSelected(RAbstractVector vector, RAbstractLogicalVector operand, Object value) {
            return 0;
        }

        @Specialization(order = 150, guards = "opLengthOne")
        public Object doComplexVectorOpLengthOne(VirtualFrame frame, RAbstractVector vector, RAbstractComplexVector operand, Object value) {
            return convertOperatorRecursive(frame, vector, operand.getDataAt(0), value);
        }

        @Specialization(order = 151, guards = {"isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doComplexVectorSubset(RAbstractVector vector, RAbstractComplexVector operand, Object value) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "complex");
        }

        @Specialization(order = 152, guards = {"!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doComplexVector(RAbstractVector vector, RAbstractComplexVector operand, Object value) {
            if (operand.getLength() == 2) {
                throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "complex");
            } else {
                throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
            }
        }

        @Specialization(order = 153, guards = {"isSubset", "opLengthZero"})
        public RIntVector doComplexVectoTooFewSelectedSubset(RAbstractVector vector, RAbstractComplexVector operand, Object value) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "complex");
        }

        @Specialization(order = 160, guards = "opLengthOne")
        public Object doRawVectorOpLengthOne(VirtualFrame frame, RAbstractVector vector, RAbstractRawVector operand, Object value) {
            return convertOperatorRecursive(frame, vector, operand.getDataAt(0), value);
        }

        @Specialization(order = 161, guards = {"isSubset", "!opLengthOne", "!opLengthZero"})
        public RAbstractIntVector doRawVectorSubset(RAbstractVector vector, RAbstractRawVector operand, Object value) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "raw");
        }

        @Specialization(order = 162, guards = {"!isSubset", "!opLengthOne", "!opLengthZero"})
        public RAbstractIntVector doRawVector(RAbstractVector vector, RAbstractRawVector operand, Object value) {
            if (operand.getLength() == 2) {
                throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "raw");
            } else {
                throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
            }
        }

        @Specialization(order = 163, guards = {"isSubset", "opLengthZero"})
        public RIntVector doRawVectorTooFewSelectedSubset(RAbstractVector vector, RAbstractRawVector operand, Object value) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "raw");
        }

        private RIntVector findPositions(RAbstractVector vector, RStringVector names, RAbstractStringVector operand, boolean retainNames) {
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
                        throw RError.getSubscriptBounds(null);
                    }
                }
            }
            return RDataFactory.createIntVector(data, !seenNA);
        }

        private static int eliminateDuplicate(RAbstractStringVector operand, int[] data, int initialPos, int currentElementPos) {
            int position = initialPos;
            String name = operand.getDataAt(currentElementPos);
            if (name == RRuntime.STRING_NA) {
                // duplicate NAs are not eliminated
                data[currentElementPos] = (position++) + 1;
            } else {
                int j = 0;
                for (; j < currentElementPos; j++) {
                    String prevName = operand.getDataAt(j);
                    if (name.equals(prevName)) {
                        data[currentElementPos] = j + 1;
                        break;
                    }
                }
                if (j == currentElementPos) {
                    data[currentElementPos] = (position++) + 1;
                }
            }
            return position;
        }

        private static RIntVector findPositionsWithNames(RAbstractVector vector, RStringVector names, RAbstractStringVector operand, boolean retainNames) {
            RStringVector resNames = operand.materialize();
            int initialPos = vector.getLength();
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
                    data[i] = initialPos++;
                }
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR, resNames);
        }

        @Specialization(order = 170, guards = "opLengthOne")
        public Object doStringlVectorOpLengthOne(VirtualFrame frame, RAbstractVector vector, RAbstractStringVector operand, Object value) {
            return convertOperatorRecursive(frame, vector, operand.getDataAt(0), value);
        }

        @Specialization(order = 171, guards = {"hasNames", "isAssignment", "numDimensionsOne", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorOneDimNamesAssignment(RAbstractVector vector, RAbstractStringVector operand, Object value) {
            RStringVector names = (RStringVector) vector.getNames();
            return findPositionsWithNames(vector, names, operand, assignment);
        }

        @Specialization(order = 172, guards = {"hasNames", "!isAssignment", "numDimensionsOne", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorOneDimNames(RAbstractVector vector, RAbstractStringVector operand, Object value) {
            RStringVector names = (RStringVector) vector.getNames();
            return findPositions(vector, names, operand, assignment);
        }

        @Specialization(order = 173, guards = {"!hasNames", "isAssignment", "numDimensionsOne", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorOneDimAssignment(RAbstractVector vector, RAbstractStringVector operand, Object value) {
            return findPositionsWithNames(vector, null, operand, assignment);
        }

        @Specialization(order = 174, guards = {"isAssignment", "numDimensionsOne", "isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorOneDimAssignment(VirtualFrame frame, RNull vector, RAbstractStringVector operand, Object value) {
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
        public RIntVector doStringVector(RAbstractVector vector, RAbstractStringVector operand, Object value) {
            RList dimNames = vector.getDimNames();
            RStringVector names = (RStringVector) dimNames.getDataAt(dimension);
            return findPositions(vector, names, operand, false);
        }

        @Specialization(order = 176, guards = {"!isSubset", "!opLengthOne", "!opLengthZero", "numDimensionsOne"})
        public RAbstractStringVector doStringVectorTooManySelected(RList vector, RAbstractStringVector operand, Object value) {
            // for recursive access
            return operand;
        }

        @Specialization(order = 177, guards = {"!isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorTooManySelected(RAbstractVector vector, RAbstractStringVector operand, Object value) {
            throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
        }

        @Specialization(order = 178, guards = {"isSubset", "!opLengthOne", "!opLengthZero"})
        public RIntVector doStringVectorNoDimNames(RAbstractVector vector, RAbstractStringVector operand, Object value) {
            if (numDimensions == 1) {
                int[] data = new int[operand.getLength()];
                Arrays.fill(data, RRuntime.INT_NA);
                return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
            } else {
                throw RError.getSubscriptBounds(null);
            }
        }

        @Specialization(order = 179, guards = "opLengthZero")
        public int doStringVectorTooFewSelected(RAbstractVector vector, RAbstractStringVector operand, Object value) {
            return 0;
        }

        @Specialization(order = 200, guards = {"numDimensionsOne", "operandHasNames", "!opLengthOne", "!opLengthZero"})
        public RAbstractIntVector doMissingVector(RNull vector, RAbstractIntVector operand, Object value) {
            RIntVector resPositions = (RIntVector) operand.copy();
            resPositions.setNames(null);
            return resPositions;
        }

        @Specialization(order = 201)
        public Object doMissingVector(VirtualFrame frame, RNull vector, Object operand, Object value) {
            return castInteger(frame, operand);
        }

        @Specialization(order = 300)
        public Object doFuncOp(RFunction vector, Object operand, Object value) {
            return operand;
        }

        private final NACheck positionNACheck = NACheck.create();
        @CompilationFinal private boolean hasSeenPositive;
        @CompilationFinal private boolean hasSeenZero;
        @CompilationFinal private boolean hasSeenNegative;
        @CompilationFinal private boolean hasSeenNA;

        private int[] eliminateZeros(RAbstractVector vector, RAbstractIntVector positions, int zeroCount) {
            int positionsLength = positions.getLength();
            int[] data = new int[positionsLength - zeroCount];
            int ind = 0;
            int i = 0;
            for (; i < positionsLength; i++) {
                int pos = positions.getDataAt(i);
                if (pos > vector.getLength()) {
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

        private RAbstractIntVector transformIntoPositive(RAbstractVector vector, RAbstractIntVector positions) {
            int zeroCount = 0;
            positionNACheck.enable(positions);
            int positionsLength = positions.getLength();
            int dimLength = numDimensions == 1 ? vector.getLength() : vector.getDimensions()[dimension];
            boolean outOfBounds = false;
            for (int i = 0; i < positionsLength; ++i) {
                int pos = positions.getDataAt(i);
                if (positionNACheck.check(pos)) {
                    if (!hasSeenNA) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenNA = true;
                    }
                } else if (pos > 0) {
                    if (numDimensions != 1 && pos > dimLength) {
                        throw RError.getSubscriptBounds(null);
                    }
                    if (numDimensions == 1 && pos > vector.getLength()) {
                        if (isSubset) {
                            outOfBounds = true;
                        } else {
                            throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
                        }
                    }
                    if (!hasSeenPositive) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenPositive = true;
                    }
                } else if (pos == 0) {
                    if (!isSubset) {
                        RError.getSelectLessThanOne(getEncapsulatingSourceSection());
                    }
                    if (!hasSeenZero) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenZero = true;
                    }
                    zeroCount++;
                } else {
                    if (!hasSeenNegative) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenNegative = true;
                    }
                }
            }
            if (hasSeenPositive || hasSeenNA) {
                if (hasSeenNegative) {
                    throw RError.getOnlyZeroMixed(getEncapsulatingSourceSection());
                } else if (hasSeenZero || (outOfBounds && numDimensions == 1)) {
                    // eliminate 0-s and handle out-of-bounds for single-subscript accesses
                    int[] data = eliminateZeros(vector, positions, zeroCount);
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
                    throw RError.getOnlyZeroMixed(getEncapsulatingSourceSection());
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

        private int getDimensionSize(RAbstractVector vector) {
            verifyDimensions(vector, dimension, numDimensions, assignment, isSubset, getEncapsulatingSourceSection());
            return numDimensions == 1 ? vector.getLength() : vector.getDimensions()[dimension];
        }

        protected boolean dimLengthOne(RAbstractVector vector, byte operand) {
            return getDimensionSize(vector) == 1;
        }

        protected boolean dimLengthOne(RAbstractVector vector, int operand) {
            return getDimensionSize(vector) == 1;
        }

        protected boolean dimLengthOne(RAbstractVector vector, RMissing operand) {
            return getDimensionSize(vector) == 1;
        }

        protected boolean dimLengthOne(RAbstractVector vector, RAbstractVector operand) {
            return getDimensionSize(vector) == 1;
        }

        protected static boolean indNA(RAbstractVector vector, int operand) {
            return RRuntime.isNA(operand);
        }

        protected static boolean indNA(RAbstractVector vector, byte operand) {
            return RRuntime.isNA(operand);
        }

        protected static boolean indNA(RAbstractVector vector, String operand) {
            return RRuntime.isNA(operand);
        }

        protected static boolean indTrue(RAbstractVector vector, byte operand) {
            return operand == RRuntime.LOGICAL_TRUE;
        }

        protected boolean outOfBoundsNegative(RAbstractVector vector, int operand) {
            return operand < 0 && -operand > getDimensionSize(vector);
        }

        protected boolean outOfBounds(RAbstractVector vector, RAbstractLogicalVector operand) {
            // out of bounds if multi dimensional access with a logical vector that's too long (in
            // single-dimensional case it's OK)
            return (operand.getLength() > getDimensionSize(vector)) && numDimensions != 1;
        }

        protected boolean outOfBounds(RAbstractVector vector, int operand) {
            return operand > getDimensionSize(vector);
        }

        protected boolean numDimensionsOne() {
            return numDimensions == 1;
        }

        protected boolean isNegative(RAbstractVector vector, int operand) {
            return operand < 0;
        }

        protected boolean isNegative(RAbstractVector vector, double operand) {
            return operand < 0;
        }

        protected boolean hasDimNames(RAbstractVector vector, String operand) {
            return vector.getDimNames() != null;
        }

        protected boolean hasNames(RAbstractVector vector, String operand) {
            return vector.getNames() != RNull.instance;
        }

        protected boolean hasDimNames(RAbstractVector vector, RAbstractStringVector operand) {
            return vector.getDimNames() != null;
        }

        protected boolean hasNames(RAbstractVector vector, RAbstractStringVector operand) {
            return vector.getNames() != RNull.instance;
        }

        protected boolean opLengthTwo(RAbstractVector vector, RList operand, Object value) {
            return operand.getLength() == 2;
        }

        protected boolean opLengthOne(RAbstractVector vector, RAbstractVector operand) {
            return operand.getLength() == 1;
        }

        protected boolean opLengthOne(RNull vector, RAbstractVector operand) {
            return operand.getLength() == 1;
        }

        protected boolean opLengthZero(RList vector, RAbstractVector operand, Object value) {
            return operand.getLength() == 0;
        }

        protected boolean opLengthZero(RAbstractVector vector, RList operand, RAbstractVector value) {
            return operand.getLength() == 0;
        }

        protected boolean opLengthZero(RAbstractVector vector, RAbstractVector operand, Object value) {
            return operand.getLength() == 0;
        }

        protected boolean opLengthZero(RNull vector, RAbstractVector operand, Object value) {
            return operand.getLength() == 0;
        }

        protected boolean isSubset() {
            return isSubset;
        }

        protected boolean emptyValue(RAbstractVector vector, int operand, RAbstractVector value) {
            return value.getLength() == 0;
        }

        protected boolean emptyValue(RAbstractVector vector, double operand, RAbstractVector value) {
            return value.getLength() == 0;
        }

        protected boolean emptyValue(RAbstractVector vector, byte operand, RAbstractVector value) {
            return value.getLength() == 0;
        }

        protected boolean emptyValue(RAbstractVector vector, Object operand, RAbstractVector value) {
            return value.getLength() == 0;
        }

        protected boolean isAssignment() {
            return assignment;
        }

        protected boolean operandHasNames(RNull vector, RAbstractIntVector operand, Object value) {
            return operand.getNames() != RNull.instance;
        }

    }
}
