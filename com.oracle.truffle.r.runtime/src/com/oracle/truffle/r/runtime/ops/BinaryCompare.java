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
package com.oracle.truffle.r.runtime.ops;

import static com.oracle.truffle.r.runtime.RDispatch.OPS_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;

/**
 * All methods must be invoked with non-NA values.
 */
public abstract class BinaryCompare extends BooleanOperation {

    /* Fake RBuiltins to unify the compare operations */
    @RBuiltin(name = "==", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class EqualBuiltin {
    }

    @RBuiltin(name = "!=", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class NotEqualBuiltin {
    }

    @RBuiltin(name = ">=", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class GreaterEqualBuiltin {
    }

    @RBuiltin(name = ">", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class GreaterBuiltin {
    }

    @RBuiltin(name = "<=", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class LessEqualBuiltin {
    }

    @RBuiltin(name = "<", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class LessBuiltin {
    }

    public static final BooleanOperationFactory NOT_EQUAL = NotEqual::new;
    public static final BooleanOperationFactory EQUAL = Equal::new;
    public static final BooleanOperationFactory GREATER_EQUAL = GreaterEqual::new;
    public static final BooleanOperationFactory GREATER_THAN = GreaterThan::new;
    public static final BooleanOperationFactory LESS_EQUAL = LessEqual::new;
    public static final BooleanOperationFactory LESS_THAN = LessThan::new;

    public static final BooleanOperationFactory[] ALL = new BooleanOperationFactory[]{NOT_EQUAL, EQUAL, GREATER_EQUAL, GREATER_THAN, LESS_EQUAL, LESS_THAN};

    private BinaryCompare(boolean commutative) {
        super(commutative, false);
    }

    private static final class NotEqual extends BinaryCompare {

        NotEqual() {
            super(true);
        }

        @Override
        public String opName() {
            return "!=";
        }

        @Override
        public boolean op(int left, int right) {
            return left != right;
        }

        @Override
        public boolean op(double left, double right) {
            return left != right;
        }

        @Override
        public boolean op(String left, String right) {
            return !left.equals(right);
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            return !left.equals(right);
        }
    }

    private static final class Equal extends BinaryCompare {

        Equal() {
            super(true);
        }

        @Override
        public String opName() {
            return "==";
        }

        @Override
        public boolean op(int left, int right) {
            return left == right;
        }

        @Override
        public boolean op(double left, double right) {
            return left == right;
        }

        @Override
        public boolean op(String left, String right) {
            return left.equals(right);
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            return left.equals(right);
        }
    }

    private static final class GreaterEqual extends BinaryCompare {

        GreaterEqual() {
            super(false);
        }

        @Override
        public String opName() {
            return ">=";
        }

        @Override
        public boolean op(int left, int right) {
            return left >= right;
        }

        @Override
        public boolean op(double left, double right) {
            return left >= right;
        }

        @Override
        public boolean op(String left, String right) {
            return left.compareTo(right) >= 0;
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            throw error(RError.Message.COMPARISON_COMPLEX);
        }
    }

    private static final class GreaterThan extends BinaryCompare {

        GreaterThan() {
            super(false);
        }

        @Override
        public String opName() {
            return ">";
        }

        @Override
        public boolean op(int left, int right) {
            return left > right;
        }

        @Override
        public boolean op(double left, double right) {
            return left > right;
        }

        @Override
        public boolean op(String left, String right) {
            return left.compareTo(right) > 0;
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            throw error(RError.Message.COMPARISON_COMPLEX);
        }
    }

    private static final class LessEqual extends BinaryCompare {

        LessEqual() {
            super(false);
        }

        @Override
        public String opName() {
            return "<=";
        }

        @Override
        public boolean op(int left, int right) {
            return left <= right;
        }

        @Override
        public boolean op(double left, double right) {
            return left <= right;
        }

        @Override
        public boolean op(String left, String right) {
            return left.compareTo(right) <= 0;
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            throw error(RError.Message.COMPARISON_COMPLEX);
        }
    }

    private static final class LessThan extends BinaryCompare {

        LessThan() {
            super(false);
        }

        @Override
        public String opName() {
            return "<";
        }

        @Override
        public boolean op(int left, int right) {
            return left < right;
        }

        @Override
        public boolean op(double left, double right) {
            return left < right;
        }

        @Override
        public boolean op(String left, String right) {
            /*
             * TODO: On GnuR the comparison of strings depends on the ICU collate setting. If ICU is
             * not in use the "1" < "-1" returns FALSE, if set to "root" as on Mac it returns TRUE.
             * The Collator class should be used when we implement the ICU support.
             */
            return left.compareTo(right) < 0;
        }

        @Override
        public boolean op(RComplex left, RComplex right) {
            throw error(RError.Message.COMPARISON_COMPLEX);
        }
    }
}
