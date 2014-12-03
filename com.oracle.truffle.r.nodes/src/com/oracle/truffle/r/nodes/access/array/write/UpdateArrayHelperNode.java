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
package com.oracle.truffle.r.nodes.access.array.write;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.array.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCastFactory.OperatorConverterNodeFactory;
import com.oracle.truffle.r.nodes.access.array.read.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "v", type = RNode.class), @NodeChild(value = "newValue", type = RNode.class), @NodeChild(value = "recursionLevel", type = RNode.class),
                @NodeChild(value = "positions", type = PositionsArrayNodeValue.class, executeWith = {"v", "newValue"}),
                @NodeChild(value = "vector", type = CoerceVector.class, executeWith = {"newValue", "v", "positions"})})
public abstract class UpdateArrayHelperNode extends RNode {

    private final boolean isSubset;

    private final NACheck elementNACheck = NACheck.create();
    private final NACheck posNACheck = NACheck.create();
    private final NACheck namesNACheck = NACheck.create();

    protected abstract RNode getVector();

    protected abstract RNode getPositions();

    protected abstract RNode getNewValue();

    protected abstract Object executeUpdate(VirtualFrame frame, Object v, Object value, int recLevel, Object positions, Object vector);

    @CompilationFinal private boolean recursiveIsSubset;

    @Child private UpdateArrayHelperNode updateRecursive;
    @Child private CastComplexNode castComplex;
    @Child private CastDoubleNode castDouble;
    @Child private CastIntegerNode castInteger;
    @Child private CastStringNode castString;
    @Child private CoerceVector coerceVector;
    @Child private ArrayPositionCast castPosition;
    @Child private OperatorConverterNode operatorConverter;
    @Child private SetMultiDimDataNode setMultiDimData;

    private final BranchProfile error = BranchProfile.create();
    private final BranchProfile warning = BranchProfile.create();
    private final ConditionProfile negativePosProfile = ConditionProfile.createBinaryProfile();

    private final BranchProfile vectorShared = BranchProfile.create();
    private final BranchProfile vectorTooShort = BranchProfile.create();
    private final BranchProfile vectorNoDims = BranchProfile.create();

    private final ConditionProfile noResultNames = ConditionProfile.createBinaryProfile();
    private final ConditionProfile multiPosProfile = ConditionProfile.createBinaryProfile();

    private final BranchProfile recLevelZero = BranchProfile.create();
    private final ConditionProfile twoPosProfile = ConditionProfile.createBinaryProfile();

    public UpdateArrayHelperNode(boolean isSubset) {
        this.isSubset = isSubset;
        this.recursiveIsSubset = isSubset;
    }

    public UpdateArrayHelperNode(UpdateArrayHelperNode other) {
        this.isSubset = other.isSubset;
        this.recursiveIsSubset = other.recursiveIsSubset;
    }

    private Object updateRecursive(VirtualFrame frame, Object v, Object value, Object vector, Object operand, int recLevel, boolean forDataFrame) {
        // for data frames, recursive update is the same as for lists but as if the [[]] operator
        // was used
        if (updateRecursive == null || (forDataFrame && isSubset) || (!forDataFrame && isSubset != recursiveIsSubset)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            boolean newIsSubset = this.isSubset;
            if (forDataFrame && isSubset) {
                newIsSubset = false;
            }
            updateRecursive = insert(UpdateArrayHelperNodeFactory.create(newIsSubset, null, null, null, null, null));
        }
        return updateRecursive.executeUpdate(frame, v, value, recLevel, operand, vector);
    }

    private Object updateRecursive(VirtualFrame frame, Object v, Object value, Object vector, Object operand, int recLevel) {
        return updateRecursive(frame, v, value, vector, operand, recLevel, false);
    }

