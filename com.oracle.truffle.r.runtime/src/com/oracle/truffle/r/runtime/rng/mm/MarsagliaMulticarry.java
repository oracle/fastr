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
package com.oracle.truffle.r.runtime.rng.mm;

import com.oracle.truffle.r.runtime.rng.RNGInitAdapter;
import com.oracle.truffle.r.runtime.rng.RRNG;
import com.oracle.truffle.r.runtime.rng.RRNG.Kind;

/**
 * "Marsaglia-Multicarry" RNG. Transcribed from GnuR RNG.c.
 */
public final class MarsagliaMulticarry extends RNGInitAdapter {

    private final int[] state = new int[2];

    @Override
    public void init(int seed) {
        super.init(seed, state);
    }

    @Override
    public void fixupSeeds(boolean initial) {
        if (state[0] == 0) {
            state[0] = 1;
        }
        if (state[1] == 0) {
            state[1] = 1;
        }
    }

    @Override
    public int[] getSeeds() {
        return state;
    }

    @Override
    public double[] genrandDouble(int count) {
        int state0 = state[0];
        int state1 = state[1];
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            state0 = 36969 * (state0 & 0177777) + (state0 >>> 16);
            state1 = 18000 * (state1 & 0177777) + (state1 >>> 16);
            int x = (state0 << 16) ^ (state1 & 0177777);
            double d = (x & 0xffffffffL) * RRNG.I2_32M1;
            result[i] = RRNG.fixup(d); /* in [0,1) */
        }
        state[0] = state0;
        state[1] = state1;
        return result;
    }

    @Override
    public Kind getKind() {
        return Kind.MARSAGLIA_MULTICARRY;
    }
}
