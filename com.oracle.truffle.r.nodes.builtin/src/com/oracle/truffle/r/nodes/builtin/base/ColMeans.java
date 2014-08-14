/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
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

    @Override
    public RNode[] getParameterValues() {
        // X, m, n, na.rm = FALSE
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.FALSE)};
    }

    @Child protected BinaryArithmetic add = BinaryArithmetic.ADD.create();
    private final NACheck na = NACheck.create();

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false, false);
        arguments[2] = CastIntegerNodeFactory.create(arguments[2], true, false, false);
        return arguments;
    }

    @Specialization(guards = "!isNaRm", order = 0)
    public RDoubleVector colMeansNaRmFalse(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
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

    @Specialization(guards = "isNaRm", order = 1)
    public RDoubleVector colMeansNaRmTrue(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
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

    @Specialization(guards = "!isNaRm", order = 2)
    public RDoubleVector colMeansNaRmFalse(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
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

    @Specialization(guards = "isNaRm", order = 3)
    public RDoubleVector colMeansNaRmTrue(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
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

    @Specialization(guards = "!isNaRm", order = 4)
    public RDoubleVector colMeansNaRmFalse(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
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

    @Specialization(guards = "isNaRm", order = 5)
    public RDoubleVector colMeansNaRmTrue(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
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
    @Specialization(order = 6)
    public RDoubleVector colMeans(VirtualFrame frame, RAbstractStringVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.X_NUMERIC);
    }

    @SuppressWarnings("unused")
    protected boolean isNaRm(RDoubleVector x, int rowNum, int colNum, byte naRm) {
        return naRm == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    protected boolean isNaRm(RIntVector x, int rowNum, int colNum, byte naRm) {
        return naRm == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    protected boolean isNaRm(RLogicalVector x, int rowNum, int colNum, byte naRm) {
        return naRm == RRuntime.LOGICAL_TRUE;
    }
}
