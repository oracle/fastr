/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.primitive.BinaryMapNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.BinaryLogic;
import com.oracle.truffle.r.runtime.ops.BinaryLogic.And;
import com.oracle.truffle.r.runtime.ops.BinaryLogic.Or;
import com.oracle.truffle.r.runtime.ops.BooleanOperation;
import com.oracle.truffle.r.runtime.ops.BooleanOperationFactory;

/**
 * Represents a binary or unary operation from the 'logical' subset of Ops R group. The concrete
 * operation is implemented by factory object given as a constructor parameter, e.g.
 * {@link com.oracle.truffle.r.runtime.ops.BinaryLogic.And}.
 */
public abstract class BinaryBooleanNode extends RBuiltinNode.Arg2 {

    protected static final int CACHE_LIMIT = 5;

    protected final BooleanOperationFactory factory;

    BinaryBooleanNode(BooleanOperationFactory factory) {
        this.factory = factory;
    }

    static {
        Casts casts = new Casts(BinaryBooleanNode.class);
        casts.arg(0).boxPrimitive();
        casts.arg(1).boxPrimitive();
    }

    @Override
    public RBaseNode getErrorContext() {
        return this;
    }

    private static boolean isLogicOp(BooleanOperation op) {
        return op instanceof And || op instanceof Or;
    }

    private static boolean isLogicOp(BooleanOperationFactory factory) {
        return factory == BinaryLogic.AND || factory == BinaryLogic.OR;
    }

    @Override
    public abstract Object execute(VirtualFrame frame, Object left, Object right);

    public static BinaryBooleanNode create(BooleanOperationFactory factory) {
        return BinaryBooleanNodeGen.create(factory);
    }

    @Specialization(limit = "CACHE_LIMIT", guards = {"cached != null", "cached.isSupported(left, right)"})
    protected Object doNumericVectorCached(RAbstractVector left, RAbstractVector right,
                    @Cached("createFastCached(left, right)") BinaryMapNode cached) {
        return cached.apply(left, right);
    }

    @Specialization(replaces = "doNumericVectorCached", guards = "isSupported(left, right)")
    @TruffleBoundary
    protected Object doNumericVectorGeneric(RAbstractVector left, RAbstractVector right,
                    @Cached("factory.createOperation()") BooleanOperation operation,
                    @Cached("createGeneric()") GenericNumericVectorNode generic) {
        return generic.get(operation, left, right).apply(left, right);
    }

    protected BinaryMapNode createFastCached(Object left, Object right) {
        if (isSupported(left, right)) {
            return createCached(factory.createOperation(), left, right, false);
        }
        return null;
    }

    protected boolean isSupported(Object left, Object right) {
        if (isLogicOp(factory) && left instanceof RAbstractRawVector && right instanceof RAbstractRawVector) {
            // for logic ops only both raw vectors are supported
            return true;
        } else if (isSupportedVector(left) && isSupportedVector(right)) {
            return true;
        }
        return false;
    }

    protected boolean isSupportedVector(Object value) {
        return value instanceof RAbstractIntVector || value instanceof RAbstractDoubleVector || value instanceof RAbstractComplexVector || value instanceof RAbstractLogicalVector ||
                        (!isLogicOp(factory) && (value instanceof RAbstractStringVector || value instanceof RAbstractRawVector));
    }

    protected static boolean isSymbolOrLang(Object obj) {
        return obj instanceof RSymbol || obj instanceof RLanguage;
    }

    @Specialization(guards = {"isSymbolOrLang(left) || isSymbolOrLang(right)"})
    protected Object doSymbol(VirtualFrame frame, Object left, Object right,
                    @Cached("createRecursive()") BinaryBooleanNode recursive) {
        Object recursiveLeft = left;
        if (isSymbolOrLang(left)) {
            recursiveLeft = deparseSymbolOrLang(left);
        }
        Object recursiveRight = right;
        if (isSymbolOrLang(right)) {
            recursiveRight = deparseSymbolOrLang(right);
        }
        return recursive.execute(frame, recursiveLeft, recursiveRight);
    }

    private static RString deparseSymbolOrLang(Object val) {
        return RString.valueOf(RDeparse.deparse(val, RDeparse.MAX_CUTOFF, false, RDeparse.KEEPINTEGER, -1));
    }

    protected static boolean isOneList(Object left, Object right) {
        return isRAbstractListVector(left) ^ isRAbstractListVector(right);
    }

