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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "log2", kind = PRIMITIVE)
public abstract class Log2 extends RBuiltinNode {

    private static final double log2value = Math.log(2);

    @SuppressWarnings("unused")
    @Specialization
    public RNull log(RNull x) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.getNonNumericArgumentFunction(this.getEncapsulatingSourceSection());
    }

    @Specialization
    public double log2(int value) {
        controlVisibility();
        return log2((double) value);
    }

    @Specialization
    public double log2(double value) {
        controlVisibility();
        return Math.log(value) / log2value;
    }

    @Specialization
    public RDoubleVector log2(RIntVector vector) {
        controlVisibility();
        double[] resultVector = new double[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            int inputValue = vector.getDataAt(i);
            double result = RRuntime.DOUBLE_NA;
            if (RRuntime.isComplete(inputValue)) {
                result = log2(inputValue);
            }
            resultVector[i] = result;
        }
        return RDataFactory.createDoubleVector(resultVector, vector.isComplete());
    }

    @Specialization
    public RDoubleVector log2(RDoubleVector vector) {
        controlVisibility();
        double[] doubleVector = new double[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            double value = vector.getDataAt(i);
            if (RRuntime.isComplete(value)) {
                value = log2(value);
            }
            doubleVector[i] = value;
        }
        return RDataFactory.createDoubleVector(doubleVector, vector.isComplete());
    }
}
