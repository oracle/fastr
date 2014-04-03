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

public abstract class CastComplexNode extends CastNode {

    private final NACheck naCheck = NACheck.create();

    public abstract Object executeComplex(VirtualFrame frame, int o);

    public abstract Object executeComplex(VirtualFrame frame, double o);

    public abstract Object executeComplex(VirtualFrame frame, byte o);

    public abstract Object executeComplex(VirtualFrame frame, Object o);

    public abstract Object executeComplexVector(VirtualFrame frame, Object o);

    @Specialization
    public RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization(order = 1)
    public RComplex doInt(int operand) {
        naCheck.enable(operand);
        return naCheck.convertIntToComplex(operand);
    }

    @Specialization(order = 2)
    public RComplex doDouble(double operand) {
        naCheck.enable(operand);
        return naCheck.convertDoubleToComplex(operand);
    }

    @Specialization(order = 3)
    public RComplex doLogical(byte operand) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToComplex(operand);
    }

    @Specialization(order = 4)
    public RComplex doComplex(RComplex operand) {
        return operand;
    }

    @Specialization(order = 5)
    public RComplex doRaw(RRaw operand) {
        return RDataFactory.createComplex(operand.getValue(), 0);
    }

    @Specialization(order = 6)
    public RComplex doCharacter(String operand) {
        naCheck.enable(operand);
        RComplex result = naCheck.convertStringToComplex(operand);
        if (RRuntime.isNA(result)) {
            RContext.getInstance().setEvalWarning(RError.NA_INTRODUCED_COERCION);
        }
        return result;
    }

    private double[] dataFromLogical(RLogicalVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength() << 1];
        for (int i = 0; i < operand.getLength(); i++) {
            byte value = operand.getDataAt(i);
            RComplex complexValue = naCheck.convertLogicalToComplex(value);
            int index = i << 1;
            ddata[index] = complexValue.getRealPart();
            ddata[index + 1] = complexValue.getImaginaryPart();
        }
        return ddata;
    }

    private double[] dataFromString(RStringVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength() << 1];
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            RComplex complexValue = naCheck.convertStringToComplex(value);
            if (RRuntime.isNA(complexValue)) {
                RContext.getInstance().setEvalWarning(RError.NA_INTRODUCED_COERCION);
            }
            int index = i << 1;
            ddata[index] = complexValue.getRealPart();
            ddata[index + 1] = complexValue.getImaginaryPart();
        }
        return ddata;
    }

    private static double[] dataFromRaw(RRawVector operand) {
        double[] ddata = new double[operand.getLength() << 1];
        for (int i = 0; i < operand.getLength(); i++) {
            byte value = operand.getDataAt(i).getValue();
            int index = i << 1;
            ddata[index] = value;
            ddata[index + 1] = 0;
        }
        return ddata;
    }

    @Specialization
    public RComplexVector doIntVector(RIntVector operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization
    public RComplexVector doIntSequence(RIntSequence operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization(order = 101, guards = {"!preserveNames", "preserveDimensions"})
    public RComplexVector doLogicalVectorDims(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
    }

    @Specialization(order = 102, guards = {"preserveNames", "!preserveDimensions"})
    public RComplexVector doLogicalVectorNames(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getNames());
    }

    @Specialization(order = 103, guards = {"preserveNames", "preserveDimensions"})
    public RComplexVector doLogicalVectorDimsNames(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
    }

    @Specialization(order = 104)
    public RComplexVector doLogicalVector(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA());
    }

    @Specialization(order = 105, guards = {"!preserveNames", "preserveDimensions"})
    public RComplexVector doStringVectorDims(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
    }

    @Specialization(order = 106, guards = {"preserveNames", "!preserveDimensions"})
    public RComplexVector doStringVectorNames(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getNames());
    }

    @Specialization(order = 107, guards = {"preserveNames", "preserveDimensions"})
    public RComplexVector doStringVectorDimsNames(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
    }

    @Specialization(order = 108)
    public RComplexVector doStringVector(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA());
    }

    @Specialization
    public RComplexVector doDoubleVector(RDoubleVector operand) {
        return performAbstractDoubleVector(operand);
    }

    @Specialization
    public RComplexVector doDoubleSequence(RDoubleSequence operand) {
        return performAbstractDoubleVector(operand);
    }

    @Specialization
    public RComplexVector doComplexVector(RComplexVector vector) {
        return vector;
    }

    @Specialization(order = 109, guards = {"!preserveNames", "preserveDimensions"})
    public RComplexVector doRawVectorDims(RRawVector operand) {
        double[] ddata = dataFromRaw(operand);
        return RDataFactory.createComplexVector(ddata, RDataFactory.COMPLETE_VECTOR, operand.getDimensions());
    }

    @Specialization(order = 110, guards = {"preserveNames", "!preserveDimensions"})
    public RComplexVector doRawVectorNames(RRawVector operand) {
        double[] ddata = dataFromRaw(operand);
        return RDataFactory.createComplexVector(ddata, RDataFactory.COMPLETE_VECTOR, operand.getNames());
    }

    @Specialization(order = 111, guards = {"preserveNames", "preserveDimensions"})
    public RComplexVector doRawVectorDimsNames(RRawVector operand) {
        double[] ddata = dataFromRaw(operand);
        return RDataFactory.createComplexVector(ddata, RDataFactory.COMPLETE_VECTOR, operand.getDimensions(), operand.getNames());
    }

    @Specialization(order = 112)
    public RComplexVector doRawVector(RRawVector operand) {
        double[] ddata = dataFromRaw(operand);
        return RDataFactory.createComplexVector(ddata, RDataFactory.COMPLETE_VECTOR);
    }

    @Generic
    public int doOther(Object operand) {
        CompilerDirectives.transferToInterpreter();
        throw new ConversionFailedException(operand.getClass().getName());
    }

    private RComplexVector performAbstractIntVector(RAbstractIntVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength() << 1];
        for (int i = 0; i < operand.getLength(); i++) {
            int value = operand.getDataAt(i);
            RComplex complexValue = naCheck.convertIntToComplex(value);
            int index = i << 1;
            ddata[index] = complexValue.getRealPart();
            ddata[index + 1] = complexValue.getImaginaryPart();
        }
        if (preserveDimensions() && preserveNames()) {
            return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        } else if (preserveDimensions()) {
            return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        } else if (preserveNames()) {
            return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        } else {
            return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA());
        }
    }

    private RComplexVector performAbstractDoubleVector(RAbstractDoubleVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength() << 1];
        for (int i = 0; i < operand.getLength(); i++) {
            double value = operand.getDataAt(i);
            RComplex complexValue = naCheck.convertDoubleToComplex(value);
            int index = i << 1;
            ddata[index] = complexValue.getRealPart();
            ddata[index + 1] = complexValue.getImaginaryPart();
        }
        if (preserveDimensions() && preserveNames()) {
            return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        } else if (preserveDimensions()) {
            return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        } else if (preserveNames()) {
            return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        } else {
            return RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA());
        }
    }

}
