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
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.common.Generic_PkgInit;
import com.oracle.truffle.r.ffi.impl.common.PkgInitUpCalls;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.ForceSymbolsCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.GetCCallableCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.RegisterCCallableCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.RegisterRoutinesCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.SetDotSymbolValuesCall;
import com.oracle.truffle.r.ffi.impl.interop.pkginit.UseDynamicSymbolsCall;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

public final class TruffleNFI_PkgInit extends Generic_PkgInit {

    private static final String SETSYMBOL_SIGNATURE = "(object, sint32, uint64, sint32): object";
    private static TruffleObject setSymbolFunction;

    static void initialize() {
        Node bind = Message.createInvoke(1).createNode();
        SymbolHandle symbolHandle = DLL.findSymbol("Rdynload_init", null);
        Node executeNode = Message.createExecute(2).createNode();
        TruffleNFI_PkgInit trufflePkgInit = new TruffleNFI_PkgInit();
        try {
            for (PkgInitUpCalls.Index upCall : PkgInitUpCalls.Index.values()) {
                String addCallbackSignature = String.format("(env, sint32, %s): void", upCall.signature);
                TruffleObject addCallbackFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", addCallbackSignature);
                TruffleObject callbackObject;
                switch (upCall) {
                    case registerRoutines:
                        callbackObject = new RegisterRoutinesCall(trufflePkgInit);
                        break;
                    case setDotSymbolValues:
                        callbackObject = new SetDotSymbolValuesCall(trufflePkgInit);
                        break;

                    case useDynamicSymbols:
                        callbackObject = new UseDynamicSymbolsCall(trufflePkgInit);
                        break;
                    case forceSymbols:
                        callbackObject = new ForceSymbolsCall(trufflePkgInit);
                        break;
                    case registerCCallable:
                        callbackObject = new RegisterCCallableCall(trufflePkgInit);
                        break;
                    case getCCallable:
                        callbackObject = new GetCCallableCall(trufflePkgInit);
                        break;
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
                ForeignAccess.sendExecute(executeNode, addCallbackFunction, upCall.ordinal(), callbackObject);
            }
            symbolHandle = DLL.findSymbol("Rdynload_setSymbol", null);
            setSymbolFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", SETSYMBOL_SIGNATURE);
        } catch (Throwable t) {
            throw RInternalError.shouldNotReachHere(t);
        }
    }

    @Override
    public Object getCCallable(String pkgName, String functionName) {
        DLLInfo lib = DLL.safeFindLibrary(pkgName);
        CEntry result = lib.lookupCEntry(functionName);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, RError.Message.UNKNOWN_OBJECT, functionName);
        }
        return result.address.asAddress();
    }

    @Override
    protected Object setSymbol(DLLInfo dllInfo, int nstOrd, long routines, int index) {
        Node executeNode = Message.createExecute(4).createNode();
        try {
            DotSymbol result = (DotSymbol) ForeignAccess.sendExecute(executeNode, setSymbolFunction, dllInfo, nstOrd, routines, index);
            return result;
        } catch (Throwable t) {
            throw RInternalError.shouldNotReachHere(t);
        }
    }
}
