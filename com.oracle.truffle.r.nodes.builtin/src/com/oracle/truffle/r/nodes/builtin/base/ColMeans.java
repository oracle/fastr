/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
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
import com.sun.scenario.effect.impl.Renderer.RendererState;

//Implements .colMeans
@RBuiltin(name = "colMeans", kind = INTERNAL, parameterNames = {"X", "m", "n", "na.rm"}, behavior = PURE)
public abstract class ColMeans extends RBuiltinNode {

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();

    private final NACheck na = NACheck.create();
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
    protected RDoubleVector colMeansEmptyMatrix(Object x, int rowNum, int colNum, boolean naRm) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = "!naRm")
    protected RDoubleVector colMeansScalarNaRmFalse(double x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        if (vectorLengthProfile.profile(rowNum * colNum > 1)) {
            throw RError.error(RError.NO_CALLER, RError.Message.TOO_SHORT, "X");
        }

        return RDataFactory.createDoubleVectorFromScalar(x);
    }

    @Specialization(guards = "naRm")
    protected RDoubleVector colMeansScalarNaRmTrue(double x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansScalarNaRmFalse(int x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansScalarNaRmTrue(int x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansScalarNaRmFalse(byte x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansScalarNaRmTrue(byte x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansNaRmFalse(RAbstractDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkVectorLength(x, rowNum, colNum);

        double[] result = new double[colNum];
        boolean isComplete = true;
        na.enable(x);
        nextCol: for (int c = 0; c < colNum; c++) {
            double sum = 0;
            for (int i = 0; i < rowNum; i++) {
                double el = x.getDataAt(c * rowNum + i);
                if (na.check(el)) {
                    result[c] = RRuntime.DOUBLE_NA;
                    continue nextCol;
                }
                if (Double.isNaN(el)) {
                    result[c] = Double.NaN;
                    isComplete = false;
                    continue nextCol;
                }
                sum = add.op(sum, el);
            }
            result[c] = sum / rowNum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA() && isComplete);
    }

    @Specialization(guards = "naRm")
    protected RDoubleVector colMeansNaRmTrue(RAbstractDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkVectorLength(x, rowNum, colNum);

        double[] result = new double[colNum];
        boolean isComplete = true;
        na.enable(x);
        for (int c = 0; c < colNum; c++) {
            double sum = 0;
            int nonNaNumCount = 0;
            for (int i = 0; i < rowNum; i++) {
                double el = x.getDataAt(c * rowNum + i);
                if (!na.check(el) && !Double.isNaN(el)) {
                    sum = add.op(sum, el);
                    nonNaNumCount++;
                }
            }
            if (nonNaNumCount == 0) {
                result[c] = Double.NaN;
                isComplete = false;
            } else {
                result[c] = sum / nonNaNumCount;
            }
        }
        return RDataFactory.createDoubleVector(result, isComplete);
    }

    @Specialization(guards = "!naRm")
    protected RDoubleVector colMeansNaRmFalse(RAbstractLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkVectorLength(x, rowNum, colNum);

        double[] result = new double[colNum];
        na.enable(x);
        nextCol: for (int c = 0; c < colNum; c++) {
            double sum = 0;
            for (int i = 0; i < rowNum; i++) {
                byte el = x.getDataAt(c * rowNum + i);
                if (na.check(el)) {
                    result[c] = RRuntime.DOUBLE_NA;
                    continue nextCol;
                }
                sum = add.op(sum, el);
            }
            result[c] = sum / rowNum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    @Specialization(guards = "naRm")
    protected RDoubleVector colMeansNaRmTrue(RAbstractLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkVectorLength(x, rowNum, colNum);

        double[] result = new double[colNum];
        boolean isComplete = true;
        na.enable(x);
        for (int c = 0; c < colNum; c++) {
            double sum = 0;
            int nonNaNumCount = 0;
            for (int i = 0; i < rowNum; i++) {
                byte el = x.getDataAt(c * rowNum + i);
                if (!na.check(el)) {
                    sum = add.op(sum, el);
                    nonNaNumCount++;
                }
            }
            if (nonNaNumCount == 0) {
                result[c] = Double.NaN;
                isComplete = false;
            } else {
                result[c] = sum / nonNaNumCount;
            }
        }
        return RDataFactory.createDoubleVector(result, isComplete);
    }

    @Specialization(guards = "!naRm")
    protected RDoubleVector colMeansNaRmFalse(RAbstractIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkVectorLength(x, rowNum, colNum);

        double[] result = new double[colNum];
        na.enable(x);
        nextCol: for (int c = 0; c < colNum; c++) {
            double sum = 0;
            for (int i = 0; i < rowNum; i++) {
                int el = x.getDataAt(c * rowNum + i);
                if (na.check(el)) {
                    result[c] = RRuntime.DOUBLE_NA;
                    continue nextCol;
                }
                sum = add.op(sum, el);
            }
            result[c] = sum / rowNum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    @Specialization(guards = "naRm")
    protected RDoubleVector colMeansNaRmTrue(RAbstractIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        checkVectorLength(x, rowNum, colNum);

        double[] result = new double[colNum];
        boolean isComplete = true;
        na.enable(x);
        for (int c = 0; c < colNum; c++) {
            double sum = 0;
            int nonNaNumCount = 0;
            for (int i = 0; i < rowNum; i++) {
                int el = x.getDataAt(c * rowNum + i);
                if (!na.check(el)) {
                    sum = add.op(sum, el);
                    nonNaNumCount++;
                }
            }
            if (nonNaNumCount == 0) {
                result[c] = Double.NaN;
                isComplete = false;
            } else {
                result[c] = sum / nonNaNumCount;
            }
        }
        return RDataFactory.createDoubleVector(result, isComplete);
    }
}
