/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.primitive.BinaryMapNAFunctionNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSeqVectorData;
import com.oracle.truffle.r.runtime.data.RIntSeqVectorData;
import com.oracle.truffle.r.runtime.data.RSeq;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Add;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Div;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.IntegerDiv;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Multiply;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Subtract;
import com.oracle.truffle.r.runtime.ops.Operation;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.WarningInfo;

/**
 *
 */
public final class BinaryMapArithmeticFunctionNode extends BinaryMapNAFunctionNode {

    @Child private BinaryArithmetic arithmetic;
    @Child private VectorDataLibrary leftDataLib;
    @Child private VectorDataLibrary rightDataLib;

    protected final NACheck resultNACheck = NACheck.create();

    private final ConditionProfile finiteResult = ConditionProfile.createBinaryProfile();

    public BinaryMapArithmeticFunctionNode(BinaryArithmetic arithmetic) {
        this.arithmetic = arithmetic;
    }

    public Object getLeftDataAt(Object leftData, int index) {
        if (leftDataLib == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            leftDataLib = insert(VectorDataLibrary.getFactory().create(leftData));
        }
        return leftDataLib.getDataAtAsObject(leftData, index);
    }

    public Object getRightDataAt(Object rightData, int index) {
        if (rightDataLib == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rightDataLib = insert(VectorDataLibrary.getFactory().create(rightData));
        }
        return rightDataLib.getDataAtAsObject(rightData, index);
    }

    @Override
    protected boolean introducesNA() {
        return arithmetic.introducesNA();
    }

    @Override
    public boolean mayFoldConstantTime(RAbstractVector left, RAbstractVector right) {
        return (isSequenceAddArithmetic() || isSequenceMulArithmetic()) && (left.isSequence() || right.isSequence());
    }

    @Override
    public RAbstractVector tryFoldConstantTime(WarningInfo warningInfo, Object leftData, int leftLength, Object rightData, int rightLength) {
        if (isSequenceAddArithmetic()) {
            return sequenceAddOperation(warningInfo, leftData, leftLength, rightData, rightLength);
        } else if (isSequenceMulArithmetic()) {
            return sequenceMulOperation(warningInfo, leftData, leftLength, rightData, rightLength);
        }
        return null;
    }

    private boolean isSequenceMulArithmetic() {
        return arithmetic instanceof Multiply || arithmetic instanceof IntegerDiv || arithmetic instanceof Div;
    }

    private boolean isSequenceAddArithmetic() {
        return arithmetic instanceof Add || arithmetic instanceof Subtract;
    }

