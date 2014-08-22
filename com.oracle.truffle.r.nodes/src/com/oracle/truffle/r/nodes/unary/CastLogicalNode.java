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
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

public abstract class CastLogicalNode extends CastNode {

    private final NACheck naCheck = NACheck.create();

    public abstract Object executeByte(VirtualFrame frame, Object o);

    public abstract Object executeLogical(VirtualFrame frame, Object o);

    @Child private CastLogicalNode recursiveCastLogical;

    private Object castLogicalRecursive(VirtualFrame frame, Object o) {
        if (recursiveCastLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastLogical = insert(CastLogicalNodeFactory.create(null, isNamesPreservation(), isDimensionsPreservation(), isAttrPreservation()));
        }
        return recursiveCastLogical.executeLogical(frame, o);
    }

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected byte doLogical(byte operand) {
        return operand;
    }

    @Specialization
    protected byte doDouble(double operand) {
        naCheck.enable(operand);
        return naCheck.convertDoubleToLogical(operand);
    }

    @Specialization
    protected byte doInt(int operand) {
        naCheck.enable(operand);
        return naCheck.convertIntToLogical(operand);
    }

    @Specialization
    protected byte doComplex(RComplex operand) {
        naCheck.enable(operand);
        return naCheck.convertComplexToLogical(operand);
    }

    @Specialization
    protected byte doString(String operand) {
        naCheck.enable(operand);
        return naCheck.convertStringToLogical(operand);
    }

    @Specialization
    protected byte doRaw(RRaw operand) {
        return RRuntime.raw2logical(operand);
    }

    private byte[] dataFromString(RStringVector operand) {
        naCheck.enable(operand);
        byte[] ldata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            ldata[i] = naCheck.convertStringToLogical(value);
        }
        return ldata;
    }

    private byte[] dataFromComplex(RComplexVector operand) {
        naCheck.enable(operand);
        byte[] ldata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ldata[i] = naCheck.convertComplexToLogical(value);
        }
        return ldata;
    }

    private static byte[] dataFromRaw(RRawVector operand) {
        byte[] ldata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RRaw value = operand.getDataAt(i);
            ldata[i] = RRuntime.raw2logical(value);
        }
        return ldata;
    }

    @Specialization
    protected RLogicalVector doLogicalVector(RLogicalVector operand) {
        return operand;
    }

    @Specialization
    protected RLogicalVector doIntVector(RIntVector operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization
    protected RLogicalVector doIntSequence(RIntSequence operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization
    protected RLogicalVector doDoubleVector(RDoubleVector operand) {
        return performAbstractDoubleVector(operand);
    }

    @Specialization
    protected RLogicalVector doDoubleSequence(RDoubleSequence operand) {
        return performAbstractDoubleVector(operand);
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RLogicalVector doStringVectorDims(RStringVector operand) {
        byte[] ldata = dataFromString(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, naCheck.neverSeenNA(), operand.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RLogicalVector doStringVectorNames(RStringVector operand) {
        byte[] ldata = dataFromString(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, naCheck.neverSeenNA(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RLogicalVector doStringVectorDimsNames(RStringVector operand) {
        byte[] ldata = dataFromString(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RLogicalVector doStringVector(RStringVector operand) {
        byte[] ldata = dataFromString(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, naCheck.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RLogicalVector doComplexVectorDims(RComplexVector operand) {
        byte[] ldata = dataFromComplex(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, naCheck.neverSeenNA(), operand.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RLogicalVector doComplexVectorNames(RComplexVector operand) {
        byte[] ldata = dataFromComplex(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, naCheck.neverSeenNA(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RLogicalVector doComplexVectorDimsNames(RComplexVector operand) {
        byte[] ldata = dataFromComplex(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RLogicalVector doComplexVector(RComplexVector operand) {
        byte[] ldata = dataFromComplex(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, naCheck.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "preserveDimensions"})
    protected RLogicalVector doRawVectorDims(RRawVector operand) {
        byte[] ldata = dataFromRaw(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, RDataFactory.COMPLETE_VECTOR, operand.getDimensions());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "!preserveDimensions"})
    protected RLogicalVector doRawVectorNames(RRawVector operand) {
        byte[] ldata = dataFromRaw(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, RDataFactory.COMPLETE_VECTOR, operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"preserveNames", "preserveDimensions"})
    protected RLogicalVector doRawVectorDimsNames(RRawVector operand) {
        byte[] ldata = dataFromRaw(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, RDataFactory.COMPLETE_VECTOR, operand.getDimensions(), operand.getNames());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization(guards = {"!preserveNames", "!preserveDimensions"})
    protected RLogicalVector doRawVector(RRawVector operand) {
        byte[] ldata = dataFromRaw(operand);
        RLogicalVector ret = RDataFactory.createLogicalVector(ldata, RDataFactory.COMPLETE_VECTOR);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RLogicalVector doList(VirtualFrame frame, RList list) {
        int length = list.getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            Object entry = list.getDataAt(i);
            if (entry instanceof RList) {
                result[i] = RRuntime.LOGICAL_NA;
            } else {
                Object castEntry = castLogicalRecursive(frame, entry);
                if (castEntry instanceof Byte) {
                    result[i] = (Byte) castEntry;
                } else if (castEntry instanceof RLogicalVector) {
                    RLogicalVector logicalVector = (RLogicalVector) castEntry;
                    if (logicalVector.getLength() == 1) {
                        result[i] = logicalVector.getDataAt(0);
                    } else if (logicalVector.getLength() == 0) {
                        result[i] = RRuntime.LOGICAL_NA;
                    } else {
                        throw cannotCoerceListError();
                    }
                } else {
                    throw cannotCoerceListError();
                }
            }
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, naCheck.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    private RError cannotCoerceListError() {
        throw RError.error(this.getSourceSection(), RError.Message.LIST_COERCION, "logical");
    }

    @Fallback
    @SlowPath
    public int doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }

    private RLogicalVector performAbstractIntVector(RAbstractIntVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            int value = operand.getDataAt(i);
            ddata[i] = naCheck.convertIntToLogical(value);
        }
        RLogicalVector ret;
        if (preserveDimensions() && preserveNames()) {
            ret = RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        } else if (preserveDimensions()) {
            ret = RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        } else if (preserveNames()) {
            ret = RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        } else {
            ret = RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA());
        }
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    private RLogicalVector performAbstractDoubleVector(RAbstractDoubleVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            double value = operand.getDataAt(i);
            ddata[i] = naCheck.convertDoubleToLogical(value);
        }
        RLogicalVector ret;
        if (preserveDimensions() && preserveNames()) {
            ret = RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getDimensions(), operand.getNames());
        } else if (preserveDimensions()) {
            ret = RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        } else if (preserveNames()) {
            ret = RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        } else {
            ret = RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA());
        }
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }
}
