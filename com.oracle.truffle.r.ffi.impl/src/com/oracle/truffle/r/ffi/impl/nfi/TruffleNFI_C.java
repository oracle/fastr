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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_CFactory.TruffleNFI_InvokeCNodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

public class TruffleNFI_C implements CRFFI {

    private static final String[] SIGNATURES = new String[32];

    private static String getSignatureForArity(int arity) {
        CompilerAsserts.neverPartOfCompilation();
        if (arity >= SIGNATURES.length || SIGNATURES[arity] == null) {
            StringBuilder str = new StringBuilder().append('(');
            for (int i = 0; i < arity; i++) {
                str.append(i > 0 ? ", " : "");
                str.append("pointer");
            }
            String signature = str.append("): void").toString();
            if (arity < SIGNATURES.length) {
                SIGNATURES[arity] = signature;
            }
            return signature;
        } else {
            return SIGNATURES[arity];
        }
    }

    abstract static class TruffleNFI_InvokeCNode extends InvokeCNode {

        @Child private Node bindNode = Message.createInvoke(1).createNode();

        @TruffleBoundary
        protected TruffleObject getFunction(TruffleObject address, int arity) {
            // cache signatures
            try {
                return (TruffleObject) ForeignAccess.sendInvoke(bindNode, address, "bind", getSignatureForArity(arity));
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }

        @Specialization(guards = {"args.length == cachedArgsLength", "nativeCallInfo.address.asTruffleObject() == cachedAddress"})
        protected void invokeCallCached(@SuppressWarnings("unused") NativeCallInfo nativeCallInfo, Object[] args,
                        @SuppressWarnings("unused") @Cached("args.length") int cachedArgsLength,
                        @Cached("createExecute(cachedArgsLength)") Node executeNode,
                        @SuppressWarnings("unused") @Cached("nativeCallInfo.address.asTruffleObject()") TruffleObject cachedAddress,
                        @Cached("getFunction(cachedAddress, cachedArgsLength)") TruffleObject cachedFunction) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    ForeignAccess.sendExecute(executeNode, cachedFunction, args);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        }

        @Specialization(limit = "99", guards = "args.length == cachedArgsLength")
        protected void invokeCallCachedLength(NativeCallInfo nativeCallInfo, Object[] args,
                        @Cached("args.length") int cachedArgsLength,
                        @Cached("createExecute(cachedArgsLength)") Node executeNode) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    ForeignAccess.sendExecute(executeNode, getFunction(nativeCallInfo.address.asTruffleObject(), cachedArgsLength), args);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        }

        public static Node createExecute(int n) {
            return Message.createExecute(n).createNode();
        }
    }

    @Override
    public InvokeCNode createInvokeCNode() {
        return TruffleNFI_InvokeCNodeGen.create();
    }
}
