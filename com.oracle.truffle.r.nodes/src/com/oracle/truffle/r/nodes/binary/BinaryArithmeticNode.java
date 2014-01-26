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
import com.oracle.truffle.r.nodes.binary.BinaryArithmeticNodeFactory.ArithmeticCastFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@SuppressWarnings("unused")
public abstract class BinaryArithmeticNode extends BinaryNode {

    @Child private BinaryArithmetic binary;
    @Child private UnaryArithmetic unary;

    private final NACheck leftNACheck;
    private final NACheck rightNACheck;
    private final NACheck resultNACheck;

    public BinaryArithmeticNode(BinaryArithmeticFactory factory, UnaryArithmeticFactory unaryFactory) {
        this.binary = adoptChild(factory.create());
        this.unary = unaryFactory != null ? adoptChild(unaryFactory.create()) : null;
        leftNACheck = new NACheck();
        rightNACheck = new NACheck();
        resultNACheck = new NACheck();
    }

    public BinaryArithmeticNode(BinaryArithmeticNode op) {
        this.binary = adoptChild(op.binary);
        this.unary = adoptChild(op.unary);
        this.leftNACheck = op.leftNACheck;
        this.rightNACheck = op.rightNACheck;
        this.resultNACheck = op.resultNACheck;
    }

    public static BinaryArithmeticNode create(BinaryArithmeticFactory arithmetic) {
        return BinaryArithmeticNodeFactory.create(arithmetic, null, new RNode[2], null, null);
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
        return binary.isSupportsIntResult();
    }

    // unary operations

    @Specialization(order = 50)
    public int doUnaryInt(int left, RMissing right) {
        checkUnary();
        if (leftNACheck.check(left)) {
            return RRuntime.INT_NA;
        }
        return unary.op(left);
    }

    @Specialization(order = 51)
    public double doUnaryDouble(double left, RMissing right) {
        checkUnary();
        if (leftNACheck.check(left)) {
            return RRuntime.DOUBLE_NA;
        }
        return unary.op(left);
    }

    @Specialization(order = 52)
    public RComplex doUnaryComplex(RComplex left, RMissing right) {
        checkUnary();
        if (leftNACheck.check(left)) {
            return RRuntime.createComplexNA();
        }
        return unary.op(left.getRealPart(), left.getImaginaryPart());
    }

    @Specialization(order = 53)
    public RIntVector doUnaryIntVector(RIntVector left, RMissing right) {
        checkUnary();
        int[] res = new int[left.getLength()];
        leftNACheck.enable(left);
        for (int i = 0; i < left.getLength(); ++i) {
            if (leftNACheck.check(left.getDataAt(i))) {
                res[i] = RRuntime.INT_NA;
            } else {
                res[i] = unary.op(left.getDataAt(i));
            }
        }
        return RDataFactory.createIntVector(res, leftNACheck.neverSeenNA(), left.getDimensions());
    }

    @Specialization(order = 54)
    public RDoubleVector doUnaryDoubleVector(RDoubleVector left, RMissing right) {
        checkUnary();
        double[] res = new double[left.getLength()];
        leftNACheck.enable(left);
        for (int i = 0; i < left.getLength(); ++i) {
            if (leftNACheck.check(left.getDataAt(i))) {
                res[i] = RRuntime.DOUBLE_NA;
            } else {
                res[i] = unary.op(left.getDataAt(i));
            }
        }
        return RDataFactory.createDoubleVector(res, leftNACheck.neverSeenNA(), left.getDimensions());
    }

    @Specialization(order = 55)
    public RComplexVector doComplexVectorNA(RComplexVector left, RMissing right) {
        double[] res = new double[left.getLength() * 2];
        leftNACheck.enable(left);
        for (int i = 0; i < left.getLength(); ++i) {
            if (leftNACheck.check(left.getDataAt(i))) {
                res[2 * i] = RRuntime.DOUBLE_NA;
                res[2 * i + 1] = 0.0;
            } else {
                RComplex r = unary.op(left.getDataAt(i).getRealPart(), left.getDataAt(i).getImaginaryPart());
                res[2 * i] = r.getRealPart();
                res[2 * i + 1] = r.getImaginaryPart();
            }
        }
        return RDataFactory.createComplexVector(res, leftNACheck.neverSeenNA(), left.getDimensions());
    }

    private void checkUnary() {
        if (unary == null) {
            throw RError.getArgumentEmpty(getSourceSection(), 2);
        }
    }

    // Scalar operations

