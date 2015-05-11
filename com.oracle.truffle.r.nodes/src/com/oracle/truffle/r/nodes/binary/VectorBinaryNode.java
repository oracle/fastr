/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.profile.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Encapsulates the generic logic for binary operations that are performed on two input vectors. It
 * ensures that attributes and dimensions are properly migrated from the source vectors to the
 * result vectors. It also implements sharing of temporary vectors as result vector. Internally it
 * uses a {@link ScalarBinaryNode} to abstract one scalar operation invocation on the vector and
 * {@link VectorMapBinaryNode} to abstract the iteration over two arrays with potentially differing
 * length. The {@link ScalarBinaryNode} instance can be passed from the outside in order to enable
 * the use for different scalar operations like logic and arithmetic operations.
 */
final class VectorBinaryNode extends Node {

    @Child private VectorMapBinaryNode vectorNode;
    @Child private ScalarBinaryNode scalarNode;

    // profiles
    private final Class<? extends RAbstractVector> leftClass;
    private final Class<? extends RAbstractVector> rightClass;
    private final VectorLengthProfile leftLengthProfile = VectorLengthProfile.create();
    private final VectorLengthProfile rightLengthProfile = VectorLengthProfile.create();
    private final ConditionProfile dimensionsProfile;
    private final BranchProfile hasAttributesProfile;
    private final RAttributeProfiles attrProfiles;
    private final BranchProfile seenEmpty = BranchProfile.create();
    private final ConditionProfile shareLeft;
    private final ConditionProfile shareRight;

    // compile-time optimization flags
    private final boolean scalarTypes;
    private final boolean mayContainMetadata;
    private final boolean mayFoldConstantTime;
    private final boolean mayShareLeft;
    private final boolean mayShareRight;

    // config flags
    private final boolean copyAttributes;

    VectorBinaryNode(ScalarBinaryNode scalarNode, Class<? extends RAbstractVector> leftclass, Class<? extends RAbstractVector> rightClass, RType leftType, RType rightType, RType argumentType,
                    RType resultType, boolean copyAttributes) {
        this.scalarNode = scalarNode;
        this.leftClass = leftclass;
        this.rightClass = rightClass;
        this.vectorNode = VectorMapBinaryNode.create(resultType, argumentType);
        this.scalarTypes = RScalarVector.class.isAssignableFrom(leftclass) && RScalarVector.class.isAssignableFrom(rightClass);
        boolean leftVectorImpl = RVector.class.isAssignableFrom(leftclass);
        boolean rightVectorImpl = RVector.class.isAssignableFrom(rightClass);
        this.mayContainMetadata = leftVectorImpl || rightVectorImpl;
        this.mayFoldConstantTime = scalarNode.mayFoldConstantTime(leftclass, rightClass);
        this.mayShareLeft = leftType == resultType && leftVectorImpl;
        this.mayShareRight = rightType == resultType && rightVectorImpl;

        // lazily create profiles only if needed to avoid unnecessary allocations
        this.shareLeft = mayShareLeft ? ConditionProfile.createBinaryProfile() : null;
        this.shareRight = mayShareRight ? ConditionProfile.createBinaryProfile() : null;
        this.attrProfiles = mayContainMetadata ? RAttributeProfiles.create() : null;
        this.hasAttributesProfile = mayContainMetadata ? BranchProfile.create() : null;
        this.dimensionsProfile = mayContainMetadata ? ConditionProfile.createBinaryProfile() : null;

        this.copyAttributes = copyAttributes;
    }

    public boolean isSupported(Object left, Object right) {
        return left.getClass() == leftClass && right.getClass() == rightClass;
    }

    public Object apply(Object originalLeft, Object originalRight) {
        assert isSupported(originalLeft, originalRight);
        RAbstractVector left = leftClass.cast(originalLeft);
        RAbstractVector right = rightClass.cast(originalRight);

        int leftLength = leftLengthProfile.profile(left.getLength());
        int rightLength = rightLengthProfile.profile(right.getLength());

        RType argumentType = getArgumentType();
        RAbstractVector leftCast = left.castSafe(argumentType);
        RAbstractVector rightCast = right.castSafe(argumentType);

        assert leftCast != null;
        assert rightCast != null;

        scalarNode.enable(leftCast, rightCast);

        if (scalarTypes) {
            assert left.getLength() == 1;
            assert right.getLength() == 1;
            return scalarOperation(leftCast, rightCast);
        } else {
            return vectorOperation(left, rightCast, leftLength, right, leftCast, rightLength);
        }
    }

