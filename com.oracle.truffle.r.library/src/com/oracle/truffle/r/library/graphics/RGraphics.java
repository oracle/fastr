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
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics;

import java.util.concurrent.atomic.AtomicBoolean;

import com.oracle.truffle.r.library.graphics.core.GraphicsEngine;
import com.oracle.truffle.r.library.graphics.core.GraphicsEngineImpl;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * A placeholder to keep {@code REngine} limited to calling the {@link #initialize} method. Two
 * possible implementations are available:
 * <ul>
 * <li>Native graphics from GnuR</li>
 * <li>Internal (Java) graphics, very incomplete implementation</li>
 * </ul>
 * The default is native graphics, selected by a startup option. Graphics is not virtualized, so
 * multiple contexts all share the same underlying implementation which is initialized exactly once.
 *
 */
public class RGraphics {
    private static final RStringVector NULL_DEVICE = RDataFactory.createStringVectorFromScalar("null device");
    /**
     * The graphics devices system maintains two variables .Device and .Devices in the base
     * environment both are always set: .Devices gives a list of character vectors of the names of
     * open devices, .Device is the element corresponding to the currently active device. The null
     * device will always be open.
     */
    private static final String DOT_DEVICE = ".Device";
    private static final String DOT_DEVICES = ".Devices";
    private static final AtomicBoolean initialized = new AtomicBoolean();

    public static void initialize() {
        if (initialized.compareAndSet(false, true)) {
            if (FastROptions.UseInternalGraphics.getBooleanValue()) {
                REnvironment baseEnv = REnvironment.baseEnv();
                baseEnv.safePut(DOT_DEVICE, NULL_DEVICE);
                RPairList devices = RDataFactory.createPairList(NULL_DEVICE);
                baseEnv.safePut(DOT_DEVICES, devices);
                registerBaseGraphicsSystem();
            } else {
                DLL.RegisteredNativeSymbol rns = DLL.RegisteredNativeSymbol.any();
                DLL.SymbolHandle func = DLL.findSymbol("InitGraphics", null, rns);
                assert func != DLL.SYMBOL_NOT_FOUND;
                RFFIFactory.getRFFI().getCallRFFI().callRFFINode().invokeVoidCall(new NativeCallInfo("InitGraphics", func, DLL.findLibrary("graphics")), new Object[0]);
            }
        }
    }

    private static void registerBaseGraphicsSystem() {
        try {
            getGraphicsEngine().registerGraphicsSystem(new BaseGraphicsSystem());
        } catch (Exception e) {
            e.printStackTrace();
            ConsoleHandler consoleHandler = RContext.getInstance().getConsoleHandler();
            consoleHandler.println("Unable to register base graphics system");
        }
    }

    private static GraphicsEngine getGraphicsEngine() {
        return GraphicsEngineImpl.getInstance();
    }
}
