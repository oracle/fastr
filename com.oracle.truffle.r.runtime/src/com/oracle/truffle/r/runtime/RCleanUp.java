/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2014,  The R Core Team
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import java.util.ArrayList;

import com.oracle.truffle.r.launcher.RStartParams;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ConsoleIO;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.gnur.SA_TYPE;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState;

public class RCleanUp {

    private static ArrayList<InstrumentationState.CleanupHandler> cleanupHandlers = new ArrayList<>();

    public static void registerCleanupHandler(InstrumentationState.CleanupHandler cleanupHandler) {
        cleanupHandlers.add(cleanupHandler);
    }

    public static void cleanUp(SA_TYPE saveType, int status, boolean runLast) {
        if (RInterfaceCallbacks.R_CleanUp.isOverridden()) {
            RFFIFactory.getREmbedRFFI().cleanUp(saveType.ordinal(), status, runLast ? 1 : 0);
        } else {
            stdCleanUp(saveType, status, runLast);
        }
    }

    public static void stdCleanUp(SA_TYPE saveActionIn, int status, boolean runLast) {
        // Output is not diverted to sink
        ConsoleIO console = RContext.getInstance().getConsole();
        SA_TYPE saveAction = saveActionIn;
        if (saveAction == SA_TYPE.DEFAULT || (saveAction == SA_TYPE.SAVEASK && !RContext.getInstance().isInteractive())) {
            RStartParams params = RContext.getInstance().getStartParams();
            saveAction = params.askForSave() ? SA_TYPE.SAVEASK : params.save() ? SA_TYPE.SAVE : SA_TYPE.NOSAVE;
        }
        if (saveAction == SA_TYPE.SAVEASK && RContext.getInstance().isInteractive()) {
            W: while (true) {
                console.setPrompt("");
                console.print("Save workspace image? [y/n/c]: ");
                String response = console.readLine();
                if (response == null) {
                    saveAction = SA_TYPE.NOSAVE;
                    break;
                }
                if (response.length() == 0) {
                    continue;
                }
                switch (response.charAt(0)) {
                    case 'c':
                        console.setPrompt("> ");
                        throw new JumpToTopLevelException();
                    case 'y':
                        saveAction = SA_TYPE.SAVE;
                        break W;
                    case 'n':
                        saveAction = SA_TYPE.NOSAVE;
                        break W;
                    default:
                        continue;
                }
            }
        }

        switch (saveAction) {
            case SAVE:
                if (runLast) {
                    runDotLast();
                }
                /*
                 * we do not have an efficient way to tell if the global environment is "dirty", so
                 * we save always
                 */
                RContext.getEngine().checkAndRunStartupShutdownFunction("sys.save.image", new String[]{"\".RData\""});
                // TODO: write out history
                break;
            case NOSAVE:
                if (runLast) {
                    runDotLast();
                }
                break;
            default:
                throw RInternalError.shouldNotReachHere();
        }
        for (InstrumentationState.CleanupHandler cleanupHandler : cleanupHandlers) {
            try {
                cleanupHandler.cleanup(status);
            } catch (Throwable t) {
                RInternalError.reportError(t);
            }
        }
        // TODO run exit finalizers (FFI) (this should happen in the FFI context beforeDestroy)

        // force sub-context threads to stop
        for (Thread thread : new ArrayList<>(RContext.getInstance().threads.values())) {
            thread.interrupt();
            try {
                thread.join(10);
            } catch (InterruptedException e) {
                // nothing to be done
            }
        }
        throw new ExitException(status, false);
    }

    private static void runDotLast() {
        RContext.getEngine().checkAndRunStartupShutdownFunction(".Last");
        // TODO errors should return to toplevel if interactive
        RContext.getEngine().checkAndRunStartupShutdownFunction(".Last.sys");
    }
}
