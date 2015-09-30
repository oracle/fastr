/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.Locale;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.debug.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.engine.repl.debug.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltinPackages;
import com.oracle.truffle.r.nodes.instrument.RInstrument;
import com.oracle.truffle.r.runtime.RAccuracyInfo;
import com.oracle.truffle.r.runtime.RPerfStats;
import com.oracle.truffle.r.runtime.RVersionInfo;
import com.oracle.truffle.r.runtime.TempPathName;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.ffi.Load_RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * Only does the minimum for running under the debugger. It is not completely clear how to correctly
 * integrate the R startup in {@code RCommand} with this API.
 */
@TruffleLanguage.Registration(name = "R", version = "0.1", mimeType = {"application/x-r", "text/x-r"})
public final class TruffleRLanguage extends TruffleLanguage<RContext> {

    private static boolean initialized;

    /**
     * The choice of {@link RFFIFactory} is made statically so that it is bound into an AOT-compiled
     * VM. The decision is node made directly in {@link RFFIFactory} to avoid some project
     * dependencies that cause build problems.
     */
    private static synchronized void initialize() {
        if (!initialized) {
            initialized = true;
            try {
                Load_RFFIFactory.initialize();
                RInstrument.initialize();
                RPerfStats.initialize();
                Locale.setDefault(Locale.ROOT);
                RAccuracyInfo.initialize();
                RVersionInfo.initialize();
                TempPathName.initialize();
                RContext.initialize(new RRuntimeASTAccessImpl(), RBuiltinPackages.getInstance());
            } catch (Throwable t) {
                System.out.println("error during engine initialization:");
                t.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private DebugSupportProvider debugSupport;

    public static final TruffleRLanguage INSTANCE = new TruffleRLanguage();

    public static final String MIME = "application/x-r";

    private TruffleRLanguage() {
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return false;
    }

    @Override
    protected ToolSupportProvider getToolSupport() {
        return getDebugSupport();
    }

    @Override
    protected DebugSupportProvider getDebugSupport() {
        if (debugSupport == null) {
            debugSupport = new RDebugSupportProvider();
        }
        return debugSupport;
    }

    @Override
    protected RContext createContext(Env env) {
        initialize();
        return RContext.create(env);
    }

    @Override
    protected CallTarget parse(Source source, Node context, String... argumentNames) throws IOException {
        /*
         * When running under the debugger the loadrun command eventually arrives here with a
         * FileSource. Since FastR has a custom mechanism for executing a (Root)CallTarget that
         * PolyglotEngine does not know about, we have to use a delegation mechanism via a wrapper
         * CallTarget class, using a special REngine entry point.
         */
        return RContext.getEngine().parseToCallTarget(source, true);
    }

    @Override
    protected Object findExportedSymbol(RContext context, String globalName, boolean onlyExplicit) {
        return context.getExportedSymbols().get(globalName);
    }

    @Override
    protected Object getLanguageGlobal(RContext context) {
        // TODO: what's the meaning of "language global" for R?
        return null;
    }

    // TODO: why isn't the original method public?
    public Node actuallyCreateFindContextNode() {
        return createFindContextNode();
    }
}
