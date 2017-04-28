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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.interop.ffi.nfi.TruffleNFI_CFactory.TruffleNFI_InvokeCNodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

public class TruffleNFI_C implements CRFFI {
    public abstract static class TruffleNFI_InvokeCNode extends InvokeCNode {

        @Child Node bindNode = Message.createInvoke(1).createNode();

        @Specialization(guards = "args.length == 0")
        protected void invokeCall0(NativeCallInfo nativeCallInfo, @SuppressWarnings("unused") Object[] args,
                        @Cached("createExecute(args.length)") Node executeNode) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                    nativeCallInfo.address.asTruffleObject(), "bind", "(): object");
                    ForeignAccess.sendExecute(executeNode, callFunction);
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
