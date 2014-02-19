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
import com.oracle.truffle.r.nodes.access.ArrayPositionCastFactory.OperatorConverterNodeFactory;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
public abstract class ArrayPositionCast extends RNode {

    abstract int executeArg(VirtualFrame frame, Object o, RAbstractVector vector);

    abstract RNode getVector();

    private final int dimension;

    protected ArrayPositionCast(int dimension) {
        this.dimension = dimension;
    }

    protected ArrayPositionCast(ArrayPositionCast other) {
        this.dimension = other.dimension;
    }

    @CreateCast({"operand"})
    public RNode createCast(RNode child) {
        return OperatorConverterNodeFactory.create(dimension, getVector(), child);
    }

    @Specialization
    public RMissing doMissing(RAbstractVector vector, RMissing operand) {
        return operand;
    }

    @Specialization
    public int doNull(RAbstractVector vector, RNull operand) {
        return 0;
    }

    @Specialization
    public int doInt(RAbstractVector vector, int operand) {
        return operand;
    }

    @Specialization(guards = "sizeOneVector")
    public int doIntVectorSizeOne(RAbstractVector vector, RIntVector operand) {
        return operand.getDataAt(0);
    }

    @Specialization(guards = "!sizeOneVector")
    public RIntVector doIntVector(RAbstractVector vector, RIntVector operand) {
        return operand;
    }

    @Specialization
    public int doStringVector(VirtualFrame frame, RAbstractVector vector, RStringVector operand) {
        throw Utils.nyi(); // TODO
    }

    public static boolean sizeOneVector(RAbstractVector vector, RIntVector operand) {
        return operand.getLength() == 1;
    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
    abstract static class OperatorConverterNode extends RNode {

        private final int dimension;
        @Child private OperatorConverterNode operatorConvertRecursive;
        @Child private CastIntegerNode castInteger;
        private final NACheck naCheck = NACheck.create();

        protected OperatorConverterNode(int dimension) {
            this.dimension = dimension;
        }

        protected OperatorConverterNode(OperatorConverterNode other) {
            this.dimension = other.dimension;
        }

        public abstract Object executeConvert(VirtualFrame frame, RAbstractVector vector, Object operand);

        public abstract Object executeConvert(VirtualFrame frame, RAbstractVector vector, double operand);

        private void initConvertCast() {
            if (operatorConvertRecursive == null) {
                CompilerDirectives.transferToInterpreter();
                operatorConvertRecursive = adoptChild(OperatorConverterNodeFactory.create(this.dimension, null, null));
            }
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractVector vector, double operand) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, vector, operand);
        }

        private Object convertOperatorRecursive(VirtualFrame frame, RAbstractVector vector, Object operand) {
            initConvertCast();
            return operatorConvertRecursive.executeConvert(frame, vector, operand);
        }

