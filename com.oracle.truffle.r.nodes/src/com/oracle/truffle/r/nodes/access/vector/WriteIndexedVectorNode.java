/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.nodes.profile.AlwaysOnBranchProfile;
import com.oracle.truffle.r.nodes.profile.IntValueProfile;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Primitive indexed N-dimensional vector write node. It can be used for vector replaces and
 * extracts. The only difference is that replace indexes the left vector and extract indexes the
 * right vector. The index direction is indicated with the boolean flag
 * {@link #positionsApplyToRight}.
 */
abstract class WriteIndexedVectorNode extends Node {

    private final int dimensionIndex;
    private final int totalDimensions;

    /**
     * Indicates if the position vectors index into the left or the right vector. This enables us to
     * share the same node for vector replaces and vector extracts.
     */
    private final boolean positionsApplyToRight;
    /**
     * If skipNA is true then no action should be invoked for NA values and its indexed
     * subdimensions.
     */
    private final boolean skipNA;

    private final VectorLengthProfile positionLengthProfile = VectorLengthProfile.create();
    private final VectorLengthProfile positionOffsetProfile = VectorLengthProfile.create();
    private final VectorLengthProfile dimensionValueProfile = VectorLengthProfile.create();
    private final ValueProfile positionClassProfile = ValueProfile.createClassProfile();
    private final NACheck positionNACheck = NACheck.create();
    private final ConditionProfile resetIndexProfile = ConditionProfile.createBinaryProfile();

    @Child private WriteIndexedScalarNode<RAbstractVector, RTypedValue> scalarNode;
    @Child private WriteIndexedVectorNode innerVectorNode;

    @SuppressWarnings("unchecked")
    protected WriteIndexedVectorNode(RType vectorType, int totalDimensions, int dimensionIndex, boolean positionAppliesToRight, boolean skipNA, boolean setListElementAsObject, boolean isReplace) {
        this.scalarNode = (WriteIndexedScalarNode<RAbstractVector, RTypedValue>) createIndexedAction(vectorType, setListElementAsObject, isReplace);
        this.dimensionIndex = dimensionIndex;
        this.totalDimensions = totalDimensions;
        this.positionsApplyToRight = positionAppliesToRight;
        this.skipNA = skipNA;
        if (dimensionIndex > 0) {
            innerVectorNode = WriteIndexedVectorNodeGen.create(vectorType, totalDimensions, dimensionIndex - 1, positionAppliesToRight, skipNA, setListElementAsObject, isReplace);
        }
    }

    public static WriteIndexedVectorNode create(RType vectorType, int totalDimensions, boolean positionAppliesToValue, boolean skipNA, boolean setListElementAsObject, boolean isReplace) {
        return WriteIndexedVectorNodeGen.create(vectorType, totalDimensions, totalDimensions - 1, positionAppliesToValue, skipNA, setListElementAsObject, isReplace);
    }

    public NACheck getValueNACheck() {
        return scalarNode.valueNACheck;
    }

    public void enableValueNACheck(RAbstractContainer vector) {
        getValueNACheck().enable(vector);
        if (innerVectorNode != null) {
            innerVectorNode.enableValueNACheck(vector);
        }
    }

    public boolean neverSeenNAInValue() {
        if (getValueNACheck().neverSeenNA()) {
            if (innerVectorNode == null || innerVectorNode.neverSeenNAInValue()) {
                return true;
            }
        }
        return false;
    }

    public final void apply(RAbstractVector left, int leftLength,
                    Object[] positions, RTypedValue right, int rightLength, int[] positionTargetDimensions) {
        assert left.getLength() == leftLength;
        assert totalDimensions == positions.length : "totalDimensions must be constant per vector write node";

        Object leftStore = left.getInternalStore();
        Object rightStore = null;
        if (right instanceof RAbstractContainer) {
            RAbstractContainer rightContainer = (RAbstractContainer) right;
            assert rightContainer.getLength() == rightLength;
            rightStore = rightContainer.getInternalStore();
        }

        int initialPositionOffset;
        if (positionsApplyToRight) {
            initialPositionOffset = rightLength;
        } else {
            initialPositionOffset = leftLength;
        }

        int firstTargetDimension;
        if (totalDimensions == 0 || positionTargetDimensions == null) {
            // no dimensions
            firstTargetDimension = initialPositionOffset;
        } else {
            firstTargetDimension = dimensionValueProfile.profile(positionTargetDimensions[dimensionIndex]);
        }

        applyImpl(left, leftStore, 0, leftLength, positionTargetDimensions, firstTargetDimension,
                        positions, initialPositionOffset,
                        right, rightStore, 0, rightLength, false);
    }

    private final ConditionProfile positionMatchesTargetDimensionsProfile = ConditionProfile.createBinaryProfile();

    private int applyImpl(//
                    RAbstractVector left, Object leftStore, int leftBase, int leftLength, Object targetDimensions, int targetDimension,
                    Object[] positions, int positionOffset,
                    RTypedValue right, Object rightStore, int rightBase, int rightLength, boolean parentNA) {

        Object position = positionClassProfile.profile(positions[dimensionIndex]);

        int positionLength = getPositionLength(position);
        int newPositionOffset;
        if (positionMatchesTargetDimensionsProfile.profile(positionOffset == targetDimension)) {
            newPositionOffset = 1;
        } else {
            newPositionOffset = positionOffsetProfile.profile(positionOffset / targetDimension);
        }
        return execute(left, leftStore, leftBase, leftLength, targetDimensions, targetDimension,
                        positions, position, newPositionOffset, positionLength,
                        right, rightStore, rightBase, rightLength, parentNA);
    }

    private int getPositionLength(Object position) {
        if (position instanceof RAbstractVector) {
            return positionLengthProfile.profile(((RAbstractVector) position).getLength());
        } else {
            return -1;
        }
    }

    protected abstract int execute(RAbstractVector left, Object leftStore, int storeBase, int storeLength, Object targetDimensions, int targetDimension,
                    Object[] positions, Object position, int positionOffset, int positionLength,
                    RTypedValue right, Object rightStore, int valueBase, int valueLength, boolean parentNA);

    @SuppressWarnings("unused")
    @Specialization
    protected int doMissing(RAbstractVector left, Object leftStore, int leftBase, int leftLength, Object targetDimensions, int targetDimension,
                    Object[] positions, RMissing position, int positionOffset, int positionLength,
                    RAbstractContainer right, Object rightStore, int rightBase, int rightLength, boolean parentNA,
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        int rightIndex = rightBase;
        profile.profileCounted(targetDimension);
        for (int positionValue = 0; profile.inject(positionValue < targetDimension); positionValue += 1) {
            rightIndex = applyInner(//
                            left, leftStore, leftBase, leftLength, targetDimensions,
                            positions, positionOffset, positionValue,
                            right, rightStore, rightLength, rightIndex, parentNA);
        }
        return rightIndex;
    }

    @Specialization
    protected int doLogicalPosition(RAbstractVector left, Object leftStore, int leftBase, int leftLength, Object targetDimensions, int targetDimension,
                    Object[] positions, RAbstractLogicalVector position, int positionOffset, int positionLength,
                    RTypedValue right, Object rightStore, int rightBase, int rightLength, boolean parentNA,
                    @Cached("create()") BranchProfile wasTrue, @Cached("create()") AlwaysOnBranchProfile outOfBounds,
                    @Cached("createCountingProfile()") LoopConditionProfile profile,
                    @Cached("createBinaryProfile()") ConditionProfile incModProfile) {
        positionNACheck.enable(!skipNA && !position.isComplete());

        int length = targetDimension;
        if (positionLength > targetDimension) {
            outOfBounds.enter();
            length = positionLength;
        }

        int rightIndex = rightBase;
        if (positionLength > 0) {

            int positionIndex = 0;
            profile.profileCounted(length);
            for (int i = 0; profile.inject(i < length); i++) {
                byte positionValue = position.getDataAt(positionIndex);
                boolean isNA = positionNACheck.check(positionValue);
                if (isNA || positionValue == RRuntime.LOGICAL_TRUE) {
                    wasTrue.enter();
                    if (outOfBounds.isVisited() && i >= targetDimension) {
                        isNA = true;
                    }
                    rightIndex = applyInner(//
                                    left, leftStore, leftBase, leftLength, targetDimensions,
                                    positions, positionOffset, i,
                                    right, rightStore, rightLength, rightIndex, isNA || parentNA);
                }
                positionIndex = Utils.incMod(positionIndex, positionLength, incModProfile);
            }
        }
        return rightIndex;
    }

    /**
     * For integer sequences we need to make sure that start and stride is profiled.
     *
     * @throws SlowPathException
     */
    @Specialization(rewriteOn = SlowPathException.class)
    protected int doIntegerSequencePosition(RAbstractVector left, Object leftStore, int leftBase, int leftLength, Object targetDimensions, @SuppressWarnings("unused") int targetDimension,
                    Object[] positions, RIntSequence position, int positionOffset, int positionLength,
                    RTypedValue right, Object rightStore, int rightBase, int rightLength, boolean parentNA,
                    @Cached("create()") IntValueProfile startProfile,
                    @Cached("create()") IntValueProfile strideProfile,
                    @Cached("createBinaryProfile()") ConditionProfile conditionProfile,
                    @Cached("createCountingProfile()") LoopConditionProfile profile) throws SlowPathException {
        // skip NA check. sequences never contain NA values.
        int rightIndex = rightBase;
        int start = startProfile.profile(position.getStart() - 1);
        int stride = strideProfile.profile(position.getStride());
        int end = start + positionLength * stride;

        if (start < 0 || end <= 0) {
            throw new SlowPathException("rewrite to doIntegerPosition");
        }

        boolean ascending = conditionProfile.profile(start < end);
        profile.profileCounted(positionLength);
        for (int positionValue = start; profile.inject(ascending ? positionValue < end : positionValue > end); positionValue += stride) {
            rightIndex = applyInner(//
                            left, leftStore, leftBase, leftLength, targetDimensions,
                            positions, positionOffset, positionValue,
                            right, rightStore, rightLength, rightIndex, parentNA);
        }
        return rightIndex;
    }

    /**
     * Integer vectors iterate over the number of positions because we assume that the number of
     * positions in an integer vector is significantly lower than the number of elements in the
     * store. This might not be always true and could benefit from more investigation.
     */
    @Specialization(contains = "doIntegerSequencePosition")
    protected int doIntegerPosition(RAbstractVector left, Object leftStore, int leftBase, int leftLength, Object targetDimensions, @SuppressWarnings("unused") int targetDimension,
                    Object[] positions, RAbstractIntVector position, int positionOffset, int positionLength,
                    RTypedValue right, Object rightStore, int rightBase, int rightLength, boolean parentNA,
                    @Cached("createCountingProfile()") LoopConditionProfile lengthProfile) {
        positionNACheck.enable(position);
        int rightIndex = rightBase;

        lengthProfile.profileCounted(positionLength);
        for (int i = 0; lengthProfile.inject(i < positionLength); i++) {
            int positionValue = position.getDataAt(i);
            boolean isNA = positionNACheck.check(positionValue);
            if (isNA) {
                if (skipNA) {
                    continue;
                }
            }
            rightIndex = applyInner(//
                            left, leftStore, leftBase, leftLength, targetDimensions,
                            positions, positionOffset, positionValue - 1,
                            right, rightStore, rightLength, rightIndex, isNA || parentNA);
        }
        return rightIndex;
    }

    @SuppressWarnings("all")
    private int applyInner(//
                    RAbstractVector left, Object leftStore, int leftBase, int leftLength, Object targetDimensions,
                    Object[] positions, int positionOffset, int positionValue,
                    RTypedValue right, Object rightStore, int rightLength, int actionIndex, boolean isNA) {
        int newTargetIndex = leftBase + positionValue * positionOffset;
        if (dimensionIndex == 0) {
            // for-loops leaf for innermost dimension

            // if position indexes value we just need to switch indices
            int actionLeftIndex;
            int actionRightIndex;
            if (positionsApplyToRight) {
                actionLeftIndex = actionIndex;
                actionRightIndex = newTargetIndex;
            } else {
                actionLeftIndex = newTargetIndex;
                actionRightIndex = actionIndex;
            }

            if (isNA) {
                scalarNode.applyNA(left, leftStore, actionLeftIndex);
            } else {
                scalarNode.apply(left, leftStore, actionLeftIndex, right, rightStore, actionRightIndex);
            }

            if (resetIndexProfile.profile((actionIndex + 1) == (positionsApplyToRight ? leftLength : rightLength))) {
                return 0;
            }
            return actionIndex + 1;
        } else {
            // generate another for-loop for other dimensions
            int nextTargetDimension = innerVectorNode.dimensionValueProfile.profile(((int[]) targetDimensions)[innerVectorNode.dimensionIndex]);
            return innerVectorNode.applyImpl(//
                            left, leftStore, newTargetIndex, leftLength, targetDimensions, nextTargetDimension,
                            positions, positionOffset,
                            right, rightStore, actionIndex, rightLength, isNA);
        }
    }

    private static WriteIndexedScalarNode<? extends RAbstractVector, ? extends RTypedValue> createIndexedAction(RType type, boolean setListElementAsObject, boolean isReplace) {
        switch (type) {
            case Logical:
                return new WriteLogicalAction();
            case Integer:
                return new WriteIntegerAction();
            case Double:
                return new WriteDoubleAction();
            case Complex:
                return new WriteComplexAction();
            case Character:
                return new WriteCharacterAction();
            case Raw:
                return new WriteRawAction();
            case Language:
            case Expression:
            case PairList:
            case List:
                return new WriteListAction(setListElementAsObject, isReplace);
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    private abstract static class WriteIndexedScalarNode<A extends RAbstractVector, V extends RTypedValue> extends Node {

        final NACheck valueNACheck = NACheck.create();

        abstract void apply(A leftAccess, Object leftStore, int leftIndex, V rightAccess, Object rightStore, int rightIndex);

        abstract void applyNA(A leftAccess, Object leftStore, int leftIndex);

    }

    private static final class WriteLogicalAction extends WriteIndexedScalarNode<RAbstractLogicalVector, RAbstractLogicalVector> {

        @Override
        void apply(RAbstractLogicalVector leftAccess, Object leftStore, int leftIndex, RAbstractLogicalVector rightAccess, Object rightStore, int rightIndex) {
            byte value = rightAccess.getDataAt(rightStore, rightIndex);
            leftAccess.setDataAt(leftStore, leftIndex, value);
            valueNACheck.check(value);
        }

        @Override
        void applyNA(RAbstractLogicalVector leftAccess, Object leftStore, int leftIndex) {
            leftAccess.setDataAt(leftStore, leftIndex, RRuntime.LOGICAL_NA);
            valueNACheck.seenNA();
        }
    }

    private static final class WriteIntegerAction extends WriteIndexedScalarNode<RAbstractIntVector, RAbstractIntVector> {

        @Override
        void apply(RAbstractIntVector leftAccess, Object leftStore, int leftIndex, RAbstractIntVector rightAccess, Object rightStore, int rightIndex) {
            int value = rightAccess.getDataAt(rightStore, rightIndex);
            leftAccess.setDataAt(leftStore, leftIndex, value);
            valueNACheck.check(value);
        }

        @Override
        void applyNA(RAbstractIntVector leftAccess, Object leftStore, int leftIndex) {
            leftAccess.setDataAt(leftStore, leftIndex, RRuntime.INT_NA);
            valueNACheck.seenNA();
        }
    }

    private static final class WriteDoubleAction extends WriteIndexedScalarNode<RAbstractDoubleVector, RAbstractDoubleVector> {

        @Override
        void apply(RAbstractDoubleVector leftAccess, Object leftStore, int leftIndex, RAbstractDoubleVector rightAccess, Object rightStore, int rightIndex) {
            double value = rightAccess.getDataAt(rightStore, rightIndex);
            leftAccess.setDataAt(leftStore, leftIndex, value);
            valueNACheck.check(value);
        }

        @Override
        void applyNA(RAbstractDoubleVector leftAccess, Object leftStore, int leftIndex) {
            leftAccess.setDataAt(leftStore, leftIndex, RRuntime.DOUBLE_NA);
            valueNACheck.seenNA();
        }
    }

    private static final class WriteComplexAction extends WriteIndexedScalarNode<RAbstractComplexVector, RAbstractComplexVector> {

        @Override
        void apply(RAbstractComplexVector leftAccess, Object leftStore, int leftIndex, RAbstractComplexVector rightAccess, Object rightStore, int rightIndex) {
            RComplex value = rightAccess.getDataAt(rightStore, rightIndex);
            leftAccess.setDataAt(leftStore, leftIndex, value);
            valueNACheck.check(value);
        }

        @Override
        void applyNA(RAbstractComplexVector leftAccess, Object leftStore, int leftIndex) {
            leftAccess.setDataAt(leftStore, leftIndex, RComplex.createNA());
            valueNACheck.seenNA();
        }
    }

    private static final class WriteCharacterAction extends WriteIndexedScalarNode<RAbstractStringVector, RAbstractStringVector> {

        @Override
        void apply(RAbstractStringVector leftAccess, Object leftStore, int leftIndex, RAbstractStringVector rightAccess, Object rightStore, int rightIndex) {
            String value = rightAccess.getDataAt(rightStore, rightIndex);
            leftAccess.setDataAt(leftStore, leftIndex, value);
            valueNACheck.check(value);
        }

        @Override
        void applyNA(RAbstractStringVector leftAccess, Object leftStore, int leftIndex) {
            leftAccess.setDataAt(leftStore, leftIndex, RRuntime.STRING_NA);
            valueNACheck.seenNA();
        }
    }

    private static final class WriteRawAction extends WriteIndexedScalarNode<RAbstractRawVector, RAbstractRawVector> {

        @Override
        void apply(RAbstractRawVector leftAccess, Object leftStore, int leftIndex, RAbstractRawVector rightAccess, Object rightStore, int rightIndex) {
            byte value = rightAccess.getRawDataAt(rightStore, rightIndex);
            leftAccess.setRawDataAt(leftStore, leftIndex, value);
            valueNACheck.check(value);
        }

        @Override
        void applyNA(RAbstractRawVector leftAccess, Object leftStore, int leftIndex) {
            // nothing to do
        }
    }

    private static final class WriteListAction extends WriteIndexedScalarNode<RAbstractListBaseVector, RTypedValue> {

        private final boolean setListElementAsObject;
        private final boolean isReplace;
        @Child private UpdateShareableChildValueNode updateStateOfListElement;
        @Child private ShareObjectNode shareObjectNode;

        WriteListAction(boolean setListElementAsObject, boolean isReplace) {
            this.setListElementAsObject = setListElementAsObject;
            this.isReplace = isReplace;
            if (!isReplace) {
                updateStateOfListElement = UpdateShareableChildValueNode.create();
            } else {
                shareObjectNode = ShareObjectNode.create();
            }
        }

        @Override
        void apply(RAbstractListBaseVector leftAccess, Object leftStore, int leftIndex, RTypedValue rightAccess, Object rightStore, int rightIndex) {
            Object rightValue;
            if (setListElementAsObject) {
                rightValue = rightAccess;
                // unbox scalar vectors
                if (rightValue instanceof RScalarVector) {
                    rightValue = ((RScalarVector) rightValue).getDataAtAsObject(rightStore, 0);
                }
            } else {
                rightValue = ((RAbstractContainer) rightAccess).getDataAtAsObject(rightStore, rightIndex);
            }

            if (isReplace) {
                // we are replacing within the same list
                if (leftAccess.getDataAtAsObject(leftStore, leftIndex) != rightValue) {
                    shareObjectNode.execute(rightValue);
                }
            } else {
                // we are writing into a list data that are being read from possibly another list
                updateStateOfListElement.execute(rightAccess, rightValue);
            }

            leftAccess.setDataAt(leftStore, leftIndex, rightValue);
            valueNACheck.checkListElement(rightValue);
        }

        @Override
        void applyNA(RAbstractListBaseVector leftAccess, Object leftStore, int leftIndex) {
            leftAccess.setDataAt(leftStore, leftIndex, RNull.instance);
            valueNACheck.seenNA();
        }
    }
}
