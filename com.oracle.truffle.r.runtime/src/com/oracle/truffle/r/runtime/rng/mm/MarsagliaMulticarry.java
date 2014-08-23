/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.rng.mm;

import com.oracle.truffle.r.runtime.rng.*;
import com.oracle.truffle.r.runtime.rng.RRNG.GeneratorPrivate;

/**
 * "Marsaglia-Multicarry" RNG. Transcribed from GnuR RNG.c.
 */
public class MarsagliaMulticarry extends RNGInitAdapter implements GeneratorPrivate {

    private final int[] state = new int[2];

    public void init(int seed) {
        super.init(seed, state);
    }

    public void fixupSeeds(boolean initial) {
        if (state[0] == 0) {
            state[0] = 1;
        }
        if (state[1] == 0) {
            state[1] = 1;
        }
    }

    public int[] getSeeds() {
        return state;
    }

    public double genrandDouble() {
        state[0] = 36969 * (state[0] & 0177777) + (state[0] >>> 16);
        state[1] = 18000 * (state[1] & 0177777) + (state[1] >>> 16);
        int x = (state[0] << 16) ^ (state[1] & 0177777);
        double d = (x & 0xffffffffL) * RRNG.I2_32M1;
        return RRNG.fixup(d); /* in [0,1) */
    }

}
