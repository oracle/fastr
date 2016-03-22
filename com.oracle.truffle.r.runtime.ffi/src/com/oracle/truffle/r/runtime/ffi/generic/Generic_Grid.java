/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;
import com.oracle.truffle.r.runtime.ffi.GridRFFI;
import com.oracle.truffle.r.runtime.ffi.LibPaths;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public class Generic_Grid implements GridRFFI {
    private static final class GridProvider {
        private static GridProvider grid;
        private static DLL.SymbolInfo initGrid;
        private static DLL.SymbolInfo killGrid;

        @TruffleBoundary
        private GridProvider() {
            System.load(LibPaths.getPackageLibPath("grid"));
            initGrid = DLL.findSymbolInfo("L_initGrid", "grid");
            killGrid = DLL.findSymbolInfo("L_killGrid", "grid");
        }

        static GridProvider gridProvider() {
            if (grid == null) {
                grid = new GridProvider();
            }
            return grid;
        }

        @SuppressWarnings("static-method")
        DLL.SymbolInfo getInitGrid() {
            return initGrid;
        }

        @SuppressWarnings("static-method")
        DLL.SymbolInfo getKillGrid() {
            return killGrid;
        }
    }

    @Override
    public Object initGrid(REnvironment gridEvalEnv) {
        SymbolInfo initGrid = GridProvider.gridProvider().getInitGrid();
        return RFFIFactory.getRFFI().getCallRFFI().invokeCall(initGrid.address, initGrid.symbol, new Object[]{gridEvalEnv});
    }

    @Override
    public Object killGrid() {
        SymbolInfo killGrid = GridProvider.gridProvider().getKillGrid();
        return RFFIFactory.getRFFI().getCallRFFI().invokeCall(killGrid.address, killGrid.symbol, new Object[0]);
    }
}
