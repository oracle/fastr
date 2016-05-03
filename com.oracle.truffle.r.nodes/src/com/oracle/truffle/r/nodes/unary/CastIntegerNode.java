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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

public abstract class CastIntegerNode extends CastIntegerBaseNode {

    private final NAProfile naProfile = NAProfile.create();

    public abstract Object executeInt(int o);

    public abstract Object executeInt(double o);

    public abstract Object executeInt(byte o);

    public abstract Object executeInt(Object o);

    @Child private CastIntegerNode recursiveCastInteger;

    @Specialization
    protected RAbstractIntVector doIntVector(RAbstractIntVector operand) {
        return operand;
    }

    @Specialization
    protected RIntSequence doDoubleSequence(RDoubleSequence operand) {
        naCheck.enable(operand);
        return RDataFactory.createIntSequence(naCheck.convertDoubleToInt(operand.getStart()), naCheck.convertDoubleToInt(operand.getStride()), operand.getLength());
    }

    private RIntVector createResultVector(RAbstractVector operand, int[] idata) {
        RIntVector ret = RDataFactory.createIntVector(idata, naCheck.neverSeenNA(), getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @FunctionalInterface
    private interface IntToIntFunction {
        int apply(int value);
    }

    private RIntVector createResultVector(RAbstractVector operand, IntToIntFunction elementFunction) {
        naCheck.enable(operand);
        int[] idata = new int[operand.getLength()];
        boolean seenNA = false;
        for (int i = 0; i < operand.getLength(); i++) {
            int value = elementFunction.apply(i);
            idata[i] = value;
            seenNA = seenNA || naProfile.isNA(value);
        }
        RIntVector ret = RDataFactory.createIntVector(idata, !seenNA, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RIntVector doComplexVector(RAbstractComplexVector operand) {
        naCheck.enable(operand);
        int length = operand.getLength();
        int[] idata = new int[length];
        boolean warning = false;
        for (int i = 0; i < length; i++) {
            RComplex data = operand.getDataAt(i);
            idata[i] = naCheck.convertComplexToInt(data, false);
            if (data.getImaginaryPart() != 0.0) {
                warning = true;
            }
        }
        if (warning) {
            warningBranch.enter();
            RError.warning(this, RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return createResultVector(operand, idata);
    }

    @Specialization
    protected RIntVector doStringVector(RAbstractStringVector operand, //
                    @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile) {
        naCheck.enable(operand);
        int[] idata = new int[operand.getLength()];
        boolean seenNA = false;
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            int intValue;
            if (naCheck.check(value) || emptyStringProfile.profile(value.isEmpty())) {
                intValue = RRuntime.INT_NA;
                seenNA = true;
            } else {
                intValue = RRuntime.string2intNoCheck(value);
                if (naProfile.isNA(intValue)) {
                    seenNA = true;
                    if (!value.isEmpty()) {
                        warningBranch.enter();
                        warning = true;
                    }
                }
            }
            idata[i] = intValue;
        }
        if (warning) {
            RError.warning(this, RError.Message.NA_INTRODUCED_COERCION);
        }
        RIntVector ret = RDataFactory.createIntVector(idata, !seenNA, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    public RIntVector doLogicalVector(RAbstractLogicalVector operand) {
        return createResultVector(operand, index -> naCheck.convertLogicalToInt(operand.getDataAt(index)));
    }

    @Specialization
    protected RIntVector doDoubleVector(RAbstractDoubleVector operand) {
        naCheck.enable(operand);
        return createResultVector(operand, naCheck.convertDoubleVectorToIntData(operand));
    }

    @Specialization
    protected RIntVector doRawVector(RAbstractRawVector operand) {
        return createResultVector(operand, index -> RRuntime.raw2int(operand.getDataAt(index)));
    }

    @Specialization
    protected RIntVector doList(RAbstractListVector list) {
        int length = list.getLength();
        int[] result = new int[length];
        boolean seenNA = false;
        for (int i = 0; i < length; i++) {
            Object entry = list.getDataAtAsObject(i);
            if (entry instanceof RList) {
                result[i] = RRuntime.INT_NA;
                seenNA = true;
            } else {
                Object castEntry = castIntegerRecursive(entry);
                if (castEntry instanceof Integer) {
                    int value = (Integer) castEntry;
                    result[i] = value;
                    seenNA = seenNA || RRuntime.isNA(value);
                } else if (castEntry instanceof RIntVector) {
                    RIntVector intVector = (RIntVector) castEntry;
                    if (intVector.getLength() == 1) {
                        int value = intVector.getDataAt(0);
                        result[i] = value;
                        seenNA = seenNA || RRuntime.isNA(value);
                    } else if (intVector.getLength() == 0) {
                        result[i] = RRuntime.INT_NA;
                        seenNA = true;
                    } else {
                        throw throwCannotCoerceListError("integer");
                    }
                } else {
                    throw throwCannotCoerceListError("integer");
                }
            }
        }
        RIntVector ret = RDataFactory.createIntVector(result, !seenNA);
        if (isAttrPreservation()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    // TODO Should be type-variable and moved to CastNode
    @Specialization(guards = {"args.getLength() == 1", "isIntVector(args.getArgument(0))"})
    protected RIntVector doRArgsValuesAndNames(RArgsValuesAndNames args) {
        return (RIntVector) args.getArgument(0);
    }

    protected static boolean isIntVector(Object arg) {
        return arg instanceof RIntVector;
    }

    @Fallback
    @TruffleBoundary
    protected int doOther(Object operand) {
        throw new ConversionFailedException(operand.getClass().getName());
    }

    public static CastIntegerNode create() {
        return CastIntegerNodeGen.create(true, true, true);
    }

    public static CastIntegerNode createNonPreserving() {
        return CastIntegerNodeGen.create(false, false, false);
    }
}
