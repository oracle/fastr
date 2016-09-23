/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.CopyAttributesNode;
import com.oracle.truffle.r.nodes.attributes.CopyAttributesNodeGen;
import com.oracle.truffle.r.nodes.primitive.BinaryMapNodeFactory.VectorMapBinaryInternalNodeGen;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
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
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

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

        RAbstractVector leftCast = left.castSafe(argumentType, leftIsNAProfile);
        RAbstractVector rightCast = right.castSafe(argumentType, rightIsNAProfile);

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

    private static boolean differentDimensions(RAbstractVector left, RAbstractVector right) {
        int[] leftDimensions = left.getDimensions();
        int[] rightDimensions = right.getDimensions();
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
        if (mayContainMetadata && (dimensionsProfile.profile(left.hasDimensions() && right.hasDimensions()))) {
            if (differentDimensions(left, right)) {
                CompilerDirectives.transferToInterpreter();
                throw RError.error(this, RError.Message.NON_CONFORMABLE_ARRAYS);
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
            target = createOrShareVector(leftLength, left, rightLength, right, maxLength);
            Object store = target.getInternalStore();

            assert left.getLength() == leftLength;
            assert right.getLength() == rightLength;
            assert leftCast.getRType() == argumentType;
            assert rightCast.getRType() == argumentType;
            assert isStoreCompatible(store, resultType, leftLength, rightLength);

            vectorNode.execute(function, store, leftCast, leftLength, rightCast, rightLength);
            RNode.reportWork(this, maxLength);
            target.setComplete(function.isComplete());
        }
        if (mayContainMetadata) {
            target = copyAttributes.execute(target, left, leftLength, right, rightLength);
        }
        return target;
    }

    private RAbstractVector createOrShareVector(int leftLength, RAbstractVector left, int rightLength, RAbstractVector right, int maxLength) {
        if (mayShareLeft && left.getRType() == resultType && shareLeft.profile(leftLength == maxLength && ((RShareable) left).isTemporary())) {
            return left;
        }
        if (mayShareRight && right.getRType() == resultType && shareRight.profile(rightLength == maxLength && ((RShareable) right).isTemporary())) {
            return right;
        }
        return resultType.create(maxLength, false);
    }

    private static boolean isStoreCompatible(Object store, RType resultType, int leftLength, int rightLength) {
        int maxLength = Math.max(leftLength, rightLength);
        switch (resultType) {
            case Raw:
                assert store instanceof byte[] && ((byte[]) store).length == maxLength;
                return true;
            case Logical:
                assert store instanceof byte[] && ((byte[]) store).length == maxLength;
                return true;
            case Integer:
                assert store instanceof int[] && ((int[]) store).length == maxLength;
                return true;
            case Double:
                assert store instanceof double[] && ((double[]) store).length == maxLength;
                return true;
            case Complex:
                assert store instanceof double[] && ((double[]) store).length >> 1 == maxLength;
                return true;
            case Character:
                assert store instanceof String[] && ((String[]) store).length == maxLength;
                return true;
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    protected abstract static class VectorMapBinaryInternalNode extends RBaseNode {

        private static final MapBinaryIndexedAction<byte[], RAbstractLogicalVector> LOGICAL_LOGICAL = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyLogical(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                        };
        private static final MapBinaryIndexedAction<byte[], RAbstractIntVector> LOGICAL_INTEGER = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyLogical(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                        };
        private static final MapBinaryIndexedAction<byte[], RAbstractDoubleVector> LOGICAL_DOUBLE = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyLogical(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                        };
        private static final MapBinaryIndexedAction<byte[], RAbstractComplexVector> LOGICAL_COMPLEX = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyLogical(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                        };
        private static final MapBinaryIndexedAction<byte[], RAbstractStringVector> LOGICAL_CHARACTER = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyLogical(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                        };
        private static final MapBinaryIndexedAction<byte[], RAbstractRawVector> LOGICAL_RAW = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyLogical(RRuntime.raw2int(left.getRawDataAt(leftIndex)), RRuntime.raw2int(right.getRawDataAt(rightIndex)));
                        };
        private static final MapBinaryIndexedAction<byte[], RAbstractRawVector> RAW_RAW = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyRaw(left.getRawDataAt(leftIndex), right.getRawDataAt(rightIndex));
                        };

        private static final MapBinaryIndexedAction<int[], RAbstractIntVector> INTEGER_INTEGER = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyInteger(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                        };

        private static final MapBinaryIndexedAction<double[], RAbstractIntVector> DOUBLE_INTEGER = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyDouble(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                        };

        private static final MapBinaryIndexedAction<double[], RAbstractDoubleVector> DOUBLE = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyDouble(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                        };

        private static final MapBinaryIndexedAction<double[], RAbstractComplexVector> COMPLEX = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            RComplex value = arithmetic.applyComplex(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                            result[resultIndex << 1] = value.getRealPart();
                            result[(resultIndex << 1) + 1] = value.getImaginaryPart();
                        };
        private static final MapBinaryIndexedAction<String[], RAbstractStringVector> CHARACTER = //
                        (arithmetic, result, resultIndex, left, leftIndex, right, rightIndex) -> {
                            result[resultIndex] = arithmetic.applyCharacter(left.getDataAt(leftIndex), right.getDataAt(rightIndex));
                        };

        private final MapBinaryIndexedAction<Object, RAbstractVector> indexedAction;

        @SuppressWarnings("unchecked")
        protected VectorMapBinaryInternalNode(RType resultType, RType argumentType) {
            this.indexedAction = (MapBinaryIndexedAction<Object, RAbstractVector>) createIndexedAction(resultType, argumentType);
        }

        public static VectorMapBinaryInternalNode create(RType resultType, RType argumentType) {
            return VectorMapBinaryInternalNodeGen.create(resultType, argumentType);
        }

        private static MapBinaryIndexedAction<? extends Object, ? extends RAbstractVector> createIndexedAction(RType resultType, RType argumentType) {
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

        public abstract void execute(BinaryMapFunctionNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength);

        @Specialization(guards = {"leftLength == 1", "rightLength == 1"})
        @SuppressWarnings("unused")
        protected void doScalarScalar(BinaryMapFunctionNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
            indexedAction.perform(node, store, 0, left, 0, right, 0);
        }

        @Specialization(contains = "doScalarScalar", guards = {"leftLength == 1"})
        @SuppressWarnings("unused")
        protected void doScalarVector(BinaryMapFunctionNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength, //
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            profile.profileCounted(rightLength);
            for (int i = 0; profile.inject(i < rightLength); ++i) {
                indexedAction.perform(node, store, i, left, 0, right, i);
            }
        }

        @Specialization(contains = "doScalarScalar", guards = {"rightLength == 1"})
        @SuppressWarnings("unused")
        protected void doVectorScalar(BinaryMapFunctionNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength, //
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            profile.profileCounted(leftLength);
            for (int i = 0; profile.inject(i < leftLength); ++i) {
                indexedAction.perform(node, store, i, left, i, right, 0);
            }
        }

        @Specialization(guards = {"leftLength == rightLength"})
        @SuppressWarnings("unused")
        protected void doSameLength(BinaryMapFunctionNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength, //
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            profile.profileCounted(leftLength);
            for (int i = 0; profile.inject(i < leftLength); ++i) {
                indexedAction.perform(node, store, i, left, i, right, i);
            }
        }

        protected static boolean multiplesMinMax(int min, int max) {
            return max % min == 0;
        }

        @Specialization(contains = {"doVectorScalar", "doScalarVector", "doSameLength"}, guards = {"multiplesMinMax(leftLength, rightLength)"})
        protected void doMultiplesLeft(BinaryMapFunctionNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength, //
                        @Cached("createCountingProfile()") LoopConditionProfile leftProfile, //
                        @Cached("createCountingProfile()") LoopConditionProfile rightProfile) {
            int j = 0;
            rightProfile.profileCounted(rightLength / leftLength);
            while (rightProfile.inject(j < rightLength)) {
                leftProfile.profileCounted(leftLength);
                for (int k = 0; leftProfile.inject(k < leftLength); k++) {
                    indexedAction.perform(node, store, j, left, k, right, j);
                    j++;
                }
            }
        }

        @Specialization(contains = {"doVectorScalar", "doScalarVector", "doSameLength"}, guards = {"multiplesMinMax(rightLength, leftLength)"})
        protected void doMultiplesRight(BinaryMapFunctionNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength, //
                        @Cached("createCountingProfile()") LoopConditionProfile leftProfile, //
                        @Cached("createCountingProfile()") LoopConditionProfile rightProfile) {
            int j = 0;
            leftProfile.profileCounted(leftLength / rightLength);
            while (leftProfile.inject(j < leftLength)) {
                rightProfile.profileCounted(rightLength);
                for (int k = 0; rightProfile.inject(k < rightLength); k++) {
                    indexedAction.perform(node, store, j, left, j, right, k);
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
        protected void doNoMultiples(BinaryMapFunctionNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength, //
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            int j = 0;
            int k = 0;
            int max = Math.max(leftLength, rightLength);
            profile.profileCounted(max);
            for (int i = 0; profile.inject(i < max); ++i) {
                indexedAction.perform(node, store, i, left, j, right, k);
                j = Utils.incMod(j, leftLength);
                k = Utils.incMod(k, rightLength);
            }
            RError.warning(this, RError.Message.LENGTH_NOT_MULTI);
        }

        private interface MapBinaryIndexedAction<A, V extends RAbstractVector> {

            void perform(BinaryMapFunctionNode action, A store, int resultIndex, V left, int leftIndex, V right, int rightIndex);

        }
    }
}
