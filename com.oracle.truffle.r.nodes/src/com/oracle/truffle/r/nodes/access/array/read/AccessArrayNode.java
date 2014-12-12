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
package com.oracle.truffle.r.nodes.access.array.read;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.array.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCastFactory.OperatorConverterNodeFactory;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.RError.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "exact", type = RNode.class), @NodeChild(value = "recursionLevel", type = RNode.class),
                @NodeChild(value = "positions", type = PositionsArrayNode.class, executeWith = {"vector", "exact"}), @NodeChild(value = "dropDim", type = RNode.class)})
public abstract class AccessArrayNode extends RNode {

    private final boolean isSubset;
    private final boolean exactInSource;
    private final boolean dropInSource;

    private final NACheck elementNACheck = NACheck.create();
    private final NACheck posNACheck = NACheck.create();
    private final NACheck namesNACheck = NACheck.create();

    private final ConditionProfile dimensionsIsNullProfile = ConditionProfile.createBinaryProfile();

    private final BranchProfile error = BranchProfile.create();

    private final BranchProfile multiPosition = BranchProfile.create();
    private final BranchProfile onePosition = BranchProfile.create();
    private final BranchProfile emptyPosition = BranchProfile.create();
    private final ConditionProfile dropDimProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile withDimensions = BranchProfile.create();
    private final ConditionProfile emptyResultProfile = ConditionProfile.createBinaryProfile();

    @CompilationFinal private boolean recursiveIsSubset;

    @Child private AccessArrayNode accessRecursive;
    @Child private CastToVectorNode castVector;
    @Child private ArrayPositionCast castPosition;
    @Child private OperatorConverterNode operatorConverter;
    @Child private GetMultiDimDataNode getMultiDimData;
    @Child private GetNamesNode getNamesNode;
    @Child private GetDimNamesNode getDimNamesNode;
    @Child ContainerRowNamesGet rowNamesGetter;

    protected abstract RNode getVector();

    protected abstract RNode getPositions();

    protected abstract RNode getExact();

    protected abstract RNode getDropDim();

    protected abstract RNode getRecursionLevel();

    public abstract Object executeAccess(VirtualFrame frame, Object vector, Object exact, int recLevel, Object operand, RAbstractLogicalVector dropDim);

    public abstract Object executeAccess(VirtualFrame frame, Object vector, Object exact, int recLevel, int operand, RAbstractLogicalVector dropDim);

    @Override
    public boolean isSyntax() {
        return true;
    }

    @Override
    public void deparse(State state) {
        getVector().deparse(state);
        state.append(isSubset ? "[" : "[[");
        getPositions().deparse(state);
        if (exactInSource) {
            state.append(", exact = ");
            getExact().deparse(state);
        }
        if (dropInSource) {
            state.append(", drop = ");
            getDropDim().deparse(state);
        }
        state.append(isSubset ? "]" : "]]");
    }

    @Override
    public RNode substitute(REnvironment env) {
        RNode vector = getVector().substitute(env);
        PositionsArrayNode positions = (PositionsArrayNode) getPositions().substitute(env);
        RNode exact = getExact().substitute(env);
        RNode dropDim = getDropDim().substitute(env);
        return AccessArrayNodeFactory.create(isSubset, true, true, vector, exact, getRecursionLevel(), positions, dropDim);
    }

    public AccessArrayNode(boolean isSubset, boolean exactInSource, boolean dropInSource) {
        this.isSubset = isSubset;
        this.recursiveIsSubset = isSubset;
        this.exactInSource = exactInSource;
        this.dropInSource = dropInSource;
    }

    public AccessArrayNode(AccessArrayNode other) {
        this.isSubset = other.isSubset;
        this.recursiveIsSubset = other.recursiveIsSubset;
        this.exactInSource = other.exactInSource;
        this.dropInSource = other.dropInSource;
    }

