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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.rng.user.UserRNG.Function;

public class TruffleLLVM_UserRng implements UserRngRFFI {

    private abstract static class RNGNode extends Node {

        @CompilationFinal protected Node message;
        @CompilationFinal protected Node readPointerNode = Message.createExecute(1).createNode();

        protected void init(int argumentCount) {
            if (message == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                message = Message.createExecute(argumentCount).createNode();
            }
        }
    }

    private static final class TruffleLLVMInitNode extends RNGNode implements InitNode {

        @Override
        public void execute(int seed) {
            init(1);
            try {
                ForeignAccess.sendExecute(message, Function.Init.getSymbolHandle().asTruffleObject(), seed);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    private static final class TruffleLLVM_RandNode extends RNGNode implements RandNode {

        @Override
        public double execute() {
            init(0);
            try {
                Object address = ForeignAccess.sendExecute(message, Function.Rand.getSymbolHandle().asTruffleObject());
                return (double) ForeignAccess.sendExecute(readPointerNode, TruffleLLVM_CAccess.Function.READ_POINTER_DOUBLE.getSymbolHandle().asTruffleObject(), address);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    private static final class TruffleLLVM_NSeedNode extends RNGNode implements NSeedNode {

        @Override
        public int execute() {
            init(0);
            try {
                Object address = ForeignAccess.sendExecute(message, Function.NSeed.getSymbolHandle().asTruffleObject());
                return (int) ForeignAccess.sendExecute(readPointerNode, TruffleLLVM_CAccess.Function.READ_POINTER_INT.getSymbolHandle().asTruffleObject(), address);
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    private static final class TruffleLLVM_SeedsNode extends RNGNode implements SeedsNode {

        @Override
        public void execute(int[] n) {
            init(0);
            try {
                Object address = ForeignAccess.sendExecute(message, Function.Seedloc.getSymbolHandle().asTruffleObject());
                for (int i = 0; i < n.length; i++) {
                    Object seed = ForeignAccess.sendExecute(readPointerNode, TruffleLLVM_CAccess.Function.READ_ARRAY_INT.getSymbolHandle().asTruffleObject(), address, i);
                    n[i] = (int) seed;
                }
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    @Override
    public InitNode createInitNode() {
        return new TruffleLLVMInitNode();
    }

    @Override
    public RandNode createRandNode() {
        return new TruffleLLVM_RandNode();
    }

    @Override
    public NSeedNode createNSeedNode() {
        return new TruffleLLVM_NSeedNode();
    }

    @Override
    public SeedsNode createSeedsNode() {
        return new TruffleLLVM_SeedsNode();
    }
}