    @Override
    public double applyDouble(double left, double right) {
        if (leftNACheck.check(left)) {
            // Note: these corner cases also apply in BinaryArithmeticSpecial node
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
        try {
            return arithmetic.op(left, right);
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    @Override
    public int applyInteger(int left, int right) {
        return applyInteger(null, left, right);
    }

    @Override
    public int applyInteger(WarningInfo warningInfo, int left, int right) {
        assert arithmetic.isSupportsIntResult();

        if (leftNACheck.check(left)) {
            return RRuntime.INT_NA;
        }
        if (rightNACheck.check(right)) {
            return RRuntime.INT_NA;
        }
        try {
            return arithmetic.op(warningInfo, left, right);
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    @Override
    public double applyDouble(int left, int right) {
        assert !arithmetic.isSupportsIntResult();

        if (leftNACheck.check(left)) {
            return RRuntime.DOUBLE_NA;
        }
        if (rightNACheck.check(right)) {
            return RRuntime.DOUBLE_NA;
        }
        try {
            return arithmetic.op((double) left, (double) right);
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    @Override
    public RComplex applyComplex(RComplex left, RComplex right) {
        if (leftNACheck.check(left)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && right.isZero()) {
                // CORNER: (0i + NA)^0 == 1
                return RDataFactory.createComplexRealOne();
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw error(RError.Message.UNIMPLEMENTED_COMPLEX);
            }
            return RComplex.createNA();
        }
        if (rightNACheck.check(right) && !(leftNACheck.checkNAorNaN(left.getRealPart()))) {

            if (this.arithmetic instanceof BinaryArithmetic.Pow && left.isZero()) {
                // CORNER: 0^(0i + NA) == NaN + NaNi
                return RDataFactory.createComplex(Double.NaN, Double.NaN);
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw error(RError.Message.UNIMPLEMENTED_COMPLEX);
            }
            return RComplex.createNA();
        }
        try {
            return arithmetic.op(left.getRealPart(), left.getImaginaryPart(), right.getRealPart(), right.getImaginaryPart());
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    private RAbstractVector sequenceMulOperation(WarningInfo warningInfo, Object leftData, int leftLength, Object rightData, int rightLength) {
        if (leftData instanceof RSeq) {
            if (rightLength == 1) {
                // result_start = left_start <op> right[[0]]
                // result_stride = left_stride <op> right[[0]]
                // result_length = left_length
                Object firstValue = getRightDataAt(rightData, 0);
                return foldSequence(warningInfo, (RSeq) leftData, firstValue, firstValue, rightNACheck);
            }
        } else if (rightData instanceof RSeq && arithmetic.isCommutative() && leftLength == 1) {
            // result_start = right_start <op> left[[0]]
            // result_stride = right_stride <op> left[[0]]
            // result_length = right_length
            Object firstValue = getLeftDataAt(leftData, 0);
            return foldSequence(warningInfo, (RSeq) rightData, firstValue, firstValue, rightNACheck);
        }
        return null;
    }

    private RAbstractVector sequenceAddOperation(WarningInfo warningInfo, Object leftData, int leftLength, Object rightData, int rightLength) {
        if (leftData instanceof RSeq) {
            if (rightLength == 1) {
                // result_start = left_start <op> right[[0]]
                // result_stride = left_stride
                // result_length = left_length
                return foldSequence(warningInfo, (RSeq) leftData, getRightDataAt(rightData, 0), null, rightNACheck);
            } else if (rightData instanceof RSeq && leftLength == rightLength) {
                // result_start = left_start <op> right_start
                // result_stride = left_stride <op> right_stride
                // result_length = left_length = right_length
                RSeq otherSequence = (RSeq) rightData;
                return foldSequence(warningInfo, (RSeq) leftData, otherSequence.getStartObject(), otherSequence.getStrideObject(), rightNACheck);
            }
        } else if (rightData instanceof RSeq && arithmetic.isCommutative() && leftLength == 1) {
            // result_start = right_start <op> left[[0]]
            // result_stride = right_stride
            // result_length = right_length
            return foldSequence(warningInfo, (RSeq) rightData, getLeftDataAt(leftData, 0), null, leftNACheck);
        }
        return null;
    }

    private RAbstractVector foldSequence(WarningInfo warningInfo, RSeq sequence, Object otherStart, Object otherStride, NACheck otherNACheck) {
        if (sequence instanceof RIntSeqVectorData) {
            return foldIntSequence(warningInfo, (RIntSeqVectorData) sequence, otherStart, otherStride, otherNACheck);
        } else if (sequence instanceof RDoubleSeqVectorData) {
            return foldDoubleSequence(sequence, otherStart, otherStride, otherNACheck);
        }
        return null;
    }

    private RAbstractVector foldDoubleSequence(RSeq sequence, Object otherStart, Object otherStride, NACheck otherNACheck) {
        double otherStartDouble = (double) otherStart;
        if (otherNACheck.check(otherStartDouble)) {
            return null;
        }
        RDoubleSeqVectorData castSequence = (RDoubleSeqVectorData) sequence;
        double newStart = applyDouble(castSequence.getStart(), otherStartDouble);
        resultNACheck.enable(arithmetic.introducesNA());
        if (resultNACheck.check(newStart)) {
            return null;
        }
        double newStride;
        double otherStrideDouble;
        if (otherStride == null) {
            newStride = castSequence.getStride();
            otherStrideDouble = 0d;
        } else {
            otherStrideDouble = (double) otherStride;
            if (otherNACheck.check(otherStrideDouble)) {
                return null;
            }
            newStride = applyDouble(castSequence.getStride(), otherStrideDouble);
            resultNACheck.enable(arithmetic.introducesNA());
            if (resultNACheck.check(newStride)) {
                return null;
            }
        }
        if (finiteResult.profile(Double.isFinite(newStart) && Double.isFinite(newStride))) {
            return RDataFactory.createDoubleSequence(newStart, newStride, castSequence.getLength());
        }
        int len = castSequence.getLength();
        double[] data = new double[len];
        double otherVal = otherStartDouble;
        for (int i = 0; i < len; i++) {
            data[i] = applyDouble(castSequence.getDoubleAt(i), otherVal);
            otherVal += otherStrideDouble;
        }
        return RDataFactory.createDoubleVector(data, true);
    }

    private RAbstractVector foldIntSequence(WarningInfo warningInfo, RIntSeqVectorData sequence, Object otherStart, Object otherStride, NACheck otherNACheck) {
        int otherStartInt = (int) otherStart;
        if (otherNACheck.check(otherStartInt)) {
            return null;
        }
        if (arithmetic.isSupportsIntResult()) {
            return foldIntSequenceIntResult(warningInfo, sequence, otherStartInt, otherStride, otherNACheck);
        } else {
            return foldIntSequenceDoubleResult(sequence, otherStartInt, otherStride, otherNACheck);
        }
    }

    private RAbstractVector foldIntSequenceDoubleResult(RIntSeqVectorData sequence, int otherStartInt, Object otherStride, NACheck otherNACheck) {
        double newStart = applyDouble(sequence.getStart(), otherStartInt);
        resultNACheck.enable(arithmetic.introducesNA());
        if (resultNACheck.check(newStart)) {
            return null;
        }
        double newStride;
        int otherStrideInt;
        if (otherStride == null) {
            newStride = sequence.getStride();
            otherStrideInt = 0;
        } else {
            otherStrideInt = (int) otherStride;
            if (otherNACheck.check(otherStartInt)) {
                return null;
            }
            newStride = applyDouble(sequence.getStride(), otherStrideInt);
            resultNACheck.enable(arithmetic.introducesNA());
            if (resultNACheck.check(newStride)) {
                return null;
            }
        }
        if (finiteResult.profile(Double.isFinite(newStart) && Double.isFinite(newStride))) {
            return RDataFactory.createDoubleSequence(newStart, newStride, sequence.getLength());
        }
        int len = sequence.getLength();
        double[] data = new double[len];
        double otherVal = otherStartInt;
        for (int i = 0; i < len; i++) {
            data[i] = applyDouble(sequence.getIntAt(i, NACheck.getDisabled()), otherVal);
            otherVal += otherStrideInt;
        }
        return RDataFactory.createDoubleVector(data, true);
    }

    private RAbstractVector foldIntSequenceIntResult(WarningInfo warningInfo, RIntSeqVectorData sequence, int otherStartInt, Object otherStride, NACheck otherNACheck) {
        int currentStart = sequence.getStart();
        int newStart = applyInteger(warningInfo, currentStart, otherStartInt);
        resultNACheck.enable(arithmetic.introducesNA());
        if (resultNACheck.check(newStart)) {
            return null;
        }

        int newStride;
        if (otherStride == null) {
            newStride = sequence.getStride();
        } else {
            int otherStrideInt = (int) otherStride;
            if (otherNACheck.check(otherStartInt)) {
                return null;
            }
            newStride = applyInteger(warningInfo, sequence.getStride(), otherStrideInt);
            resultNACheck.enable(arithmetic.introducesNA());
            if (resultNACheck.check(newStride)) {
                return null;
            }
        }
        return RDataFactory.createIntSequence(newStart, newStride, sequence.getLength());
    }
}
