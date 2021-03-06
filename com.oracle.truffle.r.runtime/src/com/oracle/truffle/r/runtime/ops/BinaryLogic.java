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
package com.oracle.truffle.r.runtime.ops;

import static com.oracle.truffle.r.runtime.RDispatch.OPS_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_ARITHMETIC;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;

/**
 * All methods must be invoked with non-NA values.
 */
public abstract class BinaryLogic extends BooleanOperation {

    /* Fake RBuiltins to unify the binary operations */

    @RBuiltin(name = "&&", kind = PRIMITIVE, parameterNames = {"", ""}, nonEvalArgs = {1}, behavior = COMPLEX)
    public static class NonVectorAndBuiltin {
    }

    @RBuiltin(name = "||", kind = PRIMITIVE, parameterNames = {"", ""}, nonEvalArgs = {1}, behavior = COMPLEX)
    public static class NonVectorOrBuiltin {
    }

    @RBuiltin(name = "&", kind = PRIMITIVE, parameterNames = {"", ""}, dispatch = OPS_GROUP_GENERIC, behavior = PURE_ARITHMETIC)
    public static class AndBuiltin {
    }

    @RBuiltin(name = "|", kind = PRIMITIVE, parameterNames = {"", ""}, dispatch = OPS_GROUP_GENERIC, behavior = PURE_ARITHMETIC)
    public static class OrBuiltin {
    }

    public static final BooleanOperationFactory NON_VECTOR_AND = BinaryLogic::createNonVectorAnd;
    public static final BooleanOperationFactory NON_VECTOR_OR = BinaryLogic::createNonVectorOr;
    public static final BooleanOperationFactory AND = BinaryLogic::createAnd;
    public static final BooleanOperationFactory OR = BinaryLogic::createOr;

    public static final BooleanOperationFactory[] ALL = new BooleanOperationFactory[]{AND, OR};

    private static BooleanOperation createAnd() {
        return new And("&");
    }

    private static BooleanOperation createOr() {
        return new Or("|");
    }

    private static BooleanOperation createNonVectorAnd() {
        return new And("&&");
    }

    private static BooleanOperation createNonVectorOr() {
        return new Or("||");
    }

    private BinaryLogic() {
        super(true, true);
    }

    public static final class And extends BinaryLogic {

        private final String opName;

        private And(String opName) {
            this.opName = opName;
        }

        @Override
        public String opName() {
            return opName;
        }

        @Override
        public boolean requiresRightOperand(byte leftOperand) {
            return leftOperand == RRuntime.LOGICAL_TRUE;
        }

        @Override
        public boolean opLogical(byte left, byte right) {
            assert RRuntime.isValidLogical(left) && !RRuntime.isNA(left);
            assert RRuntime.isValidLogical(right) && !RRuntime.isNA(right);
            return (left & right) != 0x0;
        }

        @Override
        public byte opRaw(byte left, byte right) {
            return (byte) (left & right);
        }

        @Override
        public boolean op(int left, int right) {
            return left != 0 && right != 0;
        }

        @Override
        public boolean op(double left, double right) {
            return left != 0.0 && right != 0.0;
        }

        @Override
        public boolean op(String left, String right) {
            throw error(RError.Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            return !left.isZero() && !right.isZero();
        }
    }

    public static final class Or extends BinaryLogic {

        private final String opName;

        private Or(String opName) {
            this.opName = opName;
        }

        @Override
        public String opName() {
            return opName;
        }

        @Override
        public boolean requiresRightOperand(byte leftOperand) {
            if (leftOperand == RRuntime.LOGICAL_FALSE) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean opLogical(byte left, byte right) {
            assert RRuntime.isValidLogical(left);
            assert RRuntime.isValidLogical(right);
            return (left | right) != 0x0;
        }

        @Override
        public byte opRaw(byte left, byte right) {
            return (byte) (left | right);
        }

        @Override
        public boolean op(int left, int right) {
            return left != 0 || right != 0;
        }

        @Override
        public boolean op(double left, double right) {
            return left != 0.0 || right != 0.0;
        }

        @Override
        public boolean op(String left, String right) {
            throw error(RError.Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            return !left.isZero() || !right.isZero();
        }
    }
}
