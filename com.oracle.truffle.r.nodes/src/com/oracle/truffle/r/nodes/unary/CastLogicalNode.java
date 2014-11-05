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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
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
            recursiveCastLogical = insert(CastLogicalNodeFactory.create(null, isPreserveNames(), isDimensionsPreservation(), isAttrPreservation()));
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

    @FunctionalInterface
    private interface IntToByteFunction {
        byte apply(int value);
    }

    private RLogicalVector createResultVector(RAbstractVector operand, IntToByteFunction elementFunction) {
        naCheck.enable(operand);
        byte[] bdata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            bdata[i] = elementFunction.apply(i);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(bdata, naCheck.neverSeenNA(), isPreserveDimensions() ? operand.getDimensions() : null, isPreserveNames() ? operand.getNames() : null);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RLogicalVector doLogicalVector(RLogicalVector operand) {
        return operand;
    }

    @Specialization
    protected RLogicalVector doIntVector(RAbstractIntVector operand) {
        return createResultVector(operand, index -> naCheck.convertIntToLogical(operand.getDataAt(index)));
    }

    @Specialization
    protected RLogicalVector doDoubleVector(RAbstractDoubleVector operand) {
        return createResultVector(operand, index -> naCheck.convertDoubleToLogical(operand.getDataAt(index)));
    }

    @Specialization
    protected RLogicalVector doStringVector(RStringVector operand) {
        return createResultVector(operand, index -> naCheck.convertStringToLogical(operand.getDataAt(index)));
    }

    @Specialization
    protected RLogicalVector doComplexVector(RComplexVector operand) {
        return createResultVector(operand, index -> naCheck.convertComplexToLogical(operand.getDataAt(index)));
    }

    @Specialization
    protected RLogicalVector doRawVectorDims(RRawVector operand) {
        return createResultVector(operand, index -> RRuntime.raw2logical(operand.getDataAt(index)));
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
                        throw throwCannotCoerceListError("logical");
                    }
                } else {
                    throw throwCannotCoerceListError("logical");
                }
            }
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, naCheck.neverSeenNA());
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization
    protected RArgsValuesAndNames doArgsValueAndNames(RArgsValuesAndNames values) {
        return values;
    }

    @Specialization
    protected RMissing doMissing(RMissing missing) {
        return missing;
    }

    @Fallback
    @TruffleBoundary
    public int doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }
}
