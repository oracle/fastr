/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI;

public class TruffleNFI_REmbed implements REmbedRFFI {

    private static class TruffleNFI_ReadConsoleNode extends TruffleNFI_DownCallNode implements ReadConsoleNode {
        @Child private Node unboxNode;

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.rembedded_read_console;
        }

        @Override
        public String execute(String prompt) {
            Object result = call(prompt);
            if (result instanceof String) {
                return (String) result;
            }
            assert result instanceof TruffleObject : "NFI is expected to send us TruffleObject or String";
            if (unboxNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                unboxNode = insert(Message.UNBOX.createNode());
            }
            try {
                return (String) ForeignAccess.sendUnbox(unboxNode, (TruffleObject) result);
            } catch (ClassCastException | UnsupportedMessageException e) {
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere("Unboxing TruffleObject from NFI, which should be String wrapper, failed. " + e.getMessage());
            }
        }
    }

    private static class TruffleNFI_WriteConsoleNode extends TruffleNFI_DownCallNode implements WriteConsoleNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.rembedded_write_console;
        }

        @Override
        public void execute(String x) {
            call(x, x.length());
        }
    }

    private static class TruffleNFI_WriteErrConsoleNode extends TruffleNFI_DownCallNode implements WriteErrConsoleNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.rembedded_write_err_console;
        }

        @Override
        public void execute(String x) {
            call(x, x.length());
        }
    }

    @Override
    public ReadConsoleNode createReadConsoleNode() {
        return new TruffleNFI_ReadConsoleNode();
    }

    @Override
    public WriteConsoleNode createWriteConsoleNode() {
        return new TruffleNFI_WriteConsoleNode();
    }

    @Override
    public WriteErrConsoleNode createWriteErrConsoleNode() {
        return new TruffleNFI_WriteErrConsoleNode();
    }
}
