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

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Implements a skeleton of an algorithm that traverses rows and accumulates their values.
 */
public abstract class RowSumsBase extends ColSumsBase {

    /*
     * this builtin unrolls the innermost loop (calculating multiple sums at once) to optimize cache
     * behavior.
     */
    private static final int UNROLL = 8;

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.createOperation();

    private final ConditionProfile remainderProfile = ConditionProfile.createBinaryProfile();
    private final LoopConditionProfile outerProfile = LoopConditionProfile.createCountingProfile();
    private final LoopConditionProfile innerProfile = LoopConditionProfile.createCountingProfile();

    protected abstract static class FinalTransform {
        abstract double get(double sum, int notNACount);
    }

    protected final RDoubleVector accumulateRows(VectorDataLibrary dataLib, Object data, int rowNum, int colNum, boolean naRm, FinalTransform finalTransform) {
        reportWork(dataLib.getLength(data));
        double[] result = new double[rowNum];
        outerProfile.profileCounted(rowNum / 4);
        innerProfile.profileCounted(colNum);
        int i = 0;
        RandomAccessIterator it = dataLib.randomAccessIterator(data);
        // the unrolled loop cannot handle NA values
        NACheck na = dataLib.getNACheck(data);
        if (!na.isEnabled()) {
            while (outerProfile.inject(i <= rowNum - UNROLL)) {
                double[] sum = new double[UNROLL];
                int pos = i;
                for (int c = 0; innerProfile.inject(c < colNum); c++) {
                    for (int unroll = 0; unroll < UNROLL; unroll++) {
                        sum[unroll] = add.op(sum[unroll], dataLib.getDouble(data, it, pos + unroll));
                    }
                    pos += rowNum;
                }
                for (int unroll = 0; unroll < UNROLL; unroll++) {
                    result[i + unroll] = finalTransform.get(sum[unroll], colNum);
                }
                i += UNROLL;
            }
        }
        if (remainderProfile.profile(i < rowNum)) {
            while (i < rowNum) {
                double sum = 0;
                int pos = i;
                int notNACount = 0;
                for (int c = 0; innerProfile.inject(c < colNum); c++) {
                    double el = dataLib.getDouble(data, it, pos);
                    pos += rowNum;
                    if (na.check(el)) {
                        if (!naRm) {
                            sum = RRuntime.DOUBLE_NA;
                            break;
                        }
                    } else if (Double.isNaN(el)) {
                        if (!naRm) {
                            sum = Double.NaN;
                            break;
                        }
                    } else {
                        sum = add.op(sum, el);
                        notNACount++;
                    }
                }
                result[i] = finalTransform.get(sum, notNACount);
                i++;
            }
        }
        return RDataFactory.createDoubleVector(result, dataLib.getNACheck(data).neverSeenNA());
    }
}
