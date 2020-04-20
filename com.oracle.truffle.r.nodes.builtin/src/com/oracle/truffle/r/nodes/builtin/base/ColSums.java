/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "colSums", kind = INTERNAL, parameterNames = {"X", "m", "n", "na.rm"}, behavior = PURE)
public abstract class ColSums extends ColSumsBase {

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.createOperation();

    private final ConditionProfile removeNA = ConditionProfile.createBinaryProfile();

    static {
        createCasts(ColSums.class);
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    protected RDoubleVector colSums(RDoubleVector x, int rowNum, int colNum, boolean rnaParam,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        checkVectorLength(xDataLib, x, rowNum, colNum);

        double[] result = new double[colNum];
        boolean isComplete = true;
        final boolean rna = removeNA.profile(rnaParam);

        int pos = 0;
        Object xData = x.getData();
        RandomAccessIterator xIt = xDataLib.randomAccessIterator(xData);
        NACheck naCheck = xDataLib.getNACheck(xData);
        nextCol: for (int c = 0; c < colNum; c++) {
            double sum = 0;
            for (int i = 0; i < rowNum; i++) {
                final double el = xDataLib.getDouble(xData, xIt, pos++);
                if (rna) {
                    if (!naCheck.check(el) && !Double.isNaN(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (naCheck.check(el)) {
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

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    protected RDoubleVector colSums(RLogicalVector x, int rowNum, int colNum, boolean rnaParam,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        checkVectorLength(xDataLib, x, rowNum, colNum);

        final boolean rna = removeNA.profile(rnaParam);
        double[] result = new double[colNum];
        boolean isComplete = true;
        int pos = 0;
        Object xData = x.getData();
        RandomAccessIterator xIt = xDataLib.randomAccessIterator(xData);
        NACheck naCheck = xDataLib.getNACheck(xData);
        nextCol: for (int c = 0; c < colNum; c++) {
            double sum = 0;
            for (int i = 0; i < rowNum; i++) {
                final byte el = xDataLib.getLogical(xData, xIt, pos++);
                if (rna) {
                    if (!naCheck.check(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (naCheck.check(el)) {
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

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    protected RDoubleVector colSums(RIntVector x, int rowNum, int colNum, boolean rnaParam,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        checkVectorLength(xDataLib, x, rowNum, colNum);

        final boolean rna = removeNA.profile(rnaParam);
        double[] result = new double[colNum];
        boolean isComplete = true;
        int pos = 0;
        Object xData = x.getData();
        RandomAccessIterator xIt = xDataLib.randomAccessIterator(xData);
        NACheck naCheck = xDataLib.getNACheck(xData);
        nextCol: for (int c = 0; c < colNum; c++) {
            double sum = 0;
            for (int i = 0; i < rowNum; i++) {
                final int el = xDataLib.getInt(xData, xIt, pos++);
                if (rna) {
                    if (!naCheck.check(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (naCheck.check(el)) {
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
