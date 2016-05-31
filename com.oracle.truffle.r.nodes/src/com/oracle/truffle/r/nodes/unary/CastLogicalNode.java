/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.unary.CastNode.Samples;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

public abstract class CastLogicalNode extends CastLogicalBaseNode {

    private final NAProfile naProfile = NAProfile.create();

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @FunctionalInterface
    private interface IntToByteFunction {
        byte apply(int value);
    }

    private RLogicalVector createResultVector(RAbstractVector operand, IntToByteFunction elementFunction) {
        naCheck.enable(operand);
        byte[] bdata = new byte[operand.getLength()];
        boolean seenNA = false;
        for (int i = 0; i < operand.getLength(); i++) {
            byte value = elementFunction.apply(i);
            bdata[i] = value;
            seenNA = seenNA || naProfile.isNA(value);
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(bdata, !seenNA, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RLogicalVector doLogicalVector(RLogicalVector operand) {
        return operand;
    }

    @Specialization(guards = "!isFactor(operand)")
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
    protected RLogicalVector doList(RList list) {
        int length = list.getLength();
        byte[] result = new byte[length];
        boolean seenNA = false;
        for (int i = 0; i < length; i++) {
            Object entry = list.getDataAt(i);
            if (entry instanceof RList) {
                result[i] = RRuntime.LOGICAL_NA;
                seenNA = true;
            } else {
                Object castEntry = castLogicalRecursive(entry);
                if (castEntry instanceof Byte) {
                    byte value = (Byte) castEntry;
                    result[i] = value;
                    seenNA = seenNA || RRuntime.isNA(value);
                } else if (castEntry instanceof RLogicalVector) {
                    RLogicalVector logicalVector = (RLogicalVector) castEntry;
                    if (logicalVector.getLength() == 1) {
                        byte value = logicalVector.getDataAt(0);
                        result[i] = value;
                        seenNA = seenNA || RRuntime.isNA(value);
                    } else if (logicalVector.getLength() == 0) {
                        result[i] = RRuntime.LOGICAL_NA;
                        seenNA = true;
                    } else {
                        throw throwCannotCoerceListError("logical");
                    }
                } else {
                    throw throwCannotCoerceListError("logical");
                }
            }
        }
        RLogicalVector ret = RDataFactory.createLogicalVector(result, !seenNA);
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

    @Specialization(guards = "isFactor(factor)")
    protected RLogicalVector asLogical(RAbstractIntVector factor) {
        byte[] data = new byte[factor.getLength()];
        Arrays.fill(data, RRuntime.LOGICAL_NA);
        return RDataFactory.createLogicalVector(data, RDataFactory.INCOMPLETE_VECTOR);
    }

    @Fallback
    @TruffleBoundary
    protected int doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }

    public static CastLogicalNode createNonPreserving() {
        return CastLogicalNodeGen.create(false, false, false);
    }

    @Child private InheritsCheckNode inheritsFactorCheck = new InheritsCheckNode(RRuntime.CLASS_FACTOR);

    protected boolean isFactor(Object o) {
        return inheritsFactorCheck.execute(o);
    }
}
