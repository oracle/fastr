/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.ConstantNode.ConstantMissingNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeFactory;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

/**
 * TODO complex specializations, runtime type checks on arguments
 *
 */
public class TrigExpFunctions {

    public static abstract class Adapter extends RBuiltinNode {
        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        protected byte isType(@SuppressWarnings("unused") RMissing value) {
            controlVisibility();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENTS_PASSED_0_1, getRBuiltin().name());
        }

    }

    public static abstract class AdapterCall1 extends Adapter {

        protected interface MathCall1 {
            double call(double x);
        }

        private static double doFunInt(int value, MathCall1 fun) {
            double result = RRuntime.DOUBLE_NA;
            if (RRuntime.isComplete(value)) {
                result = fun.call(value);
            }
            return result;
        }

        private static double doFunDouble(double value, MathCall1 fun) {
            double result = value;
            if (RRuntime.isComplete(value)) {
                result = fun.call(value);
            }
            return result;
        }

        protected RDoubleVector doFun(RAbstractVector x, MathCall1 fun) {
            boolean isDouble = x instanceof RDoubleVector;
            RDoubleVector dx = isDouble ? (RDoubleVector) x : null;
            RIntVector ix = !isDouble ? (RIntVector) x : null;
            double[] resultVector = new double[x.getLength()];
            for (int i = 0; i < x.getLength(); i++) {
                resultVector[i] = isDouble ? doFunDouble(dx.getDataAt(i), fun) : doFunInt(ix.getDataAt(i), fun);
            }
            RDoubleVector ret = RDataFactory.createDoubleVector(resultVector, x.isComplete());
            ret.copyAttributesFrom(x);
            return ret;
        }
    }

    public static abstract class ComplexArgumentsCallAdapter extends AdapterCall1 {
        protected interface ComplexArgumentFunctionCall {
            RComplex call(RComplex rComplex);
        }

        protected RComplexVector callFunction(RAbstractComplexVector arguments, ComplexArgumentFunctionCall functionCall) {
            int argumentsLength = arguments.getLength();
            double[] result = new double[argumentsLength * 2];
            for (int i = 0; i < argumentsLength; i++) {
                RComplex rComplexResult = functionCall.call(arguments.getDataAt(i));
                result[2 * i] = rComplexResult.getRealPart();
                result[2 * i + 1] = rComplexResult.getImaginaryPart();
            }
            return RDataFactory.createComplexVector(result, true);
        }
    }

    public static abstract class ComplexExpCalculator extends ComplexArgumentsCallAdapter {
        @Child private BinaryArithmetic calculatePowNode;

        private final ComplexArgumentFunctionCall expFunctionCall = new ComplexArgumentFunctionCall() {
            @Override
            public RComplex call(RComplex rComplex) {
                ensureCalculatePowNodeCreated();
                return calculatePowNode.op(Math.E, 0, rComplex.getRealPart(), rComplex.getImaginaryPart());
            }

            private void ensureCalculatePowNodeCreated() {
                if (calculatePowNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    calculatePowNode = insert(BinaryArithmetic.POW.create());
                }
            }
        };

        protected RComplexVector calculateExpUsing(RAbstractComplexVector powersVector) {
            return callFunction(powersVector, expFunctionCall);
        }

        protected RComplex calculateExpUsing(RComplex power) {
            return expFunctionCall.call(power);
        }
    }

    @RBuiltin(name = "exp", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Exp extends ComplexExpCalculator {
        @Specialization
        protected double exp(int x) {
            controlVisibility();
            return Math.exp(x);
        }

        @Specialization
        protected double exp(double x) {
            controlVisibility();
            return Math.exp(x);
        }

        @Specialization
        protected RDoubleVector exp(RIntVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.exp(d));
        }

        @Specialization
        protected RDoubleVector exp(RDoubleVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.exp(d));
        }

        @Specialization
        protected RComplex exp(RComplex power) {
            controlVisibility();
            return calculateExpUsing(power);
        }

        @Specialization
        protected RComplexVector exp(RComplexVector powersVector) {
            controlVisibility();
            return calculateExpUsing(powersVector);
        }
    }

    @RBuiltin(name = "expm1", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class ExpM1 extends ComplexExpCalculator {
        @Specialization
        protected double expm1(int x) {
            controlVisibility();
            return Math.expm1(x);
        }

        @Specialization
        protected double expm1(double x) {
            controlVisibility();
            return Math.expm1(x);
        }

        @Specialization
        protected RDoubleVector expm1(RIntVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.expm1(d));
        }

        @Specialization
        protected RDoubleVector expm1(RDoubleVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.expm1(d));
        }

        @Specialization
        protected RComplex exp(RComplex power) {
            controlVisibility();
            return substract1From(calculateExpUsing(power));
        }

        private static RComplex substract1From(RComplex rComplex) {
            double decreasedReal = rComplex.getRealPart() - 1.;
            return RDataFactory.createComplex(decreasedReal, rComplex.getImaginaryPart());
        }

        @Specialization
        protected RComplexVector exp(RComplexVector powersVector) {
            controlVisibility();
            RComplexVector exponents = calculateExpUsing(powersVector);
            return callFunction(exponents, (RComplex rComplex) -> substract1From(rComplex));
        }
    }

    @RBuiltin(name = "sin", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Sin extends AdapterCall1 {
        @Specialization
        protected double sin(int x) {
            controlVisibility();
            return Math.sin(x);
        }

        @Specialization
        protected double sin(double x) {
            controlVisibility();
            return Math.sin(x);
        }

        @Specialization
        protected RDoubleVector sin(RIntVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.sin(d));
        }

        @Specialization
        protected RDoubleVector sin(RDoubleVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.sin(d));
        }
    }

    @RBuiltin(name = "cos", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Cos extends AdapterCall1 {

        @Specialization
        protected double cos(int x) {
            controlVisibility();
            return Math.cos(x);
        }

        @Specialization
        protected double cos(double x) {
            controlVisibility();
            return Math.cos(x);
        }

        @Specialization
        protected RDoubleVector cos(RIntVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.cos(d));
        }

        @Specialization
        protected RDoubleVector cos(RDoubleVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.cos(d));
        }
    }

    @RBuiltin(name = "tan", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Tan extends AdapterCall1 {

        @Specialization
        protected double tan(int x) {
            controlVisibility();
            return Math.tan(x);
        }

        @Specialization
        protected double tan(double x) {
            controlVisibility();
            return Math.tan(x);
        }

        @Specialization
        protected RDoubleVector tan(RIntVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.tan(d));
        }

        @Specialization
        protected RDoubleVector tan(RDoubleVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.tan(d));
        }
    }

    @RBuiltin(name = "asin", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Asin extends AdapterCall1 {
        @Specialization
        protected double asin(int x) {
            controlVisibility();
            return Math.asin(x);
        }

        @Specialization
        protected double asin(double x) {
            controlVisibility();
            return Math.asin(x);
        }

        @Specialization
        protected RDoubleVector asin(RIntVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.asin(d));
        }

        @Specialization
        protected RDoubleVector asin(RDoubleVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.asin(d));
        }
    }

    @RBuiltin(name = "acos", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Acos extends AdapterCall1 {
        @Specialization
        protected double acos(int x) {
            controlVisibility();
            return Math.acos(x);
        }

        @Specialization
        protected double acos(double x) {
            controlVisibility();
            return Math.acos(x);
        }

        @Specialization
        protected RDoubleVector acos(RIntVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.acos(d));
        }

        @Specialization
        protected RDoubleVector acos(RDoubleVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.acos(d));
        }
    }

    @RBuiltin(name = "atan", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Atan extends AdapterCall1 {
        @Specialization
        protected double atan(int x) {
            controlVisibility();
            return Math.atan(x);
        }

        @Specialization
        protected double atan(double x) {
            controlVisibility();
            return Math.atan(x);
        }

        @Specialization
        protected RDoubleVector atan(RIntVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.atan(d));
        }

        @Specialization
        protected RDoubleVector atan(RDoubleVector x) {
            controlVisibility();
            return doFun(x, (double d) -> Math.atan(d));
        }
    }

    public static abstract class AdapterCall2 extends RBuiltinNode {

        protected interface MathCall2 {
            double call(double x, double y);
        }

        private static double doFunDouble(double x, double y, MathCall2 fun) {
            double result = x;
            if (RRuntime.isComplete(x)) {
                result = fun.call(x, y);
            }
            return result;
        }

        protected RDoubleVector doFun(RDoubleVector x, RDoubleVector y, MathCall2 fun) {
            double[] resultVector = new double[x.getLength()];
            int yl = y.getLength();
            for (int i = 0; i < x.getLength(); i++) {
                resultVector[i] = doFunDouble(x.getDataAt(i), y.getDataAt(i % yl), fun);
            }
            return RDataFactory.createDoubleVector(resultVector, x.isComplete());
        }

    }

    /**
     * {@code atan2} takes two args. To avoid combinatorial explosion in specializations we coerce
     * the {@code int} forms to {@code double}.
     */
    @RBuiltin(name = "atan2", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"y", "x"})
    public abstract static class Atan2 extends AdapterCall2 {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
        }

        private static RNode castArgument(RNode node) {
            if (!(node instanceof ConstantMissingNode)) {
                return CastDoubleNodeFactory.create(node, false, false, false);
            }
            return node;
        }

        @CreateCast("arguments")
        protected static RNode[] castArguments(RNode[] arguments) {
            arguments[0] = castArgument(arguments[0]);
            arguments[1] = castArgument(arguments[1]);
            return arguments;
        }

        @Specialization
        protected Object atan(@SuppressWarnings("unused") RMissing x, @SuppressWarnings("unused") RMissing y) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENT_MISSING, getRBuiltin().parameterNames()[0]);
        }

        @Specialization
        protected Object atan(@SuppressWarnings("unused") RAbstractDoubleVector x, @SuppressWarnings("unused") RMissing y) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENT_MISSING, getRBuiltin().parameterNames()[1]);
        }

        @Specialization
        protected double atan2(double x, double y) {
            controlVisibility();
            return Math.atan2(x, y);
        }

        @Specialization
        protected RDoubleVector atan2(RDoubleVector x, RDoubleVector y) {
            controlVisibility();
            return doFun(x, y, (double d1, double d2) -> Math.atan2(d1, d2));
        }

        @Specialization
        protected RDoubleVector atan2(double x, RDoubleVector y) {
            controlVisibility();
            RDoubleVector xv = RDataFactory.createDoubleVectorFromScalar(x).copyResized(y.getLength(), false);
            return doFun(xv, y, (double d1, double d2) -> Math.atan2(d1, d2));
        }

        @Specialization
        protected RDoubleVector atan2(RDoubleVector x, double y) {
            controlVisibility();
            RDoubleVector yv = RDataFactory.createDoubleVectorFromScalar(y);
            return doFun(x, yv, (double d1, double d2) -> Math.atan2(d1, d2));
        }
    }

}
