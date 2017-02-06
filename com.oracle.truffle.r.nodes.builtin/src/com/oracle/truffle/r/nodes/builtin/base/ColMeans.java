/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

//Implements .colMeans
@SuppressWarnings("unused")
@RBuiltin(name = "colMeans", kind = INTERNAL, parameterNames = {"X", "m", "n", "na.rm"}, behavior = PURE)
public abstract class ColMeans extends ColSumsBase {

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.createOperation();

    static {
        new ColSumsCasts(ColMeans.class);
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
