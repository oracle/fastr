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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class UnaryArithmeticNode extends UnaryNode {

    private final UnaryArithmetic arithmetic;

    private final NACheck na = NACheck.create();

    public UnaryArithmeticNode(UnaryArithmeticFactory factory) {
        this.arithmetic = factory.create();
    }

    public UnaryArithmeticNode(UnaryArithmeticNode prev) {
        this.arithmetic = prev.arithmetic;
    }

    public abstract Object execute(VirtualFrame frame, Object operand);

    @Specialization(guards = "!isNA")
    public int doInt(int operand) {
        return arithmetic.op(operand);
    }

    @Specialization(guards = "isNA")
    public int doIntNA(@SuppressWarnings("unused") int operand) {
        return RRuntime.INT_NA;
    }

    @Specialization(guards = "!isNA")
    public double doDouble(double operand) {
        return arithmetic.op(operand);
    }

    @Specialization(guards = "isNA")
    public double doDoubleNA(@SuppressWarnings("unused") double operand) {
        return RRuntime.DOUBLE_NA;
    }

    @Specialization(guards = "!isComplexNA")
    public RComplex doComplex(RComplex operand) {
        return arithmetic.op(operand.getRealPart(), operand.getImaginaryPart());
    }

    @Specialization(guards = "isComplexNA")
    public RComplex doComplexNA(@SuppressWarnings("unused") RComplex operand) {
        return RRuntime.createComplexNA();
    }

    @Specialization(guards = "!isNA")
    public int doLogical(byte operand) {
        return arithmetic.op(operand);
    }

    @Specialization(guards = "isNA")
    public int doLogicalNA(@SuppressWarnings("unused") byte operand) {
        return RRuntime.INT_NA;
    }

    private static void copyAttributes(RVector ret, RAbstractVector v) {
        ret.copyRegAttributesFrom(v);
        ret.setDimensions(v.getDimensions());
        ret.copyNamesFrom(v);
    }

    @Specialization(guards = "isComplete")
    public RDoubleVector doDoubleVector(RAbstractDoubleVector operands) {
        double[] res = new double[operands.getLength()];
        for (int i = 0; i < operands.getLength(); ++i) {
            res[i] = arithmetic.op(operands.getDataAt(i));
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(res, RDataFactory.COMPLETE_VECTOR);
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "!isComplete")
    public RDoubleVector doDoubleVectorNA(RAbstractDoubleVector operands) {
        double[] res = new double[operands.getLength()];
        na.enable(operands);
        for (int i = 0; i < operands.getLength(); ++i) {
            if (na.check(operands.getDataAt(i))) {
                res[i] = RRuntime.DOUBLE_NA;
            } else {
                res[i] = arithmetic.op(operands.getDataAt(i));
            }
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(res, na.neverSeenNA());
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "isComplete")
    public RComplexVector doComplexVector(RAbstractComplexVector operands) {
        double[] res = new double[operands.getLength() * 2];
        for (int i = 0; i < operands.getLength(); ++i) {
            RComplex r = arithmetic.op(operands.getDataAt(i).getRealPart(), operands.getDataAt(i).getImaginaryPart());
            res[2 * i] = r.getRealPart();
            res[2 * i + 1] = r.getImaginaryPart();
        }
        RComplexVector ret = RDataFactory.createComplexVector(res, RDataFactory.COMPLETE_VECTOR);
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "!isComplete")
    public RComplexVector doComplexVectorNA(RAbstractComplexVector operands) {
        double[] res = new double[operands.getLength() * 2];
        na.enable(operands);
        for (int i = 0; i < operands.getLength(); ++i) {
            if (na.check(operands.getDataAt(i))) {
                res[2 * i] = RRuntime.DOUBLE_NA;
                res[2 * i + 1] = 0.0;
            } else {
                RComplex r = arithmetic.op(operands.getDataAt(i).getRealPart(), operands.getDataAt(i).getImaginaryPart());
                res[2 * i] = r.getRealPart();
                res[2 * i + 1] = r.getImaginaryPart();
            }
        }
        RComplexVector ret = RDataFactory.createComplexVector(res, na.neverSeenNA());
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "isComplete")
    public RIntVector doIntVector(RAbstractIntVector operands) {
        int[] res = new int[operands.getLength()];
        for (int i = 0; i < operands.getLength(); ++i) {
            res[i] = arithmetic.op(operands.getDataAt(i));
        }
        RIntVector ret = RDataFactory.createIntVector(res, RDataFactory.COMPLETE_VECTOR);
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "!isComplete")
    public RIntVector doIntVectorNA(RAbstractIntVector operands) {
        int[] res = new int[operands.getLength()];
        na.enable(operands);
        for (int i = 0; i < operands.getLength(); ++i) {
            if (na.check(operands.getDataAt(i))) {
                res[i] = RRuntime.INT_NA;
            } else {
                res[i] = arithmetic.op(operands.getDataAt(i));
            }
        }
        RIntVector ret = RDataFactory.createIntVector(res, na.neverSeenNA());
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "isComplete")
    public RIntVector doLogicalVector(RAbstractLogicalVector operands) {
        return doIntVector(RClosures.createLogicalToIntVector(operands, na));
    }

    @Specialization(guards = "!isComplete")
    public RIntVector doLogicalVectorNA(RAbstractLogicalVector operands) {
        return doIntVectorNA(RClosures.createLogicalToIntVector(operands, na));
    }

    @Specialization
    public Object doStringVector(VirtualFrame frame, @SuppressWarnings("unused") RAbstractStringVector operands) {
        throw RError.error(frame, this.getEncapsulatingSourceSection(), RError.Message.INVALID_ARG_TYPE_UNARY);
    }

    @Specialization
    public Object doRawVector(VirtualFrame frame, @SuppressWarnings("unused") RAbstractRawVector operands) {
        throw RError.error(frame, this.getEncapsulatingSourceSection(), RError.Message.INVALID_ARG_TYPE_UNARY);
    }

    protected static boolean isComplexNA(RComplex c) {
        return c.isNA();
    }

    protected static boolean isComplete(RAbstractVector cv) {
        return cv.isComplete();
    }

}
