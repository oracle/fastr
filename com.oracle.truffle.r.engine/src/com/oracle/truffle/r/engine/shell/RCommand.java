/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.EXPR;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.FILE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.INTERACTIVE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_ENVIRON;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_INIT_FILE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_READLINE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_RESTORE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.NO_SAVE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.QUIET;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.SAVE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.SILENT;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.SLAVE;
import static com.oracle.truffle.r.runtime.RCmdOptions.RCmdOption.VANILLA;

import java.io.Console;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.nodes.builtin.base.Quit;
import com.oracle.truffle.r.runtime.BrowserQuitException;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RInternalSourceDescriptions;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.runtime.context.DefaultConsoleHandler;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.data.RLogicalVector;

import jline.console.ConsoleReader;
import jline.console.UserInterruptException;

/**
 * Emulates the (Gnu)R command as precisely as possible.
 */
public class RCommand {

    // CheckStyle: stop system..print check

    public static void main(String[] args) {
        RCmdOptions options = RCmdOptions.parseArguments(RCmdOptions.Client.R, args);
        options.printHelpAndVersion();
        ContextInfo info = createContextInfoFromCommandLine(options);
        // never returns
        readEvalPrint(info);
        throw RInternalError.shouldNotReachHere();
    }

    static ContextInfo createContextInfoFromCommandLine(RCmdOptions options) {
        if (options.getBoolean(SLAVE)) {
            options.setValue(QUIET, true);
            options.setValue(NO_SAVE, true);
        }

        if (options.getBoolean(VANILLA)) {
            options.setValue(NO_SAVE, true);
            options.setValue(NO_ENVIRON, true);
            options.setValue(NO_INIT_FILE, true);
            options.setValue(NO_RESTORE, true);
        }

        String fileArg = options.getString(FILE);
        if (fileArg != null) {
            if (options.getStringList(EXPR) != null) {
                Utils.fatalError("cannot use -e with -f or --file");
            }
            if (!options.getBoolean(SLAVE)) {
                options.setValue(NO_SAVE, true);
            }
            if (fileArg.equals("-")) {
                // means stdin, but still implies NO_SAVE
                fileArg = null;
            }
        }

        if (!(options.getBoolean(QUIET) || options.getBoolean(SILENT))) {
            System.out.println(RRuntime.WELCOME_MESSAGE);
        }
        /*
         * Whether the input is from stdin, a file (-f), or an expression on the command line (-e)
         * it goes through the console. N.B. -f and -e can't be used together and this is already
         * checked.
         */
        ConsoleHandler consoleHandler;
        InputStream consoleInput = System.in;
        OutputStream consoleOutput = System.out;
        if (fileArg != null) {
            List<String> lines;
            String filePath;
            try {
                File file = new File(fileArg);
                lines = Files.readAllLines(file.toPath());
                filePath = file.getCanonicalPath();
            } catch (IOException e) {
                throw Utils.fatalError("cannot open file '" + fileArg + "': " + e.getMessage());
            }
            consoleHandler = new StringConsoleHandler(lines, System.out, filePath);
        } else if (options.getStringList(EXPR) != null) {
            List<String> exprs = options.getStringList(EXPR);
            if (!options.getBoolean(SLAVE)) {
                options.setValue(NO_SAVE, true);
            }
            consoleHandler = new StringConsoleHandler(exprs, System.out, RInternalSourceDescriptions.EXPRESSION_INPUT);
        } else {
            /*
             * GnuR behavior differs from the manual entry for {@code interactive} in that {@code
             * --interactive} never applies to {@code -e/-f}, only to console input that has been
             * redirected from a pipe/file etc.
             */
            Console sysConsole = System.console();
            boolean useReadLine = !options.getBoolean(NO_READLINE);
            ConsoleReader consoleReader = null;
            if (useReadLine) {
                try {
                    consoleReader = new ConsoleReader(consoleInput, consoleOutput);
                    consoleReader.setHandleUserInterrupt(true);
                    consoleReader.setExpandEvents(false);
                } catch (IOException ex) {
                    throw Utils.fail("unexpected error opening console reader");
                }
            }
            boolean isInteractive = options.getBoolean(INTERACTIVE) || sysConsole != null;
            if (!isInteractive && !options.getBoolean(SAVE) && !options.getBoolean(NO_SAVE) && !options.getBoolean(VANILLA)) {
                throw Utils.fatalError("you must specify '--save', '--no-save' or '--vanilla'");
            }
            // long start = System.currentTimeMillis();
            if (useReadLine) {
                consoleHandler = new JLineConsoleHandler(isInteractive, consoleReader);
            } else {
                consoleHandler = new DefaultConsoleHandler(consoleInput, consoleOutput);
            }
        }
        return ContextInfo.create(options, ContextKind.SHARE_NOTHING, null, consoleHandler);
    }

