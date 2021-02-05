/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "rep_len", kind = INTERNAL, parameterNames = {"x", "length.out"}, behavior = PURE)
public abstract class RepeatLength extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(RepeatLength.class);
        casts.arg("x").allowNull().mustBe(abstractVectorValue().or(instanceOf(RExpression.class)), RError.Message.ATTEMPT_TO_REPLICATE_NO_VECTOR);
        casts.arg("length.out").defaultError(RError.Message.INVALID_VALUE, "length.out").mustNotBeNull().asIntegerVector().mustBe(singleElement()).findFirst().mustNotBeNA().mustBe(gte0());
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RNull repLen(RNull value, int length) {
        if (length != 0) {
            RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_REPLICATE_NULL);
        }
        return RNull.instance;
    }

    //
    // Specialization for single values
    //
    @Specialization
    protected RRawVector repLen(RRaw value, int length) {
        byte[] array = new byte[length];
        Arrays.fill(array, value.getValue());
        return RDataFactory.createRawVector(array);
    }

    @Specialization
    protected RIntVector repLen(int value, int length) {
        int[] array = new int[length];
        Arrays.fill(array, value);
        return RDataFactory.createIntVector(array, !RRuntime.isNA(value));
    }

    @Specialization
    protected RDoubleVector repLen(double value, int length) {
        double[] array = new double[length];
        Arrays.fill(array, value);
        return RDataFactory.createDoubleVector(array, !RRuntime.isNA(value));
    }

    @Specialization
    protected RStringVector repLen(String value, int length) {
        String[] array = new String[length];
        Arrays.fill(array, value);
        return RDataFactory.createStringVector(array, !RRuntime.isNA(value));
    }

    @Specialization
    protected RComplexVector repLen(RComplex value, int length) {
        int complexLength = length * 2;
        double[] array = new double[complexLength];
        for (int i = 0; i < complexLength; i += 2) {
            array[i] = value.getRealPart();
            array[i + 1] = value.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, !value.isNA());
    }

    @Specialization
    protected RLogicalVector repLen(byte value, int length) {
        byte[] array = new byte[length];
        Arrays.fill(array, value);
        return RDataFactory.createLogicalVector(array, value != RRuntime.LOGICAL_NA);
    }

    @Specialization(limit = "getGenericDataLibraryCacheSize()")
    protected RAbstractVector repLenCached(RAbstractVector x, int length,
                    @CachedLibrary("x") AbstractContainerLibrary xContainerLib,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @CachedLibrary(limit = "getGenericDataLibraryCacheSize()") VectorDataLibrary resultDataLib,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile) {
        Object xData = x.getData();
        if (emptyProfile.profile(xDataLib.getLength(xData) == 0)) {
            return xContainerLib.createEmptySameType(x, length, true);
        }
        SeqIterator xIt = xDataLib.iterator(xData);
        RAbstractVector result = xContainerLib.createEmptySameType(x, length, false);
        Object resultData = result.getData();
        SeqWriteIterator resultIt = resultDataLib.writeIterator(resultData);
        try {
            while (resultDataLib.next(resultData, resultIt)) {
                xDataLib.nextWithWrap(xData, xIt);
                resultDataLib.transferNext(resultData, resultIt, xDataLib, xIt, xData);
            }
        } finally {
            resultDataLib.commitWriteIterator(resultData, resultIt, xDataLib.getNACheck(xData).neverSeenNA());
        }
        return result;
    }
}
