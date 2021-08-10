/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates
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
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RNull;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

/**
 * Transcribed from {@code src/library/stats/src/approx.c:R_approxtest}.
 */
public abstract class ApproxTest extends RExternalBuiltinNode.Arg5 {
    public static ApproxTest create() {
        return ApproxTestNodeGen.create();
    }

    static {
        Casts casts = new Casts(ApproxTest.class);
        casts.arg(2).asIntegerVector().findFirst();
        casts.arg(3).asDoubleVector().findFirst();
        casts.arg(4).asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    protected RNull approxtest(RDoubleVector x, RDoubleVector y, int method, double f, boolean naRm) {
        int nx = x.getLength();
        switch (method) {
            case 1:
                break;
            case 2:
                if (!RRuntime.isFinite(f) || f < 0.0 || f > 1.0) {
                    throw error(RError.Message.GENERIC, "approx(): invalid f value");
                }
                break;
            default:
                throw error(RError.Message.GENERIC, "approx(): invalid interpolation method");
        }

        for (int i = 0; i < nx; i++) {
            if (naRm) {
                // (x,y) should not have any NA's anymore
                if (RRuntime.isNAorNaN(x.getDataAt(i)) || RRuntime.isNAorNaN(y.getDataAt(i))) {
                    throw error(RError.Message.GENERIC, ("approx(): attempted to interpolate NA values"));
                }
            } else {
                // na.rm = FALSE ==> at least y may contain NA's
                if (RRuntime.isNAorNaN(x.getDataAt(i))) {
                    throw error(RError.Message.GENERIC, ("approx(x,y, .., na.rm=FALSE): NA values in x are not allowed"));
                }
            }
        }

        return RNull.instance;
    }
}
