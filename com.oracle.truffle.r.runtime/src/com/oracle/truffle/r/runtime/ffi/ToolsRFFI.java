/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

/**
 * Interface to native (C) methods provided by the {@code tools} package.
 */
public final class ToolsRFFI {
    public static class ParseRdNode extends Node {
        private static final String C_PARSE_RD = "C_parseRd";
        protected static final String TOOLS = "tools";

        @Child private CallRFFI.InvokeCallNode callRFFINode = RFFIFactory.getCallRFFI().createInvokeCallNode();
        @Child private DLL.RFindSymbolNode findSymbolNode = DLL.RFindSymbolNode.create();

        @CompilationFinal private NativeCallInfo nativeCallInfo;

        /**
         * This invokes the Rd parser, written in C, and part of GnuR, that does its work using the
         * R FFI interface. The R code initially invokes this via {@code .External2(C_parseRd, ...)}
         * , which has a custom specialization in the implementation of the {@code .External2}
         * builtin. That does some work in Java, and then calls this method to invoke the actual C
         * code. We can't go straight to the GnuR C entry point as that makes GnuR-specific
         * assumptions about, for example, how connections are implemented.
         */
        public Object execute(RConnection con, REnvironment srcfile, RLogicalVector verbose, RLogicalVector fragment, RStringVector basename, RLogicalVector warningCalls, Object macros,
                        RLogicalVector warndups) {
            synchronized (ToolsRFFI.class) {
                try {
                    if (nativeCallInfo == null) {
                        // lookup entry point (assert library is loaded)
                        DLLInfo toolsDLLInfo = DLL.findLibrary(TOOLS);
                        assert toolsDLLInfo != null;
                        SymbolHandle symbolHandle = findSymbolNode.execute(C_PARSE_RD, TOOLS, DLL.RegisteredNativeSymbol.any());
                        assert symbolHandle != DLL.SYMBOL_NOT_FOUND;
                        nativeCallInfo = new NativeCallInfo(C_PARSE_RD, symbolHandle, toolsDLLInfo);
                    }
                    return callRFFINode.dispatch(nativeCallInfo,
                                    new Object[]{con, srcfile, verbose, fragment, basename, warningCalls, macros, warndups});
                } catch (Throwable ex) {
                    throw RInternalError.shouldNotReachHere(ex, "error during Rd parsing" + ex.getMessage());
                }
            }
        }
    }

    public static ParseRdNode createParseRdNode() {
        return new ParseRdNode();
    }
}
