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

import java.util.function.IntToDoubleFunction;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class CastDoubleNode extends CastDoubleBaseNode {

    protected CastDoubleNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes);
    }

    @Child private CastDoubleNode recursiveCastDouble;

    private Object castDoubleRecursive(Object o) {
        if (recursiveCastDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveCastDouble = insert(CastDoubleNodeGen.create(preserveNames(), preserveDimensions(), preserveAttributes()));
        }
        return recursiveCastDouble.executeDouble(o);
    }

    private RDoubleVector createResultVector(RAbstractVector operand, double[] ddata) {
        RDoubleVector ret = RDataFactory.createDoubleVector(ddata, naCheck.neverSeenNA(), getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (preserveAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    private RDoubleVector vectorCopy(RAbstractContainer operand, double[] data, boolean isComplete) {
        RDoubleVector ret = RDataFactory.createDoubleVector(data, isComplete, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (preserveAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    private RDoubleVector createResultVector(RAbstractVector operand, IntToDoubleFunction elementFunction) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        boolean seenNA = false;
        for (int i = 0; i < operand.getLength(); i++) {
            double value = elementFunction.applyAsDouble(i);
            ddata[i] = value;
            seenNA = seenNA || naProfile.isNA(value);
        }
        return vectorCopy(operand, ddata, !seenNA);
    }

    @Specialization
    protected RDoubleVector doIntVector(RAbstractIntVector operand) {
        return createResultVector(operand, index -> naCheck.convertIntToDouble(operand.getDataAt(index)));
    }

    @Specialization
    protected RDoubleVector doLogicalVectorDims(RAbstractLogicalVector operand) {
        return createResultVector(operand, index -> naCheck.convertLogicalToDouble(operand.getDataAt(index)));
    }

    @Specialization
    protected RDoubleVector doStringVector(RStringVector operand, //
                    @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        boolean seenNA = false;
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            double doubleValue;
            if (naCheck.check(value) || emptyStringProfile.profile(value.isEmpty())) {
                doubleValue = RRuntime.DOUBLE_NA;
                seenNA = true;
            } else {
                doubleValue = RRuntime.string2doubleNoCheck(value);
                if (naProfile.isNA(doubleValue)) {
                    seenNA = true;
                    if (!value.isEmpty()) {
                        warningBranch.enter();
                        warning = true;
                    }
                }
            }
            ddata[i] = doubleValue;
        }
        if (warning) {
            RError.warning(this, RError.Message.NA_INTRODUCED_COERCION);
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(ddata, !seenNA, getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (preserveAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RDoubleVector doComplexVector(RComplexVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        boolean warning = false;
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ddata[i] = naCheck.convertComplexToDouble(value, false);
            if (value.getImaginaryPart() != 0.0) {
                warning = true;
            }
        }
        if (warning) {
            warningBranch.enter();
            RError.warning(this, RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        }
        return createResultVector(operand, ddata);
    }

    @Specialization
    protected RDoubleVector doRawVector(RRawVector operand) {
        return createResultVector(operand, index -> RRuntime.raw2double(operand.getDataAt(index)));
    }

    @Specialization
    protected RDoubleVector doDoubleVector(RDoubleVector operand) {
        if (preserveAttributes() && preserveDimensions() && preserveNames()) {
            return operand;
        } else {
            return vectorCopy(operand, operand.getDataCopy(), operand.isComplete());
        }
    }

    @Specialization
    protected RDoubleSequence doDoubleVector(RDoubleSequence operand) {
        // sequence does not have attributes - nothing to copy or drop
        return operand;
    }

    @Specialization
    protected RDoubleVector doList(RAbstractListVector list) {
        int length = list.getLength();
        double[] result = new double[length];
        boolean seenNA = false;
        for (int i = 0; i < length; i++) {
            Object entry = list.getDataAt(i);
            if (entry instanceof RList) {
                result[i] = RRuntime.DOUBLE_NA;
                seenNA = true;
            } else {
                Object castEntry = castDoubleRecursive(entry);
                if (castEntry instanceof Double) {
                    double value = (Double) castEntry;
                    result[i] = value;
                    seenNA = seenNA || RRuntime.isNA(value);
                } else if (castEntry instanceof RDoubleVector) {
                    RDoubleVector doubleVector = (RDoubleVector) castEntry;
                    if (doubleVector.getLength() == 1) {
                        double value = doubleVector.getDataAt(0);
                        result[i] = value;
                        seenNA = seenNA || RRuntime.isNA(value);
                    } else if (doubleVector.getLength() == 0) {
                        result[i] = RRuntime.DOUBLE_NA;
                        seenNA = true;
                    } else {
                        throw throwCannotCoerceListError("numeric");
                    }
                } else {
                    throw throwCannotCoerceListError("numeric");
                }
            }
        }
        RDoubleVector ret = RDataFactory.createDoubleVector(result, !seenNA);
        if (preserveAttributes()) {
            ret.copyRegAttributesFrom(list);
        }
        return ret;
    }

    public static CastDoubleNode create() {
        return CastDoubleNodeGen.create(true, true, true);
    }

    public static CastDoubleNode createNonPreserving() {
        return CastDoubleNodeGen.create(false, false, false);
    }
}
