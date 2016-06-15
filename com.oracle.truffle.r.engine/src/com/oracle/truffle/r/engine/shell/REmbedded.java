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

import java.io.IOException;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RInternalSourceDescriptions;
import com.oracle.truffle.r.runtime.RStartParams;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;

public class REmbedded {

    /**
     * Creates the {@link PolyglotEngine} and initializes it. Called from native code when FastR is
     * embedded. Corresponds to FFI method {@code Rf_initialize_R}. N.B. This does not actually
     * initialize FastR as that happens indirectly when the {@link PolyglotEngine} does the first
     * {@code eval} and we cannot do that until the embedding system has had a chance to adjust the
     * {@link RStartParams}.
     */
    private static PolyglotEngine initializeR(String[] args) {
        RCmdOptions options = RCmdOptions.parseArguments(RCmdOptions.Client.R, args, true);
        PolyglotEngine vm = RCommand.createContextInfoFromCommandLine(options, true);
        return vm;
    }

    private static final Source INIT = Source.fromText("1", RInternalSourceDescriptions.GET_ECHO).withMimeType(TruffleRLanguage.MIME);

    /**
     * Upcall from native code to prepare for the main read-eval-print loop, Since we are in
     * embedded mode we first do a dummy eval that has the important side effect of creating the
     * {@link RContext}. Then we use that to complete the initialization that was deferred.
     *
     * @param vm
     */
    private static void setupRmainloop(PolyglotEngine vm) {
        try {
            vm.eval(INIT);
            RContext.getInstance().completeEmbeddedInitialization();
        } catch (IOException ex) {
            Utils.fatalError("setupRmainloop");
        }
    }

    private static void mainloop(PolyglotEngine vm) {
        setupRmainloop(vm);
        runRmainloop(vm);
    }

    private static void runRmainloop(PolyglotEngine vm) {
        RCommand.readEvalPrint(vm);
    }

    /**
     * Testing vehicle, emulates a native upcall.
     */
    public static void main(String[] args) {
        PolyglotEngine vm = initializeR(args);
        RStartParams startParams = RCommand.getContextInfo(vm).getStartParams();
        startParams.setEmbedded();
        startParams.setLoadInitFile(false);
        startParams.setNoRenviron(true);
        mainloop(vm);
    }

}
