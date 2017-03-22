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
import com.oracle.truffle.r.nodes.binary.BinaryMapArithmeticFunctionNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;
import java.util.Arrays;
import java.util.function.Function;

public class LogFunctions {
    @RBuiltin(name = "log", kind = PRIMITIVE, parameterNames = {"x", "base"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class Log extends RBuiltinNode {

        private final NAProfile naX = NAProfile.create();
        private final BranchProfile nanProfile = BranchProfile.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance, Math.E};
        }

        static {
            Casts casts = new Casts(Log.class);
            casts.arg("x").defaultError(RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION).mustBe(numericValue().or(complexValue()));
            casts.arg("base").defaultError(RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION).mustBe(numericValue().or(complexValue())).mapIf(numericValue(), Predef.asDoubleVector(),
                            Predef.asComplexVector()).asVector().findFirst();
        }

        static BinaryMapArithmeticFunctionNode createDivNode() {
            return new BinaryMapArithmeticFunctionNode(BinaryArithmetic.DIV.createOperation());
        }

        @Specialization
        protected double log(byte x, double base,
                        @Cached("create()") NAProfile naBase) {
            if (naX.isNA(x)) {
                return RRuntime.DOUBLE_NA;
            }
            return logb(x, base, naBase);
        }

        @Specialization
        protected double log(int x, double base,
                        @Cached("create()") NAProfile naBase) {
            if (naX.isNA(x)) {
                return RRuntime.DOUBLE_NA;
            }
            return logb(x, base, naBase);
        }

        @Specialization
        protected double log(double x, double base,
                        @Cached("create()") NAProfile naBase) {
            if (naX.isNA(x)) {
                return RRuntime.DOUBLE_NA;
            }
            return logb(x, base, naBase);
        }

        @Specialization
        protected RComplex log(RComplex x, double base,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NAProfile naBase) {
            if (naX.isNA(x)) {
                return x;
            }
            return logb(x, RComplex.valueOf(base, 0), divNode, naBase);
        }

        @Specialization
        protected RComplex log(byte x, RComplex base,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NAProfile naBase) {
            if (naX.isNA(x)) {
                return RRuntime.createComplexNA();
            }
            return logb(RComplex.valueOf(x, 0), base, divNode, naBase);
        }

        @Specialization
        protected RComplex log(int x, RComplex base,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NAProfile naBase) {
            if (naX.isNA(x)) {
                return RRuntime.createComplexNA();
            }
            return logb(RComplex.valueOf(x, 0), base, divNode, naBase);
        }

        @Specialization
        protected RComplex log(double x, RComplex base,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NAProfile naBase) {
            if (naX.isNA(x)) {
                return RRuntime.createComplexNA();
            }
            return logb(RComplex.valueOf(x, 0), base, divNode, naBase);
        }

        @Specialization
        protected RComplex log(RComplex x, RComplex base,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NAProfile naBase) {
            if (naX.isNA(x)) {
                return RRuntime.createComplexNA();
            }
            return logb(x, base, divNode, naBase);
        }

        @Specialization
        protected RDoubleVector log(RAbstractIntVector vector, double base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("create()") NACheck xNACheck,
                        @Cached("create()") NACheck baseNACheck) {
            return log(vector, base, index -> xNACheck.convertIntToDouble(vector.getDataAt(index)), copyAttrsNode, getNamesNode, getDimsNode, xNACheck, baseNACheck);
        }

        @Specialization
        protected RDoubleVector log(RAbstractDoubleVector vector, double base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("create()") NACheck xNACheck,
                        @Cached("create()") NACheck baseNACheck) {
            return log(vector, base, index -> checkDouble(vector.getDataAt(index), xNACheck), copyAttrsNode, getNamesNode, getDimsNode, xNACheck, baseNACheck);
        }

        private double checkDouble(double d, NACheck na) {
            na.check(d);
            return d;
        }

        @Specialization
        protected RDoubleVector log(RAbstractLogicalVector vector, double base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("create()") NACheck xNACheck,
                        @Cached("create()") NACheck baseNACheck) {
            return log(vector, base, index -> xNACheck.convertLogicalToDouble(vector.getDataAt(index)), copyAttrsNode, getNamesNode, getDimsNode, xNACheck, baseNACheck);
        }

        @Specialization
        protected RComplexVector log(RAbstractComplexVector vector, double base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NACheck xNACheck,
                        @Cached("create()") NACheck baseNACheck) {
            return log(vector, RComplex.valueOf(base, 0), copyAttrsNode, getNamesNode, getDimsNode, divNode, xNACheck, baseNACheck);
        }

        @Specialization
        protected RAbstractComplexVector log(RAbstractIntVector vector, RComplex base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NACheck xNACheck,
                        @Cached("create()") NACheck baseNACheck) {
            return log(vector, base, index -> xNACheck.convertIntToComplex(vector.getDataAt(index)), divNode, getDimsNode, getNamesNode, copyAttrsNode, xNACheck, baseNACheck);
        }

        @Specialization
        protected RAbstractComplexVector log(RAbstractDoubleVector vector, RComplex base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NACheck xNACheck,
                        @Cached("create()") NACheck baseNACheck) {
            return log(vector, base, index -> xNACheck.convertDoubleToComplex(vector.getDataAt(index)), divNode, getDimsNode, getNamesNode, copyAttrsNode, xNACheck, baseNACheck);
        }

        @Specialization
        protected RAbstractComplexVector log(RAbstractLogicalVector vector, RComplex base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NACheck xNACheck,
                        @Cached("create()") NACheck baseNACheck) {
            return log(vector, base, index -> xNACheck.convertLogicalToComplex(vector.getDataAt(index)), divNode, getDimsNode, getNamesNode, copyAttrsNode, xNACheck, baseNACheck);
        }

        @Specialization
        protected RComplexVector log(RAbstractComplexVector vector, RComplex base,
                        @Cached("create()") CopyOfRegAttributesNode copyAttrsNode,
                        @Cached("create()") GetNamesAttributeNode getNamesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("createDivNode()") BinaryMapArithmeticFunctionNode divNode,
                        @Cached("create()") NACheck xNACheck,
                        @Cached("create()") NACheck baseNACheck) {
            return log(vector, base, index -> checkComplex(vector.getDataAt(index), xNACheck), divNode, getDimsNode, getNamesNode, copyAttrsNode, xNACheck, baseNACheck);
        }

        private RComplex checkComplex(RComplex rc, NACheck xNACheck) {
            xNACheck.check(rc);
            return rc;
        }

        private RDoubleVector log(RAbstractVector vector, double base, Function<Integer, Double> toDouble, CopyOfRegAttributesNode copyAttrsNode, GetNamesAttributeNode getNamesNode,
                        GetDimAttributeNode getDimsNode, NACheck xNACheck, NACheck baseNACheck) {
            baseNACheck.enable(base);
            double[] resultVector = new double[vector.getLength()];
            if (baseNACheck.check(base)) {
                Arrays.fill(resultVector, 0, resultVector.length, base);
            } else if (Double.isNaN(base)) {
                nanProfile.enter();
                Arrays.fill(resultVector, 0, resultVector.length, Double.NaN);
            } else {
                xNACheck.enable(vector);
                Runnable[] warningResult = new Runnable[1];
                for (int i = 0; i < vector.getLength(); i++) {
                    double value = toDouble.apply(i);
                    if (!naX.isNA(value)) {
                        resultVector[i] = logb(value, base, warningResult);
                    } else {
                        resultVector[i] = value;
                    }
                }
                if (warningResult[0] != null) {
                    warningResult[0].run();
                }
            }
            boolean complete = xNACheck.neverSeenNA() && baseNACheck.neverSeenNA();
            return createResult(vector, resultVector, complete, copyAttrsNode, getNamesNode, getDimsNode);
        }

        private double logb(double x, double base, NAProfile naBase) {
            if (naBase.isNA(base)) {
                return RRuntime.DOUBLE_NA;
            }

            if (Double.isNaN(base)) {
                nanProfile.enter();
                return base;
            }
            Runnable[] warningResult = new Runnable[1];
            double ret = logb(x, base, warningResult);
            if (warningResult[0] != null) {
                warningResult[0].run();
            }
            return ret;
        }

        private double logb(double x, double base, Runnable[] warningResult) {
            double logx = Math.log(x);
            if (Double.isNaN(logx)) {
                warningResult[0] = () -> RError.warning(this, RError.Message.NAN_PRODUCED);
            }
            if (base == Math.E) {
                return logx;
            }

            double result = logx / Math.log(base);
            if (warningResult[0] == null && Double.isNaN(result)) {
                warningResult[0] = () -> RError.warning(RError.SHOW_CALLER, RError.Message.NAN_PRODUCED);
            }

            return result;
        }

        private RComplexVector log(RAbstractVector vector, RComplex base, Function<Integer, RComplex> toComplex, BinaryMapArithmeticFunctionNode divNode, GetDimAttributeNode getDimsNode,
                        GetNamesAttributeNode getNamesNode, CopyOfRegAttributesNode copyAttrsNode, NACheck xNACheck, NACheck baseNACheck) {
            baseNACheck.enable(base);
            double[] complexVector = new double[vector.getLength() * 2];
            if (baseNACheck.check(base)) {
                Arrays.fill(complexVector, 0, complexVector.length, RRuntime.DOUBLE_NA);
            } else if (Double.isNaN(base.getRealPart()) || Double.isNaN(base.getImaginaryPart())) {
                nanProfile.enter();
                Arrays.fill(complexVector, 0, complexVector.length, Double.NaN);
            } else {
                xNACheck.enable(vector);
                boolean seenNaN = false;
                for (int i = 0; i < vector.getLength(); i++) {
                    RComplex value = toComplex.apply(i);
                    if (!naX.isNA(value)) {
                        RComplex rc = logb(value, base, divNode, false);
                        seenNaN = isNaN(rc);
                        fill(complexVector, i * 2, rc);
                    } else {
                        fill(complexVector, i * 2, value);
                    }
                }
                if (seenNaN) {
                    RError.warning(this, RError.Message.NAN_PRODUCED_IN_FUNCTION, "log");
                }
            }
            boolean complete = xNACheck.neverSeenNA() && baseNACheck.neverSeenNA();
            return createResult(vector, complexVector, complete, getDimsNode, getNamesNode, copyAttrsNode);
        }

        private void fill(double[] array, int i, RComplex rc) {
            array[i] = rc.getRealPart();
            array[i + 1] = rc.getImaginaryPart();
        }

        private RComplex logb(RComplex x, RComplex base, BinaryMapArithmeticFunctionNode div, NAProfile naBase) {
            if (naBase.isNA(base)) {
                return RRuntime.createComplexNA();
            }
            if (isNaN(base)) {
                nanProfile.enter();
                return base;
            }
            return logb(x, base, div, true);
        }

        private RComplex logb(RComplex x, RComplex base, BinaryMapArithmeticFunctionNode div, boolean nanWarning) {
            RComplex logx = logb(x);
            if (base.getRealPart() == Math.E) {
                return logx;
            }

            RComplex logbase = logb(base);
            RComplex ret = div.applyComplex(logx, logbase);
            if (nanWarning && isNaN(ret)) {
                RError.warning(this, RError.Message.NAN_PRODUCED_IN_FUNCTION, "log");
            }
            return ret;
        }

        private RComplex logb(RComplex x) {
            double re = x.getRealPart();
            double im = x.getImaginaryPart();

            double mod = RComplex.abs(re, im);
            double arg = Math.atan2(im, re);

            return RComplex.valueOf(Math.log(mod), arg);
        }

        private static RDoubleVector createResult(RAbstractVector source, double[] resultData, boolean complete, CopyOfRegAttributesNode copyAttrsNode, GetNamesAttributeNode getNamesNode,
                        GetDimAttributeNode getDimsNode) {
            RDoubleVector result = RDataFactory.createDoubleVector(resultData, complete, getDimsNode.getDimensions(source), getNamesNode.getNames(source));
            copyAttrsNode.execute(source, result);
            return result;
        }

        private RComplexVector createResult(RAbstractVector source, double[] resultData, boolean complete, GetDimAttributeNode getDimsNode, GetNamesAttributeNode getNamesNode,
                        CopyOfRegAttributesNode copyAttrsNode) {
            RComplexVector result = RDataFactory.createComplexVector(resultData, complete, getDimsNode.getDimensions(source), getNamesNode.getNames(source));
            copyAttrsNode.execute(source, result);
            return result;
        }

        private boolean isNaN(RComplex base) {
            return Double.isNaN(base.getRealPart()) || Double.isNaN(base.getImaginaryPart());
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
