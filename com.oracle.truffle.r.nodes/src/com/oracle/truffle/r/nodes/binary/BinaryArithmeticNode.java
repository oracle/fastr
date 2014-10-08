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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
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
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class BinaryArithmeticNode extends RBuiltinNode {

    @Child private BinaryArithmetic arithmetic;
    @Child private UnaryArithmeticNode unaryNode;
    private final UnaryArithmeticFactory unaryFactory;

    private final NACheck leftNACheck;
    private final NACheck rightNACheck;
    private final NACheck resultNACheck;

    private final ConditionProfile emptyVector = ConditionProfile.createBinaryProfile();

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
                // No profile needed, as this is only needed on 1. execution and thus cannot get
                // compiled
                throw RError.error(getSourceSection(), RError.Message.ARGUMENT_EMPTY, 2);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                unaryNode = insert(UnaryArithmeticNodeFactory.create(unaryFactory, null));
            }
        }
        return unaryNode;
    }

    public static BinaryArithmeticNode create(BinaryArithmeticFactory arithmetic) {
        return BinaryArithmeticNodeFactory.create(arithmetic, null, new RNode[2], null, null);
    }

    @Specialization
    protected Object doUnary(VirtualFrame frame, Object left, @SuppressWarnings("unused") RMissing right) {
        return specializeToUnaryOp().execute(frame, left);
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
    @Specialization
    protected RDoubleVector doRightNull(RNull left, RNull right) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization
    protected Object doLeftString(RAbstractStringVector left, Object right) {
        return doRightString(right, left);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doRightString(Object left, RAbstractStringVector right) {
        throw RError.error(this.getSourceSection(), RError.Message.NON_NUMERIC_BINARY);
    }

    @Specialization
    protected Object doLeftRaw(RAbstractRawVector left, Object right) {
        return doRightRaw(right, left);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doRightRaw(Object left, RAbstractRawVector right) {
        throw RError.error(this.getSourceSection(), RError.Message.NON_NUMERIC_BINARY);
    }

    protected boolean supportsIntResult() {
        return arithmetic.isSupportsIntResult();
    }

    // int

    @Specialization(guards = {"supportsIntResult"})
    public int doInt(int left, int right) {
        return performArithmeticEnableNACheck(left, right);
    }

    @Specialization
    protected double doInt(int left, double right) {
        return performArithmeticDoubleEnableNACheck(RRuntime.int2double(left), right);
    }

    @Specialization
    public double doInt(double left, int right) {
        return performArithmeticDoubleEnableNACheck(left, RRuntime.int2double(right));
    }

    @Specialization(guards = {"supportsIntResult"})
    protected int doInt(int left, byte right) {
        return performArithmeticEnableNACheck(left, RRuntime.logical2int(right));
    }

    @Specialization(guards = {"supportsIntResult"})
    protected int doInt(byte left, int right) {
        return performArithmeticEnableNACheck(RRuntime.logical2int(left), right);
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
        return performArithmeticDoubleEnableNACheck(left, right);
    }

    @Specialization
    protected double doDouble(double left, byte right) {
        return performArithmeticDoubleEnableNACheck(left, RRuntime.logical2double(right));
    }

    @Specialization
    protected double doDouble(byte left, double right) {
        return performArithmeticDoubleEnableNACheck(RRuntime.logical2double(left), right);
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
        return performArithmeticEnableNACheck(RRuntime.logical2int(left), RRuntime.logical2int(right));
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

    protected static boolean differentDimensions(RAbstractVector left, RAbstractVector right) {
        return BinaryBooleanNode.differentDimensions(left, right);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "differentDimensions")
    protected RLogicalVector doIntVectorDifferentLength(RAbstractVector left, RAbstractVector right) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.NON_CONFORMABLE_ARRAYS);
    }

    // int vector and vectors

    @Specialization(guards = {"!areSameLength", "supportsIntResult"})
    protected RIntVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpDifferentLength(left, right);
    }

    @Specialization(guards = {"areSameLength", "supportsIntResult"})
    protected RIntVector doIntVectorSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        return performIntVectorOpSameLength(left, right);
    }

    @Specialization(guards = "!areSameLength")
    protected RDoubleVector doIntVectorDifferentLength(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = "areSameLength")
    protected RDoubleVector doIntVectorSameLength(RAbstractIntVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpSameLength(RClosures.createIntToDoubleVector(left, leftNACheck), right);
    }

    @Specialization(guards = "!areSameLength")
    protected RDoubleVector doIntVectorDifferentLength(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorOpDifferentLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = "areSameLength")
    protected RDoubleVector doIntVectorIntVectorSameLength(RAbstractDoubleVector left, RAbstractIntVector right) {
        return performDoubleVectorOpSameLength(left, RClosures.createIntToDoubleVector(right, rightNACheck));
    }

    @Specialization(guards = {"!areSameLength", "supportsIntResult"})
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

    @Specialization(guards = "!areSameLength")
    protected RDoubleVector doDoubleVectorDifferentLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        return performDoubleVectorOpDifferentLength(left, right);
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

    // implementation

    @SlowPath
    private void copyAttributes(RVector ret, RAbstractVector left, RAbstractVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int length = Math.max(leftLength, rightLength);
        ret.copyRegAttributesFrom(leftLength == length ? left : right);
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), this.getSourceSection());
        ret.copyNamesFrom(leftLength == length ? left : right);
    }

    @SlowPath
    private void copyAttributesSameLength(RVector ret, RAbstractVector left, RAbstractVector right) {
        ret.copyRegAttributesFrom(right);
        ret.copyRegAttributesFrom(left);
        ret.setDimensions(left.hasDimensions() ? left.getDimensions() : right.getDimensions(), getEncapsulatingSourceSection());
        if (!ret.copyNamesFrom(left)) {
            ret.copyNamesFrom(right);
        }
    }

    private RComplexVector performComplexVectorOpDifferentLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        if (emptyVector.profile(leftLength == 0 || rightLength == 0)) {
            return RDataFactory.createEmptyComplexVector();
        }
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
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
        boolean notMultiple = j != 0 || k != 0;
        if (notMultiple) {
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        RComplexVector ret = RDataFactory.createComplexVector(result, isComplete());
        copyAttributes(ret, left, right);
        return ret;
    }

    private RDoubleVector performDoubleVectorOpDifferentLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        int length = Math.max(leftLength, rightLength);
        if (emptyVector.profile(leftLength == 0 || rightLength == 0)) {
            return RDataFactory.createEmptyDoubleVector();
        }
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
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
        boolean notMultiple = j != 0 || k != 0;
        if (notMultiple) {
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(result, isComplete());
        copyAttributes(ret, left, right);
        return ret;
    }

    private RIntVector performIntVectorOpDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        if (emptyVector.profile(leftLength == 0 || rightLength == 0)) {
            return RDataFactory.createEmptyIntVector();
        }
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
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
        boolean notMultiple = j != 0 || k != 0;
        if (notMultiple) {
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        RIntVector ret = RDataFactory.createIntVector(result, isComplete());
        copyAttributes(ret, left, right);
        return ret;
    }

    private RDoubleVector performIntVectorOpDoubleDifferentLength(RAbstractIntVector left, RAbstractIntVector right) {
        int leftLength = left.getLength();
        int rightLength = right.getLength();
        if (emptyVector.profile(leftLength == 0 || rightLength == 0)) {
            return RDataFactory.createEmptyDoubleVector();
        }
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        int length = Math.max(leftLength, rightLength);
        double[] result = new double[length];
        int j = 0;
        int k = 0;
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(k);
            int rightValue = right.getDataAt(j);
            result[i] = performArithmeticIntIntDouble(leftValue, rightValue);
            j = Utils.incMod(j, rightLength);
            k = Utils.incMod(k, leftLength);
        }
        boolean notMultiple = j != 0 || k != 0;
        if (notMultiple) {
            RError.warning(RError.Message.LENGTH_NOT_MULTI);
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(result, isComplete());
        copyAttributes(ret, left, right);
        return ret;
    }

    private RComplexVector performComplexVectorOpSameLength(RAbstractComplexVector left, RAbstractComplexVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        if (emptyVector.profile(length == 0)) {
            return RDataFactory.createEmptyComplexVector();
        }
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        double[] result = new double[length << 1];
        for (int i = 0; i < length; ++i) {
            RComplex leftValue = left.getDataAt(i);
            RComplex rightValue = right.getDataAt(i);
            RComplex resultValue = performArithmeticComplex(leftValue, rightValue);
            int index = i << 1;
            result[index] = resultValue.getRealPart();
            result[index + 1] = resultValue.getImaginaryPart();
        }
        RComplexVector ret = RDataFactory.createComplexVector(result, isComplete());
        copyAttributesSameLength(ret, left, right);
        return ret;
    }

    private RDoubleVector performDoubleVectorOpSameLength(RAbstractDoubleVector left, RAbstractDoubleVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        if (emptyVector.profile(length == 0)) {
            return RDataFactory.createEmptyDoubleVector();
        }
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            double leftValue = left.getDataAt(i);
            double rightValue = right.getDataAt(i);
            result[i] = performArithmeticDouble(leftValue, rightValue);
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(result, isComplete());
        copyAttributesSameLength(ret, left, right);
        return ret;
    }

    private RIntVector performIntVectorOpSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        if (emptyVector.profile(length == 0)) {
            return RDataFactory.createEmptyIntVector();
        }
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        int[] result = new int[length];
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            int rightValue = right.getDataAt(i);
            result[i] = performArithmetic(leftValue, rightValue);
        }
        RIntVector ret = RDataFactory.createIntVector(result, isComplete());
        copyAttributesSameLength(ret, left, right);
        return ret;
    }

    private RDoubleVector performIntVectorOpDoubleSameLength(RAbstractIntVector left, RAbstractIntVector right) {
        assert areSameLength(left, right);
        int length = left.getLength();
        if (emptyVector.profile(length == 0)) {
            return RDataFactory.createEmptyDoubleVector();
        }
        leftNACheck.enable(!left.isComplete());
        rightNACheck.enable(!right.isComplete());
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            int leftValue = left.getDataAt(i);
            int rightValue = right.getDataAt(i);
            result[i] = performArithmeticIntIntDouble(leftValue, rightValue);
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(result, isComplete());
        copyAttributesSameLength(ret, left, right);
        return ret;
    }

    private double performArithmeticDoubleEnableNACheck(double left, double right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticDouble(left, right);
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

    private RComplex performArithmeticComplexEnableNACheck(RComplex left, RComplex right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticComplex(left, right);
    }

    private RComplex performArithmeticComplex(RComplex left, RComplex right) {
        if (leftNACheck.check(left)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && right.isZero()) {
                // CORNER: (0i + NA)^0 == 1
                return RDataFactory.createComplexRealOne();
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw RError.error(this.getEncapsulatingSourceSection(), RError.Message.UNIMPLEMENTED_COMPLEX);
            }
            return RRuntime.createComplexNA();
        }
        if (rightNACheck.check(right)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && left.isZero()) {
                // CORNER: 0^(0i + NA) == NaN + NaNi
                return RDataFactory.createComplex(Double.NaN, Double.NaN);
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw RError.error(this.getEncapsulatingSourceSection(), RError.Message.UNIMPLEMENTED_COMPLEX);
            }
            return RRuntime.createComplexNA();
        }
        return arithmetic.op(left.getRealPart(), left.getImaginaryPart(), right.getRealPart(), right.getImaginaryPart());
    }

    private int performArithmeticEnableNACheck(int left, int right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmetic(left, right);
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

    private double performArithmeticIntIntDoubleEnableNACheck(int left, int right) {
        leftNACheck.enable(left);
        rightNACheck.enable(right);
        return performArithmeticIntIntDouble(left, right);
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

    private boolean isComplete() {
        return leftNACheck.neverSeenNA() && rightNACheck.neverSeenNA() && resultNACheck.neverSeenNA();
    }

}
