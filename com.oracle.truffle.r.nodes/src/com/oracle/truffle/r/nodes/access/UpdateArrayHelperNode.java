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

@NodeChildren({@NodeChild(value = "newValue", type = RNode.class), @NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "positions", type = RNode[].class)})
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
        RNode[] positions = new RNode[children.length];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = ArrayPositionCastFactory.create(i, positions.length, true, isSubset, getVector(), getNewValue(), children[i]);
        }
        return new RNode[]{varArgAsObjectArrayNodeFactory.makeList(positions, null)};
    }

    @Specialization(order = 10, guards = "emptyValue")
    RAbstractVector update(RAbstractVector value, RAbstractVector vector, Object[] positions) {
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
    RAbstractVector updatePosZero(RAbstractVector value, RAbstractVector vector, int position) {
        if (!isSubset) {
            throw RError.getReplacementZero(getEncapsulatingSourceSection());
        }
        return value;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 12, guards = {"emptyValue", "!isPositionZero"})
    RAbstractVector update(RAbstractVector value, RAbstractVector vector, int position) {
        throw RError.getReplacementZero(getEncapsulatingSourceSection());
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

    @Specialization(order = 100, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RIntVector update(RAbstractIntVector value, RIntVector vector, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @Specialization(order = 102, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RIntVector update(VirtualFrame frame, RAbstractLogicalVector value, RIntVector vector, Object[] positions) {
        return updateVector((RIntVector) castInteger(frame, value), vector, positions);
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

    @Specialization(order = 200, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RDoubleVector update(VirtualFrame frame, RAbstractIntVector value, RDoubleVector vector, Object[] positions) {
        return updateVector((RDoubleVector) castDouble(frame, value), vector, positions);
    }

    @Specialization(order = 201, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RDoubleVector update(RAbstractDoubleVector value, RDoubleVector vector, Object[] positions) {
        return updateVector(value, vector, positions);
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

    @Specialization(order = 302, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RLogicalVector update(RAbstractLogicalVector value, RLogicalVector vector, Object[] positions) {
        return updateVector(value, vector, positions);
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

    @Specialization(order = 403, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RStringVector update(RAbstractStringVector value, RStringVector vector, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @Specialization(order = 406, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RStringVector update(VirtualFrame frame, RAbstractVector value, RStringVector vector, Object[] positions) {
        return updateVector((RStringVector) castString(frame, value), vector, positions);
    }

    // double vector

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

    @Specialization(order = 500, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RComplexVector update(VirtualFrame frame, RAbstractIntVector value, RComplexVector vector, Object[] positions) {
        return updateVector((RComplexVector) castComplex(frame, value), vector, positions);
    }

    @Specialization(order = 501, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RComplexVector update(VirtualFrame frame, RAbstractDoubleVector value, RComplexVector vector, Object[] positions) {
        return updateVector((RComplexVector) castComplex(frame, value), vector, positions);
    }

    @Specialization(order = 503, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RComplexVector update(RAbstractComplexVector value, RComplexVector vector, Object[] positions) {
        return updateVector(value, vector, positions);
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

    @Specialization(order = 606, guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    RRawVector update(RAbstractRawVector value, RRawVector vector, Object[] positions) {
        return updateVector(value, vector, positions);
    }

    @SuppressWarnings("unused")
    protected boolean emptyValue(RAbstractVector value, RAbstractVector vector, Object[] positions) {
        return value.getLength() == 0;
    }

    @SuppressWarnings("unused")
    protected boolean emptyValue(RAbstractVector value, RAbstractVector vector, int position) {
        return value.getLength() == 0;
    }

    @SuppressWarnings("unused")
    protected boolean wrongDimensionsMatrix(RAbstractVector value, RAbstractVector vector, Object[] positions) {
        if (positions.length == 2 && (vector.getDimensions() == null || vector.getDimensions().length != positions.length)) {
            throw RError.getIncorrectSubscriptsMatrix(getEncapsulatingSourceSection());
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected boolean wrongDimensions(RAbstractVector value, RAbstractVector vector, Object[] positions) {
        if (!((vector.getDimensions() == null && positions.length == 1) || vector.getDimensions().length == positions.length)) {
            throw RError.getIncorrectSubscripts(getEncapsulatingSourceSection());
        }
        return false;
    }

    @SuppressWarnings("unused")
    protected boolean multiDim(RAbstractVector value, RAbstractVector vector, Object[] positions) {
        return vector.getDimensions() != null && vector.getDimensions().length > 1;
    }

    @SuppressWarnings("unused")
    protected boolean isPositionZero(RAbstractVector value, RAbstractVector vector, int position) {
        return position == 0;
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
        Object coerce(RAbstractVector value, RAbstractVector operand) {
            return operand.materialize();
        }
    }
}
