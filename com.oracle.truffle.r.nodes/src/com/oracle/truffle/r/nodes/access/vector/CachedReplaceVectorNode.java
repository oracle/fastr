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
package com.oracle.truffle.r.nodes.access.vector;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.access.vector.PositionsCheckNode.PositionProfile;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.profile.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.*;

final class CachedReplaceVectorNode extends CachedVectorNode {

    private static final Object DELETE_MARKER = new Object();

    private final Class<?> vectorClass;
    private final Class<?> valueClass;

    private final VectorLengthProfile targetLengthProfile = VectorLengthProfile.create();
    private final VectorLengthProfile valueLengthProfile = VectorLengthProfile.create();
    private final BranchProfile warningBranch = BranchProfile.create();
    private final RAttributeProfiles vectorNamesProfile = RAttributeProfiles.create();
    private final RAttributeProfiles positionNamesProfile = RAttributeProfiles.create();
    private final ConditionProfile rightIsShared = ConditionProfile.createBinaryProfile();
    private final BranchProfile rightIsNonTemp = BranchProfile.create();
    private final BranchProfile rightIsTemp = BranchProfile.create();
    private final BranchProfile resizeProfile = BranchProfile.create();
    private final BranchProfile sharedProfile = BranchProfile.create();

    private final RType valueType;
    private final RType castType;
    private final boolean updatePositionNames;

    @Child private WriteIndexedVectorNode writeVectorNode;
    @Child private PositionsCheckNode positionsCheckNode;
    @Child private CastNode castVectorNode;
    @Child private CachedReplaceVectorNode copyPositionNames;
    @Child private DeleteElementsNode deleteElementsNode;

    public CachedReplaceVectorNode(ElementAccessMode mode, RTypedValue vector, Object[] positions, RTypedValue value, boolean updatePositionNames, boolean recursive) {
        super(mode, vector, positions, recursive);

        this.updatePositionNames = updatePositionNames;
        this.vectorClass = vector.getClass();
        this.valueClass = value.getClass();
        this.valueType = value.getRType();
        this.castType = verifyCastType(resolveCastVectorType());
        this.castVectorNode = createCastVectorNode();
        this.deleteElementsNode = isDeleteElements() ? new DeleteElementsNode() : null;

        Object[] convertedPositions = filterPositions(positions);
        this.positionsCheckNode = new PositionsCheckNode(mode, vectorType, convertedPositions, true, true, recursive);
        if (castType != null && !castType.isNull()) {
            this.writeVectorNode = WriteIndexedVectorNode.create(castType, convertedPositions.length, false, true, mode.isSubscript() && !isDeleteElements());
        }
    }

    public boolean isSupported(Object target, Object[] positions, Object values) {
        if (vectorClass == target.getClass() && values.getClass() == valueClass) {
            return positionsCheckNode.isSupported(positions);
        }
        return false;
    }

