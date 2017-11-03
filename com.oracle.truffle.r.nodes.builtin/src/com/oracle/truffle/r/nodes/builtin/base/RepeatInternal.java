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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.function.IntFunction;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "rep.int", kind = INTERNAL, parameterNames = {"x", "times"}, behavior = PURE)
public abstract class RepeatInternal extends RBuiltinNode.Arg2 {

    private final ConditionProfile timesOneProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(RepeatInternal.class);
        casts.arg("x").mustBe(abstractVectorValue(), RError.Message.ATTEMPT_TO_REPLICATE, typeName());
        casts.arg("times").defaultError(RError.Message.INVALID_TYPE, typeName(), "times", "vector").mustBe(abstractVectorValue()).asIntegerVector().mustBe(notEmpty(),
                        RError.Message.INVALID_VALUE, "times");
    }

    @FunctionalInterface
    private interface ArrayUpdateFunction<ValueT, ArrayT> {
        void update(ArrayT array, int pos, ValueT value, int index);
    }

    @FunctionalInterface
    private interface CreateResultFunction<ResultT, ArrayT> {
        ResultT create(ArrayT array, boolean complete);
    }

    private <ValueT extends RAbstractVector, ResultT extends ValueT, ArrayT> ResultT repInt(ValueT value, RAbstractIntVector times, IntFunction<ArrayT> arrayConstructor,
                    ArrayUpdateFunction<ValueT, ArrayT> arrayUpdate, CreateResultFunction<ResultT, ArrayT> createResult) {
        ArrayT result;
        int timesLength = times.getLength();
        int valueLength = value.getLength();
        if (timesOneProfile.profile(timesLength == 1)) {
            int timesValue = times.getDataAt(0);
            if (timesValue < 0) {
                throw error(RError.Message.INVALID_VALUE, "times");
            }
            int count = timesValue * valueLength;
            result = arrayConstructor.apply(count);
            int pos = 0;
            for (int i = 0; i < timesValue; i++) {
                for (int j = 0; j < valueLength; j++) {
                    arrayUpdate.update(result, pos++, value, j);
                }
            }
        } else if (timesLength == valueLength) {
            int count = 0;
            for (int i = 0; i < timesLength; i++) {
                int data = times.getDataAt(i);
                if (data < 0) {
                    throw error(RError.Message.INVALID_VALUE, "times");
                }
                count += data;
            }
            result = arrayConstructor.apply(count);
            int pos = 0;
            for (int i = 0; i < valueLength; i++) {
                int num = times.getDataAt(i);
                for (int j = 0; j < num; j++) {
                    arrayUpdate.update(result, pos++, value, i);
                }
            }
        } else {
            throw error(RError.Message.INVALID_VALUE, "times");
        }
        return createResult.create(result, value.isComplete());
    }

    @Specialization
    protected RDoubleVector repInt(RAbstractDoubleVector value, RAbstractIntVector times) {
        return repInt(value, times, double[]::new, (array, pos, val, index) -> array[pos] = val.getDataAt(index), RDataFactory::createDoubleVector);
    }

    @Specialization
    protected RIntVector repInt(RAbstractIntVector value, RAbstractIntVector times) {
        return repInt(value, times, int[]::new, (array, pos, val, index) -> array[pos] = val.getDataAt(index), RDataFactory::createIntVector);
    }

    @Specialization
    protected RLogicalVector repInt(RAbstractLogicalVector value, RAbstractIntVector times) {
        return repInt(value, times, byte[]::new, (array, pos, val, index) -> array[pos] = val.getDataAt(index), RDataFactory::createLogicalVector);
    }

    @Specialization
    protected RStringVector repInt(RAbstractStringVector value, RAbstractIntVector times) {
        return repInt(value, times, String[]::new, (array, pos, val, index) -> array[pos] = val.getDataAt(index), RDataFactory::createStringVector);
    }

    @Specialization
    protected RRawVector repInt(RAbstractRawVector value, RAbstractIntVector times) {
        return repInt(value, times, byte[]::new, (array, pos, val, index) -> array[pos] = val.getRawDataAt(index), (array, complete) -> RDataFactory.createRawVector(array));
    }

    @Specialization
    protected RComplexVector repComplex(RAbstractComplexVector value, RAbstractIntVector times) {
        int timesLength = times.getLength();
        int valueLength = value.getLength();
        double[] resultArray;
        if (timesOneProfile.profile(timesLength == 1)) {
            int timesValue = times.getDataAt(0);
            if (timesValue < 0) {
                throw error(RError.Message.INVALID_VALUE, "times");
            }
            resultArray = new double[(timesValue * valueLength) << 1];
            int pos = 0;
            for (int i = 0; i < timesValue; i++) {
                for (int j = 0; j < valueLength; j++) {
                    RComplex complex = value.getDataAt(j);
                    resultArray[pos++] = complex.getRealPart();
                    resultArray[pos++] = complex.getImaginaryPart();
                }
            }
        } else if (timesLength == valueLength) {
            int count = 0;
            for (int i = 0; i < timesLength; i++) {
                int data = times.getDataAt(i);
                if (data < 0) {
                    throw error(RError.Message.INVALID_VALUE, "times");
                }
                count += data;
            }
            resultArray = new double[count << 1];
            int pos = 0;
            for (int i = 0; i < valueLength; i++) {
                int num = times.getDataAt(i);
                RComplex complex = value.getDataAt(i);
                for (int j = 0; j < num; j++) {
                    resultArray[pos++] = complex.getRealPart();
                    resultArray[pos++] = complex.getImaginaryPart();
                }
            }
        } else {
            throw error(RError.Message.INVALID_VALUE, "times");
        }
        return RDataFactory.createComplexVector(resultArray, value.isComplete());
    }

    @Specialization
    protected RList repList(RList value, int times) {
        int oldLength = value.getLength();
        int length = value.getLength() * times;
        Object[] array = new Object[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; j++) {
                array[i * oldLength + j] = value.getDataAt(j);
            }
        }
        return RDataFactory.createList(array);
    }
}
