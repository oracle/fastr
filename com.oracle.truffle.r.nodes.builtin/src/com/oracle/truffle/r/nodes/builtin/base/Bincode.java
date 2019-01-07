/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "bincode", kind = INTERNAL, parameterNames = {"x", "breaks", "right", "include.lowest"}, behavior = PURE)
public abstract class Bincode extends RBuiltinNode.Arg4 {

    private final NACheck naCheck = NACheck.create();

    static {
        Casts casts = new Casts(Bincode.class);
        casts.arg("x").mustNotBeMissing(RError.Message.ARGUMENT_EMPTY, 1).asDoubleVector();

        casts.arg("breaks").mustNotBeMissing(RError.Message.ARGUMENT_EMPTY, 2).mustNotBeNull(RError.Message.LONG_VECTOR_NOT_SUPPORTED, "breaks").asDoubleVector();

        casts.arg("right").asLogicalVector().findFirst().map(toBoolean());

        casts.arg("include.lowest").asLogicalVector().findFirst().map(toBoolean());
    }

    @SuppressWarnings("unused")
    @Specialization
    RIntVector formatC(RNull x, RAbstractDoubleVector breaks, boolean right, boolean includeBorder) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization
    RIntVector formatC(RAbstractDoubleVector x, RAbstractDoubleVector breaks, boolean right, boolean includeBorder) {
        int n = x.getLength();
        int nb = breaks.getLength();

        int lo;
        int hi;
        int nb1 = nb - 1;
        int newVal;

        boolean lft = !right;

        /* This relies on breaks being sorted, so wise to check that */
        for (int i = 1; i < nb; i++) {
            if (breaks.getDataAt(i - 1) > breaks.getDataAt(i)) {
                throw error(RError.Message.GENERIC, "'breaks' is not sorted");
            }
        }

        naCheck.enable(true);
        int[] data = new int[n];
        boolean complete = true;
        for (int i = 0; i < n; i++) {
            data[i] = RRuntime.INT_NA;
            if (!naCheck.check(x.getDataAt(i))) {
                lo = 0;
                hi = nb1;
                if (x.getDataAt(i) < breaks.getDataAt(lo) || breaks.getDataAt(hi) < x.getDataAt(i) || (x.getDataAt(i) == breaks.getDataAt(lft ? hi : lo) && !includeBorder)) {
                    complete = false;
                    continue;
                } else {
                    while (hi - lo >= 2) {
                        newVal = (hi + lo) / 2;
                        if (x.getDataAt(i) > breaks.getDataAt(newVal) || (lft && x.getDataAt(i) == breaks.getDataAt(newVal))) {
                            lo = newVal;
                        } else {
                            hi = newVal;
                        }
                    }
                    data[i] = lo + 1;
                }
            }
        }

        return RDataFactory.createIntVector(data, naCheck.neverSeenNA() && complete);
    }
}
