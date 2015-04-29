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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@SuppressWarnings("unused")
public abstract class VectorMapUnaryNode extends Node {

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
    private static final MapIndexedAction<String[], RAbstractStringVector> CHARACTER = //
    (arithmetic, result, resultIndex, left, leftIndex) -> {
        result[resultIndex] = arithmetic.applyCharacter(left.getDataAt(leftIndex));
    };

    private final MapIndexedAction<Object, RAbstractVector> indexedAction;
    private final RType argumentType;
    private final RType resultType;

    @SuppressWarnings("unchecked")
    protected VectorMapUnaryNode(RType resultType, RType argumentType) {
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

    public static VectorMapUnaryNode create(RType resultType, RType argumentType) {
        return VectorMapUnaryNodeGen.create(resultType, argumentType);
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
                return COMPLEX;
            case Character:
                return CHARACTER;
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    public final void apply(ScalarUnaryNode scalarAction, Object store, RAbstractVector operand, int operandLength) {
        assert operand.getLength() == operandLength;
        assert operand.getRType() == argumentType;
        assert isStoreCompatible(store, resultType, operandLength);

        executeInternal(scalarAction, store, operand, operandLength);
    }

    protected static boolean isStoreCompatible(Object store, RType resultType, int operandLength) {
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

    protected abstract void executeInternal(ScalarUnaryNode node, Object store, RAbstractVector operand, int operandLength);

    @Specialization(guards = {"operandLength == 1"})
    protected void doScalar(ScalarUnaryNode node, Object store, RAbstractVector operand, int operandLength) {
        indexedAction.perform(node, store, 0, operand, 0);
    }

    @Specialization(contains = "doScalar")
    protected void doScalarVector(ScalarUnaryNode node, Object store, RAbstractVector operand, int operandLength) {
        for (int i = 0; i < operandLength; ++i) {
            indexedAction.perform(node, store, i, operand, i);
        }
    }

    private interface MapIndexedAction<A, V extends RAbstractVector> {

        void perform(ScalarUnaryNode action, A store, int resultIndex, V operand, int operandIndex);

    }

}
