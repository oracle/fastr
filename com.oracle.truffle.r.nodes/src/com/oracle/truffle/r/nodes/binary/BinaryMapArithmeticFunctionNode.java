/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.nodes.primitive.BinaryMapNAFunctionNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Add;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Div;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.IntegerDiv;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Multiply;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic.Subtract;
import com.oracle.truffle.r.runtime.ops.Operation;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 *
 */
public final class BinaryMapArithmeticFunctionNode extends BinaryMapNAFunctionNode {

    @Child private BinaryArithmetic arithmetic;

    public BinaryMapArithmeticFunctionNode(BinaryArithmetic arithmetic) {
        this.arithmetic = arithmetic;
    }

    @Override
    protected boolean resultNeedsNACheck() {
        return arithmetic.introducesNA();
    }

    @Override
    public boolean mayFoldConstantTime(Class<? extends RAbstractVector> leftClass, Class<? extends RAbstractVector> rightClass) {
        return (isSequenceAddArithmetic() || isSequenceMulArithmetic()) && (RSequence.class.isAssignableFrom(leftClass) || RSequence.class.isAssignableFrom(rightClass));
    }

    @Override
    public RAbstractVector tryFoldConstantTime(RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        if (isSequenceAddArithmetic()) {
            return sequenceAddOperation(left, leftLength, right, rightLength);
        } else if (isSequenceMulArithmetic()) {
            return sequenceMulOperation(left, leftLength, right, rightLength);
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
            double value = arithmetic.op(left, right);
            resultNACheck.check(value);
            return value;
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    @Override
    public int applyInteger(int left, int right) {
        assert arithmetic.isSupportsIntResult();

        if (leftNACheck.check(left)) {
            return RRuntime.INT_NA;
        }
        if (rightNACheck.check(right)) {
            return RRuntime.INT_NA;
        }
        try {
            int value = arithmetic.op(left, right);
            resultNACheck.check(value);
            return value;
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
            double value = arithmetic.op((double) left, (double) right);
            resultNACheck.check(value);
            return value;
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
                throw RError.error(this, RError.Message.UNIMPLEMENTED_COMPLEX);
            }
            return RRuntime.createComplexNA();
        }
        if (rightNACheck.check(right)) {
            if (this.arithmetic instanceof BinaryArithmetic.Pow && left.isZero()) {
                // CORNER: 0^(0i + NA) == NaN + NaNi
                return RDataFactory.createComplex(Double.NaN, Double.NaN);
            } else if (this.arithmetic instanceof BinaryArithmetic.Mod) {
                // CORNER: Must throw error on modulo operation on complex numbers.
                throw RError.error(this, RError.Message.UNIMPLEMENTED_COMPLEX);
            }
            return RRuntime.createComplexNA();
        }
        try {
            RComplex value = arithmetic.op(left.getRealPart(), left.getImaginaryPart(), right.getRealPart(), right.getImaginaryPart());
            resultNACheck.check(value);
            return value;
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            throw Operation.handleException(e);
        }
    }

    private RAbstractVector sequenceMulOperation(RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        if (left instanceof RSequence) {
            if (rightLength == 1) {
                // result_start = left_start <op> right[[0]]
                // result_stride = left_stride <op> right[[0]]
                // result_length = left_length
                Object firstValue = right.getDataAtAsObject(0);
                return foldSequence((RSequence) left, firstValue, firstValue, rightNACheck);
            }
        } else if (right instanceof RSequence && arithmetic.isCommutative() && leftLength == 1) {
            // result_start = right_start <op> left[[0]]
            // result_stride = right_stride <op> left[[0]]
            // result_length = right_length
            Object firstValue = left.getDataAtAsObject(0);
            return foldSequence((RSequence) right, firstValue, firstValue, rightNACheck);
        }
        return null;
    }

    private RAbstractVector sequenceAddOperation(RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
        if (left instanceof RSequence) {
            if (rightLength == 1) {
                // result_start = left_start <op> right[[0]]
                // result_stride = left_stride
                // result_length = left_length
                return foldSequence((RSequence) left, right.getDataAtAsObject(0), null, rightNACheck);
            } else if (right instanceof RSequence && leftLength == rightLength) {
                // result_start = left_start <op> right_start
                // result_stride = left_stride <op> right_stride
                // result_length = left_length = right_length
                RSequence otherSequence = (RSequence) right;
                return foldSequence((RSequence) left, otherSequence.getStartObject(), otherSequence.getStrideObject(), rightNACheck);
            }
        } else if (right instanceof RSequence && arithmetic.isCommutative() && leftLength == 1) {
            // result_start = right_start <op> left[[0]]
            // result_stride = right_stride
            // result_length = right_length
            return foldSequence((RSequence) right, left.getDataAtAsObject(0), null, leftNACheck);
        }
        return null;
    }

    private RAbstractVector foldSequence(RSequence sequence, Object otherStart, Object otherStride, NACheck otherNACheck) {
        if (sequence instanceof RIntSequence) {
            return foldIntSequence(sequence, otherStart, otherStride, otherNACheck);
        } else if (sequence instanceof RDoubleSequence) {
            return foldDoubleSequence(sequence, otherStart, otherStride, otherNACheck);
        }
        return null;
    }

    private RAbstractVector foldDoubleSequence(RSequence sequence, Object otherStart, Object otherStride, NACheck otherNACheck) {
        double otherStartDouble = (double) otherStart;
        if (otherNACheck.check(otherStartDouble)) {
            return null;
        }
        RDoubleSequence castSequence = (RDoubleSequence) sequence;
        double newStart = applyDouble(castSequence.getStart(), otherStartDouble);
        if (resultNACheck.check(newStart)) {
            return null;
        }
        double newStride;
        if (otherStride == null) {
            newStride = castSequence.getStride();
        } else {
            double otherStrideDouble = (double) otherStride;
            if (otherNACheck.check(otherStrideDouble)) {
                return null;
            }
            newStride = applyDouble(castSequence.getStride(), otherStrideDouble);
            if (resultNACheck.check(newStride)) {
                return null;
            }
        }
        return RDataFactory.createDoubleSequence(newStart, newStride, castSequence.getLength());
    }

    private RAbstractVector foldIntSequence(RSequence sequence, Object otherStart, Object otherStride, NACheck otherNACheck) {
        int otherStartInt = (int) otherStart;
        if (otherNACheck.check(otherStartInt)) {
            return null;
        }
        if (arithmetic.isSupportsIntResult()) {
            return foldIntSequenceIntResult(sequence, otherStartInt, otherStride, otherNACheck);
        } else {
            return foldIntSequenceDoubleResult(sequence, otherStartInt, otherStride, otherNACheck);
        }
    }

    private RAbstractVector foldIntSequenceDoubleResult(RSequence sequence, int otherStartInt, Object otherStride, NACheck otherNACheck) {
        RIntSequence castSequence = (RIntSequence) sequence;
        double newStart = applyDouble(castSequence.getStart(), otherStartInt);
        if (resultNACheck.check(newStart)) {
            return null;
        }
        double newStride;
        if (otherStride == null) {
            newStride = castSequence.getStride();
        } else {
            int otherStrideInt = (int) otherStride;
            if (otherNACheck.check(otherStartInt)) {
                return null;
            }
            newStride = applyDouble(castSequence.getStride(), otherStrideInt);
            if (resultNACheck.check(newStride)) {
                return null;
            }
        }
        return RDataFactory.createDoubleSequence(newStart, newStride, castSequence.getLength());
    }

    private RAbstractVector foldIntSequenceIntResult(RSequence sequence, int otherStartInt, Object otherStride, NACheck otherNACheck) {
        RIntSequence castSequence = (RIntSequence) sequence;
        int currentStart = castSequence.getStart();
        int newStart = applyInteger(currentStart, otherStartInt);
        if (resultNACheck.check(newStart)) {
            return null;
        }

        int newStride;
        if (otherStride == null) {
            newStride = castSequence.getStride();
        } else {
            int otherStrideInt = (int) otherStride;
            if (otherNACheck.check(otherStartInt)) {
                return null;
            }
            newStride = applyInteger(castSequence.getStride(), otherStrideInt);
            if (resultNACheck.check(newStride)) {
                return null;
            }
        }
        return RDataFactory.createIntSequence(newStart, newStride, castSequence.getLength());
    }
}
