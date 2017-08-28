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
package com.oracle.truffle.r.nodes.primitive;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.CopyAttributesNode;
import com.oracle.truffle.r.nodes.attributes.CopyAttributesNodeGen;
import com.oracle.truffle.r.nodes.attributes.HasFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.primitive.BinaryMapNodeFactory.VectorMapBinaryInternalNodeGen;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.GetDataStore;
import com.oracle.truffle.r.runtime.data.nodes.SetDataAt;
import com.oracle.truffle.r.runtime.data.nodes.VectorIterator;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Implements a binary map operation that maps two vectors into a single result vector of the
 * maximum size of both vectors. Vectors with smaller length are repeated. The actual implementation
 * is provided using a {@link BinaryMapFunctionNode}.
 *
 * The implementation tries to share input vectors if they are implementing {@link RShareable}.
 */
public final class BinaryMapNode extends RBaseNode {

    @Child private VectorMapBinaryInternalNode vectorNode;
    @Child private BinaryMapFunctionNode function;
    @Child private CopyAttributesNode copyAttributes;
    @Child private GetDimAttributeNode getLeftDimNode = GetDimAttributeNode.create();
    @Child private GetDimAttributeNode getRightDimNode = GetDimAttributeNode.create();
    @Child private HasFixedAttributeNode hasLeftDimNode = HasFixedAttributeNode.createDim();
    @Child private HasFixedAttributeNode hasRightDimNode = HasFixedAttributeNode.createDim();

    // profiles
    private final Class<? extends RAbstractVector> leftClass;
    private final Class<? extends RAbstractVector> rightClass;
    private final VectorLengthProfile leftLengthProfile = VectorLengthProfile.create();
    private final VectorLengthProfile rightLengthProfile = VectorLengthProfile.create();
    private final ConditionProfile dimensionsProfile;
    private final ConditionProfile maxLengthProfile;
    private final ConditionProfile leftIsNAProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile rightIsNAProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile seenEmpty = ConditionProfile.createBinaryProfile();
    private final ConditionProfile shareLeft;
    private final ConditionProfile shareRight;
    private final RType argumentType;
    private final RType resultType;

    // compile-time optimization flags
    private final boolean scalarTypes;
    private final boolean mayContainMetadata;
    private final boolean mayFoldConstantTime;
    private final boolean mayShareLeft;
    private final boolean mayShareRight;

    private BinaryMapNode(BinaryMapFunctionNode function, RAbstractVector left, RAbstractVector right, RType argumentType, RType resultType, boolean copyAttributes) {
        this.function = function;
        this.leftClass = left.getClass();
        this.rightClass = right.getClass();
        this.vectorNode = VectorMapBinaryInternalNode.create(resultType, argumentType);
        this.scalarTypes = left instanceof RScalarVector && right instanceof RScalarVector;
        boolean leftVectorImpl = RVector.class.isAssignableFrom(leftClass);
        boolean rightVectorImpl = RVector.class.isAssignableFrom(rightClass);
        this.mayContainMetadata = leftVectorImpl || rightVectorImpl;
        this.mayFoldConstantTime = function.mayFoldConstantTime(leftClass, rightClass);
        this.mayShareLeft = left.getRType() == resultType && leftVectorImpl;
        this.mayShareRight = right.getRType() == resultType && rightVectorImpl;
        this.argumentType = argumentType;
        this.resultType = resultType;
        this.maxLengthProfile = ConditionProfile.createBinaryProfile();

        // lazily create profiles only if needed to avoid unnecessary allocations
        this.shareLeft = mayShareLeft ? ConditionProfile.createBinaryProfile() : null;
        this.shareRight = mayShareRight ? ConditionProfile.createBinaryProfile() : null;
        this.dimensionsProfile = mayContainMetadata ? ConditionProfile.createBinaryProfile() : null;

        this.copyAttributes = mayContainMetadata ? CopyAttributesNodeGen.create(copyAttributes) : null;
    }

