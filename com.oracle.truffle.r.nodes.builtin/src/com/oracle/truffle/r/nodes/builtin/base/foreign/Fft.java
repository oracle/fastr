/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
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
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;

public abstract class Fft extends RExternalBuiltinNode.Arg2 {

    private final ConditionProfile zVecLgt1 = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noDims = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(Fft.class);
        casts.arg(0).mustNotBeMissing().mustBe(nullValue().not()).asComplexVector(false, true, false);
        casts.arg(1).mustNotBeNull().asLogicalVector().findFirst().map(Predef.toBoolean());
    }

    @Child private StatsRFFI.FactorNode factorNode = StatsRFFI.FactorNode.create();
    @Child private StatsRFFI.SetupWorkNode setupWorkNode = StatsRFFI.SetupWorkNode.create();

    // TODO: handle more argument types (this is sufficient to run the b25 benchmarks)
    @Specialization
    public Object execute(RAbstractComplexVector zVec, boolean inverse,
                    @Cached("create()") GetDimAttributeNode getDimNode) {
        double[] z = zVec.getDataTemp();
        int inv = inverse ? 2 : -2;
        int[] d = getDimNode.getDimensions(zVec);
        if (zVecLgt1.profile(zVec.getLength() > 1)) {
            int[] maxf = new int[1];
            int[] maxp = new int[1];
            if (noDims.profile(d == null)) {
                int n = zVec.getLength();
                factorNode.execute(n, maxf, maxp);
                if (maxf[0] == 0) {
                    throw error(RError.Message.FFT_FACTORIZATION);
                }
                double[] work = new double[4 * maxf[0]];
                int[] iwork = new int[maxp[0]];
                setupWorkNode.execute(z, 1, n, 1, inv, work, iwork);
            } else {
                int maxmaxf = 1;
                int maxmaxp = 1;
                int ndims = d.length;
                /* do whole loop just for error checking and maxmax[fp] .. */
                for (int i = 0; i < ndims; i++) {
                    if (d[i] > 1) {
                        factorNode.execute(d[i], maxf, maxp);
                        if (maxf[0] == 0) {
                            throw error(RError.Message.FFT_FACTORIZATION);
                        }
                        if (maxf[0] > maxmaxf) {
                            maxmaxf = maxf[0];
                        }
                        if (maxp[0] > maxmaxp) {
                            maxmaxp = maxp[0];
                        }
                    }
                }
                double[] work = new double[4 * maxmaxf];
                int[] iwork = new int[maxmaxp];
                int nseg = zVec.getLength();
                int n = 1;
                int nspn = 1;
                for (int i = 0; i < ndims; i++) {
                    if (d[i] > 1) {
                        nspn *= n;
                        n = d[i];
                        nseg /= n;
                        factorNode.execute(n, maxf, maxp);
                        setupWorkNode.execute(z, nseg, n, nspn, inv, work, iwork);
                    }
                }
            }
        }
        return RDataFactory.createComplexVector(z, zVec.isComplete(), d);
    }
}
