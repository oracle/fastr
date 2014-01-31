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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class CastDoubleNode extends CastNode {

    private final NACheck naCheck = NACheck.create();

    public abstract Object executeDouble(VirtualFrame frame, int o);

    public abstract Object executeDouble(VirtualFrame frame, double o);

    public abstract Object executeDouble(VirtualFrame frame, byte o);

    public abstract Object executeDouble(VirtualFrame frame, Object o);

    public abstract Object executeDoubleVector(VirtualFrame frame, Object o);

    @Specialization
    public RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization(order = 1)
    public double doInt(int operand) {
        naCheck.enable(operand);
        return naCheck.convertIntToDouble(operand);
    }

    @Specialization(order = 10)
    public double doDouble(double operand) {
        return operand;
    }

    @Specialization(order = 20)
    public double doDouble(RComplex operand) {
        naCheck.enable(operand);
        double result = naCheck.convertComplexToDouble(operand);
        if (operand.getImaginaryPart() != 0.0) {
            RContext.getInstance().setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return result;
    }

    @Specialization(order = 30)
    public double doLogical(byte operand) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToDouble(operand);
    }

    @Specialization(order = 40)
    public double doString(String operand) {
        naCheck.enable(operand);
        double result = naCheck.convertStringToDouble(operand);
        if (isNA(result)) {
            RContext.getInstance().setEvalWarning(RError.NA_INTRODUCED_COERCION);
        }
        return result;
    }

    @Specialization
    public double doRaw(RRaw operand) {
        return RRuntime.raw2double(operand);
    }

    private double[] dataFromLogical(RLogicalVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            byte value = operand.getDataAt(i);
            ddata[i] = naCheck.convertLogicalToDouble(value);
        }
        return ddata;
    }

    private double[] dataFromString(RStringVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            ddata[i] = naCheck.convertStringToDouble(value);
            if (RRuntime.isNA(ddata[i])) {
                warning = true;
            }
        }
        if (warning) {
            RContext.getInstance().setEvalWarning(RError.NA_INTRODUCED_COERCION);
        }
        return ddata;
    }

    private static double[] dataFromRaw(RRawVector operand) {
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RRaw value = operand.getDataAt(i);
            ddata[i] = RRuntime.raw2double(value);
        }
        return ddata;
    }

    private double[] dataFromComplex(RComplexVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ddata[i] = naCheck.convertComplexToDouble(value);
            if (value.getImaginaryPart() != 0.0) {
                warning = true;
            }
        }
        if (warning) {
            RContext.getInstance().setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return ddata;
    }

    @Specialization
    public RDoubleVector doIntVector(RIntVector operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization
    public RDoubleVector doIntVector(RIntSequence operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization(order = 101, guards = "preserveDimensions")
    public RDoubleVector doLogicalVectorDims(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
    }

    @Specialization(order = 102, guards = "preserveNames")
    public RDoubleVector doLogicalVectorNames(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), operand.getNames());
    }

    @Specialization(order = 103)
    public RDoubleVector doLogicalVector(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA());
    }

    @Specialization(order = 104, guards = "preserveDimensions")
    public RDoubleVector doStringVectorDims(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
    }

    @Specialization(order = 105, guards = "preserveNames")
    public RDoubleVector doStringVectorNames(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), operand.getNames());
    }

    @Specialization(order = 106)
    public RDoubleVector doStringVector(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA());
    }

    @Specialization(order = 107, guards = "preserveDimensions")
    public RDoubleVector doComplexVectorDims(RComplexVector operand) {
        double[] ddata = dataFromComplex(operand);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
    }

    @Specialization(order = 108, guards = "preserveNames")
    public RDoubleVector doComplexVectorNames(RComplexVector operand) {
        double[] ddata = dataFromComplex(operand);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), operand.getNames());
    }

    @Specialization(order = 109)
    public RDoubleVector doComplexVector(RComplexVector operand) {
        double[] ddata = dataFromComplex(operand);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA());
    }

    @Specialization(order = 110, guards = "preserveDimensions")
    public RDoubleVector doRawVectorDims(RRawVector vector) {
        double[] ddata = dataFromRaw(vector);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 111, guards = "preserveNames")
    public RDoubleVector doRawVectorNames(RRawVector vector) {
        double[] ddata = dataFromRaw(vector);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 112)
    public RDoubleVector doRawVector(RRawVector vector) {
        double[] ddata = dataFromRaw(vector);
        return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA());
    }

    @Specialization
    public RDoubleVector doDoubleVector(RDoubleVector operand) {
        return operand;
    }

    @Specialization
    public RDoubleSequence doDoubleSequence(RDoubleSequence operand) {
        return operand;
    }

    @Generic
    public double doOther(Object operand) {
        CompilerDirectives.transferToInterpreter();
        throw new ConversionFailedException(operand.getClass().getName());
    }

    private RDoubleVector performAbstractIntVector(RAbstractIntVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            int value = operand.getDataAt(i);
            ddata[i] = naCheck.convertIntToDouble(value);
        }
        if (preserveDimensions()) {
            return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        } else if (preserveNames()) {
            return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        } else {
            return RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA());
        }
    }
}
