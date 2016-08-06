/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "colSums", kind = INTERNAL, parameterNames = {"X", "m", "n", "na.rm"}, behavior = PURE)
public abstract class ColSums extends RBuiltinNode {

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();

    private final NACheck na = NACheck.create();
    private final ConditionProfile removeNA = ConditionProfile.createBinaryProfile();
    private final ValueProfile concreteVectorProfile = ValueProfile.createClassProfile();
    private final ConditionProfile vectorLengthProfile = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("X").mustBe(numericValue(), RError.NO_CALLER, RError.Message.X_NUMERIC);

        casts.arg("m").defaultError(RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, "n").asIntegerVector().findFirst().notNA(RError.NO_CALLER, RError.Message.VECTOR_SIZE_NA);

        casts.arg("n").defaultError(RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, "p").asIntegerVector().findFirst().notNA(RError.NO_CALLER, RError.Message.VECTOR_SIZE_NA);

        casts.arg("na.rm").defaultError(RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, "na.rm").asLogicalVector().findFirst().notNA().map(toBoolean());
    }

    private void checkVectorLength(RAbstractVector x, int rowNum, int colNum) {
        if (vectorLengthProfile.profile(x.getLength() < rowNum * colNum)) {
            throw RError.error(RError.NO_CALLER, RError.Message.TOO_SHORT, "X");
        }
    }

    protected boolean isEmptyMatrix(int rowNum, int colNum) {
        return rowNum == 0 && colNum == 0;
    }

    @Specialization(guards = "isEmptyMatrix(rowNum, colNum)")
    @SuppressWarnings("unused")
    protected RDoubleVector colSumsEmptyMatrix(Object x, int rowNum, int colNum, boolean naRm) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = "!naRm")
    protected RDoubleVector colSumsScalarNaRmFalse(double x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        if (vectorLengthProfile.profile(rowNum * colNum > 1)) {
            throw RError.error(RError.NO_CALLER, RError.Message.TOO_SHORT, "X");
        }

        return RDataFactory.createDoubleVectorFromScalar(x);
    }

    @Specialization(guards = "naRm")
    protected RDoubleVector colSumsScalarNaRmTrue(double x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        if (vectorLengthProfile.profile(rowNum * colNum > 1)) {
            throw RError.error(RError.NO_CALLER, RError.Message.TOO_SHORT, "X");
        }

        na.enable(x);
        if (!na.check(x) && !Double.isNaN(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(Double.NaN);
        }
    }

    @Specialization(guards = "!naRm")
    protected RDoubleVector colSumsScalarNaRmFalse(int x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        if (vectorLengthProfile.profile(rowNum * colNum > 1)) {
            throw RError.error(RError.NO_CALLER, RError.Message.TOO_SHORT, "X");
        }

        na.enable(x);
        if (!na.check(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(RRuntime.DOUBLE_NA);
        }
    }

    @Specialization(guards = "naRm")
    protected RDoubleVector colSumsScalarNaRmTrue(int x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        if (vectorLengthProfile.profile(rowNum * colNum > 1)) {
            throw RError.error(RError.NO_CALLER, RError.Message.TOO_SHORT, "X");
        }

        na.enable(x);
        if (!na.check(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(Double.NaN);
        }
    }

    @Specialization(guards = "!naRm")
    protected RDoubleVector colSumsScalarNaRmFalse(byte x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        if (vectorLengthProfile.profile(rowNum * colNum > 1)) {
            throw RError.error(RError.NO_CALLER, RError.Message.TOO_SHORT, "X");
        }

        na.enable(x);
        if (!na.check(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(RRuntime.DOUBLE_NA);
        }
    }

    @Specialization(guards = "naRm")
    protected RDoubleVector colSumsScalarNaRmTrue(byte x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        if (vectorLengthProfile.profile(rowNum * colNum > 1)) {
            throw RError.error(RError.NO_CALLER, RError.Message.TOO_SHORT, "X");
        }

        na.enable(x);
        if (!na.check(x)) {
            return RDataFactory.createDoubleVectorFromScalar(x);
        } else {
            return RDataFactory.createDoubleVectorFromScalar(Double.NaN);
        }
    }

    @Specialization
    protected RDoubleVector colSums(RAbstractDoubleVector x, int rowNum, int colNum, boolean rnaParam) {
        checkVectorLength(x, rowNum, colNum);

        double[] result = new double[colNum];
        boolean isComplete = true;
        na.enable(x);
        final boolean rna = removeNA.profile(rnaParam);
        final RAbstractDoubleVector profiledX = concreteVectorProfile.profile(x);

        int pos = 0;
        nextCol: for (int c = 0; c < colNum; c++) {
            double sum = 0;
            for (int i = 0; i < rowNum; i++) {
                final double el = profiledX.getDataAt(pos++);
                if (rna) {
                    if (!na.check(el) && !Double.isNaN(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (na.check(el)) {
                        result[c] = RRuntime.DOUBLE_NA;
                        isComplete = false;
                        pos += rowNum - i - 1;
                        continue nextCol;
                    }
                    if (Double.isNaN(el)) {
                        result[c] = Double.NaN;
                        pos += rowNum - i - 1;
                        continue nextCol;
                    }
                    sum = add.op(sum, el);
                }
            }
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, isComplete);
    }

    @Specialization
    protected RDoubleVector colSums(RAbstractLogicalVector x, int rowNum, int colNum, boolean rna) {
        checkVectorLength(x, rowNum, colNum);

        double[] result = new double[colNum];
        boolean isComplete = true;
        na.enable(x);
        final RAbstractLogicalVector profiledX = concreteVectorProfile.profile(x);
        int pos = 0;
        nextCol: for (int c = 0; c < colNum; c++) {
            double sum = 0;
            for (int i = 0; i < rowNum; i++) {
                final byte el = profiledX.getDataAt(pos++);
                if (rna) {
                    if (!na.check(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (na.check(el)) {
                        result[c] = RRuntime.DOUBLE_NA;
                        pos += rowNum - i - 1;
                        isComplete = false;
                        continue nextCol;
                    }
                    sum = add.op(sum, el);
                }
            }
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, isComplete);
    }

    @Specialization
    protected RDoubleVector colSums(RAbstractIntVector x, int rowNum, int colNum, boolean rna) {
        checkVectorLength(x, rowNum, colNum);

        double[] result = new double[colNum];
        boolean isComplete = true;
        na.enable(x);
        final RAbstractIntVector profiledX = concreteVectorProfile.profile(x);
        int pos = 0;
        nextCol: for (int c = 0; c < colNum; c++) {
            double sum = 0;
            for (int i = 0; i < rowNum; i++) {
                final int el = profiledX.getDataAt(pos++);
                if (rna) {
                    if (!na.check(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (na.check(el)) {
                        result[c] = RRuntime.DOUBLE_NA;
                        pos += rowNum - i - 1;
                        isComplete = false;
                        continue nextCol;
                    }
                    sum = add.op(sum, el);
                }
            }
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, isComplete);
    }
}
