/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.RCallNode.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.access.UpdateArrayHelperNodeFactory.CoerceOperandFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild(value = "newValue", type = RNode.class), @NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "recursionLevel", type = RNode.class),
                @NodeChild(value = "positions", type = RNode[].class)})
public abstract class UpdateArrayHelperNode extends RNode {

    private static final VarArgsAsObjectArrayNodeFactory varArgAsObjectArrayNodeFactory = new VarArgsAsObjectArrayNodeFactory();

    private final boolean isSubset;

    private final NACheck elementNACheck = NACheck.create();

    abstract RNode getVector();

    abstract RNode getNewValue();

    @Child private CastComplexNode castComplex;
    @Child private CastDoubleNode castDouble;
    @Child private CastIntegerNode castInteger;
    @Child private CastStringNode castString;

    public UpdateArrayHelperNode(boolean isSubset) {
        this.isSubset = isSubset;
    }

    public UpdateArrayHelperNode(UpdateArrayHelperNode other) {
        this.isSubset = other.isSubset;
    }

    private Object castComplex(VirtualFrame frame, Object operand) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreter();
            castComplex = adoptChild(CastComplexNodeFactory.create(null, true, true));
        }
        return castComplex.executeCast(frame, operand);
    }

    private Object castDouble(VirtualFrame frame, Object operand) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreter();
            castDouble = adoptChild(CastDoubleNodeFactory.create(null, true, true));
        }
        return castDouble.executeCast(frame, operand);
    }

    private Object castInteger(VirtualFrame frame, Object operand) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreter();
            castInteger = adoptChild(CastIntegerNodeFactory.create(null, true, true));
        }
        return castInteger.executeCast(frame, operand);
    }

    private Object castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreter();
            castString = adoptChild(CastStringNodeFactory.create(null, false, true, true));
        }
        return castString.executeCast(frame, operand);
    }

    @CreateCast({"newValue"})
    public RNode createCastValue(RNode child) {
        return CastToVectorNodeFactory.create(child, false, false, false);
    }

    @CreateCast({"vector"})
    public RNode createCastVector(RNode child) {
        return CoerceOperandFactory.create(getNewValue(), child);
    }

    @CreateCast({"positions"})
    public RNode[] createCastPositions(RNode[] children) {
        RNode[] positions;
        if (children.length == 0) {
            // []
            positions = new RNode[]{ArrayPositionCastFactory.create(0, 1, true, isSubset, getVector(), getNewValue(), ConstantNode.create(RMissing.instance))};
        } else {
            positions = new RNode[children.length];
            for (int i = 0; i < positions.length; i++) {
                positions[i] = ArrayPositionCastFactory.create(i, positions.length, true, isSubset, getVector(), getNewValue(), children[i]);
            }
        }
        return new RNode[]{varArgAsObjectArrayNodeFactory.makeList(positions, null)};
    }

    @Specialization(order = 10, guards = "emptyValue")
    RAbstractVector update(RAbstractVector value, RAbstractVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        if (isSubset) {
            int replacementLength = getReplacementLength(positions, value.getLength());
            if (replacementLength == 0) {
                return vector;
            }
        }
        throw RError.getReplacementZero(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 11, guards = {"emptyValue", "isPositionZero"})
    RAbstractVector updatePosZero(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        if (!isSubset) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }
        return vector;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 12, guards = {"emptyValue", "!isPositionZero"})
    RAbstractVector update(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        throw RError.getReplacementZero(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 15)
    RAbstractVector update(RAbstractVector value, RAbstractVector vector, int recLevel, RNull position) {
        throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 16, guards = {"isPosNA", "isValueLengthOne"})
    RAbstractVector updateNAValueLengthOne(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        } else {
            return vector;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 17, guards = {"isPosNA", "!isValueLengthOne"})
    RAbstractVector updateNA(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        if (isSubset) {
            throw RError.getNASubscripted(getEncapsulatingSourceSection());
        } else {
            throw RError.getMoreElementsSupplied(getEncapsulatingSourceSection());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 18, guards = "isPosZero")
    RAbstractVector updateNAOrZero(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        } else {
            return vector;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 19, guards = {"!isValueLengthOne", "!isSubset"})
    RAbstractVector updateTooManyValues(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        throw RError.getMoreElementsSupplied(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 20, guards = "wrongLength")
    RAbstractVector update(RAbstractVector value, RAbstractVector vector, int recLevel, RIntVector positions) {
        throw RError.getNotMultipleReplacement(getEncapsulatingSourceSection());
    }

    private int getNewArrayBase(int srcArrayBase, RIntVector p, int i, int newAccSrcDimensions) {
        int newSrcArrayBase;
        int pos = p.getDataAt(i);
        if (elementNACheck.check(pos)) {
            throw RError.getNASubscripted(getEncapsulatingSourceSection());
        } else {
            newSrcArrayBase = srcArrayBase + newAccSrcDimensions * (pos - 1);
        }
        return newSrcArrayBase;
    }

    private int getSrcIndex(int srcArrayBase, RIntVector p, int i, int newAccSrcDimensions) {
        int pos = p.getDataAt(i);
        if (elementNACheck.check(pos)) {
            throw RError.getNASubscripted(getEncapsulatingSourceSection());
        } else {
            return srcArrayBase + newAccSrcDimensions * (pos - 1);
        }
    }

    private int getSrcArrayBase(int pos, int accSrcDimensions) {
        if (elementNACheck.check(pos)) {
            throw RError.getNASubscripted(getEncapsulatingSourceSection());
        } else {
            return accSrcDimensions * (pos - 1);
        }
    }

    private int getReplacementLength(Object[] positions, int valueLength) {
        int length = 1;
        for (int i = 0; i < positions.length; i++) {
            RIntVector p = (RIntVector) positions[i];
            int len = p.getLength();
            boolean allZeros = true;
            for (int j = 0; j < len; j++) {
                if (p.getDataAt(j) != 0) {
                    allZeros = false;
                }
            }
            if (allZeros) {
                length = 0;
            } else {
                length *= p.getLength();
            }
        }
        if (valueLength != 0 && length != 0 && length % valueLength != 0) {
            throw RError.getNotMultipleReplacement(getEncapsulatingSourceSection());
        }
        return length;
    }

    private int getHighestPos(RIntVector positions, RAbstractVector value) {
        int highestPos = 0;
        elementNACheck.enable(!value.isComplete());
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            assert !RRuntime.isNA(p);
            if (p > highestPos) {
                highestPos = p;
            }
        }
        return highestPos;
    }

    // null

    @Specialization(order = 50)
    RIntVector update(RAbstractIntVector value, @SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, RDataFactory.createIntVector(positions.getLength()), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 51)
    RIntVector update(RAbstractIntVector value, RNull vector, int recLevel, int position) {
        return updateSingleDim(value, RDataFactory.createIntVector(1), position);
    }

    @Specialization(order = 55)
    RDoubleVector update(RAbstractDoubleVector value, @SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, RDataFactory.createDoubleVector(positions.getLength()), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 56)
    RDoubleVector update(RAbstractDoubleVector value, RNull vector, int recLevel, int position) {
        return updateSingleDim(value, RDataFactory.createDoubleVector(1), position);
    }

    @Specialization(order = 60)
    RLogicalVector update(RAbstractLogicalVector value, @SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, RDataFactory.createLogicalVector(positions.getLength()), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 61)
    RLogicalVector update(RAbstractLogicalVector value, RNull vector, int recLevel, int position) {
        return updateSingleDim(value, RDataFactory.createLogicalVector(1), position);
    }

    @Specialization(order = 65)
    RStringVector update(RAbstractStringVector value, @SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, RDataFactory.createStringVector(positions.getLength()), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 66)
    RStringVector update(RAbstractStringVector value, RNull vector, int recLevel, int position) {
        return updateSingleDim(value, RDataFactory.createStringVector(1), position);
    }

    @Specialization(order = 70)
    RComplexVector update(RAbstractComplexVector value, @SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, RDataFactory.createComplexVector(positions.getLength()), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 71)
    RComplexVector update(RAbstractComplexVector value, RNull vector, int recLevel, int position) {
        return updateSingleDim(value, RDataFactory.createComplexVector(1), position);
    }

    @Specialization(order = 75)
    RRawVector update(RAbstractRawVector value, @SuppressWarnings("unused") RNull vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, RDataFactory.createRawVector(positions.getLength()), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 76)
    RRawVector update(RAbstractRawVector value, RNull vector, int recLevel, int position) {
        return updateSingleDim(value, RDataFactory.createRawVector(1), position);
    }

    // list

    private void setData(RAbstractVector value, RList vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAtAsObject(dstIndex % value.getLength()), null);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                setData(value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    private RList updateVector(RAbstractVector value, RList vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value.getLength());
        RList resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            resultVector = (RList) vector.copy();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setData(value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
        }
        return resultVector;
    }

    @Specialization(order = 100, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RList update(RAbstractVector value, RList vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 190, guards = {"!wrongDimensionsMatrix", "!wrongDimensions"})
    RList update(RNull value, RList vector, int recLevel, Object[] positions) {
        RList resultVector = vector;
        if (vector.isShared()) {
            resultVector = (RList) vector.copy();
        }
        return resultVector;
    }

    // int vector

    private void setData(RAbstractIntVector value, RIntVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                setData(value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    private RIntVector updateVector(RAbstractIntVector value, RIntVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value.getLength());
        RIntVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            resultVector = (RIntVector) vector.copy();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(!resultVector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setData(value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
        }
        return resultVector;
    }

    private static RIntVector getResultVector(RIntVector vector, int highestPos) {
        RIntVector resultVector = vector;
        if (vector.isShared()) {
            resultVector = (RIntVector) vector.copy();
        }
        if (resultVector.getLength() < highestPos) {
            resultVector = resultVector.copyResized(highestPos, true);
        }
        return resultVector;
    }

    private RIntVector updateSingleDim(RAbstractIntVector value, RIntVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private RIntVector updateSingleDimVector(RAbstractIntVector value, RIntVector resultVector, RIntVector positions) {
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
        }
        return resultVector;
    }

    @Specialization(order = 200, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RIntVector update(RAbstractIntVector value, RIntVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @Specialization(order = 202, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RIntVector update(VirtualFrame frame, RAbstractLogicalVector value, RIntVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector((RIntVector) castInteger(frame, value), vector, positions);
    }

    @Specialization(order = 220)
    RAbstractIntVector update(RAbstractIntVector value, RIntVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 221, guards = {"!isValueLengthOne", "isSubset"})
    RIntVector updateTooManyValuesSubset(RAbstractIntVector value, RIntVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(order = 222, guards = "isValueLengthOne")
    RIntVector update(RAbstractIntVector value, RIntVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(order = 240)
    RAbstractIntVector update(VirtualFrame frame, RAbstractLogicalVector value, RIntVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector((RIntVector) castInteger(frame, value), getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 241, guards = {"!isValueLengthOne", "isSubset"})
    RIntVector updateTooManyValuesSubset(VirtualFrame frame, RAbstractLogicalVector value, RIntVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim((RIntVector) castInteger(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(order = 242, guards = "isValueLengthOne")
    RIntVector update(VirtualFrame frame, RAbstractLogicalVector value, RIntVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim((RIntVector) castInteger(frame, value), getResultVector(vector, position), position);
    }

    // double vector

    private void setData(RAbstractDoubleVector value, RDoubleVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                setData(value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    private RDoubleVector updateVector(RAbstractDoubleVector value, RDoubleVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value.getLength());
        RDoubleVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            resultVector = (RDoubleVector) vector.copy();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(!resultVector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setData(value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
        }
        return resultVector;
    }

    private static RDoubleVector getResultVector(RDoubleVector vector, int highestPos) {
        RDoubleVector resultVector = vector;
        if (vector.isShared()) {
            resultVector = (RDoubleVector) vector.copy();
        }
        if (resultVector.getLength() < highestPos) {
            resultVector = resultVector.copyResized(highestPos, true);
        }
        return resultVector;
    }

    private RDoubleVector updateSingleDim(RAbstractDoubleVector value, RDoubleVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private RDoubleVector updateSingleDimVector(RAbstractDoubleVector value, RDoubleVector resultVector, RIntVector positions) {
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
        }
        return resultVector;
    }

    @Specialization(order = 300, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RDoubleVector update(VirtualFrame frame, RAbstractIntVector value, RDoubleVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector((RDoubleVector) castDouble(frame, value), vector, positions);
    }

    @Specialization(order = 301, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RDoubleVector update(RAbstractDoubleVector value, RDoubleVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @Specialization(order = 320)
    RAbstractDoubleVector update(VirtualFrame frame, RAbstractIntVector value, RDoubleVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector((RDoubleVector) castDouble(frame, value), getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 321, guards = {"!isValueLengthOne", "isSubset"})
    RDoubleVector updateTooManyValuesSubset(VirtualFrame frame, RAbstractIntVector value, RDoubleVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim((RDoubleVector) castDouble(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(order = 322, guards = "isValueLengthOne")
    RDoubleVector update(VirtualFrame frame, RAbstractIntVector value, RDoubleVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim((RDoubleVector) castDouble(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(order = 330)
    RAbstractDoubleVector update(RAbstractDoubleVector value, RDoubleVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 331, guards = {"!isValueLengthOne", "isSubset"})
    RDoubleVector updateTooManyValuesSubset(RAbstractDoubleVector value, RDoubleVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(order = 332, guards = "isValueLengthOne")
    RDoubleVector update(RAbstractDoubleVector value, RDoubleVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    // logical vector

    private void setData(RAbstractLogicalVector value, RLogicalVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                setData(value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    private RLogicalVector updateVector(RAbstractLogicalVector value, RLogicalVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value.getLength());
        RLogicalVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            resultVector = (RLogicalVector) vector.copy();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(!resultVector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setData(value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
        }
        return resultVector;
    }

    private static RLogicalVector getResultVector(RLogicalVector vector, int highestPos) {
        RLogicalVector resultVector = vector;
        if (vector.isShared()) {
            resultVector = (RLogicalVector) vector.copy();
        }
        if (resultVector.getLength() < highestPos) {
            resultVector = resultVector.copyResized(highestPos, true);
        }
        return resultVector;
    }

    private RLogicalVector updateSingleDim(RAbstractLogicalVector value, RLogicalVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private RLogicalVector updateSingleDimVector(RAbstractLogicalVector value, RLogicalVector resultVector, RIntVector positions) {
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
        }
        return resultVector;
    }

    @Specialization(order = 402, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RLogicalVector update(RAbstractLogicalVector value, RLogicalVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @Specialization(order = 440)
    RAbstractLogicalVector update(RAbstractLogicalVector value, RLogicalVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 441, guards = {"!isValueLengthOne", "isSubset"})
    RLogicalVector updateTooManyValuesSubset(RAbstractLogicalVector value, RLogicalVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(order = 442, guards = "isValueLengthOne")
    RLogicalVector update(RAbstractLogicalVector value, RLogicalVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    // string vector

    private void setData(RAbstractStringVector value, RStringVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                setData(value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    private RStringVector updateVector(RAbstractStringVector value, RStringVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value.getLength());
        RStringVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            resultVector = (RStringVector) vector.copy();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(!resultVector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setData(value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
        }
        return resultVector;
    }

    private static RStringVector getResultVector(RStringVector vector, int highestPos) {
        RStringVector resultVector = vector;
        if (vector.isShared()) {
            resultVector = (RStringVector) vector.copy();
        }
        if (resultVector.getLength() < highestPos) {
            resultVector = resultVector.copyResized(highestPos, true);
        }
        return resultVector;
    }

    private RStringVector updateSingleDim(RAbstractStringVector value, RStringVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private RStringVector updateSingleDimVector(RAbstractStringVector value, RStringVector resultVector, RIntVector positions) {
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
        }
        return resultVector;
    }

    @Specialization(order = 503, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RStringVector update(RAbstractStringVector value, RStringVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @Specialization(order = 507, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RStringVector update(VirtualFrame frame, RAbstractVector value, RStringVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector((RStringVector) castString(frame, value), vector, positions);
    }

    @Specialization(order = 550)
    RAbstractStringVector update(RAbstractStringVector value, RStringVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 551, guards = {"!isValueLengthOne", "isSubset"})
    RStringVector updateTooManyValuesSubset(RAbstractStringVector value, RStringVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(order = 552, guards = "isValueLengthOne")
    RStringVector update(RAbstractStringVector value, RStringVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(order = 590)
    RAbstractStringVector update(VirtualFrame frame, RAbstractVector value, RStringVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector((RStringVector) castString(frame, value), getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 591, guards = {"!isValueLengthOne", "isSubset"})
    RStringVector updateTooManyValuesSubset(VirtualFrame frame, RAbstractVector value, RStringVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim((RStringVector) castString(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(order = 592, guards = "isValueLengthOne")
    RStringVector update(VirtualFrame frame, RAbstractVector value, RStringVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim((RStringVector) castString(frame, value), getResultVector(vector, position), position);
    }

    // complex vector

    private void setData(RAbstractComplexVector value, RComplexVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()), elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                setData(value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    private RComplexVector updateVector(RAbstractComplexVector value, RComplexVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value.getLength());
        RComplexVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            resultVector = (RComplexVector) vector.copy();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(!resultVector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setData(value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
        }
        return resultVector;
    }

    private static RComplexVector getResultVector(RComplexVector vector, int highestPos) {
        RComplexVector resultVector = vector;
        if (vector.isShared()) {
            resultVector = (RComplexVector) vector.copy();
        }
        if (resultVector.getLength() < highestPos) {
            resultVector = resultVector.copyResized(highestPos, true);
        }
        return resultVector;
    }

    private RComplexVector updateSingleDim(RAbstractComplexVector value, RComplexVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private RComplexVector updateSingleDimVector(RAbstractComplexVector value, RComplexVector resultVector, RIntVector positions) {
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
        }
        return resultVector;
    }

    @Specialization(order = 600, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RComplexVector update(VirtualFrame frame, RAbstractIntVector value, RComplexVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector((RComplexVector) castComplex(frame, value), vector, positions);
    }

    @Specialization(order = 601, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RComplexVector update(VirtualFrame frame, RAbstractDoubleVector value, RComplexVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector((RComplexVector) castComplex(frame, value), vector, positions);
    }

    @Specialization(order = 603, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RComplexVector update(RAbstractComplexVector value, RComplexVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @Specialization(order = 620)
    RAbstractComplexVector update(VirtualFrame frame, RAbstractIntVector value, RComplexVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 621, guards = {"!isValueLengthOne", "isSubset"})
    RComplexVector updateTooManyValuesSubset(VirtualFrame frame, RAbstractIntVector value, RComplexVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim((RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(order = 622, guards = "isValueLengthOne")
    RComplexVector update(VirtualFrame frame, RAbstractIntVector value, RComplexVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim((RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(order = 630)
    RAbstractComplexVector update(VirtualFrame frame, RAbstractDoubleVector value, RComplexVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 631, guards = {"!isValueLengthOne", "isSubset"})
    RComplexVector updateTooManyValuesSubset(VirtualFrame frame, RAbstractDoubleVector value, RComplexVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim((RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(order = 632, guards = "isValueLengthOne")
    RComplexVector update(VirtualFrame frame, RAbstractDoubleVector value, RComplexVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim((RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(order = 650)
    RAbstractComplexVector update(RAbstractComplexVector value, RComplexVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 651, guards = {"!isValueLengthOne", "isSubset"})
    RComplexVector updateTooManyValuesSubset(RAbstractComplexVector value, RComplexVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(order = 652, guards = "isValueLengthOne")
    RComplexVector update(RAbstractComplexVector value, RComplexVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    // raw vector

    private void setData(RAbstractRawVector value, RRawVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                vector.updateDataAt(srcIndex, value.getDataAt(dstIndex % value.getLength()));
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                setData(value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    private RRawVector updateVector(RAbstractRawVector value, RRawVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value.getLength());
        RRawVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            resultVector = (RRawVector) vector.copy();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(!resultVector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setData(value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
        }
        return resultVector;
    }

    private static RRawVector getResultVector(RRawVector vector, int highestPos) {
        RRawVector resultVector = vector;
        if (vector.isShared()) {
            resultVector = (RRawVector) vector.copy();
        }
        if (resultVector.getLength() < highestPos) {
            resultVector = resultVector.copyResized(highestPos, true);
        }
        return resultVector;
    }

    private static RRawVector updateSingleDim(RAbstractRawVector value, RRawVector resultVector, int position) {
        resultVector.updateDataAt(position - 1, value.getDataAt(0));
        return resultVector;
    }

    private static RRawVector updateSingleDimVector(RAbstractRawVector value, RRawVector resultVector, RIntVector positions) {
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()));
        }
        return resultVector;
    }

    @Specialization(order = 706, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RRawVector update(RAbstractRawVector value, RRawVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @Specialization(order = 780)
    RAbstractRawVector update(RAbstractRawVector value, RRawVector vector, @SuppressWarnings("unused") int recLevel, RIntVector positions) {
        return updateSingleDimVector(value, getResultVector(vector, getHighestPos(positions, value)), positions);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 781, guards = {"!isValueLengthOne", "isSubset"})
    RRawVector updateTooManyValuesSubset(RAbstractRawVector value, RRawVector vector, int recLevel, int position) {
        RContext.getInstance().setEvalWarning(RError.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(order = 782, guards = "isValueLengthOne")
    RRawVector update(RAbstractRawVector value, RRawVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @SuppressWarnings("unused")
    protected boolean emptyValue(RAbstractVector value, RAbstractVector vector, int recLevel, Object[] positions) {
        return value.getLength() == 0;
    }

    @SuppressWarnings("unused")
    protected boolean emptyValue(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        return value.getLength() == 0;
    }

    @SuppressWarnings("unused")
    protected boolean wrongDimensionsMatrix(RAbstractVector value, RAbstractVector vector, int recLevel, Object[] positions) {
        if (positions.length == 2 && (vector.getDimensions() == null || vector.getDimensions().length != positions.length)) {
            throw RError.getIncorrectSubscriptsMatrix(getEncapsulatingSourceSection());
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected boolean wrongDimensionsMatrix(RNull value, RAbstractVector vector, int recLevel, Object[] positions) {
        if (positions.length == 2 && (vector.getDimensions() == null || vector.getDimensions().length != positions.length)) {
            throw RError.getIncorrectSubscriptsMatrix(getEncapsulatingSourceSection());
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected boolean wrongDimensions(RAbstractVector value, RAbstractVector vector, int recLevel, Object[] positions) {
        if (!((vector.getDimensions() == null && positions.length == 1) || vector.getDimensions().length == positions.length)) {
            throw RError.getIncorrectSubscripts(getEncapsulatingSourceSection());
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected boolean wrongDimensions(RNull value, RAbstractVector vector, int recLevel, Object[] positions) {
        if (!((vector.getDimensions() == null && positions.length == 1) || vector.getDimensions().length == positions.length)) {
            throw RError.getIncorrectSubscripts(getEncapsulatingSourceSection());
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected boolean multiDim(RAbstractVector value, RAbstractVector vector, int recLevel, Object[] positions) {
        return vector.getDimensions() != null && vector.getDimensions().length > 1;
    }

    @SuppressWarnings("unused")
    protected boolean isPositionZero(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        return position == 0;
    }

    @SuppressWarnings("unused")
    protected boolean wrongLength(RAbstractVector value, RAbstractVector vector, int recLevel, RIntVector positions) {
        int valLength = value.getLength();
        int posLength = positions.getLength();
        return valLength > posLength || (posLength % valLength != 0);
    }

    @SuppressWarnings("unused")
    protected boolean isPosNA(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        return RRuntime.isNA(position);
    }

    @SuppressWarnings("unused")
    protected boolean isPosZero(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        return position == 0;
    }

    @SuppressWarnings("unused")
    protected boolean isValueLengthOne(RAbstractVector value, RAbstractVector vector, int recLevel, int position) {
        return value.getLength() == 1;
    }

    @SuppressWarnings("unused")
    protected boolean isPosLengthOne(RAbstractVector value, RAbstractVector vector, int recLevel, RIntVector position) {
        return position.getLength() == 1;
    }

    protected boolean isSubset() {
        return isSubset;
    }

    @SuppressWarnings("unused")
    @NodeChildren({@NodeChild(value = "newValue", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
    abstract static class CoerceOperand extends RNode {

        @Child private CastComplexNode castComplex;
        @Child private CastDoubleNode castDouble;
        @Child private CastIntegerNode castInteger;
        @Child private CastStringNode castString;

        private Object castComplex(VirtualFrame frame, Object operand) {
            if (castComplex == null) {
                CompilerDirectives.transferToInterpreter();
                castComplex = adoptChild(CastComplexNodeFactory.create(null, true, true));
            }
            return castComplex.executeCast(frame, operand);
        }

        private Object castDouble(VirtualFrame frame, Object operand) {
            if (castDouble == null) {
                CompilerDirectives.transferToInterpreter();
                castDouble = adoptChild(CastDoubleNodeFactory.create(null, true, true));
            }
            return castDouble.executeCast(frame, operand);
        }

        private Object castInteger(VirtualFrame frame, Object operand) {
            if (castInteger == null) {
                CompilerDirectives.transferToInterpreter();
                castInteger = adoptChild(CastIntegerNodeFactory.create(null, true, true));
            }
            return castInteger.executeCast(frame, operand);
        }

        private Object castString(VirtualFrame frame, Object operand) {
            if (castString == null) {
                CompilerDirectives.transferToInterpreter();
                castString = adoptChild(CastStringNodeFactory.create(null, false, true, true));
            }
            return castString.executeCast(frame, operand);
        }

        @Specialization(order = 10)
        RIntVector coerce(RAbstractIntVector value, RNull operand) {
            return RDataFactory.createEmptyIntVector();
        }

        @Specialization(order = 11)
        RDoubleVector coerce(RAbstractDoubleVector value, RNull operand) {
            return RDataFactory.createEmptyDoubleVector();
        }

        @Specialization(order = 12)
        RLogicalVector coerce(RAbstractLogicalVector value, RNull operand) {
            return RDataFactory.createEmptyLogicalVector();
        }

        @Specialization(order = 13)
        RStringVector coerce(RAbstractStringVector value, RNull operand) {
            return RDataFactory.createEmptyStringVector();
        }

        @Specialization(order = 14)
        RComplexVector coerce(RAbstractComplexVector value, RNull operand) {
            return RDataFactory.createEmptyComplexVector();
        }

        @Specialization(order = 15)
        RRawVector coerce(RAbstractRawVector value, RNull operand) {
            return RDataFactory.createEmptyRawVector();
        }

        // int vector value

        @Specialization(order = 102)
        RIntVector coerce(VirtualFrame frame, RAbstractIntVector value, RAbstractLogicalVector operand) {
            return (RIntVector) castInteger(frame, operand);
        }

        @Specialization(order = 105)
        RIntVector coerce(VirtualFrame frame, RAbstractIntVector value, RAbstractRawVector operand) {
            throw RError.getSubassignTypeFix(getEncapsulatingSourceSection(), "integer", "raw");
        }

        // double vector value

        @Specialization(order = 200)
        RDoubleVector coerce(VirtualFrame frame, RAbstractDoubleVector value, RAbstractIntVector operand) {
            return (RDoubleVector) castDouble(frame, operand);
        }

        @Specialization(order = 202)
        RDoubleVector coerce(VirtualFrame frame, RAbstractDoubleVector value, RAbstractLogicalVector operand) {
            return (RDoubleVector) castDouble(frame, operand);
        }

        @Specialization(order = 205)
        RDoubleVector coerce(VirtualFrame frame, RAbstractDoubleVector value, RAbstractRawVector operand) {
            throw RError.getSubassignTypeFix(getEncapsulatingSourceSection(), "double", "raw");
        }

        // logical vector value

        @Specialization(order = 305)
        RLogicalVector coerce(VirtualFrame frame, RAbstractLogicalVector value, RAbstractRawVector operand) {
            throw RError.getSubassignTypeFix(getEncapsulatingSourceSection(), "logical", "raw");
        }

        // string vector value

        @Specialization(order = 405)
        RStringVector coerce(VirtualFrame frame, RAbstractStringVector value, RAbstractRawVector operand) {
            throw RError.getSubassignTypeFix(getEncapsulatingSourceSection(), "character", "raw");
        }

        @Specialization(order = 406)
        RStringVector coerce(VirtualFrame frame, RAbstractStringVector value, RAbstractVector operand) {
            return (RStringVector) castString(frame, operand);
        }

        // complex vector value

        @Specialization(order = 500)
        RComplexVector coerce(VirtualFrame frame, RAbstractComplexVector value, RAbstractIntVector operand) {
            return (RComplexVector) castComplex(frame, operand);
        }

        @Specialization(order = 502)
        RComplexVector coerce(VirtualFrame frame, RAbstractComplexVector value, RAbstractLogicalVector operand) {
            return (RComplexVector) castComplex(frame, operand);
        }

        @Specialization(order = 505)
        RComplexVector coerce(VirtualFrame frame, RAbstractComplexVector value, RAbstractRawVector operand) {
            throw RError.getSubassignTypeFix(getEncapsulatingSourceSection(), "complex", "raw");
        }

        // raw vector value

        @Specialization(order = 605)
        RRawVector coerce(VirtualFrame frame, RAbstractRawVector value, RAbstractRawVector operand) {
            return operand.materialize();
        }

        @Specialization(order = 606)
        RRawVector coerce(VirtualFrame frame, RAbstractRawVector value, RAbstractVector operand) {
            throw RError.getSubassignTypeFix(getEncapsulatingSourceSection(), "raw", RRuntime.classToString(operand.getElementClass(), false));
        }

        // in all other cases, simply return the operand (no coercion)

        @Specialization(order = 1000)
        RNull coerce(RAbstractVector value, RNull operand) {
            return operand;
        }

        @Specialization(order = 1001)
        RAbstractVector coerce(RAbstractVector value, RAbstractVector operand) {
            return operand;
        }
    }
}
