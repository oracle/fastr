/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Add;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Multiply;
import com.oracle.truffle.r.runtime.ops.na.*;

@GenerateNodeFactory
public abstract class BinaryArithmeticNode extends RBuiltinNode {

    @Child private BinaryArithmetic arithmetic;
    @Child private UnaryArithmeticNode unaryNode;
    private final UnaryArithmeticFactory unaryFactory;

    private final NACheck leftNACheck;
    private final NACheck rightNACheck;
    private final NACheck resultNACheck;

    private final ConditionProfile noDimensionsProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile emptyVector = ConditionProfile.createBinaryProfile();
    private final BranchProfile hasAttributesProfile = BranchProfile.create();
    private final BranchProfile warningProfile = BranchProfile.create();
    private final ConditionProfile leftLongerProfile = ConditionProfile.createBinaryProfile();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public BinaryArithmeticNode(BinaryArithmeticFactory factory, UnaryArithmeticFactory unaryFactory) {
        this.arithmetic = factory.create();
        this.unaryFactory = unaryFactory;
        leftNACheck = new NACheck();
        rightNACheck = new NACheck();
        resultNACheck = new NACheck();
    }

    public BinaryArithmeticNode(BinaryArithmeticNode op) {
        this.arithmetic = op.arithmetic;
        this.unaryFactory = op.unaryFactory;
        this.leftNACheck = op.leftNACheck;
        this.rightNACheck = op.rightNACheck;
        this.resultNACheck = op.resultNACheck;
    }

    private UnaryArithmeticNode specializeToUnaryOp() {
        if (unaryNode == null) {
            if (unaryFactory == null) {
                // No profile needed, as all conditions are (Truffle/Graal) compile time constant
                throw RError.error(getSourceSection(), RError.Message.ARGUMENT_EMPTY, 2);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                unaryNode = insert(UnaryArithmeticNodeFactory.create(unaryFactory, RError.Message.INVALID_ARG_TYPE_UNARY, null));
            }
        }
        return unaryNode;
    }

    public static BinaryArithmeticNode create(BinaryArithmeticFactory arithmetic) {
        return BinaryArithmeticNodeFactory.create(arithmetic, null, new RNode[2], null, null);
    }

    // special cases for sequences

    protected boolean isAdd() {
        return arithmetic instanceof Add;
    }

    protected boolean isMul() {
        return arithmetic instanceof Multiply;
    }

    @Specialization(guards = "isAdd")
    protected RIntSequence doIntAdd(int left, RIntSequence right) {
        // TODO: handle cases that exit the int range
        return RDataFactory.createIntSequence(right.getStart() + left, right.getStride(), right.getLength());
    }

    @Specialization(guards = "isMul")
    protected RIntSequence doIntMul(RIntSequence left, int right) {
        // TODO: handle cases that exit the int range
        return RDataFactory.createIntSequence(left.getStart() * right, left.getStride() * right, left.getLength());
    }

    @Specialization(guards = "isAdd")
    protected RDoubleSequence doDoubleAdd(int left, RDoubleSequence right) {
        return RDataFactory.createDoubleSequence(right.getStart() + left, right.getStride(), right.getLength());
    }

    @Specialization(guards = "isMul")
    protected RDoubleSequence doDoubleMul(RDoubleSequence left, int right) {
        return RDataFactory.createDoubleSequence(left.getStart() * right, left.getStride() * right, left.getLength());
    }

    @Specialization(guards = "isAdd")
    protected RDoubleSequence doDoubleAdd(double left, RDoubleSequence right) {
        return RDataFactory.createDoubleSequence(right.getStart() + left, right.getStride(), right.getLength());
    }

    @Specialization(guards = "isMul")
    protected RDoubleSequence doDoubleMul(RDoubleSequence left, double right) {
        return RDataFactory.createDoubleSequence(left.getStart() * right, left.getStride() * right, left.getLength());
    }

    @Specialization(guards = "isAdd")
    protected RDoubleSequence doDoubleAdd(double left, RIntSequence right) {
        return RDataFactory.createDoubleSequence(right.getStart() + left, right.getStride(), right.getLength());
    }

    @Specialization(guards = "isMul")
    protected RDoubleSequence doDoubleMul(RIntSequence left, double right) {
        return RDataFactory.createDoubleSequence(left.getStart() * right, left.getStride() * right, left.getLength());
    }

    //

    @Specialization
    protected Object doUnary(VirtualFrame frame, Object left, @SuppressWarnings("unused") RMissing right) {
        return specializeToUnaryOp().execute(frame, left);
    }

