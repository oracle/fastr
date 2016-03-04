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
import java.util.*;
import java.util.concurrent.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.*;
import com.oracle.truffle.r.engine.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RCmdOptions.Client;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;

public final class FastRSession implements RSession {

    private static final int TIMEOUT = System.getProperty("DisableTestTimeout") != null ? Integer.MAX_VALUE : 10000;

    /**
     * A (virtual) console handler that collects the output in a {@link StringBuilder} for
     * comparison. It does not separate error output as the test analysis doesn't need it.
     */
    public static class TestConsoleHandler implements ConsoleHandler {
        private final StringBuilder buffer = new StringBuilder();

        @TruffleBoundary
        public void println(String s) {
            buffer.append(s);
            buffer.append('\n');
        }

        @TruffleBoundary
        public void print(String s) {
            buffer.append(s);
        }

        public String readLine() {
            return null;
        }

        public boolean isInteractive() {
            return false;
        }

        @TruffleBoundary
        public void printErrorln(String s) {
            println(s);
        }

        @TruffleBoundary
        public void printError(String s) {
            print(s);
        }

        public void redirectError() {
            // always
        }

        public String getPrompt() {
            return null;
        }

        public void setPrompt(String prompt) {
            // ignore
        }

        @TruffleBoundary
        void reset() {
            buffer.delete(0, buffer.length());
        }

        public int getWidth() {
            return RContext.CONSOLE_WIDTH;
        }

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

    private static final Source GET_CONTEXT = Source.fromText("invisible(fastr.context.get())", "<get_context>").withMimeType(TruffleRLanguage.MIME);

    public PolyglotEngine createTestContext() {
        create();
        RCmdOptions options = RCmdOptions.parseArguments(Client.RSCRIPT, new String[0]);
        ContextInfo info = ContextInfo.create(options, ContextKind.SHARE_PARENT_RW, mainContext, consoleHandler, TimeZone.getTimeZone("CET"));
        return info.apply(PolyglotEngine.newBuilder()).build();
    }

    private FastRSession() {
        consoleHandler = new TestConsoleHandler();
        try {
            RCmdOptions options = RCmdOptions.parseArguments(Client.RSCRIPT, new String[0]);
            ContextInfo info = ContextInfo.create(options, ContextKind.SHARE_NOTHING, null, consoleHandler);
            main = info.apply(PolyglotEngine.newBuilder()).build();
            try {
                mainContext = main.eval(GET_CONTEXT).as(RContext.class);
            } catch (IOException e) {
                throw new RuntimeException("error while retrieving test context", e);
            }
        } finally {
            System.out.print(consoleHandler.buffer.toString());
        }
    }

    @SuppressWarnings("deprecation")
    public String eval(String expression) throws Throwable {
        consoleHandler.reset();

        EvalThread thread = evalThread;
        if (thread == null || !thread.isAlive()) {
            thread = new EvalThread();
            thread.setName("FastR evaluation");
            thread.start();
            evalThread = thread;
        }

        thread.push(expression);

        try {
            if (!thread.await(TIMEOUT)) {
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

        EvalThread() {
            super(null);
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
                    PolyglotEngine vm = createTestContext();
                    try {
                        Source source = Source.fromText(expression, "<eval>").withMimeType(TruffleRLanguage.MIME);
                        vm.eval(source);
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
                        t.printStackTrace();
                        killedByException = t;
                    }
                } finally {
                    exit.release();
                }
            }
        }
    }

    public String name() {
        return "FastR";
    }
}
