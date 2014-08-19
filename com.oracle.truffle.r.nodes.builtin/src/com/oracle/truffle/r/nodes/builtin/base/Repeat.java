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
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

// FIXME honour times in case length.out is given but NA
// FIXME honour "each" parameter

@RBuiltin(name = "rep", kind = PRIMITIVE, parameterNames = {"x", "times", "length.out", "each"})
public abstract class Repeat extends RBuiltinNode {

    private final BranchProfile withNames = new BranchProfile();
    private final BranchProfile noNames = new BranchProfile();

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

    @Specialization
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

    @Specialization
    @SuppressWarnings("unused")
    public RIntVector repeat(int value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int[] array = new int[times];
        Arrays.fill(array, value);
        return RDataFactory.createIntVector(array, RRuntime.isComplete(value));
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public int repeatLengthNA(int value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RIntVector repeat(int value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization
    @SuppressWarnings("unused")
    public RDoubleVector repeat(double value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        double[] array = new double[times];
        Arrays.fill(array, value);
        return RDataFactory.createDoubleVector(array, RRuntime.isComplete(value));
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public double repeatLengthNA(double value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RDoubleVector repeat(double value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization
    @SuppressWarnings("unused")
    public RRawVector repeat(RRaw value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        byte[] array = new byte[times];
        Arrays.fill(array, value.getValue());
        return RDataFactory.createRawVector(array);
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public RRaw repeatLengthNA(RRaw value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RRawVector repeat(RRaw value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization
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

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public RComplex repeatLengthNA(RComplex value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RComplexVector repeat(RComplex value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization
    @SuppressWarnings("unused")
    public RStringVector repeat(String value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        String[] array = new String[times];
        Arrays.fill(array, value);
        return RDataFactory.createStringVector(array, !RRuntime.isNA(value));
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public String repeatLengthNA(String value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RStringVector repeat(String value, Object times, int lengthOut, Object each) {
        return repeat(value, lengthOut, RMissing.instance, each);
    }

    @Specialization
    @SuppressWarnings("unused")
    public RLogicalVector repeat(byte value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        byte[] array = new byte[times];
        Arrays.fill(array, value);
        return RDataFactory.createLogicalVector(array, value != RRuntime.LOGICAL_NA);
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public byte repeatLengthNA(byte value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
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

    private static RStringVector getNamesTimes(RAbstractVector value, int times) {
        int oldLength = value.getLength();
        int length = oldLength * times;
        String[] names = new String[length];
        RStringVector oldNames = (RStringVector) value.getNames();
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                names[i * oldLength + j] = oldNames.getDataAt(j);
            }
        }
        return RDataFactory.createStringVector(names, oldNames.isComplete());
    }

    private static RStringVector getNamesLength(RAbstractVector value, int lengthOut) {
        String[] names = new String[lengthOut];
        RStringVector oldNames = (RStringVector) value.getNames();
        for (int i = 0, j = 0; i < lengthOut; ++i, j = Utils.incMod(j, value.getLength())) {
            names[i] = oldNames.getDataAt(j);
        }
        return RDataFactory.createStringVector(names, oldNames.isComplete());
    }

    @Specialization
    @SuppressWarnings("unused")
    public RIntVector repeat(RAbstractIntVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = oldLength * times;
        int[] array = new int[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                array[i * oldLength + j] = value.getDataAt(j);
            }
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createIntVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createIntVector(array, value.isComplete(), getNamesTimes(value, times));
        }
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public RAbstractIntVector repeatLengthNA(RAbstractIntVector value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RIntVector repeat(RAbstractIntVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        int[] array = new int[lengthOut];
        for (int i = 0, j = 0; i < lengthOut; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createIntVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createIntVector(array, value.isComplete(), getNamesLength(value, lengthOut));
        }
    }

    @Specialization
    @SuppressWarnings("unused")
    public RDoubleVector repeat(RAbstractDoubleVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = value.getLength() * times;
        double[] array = new double[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                array[i * oldLength + j] = value.getDataAt(j);
            }
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createDoubleVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createDoubleVector(array, value.isComplete(), getNamesTimes(value, times));
        }
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public RAbstractDoubleVector repeatLengthNA(RAbstractDoubleVector value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RDoubleVector repeat(RAbstractDoubleVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        double[] array = new double[lengthOut];
        for (int i = 0, j = 0; i < lengthOut; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createDoubleVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createDoubleVector(array, value.isComplete(), getNamesLength(value, lengthOut));
        }
    }

    @Specialization
    @SuppressWarnings("unused")
    public RRawVector repeat(RRawVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = oldLength * times;
        byte[] array = new byte[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                array[i * oldLength + j] = value.getDataAt(j).getValue();
            }
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createRawVector(array);
        } else {
            withNames.enter();
            return RDataFactory.createRawVector(array, getNamesTimes(value, times));
        }
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public RRawVector repeatLengthNA(RRawVector value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RRawVector repeat(RRawVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        byte[] array = new byte[lengthOut];
        for (int i = 0, j = 0; i < lengthOut; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j).getValue();
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createRawVector(array);
        } else {
            withNames.enter();
            return RDataFactory.createRawVector(array, getNamesLength(value, lengthOut));
        }
    }

    @Specialization
    @SuppressWarnings("unused")
    public RComplexVector repeat(RComplexVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = value.getLength() * times;
        double[] array = new double[length << 1];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                RComplex complex = value.getDataAt(j);
                int index = (i * oldLength + j) << 1;
                array[index] = complex.getRealPart();
                array[index + 1] = complex.getImaginaryPart();
            }
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createComplexVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createComplexVector(array, value.isComplete(), getNamesTimes(value, times));
        }
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public RComplexVector repeatLengthNA(RComplexVector value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RComplexVector repeat(RComplexVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        int length = lengthOut << 1;
        double[] array = new double[length];
        for (int i = 0, j = 0; i < length; i += 2, j = Utils.incMod(j, value.getLength())) {
            RComplex complex = value.getDataAt(j);
            array[i] = complex.getRealPart();
            array[i + 1] = complex.getImaginaryPart();
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createComplexVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createComplexVector(array, value.isComplete(), getNamesLength(value, lengthOut));
        }
    }

    @Specialization
    @SuppressWarnings("unused")
    public RLogicalVector repeat(RAbstractLogicalVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = value.getLength() * times;
        byte[] array = new byte[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                array[i * oldLength + j] = value.getDataAt(j);
            }
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createLogicalVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createLogicalVector(array, value.isComplete(), getNamesTimes(value, times));
        }
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public RAbstractLogicalVector repeatLengthNA(RAbstractLogicalVector value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RLogicalVector repeat(RAbstractLogicalVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        byte[] array = new byte[lengthOut];
        for (int i = 0, j = 0; i < lengthOut; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createLogicalVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createLogicalVector(array, value.isComplete(), getNamesLength(value, lengthOut));
        }
    }

    @Specialization
    @SuppressWarnings("unused")
    public RStringVector repeat(RAbstractStringVector value, int times, RMissing lengthOut, Object each) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = value.getLength() * times;
        String[] array = new String[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                array[i * oldLength + j] = value.getDataAt(j);
            }
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createStringVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createStringVector(array, value.isComplete(), getNamesTimes(value, times));
        }
    }

    @Specialization(guards = "lengthNA")
    @SuppressWarnings("unused")
    public RAbstractStringVector repeatLengthNA(RAbstractStringVector value, Object times, int lengthOut, Object each) {
        return value;
    }

    @Specialization(guards = "!lengthNA")
    @SuppressWarnings("unused")
    public RStringVector repeat(RAbstractStringVector value, Object times, int lengthOut, Object each) {
        controlVisibility();
        String[] array = new String[lengthOut];
        for (int i = 0, j = 0; i < lengthOut; ++i, j = Utils.incMod(j, value.getLength())) {
            array[i] = value.getDataAt(j);
        }
        if (value.getNames() == RNull.instance) {
            noNames.enter();
            return RDataFactory.createStringVector(array, value.isComplete());
        } else {
            withNames.enter();
            return RDataFactory.createStringVector(array, value.isComplete(), getNamesLength(value, lengthOut));
        }
    }

    //
    // abstract specialisation for vector times arguments
    //

    private static RStringVector getNamesTimes(RAbstractVector value, RIntVector times, int newLength) {
        RStringVector oldNames = (RStringVector) value.getNames();
        RStringVector names = oldNames.createEmptySameType(newLength, oldNames.isComplete());
        int w = 0; // write index
        for (int r = 0; r < oldNames.getLength(); ++r) {
            for (int k = 0; k < times.getDataAt(r); ++k) {
                names.transferElementSameType(w++, oldNames, r);
            }
        }
        return names;
    }

    @Specialization
    @SuppressWarnings("unused")
    public RAbstractVector repeatTV(VirtualFrame frame, RAbstractVector value, RIntVector times, RMissing lengthOut, Object each) {
        controlVisibility();
        if (value.getLength() != times.getLength()) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "times");
        }
        RVector valueMaterialized = value.materialize();
        RVector result = valueMaterialized.createEmptySameType(resultLength(times), valueMaterialized.isComplete());

        int w = 0; // write index
        for (int r = 0; r < valueMaterialized.getLength(); ++r) {
            for (int k = 0; k < times.getDataAt(r); ++k) {
                result.transferElementSameType(w++, valueMaterialized, r);
            }
        }
        if (value.getNames() != RNull.instance) {
            withNames.enter();
            result.setNames(getNamesTimes(value, times, result.getLength()));
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

    @SuppressWarnings("unused")
    protected boolean lengthNA(int value, Object times, int lengthOut, Object each) {
        return RRuntime.isNA(lengthOut);
    }

    @SuppressWarnings("unused")
    protected boolean lengthNA(double value, Object times, int lengthOut, Object each) {
        return RRuntime.isNA(lengthOut);
    }

    @SuppressWarnings("unused")
    protected boolean lengthNA(RRaw value, Object times, int lengthOut, Object each) {
        return RRuntime.isNA(lengthOut);
    }

    @SuppressWarnings("unused")
    protected boolean lengthNA(RComplex value, Object times, int lengthOut, Object each) {
        return RRuntime.isNA(lengthOut);
    }

    @SuppressWarnings("unused")
    protected boolean lengthNA(String value, Object times, int lengthOut, Object each) {
        return RRuntime.isNA(lengthOut);
    }

    @SuppressWarnings("unused")
    protected boolean lengthNA(byte value, Object times, int lengthOut, Object each) {
        return RRuntime.isNA(lengthOut);
    }

    @SuppressWarnings("unused")
    protected boolean lengthNA(RAbstractVector value, Object times, int lengthOut, Object each) {
        return RRuntime.isNA(lengthOut);
    }

}
