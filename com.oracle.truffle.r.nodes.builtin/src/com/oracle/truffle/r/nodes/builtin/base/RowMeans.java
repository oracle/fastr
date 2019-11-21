/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
import com.oracle.truffle.r.runtime.data.model.RIntVector;
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
    protected RDoubleVector rowMeans(RIntVector x, int rowNum, int colNum, boolean naRm) {
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