    private Object castComplex(VirtualFrame frame, Object operand) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplex = insert(CastComplexNodeFactory.create(null, true, true, false));
        }
        return castComplex.executeCast(frame, operand);
    }

    private Object castDouble(VirtualFrame frame, Object operand) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeFactory.create(null, true, true, false));
        }
        return castDouble.executeCast(frame, operand);
    }

    private Object castInteger(VirtualFrame frame, Object operand) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeFactory.create(null, true, true, false));
        }
        return castInteger.executeCast(frame, operand);
    }

    private Object castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeFactory.create(null, true, true, false, true));
        }
        return castString.executeCast(frame, operand);
    }

    private Object coerceVector(VirtualFrame frame, Object vector, Object value, Object operand) {
        if (coerceVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            coerceVector = insert(CoerceVectorFactory.create(null, null, null));
        }
        return coerceVector.executeEvaluated(frame, value, vector, operand);
    }

    private Object castPosition(VirtualFrame frame, Object vector, Object operand) {
        if (castPosition == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castPosition = insert(ArrayPositionCastFactory.create(0, 1, true, false, null, null));
        }
        return castPosition.executeArg(frame, vector, operand);
    }

    private void initOperatorConvert() {
        if (operatorConverter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            operatorConverter = insert(OperatorConverterNodeFactory.create(0, 1, true, false, null, null, null));
        }
    }

    private Object convertOperand(VirtualFrame frame, Object vector, int operand) {
        initOperatorConvert();
        return operatorConverter.executeConvert(frame, vector, operand, RRuntime.LOGICAL_TRUE);
    }

    private Object convertOperand(VirtualFrame frame, Object vector, String operand) {
        initOperatorConvert();
        return operatorConverter.executeConvert(frame, vector, operand, RRuntime.LOGICAL_TRUE);
    }

    private Object setMultiDimData(VirtualFrame frame, RAbstractContainer value, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions, NACheck posNACheck, NACheck elementNACheck) {
        if (setMultiDimData == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setMultiDimData = insert(SetMultiDimDataNodeFactory.create(posNACheck, elementNACheck, this.isSubset, null, null, null, null, null, null, null, null));
        }
        return setMultiDimData.executeMultiDimDataSet(frame, value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
    }

    @CreateCast({"newValue"})
    public RNode createCastValue(RNode child) {
        return CastToContainerNodeFactory.create(child, false, false, false);
    }

    @Specialization
    protected Object update(VirtualFrame frame, Object v, Object value, int recLevel, Object positions, RDataFrame vector) {
        RVector inner = vector.getVector();
        RVector res = (RVector) updateRecursive(frame, v, value, inner, positions, recLevel, true);
        if (res != inner) {
            return RDataFactory.createDataFrame(res);
        } else {
            return vector;
        }
    }

    @Specialization(guards = "isSubset")
    protected Object update(VirtualFrame frame, Object v, RFactor value, int recLevel, Object positions, Object vector) {
        return updateRecursive(frame, v, value.getVector(), vector, positions, recLevel);
    }

    @Specialization(guards = "emptyValue")
    protected RAbstractVector update(Object v, RAbstractVector value, int recLevel, Object[] positions, RAbstractVector vector) {
        if (isSubset) {
            int replacementLength = getReplacementLength(positions, value, false);
            if (replacementLength == 0) {
                return vector;
            }
        }
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
    }

    @Specialization
    protected RNull accessFunction(Object v, Object value, int recLevel, Object position, RFunction vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.OBJECT_NOT_SUBSETTABLE, "closure");
    }

    @Specialization
    protected RAbstractVector update(Object v, RNull value, int recLevel, Object[] positions, RList vector) {
        if (isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NOT_MULTIPLE_REPLACEMENT);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_TYPES, "NULL", "list");
        }
    }

    @Specialization(guards = "isPosZero")
    protected RAbstractVector updateNAOrZero(Object v, RNull value, int recLevel, int position, RList vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            return vector;
        }
    }

    @Specialization
    protected RAbstractVector update(Object v, RNull value, int recLevel, Object[] positions, RAbstractVector vector) {
        if (isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NOT_MULTIPLE_REPLACEMENT);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
    }

    @Specialization(guards = {"emptyValue", "isPosZero"})
    protected RAbstractVector updatePosZero(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        }
        return vector;
    }

    @Specialization(guards = {"emptyValue", "!isPosZero", "!isPosNA", "!isVectorList"})
    protected RAbstractVector update(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
    }

    @Specialization(guards = "!isVectorLongerThanOne")
    protected RAbstractVector updateVectorLongerThanOne(Object v, RNull value, int recLevel, RNull position, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @Specialization(guards = "isVectorLongerThanOne")
    protected RAbstractVector update(Object v, RNull value, int recLevel, RNull position, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @Specialization
    protected RAbstractVector update(Object v, RAbstractVector value, int recLevel, RNull position, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @Specialization(guards = {"isPosNA", "isValueLengthOne", "isVectorLongerThanOne"})
    protected RAbstractVector updateNAValueLengthOneLongVector(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else {
            return vector;
        }
    }

    @Specialization(guards = {"isPosNA", "isValueLengthOne", "!isVectorLongerThanOne"})
    protected RAbstractVector updateNAValueLengthOne(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            return vector;
        }
    }

    @Specialization(guards = {"isPosNA", "!isValueLengthOne"})
    protected RAbstractVector updateNA(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        if (isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NA_SUBSCRIPTED);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
    }

    @Specialization(guards = {"isPosZero", "isValueLengthOne"})
    protected RAbstractVector updateZeroValueLengthOne(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            return vector;
        }
    }

    @Specialization(guards = {"isPosZero", "!isValueLengthOne"})
    protected RAbstractVector updateZero(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            return vector;
        }
    }

    private int getSrcArrayBase(int pos, int accSrcDimensions) {
        if (posNACheck.check(pos)) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NA_SUBSCRIPTED);
        } else {
            return accSrcDimensions * (pos - 1);
        }
    }

    private final BranchProfile handleNa = BranchProfile.create();

    private int getReplacementLength(Object[] positions, RAbstractContainer value, boolean isList) {
        int valueLength = value.getLength();
        int length = 1;
        boolean seenNA = false;
        for (int i = 0; i < positions.length; i++) {
            RIntVector p = (RIntVector) positions[i];
            int len = p.getLength();
            posNACheck.enable(p);
            boolean allZeros = true;
            for (int j = 0; j < len; j++) {
                int pos = p.getDataAt(j);
                if (pos != 0) {
                    allZeros = false;
                    if (posNACheck.check(pos)) {
                        if (len == 1) {
                            handleNaMultiDim(value, isList, isSubset, getEncapsulatingSourceSection());
                        } else {
                            seenNA = true;
                        }
                    }
                }
            }
            length = allZeros ? 0 : length * p.getLength();
        }
        if (valueLength != 0 && length != 0 && length % valueLength != 0) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NOT_MULTIPLE_REPLACEMENT);
        } else if (seenNA) {
            handleNa.enter();
            handleNaMultiDim(value, isList, isSubset, getEncapsulatingSourceSection());
        }
        return length;
    }

    private int getHighestPos(RIntVector positions) {
        int highestPos = 0;
        posNACheck.enable(positions);
        int numNAs = 0;
        for (int i = 0; i < positions.getLength(); i++) {
            int pos = positions.getDataAt(i);
            if (posNACheck.check(pos)) {
                // ignore
                numNAs++;
                continue;
            } else if (pos < 0) {
                if (-pos > highestPos) {
                    highestPos = -pos;
                }
            } else if (pos > highestPos) {
                highestPos = pos;
            }
        }
        return numNAs == positions.getLength() ? numNAs : highestPos;
    }

    private RStringVector getNamesVector(RVector resultVector) {
        if (noResultNames.profile(resultVector.getNames() == RNull.instance)) {
            String[] namesData = new String[resultVector.getLength()];
            Arrays.fill(namesData, RRuntime.NAMES_ATTR_EMPTY_VALUE);
            RStringVector names = RDataFactory.createStringVector(namesData, RDataFactory.COMPLETE_VECTOR);
            resultVector.setNames(names);
            return names;
        } else {
            return (RStringVector) resultVector.getNames();
        }
    }

    private final BranchProfile posNames = BranchProfile.create();

    private void updateNames(RVector resultVector, RIntVector positions) {
        if (positions.getNames() != RNull.instance) {
            posNames.enter();
            RStringVector names = getNamesVector(resultVector);
            RStringVector newNames = (RStringVector) positions.getNames();
            namesNACheck.enable(newNames);
            for (int i = 0; i < positions.getLength(); i++) {
                int p = positions.getDataAt(i);
                names.updateDataAt(p - 1, newNames.getDataAt(i), namesNACheck);
            }
        }
    }

    // null

    @Specialization
    protected RNull updateWrongDimensions(Object v, RNull value, int recLevel, Object[] positions, RNull vector) {
        return vector;
    }

    @Specialization(guards = {"!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RNull updateWrongDimensions(Object v, RAbstractVector value, int recLevel, Object[] positions, RNull vector) {
        return vector;
    }

    @Specialization(guards = "emptyValue")
    protected RNull updatePosZero(Object v, RAbstractVector value, int recLevel, int position, RNull vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        }
        return vector;
    }

    @Specialization(guards = "emptyValue")
    protected RNull updatePosZero(Object v, RAbstractVector value, int recLevel, RIntVector positions, RNull vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        }
        return vector;
    }

    @Specialization(guards = "!emptyValue")
    protected RIntVector update(Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RNull vector) {
        int highestPos = getHighestPos(positions);
        int[] data = new int[highestPos];
        Arrays.fill(data, RRuntime.INT_NA);
        return updateSingleDimVector(value, 0, RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR), positions);
    }

    @Specialization(guards = {"!emptyValue", "!isPosNA", "!isPosZero"})
    protected RIntVector update(Object v, RAbstractIntVector value, int recLevel, int position, RNull vector) {
        if (multiPosProfile.profile(position > 1)) {
            int[] data = new int[position];
            Arrays.fill(data, RRuntime.INT_NA);
            return updateSingleDim(value, RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR), position);
        } else {
            return updateSingleDim(value, RDataFactory.createIntVector(position), position);
        }
    }

    @Specialization(guards = "!emptyValue")
    protected RDoubleVector update(Object v, RAbstractDoubleVector value, int recLevel, RIntVector positions, RNull vector) {
        int highestPos = getHighestPos(positions);
        double[] data = new double[highestPos];
        Arrays.fill(data, RRuntime.DOUBLE_NA);
        return updateSingleDimVector(value, 0, RDataFactory.createDoubleVector(data, RDataFactory.INCOMPLETE_VECTOR), positions);
    }

    @Specialization(guards = {"!emptyValue", "!isPosNA", "!isPosZero"})
    protected RDoubleVector update(Object v, RAbstractDoubleVector value, int recLevel, int position, RNull vector) {
        if (multiPosProfile.profile(position > 1)) {
            double[] data = new double[position];
            Arrays.fill(data, RRuntime.DOUBLE_NA);
            return updateSingleDim(value, RDataFactory.createDoubleVector(data, RDataFactory.INCOMPLETE_VECTOR), position);
        } else {
            return updateSingleDim(value, RDataFactory.createDoubleVector(position), position);
        }
    }

    @Specialization(guards = "!emptyValue")
    protected RLogicalVector update(Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RNull vector) {
        int highestPos = getHighestPos(positions);
        byte[] data = new byte[highestPos];
        Arrays.fill(data, RRuntime.LOGICAL_NA);
        return updateSingleDimVector(value, 0, RDataFactory.createLogicalVector(data, RDataFactory.INCOMPLETE_VECTOR), positions);
    }

    @Specialization(guards = {"!emptyValue", "!isPosNA", "!isPosZero"})
    protected RLogicalVector update(Object v, RAbstractLogicalVector value, int recLevel, int position, RNull vector) {
        if (multiPosProfile.profile(position > 1)) {
            byte[] data = new byte[position];
            Arrays.fill(data, RRuntime.LOGICAL_NA);
            return updateSingleDim(value, RDataFactory.createLogicalVector(data, RDataFactory.INCOMPLETE_VECTOR), position);
        } else {
            return updateSingleDim(value, RDataFactory.createLogicalVector(position), position);
        }
    }

    @Specialization(guards = "!emptyValue")
    protected RStringVector update(Object v, RAbstractStringVector value, int recLevel, RIntVector positions, RNull vector) {
        int highestPos = getHighestPos(positions);
        String[] data = new String[highestPos];
        Arrays.fill(data, RRuntime.STRING_NA);
        return updateSingleDimVector(value, 0, RDataFactory.createStringVector(data, RDataFactory.INCOMPLETE_VECTOR), positions);
    }

    @Specialization(guards = {"!emptyValue", "!isPosNA", "!isPosZero"})
    protected RStringVector update(Object v, RAbstractStringVector value, int recLevel, int position, RNull vector) {
        if (multiPosProfile.profile(position > 1)) {
            String[] data = new String[position];
            Arrays.fill(data, RRuntime.STRING_NA);
            return updateSingleDim(value, RDataFactory.createStringVector(data, RDataFactory.INCOMPLETE_VECTOR), position);
        } else {
            return updateSingleDim(value, RDataFactory.createStringVector(position), position);
        }
    }

    @Specialization(guards = "!emptyValue")
    protected RComplexVector update(Object v, RAbstractComplexVector value, int recLevel, RIntVector positions, RNull vector) {
        int highestPos = getHighestPos(positions);
        double[] data = new double[highestPos << 1];
        int ind = 0;
        for (int i = 0; i < highestPos; i++) {
            data[ind++] = RRuntime.COMPLEX_NA_REAL_PART;
            data[ind++] = RRuntime.COMPLEX_NA_IMAGINARY_PART;
        }
        return updateSingleDimVector(value, 0, RDataFactory.createComplexVector(data, RDataFactory.INCOMPLETE_VECTOR), positions);
    }

    @Specialization(guards = {"!emptyValue", "!isPosNA", "!isPosZero"})
    protected RComplexVector update(VirtualFrame frame, Object v, RAbstractComplexVector value, int recLevel, int position, RNull vector) {
        if (multiPosProfile.profile(position > 1)) {
            double[] data = new double[position << 1];
            int ind = 0;
            for (int i = 0; i < position; i++) {
                data[ind++] = RRuntime.COMPLEX_NA_REAL_PART;
                data[ind++] = RRuntime.COMPLEX_NA_IMAGINARY_PART;
            }
            return updateSingleDim(frame, value, RDataFactory.createComplexVector(data, RDataFactory.INCOMPLETE_VECTOR), position);
        } else {
            return updateSingleDim(frame, value, RDataFactory.createComplexVector(position), position);
        }
    }

    @Specialization(guards = "!emptyValue")
    protected RRawVector update(Object v, RAbstractRawVector value, int recLevel, RIntVector positions, RNull vector) {
        return updateSingleDimVector(value, 0, RDataFactory.createRawVector(getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!emptyValue", "!isPosNA", "!isPosZero"})
    protected RRawVector update(Object v, RAbstractRawVector value, int recLevel, int position, RNull vector) {
        return updateSingleDim(value, RDataFactory.createRawVector(position), position);
    }

    @Specialization(guards = {"!isPosNA", "isPositionNegative", "!isVectorList"})
    protected RList updateNegativeNull(Object v, RNull value, int recLevel, int position, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
    }

    @Specialization(guards = {"!isPosNA", "isPositionNegative", "!outOfBoundsNegative"})
    protected RList updateNegativeNull(Object v, RNull value, int recLevel, int position, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @Specialization(guards = {"!isPosNA", "isPositionNegative", "outOfBoundsNegative", "oneElemVector"})
    protected RList updateNegativeOutOfBoundsOneElemNull(Object v, RNull value, int recLevel, int position, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @Specialization(guards = {"!isPosNA", "isPositionNegative", "outOfBoundsNegative", "!oneElemVector"})
    protected RList updateNegativeOutOfBoundsNull(Object v, RNull value, int recLevel, int position, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @Specialization(guards = {"!isPosNA", "isPositionNegative", "!outOfBoundsNegative"})
    protected RList updateNegative(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @Specialization(guards = {"!isPosNA", "isPositionNegative", "outOfBoundsNegative", "oneElemVector"})
    protected RList updateNegativeOneElem(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @Specialization(guards = {"!isPosNA", "isPositionNegative", "outOfBoundsNegative", "!oneElemVector"})
    protected RList updateOutOfBoundsNegative(Object v, RAbstractVector value, int recLevel, int position, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    // list

    private RList updateVector(VirtualFrame frame, RAbstractContainer value, RList vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value, true);
        RList resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            vectorShared.enter();
            resultVector = (RList) vector.copy();
            resultVector.markNonTemporary();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        posNACheck.enable(p);
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            if (posNACheck.check(pos)) {
                handleNaMultiDim(value, true, isSubset, getEncapsulatingSourceSection());
                continue;
            }
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setMultiDimData(frame, value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
        }
        return resultVector;
    }

    private RList getResultVector(RList vector, int highestPos, boolean resetDims) {
        RList resultVector = vector;
        if (resultVector.isShared()) {
            vectorShared.enter();
            resultVector = (RList) vector.copy();
            resultVector.markNonTemporary();
        }
        if (resultVector.getLength() < highestPos) {
            vectorTooShort.enter();
            int orgLength = resultVector.getLength();
            resultVector.resizeWithNames(highestPos);
            for (int i = orgLength; i < highestPos; i++) {
                resultVector.updateDataAt(i, RNull.instance, null);
            }
        } else if (resetDims) {
            vectorNoDims.enter();
            resultVector.setDimensions(null);
            resultVector.setDimNames(null);
        }
        return resultVector;
    }

    private int getPositionInRecursion(RList vector, int position, int recLevel, boolean lastPos) {
        if (RRuntime.isNA(position)) {
            error.enter();
            if (lastPos && recLevel > 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
            } else if (recLevel == 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_SUCH_INDEX, recLevel + 1);
            }
        } else if (!lastPos && position > vector.getLength()) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_SUCH_INDEX, recLevel + 1);
        } else if (position < 0) {
            error.enter();
            return AccessArrayNode.getPositionFromNegative(vector, position, getEncapsulatingSourceSection(), error);
        } else if (position == 0) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        return position;
    }

    private static RList updateSingleDim(RAbstractContainer value, RList resultVector, int position) {
        resultVector.updateDataAt(position - 1, value.getDataAtAsObject(0), null);
        return resultVector;
    }

    private final BranchProfile valShared = BranchProfile.create();
    private final BranchProfile valNonTmp = BranchProfile.create();
    private final BranchProfile valTmp = BranchProfile.create();

    // this is similar to what happens on "regular" (non-vector) assignment - the state has to
    // change to avoid erroneous sharing
    private RShareable adjustRhsStateOnAssignment(RAbstractContainer value) {
        RShareable val = value.materializeToShareable();
        if (val.isShared()) {
            valShared.enter();
            val = val.copy();
        } else if (!val.isTemporary()) {
            valNonTmp.enter();
            val.makeShared();
        } else if (val.isTemporary()) {
            valTmp.enter();
            val.markNonTemporary();
        }
        return val;
    }

    private RList updateSingleDimRec(RAbstractContainer value, RList resultVector, RIntVector p, int recLevel) {
        int position = getPositionInRecursion(resultVector, p.getDataAt(0), recLevel, true);
        resultVector.updateDataAt(position - 1, adjustRhsStateOnAssignment(value), null);
        updateNames(resultVector, p);
        return resultVector;
    }

    private RList updateSingleDimVector(RAbstractContainer value, int orgVectorLength, RList resultVector, RIntVector positions) {
        if (positions.getLength() == 1 && value.getLength() > 1) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            if (seenNaOrNegative(p, value)) {
                continue;
            }
            if (negativePosProfile.profile(p < 0 && -(p + 1) >= orgVectorLength)) {
                resultVector.updateDataAt(-(p + 1), RNull.instance, null);
            } else {
                resultVector.updateDataAt(p - 1, value.getDataAtAsObject(i % value.getLength()), null);
            }
        }
        if (positions.getLength() % value.getLength() != 0) {
            warning.enter();
            RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        }
        updateNames(resultVector, positions);
        return resultVector;
    }

    private Object updateListRecursive(VirtualFrame frame, Object v, Object value, RList vector, int recLevel, RStringVector p) {
        int position = AccessArrayNode.getPositionInRecursion(vector, p.getDataAt(0), recLevel, getEncapsulatingSourceSection(), error);
        if (p.getLength() == 2 && RRuntime.isNA(p.getDataAt(1))) {
            // catch it here, otherwise it will get caught at lower level of recursion resulting in
            // a different message
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        Object el;
        RList resultList = vector;
        if (recLevel == 0) {
            recLevelZero.enter();
            // TODO: does it matter to make it smarter (mark nested lists as shared during copy?)
            resultList = (RList) vector.deepCopy();
        }
        if (twoPosProfile.profile(p.getLength() == 2)) {
            Object finalVector = coerceVector(frame, resultList.getDataAt(position - 1), value, p);
            Object lastPosition = castPosition(frame, finalVector, convertOperand(frame, finalVector, p.getDataAt(1)));
            el = updateRecursive(frame, v, value, finalVector, lastPosition, recLevel + 1);
        } else {
            RStringVector newP = AccessArrayNode.popHead(p, posNACheck);
            el = updateRecursive(frame, v, value, resultList.getDataAt(position - 1), newP, recLevel + 1);
        }

        resultList.updateDataAt(position - 1, el, null);
        return resultList;
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RList update(VirtualFrame frame, Object v, RAbstractContainer value, int recLevel, Object[] positions, RList vector) {
        return updateVector(frame, value, vector, positions);
    }

    @Specialization
    protected Object updateString(VirtualFrame frame, Object v, RNull value, int recLevel, RStringVector positions, RList vector) {
        return updateListRecursive(frame, v, value, vector, recLevel, positions);
    }

    @Specialization
    protected Object updateString(VirtualFrame frame, Object v, RAbstractContainer value, int recLevel, RStringVector positions, RList vector) {
        return updateListRecursive(frame, v, value, vector, recLevel, positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RList update(Object v, RAbstractContainer value, int recLevel, RIntVector positions, RList vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions), false), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateOne(VirtualFrame frame, Object v, RAbstractContainer value, int recLevel, RIntVector positions, RList vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RList updateNames(Object v, RAbstractContainer value, int recLevel, RIntVector positions, RList vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions), false), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero", "!isPositionNegative"})
    protected RList updateTooManyValuesSubset(Object v, RAbstractContainer value, int recLevel, int position, RList vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position, false), position);
    }

    @Specialization(guards = {"isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero", "!isPositionNegative"})
    protected RList update(Object v, RAbstractContainer value, int recLevel, int position, RList vector) {
        return updateSingleDim(value, getResultVector(vector, position, false), position);
    }

    @Specialization(guards = {"!isSubset", "!isPosNA", "!isPosZero", "!isPositionNegative"})
    protected RList updateTooManyValuesSubscript(Object v, RAbstractContainer value, int recLevel, int position, RList vector) {
        RList resultVector = getResultVector(vector, position, false);
        resultVector.updateDataAt(position - 1, adjustRhsStateOnAssignment(value), null);
        return resultVector;
    }

    @Specialization(guards = "isPosNA")
    protected RList updateListNullValue(Object v, RNull value, int recLevel, int position, RList vector) {
        return vector;
    }

    @Specialization(guards = {"!isPosZero", "emptyList", "!isPosNA", "!isPositionNegative"})
    protected RList updateEmptyList(Object v, RNull value, int recLevel, int position, RList vector) {
        return vector;
    }

    private RList removeElement(RList vector, int position, boolean inRecursion, boolean resetDims) {
        if (position > vector.getLength()) {
            vectorTooShort.enter();
            if (inRecursion || !isSubset) {
                // simply return the vector unchanged
                return vector;
            } else {
                // this is equivalent to extending the vector to appropriate length and then
                // removing the last element
                return getResultVector(vector, position - 1, resetDims);
            }
        }
        Object[] data = new Object[vector.getLength() - 1];
        RStringVector orgNames = null;
        String[] namesData = null;
        if (vector.getNames() != RNull.instance) {
            namesData = new String[vector.getLength() - 1];
            orgNames = (RStringVector) vector.getNames();
        }

        int ind = 0;
        for (int i = 0; i < vector.getLength(); i++) {
            if (i != (position - 1)) {
                data[ind] = vector.getDataAt(i);
                if (orgNames != null) {
                    namesData[ind] = orgNames.getDataAt(i);
                }
                ind++;
            }
        }

        RList result = RDataFactory.createList(data, orgNames == null ? null : RDataFactory.createStringVector(namesData, vector.isComplete()));
        result.copyRegAttributesFrom(vector);
        return result;
    }

    @Specialization(guards = {"!isPosZero", "!emptyList", "!isPosNA", "!isPositionNegative"})
    protected RList update(Object v, RNull value, int recLevel, int position, RList vector) {
        return removeElement(vector, position, false, isSubset);
    }

    private static final Object DELETE_MARKER = new Object();

    @Specialization(guards = {"isSubset", "noPosition"})
    protected RList updateEmptyPos(Object v, RNull value, int recLevel, RIntVector positions, RList vector) {
        return vector;
    }

    @Specialization(guards = {"isSubset", "!noPosition"})
    protected RList update(Object v, RNull value, int recLevel, RIntVector positions, RList vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }

        RList list = vector;
        if (list.isShared()) {
            vectorShared.enter();
            list = (RList) vector.copy();
            list.markNonTemporary();
        }
        int highestPos = getHighestPos(positions);
        if (list.getLength() < highestPos) {
            vectorTooShort.enter();
            // to mark duplicate deleted elements with positions > vector length
            list = list.copyResized(highestPos, false);
        }
        int posDeleted = 0;
        for (int i = 0; i < positions.getLength(); i++) {
            int pos = positions.getDataAt(i);
            if (RRuntime.isNA(pos) || pos < 0) {
                continue;
            }
            if (list.getDataAt(pos - 1) != DELETE_MARKER) {
                list.updateDataAt(pos - 1, DELETE_MARKER, null);
                // count each position only once
                posDeleted++;
            }
        }
        int resultVectorLength = highestPos > list.getLength() ? highestPos - posDeleted : list.getLength() - posDeleted;
        Object[] data = new Object[resultVectorLength];

        RList result;
        if (noResultNames.profile(vector.getNames() == RNull.instance)) {
            int ind = 0;
            for (int i = 0; i < vector.getLength(); i++) {
                Object el = list.getDataAt(i);
                if (el != DELETE_MARKER) {
                    data[ind] = el;
                    ind++;
                }
            }
            Arrays.fill(data, ind, data.length, RNull.instance);
            result = RDataFactory.createList(data, null);
        } else {
            String[] namesData = new String[resultVectorLength];
            RStringVector orgNames = (RStringVector) vector.getNames();
            int ind = 0;
            for (int i = 0; i < vector.getLength(); i++) {
                Object el = list.getDataAt(i);
                if (el != DELETE_MARKER) {
                    data[ind] = el;
                    namesData[ind] = orgNames.getDataAt(i);
                    ind++;
                }
            }
            Arrays.fill(data, ind, data.length, RNull.instance);
            Arrays.fill(namesData, ind, data.length, RRuntime.NAMES_ATTR_EMPTY_VALUE);
            result = RDataFactory.createList(data, RDataFactory.createStringVector(namesData, orgNames.isComplete()));
        }
        result.copyRegAttributesFrom(vector);
        return result;
    }

    private Object updateListRecursive(VirtualFrame frame, Object v, Object value, RList vector, int recLevel, RIntVector p) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, false);
        if (p.getLength() == 2 && RRuntime.isNA(p.getDataAt(1))) {
            // catch it here, otherwise it will get caught at lower level of recursion resulting in
            // a different message
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        RList resultList = vector;
        if (recLevel == 0) {
            // TODO: does it matter to make it smarter (mark nested lists as shared during copy?)
            recLevelZero.enter();
            resultList = (RList) vector.deepCopy();
        }
        Object el;
        if (twoPosProfile.profile(p.getLength() == 2)) {
            Object finalVector = coerceVector(frame, resultList.getDataAt(position - 1), value, p);
            Object lastPosition = castPosition(frame, finalVector, convertOperand(frame, finalVector, p.getDataAt(1)));
            el = updateRecursive(frame, v, value, finalVector, lastPosition, recLevel + 1);
        } else {
            RIntVector newP = AccessArrayNode.popHead(p, posNACheck);
            el = updateRecursive(frame, v, value, resultList.getDataAt(position - 1), newP, recLevel + 1);
        }

        resultList.updateDataAt(position - 1, el, null);
        return resultList;
    }

    @Specialization(guards = {"!isSubset", "multiPos"})
    protected Object access(VirtualFrame frame, Object v, RNull value, int recLevel, RIntVector p, RList vector) {
        return updateListRecursive(frame, v, value, vector, recLevel, p);
    }

    @Specialization(guards = {"!isSubset", "multiPos"})
    protected Object access(VirtualFrame frame, Object v, RAbstractContainer value, int recLevel, RIntVector p, RList vector) {
        return updateListRecursive(frame, v, value, vector, recLevel, p);
    }

    @Specialization(guards = {"!isSubset", "inRecursion", "multiPos"})
    protected Object accessRecFailed(Object v, RAbstractContainer value, int recLevel, RIntVector p, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.RECURSIVE_INDEXING_FAILED, recLevel + 1);
    }

    @Specialization(guards = {"!isSubset", "!multiPos"})
    protected Object accessSubscriptListValue(Object v, RList value, int recLevel, RIntVector p, RList vector) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, true);
        return updateSingleDimRec(value, getResultVector(vector, position, false), p, recLevel);
    }

    @Specialization(guards = {"!isSubset", "inRecursion", "!multiPos"})
    protected Object accessSubscriptNullValueInRecursion(Object v, RNull value, int recLevel, RIntVector p, RList vector) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, true);
        return removeElement(vector, position, true, false);
    }

    @Specialization(guards = {"!isSubset", "!inRecursion", "!multiPos"})
    protected Object accessSubscriptNullValue(Object v, RNull value, int recLevel, RIntVector p, RList vector) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, true);
        return removeElement(vector, position, false, false);
    }

    @Specialization(guards = {"!isSubset", "!multiPos"})
    protected Object accessSubscript(Object v, RAbstractContainer value, int recLevel, RIntVector p, RList vector) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, true);
        return updateSingleDimRec(value, getResultVector(vector, position, false), p, recLevel);
    }

    @Specialization(guards = {"!isValueLengthOne", "!emptyValue", "!isSubset", "!isPosNA", "!isPosZero"})
    protected RAbstractVector updateTooManyValues(Object v, RAbstractContainer value, int recLevel, int position, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
    }

    // null value (with vectors)

    @Specialization(guards = {"isPosZero", "!isVectorList"})
    protected RAbstractVector updatePosZero(Object v, RNull value, int recLevel, int position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        }
        return vector;
    }

    @Specialization(guards = {"!isPosZero", "!isPosNA", "!isVectorList"})
    protected RAbstractVector update(Object v, RNull value, int recLevel, int position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        }
    }

    @Specialization(guards = {"isSubset", "!isVectorList", "noPosition"})
    protected RAbstractVector updateNullSubsetNoPos(Object v, RNull value, int recLevel, RIntVector positions, RAbstractVector vector) {
        return vector;
    }

    @Specialization(guards = {"isSubset", "!isVectorList", "!noPosition"})
    protected RAbstractVector updateNullSubset(Object v, RNull value, int recLevel, RIntVector positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
    }

    @Specialization(guards = {"!isSubset", "!isVectorList", "noPosition"})
    protected RAbstractVector updateNullNoPos(Object v, RNull value, int recLevel, RIntVector positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
    }

    @Specialization(guards = {"!isSubset", "!isVectorList", "onePosition"})
    protected RAbstractVector updateNullOnePos(Object v, RNull value, int recLevel, RIntVector positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
    }

    @Specialization(guards = {"!isSubset", "!isVectorList", "twoPositions", "firstPosZero"})
    protected RAbstractVector updateNullTwoElemsZero(Object v, RNull value, int recLevel, RIntVector positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @Specialization(guards = {"!isSubset", "!isVectorList", "twoPositions", "!firstPosZero"})
    protected RAbstractVector updateNullTwoElems(Object v, RNull value, int recLevel, RIntVector positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @Specialization(guards = {"!isSubset", "!isVectorList", "multiPos"})
    protected RAbstractVector updateNull(Object v, RNull value, int recLevel, RIntVector positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    // int vector

    private RIntVector updateVector(VirtualFrame frame, RAbstractIntVector value, RAbstractIntVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value, false);
        RIntVector resultVector = vector.materialize();
        if (replacementLength == 0) {
            return resultVector;
        }
        if (resultVector.isShared()) {
            vectorShared.enter();
            resultVector = (RIntVector) vector.copy();
            resultVector.markNonTemporary();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(value);
        posNACheck.enable(p);
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            if (posNACheck.check(pos)) {
                handleNaMultiDim(value, false, isSubset, getEncapsulatingSourceSection());
                continue;
            }
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setMultiDimData(frame, value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
        }
        return resultVector;
    }

    private RIntVector getResultVector(RAbstractIntVector vector, int highestPos) {
        RIntVector resultVector = vector.materialize();
        if (resultVector.isShared()) {
            vectorShared.enter();
            resultVector = (RIntVector) vector.copy();
            resultVector.markNonTemporary();
        }
        if (resultVector.getLength() < highestPos) {
            vectorTooShort.enter();
            resultVector.resizeWithNames(highestPos);
        }
        return resultVector;
    }

    private RIntVector updateSingleDim(RAbstractIntVector value, RIntVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private ConditionProfile naOrNegativeProfile = ConditionProfile.createBinaryProfile();

    private boolean seenNaOrNegative(int p, RAbstractContainer value) {
        if (naOrNegativeProfile.profile(posNACheck.check(p) || p < 0)) {
            if (value.getLength() == 1) {
                return true;
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NA_SUBSCRIPTED);
            }
        } else {
            return false;
        }
    }

    protected static void handleNaMultiDim(RAbstractContainer value, boolean isList, boolean isSubset, SourceSection sourceSection) {
        if (value.getLength() == 1) {
            if (!isSubset) {
                throw RError.error(sourceSection, RError.Message.SUBSCRIPT_BOUNDS_SUB);
            }
        } else {
            if (!isSubset) {
                if (isList) {
                    throw RError.error(sourceSection, RError.Message.SUBSCRIPT_BOUNDS_SUB);
                } else {
                    throw RError.error(sourceSection, RError.Message.MORE_SUPPLIED_REPLACE);
                }
            } else {
                throw RError.error(sourceSection, RError.Message.NA_SUBSCRIPTED);
            }
        }
    }

    private RIntVector updateSingleDimVector(RAbstractIntVector value, int orgVectorLength, RIntVector resultVector, RIntVector positions) {
        if (positions.getLength() == 1 && value.getLength() > 1) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
        elementNACheck.enable(value);
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            if (seenNaOrNegative(p, value)) {
                continue;
            }
            if (negativePosProfile.profile(p < 0 && -(p + 1) >= orgVectorLength)) {
                resultVector.updateDataAt(-(p + 1), RRuntime.INT_NA, elementNACheck);
            } else {
                resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
            }
        }
        if (positions.getLength() % value.getLength() != 0) {
            warning.enter();
            RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        }
        updateNames(resultVector, positions);
        return resultVector;
    }

    @Specialization(guards = {"!isSubset", "!isVectorList", "!posNames", "!twoPositions"})
    protected Object update(Object v, RAbstractVector value, int recLevel, RIntVector positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @Specialization(guards = {"!isSubset", "!isVectorList", "!posNames", "twoPositions", "firstPosZero"})
    protected RList updateTwoElemsZero(Object v, RAbstractVector value, int recLevel, RIntVector positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @Specialization(guards = {"!isSubset", "!isVectorList", "!posNames", "twoPositions", "!firstPosZero"})
    protected RList updateTwoElems(Object v, RAbstractVector value, int recLevel, RIntVector positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RIntVector update(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, Object[] positions, RAbstractIntVector vector) {
        return updateVector(frame, value, vector, positions);
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RIntVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, Object[] positions, RAbstractIntVector vector) {
        return updateVector(frame, (RIntVector) castInteger(frame, value), vector, positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractIntVector updateSubset(Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RAbstractIntVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RAbstractIntVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractIntVector updateSubsetNames(Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RAbstractIntVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractIntVector update(Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RAbstractIntVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RIntVector updateTooManyValuesSubset(Object v, RAbstractIntVector value, int recLevel, int position, RAbstractIntVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RIntVector update(Object v, RAbstractIntVector value, int recLevel, int position, RAbstractIntVector vector) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractIntVector updateSubset(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RAbstractIntVector vector) {
        return updateSingleDimVector((RIntVector) castInteger(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RAbstractIntVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractIntVector updateSubsetNames(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RAbstractIntVector vector) {
        return updateSingleDimVector((RIntVector) castInteger(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractIntVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RAbstractIntVector vector) {
        return updateSingleDimVector((RIntVector) castInteger(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RIntVector updateTooManyValuesSubset(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, int position, RAbstractIntVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim((RIntVector) castInteger(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RIntVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, int position, RAbstractIntVector vector) {
        return updateSingleDim((RIntVector) castInteger(frame, value), getResultVector(vector, position), position);
    }

    // double vector

    private RDoubleVector updateVector(VirtualFrame frame, RAbstractDoubleVector value, RAbstractDoubleVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value, false);
        RDoubleVector resultVector = vector.materialize();
        if (replacementLength == 0) {
            return resultVector;
        }
        if (resultVector.isShared()) {
            vectorShared.enter();
            resultVector = (RDoubleVector) vector.copy();
            resultVector.markNonTemporary();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(value);
        posNACheck.enable(p);
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            if (posNACheck.check(pos)) {
                handleNaMultiDim(value, false, isSubset, getEncapsulatingSourceSection());
                continue;
            }
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setMultiDimData(frame, value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
        }
        return resultVector;
    }

    private RDoubleVector getResultVector(RAbstractDoubleVector vector, int highestPos) {
        RDoubleVector resultVector = vector.materialize();
        if (resultVector.isShared()) {
            vectorShared.enter();
            resultVector = (RDoubleVector) vector.copy();
            resultVector.markNonTemporary();
        }
        if (resultVector.getLength() < highestPos) {
            vectorTooShort.enter();
            resultVector.resizeWithNames(highestPos);
        }
        return resultVector;
    }

    private RDoubleVector updateSingleDim(RAbstractDoubleVector value, RDoubleVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private RDoubleVector updateSingleDimVector(RAbstractDoubleVector value, int orgVectorLength, RDoubleVector resultVector, RIntVector positions) {
        if (positions.getLength() == 1 && value.getLength() > 1) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
        elementNACheck.enable(value);
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            if (seenNaOrNegative(p, value)) {
                continue;
            }
            if (negativePosProfile.profile(p < 0 && -(p + 1) >= orgVectorLength)) {
                resultVector.updateDataAt(-(p + 1), RRuntime.DOUBLE_NA, elementNACheck);
            } else {
                resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
            }
        }
        if (value.getLength() == 0) {
            Utils.nyi();
        }
        if (positions.getLength() % value.getLength() != 0) {
            warning.enter();
            RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        }
        updateNames(resultVector, positions);
        return resultVector;
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RDoubleVector update(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, Object[] positions, RAbstractDoubleVector vector) {
        return updateVector(frame, (RDoubleVector) castDouble(frame, value), vector, positions);
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RDoubleVector update(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, Object[] positions, RAbstractDoubleVector vector) {
        return updateVector(frame, value, vector, positions);
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RDoubleVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, Object[] positions, RAbstractDoubleVector vector) {
        return updateVector(frame, (RDoubleVector) castDouble(frame, value), vector, positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractDoubleVector updateSubset(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateSingleDimVector((RDoubleVector) castDouble(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractDoubleVector updateSubsetNames(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateSingleDimVector((RDoubleVector) castDouble(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractDoubleVector update(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateSingleDimVector((RDoubleVector) castDouble(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RDoubleVector updateTooManyValuesSubset(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, int position, RAbstractDoubleVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim((RDoubleVector) castDouble(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RDoubleVector update(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, int position, RAbstractDoubleVector vector) {
        return updateSingleDim((RDoubleVector) castDouble(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractDoubleVector updateSubset(Object v, RAbstractDoubleVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractDoubleVector updateSubsetNames(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateSingleDimVector((RDoubleVector) castDouble(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractDoubleVector update(Object v, RAbstractDoubleVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RDoubleVector updateTooManyValuesSubset(Object v, RAbstractDoubleVector value, int recLevel, int position, RAbstractDoubleVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RDoubleVector update(Object v, RAbstractDoubleVector value, int recLevel, int position, RAbstractDoubleVector vector) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractDoubleVector updateSubset(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateSingleDimVector((RDoubleVector) castDouble(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractDoubleVector updateSubsetNames(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateSingleDimVector((RDoubleVector) castDouble(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractDoubleVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RAbstractDoubleVector vector) {
        return updateSingleDimVector((RDoubleVector) castDouble(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RDoubleVector updateTooManyValuesSubset(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, int position, RAbstractDoubleVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim((RDoubleVector) castDouble(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RDoubleVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, int position, RAbstractDoubleVector vector) {
        return updateSingleDim((RDoubleVector) castDouble(frame, value), getResultVector(vector, position), position);
    }

    // logical vector

    private RLogicalVector updateVector(VirtualFrame frame, RAbstractLogicalVector value, RLogicalVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value, false);
        RLogicalVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            vectorShared.enter();
            resultVector = (RLogicalVector) vector.copy();
            resultVector.markNonTemporary();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(value);
        posNACheck.enable(p);
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            if (posNACheck.check(pos)) {
                handleNaMultiDim(value, false, isSubset, getEncapsulatingSourceSection());
                continue;
            }
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setMultiDimData(frame, value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
        }
        return resultVector;
    }

    private RLogicalVector getResultVector(RLogicalVector vector, int highestPos) {
        RLogicalVector resultVector = vector;
        if (vector.isShared()) {
            vectorShared.enter();
            resultVector = (RLogicalVector) vector.copy();
            resultVector.markNonTemporary();
        }
        if (resultVector.getLength() < highestPos) {
            vectorTooShort.enter();
            resultVector.resizeWithNames(highestPos);
        }
        return resultVector;
    }

    private RLogicalVector updateSingleDim(RAbstractLogicalVector value, RLogicalVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private RLogicalVector updateSingleDimVector(RAbstractLogicalVector value, int orgVectorLength, RLogicalVector resultVector, RIntVector positions) {
        if (positions.getLength() == 1 && value.getLength() > 1) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
        elementNACheck.enable(value);
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            if (seenNaOrNegative(p, value)) {
                continue;
            }
            if (negativePosProfile.profile(p < 0 && -(p + 1) >= orgVectorLength)) {
                resultVector.updateDataAt(-(p + 1), RRuntime.LOGICAL_NA, elementNACheck);
            } else {
                resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
            }
        }
        if (positions.getLength() % value.getLength() != 0) {
            warning.enter();
            RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        }
        updateNames(resultVector, positions);
        return resultVector;
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RLogicalVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, Object[] positions, RLogicalVector vector) {
        return updateVector(frame, value, vector, positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractLogicalVector updateSubset(Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RLogicalVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RLogicalVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractLogicalVector updateSubsetNames(Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RLogicalVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractLogicalVector update(Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RLogicalVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RLogicalVector updateTooManyValuesSubset(Object v, RAbstractLogicalVector value, int recLevel, int position, RLogicalVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RLogicalVector update(Object v, RAbstractLogicalVector value, int recLevel, int position, RLogicalVector vector) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    // string vector

    private RStringVector updateVector(VirtualFrame frame, RAbstractStringVector value, RStringVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value, false);
        RStringVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            vectorShared.enter();
            resultVector = (RStringVector) vector.copy();
            resultVector.markNonTemporary();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(value);
        posNACheck.enable(p);
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            if (posNACheck.check(pos)) {
                handleNaMultiDim(value, false, isSubset, getEncapsulatingSourceSection());
                continue;
            }
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setMultiDimData(frame, value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
        }
        return resultVector;
    }

    private RStringVector getResultVector(RStringVector vector, int highestPos) {
        RStringVector resultVector = vector;
        if (vector.isShared()) {
            vectorShared.enter();
            resultVector = (RStringVector) vector.copy();
            resultVector.markNonTemporary();
        }
        if (resultVector.getLength() < highestPos) {
            vectorTooShort.enter();
            resultVector.resizeWithNames(highestPos);
        }
        return resultVector;
    }

    private RStringVector updateSingleDim(RAbstractStringVector value, RStringVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private RStringVector updateSingleDimVector(RAbstractStringVector value, int orgVectorLength, RStringVector resultVector, RIntVector positions) {
        if (positions.getLength() == 1 && value.getLength() > 1) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
        elementNACheck.enable(value);
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            if (seenNaOrNegative(p, value)) {
                continue;
            }
            if (negativePosProfile.profile(p < 0 && -(p + 1) >= orgVectorLength)) {
                resultVector.updateDataAt(-(p + 1), RRuntime.STRING_NA, elementNACheck);
            } else {
                resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
            }
        }
        if (positions.getLength() % value.getLength() != 0) {
            warning.enter();
            RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        }
        updateNames(resultVector, positions);
        return resultVector;
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RStringVector update(VirtualFrame frame, Object v, RAbstractStringVector value, int recLevel, Object[] positions, RStringVector vector) {
        return updateVector(frame, value, vector, positions);
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RStringVector update(VirtualFrame frame, Object v, RAbstractVector value, int recLevel, Object[] positions, RStringVector vector) {
        return updateVector(frame, (RStringVector) castString(frame, value), vector, positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractStringVector updateSubset(Object v, RAbstractStringVector value, int recLevel, RIntVector positions, RStringVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractStringVector value, int recLevel, RIntVector positions, RStringVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractStringVector updateSubsetNames(Object v, RAbstractStringVector value, int recLevel, RIntVector positions, RStringVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractStringVector update(Object v, RAbstractStringVector value, int recLevel, RIntVector positions, RStringVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RStringVector updateTooManyValuesSubset(Object v, RAbstractStringVector value, int recLevel, int position, RStringVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RStringVector update(Object v, RAbstractStringVector value, int recLevel, int position, RStringVector vector) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractStringVector updateSubset(VirtualFrame frame, Object v, RAbstractVector value, int recLevel, RIntVector positions, RStringVector vector) {
        return updateSingleDimVector((RStringVector) castString(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractVector value, int recLevel, RIntVector positions, RStringVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractStringVector updateSubsetNames(VirtualFrame frame, Object v, RAbstractVector value, int recLevel, RIntVector positions, RStringVector vector) {
        return updateSingleDimVector((RStringVector) castString(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractStringVector update(VirtualFrame frame, Object v, RAbstractVector value, int recLevel, RIntVector positions, RStringVector vector) {
        return updateSingleDimVector((RStringVector) castString(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RStringVector updateTooManyValuesSubset(VirtualFrame frame, Object v, RAbstractVector value, int recLevel, int position, RStringVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim((RStringVector) castString(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RStringVector update(VirtualFrame frame, Object v, RAbstractVector value, int recLevel, int position, RStringVector vector) {
        return updateSingleDim((RStringVector) castString(frame, value), getResultVector(vector, position), position);
    }

    // complex vector

    private RComplexVector updateVector(VirtualFrame frame, RAbstractComplexVector value, RComplexVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value, false);
        RComplexVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            vectorShared.enter();
            resultVector = (RComplexVector) vector.copy();
            resultVector.markNonTemporary();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        elementNACheck.enable(value);
        posNACheck.enable(p);
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            if (posNACheck.check(pos)) {
                handleNaMultiDim(value, false, isSubset, getEncapsulatingSourceSection());
                continue;
            }
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setMultiDimData(frame, value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
        }
        return resultVector;
    }

    private RComplexVector getResultVector(RComplexVector vector, int highestPos) {
        RComplexVector resultVector = vector;
        if (vector.isShared()) {
            vectorShared.enter();
            resultVector = (RComplexVector) vector.copy();
            resultVector.markNonTemporary();
        }
        if (resultVector.getLength() < highestPos) {
            vectorTooShort.enter();
            resultVector.resizeWithNames(highestPos);
        }
        return resultVector;
    }

    private RComplexVector updateSingleDim(VirtualFrame frame, RAbstractComplexVector value, RComplexVector resultVector, int position) {
        elementNACheck.enable(value);
        resultVector.updateDataAt(position - 1, value.getDataAt(0), elementNACheck);
        return resultVector;
    }

    private RComplexVector updateSingleDimVector(RAbstractComplexVector value, int orgVectorLength, RComplexVector resultVector, RIntVector positions) {
        if (positions.getLength() == 1 && value.getLength() > 1) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
        elementNACheck.enable(value);
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            if (seenNaOrNegative(p, value)) {
                continue;
            }
            if (negativePosProfile.profile(p < 0 && -(p + 1) >= orgVectorLength)) {
                resultVector.updateDataAt(-(p + 1), RRuntime.createComplexNA(), elementNACheck);
            } else {
                resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()), elementNACheck);
            }
        }
        if (positions.getLength() % value.getLength() != 0) {
            warning.enter();
            RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        }
        updateNames(resultVector, positions);
        return resultVector;
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RComplexVector update(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, Object[] positions, RComplexVector vector) {
        return updateVector(frame, (RComplexVector) castComplex(frame, value), vector, positions);
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RComplexVector update(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, Object[] positions, RComplexVector vector) {
        return updateVector(frame, (RComplexVector) castComplex(frame, value), vector, positions);
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RComplexVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, Object[] positions, RComplexVector vector) {
        return updateVector(frame, (RComplexVector) castComplex(frame, value), vector, positions);
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RComplexVector update(VirtualFrame frame, Object v, RAbstractComplexVector value, int recLevel, Object[] positions, RComplexVector vector) {
        return updateVector(frame, value, vector, positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractComplexVector updateSubset(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractComplexVector updateSubsetNames(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractComplexVector update(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RComplexVector updateTooManyValuesSubset(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, int position, RComplexVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(frame, (RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RComplexVector update(VirtualFrame frame, Object v, RAbstractIntVector value, int recLevel, int position, RComplexVector vector) {
        return updateSingleDim(frame, (RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractComplexVector updateSubset(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractComplexVector updateSubsetNames(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractComplexVector update(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RComplexVector updateTooManyValuesSubset(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, int position, RComplexVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(frame, (RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RComplexVector update(VirtualFrame frame, Object v, RAbstractDoubleVector value, int recLevel, int position, RComplexVector vector) {
        return updateSingleDim(frame, (RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractComplexVector updateSubset(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractComplexVector updateSubsetNames(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractComplexVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector((RComplexVector) castComplex(frame, value), vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RComplexVector updateTooManyValuesSubset(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, int position, RComplexVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(frame, (RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RComplexVector update(VirtualFrame frame, Object v, RAbstractLogicalVector value, int recLevel, int position, RComplexVector vector) {
        return updateSingleDim(frame, (RComplexVector) castComplex(frame, value), getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractComplexVector updateSubset(Object v, RAbstractComplexVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractComplexVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractComplexVector updateSubsetNames(Object v, RAbstractComplexVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractComplexVector update(Object v, RAbstractComplexVector value, int recLevel, RIntVector positions, RComplexVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RComplexVector updateTooManyValuesSubset(VirtualFrame frame, Object v, RAbstractComplexVector value, int recLevel, int position, RComplexVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(frame, value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RComplexVector update(VirtualFrame frame, Object v, RAbstractComplexVector value, int recLevel, int position, RComplexVector vector) {
        return updateSingleDim(frame, value, getResultVector(vector, position), position);
    }

    // raw vector

    private RRawVector updateVector(VirtualFrame frame, RAbstractRawVector value, RRawVector vector, Object[] positions) {
        int replacementLength = getReplacementLength(positions, value, false);
        RRawVector resultVector = vector;
        if (replacementLength == 0) {
            return resultVector;
        }
        if (vector.isShared()) {
            vectorShared.enter();
            resultVector = (RRawVector) vector.copy();
            resultVector.markNonTemporary();
        }
        int[] srcDimensions = resultVector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int srcDimSize = srcDimensions[numSrcDimensions - 1];
        int accSrcDimensions = resultVector.getLength() / srcDimSize;
        RIntVector p = (RIntVector) positions[positions.length - 1];
        int accDstDimensions = replacementLength / p.getLength();
        posNACheck.enable(p);
        elementNACheck.enable(value);
        for (int i = 0; i < p.getLength(); i++) {
            int dstArrayBase = accDstDimensions * i;
            int pos = p.getDataAt(i);
            if (posNACheck.check(pos)) {
                handleNaMultiDim(value, false, isSubset, getEncapsulatingSourceSection());
                continue;
            }
            int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
            setMultiDimData(frame, value, resultVector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
        }
        return resultVector;
    }

    private RRawVector getResultVector(RRawVector vector, int highestPos) {
        RRawVector resultVector = vector;
        if (vector.isShared()) {
            vectorShared.enter();
            resultVector = (RRawVector) vector.copy();
            resultVector.markNonTemporary();
        }
        if (resultVector.getLength() < highestPos) {
            vectorTooShort.enter();
            resultVector.resizeWithNames(highestPos);
        }
        return resultVector;
    }

    private static RRawVector updateSingleDim(RAbstractRawVector value, RRawVector resultVector, int position) {
        resultVector.updateDataAt(position - 1, value.getDataAt(0));
        return resultVector;
    }

    private RRawVector updateSingleDimVector(RAbstractRawVector value, int orgVectorLength, RRawVector resultVector, RIntVector positions) {
        if (positions.getLength() == 1 && value.getLength() > 1) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        }
        elementNACheck.enable(value);
        for (int i = 0; i < positions.getLength(); i++) {
            int p = positions.getDataAt(i);
            if (seenNaOrNegative(p, value)) {
                continue;
            }
            if (negativePosProfile.profile(p < 0 && -(p + 1) >= orgVectorLength)) {
                resultVector.updateDataAt(-(p + 1), RDataFactory.createRaw((byte) 0));
            } else {
                resultVector.updateDataAt(p - 1, value.getDataAt(i % value.getLength()));
            }
        }
        if (positions.getLength() % value.getLength() != 0) {
            warning.enter();
            RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        }
        updateNames(resultVector, positions);
        return resultVector;
    }

    @Specialization(guards = {"multiDim", "!wrongDimensionsMatrix", "!wrongDimensions"})
    protected RRawVector update(VirtualFrame frame, Object v, RAbstractRawVector value, int recLevel, Object[] positions, RRawVector vector) {
        return updateVector(frame, value, vector, positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "multiPos"})
    protected RAbstractRawVector updateSubset(Object v, RAbstractRawVector value, int recLevel, RIntVector positions, RRawVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"isSubset", "!posNames", "onePosition"})
    protected Object updateSubsetOne(VirtualFrame frame, Object v, RAbstractRawVector value, int recLevel, RIntVector positions, RRawVector vector) {
        return updateRecursive(frame, v, value, vector, positions.getDataAt(0), recLevel);
    }

    @Specialization(guards = {"isSubset", "posNames"})
    protected RAbstractRawVector updateSubsetNames(Object v, RAbstractRawVector value, int recLevel, RIntVector positions, RRawVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isSubset", "posNames"})
    protected RAbstractRawVector update(Object v, RAbstractRawVector value, int recLevel, RIntVector positions, RRawVector vector) {
        return updateSingleDimVector(value, vector.getLength(), getResultVector(vector, getHighestPos(positions)), positions);
    }

    @Specialization(guards = {"!isValueLengthOne", "isSubset", "!isPosNA", "!isPosZero"})
    protected RRawVector updateTooManyValuesSubset(Object v, RAbstractRawVector value, int recLevel, int position, RRawVector vector) {
        RError.warning(RError.Message.NOT_MULTIPLE_REPLACEMENT);
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"isValueLengthOne", "!isPosNA", "!isPosZero"})
    protected RRawVector update(Object v, RAbstractRawVector value, int recLevel, int position, RRawVector vector) {
        return updateSingleDim(value, getResultVector(vector, position), position);
    }

    @Specialization(guards = {"noPosition", "emptyValue"})
    protected Object accessListEmptyPosEmptyValueList(Object v, RAbstractVector value, int recLevel, RList positions, RList vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = {"noPosition", "emptyValue", "!isVectorList"})
    protected Object accessListEmptyPosEmptyValue(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = {"noPosition", "valueLengthOne"})
    protected Object accessListEmptyPosValueLengthOne(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = {"noPosition", "valueLongerThanOne"})
    protected Object accessListEmptyPosValueLongerThanOne(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = "noPosition")
    protected Object accessListEmptyPosValueNullList(Object v, RNull value, int recLevel, RList positions, RList vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = {"noPosition", "!isVectorList"})
    protected Object accessListEmptyPosValueNull(Object v, RNull value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = {"onePosition", "emptyValue"})
    protected Object accessListOnePosEmptyValueList(Object v, RAbstractVector value, int recLevel, RList positions, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @Specialization(guards = {"onePosition", "emptyValue", "!isVectorList"})
    protected Object accessListOnePosEmptyValue(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = {"onePosition", "valueLengthOne"})
    protected Object accessListOnePosValueLengthOne(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @Specialization(guards = {"onePosition", "valueLongerThanOne"})
    protected Object accessListOnePosValueLongerThanTwo(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = "onePosition")
    protected Object accessListOnePosValueNullList(Object v, RNull value, int recLevel, RList positions, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @Specialization(guards = {"onePosition", "!isVectorList"})
    protected Object accessListOnePosValueNull(Object v, RNull value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = "twoPositions")
    protected Object accessListTwoPos(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @Specialization(guards = "twoPositions")
    protected Object accessListTwoPosValueNull(Object v, RNull value, int recLevel, RList positions, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @Specialization(guards = {"moreThanTwoPos", "emptyValue"})
    protected Object accessListMultiPosEmptyValueList(Object v, RAbstractVector value, int recLevel, RList positions, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @Specialization(guards = {"moreThanTwoPos", "emptyValue", "!isVectorList"})
    protected Object accessListMultiPosEmptyValue(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = {"moreThanTwoPos", "valueLengthOne"})
    protected Object accessListMultiPosValueLengthOneList(Object v, RAbstractVector value, int recLevel, RList positions, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @Specialization(guards = {"moreThanTwoPos", "valueLengthOne", "!isVectorList"})
    protected Object accessListMultiPosValueLengthOne(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = {"moreThanTwoPos", "valueLongerThanOne"})
    protected Object accessListMultiPos(Object v, RAbstractVector value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = "moreThanTwoPos")
    protected Object accessListMultiPosValueNullList(Object v, RNull value, int recLevel, RList positions, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @Specialization(guards = {"moreThanTwoPos", "!isVectorList"})
    protected Object accessListMultiPosValueNull(Object v, RNull value, int recLevel, RList positions, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @Specialization(guards = {"emptyValue", "!isVectorList"})
    protected Object accessComplexEmptyValue(Object v, RAbstractVector value, int recLevel, RComplex position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
        }
    }

    @Specialization(guards = {"valueLongerThanOne", "!isVectorList"})
    protected Object accessComplexValueLongerThanOne(Object v, RAbstractVector value, int recLevel, RComplex position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
        }
    }

    @Specialization(guards = {"!valueLongerThanOne", "!emptyValue", "!isVectorList"})
    protected Object accessComplex(Object v, RAbstractVector value, int recLevel, RComplex position, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
    }

    @Specialization
    protected Object accessComplexList(Object v, RAbstractVector value, int recLevel, RComplex position, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
    }

    @Specialization(guards = "!isVectorList")
    protected Object accessComplex(Object v, RNull value, int recLevel, RComplex position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
        }
    }

    @Specialization
    protected Object accessComplexList(Object v, RNull value, int recLevel, RComplex position, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
    }

    @Specialization(guards = {"emptyValue", "!isVectorList"})
    protected Object accessRawEmptyValue(Object v, RAbstractVector value, int recLevel, RRaw position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_0);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
        }
    }

    @Specialization(guards = {"valueLongerThanOne", "!isVectorList"})
    protected Object accessRawValueLongerThanOne(Object v, RAbstractVector value, int recLevel, RRaw position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
        }
    }

    @Specialization(guards = {"!valueLongerThanOne", "!emptyValue", "!isVectorList"})
    protected Object accessRaw(Object v, RAbstractVector value, int recLevel, RRaw position, RAbstractVector vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
    }

    @Specialization
    protected Object accessRawList(Object v, RAbstractVector value, int recLevel, RRaw position, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
    }

    @Specialization(guards = "!isVectorList")
    protected Object accessRaw(Object v, RNull value, int recLevel, RRaw position, RAbstractVector vector) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MORE_SUPPLIED_REPLACE);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
        }
    }

    @Specialization
    protected Object accessRawList(Object v, RNull value, int recLevel, RRaw position, RList vector) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
    }

    protected boolean firstPosZero(Object v, RAbstractContainer value, int recLevel, RIntVector positions) {
        return positions.getDataAt(0) == 0;
    }

    protected boolean firstPosZero(Object v, RNull value, int recLevel, RIntVector positions) {
        return positions.getDataAt(0) == 0;
    }

    protected boolean outOfBoundsNegative(Object v, RNull value, int recLevel, int position, RAbstractVector vector) {
        return -position > vector.getLength();
    }

    protected boolean outOfBoundsNegative(Object v, RAbstractContainer value, int recLevel, int position, RAbstractVector vector) {
        return -position > vector.getLength();
    }

    protected boolean oneElemVector(Object v, RNull value, int recLevel, Object positions, RAbstractVector vector) {
        return vector.getLength() == 1;
    }

    protected boolean oneElemVector(Object v, RAbstractContainer value, int recLevel, Object positions, RAbstractVector vector) {
        return vector.getLength() == 1;
    }

    protected boolean posNames(Object v, RAbstractContainer value, int recLevel, RIntVector positions) {
        return positions.getNames() != RNull.instance;
    }

    protected boolean isPositionNegative(Object v, RAbstractContainer value, int recLevel, int position) {
        return position < 0;
    }

    protected boolean isPositionNegative(Object v, RNull value, int recLevel, int position) {
        return position < 0;
    }

    protected boolean isVectorList(Object v, RNull value, int recLevel, Object positions, RAbstractVector vector) {
        return vector.getElementClass() == Object.class;
    }

    protected boolean isVectorList(Object v, RAbstractContainer value, int recLevel, Object positions, RAbstractVector vector) {
        return vector.getElementClass() == Object.class;
    }

    protected boolean isVectorLongerThanOne(Object v, RAbstractContainer value, int recLevel, Object positions, RAbstractVector vector) {
        return vector.getLength() > 1;
    }

    protected boolean isVectorLongerThanOne(Object v, RNull value, int recLevel, Object positions, RAbstractVector vector) {
        return vector.getLength() > 1;
    }

    protected boolean emptyValue(Object v, RAbstractContainer value) {
        return value.getLength() == 0;
    }

    protected boolean valueLengthOne(Object v, RAbstractContainer value) {
        return value.getLength() == 1;
    }

    protected boolean valueLongerThanOne(Object v, RAbstractContainer value) {
        return value.getLength() > 1;
    }

    protected boolean wrongDimensionsMatrix(Object v, RAbstractContainer value, int recLevel, Object[] positions, RAbstractVector vector) {
        if (positions.length == 2 && (vector.getDimensions() == null || vector.getDimensions().length != positions.length)) {
            error.enter();
            if (isSubset) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_SUBSCRIPTS_MATRIX);
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.IMPROPER_SUBSCRIPT);
            }
        }
        return false;
    }

    protected boolean wrongDimensionsMatrix(Object v, RAbstractContainer value, int recLevel, Object[] positions, RNull vector) {
        if (positions.length == 2) {
            error.enter();
            if (isSubset) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_SUBSCRIPTS_MATRIX);
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.IMPROPER_SUBSCRIPT);
            }
        }
        return false;
    }

    protected boolean wrongDimensionsMatrix(Object v, RNull value, int recLevel, Object[] positions, RAbstractVector vector) {
        if (positions.length == 2 && (vector.getDimensions() == null || vector.getDimensions().length != positions.length)) {
            error.enter();
            if (isSubset) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_SUBSCRIPTS_MATRIX);
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.IMPROPER_SUBSCRIPT);
            }
        }
        return false;
    }

    protected boolean wrongDimensions(Object v, RAbstractContainer value, int recLevel, Object[] positions, RAbstractVector vector) {
        if (!((vector.getDimensions() == null && positions.length == 1) || vector.getDimensions().length == positions.length)) {
            error.enter();
            if (isSubset) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_SUBSCRIPTS);
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.IMPROPER_SUBSCRIPT);
            }
        }
        return false;
    }

    protected boolean wrongDimensions(Object v, RAbstractContainer value, int recLevel, Object[] positions, RNull vector) {
        if (positions.length > 2) {
            error.enter();
            if (isSubset) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_SUBSCRIPTS);
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.IMPROPER_SUBSCRIPT);
            }
        }
        return false;
    }

    protected boolean wrongDimensions(Object v, RNull value, int recLevel, Object[] positions, RAbstractVector vector) {
        if (!((vector.getDimensions() == null && positions.length == 1) || vector.getDimensions().length == positions.length)) {
            error.enter();
            if (isSubset) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_SUBSCRIPTS);
            } else {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.IMPROPER_SUBSCRIPT);
            }
        }
        return false;
    }

    protected boolean multiDim(Object v, RAbstractContainer value, int recLevel, Object[] positions, RAbstractVector vector) {
        return vector.getDimensions() != null && vector.getDimensions().length > 1;
    }

    protected boolean wrongLength(Object v, RAbstractContainer value, int recLevel, RIntVector positions, RAbstractVector vector) {
        int valLength = value.getLength();
        int posLength = positions.getLength();
        return valLength > posLength || (posLength % valLength != 0);
    }

    protected boolean isPosNA(Object v, RAbstractContainer value, int recLevel, int position) {
        return RRuntime.isNA(position);
    }

    protected boolean isPosNA(Object v, RNull value, int recLevel, int position) {
        return RRuntime.isNA(position);
    }

    protected boolean isPosZero(Object v, RAbstractContainer value, int recLevel, int position) {
        return position == 0;
    }

    protected boolean isPosZero(Object v, RNull value, int recLevel, int position) {
        return position == 0;
    }

    protected boolean isValueLengthOne(Object v, RAbstractContainer value) {
        return value.getLength() == 1;
    }

    protected boolean twoPositions(Object v, RAbstractContainer value, int recLevel, RAbstractVector position) {
        return position.getLength() == 2;
    }

    protected boolean twoPositions(Object v, RNull value, int recLevel, RAbstractVector position) {
        return position.getLength() == 2;
    }

    protected boolean onePosition(Object v, RAbstractContainer value, int recLevel, RAbstractVector position) {
        return position.getLength() == 1;
    }

    protected boolean onePosition(Object v, RNull value, int recLevel, RAbstractVector position) {
        return position.getLength() == 1;
    }

    protected boolean noPosition(Object v, RAbstractContainer value, int recLevel, RAbstractVector position) {
        return position.getLength() == 0;
    }

    protected boolean noPosition(Object v, RNull value, int recLevel, RAbstractVector position) {
        return position.getLength() == 0;
    }

    protected boolean isSubset() {
        return isSubset;
    }

    protected boolean inRecursion(Object v, RAbstractContainer value, int recLevel) {
        return recLevel > 0;
    }

    protected boolean inRecursion(Object v, RNull value, int recLevel) {
        return recLevel > 0;
    }

    protected boolean multiPos(Object v, RNull value, int recLevel, RIntVector positions) {
        return positions.getLength() > 1;
    }

    protected boolean multiPos(Object v, RAbstractContainer value, int recLevel, RIntVector positions) {
        return positions.getLength() > 1;
    }

    protected boolean moreThanTwoPos(Object v, RAbstractContainer value, int recLevel, RList positions) {
        return positions.getLength() > 2;
    }

    protected boolean moreThanTwoPos(Object v, RNull value, int recLevel, RList positions) {
        return positions.getLength() > 2;
    }

    protected boolean emptyList(Object v, RNull value, int recLevel, int positions, RList vector) {
        return vector.getLength() == 0;
    }

    /**
     * N.B. array updates are always part of a "replacement" so all that deparse does is handle the
     * {@link #getPositions()}. See {@code SequenceNode.Replacement}.
     */
    @Override
    public void deparse(State state) {
        state.append(isSubset ? "[" : "[[");
        getPositions().deparse(state);
        state.append(isSubset ? "]" : "]]");
    }
}
