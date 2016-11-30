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
