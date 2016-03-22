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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.r.nodes.primitive.UnaryMapNAFunctionNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic.Negate;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic.Plus;

public class ScalarUnaryArithmeticNode extends UnaryMapNAFunctionNode {

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
    public final double applyDouble(RComplex operand) {
        if (operandNACheck.check(operand)) {
            return RRuntime.DOUBLE_NA;
        }
        return arithmetic.opd(operand.getRealPart(), operand.getImaginaryPart());
    }

    @Override
    public final RComplex applyComplex(RComplex operand) {
        if (operandNACheck.check(operand)) {
            return RComplex.createNA();
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
