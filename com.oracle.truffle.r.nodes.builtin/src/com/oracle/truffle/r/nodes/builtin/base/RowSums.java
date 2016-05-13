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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "rowSums", kind = RBuiltinKind.INTERNAL, parameterNames = {"X", "m", "n", "na.rm"})
public abstract class RowSums extends RBuiltinNode {

    /*
     * this builtin unrolls the innermost loop (calculating multiple sums at once) to optimize cache
     * behavior.
     */
    private static final int UNROLL = 8;

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();

    private final NACheck na = NACheck.create();

    private final ConditionProfile removeNA = ConditionProfile.createBinaryProfile();
    private final ConditionProfile remainderProfile = ConditionProfile.createBinaryProfile();
    private final LoopConditionProfile outerProfile = LoopConditionProfile.createCountingProfile();
    private final LoopConditionProfile innerProfile = LoopConditionProfile.createCountingProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1).toInteger(2);
    }

    @FunctionalInterface
    private interface GetFunction<T extends RAbstractVector> {
        double get(T vector, NACheck na, int index);
    }

    private <T extends RAbstractVector> RDoubleVector performSums(T x, int rowNum, int colNum, byte naRm, GetFunction<T> get) {
        reportWork(x.getLength());
        double[] result = new double[rowNum];
        final boolean rna = removeNA.profile(naRm == RRuntime.LOGICAL_TRUE);
        na.enable(x);
        outerProfile.profileCounted(rowNum / 4);
        innerProfile.profileCounted(colNum);
        int i = 0;
        // the unrolled loop cannot handle NA values
        if (!na.isEnabled()) {
            while (outerProfile.inject(i <= rowNum - UNROLL)) {
                double[] sum = new double[UNROLL];
                int pos = i;
                for (int c = 0; innerProfile.inject(c < colNum); c++) {
                    for (int unroll = 0; unroll < UNROLL; unroll++) {
                        sum[unroll] = add.op(sum[unroll], get.get(x, na, pos + unroll));
                    }
                    pos += rowNum;
                }
                for (int unroll = 0; unroll < UNROLL; unroll++) {
                    result[i + unroll] = sum[unroll];
                }
                i += UNROLL;
            }
        }
        if (remainderProfile.profile(i < rowNum)) {
            while (i < rowNum) {
                double sum = 0;
                int pos = i;
                for (int c = 0; innerProfile.inject(c < colNum); c++) {
                    double el = get.get(x, na, pos);
                    pos += rowNum;
                    if (Double.isNaN(el)) {
                        // call check to make sure neverSeenNA is correct
                        na.check(el);
                        if (!rna) {
                            sum = el;
                            break;
                        }
                    } else {
                        sum = add.op(sum, el);
                    }
                }
                result[i] = sum;
                i++;
            }
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    @Specialization
    protected RDoubleVector rowSums(RAbstractDoubleVector x, int rowNum, int colNum, byte naRm) {
        return performSums(x, rowNum, colNum, naRm, (v, nacheck, i) -> v.getDataAt(i));
    }

    @Specialization
    protected RDoubleVector rowSums(RAbstractIntVector x, int rowNum, int colNum, byte naRm) {
        return performSums(x, rowNum, colNum, naRm, (v, nacheck, i) -> nacheck.convertIntToDouble(v.getDataAt(i)));
    }

    @Specialization
    protected RDoubleVector rowSums(RAbstractLogicalVector x, int rowNum, int colNum, byte naRm) {
        return performSums(x, rowNum, colNum, naRm, (v, nacheck, i) -> nacheck.convertLogicalToDouble(v.getDataAt(i)));
    }

    @SuppressWarnings("unused")
    @Specialization
    @TruffleBoundary
    protected RDoubleVector rowSums(RAbstractStringVector x, int rowNum, int colNum, byte naRm) {
        throw RError.error(this, RError.Message.X_NUMERIC);
    }
}
