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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.access.ArrayPositionCastFactory.OperatorConverterNodeFactory;
import com.oracle.truffle.r.nodes.access.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.access.AccessArrayNodeFactory.GetMultiDimDataNodeFactory;
import com.oracle.truffle.r.nodes.access.AccessArrayNodeFactory.GetNamesNodeFactory;
import com.oracle.truffle.r.nodes.access.AccessArrayNodeFactory.GetDimNamesNodeFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "recursionLevel", type = RNode.class),
                @NodeChild(value = "positions", type = PositionsArrayNode.class, executeWith = {"vector"}), @NodeChild(value = "dropDim", type = RNode.class)})
public abstract class AccessArrayNode extends RNode {

    private final boolean isSubset;

    private final NACheck elementNACheck = NACheck.create();
    private final NACheck posNACheck = NACheck.create();
    private final NACheck namesNACheck = NACheck.create();

    @Child private AccessArrayNode accessRecursive;
    @Child private CastToVectorNode castVector;
    @Child private ArrayPositionCast castPosition;
    @Child private OperatorConverterNode operatorConverter;
    @Child private GetMultiDimDataNode getMultiDimData;
    @Child private GetNamesNode getNamesNode;
    @Child private GetDimNamesNode getDimNamesNode;

    abstract RNode getVector();

    public abstract Object executeAccess(VirtualFrame frame, Object vector, int recLevel, Object operand, RAbstractLogicalVector dropDim);

    public abstract Object executeAccess(VirtualFrame frame, Object vector, int recLevel, int operand, RAbstractLogicalVector dropDim);

    public AccessArrayNode(boolean isSubset) {
        this.isSubset = isSubset;
    }

    public AccessArrayNode(AccessArrayNode other) {
        this.isSubset = other.isSubset;
    }

    private Object accessRecursive(VirtualFrame frame, Object vector, Object operand, int recLevel, RAbstractLogicalVector dropDim) {
        if (accessRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            accessRecursive = insert(AccessArrayNodeFactory.create(this.isSubset, null, null, null, null));
        }
        return executeAccess(frame, vector, recLevel, operand, dropDim);
    }

