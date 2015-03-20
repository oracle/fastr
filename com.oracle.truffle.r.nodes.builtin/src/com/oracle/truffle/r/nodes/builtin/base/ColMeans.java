/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

//Implements .colMeans
@RBuiltin(name = "colMeans", kind = RBuiltinKind.INTERNAL, parameterNames = {"X", "m", "n", "na.rm"})
public abstract class ColMeans extends RBuiltinNode {

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();
    private final NACheck na = NACheck.create();

    @CreateCast("arguments")
    protected RNode[] castArguments(RNode[] arguments) {
        arguments[1] = CastIntegerNodeGen.create(arguments[1], true, false, false);
        arguments[2] = CastIntegerNodeGen.create(arguments[2], true, false, false);
        return arguments;
    }

    @Specialization(guards = "!isNaRm(naRm)")
    protected RDoubleVector colMeansNaRmFalse(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
        double[] result = new double[colNum];
        boolean isComplete = true;
        na.enable(x);
        nextCol: for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            for (int i = 0; i < rowNum; ++i) {
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
        for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            int nonNaNumCount = 0;
            for (int i = 0; i < rowNum; ++i) {
                double el = x.getDataAt(c * rowNum + i);
                if (!na.check(el) && !Double.isNaN(el)) {
                    sum = add.op(sum, el);
                    ++nonNaNumCount;
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
        nextCol: for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            for (int i = 0; i < rowNum; ++i) {
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
        for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            int nonNaNumCount = 0;
            for (int i = 0; i < rowNum; ++i) {
                byte el = x.getDataAt(c * rowNum + i);
                if (!na.check(el)) {
                    sum = add.op(sum, el);
                    ++nonNaNumCount;
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
        nextCol: for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            for (int i = 0; i < rowNum; ++i) {
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
        for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            int nonNaNumCount = 0;
            for (int i = 0; i < rowNum; ++i) {
                int el = x.getDataAt(c * rowNum + i);
                if (!na.check(el)) {
                    sum = add.op(sum, el);
                    ++nonNaNumCount;
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
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.X_NUMERIC);
    }

    protected boolean isNaRm(byte naRm) {
        return naRm == RRuntime.LOGICAL_TRUE;
    }

}
