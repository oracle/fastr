/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ops;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class BinaryLogic extends BooleanOperation {

    /* Fake RBuiltins to unify the binary operations */

    @RBuiltin(name = "&&", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"", ""})
    public static class NonVectorAndBuiltin {
    }

    @RBuiltin(name = "||", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"", ""})
    public static class NonVectorOrBuiltin {
    }

    @RBuiltin(name = "&", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"", ""})
    public static class AndBuiltin {
    }

    @RBuiltin(name = "|", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"", ""})
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

    public BinaryLogic() {
        super(true, true);
    }

    public static final class And extends BinaryLogic {

        private final String opName;

        public And(String opName) {
            this.opName = opName;
        }

        @Override
        public String opName() {
            return opName;
        }

        @Override
        public boolean requiresRightOperand(byte leftOperand) {
            if (leftOperand == RRuntime.LOGICAL_TRUE) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean opLogical(byte left, byte right) {
            assert RLogical.isValid(left) && !RRuntime.isNA(left);
            assert RLogical.isValid(right) && !RRuntime.isNA(right);
            return (left & right) == 0x1;
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
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            return !left.isZero() && !right.isZero();
        }

        @Override
        public RRaw op(RRaw left, RRaw right) {
            return RDataFactory.createRaw((byte) (left.getValue() & right.getValue()));
        }

        @Override
        public boolean op(RRaw left, Object right) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
        }

        @Override
        public boolean op(Object left, RRaw right) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
        }

    }

    public static final class Or extends BinaryLogic {

        private final String opName;

        public Or(String opName) {
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
            assert RLogical.isValid(left);
            assert RLogical.isValid(right);
            return (left | right) == 0x1;
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
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            return !left.isZero() || !right.isZero();
        }

        @Override
        public RRaw op(RRaw left, RRaw right) {
            return RDataFactory.createRaw((byte) (left.getValue() | right.getValue()));
        }

        @Override
        public boolean op(RRaw left, Object right) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
        }

        @Override
        public boolean op(Object left, RRaw right) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OPERATIONS_NUMERIC_LOGICAL_COMPLEX);
        }

    }
}
