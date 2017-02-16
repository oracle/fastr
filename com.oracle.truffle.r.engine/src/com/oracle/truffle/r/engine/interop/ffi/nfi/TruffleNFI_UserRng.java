/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop.ffi.nfi;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.rng.user.UserRNG.Function;

public class TruffleNFI_UserRng implements UserRngRFFI {

    private static class NFIUserRngRFFINode extends UserRngRFFINode {
        Node initMessage;
        Node randMessage;
        Node nSeedMessage;
        Node seedsMessage;
        Node readPointerNode = Message.createExecute(1).createNode();

        TruffleObject initFunction;
        TruffleObject nSeedFunction;
        TruffleObject randFunction;
        TruffleObject seedsFunction;

        @Override
        public void init(int seed) {
            if (initMessage == null) {
                initMessage = Message.createExecute(1).createNode();
            }
            try {
                if (initFunction == null) {
                    Node bind = Message.createInvoke(1).createNode();
                    initFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, Function.Init.getSymbolHandle().asTruffleObject(), "bind", "(sint32): void");
                }
                ForeignAccess.sendExecute(initMessage, initFunction, seed);
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Override
        public double rand() {
            if (randMessage == null) {
                randMessage = Message.createExecute(0).createNode();
            }
            try {
                if (randFunction == null) {
                    Node bind = Message.createInvoke(1).createNode();
                    randFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, Function.Rand.getSymbolHandle().asTruffleObject(), "bind", "(): pointer");
                }
                Object address = ForeignAccess.sendExecute(randMessage, randFunction);
                Object value = ForeignAccess.sendExecute(readPointerNode, TruffleNFI_CAccess.Function.READ_POINTER_DOUBLE.getSymbolFunction(), address);
                return (double) value;
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Override
        public int nSeed() {
            if (nSeedMessage == null) {
                nSeedMessage = Message.createExecute(0).createNode();
            }
            try {
                if (nSeedFunction == null) {
                    Node bind = Message.createInvoke(1).createNode();
                    nSeedFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, Function.NSeed.getSymbolHandle().asTruffleObject(), "bind", "(): pointer");
                }
                Object address = ForeignAccess.sendExecute(nSeedMessage, nSeedFunction);
                Object n = ForeignAccess.sendExecute(readPointerNode, TruffleNFI_CAccess.Function.READ_POINTER_INT.getSymbolFunction(), address);
                return (int) n;
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Override
        public void seeds(int[] n) {
            if (seedsMessage == null) {
                seedsMessage = Message.createExecute(0).createNode();
            }
            try {
                if (seedsFunction == null) {
                    Node bind = Message.createInvoke(1).createNode();
                    seedsFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, Function.Seedloc.getSymbolHandle().asTruffleObject(), "bind", "(): pointer");
                }
                Object address = ForeignAccess.sendExecute(seedsMessage, seedsFunction);
                for (int i = 0; i < n.length; i++) {
                    Object seed = ForeignAccess.sendExecute(readPointerNode, TruffleNFI_CAccess.Function.READ_ARRAY_INT.getSymbolFunction(), address, i);
                    n[i] = (int) seed;
                }
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    @Override
    public UserRngRFFINode createUserRngRFFINode() {
        return new NFIUserRngRFFINode();
    }
}
