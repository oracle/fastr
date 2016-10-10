/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.rng.RandomNumberNode;

/**
 * TODO GnuR checks/updates {@code .Random.seed} across this call. TODO Honor min/max.
 */
public abstract class Rnorm extends RExternalBuiltinNode.Arg3 {

    @Child private RandomNumberNode random = new RandomNumberNode();

    private static final double BIG = 134217728;

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(1).asDoubleVector();
        casts.arg(2).asDoubleVector();
    }

    @Specialization
    protected Object doRnorm(Object n, RAbstractDoubleVector mean, RAbstractDoubleVector stdDev, //
                    @Cached("create()") VectorLengthProfile meanProfile, //
                    @Cached("create()") VectorLengthProfile stdDevProfile) {
        int nInt = 1;
        if (n instanceof RAbstractVector) {
            nInt = ((RAbstractVector) n).getLength();
        }
        if (nInt == 1) {
            nInt = castInt(castVector(n));
        }
        RNode.reportWork(this, nInt);

        double[] numbers = random.executeDouble(nInt * 2);
        double[] result = new double[numbers.length / 2];

        int meanLength = meanProfile.profile(mean.getLength());
        int stdDEvLength = stdDevProfile.profile(stdDev.getLength());

        /* unif_rand() alone is not of high enough precision */
        for (int i = 0; i < result.length; i++) {
            double u1 = (int) (BIG * numbers[i * 2]) + numbers[i * 2 + 1];

            double meanValue = mean.getDataAt(i % meanLength);
            double stdDevValue = stdDev.getDataAt(i % stdDEvLength);

            result[i] = Random2.qnorm5(u1 / BIG, 0.0, 1.0, true, false) * stdDevValue + meanValue;
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }
}
