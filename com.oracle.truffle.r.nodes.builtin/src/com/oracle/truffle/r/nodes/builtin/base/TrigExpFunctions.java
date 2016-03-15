/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNodeGen;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public class TrigExpFunctions {
    public abstract static class AdapterCall1 extends RBuiltinNode {

        private final BranchProfile notCompleteIntValueMet = BranchProfile.create();
        private final BranchProfile notCompleteDoubleValueMet = BranchProfile.create();
        private final NACheck na = NACheck.create();

        @Child private UnaryCopyAttributesNode copyAttributes = UnaryCopyAttributesNodeGen.create(true);

        @Specialization
        protected byte isType(@SuppressWarnings("unused") RMissing value) {
            controlVisibility();
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.ARGUMENTS_PASSED_0_1, getRBuiltin().name());
        }

        protected double op(@SuppressWarnings("unused") double x) {
            // not abstract because this would confuse the DSL annotation processor
            throw RInternalError.shouldNotReachHere("this method needs to be implemented in subclasses");
        }

        private double doFunInt(int value) {
            if (na.check(value)) {
                notCompleteIntValueMet.enter();
                return RRuntime.DOUBLE_NA;
            }
            return op(value);
        }

        private double doFunDouble(double value) {
            if (na.check(value)) {
                notCompleteDoubleValueMet.enter();
                return value;
            }
            return op(value);
        }

        @Specialization
        protected double trigOp(int x) {
            controlVisibility();
            na.enable(x);
            return doFunInt(x);
        }

        @Specialization
        protected double trigOp(double x) {
            controlVisibility();
            na.enable(x);
            return doFunDouble(x);
        }

        @Specialization
        protected RAbstractVector trigOp(RIntVector vector, //
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            controlVisibility();
            int length = vector.getLength();
            double[] resultVector = new double[length];
            reportWork(length);
            profile.profileCounted(length);
            na.enable(vector);
            for (int i = 0; profile.inject(i < length); i++) {
                resultVector[i] = doFunInt(vector.getDataAt(i));
            }
            return createDoubleVectorBasedOnOrigin(resultVector, vector);
        }

        @Specialization
        protected RAbstractVector trigOp(RDoubleVector vector, //
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            controlVisibility();
            int length = vector.getLength();
            double[] resultVector = new double[length];
            reportWork(length);
            profile.profileCounted(length);
            na.enable(vector);
            for (int i = 0; profile.inject(i < length); i++) {
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
    public abstract static class Exp extends RBuiltinNode {

        @Child private BinaryArithmetic calculatePowNode;
        @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
        @Child private UnaryArithmeticNode expNode = UnaryArithmeticNodeGen.create(ExpArithmetic::new, RType.Double,
                        RError.Message.ARGUMENTS_PASSED_0_1, new Object[]{getRBuiltin().name()});

        @Specialization
        protected Object exp(Object value) {
            return expNode.execute(boxPrimitive.execute(value));
        }

        public class ExpArithmetic extends UnaryArithmetic {

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
                return Math.exp(op);
            }

            @Override
            public RComplex op(double re, double im) {
                if (calculatePowNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    calculatePowNode = insert(BinaryArithmetic.POW.create());
                }
                return calculatePowNode.op(Math.E, 0, re, im);
            }

        }

    }

    @RBuiltin(name = "expm1", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class ExpM1 extends RBuiltinNode {

        @Child private BinaryArithmetic calculatePowNode;
        @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
        @Child private UnaryArithmeticNode expm1Node = UnaryArithmeticNodeGen.create(ExpM1Arithmetic::new, RType.Double,
                        RError.Message.ARGUMENTS_PASSED_0_1, new Object[]{getRBuiltin().name()});

        @Specialization
        protected Object exp(Object value) {
            return expm1Node.execute(boxPrimitive.execute(value));
        }

        public class ExpM1Arithmetic extends UnaryArithmetic {

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
                return Math.expm1(op);
            }

            @Override
            public RComplex op(double re, double im) {
                if (calculatePowNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    calculatePowNode = insert(BinaryArithmetic.POW.create());
                }
                RComplex x = calculatePowNode.op(Math.E, 0, re, im);
                return RDataFactory.createComplex(x.getRealPart() - 1d, x.getImaginaryPart());
            }

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

        private double doFunDouble(double y, double x) {
            double result = x;
            if (!yNACheck.check(y) && !xNACheck.check(x)) {
                result = Math.atan2(y, x);
            }
            return result;
        }

        @FunctionalInterface
        protected interface IntDoubleFunction {
            double apply(int i);
        }

        protected RDoubleVector doFun(int length, IntDoubleFunction yFun, IntDoubleFunction xFun, LoopConditionProfile profile) {
            controlVisibility();
            double[] resultVector = new double[length];
            reportWork(length);
            profile.profileCounted(length);
            for (int i = 0; profile.inject(i < length); i++) {
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
            xNACheck.enable(x);
            yNACheck.enable(y);
            return doFunDouble(y, x);
        }

        @Specialization
        protected RDoubleVector atan2(double y, RAbstractDoubleVector x, //
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            xNACheck.enable(x);
            yNACheck.enable(y);
            return doFun(x.getLength(), i -> y, i -> x.getDataAt(i), profile);
        }

        @Specialization
        protected RDoubleVector atan2(RAbstractDoubleVector y, double x, //
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            xNACheck.enable(x);
            yNACheck.enable(y);
            return doFun(y.getLength(), i -> y.getDataAt(i), i -> x, profile);
        }

        @Specialization
        protected RDoubleVector atan2(RAbstractDoubleVector y, RAbstractDoubleVector x, //
                        @Cached("createCountingProfile()") LoopConditionProfile profile) {
            int xLength = x.getLength();
            int yLength = y.getLength();
            xNACheck.enable(x);
            yNACheck.enable(y);
            return doFun(Math.max(yLength, xLength), i -> y.getDataAt(i % yLength), i -> x.getDataAt(i % xLength), profile);
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
