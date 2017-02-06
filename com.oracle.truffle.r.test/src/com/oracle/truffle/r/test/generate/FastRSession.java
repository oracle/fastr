/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.SuspendedCallback;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RCmdOptions;
import com.oracle.truffle.r.runtime.RCmdOptions.Client;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RStartParams;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.test.TestBase;

public final class FastRSession implements RSession {

    private static final String TEST_TIMEOUT_PROPERTY = "FastRTestTimeout";
    private static final String DISABLE_TIMEOUT_PROPERTY = "DisableTestTimeout"; // legacy
    private static int timeoutValue = 10000;
    /**
     * The long timeout is used for package installation and currently needs to be 5 mins for the
     * {@code Matrix} package.
     */
    private static int longTimeoutValue = 300000;

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
        public String getInputDescription() {
            return "<test input>";
        }
    }

    private static FastRSession singleton;

    private final TestConsoleHandler consoleHandler;
    private final PolyglotEngine main;
    private final RContext mainContext;

    public static FastRSession create() {
        if (singleton == null) {
            singleton = new FastRSession();
        }
        return singleton;
    }

    public static final Source GET_CONTEXT = RSource.fromTextInternal("invisible(.fastr.context.get())", RSource.Internal.GET_CONTEXT);

    public ContextInfo checkContext(ContextInfo contextInfoArg) {
        create();
        ContextInfo contextInfo;
        if (contextInfoArg == null) {
            contextInfo = createContextInfo(ContextKind.SHARE_PARENT_RW);
        } else {
            contextInfo = contextInfoArg;
        }
        return contextInfo;
    }

    public ContextInfo createContextInfo(ContextKind contextKind) {
        RStartParams params = new RStartParams(RCmdOptions.parseArguments(Client.RSCRIPT, new String[0], false), false);
        return ContextInfo.create(params, null, contextKind, mainContext, consoleHandler, TimeZone.getTimeZone("GMT"));
    }

    private FastRSession() {
        if (System.getProperty(DISABLE_TIMEOUT_PROPERTY) != null) {
            timeoutValue = Integer.MAX_VALUE;
            longTimeoutValue = Integer.MAX_VALUE;
        } else if (System.getProperty(TEST_TIMEOUT_PROPERTY) != null) {
            int timeoutGiven = Integer.parseInt(System.getProperty(TEST_TIMEOUT_PROPERTY));
            timeoutValue = timeoutGiven * 1000;
            // no need to scale longTimeoutValue
        }
        consoleHandler = new TestConsoleHandler();
        try {
            RStartParams params = new RStartParams(RCmdOptions.parseArguments(Client.RSCRIPT, new String[]{"--no-restore"}, false), false);
            ContextInfo info = ContextInfo.create(params, null, ContextKind.SHARE_NOTHING, null, consoleHandler);
            main = info.createVM();
            mainContext = main.eval(GET_CONTEXT).as(RContext.class);
        } finally {
            System.out.print(consoleHandler.buffer.toString());
        }
    }

    @Override
    public String eval(TestBase testClass, String expression, ContextInfo contextInfo, boolean longTimeout) throws Throwable {
        Timer timer = null;
        consoleHandler.reset();
        try {
            ContextInfo actualContextInfo = checkContext(contextInfo);
            // set up some interop objects used by fastr-specific tests:
            PolyglotEngine.Builder builder = PolyglotEngine.newBuilder();
            if (testClass != null) {
                testClass.addPolyglotSymbols(builder);
            }
            PolyglotEngine vm = actualContextInfo.createVM(builder);
            timer = scheduleTimeBoxing(vm, longTimeout ? longTimeoutValue : timeoutValue);
            consoleHandler.setInput(expression.split("\n"));
            try {
                String input = consoleHandler.readLine();
                while (input != null) {
                    Source source = RSource.fromTextInternal(input, RSource.Internal.UNIT_TEST);
                    try {
                        try {
                            vm.eval(source);
                            // checked exceptions are wrapped in RuntimeExceptions
                        } catch (RuntimeException e) {
                            if (e.getCause() instanceof com.oracle.truffle.api.vm.IncompleteSourceException) {
                                throw e.getCause().getCause();
                            } else {
                                throw e;
                            }
                        }
                        input = consoleHandler.readLine();
                    } catch (IncompleteSourceException e) {
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
        } catch (RError e) {
            // nothing to do
        } catch (Throwable t) {
            if (!TestBase.ProcessFailedTests) {
                if (t instanceof RInternalError) {
                    RInternalError.reportError(t);
                }
                t.printStackTrace();
            }
            throw t;
        } finally {
            if (timer != null) {
                timer.cancel();
            }
        }
        return consoleHandler.buffer.toString();
    }

    private static Timer scheduleTimeBoxing(PolyglotEngine engine, long timeout) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Debugger.find(engine).startSession(new SuspendedCallback() {
                    @Override
                    public void onSuspend(SuspendedEvent event) {
                        event.prepareKill();
                    }
                }).suspendNextExecution();
            }
        }, timeout);
        return timer;
    }

    @Override
    public String name() {
        return "FastR";
    }
}
