/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;

public abstract class Fft extends RExternalBuiltinNode.Arg2 {

    private final ConditionProfile zVecLgt1 = ConditionProfile.createBinaryProfile();
    private final ConditionProfile noDims = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(0).mustNotBeNull().asComplexVector(false, true, false);
        casts.arg(1).mustNotBeNull().asLogicalVector().findFirst().map(Predef.toBoolean());
    }

    // TODO: handle more argument types (this is sufficient to run the b25 benchmarks)
    @Specialization
    public Object execute(RAbstractComplexVector zVec, boolean inverse,
                    @Cached("create()") GetDimAttributeNode getDimNode,
                    @Cached("create()") StatsRFFI.FactorNode factorNode,
                    @Cached("create()") StatsRFFI.WorkNode workNode) {
        double[] z = zVec.materialize().getDataTemp();
        int inv = inverse ? 2 : -2;
        int[] d = getDimNode.getDimensions(zVec);
        @SuppressWarnings("unused")
        int retCode = 7;
        if (zVecLgt1.profile(zVec.getLength() > 1)) {
            int[] maxf = new int[1];
            int[] maxp = new int[1];
            if (noDims.profile(d == null)) {
                int n = zVec.getLength();
                factorNode.execute(n, maxf, maxp);
                if (maxf[0] == 0) {
                    errorProfile.enter();
                    throw RError.error(this, RError.Message.FFT_FACTORIZATION);
                }
                double[] work = new double[4 * maxf[0]];
                int[] iwork = new int[maxp[0]];
                retCode = workNode.execute(z, 1, n, 1, inv, work, iwork);
            } else {
                int maxmaxf = 1;
                int maxmaxp = 1;
                int ndims = d.length;
                /* do whole loop just for error checking and maxmax[fp] .. */
                for (int i = 0; i < ndims; i++) {
                    if (d[i] > 1) {
                        factorNode.execute(d[i], maxf, maxp);
                        if (maxf[0] == 0) {
                            errorProfile.enter();
                            throw RError.error(this, RError.Message.FFT_FACTORIZATION);
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
                        workNode.execute(z, nseg, n, nspn, inv, work, iwork);
                    }
                }
            }
        }
        return RDataFactory.createComplexVector(z, zVec.isComplete(), d);
    }
}
