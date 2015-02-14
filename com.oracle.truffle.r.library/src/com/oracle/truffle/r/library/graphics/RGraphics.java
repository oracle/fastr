/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2011, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics;

import com.oracle.truffle.r.library.graphics.core.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * A placeholder to keep {@code REngine} limited to calling the {@link #initialize} method. It sets
 * the {@code .Device} and {@code .Devices} variables in "base" as per GnuR.
 *
 * TODO Sort out the relationship of {@link GraphicsEngine} and the {@code grDevices} and
 * {@code graphics} packages.
 *
 */
public class RGraphics {
    private static final RStringVector NULL_DEVICE = RDataFactory.createStringVectorFromScalar("null device");
    private static final String DOT_DEVICE = ".Device";
    private static final String DOT_DEVICES = ".Devices";

    public static void initialize() {
        REnvironment baseEnv = REnvironment.baseEnv();
        baseEnv.safePut(DOT_DEVICE, NULL_DEVICE);
        RPairList devices = RDataFactory.createPairList(NULL_DEVICE, null, null);
        baseEnv.safePut(DOT_DEVICES, devices);
        registerBaseGraphicsSystem();
    }

    private static void registerBaseGraphicsSystem() {
        try {
            getGraphicsEngine().registerGraphicsSystem(new BaseGraphicsSystem());
        } catch (Exception e) {
            ConsoleHandler consoleHandler = RContext.getInstance().getConsoleHandler();
            consoleHandler.println("Unable to register base graphics system");
        }
    }

    private static GraphicsEngine getGraphicsEngine() {
        return GraphicsEngineImpl.getInstance();
    }

}
