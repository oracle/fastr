/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.launcher;

import java.io.InputStream;
import java.io.OutputStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.r.common.RCmdOptions;
import com.oracle.truffle.r.common.RStartParams;
import com.oracle.truffle.r.common.SuppressFBWarnings;

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
 * {@code Rf_initialize_R} invokes {@link #initializeR(String[],boolean)}, which creates new
 * polyglot {@link Context}. The call to {@code R_SetParams} adjusts the values stored in the
 * {@link RStartParams} object.
 *
 * Note that this relies upon reading the class-path from $R_HOME/bin/execRextras/Rclasspath and for
 * the time being, this file is available only in in-source dev build of FastR (where it delegates
 * to MX).
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
    @SuppressFBWarnings(value = "LI_LAZY_INIT_UPDATE_STATIC", justification = "one-time initialization")
    private static void initializeR(String[] args, boolean initMainLoop) {
        try {
            assert context == null;
            RCmdOptions options = RCmdOptions.parseArguments(args, false);

            EmbeddedConsoleHandler embeddedConsoleHandler = new EmbeddedConsoleHandler();

            consoleHandler = ConsoleHandler.createConsoleHandler(options, embeddedConsoleHandler, System.in, System.out);
            InputStream input = consoleHandler.createInputStream();
            boolean useEmbedded = consoleHandler == embeddedConsoleHandler;
            OutputStream stdOut = useEmbedded ? embeddedConsoleHandler.createStdOutputStream(System.out) : System.out;
            OutputStream stdErr = useEmbedded ? embeddedConsoleHandler.createErrOutputStream(System.err) : System.err;
            context = Context.newBuilder("R", "llvm").//
                            allowAllAccess(true).//
                            option("R.IsNativeEmbeddedMode", "true").//
                            arguments("R", options.getArguments()).//
                            in(input).out(stdOut).err(stdErr).//
                            build();
            consoleHandler.setContext(context);
            context.initialize("R");

            if (initMainLoop) {
                context.enter();
                initializeEmbedded();
                // stay in the context TODO should we?
            }
        } catch (Throwable ex) {
            System.err.println("Unexpected internal error: ");
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Adjusts the values stored in {@link RStartParams}. Invoked from the native embedding code,
     * i.e. not from a down-call, so the callbacks native array is not set-up properly. Moreover,
     * this call is made during R initialization, so it not entirely clear if the FFI implementation
     * has been fully initialized yet.
     */
    @SuppressWarnings("unused")
    private static void setParams(boolean quietA, boolean noEchoA, boolean interactiveA, boolean verboseA, boolean loadSiteFileA,
                    boolean loadInitFileA, boolean debugInitFileA, int restoreActionA, int saveActionA, boolean noRenvironA) {
        context.enter();
        // Todo: params = RContext.getInstance().getStartParams();
        RStartParams params = new RStartParams(RCmdOptions.parseArguments(new String[0], true), true);
        params.setParams(quietA, noEchoA, interactiveA, verboseA, loadSiteFileA, loadInitFileA, debugInitFileA, restoreActionA, saveActionA, noRenvironA);
        context.leave();
    }

    /**
     * N.B. This expression cannot contain any R functions, e.g. "invisible", because at the time it
     * is evaluated the R builtins have not been installed, see {@link #initializeR}. The
     * suppression of printing is handled as a special case based on {@code Internal#INIT_EMBEDDED}.
     */
    private static final Source INIT = Source.newBuilder("R", "init-embedded", "<embedded>").internal(true).interactive(false).buildLiteral();

    // TODO: to be invoked from native
    @SuppressWarnings("unused")
    private static void endRmainloop(int status) {
        context.leave();
        context.close();
        System.exit(status);
    }

    /**
     * This is where we can complete the initialization based on what modifications were made by the
     * native code after {@link #initializeR} returned.
     */
    private static void runRmainloop() {
        try {
            context.enter();
            initializeEmbedded();
            int status = REPL.readEvalPrint(context, consoleHandler, null);
            context.leave();
            context.close();
            System.exit(status);
        } catch (Throwable ex) {
            System.err.println("Unexpected internal error: ");
            ex.printStackTrace(System.err);
        }
    }

    private static void initializeEmbedded() {
        context.eval(INIT);
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
        // TODO: real implementation of R_Suicide would have to long jump to the end of the current
        // downcall, or if not in a downcall, then cleanup and close the R Context
    }

}
