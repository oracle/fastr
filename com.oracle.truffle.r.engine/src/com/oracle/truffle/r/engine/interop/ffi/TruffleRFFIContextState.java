/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop.ffi;

import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;

/**
 * A facade for the context state for the Truffle LLVM factory. Delegates to the various
 * module-specific pieces of state. This may get merged into a single instance eventually.
 */
class TruffleRFFIContextState implements ContextState {
    TruffleDLL.ContextStateImpl dllState;
    TrufflePkgInit.ContextStateImpl pkgInitState;
    TruffleCall.ContextStateImpl callState;
    TruffleStats.ContextStateImpl statsState;

    TruffleRFFIContextState() {
        dllState = TruffleDLL.newContextState();
        pkgInitState = TrufflePkgInit.newContextState();
        callState = TruffleCall.newContextState();
        statsState = TruffleStats.newContextState();
    }

    static TruffleRFFIContextState getContextState() {
        return (TruffleRFFIContextState) RContext.getInstance().getStateRFFI();
    }

    static TruffleRFFIContextState getContextState(RContext context) {
        return (TruffleRFFIContextState) context.getStateRFFI();
    }

    @Override
    public ContextState initialize(RContext context) {
        dllState.initialize(context);
        pkgInitState.initialize(context);
        callState.initialize(context);
        statsState.initialize(context);
        return this;
    }

    @Override
    public void beforeDestroy(RContext context) {
        dllState.beforeDestroy(context);
        pkgInitState.beforeDestroy(context);
        callState.beforeDestroy(context);
        statsState.beforeDestroy(context);
    }

}
