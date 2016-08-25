/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.shell;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RStartParams;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * Support for embedding FastR in a C/C++ application according to {@code Rembedded.h}. The
 * embedding interface consists of several functions and can be used in several ways. Since it is
 * not specified other than by example, we only have existing use-cases to work from. This is the
 * sequence used by {@code RStudio}.
 *
 * <pre>
 * Rf_initialize_R(argv, args);
 * Rstart rs;
 * // set some rs fields
 * R_SetParams(rs);
 * // set some Rinterface function callbacks
 * ptr_R_WriteConsole = local_R_WriteConsole
 * Rf_mainloop();
 * </pre>
 *
 * {@code Rf_initialize_R} invokes {@link #initializeR(String[])}. This creates an
 * {@link RStartParams} object in {@code embedded} mode that is recorded in the {@link ContextInfo}
 * object which is itself stored as a global symbol in the associated {@link PolyglotEngine}
 * instance. The FastR {@link PolyglotEngine} is then partially initialized. The call to
 * {@code R_SetParams} will adjust the values stored in the {@link RStartParams} object and then
 * {@code Rf_mainloop}, which calls {@link #setupRmainloop(PolyglotEngine)} and then
 * {@link #runRmainloop(PolyglotEngine)}, which will complete the FastR initialization and enter the
 * read-eval-print loop.
 */
public class REmbedded {

    /**
     * Creates the {@link PolyglotEngine} and initializes it. Called from native code when FastR is
     * embedded. Corresponds to FFI method {@code Rf_initialize_R}. N.B. This does not completely
     * initialize FastR as we cannot do that until the embedding system has had a chance to adjust
     * the {@link RStartParams}, which happens after this call returns.
     */
    private static PolyglotEngine initializeR(String[] args) {
        RContext.setEmbedded();
        RCmdOptions options = RCmdOptions.parseArguments(RCmdOptions.Client.R, args, true);
        PolyglotEngine vm = RCommand.createPolyglotEngineFromCommandLine(options, true, true, System.in, System.out);
        try {
            vm.eval(INIT);
        } catch (Exception ex) {
            Utils.rSuicideDefault("initializeR");
        }
        return vm;
    }

    private static final Source INIT = RSource.fromTextInternal("1", RSource.Internal.GET_ECHO);

    /**
     * GnuR distinguishes {@code setup_Rmainloop} and {@code run_Rmainloop}. Currently we don't have
     * the equivalent separation in FastR.
     */
    private static void setupRmainloop(@SuppressWarnings("unused") PolyglotEngine vm) {
    }

    /**
     * This is where we can complete the initialization based on what modifications were made by the
     * native code after {@link #initializeR} returned.
     */
    private static void runRmainloop(PolyglotEngine vm) {
        RContext.getInstance().completeEmbeddedInitialization();
        if (!RContext.getInstance().getStartParams().getQuiet()) {
            RContext.getInstance().getConsoleHandler().println(RRuntime.WELCOME_MESSAGE);
        }
        RCommand.readEvalPrint(vm);
    }

    /**
     * Testing vehicle, emulates a native upcall.
     */
    public static void main(String[] args) {
        PolyglotEngine vm = initializeR(args);
        RStartParams startParams = ContextInfo.getContextInfo(vm).getStartParams();
        startParams.setEmbedded();
        startParams.setLoadInitFile(false);
        startParams.setNoRenviron(true);
        setupRmainloop(vm);
        runRmainloop(vm);
    }

    // Checkstyle: stop method name check

    /**
     * Upcalled from embedded mode to (really) commit suicide. This provides the default
     * implementation of the {@code R_Suicide} function in the {@code Rinterface} API. If an
     * embeddee overrides it, it typically will save this value and invoke it after its own
     * customization.
     */
    @SuppressWarnings("unused")
    private static void R_Suicide(String msg) {
        Utils.rSuicideDefault(msg);
    }

}
