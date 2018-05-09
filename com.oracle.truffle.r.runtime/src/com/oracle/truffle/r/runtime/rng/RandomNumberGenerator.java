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

import com.oracle.truffle.r.runtime.rng.RRNG.Kind;

/**
 * Interface for a random number generator algorithm.
 */
public interface RandomNumberGenerator {
    void init(int seed);

    void fixupSeeds(boolean initial);

    /**
     * Returns array in the format of .Random.seed, i.e. under index 0 is id of the generator and
     * the rest are the actual seeds.
     */
    int[] getSeeds();

    double genrandDouble();

    Kind getKind();

    /**
     * Returns number of seed values this generator requires.
     */
    int getNSeed();

    /**
     * Sets array which contains current generator flag under index 0 and seeds for random
     * generation under the other indices. The generator may use the array as is without making a
     * defensive copy, the caller must make sure the array contents do not get changed.
     */
    void setISeed(int[] seeds);
}
