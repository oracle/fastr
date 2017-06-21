/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.common;

import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.CEntry;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.DotSymbol;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

public abstract class Generic_PkgInit implements PkgInitUpCalls {
    @Override
    public int registerRoutines(DLLInfo dllInfo, int nstOrd, int num, long routines) {
        DotSymbol[] array = new DotSymbol[num];
        for (int i = 0; i < num; i++) {
            Object sym = setSymbol(dllInfo, nstOrd, routines, i);
            array[i] = (DotSymbol) sym;
        }
        dllInfo.setNativeSymbols(nstOrd, array);
        return 0;
    }

    @Override
    public int registerCCallable(String pkgName, String functionName, Object address) {
        DLLInfo lib = DLL.safeFindLibrary(pkgName);
        lib.registerCEntry(new CEntry(functionName, new SymbolHandle(address)));
        return 0;
    }

    @Override
    public int useDynamicSymbols(DLLInfo dllInfo, int value) {
        return DLL.useDynamicSymbols(dllInfo, value);
    }

    @Override
    public int forceSymbols(DLLInfo dllInfo, int value) {
        return DLL.forceSymbols(dllInfo, value);
    }

    @Override
    public DotSymbol setDotSymbolValues(DLLInfo dllInfo, String name, Object fun, int numArgs) {
        DotSymbol result = new DotSymbol(name, new SymbolHandle(fun), numArgs);
        return result;
    }

    protected abstract Object setSymbol(DLLInfo dllInfo, int nstOrd, long routines, int index);
}
