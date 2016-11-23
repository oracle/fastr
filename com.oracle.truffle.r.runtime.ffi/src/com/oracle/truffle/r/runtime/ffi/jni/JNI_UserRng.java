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
package com.oracle.truffle.r.runtime.ffi.jni;

import static com.oracle.truffle.r.runtime.rng.user.UserRNG.Function;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;

public class JNI_UserRng implements UserRngRFFI {
    @Override
    @TruffleBoundary
    public void init(int seed) {
        init(Function.Init.getSymbolHandle().asAddress(), seed);

    }

    @Override
    @TruffleBoundary
    public double rand() {
        return rand(Function.Rand.getSymbolHandle().asAddress());
    }

    @Override
    @TruffleBoundary
    public int nSeed() {
        return nSeed(Function.NSeed.getSymbolHandle().asAddress());
    }

    @Override
    @TruffleBoundary
    public void seeds(int[] n) {
        seeds(Function.Seedloc.getSymbolHandle().asAddress(), n);
    }

    private static native void init(long address, int seed);

    private static native double rand(long address);

    private static native int nSeed(long address);

    private static native void seeds(long address, int[] n);
}