    public static BinaryMapNode create(BinaryMapFunctionNode function, RAbstractVector left, RAbstractVector right, RType argumentType, RType resultType, boolean copyAttributes) {
        return new BinaryMapNode(function, left, right, argumentType, resultType, copyAttributes);
    }

    public boolean isSupported(Object left, Object right) {
        return left.getClass() == leftClass && right.getClass() == rightClass;
    }

    public Object apply(Object originalLeft, Object originalRight) {
        assert isSupported(originalLeft, originalRight);
        RAbstractVector left = leftClass.cast(originalLeft);
        RAbstractVector right = rightClass.cast(originalRight);

        RAbstractVector leftCast = left.castSafe(argumentType, leftIsNAProfile, false);
        RAbstractVector rightCast = right.castSafe(argumentType, rightIsNAProfile, false);

        assert leftCast != null;
        assert rightCast != null;

        function.enable(leftCast, rightCast);

        if (scalarTypes) {
            assert left.getLength() == 1;
            assert right.getLength() == 1;
            return applyScalar(leftCast, rightCast);
        } else {
            int leftLength = leftLengthProfile.profile(left.getLength());
            int rightLength = rightLengthProfile.profile(right.getLength());
            return applyVectorized(left, leftCast, leftLength, right, rightCast, rightLength);
        }
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

    private Object applyScalar(RAbstractVector left, RAbstractVector right) {
        switch (argumentType) {
            case Raw:
                byte leftValueRaw = ((RAbstractRawVector) left).getRawDataAt(0);
                byte rightValueRaw = ((RAbstractRawVector) right).getRawDataAt(0);
                switch (resultType) {
                    case Raw:
                        return RRaw.valueOf(function.applyRaw(leftValueRaw, rightValueRaw));
                    case Logical:
                        return function.applyLogical(RRuntime.raw2int(leftValueRaw), RRuntime.raw2int(rightValueRaw));
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Logical:
                byte leftValueLogical = ((RAbstractLogicalVector) left).getDataAt(0);
                byte rightValueLogical = ((RAbstractLogicalVector) right).getDataAt(0);
                return function.applyLogical(leftValueLogical, rightValueLogical);
            case Integer:
                int leftValueInt = ((RAbstractIntVector) left).getDataAt(0);
                int rightValueInt = ((RAbstractIntVector) right).getDataAt(0);
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
                double leftValueDouble = ((RAbstractDoubleVector) left).getDataAt(0);
                double rightValueDouble = ((RAbstractDoubleVector) right).getDataAt(0);
                switch (resultType) {
                    case Logical:
                        return function.applyLogical(leftValueDouble, rightValueDouble);
                    case Double:
                        return function.applyDouble(leftValueDouble, rightValueDouble);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Complex:
                RComplex leftValueComplex = ((RAbstractComplexVector) left).getDataAt(0);
                RComplex rightValueComplex = ((RAbstractComplexVector) right).getDataAt(0);
                switch (resultType) {
                    case Logical:
                        return function.applyLogical(leftValueComplex, rightValueComplex);
                    case Complex:
                        return function.applyComplex(leftValueComplex, rightValueComplex);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Character:
                String leftValueString = ((RAbstractStringVector) left).getDataAt(0);
                String rightValueString = ((RAbstractStringVector) right).getDataAt(0);
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

    private Object applyVectorized(RAbstractVector left, RAbstractVector leftCast, int leftLength, RAbstractVector right, RAbstractVector rightCast, int rightLength) {
        if (mayContainMetadata && (dimensionsProfile.profile(hasLeftDimNode.execute(left) && hasRightDimNode.execute(right)))) {
            if (differentDimensions(left, right)) {
                throw error(RError.Message.NON_CONFORMABLE_ARRAYS);
            }
        }

        if (seenEmpty.profile(leftLength == 0 || rightLength == 0)) {
            /*
             * It is safe to skip attribute handling here as they are never copied if length is 0 of
             * either side. Note that dimension check still needs to be performed.
             */
            return resultType.getEmpty();
        }

        RAbstractVector target = null;
        if (mayFoldConstantTime) {
            target = function.tryFoldConstantTime(leftCast, leftLength, rightCast, rightLength);
        }
        if (target == null) {
            int maxLength = maxLengthProfile.profile(leftLength >= rightLength) ? leftLength : rightLength;
            RVector<?> targetVec = createOrShareVector(leftLength, left, rightLength, right, maxLength);
            target = targetVec;

            assert left.getLength() == leftLength;
            assert right.getLength() == rightLength;
            assert leftCast.getRType() == argumentType;
            assert rightCast.getRType() == argumentType;

            vectorNode.execute(function, targetVec, leftCast, leftLength, rightCast, rightLength);
            RBaseNode.reportWork(this, maxLength);
            target.setComplete(function.isComplete());
        }
        if (mayContainMetadata) {
            target = copyAttributes.execute(target, left, leftLength, right, rightLength);
        }
        return target;
    }

    private RVector<?> createOrShareVector(int leftLength, RAbstractVector left, int rightLength, RAbstractVector right, int maxLength) {
        if (mayShareLeft && left.getRType() == resultType && shareLeft.profile(leftLength == maxLength && ((RShareable) left).isTemporary()) && left instanceof RVector<?>) {
            return (RVector<?>) left;
        }
        if (mayShareRight && right.getRType() == resultType && shareRight.profile(rightLength == maxLength && ((RShareable) right).isTemporary()) && right instanceof RVector<?>) {
            return (RVector<?>) right;
        }
        return resultType.create(maxLength, false);
    }

    @ImportStatic(Utils.class)
    protected abstract static class VectorMapBinaryInternalNode extends RBaseNode {

        private static final MapBinaryIndexedAction<Byte> LOGICAL_LOGICAL = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyLogical(leftVal, rightVal);
        private static final MapBinaryIndexedAction<Integer> LOGICAL_INTEGER = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyLogical(leftVal, rightVal);
        private static final MapBinaryIndexedAction<Double> LOGICAL_DOUBLE = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyLogical(leftVal, rightVal);
        private static final MapBinaryIndexedAction<RComplex> LOGICAL_COMPLEX = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyLogical(leftVal, rightVal);
        private static final MapBinaryIndexedAction<String> LOGICAL_CHARACTER = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyLogical(leftVal, rightVal);
        private static final MapBinaryIndexedAction<Byte> LOGICAL_RAW = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyLogical(RRuntime.raw2int(leftVal), RRuntime.raw2int(rightVal));
        private static final MapBinaryIndexedAction<Byte> RAW_RAW = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyRaw(leftVal, rightVal);
        private static final MapBinaryIndexedAction<Integer> INTEGER_INTEGER = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyInteger(leftVal, rightVal);
        private static final MapBinaryIndexedAction<Integer> DOUBLE_INTEGER = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyDouble(leftVal, rightVal);
        private static final MapBinaryIndexedAction<Double> DOUBLE = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyDouble(leftVal, rightVal);
        private static final MapBinaryIndexedAction<RComplex> COMPLEX = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyComplex(leftVal, rightVal);
        private static final MapBinaryIndexedAction<String> CHARACTER = //
                        (arithmetic, leftVal, rightVal) -> arithmetic.applyCharacter(leftVal, rightVal);

        private final MapBinaryIndexedAction<Object> indexedAction;

        @Child private GetDataStore getTargetDataStore = GetDataStore.create();
        @Child private SetDataAt targetSetDataAt;

        @SuppressWarnings("unchecked")
        protected VectorMapBinaryInternalNode(RType resultType, RType argumentType) {
            this.indexedAction = (MapBinaryIndexedAction<Object>) createIndexedAction(resultType, argumentType);
            this.targetSetDataAt = Utils.createSetDataAtNode(resultType);
        }

        public static VectorMapBinaryInternalNode create(RType resultType, RType argumentType) {
            return VectorMapBinaryInternalNodeGen.create(resultType, argumentType);
        }

        private static MapBinaryIndexedAction<?> createIndexedAction(RType resultType, RType argumentType) {
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

        public abstract void execute(BinaryMapFunctionNode node, RVector<?> store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength);

        @Specialization(guards = {"leftLength == 1", "rightLength == 1"})
        @SuppressWarnings("unused")
        protected void doScalarScalar(BinaryMapFunctionNode node, RVector<?> result, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength,
                        @Cached("createIterator()") VectorIterator.Generic leftIterator,
                        @Cached("createIterator()") VectorIterator.Generic rightIterator) {
            Object itLeft = leftIterator.init(left);
            Object itRight = rightIterator.init(right);
            Object value = indexedAction.perform(node, leftIterator.next(left, itLeft), rightIterator.next(right, itRight));
            targetSetDataAt.setDataAtAsObject(result, getTargetDataStore.execute(result), 0, value);
        }

        @Specialization(replaces = "doScalarScalar", guards = {"leftLength == 1"})
        @SuppressWarnings("unused")
        protected void doScalarVector(BinaryMapFunctionNode node, RVector<?> result, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength,
                        @Cached("createIterator()") VectorIterator.Generic leftIterator,
                        @Cached("createIterator()") VectorIterator.Generic rightIterator,
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            profile.profileCounted(rightLength);
            Object itLeft = leftIterator.init(left);
            Object itRight = rightIterator.init(right);
            Object resultStore = getTargetDataStore.execute(result);
            Object leftValue = leftIterator.next(left, itLeft);
            for (int i = 0; profile.inject(i < rightLength); ++i) {
                Object value = indexedAction.perform(node, leftValue, rightIterator.next(right, itRight));
                targetSetDataAt.setDataAtAsObject(result, resultStore, i, value);
            }
        }

        @Specialization(replaces = "doScalarScalar", guards = {"rightLength == 1"})
        @SuppressWarnings("unused")
        protected void doVectorScalar(BinaryMapFunctionNode node, RVector<?> result, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength,
                        @Cached("createIterator()") VectorIterator.Generic leftIterator,
                        @Cached("createIterator()") VectorIterator.Generic rightIterator,
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            profile.profileCounted(leftLength);
            Object itLeft = leftIterator.init(left);
            Object itRight = rightIterator.init(right);
            Object resultStore = getTargetDataStore.execute(result);
            Object rightValue = rightIterator.next(right, itRight);
            for (int i = 0; profile.inject(i < leftLength); ++i) {
                Object value = indexedAction.perform(node, leftIterator.next(left, itLeft), rightValue);
                targetSetDataAt.setDataAtAsObject(result, resultStore, i, value);
            }
        }

        @Specialization(guards = {"leftLength == rightLength"})
        @SuppressWarnings("unused")
        protected void doSameLength(BinaryMapFunctionNode node, RVector<?> result, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength,
                        @Cached("createIterator()") VectorIterator.Generic leftIterator,
                        @Cached("createIterator()") VectorIterator.Generic rightIterator,
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            profile.profileCounted(leftLength);
            Object itLeft = leftIterator.init(left);
            Object itRight = rightIterator.init(right);
            Object resultStore = getTargetDataStore.execute(result);
            for (int i = 0; profile.inject(i < leftLength); ++i) {
                Object value = indexedAction.perform(node, leftIterator.next(left, itLeft), rightIterator.next(right, itRight));
                targetSetDataAt.setDataAtAsObject(result, resultStore, i, value);
            }
        }

        protected static boolean multiplesMinMax(int min, int max) {
            return max % min == 0;
        }

        @Specialization(replaces = {"doVectorScalar", "doScalarVector", "doSameLength"}, guards = {"multiplesMinMax(leftLength, rightLength)"})
        protected void doMultiplesLeft(BinaryMapFunctionNode node, RVector<?> result, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength,
                        @Cached("createIteratorWrapAround()") VectorIterator.Generic leftIterator,
                        @Cached("createIterator()") VectorIterator.Generic rightIterator,
                        @Cached("createCountingProfile()") LoopConditionProfile leftProfile,
                        @Cached("createCountingProfile()") LoopConditionProfile rightProfile) {
            int j = 0;
            rightProfile.profileCounted(rightLength / leftLength);
            Object itLeft = leftIterator.init(left);
            Object itRight = rightIterator.init(right);
            Object resultStore = getTargetDataStore.execute(result);
            while (rightProfile.inject(j < rightLength)) {
                leftProfile.profileCounted(leftLength);
                for (int k = 0; leftProfile.inject(k < leftLength); k++) {
                    Object value = indexedAction.perform(node, leftIterator.next(left, itLeft), rightIterator.next(right, itRight));
                    targetSetDataAt.setDataAtAsObject(result, resultStore, j, value);
                    j++;
                }
            }
        }

        @Specialization(replaces = {"doVectorScalar", "doScalarVector", "doSameLength"}, guards = {"multiplesMinMax(rightLength, leftLength)"})
        protected void doMultiplesRight(BinaryMapFunctionNode node, RVector<?> result, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength,
                        @Cached("createIterator()") VectorIterator.Generic leftIterator,
                        @Cached("createIteratorWrapAround()") VectorIterator.Generic rightIterator,
                        @Cached("createCountingProfile()") LoopConditionProfile leftProfile,
                        @Cached("createCountingProfile()") LoopConditionProfile rightProfile) {
            int j = 0;
            leftProfile.profileCounted(leftLength / rightLength);
            Object itLeft = leftIterator.init(left);
            Object itRight = rightIterator.init(right);
            Object resultStore = getTargetDataStore.execute(result);
            while (leftProfile.inject(j < leftLength)) {
                rightProfile.profileCounted(rightLength);
                for (int k = 0; rightProfile.inject(k < rightLength); k++) {
                    Object value = indexedAction.perform(node, leftIterator.next(left, itLeft), rightIterator.next(right, itRight));
                    targetSetDataAt.setDataAtAsObject(result, resultStore, j, value);
                    j++;
                }
            }
        }

        protected static boolean multiples(int leftLength, int rightLength) {
            int min;
            int max;
            if (leftLength >= rightLength) {
                min = rightLength;
                max = leftLength;
            } else {
                min = leftLength;
                max = rightLength;
            }
            return max % min == 0;
        }

        @Specialization(guards = {"!multiples(leftLength, rightLength)"})
        protected void doNoMultiples(BinaryMapFunctionNode node, RVector<?> result, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength,
                        @Cached("createIteratorWrapAround()") VectorIterator.Generic leftIterator,
                        @Cached("createIteratorWrapAround()") VectorIterator.Generic rightIterator,
                        @Cached("createCountingProfile()") LoopConditionProfile profile,
                        @Cached("createBinaryProfile()") ConditionProfile leftIncModProfile,
                        @Cached("createBinaryProfile()") ConditionProfile rightIncModProfile) {
            int max = Math.max(leftLength, rightLength);
            profile.profileCounted(max);
            Object itLeft = leftIterator.init(left);
            Object itRight = rightIterator.init(right);
            Object resultStore = getTargetDataStore.execute(result);
            for (int i = 0; profile.inject(i < max); ++i) {
                Object value = indexedAction.perform(node, leftIterator.next(left, itLeft), rightIterator.next(right, itRight));
                targetSetDataAt.setDataAtAsObject(result, resultStore, i, value);
            }
            RError.warning(this, RError.Message.LENGTH_NOT_MULTI);
        }

        private interface MapBinaryIndexedAction<V> {
            Object perform(BinaryMapFunctionNode action, V left, V right);
        }
    }
}
