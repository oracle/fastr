/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine;

import java.util.*;

import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.*;
import com.oracle.truffle.r.runtime.ffi.*;

/**
 * A factory for creating new instances of {@link RContext} for multi-tenancy.
 */
public class RContextFactory {

    /**
     * The choice of {@link RFFIFactory} is made statically so that it is bound into an AOT-compiled
     * VM. The decision is node made directly in {@link RFFIFactory} to avoid some project
     * dependencies that cause build problems.
     */
    static {
        Load_RFFIFactory.initialize();
    }

    private static boolean initialized;

    /**
     * Initialize all the context-independent aspects of the system.
     */
    public static void initialize() {
        if (!initialized) {
            FastROptions.initialize();
            REnvVars.initialize();
            RInstrument.initialize();
            RPerfStats.initialize();
            Locale.setDefault(Locale.ROOT);
            RAccuracyInfo.initialize();
            RVersionInfo.initialize();
            TempPathName.initialize();
            RProfile.initialize();
            RContext.initialize(new RRuntimeASTAccessImpl(), RBuiltinPackages.getInstance(), FastROptions.IgnoreVisibility.getValue());
            initialized = true;
        }
    }

    public static RContext createShareParentReadWrite(RContext parent, String[] commandArgs, ConsoleHandler consoleHandler) {
        RContext context = RContext.createShareParentReadWrite(parent, commandArgs, consoleHandler);
        return context;
    }

    public static RContext createShareParentReadOnly(RContext parent, String[] commandArgs, ConsoleHandler consoleHandler) {
        RContext context = RContext.createShareParentReadOnly(parent, commandArgs, consoleHandler);
        return context;
    }

    /**
     * Create the initial context with no parent.
     */
    public static RContext createInitial(String[] commandArgs, ConsoleHandler consoleHandler) {
        RContext context = RContext.createShareNothing(null, commandArgs, consoleHandler);
        return context;
    }

    /**
     * Create a context of given kind.
     */
    public static RContext create(RContext parent, Kind kind, String[] commandArgs, ConsoleHandler consoleHandler) {
        RContext context = RContext.create(parent, kind, commandArgs, consoleHandler);
        return context;
    }
}
