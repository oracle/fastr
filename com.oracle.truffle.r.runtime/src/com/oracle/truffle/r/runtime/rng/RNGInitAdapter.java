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
package com.oracle.truffle.r.runtime.rng;

/**
 * Manages the iSeed array for generators and contains some useful common code.
 */
public abstract class RNGInitAdapter implements RandomNumberGenerator {

    protected static final double I2_32M1 = 2.3283064365386963e-10;
    protected static final int MAX_ISEED_SIZE = 625;

    // TODO: it seems like GNU R this is shared between the generators (does it matter?)
    private int[] iSeed = new int[MAX_ISEED_SIZE + 1];

    @Override
    public void setISeed(int[] seeds) {
        iSeed = seeds;
        fixupSeeds(false);
    }

    @Override
    public final int[] getSeeds() {
        return iSeed;
    }

    protected final int getISeedItem(int index) {
        return iSeed[index + 1];
    }

    protected final void setISeedItem(int index, int value) {
        iSeed[index + 1] = value;
    }

    /**
     * Ensure 0 and 1 are never returned from rand generation algorithm.
     */
    protected static double fixup(double x) {
        /* transcribed from GNU R, RNG.c (fixup) */
        if (x <= 0.0) {
            return 0.5 * I2_32M1;
        }
        // removed fixup for 1.0 since x is in [0,1).
        // TODO Since GnuR does include this is should we not for compatibility (probably).
        // if ((1.0 - x) <= 0.0) return 1.0 - 0.5 * I2_32M1;
        return x;
    }
}
