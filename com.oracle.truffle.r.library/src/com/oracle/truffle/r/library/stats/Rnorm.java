/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.rng.RandomNumberNode;
import com.oracle.truffle.r.runtime.rng.RandomNumberNode.RNGState;

/**
 * TODO GnuR checks/updates {@code .Random.seed} across this call. TODO Honor min/max.
 */
public abstract class Rnorm extends RExternalBuiltinNode.Arg3 {

    @Child private RandomNumberNode random = new RandomNumberNode();

    private static final double BIG = 134217728;

    // from GNUR: snorm.c, rnorm.c
    private double normRand(RNGState gen) {
        double u1;

        /* unif_rand() alone is not of high enough precision */
        u1 = random.unifRand(gen);
        u1 = (int) (BIG * u1) + random.unifRand(gen);
        return Random2.qnorm5(u1 / BIG, 0.0, 1.0, true, false);
    }

    @Specialization
    protected Object doRnorm(Object n, double mean, double standardd) {
        // TODO full error checks
        int nInt = castInt(castVector(n));
        RNode.reportWork(this, nInt);

        RNGState gen = random.initialize();

        double[] result = new double[nInt];
        for (int i = 0; i < nInt; i++) {
            result[i] = mean + standardd * normRand(gen);
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }
}
