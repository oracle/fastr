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
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
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
    protected Object doNumericVectorCached(Object left, Object right,
                    @Cached("createFastCached(left, right)") BinaryMapNode cached) {
        return cached.apply(left, right);
    }

    @Specialization(replaces = "doNumericVectorCached", guards = "isSupported(left, right)")
    @TruffleBoundary
    protected Object doNumericVectorGeneric(Object left, Object right,
                    @Cached("factory.createOperation()") BooleanOperation operation,
                    @Cached("new(createCached(operation, left, right))") GenericNumericVectorNode generic) {
        RAbstractVector leftVector = (RAbstractVector) left;
        RAbstractVector rightVector = (RAbstractVector) right;
        return generic.get(operation, leftVector, rightVector).apply(leftVector, rightVector);
    }

    protected BinaryMapNode createFastCached(Object left, Object right) {
        if (isSupported(left, right)) {
            return createCached(factory.createOperation(), left, right);
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
        return RString.valueOf(RDeparse.deparse(val, RDeparse.MAX_Cutoff, false, RDeparse.KEEPINTEGER, -1));
    }

    protected BinaryBooleanNode createRecursive() {
        return BinaryBooleanNode.create(factory);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isRNullOrEmptyAndNotMissing(left, right)"})
    protected static Object doEmptyOrNull(Object left, Object right) {
        return RType.Logical.getEmpty();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"(isRMissing(left) || isRMissing(right))"})
    protected Object doOneArg(Object left, Object right) {
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

    @SuppressWarnings("unused")
    @Fallback
    protected Object doInvalidType(Object left, Object right) {
        throw error(Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
    }

    protected static BinaryMapNode createCached(BooleanOperation operation, Object left, Object right) {
        RAbstractVector leftVector = (RAbstractVector) left;
        RAbstractVector rightVector = (RAbstractVector) right;

        RType argumentType = RType.maxPrecedence(leftVector.getRType(), rightVector.getRType());
        RType resultType = RType.Logical;
        if (isLogicOp(operation) && argumentType == RType.Raw) {
            resultType = RType.Raw;
        } else {
            resultType = RType.Logical;
        }

        return BinaryMapNode.create(new BinaryMapBooleanFunctionNode(operation), leftVector, rightVector, argumentType, resultType, false);
    }

    protected static final class GenericNumericVectorNode extends TruffleBoundaryNode {

        @Child private BinaryMapNode cached;

        public GenericNumericVectorNode(BinaryMapNode cachedOperation) {
            this.cached = insert(cachedOperation);
        }

        private BinaryMapNode get(BooleanOperation arithmetic, RAbstractVector left, RAbstractVector right) {
            CompilerAsserts.neverPartOfCompilation();
            if (!cached.isSupported(left, right)) {
                cached = cached.replace(createCached(arithmetic, left, right));
            }
            return cached;
        }
    }
}
