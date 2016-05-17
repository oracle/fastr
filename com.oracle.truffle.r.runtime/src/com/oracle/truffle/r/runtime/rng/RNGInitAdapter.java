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

import com.oracle.truffle.r.runtime.rng.RRNG.RandomNumberGenerator;

public abstract class RNGInitAdapter implements RandomNumberGenerator {

    // TODO: it seems like GNU R this is shared between the generators (does it matter?)
    protected int[] iSeed = new int[625];

    @Override
    public void setISeed(int[] seeds) {
        for (int i = 1; i <= getNSeed(); i++) {
            iSeed[i - 1] = seeds[i];
        }
        fixupSeeds(false);
    }
}
