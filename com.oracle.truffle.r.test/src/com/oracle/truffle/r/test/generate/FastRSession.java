/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.generate;

import static com.oracle.truffle.r.runtime.context.FastROptions.PrintErrorStacktraces;
import static com.oracle.truffle.r.runtime.context.FastROptions.PrintErrorStacktracesToFile;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Instrument;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.SuspendedCallback;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.r.launcher.RCmdOptions;
import com.oracle.truffle.r.launcher.RCmdOptions.Client;
import com.oracle.truffle.r.launcher.REPL;
import com.oracle.truffle.r.launcher.RStartParams;
import com.oracle.truffle.r.launcher.StringConsoleHandler;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.ChildContextInfo;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.engine.interop.VectorInteropTest;
import com.oracle.truffle.r.test.generate.FastRContext.SharedFastRContext;
import com.oracle.truffle.r.test.generate.FastRContext.TestByteArrayInputStream;

public final class FastRSession implements RSession {

    public static final Source GET_CONTEXT = createSource("invisible(.fastr.context.get())", RSource.Internal.GET_CONTEXT.string);

    private static final String TEST_TIMEOUT_PROPERTY = "fastr.test.timeout";
    private static int timeoutValue = VectorDataLibrary.ENABLE_VERY_SLOW_ASSERTS ? 30000 : 10000;

    private static FastRSession singleton;

    private SharedFastRContext sharedContext;

    public static FastRSession create() {
        if (singleton == null) {
            singleton = new FastRSession();
        }
        return singleton;
    }

    public static Source createSource(String txt, String name) {
        return Source.newBuilder("R", txt, name).internal(true).interactive(true).buildLiteral();
    }

    // TODO: used from the outsize: can the users use the shared context? Otherwise change them to the exclusive one
    public FastRContext createContext(ContextKind contextKind) {
        return getContext(contextKind, true);
    }

    public FastRContext getContext(ContextKind contextKind, @SuppressWarnings("unused") boolean allowHostAccess) {
        if (contextKind == ContextKind.SHARE_PARENT_RW) {
            return sharedContext.newSession();
        } else if (contextKind == ContextKind.SHARE_NOTHING) {
            return createVanillaContext(false);
        } else {
            throw new IllegalStateException("Unexpected: " + contextKind);
        }
    }

    public RContext getContext() {
        return sharedContext.getInternalContext();
    }

    public static Context.Builder getContextBuilder(String... languages) {
        Context.Builder builder = Context.newBuilder(languages).allowExperimentalOptions(true);
        setCLIOptions(builder);
        builder.allowAllAccess(true);
        builder.option(FastROptions.getName(PrintErrorStacktraces), "true");
        // no point in printing errors to file when running tests (that contain errors on purpose)
        builder.option(FastROptions.getName(PrintErrorStacktracesToFile), "false");
        return builder;
    }

    private static void setCLIOptions(Context.Builder builder) {
        for (Map.Entry<String, String> entry : TestBase.options.entrySet()) {
            builder.option(entry.getKey(), entry.getValue());
        }
    }

