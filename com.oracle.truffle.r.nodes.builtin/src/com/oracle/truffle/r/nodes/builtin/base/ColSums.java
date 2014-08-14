/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

//Implements .colSums
@RBuiltin(name = "colSums", kind = RBuiltinKind.INTERNAL, parameterNames = {"X", "m", "n", "na.rm"})
public abstract class ColSums extends RBuiltinNode {

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
    public RDoubleVector colSumsNaRmFalse(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
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
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA() && isComplete);
    }

    @Specialization(guards = "isNaRm", order = 1)
    public RDoubleVector colSumsNaRmTrue(RDoubleVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
        double[] result = new double[colNum];
        na.enable(x);
        for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            for (int i = 0; i < rowNum; ++i) {
                double el = x.getDataAt(c * rowNum + i);
                if (!na.check(el) && !Double.isNaN(el)) {
                    sum = add.op(sum, el);
                }
            }
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "!isNaRm", order = 2)
    public RDoubleVector colSumsNaRmFalse(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
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
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    @Specialization(guards = "isNaRm", order = 3)
    public RDoubleVector colSumsNaRmTrue(RLogicalVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
        double[] result = new double[colNum];
        na.enable(x);
        for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            for (int i = 0; i < rowNum; ++i) {
                byte el = x.getDataAt(c * rowNum + i);
                if (!na.check(el)) {
                    sum = add.op(sum, el);
                }
            }
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "!isNaRm", order = 4)
    public RDoubleVector colSumsNaRmFalse(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
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
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    @Specialization(guards = "isNaRm", order = 5)
    public RDoubleVector colSumsNaRmTrue(RIntVector x, int rowNum, int colNum, @SuppressWarnings("unused") byte naRm) {
        controlVisibility();
        double[] result = new double[colNum];
        na.enable(x);
        for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            for (int i = 0; i < rowNum; ++i) {
                int el = x.getDataAt(c * rowNum + i);
                if (!na.check(el)) {
                    sum = add.op(sum, el);
                }
            }
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 6)
    public RDoubleVector colSums(VirtualFrame frame, RAbstractStringVector x, int rowNum, int colNum, byte naRm) {
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
