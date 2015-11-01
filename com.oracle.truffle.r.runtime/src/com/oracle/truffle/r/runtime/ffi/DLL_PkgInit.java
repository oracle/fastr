/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;

/**
 * Methods called from native code during library loading. These methods are single threaded by
 * virtue of the Semaphore in {@link DLL#loadPackageDLL}. This is a {code static} facade to keep
 * things simple in the C code (avoid a pass down).
 */
public class DLL_PkgInit {
    private static PkgInitRFFI pkgInit;

    public static void initialize(PkgInitRFFI pkgInitArg) {
        DLL_PkgInit.pkgInit = pkgInitArg;
    }

    public static void registerRoutines(DLLInfo dllInfo, int nstOrd, int num, long routines) {
        pkgInit.registerRoutines(dllInfo, nstOrd, num, routines);
    }

    public static int useDynamicSymbols(DLLInfo dllInfo, int value) {
        return pkgInit.useDynamicSymbols(dllInfo, value);
    }

    public static int forceSymbols(DLLInfo dllInfo, int value) {
        return pkgInit.forceSymbols(dllInfo, value);
    }

    public static void registerCCallable(String pkgName, String functionName, long address) {
        pkgInit.registerCCallable(pkgName, functionName, address);
    }

    public static long getCCallable(String pkgName, String functionName) {
        return pkgInit.getCCallable(pkgName, functionName);
    }

}
