/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1996-2012, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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
import static com.oracle.truffle.r.runtime.RError.NO_CALLER;

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
        casts.arg(4).asIntegerVector().findFirst().mustBe(gt0().and(intNA().not()), NO_CALLER, Message.INVALID_ARGUMENT, "n");
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
