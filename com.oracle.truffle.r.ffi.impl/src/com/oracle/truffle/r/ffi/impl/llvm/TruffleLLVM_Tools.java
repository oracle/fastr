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
package com.oracle.truffle.r.ffi.impl.llvm;

import java.io.IOException;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.r.ffi.impl.common.Generic_Tools;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;

public class TruffleLLVM_Tools implements ToolsRFFI {

    private static TruffleObject truffleToolsTruffleObject;

    TruffleLLVM_Tools() {
        truffleToolsTruffleObject = JavaInterop.asTruffleObject(this);
    }

    static class ContextStateImpl implements RContext.ContextState {
        @Override
        public ContextState initialize(RContext context) {
            RFFIFactory.getRFFI().getToolsRFFI();
            context.getEnv().exportSymbol("_fastr_rffi_tools", truffleToolsTruffleObject);
            return this;
        }
    }

    private static class TruffleLLVM_ToolsRFFINode extends Generic_Tools.Generic_ToolsRFFINode {
        /**
         * Invoke C implementation, N.B., code is not thread safe.
         */
        @Override
        public synchronized Object execute(RConnection con, REnvironment srcfile, RLogicalVector verbose, RLogicalVector fragment, RStringVector basename, RLogicalVector warningCalls, Object macros,
                        RLogicalVector warndups) {
            Object result = super.execute(con, srcfile, verbose, fragment, basename, warningCalls, macros, warndups);
            return result;
        }
    }

    public static int getC(RConnection conn) {
        try {
            return conn.getc();
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Override
    public ParseRdNode createParseRdNode() {
        return new TruffleLLVM_ToolsRFFINode();
    }

}