    private FastRSession() {
        String timeOutProp = System.getProperty(TEST_TIMEOUT_PROPERTY);
        if (timeOutProp != null) {
            if (timeOutProp.length() == 0) {
                timeoutValue = Integer.MAX_VALUE;
            } else {
                int timeoutGiven = Integer.parseInt(timeOutProp);
                timeoutValue = timeoutGiven * 1000;
                // no need to scale longTimeoutValue
            }
        }
        try {
            sharedContext = (SharedFastRContext) createVanillaContext(true);
        } finally {
            try {
                System.out.print(sharedContext.getOutput().toString("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private static FastRContext createVanillaContext(boolean isShared) {
        RStartParams params = new RStartParams(RCmdOptions.parseArguments(new String[]{Client.R.argumentName(), "--vanilla", "--no-echo", "--silent", "--no-restore"}, false), false);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        TestByteArrayInputStream input = new TestByteArrayInputStream();
        ChildContextInfo info = ChildContextInfo.create(params, null, ContextKind.SHARE_NOTHING, null, input, output, output);
        // TODO: do we need the config info in this case? -- we should eventually remove it altogether
        RContext.childInfo = info;
        Context truffleContext = getContextBuilder("R", "llvm").in(input).out(output).err(output).build();
        if (isShared) {
            return new SharedFastRContext(truffleContext, input, output);
        } else {
            return new FastRContext(truffleContext, input, output);
        }
    }

    private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

    {
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
    }

    private String readLine(TestByteArrayInputStream input) {
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
    public String eval(TestBase testClass, String expression, ContextKind contextKind, long timeout) throws Throwable {
        return eval(testClass, expression, contextKind, timeout, true);
    }

    public String eval(TestBase testClass, String expression, ContextKind contextKind, long timeout, boolean allowHostAccess) throws Throwable {
        assert contextKind != null;
        ByteArrayOutputStream output;
        try (FastRContext evalContext = getContext(contextKind, allowHostAccess)) {
            output = evalContext.getOutput();
            eval(evalContext, testClass, expression, contextKind, timeout);
        }
        try {
            return output.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "<exception>";
        }
    }

    private void eval(FastRContext evalContext, TestBase testClass, String expression, ContextKind contextKind, long timeout) throws Throwable {
        try {
            evalContext.reset();
            evalContext.getInput().setContents(expression);
            Timer timer = null;
            try {
                // set up some interop objects used by fastr-specific tests:
                if (testClass != null) {
                    // TODO: either provide a way to reset them or disallow this with the shared
                    // context
                    testClass.addPolyglotSymbols(evalContext);
                }
                timer = scheduleTimeBoxing(evalContext.getEngine(), timeout == USE_DEFAULT_TIMEOUT ? timeoutValue : timeout);
                String consoleInput = readLine(evalContext.getInput());
                while (consoleInput != null) {
                    try {
                        try {
                            Source src = createSource(consoleInput, RSource.Internal.UNIT_TEST.string);
                            evalContext.eval(src);
                            // checked exceptions are wrapped in PolyglotException
                        } catch (PolyglotException e) {
                            // TODO see bellow - need the wrapped exception for special handling of
                            // ParseException, etc
                            Throwable wt = getWrappedThrowable(e);
                            if (wt instanceof RError) {
                                REPL.handleError(null, evalContext.getContext(), e);
                            }
                            throw wt;
                        }
                        consoleInput = readLine(evalContext.getInput());
                    } catch (IncompleteSourceException e) {
                        String additionalInput = readLine(evalContext.getInput());
                        if (additionalInput == null) {
                            throw e;
                        }
                        consoleInput += "\n" + additionalInput;
                    }
                }
            } finally {
                if (timer != null) {
                    timer.cancel();
                }
            }
        } catch (ParseException e) {
            e.report(evalContext.getOutput());
        } catch (ExitException | JumpToTopLevelException e) {
            // exit and jumpToTopLevel exceptions are legitimate if a test case calls "q()" or "Q"
            // during debugging
        } catch (RError e) {
            // nothing to do
        } catch (Throwable t) {
            if (!TestBase.ProcessFailedTests || TestBase.ShowFailedTestsResults) {
                if (t instanceof RInternalError) {
                    RInternalError.reportError(t, evalContext.getInternalContext());
                }
                t.printStackTrace();
            }
            throw t;
        }
    }

    public String evalInREPL(TestBase testClass, String expression, ContextKind contextKind, long timeout, boolean allowHostAccess) throws Throwable {
        assert contextKind != null;
        Timer timer = null;
        ByteArrayOutputStream output;
        try (FastRContext evalContext = getContext(contextKind, allowHostAccess)) {
            evalContext.reset();
            output = evalContext.getOutput();
            evalContext.getInput().setContents(expression);

            // set up some interop objects used by fastr-specific tests:
            if (testClass != null) {
                testClass.addPolyglotSymbols(evalContext);
            }
            timer = scheduleTimeBoxing(evalContext.getEngine(), timeout == USE_DEFAULT_TIMEOUT ? timeoutValue : timeout);
            REPL.readEvalPrint(evalContext.getContext(), new StringConsoleHandler(Arrays.asList(expression.split("\n")), output), null, null);
            String consoleInput = readLine(evalContext.getInput());
            while (consoleInput != null) {
                consoleInput = readLine(evalContext.getInput());
            }
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

    private static Throwable getWrappedThrowable(PolyglotException e) {
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
                        // print diagnostic info
                        System.out.println("fastr unittest timeout of " + timeout + " ms reached");
                        Thread.dumpStack();
                        System.out.println(Utils.createStackTrace(true));

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

    /**
     * Finds given field from the class of {@code value} or from its super classes and return its
     * value. Throws {@link AssertionError} if not found.
     * 
     * @return Value of the field. TODO we need better support to access the TruffleObject in Value
     */
    private static Object getField(Object value, String name) {
        try {
            Field field = null;
            Class<?> klazz = value.getClass();
            while (klazz != null) {
                try {
                    field = klazz.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    // noop
                }
                if (field != null) {
                    break;
                }
                klazz = klazz.getSuperclass();
            }
            assert field != null;
            setAccessible(field, true);
            return field.get(value);
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

    public static Value execInContext(FastRContext context, Callable<Object> c) {
        return execInContext(context, c, (Class<?>[]) null);
    }

    public static <E extends Exception> Value execInContext(FastRContext context, Callable<Object> c, Class<?>... acceptExceptions) {
        context.eval(FastRSession.GET_CONTEXT); // ping creation of TruffleRLanguage
        context.getPolyglotBindings().putMember("testSymbol", (ProxyExecutable) (Value... args) -> {
            try {
                return c.call();
            } catch (Exception ex) {
                if (acceptExceptions != null) {
                    for (Class<?> cs : acceptExceptions) {
                        if (cs.isAssignableFrom(ex.getClass())) {
                            return null;
                        }
                    }
                }
                RLogger.getLogger(VectorInteropTest.class.getName()).log(Level.SEVERE, null, ex);
                fail();
            }
            return null;
        });
        return context.getPolyglotBindings().getMember("testSymbol").execute();
    }

}
