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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.HasFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.primitive.UnaryMapNodeFactory.MapUnaryVectorInternalNodeGen;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

public final class UnaryMapNode extends RBaseNode {

    @Child private UnaryMapFunctionNode scalarNode;
    @Child private MapUnaryVectorInternalNode vectorNode;
    @Child private GetDimAttributeNode getDimNode;
    @Child private SetDimAttributeNode setDimNode;
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    // profiles
    private final Class<? extends RAbstractVector> operandClass;
    private final VectorLengthProfile operandLengthProfile = VectorLengthProfile.create();
    private final ConditionProfile operandIsNAProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile hasAttributesProfile;
    private final RAttributeProfiles attrProfiles;
    private final ConditionProfile shareOperand;

    // compile-time optimization flags
    private final boolean scalarType;
    private final boolean mayContainMetadata;
    private final boolean mayFoldConstantTime;
    private final boolean mayShareOperand;

    private UnaryMapNode(UnaryMapFunctionNode scalarNode, RAbstractVector operand, RType argumentType, RType resultType) {
        this.scalarNode = scalarNode;
        this.vectorNode = MapUnaryVectorInternalNode.create(resultType, argumentType);
        this.operandClass = operand.getClass();
        this.scalarType = operand instanceof RScalarVector;
        boolean operandVector = operand instanceof RVector;
        this.mayContainMetadata = operandVector;
        this.mayFoldConstantTime = scalarNode.mayFoldConstantTime(operandClass);
        this.mayShareOperand = operandVector;

        // lazily create profiles only if needed to avoid unnecessary allocations
        this.shareOperand = operandVector ? ConditionProfile.createBinaryProfile() : null;
        this.attrProfiles = mayContainMetadata ? RAttributeProfiles.create() : null;
        this.hasAttributesProfile = mayContainMetadata ? BranchProfile.create() : null;
    }

    public static UnaryMapNode create(UnaryMapFunctionNode scalarNode, RAbstractVector operand, RType argumentType, RType resultType) {
        return new UnaryMapNode(scalarNode, operand, argumentType, resultType);
    }

    public Class<? extends RAbstractVector> getOperandClass() {
        return operandClass;
    }

    public RType getArgumentType() {
        return vectorNode.getArgumentType();
    }

    public RType getResultType() {
        return vectorNode.getResultType();
    }

    public boolean isSupported(Object operand) {
        return operand.getClass() == operandClass;
    }

    public Object apply(Object originalOperand) {
        assert isSupported(originalOperand);
        RAbstractVector operand = operandClass.cast(originalOperand);

        RAbstractVector operandCast = operand.castSafe(getArgumentType(), operandIsNAProfile);

        scalarNode.enable(operandCast);
        if (scalarType) {
            assert operand.getLength() == 1;
            return scalarOperation(operandCast);
        } else {
            int operandLength = operandLengthProfile.profile(operand.getLength());
            return vectorOperation(operand, operandCast, operandLength);
        }
    }