    @Specialization(guards = {"isOneList(left, right)"})
    protected Object doList(VirtualFrame frame, RAbstractVector left, RAbstractVector right,
                    @Cached("create()") CastTypeNode cast,
                    @Cached("createRecursive()") BinaryBooleanNode recursive) {
        Object recursiveLeft = left;
        if (isRAbstractListVector(left)) {
            recursiveLeft = castListToAtomic((RAbstractListBaseVector) left, cast, right.getRType());
        }
        Object recursiveRight = right;
        if (isRAbstractListVector(right)) {
            recursiveRight = castListToAtomic((RAbstractListBaseVector) right, cast, left.getRType());
        }
        return recursive.execute(frame, recursiveLeft, recursiveRight);
    }

    @TruffleBoundary
    private static Object castListToAtomic(RAbstractListBaseVector source, CastTypeNode cast, RType type) {
        RVector<?> result = type.create(source.getLength(), false);
        Object store = result.getInternalStore();
        for (int i = 0; i < source.getLength(); i++) {
            Object value = source.getDataAt(i);
            if (type == RType.Character) {
                value = RDeparse.deparse(value);
                ((RStringVector) result).setDataAt(store, i, (String) value);
            } else {
                value = cast.execute(value, type);
                if (value instanceof RAbstractVector && ((RAbstractVector) value).getLength() == 1) {
                    value = ((RAbstractVector) value).getDataAtAsObject(0);
                }

                if (type == RType.Integer && value instanceof Integer) {
                    if (RRuntime.isNA((int) value)) {
                        result.setComplete(false);
                    }
                    ((RIntVector) result).setDataAt(store, i, (int) value);
                } else if (type == RType.Double && value instanceof Double) {
                    if (RRuntime.isNA((double) value)) {
                        result.setComplete(false);
                    }
                    ((RDoubleVector) result).setDataAt(store, i, (double) value);
                } else if (type == RType.Logical && value instanceof Byte) {
                    if (RRuntime.isNA((byte) value)) {
                        result.setComplete(false);
                    }
                    ((RLogicalVector) result).setDataAt(store, i, (byte) value);
                } else if (type == RType.Complex && value instanceof RComplex) {
                    if (RRuntime.isNA((RComplex) value)) {
                        result.setComplete(false);
                    }
                    ((RComplexVector) result).setDataAt(store, i, (RComplex) value);
                } else if (type == RType.Raw && value instanceof RRaw) {
                    ((RRawVector) result).setRawDataAt(store, i, ((RRaw) value).getValue());
                }
            }
        }
        return result;
    }

    protected BinaryBooleanNode createRecursive() {
        return BinaryBooleanNode.create(factory);
    }

    @Specialization(guards = {"isRNullOrEmptyAndNotMissing(left, right)"})
    protected static Object doEmptyOrNull(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") Object right) {
        return RType.Logical.getEmpty();
    }

    @Specialization(guards = {"(isRMissing(left) || isRMissing(right))"})
    protected Object doOneArg(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") Object right) {
        throw error(RError.Message.IS_OF_WRONG_ARITY, 1, factory.createOperation().opName(), 2);
    }

    protected static boolean isRNullOrEmptyAndNotMissing(Object left, Object right) {
        return (isRNullOrEmpty(left) || isRNullOrEmpty(right)) && !(isRMissing(left) || isRMissing(right));
    }

    private static boolean isRNullOrEmpty(Object value) {
        return isRNull(value) || isEmpty(value);
    }

    private static boolean isEmpty(Object value) {
        return (isRAbstractVector(value) && ((RAbstractVector) value).getLength() == 0);
    }

    @Fallback
    protected Object doInvalidType(@SuppressWarnings("unused") Object left, @SuppressWarnings("unused") Object right) {
        throw error(Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
    }

    protected static BinaryMapNode createCached(BooleanOperation operation, Object left, Object right, boolean isGeneric) {
        RAbstractVector leftVector = (RAbstractVector) left;
        RAbstractVector rightVector = (RAbstractVector) right;

        RType argumentType = RType.maxPrecedence(leftVector.getRType(), rightVector.getRType());
        RType resultType = RType.Logical;
        if (isLogicOp(operation) && argumentType == RType.Raw) {
            resultType = RType.Raw;
        } else {
            resultType = RType.Logical;
        }

        return BinaryMapNode.create(new BinaryMapBooleanFunctionNode(operation), leftVector, rightVector, argumentType, resultType, false, isGeneric);
    }

    protected static GenericNumericVectorNode createGeneric() {
        return new GenericNumericVectorNode();
    }

    protected static final class GenericNumericVectorNode extends TruffleBoundaryNode {

        @Child private BinaryMapNode cached;

        private BinaryMapNode get(BooleanOperation arithmetic, RAbstractVector left, RAbstractVector right) {
            CompilerAsserts.neverPartOfCompilation();
            BinaryMapNode map = cached;
            if (map == null || !map.isSupported(left, right)) {
                cached = map = insert(createCached(arithmetic, left, right, true));
            }
            return map;
        }
    }
}
