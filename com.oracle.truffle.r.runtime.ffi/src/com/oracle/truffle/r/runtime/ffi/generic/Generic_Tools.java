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

import java.util.concurrent.Semaphore;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.LibPaths;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;

public class Generic_Tools implements ToolsRFFI {
    private static final class ToolsProvider {
        private static ToolsProvider tools;
        private static DLL.SymbolInfo parseRd;

        @TruffleBoundary
        private ToolsProvider() {
            System.load(LibPaths.getPackageLibPath("tools"));
            parseRd = DLL.findSymbolInfo("C_parseRd", "tools");
        }

        static ToolsProvider toolsProvider() {
            if (tools == null) {
                tools = new ToolsProvider();
            }
            return tools;
        }

        @SuppressWarnings("static-method")
        DLL.SymbolInfo getParseRd() {
            return parseRd;
        }

    }

    private static final Semaphore parseRdCritical = new Semaphore(1, false);

    public Object parseRd(RConnection con, REnvironment srcfile, RLogicalVector verbose, RLogicalVector fragment, RStringVector basename, RLogicalVector warningCalls, Object macros,
                    RLogicalVector warndups) {
        // The C code is not thread safe.
        try {
            parseRdCritical.acquire();
            SymbolInfo parseRd = ToolsProvider.toolsProvider().getParseRd();
            return RFFIFactory.getRFFI().getCallRFFI().invokeCall(parseRd.address, parseRd.symbol, new Object[]{con, srcfile, verbose, fragment, basename, warningCalls, macros, warndups});
        } catch (Throwable ex) {
            throw RInternalError.shouldNotReachHere();
        } finally {
            parseRdCritical.release();
        }
    }

}