    private Object scalarOperation(RAbstractVector operand) {
        switch (getArgumentType()) {
            case Logical:
                return scalarNode.applyLogical(((RAbstractLogicalVector) operand).getDataAt(0));
            case Integer:
                return scalarNode.applyInteger(((RAbstractIntVector) operand).getDataAt(0));
            case Double:
                return scalarNode.applyDouble(((RAbstractDoubleVector) operand).getDataAt(0));
            case Complex:
                switch (getResultType()) {
                    case Double:
                        return scalarNode.applyDouble(((RAbstractComplexVector) operand).getDataAt(0));
                    case Complex:
                        return scalarNode.applyComplex(((RAbstractComplexVector) operand).getDataAt(0));
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    private Object vectorOperation(RAbstractVector operand, RAbstractVector operandCast, int operandLength) {
        RAbstractVector target = null;
        if (mayFoldConstantTime) {
            target = scalarNode.tryFoldConstantTime(operandCast, operandLength);
        }
        if (target == null) {
            target = createOrShareVector(operandLength, operand);
            Object store = target.getInternalStore();
            vectorNode.apply(scalarNode, store, operandCast, operandLength);
            RNode.reportWork(this, operandLength);
            target.setComplete(scalarNode.isComplete());
        }
        if (mayContainMetadata) {
            target = handleMetadata(target, operand);
        }
        return target;
    }

    private RAbstractVector createOrShareVector(int operandLength, RAbstractVector operand) {
        RType resultType = getResultType();
        if (mayShareOperand && operand.getRType() == resultType && shareOperand.profile(((RShareable) operand).isTemporary())) {
            return operand;
        }
        return resultType.create(operandLength, false);
    }

    private RAbstractVector handleMetadata(RAbstractVector target, RAbstractVector operand) {
        RAbstractVector result = target;
        if (containsMetadata(operand) && operand != target) {
            hasAttributesProfile.enter();
            result = result.materialize();

            if (getDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getDimNode = insert(GetDimAttributeNode.create());
            }

            if (setDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setDimNode = insert(SetDimAttributeNode.create());
            }

            setDimNode.setDimensions(result, getDimNode.getDimensions(operand));

            copyAttributesInternal((RVector<?>) result, operand);
        }
        return result;
    }

    private final ConditionProfile hasDimensionsProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile hasNamesProfile = ConditionProfile.createBinaryProfile();

    @Child private HasFixedAttributeNode hasDimNode = HasFixedAttributeNode.createDim();
    @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();

    private boolean containsMetadata(RAbstractVector vector) {
        return vector instanceof RVector &&
                        (hasDimensionsProfile.profile(hasDimNode.execute(vector)) || vector.getAttributes() != null || hasNamesProfile.profile(getNamesNode.getNames(vector) != null) ||
                                        getDimNamesNode.getDimNames(vector) != null);
    }

    @TruffleBoundary
    private void copyAttributesInternal(RVector<?> result, RAbstractVector attributeSource) {
        result.copyRegAttributesFrom(attributeSource);
        result.copyNamesFrom(attrProfiles, attributeSource);
    }

    @SuppressWarnings("unused")
    protected abstract static class MapUnaryVectorInternalNode extends RBaseNode {

        private static final MapIndexedAction<byte[], RAbstractLogicalVector> LOGICAL = //
                        (arithmetic, result, resultIndex, left, leftIndex) -> {
                            result[resultIndex] = arithmetic.applyLogical(left.getDataAt(leftIndex));
                        };

        private static final MapIndexedAction<int[], RAbstractIntVector> INTEGER = //
                        (arithmetic, result, resultIndex, left, leftIndex) -> {
                            result[resultIndex] = arithmetic.applyInteger(left.getDataAt(leftIndex));
                        };

        private static final MapIndexedAction<double[], RAbstractDoubleVector> DOUBLE = //
                        (arithmetic, result, resultIndex, left, leftIndex) -> {
                            result[resultIndex] = arithmetic.applyDouble(left.getDataAt(leftIndex));
                        };

        private static final MapIndexedAction<double[], RAbstractComplexVector> COMPLEX = //
                        (arithmetic, result, resultIndex, left, leftIndex) -> {
                            RComplex value = arithmetic.applyComplex(left.getDataAt(leftIndex));
                            result[resultIndex << 1] = value.getRealPart();
                            result[(resultIndex << 1) + 1] = value.getImaginaryPart();
                        };

        private static final MapIndexedAction<double[], RAbstractComplexVector> DOUBLE_COMPLEX = //
                        (arithmetic, result, resultIndex, left, leftIndex) -> {
                            result[resultIndex] = arithmetic.applyDouble(left.getDataAt(leftIndex));
                        };

        private static final MapIndexedAction<String[], RAbstractStringVector> CHARACTER = //
                        (arithmetic, result, resultIndex, left, leftIndex) -> {
                            result[resultIndex] = arithmetic.applyCharacter(left.getDataAt(leftIndex));
                        };

        private final MapIndexedAction<Object, RAbstractVector> indexedAction;
        private final RType argumentType;
        private final RType resultType;

        @SuppressWarnings("unchecked")
        protected MapUnaryVectorInternalNode(RType resultType, RType argumentType) {
            this.indexedAction = (MapIndexedAction<Object, RAbstractVector>) createIndexedAction(resultType, argumentType);
            this.argumentType = argumentType;
            this.resultType = resultType;
        }

        public RType getArgumentType() {
            return argumentType;
        }

        public RType getResultType() {
            return resultType;
        }

        public static MapUnaryVectorInternalNode create(RType resultType, RType argumentType) {
            return MapUnaryVectorInternalNodeGen.create(resultType, argumentType);
        }

        private static MapIndexedAction<? extends Object, ? extends RAbstractVector> createIndexedAction(RType resultType, RType argumentType) {
            switch (argumentType) {
                case Logical:
                    return LOGICAL;
                case Integer:
                    switch (resultType) {
                        case Integer:
                            return INTEGER;
                        case Double:
                            return DOUBLE;
                        default:
                            throw RInternalError.shouldNotReachHere();
                    }
                case Double:
                    return DOUBLE;
                case Complex:
                    switch (resultType) {
                        case Double:
                            return DOUBLE_COMPLEX;
                        case Complex:
                            return COMPLEX;
                        default:
                            throw RInternalError.shouldNotReachHere();
                    }
                case Character:
                    return CHARACTER;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }

        private void apply(UnaryMapFunctionNode scalarAction, Object store, RAbstractVector operand, int operandLength) {
            assert operand.getLength() == operandLength;
            assert operand.getRType() == argumentType;
            assert isStoreCompatible(store, resultType, operandLength);

            executeInternal(scalarAction, store, operand, operandLength);
        }

        private static boolean isStoreCompatible(Object store, RType resultType, int operandLength) {
            switch (resultType) {
                case Logical:
                    assert store instanceof byte[] && ((byte[]) store).length == operandLength;
                    return true;
                case Integer:
                    assert store instanceof int[] && ((int[]) store).length == operandLength;
                    return true;
                case Double:
                    assert store instanceof double[] && ((double[]) store).length == operandLength;
                    return true;
                case Complex:
                    assert store instanceof double[] && ((double[]) store).length >> 1 == operandLength;
                    return true;
                case Character:
                    assert store instanceof String[] && ((String[]) store).length == operandLength;
                    return true;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }

        protected abstract void executeInternal(UnaryMapFunctionNode node, Object store, RAbstractVector operand, int operandLength);

        @Specialization(guards = {"operandLength == 1"})
        protected void doScalar(UnaryMapFunctionNode node, Object store, RAbstractVector operand, int operandLength) {
            indexedAction.perform(node, store, 0, operand, 0);
        }

        @Specialization(contains = "doScalar")
        protected void doScalarVector(UnaryMapFunctionNode node, Object store, RAbstractVector operand, int operandLength, @Cached("createCountingProfile()") LoopConditionProfile profile) {
            profile.profileCounted(operandLength);
            for (int i = 0; profile.inject(i < operandLength); ++i) {
                indexedAction.perform(node, store, i, operand, i);
            }
        }

        private interface MapIndexedAction<A, V extends RAbstractVector> {

            void perform(UnaryMapFunctionNode action, A store, int resultIndex, V operand, int operandIndex);

        }
    }
}
