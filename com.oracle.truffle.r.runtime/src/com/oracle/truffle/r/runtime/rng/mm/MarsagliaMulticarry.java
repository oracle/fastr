/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime.rng.mm;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.rng.RNGInitAdapter;
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
            setISeedItem(i, seed);
        }
        fixupSeeds(true);
    }

    @Override
    @TruffleBoundary
    public void fixupSeeds(boolean initial) {
        if (getISeedItem(0) == 0) {
            setISeedItem(0, 1);
        }
        if (getISeedItem(1) == 0) {
            setISeedItem(1, 1);
        }
    }

    @Override
    public double genrandDouble() {
        int state0 = getISeedItem(0);
        int state1 = getISeedItem(1);
        state0 = 36969 * (state0 & 0177777) + (state0 >>> 16);
        state1 = 18000 * (state1 & 0177777) + (state1 >>> 16);
        int x = (state0 << 16) ^ (state1 & 0177777);
        double d = (x & 0xffffffffL) * I2_32M1;
        setISeedItem(0, state0);
        setISeedItem(1, state1);
        return fixup(d); /* in [0,1) */
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
