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
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;

@RBuiltin(name = "tabulate", kind = INTERNAL, parameterNames = {"bin", "nbins"}, behavior = PURE)
public abstract class Tabulate extends RBuiltinNode.Arg2 {

    private final LoopConditionProfile loopProfile = LoopConditionProfile.createCountingProfile();

    static {
        Casts casts = new Casts(Tabulate.class);
        casts.arg("bin").defaultError(RError.Message.INVALID_INPUT).mustBe(integerValue()).asIntegerVector();
        casts.arg("nbins").defaultError(RError.Message.INVALID_ARGUMENT, "nbin").asIntegerVector().findFirst().mustBe(gte(0));
    }

    @Specialization
    protected com.oracle.truffle.r.runtime.data.RIntVector tabulate(RIntVector bin, int nBins) {
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
