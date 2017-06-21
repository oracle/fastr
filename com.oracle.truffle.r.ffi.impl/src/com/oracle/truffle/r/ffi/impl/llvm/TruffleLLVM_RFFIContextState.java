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

import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.DLL;

/**
 * A facade for the context state for the Truffle LLVM factory. Delegates to the various
 * module-specific pieces of state.
 */
class TruffleLLVM_RFFIContextState implements ContextState {
    TruffleLLVM_DLL.ContextStateImpl dllState;
    TruffleLLVM_PkgInit.ContextStateImpl pkgInitState;
    TruffleLLVM_Call.ContextStateImpl callState;

    TruffleLLVM_RFFIContextState() {
        dllState = new TruffleLLVM_DLL.ContextStateImpl();
        pkgInitState = new TruffleLLVM_PkgInit.ContextStateImpl();
        callState = new TruffleLLVM_Call.ContextStateImpl();
    }

    static TruffleLLVM_RFFIContextState getContextState() {
        return (TruffleLLVM_RFFIContextState) RContext.getInstance().getStateRFFI();
    }

    static TruffleLLVM_RFFIContextState getContextState(RContext context) {
        return (TruffleLLVM_RFFIContextState) context.getStateRFFI();
    }

    @Override
    public ContextState initialize(RContext context) {
        if (context.isInitial()) {
            String librffiPath = LibPaths.getBuiltinLibPath("R");
            DLL.loadLibR(librffiPath);
        }
        dllState.initialize(context);
        pkgInitState.initialize(context);
        callState.initialize(context);
        return this;
    }

    @Override
    public void beforeDestroy(RContext context) {
        dllState.beforeDestroy(context);
        pkgInitState.beforeDestroy(context);
        callState.beforeDestroy(context);
    }
}
