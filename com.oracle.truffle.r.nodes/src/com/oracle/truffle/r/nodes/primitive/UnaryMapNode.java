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
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

final class UnaryMapScalarNode extends UnaryMapNode {

    @Child private VectorAccess operandAccess;

    UnaryMapScalarNode(UnaryMapFunctionNode scalarNode, RAbstractVector operand, RType argumentType, RType resultType) {
        super(scalarNode, operand, argumentType, resultType);
        this.operandAccess = operand.access();
    }

    @Override
    public boolean isSupported(RAbstractVector operand) {
        return operandAccess.supports(operand);
    }

    @Override
    public Object apply(RAbstractVector operand) {
        assert isSupported(operand);

        function.enable(operand);
        assert operand.getLength() == 1;

        try (RandomIterator iter = operandAccess.randomAccess(operand)) {
            switch (argumentType) {
                case Logical:
                    return function.applyLogical(operandAccess.getLogical(iter, 0));
                case Integer:
                    return function.applyInteger(operandAccess.getInt(iter, 0));
                case Double:
                    return function.applyDouble(operandAccess.getDouble(iter, 0));
                case Complex:
                    switch (resultType) {
                        case Double:
                            return function.applyDouble(operandAccess.getComplex(iter, 0));
                        case Complex:
                            return function.applyComplex(operandAccess.getComplex(iter, 0));
                        default:
                            throw RInternalError.shouldNotReachHere();
                    }
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }
    }
}

final class UnaryMapVectorNode extends UnaryMapNode {

    @Child private MapUnaryVectorInternalNode vectorNode;
    @Child private GetDimAttributeNode getDimNode;
    @Child private SetDimAttributeNode setDimNode;
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
    @Child private VectorAccess fastOperandAccess;
    @Child private VectorAccess resultAccess;

    // profiles
    private final VectorLengthProfile operandLengthProfile = VectorLengthProfile.create();
    private final BranchProfile hasAttributesProfile;
    private final ConditionProfile shareOperand;

    // compile-time optimization flags
    private final boolean mayContainMetadata;
    private final boolean mayFoldConstantTime;
    private final boolean mayShareOperand;
    private final boolean isGeneric;

    UnaryMapVectorNode(UnaryMapFunctionNode scalarNode, RAbstractVector operand, RType argumentType, RType resultType, boolean isGeneric) {
        super(scalarNode, operand, argumentType, resultType);
        this.fastOperandAccess = isGeneric ? null : operand.access();
        this.vectorNode = MapUnaryVectorInternalNode.create(resultType, argumentType);
        boolean operandVector = operand instanceof RVector;
        this.mayContainMetadata = operandVector;
        this.mayFoldConstantTime = argumentType == operand.getRType() && scalarNode.mayFoldConstantTime(operandClass);
        this.mayShareOperand = operandVector;
        this.isGeneric = isGeneric;

        // lazily create profiles only if needed to avoid unnecessary allocations
        this.shareOperand = mayShareOperand ? ConditionProfile.createBinaryProfile() : null;
        this.hasAttributesProfile = mayContainMetadata ? BranchProfile.create() : null;

    }

    @Override
    public boolean isSupported(RAbstractVector operand) {
        return operand.getClass() == operandClass && (isGeneric || fastOperandAccess.supports(operand));
    }

    @Override
    public Object apply(RAbstractVector originalOperand) {
        assert isSupported(originalOperand);
        RAbstractVector operand = operandClass.cast(originalOperand);
        function.enable(operand);
        int operandLength = operandLengthProfile.profile(operand.getLength());

        RAbstractVector target = null;
        if (mayFoldConstantTime) {
            target = function.tryFoldConstantTime(operand, operandLength);
        }
        if (target == null) {
            VectorAccess operandAccess = isGeneric ? operand.slowPathAccess() : fastOperandAccess;
            try (SequentialIterator operandIter = operandAccess.access(operand)) {
                if (mayShareOperand && operand.getRType() == resultType && shareOperand.profile(((RShareable) operand).isTemporary())) {
                    target = operand;
                    vectorNode.execute(function, operandLength, operandAccess, operandIter, operandAccess, operandIter);
                } else {
                    if (resultAccess == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        target = resultType.create(operandLength, false);
                        resultAccess = insert(target.access());
                    } else {
                        target = resultType.create(operandLength, false);
                    }
                    try (SequentialIterator resultIter = resultAccess.access(target)) {
                        vectorNode.execute(function, operandLength, resultAccess, resultIter, operandAccess, operandIter);
                    }
                }
            }
            RBaseNode.reportWork(this, operandLength);
            target.setComplete(function.isComplete());
        }
        if (mayContainMetadata) {
            target = handleMetadata(target, operand);
        }
        return target;
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
    private static void copyAttributesInternal(RVector<?> result, RAbstractVector attributeSource) {
        result.copyRegAttributesFrom(attributeSource);
        result.copyNamesFrom(attributeSource);
    }
}

abstract class MapUnaryVectorInternalNode extends RBaseNode {

