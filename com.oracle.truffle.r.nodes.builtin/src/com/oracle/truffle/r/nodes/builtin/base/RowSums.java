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
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;

@RBuiltin(name = "rowSums", kind = INTERNAL, parameterNames = {"X", "m", "n", "na.rm"}, behavior = PURE)
public abstract class RowSums extends RowSumsBase {

    static {
        createCasts(RowSums.class);
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    protected RDoubleVector rowSums(RDoubleVector x, int rowNum, int colNum, boolean naRm,
                    @CachedLibrary("x.getData()") VectorDataLibrary dataLib) {
        return accumulateRows(dataLib, x.getData(), rowNum, colNum, naRm, SelectSum.INSTANCE);
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    protected RDoubleVector rowSums(RIntVector x, int rowNum, int colNum, boolean naRm,
                    @CachedLibrary("x.getData()") VectorDataLibrary dataLib) {
        return accumulateRows(dataLib, x.getData(), rowNum, colNum, naRm, SelectSum.INSTANCE);
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    protected RDoubleVector rowSums(RLogicalVector x, int rowNum, int colNum, boolean naRm,
                    @CachedLibrary("x.getData()") VectorDataLibrary dataLib) {
        return accumulateRows(dataLib, x.getData(), rowNum, colNum, naRm, SelectSum.INSTANCE);
    }

    private static final class SelectSum extends FinalTransform {
        private static final SelectSum INSTANCE = new SelectSum();

        @Override
        public double get(double sum, int notNACount) {
            return sum;
        }
    }
}
