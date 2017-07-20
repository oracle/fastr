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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RDispatch.COMPLEX_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic;

public class NumericalFunctions {

    @RBuiltin(name = "abs", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public static final class Abs extends UnaryArithmetic {

        @Override
        public RType calculateResultType(RType argumentType) {
            return argumentType == RType.Complex ? RType.Double : argumentType;
        }

        @Override
        public RType getMinPrecedence() {
            return RType.Integer;
        }

        @Override
        public int op(byte op) {
            return Math.abs(op);
        }

        @Override
        public int op(int op) {
            return Math.abs(op);
        }

        @Override
        public double op(double op) {
            return Math.abs(op);
        }

        @Override
        public double opd(double re, double im) {
            return RComplex.abs(re, im);
        }
    }

    @RBuiltin(name = "Re", kind = PRIMITIVE, parameterNames = {"z"}, dispatch = COMPLEX_GROUP_GENERIC, behavior = PURE)
    public static final class Re extends UnaryArithmetic {

        @Override
        public RType calculateResultType(RType argumentType) {
            return argumentType == RType.Complex ? RType.Double : argumentType;
        }

        @Override
        public Message getArgumentError() {
            return RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION;
        }

        @Override
        public int op(byte op) {
            return op;
        }

        @Override
        public int op(int op) {
            return op;
        }

        @Override
        public double op(double op) {
            return op;
        }

        @Override
        public double opd(double re, double im) {
            return re;
        }
    }

    @RBuiltin(name = "Im", kind = PRIMITIVE, parameterNames = {"z"}, dispatch = COMPLEX_GROUP_GENERIC, behavior = PURE)
    public static final class Im extends UnaryArithmetic {

        @Override
        public RType calculateResultType(RType argumentType) {
            return argumentType == RType.Complex ? RType.Double : argumentType;
        }

        @Override
        public Message getArgumentError() {
            return RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION;
        }

        @Override
        public int op(byte op) {
            return 0;
        }

        @Override
        public int op(int op) {
            return 0;
        }

        @Override
        public double op(double op) {
            return 0;
        }

        @Override
        public double opd(double re, double im) {
            return im;
        }
    }

    @RBuiltin(name = "Conj", kind = PRIMITIVE, parameterNames = {"z"}, dispatch = COMPLEX_GROUP_GENERIC, behavior = PURE)
    public static final class Conj extends UnaryArithmetic {

        @Override
        public Message getArgumentError() {
            return RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION;
        }

        @Override
        public int op(byte op) {
            return op;
        }

        @Override
        public int op(int op) {
            return op;
        }

        @Override
        public double op(double op) {
            return op;
        }

        @Override
        public RComplex op(double re, double im) {
            return RComplex.valueOf(re, -im);
        }
    }

    @RBuiltin(name = "Mod", kind = PRIMITIVE, parameterNames = {"z"}, dispatch = COMPLEX_GROUP_GENERIC, behavior = PURE)
    public static final class Mod extends UnaryArithmetic {

        @Override
        public RType calculateResultType(RType argumentType) {
            return argumentType == RType.Complex ? RType.Double : argumentType;
        }

        @Override
        public Message getArgumentError() {
            return RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION;
        }

        @Override
        public int op(byte op) {
            return op;
        }

        @Override
        public int op(int op) {
            return op;
        }

        @Override
        public double op(double op) {
            return op;
        }

        @Override
        public double opd(double re, double im) {
            return RComplex.abs(re, im);
        }
    }

    @RBuiltin(name = "Arg", kind = PRIMITIVE, parameterNames = {"z"}, dispatch = COMPLEX_GROUP_GENERIC, behavior = PURE)
    public static final class Arg extends UnaryArithmetic {

        @Override
        public RType calculateResultType(RType argumentType) {
            return argumentType == RType.Complex ? RType.Double : argumentType;
        }

        @Override
        public int op(byte op) {
            return 0;
        }

        @Override
        public int op(int op) {
            return op;
        }

        @Override
        public double op(double op) {
            if (op >= 0) {
                return 0;
            } else {
                return Math.PI;
            }
        }

        @Override
        public double opd(double re, double im) {
            return Math.atan2(im, re);
        }
    }

    @RBuiltin(name = "sign", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public static final class Sign extends UnaryArithmetic {

        @Override
        public int op(byte op) {
            return op == RRuntime.LOGICAL_TRUE ? 1 : 0;
        }

        @Override
        public int op(int op) {
            return op == 0 ? 0 : (op > 0 ? 1 : -1);
        }

        @Override
        public double op(double op) {
            return Math.signum(op);
        }

        @Override
        public RComplex op(double re, double im) {
            throw error(Message.UNIMPLEMENTED_COMPLEX_FUN);
        }
    }

    @RBuiltin(name = "sqrt", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public static final class Sqrt extends UnaryArithmetic {

        @Override
        public int op(byte op) {
            return op;
        }

        @Override
        public int op(int op) {
            return (int) Math.sqrt(op);
        }

        @Override
        public double op(double op) {
            return Math.sqrt(op);
        }

        @Override
        public RComplex op(double re, double im) {
            double r = Math.sqrt(Math.sqrt(re * re + im * im));
            double theta = Math.atan2(im, re) / 2;
            return RComplex.valueOf(r * Math.cos(theta), r * Math.sin(theta));
        }
    }
}
