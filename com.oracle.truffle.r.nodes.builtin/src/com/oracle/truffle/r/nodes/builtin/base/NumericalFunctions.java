/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;

public class NumericalFunctions {

    /**
     * This node is only a workaround that makes the annotation processor process the other inner
     * node classes. These classes would be ignored otherwise, since they do not contain any
     * specialization, which would trigger the code generation performed by the annotation
     * processor.
     */
    public abstract static class DummyNode extends RBuiltinNode {

        @Specialization
        protected Object dummySpec(@SuppressWarnings("unused") Object value) {
            return null;
        }

    }

    @RBuiltin(name = "abs", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class Abs extends UnaryArithmeticBuiltinNode {

        public Abs() {
            super(RType.Integer, RError.Message.NON_NUMERIC_MATH, null);
        }

        @Override
        public RType calculateResultType(RType argumentType) {
            switch (argumentType) {
                case Complex:
                    return RType.Double;
                default:
                    return super.calculateResultType(argumentType);
            }
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

    @RBuiltin(name = "Re", kind = PRIMITIVE, parameterNames = {"z"})
    public abstract static class Re extends UnaryArithmeticBuiltinNode {

        public Re() {
            super(RType.Double, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, null);
        }

        @Override
        public RType calculateResultType(RType argumentType) {
            switch (argumentType) {
                case Complex:
                    return RType.Double;
                default:
                    return super.calculateResultType(argumentType);
            }
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

    @RBuiltin(name = "Im", kind = PRIMITIVE, parameterNames = {"z"})
    public abstract static class Im extends UnaryArithmeticBuiltinNode {

        public Im() {
            super(RType.Double, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, null);
        }

        @Override
        public RType calculateResultType(RType argumentType) {
            switch (argumentType) {
                case Complex:
                    return RType.Double;
                default:
                    return super.calculateResultType(argumentType);
            }
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

    @RBuiltin(name = "Conj", kind = PRIMITIVE, parameterNames = {"z"})
    public abstract static class Conj extends UnaryArithmeticBuiltinNode {

        public Conj() {
            super(RType.Double, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, null);
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

    @RBuiltin(name = "Mod", kind = PRIMITIVE, parameterNames = {"z"})
    public abstract static class Mod extends UnaryArithmeticBuiltinNode {

        public Mod() {
            super(RType.Double, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, null);
        }

        @Override
        public RType calculateResultType(RType argumentType) {
            switch (argumentType) {
                case Complex:
                    return RType.Double;
                default:
                    return super.calculateResultType(argumentType);
            }
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

    @RBuiltin(name = "sign", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Sign extends UnaryArithmeticBuiltinNode {

        public Sign() {
            super(RType.Logical);
        }

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

    }

    @RBuiltin(name = "sqrt", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class Sqrt extends UnaryArithmeticBuiltinNode {

        public Sqrt() {
            super(RType.Double, RError.Message.NON_NUMERIC_MATH, null);
        }

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
