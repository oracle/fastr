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
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.LibPaths;

public class TruffleNFI_CAccess {
    private static TruffleNFI_DLL.NFIHandle handle;

    public enum Function {
        READ_POINTER_INT("(pointer): sint32"),
        READ_ARRAY_INT("(pointer, sint64): sint32"),
        READ_POINTER_DOUBLE("(pointer): double"),
        READ_ARRAY_DOUBLE("(pointer, sint32): double");

        private TruffleObject symbolFunction;
        private final String signature;

        Function(String signature) {
            this.signature = signature;

        }

        public TruffleObject getSymbolFunction() {
            if (handle == null) {
                handle = (TruffleNFI_DLL.NFIHandle) DLLRFFI.DLOpenRootNode.create().getCallTarget().call(LibPaths.getBuiltinLibPath("caccess"), true, true);
            }
            if (symbolFunction == null) {
                DLL.SymbolHandle symbolHandle = (DLL.SymbolHandle) DLLRFFI.DLSymRootNode.create().getCallTarget().call(handle, cName());
                assert symbolHandle != null;
                Node bind = Message.createInvoke(1).createNode();
                try {
                    symbolFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", signature);
                } catch (InteropException ex) {
                    throw RInternalError.shouldNotReachHere(ex);
                }

            }
            return symbolFunction;
        }

        public String cName() {
            return "caccess_" + name().toLowerCase();
        }
    }

}
