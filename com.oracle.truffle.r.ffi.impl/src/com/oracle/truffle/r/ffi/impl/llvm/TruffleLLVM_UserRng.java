/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.rng.user.UserRNG.Function;

public class TruffleLLVM_UserRng implements UserRngRFFI {
    private static class TruffleLLVM_UserRngRFFINode extends Node implements UserRngRFFINode {
        Node initMessage;
        Node randMessage;
        Node nSeedMessage;
        Node seedsMessage;
        Node readPointerNode = Message.createExecute(1).createNode();

        @Override
        public void init(int seed) {
            if (initMessage == null) {
                initMessage = Message.createExecute(1).createNode();
            }
            try {
                ForeignAccess.sendExecute(initMessage, Function.Init.getSymbolHandle().asTruffleObject(), seed);
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
                Object address = ForeignAccess.sendExecute(randMessage, Function.Rand.getSymbolHandle().asTruffleObject());
                Object value = ForeignAccess.sendExecute(readPointerNode, TruffleLLVM_CAccess.Function.READ_POINTER_DOUBLE.getSymbolHandle().asTruffleObject(), address);
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
                Object address = ForeignAccess.sendExecute(nSeedMessage, Function.NSeed.getSymbolHandle().asTruffleObject());
                Object n = ForeignAccess.sendExecute(readPointerNode, TruffleLLVM_CAccess.Function.READ_POINTER_INT.getSymbolHandle().asTruffleObject(), address);
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
                Object address = ForeignAccess.sendExecute(seedsMessage, Function.Seedloc.getSymbolHandle().asTruffleObject());
                for (int i = 0; i < n.length; i++) {
                    Object seed = ForeignAccess.sendExecute(readPointerNode, TruffleLLVM_CAccess.Function.READ_ARRAY_INT.getSymbolHandle().asTruffleObject(), address, i);
                    n[i] = (int) seed;
                }
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    @Override
    public UserRngRFFINode createUserRngRFFINode() {
        return new TruffleLLVM_UserRngRFFINode();
    }
}
