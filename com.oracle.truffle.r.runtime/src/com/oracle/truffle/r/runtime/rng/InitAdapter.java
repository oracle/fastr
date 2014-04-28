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
package com.oracle.truffle.r.runtime.rng;

import com.oracle.truffle.r.runtime.rng.RRNG.Generator;

public abstract class InitAdapter implements Generator {

    /**
     * This function is derived from GNU R, RNG.c (RNG_Init).
     */
    protected void init(int seedParam, int[] array) {
        int seed = seedParam;
        for (int j = 0; j < array.length; j++) {
            seed = (69069 * seed + 1);
            array[j] = seed;
        }

        fixupSeeds(true);
    }

}
