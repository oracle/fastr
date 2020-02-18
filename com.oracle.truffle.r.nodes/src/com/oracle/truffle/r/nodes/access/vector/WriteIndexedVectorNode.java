/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.data.RIntSeqVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.nodes.profile.AlwaysOnBranchProfile;
import com.oracle.truffle.r.nodes.profile.IntValueProfile;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Delegates to {@link WriteIndexedVectorAccessNode} and only takes care of caching the
 * {@link VectorAccess} for the vectors and fallback to slow path {@link VectorAccess} if necessary.
 *
 * @see WriteIndexedVectorNode
 */
@ImportStatic(DSLConfig.class)
abstract class WriteIndexedVectorNode extends Node {

    private final WriteIndexedVectorParameters params;
    private final int dimensionIndex;

    protected WriteIndexedVectorNode(RType vectorType, int totalDimensions, int dimensionIndex, boolean skipNA, boolean isReplace) {
        this.params = new WriteIndexedVectorParameters(vectorType, totalDimensions, skipNA, isReplace);
        this.dimensionIndex = dimensionIndex;
    }

    public static WriteIndexedVectorNode create(RType vectorType, int totalDimensions, boolean skipNA, boolean isReplace) {
        return WriteIndexedVectorNodeGen.create(vectorType, totalDimensions, totalDimensions - 1, skipNA, isReplace);
    }

    protected abstract void execute(RAbstractVector left, Object[] positions, RAbstractContainer right, int[] positionTargetDimensions);

    protected WriteIndexedVectorAccessNode createWrite() {
        return WriteIndexedVectorAccessNodeGen.create(params, dimensionIndex);
    }

    // XXX getTypedVectorDataLibraryCacheSize()
    @Specialization(guards = {"leftAccess.supports(left)", "rightAccess.supports(right)"}, limit = "getGenericVectorAccessCacheSize()")
    protected void write(RAbstractVector left, Object[] positions, RAbstractContainer right, int[] positionTargetDimensions,
                    @Cached("left.access()") VectorAccess leftAccess,
                    @Cached("right.access()") VectorAccess rightAccess,
                    @Cached("createBinaryProfile()") ConditionProfile completeVectorProfile,
                    @Cached("createWrite()") WriteIndexedVectorAccessNode write,
                    @CachedLibrary("left") AbstractContainerLibrary leftLibrary,
                    @CachedLibrary("right") AbstractContainerLibrary rightLibrary,
                    @CachedLibrary("getPosition(positions)") AbstractContainerLibrary positionContainerLibrary) {
        try (RandomIterator leftIter = leftAccess.randomAccess(leftLibrary, left); RandomIterator rightIter = rightAccess.randomAccess(rightLibrary, right)) {
            write.apply(positionContainerLibrary, leftIter, leftAccess, positions, rightIter, rightAccess, right, positionTargetDimensions);

            if (completeVectorProfile.profile(leftLibrary.isComplete(left))) {
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
            write.apply(AbstractContainerLibrary.getFactory().getUncached(), leftIter, leftAccess, positions, rightIter, rightAccess, right, positionTargetDimensions);
            if (!(leftAccess.na.neverSeenNA() && rightAccess.na.neverSeenNA())) {
                left.setComplete(false);
            }
        }
    }

    protected Object getPosition(Object[] positions) {
        return positions[dimensionIndex];
    }
}

final class WriteIndexedVectorParameters {
    final RType vectorType;
    final int totalDimensions;
    final boolean skipNA;
    final boolean isReplace;

    /**
     * @param vectorType Type of the "left" vector passed to the {@code apply} method.
     * @param totalDimensions Total number of dimensions, i.e. the size of the array passed to the
     *            {@code apply} method.
     * @param skipNA if true then no action should be invoked for NA values and its indexed
     *            subdimensions (i.e. no write to the "left" argument of {@code apply}). Applies to
     *            integer indexes only.
     * @param isReplace the "left" argument of {@code apply} is target of a read from the "right",
     *            target of a replace.
     */
    WriteIndexedVectorParameters(RType vectorType, int totalDimensions, boolean skipNA, boolean isReplace) {
        this.vectorType = vectorType;
        this.totalDimensions = totalDimensions;
        this.skipNA = skipNA;
        this.isReplace = isReplace;
    }
}

/**
 * Primitive indexed N-dimensional vector write node. This node can be used for vector replaces and
 * extracts (in which case the write target is a vector preallocated to hold the extraction result).
 * The entry point is the {@code apply} method.
 */
@ImportStatic(DSLConfig.class)
abstract class WriteIndexedVectorAccessNode extends Node {

