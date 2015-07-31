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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.attributes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public class TrigExpFunctions {
    public abstract static class AdapterCall1 extends RBuiltinNode {

        private final BranchProfile notCompleteIntValueMet = BranchProfile.create();
        private final BranchProfile notCompleteDoubleValueMet = BranchProfile.create();

        @Child private UnaryCopyAttributesNode copyAttributes = UnaryCopyAttributesNodeGen.create(true);

        @Specialization
        protected byte isType(@SuppressWarnings("unused") RMissing value) {
            controlVisibility();
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.ARGUMENTS_PASSED_0_1, getRBuiltin().name());
        }

        protected double op(@SuppressWarnings("unused") double x) {
            throw RInternalError.shouldNotReachHere("this method needs to be implemented in subclasses");
        }

        private double doFunInt(int value) {
            if (RRuntime.isNA(value)) {
                notCompleteIntValueMet.enter();
                return RRuntime.DOUBLE_NA;
            }
            return op(value);
        }

        private double doFunDouble(double value) {
            if (RRuntime.isNA(value)) {
                notCompleteDoubleValueMet.enter();
                return value;
            }
            return op(value);
        }

        @Specialization
        protected double trigOp(int x) {
            controlVisibility();
            return doFunInt(x);
        }

        @Specialization
        protected double trigOp(double x) {
            controlVisibility();
            return doFunDouble(x);
        }

        @Specialization
        protected RAbstractVector trigOp(RIntVector vector) {
            controlVisibility();
            int length = vector.getLength();
            double[] resultVector = new double[length];
            for (int i = 0; i < length; i++) {
                resultVector[i] = doFunInt(vector.getDataAt(i));
            }
            return createDoubleVectorBasedOnOrigin(resultVector, vector);
        }

        @Specialization
        protected RAbstractVector trigOp(RDoubleVector vector) {
            controlVisibility();
            int length = vector.getLength();
            double[] resultVector = new double[length];
            for (int i = 0; i < length; i++) {
                resultVector[i] = doFunDouble(vector.getDataAt(i));
            }
            return createDoubleVectorBasedOnOrigin(resultVector, vector);
        }

        private RAbstractVector createDoubleVectorBasedOnOrigin(double[] values, RAbstractVector originVector) {
            RDoubleVector result = RDataFactory.createDoubleVector(values, originVector.isComplete());
            return copyAttributes.execute(result, originVector);
        }
    }

    @RBuiltin(name = "exp", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Exp extends AdapterCall1 {

        @Child private BinaryArithmetic calculatePowNode;

        public RComplex complexOp(RComplex rComplex) {
            if (calculatePowNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                calculatePowNode = insert(BinaryArithmetic.POW.create());
            }
            return calculatePowNode.op(Math.E, 0, rComplex.getRealPart(), rComplex.getImaginaryPart());
        }

        @Override
        protected double op(double x) {
            return Math.exp(x);
        }

        @Specialization
        protected RComplex exp(RComplex power) {
            controlVisibility();
            return complexOp(power);
        }

        @Specialization
        protected RComplexVector exp(RComplexVector powersVector) {
            controlVisibility();
            int argumentsLength = powersVector.getLength();
            double[] result = new double[argumentsLength * 2];
            for (int i = 0; i < argumentsLength; i++) {
                RComplex rComplexResult = complexOp(powersVector.getDataAt(i));
                result[2 * i] = rComplexResult.getRealPart();
                result[2 * i + 1] = rComplexResult.getImaginaryPart();
            }
            return RDataFactory.createComplexVector(result, true);
        }

    }

    @RBuiltin(name = "expm1", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class ExpM1 extends Exp {

        @Child private BinaryArithmetic calculatePowNode;

        @Override
        public RComplex complexOp(RComplex rComplex) {
            RComplex intermediate = super.complexOp(rComplex);
            return RDataFactory.createComplex(intermediate.getRealPart() - 1d, intermediate.getImaginaryPart());
        }

        @Override
        protected double op(double x) {
            return Math.expm1(x);
        }
    }

    @RBuiltin(name = "sin", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Sin extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.sin(x);
        }
    }

    @RBuiltin(name = "sinh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Sinh extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.sinh(x);
        }
    }

    @RBuiltin(name = "sinpi", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Sinpi extends AdapterCall1 {
        @Override
        protected double op(double x) {
            if (Double.isNaN(x)) {
                return x;
            }
            double norm = x % 2d;
            if (norm == 0d || norm == 1d) {
                return 0d;
            }
            if (norm == -1.5d || norm == 0.5d) {
                return 1d;
            }
            if (norm == -0.5d || norm == 1.5d) {
                return -1d;
            }
            return Math.sin(norm * Math.PI);
        }
    }

    @RBuiltin(name = "cos", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Cos extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.cos(x);
        }
    }

    @RBuiltin(name = "cosh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Cosh extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.cosh(x);
        }
    }

    @RBuiltin(name = "cospi", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Cospi extends AdapterCall1 {
        @Override
        protected double op(double x) {
            if (Double.isNaN(x)) {
                return x;
            }
            double norm = x % 2d;
            if (norm == 0d) {
                return 1d;
            }
            if (norm == -1d || norm == 1d) {
                return -1d;
            }
            if (norm == -1.5d || norm == -0.5d || norm == 0.5d || norm == 1.5d) {
                return 0d;
            }
            return Math.cos(norm * Math.PI);
        }
    }

    @RBuiltin(name = "tan", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Tan extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.tan(x);
        }
    }

    @RBuiltin(name = "tanh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Tanh extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.tanh(x);
        }
    }

    @RBuiltin(name = "tanpi", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Tanpi extends AdapterCall1 {
        @Override
        protected double op(double x) {
            if (Double.isNaN(x)) {
                return x;
            }
            double norm = x % 1d;
            if (norm == 0d) {
                return 1d;
            }
            if (norm == -0.5d || norm == 0.5d) {
                return Double.NaN;
            }
            return Math.tan(norm * Math.PI);
        }
    }

    @RBuiltin(name = "asin", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Asin extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.asin(x);
        }
    }

    @RBuiltin(name = "asinh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Asinh extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.log(x + Math.sqrt(x * x + 1d));
        }
    }

    @RBuiltin(name = "acos", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Acos extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.acos(x);
        }
    }

    @RBuiltin(name = "acosh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Acosh extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.log(x + Math.sqrt(x * x - 1d));
        }
    }

    @RBuiltin(name = "atan", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Atan extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return Math.atan(x);
        }
    }

    @RBuiltin(name = "atanh", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class Atanh extends AdapterCall1 {
        @Override
        protected double op(double x) {
            return 0.5 * Math.log((x + 1d) / (x - 1d));
        }
    }

    /**
     * {@code atan2} takes two args. To avoid combinatorial explosion in specializations we coerce
     * the {@code int} forms to {@code double}.
     */
    @RBuiltin(name = "atan2", kind = RBuiltinKind.INTERNAL, parameterNames = {"y", "x"})
    public abstract static class Atan2 extends RBuiltinNode {

        private final NACheck yNACheck = NACheck.create();
        private final NACheck xNACheck = NACheck.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.toDouble(0).toDouble(1);
        }

        private static double doFunDouble(double y, double x) {
            double result = x;
            if (RRuntime.isComplete(y) && RRuntime.isComplete(x)) {
                result = Math.atan2(y, x);
            }
            return result;
        }

        @FunctionalInterface
        protected interface IntDoubleFunction {
            double apply(int i);
        }

        protected RDoubleVector doFun(int length, IntDoubleFunction yFun, IntDoubleFunction xFun) {
            controlVisibility();
            double[] resultVector = new double[length];
            for (int i = 0; i < length; i++) {
                double y = yFun.apply(i);
                double x = xFun.apply(i);
                if (xNACheck.check(y) || yNACheck.check(x)) {
                    resultVector[i] = RRuntime.DOUBLE_NA;
                } else {
                    resultVector[i] = Math.atan2(y, x);
                }
            }
            return RDataFactory.createDoubleVector(resultVector, xNACheck.neverSeenNA() && yNACheck.neverSeenNA());
        }

        @Specialization
        protected double atan2(double y, double x) {
            controlVisibility();
            return doFunDouble(y, x);
        }

        @Specialization
        protected RDoubleVector atan2(double y, RAbstractDoubleVector x) {
            yNACheck.enable(y);
            xNACheck.enable(x);
            return doFun(x.getLength(), i -> y, i -> x.getDataAt(i));
        }

        @Specialization
        protected RDoubleVector atan2(RAbstractDoubleVector y, double x) {
            yNACheck.enable(y);
            xNACheck.enable(x);
            return doFun(y.getLength(), i -> y.getDataAt(i), i -> x);
        }

        @Specialization
        protected RDoubleVector atan2(RAbstractDoubleVector y, RAbstractDoubleVector x) {
            int yLength = y.getLength();
            int xLength = x.getLength();
            yNACheck.enable(y);
            xNACheck.enable(x);
            return doFun(Math.max(yLength, xLength), i -> y.getDataAt(i % yLength), i -> x.getDataAt(i % xLength));
        }

        @Fallback
        @TruffleBoundary
        protected Object atan2(Object x, Object y) {
            if (x instanceof RMissing) {
                throw RError.error(this, RError.Message.ARGUMENT_MISSING, getRBuiltin().parameterNames()[0]);
            } else if (y instanceof RMissing) {
                throw RError.error(this, RError.Message.ARGUMENT_MISSING, getRBuiltin().parameterNames()[1]);
            }
            throw RInternalError.unimplemented();
        }
    }
}
