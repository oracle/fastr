/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2RNodeGen;
import com.oracle.truffle.r.runtime.ops.na.NAProfile;

@ImportStatic(RRuntime.class)
public abstract class CastIntegerNode extends CastIntegerBaseNode {

    private final NAProfile naProfile = NAProfile.create();

    private final BranchProfile warningBranch = BranchProfile.create();

    protected CastIntegerNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI, boolean withReuse) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI, withReuse);
    }

    protected CastIntegerNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes, false, false);
    }

    public abstract Object executeInt(int o);

    public abstract Object executeInt(double o);

    public abstract Object executeInt(byte o);

    public abstract Object executeInt(Object o);

    @Specialization
    protected RAbstractIntVector doIntVector(RAbstractIntVector operand) {
        return operand;
    }

    @Specialization
    protected RIntSequence doDoubleSequence(RDoubleSequence operand) {
        // start and stride cannot be NA so no point checking
        return RDataFactory.createIntSequence(RRuntime.double2intNoCheck(operand.getStart()), RRuntime.double2intNoCheck(operand.getStride()), operand.getLength());
    }

    private RIntVector vectorCopy(RAbstractVector operand, int[] idata, boolean isComplete) {
        RIntVector ret = RDataFactory.createIntVector(idata, isComplete, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (preserveRegAttributes()) {
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
        return vectorCopy(operand, idata, !seenNA);
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
            warning(RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return vectorCopy(operand, idata, naCheck.neverSeenNA());
    }

    @Specialization
    protected RIntVector doStringVector(RAbstractStringVector operand,
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
            warning(RError.Message.NA_INTRODUCED_COERCION);
        }
        return vectorCopy(operand, idata, !seenNA);
    }

    @Specialization(guards = "isReusable(operand)")
    public RAbstractIntVector doLogicalVectorReuse(RAbstractLogicalVector operand) {
        return (RAbstractIntVector) castWithReuse(RType.Integer, operand, naProfile.getConditionProfile());
    }

    @Specialization(guards = "isReusable(operand)")
    protected RAbstractIntVector doDoubleVectorReuse(RAbstractDoubleVector operand) {
        return (RAbstractIntVector) castWithReuse(RType.Integer, operand, naProfile.getConditionProfile());
    }

    @Specialization(guards = "isReusable(operand)")
    protected RAbstractIntVector doRawVectorReuse(RAbstractRawVector operand) {
        return (RAbstractIntVector) castWithReuse(RType.Integer, operand, naProfile.getConditionProfile());
    }

    @Specialization
    public RIntVector doLogicalVector(RAbstractLogicalVector operand) {
        return createResultVector(operand, index -> naCheck.convertLogicalToInt(operand.getDataAt(index)));
    }

    @Specialization
    protected RIntVector doDoubleVector(RAbstractDoubleVector operand) {
        return createResultVector(operand, index -> naCheck.convertDoubleToInt(operand.getDataAt(index)));
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
                } else if (castEntry instanceof RAbstractIntVector) {
                    RAbstractIntVector intVector = (RAbstractIntVector) castEntry;
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
        RIntVector ret = RDataFactory.createIntVector(result, !seenNA, getPreservedDimensions(list), getPreservedNames(list));
        if (preserveRegAttributes()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected RIntVector doForeignObject(TruffleObject obj,
                    @Cached("createForeignArray2RNode()") ForeignArray2R foreignArray2R) {
        Object o = foreignArray2R.execute(obj, true);
        if (!RRuntime.isForeignObject(o)) {
            if (o instanceof RIntVector) {
                return (RIntVector) o;
            }
            o = castIntegerRecursive(o);
            if (o instanceof RIntVector) {
                return (RIntVector) o;
            }
        }
        throw error(RError.Message.CANNOT_COERCE_EXTERNAL_OBJECT_TO_VECTOR, "vector");
    }

    // TODO Should be type-variable and moved to CastNode
    @Specialization(guards = {"args.getLength() == 1", "isIntVector(args.getArgument(0))"})
    protected RIntVector doRArgsValuesAndNames(RArgsValuesAndNames args) {
        return (RIntVector) args.getArgument(0);
    }

    protected static boolean isIntVector(Object arg) {
        return arg instanceof RIntVector;
    }

    public static CastIntegerNode create() {
        return CastIntegerNodeGen.create(true, true, true, false, false);
    }

    public static CastIntegerNode createWithReuse() {
        return CastIntegerNodeGen.create(true, true, true, false, true);
    }

    public static CastIntegerNode createForRFFI(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        return CastIntegerNodeGen.create(preserveNames, preserveDimensions, preserveAttributes, true, false);
    }

    public static CastIntegerNode createNonPreserving() {
        return CastIntegerNodeGen.create(false, false, false, false, false);
    }

    protected ForeignArray2R createForeignArray2RNode() {
        return ForeignArray2RNodeGen.create();
    }
}
