/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.binary.BinaryArithmeticNodeFactory.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class BinaryArithmeticExperimentalNode extends BinaryNode {

    @Child private BinaryArithmetic arithmetic;

    private final NACheck leftNACheck;
    private final NACheck rightNACheck;
    private final NACheck resultNACheck;

    public BinaryArithmeticExperimentalNode(BinaryArithmeticFactory factory) {
        this.arithmetic = adoptChild(factory.create());
        leftNACheck = new NACheck();
        rightNACheck = new NACheck();
        resultNACheck = new NACheck();
    }

    public BinaryArithmeticExperimentalNode(BinaryArithmeticExperimentalNode op) {
        this.arithmetic = adoptChild(op.arithmetic);
        this.leftNACheck = op.leftNACheck;
        this.rightNACheck = op.rightNACheck;
        this.resultNACheck = op.resultNACheck;
    }

    @CreateCast({"arguments"})
    public RNode[] createCastLeft(RNode[] child) {
        return new RNode[]{createCast(child[0], leftNACheck), createCast(child[1], rightNACheck)};
    }

    private static RNode createCast(RNode child, NACheck check) {
        return ArithmeticCastFactory.create(child, check);
    }

    @Specialization(order = 0)
    public Object doLeftNull(RNull left, Object right) {
        return doRightNull(right, left);
    }

    @Specialization(order = 1)
    public Object doRightNull(Object left, RNull right) {
        if (left instanceof RVector) {
            RVector rVector = (RVector) left;
            if (rVector.getElementClass() == RInt.class || rVector.getElementClass() == RLogical.class) {
                return RDataFactory.createDoubleVector(0);
            }
            return rVector.createEmptySameType(0, RDataFactory.COMPLETE_VECTOR);
        } else if (left instanceof RNull) {
            return RDataFactory.createDoubleVector(0);
        } else if (left instanceof Byte || left instanceof Integer || left instanceof Double) {
            return RDataFactory.createDoubleVector(0);
        } else if (left instanceof String) {
            return RDataFactory.createStringVector(0);
        } else if (left instanceof RRaw) {
            return RDataFactory.createRawVector(0);
        } else if (left instanceof RComplex) {
            return RDataFactory.createComplexVector(0);
        }
        return right;
    }

    public boolean supportsIntResult() {
        return arithmetic.isSupportsIntResult();
    }

    // Scalar operations

    // int vector specializations

    @Specialization(order = 100, guards = {"supportsIntResult"})
    public int doIntInt(int left, int right) {
        return performArithmetic(left, right);
    }

    @Specialization(order = 101, guards = {"!supportsIntResult"})
    public double doIntIntDouble(int left, int right) {
        return performArithmeticDouble(left, right);
    }

    @Specialization(order = 110, guards = {"areSameLength", "supportsIntResult"})
    public RIntVector doIntIntSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorIntVectorOpSameLengthInt(left, right);
    }

    @Specialization(order = 111, guards = {"!areSameLength", "supportsIntResult"})
    public RIntVector doIntInt(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorIntVectorOpInt(left, right);
    }

    @Specialization(order = 120, guards = {"areSameLength", "!supportsIntResult"})
    public RDoubleVector doIntIntDoubleSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorIntVectorOpSameLengthIntDouble(left, right);
    }

    @Specialization(order = 121, guards = {"!areSameLength", "!supportsIntResult"})
    public RDoubleVector doIntIntDouble(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorIntVectorOpIntDouble(left, right);
    }

    // double vector specializations

    @Specialization(order = 125)
    public double doDoubleDouble(double left, double right) {
        return performArithmeticDouble(left, right);
    }

    @Specialization(order = 130, guards = "areSameLength")
    public RDoubleVector doDoubleDoubleSameLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performDoubleVectorDoubleVectorOpSameLength(left, right);
    }

    @Specialization(order = 131, guards = "areSameLength")
    public RDoubleVector doDoubleDoubleSameLength(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorDoubleVectorOpSameLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 132, guards = "areSameLength")
    public RDoubleVector doDoubleDoubleSameLength(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorDoubleVectorOpSameLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 133, guards = "!areSameLength")
    public RDoubleVector doDoubleDouble(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performDoubleVectorDoubleVectorOp(left, right);
    }

    @Specialization(order = 134, guards = "!areSameLength")
    public RDoubleVector doDoubleDouble(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorDoubleVectorOp(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 135, guards = "!areSameLength")
    public RDoubleVector doDoubleDouble(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorDoubleVectorOp(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    // complex vector specializations

    @Specialization(order = 139)
    public RComplex doDoubleDouble(RComplex left, RComplex right) {
        return performArithmeticComplex(left, right);
    }

    @Specialization(order = 140, guards = "areSameLength")
    public RComplexVector doComplexComplexSameLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        return performComplexVectorComplexVectorOpSameLength(left, right);
    }

    @Specialization(order = 141, guards = "areSameLength")
    public RComplexVector doIntComplexSameLength(RAbstractIntVector left, RAbstractComplexVector right) {
        return performComplexVectorComplexVectorOpSameLength(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 142, guards = "areSameLength")
    public RComplexVector doDoubleComplexSameLength(RAbstractDoubleVector left, RAbstractComplexVector right) {
        return performComplexVectorComplexVectorOpSameLength(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 143, guards = "areSameLength")
    public RComplexVector doComplexIntSameLength(RAbstractComplexVector left, RAbstractIntVector right) {
        return performComplexVectorComplexVectorOpSameLength(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 144, guards = "areSameLength")
    public RComplexVector doComplexDoubleSameLength(RAbstractComplexVector left, RAbstractDoubleVector right) {
        return performComplexVectorComplexVectorOpSameLength(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 145, guards = "!areSameLength")
    public RComplexVector doComplexComplex(RAbstractComplexVector left, RAbstractComplexVector right) {
        return performComplexVectorComplexVectorOp(left, right);
    }

    @Specialization(order = 146, guards = "!areSameLength")
    public RComplexVector doIntComplex(RAbstractIntVector left, RAbstractComplexVector right) {
        return performComplexVectorComplexVectorOp(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 147, guards = "!areSameLength")
    public RComplexVector doDoubleComplex(RAbstractDoubleVector left, RAbstractComplexVector right) {
        return performComplexVectorComplexVectorOp(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 148, guards = "!areSameLength")
    public RComplexVector doComplexInt(RAbstractComplexVector left, RAbstractIntVector right) {
        return performComplexVectorComplexVectorOp(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 149, guards = "!areSameLength")
    public RComplexVector doComplexDouble(RAbstractComplexVector left, RAbstractDoubleVector right) {
        return performComplexVectorComplexVectorOp(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    // old specializations

    private RComplexVector performComplexVectorComplexVectorOp(RAbstractComplexVector left, RAbstractComplexVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int length = Math.max(leftLength, rightLength);
        double[] result = new double[length << 1];
        int j = 0;
        int k = 0;
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = left.getDataAt(k);
            RComplex rightValue = right.getDataAt(j);
            RComplex resultValue = performArithmeticComplex(leftValue, rightValue);
            int index = i << 1;
            result[index] = resultValue.getRealPart();
            result[index + 1] = resultValue.getImaginaryPart();
            j = Utils.incMod(j, rightLength);
            k = Utils.incMod(k, leftLength);
        }
        return RDataFactory.createComplexVector(result, isComplete());
    }

    private RDoubleVector performDoubleVectorDoubleVectorOp(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int length = Math.max(leftLength, rightLength);
        double[] result = new double[length];
        int j = 0;
        int k = 0;
        for (int i = 0; i < length; ++i) {
            double leftValue = left.getDataAt(k);
            double rightValue = right.getDataAt(j);
            result[i] = performArithmeticDouble(leftValue, rightValue);
            j = Utils.incMod(j, rightLength);
            k = Utils.incMod(k, leftLength);
        }
        return RDataFactory.createDoubleVector(result, isComplete());
    }

    private RIntVector performIntVectorIntVectorOpInt(RAbstractIntVector left, RAbstractIntVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int length = Math.max(leftLength, rightLength);
        int[] result = new int[length];
        int j = 0;
        int k = 0;
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(k);
            int rightValue = right.getDataAt(j);
            result[i] = performArithmetic(leftValue, rightValue);
            j = Utils.incMod(j, rightLength);
            k = Utils.incMod(k, leftLength);
        }
        return RDataFactory.createIntVector(result, isComplete());
    }

    private RDoubleVector performIntVectorIntVectorOpIntDouble(RAbstractIntVector left, RAbstractIntVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int length = Math.max(leftLength, rightLength);
        double[] result = new double[length];
        int j = 0;
        int k = 0;
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(k);
            int rightValue = right.getDataAt(j);
            result[i] = performArithmeticDouble(leftValue, rightValue);
            j = Utils.incMod(j, rightLength);
            k = Utils.incMod(k, leftLength);
        }
        return RDataFactory.createDoubleVector(result, isComplete());
    }

    private RComplexVector performComplexVectorComplexVectorOpSameLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        double[] result = new double[length << 1];
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = left.getDataAt(i);
            RComplex rightValue = right.getDataAt(i);
            RComplex resultValue = performArithmeticComplex(leftValue, rightValue);
            int index = i << 1;
            result[index] = resultValue.getRealPart();
            result[index + 1] = resultValue.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(result, isComplete());
    }

    private RDoubleVector performDoubleVectorDoubleVectorOpSameLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            double leftValue = left.getDataAt(i);
            double rightValue = right.getDataAt(i);
            result[i] = performArithmeticDouble(leftValue, rightValue);
        }
        return RDataFactory.createDoubleVector(result, isComplete());
    }

    private RIntVector performIntVectorIntVectorOpSameLengthInt(RAbstractIntVector left, RAbstractIntVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            int rightValue = right.getDataAt(i);
            result[i] = performArithmetic(leftValue, rightValue);
        }
        return RDataFactory.createIntVector(result, isComplete());
    }

    private RDoubleVector performIntVectorIntVectorOpSameLengthIntDouble(RAbstractIntVector left, RAbstractIntVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            int rightValue = right.getDataAt(i);
            result[i] = performArithmeticIntIntDouble(leftValue, rightValue);
        }
        return RDataFactory.createDoubleVector(result, isComplete());
    }

    private double performArithmeticDouble(double left, double right) {
        if (leftNACheck.check(left)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && right == 0) {
                // CORNER: Make sure NA^0 == 1
                return 1;
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod && right == 0) {
                // CORNER: Make sure NA%%0 == NaN
                return Double.NaN;
            }
            return RRuntime.DOUBLE_NA;
        }
        if (rightNACheck.check(right)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && left == 1) {
                // CORNER: Make sure 1^NA == 1
                return 1;
            }
            if (leftNACheck.checkNAorNaN(left)) {
                // CORNER: Make sure NaN op NA == NaN
                return left;
            }
            return RRuntime.DOUBLE_NA;
        }
        return arithmetic.op(left, right);
    }

    private RComplex performArithmeticComplex(RComplex left, RComplex right) {
        if (leftNACheck.check(left)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && right.isZero()) {
                // CORNER: (0i + NA)^0 == 1
                return RDataFactory.createComplexRealOne();
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw RError.getUnimplementedComplex(this.getEncapsulatingSourceSection());
            }
            return RRuntime.createComplexNA();
        }
        if (rightNACheck.check(right)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && left.isZero()) {
                // CORNER: 0^(0i + NA) == NaN + NaNi
                return RDataFactory.createComplex(Double.NaN, Double.NaN);
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw RError.getUnimplementedComplex(this.getEncapsulatingSourceSection());
            }
            return RRuntime.createComplexNA();
        }
        return arithmetic.op(left.getRealPart(), left.getImaginaryPart(), right.getRealPart(), right.getImaginaryPart());
    }

    private boolean isComplete() {
        return leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA() && resultNACheck.neverSeenNA();
    }

    private int performArithmetic(int left, int right) {
        if (leftNACheck.check(left)) {
            return RRuntime.INT_NA;
        }
        if (rightNACheck.check(right)) {
            return RRuntime.INT_NA;
        }
        int value = arithmetic.op(left, right);
        resultNACheck.check(value);
        return value;
    }

    private double performArithmeticIntIntDouble(int left, int right) {
        if (leftNACheck.check(left)) {
            return RRuntime.DOUBLE_NA;
        }
        if (rightNACheck.check(right)) {
            return RRuntime.DOUBLE_NA;
        }
        return arithmetic.op(leftNACheck.convertIntToDouble(left), rightNACheck.convertIntToDouble(right));
    }

    @NodeField(name = "NACheck", type = NACheck.class)
    @NodeChild("operand")
    public abstract static class ArithmeticCast extends RNode {

        protected abstract NACheck getNACheck();

        @Specialization
        public RNull doNull(RNull operand) {
            return operand;
        }

        @Specialization
        public RAbstractIntVector doInt(RAbstractIntVector operand) {
            getNACheck().enable(!operand.isComplete());
            return operand;
        }

        @Specialization
        public RAbstractDoubleVector doDouble(RAbstractDoubleVector operand) {
            getNACheck().enable(!operand.isComplete());
            return operand;
        }

        @Specialization
        public RAbstractComplexVector doComplex(RAbstractComplexVector operand) {
            getNACheck().enable(!operand.isComplete());
            return operand;
        }

        @Specialization
        public RIntVector doLogical(RAbstractLogicalVector operand) {
            getNACheck().enable(!operand.isComplete());
            return logicalToInt(operand);
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object doString(RAbstractStringVector operand) {
            throw RError.getNonNumericBinary(this.getSourceSection());
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object doRaw(RAbstractRawVector operand) {
            throw RError.getNonNumericBinary(this.getSourceSection());
        }

        private RIntVector logicalToInt(RAbstractLogicalVector operand) {
            int[] result = new int[operand.getLength()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = getNACheck().convertLogicalToInt(operand.getDataAt(i));
            }
            return RDataFactory.createIntVector(result, getNACheck().neverSeenNA());
        }
    }
}
