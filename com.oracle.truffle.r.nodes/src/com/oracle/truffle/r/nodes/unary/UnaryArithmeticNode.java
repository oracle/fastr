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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class UnaryArithmeticNode extends UnaryNode {

    private final UnaryArithmetic arithmetic;

    private final NAProfile naProfile = NAProfile.create();

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    private final Message error;

    public UnaryArithmeticNode(UnaryArithmeticFactory factory, Message error) {
        this.arithmetic = factory.create();
        this.error = error;
    }

    public UnaryArithmeticNode(UnaryArithmeticNode prev) {
        this.arithmetic = prev.arithmetic;
        this.error = prev.error;
    }

    public abstract Object execute(VirtualFrame frame, Object operand);

    @Specialization
    protected int doInt(int operand) {
        return naProfile.isNA(operand) ? RRuntime.INT_NA : arithmetic.op(operand);
    }

    @Specialization
    protected double doDouble(double operand) {
        return naProfile.isNA(operand) ? RRuntime.DOUBLE_NA : arithmetic.op(operand);
    }

    @Specialization
    protected RComplex doComplex(RComplex operand) {
        return naProfile.isNA(operand) ? RRuntime.createComplexNA() : arithmetic.op(operand.getRealPart(), operand.getImaginaryPart());
    }

    @Specialization
    protected int doLogical(byte operand) {
        return naProfile.isNA(operand) ? RRuntime.INT_NA : arithmetic.op(operand);
    }

    private void copyAttributes(RVector ret, RAbstractVector v) {
        ret.copyRegAttributesFrom(v);
        ret.setDimensions(v.getDimensions());
        ret.copyNamesFrom(attrProfiles, v);
    }

    @Specialization(guards = "operands.isComplete()")
    protected RDoubleVector doDoubleVector(RAbstractDoubleVector operands) {
        double[] res = new double[operands.getLength()];
        for (int i = 0; i < operands.getLength(); i++) {
            res[i] = arithmetic.op(operands.getDataAt(i));
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(res, RDataFactory.COMPLETE_VECTOR);
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "!operands.isComplete()")
    protected RDoubleVector doDoubleVectorNA(RAbstractDoubleVector operands) {
        double[] res = new double[operands.getLength()];
        for (int i = 0; i < operands.getLength(); i++) {
            if (RRuntime.isNA(operands.getDataAt(i))) {
                res[i] = RRuntime.DOUBLE_NA;
            } else {
                res[i] = arithmetic.op(operands.getDataAt(i));
            }
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(res, false);
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "operands.isComplete()")
    protected RComplexVector doComplexVector(RAbstractComplexVector operands) {
        double[] res = new double[operands.getLength() * 2];
        for (int i = 0; i < operands.getLength(); i++) {
            RComplex r = arithmetic.op(operands.getDataAt(i).getRealPart(), operands.getDataAt(i).getImaginaryPart());
            res[2 * i] = r.getRealPart();
            res[2 * i + 1] = r.getImaginaryPart();
        }
        RComplexVector ret = RDataFactory.createComplexVector(res, RDataFactory.COMPLETE_VECTOR);
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "!operands.isComplete()")
    protected RComplexVector doComplexVectorNA(RAbstractComplexVector operands) {
        double[] res = new double[operands.getLength() * 2];
        for (int i = 0; i < operands.getLength(); i++) {
            if (RRuntime.isNA(operands.getDataAt(i))) {
                res[2 * i] = RRuntime.DOUBLE_NA;
                res[2 * i + 1] = 0.0;
            } else {
                RComplex r = arithmetic.op(operands.getDataAt(i).getRealPart(), operands.getDataAt(i).getImaginaryPart());
                res[2 * i] = r.getRealPart();
                res[2 * i + 1] = r.getImaginaryPart();
            }
        }
        RComplexVector ret = RDataFactory.createComplexVector(res, false);
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "operands.isComplete()")
    protected RIntVector doIntVector(RAbstractIntVector operands) {
        int[] res = new int[operands.getLength()];
        for (int i = 0; i < operands.getLength(); i++) {
            res[i] = arithmetic.op(operands.getDataAt(i));
        }
        RIntVector ret = RDataFactory.createIntVector(res, RDataFactory.COMPLETE_VECTOR);
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "!operands.isComplete()")
    protected RIntVector doIntVectorNA(RAbstractIntVector operands) {
        int[] res = new int[operands.getLength()];
        for (int i = 0; i < operands.getLength(); i++) {
            if (RRuntime.isNA(operands.getDataAt(i))) {
                res[i] = RRuntime.INT_NA;
            } else {
                res[i] = arithmetic.op(operands.getDataAt(i));
            }
        }
        RIntVector ret = RDataFactory.createIntVector(res, false);
        copyAttributes(ret, operands);
        return ret;
    }

    @Specialization(guards = "operands.isComplete()")
    protected RIntVector doLogicalVector(RAbstractLogicalVector operands) {
        return doIntVector(RClosures.createLogicalToIntVector(operands));
    }

    @Specialization(guards = "!operands.isComplete()")
    protected RIntVector doLogicalVectorNA(RAbstractLogicalVector operands) {
        return doIntVectorNA(RClosures.createLogicalToIntVector(operands));
    }

    @Fallback
    protected Object invalidArgType(@SuppressWarnings("unused") Object operand) {
        throw RError.error(getEncapsulatingSourceSection(), error);
    }

}
