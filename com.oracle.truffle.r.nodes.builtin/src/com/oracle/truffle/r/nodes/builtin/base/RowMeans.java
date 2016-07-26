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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// Implements .rowMeans
@RBuiltin(name = "rowMeans", kind = RBuiltinKind.INTERNAL, parameterNames = {"X", "m", "n", "na.rm"})
public abstract class RowMeans extends RBuiltinNode {

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();
    private final NACheck na = NACheck.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("m").asIntegerVector().findFirst().notNA();

        casts.arg("n").asIntegerVector().findFirst().notNA();

        casts.arg("na.rm").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization(guards = "!naRm")
    @TruffleBoundary
    protected RDoubleVector rowMeansNaRmFalse(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        double[] result = new double[rowNum];
        boolean isComplete = true;
        na.enable(x);
        nextRow: for (int i = 0; i < rowNum; i++) {
            double sum = 0;
            for (int c = 0; c < colNum; c++) {
                double el = x.getDataAt(c * rowNum + i);
                if (na.check(el)) {
                    result[i] = RRuntime.DOUBLE_NA;
                    continue nextRow;
                }
                if (Double.isNaN(el)) {
                    result[i] = Double.NaN;
                    isComplete = false;
                    continue nextRow;
                }
                sum = add.op(sum, el);
            }
            result[i] = sum / colNum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA() && isComplete);
    }

    @Specialization(guards = "naRm")
    @TruffleBoundary
    protected RDoubleVector rowMeansNaRmTrue(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        double[] result = new double[rowNum];
        boolean isComplete = true;
        na.enable(x);
        for (int i = 0; i < rowNum; i++) {
            double sum = 0;
            int nonNaNumCount = 0;
            for (int c = 0; c < colNum; c++) {
                double el = x.getDataAt(c * rowNum + i);
                if (!na.check(el) && !Double.isNaN(el)) {
                    sum = add.op(sum, el);
                    nonNaNumCount++;
                }
            }
            if (nonNaNumCount == 0) {
                result[i] = Double.NaN;
                isComplete = false;
            } else {
                result[i] = sum / nonNaNumCount;
            }
        }
        return RDataFactory.createDoubleVector(result, isComplete);
    }

    @Specialization(guards = "!naRm")
    @TruffleBoundary
    protected RDoubleVector rowMeansNaRmFalse(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        double[] result = new double[rowNum];
        na.enable(x);
        nextRow: for (int i = 0; i < rowNum; i++) {
            double sum = 0;
            for (int c = 0; c < colNum; c++) {
                byte el = x.getDataAt(c * rowNum + i);
                if (na.check(el)) {
                    result[i] = RRuntime.DOUBLE_NA;
                    continue nextRow;
                }
                sum = add.op(sum, el);
            }
            result[i] = sum / colNum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    @Specialization(guards = "naRm")
    @TruffleBoundary
    protected RDoubleVector rowMeansNaRmTrue(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        double[] result = new double[rowNum];
        boolean isComplete = true;
        na.enable(x);
        for (int i = 0; i < rowNum; i++) {
            double sum = 0;
            int nonNaNumCount = 0;
            for (int c = 0; c < colNum; c++) {
                byte el = x.getDataAt(c * rowNum + i);
                if (!na.check(el)) {
                    sum = add.op(sum, el);
                    nonNaNumCount++;
                }
            }
            if (nonNaNumCount == 0) {
                result[i] = Double.NaN;
                isComplete = false;
            } else {
                result[i] = sum / nonNaNumCount;
            }
        }
        return RDataFactory.createDoubleVector(result, isComplete);
    }

    @Specialization(guards = "!naRm")
    @TruffleBoundary
    protected RDoubleVector rowMeansNaRmFalse(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        double[] result = new double[rowNum];
        na.enable(x);
        nextRow: for (int i = 0; i < rowNum; i++) {
            double sum = 0;
            for (int c = 0; c < colNum; c++) {
                int el = x.getDataAt(c * rowNum + i);
                if (na.check(el)) {
                    result[i] = RRuntime.DOUBLE_NA;
                    continue nextRow;
                }
                sum = add.op(sum, el);
            }
            result[i] = sum / colNum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    @Specialization(guards = "naRm")
    @TruffleBoundary
    protected RDoubleVector rowMeansNaRmTrue(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
        double[] result = new double[rowNum];
        boolean isComplete = true;
        na.enable(x);
        for (int i = 0; i < rowNum; i++) {
            double sum = 0;
            int nonNaNumCount = 0;
            for (int c = 0; c < colNum; c++) {
                int el = x.getDataAt(c * rowNum + i);
                if (!na.check(el)) {
                    sum = add.op(sum, el);
                    nonNaNumCount++;
                }
            }
            if (nonNaNumCount == 0) {
                result[i] = Double.NaN;
                isComplete = false;
            } else {
                result[i] = sum / nonNaNumCount;
            }
        }
        return RDataFactory.createDoubleVector(result, isComplete);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector rowMeans(RAbstractStringVector x, int rowNum, int colNum, boolean naRm) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.X_NUMERIC);
    }

}
