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
package com.oracle.truffle.r.engine.interop.ffi.nfi;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

public final class TruffleNFI_PkgInit {

    private enum UpCall {
        registerRoutines("(object, sint32, sint32, uint64): void"),
        useDynamicSymbols("(object, sint32): sint32"),
        setDotSymbolValues("(object, string, pointer, sint32): object"),
        forceSymbols("(object, sint32): sint32");
        private final String signature;

        UpCall(String signature) {
            this.signature = signature;
        }
    }

    /**
     * The upcalls from native code that support symbol registration.
     */
    interface UpCalls {

        /**
         * This is the start, called from {@code R_RegisterRoutines}.
         *
         * @param dllInfo library the symbols are defined in
         * @param nstOrd the ordinal value corresponding to
         *            {@link com.oracle.truffle.r.runtime.ffi.DLL.NativeSymbolType}.
         * @param num the number of functions being registered
         * @param routines the C address of the function table (not interpreted).
         */
        void registerRoutines(DLLInfo dllInfo, int nstOrd, int num, long routines);

        /**
         * Internal upcall used by {@code Rdynload_setSymbol}. The {@code fun} value must be
         * converted to a {@link TruffleObject} representing the symbol}.
         *
         * @param dllInfo library the symbol is defined in
         * @param name name of function
         * @param fun the C address of the function (in the table)
         * @param numArgs the number of arguments the function takes.
         */
        DotSymbol setDotSymbolValues(DLLInfo dllInfo, String name, TruffleObject fun, int numArgs);

        /**
         * Directly implements {@code R_useDynamicSymbols}.
         */
        int useDynamicSymbols(DLLInfo dllInfo, int value);

        /**
         * Directly implements {@code R_forceSymbols}.
         */
        int forceSymbols(DLLInfo dllInfo, int value);

    }

    private static class UpCallsImpl implements UpCalls {
        /**
         * First create the array, then downcall to native to get the specific info for each symbol
         * which is delivered by {@link #setDotSymbolValues}.
         */
        @Override
        public void registerRoutines(DLLInfo dllInfo, int nstOrd, int num, long routines) {
            DotSymbol[] array = new DotSymbol[num];
            for (int i = 0; i < num; i++) {
                array[i] = setSymbol(dllInfo, nstOrd, routines, i);
            }
            dllInfo.setNativeSymbols(nstOrd, array);
        }

        @Override
        public int useDynamicSymbols(DLLInfo dllInfo, int value) {
            return DLL.useDynamicSymbols(dllInfo, value);
        }

        @Override
        public DotSymbol setDotSymbolValues(DLLInfo dllInfo, String name, TruffleObject fun, int numArgs) {
            /*
             * We don't know the NFI signature at this point, so we cannot bind the function here.
             */
            DotSymbol result = new DotSymbol(name, new SymbolHandle(fun), numArgs);
            return result;
        }

        @Override
        public int forceSymbols(DLLInfo dllInfo, int value) {
            return DLL.forceSymbols(dllInfo, value);
        }
    }

    private static final String SETSYMBOL_SIGNATURE = "(object, sint32, uint64, sint32): object";
    private static TruffleObject setSymbolFunction;

    static void initialize() {
        Node bind = Message.createInvoke(1).createNode();
        SymbolHandle symbolHandle = DLL.findSymbol("Rdynload_init", null);
        Node executeNode = Message.createExecute(2).createNode();
        UpCallsImpl upCalls = new UpCallsImpl();
        TruffleObject upCallsObject = JavaInterop.asTruffleObject(upCalls);
        Node readNode = Message.READ.createNode();
        try {
            for (UpCall upCall : UpCall.values()) {
                Object upCallMethodObject = ForeignAccess.sendRead(readNode, upCallsObject, upCall.name());
                String addCallbackSignature = String.format("(sint32, %s): void", upCall.signature);
                TruffleObject addCallbackFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", addCallbackSignature);
                ForeignAccess.sendExecute(executeNode, addCallbackFunction, upCall.ordinal(), upCallMethodObject);
            }
            symbolHandle = DLL.findSymbol("Rdynload_setSymbol", null);
            setSymbolFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, symbolHandle.asTruffleObject(), "bind", SETSYMBOL_SIGNATURE);
        } catch (Throwable t) {
            throw RInternalError.shouldNotReachHere(t);
        }
    }

    private static DotSymbol setSymbol(DLLInfo dllInfo, int nstOrd, long routines, int index) {
        Node executeNode = Message.createExecute(4).createNode();
        try {
            DotSymbol result = (DotSymbol) ForeignAccess.sendExecute(executeNode, setSymbolFunction, dllInfo, nstOrd, routines, index);
            return result;
        } catch (Throwable t) {
            throw RInternalError.shouldNotReachHere(t);
        }

    }
}