    private static boolean differentDimensions(RAbstractVector left, RAbstractVector right) {
        int[] leftDimensions = left.getDimensions();
        int[] rightDimensions = right.getDimensions();
        int leftLength = leftDimensions.length;
        int rightLength = leftDimensions.length;
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

    private Object scalarOperation(RAbstractVector left, RAbstractVector right) {
        switch (getArgumentType()) {
            case Raw:
                byte leftValueRaw = ((RAbstractRawVector) left).getRawDataAt(0);
                byte rightValueRaw = ((RAbstractRawVector) right).getRawDataAt(0);
                switch (getResultType()) {
                    case Raw:
                        return RRaw.valueOf(scalarNode.applyRaw(leftValueRaw, rightValueRaw));
                    case Logical:
                        return scalarNode.applyLogical(RRuntime.raw2int(leftValueRaw), RRuntime.raw2int(rightValueRaw));
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Logical:
                byte leftValueLogical = ((RAbstractLogicalVector) left).getDataAt(0);
                byte rightValueLogical = ((RAbstractLogicalVector) right).getDataAt(0);
                return scalarNode.applyLogical(leftValueLogical, rightValueLogical);
            case Integer:
                int leftValueInt = ((RAbstractIntVector) left).getDataAt(0);
                int rightValueInt = ((RAbstractIntVector) right).getDataAt(0);
                switch (getResultType()) {
                    case Logical:
                        return scalarNode.applyLogical(leftValueInt, rightValueInt);
                    case Integer:
                        return scalarNode.applyInteger(leftValueInt, rightValueInt);
                    case Double:
                        return scalarNode.applyDouble(leftValueInt, rightValueInt);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Double:
                double leftValueDouble = ((RAbstractDoubleVector) left).getDataAt(0);
                double rightValueDouble = ((RAbstractDoubleVector) right).getDataAt(0);
                switch (getResultType()) {
                    case Logical:
                        return scalarNode.applyLogical(leftValueDouble, rightValueDouble);
                    case Double:
                        return scalarNode.applyDouble(leftValueDouble, rightValueDouble);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Complex:
                RComplex leftValueComplex = ((RAbstractComplexVector) left).getDataAt(0);
                RComplex rightValueComplex = ((RAbstractComplexVector) right).getDataAt(0);
                switch (getResultType()) {
                    case Logical:
                        return scalarNode.applyLogical(leftValueComplex, rightValueComplex);
                    case Complex:
                        return scalarNode.applyComplex(leftValueComplex, rightValueComplex);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            case Character:
                String leftValueString = ((RAbstractStringVector) left).getDataAt(0);
                String rightValueString = ((RAbstractStringVector) right).getDataAt(0);
                switch (getResultType()) {
                    case Logical:
                        return scalarNode.applyLogical(leftValueString, rightValueString);
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    private Object vectorOperation(RAbstractVector left, RAbstractVector rightCast, int leftLength, RAbstractVector right, RAbstractVector leftCast, int rightLength) {
        if (mayContainMetadata && (dimensionsProfile.profile(left.hasDimensions() && right.hasDimensions()))) {
            if (differentDimensions(left, right)) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NON_CONFORMABLE_ARRAYS);
            }
        }

        if (leftLength == 0 || rightLength == 0) {
            /*
             * It is safe to skip attribute handling here as they are never copied if length is 0 of
             * either side. Note that dimension check still needs to be performed.
             */
            seenEmpty.enter();
            return getResultType().getEmpty();
        }

        RAbstractVector target = null;
        if (mayFoldConstantTime) {
            target = scalarNode.tryFoldConstantTime(leftCast, leftLength, rightCast, rightLength);
        }
        if (target == null) {
            int maxLength = Math.max(leftLength, rightLength);
            target = createOrShareVector(leftLength, left, rightLength, right, maxLength);
            Object store;
            if (target instanceof RAccessibleStore) {
                store = ((RAccessibleStore<?>) target).getInternalStore();
            } else {
                throw RInternalError.shouldNotReachHere();
            }
            vectorNode.apply(scalarNode, store, leftCast, leftLength, rightCast, rightLength);
            RNode.reportWork(this, maxLength);
        }
        if (mayContainMetadata) {
            target = handleMetadata(target, left, leftLength, right, rightLength);
        }
        target.setComplete(scalarNode.isComplete());
        return target;
    }

    private RAbstractVector createOrShareVector(int leftLength, RAbstractVector left, int rightLength, RAbstractVector right, int maxLength) {

        RType resultType = getResultType();
        if (mayShareLeft && left.getRType() == resultType && shareLeft.profile(leftLength == maxLength && ((RShareable) left).isTemporary())) {
            return left;
        }
        if (mayShareRight && right.getRType() == resultType && shareRight.profile(rightLength == maxLength && ((RShareable) right).isTemporary())) {
            return right;
        }
        return getResultType().create(maxLength);
    }

    private RType getArgumentType() {
        return vectorNode.getArgumentType();
    }

    private RType getResultType() {
        return vectorNode.getResultType();
    }

    private RAbstractVector handleMetadata(RAbstractVector target, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        RAbstractVector result = target;
        if (containsMetadata(left) || containsMetadata(right)) {
            hasAttributesProfile.enter();
            result = result.materialize();
            copyAttributesInternal((RVector) result, left, leftLength, right, rightLength);
        }
        return result;
    }

    private boolean containsMetadata(RAbstractVector vector) {
        return vector instanceof RVector &&
                        (vector.hasDimensions() || (copyAttributes && vector.getAttributes() != null) || vector.getNames(attrProfiles) != null || vector.getDimNames(attrProfiles) != null);
    }

    @TruffleBoundary
    private void copyAttributesInternal(RVector result, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        // TODO this method needs its own specializing node
        if (leftLength == rightLength) {
            if (copyAttributes) {
                if (result != right) {
                    result.copyRegAttributesFrom(right);
                }
                if (result != left) {
                    result.copyRegAttributesFrom(left);
                }
            }

            result.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), getEncapsulatingSourceSection());

            boolean hadNames;
            if (result == left) {
                hadNames = result.getNames() != null;
            } else {
                hadNames = result.copyNamesFrom(attrProfiles, left);
            }
            if (!hadNames && result != right) {
                result.copyNamesFrom(attrProfiles, right);
            }
        } else {
            RAbstractVector attributeSource = leftLength < rightLength ? right : left;
            if (copyAttributes && result != attributeSource) {
                result.copyRegAttributesFrom(attributeSource);
            }
            result.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), getEncapsulatingSourceSection());
            if (attributeSource != result) {
                result.copyNamesFrom(attrProfiles, attributeSource);
            }
        }
    }

}