    private abstract static class MapIndexedAction {
        public abstract void perform(UnaryMapFunctionNode action, VectorAccess result, SequentialIterator resultIter, VectorAccess operand, SequentialIterator operandIter);
    }

    private static final MapIndexedAction LOGICAL = new MapIndexedAction() {
        @Override
        public void perform(UnaryMapFunctionNode arithmetic, VectorAccess result, SequentialIterator resultIter, VectorAccess operand, SequentialIterator operandIter) {
            result.setLogical(resultIter, arithmetic.applyLogical(operand.getLogical(operandIter)));
        }
    };
    private static final MapIndexedAction INTEGER = new MapIndexedAction() {
        @Override
        public void perform(UnaryMapFunctionNode arithmetic, VectorAccess result, SequentialIterator resultIter, VectorAccess operand, SequentialIterator operandIter) {
            result.setInt(resultIter, arithmetic.applyInteger(operand.getInt(operandIter)));
        }
    };
    private static final MapIndexedAction DOUBLE = new MapIndexedAction() {
        @Override
        public void perform(UnaryMapFunctionNode arithmetic, VectorAccess result, SequentialIterator resultIter, VectorAccess operand, SequentialIterator operandIter) {
            result.setDouble(resultIter, arithmetic.applyDouble(operand.getDouble(operandIter)));
        }
    };
    private static final MapIndexedAction COMPLEX = new MapIndexedAction() {
        @Override
        public void perform(UnaryMapFunctionNode arithmetic, VectorAccess result, SequentialIterator resultIter, VectorAccess operand, SequentialIterator operandIter) {
            RComplex value = arithmetic.applyComplex(operand.getComplex(operandIter));
            result.setComplex(resultIter, value.getRealPart(), value.getImaginaryPart());
        }
    };
    private static final MapIndexedAction DOUBLE_COMPLEX = new MapIndexedAction() {
        @Override
        public void perform(UnaryMapFunctionNode arithmetic, VectorAccess result, SequentialIterator resultIter, VectorAccess operand, SequentialIterator operandIter) {
            result.setDouble(resultIter, arithmetic.applyDouble(operand.getComplex(operandIter)));
        }
    };
    private static final MapIndexedAction CHARACTER = new MapIndexedAction() {
        @Override
        public void perform(UnaryMapFunctionNode arithmetic, VectorAccess result, SequentialIterator resultIter, VectorAccess operand, SequentialIterator operandIter) {
            result.setString(resultIter, arithmetic.applyCharacter(operand.getString(operandIter)));
        }
    };

    private final MapIndexedAction indexedAction;

    protected MapUnaryVectorInternalNode(RType resultType, RType argumentType) {
        this.indexedAction = createIndexedAction(resultType, argumentType);
    }

    public static MapUnaryVectorInternalNode create(RType resultType, RType argumentType) {
        return MapUnaryVectorInternalNodeGen.create(resultType, argumentType);
    }

    private static MapIndexedAction createIndexedAction(RType resultType, RType argumentType) {
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

    protected abstract void execute(UnaryMapFunctionNode node, int operandLength, VectorAccess result, SequentialIterator resultIter, VectorAccess operand, SequentialIterator operandIter);

    @Specialization(guards = {"operandLength == 1"})
    protected void doScalar(UnaryMapFunctionNode node, @SuppressWarnings("unused") int operandLength, VectorAccess result, SequentialIterator resultIter, VectorAccess operand,
                    SequentialIterator operandIter) {
        operand.next(operandIter);
        if (result != operand) {
            result.next(resultIter);
        }
        indexedAction.perform(node, result, resultIter, operand, operandIter);
    }

    @Specialization(replaces = "doScalar")
    protected void doScalarVector(UnaryMapFunctionNode node, int operandLength, VectorAccess result, SequentialIterator resultIter, VectorAccess operand, SequentialIterator operandIter,
                    @Cached("createCountingProfile()") LoopConditionProfile profile) {
        profile.profileCounted(operandLength);
        while (profile.inject(operand.next(operandIter))) {
            if (result != operand) {
                result.next(resultIter);
            }
            indexedAction.perform(node, result, resultIter, operand, operandIter);
        }
    }
}

public abstract class UnaryMapNode extends RBaseNode {

    @Child protected UnaryMapFunctionNode function;
    protected final Class<? extends RAbstractVector> operandClass;
    protected final RType argumentType;
    protected final RType resultType;

    protected UnaryMapNode(UnaryMapFunctionNode function, RAbstractVector operand, RType argumentType, RType resultType) {
        this.function = function;
        this.operandClass = operand.getClass();
        this.argumentType = argumentType;
        this.resultType = resultType;
    }

    public static UnaryMapNode create(UnaryMapFunctionNode scalarNode, RAbstractVector operand, RType argumentType, RType resultType, boolean isGeneric) {
        if (operand instanceof RScalarVector) {
            return new UnaryMapScalarNode(scalarNode, operand, argumentType, resultType);
        } else {
            return new UnaryMapVectorNode(scalarNode, operand, argumentType, resultType, isGeneric);
        }
    }

    public abstract boolean isSupported(RAbstractVector operand);

    public abstract Object apply(RAbstractVector originalOperand);
}
