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
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.SuspendedCallback;
import com.oracle.truffle.api.debug.SuspendedEvent;
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
import com.oracle.truffle.r.test.engine.interop.AbstractMRTest;
import com.oracle.truffle.r.test.engine.interop.VectorMRTest;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Instrument;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import static org.junit.Assert.fail;

public final class FastRSession implements RSession {

    public static final Source GET_CONTEXT = createSource("invisible(.fastr.context.get())", RSource.Internal.GET_CONTEXT.string);

    private static final String TEST_TIMEOUT_PROPERTY = "fastr.test.timeout";
    private static int timeoutValue = 10000;
    /**
     * The long timeout is used for package installation and currently needs to be 5 mins for the
     * {@code Matrix} package.
     */
    private static int longTimeoutValue = 300000;

    private static FastRSession singleton;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final TestByteArrayInputStream input = new TestByteArrayInputStream();

    private Context mainContext;
    private RContext mainRContext;

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

    public static FastRSession create() {
        if (singleton == null) {
            singleton = new FastRSession();
        }
        return singleton;
    }

    public static Source createSource(String txt, String name) {
        try {
            return Source.newBuilder("R", txt, name).internal(true).interactive(true).build();
        } catch (IOException ex) {
            Logger.getLogger(FastRSession.class.getName()).log(Level.SEVERE, null, ex);
            assert false;
        }
        return null;
    }

    public ChildContextInfo checkContext(ChildContextInfo contextInfoArg) {
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
        Map<String, String> env = new HashMap<>();
        env.put("TZ", "GMT");
        ChildContextInfo ctx = ChildContextInfo.create(params, env, contextKind, mainRContext, input, output, output);
        RContext.childInfo = ctx;
        return ctx;
    }

    public Context createContext() {
        return Context.newBuilder("R").in(input).out(output).err(output).build();
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
            RContext.childInfo = info;
            mainContext = createContext();
            mainRContext = mainContext.eval(GET_CONTEXT).asHostObject();
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
        return mainRContext;
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
            checkContext(contextInfo);
            Context evalContext = createContext();
            // set up some interop objects used by fastr-specific tests:
            if (testClass != null) {
                testClass.addPolyglotSymbols(evalContext);
            }
            timer = scheduleTimeBoxing(evalContext.getEngine(), longTimeout ? longTimeoutValue : timeoutValue);
            try {
                String consoleInput = readLine();
                while (consoleInput != null) {
                    try {
                        try {
                            Source src = createSource(consoleInput, RSource.Internal.UNIT_TEST.string);
                            evalContext.eval(src);
                            // checked exceptions are wrapped in PolyglotException
                        } catch (PolyglotException e) {
                            // TODO need the wrapped exception for special handling of:
                            // ParseException, RError, ... see bellow
                            throw getWrappedThrowable(e);
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
                evalContext.close();
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

    private Throwable getWrappedThrowable(PolyglotException e) {
        Object f = getField(e, "impl");
        return (Throwable) getField(f, "exception");
    }

    private static Timer scheduleTimeBoxing(Engine engine, long timeout) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Instrument i = engine.getInstruments().get("debugger");
                Debugger debugger = i.lookup(Debugger.class);
                debugger.startSession(new SuspendedCallback() {
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

    public static Object getReceiver(Value value) {
        return getField(value, "receiver");
    }

    // Copied from ReflectionUtils.
    // TODO we need better support to access the TruffleObject in Value
    private static Object getField(Object value, String name) {
        try {
            Field f = value.getClass().getDeclaredField(name);
            setAccessible(f, true);
            return f.get(value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static final boolean Java8OrEarlier = System.getProperty("java.specification.version").compareTo("1.9") < 0;

    private static void setAccessible(Field field, boolean flag) {
        if (!Java8OrEarlier) {
            openForReflectionTo(field.getDeclaringClass(), FastRSession.class);
        }
        field.setAccessible(flag);
    }

    /**
     * Opens {@code declaringClass}'s package to allow a method declared in {@code accessor} to call
     * {@link AccessibleObject#setAccessible(boolean)} on an {@link AccessibleObject} representing a
     * field or method declared by {@code declaringClass}.
     */
    private static void openForReflectionTo(Class<?> declaringClass, Class<?> accessor) {
        try {
            Method getModule = Class.class.getMethod("getModule");
            Class<?> moduleClass = getModule.getReturnType();
            Class<?> modulesClass = Class.forName("jdk.internal.module.Modules");
            Method addOpens = maybeGetAddOpensMethod(moduleClass, modulesClass);
            if (addOpens != null) {
                Object moduleToOpen = getModule.invoke(declaringClass);
                Object accessorModule = getModule.invoke(accessor);
                if (moduleToOpen != accessorModule) {
                    addOpens.invoke(null, moduleToOpen, declaringClass.getPackage().getName(), accessorModule);
                }
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Method maybeGetAddOpensMethod(Class<?> moduleClass, Class<?> modulesClass) {
        try {
            return modulesClass.getDeclaredMethod("addOpens", moduleClass, String.class, moduleClass);
        } catch (NoSuchMethodException e) {
            // This method was introduced by JDK-8169069
            return null;
        }
    }

    public static void execInContext(Context context, Callable<Object> c) {
        execInContext(context, c, (Class<?>) null);
    }

    public static <E extends Exception> void execInContext(Context context, Callable<Object> c, Class<?>... acceptExceptions) {
        context.eval(FastRSession.GET_CONTEXT); // ping creation of TruffleRLanguage
        context.exportSymbol("testSymbol", (ProxyExecutable) (Value... args) -> {
            try {
                c.call();
            } catch (Exception ex) {
                if (acceptExceptions != null) {
                    for (Class<?> cs : acceptExceptions) {
                        if (cs.isAssignableFrom(ex.getClass())) {
                            return null;
                        }
                    }
                }
                Logger.getLogger(VectorMRTest.class.getName()).log(Level.SEVERE, null, ex);
                fail();
            }
            return null;
        });
        context.importSymbol("testSymbol").execute();
    }

}
