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
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.LibPaths;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;

public class Generic_Tools implements ToolsRFFI {
    private static class Generic_ToolsRFFINode extends ToolsRFFINode {
        private CallRFFI.CallRFFINode callRFFINode = RFFIFactory.getRFFI().getCallRFFI().callRFFINode();

        private static final class ToolsProvider {
            private static final String C_PARSE_RD = "C_parseRd";
            private static ToolsProvider tools;
            private static DLL.SymbolHandle parseRd;

            @TruffleBoundary
            private ToolsProvider() {
                System.load(LibPaths.getPackageLibPath("tools"));
                parseRd = DLL.findSymbol(C_PARSE_RD, "tools", DLL.RegisteredNativeSymbol.any());
                assert parseRd != DLL.SYMBOL_NOT_FOUND;
            }

            static ToolsProvider toolsProvider() {
                if (tools == null) {
                    tools = new ToolsProvider();
                }
                return tools;
            }

            @SuppressWarnings("static-method")
            long getParseRd() {
                return parseRd.asAddress();
            }
        }

        private static final Semaphore parseRdCritical = new Semaphore(1, false);
        private NativeCallInfo nativeCallInfo;

        @Override
        public Object parseRd(RConnection con, REnvironment srcfile, RLogicalVector verbose, RLogicalVector fragment, RStringVector basename, RLogicalVector warningCalls, Object macros,
                        RLogicalVector warndups) {
            // The C code is not thread safe.
            try {
                parseRdCritical.acquire();
                long parseRd = ToolsProvider.toolsProvider().getParseRd();
                if (nativeCallInfo == null) {
                    nativeCallInfo = new NativeCallInfo("parseRd", new SymbolHandle(parseRd), DLL.findLibrary("tools"));
                }
                return callRFFINode.invokeCall(nativeCallInfo,
                                new Object[]{con, srcfile, verbose, fragment, basename, warningCalls, macros, warndups});
            } catch (Throwable ex) {
                throw RInternalError.shouldNotReachHere(ex, "error during Rd parsing");
            } finally {
                parseRdCritical.release();
            }
        }
    }

    @Override
    public ToolsRFFINode toolsRFFINode() {
        return new Generic_ToolsRFFINode();
    }
}
