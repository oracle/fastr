/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.ConstantNode.ConstantMissingNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

/**
 * TODO complex specializations, runtime type checks on arguments.
 *
 */
public class TrigExpFunctions {
    public abstract static class AdapterCall1 extends RBuiltinNode {
        private static final BranchProfile notCompleteIntValueMet = BranchProfile.create();
        private static final BranchProfile notCompleteDoubleValueMet = BranchProfile.create();
        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{RMissing.instance};
        }

        @Specialization
        protected byte isType(@SuppressWarnings("unused") RMissing value) {
            controlVisibility();
            CompilerDirectives.transferToInterpreter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENTS_PASSED_0_1, getRBuiltin().name());
        }

        protected interface MathCall1 {
            double call(double x);
        }

        private static double doFunInt(int value, MathCall1 fun) {
            if (!RRuntime.isComplete(value)) {
                notCompleteIntValueMet.enter();
                return RRuntime.DOUBLE_NA;
            }
            return fun.call(value);
        }

        private static double doFunDouble(double value, MathCall1 fun) {
            if (!RRuntime.isComplete(value)) {
                notCompleteDoubleValueMet.enter();
                return value;
            }
            return fun.call(value);
        }

        protected RDoubleVector doFunForDoubleVector(RDoubleVector vector, MathCall1 mathCall) {
            final int length = vector.getLength();
            final double[] resultVector = new double[length];
            for (int i = 0; i < length; i++) {
                resultVector[i] = doFunDouble(vector.getDataAt(i), mathCall);
            }
            return createDoubleVectorBasedOnOrigin(resultVector, vector);
        }

        protected RDoubleVector doFunForIntVector(RIntVector vector, MathCall1 mathCall) {
            final int length = vector.getLength();
            final double[] resultVector = new double[length];
            for (int i = 0; i < length; i++) {
                resultVector[i] = doFunInt(vector.getDataAt(i), mathCall);
            }
            return createDoubleVectorBasedOnOrigin(resultVector, vector);
        }

        private RDoubleVector createDoubleVectorBasedOnOrigin(double[] values, RAbstractVector originVector) {
            RDoubleVector result = RDataFactory.createDoubleVector(values, originVector.isComplete());
            result.copyAttributesFrom(attrProfiles, originVector);
            return result;
        }
    }

    public abstract static class ComplexArgumentsCallAdapter extends AdapterCall1 {
        protected interface ComplexArgumentFunctionCall {
            RComplex call(RComplex rComplex);
        }

        protected static RComplexVector doFunForComplexVector(RAbstractComplexVector arguments, ComplexArgumentFunctionCall functionCall) {
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

    public abstract static class ComplexExpCalculator extends ComplexArgumentsCallAdapter {
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
            return doFunForComplexVector(powersVector, expFunctionCall);
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
            return doFunForIntVector(x, Math::exp);
        }

        @Specialization
        protected RDoubleVector exp(RDoubleVector x) {
            controlVisibility();
            return doFunForDoubleVector(x, Math::exp);
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
            return doFunForIntVector(x, Math::expm1);
        }

        @Specialization
        protected RDoubleVector expm1(RDoubleVector x) {
            controlVisibility();
            return doFunForDoubleVector(x, Math::expm1);
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
            return doFunForComplexVector(exponents, ExpM1::substract1From);
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
            return doFunForIntVector(x, Math::sin);
        }

        @Specialization
        protected RDoubleVector sin(RDoubleVector x) {
            controlVisibility();
            return doFunForDoubleVector(x, Math::sin);
        }
    }

    @RBuiltin(name = "sinh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Sinh extends AdapterCall1 {
        @Specialization
        protected double sinh(int x) {
            controlVisibility();
            return Math.sin(x);
        }

        @Specialization
        protected double sinh(double x) {
            controlVisibility();
            return Math.sin(x);
        }

        @Specialization
        protected RDoubleVector sinh(RIntVector x) {
            controlVisibility();
            return doFunForIntVector(x, Math::sin);
        }

        @Specialization
        protected RDoubleVector sinh(RDoubleVector x) {
            controlVisibility();
            return doFunForDoubleVector(x, Math::sin);
        }
    }

    @RBuiltin(name = "sinpi", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Sinpi extends AdapterCall1 {
        @Specialization
        protected Object sinpi(@SuppressWarnings("unused") Object x) {
            throw RError.nyi(getEncapsulatingSourceSection(), " sinpi");
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
            return doFunForIntVector(x, Math::cos);
        }

        @Specialization
        protected RDoubleVector cos(RDoubleVector x) {
            controlVisibility();
            return doFunForDoubleVector(x, Math::cos);
        }
    }

    @RBuiltin(name = "cosh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Cosh extends AdapterCall1 {

        @Specialization
        protected double cosh(@SuppressWarnings("unused") int x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected double cosh(@SuppressWarnings("unused") double x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector cosh(@SuppressWarnings("unused") RIntVector x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector cosh(@SuppressWarnings("unused") RDoubleVector x) {
            controlVisibility();
            throw nyi();
        }

        private RError nyi() throws RError {
            throw RError.nyi(getEncapsulatingSourceSection(), " cosh");
        }
    }

    @RBuiltin(name = "cospi", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Cospi extends AdapterCall1 {
        @Specialization
        protected Object cospi(@SuppressWarnings("unused") Object x) {
            throw RError.nyi(getEncapsulatingSourceSection(), " cospi");
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
            return doFunForIntVector(x, Math::tan);
        }

        @Specialization
        protected RDoubleVector tan(RDoubleVector x) {
            controlVisibility();
            return doFunForDoubleVector(x, Math::tan);
        }
    }

    @RBuiltin(name = "tanh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Tanh extends AdapterCall1 {

        @Specialization
        protected double tanh(@SuppressWarnings("unused") int x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected double tanh(@SuppressWarnings("unused") double x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector tanh(@SuppressWarnings("unused") RIntVector x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector tanh(@SuppressWarnings("unused") RDoubleVector x) {
            controlVisibility();
            throw nyi();
        }

        private RError nyi() throws RError {
            throw RError.nyi(getEncapsulatingSourceSection(), " cosh");
        }
    }

    @RBuiltin(name = "tanpi", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Tanpi extends AdapterCall1 {
        @Specialization
        protected Object tanpi(@SuppressWarnings("unused") Object x) {
            throw RError.nyi(getEncapsulatingSourceSection(), " tanpi");
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
            return doFunForIntVector(x, Math::asin);
        }

        @Specialization
        protected RDoubleVector asin(RDoubleVector x) {
            controlVisibility();
            return doFunForDoubleVector(x, Math::asin);
        }
    }

    @RBuiltin(name = "asinh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Asinh extends AdapterCall1 {
        @Specialization
        protected double asinh(@SuppressWarnings("unused") int x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected double asinh(@SuppressWarnings("unused") double x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector asinh(@SuppressWarnings("unused") RIntVector x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector asinh(@SuppressWarnings("unused") RDoubleVector x) {
            controlVisibility();
            throw nyi();
        }

        private RError nyi() throws RError {
            throw RError.nyi(getEncapsulatingSourceSection(), " asinh");
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
            return doFunForIntVector(x, Math::acos);
        }

        @Specialization
        protected RDoubleVector acos(RDoubleVector x) {
            controlVisibility();
            return doFunForDoubleVector(x, Math::acos);
        }
    }

    @RBuiltin(name = "acosh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Acosh extends AdapterCall1 {
        @Specialization
        protected double acosh(@SuppressWarnings("unused") int x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected double acosh(@SuppressWarnings("unused") double x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector acosh(@SuppressWarnings("unused") RIntVector x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector acosh(@SuppressWarnings("unused") RDoubleVector x) {
            controlVisibility();
            throw nyi();
        }

        private RError nyi() throws RError {
            throw RError.nyi(getEncapsulatingSourceSection(), " acosh");
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
            return doFunForIntVector(x, Math::atan);
        }

        @Specialization
        protected RDoubleVector atan(RDoubleVector x) {
            controlVisibility();
            return doFunForDoubleVector(x, Math::atan);
        }
    }

    @RBuiltin(name = "atanh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Atanh extends AdapterCall1 {
        @Specialization
        protected double atanh(@SuppressWarnings("unused") int x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected double atanh(@SuppressWarnings("unused") double x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector atanh(@SuppressWarnings("unused") RIntVector x) {
            controlVisibility();
            throw nyi();
        }

        @Specialization
        protected RDoubleVector atanh(@SuppressWarnings("unused") RDoubleVector x) {
            controlVisibility();
            throw nyi();
        }

        private RError nyi() throws RError {
            throw RError.nyi(getEncapsulatingSourceSection(), " atanh");
        }
    }

    public abstract static class AdapterCall2 extends RBuiltinNode {

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
    @RBuiltin(name = "atan2", kind = RBuiltinKind.INTERNAL, parameterNames = {"y", "x"})
    public abstract static class Atan2 extends AdapterCall2 {

        private static RNode castArgument(RNode node) {
            if (!(node instanceof ConstantMissingNode)) {
                return CastDoubleNodeGen.create(node, false, false, false);
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
            return doFun(x, y, Math::atan2);
        }

        @Specialization
        protected RDoubleVector atan2(double x, RDoubleVector y) {
            controlVisibility();
            RDoubleVector xv = RDataFactory.createDoubleVectorFromScalar(x).copyResized(y.getLength(), false);
            return doFun(xv, y, Math::atan2);
        }

        @Specialization
        protected RDoubleVector atan2(RDoubleVector x, double y) {
            controlVisibility();
            RDoubleVector yv = RDataFactory.createDoubleVectorFromScalar(y);
            return doFun(x, yv, Math::atan2);
        }
    }
}
