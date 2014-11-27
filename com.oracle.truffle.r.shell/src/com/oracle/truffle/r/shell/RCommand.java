/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.engine.*;
import com.oracle.truffle.r.options.FastROptions;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine;
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

        // Whether the input is from stdin, a file or an expression on the command line (-e)
        // it goes through the console. However, we cannot (yet) do incremental parsing, so file
        // input has to be treated specially.
        if (fileArg != null) {
            evalFileInput(fileArg, args);
        } else {
            InputStream consoleInput = System.in;
            OutputStream consoleOutput = System.out;
            List<String> exprs = EXPR.getValue();
            if (exprs != null) {
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
            ConsoleReader console = null;
            try {
                console = new RJLineConsoleReader(consoleInput, consoleOutput);
            } catch (IOException ex) {
                Utils.fail("unexpected error opening console reader");
            }
            readEvalPrint(consoleInput == System.in, console, args);
        }
        // TODO exit code
        Utils.exit(0);
    }

    private static void printVersionAndExit() {
        System.out.print("FastR version ");
        System.out.println(RVersionNumber.FULL);
        System.out.println(RRuntime.LICENSE);
        Utils.exit(0);
    }

    private static void printRHomeAndExit() {
        System.out.println(REnvVars.rHome());
        Utils.exit(0);
    }

    private static void evalFileInput(String filePath, String[] commandArgs) {
        File file = new File(filePath);
        if (!file.exists()) {
            Utils.fatalError("cannot open file '" + filePath + "': No such file or directory");
        }
        String content = null;
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] bytes = new byte[(int) file.length()];
            is.read(bytes);
            content = new String(bytes);
        } catch (IOException ex) {
            Utils.fail("unexpected error reading file input");
        }
        try {
            JLineConsoleHandler consoleHandler = new JLineConsoleHandler(false, new ConsoleReader(null, System.out));
            VirtualFrame frame = REngine.initialize(commandArgs, consoleHandler, true, true);
            REngine.getInstance().parseAndEval(file, content, frame.materialize(), REnvironment.globalEnv(), false);
        } catch (IOException ex) {
            Utils.fail("unexpected error creating console");
        }
    }

    private static void readEvalPrint(boolean isInteractive, ConsoleReader console, String[] commandArgs) {
        if (!(QUIET.getValue() || SILENT.getValue())) {
            System.out.println(RRuntime.WELCOME_MESSAGE);
        }
        try {
            // long start = System.currentTimeMillis();
            VirtualFrame globalFrame = REngine.initialize(commandArgs, new JLineConsoleHandler(isInteractive, console), true, false);
            // console.println("initialize time: " + (System.currentTimeMillis() - start));
            for (;;) {
                console.setPrompt(SLAVE.getValue() ? "" : "> ");
                String input = console.readLine();
                if (input == null) {
                    return;
                }
                input = input.trim();
                if (input.equals("") || input.charAt(0) == '#') {
                    continue;
                }

                try {
                    while (REngine.getInstance().parseAndEval("<shell_input>", input, globalFrame.materialize(), REnvironment.globalEnv(), true, true) == Engine.INCOMPLETE_SOURCE) {
                        console.setPrompt(SLAVE.getValue() ? "" : "+ ");
                        String additionalInput = console.readLine();
                        if (additionalInput == null) {
                            return;
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
    }
}
