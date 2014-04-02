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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin("rep.int")
public abstract class RepeatInternal extends RBuiltinNode {

    @CreateCast("arguments")
    protected RNode[] castStatusArgument(RNode[] arguments) {
        // times argument is at index 1
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false);
        return arguments;
    }

    @Specialization
    public RDoubleVector repInt(double value, int times) {
        controlVisibility();
        double[] array = new double[times];
        Arrays.fill(array, value);
        return RDataFactory.createDoubleVector(array, RRuntime.isComplete(value));
    }

    @Specialization
    public RRawVector repInt(RRaw value, int times) {
        controlVisibility();
        byte[] array = new byte[times];
        Arrays.fill(array, value.getValue());
        return RDataFactory.createRawVector(array);
    }

    @Specialization
    public RIntVector repInt(RIntSequence value, int times) {
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
    public RDoubleVector repInt(RDoubleVector value, int times) {
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

    @Specialization
    public RIntVector repInt(int value, int times) {
        controlVisibility();
        int[] array = new int[times];
        Arrays.fill(array, value);
        return RDataFactory.createIntVector(array, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    public RStringVector repInt(String value, int times) {
        controlVisibility();
        String[] array = new String[times];
        Arrays.fill(array, value);
        return RDataFactory.createStringVector(array, RDataFactory.COMPLETE_VECTOR);
    }

}
