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

import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.runtime.*;
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

    public static void initialize() {
        if (!initialized) {
            FastROptions.initialize();
            REnvVars.initialize();
            RContext.initialize(new RASTHelperImpl(), RBuiltinPackages.getInstance(), FastROptions.IgnoreVisibility.getValue());
            // TODO options are (partly at least) context specific
            ROptions.initialize();
            initialized = true;
        }

    }

    public static RContext createContext(String[] commandArgs, ConsoleHandler consoleHandler) {
        RContext context = RContext.create(commandArgs, consoleHandler);
        REngine engine = REngine.create(context);
        context.setEngine(engine);
        engine.initializeShared();
        return context;
    }
}
