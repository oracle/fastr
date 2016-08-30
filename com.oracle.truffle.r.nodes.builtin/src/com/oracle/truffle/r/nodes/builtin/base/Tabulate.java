/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

@RBuiltin(name = "tabulate", kind = INTERNAL, parameterNames = {"bin", "nbins"}, behavior = PURE)
public abstract class Tabulate extends RBuiltinNode {

    private final LoopConditionProfile loopProfile = LoopConditionProfile.createCountingProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("bin").defaultError(RError.NO_CALLER, RError.Message.INVALID_INPUT).mustBe(integerValue()).asIntegerVector();
        casts.arg("nbins").defaultError(RError.NO_CALLER, RError.Message.INVALID_ARGUMENT, "nbin").asIntegerVector().findFirst().mustBe(gte(0));
    }

    @Specialization
    protected RIntVector tabulate(RAbstractIntVector bin, int nBins) {
        int[] ans = new int[nBins];
        loopProfile.profileCounted(bin.getLength());
        for (int i = 0; loopProfile.inject(i < bin.getLength()); i++) {
            int currentEl = bin.getDataAt(i);
            if (!RRuntime.isNA(currentEl) && currentEl > 0 && currentEl <= nBins) {
                ans[currentEl - 1]++;
            }
        }
        return RDataFactory.createIntVector(ans, RDataFactory.COMPLETE_VECTOR);
    }

}
