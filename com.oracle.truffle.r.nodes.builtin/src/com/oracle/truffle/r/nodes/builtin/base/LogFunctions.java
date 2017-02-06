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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

public class LogFunctions {
    @RBuiltin(name = "log", kind = PRIMITIVE, parameterNames = {"x", "base"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class Log extends RBuiltinNode {

        private final NAProfile naProfile = NAProfile.create();
        private final BranchProfile nanProfile = BranchProfile.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, Math.E};
        }

        static {
            Casts casts = new Casts(Log.class);
            casts.arg("x").defaultError(RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION).mustBe(numericValue().or(complexValue()));
            casts.arg("base").defaultError(RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION).mustBe(numericValue()).asDoubleVector().findFirst();
        }

        @Specialization
        protected double log(int x, double base) {
            return logb(x, base);
        }

        @Specialization
        protected double log(double x, double base) {
            return logb(x, base);
        }

        @Specialization
        protected RDoubleVector log(RAbstractIntVector vector, double base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode) {
            double[] resultVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                int inputValue = vector.getDataAt(i);
                double result = RRuntime.DOUBLE_NA;
                if (!naProfile.isNA(inputValue)) {
                    result = logb(inputValue, base);
                }
                resultVector[i] = result;
            }
            return createResult(vector, resultVector, base, copyAttrsNode, getNamesNode, getDimsNode);
        }

        @Specialization
        protected RDoubleVector log(RAbstractDoubleVector vector, double base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode) {
            double[] doubleVector = new double[vector.getLength()];
            for (int i = 0; i < vector.getLength(); i++) {
                double value = vector.getDataAt(i);
                if (!RRuntime.isNA(value)) {
                    value = logb(value, base);
                }
                doubleVector[i] = value;
            }
            return createResult(vector, doubleVector, base, copyAttrsNode, getNamesNode, getDimsNode);
        }

        private double logb(double x, double base) {
            if (naProfile.isNA(base)) {
                return RRuntime.DOUBLE_NA;
            }

            if (Double.isNaN(base)) {
                nanProfile.enter();
                return base;
            }

            return Math.log(x) / Math.log(base);
        }

        private static RDoubleVector createResult(RAbstractVector source, double[] resultData, double base, CopyOfRegAttributesNode copyAttrsNode, GetNamesAttributeNode getNamesNode,
                        GetDimAttributeNode getDimsNode) {
            RDoubleVector result = RDataFactory.createDoubleVector(resultData, source.isComplete() && !RRuntime.isNA(base), getDimsNode.getDimensions(source), getNamesNode.getNames(source));
            copyAttrsNode.execute(source, result);
            return result;
        }
    }

    @RBuiltin(name = "log10", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class Log10 extends UnaryArithmeticBuiltinNode {

        public Log10() {
            super(RType.Double, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, null);
        }

        private static final double LOG_10 = Math.log(10);

        static {
            Casts casts = new Casts(Log10.class);
            casts.arg("x").defaultError(RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION).mustBe(numericValue().or(complexValue()));
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

    @RBuiltin(name = "log2", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class Log2 extends UnaryArithmeticBuiltinNode {

        public Log2() {
            super(RType.Double, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, null);
        }

        private static final double LOG_2 = Math.log(2);

        static {
            Casts casts = new Casts(Log2.class);
            casts.arg("x").defaultError(RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION).mustBe(numericValue().or(complexValue()));
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

    @RBuiltin(name = "log1p", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class Log1p extends UnaryArithmeticBuiltinNode {

        public Log1p() {
            super(RType.Double, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, null);
        }

        static {
            Casts casts = new Casts(Log1p.class);
            casts.arg("x").defaultError(RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION).mustBe(numericValue().or(complexValue()));
        }

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