    private final WriteIndexedVectorParameters params;
    private final int dimensionIndex;

    @CompilationFinal private VectorLengthProfile positionOffsetProfile;
    @CompilationFinal private NACheck positionNACheck;
    @CompilationFinal private VectorLengthProfile dimensionValueProfile;
    @CompilationFinal private ConditionProfile resetIndexProfile;

    private final ValueProfile positionClassProfile = ValueProfile.createClassProfile();
    private final ConditionProfile positionMatchesTargetDimensionsProfile = ConditionProfile.createBinaryProfile();

    @Child private WriteIndexedVectorAccessNode innerVectorNode;

    @Child private UpdateShareableChildValueNode updateStateOfListElement;
    @Child private ShareObjectNode shareObjectNode;

    /**
     * @param dimensionIndex The index within the {@code positions} array passed to the
     *            {@code apply} method that this instance should handle. If not the last index, then
     *            this node will delegate to another instance to handle the next index.
     * @param params See the documentation of {@link WriteIndexedVectorParameters}.
     */
    protected WriteIndexedVectorAccessNode(WriteIndexedVectorParameters params, int dimensionIndex) {
        this.params = params;
        this.dimensionIndex = dimensionIndex;
        if (dimensionIndex > 0) {
            innerVectorNode = WriteIndexedVectorAccessNodeGen.create(params, dimensionIndex - 1);
        }
        if (params.vectorType == RType.List || params.vectorType == RType.Expression) {
            if (!params.isReplace) {
                updateStateOfListElement = UpdateShareableChildValueNode.create();
            } else {
                shareObjectNode = ShareObjectNode.create();
            }
        }
    }

    /**
     * Positions is an array of instances of {@link RMissing} (What is that doing?), or
     * {@link RIntVector}, or {@link RAbstractLogicalVector} (select only elements under
     * {@code TRUE} indexes). Each dimension must have one entry in this array.
     * 
     * The left vector is either the result of read (newly created vector to hold the result) or the
     * target of write operation. The boolean flag {@link WriteIndexedVectorParameters#isReplace}
     * passed to constructor distinguishes those two cases.
     */
    void apply(AbstractContainerLibrary library, RandomIterator leftIter, VectorAccess leftAccess, Object[] positions, RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right,
                    int[] positionTargetDimensions) {
        assert params.totalDimensions == positions.length : "totalDimensions must be constant per vector write node";

        int leftLength = leftAccess.getLength(leftIter);
        int rightLength = rightAccess.getLength(rightIter);

        int initialPositionOffset;
        if (!params.isReplace) {
            initialPositionOffset = rightLength;
        } else {
            initialPositionOffset = leftLength;
        }

        int firstTargetDimension;
        if (params.totalDimensions == 0 || positionTargetDimensions == null) {
            // no dimensions
            firstTargetDimension = initialPositionOffset;
        } else {
            firstTargetDimension = getDimensionValueProfile().profile(positionTargetDimensions[dimensionIndex]);
        }

        applyImpl(library, leftIter, leftAccess, 0, leftLength, positionTargetDimensions, firstTargetDimension,
                        positions, initialPositionOffset,
                        rightIter, rightAccess, right, 0, rightLength, false);
    }

    private int applyImpl(//
                    AbstractContainerLibrary library, RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, int targetDimension,
                    Object[] positions, int positionOffset,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA) {

        Object position = positionClassProfile.profile(positions[dimensionIndex]);

        int positionLength = getPositionLength(library, position);
        int newPositionOffset;
        if (positionMatchesTargetDimensionsProfile.profile(positionOffset == targetDimension)) {
            newPositionOffset = 1;
        } else {
            newPositionOffset = getPositionOffsetProfile().profile(positionOffset / targetDimension);
        }
        return execute(leftIter, leftAccess, leftBase, leftLength, targetDimensions, targetDimension,
                        positions, position, newPositionOffset, positionLength,
                        rightIter, rightAccess, right, rightBase, rightLength, parentNA);
    }

