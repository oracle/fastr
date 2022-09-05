/*
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2014, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.library.graphics;

import static com.oracle.truffle.r.runtime.context.FastROptions.LoadPackagesNativeCode;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

/**
 * A placeholder to keep {@code REngine} limited to calling the {@link #initialize} method. The code
 * in R has a hard-coded invocation to InitGraphics in it. This initialization either invokes it
 * too, or it runs a Java version of it if the internal grid package implementation is to be used.
 */
public class RGraphics {
    public static void initialize(RContext context) {
        if (!context.graphicsInitialized) {
            callVoidNativeFunction(context, "InitGraphics");
            context.graphicsInitialized = true;
        }
    }

    public static void dispose(RContext context) {
        callVoidNativeFunction(context, "KillAllDevices");
        context.graphicsInitialized = false;
    }

    private static void callVoidNativeFunction(RContext context, String funcName) {
        assert context.getOption(LoadPackagesNativeCode);
        DLL.DLLInfo dllInfo = DLL.findLibraryContainingSymbol(context, funcName);
        DLL.SymbolHandle symbolHandle = DLL.findSymbol(funcName, dllInfo);
        assert symbolHandle != DLL.SYMBOL_NOT_FOUND;
        CallRFFI.InvokeVoidCallRootNode.create(context).call(new NativeCallInfo(funcName, symbolHandle, dllInfo), new Object[0]);
    }
}
