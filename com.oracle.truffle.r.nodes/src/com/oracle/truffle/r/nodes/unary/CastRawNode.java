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

public abstract class CastRawNode extends CastNode {

    public abstract Object executeRaw(VirtualFrame frame, int o);

    public abstract Object executeRaw(VirtualFrame frame, double o);

    public abstract Object executeRaw(VirtualFrame frame, byte o);

    public abstract Object executeRaw(VirtualFrame frame, Object o);

    @Specialization
    public RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    public RRaw doInt(int operand) {
        int intResult = RRuntime.int2rawIntValue(operand);
        if (intResult != operand) {
            RContext.getInstance().setEvalWarning(RError.OUT_OF_RANGE);
        }
        return RDataFactory.createRaw((byte) intResult);
    }

    @Specialization
    public RRaw doDouble(double operand) {
        int intResult = RRuntime.double2rawIntValue(operand);
        if (intResult != (int) operand) {
            RContext.getInstance().setEvalWarning(RError.OUT_OF_RANGE);
        }
        return RDataFactory.createRaw((byte) intResult);
    }

    @Specialization
    public RRaw doComplex(RComplex operand) {
        int intResult = RRuntime.complex2rawIntValue(operand);
        if (operand.getImaginaryPart() != 0) {
            RContext.getInstance().setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        if (intResult != (int) operand.getRealPart()) {
            RContext.getInstance().setEvalWarning(RError.OUT_OF_RANGE);
        }
        return RDataFactory.createRaw((byte) intResult);
    }

    @Specialization
    public RRaw doRaw(RRaw operand) {
        return operand;
    }

    @Specialization
    public RRaw doLogical(byte operand) {
        // need to convert to int so that NA-related warning is caught
        int intVal = RRuntime.logical2int(operand);
        return doInt(intVal);
    }

    @Specialization
    public RRaw doString(String operand) {
        // need to cast to int to catch conversion warnings
        int intVal = RRuntime.string2int(operand);
        if (RRuntime.isNA(intVal)) {
            RContext.getInstance().setEvalWarning(RError.NA_INTRODUCED_COERCION);
        }
        return doInt(intVal);
    }

    private static byte[] dataFromComplex(RComplexVector operand) {
        byte[] bdata = new byte[operand.getLength()];
        boolean imaginaryDiscardedWarning = false;
        boolean outOfRangeWarning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex complexVal = operand.getDataAt(i);
            int intRawValue = RRuntime.complex2rawIntValue(complexVal);
            if (complexVal.getImaginaryPart() != 0.0) {
                imaginaryDiscardedWarning = true;
            }
            if ((int) complexVal.getRealPart() != intRawValue) {
                outOfRangeWarning = true;
            }
            bdata[i] = (byte) intRawValue;
        }
        if (imaginaryDiscardedWarning) {
            RContext.getInstance().setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        if (outOfRangeWarning) {
            RContext.getInstance().setEvalWarning(RError.OUT_OF_RANGE);
        }
        return bdata;
    }

    private static byte[] dataFromLogical(RLogicalVector operand) {
        byte[] bdata = new byte[operand.getLength()];
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            int intVal = RRuntime.logical2int(operand.getDataAt(i));
            int intRawValue = RRuntime.int2rawIntValue(intVal);
            if (intVal != intRawValue) {
                warning = true;
            }
            bdata[i] = (byte) intRawValue;
        }
        if (warning) {
            RContext.getInstance().setEvalWarning(RError.OUT_OF_RANGE);
        }
        return bdata;
    }

