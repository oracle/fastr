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

import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
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
            RInstrument.initialize();
            RPerfStats.initialize();
            Locale.setDefault(Locale.ROOT);
            RAccuracyInfo.initialize();
            RVersionInfo.initialize();
            TempPathName.initialize();
            RContext.initialize(new RRuntimeASTAccessImpl(), RBuiltinPackages.getInstance(), FastROptions.IgnoreVisibility);
            initialized = true;
        }
    }

    /**
     * Create the initial context with no parent.
     */
    public static RContext createInitial(RCmdOptions options, ConsoleHandler consoleHandler, Env env) {
        return RContext.create(null, ContextKind.SHARE_NOTHING, options, consoleHandler, env);
    }

    /**
     * Create a context of given kind.
     */
    public static RContext create(RContext parent, ContextKind kind, RCmdOptions options, ConsoleHandler consoleHandler, Env env) {
        return RContext.create(parent, kind, options, consoleHandler, env);
    }
}