    private Object accessRecursive(VirtualFrame frame, Object vector, Object exact, Object operand, int recLevel, RAbstractLogicalVector dropDim, boolean forDataFrame) {
        // for data frames, recursive update is the same as for lists but as if the [[]] operator
        // was used
        if (accessRecursive == null || (forDataFrame && isSubset) || (!forDataFrame && isSubset != recursiveIsSubset)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            boolean newIsSubset = this.isSubset;
            if (forDataFrame && isSubset) {
                newIsSubset = false;
            }
            accessRecursive = insert(AccessArrayNodeFactory.create(newIsSubset, false, false, null, null, null, null, null));
        }
        return accessRecursive.executeAccess(frame, vector, exact, recLevel, operand, dropDim);
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
            castPosition = insert(ArrayPositionCastFactory.create(0, 1, false, false, null, null));
        }
        return castPosition.executeArg(frame, vector, operand);
    }

    private void initOperatorConvert() {
        if (operatorConverter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            operatorConverter = insert(OperatorConverterNodeFactory.create(0, 1, false, false, null, null, null));
        }
    }

    private Object convertOperand(VirtualFrame frame, Object vector, int operand, Object exact) {
        initOperatorConvert();
        return operatorConverter.executeConvert(frame, vector, operand, exact);
    }

    private Object convertOperand(VirtualFrame frame, Object vector, String operand, Object exact) {
        initOperatorConvert();
        return operatorConverter.executeConvert(frame, vector, operand, exact);
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
            getNamesNode = insert(GetNamesNodeFactory.create(namesCheck, null, null, null, null));
        }
        return (RStringVector) getNamesNode.executeNamesGet(frame, vector, positions, currentDimLevel, RNull.instance);
    }

    private RStringVector getDimNames(VirtualFrame frame, RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel, NACheck namesCheck) {
        if (getDimNamesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getDimNamesNode = insert(GetDimNamesNodeFactory.create(namesCheck, null, null, null, null, null));
        }
        return (RStringVector) getDimNamesNode.executeDimNamesGet(frame, dstDimNames, vector, positions, currentSrcDimLevel, currentDstDimLevel);
    }

    private Object getContainerRowNames(VirtualFrame frame, RAbstractContainer value) {
        if (rowNamesGetter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rowNamesGetter = insert(ContainerRowNamesGetFactory.create(null));
        }
        return rowNamesGetter.execute(frame, value);
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

    // TODO: ultimately factor accesses should be turned into generic function
    @Specialization
    protected Object accessFactor(VirtualFrame frame, RFactor factor, Object exact, int recLevel, Object position, RAbstractLogicalVector dropDim) {
        RIntVector res = (RIntVector) castVector(frame, accessRecursive(frame, factor.getVector(), exact, position, recLevel, dropDim, false));
        if (res == RDataFactory.createEmptyIntVector()) {
            res = RDataFactory.createIntVector(0);
        }
        res.setLevels(factor.getLevels());
        return RVector.setVectorClassAttr(res, RDataFactory.createStringVector("factor"), null, null);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion", "isFirstPositionPositive"})
    protected RNull accessNullInRecursionPosPositive(RNull vector, Object exact, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion", "!isFirstPositionPositive"})
    protected RNull accessNullInRecursion(RNull vector, Object exact, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RNull access(RNull vector, Object exact, int recLevel, Object positions, RAbstractLogicalVector dropDim) {
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion", "isFirstPositionOne"})
    protected RNull accessFunctionInRecursionPosOne(RFunction vector, Object exact, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_LENGTH, "closure", 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion", "isFirstPositionPositive", "!isFirstPositionOne"})
    protected RNull accessFunctionInRecursionPosPositive(RFunction vector, Object exact, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion", "!isFirstPositionPositive"})
    protected RNull accessFunctionInRecursion(RFunction vector, Object exact, int recLevel, RAbstractIntVector positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "inRecursion")
    protected RNull accessFunctionInRecursionString(RFunction vector, Object exact, int recLevel, RAbstractStringVector positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RNull accessFunction(RFunction vector, Object exact, int recLevel, Object position, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.OBJECT_NOT_SUBSETTABLE, "closure");
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RNull access(RAbstractContainer container, Object exact, int recLevel, RNull positions, RAbstractLogicalVector dropDim) {
        // this is a special case (see ArrayPositionCast) - RNull can only appear to represent the
        // x[NA] case which has to return null and not a null vector
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object access(RAbstractContainer container, Object exact, int recLevel, RMissing positions, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "symbol");
        } else {
            return container;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "wrongDimensions")
    protected Object access(RAbstractVector container, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_DIMENSIONS);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isPositionNA", "!isSubset"})
    protected RIntVector accessNA(RAbstractContainer container, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    private RStringVector getName(RAbstractVector vector, int position) {
        RStringVector srcNames = (RStringVector) vector.getNames();
        namesNACheck.enable(srcNames);
        String name = srcNames.getDataAt(position - 1);
        namesNACheck.check(name);
        return RDataFactory.createStringVector(new String[]{name}, namesNACheck.neverSeenNA());
    }

    private static class DimsAndResultLength {
        @CompilationFinal public final int[] dimensions;
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
            if (dropDimProfile.profile(pLength == 1 && p.getDataAt(0) != 0)) {
                onePosition.enter();
                if (!isSubset || dropDim == RRuntime.LOGICAL_TRUE) {
                    // always drop dimensions with subscript
                    continue;
                } else {
                    single++;
                }
            } else if (pLength > 1) {
                multiPosition.enter();
                multi++;
                resLength *= pLength;
            } else {
                emptyPosition.enter();
                resLength = 0;
                zero++;
            }
            dimLength++;
        }
        // create dimensions array
        int[] dimensions = null;

        if (dimLength > 0 && ((zero > 1 || multi > 1) || (zero > 0 && multi > 0) || single > 0)) {
            withDimensions.enter();
            dimensions = new int[dimLength];
            int ind = 0;
            for (int i = 0; i < positions.length; i++) {
                RIntVector p = (RIntVector) positions[i];
                int pLength = p.getLength();
                if (pLength == 1 && p.getDataAt(0) != 0) {
                    onePosition.enter();
                    if (dropDimProfile.profile(!isSubset || dropDim == RRuntime.LOGICAL_TRUE)) {
                        // always drop dimensions with subscript
                        continue;
                    } else {
                        dimensions[ind++] = pLength;
                    }
                } else if (pLength > 1) {
                    multiPosition.enter();
                    dimensions[ind++] = pLength;
                } else {
                    emptyPosition.enter();
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

    protected Object[] accessInternal(VirtualFrame frame, DimsAndResultLength res, RList vector, Object[] positions) {
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        Object[] data;
        if (emptyResultProfile.profile(resLength == 0)) {
            data = new Object[0];
        } else {
            data = new Object[resLength];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(p);
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int pos = p.getDataAt(i);
                int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
                getMultiDimData(frame, data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
            }
        }
        return data;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasDimNames")
    protected RList accessNames(VirtualFrame frame, RList vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        Object[] data = accessInternal(frame, res, vector, positions);
        RList dimNames = vector.getDimNames();
        if (dimensionsIsNullProfile.profile(dimensions == null)) {
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

    @SuppressWarnings("unused")
    @Specialization(guards = "!hasDimNames")
    protected RList access(VirtualFrame frame, RList vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        Object[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createList(data, res.dimensions);
    }

    protected Object[] accessSubsetInternal(RList vector, RIntVector p) {
        int resLength = p.getLength();
        Object[] data = new Object[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < resLength; i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RNull.instance;
            } else {
                data[i] = vector.getDataAt(position - 1);
            }
        }
        return data;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isSubset", "hasNames"})
    protected RList accessSubsetNames(RList vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        Object[] data = accessSubsetInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(), p, p.getLength(), namesNACheck);
        return RDataFactory.createList(data, names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isSubset", "!hasNames"})
    protected RList accessSubset(RList vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        Object[] data = accessSubsetInternal(vector, p);
        return RDataFactory.createList(data);
    }

    // lists require special handling for one-dimensional "subscript", that is [[]], accesses due to
    // support for recursive access

    public static int getPositionInRecursion(RList vector, String position, int recLevel, SourceSection sourceSection, final BranchProfile error) {
        if (vector.getNames() == RNull.instance) {
            error.enter();
            throw RError.error(sourceSection, RError.Message.NO_SUCH_INDEX, recLevel + 1);
        }
        RStringVector names = (RStringVector) vector.getNames();
        int i = 0;
        for (; i < names.getLength(); i++) {
            if (position.equals(names.getDataAt(i))) {
                break;
            }
        }
        if (i == names.getLength()) {
            error.enter();
            throw RError.error(sourceSection, RError.Message.NO_SUCH_INDEX, recLevel + 1);
        } else {
            return i + 1;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!hasNames")
    protected RList accessStringNoNames(RList vector, Object exact, int recLevel, RStringVector p, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_SUCH_INDEX, 1);
    }

    @Specialization(guards = {"hasNames", "!isSubset", "twoPosition"})
    protected Object accessStringTwoPosRec(VirtualFrame frame, RList vector, Object exact, int recLevel, RStringVector p, RAbstractLogicalVector dropDim) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, getEncapsulatingSourceSection(), error);
        Object newVector = castVector(frame, vector.getDataAt(position - 1));
        Object newPosition = castPosition(frame, newVector, convertOperand(frame, newVector, p.getDataAt(1), exact));
        return accessRecursive(frame, newVector, exact, newPosition, recLevel + 1, dropDim, false);
    }

    @Specialization(guards = {"hasNames", "!isSubset", "!twoPosition"})
    protected Object accessString(VirtualFrame frame, RList vector, Object exact, int recLevel, RStringVector p, RAbstractLogicalVector dropDim) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, getEncapsulatingSourceSection(), error);
        RStringVector newP = popHead(p, posNACheck);
        return accessRecursive(frame, vector.getDataAt(position - 1), exact, newP, recLevel + 1, dropDim, false);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSubset", "onePosition", "!inRecursion"})
    protected Object accessOnePos(RList vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int position = p.getDataAt(0);
        if (RRuntime.isNA(position)) {
            error.enter(); // it's essentially an (unlikely) error
            return RNull.instance;
        } else if (position < 0) {
            error.enter();
            return getPositionFromNegative(vector, position, getEncapsulatingSourceSection(), error);
        } else if (position == 0) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else if (position > vector.getLength()) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
        }
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSubset", "noPosition"})
    protected Object accessNoPos(RList vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    public static int getPositionFromNegative(RList vector, int position, SourceSection sourceSection, final BranchProfile error) {
        if (vector.getLength() == 1 && position == -1) {
            // x<-c(1); x[-1] <==> x[0]
            error.enter();
            throw RError.error(sourceSection, RError.Message.SELECT_LESS_1);
        } else if (vector.getLength() > 1 && position < -vector.getLength()) {
            // x<-c(1,2); x[-3] <==> x[1,2]
            error.enter();
            throw RError.error(sourceSection, RError.Message.SELECT_MORE_1);
        } else if (vector.getLength() > 2 && position > -vector.getLength()) {
            // x<-c(1,2,3); x[-2] <==> x[1,3]
            error.enter();
            throw RError.error(sourceSection, RError.Message.SELECT_MORE_1);
        }
        assert (vector.getLength() == 2);
        return position == -1 ? 2 : 1;
    }

    private int getPositionInRecursion(RList vector, int position, int recLevel) {
        if (RRuntime.isNA(position) || position > vector.getLength()) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_SUCH_INDEX, recLevel + 1);
        } else if (position < 0) {
            return getPositionFromNegative(vector, position, getEncapsulatingSourceSection(), error);
        } else if (position == 0) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        return position;
    }

    @Specialization(guards = {"!isSubset", "onePosition", "inRecursion"})
    protected Object accessSubscript(RList vector, @SuppressWarnings("unused") Object exact, int recLevel, RIntVector p, @SuppressWarnings("unused") RAbstractLogicalVector dropDim) {
        int position = p.getDataAt(0);
        position = getPositionInRecursion(vector, position, recLevel);
        return vector.getDataAt(position - 1);
    }

    @Specialization(guards = {"!isSubset", "twoPosition"})
    protected Object accessTwoPosRec(VirtualFrame frame, RList vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int position = p.getDataAt(0);
        position = getPositionInRecursion(vector, position, recLevel);
        Object newVector = castVector(frame, vector.getDataAt(position - 1));
        Object newPosition = castPosition(frame, newVector, convertOperand(frame, newVector, p.getDataAt(1), exact));
        return accessRecursive(frame, newVector, exact, newPosition, recLevel + 1, dropDim, false);
    }

    @Specialization(guards = {"!isSubset", "multiPos"})
    protected Object access(VirtualFrame frame, RList vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int position = p.getDataAt(0);
        position = getPositionInRecursion(vector, position, recLevel);
        RIntVector newP = popHead(p, posNACheck);
        return accessRecursive(frame, vector.getDataAt(position - 1), exact, newP, recLevel + 1, dropDim, false);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isPositionNA", "isSubset"})
    protected RList accessNA(RList vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createList(new Object[]{RNull.instance});
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createList(new Object[]{RNull.instance}, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionNA", "isPositionNegative", "!outOfBoundsNegative"})
    protected RList accessNegativeInBounds(RAbstractContainer container, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionNA", "isPositionNegative", "outOfBoundsNegative", "oneElemVector"})
    protected RList accessNegativeOutOfBoundsOneElemVector(RAbstractContainer container, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionNA", "isPositionNegative", "outOfBoundsNegative", "!oneElemVector"})
    protected RList accessNegativeOutOfBounds(RAbstractContainer container, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    protected RList accessNamesSubset(RList vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        Object val = vector.getDataAt(position - 1);
        return RDataFactory.createList(new Object[]{val}, getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "hasNames", "!isSubset", "!isPositionNA", "!isPositionNegative", "!outOfBounds"})
    protected Object accessNames(RList vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!hasNames", "isSubset", "!isPositionNA", "!isPositionNegative"})
    protected RList accessSubset(RList vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return RDataFactory.createList(new Object[]{vector.getDataAt(position - 1)});
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!hasNames", "!isSubset", "!isPositionNA", "!isPositionNegative", "!outOfBounds"})
    protected Object access(RList vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSubset", "outOfBounds"})
    protected Object accessOutOfBounds(RList vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isPositionZero")
    protected RList accessPosZero(RList vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createList();
        } else {
            return RDataFactory.createList(new Object[0], RDataFactory.createEmptyStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSubset", "inRecursion", "multiPos", "!isVectorList"})
    protected Object accessRecFailedRec(RAbstractContainer container, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.RECURSIVE_INDEXING_FAILED, recLevel + 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSubset", "!inRecursion", "multiPos", "!isVectorList"})
    protected Object accessRecFailed(RAbstractContainer container, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    protected int[] accessInternal(VirtualFrame frame, DimsAndResultLength res, RAbstractIntVector vector, Object[] positions) {
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int[] data;
        if (emptyResultProfile.profile(resLength == 0)) {
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
        return data;
    }

    @Specialization(guards = "hasDimNames")
    protected RIntVector accessNames(VirtualFrame frame, RAbstractIntVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int[] data = accessInternal(frame, res, vector, positions);
        if (dimensionsIsNullProfile.profile(dimensions == null)) {
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

    @Specialization(guards = "!hasDimNames")
    protected RIntVector access(VirtualFrame frame, RAbstractIntVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        int[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    protected int[] accessInternal(RAbstractIntVector vector, RIntVector p) {
        int resLength = p.getLength();
        int[] data = new int[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < resLength; i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RRuntime.INT_NA;
            } else {
                try {
                    data[i] = vector.getDataAt(position - 1);
                } catch (ArrayIndexOutOfBoundsException x) {
                    x.printStackTrace();

                }
                elementNACheck.check(data[i]);
            }
        }
        return data;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasNames")
    protected RIntVector accessNames(RAbstractIntVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(), p, p.getLength(), namesNACheck);
        return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!hasNames")
    protected RIntVector access(RAbstractIntVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        int[] data = accessInternal(vector, p);
        return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isPositionNA", "isSubset"})
    protected RIntVector accessNA(RAbstractIntVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "isSubset"})
    protected RIntVector accessNames(RAbstractIntVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        int val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createIntVector(new int[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "!isSubset"})
    protected int accessNoSubset(RAbstractIntVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "!hasNames"})
    protected int accessNoNames(RAbstractIntVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    protected int access(RAbstractIntVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isPositionZero")
    protected RIntVector accessPosZero(RAbstractIntVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createIntVector(new int[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    protected double[] accessInternal(VirtualFrame frame, DimsAndResultLength res, RAbstractDoubleVector vector, Object[] positions) {
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        double[] data;
        if (emptyResultProfile.profile(resLength == 0)) {
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
        return data;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasDimNames")
    protected RDoubleVector accessNames(VirtualFrame frame, RAbstractDoubleVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        double[] data = accessInternal(frame, res, vector, positions);
        if (dimensionsIsNullProfile.profile(dimensions == null)) {
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

    @Specialization(guards = "!hasDimNames")
    protected RDoubleVector access(VirtualFrame frame, RAbstractDoubleVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        double[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    protected double[] accessInternal(RAbstractDoubleVector vector, RIntVector p) {
        int resLength = p.getLength();
        double[] data = new double[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < resLength; i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RRuntime.DOUBLE_NA;
            } else {
                data[i] = vector.getDataAt(position - 1);
                elementNACheck.check(data[i]);
            }
        }
        return data;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasNames")
    protected RDoubleVector accessNames(RAbstractDoubleVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        double[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(), p, p.getLength(), namesNACheck);
        return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!hasNames")
    protected RDoubleVector access(RAbstractDoubleVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        double[] data = accessInternal(vector, p);
        return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isPositionNA", "isSubset"})
    protected RDoubleVector accessNA(RAbstractDoubleVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "isSubset"})
    protected RDoubleVector accessNames(RAbstractDoubleVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        double val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createDoubleVector(new double[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "!isSubset"})
    protected double accessNoSubset(RAbstractDoubleVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "!hasNames"})
    protected double accessNoNames(RAbstractDoubleVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    protected double access(RAbstractDoubleVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isPositionZero")
    protected RDoubleVector accessPosZero(RAbstractDoubleVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyDoubleVector();
        } else {
            return RDataFactory.createDoubleVector(new double[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    protected byte[] accessInternal(VirtualFrame frame, DimsAndResultLength res, RLogicalVector vector, Object[] positions) {
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        byte[] data;
        if (emptyResultProfile.profile(resLength == 0)) {
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
        return data;
    }

    @Specialization(guards = "hasDimNames")
    protected RLogicalVector accessNames(VirtualFrame frame, RLogicalVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        byte[] data = accessInternal(frame, res, vector, positions);
        if (dimensionsIsNullProfile.profile(dimensions == null)) {
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

    @Specialization(guards = "!hasDimNames")
    protected RLogicalVector access(VirtualFrame frame, RLogicalVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        byte[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    protected byte[] accessInternal(RLogicalVector vector, RIntVector p) {
        int resLength = p.getLength();
        byte[] data = new byte[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < resLength; i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RRuntime.LOGICAL_NA;
            } else {
                data[i] = vector.getDataAt(position - 1);
                elementNACheck.check(data[i]);
            }
        }
        return data;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasNames")
    protected RLogicalVector accessNames(RLogicalVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        byte[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(), p, p.getLength(), namesNACheck);
        return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!hasNames")
    protected RLogicalVector access(RLogicalVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        byte[] data = accessInternal(vector, p);
        return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isPositionNA", "isSubset"})
    protected RLogicalVector accessNA(RLogicalVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "isSubset"})
    protected RLogicalVector accessNames(RAbstractLogicalVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        byte val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createLogicalVector(new byte[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "!isSubset"})
    protected byte accessNoSubset(RLogicalVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "!hasNames"})
    protected byte accessNoNames(RLogicalVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    protected byte access(RLogicalVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isPositionZero")
    protected RLogicalVector accessPosZero(RLogicalVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyLogicalVector();
        } else {
            return RDataFactory.createLogicalVector(new byte[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    protected String[] accessInternal(VirtualFrame frame, DimsAndResultLength res, RStringVector vector, Object[] positions) {
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        String[] data;
        if (emptyResultProfile.profile(resLength == 0)) {
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
        return data;
    }

    @Specialization(guards = "hasDimNames")
    protected RStringVector accessNames(VirtualFrame frame, RStringVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        String[] data = accessInternal(frame, res, vector, positions);
        if (dimensionsIsNullProfile.profile(dimensions == null)) {
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

    @Specialization(guards = "!hasDimNames")
    protected RStringVector access(VirtualFrame frame, RStringVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        String[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    protected String[] accessInternal(RStringVector vector, RIntVector p) {
        int resLength = p.getLength();
        String[] data = new String[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < resLength; i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = RRuntime.STRING_NA;
            } else {
                data[i] = vector.getDataAt(position - 1);
                elementNACheck.check(data[i]);
            }
        }
        return data;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasNames")
    protected RStringVector accessNames(RStringVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        String[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(), p, p.getLength(), namesNACheck);
        return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!hasNames")
    protected RStringVector access(RStringVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        String[] data = accessInternal(vector, p);
        return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isPositionNA", "isSubset"})
    protected RStringVector accessNA(RStringVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "isSubset"})
    protected RStringVector accessNames(RAbstractStringVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        String val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createStringVector(new String[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "!isSubset"})
    protected String accessNoSubset(RStringVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "!hasNames"})
    protected String accessNoNames(RStringVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    protected String access(RStringVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isPositionZero")
    protected RStringVector accessPosZero(RStringVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyStringVector();
        } else {
            return RDataFactory.createStringVector(new String[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    protected double[] accessInternal(VirtualFrame frame, DimsAndResultLength res, RComplexVector vector, Object[] positions) {
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        double[] data;
        if (emptyResultProfile.profile(resLength == 0)) {
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
        return data;
    }

    @Specialization(guards = "hasDimNames")
    protected RComplexVector accessNames(VirtualFrame frame, RComplexVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        double[] data = accessInternal(frame, res, vector, positions);
        if (dimensionsIsNullProfile.profile(dimensions == null)) {
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

    @Specialization(guards = "!hasDimNames")
    protected RComplexVector access(VirtualFrame frame, RComplexVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        double[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    protected double[] accessInternal(RComplexVector vector, RIntVector p) {
        int resLength = p.getLength();
        double[] data = new double[resLength << 1];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        int ind = 0;
        for (int i = 0; i < resLength; i++) {
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

        return data;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasNames")
    protected RComplexVector accessNames(RComplexVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        double[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(), p, p.getLength(), namesNACheck);
        return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!hasNames")
    protected RComplexVector access(RComplexVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        double[] data = accessInternal(vector, p);
        return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isPositionNA", "isSubset"})
    protected RComplexVector accessNA(RComplexVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "isSubset"})
    protected RComplexVector accessNames(RAbstractComplexVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        RComplex val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createComplexVector(new double[]{val.getRealPart(), val.getImaginaryPart()}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "!isSubset"})
    protected RComplex accessNoSubset(RComplexVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "!hasNames"})
    protected RComplex accessNoNames(RComplexVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    protected RComplex access(RComplexVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isPositionZero")
    protected RComplexVector accessPosZero(RComplexVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyComplexVector();
        } else {
            return RDataFactory.createComplexVector(new double[0], RDataFactory.COMPLETE_VECTOR, RDataFactory.createEmptyStringVector());
        }
    }

    protected byte[] accessInternal(VirtualFrame frame, DimsAndResultLength res, RRawVector vector, Object[] positions) {
        int resLength = res.resLength;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        byte[] data;
        if (emptyResultProfile.profile(resLength == 0)) {
            data = new byte[0];
        } else {
            data = new byte[resLength];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(p);
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int pos = p.getDataAt(i);
                int srcArrayBase = getSrcArrayBase(pos, accSrcDimensions);
                getMultiDimData(frame, data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, posNACheck, elementNACheck);
            }
        }
        return data;
    }

    @Specialization(guards = "hasDimNames")
    protected RRawVector accessNames(VirtualFrame frame, RRawVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        int[] dimensions = res.dimensions;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        byte[] data = accessInternal(frame, res, vector, positions);
        if (dimensionsIsNullProfile.profile(dimensions == null)) {
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

    @Specialization(guards = "!hasDimNames")
    protected RRawVector access(VirtualFrame frame, RRawVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    RAbstractLogicalVector dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
        byte[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createRawVector(data, res.dimensions);
    }

    protected byte[] accessInternal(RRawVector vector, RIntVector p) {
        int resLength = p.getLength();
        byte[] data = new byte[resLength];
        elementNACheck.enable(!vector.isComplete() || !p.isComplete());
        for (int i = 0; i < resLength; i++) {
            int position = p.getDataAt(i);
            if (elementNACheck.check(position)) {
                data[i] = 0;
            } else {
                data[i] = vector.getDataAt(position - 1).getValue();
                elementNACheck.check(data[i]);
            }
        }
        return data;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasNames")
    protected RRawVector accessNames(RRawVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        byte[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(), p, p.getLength(), namesNACheck);
        return RDataFactory.createRawVector(data, names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!hasNames")
    protected RRawVector access(RRawVector vector, Object exact, int recLevel, RIntVector p, RAbstractLogicalVector dropDim) {
        byte[] data = accessInternal(vector, p);
        return RDataFactory.createRawVector(data);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isPositionNA", "isSubset"})
    protected RRawVector accessNA(RRawVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createRawVector(new byte[]{0});
        } else {
            RStringVector names = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR);
            return RDataFactory.createRawVector(new byte[]{0}, names);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "isSubset"})
    protected RRawVector accessNames(RAbstractRawVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        RRaw val = vector.getDataAt(position - 1);
        return RDataFactory.createRawVector(new byte[]{val.getValue()}, getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "hasNames", "!isSubset"})
    protected RRaw accessNoSubset(RRawVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative", "!hasNames"})
    protected RRaw accessNoNames(RRawVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isPositionZero", "!isPositionNA", "!isPositionNegative"})
    protected RRaw access(RRawVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isPositionZero")
    protected RRawVector accessPosZero(RRawVector vector, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames() == RNull.instance) {
            return RDataFactory.createEmptyRawVector();
        } else {
            return RDataFactory.createRawVector(new byte[0], RDataFactory.createEmptyStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "noPosition")
    protected Object accessListEmptyPos(RAbstractContainer container, Object exact, int recLevel, RList positions, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "onePosition")
    protected Object accessListOnePos(RAbstractContainer container, Object exact, int recLevel, RList positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "multiPos")
    protected Object accessListMultiPosList(RList vector, Object exact, int recLevel, RList positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"multiPos", "!isVectorList"})
    protected Object accessListMultiPos(RAbstractContainer container, Object exact, int recLevel, RList positions, RAbstractLogicalVector dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object accessListMultiPos(RAbstractContainer container, Object exact, int recLevel, RComplex positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object accessListMultiPos(RAbstractContainer container, Object exact, int recLevel, RRaw positions, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
    }

    // this should really be implemented in R
    @Specialization(guards = "!isSubset")
    protected Object access(VirtualFrame frame, RDataFrame dataFrame, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        return accessRecursive(frame, dataFrame.getVector(), exact, position, recLevel, dropDim, true);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isSubset")
    protected Object accessSubset(RDataFrame dataFrame, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.DATA_FRAMES_SUBSET_ACCESS);
    }

    @Specialization
    protected Object access(VirtualFrame frame, RDataFrame dataFrame, Object exact, int recLevel, Object[] position, RAbstractLogicalVector dropDim) {
        // there should be error checks here, but since it will ultimately be implemented in R...
        assert position.length > 1;
        RIntVector firstIndVec = (RIntVector) position[0];
        RIntVector secondIndVec = (RIntVector) position[1];
        assert firstIndVec.getLength() > 0;
        if (secondIndVec.getLength() == 1) {
            RList l = (RList) dataFrame.getVector();
            int secondInd = secondIndVec.getDataAt(0);
            assert l.getLength() >= secondInd;
            return accessRecursive(frame, l.getDataAt(secondInd - 1), exact, firstIndVec.getLength() == 1 ? firstIndVec.getDataAt(0) : firstIndVec, recLevel, dropDim, false);
        } else {
            RList l = (RList) dataFrame.getVector();
            Object[] data = new Object[secondIndVec.getLength()];
            for (int i = 0; i < secondIndVec.getLength(); i++) {
                int secondInd = secondIndVec.getDataAt(i);
                assert l.getLength() >= secondInd;
                data[i] = accessRecursive(frame, l.getDataAt(secondInd - 1), exact, firstIndVec.getLength() == 1 ? firstIndVec.getDataAt(0) : firstIndVec, recLevel, dropDim, false);
            }
            RList resList = RDataFactory.createList(data, dataFrame.getNames());
            Object newRowNames = accessRecursive(frame, getContainerRowNames(frame, dataFrame), exact, firstIndVec.getLength() == 1 ? firstIndVec.getDataAt(0) : firstIndVec, 0,
                            RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_FALSE}, RDataFactory.COMPLETE_VECTOR), false);
            resList.setRowNames(castVector(frame, newRowNames));
            return resList.setClassAttr(RDataFactory.createStringVector("data.frame"));
        }

    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object access(VirtualFrame frame, RExpression expression, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (position < 1) {
            error.enter();
            throw RError.error(Message.SELECT_LESS_1);
        }
        return expression.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object access(VirtualFrame frame, RLanguage lang, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (position < 1) {
            error.enter();
            throw RError.error(Message.SELECT_LESS_1);
        }
        return lang.getDataAtAsObject(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object access(RPairList pairlist, Object exact, int recLevel, int position, RAbstractLogicalVector dropDim) {
        if (position < 1) {
            error.enter();
            throw RError.error(Message.SELECT_LESS_1);
        }
        return pairlist.getDataAtAsObject(position - 1);
    }

    protected boolean outOfBounds(RList vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, int position) {
        return position > vector.getLength();
    }

    protected boolean outOfBoundsNegative(RAbstractContainer container, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, int position) {
        return -position > container.getLength();
    }

    @SuppressWarnings("unused")
    protected boolean oneElemVector(RAbstractContainer container, Object exact, int recLevel, int position) {
        return container.getLength() == 1;
    }

    @SuppressWarnings("unused")
    protected boolean isPositionNegative(RAbstractContainer container, Object exact, int recLevel, int position) {
        return position < 0;
    }

    protected boolean isVectorList(RAbstractContainer container) {
        return container.getElementClass() == Object.class;
    }

    protected boolean wrongDimensions(RAbstractVector container, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions) {
        return container.getDimensions() == null || container.getDimensions().length != positions.length;
    }

    @SuppressWarnings("unused")
    protected static boolean isFirstPositionPositive(RNull vector, Object exact, int recLevel, RAbstractIntVector positions) {
        return positions.getDataAt(0) > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean isFirstPositionPositive(RFunction vector, Object exact, int recLevel, RAbstractIntVector positions) {
        return positions.getDataAt(0) > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean isFirstPositionOne(RFunction vector, Object exact, int recLevel, RAbstractIntVector positions) {
        return positions.getDataAt(0) == 1;
    }

    @SuppressWarnings("unused")
    protected static boolean isPositionZero(RAbstractContainer container, Object exact, int recLevel, int position) {
        return position == 0;
    }

    @SuppressWarnings("unused")
    protected static boolean isPositionNA(RAbstractContainer container, Object exact, int recLevel, int position) {
        return RRuntime.isNA(position);
    }

    protected boolean isSubset() {
        return isSubset;
    }

    protected static boolean hasNames(RAbstractContainer container) {
        return container.getNames() != RNull.instance;
    }

    protected static boolean hasDimNames(RAbstractContainer container) {
        return container.getDimNames() != null;
    }

    @SuppressWarnings("unused")
    protected static boolean twoPosition(RAbstractContainer container, Object exact, int recLevel, RAbstractVector p) {
        return p.getLength() == 2;
    }

    @SuppressWarnings("unused")
    protected static boolean onePosition(RAbstractContainer container, Object exact, int recLevel, RAbstractVector p) {
        return p.getLength() == 1;
    }

    @SuppressWarnings("unused")
    protected static boolean noPosition(RAbstractContainer container, Object exact, int recLevel, RAbstractVector p) {
        return p.getLength() == 0;
    }

    @SuppressWarnings("unused")
    protected static boolean multiPos(RAbstractContainer container, Object exact, int recLevel, RAbstractVector positions) {
        return positions.getLength() > 1;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RAbstractContainer container, Object exact, int recLevel, RIntVector positions) {
        return recLevel > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RNull vector, Object exact, int recLevel, RAbstractIntVector positions) {
        return recLevel > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RFunction vector, Object exact, int recLevel, RAbstractIntVector positions) {
        return recLevel > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RFunction vector, Object exact, int recLevel, RAbstractStringVector positions) {
        return recLevel > 0;
    }

    @SuppressWarnings("unused")
    protected static boolean inRecursion(RFunction vector, Object exact, int recLevel, Object positions) {
        return recLevel > 0;
    }

    public static AccessArrayNode create(boolean isSubset, boolean exactInSource, boolean dropInSource, RNode vector, RNode exact, PositionsArrayNode positions, RNode dropDim) {
        return AccessArrayNodeFactory.create(isSubset, exactInSource, dropInSource, vector, exact, ConstantNode.create(0), positions, dropDim);
    }

}
