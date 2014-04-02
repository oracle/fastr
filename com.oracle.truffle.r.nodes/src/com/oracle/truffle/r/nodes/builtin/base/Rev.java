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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin({"rev", "rev.default"})
@SuppressWarnings("unused")
public abstract class Rev extends RBuiltinNode {

    @Specialization
    public int rev(int value) {
        controlVisibility();
        return value;
    }

    @Specialization()
    public double rev(double value) {
        controlVisibility();
        return value;
    }

    @Specialization
    public byte rev(byte value) {
        controlVisibility();
        return value;
    }

    @Specialization
    public String rev(String value) {
        controlVisibility();
        return value;
    }

    @Specialization
    public RComplex asInteger(RComplex value) {
        controlVisibility();
        return value;
    }

    @Specialization
    public RRaw asInteger(RRaw value) {
        controlVisibility();
        return value;
    }

    @Specialization
    public RNull asInteger(RNull value) {
        controlVisibility();
        return value;
    }

    @Specialization
    public RIntVector rev(RIntVector vector) {
        controlVisibility();
        int len = vector.getLength();
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = vector.getDataAt(len - 1 - i);
        }
        return RDataFactory.createIntVector(result, vector.isComplete());
    }

    @Specialization
    public RDoubleVector rev(RDoubleVector vector) {
        controlVisibility();
        int len = vector.getLength();
        double[] result = new double[len];
        for (int i = 0; i < len; i++) {
            result[i] = vector.getDataAt(len - 1 - i);
        }
        return RDataFactory.createDoubleVector(result, vector.isComplete());
    }

    @Specialization
    public RStringVector rev(RStringVector vector) {
        controlVisibility();
        int len = vector.getLength();
        String[] result = new String[len];
        for (int i = 0; i < len; i++) {
            result[i] = vector.getDataAt(len - 1 - i);
        }
        return RDataFactory.createStringVector(result, vector.isComplete());
    }

    @Specialization
    public RLogicalVector rev(RLogicalVector vector) {
        controlVisibility();
        int len = vector.getLength();
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = vector.getDataAt(len - 1 - i);
        }
        return RDataFactory.createLogicalVector(result, vector.isComplete());
    }

    @Specialization
    public RComplexVector rev(RComplexVector vector) {
        controlVisibility();
        int len = vector.getLength();
        double[] result = new double[len * 2];
        for (int i = 0; i < len; i++) {
            int index = i << 1;
            RComplex data = vector.getDataAt(len - 1 - i);
            result[index] = data.getRealPart();
            result[index + 1] = data.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(result, vector.isComplete());
    }

    @Specialization
    public RRawVector rev(RRawVector vector) {
        controlVisibility();
        int len = vector.getLength();
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = vector.getDataAt(len - 1 - i).getValue();
        }
        return RDataFactory.createRawVector(result);
    }

    @Specialization
    public RIntSequence rev(RIntSequence sequence) {
        controlVisibility();
        int start = sequence.getStart() + (sequence.getLength() - 1) * sequence.getStride();
        return RDataFactory.createIntSequence(start, -sequence.getStride(), sequence.getLength());
    }

    @Specialization
    public RDoubleSequence rev(RDoubleSequence sequence) {
        controlVisibility();
        double start = sequence.getStart() + (sequence.getLength() - 1) * sequence.getStride();
        return RDataFactory.createDoubleSequence(start, -sequence.getStride(), sequence.getLength());
    }
}
