/*
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2016,  The R Core Team
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
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

/*
 This is a Java reimplementation of MT19937, derived by translation from
 the C implementation, which is available from the GNU R file RNG.c

The RNG.c file contains the following mention:
 */
/* ===================  Mersenne Twister ========================== */
/* From http://www.math.keio.ac.jp/~matumoto/emt.html */

/* A C-program for MT19937: Real number version([0,1)-interval)
   (1999/10/28)
     genrand() generates one pseudorandom real number (double)
   which is uniformly distributed on [0,1)-interval, for each
   call. sgenrand(seed) sets initial values to the working area
   of 624 words. Before genrand(), sgenrand(seed) must be
   called once. (seed is any 32-bit integer.)
   Integer generator is obtained by modifying two lines.
     Coded by Takuji Nishimura, considering the suggestions by
   Topher Cooper and Marc Rieffel in July-Aug. 1997.

   Copyright (C) 1997, 1999 Makoto Matsumoto and Takuji Nishimura.
   When you use this, send an email to: matumoto@math.keio.ac.jp
   with an appropriate reference to your work.

   REFERENCE
   M. Matsumoto and T. Nishimura,
   "Mersenne Twister: A 623-Dimensionally Equidistributed Uniform
   Pseudo-Random Number Generator",
   ACM Transactions on Modeling and Computer Simulation,
   Vol. 8, No. 1, January 1998, pp 3--30.
*/
package com.oracle.truffle.r.runtime.rng.mt;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.rng.RNGInitAdapter;
import com.oracle.truffle.r.runtime.rng.RRNG;
import com.oracle.truffle.r.runtime.rng.RRNG.Kind;

public final class MersenneTwister extends RNGInitAdapter {

    /* Period parameters */
    private static final int N = 624;
    private static final int M = 397;
    private static final int MATRIXA = 0x9908b0df; /* constant vector a */
    private static final int UPPERMASK = 0x80000000; /* most significant w-r bits */
    private static final int LOWERMASK = 0x7fffffff; /* least significant r bits */
    private static final int TEMPERING_MASK_B = 0x9d2c5680;
    private static final int TEMPERING_MASK_C = 0xefc60000;

    /*
     * This generator can efficiently generate many random numbers in one go, upon each call to
     * genrandDouble() we take the next random value from 'buffer'. If the buffer is empty, we fill
     * it.
     */
    private static final int BUFFER_SIZE = N;
    private final double[] buffer = new double[BUFFER_SIZE];
    private int bufferIndex = BUFFER_SIZE;

    /**
     * The array for the state vector. In GnuR, the state array is common to all algorithms (named
     * {@code dummy}), and the zero'th element is the number of seeds, but the algorithm uses
     * pointer arithmetic to set {@code mt} to {@code dummy + 1}.
     */
    private int getMt(int i) {
        return getISeedItem(i + 1);
    }

    private void setMt(int i, int val) {
        setISeedItem(i + 1, val);
    }

    @Override
    public void setISeed(int[] seeds) {
        fixupSeeds(false);
        // kill the current buffer if the seed changes
        bufferIndex = BUFFER_SIZE;
    }

    /**
     * This function is derived from GNU R, RNG.c (RNG_Init). N.B. GnuR generates N+1 seeds in
     * {@code dummy} and then overwrites the zeroth element with N, which is evidently important for
     * "historical compatibility". Since we can't play pointer arithmetic games, we generate but do
     * not store the seed in {@code mt[0]}. To compare, call set.seed(4357) followed by .Random.seed
     * in GnuR and FastR. The values in the vector should be the same.
     *
     */
    @Override
    @TruffleBoundary
    public void init(int seedParam) {
        int seed = seedParam;
        for (int i = 0; i < getNSeed(); i++) {
            seed = (69069 * seed + 1);
            setISeedItem(i, seed);
        }
        fixupSeeds(true);
        bufferIndex = BUFFER_SIZE;
    }

    @Override
    @TruffleBoundary
    public void fixupSeeds(boolean initial) {
        if (initial) {
            setISeedItem(0, N);
        }
        if (getISeedItem(0) <= 0) {
            setISeedItem(0, N);
        }
        boolean notAllZero = false;
        for (int i = 1; i <= N; i++) {
            if (getISeedItem(i) != 0) {
                notAllZero = true;
            }
        }
        if (!notAllZero) {
            init(RRNG.timeToSeed());
        }
    }

    /**
     * The actual generating method, essentially transcribed from MT_genrand in GnuR RNG.c.
     */
    @Override
    public double genrandDouble() {
        if (bufferIndex == BUFFER_SIZE) {
            int localDummy0 = getISeedItem(0);
            int localMti = localDummy0;
            // It appears that this never happens
            // sgenrand(4357);
            assert localMti != N + 1;
            int pos = 0;
            while (true) {
                int loopCount = Math.min(BUFFER_SIZE - pos, N - localMti);
                for (int i = 0; i < loopCount; i++) {
                    int y = getMt(localMti + i);
                    /* Tempering */
                    y ^= (y >>> 11);
                    y ^= (y << 7) & TEMPERING_MASK_B;
                    y ^= (y << 15) & TEMPERING_MASK_C;
                    y ^= (y >>> 18);
                    buffer[pos + i] = ((y + Integer.MIN_VALUE) - (double) Integer.MIN_VALUE) * I2_32M1;
                }
                for (int i = 0; i < loopCount; i++) {
                    buffer[pos + i] = fixup(buffer[pos + i]);
                }
                localMti += loopCount;
                pos += loopCount;

                if (pos == BUFFER_SIZE) {
                    break;
                }
                /* generate N words at one time */
                int kk;
                for (kk = 0; kk < N - M; kk++) {
                    int y2y = (getMt(kk) & UPPERMASK) | (getMt(kk + 1) & LOWERMASK);
                    setMt(kk, getMt(kk + M) ^ (y2y >>> 1) ^ mag01(y2y & 0x1));
                }
                for (; kk < N - 1; kk++) {
                    int y2y = (getMt(kk) & UPPERMASK) | (getMt(kk + 1) & LOWERMASK);
                    setMt(kk, getMt(kk + (M - N)) ^ (y2y >>> 1) ^ mag01(y2y & 0x1));
                }
                int y2y = (getMt(N - 1) & UPPERMASK) | (getMt(0) & LOWERMASK);
                setMt(N - 1, getMt(M - 1) ^ (y2y >>> 1) ^ mag01(y2y & 0x1));

                localMti = 0;
            }
            localDummy0 = localMti;
            setISeedItem(0, localDummy0);
            bufferIndex = 0;
        }
        return buffer[bufferIndex++];
    }

    private static int mag01(int v) {
        return (v & 1) != 0 ? MATRIXA : 0;
    }

    @Override
    public Kind getKind() {
        return Kind.MERSENNE_TWISTER;
    }

    @Override
    public int getNSeed() {
        return 1 + N;
    }
}
