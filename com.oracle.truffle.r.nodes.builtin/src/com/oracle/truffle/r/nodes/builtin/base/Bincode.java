/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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
public abstract class Bincode extends RBuiltinNode {

    private final NACheck naCheck = NACheck.create();

    static {
        Casts casts = new Casts(Bincode.class);
        casts.arg("x").mustNotBeMissing(RError.Message.ARGUMENT_EMPTY, 1).asDoubleVector();

        casts.arg("breaks").mustNotBeMissing(RError.Message.ARGUMENT_EMPTY, 2).asDoubleVector();

        casts.arg("right").asLogicalVector().findFirst().map(toBoolean());

        casts.arg("include.lowest").asLogicalVector().findFirst().map(toBoolean());
    }

    @SuppressWarnings("unused")
    @Specialization
    RIntVector formatC(RNull x, RAbstractDoubleVector breaks, boolean right, boolean includeBorder) {
        return RDataFactory.createEmptyIntVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    RIntVector formatC(RNull x, RNull breaks, boolean right, boolean includeBorder) {
        return RDataFactory.createEmptyIntVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    RIntVector formatC(RAbstractDoubleVector x, RNull breaks, boolean right, boolean includeBorder) {
        return RDataFactory.createIntVector(x.getLength(), true);
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
