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
package com.oracle.truffle.r.runtime.ffi.jni;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

/**
 * The JNI-based implementation of the package init code. This is only up-called from JNI.
 */
final class JNI_PkgInit {

    private static void registerRoutines(DLLInfo dllInfo, int nstOrd, int num, long routines) {
        DotSymbol[] array = new DotSymbol[num];
        for (int i = 0; i < num; i++) {
            array[i] = setSymbol(nstOrd, routines, i);
        }
        dllInfo.setNativeSymbols(nstOrd, array);
    }

    private static void registerCCallable(String pkgName, String functionName, long address) {
        DLLInfo lib = safeFindLibrary(pkgName);
        lib.registerCEntry(new CEntry(functionName, new SymbolHandle(address)));
    }

    public static long getCCallable(String pkgName, String functionName) {
        DLLInfo lib = safeFindLibrary(pkgName);
        CEntry result = lib.lookupCEntry(functionName);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, Message.UNKNOWN_OBJECT, functionName);
        }
        return result.address.asAddress();
    }

    /**
     * Upcall from native to create a {@link DotSymbol} value.
     */
    private static DotSymbol setDotSymbolValues(String name, long fun, int numArgs) {
        return new DotSymbol(name, new SymbolHandle(fun), numArgs);
    }

    private static native DotSymbol setSymbol(int nstOrd, long routines, int index);

    public static int useDynamicSymbols(DLLInfo dllInfo, int value) {
        return DLL.useDynamicSymbols(dllInfo, value);
    }

    public static int forceSymbols(DLLInfo dllInfo, int value) {
        return DLL.forceSymbols(dllInfo, value);
    }

    public static DLLInfo getEmbeddingDllInfo() {
        return DLL.getEmbeddingDLLInfo();
    }

    @SuppressWarnings("unused")
    public static int findSymbol(String name, String pkg, DLL.RegisteredNativeSymbol rns) {
        throw RInternalError.unimplemented();
    }

    private static DLLInfo safeFindLibrary(String pkgName) {
        DLLInfo lib = DLL.findLibrary(pkgName);
        if (lib == null) {
            // It seems GNU R would create an C entry even for non-existing package, we are more
            // defensive
            throw RError.error(RError.NO_CALLER, Message.DLL_NOT_LOADED, pkgName);
        }
        return lib;
    }
}
