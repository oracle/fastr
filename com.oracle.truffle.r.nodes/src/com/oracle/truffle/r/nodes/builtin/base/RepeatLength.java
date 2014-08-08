/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "rep_len", kind = SUBSTITUTE)
public abstract class RepeatLength extends RBuiltinNode {
    private final String[] PARAMETER_NAMES={"x", "length.out"};

    @Override
    protected Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @CreateCast("arguments")
    protected RNode[] castStatusArgument(RNode[] arguments) {
        // length.out is at index 1 of arguments
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false, false);
        return arguments;
    }

    @Specialization
    @SuppressWarnings("unused")
    public RNull repLen(RNull value, int length) {
        controlVisibility();
        return RNull.instance;
    }

    //
    // Specialization for single values
    //
    @Specialization
    public RRawVector repLen(RRaw value, int length) {
        controlVisibility();
        byte[] array = new byte[length];
        Arrays.fill(array, value.getValue());
        return RDataFactory.createRawVector(array);
    }

    @Specialization
    public RIntVector repLen(int value, int length) {
        controlVisibility();
        int[] array = new int[length];
        Arrays.fill(array, length);
        return RDataFactory.createIntVector(array, RRuntime.isComplete(value));
    }

    @Specialization
    public RDoubleVector repLen(double value, int length) {
        controlVisibility();
        double[] array = new double[length];
        Arrays.fill(array, value);
        return RDataFactory.createDoubleVector(array, RRuntime.isComplete(value));
    }

    @Specialization
    public RStringVector repLen(String value, int length) {
        controlVisibility();
        String[] array = new String[length];
        Arrays.fill(array, value);
        return RDataFactory.createStringVector(array, !RRuntime.isNA(value));
    }

    @Specialization
    public RComplexVector repLen(RComplex value, int length) {
        controlVisibility();
        int complexLength = length * 2;
        double[] array = new double[complexLength];
        for (int i = 0; i < complexLength; i += 2) {
            array[i] = value.getRealPart();
            array[i + 1] = value.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, !value.isNA());
    }

    @Specialization
    public RLogicalVector repLen(byte value, int length) {
        controlVisibility();
        byte[] array = new byte[length];
        Arrays.fill(array, value);
        return RDataFactory.createLogicalVector(array, value != RRuntime.LOGICAL_NA);
    }

    //
    // Specialization for vector values
    //
    @Specialization
    public RIntVector repLen(RAbstractIntVector value, int length) {
        controlVisibility();
        int[] array = new int[length];
        for (int i = 0, j = 0; i < length; i++, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        return RDataFactory.createIntVector(array, value.isComplete());
    }

    @Specialization
    public RDoubleVector repLen(RDoubleVector value, int length) {
        controlVisibility();
        double[] array = new double[length];
        for (int i = 0, j = 0; i < length; i++, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        return RDataFactory.createDoubleVector(array, value.isComplete());
    }

    @Specialization
    public RRawVector repLen(RRawVector value, int length) {
        controlVisibility();
        byte[] array = new byte[length];
        for (int i = 0, j = 0; i < length; i++, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j).getValue();
        }
        return RDataFactory.createRawVector(array);
    }

    @Specialization
    public RComplexVector repLen(RComplexVector value, int length) {
        controlVisibility();
        final int resultLength = length * 2;
        double[] array = new double[resultLength];
        for (int i = 0, j = 0; i < resultLength; i += 2, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j).getRealPart();
            array[i + 1] = value.getDataAt(j).getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, value.isComplete());
    }
}
