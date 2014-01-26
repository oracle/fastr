/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1997-2002, Makoto Matsumoto and Takuji Nishimura
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

/*  
 This is a Java reimplementation of MT19937, derived by translation from 
 the C implementation, which is available from 
 http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/MT2002/CODES/mt19937ar.c
 and is copyright (c) 1997-2002, Makoto Matsumoto and Takuji Nishimura, 
 and also from the GNU R file RNG.c

 The original header from the C implementation by Matsumoto and Nishimura is included below.
 */

/*   
 A C-program for MT19937, with initialization improved 2002/1/26.
 Coded by Takuji Nishimura and Makoto Matsumoto.

 Before using, initialize the state by using init_genrand(seed)
 or init_by_array(init_key, key_length).

 Copyright (C) 1997 - 2002, Makoto Matsumoto and Takuji Nishimura,
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.

 3. The names of its contributors may not be used to endorse or promote
 products derived from this software without specific prior written
 permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


 Any feedback is very welcome.
 http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html
 email: m-mat @ math.sci.hiroshima-u.ac.jp (remove space)
 */
package com.oracle.truffle.r.runtime;

public final class RRandomNumberGenerator {

    public RRandomNumberGenerator() {
        sgenrand(4357);
    }

    public RRandomNumberGenerator(int initialSeed) {
        setSeed(initialSeed);
    }

    /* Period parameters */
    private static final int N = 624;
    private static final int M = 397;
    private static final int MATRIXA = 0x9908b0df; /* constant vector a */
    private static final int UPPERMASK = 0x80000000; /* most significant w-r bits */
    private static final int LOWERMASK = 0x7fffffff; /* least significant r bits */
    private static final int TEMPERING_MASK_B = 0x9d2c5680;
    private static final int TEMPERING_MASK_C = 0xefc60000;
    private static final double I2_32M1 = 2.328306437080797e-10; /* = 1/(2^32 - 1) */

    private int[] mt = new int[N]; /* the array for the state vector */
    private int mti;

    public int[] getState() {
        return mt;
    }

    public void setSeed(int seedParam) {
        /* cwirth: this function is derived from GNU R, RNG.c (RNG_Init) */
        int j;
        int seed = seedParam;

        /* Initial scrambling. */
        /* First seed value is discarded in GNU R, thus + 1. */
        for (j = 0; j < 51; j++) {
            seed = (69069 * seed + 1);
        }

        for (j = 0; j < N; j++) {
            seed = (69069 * seed + 1);
            mt[j] = seed;
        }

        mti = N;
    }

    private void sgenrand(int seedParam) {
        /* transcribed from GNU R, RNG.c (MT_sgenrand) */
        int i;
        int seed = seedParam;
        for (i = 0; i < N; i++) {
            mt[i] = seed & 0xffff0000;
            seed = 69069 * seed + 1;
            mt[i] |= (seed & 0xffff0000) >>> 16;
            seed = 69069 * seed + 1;
        }
        mti = N;
    }

    /* initializes mt[N] with a seed */
    public void initGenrand(int s) {
        mt[0] = s & 0xffffffff;
        for (mti = 1; mti < N; mti++) {
            mt[mti] = (1812433253 * (mt[mti - 1] ^ (mt[mti - 1] >>> 30)) + mti);
            /* See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier. */
            /* In the previous versions, MSBs of the seed affect */
            /* only MSBs of the array mt[]. */
            /* 2002/01/09 modified by Makoto Matsumoto */
            mt[mti] &= 0xffffffffL;
            /* for >32 bit machines */
        }
    }

    /* initialize by an array with array-length */
    /* init_key is the array for initializing keys */
    /* key_length is its length */
    /* slight change for C++, 2004/2/26 */
    public void initByArray(int[] initKey, int keyLength) {
        int i;
        int j;
        int k;
        initGenrand(19650218);
        i = 1;
        j = 0;
        k = (N > keyLength ? N : keyLength);
        for (; k != 0; k--) {
            mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * 1664525)) + initKey[j] + j; /*
                                                                                              * non
                                                                                              * linear
                                                                                              */
            mt[i] &= 0xffffffffL; /* for WORDSIZE > 32 machines */
            i++;
            j++;
            if (i >= N) {
                mt[0] = mt[N - 1];
                i = 1;
            }
            if (j >= keyLength) {
                j = 0;
            }
        }
        for (k = N - 1; k != 0; k--) {
            mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * 1566083941)) - i; /* non linear */
            mt[i] &= 0xffffffff; /* for WORDSIZE > 32 machines */
            i++;
            if (i >= N) {
                mt[0] = mt[N - 1];
                i = 1;
            }
        }

        mt[0] = 0x80000000; /* MSB is 1; assuring non-zero initial array */
    }

    private static final int[] MAG01 = new int[]{0x0, MATRIXA};

    /* generates a random number on [0,0xffffffff]-interval */
    private int genrandInt32() {
        int y;

        /* mag01[x] = x * MATRIX_A for x=0,1 */

        if (mti >= N) { /* generate N words at one time */
            generateNewNumbers();
        }

        y = mt[mti++];

        /* Tempering */
        y ^= (y >>> 11);
        y ^= (y << 7) & TEMPERING_MASK_B;
        y ^= (y << 15) & TEMPERING_MASK_C;
        y ^= (y >>> 18);

        return y;
    }

    @com.oracle.truffle.api.CompilerDirectives.SlowPath
    private void generateNewNumbers() {
        int y;
        int kk;

        for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPERMASK) | (mt[kk + 1] & LOWERMASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ MAG01[y & 0x1];
        }
        for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPERMASK) | (mt[kk + 1] & LOWERMASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ MAG01[y & 0x1];
        }
        y = (mt[N - 1] & UPPERMASK) | (mt[0] & LOWERMASK);
        mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ MAG01[y & 0x1];

        mti = 0;
    }

    public double genrandDouble() {
        return fixup((genrandInt32() & 0xffffffffL) * 2.3283064365386963e-10);
    }

    private static double fixup(double x) {
        /* transcribed from GNU R, RNG.c (fixup) */
        /* ensure 0 and 1 are never returned */
        if (x <= 0.0) {
            return 0.5 * I2_32M1;
        }
        // aw: removed fixup for 1.0 since x is in [0,1).
        // if ((1.0 - x) <= 0.0) return 1.0 - 0.5 * I2_32M1;
        return x;
    }

    /* generates a random number on [0,0x7fffffff]-interval */
    public int genrandInt31() {
        return (genrandInt32() >>> 1);
    }

    /* generates a random number on [0,1]-real-interval */
    public double genrandReal1() {
        return (genrandInt32() & 0xffffffffL) * (1.0 / 4294967295.0);
        /* divided by 2^32-1 */
    }

    /* generates a random number on [0,1)-real-interval */
    public double genrandReal2() {
        return (genrandInt32() & 0xffffffffL) * (1.0 / 4294967296.0);
        /* divided by 2^32 */
    }

    /* generates a random number on (0,1)-real-interval */
    public double genrandReal3() {
        return ((genrandInt32() & 0xffffffffL) + 0.5) * (1.0 / 4294967296.0);
        /* divided by 2^32 */
    }

    /* generates a random number on [0,1) with 53-bit resolution */
    public double genrandRes53() {
        int a = genrandInt32() >>> 5;
        int b = genrandInt32() >>> 6;
        return (a * 67108864.0 + b) * (1.0 / 9007199254740992.0);
    }

    /* These real versions are due to Isaku Wada, 2002/01/09 added */

}
