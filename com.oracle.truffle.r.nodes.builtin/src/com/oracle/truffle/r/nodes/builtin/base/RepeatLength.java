/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

@RBuiltin(name = "rep_len", kind = INTERNAL, parameterNames = {"x", "length.out"})
public abstract class RepeatLength extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RNull repLen(RNull value, int length) {
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
        Arrays.fill(array, length);
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

    //
    // Specialization for vector values
    //
    @Specialization
    protected RIntVector repLen(RAbstractIntVector value, int length) {
        int[] array = new int[length];
        for (int i = 0, j = 0; i < length; i++, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        return RDataFactory.createIntVector(array, value.isComplete());
    }

    @Specialization
    protected RDoubleVector repLen(RDoubleVector value, int length) {
        double[] array = new double[length];
        for (int i = 0, j = 0; i < length; i++, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        return RDataFactory.createDoubleVector(array, value.isComplete());
    }

    @Specialization
    protected RStringVector repLen(RStringVector vectorToRepeat, int length) {
        String[] result = new String[length];
        int vectorToRepeatLength = vectorToRepeat.getLength();
        for (int i = 0; i < length; i++) {
            result[i] = vectorToRepeat.getDataAt(i % vectorToRepeatLength);
        }
        return RDataFactory.createStringVector(result, vectorToRepeat.isComplete());
    }

    @Specialization
    protected RRawVector repLen(RRawVector value, int length) {
        byte[] array = new byte[length];
        for (int i = 0, j = 0; i < length; i++, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j).getValue();
        }
        return RDataFactory.createRawVector(array);
    }

    @Specialization
    protected RComplexVector repLen(RComplexVector value, int length) {
        final int resultLength = length * 2;
        double[] array = new double[resultLength];
        for (int i = 0, j = 0; i < resultLength; i += 2, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j).getRealPart();
            array[i + 1] = value.getDataAt(j).getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, value.isComplete());
    }

    @Specialization
    protected RList repLen(RList list, int length) {
        Object[] data = new Object[length];
        for (int i = 0; i < length; i++) {
            data[i] = list.getDataAt(i % list.getLength());
        }
        return RDataFactory.createList(data);
    }
}
