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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

// FIXME honour times in case length.out is given but NA
// FIXME honour "each" parameter

@RBuiltin(name = "rep", kind = PRIMITIVE)
public abstract class Repeat extends RBuiltinNode {

    private static final String[] PARAMETER_NAMES = new String[]{"x", "times", "length.out", "each"};

    @Override
    public String[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(1), ConstantNode.create(RMissing.instance), ConstantNode.create(1)};
    }

    @CreateCast("arguments")
    protected RNode[] castTimesLength(RNode[] arguments) {
        // times is at index 1; length.out, at 2
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false, false);
        arguments[2] = CastIntegerNodeFactory.create(arguments[2], true, false, false);
        return arguments;
    }

    @Specialization(order = 0)
    @SuppressWarnings("unused")
    public RNull repeat(VirtualFrame frame, RNull value, Object times, Object lengthOut, Object each) {
        controlVisibility();
        return RNull.instance;
    }

    //
    // Specialisations for single values.
    // For each value type, these specialisations are given:
    // * if times is given but length.out is not,
    // * if length.out is given (it supersedes times)
    //

    @Specialization(order = 10)
    @SuppressWarnings("unused")
    public RIntVector repeat(int value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int[] array = new int[times];
        Arrays.fill(array, value);
        return RDataFactory.createIntVector(array, RRuntime.isComplete(value));
    }

    @Specialization(order = 11)
    @SuppressWarnings("unused")
    public RIntVector repeat(int value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization(order = 20)
    @SuppressWarnings("unused")
    public RDoubleVector repeat(double value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        double[] array = new double[times];
        Arrays.fill(array, value);
        return RDataFactory.createDoubleVector(array, RRuntime.isComplete(value));
    }

    @Specialization(order = 21)
    @SuppressWarnings("unused")
    public RDoubleVector repeat(double value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization(order = 30)
    @SuppressWarnings("unused")
    public RRawVector repeat(RRaw value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        byte[] array = new byte[times];
        Arrays.fill(array, value.getValue());
        return RDataFactory.createRawVector(array);
    }

    @Specialization(order = 31)
    @SuppressWarnings("unused")
    public RRawVector repeat(RRaw value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization(order = 40)
    @SuppressWarnings("unused")
    public RComplexVector repeat(RComplex value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        double[] array = new double[times << 1];
        for (int i = 0; i < times; ++i) {
            int index = i << 1;
            array[index] = value.getRealPart();
            array[index + 1] = value.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, !value.isNA());
    }

    @Specialization(order = 41)
    @SuppressWarnings("unused")
    public RComplexVector repeat(RComplex value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization(order = 50)
    @SuppressWarnings("unused")
    public RStringVector repeat(String value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        String[] array = new String[times];
        Arrays.fill(array, value);
        return RDataFactory.createStringVector(array, !RRuntime.isNA(value));
    }

    @Specialization(order = 51)
    @SuppressWarnings("unused")
    public RStringVector repeat(String value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization(order = 60)
    @SuppressWarnings("unused")
    public RLogicalVector repeat(byte value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        byte[] array = new byte[times];
        Arrays.fill(array, value);
        return RDataFactory.createLogicalVector(array, value != RRuntime.LOGICAL_NA);
    }

    @Specialization(order = 61)
    @SuppressWarnings("unused")
    public RLogicalVector repeat(byte value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    //
    // Specialisations for vector values.
    // The specialisations given for each value type are as follows:
    // * single times argument
    // * single length.out argument (supersedes times)
    //

    @Specialization(order = 100)
    @SuppressWarnings("unused")
    public RIntVector repeatT(RAbstractIntVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = oldLength * times;
        int[] array = new int[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                array[i * oldLength + j] = value.getDataAt(j);
            }
        }
        return RDataFactory.createIntVector(array, value.isComplete());
    }

    @Specialization(order = 102)
    @SuppressWarnings("unused")
    public RIntVector repeatL(RAbstractIntVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        int[] array = new int[lengthOut];
        for (int i = 0, j = 0; i < lengthOut; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        return RDataFactory.createIntVector(array, value.isComplete());
    }

    @Specialization(order = 110)
    @SuppressWarnings("unused")
    public RDoubleVector repeatT(RAbstractDoubleVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = value.getLength() * times;
        double[] array = new double[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                array[i * oldLength + j] = value.getDataAt(j);
            }
        }
        return RDataFactory.createDoubleVector(array, value.isComplete());
    }

    @Specialization(order = 111)
    @SuppressWarnings("unused")
    public RDoubleVector repeatL(RAbstractDoubleVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        double[] array = new double[lengthOut];
        for (int i = 0, j = 0; i < lengthOut; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        return RDataFactory.createDoubleVector(array, value.isComplete());
    }

    @Specialization(order = 120)
    @SuppressWarnings("unused")
    public RRawVector repeatT(RRawVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = oldLength * times;
        byte[] array = new byte[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                array[i * oldLength + j] = value.getDataAt(j).getValue();
            }
        }
        return RDataFactory.createRawVector(array);
    }

    @Specialization(order = 121)
    @SuppressWarnings("unused")
    public RRawVector repeatL(RRawVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        byte[] array = new byte[lengthOut];
        for (int i = 0, j = 0; i < lengthOut; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j).getValue();
        }
        return RDataFactory.createRawVector(array);
    }

    @Specialization(order = 130)
    @SuppressWarnings("unused")
    public RComplexVector repeatT(RComplexVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = value.getLength() * times;
        double[] array = new double[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                RComplex complex = value.getDataAt(j);
                int index = (i * oldLength + j) << 1;
                array[index] = complex.getRealPart();
                array[index + 1] = complex.getImaginaryPart();
            }
        }
        return RDataFactory.createComplexVector(array, value.isComplete());
    }

    @Specialization(order = 131)
    @SuppressWarnings("unused")
    public RComplexVector repeatL(RComplexVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        int length = lengthOut << 1;
        double[] array = new double[length];
        for (int i = 0, j = 0; i < length; i += 2, j = Utils.incMod(j, value.getLength())) {
            RComplex complex = value.getDataAt(j);
            array[i] = complex.getRealPart();
            array[i + 1] = complex.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, value.isComplete());
    }

    //
    // abstract specialisation for vector times arguments
    //

    @Specialization(order = 500)
    @SuppressWarnings("unused")
    public RAbstractVector repeatTV(RAbstractVector value, RIntVector times, RMissing lengthOut, Object each) {
        controlVisibility();
        if (value.getLength() != times.getLength()) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TIMES);
        }
        RVector valueMaterialized = value.materialize();
        RVector result = valueMaterialized.createEmptySameType(resultLength(times), valueMaterialized.isComplete());

        int w = 0; // write index
        for (int r = 0; r < valueMaterialized.getLength(); ++r) {
            for (int k = 0; k < times.getDataAt(r); ++k) {
                result.transferElementSameType(w++, valueMaterialized, r);
            }
        }

        return result;
    }

    //
    // auxiliary methods
    //

    private static int resultLength(RAbstractIntVector times) {
        int l = 0;
        for (int i = 0; i < times.getLength(); ++i) {
            l += times.getDataAt(i);
        }
        return l;
    }

}
