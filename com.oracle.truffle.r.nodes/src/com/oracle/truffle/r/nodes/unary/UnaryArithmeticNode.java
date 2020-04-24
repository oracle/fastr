/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.primitive.UnaryMapNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.unary.UnaryNode;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic;
import com.oracle.truffle.r.runtime.ops.UnaryArithmeticFactory;

public abstract class UnaryArithmeticNode extends UnaryNode {

    protected final UnaryArithmeticFactory unary;

    public UnaryArithmeticNode(UnaryArithmeticFactory factory) {
        this.unary = factory;
    }

    public abstract Object execute(Object value);

    @Specialization(guards = {"cachedNode != null", "cachedNode.isSupported(operand)"})
    protected Object doCached(RAbstractVector operand,
                    @Cached("createCachedFast(operand)") UnaryMapNode cachedNode) {
        return cachedNode.apply(operand);
    }

    protected UnaryMapNode createCachedFast(RAbstractVector operand) {
        if (isNumericVector(operand)) {
            return createCached(unary.createOperation(), operand, false);
        }
        return null;
    }

    protected static UnaryMapNode createCached(UnaryArithmetic arithmetic, Object operand, boolean isGeneric) {
        if (operand instanceof RAbstractVector) {
            RAbstractVector castOperand = (RAbstractVector) operand;
            RType operandType = castOperand.getRType();
            if (operandType.isNumeric()) {
                RType type = RType.maxPrecedence(operandType, arithmetic.getMinPrecedence());
                RType resultType = arithmetic.calculateResultType(type);
                return UnaryMapNode.create(new ScalarUnaryArithmeticNode(arithmetic), castOperand, type, resultType, isGeneric);
            }
        }
        return null;
    }

    protected static boolean isNumericVector(RAbstractVector value) {
        return value instanceof RIntVector || value instanceof RDoubleVector || value instanceof RComplexVector || value instanceof RLogicalVector;
    }

    @Specialization(replaces = "doCached", guards = {"isNumericVector(operand)"})
    @TruffleBoundary
    protected Object doGeneric(RAbstractVector operand,
                    @Cached("unary.createOperation()") UnaryArithmetic arithmetic,
                    @Cached("createGeneric()") GenericNumericVectorNode generic) {
        return generic.get(arithmetic, operand).apply(operand);
    }

    protected static GenericNumericVectorNode createGeneric() {
        return new GenericNumericVectorNode();
    }

    @Override
    public RBaseNode getErrorContext() {
        return this;
    }

    @Fallback
    protected Object invalidArgType(Object operand) {
        CompilerDirectives.transferToInterpreter();
        UnaryArithmetic op = unary.createOperation();
        if (operand instanceof RMissing) {
            throw error(RError.Message.ARGUMENTS_PASSED, 0, "'" + op.getClass().getSimpleName().toLowerCase() + "'", 1);
        } else {
            throw error(op.getArgumentError());
        }
    }

    protected static final class GenericNumericVectorNode extends TruffleBoundaryNode {

        @Child private UnaryMapNode cached;

        public UnaryMapNode get(UnaryArithmetic arithmetic, RAbstractVector operand) {
            UnaryMapNode map = cached;
            if (map == null || !map.isSupported(operand)) {
                cached = map = insert(createCached(arithmetic, operand, true));
            }
            return map;
        }
    }
}
