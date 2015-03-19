/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "colSums", kind = RBuiltinKind.INTERNAL, parameterNames = {"X", "m", "n", "na.rm"})
public abstract class ColSums extends RBuiltinNode {

    @Override
    public Object[] getDefaultParameterValues() {
        // X, m, n, na.rm = FALSE
        return new Object[]{RMissing.instance, RMissing.instance, RMissing.instance, RRuntime.LOGICAL_FALSE};
    }

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();

    private final NACheck na = NACheck.create();

    private final BinaryConditionProfile removeNA = (BinaryConditionProfile) ConditionProfile.createBinaryProfile();

    @CreateCast("arguments")
    protected RNode[] castArguments(RNode[] arguments) {
        arguments[1] = CastIntegerNodeGen.create(arguments[1], true, false, false);
        arguments[2] = CastIntegerNodeGen.create(arguments[2], true, false, false);
        return arguments;
    }

    @Specialization
    protected RDoubleVector colSums(RDoubleVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        double[] result = new double[colNum];
        boolean isComplete = true;
        final boolean rna = removeNA.profile(naRm == RRuntime.LOGICAL_TRUE);
        na.enable(x);
        double[] data = x.getDataWithoutCopying();
        int pos = 0;
        nextCol: for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            for (int i = 0; i < rowNum; ++i) {
                double el = data[pos++];
                if (rna) {
                    if (!na.check(el) && !Double.isNaN(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (na.check(el)) {
                        result[c] = RRuntime.DOUBLE_NA;
                        pos += rowNum - i - 1;
                        continue nextCol;
                    }
                    if (Double.isNaN(el)) {
                        result[c] = Double.NaN;
                        isComplete = false;
                        pos += rowNum - i - 1;
                        continue nextCol;
                    }
                    sum = add.op(sum, el);
                }
            }
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, removeNA.wasTrue() ? RDataFactory.COMPLETE_VECTOR : na.neverSeenNA() && isComplete);
    }

    @Specialization
    protected RDoubleVector colSums(RLogicalVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        double[] result = new double[colNum];
        final boolean rna = removeNA.profile(naRm == RRuntime.LOGICAL_TRUE);
        na.enable(x);
        byte[] data = x.getDataWithoutCopying();
        int pos = 0;
        nextCol: for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            for (int i = 0; i < rowNum; ++i) {
                byte el = data[pos++];
                if (rna) {
                    if (!na.check(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (na.check(el)) {
                        result[c] = RRuntime.DOUBLE_NA;
                        pos += rowNum - i - 1;
                        continue nextCol;
                    }
                    sum = add.op(sum, el);
                }
            }
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, removeNA.wasTrue() ? RDataFactory.COMPLETE_VECTOR : na.neverSeenNA());
    }

    @Specialization
    protected RDoubleVector colSums(RIntVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        double[] result = new double[colNum];
        final boolean rna = removeNA.profile(naRm == RRuntime.LOGICAL_TRUE);
        na.enable(x);
        int[] data = x.getDataWithoutCopying();
        int pos = 0;
        nextCol: for (int c = 0; c < colNum; ++c) {
            double sum = 0;
            for (int i = 0; i < rowNum; ++i) {
                int el = data[pos++];
                if (rna) {
                    if (!na.check(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (na.check(el)) {
                        result[c] = RRuntime.DOUBLE_NA;
                        pos += rowNum - i - 1;
                        continue nextCol;
                    }
                    sum = add.op(sum, el);
                }
            }
            result[c] = sum;
        }
        return RDataFactory.createDoubleVector(result, removeNA.wasTrue() ? RDataFactory.COMPLETE_VECTOR : na.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector colSums(RAbstractStringVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.X_NUMERIC);
    }
}
