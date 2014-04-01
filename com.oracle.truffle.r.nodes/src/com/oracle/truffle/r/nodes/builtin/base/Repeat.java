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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

// FIXME honour times in case length.out is given but NA
// FIXME honour "each" parameter

@RBuiltin("rep")
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

    @Child ConvertInt convertInt;

    private int coerceTimesOrLength(VirtualFrame frame, Object times, Object lengthOut) {
        // length.out supersedes times
        return coerce0(frame, lengthOut == RMissing.instance ? times : lengthOut);
    }

    private int coerce0(VirtualFrame frame, Object o) {
        if (convertInt == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            convertInt = insert(ConvertIntFactory.create(null));
        }
        try {
            return convertInt.executeInteger(frame, o);
        } catch (ConversionFailedException e) {
            CompilerDirectives.transferToInterpreter();
            throw RError.getInvalidTimes(getEncapsulatingSourceSection());
        }
    }

    @Specialization
    @SuppressWarnings("unused")
    public RNull repeat(VirtualFrame frame, RNull value, Object times, Object lengthOut, Object each) {
        controlVisibility();
        coerceTimesOrLength(frame, times, lengthOut);
        return RNull.instance;
    }

    @Specialization
    public RIntVector repeat(VirtualFrame frame, int value, Object timesObject, Object lengthOut, @SuppressWarnings("unused") Object each) {
        controlVisibility();
        int times = coerceTimesOrLength(frame, timesObject, lengthOut);
        int[] array = new int[times];
        Arrays.fill(array, value);
        return RDataFactory.createIntVector(array, RRuntime.isComplete(value));
    }

    @Specialization
    public RDoubleVector repeat(VirtualFrame frame, double value, Object timesObject, Object lengthOut, @SuppressWarnings("unused") Object each) {
        controlVisibility();
        int times = coerceTimesOrLength(frame, timesObject, lengthOut);
        double[] array = new double[times];
        Arrays.fill(array, value);
        return RDataFactory.createDoubleVector(array, RRuntime.isComplete(value));
    }

    @Specialization
    public RRawVector repeat(VirtualFrame frame, RRaw value, Object timesObject, Object lengthOut, @SuppressWarnings("unused") Object each) {
        controlVisibility();
        int times = coerceTimesOrLength(frame, timesObject, lengthOut);
        byte[] array = new byte[times];
        Arrays.fill(array, value.getValue());
        return RDataFactory.createRawVector(array);
    }

    @Specialization
    public RComplexVector repeat(VirtualFrame frame, RComplex value, Object timesObject, Object lengthOut, @SuppressWarnings("unused") Object each) {
        controlVisibility();
        int times = coerceTimesOrLength(frame, timesObject, lengthOut);
        double[] array = new double[times << 1];
        for (int i = 0; i < times; ++i) {
            int index = i << 1;
            array[index] = value.getRealPart();
            array[index + 1] = value.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, !value.isNA());
    }

    @Specialization
    public RStringVector repeat(VirtualFrame frame, String value, Object timesObject, Object lengthOut, @SuppressWarnings("unused") Object each) {
        controlVisibility();
        int times = coerceTimesOrLength(frame, timesObject, lengthOut);
        String[] array = new String[times];
        Arrays.fill(array, value);
        return RDataFactory.createStringVector(array, !RRuntime.isNA(value));
    }

    // All subsequent specialisations either use the times parameter, or the length.out parameter.
    // As the shape of the inner loop is different depending on which parameter is used, they are
    // specialised for parameter presence.

    @Specialization(order = 10, guards = "!lengthOutGiven")
    @SuppressWarnings("unused")
    public RIntVector repeatT(VirtualFrame frame, RIntVector value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        return performAbstractIntVectorT(value, coerce0(frame, timesObject));
    }

    @Specialization(order = 11, guards = "lengthOutGiven")
    @SuppressWarnings("unused")
    public RIntVector repeatL(VirtualFrame frame, RIntVector value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        return performAbstractIntVectorL(value, coerce0(frame, lengthOut));
    }

    @Specialization(order = 20, guards = "!lengthOutGiven")
    @SuppressWarnings("unused")
    public RIntVector repeatT(VirtualFrame frame, RIntSequence value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        return performAbstractIntVectorT(value, coerce0(frame, timesObject));
    }

    @Specialization(order = 21, guards = "lengthOutGiven")
    @SuppressWarnings("unused")
    public RIntVector repeatL(VirtualFrame frame, RIntSequence value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        return performAbstractIntVectorL(value, coerce0(frame, lengthOut));
    }

    @Specialization(order = 30, guards = "!lengthOutGiven")
    @SuppressWarnings("unused")
    public RDoubleVector repeatT(VirtualFrame frame, RDoubleVector value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        return performAbstractDoubleVectorT(value, coerce0(frame, timesObject));
    }

    @Specialization(order = 31, guards = "lengthOutGiven")
    @SuppressWarnings("unused")
    public RDoubleVector repeatL(VirtualFrame frame, RDoubleVector value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        return performAbstractDoubleVectorL(value, coerce0(frame, lengthOut));
    }

    @Specialization(order = 40, guards = "!lengthOutGiven")
    @SuppressWarnings("unused")
    public RDoubleVector repeatT(VirtualFrame frame, RDoubleSequence value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        return performAbstractDoubleVectorT(value, coerce0(frame, timesObject));
    }

    @Specialization(order = 41, guards = "lengthOutGiven")
    @SuppressWarnings("unused")
    public RDoubleVector repeatL(VirtualFrame frame, RDoubleSequence value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        return performAbstractDoubleVectorL(value, coerce0(frame, lengthOut));
    }

    @Specialization(order = 50, guards = "!lengthOutGiven")
    @SuppressWarnings("unused")
    public RRawVector repeatT(VirtualFrame frame, RRawVector value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        int times = coerce0(frame, timesObject);
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

    @Specialization(order = 51, guards = "lengthOutGiven")
    @SuppressWarnings("unused")
    public RRawVector repeatL(VirtualFrame frame, RRawVector value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        int length = coerce0(frame, lengthOut);
        byte[] array = new byte[length];
        for (int i = 0, j = 0; i < length; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j).getValue();
        }
        return RDataFactory.createRawVector(array);
    }

    @Specialization(order = 60, guards = "!lengthOutGiven")
    @SuppressWarnings("unused")
    public RComplexVector repeatT(VirtualFrame frame, RComplexVector value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        int times = coerce0(frame, timesObject);
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

    @Specialization(order = 61, guards = "lengthOutGiven")
    @SuppressWarnings("unused")
    public RComplexVector repeatL(VirtualFrame frame, RComplexVector value, Object timesObject, Object lengthOut, Object each) {
        controlVisibility();
        int length = coerce0(frame, lengthOut) << 1;
        double[] array = new double[length];
        for (int i = 0, j = 0; i < length; i += 2, j = Utils.incMod(j, value.getLength())) {
            RComplex complex = value.getDataAt(j);
            array[i] = complex.getRealPart();
            array[i + 1] = complex.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, value.isComplete());
    }

    private static RIntVector performAbstractIntVectorT(RAbstractIntVector value, int times) {
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

    private static RIntVector performAbstractIntVectorL(RAbstractIntVector value, int length) {
        int[] array = new int[length];
        for (int i = 0, j = 0; i < length; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        return RDataFactory.createIntVector(array, value.isComplete());
    }

    private static RDoubleVector performAbstractDoubleVectorT(RAbstractDoubleVector value, int times) {
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

    private static RDoubleVector performAbstractDoubleVectorL(RAbstractDoubleVector value, int length) {
        double[] array = new double[length];
        for (int i = 0, j = 0; i < length; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        return RDataFactory.createDoubleVector(array, value.isComplete());
    }

    @SuppressWarnings("unused")
    protected static boolean lengthOutGiven(Object x, Object timesObject, Object lengthOut, Object each) {
        return lengthOut != RMissing.instance;
    }

}
