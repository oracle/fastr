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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.nodes.builtin.base.Quit;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RStartParams;
import com.oracle.truffle.r.runtime.RSource;
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

import jline.console.UserInterruptException;

/**
 * Emulates the (Gnu)R command as precisely as possible.
 */
public class RCommand {

    // CheckStyle: stop system..print check

    public static void main(String[] args) {
        doMain(args, true, System.in, System.out);
        // never returns
        throw RInternalError.shouldNotReachHere();
    }

    public static int doMain(String[] args, boolean initial, InputStream inStream, OutputStream outStream) {
        RCmdOptions options = RCmdOptions.parseArguments(RCmdOptions.Client.R, args, false);
        options.printHelpAndVersion();
        PolyglotEngine vm = createPolyglotEngineFromCommandLine(options, false, initial, inStream, outStream);
        return readEvalPrint(vm);
    }

    /**
     * The standard R script escapes spaces to "~+~" in "-e" and "-f" commands.
     */
    private static String unescapeSpace(String input) {
        return input.replace("~+~", " ");
    }

    static PolyglotEngine createPolyglotEngineFromCommandLine(RCmdOptions options, boolean embedded, boolean initial, InputStream inStream, OutputStream outStream) {
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
                /*
                 * If initial==false, ~ expansion will not have been done and the open will fail.
                 * It's harmless to always do it.
                 */
                File file = new File(Utils.tildeExpand(fileArg));
                lines = Files.readAllLines(file.toPath());
                filePath = file.getCanonicalPath();
            } catch (IOException e) {
                if (initial) {
                    throw Utils.rSuicide(String.format(RError.Message.NO_SUCH_FILE.message, fileArg));
                } else {
                    throw RError.error(RError.NO_CALLER, RError.Message.NO_SUCH_FILE, fileArg);
                }
            }
            consoleHandler = new StringConsoleHandler(lines, outStream, filePath);
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
            consoleHandler = new StringConsoleHandler(exprs, outStream, RSource.Internal.EXPRESSION_INPUT.string);
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
            Console sysConsole = System.console(); // TODO fix for context sessions
            boolean isInteractive = options.getBoolean(INTERACTIVE) || sysConsole != null;
            if (!isInteractive && rsp.getSaveAction() != SA_TYPE.SAVE && rsp.getSaveAction() != SA_TYPE.NOSAVE) {
                String msg = "you must specify '--save', '--no-save' or '--vanilla'";
                if (initial) {
                    throw Utils.rSuicide(msg);
                } else {
                    throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, msg);
                }
            }
            if (embedded) {
                consoleHandler = new EmbeddedConsoleHandler(rsp);
            } else {
                boolean useReadLine = !rsp.getNoReadline();
                if (useReadLine) {
                    consoleHandler = new JLineConsoleHandler(rsp, inStream, outStream);
                } else {
                    consoleHandler = new DefaultConsoleHandler(inStream, outStream);
                }
            }
        }
        return ContextInfo.create(rsp, ContextKind.SHARE_NOTHING, initial ? null : RContext.getInstance(), consoleHandler).createVM();
    }

    private static final Source GET_ECHO = RSource.fromTextInternal("invisible(getOption('echo'))", RSource.Internal.GET_ECHO);
    private static final Source QUIT_EOF = RSource.fromTextInternal("quit(\"default\", 0L, TRUE)", RSource.Internal.QUIT_EOF);

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
    static int readEvalPrint(PolyglotEngine vm) {
        int lastStatus = 0;
        ContextInfo contextInfo = ContextInfo.getContextInfo(vm);
        ConsoleHandler consoleHandler = contextInfo.getConsoleHandler();
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
                    Source source = RSource.fromTextInternal(sb.toString(), RSource.Internal.SHELL_INPUT);
                    while (true) {
                        /*
                         * N.B. As of Truffle rev 371045b1312d412bafa29882e6c3f7bfe6c0f8f1, only
                         * exceptions that are <: Exception are converted to IOException, Error
                         * subclasses pass through.
                         */
                        lastStatus = 0;
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
                            source = RSource.fromTextInternal(sb.toString(), RSource.Internal.SHELL_INPUT);
                            // The only continuation in the while loop
                            continue;
                        } catch (ParseException e) {
                            e.report(consoleHandler);
                        } catch (IOException e) {
                            /*
                             * We have to extract the underlying cause and handle the special cases
                             * appropriately.
                             */
                            lastStatus = 1;
                            Throwable cause = e.getCause();
                            if (cause instanceof RError) {
                                // drop through to continue REPL and remember last eval was an error
                            } else if (cause instanceof JumpToTopLevelException) {
                                // drop through to continue REPL
                            } else if (cause instanceof DebugExitException) {
                                throw (RuntimeException) cause;
                            } else if (cause instanceof ExitException) {
                                // usually from quit
                                int status = ((ExitException) cause).getStatus();
                                if (contextInfo.getParent() == null) {
                                    vm.dispose();
                                    Utils.systemExit(status);
                                } else {
                                    return status;
                                }
                            } else {
                                RInternalError.reportErrorAndConsoleLog(cause, consoleHandler, 0);
                                // We continue the repl even though the system may be broken
                            }
                        }
                        continue REPL;
                    }
                } catch (UserInterruptException e) {
                    // interrupted by ctrl-c
                }
            }
        } catch (JumpToTopLevelException | EOFException ex) {
            // JumpToTopLevelException can happen if user profile invokes browser (unlikely but
            // possible)
            try {
                vm.eval(QUIT_EOF);
            } catch (JumpToTopLevelException e) {
                Utils.systemExit(0);
            } catch (Throwable e) {
                if (e.getCause() instanceof ExitException) {
                    // normal quit, but with exit code based on lastStatus
                    if (contextInfo.getParent() == null) {
                        Utils.systemExit(lastStatus);
                    } else {
                        return lastStatus;
                    }
                }
                throw RInternalError.shouldNotReachHere(e);
            }
        } finally {
            vm.dispose();
        }
        return 0;
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

    @SuppressWarnings("unused")
    private static void emitIO() throws IOException {
    }
}
