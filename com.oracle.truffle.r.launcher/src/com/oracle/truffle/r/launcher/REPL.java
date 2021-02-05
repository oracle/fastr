/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.shadowed.org.jline.reader.UserInterruptException;

/**
 * Implements the read-eval-print loop.
 *
 * @see #readEvalPrint(Context, ConsoleHandler, File, REngineExecutor)
 */
public class REPL {
    /**
     * The standard R script escapes spaces to "~+~" in "-e" and "-f" commands.
     */
    static String unescapeSpace(String input) {
        return input.replace("~+~", " ");
    }

    public static final String CANCEL_QUITTING_MEMBER_NAME = "FastR_error_cancelQuitting";
    public static final String ERROR_PRINTED_MEMBER_NAME = "FastR_error_printed";
    private static final Source GET_ECHO = Source.newBuilder("R", ".Internal(getOption('echo'))", "<echo>").internal(true).buildLiteral();
    private static final Source QUIT_EOF = Source.newBuilder("R", ".Internal(quit('default', 0L, TRUE))", "<quit-on-eof>").internal(true).buildLiteral();
    private static final Source GET_PROMPT = Source.newBuilder("R", ".Internal(getOption('prompt'))", "<prompt>").internal(true).buildLiteral();
    private static final Source GET_CONTINUE_PROMPT = Source.newBuilder("R", ".Internal(getOption('continue'))", "<continue-prompt>").internal(true).buildLiteral();
    private static final Source SET_CONSOLE_PROMPT_HANDLER = Source.newBuilder("R", ".fastr.set.consoleHandler", "<set-console-handler>").internal(true).buildLiteral();
    private static final Source GET_EXECUTOR = Source.newBuilder("R", ".fastr.getExecutor()", "<get-executor>").internal(true).buildLiteral();
    private static final Source PRINT_ERROR = Source.newBuilder("R", ".fastr.printError", "<print-error>").internal(true).buildLiteral();

    public static final class REngineExecutor {
        private ExecutorService executorService;

        public void setExecutorService(ExecutorService executorService) {
            this.executorService = executorService;
        }

        public boolean run(Runnable action) {
            if (executorService != null) {
                try {
                    executorService.submit(action, null).get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Unexpected error when closing the context: " + e.getClass().getSimpleName());
                    return false;
                }
            } else {
                action.run();
            }
            return true;
        }
    }

