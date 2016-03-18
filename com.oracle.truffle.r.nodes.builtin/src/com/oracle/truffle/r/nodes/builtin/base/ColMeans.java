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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

//Implements .colMeans
@RBuiltin(name = "colMeans", kind = RBuiltinKind.INTERNAL, parameterNames = {"X", "m", "n", "na.rm"})
public abstract class ColMeans extends RBuiltinNode {

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();
    private final NACheck na = NACheck.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
        casts.toInteger(2);
    }

    @Specialization(guards = "!isNaRm(naRm)")
    protected RDoubleVector colMeansNaRmFalse(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
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

    @Specialization(guards = "isNaRm(naRm)")
    protected RDoubleVector colMeansNaRmTrue(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
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

    @Specialization(guards = "!isNaRm(naRm)")
    protected RDoubleVector colMeansNaRmFalse(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
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

    @Specialization(guards = "isNaRm(naRm)")
    protected RDoubleVector colMeansNaRmTrue(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
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

    @Specialization(guards = "!isNaRm(naRm)")
    protected RDoubleVector colMeansNaRmFalse(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
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

    @Specialization(guards = "isNaRm(naRm)")
    protected RDoubleVector colMeansNaRmTrue(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
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

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector colMeans(RAbstractStringVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        throw RError.error(this, RError.Message.X_NUMERIC);
    }

    protected boolean isNaRm(byte naRm) {
        return naRm == RRuntime.LOGICAL_TRUE;
    }
}
