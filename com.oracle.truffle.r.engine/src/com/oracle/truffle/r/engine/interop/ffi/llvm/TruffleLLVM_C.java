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
package com.oracle.truffle.r.engine.interop.ffi.llvm;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.interop.NativeDoubleArray;
import com.oracle.truffle.r.engine.interop.NativeIntegerArray;
import com.oracle.truffle.r.engine.interop.NativeRawArray;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.jni.JNI_C.JNI_InvokeCNode;

class TruffleLLVM_C implements CRFFI {
    private static class TruffleLLVM_InvokeCNode extends JNI_InvokeCNode {

        @Override
        public synchronized void execute(NativeCallInfo nativeCallInfo, Object[] args) {
            if (nativeCallInfo.address.value instanceof Long) {
                super.execute(nativeCallInfo, args);
            } else {
                TruffleLLVM_DLL.ensureParsed(nativeCallInfo);
                Object[] wargs = wrap(args);
                try {
                    Node messageNode = Message.createExecute(0).createNode();
                    ForeignAccess.sendExecute(messageNode, nativeCallInfo.address.asTruffleObject(), wargs);
                } catch (Throwable t) {
                    throw RInternalError.shouldNotReachHere(t);
                }
            }
        }

        Object[] wrap(Object[] args) {
            Object[] nargs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                Object narg;
                if (arg instanceof int[]) {
                    narg = new NativeIntegerArray((int[]) arg);
                } else if (arg instanceof double[]) {
                    narg = new NativeDoubleArray((double[]) arg);
                } else if (arg instanceof byte[]) {
                    narg = new NativeRawArray((byte[]) arg);
                } else {
                    throw RInternalError.unimplemented(".C type: " + arg.getClass().getSimpleName());
                }
                nargs[i] = narg;
            }
            return nargs;
        }
    }

    @Override
    public InvokeCNode createInvokeCNode() {
        return new TruffleLLVM_InvokeCNode();
    }
}