    private static final Source GET_ECHO = RSource.fromText("invisible(getOption('echo'))", RInternalSourceDescriptions.GET_ECHO);
    private static final Source QUIT_EOF = RSource.fromText("quit(\"default\", 0L, TRUE)", RInternalSourceDescriptions.QUIT_EOF);

    /**
     * The read-eval-print loop, which can take input from a console, command line expression or a
     * file. There are two ways the repl can terminate:
     * <ol>
     * <li>A {@code quit} command is executed successfully, which case the system exits from the
     * {@link Quit} {@code .Internal} .</li>
     * <li>EOF on the input.</li>
     * </ol>
     * In case 2, we must implicitly execute a {@code quit("default, 0L, TRUE} command before
     * exiting. So,in either case, we never return.
     */
    static void readEvalPrint(ContextInfo info) {
        PolyglotEngine vm = info.apply(PolyglotEngine.newBuilder()).build();
        ConsoleHandler consoleHandler = info.getConsoleHandler();
        try {
            // console.println("initialize time: " + (System.currentTimeMillis() - start));
            REPL: for (;;) {
                boolean doEcho = doEcho(vm);
                consoleHandler.setPrompt(doEcho ? "> " : null);
                try {
                    String input = consoleHandler.readLine();
                    if (input == null) {
                        throw new EOFException();
                    }
                    String trInput = input.trim();
                    if (trInput.equals("") || trInput.charAt(0) == '#') {
                        // nothing to parse
                        continue;
                    }

                    String continuePrompt = getContinuePrompt();
                    StringBuffer sb = new StringBuffer(input);
                    Source source = RSource.fromText(sb.toString(), RInternalSourceDescriptions.SHELL_INPUT);
                    while (true) {
                        /*
                         * N.B. As of Truffle rev 371045b1312d412bafa29882e6c3f7bfe6c0f8f1, only
                         * exceptions that are <: Exception are converted to IOException, Error
                         * subclasses pass through.
                         */
                        try {
                            vm.eval(source);
                            emitIO();
                        } catch (IncompleteSourceException | com.oracle.truffle.api.vm.IncompleteSourceException e) {
                            // read another line of input
                            consoleHandler.setPrompt(doEcho ? continuePrompt : null);
                            String additionalInput = consoleHandler.readLine();
                            if (additionalInput == null) {
                                throw new EOFException();
                            }
                            sb.append(additionalInput);
                            source = RSource.fromText(sb.toString(), RInternalSourceDescriptions.SHELL_INPUT);
                            // The only continuation in the while loop
                            continue;
                        } catch (ParseException e) {
                            e.report(consoleHandler);
                        } catch (IOException e) {
                            /*
                             * We have to extract QuitException and DebugExitException and rethrow
                             * them explicitly
                             */
                            Throwable cause = e.getCause();
                            if (cause instanceof BrowserQuitException) {
                                // drop through to continue REPL
                            } else if (cause instanceof DebugExitException) {
                                throw (RuntimeException) cause;
                            } else if (cause instanceof RInternalError) {
                                /*
                                 * Placing this here makes it a non-fatal error. With default error
                                 * logging the report will go to a file, so we print a message on
                                 * the console as well.
                                 */
                                consoleHandler.println("internal error: " + e.getMessage() + " (see fastr_errors.log)");
                                RInternalError.reportError(e);
                            } else {
                                /*
                                 * This should never happen owing to earlier invariants of
                                 * converting everything else to an RInternalError
                                 */
                                consoleHandler.println("unexpected internal error (" + e.getClass().getSimpleName() + "); " + e.getMessage());
                                RInternalError.reportError(e);
                            }
                        }
                        continue REPL;
                    }
                } catch (UserInterruptException e) {
                    // interrupted by ctrl-c
                }
            }
        } catch (BrowserQuitException e) {
            // can happen if user profile invokes browser (unlikely but possible)
        } catch (EOFException ex) {
            try {
                vm.eval(QUIT_EOF);
            } catch (Throwable e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        } finally {
            vm.dispose();
        }
    }

    private static boolean doEcho(PolyglotEngine vm) {
        PolyglotEngine.Value echoValue;
        try {
            echoValue = vm.eval(GET_ECHO);
            emitIO();
            Object echo = echoValue.get();
            if (echo instanceof TruffleObject) {
                RLogicalVector echoVec = echoValue.as(RLogicalVector.class);
                return RRuntime.fromLogical(echoVec.getDataAt(0));
            } else if (echo instanceof Byte) {
                return RRuntime.fromLogical((Byte) echo);
            } else {
                throw RInternalError.shouldNotReachHere();
            }
        } catch (IOException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static String getContinuePrompt() {
        return RRuntime.asString(RRuntime.asAbstractVector(RContext.getInstance().stateROptions.getValue("continue")));
    }

    private static void emitIO() throws IOException {
    }
}
