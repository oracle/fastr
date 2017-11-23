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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

abstract class WriteIndexedVectorNode extends Node {

    private final RType vectorType;
    private final int totalDimensions;
    private final int dimensionIndex;
    private final boolean positionAppliesToRight;
    private final boolean skipNA;
    private final boolean isReplace;

    private final ConditionProfile completeVectorProfile = ConditionProfile.createBinaryProfile();

    protected WriteIndexedVectorNode(RType vectorType, int totalDimensions, int dimensionIndex, boolean positionAppliesToRight, boolean skipNA, boolean isReplace) {
        this.vectorType = vectorType;
        this.totalDimensions = totalDimensions;
        this.dimensionIndex = dimensionIndex;
        this.positionAppliesToRight = positionAppliesToRight;
        this.skipNA = skipNA;
        this.isReplace = isReplace;
    }

    public static WriteIndexedVectorNode create(RType vectorType, int totalDimensions, boolean positionAppliesToValue, boolean skipNA, boolean isReplace) {
        return WriteIndexedVectorNodeGen.create(vectorType, totalDimensions, totalDimensions - 1, positionAppliesToValue, skipNA, isReplace);
    }

    protected abstract void execute(RAbstractVector left, Object[] positions, RAbstractContainer right, int[] positionTargetDimensions);

    protected WriteIndexedVectorAccessNode createWrite() {
        return WriteIndexedVectorAccessNodeGen.create(vectorType, totalDimensions, dimensionIndex, positionAppliesToRight, skipNA, isReplace);
    }

    @Specialization(guards = {"leftAccess.supports(left)", "rightAccess.supports(right)"})
    protected void write(RAbstractVector left, Object[] positions, RAbstractContainer right, int[] positionTargetDimensions,
                    @Cached("left.access()") VectorAccess leftAccess,
                    @Cached("right.access()") VectorAccess rightAccess,
                    @Cached("createWrite()") WriteIndexedVectorAccessNode write) {
        try (RandomIterator leftIter = leftAccess.randomAccess(left); RandomIterator rightIter = rightAccess.randomAccess(right)) {
            write.apply(leftIter, leftAccess, positions, rightIter, rightAccess, right, positionTargetDimensions);

            if (completeVectorProfile.profile(left.isComplete())) {
                if (!(leftAccess.na.neverSeenNA() && rightAccess.na.neverSeenNA())) {
                    left.setComplete(false);
                }
            }
        }
    }

    @Specialization(replaces = "write")
    @TruffleBoundary
    protected void writeGeneric(RAbstractVector left, Object[] positions, RAbstractContainer right, int[] positionTargetDimensions,
                    @Cached("createWrite()") WriteIndexedVectorAccessNode write) {
        VectorAccess leftAccess = left.slowPathAccess();
        VectorAccess rightAccess = right.slowPathAccess();
        try (RandomIterator leftIter = leftAccess.randomAccess(left); RandomIterator rightIter = rightAccess.randomAccess(right)) {
            write.apply(leftIter, leftAccess, positions, rightIter, rightAccess, right, positionTargetDimensions);
        }
    }
}

/**
 * Primitive indexed N-dimensional vector write node. It can be used for vector replaces and
 * extracts. The only difference is that replace indexes the left vector and extract indexes the
 * right vector. The index direction is indicated with the boolean flag
 * {@link #positionsApplyToRight}.
 */
abstract class WriteIndexedVectorAccessNode extends Node {

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

    @Child private WriteIndexedVectorAccessNode innerVectorNode;

    @Child private UpdateShareableChildValueNode updateStateOfListElement;
    @Child private ShareObjectNode shareObjectNode;

    private final boolean isReplace;
    private final RType vectorType;

    protected WriteIndexedVectorAccessNode(RType vectorType, int totalDimensions, int dimensionIndex, boolean positionAppliesToRight, boolean skipNA, boolean isReplace) {
        this.vectorType = vectorType;
        this.dimensionIndex = dimensionIndex;
        this.totalDimensions = totalDimensions;
        this.positionsApplyToRight = positionAppliesToRight;
        this.skipNA = skipNA;
        this.isReplace = isReplace;
        if (dimensionIndex > 0) {
            innerVectorNode = WriteIndexedVectorAccessNodeGen.create(vectorType, totalDimensions, dimensionIndex - 1, positionAppliesToRight, skipNA, isReplace);
        }
        if (vectorType == RType.List || vectorType == RType.Expression) {
            if (!isReplace) {
                updateStateOfListElement = UpdateShareableChildValueNode.create();
            } else {
                shareObjectNode = ShareObjectNode.create();
            }
        }
    }

    public void apply(RandomIterator leftIter, VectorAccess leftAccess, Object[] positions, RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right,
                    int[] positionTargetDimensions) {
        assert totalDimensions == positions.length : "totalDimensions must be constant per vector write node";

        int leftLength = leftAccess.getLength(leftIter);
        int rightLength = rightAccess.getLength(rightIter);

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

        applyImpl(leftIter, leftAccess, 0, leftLength, positionTargetDimensions, firstTargetDimension,
                        positions, initialPositionOffset,
                        rightIter, rightAccess, right, 0, rightLength, false);
    }

    private final ConditionProfile positionMatchesTargetDimensionsProfile = ConditionProfile.createBinaryProfile();

