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
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.access.ArrayPositionCastFactory.OperatorConverterNodeFactory;
import com.oracle.truffle.r.nodes.access.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.function.RCallNode.VarArgsAsObjectArrayNodeFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "recursionLevel", type = RNode.class), @NodeChild(value = "positions", type = RNode[].class)})
public abstract class AccessArrayNode extends RNode {

    private final boolean isSubset;

    private final NACheck elementNACheck = NACheck.create();
    private final NACheck namesNACheck = NACheck.create();

    @Child private AccessArrayNode accessRecursive;
    @Child private CastToVectorNode castVector;
    @Child private ArrayPositionCast castPosition;
    @Child private OperatorConverterNode operatorConverter;

    private static final VarArgsAsObjectArrayNodeFactory varArgAsObjectArrayNodeFactory = new VarArgsAsObjectArrayNodeFactory();

    abstract RNode getVector();

    public abstract Object executeAccess(VirtualFrame frame, RAbstractVector vector, int recLevel, Object operand);

    public abstract Object executeAccess(VirtualFrame frame, RAbstractVector vector, int recLevel, int operand);

    public AccessArrayNode(boolean isSubset) {
        this.isSubset = isSubset;
    }

    public AccessArrayNode(AccessArrayNode other) {
        this.isSubset = other.isSubset;
    }

