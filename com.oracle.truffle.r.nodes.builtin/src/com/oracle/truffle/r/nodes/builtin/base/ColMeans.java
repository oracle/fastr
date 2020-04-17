/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates
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
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

//Implements .colMeans
@RBuiltin(name = "colMeans", kind = INTERNAL, parameterNames = {"X", "m", "n", "na.rm"}, behavior = PURE)
public abstract class ColMeans extends ColSumsBase {

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.createOperation();

    static {
        createCasts(ColMeans.class);
    }

    @Specialization(guards = "!naRm")
    protected RDoubleVector colMeansNaRmFalse(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansNaRmTrue(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansNaRmFalse(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansNaRmTrue(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansNaRmFalse(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
    protected RDoubleVector colMeansNaRmTrue(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") boolean naRm) {
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
