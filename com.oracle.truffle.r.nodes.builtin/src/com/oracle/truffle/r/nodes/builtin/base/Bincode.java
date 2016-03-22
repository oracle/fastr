/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "bincode", kind = INTERNAL, parameterNames = {"x", "breaks", "right", "include.lowest"})
public abstract class Bincode extends RBuiltinNode {

    private final BranchProfile errorProfile = BranchProfile.create();
    private final NACheck naCheck = NACheck.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toDouble(0);
        casts.toDouble(1);
    }

    @Specialization
    RIntVector formatC(RAbstractDoubleVector x, RAbstractDoubleVector breaks, RAbstractLogicalVector rightVec, RAbstractLogicalVector includeLowestVec) {
        int n = x.getLength();
        int nb = breaks.getLength();
        if (rightVec.getLength() == 0) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "right");
        }
        boolean right = RRuntime.fromLogical(rightVec.getDataAt(0));

        if (includeLowestVec.getLength() == 0) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "include.lowest");
        }
        boolean includeBorder = RRuntime.fromLogical(includeLowestVec.getDataAt(0));

        int lo;
        int hi;
        int nb1 = nb - 1;
        int newVal;

        boolean lft = !right;

        /* This relies on breaks being sorted, so wise to check that */
        for (int i = 1; i < nb; i++) {
            if (breaks.getDataAt(i - 1) > breaks.getDataAt(i)) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.GENERIC, "'breaks' is not sorted");
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
