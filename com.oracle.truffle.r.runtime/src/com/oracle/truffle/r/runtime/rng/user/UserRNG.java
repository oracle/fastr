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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.rng.RRNG.Kind;
import com.oracle.truffle.r.runtime.rng.RandomNumberGenerator;

/**
 * Interface to a user-supplied RNG.
 */
public final class UserRNG implements RandomNumberGenerator {
    private static final boolean OPTIONAL = true;

    public enum Function {
        Rand(!OPTIONAL),
        Init(OPTIONAL),
        NSeed(OPTIONAL),
        Seedloc(OPTIONAL);

        private DLL.SymbolHandle symbolHandle;
        private final String symbol;
        private final boolean optional;

        Function(boolean optional) {
            this.symbol = "user_unif_" + name().toLowerCase();
            this.optional = optional;
        }

        private boolean isDefined() {
            return symbolHandle != null;
        }

        public DLL.SymbolHandle getSymbolHandle() {
            return symbolHandle;
        }

        private void setSymbolHandle(DLLInfo dllInfo) {
            this.symbolHandle = findSymbol(symbol, dllInfo, optional);
        }

    }

    private UserRngRFFI userRngRFFI;
    private int nSeeds = 0;

    @Override
    @TruffleBoundary
    public void init(int seed) {
        DLLInfo dllInfo = DLL.findLibraryContainingSymbol(Function.Rand.symbol);
        if (dllInfo == null) {
            throw RError.error(RError.NO_CALLER, RError.Message.RNG_SYMBOL, Function.Rand.symbol);
        }
        for (Function f : Function.values()) {
            f.setSymbolHandle(dllInfo);
        }
        userRngRFFI = RFFIFactory.getRFFI().getUserRngRFFI();
        if (Function.Init.isDefined()) {
            userRngRFFI.init(seed);
        }
        if (Function.Seedloc.isDefined() && !Function.NSeed.isDefined()) {
            RError.warning(RError.NO_CALLER, RError.Message.RNG_READ_SEEDS);
        }
        if (Function.NSeed.isDefined()) {
            int ns = userRngRFFI.nSeed();
            if (ns < 0 || ns > 625) {
                RError.warning(RError.NO_CALLER, RError.Message.GENERIC, "seed length must be in 0...625; ignored");
            } else {
                nSeeds = ns;
                /*
                 * TODO: if we ever (initially) share iSeed (as GNU R does) we may need to assign
                 * this generator's iSeed here
                 */
            }
        }
    }

    private static DLL.SymbolHandle findSymbol(String symbol, DLLInfo dllInfo, boolean optional) {
        DLL.SymbolHandle func = DLL.findSymbol(symbol, dllInfo.name, DLL.RegisteredNativeSymbol.any());
        if (func == DLL.SYMBOL_NOT_FOUND) {
            if (!optional) {
                throw RError.error(RError.NO_CALLER, RError.Message.RNG_SYMBOL, symbol);
            } else {
                return null;
            }
        } else {
            return func;
        }
    }

    @Override
    @TruffleBoundary
    public void fixupSeeds(boolean initial) {
        // no fixup
    }

    @Override
    public int[] getSeeds() {
        if (!Function.Seedloc.isDefined()) {
            return null;
        }
        int[] seeds = new int[nSeeds];
        userRngRFFI.seeds(seeds);
        int[] result = new int[nSeeds + 1];
        System.arraycopy(seeds, 0, result, 1, seeds.length);
        return result;
    }

    @Override
    public double genrandDouble() {
        return userRngRFFI.rand();
    }

    @Override
    public Kind getKind() {
        return Kind.USER_UNIF;
    }

    @Override
    public int getNSeed() {
        return nSeeds;
    }

    @Override
    public void setISeed(int[] seeds) {
        // TODO: userRNG seems to be not using iseed?
    }

}