        private void initIntCast() {
            if (castInteger == null) {
                CompilerDirectives.transferToInterpreter();
                castInteger = adoptChild(CastIntegerNodeFactory.create(null, true, false));
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

        @Specialization(order = 1, guards = "dimLengthOne")
        public int doMissingDimLengthOne(RAbstractVector vector, RMissing operand) {
            return 1;
        }

        @Specialization(order = 2, guards = "!dimLengthOne")
        public RMissing doMissing(RAbstractVector vector, RMissing operand) {
            return operand;
        }

        @Specialization
        public RNull doNull(RAbstractVector vector, RNull operand) {
            return operand;
        }

        @Specialization(order = 3, guards = "indNA")
        public int doIntNA(RAbstractVector vector, int operand) {
            return operand;
        }

        @Specialization(order = 4, guards = "outOfBounds")
        public int doIntOutOfBounds(RAbstractVector vector, int operand) {
            throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
        }

        @Specialization(order = 5, guards = "outOfBoundsNegative")
        public RMissing doIntOutOfBoundsNegative(RAbstractVector vector, int operand) {
            // all indexes - result is the same as with missing index
            return RMissing.instance;
        }

        @Specialization(order = 6, guards = {"!outOfBounds", "!isNegative"})
        public int doInt(RAbstractVector vector, int operand) {
            return operand;
        }

        @Specialization(order = 7, guards = "isNegative")
        public RIntVector doIntNegative(RAbstractVector vector, int operand) {
            // it's negative, but not out of bounds - pick all indexes apart from the negative one
            int dimLength = vector.getDimensions()[dimension];
            int[] positions = new int[dimLength - 1];
            int ind = 0;
            for (int i = 1; i <= dimLength; i++) {
                if (i != -operand) {
                    positions[ind++] = i;
                }
            }
            return RDataFactory.createIntVector(positions, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(order = 8, guards = {"isNegative", "dimLengthOne"})
        public int doIntNegativeNoDimLeft(RAbstractVector vector, int operand) {
            // it's negative, but not out of bounds and dimension has length one - result is no
            // dimensions left
            return 0;
        }

        @Specialization(order = 10, guards = "!isNegative")
        public int doDouble(VirtualFrame frame, RAbstractVector vector, double operand) {
            return (int) convertOperatorRecursive(frame, vector, castInteger(frame, operand));
        }

        @Specialization(order = 11, guards = "isNegative")
        public Object doDoubleNegative(VirtualFrame frame, RAbstractVector vector, double operand) {
            // returns object as it may return either int or RIntVector due to conversion
            return convertOperatorRecursive(frame, vector, castInteger(frame, operand));
        }

        @Specialization(order = 20, guards = "dimLengthOne")
        public int doLogicalDimLengthOne(VirtualFrame frame, RAbstractVector vector, byte operand) {
            return (int) castInteger(frame, operand);
        }

        @Specialization(order = 21, guards = {"!dimLengthOne", "indNA"})
        public RIntVector doLogicalNA(VirtualFrame frame, RAbstractVector vector, byte operand) {
            int dimLength = vector.getDimensions()[dimension];
            int[] data = new int[dimLength];
            Arrays.fill(data, RRuntime.INT_NA);
            return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
        }

        @Specialization(order = 22, guards = {"!dimLengthOne", "!indTrue"})
        public int doLogical(VirtualFrame frame, RAbstractVector vector, byte operand) {
            return (int) castInteger(frame, operand);
        }

        @Specialization(order = 23, guards = {"!dimLengthOne", "indTrue"})
        public RIntVector doLogicalIndTrue(RAbstractVector vector, byte operand) {
            int dimLength = vector.getDimensions()[dimension];
            int[] data = new int[dimLength];
            for (int i = 0; i < dimLength; i++) {
                data[i] = i + 1;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization
        public int doComplex(VirtualFrame frame, RAbstractVector vector, RComplex operand) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "complex");
        }

        @Specialization
        public int doRaw(VirtualFrame frame, RAbstractVector vector, RRaw operand) {
            return (int) castInteger(frame, operand);
        }

        @Specialization(order = 30, guards = "hasDimNames")
        public int doString(VirtualFrame frame, RAbstractVector vector, String operand) {
            RList dimNames = vector.getDimNames();
            RStringVector names = (RStringVector) dimNames.getDataAt(dimension);
            for (int j = 0; j < names.getLength(); j++) {
                if (operand.equals(names.getDataAt(j))) {
                    return j + 1;
                }
            }
            throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
        }

        @Specialization(order = 31, guards = "!hasDimNames")
        public int doStringNoNames(VirtualFrame frame, RAbstractVector vector, String operand) {
            throw RError.getNoArrayDimnames(getEncapsulatingSourceSection());
        }

        @Specialization
        public RIntVector doIntVector(VirtualFrame frame, RAbstractVector vector, RIntVector operand) {
            return transformIntoPositive(vector, operand);
        }

        @Specialization
        public RIntVector doDoubleVector(VirtualFrame frame, RAbstractVector vector, RDoubleVector operand) {
            RIntVector resultVector = (RIntVector) castInteger(frame, operand);
            return transformIntoPositive(vector, resultVector);
        }

        @Specialization(order = 50, guards = "outOfBounds")
        public RLogicalVector doLogicalVectorOutOfBounds(RAbstractVector vector, RLogicalVector operand) {
            throw RError.getLogicalSubscriptLong(getEncapsulatingSourceSection());
        }

        @Specialization(order = 51, guards = "!outOfBounds")
        public RIntVector doLogicalVector(RAbstractVector vector, RLogicalVector operand) {
            int resultLength = vector.getDimensions()[dimension];
            int logicalVectorLength = operand.getLength();
            int logicalVectorInd = 0;
            int data[] = new int[resultLength];
            naCheck.enable(!operand.isComplete());
            boolean hasSeenFalse = false;
            for (int i = 0; i < resultLength; i++, logicalVectorInd = Utils.incMod(logicalVectorInd, logicalVectorLength)) {
                byte b = operand.getDataAt(logicalVectorInd);
                if (naCheck.check(b)) {
                    data[i] = RRuntime.INT_NA;
                } else if (b == RRuntime.LOGICAL_TRUE) {
                    data[i] = i + 1;
                } else if (b == RRuntime.LOGICAL_FALSE) {
                    hasSeenFalse = true;
                    data[i] = 0;
                }
            }
            if (hasSeenFalse) {
                // remove 0s (used to be FALSE) and resize the vector
                return transformIntoPositive(vector, RDataFactory.createIntVector(data, naCheck.neverSeenNA()));
            } else {
                return RDataFactory.createIntVector(data, naCheck.neverSeenNA());
            }
        }

        @Specialization
        public RIntVector doComplexVector(VirtualFrame frame, RAbstractVector vector, RComplexVector operand) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "complex");
        }

        @Specialization
        public RIntVector doRawVector(VirtualFrame frame, RAbstractVector vector, RRawVector operand) {
            RIntVector resultVector = (RIntVector) castInteger(frame, operand);
            return transformIntoPositive(vector, resultVector);
        }

        @Specialization(order = 60, guards = "hasDimNames")
        public RIntVector doStringVector(VirtualFrame frame, RAbstractVector vector, RStringVector operand) {
            RList dimNames = vector.getDimNames();
            RStringVector names = (RStringVector) dimNames.getDataAt(dimension);
            int[] data = new int[operand.getLength()];
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
                    throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
                }
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization(order = 61, guards = "!hasDimNames")
        public RIntVector doStringVectorNoDimNames(VirtualFrame frame, RAbstractVector vector, RStringVector operand) {
            throw RError.getNoArrayDimnames(getEncapsulatingSourceSection());
        }

        private final NACheck positionNACheck = NACheck.create();
        @CompilationFinal private boolean hasSeenPositive;
        @CompilationFinal private boolean hasSeenZero;
        @CompilationFinal private boolean hasSeenNegative;
        @CompilationFinal private boolean hasSeenNA;

        private RIntVector transformIntoPositive(RAbstractVector vector, RIntVector positions) {
            int zeroCount = 0;
            positionNACheck.enable(positions);
            int positionsLength = positions.getLength();
            int dimLength = vector.getDimensions()[dimension];
            for (int i = 0; i < positionsLength; ++i) {
                int pos = positions.getDataAt(i);
                if (positionNACheck.check(pos)) {
                    if (!hasSeenNA) {
                        CompilerDirectives.transferToInterpreter();
                        hasSeenNA = true;
                    }
                } else if (pos > 0) {
                    if (pos > dimLength) {
                        throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
                    }
                    if (!hasSeenPositive) {
                        CompilerDirectives.transferToInterpreter();
                        hasSeenPositive = true;
                    }
                } else if (pos == 0) {
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
            if (hasSeenPositive) {
                if (hasSeenNegative) {
                    throw RError.getOnlyZeroMixed(getEncapsulatingSourceSection());
                } else if (hasSeenZero || hasSeenNA) {
                    // eliminate 0-s
                    int[] data = new int[positionsLength - zeroCount];
                    int ind = 0;
                    for (int i = 0; i < positionsLength; i++) {
                        int pos = positions.getDataAt(i);
                        if (pos != 0) {
                            data[ind++] = pos;
                        }
                    }
                    return RDataFactory.createIntVector(data, positionNACheck.neverSeenNA());
                } else {
                    // fast path (most common expected behavior)
                    return positions;
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

        protected boolean dimLengthOne(RAbstractVector vector, byte operand) {
            return vector.getDimensions()[dimension] == 1;
        }

        protected boolean dimLengthOne(RAbstractVector vector, int operand) {
            return vector.getDimensions()[dimension] == 1;
        }

        protected boolean dimLengthOne(RAbstractVector vector, RMissing operand) {
            return vector.getDimensions()[dimension] == 1;
        }

        protected static boolean indNA(RAbstractVector vector, int operand) {
            return RRuntime.isNA(operand);
        }

        protected static boolean indNA(RAbstractVector vector, byte operand) {
            return RRuntime.isNA(operand);
        }

        protected static boolean indTrue(RAbstractVector vector, byte operand) {
            return operand == RRuntime.LOGICAL_TRUE;
        }

        protected boolean outOfBoundsNegative(RAbstractVector vector, int operand) {
            return operand < 0 && -operand > vector.getDimensions()[dimension];
        }

        protected boolean outOfBounds(RAbstractVector vector, RLogicalVector operand) {
            return operand.getLength() > vector.getDimensions()[dimension];
        }

        protected boolean outOfBounds(RAbstractVector vector, int operand) {
            return operand > vector.getDimensions()[dimension];
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

        protected boolean hasDimNames(RAbstractVector vector, RStringVector operand) {
            return vector.getDimNames() != null;
        }
    }
}
