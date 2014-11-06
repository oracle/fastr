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
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "rep.int", kind = INTERNAL, parameterNames = {"x", "times"})
public abstract class RepeatInternal extends RBuiltinNode {

    private final ConditionProfile timesOne = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorProfile = BranchProfile.create();

    @CreateCast("arguments")
    protected RNode[] castStatusArgument(RNode[] arguments) {
        // times argument is at index 1
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false, false);
        return arguments;
    }

    @Specialization
    protected RDoubleVector repInt(double value, int times) {
        controlVisibility();
        double[] array = new double[times];
        Arrays.fill(array, value);
        return RDataFactory.createDoubleVector(array, RRuntime.isComplete(value));
    }

    @Specialization
    protected RRawVector repInt(RRaw value, int times) {
        controlVisibility();
        byte[] array = new byte[times];
        Arrays.fill(array, value.getValue());
        return RDataFactory.createRawVector(array);
    }

    @Specialization
    protected RIntVector repInt(RIntSequence value, int times) {
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

    @Specialization
    protected RDoubleVector repInt(RDoubleVector value, int times) {
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

    @Specialization(guards = "isTimesValid")
    protected RDoubleVector repInt(RAbstractDoubleVector value, RIntVector times) {
        controlVisibility();
        List<Double> result = new ArrayList<>();
        for (int i = 0; i < value.getLength(); i++) {
            for (int j = 0; j < times.getDataAt(i); ++j) {
                result.add(value.getDataAt(i));
            }
        }
        double[] ans = new double[result.size()];
        for (int i = 0; i < ans.length; ++i) {
            ans[i] = result.get(i);
        }
        return RDataFactory.createDoubleVector(ans, value.isComplete());
    }

    @Specialization(guards = "isTimesValid")
    protected RIntVector repInt(RAbstractIntVector value, RIntVector times) {
        controlVisibility();
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < value.getLength(); i++) {
            for (int j = 0; j < times.getDataAt(i); ++j) {
                result.add(value.getDataAt(i));
            }
        }
        int[] ans = new int[result.size()];
        for (int i = 0; i < ans.length; ++i) {
            ans[i] = result.get(i);
        }
        return RDataFactory.createIntVector(ans, value.isComplete());
    }

    @Specialization
    protected RIntVector repInt(int value, int times) {
        controlVisibility();
        int[] array = new int[times];
        Arrays.fill(array, value);
        return RDataFactory.createIntVector(array, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RStringVector repInt(String value, int times) {
        controlVisibility();
        String[] array = new String[times];
        Arrays.fill(array, value);
        return RDataFactory.createStringVector(array, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RStringVector repInt(RStringVector value, RIntVector timesVec) {
        controlVisibility();
        int valueLength = value.getLength();
        int times = timesVec.getLength();
        if (!(times == 1 || times == valueLength)) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_TIMES_ARG);
        }
        String[] array;
        if (timesOne.profile(times == 1)) {
            int length = value.getLength() * times;
            array = new String[length];
            for (int i = 0; i < times; i++) {
                for (int j = 0; j < valueLength; ++j) {
                    array[i * valueLength + j] = value.getDataAt(j);
                }
            }
        } else {
            int length = 0;
            for (int k = 0; k < times; k++) {
                length = length + timesVec.getDataAt(k);
            }
            array = new String[length];
            int arrayIndex = 0;
            for (int i = 0; i < valueLength; i++) {
                String s = value.getDataAt(i);
                int timesLen = timesVec.getDataAt(i);
                for (int k = 0; k < timesLen; k++) {
                    array[arrayIndex++] = s;
                }
            }
        }
        return RDataFactory.createStringVector(array, value.isComplete());
    }

    @Specialization
    protected RList repList(RList value, int times) {
        controlVisibility();
        int oldLength = value.getLength();
        int length = value.getLength() * times;
        Object[] array = new Object[length];
        for (int i = 0; i < times; i++) {
            for (int j = 0; j < oldLength; ++j) {
                array[i * oldLength + j] = value.getDataAt(j);
            }
        }
        return RDataFactory.createList(array);
    }

    protected boolean isTimesValid(RAbstractVector value, RIntVector times) {
        if (value.getLength() != times.getLength()) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_VALUE, "times");
        }
        return true;
    }
}
