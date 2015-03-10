/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.array.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCastNodeGen.OperatorConverterNodeGen;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.DispatchedCallNode.DispatchType;
import com.oracle.truffle.r.nodes.function.DispatchedCallNode.NoGenericMethodException;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "exact", type = RNode.class), @NodeChild(value = "recursionLevel", type = RNode.class),
                @NodeChild(value = "positions", type = PositionsArrayNode.class, executeWith = {"vector", "exact"}), @NodeChild(value = "dropDim", type = RNode.class)})
public abstract class AccessArrayNode extends RNode {

    public final boolean isSubset;
    public final boolean forObjects;
    public final boolean exactInSource;
    public final boolean dropInSource;

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
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private AccessArrayNode accessRecursive;
    @Child private CastToVectorNode castVector;
    @Child private ArrayPositionCast castPosition;
    @Child private OperatorConverterNode operatorConverter;
    @Child private GetMultiDimDataNode getMultiDimData;
    @Child private GetNamesNode getNamesNode;
    @Child private GetDimNamesNode getDimNamesNode;

    @Child private DispatchedCallNode dcn;
    @Child private DispatchedCallNode dcnDrop;

    public abstract RNode getVector();

    protected abstract RNode getPositions();

    protected abstract RNode getExact();

    protected abstract RNode getDropDim();

    protected abstract RNode getRecursionLevel();