    @Specialization
    protected RLogicalVector doFactorOp(RFactor left, @SuppressWarnings("unused") RNull right) {
        if (left.isOrdered()) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_ORDERED_FACTORS, arithmetic.opName());
        } else {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_FACTORS, arithmetic.opName());
        }
        return RDataFactory.createNAVector(left.getLength() == 0 ? 1 : left.getLength());
    }

    @Specialization
    protected RLogicalVector doFactorOp(@SuppressWarnings("unused") RNull left, RFactor right) {
        if (right.isOrdered()) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_ORDERED_FACTORS, arithmetic.opName());
        } else {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_FACTORS, arithmetic.opName());
        }
        return RDataFactory.createNAVector(right.getLength() == 0 ? 1 : right.getLength());
    }

    @Specialization
    protected RDoubleVector doLeftNull(RNull left, RAbstractIntVector right) {
        return doRightNull(right, left);
    }

    @Specialization
    protected RDoubleVector doLeftNull(RNull left, RAbstractDoubleVector right) {
        return doRightNull(right, left);
    }

    @Specialization
    protected RDoubleVector doLeftNull(RNull left, RAbstractLogicalVector right) {
        return doRightNull(right, left);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "nonNumeric")
    protected RComplexVector doLeftNull(RNull left, RAbstractVector right) {
        throw RError.error(getSourceSection(), RError.Message.NON_NUMERIC_BINARY);
    }

    @Specialization
    protected RComplexVector doLeftNull(RNull left, RAbstractComplexVector right) {
        return doRightNull(right, left);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector doRightNull(RAbstractIntVector left, RNull right) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector doRightNull(RAbstractDoubleVector left, RNull right) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector doRightNull(RAbstractLogicalVector left, RNull right) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RComplexVector doRightNull(RAbstractComplexVector left, RNull right) {
        return RDataFactory.createEmptyComplexVector();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "nonNumeric")
    protected RComplexVector doRightNull(RAbstractVector left, RNull right) {
        throw RError.error(getSourceSection(), RError.Message.NON_NUMERIC_BINARY);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector doRightNull(RNull left, RNull right) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = "!isFactorRight")
    protected Object doLeftString(RAbstractStringVector left, RAbstractContainer right) {
        return doRightString(right, left);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isFactorLeft")
    protected Object doRightString(RAbstractContainer left, RAbstractStringVector right) {
        throw RError.error(getSourceSection(), RError.Message.NON_NUMERIC_BINARY);
    }

    @Specialization(guards = "!isFactorRight")
    protected Object doLeftRaw(RAbstractRawVector left, RAbstractContainer right) {
        return doRightRaw(right, left);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isFactorLeft")
    protected Object doRightRaw(RAbstractContainer left, RAbstractRawVector right) {
        throw RError.error(getSourceSection(), RError.Message.NON_NUMERIC_BINARY);
    }

    protected boolean supportsIntResult() {
        return arithmetic.isSupportsIntResult();
    }

    // int

    @Specialization(guards = {"supportsIntResult"})
    public int doInt(int left, int right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticEnableNACheck(left, right);
    }

    @Specialization
    protected double doInt(int left, double right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticDoubleEnableNACheck(leftNACheck.check(left) ? RRuntime.DOUBLE_NA : RRuntime.int2doubleNoCheck(left), right);
    }

    @Specialization
    public double doInt(double left, int right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticDoubleEnableNACheck(left, rightNACheck.check(right) ? RRuntime.DOUBLE_NA : RRuntime.int2doubleNoCheck(right));
    }

    @Specialization(guards = {"supportsIntResult"})
    protected int doInt(int left, byte right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticEnableNACheck(left, rightNACheck.check(right) ? RRuntime.INT_NA : RRuntime.logical2intNoCheck(right));
    }

    @Specialization(guards = {"supportsIntResult"})
    protected int doInt(byte left, int right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticEnableNACheck(leftNACheck.check(left) ? RRuntime.INT_NA : RRuntime.logical2intNoCheck(left), right);
    }

    @Specialization
    protected RComplex doInt(int left, RComplex right) {
        return performArithmeticComplexEnableNACheck(RRuntime.int2complex(left), right);
    }

    @Specialization
    protected RComplex doInt(RComplex left, int right) {
        return performArithmeticComplexEnableNACheck(left, RRuntime.int2complex(right));
    }

    @Specialization(guards = {"!supportsIntResult"})
    protected double doIntDouble(int left, int right) {
        return performArithmeticIntIntDoubleEnableNACheck(left, right);
    }

    @Specialization(guards = {"!supportsIntResult"})
    protected double doIntDouble(int left, byte right) {
        return performArithmeticIntIntDoubleEnableNACheck(left, RRuntime.logical2int(right));
    }

    @Specialization(guards = {"!supportsIntResult"})
    protected double doIntDouble(byte left, int right) {
        return performArithmeticIntIntDoubleEnableNACheck(RRuntime.logical2int(left), right);
    }

    // double

    @Specialization
    public double doDouble(double left, double right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticDoubleEnableNACheck(left, right);
    }

    @Specialization
    protected double doDouble(double left, byte right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticDoubleEnableNACheck(left, rightNACheck.check(right) ? RRuntime.DOUBLE_NA : RRuntime.logical2doubleNoCheck(right));
    }

    @Specialization
    protected double doDouble(byte left, double right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticDoubleEnableNACheck(leftNACheck.check(left) ? RRuntime.DOUBLE_NA : RRuntime.logical2doubleNoCheck(left), right);
    }

    @Specialization
    protected RComplex doDouble(double left, RComplex right) {
        return performArithmeticComplexEnableNACheck(RRuntime.double2complex(left), right);
    }

    @Specialization
    protected RComplex doDouble(RComplex left, double right) {
        return performArithmeticComplexEnableNACheck(left, RRuntime.double2complex(right));
    }

    // logical

    @Specialization(guards = {"supportsIntResult"})
    protected int doLogical(byte left, byte right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticEnableNACheck(leftNACheck.check(left) ? RRuntime.INT_NA : RRuntime.logical2intNoCheck(left),
                        rightNACheck.check(right) ? RRuntime.INT_NA : RRuntime.logical2intNoCheck(right));
    }

    @Specialization
    protected RComplex doLogical(byte left, RComplex right) {
        return performArithmeticComplexEnableNACheck(RRuntime.logical2complex(left), right);
    }

    @Specialization
    protected RComplex doLogical(RComplex left, byte right) {
        return performArithmeticComplexEnableNACheck(left, RRuntime.logical2complex(right));
    }

    @Specialization(guards = {"!supportsIntResult"})
    protected double doLogicalDouble(byte left, byte right) {
        return performArithmeticIntIntDoubleEnableNACheck(RRuntime.logical2int(left), RRuntime.logical2int(right));
    }

    // complex

    @Specialization
    public RComplex doComplex(RComplex left, RComplex right) {
        return performArithmeticComplexEnableNACheck(left, right);
    }

    protected boolean differentDimensions(RAbstractVector left, RAbstractVector right) {
        if (noDimensionsProfile.profile(!left.hasDimensions() || !right.hasDimensions())) {
            return false;
        }
        int[] leftDimensions = left.getDimensions();
        int[] rightDimensions = right.getDimensions();
        assert (leftDimensions != null && rightDimensions != null);
        if (leftDimensions.length != rightDimensions.length) {
            return true;
        }
        for (int i = 0; i < leftDimensions.length; i++) {
            if (leftDimensions[i] != rightDimensions[i]) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "differentDimensions")
    protected RLogicalVector doIntVectorDifferentLength(RAbstractVector left, RAbstractVector right) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.NON_CONFORMABLE_ARRAYS);
    }

    // int vector and vectors

    public boolean isTemporary(RIntVector vector) {
        return vector.isTemporary();
    }

    @Specialization(guards = {"supportsIntResult", "isTemporary(arguments[0])"})
    protected RIntVector doIntVectorScalar(RIntVector left, int right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performOpVectorScalar(left, RIntVector::getDataWithoutCopying, RDataFactory::createEmptyIntVector, (array, i) -> array[i] = performArithmetic(array[i], right));
    }

    @Specialization(guards = {"supportsIntResult", "isTemporary(arguments[1])"})
    protected RIntVector doIntVectorScalar(int left, RIntVector right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performOpVectorScalar(right, RIntVector::getDataWithoutCopying, RDataFactory::createEmptyIntVector, (array, i) -> array[i] = performArithmetic(left, array[i]));
    }

    @Specialization(guards = "!supportsIntResult")
    protected RDoubleVector doIntVectorScalar(RIntVector left, double right) {
        return performOpDifferentLength(left, double[]::new, RDataFactory::createEmptyDoubleVector, RDataFactory::createDoubleVector,
                        (array, i) -> array[i] = performArithmeticDouble(leftNACheck.convertIntToDouble(left.getDataAt(i)), right));
    }

    @Specialization(guards = "!supportsIntResult")
    protected RDoubleVector doIntVectorScalar(double left, RIntVector right) {
        return performOpDifferentLength(right, double[]::new, RDataFactory::createEmptyDoubleVector, RDataFactory::createDoubleVector,
                        (array, i) -> array[i] = performArithmeticDouble(left, rightNACheck.convertIntToDouble(right.getDataAt(i))));
    }

    @Specialization(guards = {"!areSameLength", "supportsIntResult", "!differentDimensions"})
    protected RIntVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpDifferentLength(left, right);
    }

    @Specialization(guards = {"areSameLength", "supportsIntResult", "!differentDimensions"})
    protected RIntVector doIntVectorSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpSameLength(left, right);
    }

    @Specialization(guards = {"!areSameLength", "!differentDimensions"})
    protected RDoubleVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength", "!differentDimensions"})
    protected RDoubleVector doIntVectorSameLength(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"!areSameLength", "!differentDimensions"})
    protected RDoubleVector doIntVectorDifferentLength(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorOpDifferentLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength", "!differentDimensions"})
    protected RDoubleVector doIntVectorIntVectorSameLength(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorOpSameLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength", "supportsIntResult", "!differentDimensions"})
    protected RIntVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDifferentLength(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength", "supportsIntResult"})
    protected RIntVector doIntVectorSameLength(RAbstractIntVector left, RAbstractLogicalVector right) {
        return performIntVectorOpSameLength(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength", "supportsIntResult"})
    protected RIntVector doIntVectorDifferentLength(RAbstractLogicalVector left, RAbstractIntVector right) {
        return performIntVectorOpDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength", "supportsIntResult"})
    protected RIntVector doIntVectorSameLength(RAbstractLogicalVector left, RAbstractIntVector right) {
        return performIntVectorOpSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    @Specialization(guards = "!areSameLength")
    protected RComplexVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = "areSameLength")
    protected RComplexVector doIntVectorSameLength(RAbstractIntVector left, RAbstractComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createIntToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = "!areSameLength")
    protected RComplexVector doIntVectorDifferentLength(RAbstractComplexVector left, RAbstractIntVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = "areSameLength")
    protected RComplexVector doIntVectorSameLength(RAbstractComplexVector left, RAbstractIntVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createIntToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength", "!supportsIntResult"})
    protected RDoubleVector doIntVectorDoubleDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpDoubleDifferentLength(left, right);
    }

    @Specialization(guards = {"areSameLength", "!supportsIntResult"})
    protected RDoubleVector doIntVectorDoubleSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpDoubleSameLength(left, right);
    }

    @Specialization(guards = {"!areSameLength", "!supportsIntResult"})
    protected RDoubleVector doIntVectorDoubleDifferentLength(RAbstractIntVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDoubleDifferentLength(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength", "!supportsIntResult"})
    protected RDoubleVector doIntVectorDoubleSameLength(RAbstractIntVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDoubleSameLength(left, RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength", "!supportsIntResult"})
    protected RDoubleVector doIntVectorDoubleDifferentLength(RAbstractLogicalVector left, RAbstractIntVector right) {
        return performIntVectorOpDoubleDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    @Specialization(guards = {"areSameLength", "!supportsIntResult"})
    protected RDoubleVector doIntVectorDoubleSameLength(RAbstractLogicalVector left, RAbstractIntVector right) {
        return performIntVectorOpDoubleSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), right);
    }

    // double vector and vectors

    public boolean isTemporary(RDoubleVector vector) {
        return vector.isTemporary();
    }

    @Specialization(guards = "isTemporary(arguments[0])")
    protected RDoubleVector doDoubleVectorScalarTempLeft(RDoubleVector left, double right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performOpVectorScalar(left, RDoubleVector::getDataWithoutCopying, RDataFactory::createEmptyDoubleVector, (array, i) -> array[i] = performArithmeticDouble(array[i], right));
    }

    @Specialization(guards = "isTemporary(arguments[1])")
    protected RDoubleVector doDoubleVectorScalarTempRight(double left, RDoubleVector right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performOpVectorScalar(right, RDoubleVector::getDataWithoutCopying, RDataFactory::createEmptyDoubleVector, (array, i) -> array[i] = performArithmeticDouble(left, array[i]));
    }

    @Specialization
    protected RDoubleVector doDoubleVector(RAbstractDoubleVector left, double right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performOpDifferentLength(left, double[]::new, RDataFactory::createEmptyDoubleVector, RDataFactory::createDoubleVector,
                        (array, i) -> array[i] = performArithmeticDouble(left.getDataAt(i), right));
    }

    @Specialization
    protected RDoubleVector doDoubleVector(double left, RAbstractDoubleVector right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performOpDifferentLength(right, double[]::new, RDataFactory::createEmptyDoubleVector, RDataFactory::createDoubleVector,
                        (array, i) -> array[i] = performArithmeticDouble(left, right.getDataAt(i)));
    }

    @Specialization(guards = "!areSameLength")
    protected RDoubleVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(left, right);
    }

    @Specialization(guards = {"areSameLength", "isTemporary(arguments[0])"})
    protected RDoubleVector doDoubleVectorTempLeft(RDoubleVector left, RDoubleVector right) {
        double[] rightArray = right.getDataWithoutCopying();
        return performOpVectorScalar(left, RDoubleVector::getDataWithoutCopying, RDataFactory::createEmptyDoubleVector, (array, i) -> array[i] = performArithmeticDouble(array[i], rightArray[i]));
    }

    @Specialization(guards = {"areSameLength", "isTemporary(arguments[1])"})
    protected RDoubleVector doDoubleVectorTempRight(RDoubleVector left, RDoubleVector right) {
        double[] leftArray = left.getDataWithoutCopying();
        return performOpVectorScalar(right, RDoubleVector::getDataWithoutCopying, RDataFactory::createEmptyDoubleVector, (array, i) -> array[i] = performArithmeticDouble(leftArray[i], array[i]));
    }

    @Specialization(guards = "areSameLength")
    protected RDoubleVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(left, right);
    }

    @Specialization(guards = "!areSameLength")
    protected RDoubleVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractLogicalVector right) {
        return performDoubleVectorOpDifferentLength(left, RClosures.createLogicalToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = "areSameLength")
    protected RDoubleVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractLogicalVector right) {
        return performDoubleVectorOpSameLength(left, RClosures.createLogicalToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = "!areSameLength")
    protected RDoubleVector doDoubleVectorDifferentLength(RAbstractLogicalVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(RClosures.createLogicalToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = "areSameLength")
    protected RDoubleVector doDoubleVectorSameLength(RAbstractLogicalVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(RClosures.createLogicalToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = "!areSameLength")
    protected RComplexVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = "areSameLength")
    protected RComplexVector doDoubleVectorSameLength(RAbstractDoubleVector left, RAbstractComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createDoubleToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = "!areSameLength")
    protected RComplexVector doDoubleVectorDifferentLength(RAbstractComplexVector left, RAbstractDoubleVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = "areSameLength")
    protected RComplexVector doDoubleVectorSameLength(RAbstractComplexVector left, RAbstractDoubleVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createDoubleToComplexVector(right, rightNACheck));
    }

    // logical vector and vectors

    @Specialization(guards = {"!areSameLength", "supportsIntResult"})
    protected RIntVector doLogicalVectorDifferentLength(RAbstractLogicalVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength", "supportsIntResult"})
    protected RIntVector doLogicalVectorSameLength(RAbstractLogicalVector left, RAbstractLogicalVector right) {
        return performIntVectorOpSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = "!areSameLength")
    protected RComplexVector doLogicalVectorDifferentLength(RAbstractLogicalVector left, RAbstractComplexVector right) {
        return performComplexVectorOpDifferentLength(RClosures.createLogicalToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = "areSameLength")
    protected RComplexVector doLogicalVectorSameLength(RAbstractLogicalVector left, RAbstractComplexVector right) {
        return performComplexVectorOpSameLength(RClosures.createLogicalToComplexVector(left, leftNACheck), right);
    }

    @Specialization(guards = "!areSameLength")
    protected RComplexVector doLogicalVectorDifferentLength(RAbstractComplexVector left, RAbstractLogicalVector right) {
        return performComplexVectorOpDifferentLength(left, RClosures.createLogicalToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = "areSameLength")
    protected RComplexVector doLogicalVectorSameLength(RAbstractComplexVector left, RAbstractLogicalVector right) {
        return performComplexVectorOpSameLength(left, RClosures.createLogicalToComplexVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength", "!supportsIntResult"})
    protected RDoubleVector doLogicalVectorDoubleDifferentLength(RAbstractLogicalVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDoubleDifferentLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    @Specialization(guards = {"areSameLength", "!supportsIntResult"})
    protected RDoubleVector doLogicalVectorDoubleSameLength(RAbstractLogicalVector left, RAbstractLogicalVector right) {
        return performIntVectorOpDoubleSameLength(RClosures.createLogicalToIntVector(left, leftNACheck), RClosures.createLogicalToIntVector(right, rightNACheck));
    }

    // complex vector and vectors

    @Specialization(guards = "!areSameLength")
    protected RComplexVector doComplexVectorDifferentLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        return performComplexVectorOpDifferentLength(left, right);
    }

    @Specialization(guards = "areSameLength")
    protected RComplexVector doComplexVectorSameLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        return performComplexVectorOpSameLength(left, right);
    }

    // factors

    @Specialization
    protected RLogicalVector doFactorOp(RFactor left, RAbstractContainer right) {
        if (left.isOrdered()) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_ORDERED_FACTORS, arithmetic.opName());
        } else {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_FACTORS, arithmetic.opName());
        }
        return RDataFactory.createNAVector(Math.max(left.getLength(), right.getLength()));
    }

    @Specialization
    protected RLogicalVector doFactorOp(RAbstractContainer left, RFactor right) {
        if (right.isOrdered()) {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_ORDERED_FACTORS, arithmetic.opName());
        } else {
            RError.warning(getEncapsulatingSourceSection(), RError.Message.NOT_MEANINGFUL_FOR_FACTORS, arithmetic.opName());
        }
        return RDataFactory.createNAVector(Math.max(left.getLength(), right.getLength()));
    }

    protected boolean nonNumeric(@SuppressWarnings("unused") RNull left, RAbstractContainer right) {
        return right.getElementClass() == RString.class || right.getElementClass() == RRaw.class;
    }

    protected boolean nonNumeric(RAbstractContainer left) {
        return left.getElementClass() == RString.class || left.getElementClass() == RRaw.class;
    }

    protected boolean isFactorLeft(RAbstractContainer left) {
        return left.getElementClass() == RFactor.class;
    }

    protected boolean isFactorRight(@SuppressWarnings("unused") RAbstractStringVector left, RAbstractContainer right) {
        return right.getElementClass() == RFactor.class;
    }

    protected boolean isFactorRight(@SuppressWarnings("unused") RAbstractRawVector left, RAbstractContainer right) {
        return right.getElementClass() == RFactor.class;
    }

    // implementation

    private void copyAttributes(RVector ret, RAbstractVector left, RAbstractVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int length = Math.max(leftLength, rightLength);
        RAbstractVector attributeSource = leftLength == length ? left : right;

        if (attributeSource.getAttributes() != null || left.hasDimensions() || right.hasDimensions() || attributeSource.getNames(attrProfiles) != null || attributeSource.getDimNames() != null) {
            hasAttributesProfile.enter();
            copyAttributesInternal(ret, attributeSource, left, right);
        }
    }

    private void copyAttributes(RVector ret, RAbstractVector source) {
        if (source.getAttributes() != null || source.hasDimensions() || source.getNames(attrProfiles) != null || source.getDimNames() != null) {
            hasAttributesProfile.enter();
            copyAttributesInternal(ret, source);
        }
    }

    @TruffleBoundary
    private void copyAttributesInternal(RVector ret, RAbstractVector attributeSource, RAbstractVector left, RAbstractVector right) {
        ret.copyRegAttributesFrom(attributeSource);
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), getSourceSection());
        ret.copyNamesFrom(attrProfiles, attributeSource);
    }

    @TruffleBoundary
    private void copyAttributesInternal(RVector ret, RAbstractVector source) {
        ret.copyRegAttributesFrom(source);
        ret.setDimensions(source.getDimensions(), getSourceSection());
        ret.copyNamesFrom(attrProfiles, source);
    }

    private void copyAttributesSameLength(RVector ret, RAbstractVector left, RAbstractVector right) {
        if (left.getAttributes() != null || right.getAttributes() != null || left.hasDimensions() || right.hasDimensions() || left.getNames(attrProfiles) != null ||
                        right.getNames(attrProfiles) != null || left.getDimNames() != null || right.getDimNames() != null) {
            hasAttributesProfile.enter();
            copyAttributesSameLengthInternal(ret, left, right);
        }
    }

    @TruffleBoundary
    private void copyAttributesSameLengthInternal(RVector ret, RAbstractVector left, RAbstractVector right) {
        ret.copyRegAttributesFrom(right);
        ret.copyRegAttributesFrom(left);
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), getEncapsulatingSourceSection());
        if (!ret.copyNamesFrom(attrProfiles, left)) {
            ret.copyNamesFrom(attrProfiles, right);
        }
    }

    interface SameOpFunction<ArrayT> {
        void apply(ArrayT array, int i);
    }

    interface DifferentOpFunction<ArrayT> {
        void apply(ArrayT array, int i, int k, int j);
    }

    private <ResultT extends RVector, ParamT extends RAbstractVector, ArrayT> ResultT performOpDifferentLength(ParamT left, ParamT right, IntFunction<ArrayT> arrayConstructor,
                    Supplier<ResultT> emptyConstructor, BiFunction<ArrayT, Boolean, ResultT> resultFunction, DifferentOpFunction<ArrayT> op) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        if (emptyVector.profile(leftLength == 0 || rightLength == 0)) {
            return emptyConstructor.get();
        }
        int length = Math.max(leftLength, rightLength);
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        resultNACheck.enable(arithmetic.introducesNA());
        ArrayT result = arrayConstructor.apply(length);
        int j = 0;
        int k = 0;
        boolean notMultiple = false;
        if (leftLongerProfile.profile(leftLength > rightLength)) {
            if (leftLength % rightLength != 0) {
                warningProfile.enter();
                notMultiple = true;
            } else {
                while (j < leftLength) {
                    k = 0;
                    while (k < rightLength) {
                        op.apply(result, j, j, k);
                        j++;
                        k++;
                    }
                }
            }
        } else {
            if (rightLength % leftLength != 0) {
                warningProfile.enter();
                notMultiple = true;
            } else {
                while (k < rightLength) {
                    j = 0;
                    while (j < leftLength) {
                        op.apply(result, k, j, k);
                        j++;
                        k++;
                    }
                }
            }
        }
        if (notMultiple) {
            for (int i = 0; i < length; ++i) {
                op.apply(result, i, j, k);
                j = Utils.incMod(j, leftLength);
                k = Utils.incMod(k, rightLength);
            }
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        ResultT ret = resultFunction.apply(result, isComplete());
        copyAttributes(ret, left, right);
        return ret;
    }

    private <ResultT extends RVector, ParamT extends RAbstractVector, ArrayT> ResultT performOpDifferentLength(ParamT source, IntFunction<ArrayT> arrayConstructor, Supplier<ResultT> emptyConstructor,
                    BiFunction<ArrayT, Boolean, ResultT> resultFunction, SameOpFunction<ArrayT> op) {
        int length = source.getLength();
        if (emptyVector.profile(length == 0)) {
            return emptyConstructor.get();
        }
        resultNACheck.enable(arithmetic.introducesNA());
        ArrayT result = arrayConstructor.apply(length);
        for (int i = 0; i < length; ++i) {
            op.apply(result, i);
        }
        ResultT ret = resultFunction.apply(result, isComplete());
        if (ret != source) {
            copyAttributes(ret, source);
        }
        return ret;
    }

    private <ParamT extends RVector, ArrayT> ParamT performOpVectorScalar(ParamT source, Function<ParamT, ArrayT> arrayConstructor, Supplier<ParamT> emptyConstructor, SameOpFunction<ArrayT> op) {
        int length = source.getLength();
        if (emptyVector.profile(length == 0)) {
            return emptyConstructor.get();
        }
        resultNACheck.enable(arithmetic.introducesNA());
        ArrayT result = arrayConstructor.apply(source);
        for (int i = 0; i < length; ++i) {
            op.apply(result, i);
        }
        source.setComplete(isComplete());
        return source;
    }

    private RComplexVector performComplexVectorOpDifferentLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        return performOpDifferentLength(left, right, len -> new double[len << 1], RDataFactory::createEmptyComplexVector, RDataFactory::createComplexVector, (array, i, j, k) -> {
            RComplex result = performArithmeticComplex(left.getDataAt(j), right.getDataAt(k));
            array[i << 1] = result.getRealPart();
            array[(i << 1) + 1] = result.getImaginaryPart();
        });
    }

    private RDoubleVector performDoubleVectorOpDifferentLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performOpDifferentLength(left, right, double[]::new, RDataFactory::createEmptyDoubleVector, RDataFactory::createDoubleVector,
                        (array, i, j, k) -> array[i] = performArithmeticDouble(left.getDataAt(j), right.getDataAt(k)));
    }

    private RIntVector performIntVectorOpDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performOpDifferentLength(left, right, int[]::new, RDataFactory::createEmptyIntVector, RDataFactory::createIntVector,
                        (array, i, j, k) -> array[i] = performArithmetic(left.getDataAt(j), right.getDataAt(k)));
    }

    private RDoubleVector performIntVectorOpDoubleDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performOpDifferentLength(left, right, double[]::new, RDataFactory::createEmptyDoubleVector, RDataFactory::createDoubleVector,
                        (array, i, j, k) -> array[i] = performArithmeticIntIntDouble(left.getDataAt(j), right.getDataAt(k)));
    }

    private <ResultT extends RVector, ParamT extends RAbstractVector, ArrayT> ResultT performOpSameLength(ParamT left, ParamT right, IntFunction<ArrayT> arrayConstructor,
                    Supplier<ResultT> emptyConstructor, BiFunction<ArrayT, Boolean, ResultT> resultFunction, SameOpFunction<ArrayT> op) {
        assert areSameLength(left, right);
        int length = left.getLength();
        if (emptyVector.profile(length == 0)) {
            return emptyConstructor.get();
        }
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        resultNACheck.enable(arithmetic.introducesNA());
        ArrayT result = arrayConstructor.apply(length);
        for (int i = 0; i < length; ++i) {
            op.apply(result, i);
        }
        ResultT ret = resultFunction.apply(result, isComplete());
        copyAttributesSameLength(ret, left, right);
        return ret;
    }

    private RComplexVector performComplexVectorOpSameLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        return performOpSameLength(left, right, len -> new double[len << 1], RDataFactory::createEmptyComplexVector, RDataFactory::createComplexVector, (array, i) -> {
            RComplex result = performArithmeticComplex(left.getDataAt(i), right.getDataAt(i));
            array[i << 1] = result.getRealPart();
            array[(i << 1) + 1] = result.getImaginaryPart();
        });
    }

    private RDoubleVector performDoubleVectorOpSameLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performOpSameLength(left, right, double[]::new, RDataFactory::createEmptyDoubleVector, RDataFactory::createDoubleVector,
                        (array, i) -> array[i] = performArithmeticDouble(left.getDataAt(i), right.getDataAt(i)));
    }

    private RIntVector performIntVectorOpSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performOpSameLength(left, right, int[]::new, RDataFactory::createEmptyIntVector, RDataFactory::createIntVector,
                        (array, i) -> array[i] = performArithmetic(left.getDataAt(i), right.getDataAt(i)));
    }

    private RDoubleVector performIntVectorOpDoubleSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performOpSameLength(left, right, double[]::new, RDataFactory::createEmptyDoubleVector, RDataFactory::createDoubleVector,
                        (array, i) -> array[i] = performArithmeticIntIntDouble(left.getDataAt(i), right.getDataAt(i)));
    }

    private double performArithmeticDoubleEnableNACheck(double left, double right) {
        resultNACheck.enable(arithmetic.introducesNA());
        return performArithmeticDouble(left, right);
    }

    static int cnt = 0;

    static long nextTime = 0;

    static final HashMap<String, AtomicLong> counters = new HashMap<>();

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
        double value = arithmetic.op(left, right);
        resultNACheck.check(value);
        return value;
    }

    private RComplex performArithmeticComplexEnableNACheck(RComplex left, RComplex right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        resultNACheck.enable(arithmetic.introducesNA());
        return performArithmeticComplex(left, right);
    }

    private RComplex performArithmeticComplex(RComplex left, RComplex right) {
        if (leftNACheck.check(left)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && right.isZero()) {
                // CORNER: (0i + NA)^0 == 1
                return RDataFactory.createComplexRealOne();
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNIMPLEMENTED_COMPLEX);
            }
            return RRuntime.createComplexNA();
        }
        if (rightNACheck.check(right)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && left.isZero()) {
                // CORNER: 0^(0i + NA) == NaN + NaNi
                return RDataFactory.createComplex(Double.NaN, Double.NaN);
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNIMPLEMENTED_COMPLEX);
            }
            return RRuntime.createComplexNA();
        }
        RComplex value = arithmetic.op(left.getRealPart(), left.getImaginaryPart(), right.getRealPart(), right.getImaginaryPart());
        resultNACheck.check(value);
        return value;
    }

    private int performArithmeticEnableNACheck(int left, int right) {
        resultNACheck.enable(arithmetic.introducesNA());
        return performArithmetic(left, right);
    }

    private int performArithmetic(int left, int right) {
// if (cnt++ == 13337) {
// // VirtualFrame frame = Utils.getActualCurrentFrame();
// // String x;
// // if (frame == null || RArguments.getFunction(frame) == null) {
// // x = "none";
// // } else {
// // x = RArguments.getFunction(frame).getRootNode().toString();
// // }
// RuntimeException e = new RuntimeException();
// int i = 2;
// while (e.getStackTrace()[i + 1].getClassName().endsWith("BinaryArithmeticNode")) {
// i++;
// }
// String x = e.getStackTrace()[i].toString();
// AtomicLong counter = counters.get(x);
// if (counter == null) {
// counters.put(x, counter = new AtomicLong());
// }
// counter.incrementAndGet();
// cnt = 0;
// if (nextTime < System.currentTimeMillis()) {
// System.out.println("counters:");
// for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
// System.out.println(entry);
// }
// nextTime = System.currentTimeMillis() + 3000;
// }
// }
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

    private double performArithmeticIntIntDoubleEnableNACheck(int left, int right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        resultNACheck.enable(arithmetic.introducesNA());
        return performArithmeticIntIntDouble(left, right);
    }

    private double performArithmeticIntIntDouble(int left, int right) {
        if (leftNACheck.check(left)) {
            return RRuntime.DOUBLE_NA;
        }
        if (rightNACheck.check(right)) {
            return RRuntime.DOUBLE_NA;
        }
        double value = arithmetic.op(leftNACheck.convertIntToDouble(left), rightNACheck.convertIntToDouble(right));
        resultNACheck.check(value);
        return value;
    }

    private boolean isComplete() {
        return leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA() && resultNACheck.neverSeenNA();
    }

}