    @Specialization(order = 110, guards = "supportsIntResult")
    public int doIntInt(int left, int right) {
        return performArithmetic(left, right);
    }

    @Specialization(order = 111, guards = "!supportsIntResult")
    public double doIntIntDouble(int left, int right) {
        return performArithmeticIntIntDouble(left, right);
    }

    @Specialization(order = 112)
    public double doIntDouble(int left, double right) {
        return performArithmeticDouble(leftNACheck.convertIntToDouble(left), right);
    }

    @Specialization(order = 113)
    public RComplex doIntComplex(int left, RComplex right) {
        return performArithmeticComplex(leftNACheck.convertIntToComplex(left), right);
    }

    @Specialization(order = 120)
    public double doDoubleInt(double left, int right) {
        return performArithmeticDouble(left, rightNACheck.convertIntToDouble(right));
    }

    @Specialization(order = 121)
    public double doDoubleDouble(double left, double right) {
        return performArithmeticDouble(left, right);
    }

    @Specialization(order = 122)
    public RComplex doDoubleComplex(double left, RComplex right) {
        return performArithmeticComplex(leftNACheck.convertDoubleToComplex(left), right);
    }

    @Specialization(order = 130)
    public RComplex doComplexInt(RComplex left, int right) {
        return performArithmeticComplex(left, rightNACheck.convertIntToComplex(right));
    }

    @Specialization(order = 131)
    public RComplex doComplexDouble(RComplex left, double right) {
        return performArithmeticComplex(left, rightNACheck.convertDoubleToComplex(right));
    }

    @Specialization(order = 132)
    public RComplex doComplexComplex(RComplex left, RComplex right) {
        return performArithmeticComplex(left, right);
    }

    // Scalar + Vector mix operations

    @Specialization(order = 210, guards = "supportsIntResult")
    public RIntVector doIntScalarIntVector(int left, RIntVector right) {
        return performScalarIntVectorOp(left, right);
    }

    @Specialization(order = 1210, guards = "supportsIntResult")
    public RIntVector doIntScalarIntVector(int left, RIntSequence right) {
        return performScalarIntVectorOp(left, right);
    }

    @Specialization(order = 211, guards = "!supportsIntResult")
    public RDoubleVector doIntScalarIntVectorDouble(int left, RIntVector right) {
        return performScalarIntVectorOpDouble(left, right);
    }

    @Specialization(order = 1211, guards = "!supportsIntResult")
    public RDoubleVector doIntScalarIntVectorDouble(int left, RIntSequence right) {
        return performScalarIntVectorOpDouble(left, right);
    }

    @Specialization(order = 212)
    public RDoubleVector doIntScalarDoubleVector(int left, RDoubleVector right) {
        return performScalarDoubleVectorOp(leftNACheck.convertIntToDouble(left), right);
    }

    @Specialization(order = 213)
    public RComplexVector doIntScalarComplexVector(int left, RComplexVector right) {
        return performScalarComplexVectorOp(leftNACheck.convertIntToComplex(left), right);
    }

    @Specialization(order = 220)
    public RDoubleVector doDoubleScalarIntVector(double left, RIntVector right) {
        return performScalarDoubleWithIntVectorOp(left, right);
    }

    @Specialization(order = 1220)
    public RDoubleVector doDoubleScalarIntVector(double left, RIntSequence right) {
        return performScalarDoubleWithIntVectorOp(left, right);
    }

    @Specialization(order = 221)
    public RDoubleVector doDoubleScalarDoubleVector(double left, RDoubleVector right) {
        return performScalarDoubleVectorOp(left, right);
    }

    @Specialization(order = 222)
    public RComplexVector doDoubleScalarComplexVector(double left, RComplexVector right) {
        return performScalarComplexVectorOp(leftNACheck.convertDoubleToComplex(left), right);
    }