    public abstract Object executeAccess(VirtualFrame frame, Object vector, Object exact, int recLevel, Object operand, Object dropDim);

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
        return AccessArrayNodeGen.create(isSubset, forObjects, true, true, vector, exact, getRecursionLevel(), positions, dropDim);
    }

    public AccessArrayNode(boolean isSubset, boolean forObjects, boolean exactInSource, boolean dropInSource) {
        this.isSubset = isSubset;
        this.forObjects = forObjects;
        this.exactInSource = exactInSource;
        this.dropInSource = dropInSource;
    }

    public AccessArrayNode(AccessArrayNode other) {
        this.isSubset = other.isSubset;
        this.forObjects = other.forObjects;
        this.exactInSource = other.exactInSource;
        this.dropInSource = other.dropInSource;
    }

    private Object accessRecursive(VirtualFrame frame, Object vector, Object exact, Object operand, int recLevel, Object dropDim) {
        // for data frames, recursive update is the same as for lists but as if the [[]] operator
        // was used
        if (accessRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            accessRecursive = insert(AccessArrayNodeGen.create(isSubset, false, false, false, null, null, null, null, null));
        }
        return accessRecursive.executeAccess(frame, vector, exact, recLevel, operand, dropDim);
    }

    private Object castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(null, false, false, false, true));
        }
        return castVector.executeObject(frame, value);
    }

    private Object castPosition(VirtualFrame frame, Object vector, Object operand) {
        if (castPosition == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castPosition = insert(ArrayPositionCastNodeGen.create(0, 1, false, false, null, null));
        }
        return castPosition.executeArg(frame, vector, operand);
    }

    private void initOperatorConvert() {
        if (operatorConverter == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            operatorConverter = insert(OperatorConverterNodeGen.create(0, 1, false, false, null, null, null));
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
            getMultiDimData = insert(GetMultiDimDataNodeGen.create(posCheck, elementCheck, null, null, null, null, null, null, null, null));
        }
        return getMultiDimData.executeMultiDimDataGet(frame, data, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
    }

    private RStringVector getNames(VirtualFrame frame, RAbstractVector vector, Object[] positions, int currentDimLevel, NACheck namesCheck) {
        if (getNamesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNamesNode = insert(GetNamesNodeGen.create(namesCheck, null, null, null, null));
        }
        return (RStringVector) getNamesNode.executeNamesGet(frame, vector, positions, currentDimLevel, RNull.instance);
    }

    private RStringVector getDimNames(VirtualFrame frame, RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel, NACheck namesCheck) {
        if (getDimNamesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getDimNamesNode = insert(GetDimNamesNodeGen.create(namesCheck, null, null, null, null, null));
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

    private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get(new String[]{"", ""});
    private static final ArgumentsSignature DROP_SIGNATURE = ArgumentsSignature.get(new String[]{"", "", "drop"});
    private static final ArgumentsSignature EXACT_SIGNATURE = ArgumentsSignature.get(new String[]{"", "", "exact"});

    @Specialization(guards = {"isObject(container)", "isSubset()"})
    protected Object accessObjectSubset(VirtualFrame frame, RAbstractContainer container, Object exact, int recLevel, Object position, Object dropDim) {
        Object inds = position instanceof Object[] ? new RArgsValuesAndNames((Object[]) position, ArgumentsSignature.empty(((Object[]) position).length)) : position;
        try {
            if (dropDim != RMissing.instance) {
                if (dcnDrop == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dcnDrop = insert(DispatchedCallNode.create("[", DispatchType.UseMethod, DROP_SIGNATURE));
                }
                return dcnDrop.executeInternal(frame, container.getClassHierarchy(), new Object[]{container, inds, dropDim});
            } else {
                if (dcn == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dcn = insert(DispatchedCallNode.create("[", DispatchType.UseMethod, SIGNATURE));
                }
                return dcn.executeInternal(frame, container.getClassHierarchy(), new Object[]{container, inds});
            }
        } catch (NoGenericMethodException e) {
            return accessRecursive(frame, container, exact, position, recLevel, dropDim);
        }
    }

    @Specialization(guards = {"isObject(container)", "!isSubset()"})
    protected Object accessObject(VirtualFrame frame, RAbstractContainer container, Object exact, int recLevel, Object position, Object dropDim) {
        if (dcn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dcn = insert(DispatchedCallNode.create("[[", DispatchType.UseMethod, EXACT_SIGNATURE));
        }
        try {
            Object inds = position instanceof Object[] ? new RArgsValuesAndNames((Object[]) position, ArgumentsSignature.empty(((Object[]) position).length)) : position;
            return dcn.executeInternal(frame, container.getClassHierarchy(), new Object[]{container, inds, exact});
        } catch (NoGenericMethodException e) {
            return accessRecursive(frame, container, exact, position, recLevel, dropDim);
        }
    }

    @Specialization(guards = "!isObject(factor)")
    protected Object accessFactor(VirtualFrame frame, RFactor factor, Object exact, int recLevel, Object position, Object dropDim) {
        return accessRecursive(frame, factor.getVector(), exact, position, recLevel, dropDim);
    }

    @Specialization(guards = "!isObject(dataFrame)")
    protected Object accessFactor(VirtualFrame frame, RDataFrame dataFrame, Object exact, int recLevel, Object position, Object dropDim) {
        return accessRecursive(frame, dataFrame.getVector(), exact, position, recLevel, dropDim);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion(recLevel)", "isFirstPositionPositive(positions)"})
    protected RNull accessNullInRecursionPosPositive(RNull vector, Object exact, int recLevel, RAbstractIntVector positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion(recLevel)", "!isFirstPositionPositive(positions)"})
    protected RNull accessNullInRecursion(RNull vector, Object exact, int recLevel, RAbstractIntVector positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RNull access(RNull vector, Object exact, int recLevel, Object positions, Object dropDim) {
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion(recLevel)", "isFirstPositionOne(positions)"})
    protected RNull accessFunctionInRecursionPosOne(RFunction vector, Object exact, int recLevel, RAbstractIntVector positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TYPE_LENGTH, "closure", 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion(recLevel)", "isFirstPositionPositive(positions)", "!isFirstPositionOne(positions)"})
    protected RNull accessFunctionInRecursionPosPositive(RFunction vector, Object exact, int recLevel, RAbstractIntVector positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"inRecursion(recLevel)", "!isFirstPositionPositive(positions)"})
    protected RNull accessFunctionInRecursion(RFunction vector, Object exact, int recLevel, RAbstractIntVector positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "inRecursion(recLevel)")
    protected RNull accessFunctionInRecursionString(RFunction vector, Object exact, int recLevel, RAbstractStringVector positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RNull accessFunction(RFunction vector, Object exact, int recLevel, Object position, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.OBJECT_NOT_SUBSETTABLE, "closure");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isObject(container)")
    protected RNull access(RAbstractContainer container, Object exact, int recLevel, RNull positions, Object dropDim) {
        // this is a special case (see ArrayPositionCast) - RNull can only appear to represent the
        // x[NA] case which has to return null and not a null vector
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isObject(container)")
    protected Object access(RAbstractContainer container, Object exact, int recLevel, RMissing positions, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "symbol");
        } else {
            return container;
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "wrongDimensions(container, positions)"})
    protected Object access(RAbstractVector container, Object exact, int recLevel, Object[] positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_DIMENSIONS);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "isPositionNA(position)", "!isSubset()"})
    protected RIntVector accessNA(RAbstractContainer container, Object exact, int recLevel, int position, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    private RStringVector getName(RAbstractVector vector, int position) {
        RStringVector srcNames = vector.getNames(attrProfiles);
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
    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RList accessNames(VirtualFrame frame, RList vector, Object exact, int recLevel, Object[] positions, RMissing dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, RRuntime.LOGICAL_TRUE);
    }

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RList accessNames(VirtualFrame frame, RList vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
    }

    @SuppressWarnings("unused")
    protected RList accessNamesInternal(VirtualFrame frame, RList vector, Object exact, int recLevel, Object[] positions, byte dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim);
        int[] dimensions = res.dimensions;
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        Object[] data = accessInternal(frame, res, vector, positions);
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
    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RList access(VirtualFrame frame, RList vector, Object exact, int recLevel, Object[] positions, RMissing dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, RRuntime.LOGICAL_TRUE);
        Object[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createList(data, res.dimensions);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
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
    @Specialization(guards = {"!isObject(vector)", "isSubset()", "hasNames(vector)"})
    protected RList accessSubsetNames(RList vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        Object[] data = accessSubsetInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(attrProfiles), p, p.getLength(), namesNACheck);
        return RDataFactory.createList(data, names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isSubset()", "!hasNames(vector)"})
    protected RList accessSubset(RList vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        Object[] data = accessSubsetInternal(vector, p);
        return RDataFactory.createList(data);
    }

    // lists require special handling for one-dimensional "subscript", that is [[]], accesses due to
    // support for recursive access

    public static int getPositionInRecursion(RList vector, String position, int recLevel, SourceSection sourceSection, final BranchProfile error, final RAttributeProfiles attrProfiles) {
        if (vector.getNames(attrProfiles) == null) {
            error.enter();
            throw RError.error(sourceSection, RError.Message.NO_SUCH_INDEX, recLevel + 1);
        }
        RStringVector names = vector.getNames(attrProfiles);
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
    @Specialization(guards = {"!isObject(vector)", "!hasNames(vector)"})
    protected RList accessStringNoNames(RList vector, Object exact, int recLevel, RStringVector p, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_SUCH_INDEX, 1);
    }

    @Specialization(guards = {"!isObject(vector)", "hasNames(vector)", "!isSubset()", "twoPosition(p)"})
    protected Object accessStringTwoPosRec(VirtualFrame frame, RList vector, Object exact, int recLevel, RStringVector p, Object dropDim) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, getEncapsulatingSourceSection(), error, attrProfiles);
        Object newVector = castVector(frame, vector.getDataAt(position - 1));
        Object newPosition = castPosition(frame, newVector, convertOperand(frame, newVector, p.getDataAt(1), exact));
        return accessRecursive(frame, newVector, exact, newPosition, recLevel + 1, dropDim);
    }

    @Specialization(guards = {"!isObject(vector)", "hasNames(vector)", "!isSubset()", "!twoPosition(p)"})
    protected Object accessString(VirtualFrame frame, RList vector, Object exact, int recLevel, RStringVector p, Object dropDim) {
        int position = getPositionInRecursion(vector, p.getDataAt(0), recLevel, getEncapsulatingSourceSection(), error, attrProfiles);
        RStringVector newP = popHead(p, posNACheck);
        return accessRecursive(frame, vector.getDataAt(position - 1), exact, newP, recLevel + 1, dropDim);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isSubset()", "onePosition(p)", "!inRecursion(recLevel)"})
    protected Object accessOnePos(RList vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
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
    @Specialization(guards = {"!isObject(vector)", "!isSubset()", "noPosition(positions)"})
    protected Object accessNoPos(RList vector, Object exact, int recLevel, RIntVector positions, Object dropDim) {
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

    @Specialization(guards = {"!isObject(vector)", "!isSubset()", "onePosition(positions)", "inRecursion(recLevel)"})
    protected Object accessSubscript(RList vector, @SuppressWarnings("unused") Object exact, int recLevel, RIntVector positions, @SuppressWarnings("unused") Object dropDim) {
        int position = positions.getDataAt(0);
        position = getPositionInRecursion(vector, position, recLevel);
        return vector.getDataAt(position - 1);
    }

    @Specialization(guards = {"!isObject(vector)", "!isSubset()", "twoPosition(positions)"})
    protected Object accessTwoPosRec(VirtualFrame frame, RList vector, Object exact, int recLevel, RIntVector positions, Object dropDim) {
        int position = positions.getDataAt(0);
        position = getPositionInRecursion(vector, position, recLevel);
        Object newVector = castVector(frame, vector.getDataAt(position - 1));
        Object newPosition = castPosition(frame, newVector, convertOperand(frame, newVector, positions.getDataAt(1), exact));
        return accessRecursive(frame, newVector, exact, newPosition, recLevel + 1, dropDim);
    }

    @Specialization(guards = {"!isObject(vector)", "!isSubset()", "multiPos(positions)"})
    protected Object access(VirtualFrame frame, RList vector, Object exact, int recLevel, RIntVector positions, Object dropDim) {
        int position = positions.getDataAt(0);
        position = getPositionInRecursion(vector, position, recLevel);
        RIntVector newP = popHead(positions, posNACheck);
        return accessRecursive(frame, vector.getDataAt(position - 1), exact, newP, recLevel + 1, dropDim);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionNA(position)", "isSubset()"})
    protected RList accessNA(RList vector, Object exact, int recLevel, int position, Object dropDim) {
        if (vector.getNames(attrProfiles) == null) {
            return RDataFactory.createList(new Object[]{RNull.instance});
        } else {
            return RDataFactory.createList(new Object[]{RNull.instance}, RDataFactory.createNAStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "!isPositionNA(position)", "isPositionNegative(position)", "!outOfBoundsNegative(container, position)"})
    protected RList accessNegativeInBounds(RAbstractContainer container, Object exact, int recLevel, int position, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "!isPositionNA(position)", "isPositionNegative(position)", "outOfBoundsNegative(container, position)", "oneElemVector(container)"})
    protected RList accessNegativeOutOfBoundsOneElemVector(RAbstractContainer container, Object exact, int recLevel, int position, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "!isPositionNA(position)", "isPositionNegative(position)", "outOfBoundsNegative(container, position)", "!oneElemVector(container)"})
    protected RList accessNegativeOutOfBounds(RAbstractContainer container, Object exact, int recLevel, int position, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "hasNames(vector)", "isSubset()", "!isPositionNA(position)", "!isPositionNegative(position)"})
    protected RList accessNamesSubset(RList vector, Object exact, int recLevel, int position, Object dropDim) {
        Object val = vector.getDataAt(position - 1);
        return RDataFactory.createList(new Object[]{val}, getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "hasNames(vector)", "!isSubset()", "!isPositionNA(position)", "!isPositionNegative(position)",
                    "!outOfBounds(vector, position)"})
    protected Object accessNames(RList vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!hasNames(vector)", "isSubset()", "!isPositionNA(position)", "!isPositionNegative(position)"})
    protected RList accessSubset(RList vector, Object exact, int recLevel, int position, Object dropDim) {
        return RDataFactory.createList(new Object[]{vector.getDataAt(position - 1)});
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!hasNames(vector)", "!isSubset()", "!isPositionNA(position)", "!isPositionNegative(position)",
                    "!outOfBounds(vector, position)"})
    protected Object access(RList vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isSubset()", "outOfBounds(vector, position)"})
    protected Object accessOutOfBounds(RList vector, Object exact, int recLevel, int position, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SUBSCRIPT_BOUNDS);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionZero(position)"})
    protected RList accessPosZero(RList vector, Object exact, int recLevel, int position, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames(attrProfiles) == null) {
            return RDataFactory.createList();
        } else {
            return RDataFactory.createList(new Object[0], RDataFactory.createEmptyStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "!isSubset()", "inRecursion(recLevel)", "multiPos(positions)", "!isVectorList(container)"})
    protected Object accessRecFailedRec(RAbstractContainer container, Object exact, int recLevel, RIntVector positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.RECURSIVE_INDEXING_FAILED, recLevel + 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "!isSubset()", "!inRecursion(recLevel)", "multiPos(positions)", "!isVectorList(container)"})
    protected Object accessRecFailed(RAbstractContainer container, Object exact, int recLevel, RIntVector positions, Object dropDim) {
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

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RIntVector accessNames(VirtualFrame frame, RAbstractIntVector vector, Object exact, int recLevel, Object[] positions, @SuppressWarnings("unused") RMissing dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, RRuntime.LOGICAL_TRUE);
    }

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RIntVector accessNames(VirtualFrame frame, RAbstractIntVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
    }

    @SuppressWarnings("unused")
    protected RIntVector accessNamesInternal(VirtualFrame frame, RAbstractIntVector vector, Object exact, int recLevel, Object[] positions, byte dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim);
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

    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RIntVector access(VirtualFrame frame, RAbstractIntVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    @SuppressWarnings("unused") RMissing dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, RRuntime.LOGICAL_TRUE);
        int[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
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
    @Specialization(guards = {"!isObject(vector)", "hasNames(vector)"})
    protected RIntVector accessNames(RAbstractIntVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        int[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(attrProfiles), p, p.getLength(), namesNACheck);
        return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasNames(vector)"})
    protected RIntVector access(RAbstractIntVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        int[] data = accessInternal(vector, p);
        return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionNA(position)", "isSubset()"})
    protected RIntVector accessNA(RAbstractIntVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (vector.getNames(attrProfiles) == null) {
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR, RDataFactory.createNAStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "isSubset()"})
    protected RIntVector accessNames(RAbstractIntVector vector, Object exact, int recLevel, int position, Object dropDim) {
        int val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createIntVector(new int[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "!isSubset()"})
    protected int accessNoSubset(RAbstractIntVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "!hasNames(vector)"})
    protected int accessNoNames(RAbstractIntVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)"})
    protected int access(RAbstractIntVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionZero(position)"})
    protected RIntVector accessPosZero(RAbstractIntVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames(attrProfiles) == null) {
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

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RDoubleVector accessNames(VirtualFrame frame, RAbstractDoubleVector vector, Object exact, int recLevel, Object[] positions, @SuppressWarnings("unused") RMissing dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, RRuntime.LOGICAL_TRUE);
    }

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RDoubleVector accessNames(VirtualFrame frame, RAbstractDoubleVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
    }

    @SuppressWarnings("unused")
    protected RDoubleVector accessNamesInternal(VirtualFrame frame, RAbstractDoubleVector vector, Object exact, int recLevel, Object[] positions, byte dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim);
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

    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RDoubleVector access(VirtualFrame frame, RAbstractDoubleVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    @SuppressWarnings("unused") RMissing dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, RRuntime.LOGICAL_TRUE);
        double[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
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
    @Specialization(guards = {"!isObject(vector)", "hasNames(vector)"})
    protected RDoubleVector accessNames(RAbstractDoubleVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        double[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(attrProfiles), p, p.getLength(), namesNACheck);
        return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasNames(vector)"})
    protected RDoubleVector access(RAbstractDoubleVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        double[] data = accessInternal(vector, p);
        return RDataFactory.createDoubleVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionNA(position)", "isSubset()"})
    protected RDoubleVector accessNA(RAbstractDoubleVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (vector.getNames(attrProfiles) == null) {
            return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR, RDataFactory.createNAStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "isSubset()"})
    protected RDoubleVector accessNames(RAbstractDoubleVector vector, Object exact, int recLevel, int position, Object dropDim) {
        double val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createDoubleVector(new double[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "!isSubset()"})
    protected double accessNoSubset(RAbstractDoubleVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "!hasNames(vector)"})
    protected double accessNoNames(RAbstractDoubleVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)"})
    protected double access(RAbstractDoubleVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionZero(position)"})
    protected RDoubleVector accessPosZero(RAbstractDoubleVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames(attrProfiles) == null) {
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

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RLogicalVector accessNames(VirtualFrame frame, RLogicalVector vector, Object exact, int recLevel, Object[] positions, @SuppressWarnings("unused") RMissing dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, RRuntime.LOGICAL_TRUE);
    }

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RLogicalVector accessNames(VirtualFrame frame, RLogicalVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
    }

    @SuppressWarnings("unused")
    protected RLogicalVector accessNamesInternal(VirtualFrame frame, RLogicalVector vector, Object exact, int recLevel, Object[] positions, byte dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim);
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

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RLogicalVector access(VirtualFrame frame, RLogicalVector vector, Object exact, int recLevel, Object[] positions, RMissing dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, RRuntime.LOGICAL_TRUE);
        byte[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RLogicalVector access(VirtualFrame frame, RLogicalVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
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
    @Specialization(guards = {"!isObject(vector)", "hasNames(vector)"})
    protected RLogicalVector accessNames(RLogicalVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        byte[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(attrProfiles), p, p.getLength(), namesNACheck);
        return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasNames(vector)"})
    protected RLogicalVector access(RLogicalVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        byte[] data = accessInternal(vector, p);
        return RDataFactory.createLogicalVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionNA(position)", "isSubset()"})
    protected RLogicalVector accessNA(RLogicalVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (vector.getNames(attrProfiles) == null) {
            return RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            return RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_NA}, RDataFactory.INCOMPLETE_VECTOR, RDataFactory.createNAStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "isSubset()"})
    protected RLogicalVector accessNames(RAbstractLogicalVector vector, Object exact, int recLevel, int position, Object dropDim) {
        byte val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createLogicalVector(new byte[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "!isSubset()"})
    protected byte accessNoSubset(RLogicalVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "!hasNames(vector)"})
    protected byte accessNoNames(RLogicalVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)"})
    protected byte access(RLogicalVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionZero(position)"})
    protected RLogicalVector accessPosZero(RLogicalVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames(attrProfiles) == null) {
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

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RStringVector accessNames(VirtualFrame frame, RStringVector vector, Object exact, int recLevel, Object[] positions, @SuppressWarnings("unused") RMissing dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, RRuntime.LOGICAL_TRUE);
    }

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RStringVector accessNames(VirtualFrame frame, RStringVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
    }

    @SuppressWarnings("unused")
    protected RStringVector accessNamesInternal(VirtualFrame frame, RStringVector vector, Object exact, int recLevel, Object[] positions, byte dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim);
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

    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RStringVector access(VirtualFrame frame, RStringVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    @SuppressWarnings("unused") RMissing dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, RRuntime.LOGICAL_TRUE);
        String[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RStringVector access(VirtualFrame frame, RStringVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
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
    @Specialization(guards = {"!isObject(vector)", "hasNames(vector)"})
    protected RStringVector accessNames(RStringVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        String[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(attrProfiles), p, p.getLength(), namesNACheck);
        return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasNames(vector)"})
    protected RStringVector access(RStringVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        String[] data = accessInternal(vector, p);
        return RDataFactory.createStringVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionNA(position)", "isSubset()"})
    protected RStringVector accessNA(RStringVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (vector.getNames(attrProfiles) == null) {
            return RDataFactory.createNAStringVector();
        } else {
            return RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, RDataFactory.INCOMPLETE_VECTOR, RDataFactory.createNAStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "isSubset()"})
    protected RStringVector accessNames(RAbstractStringVector vector, Object exact, int recLevel, int position, Object dropDim) {
        String val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createStringVector(new String[]{val}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "!isSubset()"})
    protected String accessNoSubset(RStringVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "!hasNames(vector)"})
    protected String accessNoNames(RStringVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)"})
    protected String access(RStringVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionZero(position)"})
    protected RStringVector accessPosZero(RStringVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames(attrProfiles) == null) {
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

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RComplexVector accessNames(VirtualFrame frame, RComplexVector vector, Object exact, int recLevel, Object[] positions, @SuppressWarnings("unused") RMissing dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, RRuntime.LOGICAL_TRUE);
    }

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RComplexVector accessNames(VirtualFrame frame, RComplexVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
    }

    @SuppressWarnings("unused")
    protected RComplexVector accessNamesInternal(VirtualFrame frame, RComplexVector vector, Object exact, int recLevel, Object[] positions, byte dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim);
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

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RComplexVector access(VirtualFrame frame, RComplexVector vector, Object exact, int recLevel, Object[] positions, RMissing dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, RRuntime.LOGICAL_TRUE);
        double[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), res.dimensions);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RComplexVector access(VirtualFrame frame, RComplexVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
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
    @Specialization(guards = {"!isObject(vector)", "hasNames(vector)"})
    protected RComplexVector accessNames(RComplexVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        double[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(attrProfiles), p, p.getLength(), namesNACheck);
        return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA(), names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasNames(vector)"})
    protected RComplexVector access(RComplexVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        double[] data = accessInternal(vector, p);
        return RDataFactory.createComplexVector(data, elementNACheck.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionNA(position)", "isSubset()"})
    protected RComplexVector accessNA(RComplexVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (vector.getNames(attrProfiles) == null) {
            return RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            return RDataFactory.createComplexVector(new double[]{RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART}, RDataFactory.INCOMPLETE_VECTOR,
                            RDataFactory.createNAStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "isSubset()"})
    protected RComplexVector accessNames(RAbstractComplexVector vector, Object exact, int recLevel, int position, Object dropDim) {
        RComplex val = vector.getDataAt(position - 1);
        elementNACheck.check(val);
        return RDataFactory.createComplexVector(new double[]{val.getRealPart(), val.getImaginaryPart()}, elementNACheck.neverSeenNA(), getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "!isSubset()"})
    protected RComplex accessNoSubset(RComplexVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "!hasNames(vector)"})
    protected RComplex accessNoNames(RComplexVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)"})
    protected RComplex access(RComplexVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionZero(position)"})
    protected RComplexVector accessPosZero(RComplexVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames(attrProfiles) == null) {
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

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RRawVector accessNames(VirtualFrame frame, RRawVector vector, Object exact, int recLevel, Object[] positions, @SuppressWarnings("unused") RMissing dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, RRuntime.LOGICAL_TRUE);
    }

    @Specialization(guards = {"!isObject(vector)", "hasDimNames(vector)"})
    protected RRawVector accessNames(VirtualFrame frame, RRawVector vector, Object exact, int recLevel, Object[] positions, RAbstractLogicalVector dropDim) {
        return accessNamesInternal(frame, vector, exact, recLevel, positions, dropDim.getLength() == 0 ? RRuntime.LOGICAL_TRUE : dropDim.getDataAt(0));
    }

    @SuppressWarnings("unused")
    protected RRawVector accessNamesInternal(VirtualFrame frame, RRawVector vector, Object exact, int recLevel, Object[] positions, byte dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, dropDim);
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

    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
    protected RRawVector access(VirtualFrame frame, RRawVector vector, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") int recLevel, Object[] positions,
                    @SuppressWarnings("unused") RMissing dropDim) {
        DimsAndResultLength res = getDimsAndResultLength(positions, RRuntime.LOGICAL_TRUE);
        byte[] data = accessInternal(frame, res, vector, positions);
        return RDataFactory.createRawVector(data, res.dimensions);
    }

    @Specialization(guards = {"!isObject(vector)", "!hasDimNames(vector)"})
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
    @Specialization(guards = {"!isObject(vector)", "hasNames(vector)"})
    protected RRawVector accessNames(RRawVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        byte[] data = accessInternal(vector, p);
        RStringVector names = getNamesVector(vector.getNames(attrProfiles), p, p.getLength(), namesNACheck);
        return RDataFactory.createRawVector(data, names);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!hasNames(vector)"})
    protected RRawVector access(RRawVector vector, Object exact, int recLevel, RIntVector p, Object dropDim) {
        byte[] data = accessInternal(vector, p);
        return RDataFactory.createRawVector(data);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionNA(position)", "isSubset()"})
    protected RRawVector accessNA(RRawVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (vector.getNames(attrProfiles) == null) {
            return RDataFactory.createRawVector(new byte[]{0});
        } else {
            return RDataFactory.createRawVector(new byte[]{0}, RDataFactory.createNAStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "isSubset()"})
    protected RRawVector accessNames(RAbstractRawVector vector, Object exact, int recLevel, int position, Object dropDim) {
        RRaw val = vector.getDataAt(position - 1);
        return RDataFactory.createRawVector(new byte[]{val.getValue()}, getName(vector, position));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "hasNames(vector)", "!isSubset()"})
    protected RRaw accessNoSubset(RRawVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)", "!hasNames(vector)"})
    protected RRaw accessNoNames(RRawVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "!isPositionZero(position)", "!isPositionNA(position)", "!isPositionNegative(position)"})
    protected RRaw access(RRawVector vector, Object exact, int recLevel, int position, Object dropDim) {
        return vector.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "isPositionZero(position)"})
    protected RRawVector accessPosZero(RRawVector vector, Object exact, int recLevel, int position, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        }
        if (vector.getNames(attrProfiles) == null) {
            return RDataFactory.createEmptyRawVector();
        } else {
            return RDataFactory.createRawVector(new byte[0], RDataFactory.createEmptyStringVector());
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "noPosition(positions)"})
    protected Object accessListEmptyPos(RAbstractContainer container, Object exact, int recLevel, RList positions, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_LESS_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "onePosition(positions)"})
    protected Object accessListOnePos(RAbstractContainer container, Object exact, int recLevel, RList positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(vector)", "multiPos(positions)"})
    protected Object accessListMultiPosList(RList vector, Object exact, int recLevel, RList positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isObject(container)", "multiPos(positions)", "!isVectorList(container)"})
    protected Object accessListMultiPos(RAbstractContainer container, Object exact, int recLevel, RList positions, Object dropDim) {
        if (!isSubset) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.SELECT_MORE_1);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "list");
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isObject(container)")
    protected Object accessListMultiPos(RAbstractContainer container, Object exact, int recLevel, RComplex positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "complex");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isObject(container)")
    protected Object accessListMultiPos(RAbstractContainer container, Object exact, int recLevel, RRaw positions, Object dropDim) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_SUBSCRIPT_TYPE, "raw");
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object access(VirtualFrame frame, RExpression expression, Object exact, int recLevel, int position, Object dropDim) {
        if (position < 1) {
            error.enter();
            throw RError.error(Message.SELECT_LESS_1);
        }
        return expression.getDataAt(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object access(VirtualFrame frame, RLanguage lang, Object exact, int recLevel, int position, Object dropDim) {
        if (position < 1) {
            error.enter();
            throw RError.error(Message.SELECT_LESS_1);
        }
        return lang.getDataAtAsObject(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object access(RPairList pairlist, Object exact, int recLevel, int position, Object dropDim) {
        if (position < 1) {
            error.enter();
            throw RError.error(Message.SELECT_LESS_1);
        }
        return pairlist.getDataAtAsObject(position - 1);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object access(REnvironment env, Object exact, int recLevel, Object position, Object dropDim) {
        if (isSubset) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OBJECT_NOT_SUBSETTABLE, "environment");
        }
        if (!(position instanceof String || ((position instanceof RStringVector) && ((RStringVector) position).getLength() == 1))) {
            error.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.WRONG_ARGS_SUBSET_ENV);
        }
        Object obj = env.get(RRuntime.asString(position));
        return obj == null ? RNull.instance : obj;
    }

    protected boolean outOfBounds(RList vector, int position) {
        return position > vector.getLength();
    }

    protected boolean outOfBoundsNegative(RAbstractContainer container, int position) {
        return -position > container.getLength();
    }

    protected boolean oneElemVector(RAbstractContainer container) {
        return container.getLength() == 1;
    }

    protected boolean isPositionNegative(int position) {
        return position < 0;
    }

    protected boolean isVectorList(RAbstractContainer container) {
        return container instanceof RList;
    }

    protected boolean wrongDimensions(RAbstractVector container, Object[] positions) {
        return container.getDimensions() == null || container.getDimensions().length != positions.length;
    }

    protected static boolean isFirstPositionPositive(RAbstractIntVector positions) {
        return positions.getDataAt(0) > 0;
    }

    protected static boolean isFirstPositionOne(RAbstractIntVector positions) {
        return positions.getDataAt(0) == 1;
    }

    protected static boolean isPositionZero(int position) {
        return position == 0;
    }

    protected boolean isSubset() {
        return isSubset;
    }

    protected boolean hasNames(RAbstractContainer container) {
        return container.getNames(attrProfiles) != null;
    }

    protected static boolean hasDimNames(RAbstractContainer container) {
        return container.getDimNames() != null;
    }

    protected static boolean twoPosition(RAbstractVector p) {
        return p.getLength() == 2;
    }

    protected static boolean onePosition(RAbstractVector p) {
        return p.getLength() == 1;
    }

    protected static boolean noPosition(RAbstractVector p) {
        return p.getLength() == 0;
    }

    protected static boolean multiPos(RAbstractVector positions) {
        return positions.getLength() > 1;
    }

    protected static boolean inRecursion(int recLevel) {
        return recLevel > 0;
    }

    protected static boolean isPositionNA(int position) {
        return RRuntime.isNA(position);
    }

    protected boolean isObject(RAbstractContainer container) {
        return container.isObject(attrProfiles) && forObjects;
    }

    public static AccessArrayNode create(boolean isSubset, boolean forObjects, boolean exactInSource, boolean dropInSource, RNode vector, RNode exact, PositionsArrayNode positions, RNode dropDim) {
        return AccessArrayNodeGen.create(isSubset, forObjects, exactInSource, dropInSource, vector, exact, ConstantNode.create(0), positions, dropDim);
    }

}