    private static int getPositionLength(AbstractContainerLibrary library, Object position) {
        if (position instanceof RAbstractVector) {
            // XXX
            return library.getLength(position);
        } else {
            return -1;
        }
    }

    protected abstract int execute(RandomIterator leftIter, VectorAccess leftAccess, int storeBase, int storeLength, Object targetDimensions, int targetDimension,
                    Object[] positions, Object position, int positionOffset, int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int valueBase, int valueLength, boolean parentNA);

    @Specialization(limit = "getGenericVectorAccessCacheSize()")
    protected int doMissing(RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, int targetDimension,
                    Object[] positions, @SuppressWarnings("unused") RMissing position, int positionOffset, @SuppressWarnings("unused") int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA,
                    @Cached("createCountingProfile()") LoopConditionProfile profile,
                    @CachedLibrary("getPosition(positions)") AbstractContainerLibrary positionsLibrary) {
        int rightIndex = rightBase;
        profile.profileCounted(targetDimension);
        for (int positionValue = 0; profile.inject(positionValue < targetDimension); positionValue += 1) {
            rightIndex = applyInner(//
                            positionsLibrary, leftIter, leftAccess, leftBase, leftLength, targetDimensions,
                            positions, positionOffset, positionValue,
                            rightIter, rightAccess, right, rightLength, rightIndex, parentNA);
        }
        return rightIndex;
    }

