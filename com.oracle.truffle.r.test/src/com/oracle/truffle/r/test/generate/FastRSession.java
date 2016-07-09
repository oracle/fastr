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
package com.oracle.truffle.r.test.generate;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RCmdOptions.Client;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RInternalSourceDescription;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.test.TestBase;

public final class FastRSession implements RSession {

    private static final int DEFAULT_TIMEOUT = System.getProperty("DisableTestTimeout") != null ? Integer.MAX_VALUE : 10000;
    private static final int LONG_TIMEOUT = System.getProperty("DisableTestTimeout") != null ? Integer.MAX_VALUE : 60000;

    /**
     * A (virtual) console handler that collects the output in a {@link StringBuilder} for
     * comparison. It does not separate error output as the test analysis doesn't need it.
     */
    public static class TestConsoleHandler implements ConsoleHandler {
        private final StringBuilder buffer = new StringBuilder();
        private final Deque<String> input = new ArrayDeque<>();

        public void setInput(String[] lines) {
            input.clear();
            input.addAll(Arrays.asList(lines));
        }

        @Override
        @TruffleBoundary
        public void println(String s) {
            buffer.append(s);
            buffer.append('\n');
        }

        @Override
        @TruffleBoundary
        public void print(String s) {
            buffer.append(s);
        }

        @Override
        public String readLine() {
            return input.pollFirst();
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

        @Override
        @TruffleBoundary
        public void printErrorln(String s) {
            println(s);
        }

        @Override
        @TruffleBoundary
        public void printError(String s) {
            print(s);
        }

        @Override
        public String getPrompt() {
            return null;
        }

        @Override
        public void setPrompt(String prompt) {
            // ignore
        }

        @TruffleBoundary
        void reset() {
            buffer.delete(0, buffer.length());
        }

        @Override
        public int getWidth() {
            return RContext.CONSOLE_WIDTH;
        }

        @Override
        public String getInputDescription() {
            return "<test input>";
        }
    }

    private static FastRSession singleton;

    private final TestConsoleHandler consoleHandler;
    private final PolyglotEngine main;
    private final RContext mainContext;

    private EvalThread evalThread;

    public static FastRSession create() {
        if (singleton == null) {
            singleton = new FastRSession();
        }
        return singleton;
    }

    public static final Source GET_CONTEXT = RSource.fromTextInternal("invisible(.fastr.context.get())", RInternalSourceDescription.GET_CONTEXT);

    public PolyglotEngine createTestContext(ContextInfo contextInfoArg) {
        create();
        ContextInfo contextInfo;
        if (contextInfoArg == null) {
            contextInfo = createContextInfo(ContextKind.SHARE_PARENT_RW);
        } else {
            contextInfo = contextInfoArg;
        }
        return contextInfo.createVM();
    }

    public ContextInfo createContextInfo(ContextKind contextKind) {
        RCmdOptions options = RCmdOptions.parseArguments(Client.RSCRIPT, new String[0]);
        return ContextInfo.create(options, contextKind, mainContext, consoleHandler, TimeZone.getTimeZone("CET"));
    }

    private FastRSession() {
        consoleHandler = new TestConsoleHandler();
        try {
            RCmdOptions options = RCmdOptions.parseArguments(Client.RSCRIPT, new String[]{"--no-restore"});
            ContextInfo info = ContextInfo.create(options, ContextKind.SHARE_NOTHING, null, consoleHandler);
            main = info.createVM();
            try {
                mainContext = main.eval(GET_CONTEXT).as(RContext.class);
            } catch (IOException e) {
                throw new RuntimeException("error while retrieving test context", e);
            }
        } finally {
            System.out.print(consoleHandler.buffer.toString());
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public String eval(String expression, ContextInfo contextInfo, boolean longTimeout) throws Throwable {
        consoleHandler.reset();

        EvalThread thread = evalThread;
        if (thread == null || !thread.isAlive() || contextInfo != thread.contextInfo) {
            thread = new EvalThread(contextInfo);
            thread.setName("FastR evaluation");
            thread.start();
            evalThread = thread;
        }

        thread.push(expression);

        try {
            if (!thread.await(longTimeout ? LONG_TIMEOUT : DEFAULT_TIMEOUT)) {
                consoleHandler.println("<timeout>");
                thread.stop();
                evalThread = null;
                throw new TimeoutException();
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        if (thread.killedByException != null) {
            evalThread = null;
            throw thread.killedByException;
        }
        return consoleHandler.buffer.toString();
    }

    private final class EvalThread extends RContext.ContextThread {

        private volatile String expression;
        private volatile Throwable killedByException;
        private final Semaphore entry = new Semaphore(0);
        private final Semaphore exit = new Semaphore(0);
        private final ContextInfo contextInfo;

        EvalThread(ContextInfo contextInfo) {
            super(null);
            this.contextInfo = contextInfo;
        }

        public void push(String exp) {
            this.expression = exp;
            this.entry.release();
        }

        public boolean await(int millisTimeout) throws InterruptedException {
            return exit.tryAcquire(millisTimeout, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            while (killedByException == null) {
                try {
                    entry.acquire();
                } catch (InterruptedException e) {
                    break;
                }
                try {
                    PolyglotEngine vm = createTestContext(contextInfo);
                    consoleHandler.setInput(expression.split("\n"));
                    try {
                        String input = consoleHandler.readLine();
                        while (input != null) {
                            Source source = RSource.fromTextInternal(input, RInternalSourceDescription.UNIT_TEST);
                            try {
                                vm.eval(source);
                                input = consoleHandler.readLine();
                            } catch (IncompleteSourceException | com.oracle.truffle.api.vm.IncompleteSourceException e) {
                                String additionalInput = consoleHandler.readLine();
                                if (additionalInput == null) {
                                    throw e;
                                }
                                input += "\n" + additionalInput;
                            }
                        }
                    } finally {
                        vm.dispose();
                    }
                } catch (ParseException e) {
                    e.report(consoleHandler);
                } catch (Throwable t) {
                    if (t instanceof IOException) {
                        if (t.getCause() instanceof RError || t.getCause() instanceof RInternalError) {
                            t = t.getCause();
                        }
                    }
                    if (t instanceof RError) {
                        // nothing to do
                    } else {
                        if (!TestBase.ProcessFailedTests) {
                            if (t instanceof RInternalError) {
                                RInternalError.reportError(t);
                            }
                            t.printStackTrace();
                        }
                        killedByException = t;
                    }
                } finally {
                    exit.release();
                }
            }
        }
    }

    @Override
    public String name() {
        return "FastR";
    }
}