    public Object apply(Object originalVector, Object[] originalPositions, Object originalValues) {
        final Object[] positions = filterPositions(originalPositions);
        assert isSupported(originalVector, positions, originalValues);

        Object castVector = vectorClass.cast(originalVector);
        Object castValue = valueClass.cast(originalValues);

        RAbstractContainer value;
        if (valueType == RType.Null) {
            if (vectorType == RType.Null) {
                // we cast Null to Logical, but in the end it will fold and return Null
                value = RType.Logical.getEmpty();
            } else if (castType == RType.List) {
                value = RDataFactory.createList(new Object[]{DELETE_MARKER});
            } else {
                value = castType.getEmpty();
            }
        } else {
            if (!isList() && castValue instanceof RFactor) {
                value = ((RFactor) castValue).getVector();
            } else {
                value = (RAbstractContainer) castValue;
            }
        }

        int appliedValueLength = valueLengthProfile.profile(value.getLength());

        int valueLength;
        if (this.numberOfDimensions > 1 && isDeleteElements()) {
            valueLength = 0;
        } else {
            valueLength = appliedValueLength;
        }

        if (vectorType == RType.Null) {
            if (valueLength == 0) {
                return RNull.instance;
            }
        }

        /*
         * Unfortunately special behavior for some RTypes are necessary. We should aim for getting
         * rid of them as much as possible in the future.
         */
        RAbstractVector vector;
        switch (vectorType) {
            case Null:
                vector = castType.getEmpty();
                break;
            case Factor:
                vector = ((RFactor) castVector).getVector();
                break;
            case PairList:
                vector = ((RPairList) castVector).toRList();
                break;
            case Environment:
                return doEnvironment((REnvironment) castVector, positions, castValue);
            case Language:
                vector = RContext.getRRuntimeASTAccess().asList((RLanguage) castVector);
                break;
            case Expression:
                vector = ((RExpression) castVector).getList();
                break;
            default:
                vector = (RAbstractVector) castVector;
                break;
        }

        int vectorLength = targetLengthProfile.profile(vector.getLength());
        int[] vectorDimensions;
        if (numberOfDimensions == 1) {
            /* For single dimension case we never need to load the dimension. */
            vectorDimensions = null;
        } else {
            assert numberOfDimensions > 1;
            vectorDimensions = loadVectorDimensions(vector);
        }

        PositionProfile[] positionProfiles = positionsCheckNode.executeCheck(vector, vectorDimensions, vectorLength, positions);

        if (castVectorNode != null) {
            vector = (RAbstractVector) castVectorNode.execute(vector);
        }

        int replacementLength = positionsCheckNode.getSelectedPositionsCount(positionProfiles);
        if (replacementLength == 0) {
            /* Nothing to modify */
            return vector;
        }

        if (valueLength != 1) {
            verifyValueLength(positionProfiles, valueLength);
        }

        if (isList()) {
            if (mode.isSubscript()) {
                value = copyValueOnAssignment(value);
            }
        } else if (value instanceof RAbstractVector) {
            value = ((RAbstractVector) value).castSafe(castType);
        }

        vector = share(vector);

        int maxOutOfBounds = positionsCheckNode.getMaxOutOfBounds(positionProfiles);
        if (maxOutOfBounds > vectorLength) {
            resizeProfile.enter();
            if (isDeleteElements() && mode.isSubscript()) {
                return vector;
            }
            vector = resizeVector(vector, maxOutOfBounds);
            vectorLength = maxOutOfBounds;
        }
        vector = vector.materialize();

        vectorLength = targetLengthProfile.profile(vector.getLength());

        if (mode.isSubset()) {
            /*
             * Interestingly we always need to provide not NOT_MULTIPLE_REPLACEMENT error messages
             * for multi-dimensional deletes.
             */
            if ((this.numberOfDimensions > 1 && isDeleteElements()) || replacementLength % valueLength != 0) {
                if (this.numberOfDimensions > 1) {
                    errorBranch.enter();
                    throw RError.error(this, RError.Message.NOT_MULTIPLE_REPLACEMENT);
                } else {
                    warningBranch.enter();
                    RError.warning(this, RError.Message.NOT_MULTIPLE_REPLACEMENT);
                }
            }
        }

        writeVectorNode.enableValueNACheck(value);
        writeVectorNode.apply(vector, vectorLength, positions, value, appliedValueLength, vectorDimensions);
        vector.setComplete(vector.isComplete() && writeVectorNode.neverSeenNAInValue());
        RNode.reportWork(this, replacementLength);

        if (isDeleteElements()) {
            assert deleteElementsNode != null;
            vector = deleteElementsNode.deleteElements(vector, vectorLength);
        } else if (this.numberOfDimensions == 1 && updatePositionNames) {
            // depth must be == 0 to avoid recursive position name updates
            updateVectorWithPositionNames(vector, positions);
        }

        return vector;
    }

    private RType verifyCastType(RType compatibleType) {
        if (compatibleType == null && (vectorType.isNull() || vectorType.isVector())) {
            Message message;
            if (mode.isSubset()) {
                message = RError.Message.SUBASSIGN_TYPE_FIX;
            } else {
                message = RError.Message.SUBSCRIPT_TYPES;
            }
            throw RError.error(this, message, valueType.getName(), vectorType.getName(), false);
        }
        return compatibleType;
    }

    private CastNode createCastVectorNode() {
        if (castType == vectorType || castType == null || castType == RType.Null) {
            return null;
        }
        /*
         * All casts except list casts preserve dimension names.
         */
        if (castType == RType.List) {
            return CastListNodeGen.create(true, false, true);
        } else {
            return CastTypeNode.createCast(castType, true, true, true);
        }
    }

    private boolean isDeleteElements() {
        return castType == RType.List && valueClass == RNull.class;
    }

    private boolean isList() {
        return castType == RType.List;
    }

