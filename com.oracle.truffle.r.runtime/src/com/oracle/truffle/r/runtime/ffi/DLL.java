/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RError.RErrorException;

/**
 * Support for Dynamically Loaded Libraries.
 */
public class DLL {

    public static class DLLInfo {
        public static final String[] NAMES = new String[]{"DLL name", "Filename", "Dynamic lookup", "Handle"};
        public final String name;
        public final String path;
        public final boolean dynamicLookup;
        private final Object handle;

        DLLInfo(String name, String path, boolean dynamicLookup, Object handle) {
            this.name = name;
            this.path = path;
            this.dynamicLookup = dynamicLookup;
            this.handle = handle;
        }

        /**
         * Return array of values that can be plugged directly into an {@code RList}.
         */
        public Object[] toRValues() {
            Object[] result = new Object[4];
            result[0] = name;
            result[1] = path;
            result[2] = RRuntime.asLogical(dynamicLookup);
            result[3] = System.identityHashCode(handle);
            return result;
        }
    }

    public static class DLLException extends RErrorException {
        private static final long serialVersionUID = 1L;

        DLLException(RError.Message msg, Object... args) {
            super(msg, args);
        }
    }

    private static ArrayList<DLLInfo> list = new ArrayList<>();

    public static DLLInfo load(String path, boolean local, boolean now) throws DLLException {
        File file = new File(Utils.tildeExpand(path));
        String absPath = file.getAbsolutePath();
        Object handle = RFFIFactory.getRFFI().getBaseRFFI().dlopen(absPath, local, now);
        if (handle == null) {
            String dlError = RFFIFactory.getRFFI().getBaseRFFI().dlerror();
            throw new DLLException(RError.Message.DLL_LOAD_ERROR, path, dlError);
        }
        String name = file.getName();
        int dx = name.lastIndexOf('.');
        if (dx > 0) {
            name = name.substring(0, dx);
        }
        DLLInfo result = new DLLInfo(name, absPath, true, handle);
        list.add(result);
        return result;
    }

    public static void unload(String path) throws DLLException {
        String absPath = new File(Utils.tildeExpand(path)).getAbsolutePath();
        for (DLLInfo info : list) {
            if (info.path.equals(absPath)) {
                int rc = RFFIFactory.getRFFI().getBaseRFFI().dlclose(info.handle);
                if (rc != 0) {
                    throw new DLLException(RError.Message.DLL_LOAD_ERROR, path, "");
                }
                return;
            }
        }
        throw new DLLException(RError.Message.DLL_NOT_LOADED, path);
    }

    public static Object[][] getLoadedDLLs() {
        Object[][] result = new Object[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            DLLInfo info = list.get(i);
            result[i] = info.toRValues();
        }
        return result;
    }

    public static class SymbolInfo {
        private final DLLInfo libInfo;
        private final String symbol;
        private final long address;

        SymbolInfo(DLLInfo libInfo, String symbol, long address) {
            this.libInfo = libInfo;
            this.symbol = symbol;
            this.address = address;
        }

        public DLLInfo getLibInfo() {
            return libInfo;
        }

        public long getAddress() {
            return address;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    /**
     * Attempts to locate a symbol in the list of loaded libraries, possible constrained by the
     * {@code libName} argument.
     *
     * @param symbol the symbol name to search for
     * @param libName if not {@code null} restrict search to this library.
     * @return a {@code SymbolInfo} instance or {@code null} if not found.
     */
    public static SymbolInfo findSymbolInfo(String symbol, String libName) {
        long val = 0;
        DLLInfo dllInfo = null;
        for (DLLInfo info : list) {
            if (libName == null || info.name.equals(libName)) {
                val = RFFIFactory.getRFFI().getBaseRFFI().dlsym(info.handle, symbol);
                if (val != 0) {
                    dllInfo = info;
                    break;
                } else {
                    // symbol might actually be zero
                    if (RFFIFactory.getRFFI().getBaseRFFI().dlerror() == null) {
                        dllInfo = info;
                        break;
                    }
                }
            }
        }
        if (dllInfo == null) {
            return null;
        } else {
            return new SymbolInfo(dllInfo, symbol, val);
        }
    }

    public static DLLInfo findLibraryContainingSymbol(String symbol) {
        SymbolInfo symbolInfo = findSymbolInfo(symbol, null);
        if (symbolInfo == null) {
            return null;
        } else {
            return symbolInfo.libInfo;
        }
    }
}
