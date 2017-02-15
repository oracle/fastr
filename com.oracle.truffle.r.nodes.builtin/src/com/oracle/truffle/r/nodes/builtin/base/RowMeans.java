/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;

// Implements .rowMeans
@RBuiltin(name = "rowMeans", kind = INTERNAL, parameterNames = {"X", "m", "n", "na.rm"}, behavior = PURE)
public abstract class RowMeans extends RowSumsBase {

    static {
        createCasts(RowMeans.class);
    }

    @Specialization
    protected RDoubleVector rowMeans(RAbstractDoubleVector x, int rowNum, int colNum, boolean naRm) {
        return accumulateRows(x, rowNum, colNum, naRm, RowMeans::getMean, (v, nacheck, i) -> v.getDataAt(i));
    }

    @Specialization
    protected RDoubleVector rowMeans(RAbstractIntVector x, int rowNum, int colNum, boolean naRm) {
        return accumulateRows(x, rowNum, colNum, naRm, RowMeans::getMean, (v, nacheck, i) -> nacheck.convertIntToDouble(v.getDataAt(i)));
    }

    @Specialization
    protected RDoubleVector rowMeans(RAbstractLogicalVector x, int rowNum, int colNum, boolean naRm) {
        return accumulateRows(x, rowNum, colNum, naRm, RowMeans::getMean, (v, nacheck, i) -> nacheck.convertLogicalToDouble(v.getDataAt(i)));
    }

    private static double getMean(double sum, int notNACount) {
        if (Double.isNaN(sum)) {
            return sum;
        } else if (notNACount == 0) {
            return Double.NaN;
        } else {
            return sum / notNACount;
        }
    }
}
