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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.SuspendedCallback;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.launcher.RCmdOptions;
import com.oracle.truffle.r.launcher.RCmdOptions.Client;
import com.oracle.truffle.r.launcher.RStartParams;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.context.ChildContextInfo;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.test.TestBase;

public final class FastRSession implements RSession {

    private static final String TEST_TIMEOUT_PROPERTY = "fastr.test.timeout";
    private static int timeoutValue = 10000;
    /**
     * The long timeout is used for package installation and currently needs to be 5 mins for the
     * {@code Matrix} package.
     */
    private static int longTimeoutValue = 300000;

    private static final class TestByteArrayInputStream extends ByteArrayInputStream {

        TestByteArrayInputStream() {
            super(new byte[0]);
        }

        public void setContents(String data) {
            this.buf = data.getBytes(StandardCharsets.UTF_8);
            this.count = this.buf.length;
            this.pos = 0;
        }

        @Override
        public synchronized int read() {
            return super.read();
        }
    }

    private static FastRSession singleton;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final TestByteArrayInputStream input = new TestByteArrayInputStream();
    private final PolyglotEngine main;
    private final RContext mainContext;

    public static FastRSession create() {
        if (singleton == null) {
            singleton = new FastRSession();
        }
        return singleton;
    }

    public static final Source GET_CONTEXT = RSource.fromTextInternal("invisible(.fastr.context.get())", RSource.Internal.GET_CONTEXT);

    public ChildContextInfo checkContext(ChildContextInfo contextInfoArg) {
        create();
        ChildContextInfo contextInfo;
        if (contextInfoArg == null) {
            contextInfo = createContextInfo(ContextKind.SHARE_PARENT_RW);
        } else {
            contextInfo = contextInfoArg;
        }
        return contextInfo;
    }

    public ChildContextInfo createContextInfo(ContextKind contextKind) {
        RStartParams params = new RStartParams(RCmdOptions.parseArguments(Client.R, new String[]{"R", "--vanilla", "--slave", "--silent", "--no-restore"}, false), false);
        return ChildContextInfo.create(params, null, contextKind, mainContext, input, output, output, TimeZone.getTimeZone("GMT"));
    }

    private FastRSession() {
        String timeOutProp = System.getProperty(TEST_TIMEOUT_PROPERTY);
        if (timeOutProp != null) {
            if (timeOutProp.length() == 0) {
                timeoutValue = Integer.MAX_VALUE;
                longTimeoutValue = Integer.MAX_VALUE;
            } else {
                int timeoutGiven = Integer.parseInt(timeOutProp);
                timeoutValue = timeoutGiven * 1000;
                // no need to scale longTimeoutValue
            }
        }
        try {
            RStartParams params = new RStartParams(RCmdOptions.parseArguments(Client.R, new String[]{"R", "--vanilla", "--slave", "--silent", "--no-restore"}, false), false);
            ChildContextInfo info = ChildContextInfo.create(params, null, ContextKind.SHARE_NOTHING, null, input, output, output);
            main = info.createVM();
            mainContext = main.eval(GET_CONTEXT).as(RContext.class);
        } finally {
            try {
                System.out.print(output.toString("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

    {
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
    }

    public RContext getContext() {
        return mainContext;
    }

    private String readLine() {
        /*
         * We cannot use an InputStreamReader because it buffers characters internally, whereas
         * readLine() should not buffer across newlines.
         */

        ByteBuffer bytes = ByteBuffer.allocate(16);
        CharBuffer chars = CharBuffer.allocate(16);
        StringBuilder str = new StringBuilder();
        decoder.reset();
        boolean initial = true;
        while (true) {
            int inputByte = input.read();
            if (inputByte == -1) {
                return initial ? null : str.toString();
            }
            initial = false;
            bytes.put((byte) inputByte);
            bytes.flip();
            decoder.decode(bytes, chars, false);
            chars.flip();
            while (chars.hasRemaining()) {
                char c = chars.get();
                if (c == '\n' || c == '\r') {
                    return str.toString();
                }
                str.append(c);
            }
            bytes.compact();
            chars.clear();
        }
    }

    @Override
    public String eval(TestBase testClass, String expression, ChildContextInfo contextInfo, boolean longTimeout) throws Throwable {
        Timer timer = null;
        output.reset();
        input.setContents(expression);
        try {
            ChildContextInfo actualContextInfo = checkContext(contextInfo);
            // set up some interop objects used by fastr-specific tests:
            PolyglotEngine.Builder builder = PolyglotEngine.newBuilder();
            if (testClass != null) {
                testClass.addPolyglotSymbols(builder);
            }
            PolyglotEngine vm = actualContextInfo.createVM(builder);
            timer = scheduleTimeBoxing(vm, longTimeout ? longTimeoutValue : timeoutValue);
            try {
                String consoleInput = readLine();
                while (consoleInput != null) {
                    Source source = RSource.fromTextInternal(consoleInput, RSource.Internal.UNIT_TEST);
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
                        consoleInput = readLine();
                    } catch (IncompleteSourceException e) {
                        String additionalInput = readLine();
                        if (additionalInput == null) {
                            throw e;
                        }
                        consoleInput += "\n" + additionalInput;
                    }
                }
            } finally {
                vm.dispose();
            }
        } catch (ParseException e) {
            e.report(output);
        } catch (ExitException | JumpToTopLevelException e) {
            // exit and jumpToTopLevel exceptions are legitimate if a test case calls "q()" or "Q"
            // during debugging
        } catch (RError e) {
            // nothing to do
        } catch (Throwable t) {
            if (!TestBase.ProcessFailedTests || TestBase.ShowFailedTestsResults) {
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
        try {
            return output.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "<exception>";
        }
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
