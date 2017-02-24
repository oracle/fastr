/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARGUMENT;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Base class that provides arguments handling and validation helper methods and trivial cases
 * specializations shared between {@link RowSums}, {@link RowMeans}, {@link ColMeans},
 * {@link RowSums}.
 */
public abstract class ColSumsBase extends RBuiltinNode {

    protected final NACheck na = NACheck.create();
    private final ConditionProfile vectorLengthProfile = ConditionProfile.createBinaryProfile();

    protected static Casts createCasts(Class<? extends ColSumsBase> extCls) {
        Casts casts = new Casts(extCls);
        casts.arg("X").mustBe(numericValue(), RError.Message.X_NUMERIC);
        casts.arg("m").defaultError(INVALID_ARGUMENT, "n").asIntegerVector().findFirst().mustNotBeNA(RError.Message.VECTOR_SIZE_NA);
        casts.arg("n").defaultError(INVALID_ARGUMENT, "p").asIntegerVector().findFirst().mustNotBeNA(RError.Message.VECTOR_SIZE_NA);
        casts.arg("na.rm").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        return casts;
    }

    protected final void checkVectorLength(RAbstractVector x, int rowNum, int colNum) {
        if (vectorLengthProfile.profile(x.getLength() < rowNum * colNum)) {
            throw error(RError.Message.TOO_SHORT, "X");
        }
    }

    @Specialization(guards = {"rowNum == 0", "colNum == 0"})
    @SuppressWarnings("unused")
    protected static RDoubleVector doEmptyMatrix(Object x, int rowNum, int colNum, boolean naRm) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = "!naRm")
    protected final RDoubleVector doScalarNaRmFalse(double x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkLengthOne(rowNum, colNum);
        return RDataFactory.createDoubleVectorFromScalar(x);
    }

    @Specialization(guards = "naRm")
    protected final RDoubleVector doScalarNaRmTrue(double x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkLengthOne(rowNum, colNum);
        na.enable(x);
        if (!na.check(x) && !Double.isNaN(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(Double.NaN);
        }
    }

    @Specialization(guards = "!naRm")
    protected final RDoubleVector doScalarNaRmFalse(int x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkLengthOne(rowNum, colNum);
        na.enable(x);
        if (!na.check(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(RRuntime.DOUBLE_NA);
        }
    }

    @Specialization(guards = "naRm")
    protected final RDoubleVector doScalarNaRmTrue(int x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkLengthOne(rowNum, colNum);
        na.enable(x);
        if (!na.check(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(Double.NaN);
        }
    }

    @Specialization(guards = "!naRm")
    protected final RDoubleVector doScalarNaRmFalse(byte x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkLengthOne(rowNum, colNum);
        na.enable(x);
        if (!na.check(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(RRuntime.DOUBLE_NA);
        }
    }

    @Specialization(guards = "naRm")
    protected final RDoubleVector doScalarNaRmTrue(byte x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkLengthOne(rowNum, colNum);
        na.enable(x);
        if (!na.check(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(Double.NaN);
        }
    }

    private void checkLengthOne(int rowNum, int colNum) {
        if (vectorLengthProfile.profile(rowNum * colNum > 1)) {
            throw error(RError.Message.TOO_SHORT, "X");
        }
    }
}
