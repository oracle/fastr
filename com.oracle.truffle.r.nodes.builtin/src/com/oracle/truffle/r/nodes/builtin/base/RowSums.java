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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.utilities.BinaryConditionProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

@RBuiltin(name = "rowSums", kind = RBuiltinKind.INTERNAL, parameterNames = {"X", "m", "n", "na.rm"})
public abstract class RowSums extends RBuiltinNode {

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();

    private final NACheck na = NACheck.create();

    private final BinaryConditionProfile removeNA = (BinaryConditionProfile) ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1).toInteger(2);
    }

    @Specialization
    @TruffleBoundary
    protected RDoubleVector rowSums(RDoubleVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        double[] result = new double[rowNum];
        boolean isComplete = true;
        final boolean rna = removeNA.profile(naRm == RRuntime.LOGICAL_TRUE);
        na.enable(x);
        nextRow: for (int i = 0; i < rowNum; i++) {
            double sum = 0;
            for (int c = 0; c < colNum; c++) {
                double el = x.getDataAt(c * rowNum + i);
                if (rna) {
                    if (!na.check(el) && !Double.isNaN(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (na.check(el)) {
                        result[i] = RRuntime.DOUBLE_NA;
                        continue nextRow;
                    }
                    if (Double.isNaN(el)) {
                        result[i] = Double.NaN;
                        isComplete = false;
                        continue nextRow;
                    }
                    sum = add.op(sum, el);
                }
            }
            result[i] = sum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA() && isComplete);
    }

    @Specialization
    @TruffleBoundary
    protected RDoubleVector rowSums(RLogicalVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        double[] result = new double[rowNum];
        final boolean rna = removeNA.profile(naRm == RRuntime.LOGICAL_TRUE);
        na.enable(x);
        nextRow: for (int i = 0; i < rowNum; i++) {
            double sum = 0;
            for (int c = 0; c < colNum; c++) {
                byte el = x.getDataAt(c * rowNum + i);
                if (rna) {
                    if (!na.check(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (na.check(el)) {
                        result[i] = RRuntime.DOUBLE_NA;
                        continue nextRow;
                    }
                    sum = add.op(sum, el);
                }
            }
            result[i] = sum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    @Specialization
    @TruffleBoundary
    protected RDoubleVector rowSums(RIntVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        double[] result = new double[rowNum];
        final boolean rna = removeNA.profile(naRm == RRuntime.LOGICAL_TRUE);
        na.enable(x);
        nextRow: for (int i = 0; i < rowNum; i++) {
            double sum = 0;
            for (int c = 0; c < colNum; c++) {
                int el = x.getDataAt(c * rowNum + i);
                if (rna) {
                    if (!na.check(el)) {
                        sum = add.op(sum, el);
                    }
                } else {
                    if (na.check(el)) {
                        result[i] = RRuntime.DOUBLE_NA;
                        continue nextRow;
                    }
                    sum = add.op(sum, el);
                }
            }
            result[i] = sum;
        }
        return RDataFactory.createDoubleVector(result, na.neverSeenNA());
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RDoubleVector rowSums(RAbstractStringVector x, int rowNum, int colNum, byte naRm) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.X_NUMERIC);
    }

}
