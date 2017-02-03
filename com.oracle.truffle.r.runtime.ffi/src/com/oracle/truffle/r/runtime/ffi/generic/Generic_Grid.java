/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.generic;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.GridRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public class Generic_Grid implements GridRFFI {
    private static final String GRID = "grid";

    private static class Generic_InitGridNode extends InitGridNode {
        private static final String L_InitGrid = "L_initGrid";
        @Child private CallRFFI.InvokeCallNode invokeCallNode = RFFIFactory.getRFFI().getCallRFFI().createInvokeCallNode();
        @Child DLL.RFindSymbolNode findSymbolNode = DLL.RFindSymbolNode.create();
        @CompilationFinal private NativeCallInfo initNativeCallInfo;

        @Override
        public Object execute(REnvironment gridEvalEnv) {
            if (initNativeCallInfo == null) {
                initNativeCallInfo = createNativeCallInfo(L_InitGrid, findSymbolNode);
            }
            return invokeCallNode.execute(initNativeCallInfo, new Object[]{gridEvalEnv});
        }
    }

    private static class Generic_KillGridNode extends KillGridNode {
        private static final String L_KillGrid = "L_killGrid";
        @Child private CallRFFI.InvokeCallNode invokeCallNode = RFFIFactory.getRFFI().getCallRFFI().createInvokeCallNode();
        @Child DLL.RFindSymbolNode findSymbolNode = DLL.RFindSymbolNode.create();
        @CompilationFinal private NativeCallInfo killNativeCallInfo;

        @Override
        public Object execute() {
            if (killNativeCallInfo == null) {
                killNativeCallInfo = createNativeCallInfo(L_KillGrid, findSymbolNode);
            }
            return invokeCallNode.execute(killNativeCallInfo, new Object[0]);
        }

    }

    private static NativeCallInfo createNativeCallInfo(String call, DLL.RFindSymbolNode findSymbolNode) {
        DLLInfo dllInfo = DLL.findLibrary(GRID);
        assert dllInfo != null;
        SymbolHandle symbolHandle = findSymbolNode.execute(call, GRID, DLL.RegisteredNativeSymbol.any());
        assert symbolHandle != DLL.SYMBOL_NOT_FOUND;
        return new NativeCallInfo(call, symbolHandle, dllInfo);
    }

    @Override
    public InitGridNode createInitGridNode() {
        return new Generic_InitGridNode();
    }

    @Override
    public KillGridNode createKillGridNode() {
        return new Generic_KillGridNode();
    }

}