    private RType resolveCastVectorType() {
        final RType vector;
        // convert type for list like values
        switch (this.vectorType) {
            case Language:
            case DataFrame:
            case Expression:
            case PairList:
                vector = RType.List;
                break;
            default:
                vector = this.vectorType;
                break;
        }

        RType value = this.valueType;

        if (vector.isVector() && value.isVector()) {
            if (vector != value) {
                if (vector == RType.List || value == RType.List) {
                    return RType.List;
                }
                if (vector == RType.Raw || value == RType.Raw) {
                    return null;
                }
            }
            return RType.maxPrecedence(value, vector);
        } else if (vector.isNull() || value.isNull()) {
            if (!value.isNull()) {
                return value;
            }
            if (mode.isSubscript() && numberOfDimensions > 1) {
                return null;
            }
            return vector;
        } else {
            return null;
        }
    }

    private void verifyValueLength(PositionProfile[] positionProfiles, int valueLength) {
        if (mode.isSubscript()) {
            if (!isList()) {
                errorBranch.enter();
                if (valueLength == 0) {
                    throw RError.error(this, RError.Message.REPLACEMENT_0);
                } else {
                    throw RError.error(this, RError.Message.MORE_SUPPLIED_REPLACE);
                }
            }
        } else {
            assert mode.isSubset();
            if (valueLength == 0) {
                errorBranch.enter();
                throw RError.error(this, RError.Message.REPLACEMENT_0);
            } else if (positionsCheckNode.getContainsNA(positionProfiles)) {
                errorBranch.enter();
                throw RError.error(this, RError.Message.NA_SUBSCRIPTED);
            }
        }
    }

    private void updateVectorWithPositionNames(RAbstractVector vector, Object[] positions) {
        Object position = positionsCheckNode.getPositionCheckAt(0).profilePosition(positions[0]);
        RStringVector positionNames;
        if (position instanceof RMissing) {
            positionNames = null;
        } else {
            positionNames = ((RAbstractVector) position).getNames(positionNamesProfile);
        }
        if (positionNames != null && positionNames.getLength() > 0) {
            updatePositionNames(vector, positionNames, positions);
        }
    }

    private Object doEnvironment(REnvironment env, Object[] positions, Object originalValues) {
        if (mode.isSubset()) {
            errorBranch.enter();
            throw RError.error(this, RError.Message.OBJECT_NOT_SUBSETTABLE, RType.Environment.getName());
        }

        String positionString = tryCastSingleString(positionsCheckNode, positions);
        if (positionString != null) {
            env.setAttr(positionString.intern(), originalValues);
            return originalValues;
        }
        errorBranch.enter();
        throw RError.error(this, RError.Message.WRONG_ARGS_SUBSET_ENV);
    }

    private final ConditionProfile sharedConditionProfile = ConditionProfile.createBinaryProfile();

    private final ValueProfile sharedClassProfile = ValueProfile.createClassProfile();

    /*
     * TODO (chumer) share code between {@link #share(RAbstractVector)} and {@link
     * #copyValueOnAssignment(RAbstractContainer)}
     */
    private RAbstractVector share(RAbstractVector vector) {
        RAbstractVector returnVector = vector;
        if (returnVector instanceof RShareable) {
            RShareable shareable = (RShareable) returnVector;
            // TODO find out if we need to copy always in the recursive case
            if (sharedConditionProfile.profile(shareable.isShared()) || recursive) {
                sharedProfile.enter();
                shareable = (RShareable) returnVector.copy();
                returnVector = (RAbstractVector) shareable;
                if (FastROptions.NewStateTransition) {
                    shareable.incRefCount();
                } else if (shareable.isTemporary()) {
                    shareable.markNonTemporary();
                }
            }
        }
        returnVector = sharedClassProfile.profile(returnVector);

        CompilerAsserts.partialEvaluationConstant(returnVector.getClass());

        return returnVector;
    }

    private RAbstractContainer copyValueOnAssignment(RAbstractContainer value) {
        RShareable val = value.materializeToShareable();
        if (FastROptions.NewStateTransition) {
            if (rightIsShared.profile(val.isShared())) {
                val = val.copy();
            } else {
                val.incRefCount();
            }
        } else {
            if (rightIsShared.profile(val.isShared())) {
                val = val.copy();
            } else if (!val.isTemporary()) {
                rightIsNonTemp.enter();
                val.makeShared();
            } else {
                assert val.isTemporary();
                rightIsTemp.enter();
                val.markNonTemporary();
            }
        }
        return (RAbstractContainer) val;
    }

