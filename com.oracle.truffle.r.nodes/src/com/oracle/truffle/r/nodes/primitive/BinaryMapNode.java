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
package com.oracle.truffle.r.nodes.primitive;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.WarningInfo;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.attributes.CopyAttributesNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.CopyAttributesNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.HasFixedAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

final class BinaryMapScalarNode extends BinaryMapNode {

    @Child private VectorDataLibrary leftLib;
    @Child private VectorDataLibrary rightLib;

    BinaryMapScalarNode(BinaryMapFunctionNode function, RAbstractVector left, RAbstractVector right, RType argumentType, RType resultType) {
        super(function, left, right, argumentType, resultType);
        this.leftLib = VectorDataLibrary.getFactory().create(left.getData());
        this.rightLib = VectorDataLibrary.getFactory().create(right.getData());
    }

    @Override
    public boolean isSupported(RAbstractVector left, RAbstractVector right) {
        return leftLib.accepts(left.getData()) && rightLib.accepts(right.getData());
    }

    @Override
    public Object apply(RAbstractVector originalLeft, RAbstractVector originalRight) {
        assert isSupported(originalLeft, originalRight);
        RAbstractVector left = leftClass.cast(originalLeft);
        RAbstractVector right = rightClass.cast(originalRight);

        assert left != null;
        assert right != null;
        function.enable(left, right);
        Object leftData = left.getData();
        Object rightData = right.getData();
        assert leftLib.getLength(leftData) == 1;
        assert rightLib.getLength(rightData) == 1;

        switch (argumentType) {
            case Raw:
                byte leftValueRaw = leftLib.getRawAt(leftData, 0);
                byte rightValueRaw = rightLib.getRawAt(rightData, 0);
                switch (resultType) {
                    case Raw:
                        return RRaw.valueOf(function.applyRaw(leftValueRaw, rightValueRaw));
                    case Logical:
                        return function.applyLogical(RRuntime.raw2int(leftValueRaw), RRuntime.raw2int(rightValueRaw));
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Logical:
                byte leftValueLogical = leftLib.getLogicalAt(leftData, 0);
                byte rightValueLogical = rightLib.getLogicalAt(rightData, 0);
                return function.applyLogical(leftValueLogical, rightValueLogical);
            case Integer:
                int leftValueInt = leftLib.getIntAt(leftData, 0);
                int rightValueInt = rightLib.getIntAt(rightData, 0);
                switch (resultType) {
                    case Logical:
                        return function.applyLogical(leftValueInt, rightValueInt);
                    case Integer:
                        return function.applyInteger(leftValueInt, rightValueInt);
                    case Double:
                        return function.applyDouble(leftValueInt, rightValueInt);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Double:
                double leftValueDouble = leftLib.getDoubleAt(leftData, 0);
                double rightValueDouble = rightLib.getDoubleAt(rightData, 0);
                switch (resultType) {
                    case Logical:
                        return function.applyLogical(leftValueDouble, rightValueDouble);
                    case Double:
                        return function.applyDouble(leftValueDouble, rightValueDouble);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Complex:
                RComplex leftValueComplex = leftLib.getComplexAt(leftData, 0);
                RComplex rightValueComplex = rightLib.getComplexAt(rightData, 0);
                switch (resultType) {
                    case Logical:
                        return function.applyLogical(leftValueComplex, rightValueComplex);
                    case Complex:
                        return function.applyComplex(leftValueComplex, rightValueComplex);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Character:
                String leftValueString = leftLib.getStringAt(leftData, 0);
                String rightValueString = rightLib.getStringAt(rightData, 0);
                switch (resultType) {
                    case Logical:
                        return function.applyLogical(leftValueString, rightValueString);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }
}

final class BinaryMapVectorNode extends BinaryMapNode {

    @Child private VectorMapBinaryInternalNode vectorNode;
    @Child private CopyAttributesNode copyAttributes;
    @Child private GetDimAttributeNode getLeftDimNode = GetDimAttributeNode.create();
    @Child private GetDimAttributeNode getRightDimNode = GetDimAttributeNode.create();
    @Child private HasFixedAttributeNode hasLeftDimNode = HasFixedAttributeNode.createDim();
    @Child private HasFixedAttributeNode hasRightDimNode = HasFixedAttributeNode.createDim();

    @Child private VectorDataLibrary leftLibrary;
    @Child private VectorDataLibrary rightLibrary;
    @Child private VectorDataLibrary resultLibrary;

    // profiles
    private final VectorLengthProfile leftLengthProfile;
    private final VectorLengthProfile rightLengthProfile;
    private final ConditionProfile dimensionsProfile;
    private final ConditionProfile maxLengthProfile;
    private final ConditionProfile seenEmpty;
    private final ConditionProfile shareLeft;
    private final ConditionProfile shareRight;
    private final BranchProfile hasWarningsBranchProfile;

    // compile-time optimization flags
    private final boolean mayContainMetadata;
    private final boolean mayFoldConstantTime;
    private final boolean mayShareLeft;
    private final boolean mayShareRight;

    BinaryMapVectorNode(BinaryMapFunctionNode function, RAbstractVector left, RAbstractVector right, RType argumentType, RType resultType, boolean copyAttributes, boolean isGeneric) {
        super(function, left, right, argumentType, resultType);
        this.leftLengthProfile = VectorLengthProfile.create();
        this.rightLengthProfile = VectorLengthProfile.create();
        this.seenEmpty = ConditionProfile.createBinaryProfile();

        this.vectorNode = VectorMapBinaryInternalNode.create(resultType, argumentType);
        boolean leftVectorImpl = left.isMaterialized();
        boolean rightVectorImpl = right.isMaterialized();
        this.mayContainMetadata = leftVectorImpl || rightVectorImpl;
        this.mayFoldConstantTime = function.mayFoldConstantTime(left, right);
        this.mayShareLeft = left.getRType() == resultType && leftVectorImpl;
        this.mayShareRight = right.getRType() == resultType && rightVectorImpl;
        // lazily create profiles only if needed to avoid unnecessary allocations
        this.shareLeft = mayShareLeft ? ConditionProfile.createBinaryProfile() : null;
        this.shareRight = mayShareRight ? ConditionProfile.createBinaryProfile() : null;
        this.dimensionsProfile = mayContainMetadata ? ConditionProfile.createBinaryProfile() : null;

        this.hasWarningsBranchProfile = BranchProfile.create();

        this.copyAttributes = mayContainMetadata ? CopyAttributesNodeGen.create(copyAttributes) : null;
        this.maxLengthProfile = ConditionProfile.createBinaryProfile();
        if (isGeneric) {
            leftLibrary = VectorDataLibrary.getFactory().getUncached();
            rightLibrary = VectorDataLibrary.getFactory().getUncached();
        } else {
            leftLibrary = VectorDataLibrary.getFactory().create(left.getData());
            rightLibrary = VectorDataLibrary.getFactory().create(right.getData());
        }
    }

    @Override
    public boolean isSupported(RAbstractVector left, RAbstractVector right) {
        return leftLibrary.accepts(left.getData()) && rightLibrary.accepts(right.getData()) && getDataClass(left) == leftDataClass && getDataClass(right) == rightDataClass;
    }

    @Override
    public Object apply(RAbstractVector originalLeft, RAbstractVector originalRight) {
        assert isSupported(originalLeft, originalRight);
        RAbstractVector left = leftClass.cast(originalLeft);
        RAbstractVector right = rightClass.cast(originalRight);

        WarningInfo warningInfo = null;

        function.initialize(leftLibrary, left, rightLibrary, right);

        Object leftData = left.getData();
        Object rightData = right.getData();

        if (mayContainMetadata && (dimensionsProfile.profile(hasLeftDimNode.execute(left) && hasRightDimNode.execute(right)))) {
            if (differentDimensions(left, right)) {
                throw error(RError.Message.NON_CONFORMABLE_ARRAYS);
            }
        }

        RAbstractVector target = null;
        int leftLength = leftLengthProfile.profile(leftLibrary.getLength(leftData));
        int rightLength = rightLengthProfile.profile(rightLibrary.getLength(rightData));
        if (seenEmpty.profile(leftLength == 0 || rightLength == 0)) {
            /*
             * It is safe to skip attribute handling here as they are never copied if length is 0 of
             * either side. Note that dimension check still needs to be performed.
             */
            return resultType.getEmpty();
        }
        if (mayFoldConstantTime && function.mayFoldConstantTime(left, right)) {
            function.enable(left, right);
            warningInfo = new WarningInfo();
            Object leftDataCast = leftLibrary.cast(leftData, argumentType);
            Object rightDataCast = rightLibrary.cast(rightData, argumentType);
            target = function.tryFoldConstantTime(warningInfo, leftDataCast, leftLength, rightDataCast, rightLength);
        }
        if (target == null) {
            int maxLength = maxLengthProfile.profile(leftLength >= rightLength) ? leftLength : rightLength;

            assert left.getLength() == leftLength;
            assert right.getLength() == rightLength;
            SeqIterator leftIter = leftLibrary.iterator(leftData);
            SeqIterator rightIter = rightLibrary.iterator(rightData);
            if (mayShareLeft && left.getRType() == resultType && shareLeft.profile(leftLength == maxLength && ((RSharingAttributeStorage) left).isTemporary())) {
                target = left;
                try (SeqWriteIterator resultIter = leftLibrary.writeIterator(leftData)) {
                    warningInfo = resultIter.getWarningInfo();
                    vectorNode.execute(function, leftLength, rightLength, leftData, leftLibrary, resultIter, leftData, leftLibrary, leftIter, rightData, rightLibrary, rightIter);
                    leftLibrary.commitWriteIterator(leftData, resultIter, function.isComplete());
                }
            } else if (mayShareRight && right.getRType() == resultType && shareRight.profile(rightLength == maxLength && ((RSharingAttributeStorage) right).isTemporary())) {
                target = right;
                try (SeqWriteIterator resultIter = rightLibrary.writeIterator(rightData)) {
                    warningInfo = resultIter.getWarningInfo();
                    vectorNode.execute(function, leftLength, rightLength, rightData, rightLibrary, resultIter, leftData, leftLibrary, leftIter, rightData, rightLibrary, rightIter);
                    rightLibrary.commitWriteIterator(rightData, resultIter, function.isComplete());
                }
            } else {
                target = resultType.create(maxLength, false);
                Object targetData = target.getData();
                try (SeqWriteIterator resultIter = getResultLibrary().writeIterator(targetData)) {
                    warningInfo = resultIter.getWarningInfo();
                    vectorNode.execute(function, leftLength, rightLength, targetData, getResultLibrary(), resultIter, leftData, leftLibrary, leftIter, rightData, rightLibrary, rightIter);
                    getResultLibrary().commitWriteIterator(targetData, resultIter, function.isComplete());
                }
            }
            RBaseNode.reportWork(this, maxLength);
        }
        if (mayContainMetadata) {
            target = copyAttributes.execute(target, left, leftLength, right, rightLength);
        }

        assert warningInfo != null;
        if (warningInfo.hasIntergerOverflow()) {
            hasWarningsBranchProfile.enter();
            RError.warning(this, RError.Message.INTEGER_OVERFLOW);
        }

        assert RAbstractVector.verifyVector(target);
        return target;
    }

    private VectorDataLibrary getResultLibrary() {
        if (resultLibrary == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            resultLibrary = insert(VectorDataLibrary.getFactory().createDispatched(DSLConfig.getGenericVectorAccessCacheSize()));
        }
        return resultLibrary;
    }

    private boolean differentDimensions(RAbstractVector left, RAbstractVector right) {
        int[] leftDimensions = getLeftDimNode.getDimensions(left);
        int[] rightDimensions = getRightDimNode.getDimensions(right);
        int leftLength = leftDimensions.length;
        int rightLength = rightDimensions.length;
        if (leftLength != rightLength) {
            return true;
        }
        if (leftLength > 0) {
            for (int i = 0; i < leftLength; i++) {
                if (leftDimensions[i] != rightDimensions[i]) {
                    return true;
                }
            }
        }
        return false;
    }
}

abstract class VectorMapBinaryInternalNode extends RBaseNode {

    private abstract static class MapBinaryIndexedAction {
        public abstract void perform(BinaryMapFunctionNode action, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter);
    }

    private static final MapBinaryIndexedAction LOGICAL_LOGICAL = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextLogical(resultData, resultIter,
                            arithmetic.applyLogical(leftLib.getNextLogical(leftData, leftIter), rightLib.getNextLogical(rightData, rightIter)));
        }
    };
    private static final MapBinaryIndexedAction LOGICAL_INTEGER = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextLogical(resultData, resultIter,
                            arithmetic.applyLogical(leftLib.getNextInt(leftData, leftIter), rightLib.getNextInt(rightData, rightIter)));
        }
    };
    private static final MapBinaryIndexedAction LOGICAL_DOUBLE = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextLogical(resultData, resultIter,
                            arithmetic.applyLogical(leftLib.getNextDouble(leftData, leftIter), rightLib.getNextDouble(rightData, rightIter)));
        }
    };
    private static final MapBinaryIndexedAction LOGICAL_COMPLEX = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextLogical(resultData, resultIter,
                            arithmetic.applyLogical(leftLib.getNextComplex(leftData, leftIter), rightLib.getNextComplex(rightData, rightIter)));
        }
    };
    private static final MapBinaryIndexedAction LOGICAL_CHARACTER = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextLogical(resultData, resultIter,
                            arithmetic.applyLogical(leftLib.getNextString(leftData, leftIter), rightLib.getNextString(rightData, rightIter)));
        }
    };
    private static final MapBinaryIndexedAction LOGICAL_RAW = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextLogical(resultData, resultIter,
                            arithmetic.applyLogical(RRuntime.raw2int(leftLib.getNextRaw(leftData, leftIter)), RRuntime.raw2int(rightLib.getNextRaw(rightData, rightIter))));
        }
    };
    private static final MapBinaryIndexedAction RAW_RAW = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextRaw(resultData, resultIter,
                            arithmetic.applyRaw(leftLib.getNextRaw(leftData, leftIter), rightLib.getNextRaw(rightData, rightIter)));
        }
    };
    private static final MapBinaryIndexedAction INTEGER_INTEGER = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextInt(resultData, resultIter,
                            arithmetic.applyInteger(resultIter.getWarningInfo(), leftLib.getNextInt(leftData, leftIter), rightLib.getNextInt(rightData, rightIter)));
        }
    };
    private static final MapBinaryIndexedAction DOUBLE_INTEGER = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextDouble(resultData, resultIter,
                            arithmetic.applyDouble(leftLib.getNextInt(leftData, leftIter), rightLib.getNextInt(rightData, rightIter)));
        }
    };
    private static final MapBinaryIndexedAction DOUBLE = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextDouble(resultData, resultIter,
                            arithmetic.applyDouble(leftLib.getNextDouble(leftData, leftIter), rightLib.getNextDouble(rightData, rightIter)));
        }
    };
    private static final MapBinaryIndexedAction COMPLEX = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            RComplex value = arithmetic.applyComplex(leftLib.getNextComplex(leftData, leftIter), rightLib.getNextComplex(rightData, rightIter));
            resultLib.setNextComplex(resultData, resultIter, value);
        }
    };
    private static final MapBinaryIndexedAction CHARACTER = new MapBinaryIndexedAction() {
        @Override
        public void perform(BinaryMapFunctionNode arithmetic, Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                        Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter, Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
            resultLib.setNextString(resultData, resultIter,
                            arithmetic.applyCharacter(leftLib.getNextString(leftData, leftIter), rightLib.getNextString(rightData, rightIter)));
        }
    };

    private final MapBinaryIndexedAction indexedAction;

    protected VectorMapBinaryInternalNode(RType resultType, RType argumentType) {
        this.indexedAction = createIndexedAction(resultType, argumentType);
    }

    public static VectorMapBinaryInternalNode create(RType resultType, RType argumentType) {
        return VectorMapBinaryInternalNodeGen.create(resultType, argumentType);
    }

    private static MapBinaryIndexedAction createIndexedAction(RType resultType, RType argumentType) {
        switch (resultType) {
            case Raw:
                assert argumentType == RType.Raw;
                return RAW_RAW;
            case Logical:
                switch (argumentType) {
                    case Raw:
                        return LOGICAL_RAW;
                    case Logical:
                        return LOGICAL_LOGICAL;
                    case Integer:
                        return LOGICAL_INTEGER;
                    case Double:
                        return LOGICAL_DOUBLE;
                    case Complex:
                        return LOGICAL_COMPLEX;
                    case Character:
                        return LOGICAL_CHARACTER;
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Integer:
                assert argumentType == RType.Integer;
                return INTEGER_INTEGER;
            case Double:
                switch (argumentType) {
                    case Integer:
                        return DOUBLE_INTEGER;
                    case Double:
                        return DOUBLE;
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Complex:
                assert argumentType == RType.Complex;
                return COMPLEX;
            case Character:
                assert argumentType == RType.Character;
                return CHARACTER;
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    public abstract void execute(BinaryMapFunctionNode node, int leftLength, int rightLength,
                    Object resultData, VectorDataLibrary result, SeqWriteIterator resultIter,
                    Object leftData, VectorDataLibrary left, SeqIterator leftIter,
                    Object rightData, VectorDataLibrary right, SeqIterator rightIter);

    @Specialization(guards = {"leftLength == 1", "rightLength == 1"})
    protected void doScalarScalar(BinaryMapFunctionNode node, @SuppressWarnings("unused") int leftLength, @SuppressWarnings("unused") int rightLength,
                    Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                    Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter,
                    Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
        leftLib.next(leftData, leftIter);
        rightLib.next(rightData, rightIter);
        resultLib.next(resultData, resultIter);
        indexedAction.perform(node, resultData, resultLib, resultIter, leftData, leftLib, leftIter, rightData, rightLib, rightIter);
    }

    @Specialization(replaces = "doScalarScalar", guards = {"leftLength == 1"})
    protected void doScalarVector(BinaryMapFunctionNode node, @SuppressWarnings("unused") int leftLength, @SuppressWarnings("unused") int rightLength,
                    Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                    Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter,
                    Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
        leftLib.next(leftData, leftIter);
        while (rightLib.nextLoopCondition(rightData, rightIter)) {
            resultLib.next(resultData, resultIter);
            indexedAction.perform(node, resultData, resultLib, resultIter, leftData, leftLib, leftIter, rightData, rightLib, rightIter);
        }
    }

    @Specialization(replaces = "doScalarScalar", guards = {"rightLength == 1"})
    protected void doVectorScalar(BinaryMapFunctionNode node, @SuppressWarnings("unused") int leftLength, @SuppressWarnings("unused") int rightLength,
                    Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                    Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter,
                    Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
        rightLib.next(rightData, rightIter);
        while (leftLib.nextLoopCondition(leftData, leftIter)) {
            resultLib.next(resultData, resultIter);
            indexedAction.perform(node, resultData, resultLib, resultIter, leftData, leftLib, leftIter, rightData, rightLib, rightIter);
        }
    }

    @Specialization(guards = {"leftLength == rightLength"})
    protected void doSameLength(BinaryMapFunctionNode node, @SuppressWarnings("unused") int leftLength, @SuppressWarnings("unused") int rightLength,
                    Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                    Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter,
                    Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter) {
        while (leftLib.nextLoopCondition(leftData, leftIter)) {
            rightLib.next(rightData, rightIter);
            resultLib.next(resultData, resultIter);
            indexedAction.perform(node, resultData, resultLib, resultIter, leftData, leftLib, leftIter, rightData, rightLib, rightIter);
        }
    }

    @Specialization(guards = {"leftLength > rightLength"})
    protected void doMultiplesLeft(BinaryMapFunctionNode node, int leftLength, int rightLength,
                    Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                    Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter,
                    Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter,
                    @Cached("createCountingProfile()") LoopConditionProfile leftProfile,
                    @Cached("createBinaryProfile()") ConditionProfile smallRemainderProfile) {
        assert resultLib.getLength(resultData) == leftLength;
        // This specialization no longer handles leftLength == rightLength
        // (leftLength > rightLength now forces doSameLength() use)
        // because result == right would be possible and it would have to be checked
        // in each subsequent if (result != left) { result.next(resultData, resultIter); }
        assert (resultData != rightData);
        leftProfile.profileCounted(leftLength);
        while (leftProfile.inject(leftIter.getIndex() + 1 < leftLength)) {
            rightIter.reset();
            if (smallRemainderProfile.profile((leftLength - leftIter.getIndex() - 1) >= rightLength)) {
                // we need at least rightLength more elements
                while (rightLib.nextLoopCondition(rightData, rightIter) && leftLib.nextLoopCondition(leftData, leftIter)) {
                    resultLib.next(resultData, resultIter);
                    indexedAction.perform(node, resultData, resultLib, resultIter, leftData, leftLib, leftIter, rightData, rightLib, rightIter);
                }
            } else {
                while (rightLib.nextLoopCondition(rightData, rightIter) && leftLib.nextLoopCondition(leftData, leftIter)) {
                    resultLib.next(resultData, resultIter);
                    indexedAction.perform(node, resultData, resultLib, resultIter, leftData, leftLib, leftIter, rightData, rightLib, rightIter);
                }
                RError.warning(this, RError.Message.LENGTH_NOT_MULTI);
            }
        }
    }

    @Specialization(guards = {"rightLength > leftLength"})
    protected void doMultiplesRight(BinaryMapFunctionNode node, int leftLength, int rightLength,
                    Object resultData, VectorDataLibrary resultLib, SeqWriteIterator resultIter,
                    Object leftData, VectorDataLibrary leftLib, SeqIterator leftIter,
                    Object rightData, VectorDataLibrary rightLib, SeqIterator rightIter,
                    @Cached("createCountingProfile()") LoopConditionProfile rightProfile,
                    @Cached("createBinaryProfile()") ConditionProfile smallRemainderProfile) {
        assert resultLib.getLength(resultData) == rightLength;
        assert resultData != leftData;
        rightProfile.profileCounted(rightLength);
        while (rightProfile.inject(rightIter.getIndex() + 1 < rightLength)) {
            leftIter.reset();
            if (smallRemainderProfile.profile((rightLength - rightIter.getIndex() - 1) >= leftLength)) {
                // we need at least leftLength more elements
                while (leftLib.nextLoopCondition(leftData, leftIter) && rightLib.nextLoopCondition(rightData, rightIter)) {
                    resultLib.next(resultData, resultIter);
                    indexedAction.perform(node, resultData, resultLib, resultIter, leftData, leftLib, leftIter, rightData, rightLib, rightIter);
                }
            } else {
                while (leftLib.nextLoopCondition(leftData, leftIter) && rightLib.nextLoopCondition(rightData, rightIter)) {
                    resultLib.next(resultData, resultIter);
                    indexedAction.perform(node, resultData, resultLib, resultIter, leftData, leftLib, leftIter, rightData, rightLib, rightIter);
                }
                RError.warning(this, RError.Message.LENGTH_NOT_MULTI);
            }
        }
    }
}

/**
 * Implements a binary map operation that maps two vectors into a single result vector of the
 * maximum size of both vectors. Vectors with smaller length are repeated. The actual implementation
 * is provided using a {@link BinaryMapFunctionNode}.
 * <p>
 * The implementation tries to share input vectors if they are implementing
 * {@link RSharingAttributeStorage}.
 */
public abstract class BinaryMapNode extends RBaseNode {

    @Child protected BinaryMapFunctionNode function;
    protected final Class<? extends RAbstractVector> leftClass;
    protected final Class<? extends RAbstractVector> rightClass;
    protected final Class<?> leftDataClass;
    protected final Class<?> rightDataClass;
    protected final RType argumentType;
    protected final RType resultType;

    protected BinaryMapNode(BinaryMapFunctionNode function, RAbstractVector left, RAbstractVector right, RType argumentType, RType resultType) {
        this.function = function;
        this.leftClass = left.getClass();
        this.rightClass = right.getClass();
        this.leftDataClass = getDataClass(left);
        this.rightDataClass = getDataClass(right);
        this.argumentType = argumentType;
        this.resultType = resultType;
    }

    public static BinaryMapNode create(BinaryMapFunctionNode function, RAbstractVector left, RAbstractVector right, RType argumentType, RType resultType, boolean copyAttributes, boolean isGeneric) {
        if (left instanceof RScalarVector && right instanceof RScalarVector) {
            return new BinaryMapScalarNode(function, left, right, argumentType, resultType);
        } else {
            return new BinaryMapVectorNode(function, left, right, argumentType, resultType, copyAttributes, isGeneric);
        }
    }

    public abstract boolean isSupported(RAbstractVector left, RAbstractVector right);

    public abstract Object apply(RAbstractVector originalLeft, RAbstractVector originalRight);

    protected static Class<?> getDataClass(RAbstractVector vec) {
        return vec.getData().getClass();
    }

}
