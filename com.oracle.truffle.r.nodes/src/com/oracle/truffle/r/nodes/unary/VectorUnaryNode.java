package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.profile.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

class VectorUnaryNode extends Node {

    @Child private ScalarUnaryNode scalarNode;
    @Child private VectorMapUnaryNode vectorNode;

    // profiles
    private final Class<? extends RAbstractVector> operandClass;
    private final VectorLengthProfile operandLengthProfile = VectorLengthProfile.create();
    private final BranchProfile hasAttributesProfile;
    private final RAttributeProfiles attrProfiles;
    private final ConditionProfile shareOperand;

    // compile-time optimization flags
    private final boolean scalarType;
    private final boolean mayContainMetadata;
    private final boolean mayFoldConstantTime;
    private final boolean mayShareOperand;

    VectorUnaryNode(ScalarUnaryNode scalarNode, Class<? extends RAbstractVector> operandClass, RType argumentType, RType resultType) {
        this.scalarNode = scalarNode;
        this.vectorNode = VectorMapUnaryNode.create(resultType, argumentType);
        this.operandClass = operandClass;
        this.scalarType = RScalarVector.class.isAssignableFrom(operandClass);
        boolean operandVector = RVector.class.isAssignableFrom(operandClass);
        this.mayContainMetadata = operandVector;
        this.mayFoldConstantTime = scalarNode.mayFoldConstantTime(operandClass);
        this.mayShareOperand = operandVector;

        // lazily create profiles only if needed to avoid unnecessary allocations
        this.shareOperand = operandVector ? ConditionProfile.createBinaryProfile() : null;
        this.attrProfiles = mayContainMetadata ? RAttributeProfiles.create() : null;
        this.hasAttributesProfile = mayContainMetadata ? BranchProfile.create() : null;
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

        int operandLength = operandLengthProfile.profile(operand.getLength());
        RAbstractVector operandCast = operand.castSafe(getArgumentType());

        scalarNode.enable(operandCast);
        if (scalarType) {
            assert operand.getLength() == 1;
            return scalarOperation(operandCast);
        } else {
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
                return scalarNode.applyComplex(((RAbstractComplexVector) operand).getDataAt(0));
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
            Object store;
            if (target instanceof RAccessibleStore) {
                store = ((RAccessibleStore<?>) target).getInternalStore();
            } else {
                throw RInternalError.shouldNotReachHere();
            }
            vectorNode.apply(scalarNode, store, operandCast, operandLength);
        }
        if (mayContainMetadata) {
            target = handleMetadata(target, operand);
        }
        target.setComplete(scalarNode.isComplete());
        return target;
    }

    private RAbstractVector createOrShareVector(int operandLength, RAbstractVector operand) {
        RType resultType = getResultType();
        if (mayShareOperand && operand.getRType() == resultType && shareOperand.profile(((RShareable) operand).isTemporary())) {
            return operand;
        }
        return resultType.create(operandLength);
    }

    private RAbstractVector handleMetadata(RAbstractVector target, RAbstractVector operand) {
        RAbstractVector result = target;
        if (containsMetadata(operand) && operand != target) {
            hasAttributesProfile.enter();
            result = result.materialize();
            copyAttributesInternal((RVector) result, operand);
        }
        return result;
    }

    private boolean containsMetadata(RAbstractVector vector) {
        return vector instanceof RVector && (vector.hasDimensions() || vector.getAttributes() != null || vector.getNames(attrProfiles) != null || vector.getDimNames(attrProfiles) != null);
    }

    @TruffleBoundary
    private void copyAttributesInternal(RVector result, RAbstractVector attributeSource) {
        result.copyRegAttributesFrom(attributeSource);
        result.setDimensions(attributeSource.getDimensions());
        result.copyNamesFrom(attrProfiles, attributeSource);
    }
}
