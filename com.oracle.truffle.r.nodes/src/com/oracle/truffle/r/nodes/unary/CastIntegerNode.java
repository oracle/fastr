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
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class CastIntegerNode extends CastNode {

    private final NACheck check = NACheck.create();

    public abstract Object executeInt(VirtualFrame frame, int o);

    public abstract Object executeInt(VirtualFrame frame, double o);

    public abstract Object executeInt(VirtualFrame frame, byte o);

    public abstract Object executeInt(VirtualFrame frame, Object o);

    public abstract Object executeIntVector(VirtualFrame frame, Object o);

    @Child CastIntegerNode recursiveCastInteger;

    private Object castIntegerRecursive(VirtualFrame frame, Object o) {
        if (recursiveCastInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastInteger = insert(CastIntegerNodeFactory.create(null, isNamesPreservation(), isDimensionsPreservation()));
        }
        return recursiveCastInteger.executeInt(frame, o);
    }

    @Specialization
    public RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    public RMissing doMissing(RMissing operand) {
        return operand;
    }

    @Specialization
    public int doInt(int operand) {
        return operand;
    }

    @Specialization
    public int doDouble(double operand) {
        check.enable(operand);
        return check.convertDoubleToInt(operand);
    }

    @Specialization
    public RIntVector doIntVector(RIntVector operand) {
        return operand;
    }

    @Specialization
    public RIntSequence doIntSequence(RIntSequence operand) {
        return operand;
    }

    @Specialization
    public RIntSequence doDoubleSequence(RDoubleSequence operand) {
        check.enable(operand);
        return RDataFactory.createIntSequence(check.convertDoubleToInt(operand.getStart()), check.convertDoubleToInt(operand.getStride()), operand.getLength());
    }

    @Specialization
    public int doComplex(RComplex operand) {
        check.enable(operand);
        int result = check.convertComplexToInt(operand);
        if (operand.getImaginaryPart() != 0.0) {
            RContext.getInstance().setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return result;
    }

    @Specialization
    public int doCharacter(String operand) {
        check.enable(operand);
        int result = check.convertStringToInt(operand);
        if (isNA(result)) {
            RContext.getInstance().setEvalWarning(RError.NA_INTRODUCED_COERCION);
        }
        return result;
    }

    @Specialization
    public int doBoolean(byte operand) {
        check.enable(operand);
        return check.convertLogicalToInt(operand);
    }

    @Specialization
    public int doRaw(RRaw operand) {
        return RRuntime.raw2int(operand);
    }

    private int[] dataFromComplex(RComplexVector operand) {
        check.enable(operand);
        int length = operand.getLength();
        int[] idata = new int[length];
        boolean warning = false;
        for (int i = 0; i < length; i++) {
            RComplex data = operand.getDataAt(i);
            idata[i] = check.convertComplexToInt(data, false);
            if (data.getImaginaryPart() != 0.0) {
                warning = true;
            }
        }
        if (warning) {
            RContext.getInstance().setEvalWarning(RError.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return idata;
    }

    private int[] dataFromString(RStringVector operand) {
        check.enable(operand);
        int[] idata = new int[operand.getLength()];
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            idata[i] = check.convertStringToInt(value);
            if (RRuntime.isNA(idata[i])) {
                warning = true;
            }
        }
        if (warning) {
            RContext.getInstance().setEvalWarning(RError.NA_INTRODUCED_COERCION);
        }
        return idata;
    }

    private int[] dataFromLogical(RLogicalVector operand) {
        check.enable(operand);
        int[] idata = new int[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            byte value = operand.getDataAt(i);
            idata[i] = check.convertLogicalToInt(value);
        }
        return idata;
    }

    private static int[] dataFromRaw(RRawVector operand) {
        int[] idata = new int[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RRaw value = operand.getDataAt(i);
            idata[i] = RRuntime.raw2int(value);
        }
        return idata;
    }

    @Specialization(order = 101, guards = {"!preserveNames", "preserveDimensions"})
    public RIntVector doComplexVectorDims(RComplexVector vector) {
        int[] result = dataFromComplex(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 102, guards = {"preserveNames", "!preserveDimensions"})
    public RIntVector doComplexVectorNames(RComplexVector vector) {
        int[] result = dataFromComplex(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 103, guards = {"preserveNames", "preserveDimensions"})
    public RIntVector doComplexVectorDimsNames(RComplexVector vector) {
        int[] result = dataFromComplex(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
    }

    @Specialization(order = 104)
    public RIntVector doComplexVector(RComplexVector vector) {
        int[] result = dataFromComplex(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization(order = 105, guards = {"!preserveNames", "preserveDimensions"})
    public RIntVector doStringVectorDims(RStringVector vector) {
        int[] result = dataFromString(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 106, guards = {"preserveNames", "!preserveDimensions"})
    public RIntVector doStringVectorNames(RStringVector vector) {
        int[] result = dataFromString(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 107, guards = {"preserveNames", "preserveDimensions"})
    public RIntVector doStringVectorDimsNames(RStringVector vector) {
        int[] result = dataFromString(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
    }

    @Specialization(order = 108)
    public RIntVector doStringVector(RStringVector vector) {
        int[] result = dataFromString(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization(order = 109, guards = {"!preserveNames", "preserveDimensions"})
    public RIntVector doLogicalVectorDims(RLogicalVector vector) {
        int[] result = dataFromLogical(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 110, guards = {"preserveNames", "!preserveDimensions"})
    public RIntVector doLogicalVectorNames(RLogicalVector vector) {
        int[] result = dataFromLogical(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 111, guards = {"preserveNames", "preserveDimensions"})
    public RIntVector doLogicalVectorDimsNames(RLogicalVector vector) {
        int[] result = dataFromLogical(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
    }

    @Specialization(order = 112)
    public RIntVector doLogicalVector(RLogicalVector vector) {
        int[] result = dataFromLogical(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization(order = 113, guards = {"!preserveNames", "preserveDimensions"})
    public RIntVector doDoubleVectorDims(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 114, guards = {"preserveNames", "!preserveDimensions"})
    public RIntVector doDoubleVectorNames(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 115, guards = {"preserveNames", "preserveDimensions"})
    public RIntVector doDoubleVectorDimsNames(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
    }

    @Specialization(order = 116)
    public RIntVector doDoubleVector(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization(order = 117, guards = {"!preserveNames", "preserveDimensions"})
    public RIntVector doRawVectorDims(RRawVector vector) {
        int[] result = dataFromRaw(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
    }

    @Specialization(order = 118, guards = {"preserveNames", "!preserveDimensions"})
    public RIntVector doRawVectorNames(RRawVector vector) {
        int[] result = dataFromRaw(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
    }

    @Specialization(order = 119, guards = {"preserveNames", "preserveDimensions"})
    public RIntVector doRawVectorDimsNames(RRawVector vector) {
        int[] result = dataFromRaw(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
    }

    @Specialization(order = 120)
    public RIntVector doRawVector(RRawVector vector) {
        int[] result = dataFromRaw(vector);
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization
    public RIntVector doList(VirtualFrame frame, RList list) {
        int length = list.getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            Object entry = list.getDataAt(i);
            if (entry instanceof RList) {
                result[i] = RRuntime.INT_NA;
            } else {
                Object castEntry = castIntegerRecursive(frame, entry);
                if (castEntry instanceof Integer) {
                    result[i] = (Integer) castEntry;
                } else if (castEntry instanceof RIntVector) {
                    RIntVector intVector = (RIntVector) castEntry;
                    if (intVector.getLength() == 1) {
                        result[i] = intVector.getDataAt(0);
                    } else if (intVector.getLength() == 0) {
                        result[i] = RRuntime.INT_NA;
                    } else {
                        return cannotCoerceListError();
                    }
                } else {
                    return cannotCoerceListError();
                }
            }
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    private RIntVector cannotCoerceListError() {
        throw RError.getListCoercion(this.getSourceSection(), "integer");
    }

    @Generic
    public int doOther(Object operand) {
        CompilerDirectives.transferToInterpreter();
        throw new ConversionFailedException(operand.getClass().getName());
    }

}
