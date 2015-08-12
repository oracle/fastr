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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.debug.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.engine.repl.debug.*;
import com.oracle.truffle.r.runtime.*;

/**
 * Only does the minimum for running under the debugger. It is not completely clear how to correctly
 * integrate the R startup in {@code RCommand} with this API.
 */
@TruffleLanguage.Registration(name = "R", version = "3.1.3", mimeType = "application/x-r")
public final class TruffleRLanguage extends TruffleLanguage<RContext> {

    private DebugSupportProvider debugSupport;

    public static final TruffleRLanguage INSTANCE = new TruffleRLanguage();

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
        /*
         * TODO we assume here that the initial context has already been created, which is certainly
         * true by fiat when running under the debugger, but may not be in general.
         */
        RContext result = RContext.getInstance();
        return result;
    }

    @Override
    protected CallTarget parse(Source source, Node context, String... argumentNames) throws IOException {
        /*
         * When running under the debugger the loadrun command eventually arrives here with a
         * FileSource. Since FastR has a custom mechanism for executing a (Root)CallTarget that
         * TruffleVM does not know about, we have to use a delegation mechanism via a wrapper
         * CallTarget class, using a special REngine entry point.
         */
        return RContext.getEngine().parseToCallTarget(source);
    }

    @Override
    protected Object findExportedSymbol(RContext context, String globalName, boolean onlyExplicit) {
        throw RInternalError.unimplemented("findExportedSymbol");
    }

    @Override
    protected Object getLanguageGlobal(RContext context) {
        throw RInternalError.unimplemented("getLanguageGlobal");
    }

}
