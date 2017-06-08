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
package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.RFFIRootNode;

public class TruffleLLVM_Utils {
    public static long getNativeAddress(TruffleObject llvmTruffleAddress) {
        if (asPointerRootNode == null) {
            asPointerRootNode = new AsPointerRootNode();
        }
        long result = (long) asPointerRootNode.getCallTarget().call(llvmTruffleAddress);
        return result;
    }

    static Object checkNativeAddress(Object object) {
        if (object instanceof RTruffleObject) {
            return object;
        }
        TruffleObject useObj = (TruffleObject) object;
        TruffleObject foo = NativePointer.check(useObj);
        if (foo != null) {
            useObj = foo;
        }
        return useObj;
    }

    static final class AsPointerRootNode extends RFFIRootNode<AsPointerNode> {
        private AsPointerRootNode() {
            super(new AsPointerNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((TruffleObject) args[0]);
        }
    }

    private static AsPointerRootNode asPointerRootNode;

    private static class AsPointerNode extends Node {
        @Child private Node asPointerMessageNode = Message.AS_POINTER.createNode();

        public long execute(TruffleObject pointer) {
            try {
                long result = ForeignAccess.sendAsPointer(asPointerMessageNode, pointer);
                return result;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }
}