    private static byte[] dataFromString(RStringVector operand) {
        byte[] bdata = new byte[operand.getLength()];
        boolean naCoercionWarning = false;
        boolean outOfRangeWarning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            int intVal = RRuntime.string2int(operand.getDataAt(i));
            int intRawValue = RRuntime.int2rawIntValue(intVal);
            if (RRuntime.isNA(intVal)) {
                naCoercionWarning = true;
            }
            if (intVal != intRawValue) {
                outOfRangeWarning = true;
            }
            bdata[i] = (byte) intRawValue;
        }
        if (naCoercionWarning) {
            RContext.getInstance().setEvalWarning(RError.NA_INTRODUCED_COERCION);
        }
        if (outOfRangeWarning) {
            RContext.getInstance().setEvalWarning(RError.OUT_OF_RANGE);
        }
        return bdata;
    }

    @Specialization
    public RRawVector doIntVector(RIntVector value) {
        return performAbstractIntVector(value);
    }

    @Specialization
    public RRawVector doIntSequence(RIntSequence value) {
        return performAbstractIntVector(value);
    }

    @Specialization(order = 101, guards = {"!preserveNames", "preserveDimensions"})
    public RRawVector doLogicalVectorDims(RLogicalVector operand) {
        byte[] bdata = dataFromLogical(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata, operand.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 102, guards = {"preserveNames", "!preserveDimensions"})
    public RRawVector doLogicalVectorNames(RLogicalVector operand) {
        byte[] bdata = dataFromLogical(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata, operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 103, guards = {"preserveNames", "preserveDimensions"})
    public RRawVector doLogicalVectorDimsNames(RLogicalVector operand) {
        byte[] bdata = dataFromLogical(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata, operand.getDimensions(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 104, guards = {"!preserveNames", "!preserveDimensions"})
    public RRawVector doLogicalVector(RLogicalVector operand) {
        byte[] bdata = dataFromLogical(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 105, guards = {"!preserveNames", "preserveDimensions"})
    public RRawVector doStringVectorDims(RStringVector operand) {
        byte[] bdata = dataFromString(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata, operand.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 106, guards = {"preserveNames", "!preserveDimensions"})
    public RRawVector doStringVectorNames(RStringVector operand) {
        byte[] bdata = dataFromString(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata, operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 107, guards = {"preserveNames", "preserveDimensions"})
    public RRawVector doStringVectorDimsNames(RStringVector operand) {
        byte[] bdata = dataFromString(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata, operand.getDimensions(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 108, guards = {"!preserveNames", "!preserveDimensions"})
    public RRawVector doStringVector(RStringVector operand) {
        byte[] bdata = dataFromString(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 109, guards = {"!preserveNames", "preserveDimensions"})
    public RRawVector doRawVectorDims(RComplexVector operand) {
        byte[] bdata = dataFromComplex(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata, operand.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 110, guards = {"preserveNames", "!preserveDimensions"})
    public RRawVector doComplexVectorNames(RComplexVector operand) {
        byte[] bdata = dataFromComplex(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata, operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 111, guards = {"preserveNames", "preserveDimensions"})
    public RRawVector doComplexVectorDimsNames(RComplexVector operand) {
        byte[] bdata = dataFromComplex(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata, operand.getDimensions(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(order = 112, guards = {"!preserveNames", "!preserveDimensions"})
    public RRawVector doComplexVector(RComplexVector operand) {
        byte[] bdata = dataFromComplex(operand);
        RRawVector ret = RDataFactory.createRawVector(bdata);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    public RRawVector doDoubleVector(RDoubleVector value) {
        return performAbstractDoubleVector(value);
    }

    @Specialization
    public RRawVector doDoubleSequence(RDoubleSequence value) {
        return performAbstractDoubleVector(value);
    }

    @Specialization
    public RRawVector doRawVector(RRawVector operand) {
        return operand;
    }

    @Generic
    public int doOther(Object operand) {
        CompilerDirectives.transferToInterpreter();
        throw new ConversionFailedException(operand.getClass().getName());
    }

    private RRawVector performAbstractIntVector(RAbstractIntVector value) {
        int length = value.getLength();
        byte[] array = new byte[length];
        boolean warning = false;
        for (int i = 0; i < length; ++i) {
            int intValue = value.getDataAt(i);
            int intRawValue = RRuntime.int2rawIntValue(intValue);
            if (intRawValue != intValue) {
                warning = true;
            }
            array[i] = (byte) intRawValue;
        }
        if (warning) {
            RContext.getInstance().setEvalWarning(RError.OUT_OF_RANGE);
        }
        RRawVector ret;
        if (preserveDimensions() && preserveNames()) {
            ret = RDataFactory.createRawVector(array, value.getDimensions(), value.getNames());
        } else if (preserveDimensions()) {
            ret = RDataFactory.createRawVector(array, value.getDimensions());
        } else if (preserveNames()) {
            ret = RDataFactory.createRawVector(array, value.getNames());
        } else {
            ret = RDataFactory.createRawVector(array);
        }
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(value);
        }
        return ret;
    }

    private RRawVector performAbstractDoubleVector(RAbstractDoubleVector value) {
        int length = value.getLength();
        byte[] array = new byte[length];
        boolean warning = false;
        for (int i = 0; i < length; ++i) {
            double doubleValue = value.getDataAt(i);
            int intRawValue = RRuntime.double2rawIntValue(doubleValue);
            if (intRawValue != (int) doubleValue) {
                warning = true;
            }
            array[i] = (byte) intRawValue;
        }
        if (warning) {
            RContext.getInstance().setEvalWarning(RError.OUT_OF_RANGE);
        }
        RRawVector ret;
        if (preserveDimensions() && preserveNames()) {
            ret = RDataFactory.createRawVector(array, value.getDimensions(), value.getNames());
        } else if (preserveDimensions()) {
            ret = RDataFactory.createRawVector(array, value.getDimensions());
        } else if (preserveNames()) {
            ret = RDataFactory.createRawVector(array, value.getNames());
        } else {
            ret = RDataFactory.createRawVector(array);
        }
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(value);
        }
        return ret;
    }
}