    @Specialization(order = 230)
    public RComplexVector doComplexScalarIntVector(RComplex left, RIntVector right) {
        return performScalarComplexVectorOp(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1230)
    public RComplexVector doComplexScalarIntVector(RComplex left, RIntSequence right) {
        return performScalarComplexVectorOp(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 231)
    public RComplexVector doComplexScalarIntVector(RComplex left, RDoubleVector right) {
        return performScalarComplexVectorOp(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 232)
    public RComplexVector doComplexScalarIntVector(RComplex left, RComplexVector right) {
        return performScalarComplexVectorOp(left, right);
    }

    @Specialization(order = 240)
    public RIntVector doLogicalScalarIntVector(byte left, RIntVector right) {
        return performScalarIntVectorOp(leftNACheck.convertLogicalToInt(left), right);
    }

    @Specialization(order = 1240)
    public RIntVector doLogicalScalarIntVector(byte left, RIntSequence right) {
        return performScalarIntVectorOp(leftNACheck.convertLogicalToInt(left), right);
    }

    @Specialization(order = 241)
    public RDoubleVector doLogicalScalarDoubleVector(byte left, RDoubleVector right) {
        return performScalarDoubleVectorOp(leftNACheck.convertLogicalToDouble(left), right);
    }

    @Specialization(order = 242)
    public RComplexVector doLogicalScalarComplexVector(byte left, RComplexVector right) {
        return performScalarComplexVectorOp(leftNACheck.convertLogicalToComplex(left), right);
    }

    // Vector + Scalar mix operations

    @Specialization(order = 310, guards = "supportsIntResult")
    public RIntVector doIntVectorScalar(RIntVector left, int right) {
        return performIntVectorOp(left, right);
    }

    @Specialization(order = 311, guards = "!supportsIntResult")
    public RDoubleVector doIntVectorScalarDouble(RIntVector left, int right) {
        return performIntVectorOpDouble(left, right);
    }

    @Specialization(order = 312)
    public RDoubleVector doIntVectorScalar(RIntVector left, double right) {
        return performDoubleVectorOp(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 313)
    public RComplexVector doIntVectorScalar(RIntVector left, RComplex right) {
        return performComplexVectorScalarOp(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 314)
    public RIntVector doIntVectorScalar(RIntVector left, byte right) {
        return performIntVectorOp(left, rightNACheck.convertLogicalToInt(right));
    }

    @Specialization(order = 315, guards = "supportsIntResult")
    public RIntVector doIntVectorScalar(RIntSequence left, int right) {
        return performIntVectorOp(left, right);
    }

    @Specialization(order = 316, guards = "!supportsIntResult")
    public RDoubleVector doIntVectorScalarDouble(RIntSequence left, int right) {
        return performIntVectorOpDouble(left, right);
    }

    @Specialization(order = 317, guards = "canCreateSequenceResultWithRight")
    public RDoubleSequence doIntVectorScalarDoubleSequence(RIntSequence left, double right) {
        if (this.binary instanceof BinaryArithmetic.Multiply) {
            return RDataFactory.createDoubleSequence(left.getStart() * right, left.getStride() * right, left.getLength());
        } else if (this.binary instanceof BinaryArithmetic.Subtract) {
            return RDataFactory.createDoubleSequence(left.getStart() - right, left.getStride(), left.getLength());
        } else {
            assert this.binary instanceof BinaryArithmetic.Add;
            return RDataFactory.createDoubleSequence(left.getStart() + right, left.getStride(), left.getLength());
        }
    }

    @Specialization(order = 334, guards = "!canCreateSequenceResultWithRight")
    public RDoubleVector doIntVectorScalar(RIntSequence left, double right) {
        return performDoubleVectorOp(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 335, guards = "canCreateSequenceResultWithLeft")
    public RDoubleSequence doIntVectorScalarDoubleSequence(double left, RDoubleSequence right) {
        if (this.binary instanceof BinaryArithmetic.Multiply) {
            return RDataFactory.createDoubleSequence(right.getStart() * left, right.getStride() * left, right.getLength());
        } else {
            assert this.binary instanceof BinaryArithmetic.Add;
            return RDataFactory.createDoubleSequence(right.getStart() + left, right.getStride(), right.getLength());
        }
    }

    @Specialization(order = 336, guards = "!canCreateSequenceResultWithLeft")
    public RDoubleVector doIntVectorScalar(double left, RDoubleSequence right) {
        return performScalarDoubleVectorOp(left, right);
    }

    @Specialization(order = 337)
    public RDoubleVector doDoubleSequenceDoubleOp(RDoubleSequence left, double right) {
        return performDoubleVectorOp(left, right);
    }

    public boolean canCreateSequenceResultWithRight(Object left, double right) {
        return !this.rightNACheck.check(right) && (this.binary instanceof BinaryArithmetic.Multiply || this.binary instanceof BinaryArithmetic.Add || this.binary instanceof BinaryArithmetic.Subtract);
    }

    public boolean canCreateSequenceResultWithLeft(double left, Object right) {
        return !this.leftNACheck.check(left) && (this.binary instanceof BinaryArithmetic.Multiply || this.binary instanceof BinaryArithmetic.Add);
    }

    @Specialization(order = 318)
    public RComplexVector doIntVectorScalar(RIntSequence left, RComplex right) {
        return performComplexVectorScalarOp(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 319)
    public RIntVector doIntVectorScalar(RIntSequence left, byte right) {
        return performIntVectorOp(left, rightNACheck.convertLogicalToInt(right));
    }

    @Specialization(order = 320)
    public RDoubleVector doDoubleVectorScalar(RDoubleVector left, int right) {
        return performDoubleVectorOp(left, rightNACheck.convertIntToDouble(right));
    }

    @Specialization(order = 321)
    public RDoubleVector doDoubleVectorScalar(RDoubleVector left, double right) {
        return performDoubleVectorOp(left, right);
    }

    @Specialization(order = 322)
    public RComplexVector doDoubleVectorScalar(RDoubleVector left, RComplex right) {
        return performComplexVectorScalarOp(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 323)
    public RDoubleVector doDoubleVectorScalar(RDoubleVector left, byte right) {
        return performDoubleVectorOp(left, rightNACheck.convertLogicalToDouble(right));
    }

    @Specialization(order = 330)
    public RComplexVector doComplexVectorScalar(RComplexVector left, int right) {
        return performComplexVectorScalarOp(left, rightNACheck.convertIntToComplex(right));
    }

    @Specialization(order = 331)
    public RComplexVector doComplexVectorScalar(RComplexVector left, double right) {
        return performComplexVectorScalarOp(left, rightNACheck.convertDoubleToComplex(right));
    }

    @Specialization(order = 332)
    public RComplexVector doComplexVectorScalar(RComplexVector left, RComplex right) {
        return performComplexVectorScalarOp(left, right);
    }

    @Specialization(order = 333)
    public RComplexVector doComplexVectorScalar(RComplexVector left, byte right) {
        return performComplexVectorScalarOp(left, rightNACheck.convertLogicalToComplex(right));
    }

    // Vector + Vector operations, with same length

    @Specialization(order = 410, guards = {"areSameLength", "supportsIntResult"})
    public RIntVector doIntVectorIntVectorSameLengthReturnIntVector(RIntVector left, RIntVector right) {
        return performIntVectorIntVectorOpSameLengthInt(left, right);
    }

    @Specialization(order = 411, guards = {"areSameLength", "!supportsIntResult"})
    public RDoubleVector doIntVectorIntVectorSameLengthReturnIntVectorDouble(RIntVector left, RIntVector right) {
        return performIntVectorIntVectorOpSameLengthIntDouble(left, right);
    }

    @Specialization(order = 1410, guards = {"areSameLength", "supportsIntResult"})
    public RIntVector doIntVectorIntVectorSameLengthReturnIntVector(RIntVector left, RIntSequence right) {
        return performIntVectorIntVectorOpSameLengthInt(left, right);
    }

    @Specialization(order = 1411, guards = {"areSameLength", "!supportsIntResult"})
    public RDoubleVector doIntVectorIntVectorSameLengthReturnIntVectorDouble(RIntVector left, RIntSequence right) {
        return performIntVectorIntVectorOpSameLengthIntDouble(left, right);
    }

    @Specialization(order = 1412, guards = {"areSameLength", "supportsIntResult"})
    public RIntVector doIntVectorIntVectorSameLengthReturnIntVector(RIntSequence left, RIntVector right) {
        return performIntVectorIntVectorOpSameLengthInt(left, right);
    }

    @Specialization(order = 1413, guards = {"areSameLength", "!supportsIntResult"})
    public RDoubleVector doIntVectorIntVectorSameLengthReturnIntVectorDouble(RIntSequence left, RIntVector right) {
        return performIntVectorIntVectorOpSameLengthIntDouble(left, right);
    }

    @Specialization(order = 1414, guards = {"areSameLength", "supportsIntResult"})
    public RIntVector doIntVectorIntVectorSameLengthReturnIntVector(RIntSequence left, RIntSequence right) {
        return performIntVectorIntVectorOpSameLengthInt(left, right);
    }

    @Specialization(order = 1415, guards = {"areSameLength", "!supportsIntResult"})
    public RDoubleVector doIntVectorIntVectorSameLengthReturnIntVectorDouble(RIntSequence left, RIntSequence right) {
        return performIntVectorIntVectorOpSameLengthIntDouble(left, right);
    }

    @Specialization(order = 412, guards = "areSameLength")
    public RDoubleVector doIntVectorDoubleVectorSameLength(RIntVector left, RDoubleVector right) {
        return performDoubleVectorDoubleVectorOpSameLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 413, guards = "areSameLength")
    public RComplexVector doIntVectorComplexVectorSameLength(RIntVector left, RComplexVector right) {
        return performComplexVectorComplexVectorOpSameLength(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 1416, guards = "areSameLength")
    public RDoubleVector doIntVectorDoubleVectorSameLength(RIntSequence left, RDoubleVector right) {
        return performDoubleVectorDoubleVectorOpSameLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 1417, guards = "areSameLength")
    public RComplexVector doIntVectorComplexVectorSameLength(RIntSequence left, RComplexVector right) {
        return performComplexVectorComplexVectorOpSameLength(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 420, guards = "areSameLength")
    public RDoubleVector doIntVectorDoubleVectorSameLength(RDoubleVector left, RIntVector right) {
        return performDoubleVectorDoubleVectorOpSameLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 1420, guards = "areSameLength")
    public RDoubleVector doIntVectorDoubleVectorSameLength(RDoubleVector left, RIntSequence right) {
        return performDoubleVectorDoubleVectorOpSameLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 421, guards = "areSameLength")
    public RDoubleVector doIntVectorDoubleVectorSameLength(RDoubleVector left, RDoubleVector right) {
        return performDoubleVectorDoubleVectorOpSameLength(left, right);
    }

    @Specialization(order = 422, guards = "areSameLength")
    public RComplexVector doIntVectorDoubleVectorSameLength(RDoubleVector left, RComplexVector right) {
        return performComplexVectorComplexVectorOpSameLength(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 430, guards = "areSameLength")
    public RComplexVector doComplexVectorIntVectorSameLength(RComplexVector left, RIntVector right) {
        return performComplexVectorComplexVectorOpSameLength(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1430, guards = "areSameLength")
    public RComplexVector doComplexVectorIntVectorSameLength(RComplexVector left, RIntSequence right) {
        return performComplexVectorComplexVectorOpSameLength(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 431, guards = "areSameLength")
    public RComplexVector doComplexVectorIntVectorSameLength(RComplexVector left, RDoubleVector right) {
        return performComplexVectorComplexVectorOpSameLength(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 432, guards = "areSameLength")
    public RComplexVector doIntVectorDoubleVectorSameLength(RComplexVector left, RComplexVector right) {
        return performComplexVectorComplexVectorOpSameLength(left, right);
    }

    @Specialization(order = 450, guards = "areSameLength")
    public RIntVector doIntVectorLogicalVectorSameLength(RIntVector left, RLogicalVector right) {
        return performIntVectorIntVectorOpSameLengthInt(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(order = 1450, guards = "areSameLength")
    public RIntVector doIntVectorLogicalVectorSameLength(RIntSequence left, RLogicalVector right) {
        return performIntVectorIntVectorOpSameLengthInt(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(order = 451, guards = "areSameLength")
    public RIntVector doIntVectorLogicalVectorSameLength(RLogicalVector left, RIntVector right) {
        return performIntVectorIntVectorOpSameLengthInt(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    @Specialization(order = 1451, guards = "areSameLength")
    public RIntVector doIntVectorLogicalVectorSameLength(RLogicalVector left, RIntSequence right) {
        return performIntVectorIntVectorOpSameLengthInt(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    @Specialization(order = 452, guards = "areSameLength")
    public RDoubleVector doDoubeVectorLogicalVectorSameLength(RDoubleVector left, RLogicalVector right) {
        return performDoubleVectorDoubleVectorOpSameLength(left, RClosures.createLogicalToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 453, guards = "areSameLength")
    public RDoubleVector doDoubleVectorLogicalVectorSameLength(RLogicalVector left, RDoubleVector right) {
        return performDoubleVectorDoubleVectorOpSameLength(RClosures.createLogicalToDoubleVector(left, leftNACheck), right);
    }

    // Vector + Vector operations, not same length

    @Specialization(order = 510, guards = "supportsIntResult")
    public RIntVector doIntVectorIntVectorReturnIntVector(RIntVector left, RIntVector right) {
        return performIntVectorIntVectorOpInt(left, right);
    }

    public RDoubleVector doIntVectorIntVectorReturnIntVectorDouble(RIntVector left, RIntVector right) {
        return performIntVectorIntVectorOpIntDouble(left, right);
    }

    @Specialization(order = 1510, guards = "supportsIntResult")
    public RIntVector doIntVectorIntVectorReturnIntVector(RIntVector left, RIntSequence right) {
        return performIntVectorIntVectorOpInt(left, right);
    }

    public RDoubleVector doIntVectorIntVectorReturnIntVectorDouble(RIntVector left, RIntSequence right) {
        return performIntVectorIntVectorOpIntDouble(left, right);
    }

    @Specialization(order = 1511, guards = "supportsIntResult")
    public RIntVector doIntVectorIntVectorReturnIntVector(RIntSequence left, RIntSequence right) {
        return performIntVectorIntVectorOpInt(left, right);
    }

    public RDoubleVector doIntVectorIntVectorReturnIntVectorDouble(RIntSequence left, RIntSequence right) {
        return performIntVectorIntVectorOpIntDouble(left, right);
    }

    @Specialization(order = 1612, guards = "supportsIntResult")
    public RIntVector doIntVectorIntVectorReturnIntVector(RIntSequence left, RIntVector right) {
        return performIntVectorIntVectorOpInt(left, right);
    }

    public RDoubleVector doIntVectorIntVectorReturnIntVectorDouble(RIntSequence left, RIntVector right) {
        return performIntVectorIntVectorOpIntDouble(left, right);
    }

    @Specialization(order = 512)
    public RDoubleVector doIntVectorDoubleVector(RIntVector left, RDoubleVector right) {
        return performDoubleVectorDoubleVectorOp(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 513)
    public RComplexVector doIntVectorComplexVector(RIntVector left, RComplexVector right) {
        return performComplexVectorComplexVectorOp(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 1512)
    public RDoubleVector doIntVectorDoubleVector(RIntSequence left, RDoubleVector right) {
        return performDoubleVectorDoubleVectorOp(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(order = 1513)
    public RComplexVector doIntVectorComplexVector(RIntSequence left, RComplexVector right) {
        return performComplexVectorComplexVectorOp(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 520)
    public RDoubleVector doIntVectorDoubleVector(RDoubleVector left, RIntVector right) {
        return performDoubleVectorDoubleVectorOp(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 1520)
    public RDoubleVector doIntVectorDoubleVector(RDoubleVector left, RIntSequence right) {
        return performDoubleVectorDoubleVectorOp(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(order = 521)
    public RDoubleVector doIntVectorDoubleVector(RDoubleVector left, RDoubleVector right) {
        return performDoubleVectorDoubleVectorOp(left, right);
    }

    @Specialization(order = 522)
    public RComplexVector doIntVectorDoubleVector(RDoubleVector left, RComplexVector right) {
        return performComplexVectorComplexVectorOp(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(order = 530)
    public RComplexVector doComplexVectorIntVector(RComplexVector left, RIntVector right) {
        return performComplexVectorComplexVectorOp(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 1530)
    public RComplexVector doComplexVectorIntVector(RComplexVector left, RIntSequence right) {
        return performComplexVectorComplexVectorOp(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 531)
    public RComplexVector doComplexVectorIntVector(RComplexVector left, RDoubleVector right) {
        return performComplexVectorComplexVectorOp(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    @Specialization(order = 532)
    public RComplexVector doIntVectorDoubleVector(RComplexVector left, RComplexVector right) {
        return performComplexVectorComplexVectorOp(left, right);
    }

    private RIntVector performIntVectorOp(RAbstractIntVector left, int right) {
        int length = left.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            result[i] = performArithmetic(leftValue, right);
        }
        return RDataFactory.createIntVector(result, isComplete(), left.getDimensions());
    }

    private RDoubleVector performIntVectorOpDouble(RAbstractIntVector left, int right) {
        int length = left.getLength();
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            result[i] = performArithmeticIntIntDouble(leftValue, right);
        }
        return RDataFactory.createDoubleVector(result, isComplete(), left.getDimensions());
    }

    private RIntVector performScalarIntVectorOp(int left, RAbstractIntVector right) {
        int length = right.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; ++i) {
            int rightValue = right.getDataAt(i);
            result[i] = performArithmetic(left, rightValue);
        }
        return RDataFactory.createIntVector(result, isComplete(), right.getDimensions());
    }

    private RDoubleVector performScalarIntVectorOpDouble(int left, RAbstractIntVector right) {
        int length = right.getLength();
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            int rightValue = right.getDataAt(i);
            result[i] = performArithmeticIntIntDouble(left, rightValue);
        }
        return RDataFactory.createDoubleVector(result, isComplete(), right.getDimensions());
    }

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
        return RDataFactory.createComplexVector(result, isComplete(), largerDim(left, right));
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
        return RDataFactory.createDoubleVector(result, isComplete(), largerDim(left, right));
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
        return RDataFactory.createIntVector(result, isComplete(), largerDim(left, right));
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
        return RDataFactory.createDoubleVector(result, isComplete(), largerDim(left, right));
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
        return RDataFactory.createComplexVector(result, isComplete(), left.getDimensions());
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
        return RDataFactory.createDoubleVector(result, isComplete(), left.getDimensions());
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
        return RDataFactory.createIntVector(result, isComplete(), left.getDimensions());
    }

    private RDoubleVector performIntVectorIntVectorOpSameLengthIntDouble(RAbstractIntVector left, RAbstractIntVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            int rightValue = right.getDataAt(i);
            result[i] = performArithmeticDouble(leftValue, rightValue);
        }
        return RDataFactory.createDoubleVector(result, isComplete(), left.getDimensions());
    }

    private RDoubleVector performDoubleVectorOp(RAbstractDoubleVector left, double right) {
        int length = left.getLength();
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            double leftValue = left.getDataAt(i);
            result[i] = performArithmeticDouble(leftValue, right);
        }
        return RDataFactory.createDoubleVector(result, isComplete(), left.getDimensions());
    }

    private RComplexVector performComplexVectorScalarOp(RAbstractComplexVector left, RComplex right) {
        int length = left.getLength();
        double[] result = new double[length << 1];
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = left.getDataAt(i);
            RComplex complex = performArithmeticComplex(leftValue, right);
            int index = i << 1;
            result[index] = complex.getRealPart();
            result[index + 1] = complex.getImaginaryPart();

        }
        return RDataFactory.createComplexVector(result, isComplete(), left.getDimensions());
    }

    private RDoubleVector performScalarDoubleWithIntVectorOp(double left, RAbstractIntVector right) {
        int length = right.getLength();
        this.rightNACheck.enable(!right.isComplete());
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            double rightValue = rightNACheck.convertIntToDouble(right.getDataAt(i));
            result[i] = performArithmeticDouble(left, rightValue);
        }
        return RDataFactory.createDoubleVector(result, isComplete(), right.getDimensions());
    }

    private RDoubleVector performScalarDoubleVectorOp(double left, RAbstractDoubleVector right) {
        int length = right.getLength();
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            double rightValue = right.getDataAt(i);
            result[i] = performArithmeticDouble(left, rightValue);
        }
        return RDataFactory.createDoubleVector(result, isComplete(), right.getDimensions());
    }

    private RComplexVector performScalarComplexVectorOp(RComplex left, RAbstractComplexVector right) {
        int length = right.getLength();
        double[] result = new double[length << 1];
        for (int i = 0; i < length; ++i) {
            RComplex rightValue = right.getDataAt(i);
            RComplex complex = performArithmeticComplex(left, rightValue);
            int index = i << 1;
            result[index] = complex.getRealPart();
            result[index + 1] = complex.getImaginaryPart();

        }
        return RDataFactory.createComplexVector(result, isComplete(), right.getDimensions());
    }

    private double performArithmeticDouble(double left, double right) {
        if (leftNACheck.check(left)) {
            if (this.binary instanceof BinaryArithmetic.Pow && right == 0) {
                // CORNER: Make sure NA^0 == 1
                return 1;
            } else if (this.binary instanceof BinaryArithmetic.Mod && right == 0) {
                // CORNER: Make sure NA%%0 == NaN
                return Double.NaN;
            }
            return RRuntime.DOUBLE_NA;
        }
        if (rightNACheck.check(right)) {
            if (this.binary instanceof BinaryArithmetic.Pow && left == 1) {
                // CORNER: Make sure 1^NA == 1
                return 1;
            }
            if (leftNACheck.checkNAorNaN(left)) {
                // CORNER: Make sure NaN op NA == NaN
                return left;
            }
            return RRuntime.DOUBLE_NA;
        }
        return binary.op(left, right);
    }

    private RComplex performArithmeticComplex(RComplex left, RComplex right) {
        if (leftNACheck.check(left)) {
            if (this.binary instanceof BinaryArithmetic.Pow && right.isZero()) {
                // CORNER: (0i + NA)^0 == 1
                return RDataFactory.createComplexRealOne();
            } else if (this.binary instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw RError.getUnimplementedComplex(this.getEncapsulatingSourceSection());
            }
            return RRuntime.createComplexNA();
        }
        if (rightNACheck.check(right)) {
            if (this.binary instanceof BinaryArithmetic.Pow && left.isZero()) {
                // CORNER: 0^(0i + NA) == NaN + NaNi
                return RDataFactory.createComplex(Double.NaN, Double.NaN);
            } else if (this.binary instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw RError.getUnimplementedComplex(this.getEncapsulatingSourceSection());
            }
            return RRuntime.createComplexNA();
        }
        return binary.op(left.getRealPart(), left.getImaginaryPart(), right.getRealPart(), right.getImaginaryPart());
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
        int value = binary.op(left, right);
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
        return binary.op(leftNACheck.convertIntToDouble(left), rightNACheck.convertIntToDouble(right));
    }

    private static int[] largerDim(RAbstractVector left, RAbstractVector right) {
        return left.getLength() > right.getLength() ? left.getDimensions() : right.getDimensions();
    }

    @NodeField(name = "NACheck", type = NACheck.class)
    @NodeChild("operand")
    public abstract static class ArithmeticToDoubleCast extends RNode {

        protected abstract NACheck getNACheck();

        @Specialization
        public RNull cast(RNull operand) {
            return operand;
        }

        @Specialization
        public double doIntToDouble(int operand) {
            return intToDouble(operand);
        }

        private double intToDouble(int operand) {
            if (getNACheck().check(operand)) {
                return RRuntime.DOUBLE_NA;
            }
            return operand;
        }

        @Specialization
        public double doDouble(double operand) {
            return operand;
        }

        @Specialization
        public RComplex doComplex(RComplex operand) {
            return operand;
        }

        @Specialization
        public RDoubleVector doIntVectorDouble(RIntVector operand) {
            return intVectorToDoubleVector(operand);
        }

        private RDoubleVector intVectorToDoubleVector(RIntVector operand) {
            double[] result = new double[operand.getLength()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = intToDouble(operand.getDataAt(i));
            }
            return RDataFactory.createDoubleVector(result, getNACheck().neverSeenNA(), operand.getDimensions());
        }

        @Specialization
        public RDoubleVector doDoubleVector(RDoubleVector operand) {
            return operand;
        }

        @Specialization
        public RComplexVector doComplexVector(RComplexVector operand) {
            return operand;
        }
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
        public int doInt(int operand) {
            getNACheck().enable(RRuntime.isNA(operand));
            return operand;
        }

        @Specialization
        public double doDouble(double operand) {
            getNACheck().enable(operand);
            return operand;
        }

        @Specialization
        public RComplex doComplex(RComplex operand) {
            getNACheck().enable(operand);
            return operand;
        }

        @Specialization
        public int doBoolean(byte operand) {
            getNACheck().enable(operand);
            return getNACheck().convertLogicalToInt(operand);
        }

        @Specialization
        public RMissing doBoolean(RMissing operand) {
            return operand;
        }

        @Specialization
        public Object doRaw(RRaw operand) {
            return doRawVector(RDataFactory.createRawVectorFromScalar(operand));
        }

        @Specialization
        public Object doString(String operand) {
            return doStringVector(RDataFactory.createStringVectorFromScalar(operand));
        }

        @Specialization
        public RIntSequence doIntVector(RIntSequence operand) {
            // NACheck may keep disabled.
            return operand;
        }

        @Specialization
        public RDoubleSequence doIntVector(RDoubleSequence operand) {
            // NACheck may keep disabled.
            return operand;
        }

        @Specialization
        public RIntVector doIntVector(RIntVector operand) {
            getNACheck().enable(!operand.isComplete());
            return operand;
        }

        @Specialization
        public RDoubleVector doDoubleVector(RDoubleVector operand) {
            getNACheck().enable(!operand.isComplete());
            return operand;
        }

        @Specialization
        public RComplexVector doComplexVector(RComplexVector operand) {
            getNACheck().enable(!operand.isComplete());
            return operand;
        }

        @Specialization
        public RIntVector doLogicalVector(RLogicalVector operand) {
            getNACheck().enable(!operand.isComplete());
            return logicalToInt(operand);
        }

        @Specialization
        public Object doStringVector(RStringVector operand) {
            throw RError.getNonNumericBinary(this.getSourceSection());
        }

        @Specialization
        public Object doRawVector(RRawVector operand) {
            throw RError.getNonNumericBinary(this.getSourceSection());
        }

        private RIntVector logicalToInt(RLogicalVector operand) {
            int[] result = new int[operand.getLength()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = getNACheck().convertLogicalToInt(operand.getDataAt(i));
            }
            return RDataFactory.createIntVector(result, getNACheck().neverSeenNA());
        }
    }
}