    private Object accessRecursive(VirtualFrame frame, RAbstractVector vector, Object operand, int recLevel) {
        if (accessRecursive == null) {
            CompilerDirectives.transferToInterpreter();
            accessRecursive = adoptChild(AccessArrayNodeFactory.create(this.isSubset, null, null, null));
        }
        return executeAccess(frame, vector, recLevel, operand);
    }

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreter();
            castVector = adoptChild(CastToVectorNodeFactory.create(null, false, false, false));
        }
        return castVector.executeRAbstractVector(frame, value).materialize();
    }

    private Object castPosition(VirtualFrame frame, RAbstractVector vector, Object operand) {
        if (castPosition == null) {
            CompilerDirectives.transferToInterpreter();
            castPosition = adoptChild(ArrayPositionCastFactory.create(0, 1, false, false, null, ConstantNode.create(RNull.instance) /* dummy */, null));
        }
        return castPosition.executeArg(frame, vector, null, operand);
    }

    private Object convertOperand(VirtualFrame frame, RAbstractVector vector, int operand) {
        if (operatorConverter == null) {
            CompilerDirectives.transferToInterpreter();
            operatorConverter = adoptChild(OperatorConverterNodeFactory.create(0, 1, false, false, null, ConstantNode.create(RNull.instance) /* dummy */, null));
        }
        return operatorConverter.executeConvert(frame, vector, operand, null);
    }

    @CreateCast({"vector"})
    public RNode createCastVector(RNode child) {
        return CastToVectorNodeFactory.create(child, false, false, true);
    }

    @CreateCast({"positions"})
    public RNode[] createCastPositions(RNode[] children) {
        if (children == null) {
            return null;
        }
        RNode[] positions = new RNode[children.length];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = ArrayPositionCastFactory.create(i, positions.length, false, isSubset, getVector(), ConstantNode.create(RNull.instance) /* dummy */, children[i]);
        }
        return new RNode[]{varArgAsObjectArrayNodeFactory.makeList(positions, null)};
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1)
    RNull access(RNull vector, int recLevel, Object positions) {
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 2)
    RNull access(RAbstractVector vector, int recLevel, RNull positions) {
        // this is a special case (see ArrayPositionCast) - RNull can only appear to represent the
        // x[[NA]] case which has to return null and not a null vector
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 3)
    Object access(RAbstractVector vector, int recLevel, RMissing positions) {
        if (!isSubset) {
            throw RError.getInvalidSubscriptType(getEncapsulatingSourceSection(), "symbol");
        } else {
            return vector;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(order = 4, guards = "wrongDimensions")
    Object access(RAbstractVector vector, int recLevel, Object[] positions) {
        throw RError.getIncorrectDimensions(getEncapsulatingSourceSection());
    }

    private RStringVector getNamesVector(Object srcNamesObject, RIntVector p, int resLength) {
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

    @SlowPath
    private RStringVector getNames(RAbstractVector vector, Object[] positions, int currentDimLevel) {
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int numPositions = p.getLength();
        if (numPositions > 1) {
            RList dimNames = vector.getDimNames();
            Object srcNames = dimNames == null ? RNull.instance : (dimNames.getDataAt(currentDimLevel - 1) == RNull.instance ? RNull.instance : dimNames.getDataAt(currentDimLevel - 1));
            return getNamesVector(srcNames, p, numPositions);
        }
        if (currentDimLevel == 1) {
            return null;
        } else {
            return getNames(vector, positions, currentDimLevel - 1);
        }
    }

    @SlowPath
    private void getDimNames(RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel) {
        if (currentSrcDimLevel == 0) {
            return;
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
            getDimNames(dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel - 1);
        } else {
            if (p.getDataAt(0) == 0) {
                dstDimNames.updateDataAt(currentDstDimLevel - 1, RNull.instance, null);
                getDimNames(dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel - 1);
            } else {
                getDimNames(dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel);
            }
        }
    }

    private RStringVector getName(RAbstractVector vector, int position) {
        RStringVector srcNames = (RStringVector) vector.getNames();
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

    private static DimsAndResultLength getDimsAndResultLength(Object[] positions) {
        int dimLength = 0;
        int resLength = 1;
        int multi = 0; // how many times # positions > 1 ?
        int zero = 0; // how many times a position is 0
        for (int i = 0; i < positions.length; i++) {
            RIntVector p = (RIntVector) positions[i];
            int pLength = p.getLength();
            if (pLength == 1 && p.getDataAt(0) != 0) {
                continue;
            }
            if (pLength > 1) {
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

        if (dimLength > 0 && ((zero > 1 || multi > 1) || (zero > 0 && multi > 0))) {
            dimensions = new int[dimLength];
            int ind = 0;
            for (int i = 0; i < positions.length; i++) {
                RIntVector p = (RIntVector) positions[i];
                int pLength = p.getLength();
                if (pLength == 1 && p.getDataAt(0) != 0) {
                    continue;
                }
                if (pLength > 1) {
                    dimensions[ind++] = pLength;
                } else {
                    dimensions[ind++] = 0;
                }
            }
        }
        return new DimsAndResultLength(dimensions, resLength);
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

    private int getSrcArrayBase(int pos, int accSrcDimensions) {
        if (elementNACheck.check(pos)) {
            return -1; // fill with NAs at the lower levels
        } else {
            return accSrcDimensions * (pos - 1);
        }
    }

    @SlowPath
    private void getData(int[] data, RAbstractIntVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
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
                getData(data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    @SlowPath
    private void getData(Object[] data, RList vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
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
                getData(data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    @Specialization(order = 10)
    RList access(RList vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions);
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
                getData(data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createList(data, dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(vector, positions, numSrcDimensions);
                return RDataFactory.createList(data, dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RList resultVector = RDataFactory.createList(data, dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(dstDimNames, vector, positions, numSrcDimensions, numDstDimensions);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @Specialization(order = 11, guards = "isSubset")
    RList accessSubset(RList vector, @SuppressWarnings("unused") int recLevel, RIntVector p) {
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
        RStringVector names = getNamesVector(vector.getNames(), p, resLength);
        return RDataFactory.createList(data, names);
    }

    // lists require special handling for one-dimensional "subscript", that is [[]], accesses due to
    // support for recursive access

    @Specialization(order = 12, guards = {"!isSubset", "onePosition", "!inRecursion"})
    Object accessOnePos(RList vector, @SuppressWarnings("unused") int recLevel, RIntVector p) {
        int position = p.getDataAt(0);
        if (RRuntime.isNA(position)) {
            return RNull.instance;
        } else if (position <= 0) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        } else if (position > vector.getLength()) {
            throw RError.getSubscriptBounds(getEncapsulatingSourceSection());
        }
        return vector.getDataAt(0);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 13, guards = {"!isSubset", "noPosition"})
    Object accessNoPos(RList vector, int recLevel, RIntVector p) {
        throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
    }

    @Specialization(order = 14, guards = {"!isSubset", "twoPosition"})
    Object accessOnePosRec(VirtualFrame frame, RList vector, int recLevel, RIntVector p) {
        int position = p.getDataAt(0);
        if (RRuntime.isNA(position) || position > vector.getLength()) {
            throw RError.getNoSuchIndexAtLevel(getEncapsulatingSourceSection(), recLevel + 1);
        } else if (position < 0) {
            throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
        } else if (position == 0) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        RAbstractVector newVector = castVector(frame, vector.getDataAt(position - 1));
        Object newPosition = castPosition(frame, newVector, convertOperand(frame, newVector, p.getDataAt(1)));
        return accessRecursive(frame, newVector, newPosition, recLevel + 1);
    }

    @Specialization(order = 15, guards = "!isSubset")
    Object access(VirtualFrame frame, RList vector, int recLevel, RIntVector p) {
        int position = p.getDataAt(0);
        if (RRuntime.isNA(position) || position > vector.getLength()) {
            throw RError.getNoSuchIndexAtLevel(getEncapsulatingSourceSection(), recLevel + 1);
        } else if (position < 0) {
            throw RError.getSelectMoreThanOne(getEncapsulatingSourceSection());
        } else if (position == 0) {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
        RAbstractVector newVector = castVector(frame, vector.getDataAt(position - 1));
        RIntVector newP = p.copyResized(p.getLength() - 1, false);
        return accessRecursive(frame, newVector, newP, recLevel + 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 16, guards = "isPositionNA")
    RList accessNA(RList vector, int recLevel, int position) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createList(new Object[]{RNull.instance});
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createList(new Object[]{RNull.instance}, names);
        }
    }

    @Specialization(order = 17, guards = {"!isPositionZero", "hasNames", "isSubset"})
    RList accessNames(RList vector, @SuppressWarnings("unused") int recLevel, int position) {
        Object val = vector.getDataAt(position - 1);
        return RDataFactory.createList(new Object[]{val}, getName(vector, position));
    }

    @Specialization(order = 18, guards = {"!isPositionZero", "!hasNames", "isSubset"})
    RList accessSubset(RList vector, @SuppressWarnings("unused") int recLevel, int position) {
        return RDataFactory.createList(new Object[]{vector.getDataAt(position - 1)});
    }

    @Specialization(order = 19, guards = {"!isPositionZero", "!hasNames", "!isSubset"})
    Object access(RList vector, @SuppressWarnings("unused") int recLevel, int position) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 20, guards = "isPositionZero")
    RList accessPosZero(RList vector, int recLevel, int position) {
        return RDataFactory.createList();
    }

    @SuppressWarnings("unused")
    @Specialization(order = 30, guards = {"!isSubset", "inRecursion", "multiPos"})
    Object accessRecFailed(RAbstractVector vector, int recLevel, RIntVector positions) {
        throw RError.getRecursiveIndexingFailed(getEncapsulatingSourceSection(), recLevel + 1);
    }

    @Specialization(order = 40)
    RIntVector access(RAbstractIntVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions);
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
                getData(data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(vector, positions, numSrcDimensions);
                return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RIntVector resultVector = RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(dstDimNames, vector, positions, numSrcDimensions, numDstDimensions);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @Specialization(order = 41)
    RIntVector access(RAbstractIntVector vector, @SuppressWarnings("unused") int recLevel, RIntVector p) {
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
        RStringVector names = getNamesVector(vector.getNames(), p, resLength);
        return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 42, guards = "isPositionNA")
    RIntVector accessNA(RAbstractIntVector vector, int recLevel, int position) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @Specialization(order = 43, guards = {"!isPositionZero", "hasNames", "isSubset"})
    RIntVector accessNames(RAbstractIntVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        int val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createIntVector(new int[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @Specialization(order = 44, guards = "!isPositionZero")
    int access(RAbstractIntVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 45, guards = "isPositionZero")
    RIntVector accessPosZero(RAbstractIntVector vector, int recLevel, int position) {
        return RDataFactory.createEmptyIntVector();
    }

    @SlowPath
    private void getData(double[] data, RAbstractDoubleVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
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
                getData(data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    @Specialization(order = 50)
    RDoubleVector access(RAbstractDoubleVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions);
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
                getData(data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(vector, positions, numSrcDimensions);
                return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RDoubleVector resultVector = RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(dstDimNames, vector, positions, numSrcDimensions, numDstDimensions);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @Specialization(order = 51)
    RDoubleVector access(RAbstractDoubleVector vector, @SuppressWarnings("unused") int recLevel, RIntVector p) {
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
        RStringVector names = getNamesVector(vector.getNames(), p, resLength);
        return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 52, guards = "isPositionNA")
    RDoubleVector accessNA(RAbstractDoubleVector vector, int recLevel, int position) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @Specialization(order = 53, guards = {"!isPositionZero", "hasNames", "isSubset"})
    RDoubleVector accessNames(RAbstractDoubleVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        double val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createDoubleVector(new double[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @Specialization(order = 54, guards = "!isPositionZero")
    double access(RAbstractDoubleVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 55, guards = "isPositionZero")
    RDoubleVector accessPosZero(RAbstractDoubleVector vector, int recLevel, int position) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @SlowPath
    private void getData(byte[] data, RLogicalVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
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
                getData(data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    @Specialization(order = 60)
    RLogicalVector access(RLogicalVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions);
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
                getData(data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(vector, positions, numSrcDimensions);
                return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RLogicalVector resultVector = RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(dstDimNames, vector, positions, numSrcDimensions, numDstDimensions);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @Specialization(order = 61)
    RLogicalVector access(RLogicalVector vector, @SuppressWarnings("unused") int recLevel, RIntVector p) {
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
        RStringVector names = getNamesVector(vector.getNames(), p, resLength);
        return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 62, guards = "isPositionNA")
    RLogicalVector accessNA(RLogicalVector vector, int recLevel, int position) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @Specialization(order = 63, guards = {"!isPositionZero", "hasNames", "isSubset"})
    RLogicalVector accessNames(RAbstractLogicalVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        byte val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createLogicalVector(new byte[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @Specialization(order = 64, guards = "!isPositionZero")
    byte access(RLogicalVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 65, guards = "isPositionZero")
    RLogicalVector accessPosZero(RLogicalVector vector, int recLevel, int position) {
        return RDataFactory.createEmptyLogicalVector();
    }

    @SlowPath
    private void getData(String[] data, RStringVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
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
                getData(data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    @Specialization(order = 70)
    RStringVector access(RStringVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions);
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
                getData(data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(vector, positions, numSrcDimensions);
                return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RStringVector resultVector = RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(dstDimNames, vector, positions, numSrcDimensions, numDstDimensions);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @Specialization(order = 71)
    RStringVector access(RStringVector vector, @SuppressWarnings("unused") int recLevel, RIntVector p) {
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
        RStringVector names = getNamesVector(vector.getNames(), p, resLength);
        return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 72, guards = "isPositionNA")
    RStringVector accessNA(RStringVector vector, int recLevel, int position) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @Specialization(order = 73, guards = {"!isPositionZero", "hasNames", "isSubset"})
    RStringVector accessNames(RAbstractStringVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        String val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createStringVector(new String[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @Specialization(order = 74, guards = "!isPositionZero")
    String access(RStringVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 75, guards = "isPositionZero")
    RStringVector accessPosZero(RStringVector vector, int recLevel, int position) {
        return RDataFactory.createEmptyStringVector();
    }

    @SlowPath
    private void getDataComplex(double[] data, RComplexVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
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
                getDataComplex(data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    @Specialization(order = 80)
    RComplexVector access(RComplexVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions);
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
                getDataComplex(data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(vector, positions, numSrcDimensions);
                return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RComplexVector resultVector = RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(dstDimNames, vector, positions, numSrcDimensions, numDstDimensions);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @Specialization(order = 81)
    RComplexVector access(RComplexVector vector, @SuppressWarnings("unused") int recLevel, RIntVector p) {
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
        RStringVector names = getNamesVector(vector.getNames(), p, resLength);
        return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 82, guards = "isPositionNA")
    RComplexVector accessNA(RComplexVector vector, int recLevel, int position) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @Specialization(order = 83, guards = {"!isPositionZero", "hasNames", "isSubset"})
    RComplexVector accessNames(RAbstractComplexVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        RComplex val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createComplexVector(new double[]{val.getRealPart(), val.getImaginaryPart()}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @Specialization(order = 84, guards = "!isPositionZero")
    RComplex access(RComplexVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 85, guards = "isPositionZero")
    RComplexVector accessPosZero(RComplexVector vector, int recLevel, int position) {
        return RDataFactory.createEmptyComplexVector();
    }

    @SlowPath
    private void getDataRaw(byte[] data, RRawVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
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
                getDataRaw(data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    @Specialization(order = 90)
    RRawVector access(RRawVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        // compute length of dimensions array and of the resulting vector
        DimsAndResultLength res = getDimsAndResultLength(positions);
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
                getDataRaw(data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createRawVector(data, dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(vector, positions, numSrcDimensions);
                return RDataFactory.createRawVector(data, dimensions, names);
            } else {
                // construct dimnames
                int dimLength = dimensions.length;
                RRawVector resultVector = RDataFactory.createRawVector(data, dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(dstDimNames, vector, positions, numSrcDimensions, numDstDimensions);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    @Specialization(order = 91)
    RRawVector access(RRawVector vector, @SuppressWarnings("unused") int recLevel, RIntVector p) {
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
        RStringVector names = getNamesVector(vector.getNames(), p, resLength);
        return RDataFactory.createRawVector(data, names);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 92, guards = "isPositionNA")
    RRawVector accessNA(RRawVector vector, int recLevel, int position) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createRawVector(new byte[]{0});
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createRawVector(new byte[]{0}, names);
        }
    }

    @Specialization(order = 93, guards = {"!isPositionZero", "hasNames", "isSubset"})
    RRawVector accessNames(RAbstractRawVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        RRaw val = vector.getDataAt(position - 1);
        return RDataFactory.createRawVector(new byte[]{val.getValue()}, getName(vector, position));
    }

    @Specialization(order = 94, guards = "!isPositionZero")
    RRaw access(RRawVector vector, @SuppressWarnings("unused") int recLevel, int position) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 95, guards = "isPositionZero")
    RRawVector accessPosZero(RRawVector vector, int recLevel, int position) {
        return RDataFactory.createEmptyRawVector();
    }

    protected boolean wrongDimensions(RAbstractVector vector, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return vector.getDimensions() == null || vector.getDimensions().length != positions.length;
    }

    @SuppressWarnings("unused")
    protected boolean isPositionZero(RAbstractVector vector, int recLevel, int position) {
        return position == 0;
    }

    @SuppressWarnings("unused")
    protected boolean isPositionNA(RAbstractVector vector, int recLevel, int position) {
        return RRuntime.isNA(position);
    }

    protected boolean isSubset() {
        return isSubset;
    }

    @SuppressWarnings("unused")
    protected boolean hasNames(RAbstractVector vector, int recLevel, int position) {
        return vector.getNames() != RNull.instance;
    }

    @SuppressWarnings("unused")
    protected boolean twoPosition(RList vector, int recLevel, RIntVector p) {
        return p.getLength() == 2;
    }

    @SuppressWarnings("unused")
    protected boolean onePosition(RList vector, int recLevel, RIntVector p) {
        return p.getLength() == 1;
    }

    @SuppressWarnings("unused")
    protected boolean noPosition(RList vector, int recLevel, RIntVector p) {
        return p.getLength() == 0;
    }

    @SuppressWarnings("unused")
    protected boolean inRecursion(RAbstractVector vector, int recLevel, RIntVector positions) {
        return recLevel > 0;
    }

    @SuppressWarnings("unused")
    protected boolean multiPos(RAbstractVector vector, int recLevel, RIntVector positions) {
        return positions.getLength() > 1;
    }

    public static AccessArrayNode create(boolean isSubset, RNode vector, RNode[] positions) {
        return AccessArrayNodeFactory.create(isSubset, vector, ConstantNode.create(0), positions);
    }
}
