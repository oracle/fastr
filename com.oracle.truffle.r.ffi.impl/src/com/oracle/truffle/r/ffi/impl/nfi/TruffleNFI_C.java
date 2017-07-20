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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.nfi.TruffleNFI_CFactory.TruffleNFI_InvokeCNodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

public class TruffleNFI_C implements CRFFI {
    public abstract static class TruffleNFI_InvokeCNode extends InvokeCNode {

        @Child private Node bindNode = Message.createInvoke(1).createNode();

        @Specialization(guards = "args.length == 0")
        protected void invokeCall0(NativeCallInfo nativeCallInfo, @SuppressWarnings("unused") Object[] args, @SuppressWarnings("unused") boolean hasStrings,
                        @Cached("createExecute(args.length)") Node executeNode) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                    nativeCallInfo.address.asTruffleObject(), "bind", "(): void");
                    ForeignAccess.sendExecute(executeNode, callFunction);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        }

        @Specialization(guards = "args.length == 1")
        protected void invokeCall1(NativeCallInfo nativeCallInfo, Object[] args, @SuppressWarnings("unused") boolean hasStrings,
                        @Cached("createExecute(args.length)") Node executeNode) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    Object[] nargs = new Object[args.length];
                    TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                    nativeCallInfo.address.asTruffleObject(), "bind", getSignature(args, nargs));
                    ForeignAccess.sendExecute(executeNode, callFunction, nargs[0]);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        }

        @Specialization(guards = "args.length == 2")
        protected void invokeCall2(NativeCallInfo nativeCallInfo, Object[] args, @SuppressWarnings("unused") boolean hasStrings,
                        @Cached("createExecute(args.length)") Node executeNode) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    Object[] nargs = new Object[args.length];
                    TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                    nativeCallInfo.address.asTruffleObject(), "bind", getSignature(args, nargs));
                    ForeignAccess.sendExecute(executeNode, callFunction, nargs[0], nargs[1]);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        }

        @Specialization(guards = "args.length == 3")
        protected void invokeCall3(NativeCallInfo nativeCallInfo, Object[] args, @SuppressWarnings("unused") boolean hasStrings,
                        @Cached("createExecute(args.length)") Node executeNode) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    Object[] nargs = new Object[args.length];
                    TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                    nativeCallInfo.address.asTruffleObject(), "bind", getSignature(args, nargs));
                    ForeignAccess.sendExecute(executeNode, callFunction, nargs[0], nargs[1], nargs[2]);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        }

        @Specialization(guards = "args.length == 4")
        protected void invokeCall4(NativeCallInfo nativeCallInfo, Object[] args, @SuppressWarnings("unused") boolean hasStrings,
                        @Cached("createExecute(args.length)") Node executeNode) {
            synchronized (TruffleNFI_Call.class) {
                try {
                    Object[] nargs = new Object[args.length];
                    TruffleObject callFunction = (TruffleObject) ForeignAccess.sendInvoke(bindNode,
                                    nativeCallInfo.address.asTruffleObject(), "bind", getSignature(args, nargs));
                    ForeignAccess.sendExecute(executeNode, callFunction, nargs[0], nargs[1], nargs[2], nargs[3]);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }
            }
        }

        @Fallback
        protected void invokeCallN(@SuppressWarnings("unused") NativeCallInfo nativeCallInfo, @SuppressWarnings("unused") Object[] args, @SuppressWarnings("unused") boolean hasStrings) {
            synchronized (TruffleNFI_Call.class) {
                throw RInternalError.unimplemented(".C (too many args)");
            }
        }

        public static Node createExecute(int n) {
            return Message.createExecute(n).createNode();
        }
    }

    private static String getSignature(Object[] args, Object[] nargs) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof int[]) {
                sb.append("[sint32]");
            } else if (arg instanceof double[]) {
                sb.append("[double]");
            } else if (arg instanceof byte[]) {
                sb.append("[uint8]");
            } else {
                throw RInternalError.unimplemented(".C type: " + arg.getClass().getSimpleName());
            }
            nargs[i] = JavaInterop.asTruffleObject(arg);
            if (i < args.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("): void");
        return sb.toString();
    }

    @Override
    public InvokeCNode createInvokeCNode() {
        return TruffleNFI_InvokeCNodeGen.create();
    }
}
