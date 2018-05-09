/*
 * Copyright (c) 1996-2012, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates
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
/* Acknowledgement from the original header:
 *  "HACKED" to allow weights by Adrian Baddeley
 *  Changes indicated by 'AB'
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.intNA;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

/**
 * Implements the C_BinDist external.
 */
public abstract class BinDist extends RExternalBuiltinNode.Arg5 {
    public static BinDist create() {
        return BinDistNodeGen.create();
    }

    static {
        Casts casts = new Casts(BinDist.class);
        casts.arg(0).mustBe(missingValue().not()).returnIf(nullValue(), emptyDoubleVector()).asDoubleVector();
        casts.arg(1).mustBe(missingValue().not()).returnIf(nullValue(), emptyDoubleVector()).asDoubleVector();
        casts.arg(2).asDoubleVector().findFirst();
        casts.arg(3).asDoubleVector().findFirst();
        casts.arg(4).asIntegerVector().findFirst().mustBe(gt0().and(intNA().not()), Message.INVALID_ARGUMENT, "n");
    }

    @Specialization
    RDoubleVector bindist(RAbstractDoubleVector x, RAbstractDoubleVector w, double xlo, double xhi, int n) {
        int ixmin = 0;
        int ixmax = n - 2;
        double xdelta = (xhi - xlo) / (n - 1);
        double[] result = new double[2 * n];
        Arrays.fill(result, 0);
        int wLength = w.getLength();

        for (int i = 0; i < x.getLength(); i++) {
            if (RRuntime.isFinite(x.getDataAt(i))) {
                double xpos = (x.getDataAt(i) - xlo) / xdelta;
                if (!Double.isFinite(xpos)) {
                    continue;
                }
                int ix = (int) Math.floor(xpos);
                double fx = xpos - ix;
                double wi = w.getDataAt(i % wLength);
                if (ixmin <= ix && ix <= ixmax) {
                    result[ix] += (1 - fx) * wi;
                    result[ix + 1] += fx * wi;
                } else if (ix == -1) {
                    result[0] += fx * wi;
                } else if (ix == ixmax + 1) {
                    result[ix] += (1 - fx) * wi;
                }
            }
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }
}
