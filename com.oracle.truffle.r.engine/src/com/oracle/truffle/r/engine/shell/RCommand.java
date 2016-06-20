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
import java.io.Console;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.nodes.builtin.base.Quit;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RStartParams;
import com.oracle.truffle.r.runtime.RInternalSourceDescriptions;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.RStartParams.SA_TYPE;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.runtime.context.DefaultConsoleHandler;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;

import jline.console.UserInterruptException;

/**
 * Emulates the (Gnu)R command as precisely as possible.
 */
public class RCommand {

    // CheckStyle: stop system..print check

    public static void main(String[] args) {
        RCmdOptions options = RCmdOptions.parseArguments(RCmdOptions.Client.R, args, false);
        options.printHelpAndVersion();
        PolyglotEngine vm = createContextInfoFromCommandLine(options, false);
        // never returns
        readEvalPrint(vm);
        throw RInternalError.shouldNotReachHere();
    }

    /**
     * The standard R script escapes spaces to "~+~" in "-e" and "-f" commands.
     */
    private static String unescapeSpace(String input) {
        return input.replace("~+~", " ");
    }

    static PolyglotEngine createContextInfoFromCommandLine(RCmdOptions options, boolean embedded) {
        RStartParams rsp = new RStartParams(options, embedded);

        String fileArg = options.getString(FILE);
        if (fileArg != null) {
            if (options.getStringList(EXPR) != null) {
                Utils.rSuicide("cannot use -e with -f or --file");
            }
            if (!rsp.getSlave()) {
                rsp.setSaveAction(SA_TYPE.NOSAVE);
            }
            if (fileArg.equals("-")) {
                // means stdin, but still implies NO_SAVE
                fileArg = null;
            } else {
                fileArg = unescapeSpace(fileArg);
            }
            // cf GNU R
            rsp.setInteractive(false);
        }

        /*
         * Outputting the welcome message here has the virtue that the VM initialization delay
         * occurs later. However, it does not work in embedded mode as console redirects have not
         * been installed at this point. So we do it later in REmbedded.
         */
        if (!rsp.getQuiet() && !embedded) {
            System.out.println(RRuntime.WELCOME_MESSAGE);
        }
        /*
         * Whether the input is from stdin, a file (-f), or an expression on the command line (-e)
         * it goes through the console. N.B. -f and -e can't be used together and this is already
         * checked.
         */
        ConsoleHandler consoleHandler;
        if (fileArg != null) {
            List<String> lines;
            String filePath;
            try {
                File file = new File(fileArg);
                lines = Files.readAllLines(file.toPath());
                filePath = file.getCanonicalPath();
            } catch (IOException e) {
                throw Utils.rSuicide("cannot open file '" + fileArg + "': " + e.getMessage());
            }
            consoleHandler = new StringConsoleHandler(lines, System.out, filePath);
        } else if (options.getStringList(EXPR) != null) {
            List<String> exprs = options.getStringList(EXPR);
            for (int i = 0; i < exprs.size(); i++) {
                exprs.set(i, unescapeSpace(exprs.get(i)));
            }
            if (!rsp.getSlave()) {
                rsp.setSaveAction(SA_TYPE.NOSAVE);
            }
            // cf GNU R
            rsp.setInteractive(false);
            consoleHandler = new StringConsoleHandler(exprs, System.out, RInternalSourceDescriptions.EXPRESSION_INPUT);
        } else {
            /*
             * GnuR behavior differs from the manual entry for {@code interactive} in that {@code
             * --interactive} never applies to {@code -e/-f}, only to console input that has been
             * redirected from a pipe/file etc.
             *
             * If we are in embedded mode, the creation of ConsoleReader and the ConsoleHandler
             * should be lazy, as these may not be necessary and can cause hangs if stdin has been
             * redirected.
             */
            Console sysConsole = System.console();
            boolean isInteractive = options.getBoolean(INTERACTIVE) || sysConsole != null;
            if (!isInteractive && rsp.getSaveAction() != SA_TYPE.SAVE && rsp.getSaveAction() != SA_TYPE.NOSAVE) {
                throw Utils.rSuicide("you must specify '--save', '--no-save' or '--vanilla'");
            }
            if (embedded) {
                consoleHandler = new EmbeddedConsoleHandler(rsp);
            } else {
                boolean useReadLine = !rsp.getNoReadline();
                if (useReadLine) {
                    consoleHandler = new JLineConsoleHandler(rsp);
                } else {
                    consoleHandler = new DefaultConsoleHandler(System.in, System.out);
                }
            }
        }
        return ContextInfo.create(rsp, ContextKind.SHARE_NOTHING, null, consoleHandler).apply(PolyglotEngine.newBuilder()).build();
    }

    private static final Source GET_ECHO = Source.fromText("invisible(getOption('echo'))", RInternalSourceDescriptions.GET_ECHO).withMimeType(TruffleRLanguage.MIME);
    private static final Source QUIT_EOF = Source.fromText("quit(\"default\", 0L, TRUE)", RInternalSourceDescriptions.QUIT_EOF).withMimeType(TruffleRLanguage.MIME);

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
    static void readEvalPrint(PolyglotEngine vm) {
        ConsoleHandler consoleHandler = getContextInfo(vm).getConsoleHandler();
        Source source = Source.fromAppendableText(consoleHandler.getInputDescription());
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
                    // Start index of the new input
                    int startLength = source.getLength();
                    // Append the input as is
                    source.appendCode(input);
                    input = input.trim();
                    if (input.equals("") || input.charAt(0) == '#') {
                        // nothing to parse
                        continue;
                    }

                    String continuePrompt = getContinuePrompt();
                    Source subSource = Source.subSource(source, startLength).withMimeType(TruffleRLanguage.MIME);
                    while (true) {
                        /*
                         * N.B. As of Truffle rev 371045b1312d412bafa29882e6c3f7bfe6c0f8f1, only
                         * exceptions that are <: Exception are converted to IOException, Error
                         * subclasses pass through.
                         */
                        try {
                            vm.eval(subSource);
                        } catch (IncompleteSourceException | com.oracle.truffle.api.vm.IncompleteSourceException e) {
                            // read another line of input
                            consoleHandler.setPrompt(doEcho ? continuePrompt : null);
                            String additionalInput = consoleHandler.readLine();
                            if (additionalInput == null) {
                                throw new EOFException();
                            }
                            source.appendCode(additionalInput);
                            subSource = Source.subSource(source, startLength).withMimeType(TruffleRLanguage.MIME);
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
                            if (cause instanceof JumpToTopLevelException) {
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
        } catch (JumpToTopLevelException e) {
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

    static ContextInfo getContextInfo(PolyglotEngine vm) {
        try {
            return (ContextInfo) vm.findGlobalSymbol(ContextInfo.GLOBAL_SYMBOL).get();
        } catch (IOException ex) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    private static boolean doEcho(PolyglotEngine vm) {
        PolyglotEngine.Value echoValue;
        try {
            echoValue = vm.eval(GET_ECHO);
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
        RStringVector continuePrompt = (RStringVector) RRuntime.asAbstractVector(RContext.getInstance().stateROptions.getValue("continue"));
        return continuePrompt.getDataAt(0);
    }
}