    private Object castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeFactory.create(null, false, false, false, true));
        }
        return castVector.executeObject(frame, value);
    }

    private Object castPosition(VirtualFrame frame, Object vector, Object operand) {
        if (castPosition == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castPosition = insert(ArrayPositionCastFactory.create(0, 1, false, false, null, null, null));
        }
        return castPosition.executeArg(frame, operand, vector, operand);
    }

    private void initOperatorConvert() {
        if (operatorConverter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            operatorConverter = insert(OperatorConverterNodeFactory.create(0, 1, false, false, null, null));
        }
    }

    private Object convertOperand(VirtualFrame frame, Object vector, int operand) {
        initOperatorConvert();
        return operatorConverter.executeConvert(frame, vector, operand);
    }

    private Object convertOperand(VirtualFrame frame, Object vector, String operand) {
        initOperatorConvert();
        return operatorConverter.executeConvert(frame, vector, operand);
    }

    private Object getMultiDimData(VirtualFrame frame, Object data, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions, NACheck posCheck, NACheck elementCheck) {
        if (getMultiDimData == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getMultiDimData = insert(GetMultiDimDataNodeFactory.create(posCheck, elementCheck, null, null, null, null, null, null, null, null));
        }
        return getMultiDimData.executeMultiDimDataGet(frame, data, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
    }

    private RStringVector getNames(VirtualFrame frame, RAbstractVector vector, Object[] positions, int currentDimLevel, NACheck namesCheck) {
        if (getNamesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNamesNode = insert(GetNamesNodeFactory.create(namesCheck, null, null, null, null, null));
        }
        return (RStringVector) getNamesNode.executeNamesGet(frame, vector, positions, currentDimLevel, RRuntime.LOGICAL_TRUE, RNull.instance);
    }

    private RStringVector getDimNames(VirtualFrame frame, RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel, NACheck namesCheck) {
        if (getDimNamesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getDimNamesNode = insert(GetDimNamesNodeFactory.create(namesCheck, null, null, null, null, null));
        }
        return (RStringVector) getDimNamesNode.executeDimNamesGet(frame, dstDimNames, vector, positions, currentSrcDimLevel, currentDstDimLevel);
    }

    public static RIntVector popHead(RIntVector p, NACheck posNACheck) {
        int[] data = new int[p.getLength() - 1];
        posNACheck.enable(p);
        for (int i = 0; i < data.length; i++) {
            data[i] = p.getDataAt(i + 1);
            posNACheck.check(data[i]);
        }
        return RDataFactory.createIntVector(data, posNACheck.neverSeenNA());
    }

    public static RStringVector popHead(RStringVector p, NACheck posNACheck) {
        String[] data = new String[p.getLength() - 1];
        posNACheck.enable(p);
        for (int i = 0; i < data.length; i++) {
            data[i] = p.getDataAt(i + 1);
            posNACheck.check(data[i]);
        }
        return RDataFactory.createStringVector(data, posNACheck.neverSeenNA());
    }

    protected static RStringVector getNamesVector(Object srcNamesObject, RIntVector p, int resLength, NACheck namesNACheck) {
        if (srcNamesObject == RNull.instance) {
            return null;
        }
        RStringVector srcNames = (RStringVector) srcNamesObject;
        String[] namesData = new String[resLength];
        namesNACheck.enable(!srcNames.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int position = p.getDataAt(i);
            if (namesNACheck.check(position)) {
                namesData[i] = RRuntime.STRING_NA;
            } else {
                namesData[i] = srcNames.getDataAt(position - 1);
                namesNACheck.check(namesData[i]);
            }
        }
        return RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1, guards = {"inRecursion", "isFirstPositionPositive"})
    RNull accessNullInRecursionPosPositive(RNull vector, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 2, guards = {"inRecursion", "!isFirstPositionPositive"})
    RNull accessNullInRecursion(RNull vector, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 3)
    RNull access(RNull vector, int recLevel, Object positions, RAbstractLogicalVector dropDim) {
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 4, guards = {"inRecursion", "isFirstPositionOne"})
    RNull accessFunctionInRecursionPosOne(RFunction vector, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.getInvalidTypeLength(getEncapsulatingSourceSection(), "closure", 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 5, guards = {"inRecursion", "isFirstPositionPositive", "!isFirstPositionOne"})
    RNull accessFunctionInRecursionPosPositive(RFunction vector, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 6, guards = {"inRecursion", "!isFirstPositionPositive"})
    RNull accessFunctionInRecursion(RFunction vector, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 7, guards = "inRecursion")
    RNull accessFunctionInRecursionString(RFunction vector, int recLevel, RAbstractStringVector positions, RAbstractLogicalVector dropDim) {
        throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 8)
    RNull accessFunction(RFunction vector, int recLevel, Object position, RAbstractLogicalVector dropDim) {
        throw RError.getObjectNotSubsettable(getEncapsulatingSourceSection(), "closure");
    }

    @SuppressWarnings("unused")
    @Specialization(order = 9)
    RNull access(RAbstractContainer container, int recLevel, RNull positions, RAbstractLogicalVector dropDim) {
        // this is a special case (see ArrayPositionCast) - RNull can only appear to represent the
        // x[NA] case which has to return null and not a null vector
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 10)
    Object access(RAbstractContainer container, int recLevel, RMissing positions, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "symbol");
        } else {
            return container;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 11, guards = "wrongDimensions")
    Object access(RAbstractContainer container, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        throw RError.getIncorrectDimensions(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 12, guards = {"isPositionNA", "!isSubset"})
    RIntVector accessNA(RAbstractContainer container, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
    }

    private RStringVector getName(RAbstractVector vector, int position) {
        RStringVector srcNames = (RStringVector) vector.getNames();
        namesNACheck.enable(srcNames);
        String name = srcNames.getDataAt(position - 1);
        namesNACheck.check(name);
        return RDataFactory.createStringVector(new String[]{name}, namesNACheck.neverSeenNA());
    }

    private static class DimsAndResultLength {
        public final int[] dimensions;
        public final int resLength;

        public DimsAndResultLength(int[] dimensions, int resLength) {
            this.dimensions = dimensions;
            this.resLength = resLength;
        }
    }

    private DimsAndResultLength getDimsAndResultLength(Object[] positions, byte dropDim) {
        int dimLength = 0;
        int resLength = 1;
        int multi = 0; // how many times # positions > 1 ?
        int zero = 0; // how many times a position is 0
        int single = 0; // how many times # positions == 1
        for (int i = 0; i < positions.length; i++) {
            RIntVector p = (RIntVector) positions[i];
            int pLength = p.getLength();
            if (pLength == 1 && p.getDataAt(0) != 0) {
                if (!isSubset || dropDim == RRuntime.TRUE) {
                    // always drop dimensions with subscript
                    continue;
                } else {
                    single++;
                }
            } else if (pLength > 1) {
                multi++;
                resLength *= pLength;
            } else {
                resLength = 0;
                zero++;
            }
            dimLength++;
        }
        // create dimensions array
        int[] dimensions = null;

        if (dimLength > 0 && ((zero > 1 || multi > 1) || (zero > 0 && multi > 0) || single > 0)) {
            dimensions = new int[dimLength];
            int ind = 0;
            for (int i = 0; i < positions.length; i++) {
                RIntVector p = (RIntVector) positions[i];
                int pLength = p.getLength();
                if (pLength == 1 && p.getDataAt(0) != 0) {
                    if (!isSubset || dropDim == RRuntime.TRUE) {
                        // always drop dimensions with subscript
                        continue;
                    } else {
                        dimensions[ind++] = pLength;
                    }
                } else if (pLength > 1) {
                    dimensions[ind++] = pLength;
                } else {
                    dimensions[ind++] = 0;
                }
            }
        }
        return new DimsAndResultLength(dimensions, resLength);
    }

    private int getSrcArrayBase(int pos, int accSrcDimensions) {
        if (elementNACheck.check(pos)) {
            return -1; // fill with NAs at the lower levels
        } else {
            return accSrcDimensions * (pos - 1);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 13)
    RList access(VirtualFrame frame, RList vector, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        Object[] data;
        if (resLength == 0) {
            data = new Object[0];
        } else {
            data = new Object[resLength];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(!p.isComplete());
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int pos = p.getDataAt(i);
                int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
                getMultiDimData(frame, data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createList(data, dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(frame, vector, positions, numSrcDimensions, namesNACheck);
                return RDataFactory.createList(data, dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RList resultVector = RDataFactory.createList(data, dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(frame, dstDimNames, vector, positions, numSrcDimensions, numDstDimensions, namesNACheck);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 14, guards = "isSubset")
    RList accessSubset(RList vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int resLength = p.getLength();
        Object[] data = new Object[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RNull.instance;
            } else {
                data[i] = vector.getDataAt(position - 1);
            }
        }
        RStringVector names = getNamesVector(vector.getNames(), p, resLength, namesNACheck);
        return RDataFactory.createList(data, names);
    }

    // lists require special handling for one-dimensional "subscript", that is [[]], accesses due to
    // support for recursive access

    public static int getPositionInRecursion(RList vector, String position, int recLevel, SourceSection sourceSection) {
        if (vector.getNames() != RNull.instance) {
            RStringVector names = (RStringVector) vector.getNames();
            for (int i = 0; i < names.getLength(); i++) {
                if (position.equals(names.getDataAt(i))) {
                    return i + 1;
                }
            }
        }
        throw RError.getNoSuchIndexAtLevel(sourceSection, recLevel + 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 15, guards = "!hasNames")
    RList accessStringNoNames(RList vector, int recLevel, RStringVector p, RAbstractLogicalVector dropDim) {
        throw RError.getNoSuchIndexAtLevel(getEncapsulatingSourceSection(), 1);
    }

    @Specialization(order = 16, guards = {"hasNames", "!isSubset", "twoPosition"})
    Object accessStringTwoPosRec(VirtualFrame frame, RList vector, int recLevel, RStringVector p, RAbstractLogicalVector dropDim) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, getEncapsulatingSourceSection());
        Object newVector = castVector(frame, vector.getDataAt(position - 1));
        Object newPosition = castPosition(frame, newVector, convertOperand(frame, newVector, p.getDataAt(1)));
        return accessRecursive(frame, newVector, newPosition, recLevel + 1, dropDim);
    }

    @Specialization(order = 17, guards = {"hasNames", "!isSubset", "!twoPosition"})
    Object accessString(VirtualFrame frame, RList vector, int recLevel, RStringVector p, RAbstractLogicalVector dropDim) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, getEncapsulatingSourceSection());
        RStringVector newP = popHead(p, posNACheck);
        return accessRecursive(frame, vector.getDataAt(position - 1), newP, recLevel + 1, dropDim);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 20, guards = {"!isSubset", "onePosition", "!inRecursion"})
    Object accessOnePos(RList vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int position = p.getDataAt(0);
        if (RRuntime.isNA(position)) {
            return RNull.instance;
        } else if (position <= 0) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        } else if (position > vector.getLength()) {
            throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
        }
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 21, guards = {"!isSubset", "noPosition"})
    Object accessNoPos(RList vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
    }

    public static int getPositionFromNegative(RList vector, int position, SourceSection sourceSection) {
        if (vector.getLength() == 1 && position == -1) {
            // x<-c(1); x[-1] <==> x[0]
            throw RError.getSelectLessThanOne(sourceSection);
        } else if (vector.getLength() > 1 && position < -vector.getLength()) {
            // x<-c(1,2); x[-3] <==> x[1,2]
            throw RError.getSelectMoreThanOne(sourceSection);
        } else if (vector.getLength() > 2 && position > -vector.getLength()) {
            // x<-c(1,2,3); x[-2] <==> x[1,3]
            throw RError.getSelectMoreThanOne(sourceSection);
        }
        assert (vector.getLength() == 2);
        return position == -1 ? 2 : 1;
    }

    private int getPositionInRecursion(RList vector, int position, int recLevel) {
        if (RRuntime.isNA(position) || position > vector.getLength()) {
            throw RError.getNoSuchIndexAtLevel(getEncapsulatingSourceSection(), recLevel + 1);
        } else if (position < 0) {
            return getPositionFromNegative(vector, position, getEncapsulatingSourceSection());
        } else if (position == 0) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        return position;
    }

    @Specialization(order = 22, guards = {"!isSubset", "onePosition", "inRecursion"})
    Object accessSubscript(RList vector, int recLevel, RIntVector p, @SuppressWarnings("unused") RAbstractLogicalVector dropDim) {
        int position = p.getDataAt(0);
        position = getPositionInRecursion(vector, position, recLevel);
        return vector.getDataAt(position - 1);
    }

    @Specialization(order = 23, guards = {"!isSubset", "twoPosition"})
    Object accessTwoPosRec(VirtualFrame frame, RList vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int position = p.getDataAt(0);
        position = getPositionInRecursion(vector, position, recLevel);
        Object newVector = castVector(frame, vector.getDataAt(position - 1));
        Object newPosition = castPosition(frame, newVector, convertOperand(frame, newVector, p.getDataAt(1)));
        return accessRecursive(frame, newVector, newPosition, recLevel + 1, dropDim);
    }

    @Specialization(order = 24, guards = {"!isSubset", "multiPos"})
    Object access(VirtualFrame frame, RList vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int position = p.getDataAt(0);
        position = getPositionInRecursion(vector, position, recLevel);
        RIntVector newP = popHead(p, posNACheck);
        return accessRecursive(frame, vector.getDataAt(position - 1), newP, recLevel + 1, dropDim);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 25, guards = {"isPositionNA", "isSubset"})
    RList accessNA(RList vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createList(new Object[]{RNull.instance});
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createList(new Object[]{RNull.instance}, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 26, guards = {"!isPositionNA", "isPositionNegative", "!outOfBoundsNegative"})
    RList accessNegativeInBounds(RAbstractContainer container, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 27, guards = {"!isPositionNA", "isPositionNegative", "outOfBoundsNegative", "oneElemVector"})
    RList accessNegativeOutOfBoundsOneElemVector(RAbstractContainer container, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 28, guards = {"!isPositionNA", "isPositionNegative", "outOfBoundsNegative", "!oneElemVector"})
    RList accessNegativeOutOfBounds(RAbstractContainer container, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 29, guards = {"!isPositionZero", "hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    RList accessNamesSubset(RList vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        Object val = vector.getDataAt(position - 1);
        return RDataFactory.createList(new Object[]{val}, getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 30, guards = {"!isPositionZero", "hasNames", "!isSubset", "!isPositionNA", "!isPositionNegative", "!outOfBounds"})
    Object accessNames(RList vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 31, guards = {"!isPositionZero", "!hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    RList accessSubset(RList vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return RDataFactory.createList(new Object[]{vector.getDataAt(position - 1)});
    }

    @SuppressWarnings("unused")
    @Specialization(order = 32, guards = {"!isPositionZero", "!hasNames", "!isSubset", "!isPositionNA", "!isPositionNegative", "!outOfBounds"})
    Object access(RList vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 33, guards = {"!isSubset", "outOfBounds"})
    Object accessOutOfBounds(RList vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 34, guards = "isPositionZero")
    RList accessPosZero(RList vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createList();
        } else {
            return RDataFactory.createList(new Object[0], RDataFactory.createEmptyStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 35, guards = {"!isSubset", "inRecursion", "multiPos", "!isVectorList"})
    Object accessRecFailedRec(RAbstractContainer container, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        throw RError.getRecursiveIndexingFailed(getEncapsulatingSourceSection(), recLevel + 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 36, guards = {"!isSubset", "!inRecursion", "multiPos", "!isVectorList"})
    Object accessRecFailed(RAbstractContainer container, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
    }

    @Specialization(order = 40)
    RIntVector access(VirtualFrame frame, RAbstractIntVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int[] data;
        if (resLength == 0) {
            data = new int[0];
        } else {
            data = new int[resLength];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(!vector.isComplete() || !p.isComplete());
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int pos = p.getDataAt(i);
                int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
                getMultiDimData(frame, data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(frame, vector, positions, numSrcDimensions, namesNACheck);
                return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RIntVector resultVector = RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(frame, dstDimNames, vector, positions, numSrcDimensions, numDstDimensions, namesNACheck);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 41)
    RIntVector access(RAbstractIntVector vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int resLength = p.getLength();
        int[] data = new int[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RRuntime.INT_NA;
            } else {
                data[i] = vector.getDataAt(position - 1);
                elementNACheck.check(data[i]);
            }
        }
        RStringVector names = getNamesVector(vector.getNames(), p, resLength, namesNACheck);
        return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 42, guards = {"isPositionNA", "isSubset"})
    RIntVector accessNA(RAbstractIntVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 43, guards = {"!isPositionZero", "hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    RIntVector accessNames(RAbstractIntVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        int val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createIntVector(new int[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 44, guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    int access(RAbstractIntVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 45, guards = "isPositionZero")
    RIntVector accessPosZero(RAbstractIntVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createIntVector(new int[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    @Specialization(order = 50)
    RDoubleVector access(VirtualFrame frame, RAbstractDoubleVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        double[] data;
        if (resLength == 0) {
            data = new double[0];
        } else {
            data = new double[resLength];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(!vector.isComplete() || !p.isComplete());
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int pos = p.getDataAt(i);
                int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
                getMultiDimData(frame, data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(frame, vector, positions, numSrcDimensions, namesNACheck);
                return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RDoubleVector resultVector = RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(frame, dstDimNames, vector, positions, numSrcDimensions, numDstDimensions, namesNACheck);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 51)
    RDoubleVector access(RAbstractDoubleVector vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int resLength = p.getLength();
        double[] data = new double[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RRuntime.DOUBLE_NA;
            } else {
                data[i] = vector.getDataAt(position - 1);
                elementNACheck.check(data[i]);
            }
        }
        RStringVector names = getNamesVector(vector.getNames(), p, resLength, namesNACheck);
        return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 52, guards = {"isPositionNA", "isSubset"})
    RDoubleVector accessNA(RAbstractDoubleVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 53, guards = {"!isPositionZero", "hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    RDoubleVector accessNames(RAbstractDoubleVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        double val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createDoubleVector(new double[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 54, guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    double access(RAbstractDoubleVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 55, guards = "isPositionZero")
    RDoubleVector accessPosZero(RAbstractDoubleVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyDoubleVector();
        } else {
            return RDataFactory.createDoubleVector(new double[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    @Specialization(order = 60)
    RLogicalVector access(VirtualFrame frame, RLogicalVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        byte[] data;
        if (resLength == 0) {
            data = new byte[0];
        } else {
            data = new byte[resLength];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(!vector.isComplete() || !p.isComplete());
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int pos = p.getDataAt(i);
                int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
                getMultiDimData(frame, data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(frame, vector, positions, numSrcDimensions, namesNACheck);
                return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RLogicalVector resultVector = RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(frame, dstDimNames, vector, positions, numSrcDimensions, numDstDimensions, namesNACheck);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 61)
    RLogicalVector access(RLogicalVector vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int resLength = p.getLength();
        byte[] data = new byte[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RRuntime.LOGICAL_NA;
            } else {
                data[i] = vector.getDataAt(position - 1);
                elementNACheck.check(data[i]);
            }
        }
        RStringVector names = getNamesVector(vector.getNames(), p, resLength, namesNACheck);
        return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 62, guards = {"isPositionNA", "isSubset"})
    RLogicalVector accessNA(RLogicalVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 63, guards = {"!isPositionZero", "hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    RLogicalVector accessNames(RAbstractLogicalVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        byte val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createLogicalVector(new byte[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 64, guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    byte access(RLogicalVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 65, guards = "isPositionZero")
    RLogicalVector accessPosZero(RLogicalVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyLogicalVector();
        } else {
            return RDataFactory.createLogicalVector(new byte[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    @Specialization(order = 70)
    RStringVector access(VirtualFrame frame, RStringVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        String[] data;
        if (resLength == 0) {
            data = new String[0];
        } else {
            data = new String[resLength];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(!vector.isComplete() || !p.isComplete());
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int pos = p.getDataAt(i);
                int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
                getMultiDimData(frame, data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(frame, vector, positions, numSrcDimensions, namesNACheck);
                return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RStringVector resultVector = RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(frame, dstDimNames, vector, positions, numSrcDimensions, numDstDimensions, namesNACheck);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 71)
    RStringVector access(RStringVector vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int resLength = p.getLength();
        String[] data = new String[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RRuntime.STRING_NA;
            } else {
                data[i] = vector.getDataAt(position - 1);
                elementNACheck.check(data[i]);
            }
        }
        RStringVector names = getNamesVector(vector.getNames(), p, resLength, namesNACheck);
        return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 72, guards = {"isPositionNA", "isSubset"})
    RStringVector accessNA(RStringVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 73, guards = {"!isPositionZero", "hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    RStringVector accessNames(RAbstractStringVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        String val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createStringVector(new String[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 74, guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    String access(RStringVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 75, guards = "isPositionZero")
    RStringVector accessPosZero(RStringVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyStringVector();
        } else {
            return RDataFactory.createStringVector(new String[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    @Specialization(order = 80)
    RComplexVector access(VirtualFrame frame, RComplexVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        double[] data;
        if (resLength == 0) {
            data = new double[0];
        } else {
            data = new double[resLength << 1];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(!vector.isComplete() || !p.isComplete());
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int pos = p.getDataAt(i);
                int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
                getMultiDimData(frame, data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(frame, vector, positions, numSrcDimensions, namesNACheck);
                return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RComplexVector resultVector = RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(frame, dstDimNames, vector, positions, numSrcDimensions, numDstDimensions, namesNACheck);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 81)
    RComplexVector access(RComplexVector vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int resLength = p.getLength();
        double[] data = new double[resLength << 1];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        int ind = 0;
        for (int i = 0; i < p.getLength(); i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[ind++] = RRuntime.COMPLEX_NA_REAL_PART;
                data[ind++] = RRuntime.COMPLEX_NA_IMAGINARY_PART;
            } else {
                RComplex val = vector.getDataAt(position - 1);
                data[ind++] = val.getRealPart();
                data[ind++] = val.getImaginaryPart();
                elementNACheck.check(val);
            }
        }
        RStringVector names = getNamesVector(vector.getNames(), p, resLength, namesNACheck);
        return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 82, guards = {"isPositionNA", "isSubset"})
    RComplexVector accessNA(RComplexVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 83, guards = {"!isPositionZero", "hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    RComplexVector accessNames(RAbstractComplexVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        RComplex val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createComplexVector(new double[]{val.getRealPart(), val.getImaginaryPart()}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 84, guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    RComplex access(RComplexVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 85, guards = "isPositionZero")
    RComplexVector accessPosZero(RComplexVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyComplexVector();
        } else {
            return RDataFactory.createComplexVector(new double[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    @Specialization(order = 90)
    RRawVector access(VirtualFrame frame, RRawVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        byte[] data;
        if (resLength == 0) {
            data = new byte[0];
        } else {
            data = new byte[resLength];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(!p.isComplete());
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int pos = p.getDataAt(i);
                int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
                getMultiDimData(frame, data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createRawVector(data, dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(frame, vector, positions, numSrcDimensions, namesNACheck);
                return RDataFactory.createRawVector(data, dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RRawVector resultVector = RDataFactory.createRawVector(data, dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(frame, dstDimNames, vector, positions, numSrcDimensions, numDstDimensions, namesNACheck);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 91)
    RRawVector access(RRawVector vector, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int resLength = p.getLength();
        byte[] data = new byte[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < p.getLength(); i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = 0;
            } else {
                data[i] = vector.getDataAt(position - 1).getValue();
                elementNACheck.check(data[i]);
            }
        }
        RStringVector names = getNamesVector(vector.getNames(), p, resLength, namesNACheck);
        return RDataFactory.createRawVector(data, names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 92, guards = {"isPositionNA", "isSubset"})
    RRawVector accessNA(RRawVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createRawVector(new byte[]{0});
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createRawVector(new byte[]{0}, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 93, guards = {"!isPositionZero", "hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    RRawVector accessNames(RAbstractRawVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        RRaw val = vector.getDataAt(position - 1);
        return RDataFactory.createRawVector(new byte[]{val.getValue()}, getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 94, guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    RRaw access(RRawVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 95, guards = "isPositionZero")
    RRawVector accessPosZero(RRawVector vector, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyRawVector();
        } else {
            return RDataFactory.createRawVector(new byte[0], RDataFactory.createEmptyStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 200, guards = "noPosition")
    Object accessListEmptyPos(RAbstractContainer container, int recLevel, RList positions, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        } else {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "list");
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 201, guards = "onePosition")
    Object accessListOnePos(RAbstractContainer container, int recLevel, RList positions, RAbstractLogicalVector dropDim) {
        throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "list");
    }

    @SuppressWarnings("unused")
    @Specialization(order = 202, guards = "multiPos")
    Object accessListMultiPosList(RList vector, int recLevel, RList positions, RAbstractLogicalVector dropDim) {
        throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "list");
    }

    @SuppressWarnings("unused")
    @Specialization(order = 203, guards = {"multiPos", "!isVectorList"})
    Object accessListMultiPos(RAbstractContainer container, int recLevel, RList positions, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
        } else {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "list");
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 210)
    Object accessListMultiPos(RAbstractContainer container, int recLevel, RComplex positions, RAbstractLogicalVector dropDim) {
        throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "complex");
    }

    @SuppressWarnings("unused")
    @Specialization(order = 220)
    Object accessListMultiPos(RAbstractContainer container, int recLevel, RRaw positions, RAbstractLogicalVector dropDim) {
        throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "raw");
    }

    // this should really be implemented in R
    @Specialization(order = 1000, guards = "!isSubset")
    Object access(VirtualFrame frame, RDataFrame dataFrame, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return accessRecursive(frame, dataFrame.getVector(), position, recLevel, dropDim);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1001, guards = "isSubset")
    Object accessSubset(VirtualFrame frame, RDataFrame dataFrame, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.getGenericError(getEncapsulatingSourceSection(), "data frames subset access not supported");
    }

    protected boolean outOfBounds(RList vector, @SuppressWarnings("unused") int recLevel, int position) {
        return position > vector.getLength();
    }

    protected boolean outOfBoundsNegative(RAbstractContainer container, @SuppressWarnings("unused") int recLevel, int position) {
        return -position > container.getLength();
    }

    @SuppressWarnings("unused")
    protected boolean oneElemVector(RAbstractContainer container, int recLevel, int position) {
        return container.getLength() == 1;
    }

    @SuppressWarnings("unused")
    protected boolean isPositionNegative(RAbstractContainer container, int recLevel, int position) {
        return position < 0;
    }

    protected boolean isVectorList(RAbstractContainer container) {
        return container.getElementClass() == Object.class;
    }

    protected boolean wrongDimensions(RAbstractContainer container, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return container.getDimensions() == null || container.getDimensions().length != positions.length;
    }

    @SuppressWarnings("unused")
    protected static boolean isFirstPositionPositive(RNull vector, int recLevel, RAbstractIntVector positions) {
        return positions.getDataAt(0) > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean isFirstPositionPositive(RFunction vector, int recLevel, RAbstractIntVector positions) {
        return positions.getDataAt(0) > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean isFirstPositionOne(RFunction vector, int recLevel, RAbstractIntVector positions) {
        return positions.getDataAt(0) == 1;
    }

    @SuppressWarnings("unused")
    protected static boolean isPositionZero(RAbstractContainer container, int recLevel, int position) {
        return position == 0;
    }

    @SuppressWarnings("unused")
    protected static boolean isPositionNA(RAbstractContainer container, int recLevel, int position) {
        return RRuntime.isNA(position);
    }

    protected boolean isSubset() {
        return isSubset;
    }

    @SuppressWarnings("unused")
    protected static boolean hasNames(RAbstractContainer container, int recLevel, int position) {
        return container.getNames() != RNull.instance;
    }

    @SuppressWarnings("unused")
    protected static boolean hasNames(RAbstractContainer container, int recLevel, RStringVector position) {
        return container.getNames() != RNull.instance;
    }

    @SuppressWarnings("unused")
    protected static boolean twoPosition(RAbstractContainer container, int recLevel, RAbstractVector p) {
        return p.getLength() == 2;
    }

    @SuppressWarnings("unused")
    protected static boolean onePosition(RAbstractContainer container, int recLevel, RAbstractVector p) {
        return p.getLength() == 1;
    }

    @SuppressWarnings("unused")
    protected static boolean noPosition(RAbstractContainer container, int recLevel, RAbstractVector p) {
        return p.getLength() == 0;
    }

    @SuppressWarnings("unused")
    protected static boolean multiPos(RAbstractContainer container, int recLevel, RAbstractVector positions) {
        return positions.getLength() > 1;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RAbstractContainer container, int recLevel, RIntVector positions) {
        return recLevel > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RNull vector, int recLevel, RAbstractIntVector positions) {
        return recLevel > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RFunction vector, int recLevel, RAbstractIntVector positions) {
        return recLevel > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RFunction vector, int recLevel, RAbstractStringVector positions) {
        return recLevel > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RFunction vector, int recLevel, Object positions) {
        return recLevel > 0;
    }

    public static AccessArrayNode create(boolean isSubset, RNode vector, PositionsArrayNode positions, RNode dropDim) {
        return AccessArrayNodeFactory.create(isSubset, vector, ConstantNode.create(0), positions, dropDim);
    }

    @NodeChildren({@NodeChild(value = "vec", type = RNode.class), @NodeChild(value = "pos", type = RNode.class), @NodeChild(value = "currDimLevel", type = RNode.class),
                    @NodeChild(value = "allNull", type = RNode.class), @NodeChild(value = "names", type = RNode.class)})
    protected abstract static class GetNamesNode extends RNode {

        public abstract Object executeNamesGet(VirtualFrame frame, RAbstractVector vector, Object[] positions, int currentDimLevel, byte allNull, Object names);

        private final NACheck namesNACheck;

        @Child private GetNamesNode getNamesNodeRecursive;

        private RStringVector getNamesRecursive(VirtualFrame frame, RAbstractVector vector, Object[] positions, int currentDimLevel, byte allNull, Object names, NACheck namesCheck) {
            if (getNamesNodeRecursive == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNamesNodeRecursive = insert(GetNamesNodeFactory.create(namesCheck, null, null, null, null, null));
            }
            return (RStringVector) getNamesNodeRecursive.executeNamesGet(frame, vector, positions, currentDimLevel, allNull, names);
        }

        protected GetNamesNode(NACheck namesNACheck) {
            this.namesNACheck = namesNACheck;
        }

        protected GetNamesNode(GetNamesNode other) {
            this.namesNACheck = other.namesNACheck;
        }

        @Specialization
        RStringVector getNames(VirtualFrame frame, RAbstractVector vector, Object[] positions, int currentDimLevel, byte allNull, RStringVector names) {
            return getNamesInternal(frame, vector, positions, currentDimLevel, allNull, names);
        }

        @Specialization
        RStringVector getNamesNull(VirtualFrame frame, RAbstractVector vector, Object[] positions, int currentDimLevel, byte allNull, @SuppressWarnings("unused") RNull names) {
            return getNamesInternal(frame, vector, positions, currentDimLevel, allNull, null);
        }

        RStringVector getNamesInternal(VirtualFrame frame, RAbstractVector vector, Object[] positions, int currentDimLevel, byte allNull, RStringVector names) {
            RIntVector p = (RIntVector) positions[currentDimLevel - 1];
            int numPositions = p.getLength();
            RList dimNames = vector.getDimNames();
            Object srcNames = dimNames == null ? RNull.instance : (dimNames.getDataAt(currentDimLevel - 1) == RNull.instance ? RNull.instance : dimNames.getDataAt(currentDimLevel - 1));
            RStringVector newNames = null;
            if (numPositions > 0) {
                if (numPositions == 1 && p.getDataAt(0) == 0) {
                    return null;
                } else {
                    newNames = getNamesVector(srcNames, p, numPositions, namesNACheck);
                }
            }
            if (numPositions > 1) {
                return newNames;
            }
            byte newAllNull = allNull;
            if (newNames != null) {
                if (names != null) {
                    newAllNull = RRuntime.LOGICAL_FALSE;
                }
            } else {
                newNames = names;
            }
            if (currentDimLevel == 1) {
                if (newAllNull == RRuntime.LOGICAL_TRUE) {
                    return newNames != null ? newNames : (names != null ? names : null);
                } else {
                    return null;
                }
            } else {
                return getNamesRecursive(frame, vector, positions, currentDimLevel - 1, newAllNull, newNames == null ? RNull.instance : newNames, namesNACheck);
            }
        }
    }

    @NodeChildren({@NodeChild(value = "dimNames", type = RNode.class), @NodeChild(value = "vec", type = RNode.class), @NodeChild(value = "pos", type = RNode.class),
                    @NodeChild(value = "srcDimLevel", type = RNode.class), @NodeChild(value = "dstDimLevel", type = RNode.class)})
    protected abstract static class GetDimNamesNode extends RNode {

        public abstract Object executeDimNamesGet(VirtualFrame frame, RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel);

        private final NACheck namesNACheck;

        @Child private GetDimNamesNode getDimNamesNodeRecursive;

        private RStringVector getDimNamesRecursive(VirtualFrame frame, RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel, NACheck namesCheck) {
            if (getDimNamesNodeRecursive == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDimNamesNodeRecursive = insert(GetDimNamesNodeFactory.create(namesCheck, null, null, null, null, null));
            }
            return (RStringVector) getDimNamesNodeRecursive.executeDimNamesGet(frame, dstDimNames, vector, positions, currentSrcDimLevel, currentDstDimLevel);
        }

        protected GetDimNamesNode(NACheck namesNACheck) {
            this.namesNACheck = namesNACheck;
        }

        protected GetDimNamesNode(GetDimNamesNode other) {
            this.namesNACheck = other.namesNACheck;
        }

        @Specialization
        Object getDimNames(VirtualFrame frame, RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel) {
            if (currentSrcDimLevel == 0) {
                return null;
            }
            RIntVector p = (RIntVector) positions[currentSrcDimLevel - 1];
            int numPositions = p.getLength();
            if (numPositions > 1) {
                RList srcDimNames = vector.getDimNames();
                RStringVector srcNames = srcDimNames == null ? null : (srcDimNames.getDataAt(currentSrcDimLevel - 1) == RNull.instance ? null
                                : (RStringVector) srcDimNames.getDataAt(currentSrcDimLevel - 1));
                if (srcNames == null) {
                    dstDimNames.updateDataAt(currentDstDimLevel - 1, RNull.instance, null);
                } else {
                    namesNACheck.enable(!srcNames.isComplete() || !p.isComplete());
                    String[] namesData = new String[numPositions];
                    for (int i = 0; i < p.getLength(); i++) {
                        int pos = p.getDataAt(i);
                        if (namesNACheck.check(pos)) {
                            namesData[i] = RRuntime.STRING_NA;
                        } else {
                            namesData[i] = srcNames.getDataAt(pos - 1);
                            namesNACheck.check(namesData[i]);
                        }
                    }
                    RStringVector dstNames = RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA());
                    dstDimNames.updateDataAt(currentDstDimLevel - 1, dstNames, null);
                }
                getDimNamesRecursive(frame, dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel - 1, namesNACheck);
            } else {
                if (p.getDataAt(0) == 0) {
                    dstDimNames.updateDataAt(currentDstDimLevel - 1, RNull.instance, null);
                    getDimNamesRecursive(frame, dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel - 1, namesNACheck);
                } else {
                    getDimNamesRecursive(frame, dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel, namesNACheck);
                }
            }
            return null;
        }
    }

    @NodeChildren({@NodeChild(value = "data", type = RNode.class), @NodeChild(value = "vec", type = RNode.class), @NodeChild(value = "pos", type = RNode.class),
                    @NodeChild(value = "currDimLevel", type = RNode.class), @NodeChild(value = "srcArrayBase", type = RNode.class), @NodeChild(value = "dstArrayBase", type = RNode.class),
                    @NodeChild(value = "accSrcDimensions", type = RNode.class), @NodeChild(value = "accDstDimensions", type = RNode.class)})
    protected abstract static class GetMultiDimDataNode extends RNode {

        public abstract Object executeMultiDimDataGet(VirtualFrame frame, Object data, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                        int accSrcDimensions, int accDstDimensions);

        private final NACheck posNACheck;
        private final NACheck elementNACheck;

        @Child private GetMultiDimDataNode getMultiDimDataRecursive;

        private Object getMultiDimData(VirtualFrame frame, Object data, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                        int accDstDimensions, NACheck posCheck, NACheck elementCheck) {
            if (getMultiDimDataRecursive == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getMultiDimDataRecursive = insert(GetMultiDimDataNodeFactory.create(posCheck, elementCheck, null, null, null, null, null, null, null, null));
            }
            return getMultiDimDataRecursive.executeMultiDimDataGet(frame, data, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
        }

        protected GetMultiDimDataNode(NACheck posNACheck, NACheck elementNACheck) {
            this.posNACheck = posNACheck;
            this.elementNACheck = elementNACheck;
        }

        protected GetMultiDimDataNode(GetMultiDimDataNode other) {
            this.posNACheck = other.posNACheck;
            this.elementNACheck = other.elementNACheck;
        }

        @Specialization(order = 1)
        RList getData(VirtualFrame frame, Object d, RList vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
            Object[] data = (Object[]) d;
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
                    if (srcIndex == -1) {
                        data[dstIndex] = RNull.instance;
                    } else {
                        data[dstIndex] = vector.getDataAt(srcIndex);
                    }
                }
            } else {
                for (int i = 0; i < p.getLength(); i++) {
                    int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                    int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                    getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
                }
            }
            return vector;
        }

        @Specialization(order = 2)
        RAbstractIntVector getData(VirtualFrame frame, Object d, RAbstractIntVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                        int accDstDimensions) {
            int[] data = (int[]) d;
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
                    if (srcIndex == -1) {
                        data[dstIndex] = RRuntime.INT_NA;
                    } else {
                        data[dstIndex] = vector.getDataAt(srcIndex);
                        elementNACheck.check(data[dstIndex]);
                    }
                }
            } else {
                for (int i = 0; i < p.getLength(); i++) {
                    int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                    int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                    getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
                }
            }
            return vector;
        }

        @Specialization(order = 3)
        RAbstractDoubleVector getData(VirtualFrame frame, Object d, RAbstractDoubleVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                        int accDstDimensions) {
            double[] data = (double[]) d;
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
                    if (srcIndex == -1) {
                        data[dstIndex] = RRuntime.DOUBLE_NA;
                    } else {
                        data[dstIndex] = vector.getDataAt(srcIndex);
                        elementNACheck.check(data[dstIndex]);
                    }
                }
            } else {
                for (int i = 0; i < p.getLength(); i++) {
                    int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                    int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                    getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
                }
            }
            return vector;
        }

        @Specialization(order = 4)
        RAbstractLogicalVector getData(VirtualFrame frame, Object d, RAbstractLogicalVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                        int accDstDimensions) {
            byte[] data = (byte[]) d;
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
                    if (srcIndex == -1) {
                        data[dstIndex] = RRuntime.LOGICAL_NA;
                    } else {
                        data[dstIndex] = vector.getDataAt(srcIndex);
                        elementNACheck.check(data[dstIndex]);
                    }
                }
            } else {
                for (int i = 0; i < p.getLength(); i++) {
                    int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                    int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                    getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
                }
            }
            return vector;
        }

        @Specialization(order = 5)
        RAbstractStringVector getData(VirtualFrame frame, Object d, RAbstractStringVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                        int accDstDimensions) {
            String[] data = (String[]) d;
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
                    if (srcIndex == -1) {
                        data[dstIndex] = RRuntime.STRING_NA;
                    } else {
                        data[dstIndex] = vector.getDataAt(srcIndex);
                        elementNACheck.check(data[dstIndex]);
                    }
                }
            } else {
                for (int i = 0; i < p.getLength(); i++) {
                    int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                    int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                    getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
                }
            }
            return vector;
        }

        @Specialization(order = 6)
        RAbstractComplexVector getData(VirtualFrame frame, Object d, RAbstractComplexVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                        int accDstDimensions) {
            double[] data = (double[]) d;
            int[] srcDimensions = vector.getDimensions();
            RIntVector p = (RIntVector) positions[currentDimLevel - 1];
            int srcDimSize = srcDimensions[currentDimLevel - 1];
            int newAccSrcDimensions = accSrcDimensions / srcDimSize;
            int newAccDstDimensions = accDstDimensions / p.getLength();
            elementNACheck.enable(p);
            if (currentDimLevel == 1) {
                for (int i = 0; i < p.getLength(); i++) {
                    int dstIndex = (dstArrayBase + newAccDstDimensions * i) << 1;
                    int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                    if (srcIndex == -1) {
                        data[dstIndex] = RRuntime.COMPLEX_NA_REAL_PART;
                        data[dstIndex + 1] = RRuntime.COMPLEX_NA_IMAGINARY_PART;
                    } else {
                        data[dstIndex] = vector.getDataAt(srcIndex).getRealPart();
                        data[dstIndex + 1] = vector.getDataAt(srcIndex).getImaginaryPart();
                        elementNACheck.check(data[dstIndex]);
                    }
                }
            } else {
                for (int i = 0; i < p.getLength(); i++) {
                    int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                    int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                    getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
                }
            }
            return vector;
        }

        @Specialization(order = 7)
        RAbstractRawVector getData(VirtualFrame frame, Object d, RAbstractRawVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                        int accDstDimensions) {
            byte[] data = (byte[]) d;
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
                    if (srcIndex == -1) {
                        data[dstIndex] = 0;
                    } else {
                        data[dstIndex] = vector.getDataAt(srcIndex).getValue();
                    }
                }
            } else {
                for (int i = 0; i < p.getLength(); i++) {
                    int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                    int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                    getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
                }
            }
            return vector;
        }

        private int getNewArrayBase(int srcArrayBase, RIntVector p, int i, int newAccSrcDimensions) {
            int newSrcArrayBase;
            if (srcArrayBase == -1) {
                newSrcArrayBase = -1;
            } else {
                int pos = p.getDataAt(i);
                if (elementNACheck.check(pos)) {
                    newSrcArrayBase = -1;
                } else {
                    newSrcArrayBase = srcArrayBase + newAccSrcDimensions * (pos - 1);
                }
            }
            return newSrcArrayBase;
        }

        private int getSrcIndex(int srcArrayBase, RIntVector p, int i, int newAccSrcDimensions) {
            if (srcArrayBase == -1) {
                return -1;
            } else {
                int pos = p.getDataAt(i);
                if (elementNACheck.check(pos)) {
                    return -1;
                } else {
                    return srcArrayBase + newAccSrcDimensions * (pos - 1);
                }
            }
        }

    }

    @NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "operand", type = RNode.class)})
    public abstract static class MultiDimPosConverterNode extends RNode {

        public abstract RIntVector executeConvert(VirtualFrame frame, Object vector, Object p);

        private final boolean isSubset;

        protected MultiDimPosConverterNode(boolean isSubset) {
            this.isSubset = isSubset;
        }

        protected MultiDimPosConverterNode(MultiDimPosConverterNode other) {
            this.isSubset = other.isSubset;
        }

        @Specialization(order = 1, guards = {"!singleOpNegative", "!multiPos"})
        public RAbstractIntVector doIntVector(@SuppressWarnings("unused") Object vector, RAbstractIntVector positions) {
            return positions;
        }

        @Specialization(order = 2, guards = {"!singleOpNegative", "multiPos"})
        public RAbstractIntVector doIntVectorMultiPos(@SuppressWarnings("unused") Object vector, RAbstractIntVector positions) {
            if (isSubset) {
                return positions;
            } else {
                throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
            }
        }

        @SuppressWarnings("unused")
        @Specialization(order = 3, guards = {"singleOpNA"})
        public RAbstractIntVector doIntVectorNA(Object vector, RAbstractIntVector positions) {
            if (isSubset) {
                return positions;
            } else {
                throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
            }
        }

        @SuppressWarnings("unused")
        @Specialization(order = 4, guards = {"singleOpNegative", "!singleOpNA"})
        public RAbstractIntVector doIntVectorNegative(Object vector, RAbstractIntVector positions) {
            throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
        }

        @SuppressWarnings("unused")
        @Specialization(order = 10, guards = "noPosition")
        Object accessListEmptyPos(RAbstractVector vector, RList positions) {
            if (!isSubset) {
                throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
            } else {
                throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "list");
            }
        }

        @SuppressWarnings("unused")
        @Specialization(order = 11, guards = "onePosition")
        Object accessListOnePos(RAbstractVector vector, RList positions) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "list");
        }

        @SuppressWarnings("unused")
        @Specialization(order = 12, guards = "multiPos")
        Object accessListMultiPos(RAbstractVector vector, RList positions) {
            if (!isSubset) {
                throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
            } else {
                throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "list");
            }
        }

        @SuppressWarnings("unused")
        @Specialization(order = 20)
        Object accessListOnePos(RAbstractVector vector, RComplex positions) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "complex");
        }

        @SuppressWarnings("unused")
        @Specialization(order = 30)
        Object accessListOnePos(RAbstractVector vector, RRaw positions) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "raw");
        }

        @SuppressWarnings("unused")
        protected static boolean singleOpNegative(Object vector, RAbstractIntVector p) {
            return p.getLength() == 1 && p.getDataAt(0) < 0;
        }

        @SuppressWarnings("unused")
        protected static boolean singleOpNA(Object vector, RAbstractIntVector p) {
            return p.getLength() == 1 && RRuntime.isNA(p.getDataAt(0));
        }

        @SuppressWarnings("unused")
        protected static boolean onePosition(RAbstractVector vector, RAbstractVector p) {
            return p.getLength() == 1;
        }

        @SuppressWarnings("unused")
        protected static boolean noPosition(RAbstractVector vector, RAbstractVector p) {
            return p.getLength() == 0;
        }

        @SuppressWarnings("unused")
        protected static boolean multiPos(RAbstractVector vector, RAbstractVector positions) {
            return positions.getLength() > 1;
        }

        @SuppressWarnings("unused")
        protected static boolean multiPos(Object vector, RAbstractVector positions) {
            return positions.getLength() > 1;
        }
    }

}
