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
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class UnaryArithmeticNode extends UnaryNode {

    private final UnaryArithmetic arithmetic;

    private final NACheck na = NACheck.create();

    public UnaryArithmeticNode(UnaryArithmeticFactory factory) {
        this.arithmetic = adoptChild(factory.create());
    }

    public UnaryArithmeticNode(UnaryArithmeticNode prev) {
        this.arithmetic = adoptChild(prev.arithmetic);
    }

    @Specialization(order = 1, guards = "!isNA")
    public int doInt(int operand) {
        return arithmetic.op(operand);
    }

    @Specialization(order = 2, guards = "isNA")
    public int doIntNA(@SuppressWarnings("unused") int operand) {
        return RRuntime.INT_NA;
    }

    @Specialization(order = 3, guards = "!isNA")
    public double doDouble(double operand) {
        return arithmetic.op(operand);
    }

    @Specialization(order = 4, guards = "isNA")
    public double doDoubleNA(@SuppressWarnings("unused") double operand) {
        return RRuntime.DOUBLE_NA;
    }

    @Specialization(order = 5, guards = "!isComplexNA")
    public RComplex doComplex(RComplex operand) {
        return arithmetic.op(operand.getRealPart(), operand.getImaginaryPart());
    }

    @Specialization(order = 6, guards = "isComplexNA")
    public RComplex doComplexNA(@SuppressWarnings("unused") RComplex operand) {
        return RRuntime.createComplexNA();
    }

    @Specialization(order = 7, guards = "!isNA")
    public int doLogical(byte operand) {
        return arithmetic.op(operand);
    }

    @Specialization(order = 8, guards = "isNA")
    public int doLogicalNA(@SuppressWarnings("unused") int operand) {
        return RRuntime.INT_NA;
    }

    @Specialization(order = 10, guards = "isComplete")
    public RDoubleVector doDoubleVector(RDoubleVector operands) {
        double[] res = new double[operands.getLength()];
        for (int i = 0; i < operands.getLength(); ++i) {
            res[i] = arithmetic.op(operands.getDataAt(i));
        }
        return RDataFactory.createDoubleVector(res, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 11, guards = "!isComplete")
    public RDoubleVector doDoubleVectorNA(RDoubleVector operands) {
        double[] res = new double[operands.getLength()];
        na.enable(operands);
        for (int i = 0; i < operands.getLength(); ++i) {
            if (na.check(operands.getDataAt(i))) {
                res[i] = RRuntime.DOUBLE_NA;
            } else {
                res[i] = arithmetic.op(operands.getDataAt(i));
            }
        }
        return RDataFactory.createDoubleVector(res, na.neverSeenNA());
    }

    @Specialization(order = 20, guards = "isComplete")
    public RComplexVector doComplexVector(RComplexVector operands) {
        double[] res = new double[operands.getLength() * 2];
        for (int i = 0; i < operands.getLength(); ++i) {
            RComplex r = arithmetic.op(operands.getDataAt(i).getRealPart(), operands.getDataAt(i).getImaginaryPart());
            res[2 * i] = r.getRealPart();
            res[2 * i + 1] = r.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(res, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 21, guards = "!isComplete")
    public RComplexVector doComplexVectorNA(RComplexVector operands) {
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
        return RDataFactory.createComplexVector(res, na.neverSeenNA());
    }

    protected static boolean isComplexNA(RComplex c) {
        return c.isNA();
    }

    protected static boolean isComplete(RComplexVector cv) {
        return cv.isComplete();
    }

    protected static boolean isComplete(RDoubleVector dv) {
        return dv.isComplete();
    }
}
