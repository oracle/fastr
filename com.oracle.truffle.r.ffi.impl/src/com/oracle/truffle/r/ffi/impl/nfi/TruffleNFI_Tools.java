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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.Generic_Tools;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.ffi.impl.interop.tools.RConnGetCCall;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;

public class TruffleNFI_Tools implements ToolsRFFI {

    private static class TruffleNFI_ToolsRFFINode extends Generic_Tools.Generic_ToolsRFFINode {
        private static boolean initialized;

        @Child private DLLRFFI.DLSymNode dysymNode = DLLRFFI.DLSymNode.create();

        @Override
        public synchronized Object execute(RConnection con, REnvironment srcfile, RLogicalVector verbose, RLogicalVector fragment, RStringVector basename, RLogicalVector warningCalls, Object macros,
                        RLogicalVector warndups) {
            if (!initialized) {
                initCallback();
                initialized = true;
            }
            return super.execute(con, srcfile, verbose, fragment, basename, warningCalls, macros, warndups);
        }

        private void initCallback() {
            DLLInfo toolsDLLInfo = DLL.findLibrary(TOOLS);
            assert toolsDLLInfo != null;
            SymbolHandle symbolHandle = dysymNode.execute(toolsDLLInfo.handle, "gramRd_nfi_init");
            assert symbolHandle != DLL.SYMBOL_NOT_FOUND;
            Node bind = Message.createInvoke(1).createNode();
            Node executeNode = Message.createExecute(1).createNode();
            try {
                TruffleObject function = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", "(env, (object): sint32): void");
                ForeignAccess.sendExecute(executeNode, function, new RConnGetCCall());
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    @Override
    public ParseRdNode createParseRdNode() {
        return new TruffleNFI_ToolsRFFINode();
    }
}
