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
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.ConversionFailedException;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class CastIntegerNode extends CastNode {

    private final NACheck check = NACheck.create();

    public abstract Object executeInt(VirtualFrame frame, int o);

    public abstract Object executeInt(VirtualFrame frame, double o);

    public abstract Object executeInt(VirtualFrame frame, byte o);

    public abstract Object executeInt(VirtualFrame frame, Object o);

    @Child private CastIntegerNode recursiveCastInteger;

    private Object castIntegerRecursive(VirtualFrame frame, Object o) {
        if (recursiveCastInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastInteger = insert(CastIntegerNodeFactory.create(null, isNamesPreservation(), isDimensionsPreservation(), isAttrPreservation()));
        }
        return recursiveCastInteger.executeInt(frame, o);
    }

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected RMissing doMissing(RMissing operand) {
        return operand;
    }

    @Specialization
    protected int doInt(int operand) {
        return operand;
    }

    @Specialization
    protected int doDouble(double operand) {
        check.enable(operand);
        return check.convertDoubleToInt(operand);
    }

    @Specialization
    protected RIntVector doIntVector(RIntVector operand) {
        return operand;
    }

    @Specialization
    protected RIntSequence doIntSequence(RIntSequence operand) {
        return operand;
    }

    @Specialization
    protected RIntSequence doDoubleSequence(RDoubleSequence operand) {
        check.enable(operand);
        return RDataFactory.createIntSequence(check.convertDoubleToInt(operand.getStart()), check.convertDoubleToInt(operand.getStride()), operand.getLength());
    }

    @Specialization
    protected int doComplex(RComplex operand) {
        check.enable(operand);
        int result = check.convertComplexToInt(operand);
        if (operand.getImaginaryPart() != 0.0) {
            RError.warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return result;
    }

    @Specialization
    protected int doCharacter(String operand) {
        check.enable(operand);
        int result = check.convertStringToInt(operand);
        if (isNA(result)) {
            RError.warning(RError.Message.NA_INTRODUCED_COERCION);
        }
        return result;
    }

    @Specialization
    protected int doBoolean(byte operand) {
        check.enable(operand);
        return check.convertLogicalToInt(operand);
    }

    @Specialization
    protected int doRaw(RRaw operand) {
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
            RError.warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
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
            RError.warning(RError.Message.NA_INTRODUCED_COERCION);
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

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RIntVector doComplexVectorDims(RComplexVector vector) {
        int[] result = dataFromComplex(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RIntVector doComplexVectorNames(RComplexVector vector) {
        int[] result = dataFromComplex(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RIntVector doComplexVectorDimsNames(RComplexVector vector) {
        int[] result = dataFromComplex(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RIntVector doComplexVector(RComplexVector vector) {
        int[] result = dataFromComplex(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RIntVector doStringVectorDims(RStringVector vector) {
        int[] result = dataFromString(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RIntVector doStringVectorNames(RStringVector vector) {
        int[] result = dataFromString(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RIntVector doStringVectorDimsNames(RStringVector vector) {
        int[] result = dataFromString(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RIntVector doStringVector(RStringVector vector) {
        int[] result = dataFromString(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RIntVector doLogicalVectorDims(RLogicalVector vector) {
        int[] result = dataFromLogical(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RIntVector doLogicalVectorNames(RLogicalVector vector) {
        int[] result = dataFromLogical(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RIntVector doLogicalVectorDimsNames(RLogicalVector vector) {
        int[] result = dataFromLogical(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    public RIntVector doLogicalVector(RLogicalVector vector) {
        int[] result = dataFromLogical(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RIntVector doDoubleVectorDims(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RIntVector doDoubleVectorNames(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RIntVector doDoubleVectorDimsNames(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RIntVector doDoubleVector(RDoubleVector vector) {
        check.enable(vector);
        int[] result = check.convertDoubleVectorToIntData(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RIntVector doRawVectorDims(RRawVector vector) {
        int[] result = dataFromRaw(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RIntVector doRawVectorNames(RRawVector vector) {
        int[] result = dataFromRaw(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RIntVector doRawVectorDimsNames(RRawVector vector) {
        int[] result = dataFromRaw(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA(), vector.getDimensions(), vector.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RIntVector doRawVector(RRawVector vector) {
        int[] result = dataFromRaw(vector);
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(vector);
        }
        return ret;
    }

    @Specialization
    protected RIntVector doList(VirtualFrame frame, RList list) {
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
                        throw cannotCoerceListError();
                    }
                } else {
                    throw cannotCoerceListError();
                }
            }
        }
        RIntVector ret = RDataFactory.createIntVector(result, check.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization
    protected RIntVector doFactor(RFactor factor) {
        return factor.getVector();
    }

    private RError cannotCoerceListError() {
        throw RError.error(this.getSourceSection(), RError.Message.LIST_COERCION, "integer");
    }

    @Fallback
    @TruffleBoundary
    public int doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }

}
