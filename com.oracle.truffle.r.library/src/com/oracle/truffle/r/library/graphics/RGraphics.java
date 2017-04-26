/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2014, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics;

import java.util.concurrent.atomic.AtomicBoolean;

import com.oracle.truffle.r.library.fastrGrid.graphics.RGridGraphicsAdapter;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;

/**
 * A placeholder to keep {@code REngine} limited to calling the {@link #initialize} method. The code
 * in R has a hard-coded invocation to InitGraphics in it. This initialization either invokes it
 * too, or it runs a Java version of it if the internal grid package implementation is to be used.
 */
public class RGraphics {
    private static final AtomicBoolean initialized = new AtomicBoolean();

    public static void initialize() {
        if (initialized.compareAndSet(false, true)) {
            if (FastROptions.UseInternalGridGraphics.getBooleanValue()) {
                RGridGraphicsAdapter.initialize();
            } else if (FastROptions.LoadPackagesNativeCode.getBooleanValue()) {
                DLL.DLLInfo dllInfo = DLL.findLibraryContainingSymbol("InitGraphics");
                DLL.SymbolHandle symbolHandle = DLL.findSymbol("InitGraphics", dllInfo);
                assert symbolHandle != DLL.SYMBOL_NOT_FOUND;
                CallRFFI.InvokeVoidCallRootNode.create().getCallTarget().call(new NativeCallInfo("InitGraphics", symbolHandle, dllInfo), new Object[0]);
            }
        }
    }
}
