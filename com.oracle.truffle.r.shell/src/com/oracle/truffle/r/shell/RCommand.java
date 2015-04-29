/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.shell;

import static com.oracle.truffle.r.runtime.RCmdOptions.*;

import java.io.*;
import java.util.*;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.engine.*;
import com.oracle.truffle.r.options.FastROptions;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.ffi.*;

import jline.console.*;

/**
 * Emulates the (Gnu)R command as precisely as possible.
 */
public class RCommand {

    /**
     * The choice of {@link RFFIFactory} is made statically so that it is bound into an AOT-compiled
     * VM. The decision is node made directly in {@link RFFIFactory} to avoid some project
     * dependencies that cause build problems.
     */
    static {
        Load_RFFIFactory.initialize();
    }

    // CheckStyle: stop system..print check

    public static void main(String[] args) {
        try {
            RCmdOptionsParser.Result result = RCmdOptionsParser.parseArguments(RCmdOptions.Client.R, args);
            if (HELP.getValue()) {
                RCmdOptionsParser.printHelp(RCmdOptions.Client.R, 0);
            } else if (VERSION.getValue()) {
                printVersionAndExit();
            } else if (RHOME.getValue()) {
                printRHomeAndExit();
            }
            subMain(result.args);
        } catch (Utils.DebugExitException ex) {
            // running under in-process debugger, just return
            return;
        }
    }

    /**
     * Entry point for {@link RscriptCommand} avoiding re-parsing.
     */
    @SuppressWarnings("deprecation")
    public static void subMain(String[] args) {

        if (SLAVE.getValue()) {
            QUIET.setValue(true);
            NO_SAVE.setValue(true);
        }

        if (VANILLA.getValue()) {
            NO_SAVE.setValue(true);
            NO_ENVIRON.setValue(true);
            NO_INIT_FILE.setValue(true);
            NO_RESTORE.setValue(true);
        }

        String fileArg = FILE.getValue();
        if (fileArg != null) {
            if (EXPR.getValue() != null) {
                Utils.fatalError("cannot use -e with -f or --file");
            }
            if (!SAVE.getValue()) {
                NO_SAVE.setValue(true);
            }
            if (fileArg.equals("-")) {
                // means stdin, but still implies NO_SAVE
                fileArg = null;
            }
        }

        FastROptions.initialize();
        REnvVars.initialize();
        ROptions.initialize();

        if (!(QUIET.getValue() || SILENT.getValue())) {
            System.out.println(RRuntime.WELCOME_MESSAGE);
        }
        /*
         * Whether the input is from stdin, a file (-f), or an expression on the command line (-e)
         * it goes through the console. N.B. -f and -e can't be used together and this is already
         * checked.
         */
        InputStream consoleInput = System.in;
        OutputStream consoleOutput = System.out;
        if (fileArg != null) {
            File file = new File(fileArg);
            boolean ok = false;
            if (file.exists() && file.canRead()) {
                try {
                    consoleInput = new BufferedInputStream(new FileInputStream(file));
                    ok = true;
                } catch (IOException ex) {
                    // fall through to fail
                }
            }
            if (!ok) {
                Utils.fatalError("cannot open file '" + fileArg + "': No such file or directory");
            }
        } else if (EXPR.getValue() != null) {
            List<String> exprs = EXPR.getValue();
            if (!SAVE.getValue()) {
                NO_SAVE.setValue(true);
            }
            StringBuffer sb = new StringBuffer(exprs.get(0));
            if (exprs.size() > 1) {
                for (int i = 1; i < exprs.size(); i++) {
                    sb.append('\n');
                    sb.append(exprs.get(i));
                }
            }
            consoleInput = new StringBufferInputStream(sb.toString());
        }
        ConsoleReader console;
        try {
            console = new RJLineConsoleReader(consoleInput, consoleOutput);
        } catch (IOException ex) {
            throw Utils.fail("unexpected error opening console reader");
        }
        MaterializedFrame globalFrame = readEvalPrint(consoleInput, console, args);
        // We should only reach here if interactive == false
        // Need to call quit explicitly
        Source quitSource = Source.fromText("quit(\"default\", 0L, TRUE)", "<quit_file>");
        REngine.getInstance().parseAndEval(quitSource, globalFrame, REnvironment.globalEnv(), false, false);
        // never returns
        assert false;
    }

    private static void printVersionAndExit() {
        System.out.print("FastR version ");
        System.out.println(RVersionNumber.FULL);
        System.out.println(RRuntime.LICENSE);
        Utils.exit(0);
    }

    private static void printRHomeAndExit() {
        System.out.println(REnvVars.rHome());
        throw Utils.exit(0);
    }

    private static MaterializedFrame readEvalPrint(InputStream inputStream, ConsoleReader consoleReader, String[] commandArgs) {
        /*
         * GnuR behavior differs from the manual entry for {@code interactive} in that {@code
         * --interactive} never applies to {@code -e/-f}, only to console input that has been
         * redirected from a pipe/file etc.
         */
        Console sysConsole = System.console();
        boolean haveConsole = inputStream == System.in && sysConsole != null;
        boolean isInteractive = inputStream == System.in && (INTERACTIVE.getValue() || sysConsole != null);
        // long start = System.currentTimeMillis();
        JLineConsoleHandler consoleHandler = new JLineConsoleHandler(isInteractive, consoleReader);
        MaterializedFrame globalFrame = REngine.initialize(commandArgs, consoleHandler, false, isInteractive);
        try {
            // console.println("initialize time: " + (System.currentTimeMillis() - start));
            for (;;) {
                boolean doEcho = doEcho();
                consoleReader.setPrompt(doEcho ? "> " : "");
                String input = consoleReader.readLine();
                if (input == null) {
                    return globalFrame;
                }
                if (!haveConsole && doEcho) {
                    consoleHandler.println(input);
                }
                input = input.trim();
                if (input.equals("") || input.charAt(0) == '#') {
                    continue;
                }

                try {
                    String continuePrompt = getContinuePrompt();
                    while (REngine.getInstance().parseAndEval(Source.fromText(input, "<shell_input>"), globalFrame, REnvironment.globalEnv(), true, true) == Engine.INCOMPLETE_SOURCE) {
                        consoleReader.setPrompt(doEcho ? "" : continuePrompt);
                        String additionalInput = consoleReader.readLine();
                        if (additionalInput == null) {
                            return globalFrame;
                        }
                        if (!haveConsole && doEcho) {
                            consoleHandler.println(additionalInput);
                        }
                        input = input + "\n" + additionalInput;
                    }
                } catch (BrowserQuitException ex) {
                    // Q in browser
                }
            }
        } catch (BrowserQuitException e) {
            // can happen if user profile invokes browser
        } catch (UserInterruptException e) {
            // interrupted
        } catch (IOException ex) {
            Utils.fail("unexpected error reading console input");
        }
        return globalFrame;
    }

    private static boolean doEcho() {
        RLogicalVector echo = (RLogicalVector) RRuntime.asAbstractVector(ROptions.getValue("echo"));
        return RRuntime.fromLogical(echo.getDataAt(0));
    }

    private static String getContinuePrompt() {
        RStringVector continuePrompt = (RStringVector) RRuntime.asAbstractVector(ROptions.getValue("continue"));
        return continuePrompt.getDataAt(0);
    }

}