    // TODO (chumer) this is way to compilicated at the moment
    // not yet worth compiling. we should introduce some nodes for this
    @TruffleBoundary
    private static void copyAttributes(RAbstractVector vector, RList result) {
        result.copyRegAttributesFrom(vector);
    }

    // TODO (chumer) this is way to complicated at the moment
    // its not yet worth compiling it we need a better attribute system
    @TruffleBoundary
    private RVector resizeVector(RAbstractVector vector, int size) {
        RStringVector oldNames = vector.getNames(vectorNamesProfile);
        RVector res = vector.copyResized(size, true).materialize();
        if (vector instanceof RVector) {
            res.copyAttributesFrom(positionNamesProfile, vector);
        }
        res.setDimensionsNoCheck(null);
        res.setDimNamesNoCheck(null);
        if (oldNames != null) {
            oldNames = oldNames.resizeWithEmpty(size);
            res.setNames(oldNames);
        }
        return res;
    }

    private void updatePositionNames(RAbstractVector resultVector, RAbstractStringVector positionNames, Object[] positions) {
        RTypedValue names = resultVector.getNames(positionNamesProfile);
        if (names == null) {
            String[] emptyVector = new String[resultVector.getLength()];
            Arrays.fill(emptyVector, "");
            names = RDataFactory.createStringVector(emptyVector, true);
        }
        if (copyPositionNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            copyPositionNames = insert(new CachedReplaceVectorNode(mode, names, positions, positionNames, false, recursive));
        }
        assert copyPositionNames.isSupported(names, positions, positionNames);
        RAbstractStringVector newNames = (RAbstractStringVector) copyPositionNames.apply(names, positions, positionNames);
        resultVector.setNames(newNames.materialize());
    }

    private static final class DeleteElementsNode extends Node {

        private static final int PREVIOUS_RESULT_GENERIC = -2;
        private static final int PREVIOUS_RESULT_UNINITIALIZED = -1;

        /*
         * We speculate here for the result length to be always the same. This way we can omit the
         * first round of counting deleted elements.
         */
        @CompilationFinal private int previousResultLength = PREVIOUS_RESULT_UNINITIALIZED;

        private final RAttributeProfiles vectorNamesProfile = RAttributeProfiles.create();

        public RAbstractVector deleteElements(RAbstractVector vector, int vectorLength) {
            // we can speculate here that we delete always the same number of elements
            // without counting them.
            int resultLength;
            if (isPreviousResultSpecialized()) {
                resultLength = previousResultLength;
            } else if (previousResultLength == PREVIOUS_RESULT_UNINITIALIZED) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                resultLength = previousResultLength = calculateResultSize(vector, vectorLength);
            } else {
                resultLength = calculateResultSize(vector, vectorLength);
            }

            Object[] data = new Object[resultLength];
            RStringVector names = vector.getNames(vectorNamesProfile);
            boolean hasNames = names != null;
            String[] newNames = null;
            if (hasNames) {
                newNames = new String[resultLength];
            }

            int resultIndex = 0;
            for (int i = 0; i < vectorLength; i++) {
                Object element = vector.getDataAtAsObject(i);
                if (element != DELETE_MARKER) {
                    data[resultIndex] = element;
                    if (hasNames) {
                        newNames[resultIndex] = names.getDataAt(i);
                    }
                    resultIndex++;
                    if (isPreviousResultSpecialized() && resultIndex > resultLength) {
                        // got too many elements
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        previousResultLength = PREVIOUS_RESULT_GENERIC;
                        return deleteElements(vector, vectorLength);
                    }
                }
            }
            if (isPreviousResultSpecialized() && resultIndex != resultLength) {
                // got less elements to delete
                CompilerDirectives.transferToInterpreterAndInvalidate();
                previousResultLength = PREVIOUS_RESULT_GENERIC;
                return deleteElements(vector, vectorLength);
            }

            RStringVector newNamesVector = null;
            if (hasNames) {
                newNamesVector = RDataFactory.createStringVector(newNames, names.isComplete());
            }
            RList result = RDataFactory.createList(data, newNamesVector);
            copyAttributes(vector, result);
            return result;
        }

        private boolean isPreviousResultSpecialized() {
            return previousResultLength >= 0;
        }

        private static int calculateResultSize(RAbstractVector vector, int vectorLength) {
            int resultSize = 0;
            for (int i = 0; i < vectorLength; i++) {
                if (vector.getDataAtAsObject(i) != DELETE_MARKER) {
                    resultSize++;
                }
            }
            return resultSize;
        }
    }

}
