/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import jline.console.UserInterruptException;

/**
 * Implements the read-eval-print loop.
 *
 * @see #readEvalPrint(Context, ConsoleHandler, File, boolean)
 */
public class REPL {
    /**
     * The standard R script escapes spaces to "~+~" in "-e" and "-f" commands.
     */
    static String unescapeSpace(String input) {
        return input.replace("~+~", " ");
    }

    private static final Source GET_ECHO = Source.newBuilder("R", ".Internal(getOption('echo'))", "<echo>").internal(true).buildLiteral();
    private static final Source QUIT_EOF = Source.newBuilder("R", ".Internal(quit('default', 0L, TRUE))", "<quit-on-eof>").internal(true).buildLiteral();
    private static final Source GET_PROMPT = Source.newBuilder("R", ".Internal(getOption('prompt'))", "<prompt>").internal(true).buildLiteral();
    private static final Source GET_CONTINUE_PROMPT = Source.newBuilder("R", ".Internal(getOption('continue'))", "<continue-prompt>").internal(true).buildLiteral();

    public static int readEvalPrint(Context context, ConsoleHandler consoleHandler, boolean useExecutor) {
        return readEvalPrint(context, consoleHandler, null, useExecutor);
    }

    /**
     * The read-eval-print loop, which can take input from a console, command line expression or a
     * file. There are two ways the repl can terminate:
     * <ol>
     * <li>A {@code quit} command is executed successfully.</li>
     * <li>EOF on the input.</li>
     * </ol>
     * In case 2, we must implicitly execute a {@code quit("default, 0L, TRUE} command before
     * exiting. So,in either case, we never return.
     */
    public static int readEvalPrint(Context context, ConsoleHandler consoleHandler, File srcFile, boolean useExecutor) {
        final ExecutorService executor;
        if (useExecutor) {
            executor = context.eval(Source.newBuilder("R", ".fastr.getExecutor()", "<get-executor>").internal(true).buildLiteral()).asHostObject();
            initializeNativeEventLoop(context, executor);
        } else {
            executor = null;
        }
        int lastStatus = 0;

        try {
            while (true) { // processing inputs
                boolean doEcho = doEcho(executor, context);
                consoleHandler.setPrompt(doEcho ? getPrompt(executor, context) : null);
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

                    String continuePrompt = null;
                    StringBuilder sb = new StringBuilder(input);
                    int startLine = consoleHandler.getCurrentLineIndex();
                    while (true) { // processing subsequent lines while input is incomplete
                        lastStatus = 0;
                        try {
                            Source src;
                            if (srcFile != null) {
                                int endLine = consoleHandler.getCurrentLineIndex();
                                src = Source.newBuilder("R", sb.toString(), srcFile.toString() + "#" + startLine + "-" + endLine).interactive(true).uri(srcFile.toURI()).buildLiteral();
                            } else {
                                src = Source.newBuilder("R", sb.toString(), "<REPL>").interactive(true).buildLiteral();
                            }
                            if (useExecutor) {
                                try {
                                    executor.submit(() -> context.eval(src)).get();
                                } catch (ExecutionException ex) {
                                    throw ex.getCause();
                                }
                            } else {
                                context.eval(src);
                            }
                        } catch (InterruptedException ex) {
                            throw RMain.fatal("Unexpected interrup error");
                        } catch (PolyglotException e) {
                            if (e.isIncompleteSource()) {
                                if (continuePrompt == null) {
                                    continuePrompt = doEcho ? getContinuePrompt(executor, context) : null;
                                }
                                // read another line of input
                                consoleHandler.setPrompt(continuePrompt);
                                String additionalInput = consoleHandler.readLine();
                                if (additionalInput == null) {
                                    throw new EOFException();
                                }
                                sb.append('\n');
                                sb.append(additionalInput);
                                // The only continuation in the while loop
                                continue;
                            } else if (e.isExit()) {
                                // usually from quit
                                throw new ExitException(e.getExitStatus());
                            } else if (e.isHostException() || e.isInternalError()) {
                                // we continue the repl even though the system may be broken
                                lastStatus = 1;
                            } else if (e.isGuestException()) {
                                // drop through to continue REPL and remember last eval was an error
                                lastStatus = 1;
                            }
                        }
                        break;
                    }
                } catch (EOFException e) {
                    try {
                        context.eval(QUIT_EOF);
                    } catch (PolyglotException e2) {
                        if (e2.isExit()) {
                            // don't use the exit code from the PolyglotException
                            return lastStatus;
                        } else if (e2.isCancelled()) {
                            continue;
                        }
                        throw RMain.fatal(e, "error while calling quit");
                    }
                } catch (UserInterruptException e) {
                    // interrupted by ctrl-c
                }
            }
        } catch (ExitException e) {
            return e.code;
        } catch (Throwable ex) {
            System.err.println("Unexpected error in REPL");
            return 1;
        }
    }

    /**
     * See <code>FastRInitEventLoop</code> for the description of how events occurring in the native
     * code are dispatched.
     *
     * @param context
     * @param executor
     */
    private static void initializeNativeEventLoop(Context context, final ExecutorService executor) {
        Value result = context.eval(Source.newBuilder("R", ".fastr.initEventLoop", "<init-event-loop>").internal(true).buildLiteral()).execute();
        if (result.isNull()) {
            return; // event loop is not configured to be run
        } else if (result.getMember("result").asInt() != 0) {
            System.out.println("WARNING: Native event loop unavailable. Error code: " + result.getMember("result").asInt());
        } else {
            final String fifoInPath = result.getMember("fifoInPath").asString();
            Thread t = new Thread() {
                @Override
                public void run() {
                    final File fifoFile = new File(fifoInPath);
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            final FileInputStream fis = new FileInputStream(fifoFile);
                            fis.read();
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        executor.submit(() -> {
                            context.eval(Source.newBuilder("R", ".fastr.dispatchNativeHandlers", "<dispatch-native-handlers>").internal(true).buildLiteral()).execute().asInt();
                        });
                    }
                }
            };
            t.start();
        }
    }

    private static final class ExitException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final int code;

        ExitException(int code) {
            this.code = code;
        }
    }

    private static boolean doEcho(ExecutorService executor, Context context) throws InterruptedException {
        try {
            if (executor != null) {
                return executor.submit(() -> doEcho(context)).get();
            } else {
                return doEcho(context);
            }
        } catch (ExecutionException ex) {
            throw RMain.fatal(ex, "error while retrieving echo");
        } catch (PolyglotException e) {
            if (e.isExit()) {
                throw new ExitException(e.getExitStatus());
            }
            throw RMain.fatal(e, "error while retrieving echo");
        }
    }

    private static boolean doEcho(Context context) {
        return context.eval(GET_ECHO).asBoolean();
    }

    private static String getPrompt(ExecutorService executor, Context context) throws InterruptedException {
        try {
            if (executor != null) {
                return executor.submit(() -> getPrompt(context)).get();
            } else {
                return getPrompt(context);
            }
        } catch (ExecutionException ex) {
            throw RMain.fatal(ex, "error while retrieving prompt");
        } catch (PolyglotException e) {
            if (e.isExit()) {
                throw new ExitException(e.getExitStatus());
            }
            throw RMain.fatal(e, "error while retrieving prompt");
        }
    }

    private static String getPrompt(Context context) {
        return context.eval(GET_PROMPT).asString();
    }

    private static String getContinuePrompt(ExecutorService executor, Context context) throws InterruptedException {
        try {
            if (executor != null) {
                return executor.submit(() -> getContinuePrompt(context)).get();
            } else {
                return getPrompt(context);
            }
        } catch (ExecutionException ex) {
            throw RMain.fatal(ex, "error while retrieving continue prompt");
        } catch (PolyglotException e) {
            if (e.isExit()) {
                throw new ExitException(e.getExitStatus());
            }
            throw RMain.fatal(e, "error while retrieving continue prompt");
        }
    }

    private static String getContinuePrompt(Context context) {
        return context.eval(GET_CONTINUE_PROMPT).asString();
    }
}
