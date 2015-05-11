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

import sun.security.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Encapsulates the logic required to fully traverse two vectors on a given result array. If one of
 * the vectors is not big enough the other one will be traversed repeatedly. It uses a primitive
 * array as store to ensure that no field lookups are visible during the traversal. The
 * implementation also ensures that there is no internal boxing inside the loop.
 */
@SuppressWarnings("unused")
abstract class VectorMapBinaryNode extends Node {

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
    private final RType argumentType;
    private final RType resultType;

    @SuppressWarnings("unchecked")
    protected VectorMapBinaryNode(RType resultType, RType argumentType) {
        this.indexedAction = (MapBinaryIndexedAction<Object, RAbstractVector>) createIndexedAction(resultType, argumentType);
        this.argumentType = argumentType;
        this.resultType = resultType;
    }

    public RType getArgumentType() {
        return argumentType;
    }

    public RType getResultType() {
        return resultType;
    }

    public static VectorMapBinaryNode create(RType resultType, RType argumentType) {
        return VectorMapBinaryNodeGen.create(resultType, argumentType);
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

    public final void apply(ScalarBinaryNode scalarAction, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        assert left.getLength() == leftLength;
        assert right.getLength() == rightLength;
        assert left.getRType() == argumentType;
        assert right.getRType() == argumentType;
        assert isStoreCompatible(store, resultType, leftLength, rightLength);

        executeInternal(scalarAction, store, left, leftLength, right, rightLength);
    }

    protected static boolean isStoreCompatible(Object store, RType resultType, int leftLength, int rightLength) {
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

    protected abstract void executeInternal(ScalarBinaryNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength);

    @Specialization(guards = {"leftLength == 1", "rightLength == 1"})
    protected void doScalarScalar(ScalarBinaryNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        indexedAction.perform(node, store, 0, left, 0, right, 0);
    }

    @Specialization(contains = "doScalarScalar", guards = {"leftLength == 1"})
    protected void doScalarVector(ScalarBinaryNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        for (int i = 0; i < rightLength; ++i) {
            indexedAction.perform(node, store, i, left, 0, right, i);
        }
    }

    @Specialization(contains = "doScalarScalar", guards = {"rightLength == 1"})
    protected void doVectorScalar(ScalarBinaryNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        for (int i = 0; i < leftLength; ++i) {
            indexedAction.perform(node, store, i, left, i, right, 0);
        }
    }

    @Specialization(guards = {"leftLength == rightLength"})
    protected void doSameLength(ScalarBinaryNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        for (int i = 0; i < leftLength; ++i) {
            indexedAction.perform(node, store, i, left, i, right, i);
        }
    }

    protected static boolean multiplesMinMax(int min, int max) {
        return max % min == 0;
    }

    @Specialization(contains = {"doVectorScalar", "doScalarVector", "doSameLength"}, guards = {"multiplesMinMax(leftLength, rightLength)"})
    protected void doMultiplesLeft(ScalarBinaryNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        int j = 0;
        while (j < rightLength) {
            for (int k = 0; k < leftLength; k++) {
                indexedAction.perform(node, store, j, left, k, right, j);
                j++;
            }
        }
    }

    @Specialization(contains = {"doVectorScalar", "doScalarVector", "doSameLength"}, guards = {"multiplesMinMax(rightLength, leftLength)"})
    protected void doMultiplesRight(ScalarBinaryNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        int j = 0;
        while (j < leftLength) {
            for (int k = 0; k < rightLength; k++) {
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
    protected void doNoMultiples(ScalarBinaryNode node, Object store, RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        int j = 0;
        int k = 0;
        int max = Math.max(leftLength, rightLength);
        for (int i = 0; i < max; ++i) {
            indexedAction.perform(node, store, i, left, j, right, k);
            j = Utils.incMod(j, leftLength);
            k = Utils.incMod(k, rightLength);
        }
        RError.warning(getEncapsulatingSourceSection(), RError.Message.LENGTH_NOT_MULTI);
    }

    private interface MapBinaryIndexedAction<A, V extends RAbstractVector> {

        void perform(ScalarBinaryNode action, A store, int resultIndex, V left, int leftIndex, V right, int rightIndex);

    }

}
