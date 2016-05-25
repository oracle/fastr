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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.rng.RNGInitAdapter;
import com.oracle.truffle.r.runtime.rng.RRNG;
import com.oracle.truffle.r.runtime.rng.RRNG.Kind;

/**
 * "Marsaglia-Multicarry" RNG. Transcribed from GnuR RNG.c.
 */
public final class MarsagliaMulticarry extends RNGInitAdapter {

    @Override
    @TruffleBoundary
    public void init(int seedParam) {
        int seed = seedParam;
        for (int i = 0; i < getNSeed(); i++) {
            seed = (69069 * seed + 1);
            iSeed[i] = seed;
        }
        fixupSeeds(true);
    }

    @Override
    @TruffleBoundary
    public void fixupSeeds(boolean initial) {
        if (iSeed[0] == 0) {
            iSeed[0] = 1;
        }
        if (iSeed[1] == 0) {
            iSeed[1] = 1;
        }
    }

    @Override
    public int[] getSeeds() {
        return iSeed;
    }

    @Override
    public double[] genrandDouble(int count) {
        int state0 = iSeed[0];
        int state1 = iSeed[1];
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            state0 = 36969 * (state0 & 0177777) + (state0 >>> 16);
            state1 = 18000 * (state1 & 0177777) + (state1 >>> 16);
            int x = (state0 << 16) ^ (state1 & 0177777);
            double d = (x & 0xffffffffL) * RRNG.I2_32M1;
            result[i] = RRNG.fixup(d); /* in [0,1) */
        }
        iSeed[0] = state0;
        iSeed[1] = state1;
        return result;
    }

    @Override
    public Kind getKind() {
        return Kind.MARSAGLIA_MULTICARRY;
    }

    @Override
    public int getNSeed() {
        return 2;
    }

}
