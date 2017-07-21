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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.rng.user.UserRNG.Function;

public class TruffleNFI_UserRng implements UserRngRFFI {

    private abstract static class RNGNode extends Node {

        @CompilationFinal protected Node message;
        @CompilationFinal protected Node readPointerNode = Message.createExecute(1).createNode();
        @CompilationFinal protected TruffleObject targetFunction;

        protected void init(Function function, String signature) {
            if (message == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                message = Message.createExecute(TruffleNFI_Utils.getArgCount(signature)).createNode();
            }
            if (targetFunction == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                Node bind = Message.createInvoke(1).createNode();
                try {
                    targetFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, function.getSymbolHandle().asTruffleObject(), "bind", signature);
                } catch (Throwable t) {
                    throw RInternalError.shouldNotReachHere();
                }
            }
        }
    }

    private final static class TruffleNFI_InitNode extends RNGNode implements InitNode {

        @Override
        public void execute(int seed) {
            init(Function.Init, "(sint32): void");
            try {
                ForeignAccess.sendExecute(message, targetFunction, seed);
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    private final static class TruffleNFI_RandNode extends RNGNode implements RandNode {

        @Override
        public double execute() {
            init(Function.Rand, "(): pointer");
            try {
                Object address = ForeignAccess.sendExecute(message, targetFunction);
                return (double) ForeignAccess.sendExecute(readPointerNode, TruffleNFI_CAccess.Function.READ_POINTER_DOUBLE.getSymbolFunction(), address);
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    private final static class TruffleNFI_NSeedNode extends RNGNode implements NSeedNode {

        @Override
        public int execute() {
            init(Function.NSeed, "(): pointer");
            try {
                Object address = ForeignAccess.sendExecute(message, targetFunction);
                return (int) ForeignAccess.sendExecute(readPointerNode, TruffleNFI_CAccess.Function.READ_POINTER_INT.getSymbolFunction(), address);
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    private final static class TruffleNFI_SeedsNode extends RNGNode implements SeedsNode {

        @Override
        public void execute(int[] n) {
            init(Function.Seedloc, "(): pointer");
            try {
                Object address = ForeignAccess.sendExecute(message, targetFunction);
                for (int i = 0; i < n.length; i++) {
                    n[i] = (int) ForeignAccess.sendExecute(readPointerNode, TruffleNFI_CAccess.Function.READ_ARRAY_INT.getSymbolFunction(), address, i);
                }
            } catch (Throwable t) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    @Override
    public InitNode createInitNode() {
        return new TruffleNFI_InitNode();
    }

    @Override
    public RandNode createRandNode() {
        return new TruffleNFI_RandNode();
    }

    @Override
    public NSeedNode createNSeedNode() {
        return new TruffleNFI_NSeedNode();
    }

    @Override
    public SeedsNode createSeedsNode() {
        return new TruffleNFI_SeedsNode();
    }
}
