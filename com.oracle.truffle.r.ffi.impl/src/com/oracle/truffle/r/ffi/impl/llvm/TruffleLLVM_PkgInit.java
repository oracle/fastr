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
package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.PkgInitUpCalls;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.ForceSymbolsCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.GetCCallableCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.RegisterCCallableCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.RegisterRoutinesCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.SetDotSymbolValuesCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.UseDynamicSymbolsCall;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.ffi.impl.common.Generic_PkgInit;

final class TruffleLLVM_PkgInit extends Generic_PkgInit {

    private static TruffleLLVM_PkgInit trufflePkgInit;
    private static TruffleObject setSymbolHandle;

    static class ContextStateImpl implements RContext.ContextState {
        @Override
        public ContextState initialize(RContext context) {
            if (context.isInitial()) {
                TruffleLLVM_PkgInit.initialize(context);
            }
            return this;
        }
    }

    private static void initialize(RContext context) {
        trufflePkgInit = new TruffleLLVM_PkgInit();
        setSymbolHandle = new SymbolHandle(context.getEnv().importSymbol("@" + "Rdynload_setSymbol")).asTruffleObject();
        Node executeNode = Message.createExecute(2).createNode();
        TruffleObject callbackSymbol = new SymbolHandle(context.getEnv().importSymbol("@" + "Rdynload_addCallback")).asTruffleObject();
        try {
            ForeignAccess.sendExecute(executeNode, callbackSymbol, PkgInitUpCalls.Index.registerRoutines.ordinal(), new RegisterRoutinesCall(trufflePkgInit));
            ForeignAccess.sendExecute(executeNode, callbackSymbol, PkgInitUpCalls.Index.setDotSymbolValues.ordinal(), new SetDotSymbolValuesCall(trufflePkgInit));
            ForeignAccess.sendExecute(executeNode, callbackSymbol, PkgInitUpCalls.Index.useDynamicSymbols.ordinal(), new UseDynamicSymbolsCall(trufflePkgInit));
            ForeignAccess.sendExecute(executeNode, callbackSymbol, PkgInitUpCalls.Index.forceSymbols.ordinal(), new ForceSymbolsCall(trufflePkgInit));
            ForeignAccess.sendExecute(executeNode, callbackSymbol, PkgInitUpCalls.Index.registerCCallable.ordinal(), new RegisterCCallableCall(trufflePkgInit));
            ForeignAccess.sendExecute(executeNode, callbackSymbol, PkgInitUpCalls.Index.getCCallable.ordinal(), new GetCCallableCall(trufflePkgInit));
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Override
    public Object getCCallable(String pkgName, String functionName) {
        DLLInfo lib = DLL.safeFindLibrary(pkgName);
        CEntry result = lib.lookupCEntry(functionName);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, RError.Message.UNKNOWN_OBJECT, functionName);
        }
        return result.address.asTruffleObject();
    }

    @Override
    protected Object setSymbol(DLLInfo dllInfo, int nstOrd, long routines, int index) {
        Node executeNode = Message.createExecute(4).createNode();
        try {
            return ForeignAccess.sendExecute(executeNode, setSymbolHandle, dllInfo, nstOrd, routines, index);
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }
}
