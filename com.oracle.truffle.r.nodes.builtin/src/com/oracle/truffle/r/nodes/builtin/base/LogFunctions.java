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
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic;

public class LogFunctions {
    @RBuiltin(name = "log", kind = PRIMITIVE, parameterNames = {"x", "base"})
    public abstract static class Log extends RBuiltinNode {

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, Math.E};
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            // // base argument is at index 1, and double
            // arguments[1] = CastDoubleNodeGen.create(arguments[1], true, false, false);
            casts.toDouble(1);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull log(RNull x, RNull base) {
            controlVisibility();
            throw RError.error(this, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION);
        }

        @Specialization
        protected double log(int x, double base) {
            controlVisibility();
            return logb(x, base);
        }

        @Specialization
        protected double log(double x, double base) {
            controlVisibility();
            return logb(x, base);
        }

        @Specialization
        protected RDoubleVector log(RIntVector vector, double base) {
            controlVisibility();
            double[] resultVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                int inputValue = vector.getDataAt(i);
                double result = RRuntime.DOUBLE_NA;
                if (!RRuntime.isNA(inputValue)) {
                    result = logb(inputValue, base);
                }
                resultVector[i] = result;
            }
            return RDataFactory.createDoubleVector(resultVector, vector.isComplete());
        }

        @Specialization
        protected RDoubleVector log(RDoubleVector vector, double base) {
            controlVisibility();
            double[] doubleVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                double value = vector.getDataAt(i);
                if (!RRuntime.isNA(value)) {
                    value = logb(value, base);
                }
                doubleVector[i] = value;
            }
            return RDataFactory.createDoubleVector(doubleVector, vector.isComplete());
        }

        private static double logb(double x, double base) {
            return Math.log(x) / Math.log(base);
        }
    }

    @RBuiltin(name = "log10", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class Log10 extends RBuiltinNode {

        @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
        @Child private UnaryArithmeticNode log10Node = UnaryArithmeticNodeGen.create(Log10Arithmetic::new, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, RType.Double);

        @Specialization
        protected Object log10(Object value) {
            return log10Node.execute(boxPrimitive.execute(value));
        }

        private static final class Log10Arithmetic extends UnaryArithmetic {

            private static final double LOG_10 = Math.log(10);

            @Override
            public int op(byte op) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int op(int op) {
                throw new UnsupportedOperationException();
            }

            @Override
            public double op(double op) {
                return Math.log10(op);
            }

            @Override
            public RComplex op(double re, double im) {
                double arg = Math.atan2(im, re);
                double mod = RComplex.abs(re, im);
                return RComplex.valueOf(Math.log10(mod), arg / LOG_10);
            }
        }
    }

    @RBuiltin(name = "log2", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class Log2 extends RBuiltinNode {

        @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
        @Child private UnaryArithmeticNode log2Node = UnaryArithmeticNodeGen.create(Log2Arithmetic::new, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, RType.Double);

        @Specialization
        protected Object log2(Object value) {
            return log2Node.execute(boxPrimitive.execute(value));
        }

        private static final class Log2Arithmetic extends UnaryArithmetic {

            private static final double LOG_2 = Math.log(2);

            @Override
            public int op(byte op) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int op(int op) {
                throw new UnsupportedOperationException();
            }

            @Override
            public double op(double op) {
                return Math.log(op) / LOG_2;
            }

            @Override
            public RComplex op(double re, double im) {
                double arg = Math.atan2(im, re);
                double mod = RComplex.abs(re, im);
                return RComplex.valueOf(Math.log(mod) / LOG_2, arg / LOG_2);
            }
        }
    }

    @RBuiltin(name = "log1p", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class Log1p extends RBuiltinNode {

        @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
        @Child private UnaryArithmeticNode log1pNode = UnaryArithmeticNodeGen.create(Log1pArithmetic::new, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, RType.Double);

        @Specialization
        protected Object log1p(Object value) {
            return log1pNode.execute(boxPrimitive.execute(value));
        }

        private static final class Log1pArithmetic extends UnaryArithmetic {

            @Override
            public int op(byte op) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int op(int op) {
                throw new UnsupportedOperationException();
            }

            @Override
            public double op(double op) {
                return Math.log(1 + op);
            }

            @Override
            public RComplex op(double r, double i) {
                double re = r + 1;
                double im = i;
                double arg = Math.atan2(im, re);
                double mod = RComplex.abs(re, im);
                return RComplex.valueOf(Math.log(mod), arg);
            }
        }
    }
}
