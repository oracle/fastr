/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop.ffi;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.truffle.TruffleRFFIFrameHelper;

class TrufflePkgInit {

    private static TrufflePkgInit trufflePkgInit;
    private static TruffleObject trufflePkgInitTruffleObject;

    static class ContextStateImpl implements RContext.ContextState {
        @Override
        public ContextState initialize(RContext context) {
            context.addExportedSymbol("_fastr_rffi_pkginit", trufflePkgInitTruffleObject);
            return this;
        }

        @Override
        public void beforeDestroy(RContext context) {
        }
    }

    static ContextStateImpl newContextState() {
        return new ContextStateImpl();
    }

    static TrufflePkgInit initialize() {
        if (trufflePkgInit == null) {
            trufflePkgInit = new TrufflePkgInit();
            trufflePkgInitTruffleObject = JavaInterop.asTruffleObject(trufflePkgInit);
        }
        return trufflePkgInit;
    }

    public void registerRoutines(DLLInfo dllInfo, int nstOrd, int num, long routines) {
        DotSymbol[] array = new DotSymbol[num];
        SymbolHandle setSymbolHandle = new SymbolHandle(RContext.getInstance().getEnv().importSymbol("@" + "PkgInit_setSymbol"));
        for (int i = 0; i < num; i++) {
            Object sym = setSymbol(nstOrd, routines, i, setSymbolHandle);
            array[i] = (DotSymbol) sym;
        }
        dllInfo.setNativeSymbols(nstOrd, array);
    }

    private static Object setSymbol(int nstOrd, long routines, int index, SymbolHandle symbolHandle) {
        VirtualFrame frame = TruffleRFFIFrameHelper.create();
        Node executeNode = Message.createExecute(3).createNode();
        try {

            Object result = ForeignAccess.sendExecute(executeNode, frame, symbolHandle.asTruffleObject(), nstOrd, routines, index);
            return result;
        } catch (Throwable t) {
            throw RInternalError.shouldNotReachHere();
        }

    }

    @SuppressWarnings("unused")
    public void registerCCallable(String pkgName, String functionName, long address) {
        // TBD
        System.console();
    }

    @SuppressWarnings({"unused", "static-method"})
    private long getCCallable(String pkgName, String functionName) {
        // TBD
        throw RInternalError.unimplemented();
    }

    /**
     * Upcall from native to create a {@link DotSymbol} value.
     */
    public DotSymbol createDotSymbol(String name, Object fundesc, int numArgs) {
        return new DotSymbol(name, new SymbolHandle(fundesc), numArgs);
    }

    public int useDynamicSymbols(DLLInfo dllInfo, int value) {
        return DLL.useDynamicSymbols(dllInfo, value);
    }

    public int forceSymbols(DLLInfo dllInfo, int value) {
        return DLL.forceSymbols(dllInfo, value);
    }

}
