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
import com.oracle.truffle.r.nodes.unary.ConvertNode.ConversionFailedException;
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

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected RComplex doInt(int operand) {
        naCheck.enable(operand);
        return naCheck.convertIntToComplex(operand);
    }

    @Specialization
    protected RComplex doDouble(double operand) {
        naCheck.enable(operand);
        return naCheck.convertDoubleToComplex(operand);
    }

    @Specialization
    protected RComplex doLogical(byte operand) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToComplex(operand);
    }

    @Specialization
    protected RComplex doComplex(RComplex operand) {
        return operand;
    }

    @Specialization
    protected RComplex doRaw(RRaw operand) {
        return RDataFactory.createComplex(operand.getValue(), 0);
    }

    @Specialization
    protected RComplex doCharacter(String operand) {
        naCheck.enable(operand);
        RComplex result = naCheck.convertStringToComplex(operand);
        if (RRuntime.isNA(result)) {
            RError.warning(RError.Message.NA_INTRODUCED_COERCION);
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
                RError.warning(RError.Message.NA_INTRODUCED_COERCION);
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
    protected RComplexVector doIntVector(RIntVector operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization
    protected RComplexVector doIntSequence(RIntSequence operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RComplexVector doLogicalVectorDims(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RComplexVector doLogicalVectorNames(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RComplexVector doLogicalVectorDimsNames(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RComplexVector doLogicalVector(RLogicalVector operand) {
        double[] ddata = dataFromLogical(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RComplexVector doStringVectorDims(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RComplexVector doStringVectorNames(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RComplexVector doStringVectorDimsNames(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RComplexVector doStringVector(RStringVector operand) {
        double[] ddata = dataFromString(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RComplexVector doDoubleVector(RDoubleVector operand) {
        return performAbstractDoubleVector(operand);
    }

    @Specialization
    protected RComplexVector doDoubleSequence(RDoubleSequence operand) {
        return performAbstractDoubleVector(operand);
    }

    @Specialization
    protected RComplexVector doComplexVector(RComplexVector vector) {
        return vector;
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RComplexVector doRawVectorDims(RRawVector operand) {
        double[] ddata = dataFromRaw(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, RDataFactory.COMPLETE_VECTOR, operand.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RComplexVector doRawVectorNames(RRawVector operand) {
        double[] ddata = dataFromRaw(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, RDataFactory.COMPLETE_VECTOR, operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RComplexVector doRawVectorDimsNames(RRawVector operand) {
        double[] ddata = dataFromRaw(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, RDataFactory.COMPLETE_VECTOR, operand.getDimensions(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RComplexVector doRawVector(RRawVector operand) {
        double[] ddata = dataFromRaw(operand);
        RComplexVector ret = RDataFactory.createComplexVector(ddata, RDataFactory.COMPLETE_VECTOR);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Fallback
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
        RComplexVector ret;
        if (preserveDimensions() && preserveNames()) {
            ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        } else if (preserveDimensions()) {
            ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        } else if (preserveNames()) {
            ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        } else {
            ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA());
        }
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
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
        RComplexVector ret;
        if (preserveDimensions() && preserveNames()) {
            ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        } else if (preserveDimensions()) {
            ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        } else if (preserveNames()) {
            ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        } else {
            ret = RDataFactory.createComplexVector(ddata, naCheck.neverSeenNA());
        }
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

}
