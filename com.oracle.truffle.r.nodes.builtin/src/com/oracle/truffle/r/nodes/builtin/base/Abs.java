/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.attributes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "abs", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class Abs extends RBuiltinNode {

    @Child private CopyOfRegAttributesNode copyAttributes;

    private final NACheck check = NACheck.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Specialization
    protected int abs(int value) {
        controlVisibility();
        check.enable(value);
        return performInt(value);
    }

    @Specialization
    protected int abs(byte value) {
        controlVisibility();
        check.enable(value);
        if (check.check(value)) {
            return RRuntime.INT_NA;
        }
        return Math.abs(value);
    }

    @Specialization
    protected double abs(double value) {
        controlVisibility();
        check.enable(value);
        return performDouble(value);
    }

    @Specialization
    protected double abs(RComplex value) {
        controlVisibility();
        check.enable(value);
        return performComplex(value);
    }

    private void copyRegAttributes(RAbstractVector source, RVector target) {
        if (copyAttributes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            copyAttributes = insert(CopyOfRegAttributesNodeGen.create());
        }
        copyAttributes.execute(source, target);
    }

    protected boolean isPositiveSequence(RIntSequence sequence) {
        return sequence.getStart() >= 0 && (sequence.getStart() + (sequence.getLength() - 1) * (long) sequence.getStride()) <= Integer.MAX_VALUE;
    }

    @Specialization(guards = "isPositiveSequence(vector)")
    protected RIntSequence doAbsIntSequence(RIntSequence vector) {
        controlVisibility();
        return vector;
    }

    @Specialization
    protected RIntVector doAbs(RAbstractIntVector vector) {
        controlVisibility();
        check.enable(vector);
        int[] intVector = new int[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            intVector[i] = performInt(vector.getDataAt(i));
        }
        RIntVector res = RDataFactory.createIntVector(intVector, check.neverSeenNA(), vector.getDimensions(), vector.getNames(attrProfiles));
        copyRegAttributes(vector, res);
        return res;
    }

    protected boolean isPositiveSequence(RDoubleSequence sequence) {
        return sequence.getStart() >= 0 && sequence.getStride() >= 0;
    }

    @Specialization(guards = "isPositiveSequence(vector)")
    protected RDoubleSequence doAbsDoubleSequence(RDoubleSequence vector) {
        controlVisibility();
        return vector;
    }

    @Specialization
    protected RDoubleVector abs(RAbstractDoubleVector vector) {
        controlVisibility();
        check.enable(vector);
        double[] doubleVector = new double[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            doubleVector[i] = performDouble(vector.getDataAt(i));
        }
        RDoubleVector res = RDataFactory.createDoubleVector(doubleVector, check.neverSeenNA(), vector.getDimensions(), vector.getNames(attrProfiles));
        copyRegAttributes(vector, res);
        return res;
    }

    @Specialization
    protected RDoubleVector abs(RAbstractComplexVector vector) {
        controlVisibility();
        check.enable(vector);
        double[] doubleVector = new double[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            doubleVector[i] = performComplex(vector.getDataAt(i));
        }
        RDoubleVector res = RDataFactory.createDoubleVector(doubleVector, check.neverSeenNA(), vector.getDimensions(), vector.getNames(attrProfiles));
        copyRegAttributes(vector, res);
        return res;
    }

    @Specialization
    protected RIntVector abs(RAbstractLogicalVector value) {
        controlVisibility();
        check.enable(value);
        return doAbs(RClosures.createLogicalToIntVector(value));
    }

    @Fallback
    @TruffleBoundary
    protected Object abs(@SuppressWarnings("unused") Object vector) {
        controlVisibility();
        throw RError.error(this, RError.Message.NON_NUMERIC_MATH);
    }

    private int performInt(int value) {
        if (check.check(value)) {
            return RRuntime.INT_NA;
        }
        return Math.abs(value);
    }

    private double performDouble(double value) {
        if (check.check(value)) {
            return RRuntime.DOUBLE_NA;
        }
        return Math.abs(value);
    }

    private double performComplex(RComplex value) {
        if (check.check(value)) {
            return RRuntime.DOUBLE_NA;
        }
        return value.abs();
    }
}
