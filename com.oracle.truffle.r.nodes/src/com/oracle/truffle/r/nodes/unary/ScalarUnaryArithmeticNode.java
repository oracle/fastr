package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic.Negate;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic.Plus;

public class ScalarUnaryArithmeticNode extends ScalarUnaryNode {

    @Child private UnaryArithmetic arithmetic;

    public ScalarUnaryArithmeticNode(UnaryArithmetic arithmetic) {
        this.arithmetic = arithmetic;
    }

    @Override
    public RAbstractVector tryFoldConstantTime(RAbstractVector operand, int operandLength) {
        if (arithmetic instanceof Plus) {
            return operand;
        } else if (arithmetic instanceof Negate && operand instanceof RSequence) {
            if (operand instanceof RIntSequence) {
                int start = ((RIntSequence) operand).getStart();
                int stride = ((RIntSequence) operand).getStride();
                return RDataFactory.createIntSequence(applyInteger(start), applyInteger(stride), operandLength);
            } else if (operand instanceof RDoubleSequence) {
                double start = ((RDoubleSequence) operand).getStart();
                double stride = ((RDoubleSequence) operand).getStride();
                return RDataFactory.createDoubleSequence(applyDouble(start), applyDouble(stride), operandLength);
            }
        }
        return null;
    }

    @Override
    public boolean mayFoldConstantTime(Class<? extends RAbstractVector> operandClass) {
        if (arithmetic instanceof Plus) {
            return true;
        } else if (arithmetic instanceof Negate && RSequence.class.isAssignableFrom(operandClass)) {
            return true;
        }
        return false;
    }

    @Override
    public final double applyDouble(double operand) {
        if (operandNACheck.check(operand)) {
            return RRuntime.DOUBLE_NA;
        }
        return arithmetic.op(operand);
    }

    @Override
    public final RComplex applyComplex(RComplex operand) {
        if (operandNACheck.check(operand)) {
            return RComplex.NA;
        }
        return arithmetic.op(operand.getRealPart(), operand.getImaginaryPart());
    }

    @Override
    public final int applyInteger(int operand) {
        if (operandNACheck.check(operand)) {
            return RRuntime.INT_NA;
        }
        return arithmetic.op(operand);
    }

}
