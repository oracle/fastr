/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.jni;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.rng.user.UserRNG.Function;

public class JNI_UserRng implements UserRngRFFI {

    private static final class JNI_InitNode extends Node implements InitNode {

        @Override
        public void execute(int seed) {
            nativeInit(Function.Init.getSymbolHandle().asAddress(), seed);
        }
    }

    private static final class JNI_RandNode extends Node implements RandNode {

        @Override
        public double execute() {
            return nativeRand(Function.Rand.getSymbolHandle().asAddress());
        }
    }

    private static final class JNI_NSeedNode extends Node implements NSeedNode {

        @Override
        public int execute() {
            return nativeNSeed(Function.NSeed.getSymbolHandle().asAddress());
        }
    }

    private static final class JNI_SeedsNode extends Node implements SeedsNode {

        @Override
        public void execute(int[] n) {
            nativeSeeds(Function.Seedloc.getSymbolHandle().asAddress(), n);
        }
    }

    @Override
    public InitNode createInitNode() {
        return new JNI_InitNode();
    }

    @Override
    public RandNode createRandNode() {
        return new JNI_RandNode();
    }

    @Override
    public NSeedNode createNSeedNode() {
        return new JNI_NSeedNode();
    }

    @Override
    public SeedsNode createSeedsNode() {
        return new JNI_SeedsNode();
    }

    private static native void nativeInit(long address, int seed);

    private static native double nativeRand(long address);

    private static native int nativeNSeed(long address);

    private static native void nativeSeeds(long address, int[] n);

}
