/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.OutputStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.r.launcher.ConsoleHandler;
import com.oracle.truffle.r.launcher.RCmdOptions;
import com.oracle.truffle.r.launcher.RCommand;
import com.oracle.truffle.r.launcher.RStartParams;
import com.oracle.truffle.r.runtime.RSource.Internal;
import com.oracle.truffle.r.runtime.RSuicide;
import com.oracle.truffle.r.runtime.Utils;
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
 * {@code Rf_initialize_R} invokes {@link #initializeR(String[])}, which creates new polyglot
 * {@link Context}. The call to {@code R_SetParams} adjusts the values stored in the
 * {@link RStartParams} object and then {@code Rf_mainloop}, which calls {@link #setupRmainloop()}
 * and then {@link #runRmainloop()}, which will complete the FastR initialization and enter the
 * read-eval-print loop.
 */
public class REmbedded {

    private static ConsoleHandler consoleHandler;
    private static Context context;

    /**
     * Creates the {@link Engine} and initializes it. Called from native code when FastR is
     * embedded. Corresponds to FFI method {@code Rf_initialize_R}. N.B. This does not completely
     * initialize FastR as we cannot do that until the embedding system has had a chance to adjust
     * the {@link RStartParams}, which happens after this call returns.
     */
    private static void initializeR(String[] args, boolean initMainLoop) {
        assert context == null;
        RContext.setEmbedded();
        RCmdOptions options = RCmdOptions.parseArguments(RCmdOptions.Client.R, args, false);

        EmbeddedConsoleHandler embeddedConsoleHandler = new EmbeddedConsoleHandler();

        consoleHandler = RCommand.createConsoleHandler(options, embeddedConsoleHandler, System.in, System.out);
        InputStream input = consoleHandler.createInputStream();
        boolean useEmbedded = consoleHandler == embeddedConsoleHandler;
        OutputStream stdOut = useEmbedded ? embeddedConsoleHandler.createStdOutputStream(System.out) : System.out;
        OutputStream stdErr = useEmbedded ? embeddedConsoleHandler.createErrOutputStream(System.err) : System.err;
        context = Context.newBuilder().allowHostAccess(true).arguments("R", options.getArguments()).in(input).out(stdOut).err(stdErr).build();
        consoleHandler.setContext(context);
        context.eval(INIT);

        if (initMainLoop) {
            context.enter();
            RContext.getInstance().completeEmbeddedInitialization();
            // TODO: push callbacks
            // stay in the context TODO should we?
        }
    }

    /**
     * Adjusts the values stored in {@link RStartParams}. Invoked from the native embedding code,
     * i.e. not from a down-call, so the callbacks native array is not set-up properly. Moreover,
     * this call is made during R initialization, so it not entirely clear if the FFI implementation
     * has been fully initialized yet.
     */
    @SuppressWarnings("unused")
    private static void setParams(boolean quietA, boolean slaveA, boolean interactiveA, boolean verboseA, boolean loadSiteFileA,
                    boolean loadInitFileA, boolean debugInitFileA, int restoreActionA, int saveActionA, boolean noRenvironA) {
        context.enter();
        RStartParams params = RContext.getInstance().getStartParams();
        params.setParams(quietA, slaveA, interactiveA, verboseA, loadSiteFileA, loadInitFileA, debugInitFileA, restoreActionA, saveActionA, noRenvironA);
        context.leave();
    }

    /**
     * N.B. This expression cannot contain any R functions, e.g. "invisible", because at the time it
     * is evaluated the R builtins have not been installed, see {@link #initializeR}. The
     * suppression of printing is handled a a special case based on {@link Internal#INIT_EMBEDDED}.
     */
    private static final Source INIT = Source.newBuilder("R", "1", "<embedded>").buildLiteral();

    private static void endRmainloop(int status) {
        context.leave();
        context.close();
        Utils.systemExit(status);
    }

    /**
     * This is where we can complete the initialization based on what modifications were made by the
     * native code after {@link #initializeR} returned.
     */
    private static void runRmainloop() {
        context.enter();
        RContext.getInstance().completeEmbeddedInitialization();
        int status = RCommand.readEvalPrint(context, consoleHandler);
        context.leave();
        context.close();
        Utils.systemExit(status);
    }

    /**
     * Testing vehicle, emulates a native upcall.
     */
    public static void main(String[] args) {
        initializeR(args, false);
        runRmainloop();
    }

    // Checkstyle: stop method name check

    /**
     * Upcalled from embedded mode via JNI to (really) commit suicide. This provides the default
     * implementation of the {@code R_Suicide} function in the {@code Rinterface} API. If an
     * embeddee overrides it, it typically will save this value and invoke it after its own
     * customization.
     */
    @SuppressWarnings("unused")
    private static void R_Suicide(String msg) {
        RSuicide.rSuicideDefault(msg);
    }

}