    public static int readEvalPrint(Context context, ConsoleHandler consoleHandler, REngineExecutor rExecutor) {
        return readEvalPrint(context, consoleHandler, null, rExecutor);
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
     * 
     * The {@code rExecutor} parameter may be left {@code null}, in which case we do not use
     * executor, otherwise the {@code rExecutor} is updated with the actual {@code ExecutorService}.
     */
    public static int readEvalPrint(Context context, ConsoleHandler consoleHandler, File srcFile, REngineExecutor rExecutor) {
        final boolean useExecutor = rExecutor != null;
        final ExecutorService executor;
        final EventLoopThread eventLoopThread;
        if (useExecutor) {
            executor = context.eval(GET_EXECUTOR).asHostObject();
            rExecutor.setExecutorService(executor);
            eventLoopThread = initializeNativeEventLoop(context, executor);
        } else {
            executor = null;
            eventLoopThread = null;
        }
        run(executor, () -> context.eval(SET_CONSOLE_PROMPT_HANDLER).execute(consoleHandler.getPolyglotWrapper()));
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
                            } else if (e.isInternalError()) {
                                // we continue the repl even though the system may be broken
                                lastStatus = 1;
                            } else if (e.isHostException() || e.isGuestException()) {
                                handleError(executor, context, e);
                                // drop through to continue REPL and remember last eval was an error
                                lastStatus = 1;
                            }
                        }
                        break;
                    }
                } catch (EOFException e) {
                    try {
                        run(executor, () -> context.eval(QUIT_EOF));
                    } catch (PolyglotException e2) {
                        if (e2.isExit()) {
                            // don't use the exit code from the PolyglotException
                            return lastStatus;
                        } else {
                            if (isJumpToTopLevel(executor, e2)) {
                                continue;
                            }
                        }
                        throw RMain.fatal(e, "error while calling quit");
                    }
                } catch (UserInterruptException e) {
                    // interrupted by ctrl-c
                }
            }
        } catch (ExitException e) {
            // e.g. when executing 'R -e "some err statement",
            // we end up here after REPL.QUIT_EOF was invoked on EOF,
            // in such a case the exit code is always hardcoded 0
            // => we should check lastStatus too
            if (e.code == 0 && lastStatus != 0) {
                return lastStatus;
            }
            return e.code;
        } catch (Throwable ex) {
            System.err.println("Unexpected error in REPL");
            ex.printStackTrace();
            return 1;
        } finally {
            if (eventLoopThread != null) {
                eventLoopThread.stopLoop();
            }

            traceEventLoopLogger.close();
        }
    }

    private static boolean isJumpToTopLevel(ExecutorService executor, PolyglotException exception) {
        return run(executor, () -> {
            Value exObj = exception.getGuestObject();
            if (exObj != null && exObj.hasMember(CANCEL_QUITTING_MEMBER_NAME)) {
                Value val = exObj.getMember(CANCEL_QUITTING_MEMBER_NAME);
                return val.isBoolean() && val.asBoolean();
            }
            return false;
        }, false);
    }

    /**
     * This is a temporary class facilitating catching an occasional hanging attributed to the
     * native event loop mechanism.
     */
    static class SimpleTraceEventLoopLogger {
        private FileWriter traceEventLoopLogWriter;
        private final long timestamp = System.currentTimeMillis();

        {
            try {
                if ("true".equalsIgnoreCase(System.getenv("TRACE_EVENT_LOOP"))) {
                    traceEventLoopLogWriter = new FileWriter("traceEventLoop.log", true);
                }
            } catch (IOException e) {
            }
        }

        void log(String msg) {
            if (traceEventLoopLogWriter != null) {
                try {
                    traceEventLoopLogWriter.append(String.format("DEBUG[%d]: traceEventLoop: %s\n", timestamp, msg));
                    traceEventLoopLogWriter.flush();
                } catch (IOException e) {
                }
            }
        }

        void close() {
            if (traceEventLoopLogWriter != null) {
                try {
                    traceEventLoopLogWriter.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static final SimpleTraceEventLoopLogger traceEventLoopLogger = new SimpleTraceEventLoopLogger();

    static class EventLoopThread extends Thread {

        private final File fifoInFile;
        private final File fifoOutFile;
        private final Context context;
        private final ExecutorService executor;

        EventLoopThread(String fifoInPath, String fifoOutPath, Context context, ExecutorService executor) {
            this.fifoInFile = new File(fifoInPath);
            this.fifoOutFile = new File(fifoOutPath);
            this.context = context;
            this.executor = executor;
        }

        void stopLoop() {
            try {
                interrupt();
                try (FileOutputStream fis = new FileOutputStream(fifoInFile)) {
                    fis.write(66);
                    fis.flush();
                }
                join(8000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void releaseFifoOut() {
            try {
                try (FileOutputStream fis = new FileOutputStream(fifoOutFile)) {
                    fis.write(65);
                    fis.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    try (FileInputStream fis = new FileInputStream(fifoInFile)) {
                        fis.read();
                        if (isInterrupted()) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                executor.submit(() -> {
                    traceEventLoopLogger.log("before dispatching request");
                    try {
                        int res = context.eval(Source.newBuilder("R", ".fastr.dispatchNativeHandlers", "<dispatch-native-handlers>").internal(true).buildLiteral()).execute().asInt();
                        traceEventLoopLogger.log("after dispatching request, res=" + res);
                    } catch (Throwable ex) {
                        releaseFifoOut();
                        traceEventLoopLogger.log("error in dispatching request");
                        ex.printStackTrace();
                    }
                });
            }
        }
    }

    public static void handleError(ExecutorService executor, Context context, PolyglotException eIn) {
        PolyglotException e = eIn;
        if (eIn.getCause() instanceof PolyglotException) {
            e = (PolyglotException) eIn.getCause();
        }

        // we just removed all trailing <R> frames and there stil is something left =>
        // let's see if it is a R exception or polyglot

        if (!wasPrinted(executor, e)) {
            boolean isFastR = isFastRRError(executor, e);
            StringBuilder sb = new StringBuilder();
            if (!isFastR) {
                sb.append("Error in polyglot evaluation : ");
            }
            if (e.isHostException()) {
                sb.append(e.asHostException().toString());
            } else if (e.getMessage() != null) {
                sb.append(e.getMessage());
            }
            if (sb.length() > 0) {
                run(executor, () -> context.eval(PRINT_ERROR).execute(sb.toString()));
            }
        }
    }

    private static boolean wasPrinted(ExecutorService executor, PolyglotException e) {
        return run(executor, () -> {
            Value guestObject = e.getGuestObject();
            if (guestObject == null || !guestObject.hasMember(ERROR_PRINTED_MEMBER_NAME)) {
                return false;
            }
            Value wasPrinted = guestObject.getMember(ERROR_PRINTED_MEMBER_NAME);
            if (!wasPrinted.isBoolean()) {
                return false;
            }
            return wasPrinted.asBoolean();
        });
    }

    private static boolean isFastRRError(ExecutorService executor, PolyglotException e) {
        return run(executor, () -> {
            Value guestObject = e.getGuestObject();
            return guestObject != null && guestObject.hasMember(ERROR_PRINTED_MEMBER_NAME);
        });
    }

    /**
     * See <code>FastRInitEventLoop</code> for the description of how events occurring in the native
     * code are dispatched.
     *
     * @param context
     * @param executor
     * @return the event loop thread
     */
    private static EventLoopThread initializeNativeEventLoop(Context context, final ExecutorService executor) {
        Source initEventLoopSource = Source.newBuilder("R", ".fastr.initEventLoop", "<init-event-loop>").internal(true).buildLiteral();
        Value result = context.eval(initEventLoopSource).execute();
        if (result.isNull()) {
            return null; // event loop is not configured to be run
        } else if (result.getMember("result").asInt() != 0) {
            // TODO: it breaks pkgtest when parsing output
            // System.err.println("WARNING: Native event loop unavailable. Error code: " +
            // result.getMember("result").asInt());
            return null;
        } else {
            final String fifoInPath = result.getMember("fifoInPath").asString();
            final String fifoOutPath = result.getMember("fifoOutPath").asString();
            EventLoopThread t = new EventLoopThread(fifoInPath, fifoOutPath, context, executor);
            t.start();
            return t;
        }
    }

    private static final class ExitException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final int code;

        ExitException(int code) {
            this.code = code;
        }
    }

    private static boolean doEcho(ExecutorService executor, Context context) {
        return run(executor, () -> context.eval(GET_ECHO).asBoolean());
    }

    private static String getPrompt(ExecutorService executor, Context context) {
        return run(executor, () -> context.eval(GET_PROMPT).asString());
    }

    private static String getContinuePrompt(ExecutorService executor, Context context) {
        return run(executor, () -> context.eval(GET_CONTINUE_PROMPT).asString());
    }

    private static <T> T run(ExecutorService executor, Callable<T> run) {
        return run(executor, run, true);
    }

    private static <T> T run(ExecutorService executor, Callable<T> run, boolean checkTopLevelJump) {
        try {
            if (executor != null) {
                try {
                    return executor.submit(run).get();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    throw RMain.fatal(ex, "Unexpected error " + cause.getMessage());
                }
            } else {
                return run.call();
            }
        } catch (PolyglotException e) {
            if (e.isExit()) {
                throw new ExitException(e.getExitStatus());
            } else if (checkTopLevelJump && isJumpToTopLevel(executor, e)) {
                throw e;
            }
            throw RMain.fatal(e, "Unexpected error " + e.getMessage());
        } catch (Exception e) {
            throw RMain.fatal(e, "Unexpected error " + e.getMessage());
        }
    }
}