    @Specialization(limit = "getGenericVectorAccessCacheSize()")
    protected int doLogicalPosition(RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, int targetDimension,
                    Object[] positions, RAbstractLogicalVector position, int positionOffset, int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA,
                    @Cached("create()") BranchProfile wasTrue,
                    @Cached("create()") AlwaysOnBranchProfile outOfBounds,
                    @Cached("createCountingProfile()") LoopConditionProfile profile,
                    @Cached("createBinaryProfile()") ConditionProfile incModProfile,
                    @CachedLibrary("position") VectorDataLibrary positionLibrary,
                    @CachedLibrary("getPosition(positions)") AbstractContainerLibrary positionsLibrary) {
        getPositionNACheck().enable(!params.skipNA && !positionLibrary.isComplete(position));

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
                boolean isNA = getPositionNACheck().check(positionValue);
                if (isNA || positionValue == RRuntime.LOGICAL_TRUE) {
                    wasTrue.enter();
                    if (outOfBounds.isVisited() && i >= targetDimension) {
                        isNA = true;
                    }
                    rightIndex = applyInner(//
                                    positionsLibrary, leftIter, leftAccess, leftBase, leftLength, targetDimensions,
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
    @Specialization(rewriteOn = SlowPathException.class, guards = "position.isSequence()", limit = "getGenericVectorAccessCacheSize()")
    protected int doIntegerSequencePosition(RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, @SuppressWarnings("unused") int targetDimension,
                    Object[] positions, RIntVector position, int positionOffset, int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA,
                    @Cached("create()") IntValueProfile startProfile,
                    @Cached("create()") IntValueProfile strideProfile,
                    @Cached("createBinaryProfile()") ConditionProfile conditionProfile,
                    @Cached("createCountingProfile()") LoopConditionProfile profile,
                    @CachedLibrary("getPosition(positions)") AbstractContainerLibrary positionsLibrary) throws SlowPathException {
        // skip NA check. sequences never contain NA values.
        int rightIndex = rightBase;
        RIntSeqVectorData seq = (RIntSeqVectorData) position.getData();
        int start = startProfile.profile(seq.getStart() - 1);
        int stride = strideProfile.profile(seq.getStride());
        int end = start + positionLength * stride;

        if (start < 0 || end <= 0) {
            throw new SlowPathException("rewrite to doIntegerPosition");
        }

        boolean ascending = conditionProfile.profile(start < end);
        profile.profileCounted(positionLength);
        for (int positionValue = start; profile.inject(ascending ? positionValue < end : positionValue > end); positionValue += stride) {
            rightIndex = applyInner(//
                            positionsLibrary, leftIter, leftAccess, leftBase, leftLength, targetDimensions,
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
    @Specialization(replaces = "doIntegerSequencePosition", limit = "getGenericVectorAccessCacheSize()")
    protected int doIntegerPosition(RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions, @SuppressWarnings("unused") int targetDimension,
                    Object[] positions, RIntVector position, int positionOffset, int positionLength,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightBase, int rightLength, boolean parentNA,
                    @Cached("createCountingProfile()") LoopConditionProfile lengthProfile,
                    // @CachedLibrary("position") AbstractContainerLibrary positionLibrary,
                    @CachedLibrary("position.getData()") VectorDataLibrary positionLibrary,
                    @CachedLibrary("getPosition(positions)") AbstractContainerLibrary positionsLibrary) {
        getPositionNACheck().enable(positionLibrary, position);
        int rightIndex = rightBase;

        lengthProfile.profileCounted(positionLength);
        for (int i = 0; lengthProfile.inject(i < positionLength); i++) {
            int positionValue = positionLibrary.getIntAt(position.getData(), i);
            boolean isNA = getPositionNACheck().check(positionValue);
            if (isNA) {
                if (params.skipNA) {
                    continue;
                }
            }
            rightIndex = applyInner(//
                            positionsLibrary, leftIter, leftAccess, leftBase, leftLength, targetDimensions,
                            positions, positionOffset, positionValue - 1,
                            rightIter, rightAccess, right, rightLength, rightIndex, isNA || parentNA);
        }
        return rightIndex;
    }

    private int applyInner(//
                    AbstractContainerLibrary positionsLibrary, RandomIterator leftIter, VectorAccess leftAccess, int leftBase, int leftLength, Object targetDimensions,
                    Object[] positions, int positionOffset, int positionValue,
                    RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int rightLength, int actionIndex, boolean isNA) {
        int newTargetIndex = leftBase + positionValue * positionOffset;
        if (dimensionIndex == 0) {
            // for-loops leaf for innermost dimension

            // if position indexes value we just need to switch indices
            int actionLeftIndex;
            int actionRightIndex;
            if (!params.isReplace) {
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
                if (params.vectorType == RType.List || params.vectorType == RType.Expression) {
                    setListElement(leftIter, leftAccess, rightIter, rightAccess, right, actionLeftIndex, actionRightIndex);
                } else {
                    leftAccess.setFromSameType(leftIter, actionLeftIndex, rightAccess, rightIter, actionRightIndex);
                }
                rightAccess.isNA(rightIter, actionRightIndex);
            }

            if (getResetIndexProfile().profile((actionIndex + 1) == (!params.isReplace ? leftLength : rightLength))) {
                return 0;
            }
            return actionIndex + 1;
        } else {
            // generate another for-loop for other dimensions
            int nextTargetDimension = innerVectorNode.getDimensionValueProfile().profile(((int[]) targetDimensions)[innerVectorNode.dimensionIndex]);
            return innerVectorNode.applyImpl(//
                            positionsLibrary, leftIter, leftAccess, newTargetIndex, leftLength, targetDimensions, nextTargetDimension,
                            positions, positionOffset,
                            rightIter, rightAccess, right, actionIndex, rightLength, isNA);
        }

    }

    private void setListElement(RandomIterator leftIter, VectorAccess leftAccess, RandomIterator rightIter, VectorAccess rightAccess, RAbstractContainer right, int leftIndex, int rightIndex) {
        Object rightValue = rightAccess.getListElement(rightIter, rightIndex);
        if (params.isReplace) {
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

    private VectorLengthProfile getPositionOffsetProfile() {
        if (positionOffsetProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            positionOffsetProfile = VectorLengthProfile.create();
        }
        return positionOffsetProfile;
    }

    private NACheck getPositionNACheck() {
        if (positionNACheck == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            positionNACheck = NACheck.create();
        }
        return positionNACheck;
    }

    private VectorLengthProfile getDimensionValueProfile() {
        if (dimensionValueProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dimensionValueProfile = VectorLengthProfile.create();
        }
        return dimensionValueProfile;
    }

    private ConditionProfile getResetIndexProfile() {
        if (resetIndexProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            resetIndexProfile = ConditionProfile.createBinaryProfile();
        }
        return resetIndexProfile;
    }

    protected Object getPosition(Object[] positions) {

        return positions[dimensionIndex > 0 ? dimensionIndex - 1 : 0];
    }
}