    private int applyImpl(//
                    RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, int targetDimension,
                    Object[] positions, int positionOffset,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA) {

        Object position = positionClassProfile.profile(positions[dimensionIndex]);

        int positionLength = getPositionLength(position);
        int newPositionOffset;
        if (positionMatchesTargetDimensionsProfile.profile(positionOffset == targetDimension)) {
            newPositionOffset = 1;
        } else {
            newPositionOffset = positionOffsetProfile.profile(positionOffset / targetDimension);
        }
        return execute(leftIter, leftAccess, leftBase, leftLength, targetDimensions, targetDimension,
                        positions, position, newPositionOffset, positionLength,
                        rightIter, rightAccess, right, rightBase, rightLength, parentNA);
    }

    private int getPositionLength(Object position) {
        if (position instanceof RAbstractVector) {
            return positionLengthProfile.profile(((RAbstractVector) position).getLength());
        } else {
            return -1;
        }
    }

    protected abstract int execute(RandomIterator leftIter, VectorAccess leftAccess, int storeBase, int storeLength, Object targetDimensions, int targetDimension,
                    Object[] positions, Object position, int positionOffset, int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int valueBase, int valueLength, boolean parentNA);

    @Specialization
    protected int doMissing(RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, int targetDimension,
                    Object[] positions, @SuppressWarnings("unused") RMissing position, int positionOffset, @SuppressWarnings("unused") int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA,
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        int rightIndex = rightBase;
        profile.profileCounted(targetDimension);
        for (int positionValue = 0; profile.inject(positionValue < targetDimension); positionValue += 1) {
            rightIndex = applyInner(//
                            leftIter, leftAccess, leftBase, leftLength, targetDimensions,
                            positions, positionOffset, positionValue,
                            rightIter, rightAccess, right, rightLength, rightIndex, parentNA);
        }
        return rightIndex;
    }

    @Specialization
    protected int doLogicalPosition(RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, int targetDimension,
                    Object[] positions, RAbstractLogicalVector position, int positionOffset, int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA,
                    @Cached("create()") BranchProfile wasTrue,
                    @Cached("create()") AlwaysOnBranchProfile outOfBounds,
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
                                    leftIter, leftAccess, leftBase, leftLength, targetDimensions,
                                    positions, positionOffset, i,
                                    rightIter, rightAccess, right, rightLength, rightIndex, isNA || parentNA);
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
    protected int doIntegerSequencePosition(RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, @SuppressWarnings("unused") int targetDimension,
                    Object[] positions, RIntSequence position, int positionOffset, int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA,
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
                            leftIter, leftAccess, leftBase, leftLength, targetDimensions,
                            positions, positionOffset, positionValue,
                            rightIter, rightAccess, right, rightLength, rightIndex, parentNA);
        }
        return rightIndex;
    }

    /**
     * Integer vectors iterate over the number of positions because we assume that the number of
     * positions in an integer vector is significantly lower than the number of elements. This might
     * not be always true and could benefit from more investigation.
     */
    @Specialization(replaces = "doIntegerSequencePosition")
    protected int doIntegerPosition(RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, @SuppressWarnings("unused") int targetDimension,
                    Object[] positions, RAbstractIntVector position, int positionOffset, int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA,
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
                            leftIter, leftAccess, leftBase, leftLength, targetDimensions,
                            positions, positionOffset, positionValue - 1,
                            rightIter, rightAccess, right, rightLength, rightIndex, isNA || parentNA);
        }
        return rightIndex;
    }

    private int applyInner(//
                    RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions,
                    Object[] positions, int positionOffset, int positionValue,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightLength, int actionIndex, boolean isNA) {
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
                leftAccess.setNA(leftIter, actionLeftIndex);
                leftAccess.na.seenNA();
            } else {
                if (vectorType == RType.List || vectorType == RType.Expression) {
                    setListElement(leftIter, leftAccess, rightIter, rightAccess, right, actionLeftIndex, actionRightIndex);
                } else {
                    leftAccess.setFromSameType(leftIter, actionLeftIndex, rightAccess, rightIter, actionRightIndex);
                }
                rightAccess.isNA(rightIter, actionRightIndex);
            }

            if (resetIndexProfile.profile((actionIndex + 1) == (positionsApplyToRight ? leftLength : rightLength))) {
                return 0;
            }
            return actionIndex + 1;
        } else {
            // generate another for-loop for other dimensions
            int nextTargetDimension = innerVectorNode.dimensionValueProfile.profile(((int[]) targetDimensions)[innerVectorNode.dimensionIndex]);
            return innerVectorNode.applyImpl(//
                            leftIter, leftAccess, newTargetIndex, leftLength, targetDimensions, nextTargetDimension,
                            positions, positionOffset,
                            rightIter, rightAccess, right, actionIndex, rightLength, isNA);
        }
    }

    private void setListElement(RandomIterator leftIter, VectorAccess leftAccess, RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int leftIndex, int rightIndex) {
        Object rightValue = rightAccess.getListElement(rightIter, rightIndex);
        if (isReplace) {
            // we are replacing within the same list
            if (leftAccess.getListElement(leftIter, leftIndex) != rightValue) {
                shareObjectNode.execute(rightValue);
            }
        } else {
            // we are writing into a list data that are being read from possibly another list
            updateStateOfListElement.execute(right, rightValue);
        }

        leftAccess.setListElement(leftIter, leftIndex, rightValue);
    }
}
