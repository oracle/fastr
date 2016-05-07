/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.rng.user;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.rng.RNGInitAdapter;
import com.oracle.truffle.r.runtime.rng.RRNG.Kind;
import com.oracle.truffle.r.runtime.rng.RRNG.RNGException;

/**
 * Interface to a user-supplied RNG.
 */
public final class UserRNG extends RNGInitAdapter {

    private static final String USER_UNIF_RAND = "user_unif_rand";
    private static final String USER_UNIF_INIT = "user_unif_init";
    private static final boolean OPTIONAL = true;

    @SuppressWarnings("unused") private long userUnifRand;
    @SuppressWarnings("unused") private long userUnifInit;
    private long userUnifNSeed;
    private long userUnifSeedloc;
    private UserRngRFFI userRngRFFI;
    private int nSeeds;

    @Override
    public void init(int seed) throws RNGException {
        DLLInfo dllInfo = DLL.findLibraryContainingSymbol(USER_UNIF_RAND);
        if (dllInfo == null) {
            throw RNGException.raise(RError.Message.RNG_SYMBOL, true, USER_UNIF_RAND);
        }
        userUnifRand = findSymbol(USER_UNIF_RAND, dllInfo, !OPTIONAL);
        userUnifInit = findSymbol(USER_UNIF_INIT, dllInfo, OPTIONAL);
        userUnifNSeed = findSymbol(USER_UNIF_INIT, dllInfo, OPTIONAL);
        userUnifSeedloc = findSymbol(USER_UNIF_INIT, dllInfo, OPTIONAL);
        userRngRFFI = RFFIFactory.getRFFI().getUserRngRFFI();
        userRngRFFI.setLibrary(dllInfo.path);
        userRngRFFI.init(seed);
        if (userUnifSeedloc != 0 && userUnifNSeed == 0) {
            throw RNGException.raise(RError.Message.RNG_READ_SEEDS, false);
        }
        nSeeds = userRngRFFI.nSeed();
    }

    private static long findSymbol(String symbol, DLLInfo dllInfo, boolean optional) throws RNGException {
        long func = DLL.findSymbol(symbol, dllInfo.name, DLL.RegisteredNativeSymbol.any());
        if (func == DLL.SYMBOL_NOT_FOUND) {
            if (!optional) {
                throw RNGException.raise(RError.Message.RNG_SYMBOL, true, symbol);
            } else {
                return 0;
            }
        } else {
            return func;
        }
    }

    @Override
    public void fixupSeeds(boolean initial) {
        // no fixup
    }

    @Override
    public int[] getSeeds() {
        if (userUnifSeedloc == 0) {
            return null;
        }
        int[] result = new int[nSeeds];
        userRngRFFI.seeds(result);
        return result;
    }

    @Override
    public double[] genrandDouble(int count) {
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            result[i] = userRngRFFI.rand();
        }
        return result;
    }

    @Override
    public Kind getKind() {
        return Kind.USER_UNIF;
    }
}
