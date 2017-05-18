/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.debug.Breakpoint;
import com.oracle.truffle.api.debug.DebugScope;
import com.oracle.truffle.api.debug.DebugStackFrame;
import com.oracle.truffle.api.debug.DebugValue;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.DebuggerSession;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;
import com.oracle.truffle.r.runtime.RCmdOptions.Client;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.ContextInfo;
import com.oracle.truffle.r.runtime.context.DefaultConsoleHandler;
import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.runtime.data.RPromise.EagerPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;

public class FastRDebugTest {
    private Debugger debugger;
    private DebuggerSession debuggerSession;
    private final LinkedList<Runnable> run = new LinkedList<>();
    private SuspendedEvent suspendedEvent;
    private Throwable ex;
    protected PolyglotEngine engine;
    protected final ByteArrayOutputStream out = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Before
    public void before() {
        suspendedEvent = null;

        ConsoleHandler consoleHandler = new DefaultConsoleHandler(System.in, out);
        ContextInfo info = ContextInfo.createNoRestore(Client.R, null, ContextKind.SHARE_NOTHING, null, consoleHandler);
        engine = info.createVM(PolyglotEngine.newBuilder().setOut(out).setErr(err));
        debugger = Debugger.find(engine);
        debuggerSession = debugger.startSession(event -> {
            suspendedEvent = event;
            performWork();
            suspendedEvent = null;
        });

        run.clear();
    }

    @After
    public void dispose() {
        debuggerSession.close();
        if (engine != null) {
            engine.dispose();
        }
    }

    private static Source createFactorial() {
        return RSource.fromTextInternal("main <- function() {\n" +
                        "  res = fac(2)\n" +
                        "  res\n" +
                        "}\n" +
                        "fac <- function(n) {\n" +
                        "    if (n <= 1) {\n" +
                        "        1\n" +
                        "    } else {\n" +
                        "        nMinusOne = n - 1\n" +
                        "        nMOFact = Recall(n - 1)\n" +
                        "        res = n * nMOFact\n" +
                        "        res\n" +
                        "    }\n" +
                        "}\n",
                        RSource.Internal.DEBUGTEST_FACTORIAL);
    }

    protected final String getOut() {
        return new String(out.toByteArray());
    }

    protected final String getErr() {
        try {
            err.flush();
        } catch (IOException e) {
        }
        return new String(err.toByteArray());
    }

    @Test
    public void testBreakpoint() throws Throwable {
        final Source factorial = createFactorial();

        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            Breakpoint breakpoint = Breakpoint.newBuilder(factorial).lineIs(9).build();
            debuggerSession.install(breakpoint);
        });
        // Init before eval:
        performWork();
        engine.eval(factorial);
        assertExecutedOK();

        assertLocation(9, "nMinusOne = n - 1",
                        "n", "2.0");
        continueExecution();

        final Source evalSrc = RSource.fromTextInternal("main()\n", RSource.Internal.DEBUGTEST_DEBUG);
        final Value value = engine.eval(evalSrc);
        assertExecutedOK();
        Assert.assertEquals("[1] 2\n", getOut());
        final Number n = value.as(Number.class);
        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
    }

    @Test
    public void stepInStepOver() throws Throwable {
        final Source factorial = createFactorial();
        engine.eval(factorial);

        // @formatter:on
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        stepInto(1);
        assertLocation(2, "res = fac(2)");
        stepInto(2);
        assertLocation(9, "nMinusOne = n - 1",
                        "n", 2.0);
        stepOver(1);
        assertLocation(10, "nMOFact = Recall(n - 1)",
                        "n", 2.0,
                        "nMinusOne", 1.0);
        stepOver(1);
        assertLocation(11, "res = n * nMOFact",
                        "n", 2.0, "nMinusOne", 1.0,
                        "nMOFact", 1.0);
        assertMetaObjects(factorial, "n", "double", "nMOFact", "double", "nMinusOne", "double");
        stepOver(1);
        assertLocation(12, "res",
                        "n", 2.0,
                        "nMinusOne", 1.0,
                        "nMOFact", 1.0,
                        "res", 2.0);
        stepOver(1);
        assertLocation(2, "fac(2)");
        stepOver(1);
        assertLocation(3, "res", "res", 2.0);
        stepOut();

        // Init before eval:
        performWork();
        final Source evalSource = RSource.fromTextInternal("main()\n", RSource.Internal.DEBUGTEST_EVAL);
        final Value value = engine.eval(evalSource);
        assertExecutedOK();
        Assert.assertEquals("[1] 2\n", getOut());
        final Number n = value.as(Number.class);
        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
    }

    @Test
    public void testFindMetaObjectAndSourceLocation() throws Throwable {
        final Source source = RSource.fromTextInternal("main <- function() {\n" +
                        "  i = 3L\n" +
                        "  n = 15\n" +
                        "  str = 'hello'\n" +
                        "  i <- i + 1L\n" +
                        "  i\n" +
                        "}\n",
                        RSource.Internal.DEBUGTEST_DEBUG);
        engine.eval(source);

        // @formatter:on
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        stepInto(1);
        stepOver(3);
        assertLocation(5, "i <- i + 1L", "i", 3, "n", 15.0, "str", "hello");
        assertMetaObjects(source, "i", "integer", "n", "double", "str", "character");
        stepOut();
        performWork();

        final Source evalSource = RSource.fromTextInternal("main()\n", RSource.Internal.DEBUGTEST_EVAL);
        engine.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testScopeFunction() throws Throwable {
        final Source srcFunMain = RSource.fromTextInternal("function () {\n" +
                        "    i = 3L\n" +
                        "    n = 15L\n" +
                        "    str = \"hello\"\n" +
                        "    i <- i + 1L\n" +
                        "    ab <<- i\n" +
                        "    i\n" +
                        "}", RSource.Internal.DEBUGTEST_DEBUG);
        final Source source = RSource.fromTextInternal("x <- 10L\n" +
                        "makeActiveBinding('ab', function(v) { if(missing(v)) x else x <<- v }, .GlobalEnv)\n" +
                        "main <- " + srcFunMain.getCode() + "\n",
                        RSource.Internal.DEBUGTEST_DEBUG);
        engine.eval(source);

        // @formatter:on
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        assertLocation(1, "main()", "x", 10, "ab", 10, "main", srcFunMain.getCode());
        stepInto(1);
        assertLocation(4, "i = 3L");
        stepOver(1);
        assertLocation(5, "n = 15L", "i", 3);
        stepOver(1);
        assertLocation(6, "str = \"hello\"", "i", 3, "n", 15);
        stepOver(1);
        assertLocation(7, "i <- i + 1L", "i", 3, "n", 15, "str", "hello");
        stepOver(1);
        assertLocation(8, "ab <<- i", "i", 4, "n", 15, "str", "hello");
        stepOver(1);
        assertScope(9, "i", true, false, "ab", 4, "x", 4);
        stepOut();
        assertLocation(1, "main()", "x", 4, "ab", 4, "main", srcFunMain.getCode());
        performWork();

        final Source evalSource = RSource.fromTextInternal("main()\n", RSource.Internal.DEBUGTEST_EVAL);
        engine.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testScopePromise() throws Throwable {
        final Source source = RSource.fromTextInternal("main <- function(e) {\n" +
                        "   x <- 10L\n" +
                        "   e()\n" +
                        "   x\n" +
                        "}\n" +
                        "closure <- function() {\n" +
                        "   x <<- 123L\n" +
                        "   x\n" +
                        "}\n",

                        RSource.Internal.DEBUGTEST_DEBUG);
        engine.eval(source);

        // @formatter:on
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        stepOver(1);
        stepInto(1);
        stepOver(1);
        assertScope(3, "e()", false, false, "x", 10);
        stepInto(1);
        assertLocation(7, "x <<- 123L");
        assertScope(7, "x <<- 123L", true, false, "x", 0);
        stepOver(1);
        assertScope(8, "x", true, false, "x", 123);
        continueExecution();
        performWork();

        final Source evalSource = RSource.fromTextInternal("x <- 0L\nmain(closure)\n", RSource.Internal.DEBUGTEST_EVAL);
        engine.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testScopeArguments() throws Throwable {
        final Source source = RSource.fromTextInternal("main <- function(a, b, c, d) {\n" +
                        "   x <- 10L\n" +
                        "}\n" +
                        "closure <- function() {\n" +
                        "   x <<- 123L\n" +
                        "   x\n" +
                        "}\n",

                        RSource.Internal.DEBUGTEST_DEBUG);
        engine.eval(source);

        // @formatter:on
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        assertArguments(1, "main(1, 2, 3, 4)");

        stepInto(1);
        assertArguments(2, "x <- 10L", "a", "b", "c", "d");
        continueExecution();
        performWork();

        final Source evalSource = RSource.fromTextInternal("main(1, 2, 3, 4)\n", RSource.Internal.DEBUGTEST_EVAL);
        engine.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testChangedScopeChain() throws Throwable {
        final Source source = RSource.fromTextInternal("main <- function(e) {\n" +
                        "   x <- 10L\n" +
                        "   environment(e) <- environment()\n" +
                        "   e()\n" +
                        "   x\n" +
                        "}\n" +
                        "closure <- function() {\n" +
                        "   x <<- 123L\n" +
                        "   x\n" +
                        "}\n",
                        RSource.Internal.DEBUGTEST_DEBUG);
        engine.eval(source);

        // @formatter:on
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        stepOver(1);
        stepInto(1);
        stepOver(2);
        assertScope(4, "e()", false, false, "x", 10);
        stepInto(1);
        assertLocation(8, "x <<- 123L");
        assertScope(8, "x <<- 123L", true, false, "x", 10);
        stepOver(1);
        stepOut();
        assertScope(9, "x", false, false, "x", 123);
        assertIdentifiers(false, "x", "e");
        stepOut();
        assertScope(9, "x", false, false, "x", 0);
        continueExecution();
        performWork();

        final Source evalSource = RSource.fromTextInternal("x <- 0L\nmain(closure)\n", RSource.Internal.DEBUGTEST_EVAL);
        engine.eval(evalSource);

        assertExecutedOK();
    }

    private void performWork() {
        try {
            if (ex == null && !run.isEmpty()) {
                Runnable c = run.removeFirst();
                c.run();
            }
        } catch (Throwable e) {
            ex = e;
        }
    }

    private void stepOver(final int size) {
        run.addLast(() -> suspendedEvent.prepareStepOver(size));
    }

    private void stepOut() {
        run.addLast(() -> suspendedEvent.prepareStepOut(1));
    }

    private void continueExecution() {
        run.addLast(() -> suspendedEvent.prepareContinue());
    }

    private void stepInto(final int size) {
        run.addLast(() -> suspendedEvent.prepareStepInto(size));
    }

    private void assertIdentifiers(boolean includeAncestors, String... identifiers) {
        run.addLast(() -> {

            final DebugStackFrame frame = suspendedEvent.getTopStackFrame();
            DebugScope scope = frame.getScope();

            Set<String> actualIdentifiers = new HashSet<>();
            do {
                scope.getDeclaredValues().forEach((x) -> actualIdentifiers.add(x.getName()));
            } while (includeAncestors && scope != null && !REnvironment.baseEnv().getName().equals(scope.getName()));

            Set<String> expected = new HashSet<>();
            for (String s : identifiers) {
                expected.add(s);
            }

            assertEquals(expected, actualIdentifiers);

            if (!run.isEmpty()) {
                run.removeFirst().run();
            }
        });
    }

    private void assertLocation(final int line, final String code, final Object... expectedFrame) {
        run.addLast(() -> {
            try {
                assertNotNull(suspendedEvent);
                final int currentLine = suspendedEvent.getSourceSection().getStartLine();
                assertEquals(line, currentLine);
                final String currentCode = suspendedEvent.getSourceSection().getCode().trim();
                assertEquals(code, currentCode);
                compareScope(line, code, false, true, expectedFrame);
            } catch (RuntimeException | Error e) {

                final DebugStackFrame frame = suspendedEvent.getTopStackFrame();
                frame.forEach(var -> {
                    System.out.println(var);
                });
                throw e;
            }
        });
    }

    /**
     * Ensure that the scope at a certain program position contains an expected set of key-value
     * pairs.
     *
     * @param line line number
     * @param code the code snippet of the program location
     * @param includeAncestors Include current scope's ancestors for the identifier lookup.
     * @param completeMatch {@code true} if the defined key-value pairs should be the only pairs in
     *            the scope.
     * @param expectedFrame the key-value pairs (e.g. {@code "id0", 1, "id1", "strValue"})
     */
    private void assertScope(final int line, final String code, boolean includeAncestors, boolean completeMatch, final Object... expectedFrame) {
        run.addLast(() -> {
            try {
                compareScope(line, code, includeAncestors, completeMatch, expectedFrame);
            } catch (RuntimeException | Error e) {

                final DebugStackFrame frame = suspendedEvent.getTopStackFrame();
                frame.forEach(var -> {
                    System.out.println(var);
                });
                throw e;
            }
        });
    }

    private void assertArguments(final int line, final String code, final String... expectedArgs) {
        run.addLast(() -> {
            try {
                final DebugStackFrame frame = suspendedEvent.getTopStackFrame();

                DebugScope scope = frame.getScope();

                Set<String> actualIdentifiers = new HashSet<>();
                scope.getArguments().forEach((x) -> actualIdentifiers.add(x.getName()));

                assertEquals(line + ": " + code, expectedArgs.length, actualIdentifiers.size());

                Set<String> expectedIds = new HashSet<>(Arrays.asList(expectedArgs));
                Assert.assertEquals(expectedIds, actualIdentifiers);

                if (!run.isEmpty()) {
                    run.removeFirst().run();
                }
            } catch (RuntimeException | Error e) {

                final DebugStackFrame frame = suspendedEvent.getTopStackFrame();
                frame.forEach(var -> {
                    System.out.println(var);
                });
                throw e;
            }
        });
    }

    private void compareScope(final int line, final String code, boolean includeAncestors, boolean completeMatch, final Object[] expectedFrame) {
        final DebugStackFrame frame = suspendedEvent.getTopStackFrame();

        final AtomicInteger numFrameVars = new AtomicInteger(0);
        frame.forEach(var -> {
            // skip synthetic slots
            for (RFrameSlot slot : RFrameSlot.values()) {
                if (slot.toString().equals(var.getName())) {
                    return;
                }
            }
            numFrameVars.incrementAndGet();
        });
        if (completeMatch) {
            assertEquals(line + ": " + code, expectedFrame.length / 2, numFrameVars.get());
        }

        for (int i = 0; i < expectedFrame.length; i = i + 2) {
            String expectedIdentifier = (String) expectedFrame[i];
            Object expectedValue = expectedFrame[i + 1];
            String expectedValueStr = (expectedValue != null) ? expectedValue.toString() : null;
            DebugScope scope = frame.getScope();
            DebugValue value;
            do {
                value = scope.getDeclaredValue(expectedIdentifier);
                scope = scope.getParent();
            } while (includeAncestors && value == null && scope != null && !REnvironment.baseEnv().getName().equals(scope.getName()));
            assertNotNull("identifier \"" + expectedIdentifier + "\" not found", value);
            String valueStr = value.as(String.class);
            assertEquals(expectedValueStr, valueStr);
        }

        if (!run.isEmpty()) {
            run.removeFirst().run();
        }
    }

    Object getRValue(Object value) {
        // This will only work in simple cases
        if (value instanceof EagerPromise) {
            return ((EagerPromise) value).getValue();
        }
        return value;
    }

    private void assertExecutedOK() throws Throwable {
        Assert.assertTrue(getErr(), getErr().isEmpty());
        if (ex != null) {
            if (ex instanceof AssertionError) {
                throw ex;
            } else {
                throw new AssertionError("Error during execution", ex);
            }
        }
        assertTrue("Assuming all requests processed: " + run, run.isEmpty());
    }

    private void assertMetaObjects(final Source expectedSource, final String... nameAndMetaObjectPairs) {
        run.addLast((Runnable) () -> {
            DebugStackFrame frame = suspendedEvent.getTopStackFrame();
            for (int i = 0; i < nameAndMetaObjectPairs.length;) {
                String name = nameAndMetaObjectPairs[i++];
                String expectedMO = nameAndMetaObjectPairs[i++];
                boolean found = false;
                for (DebugValue value : frame) {
                    if (name.equals(value.getName())) {
                        DebugValue moDV = value.getMetaObject();
                        if (moDV != null || expectedMO != null) {
                            String mo = moDV.as(String.class);
                            Assert.assertEquals("MetaObjects of '" + name + "' differ:", expectedMO, mo);
                        }
                        found = true;
                        // Trigger findSourceLocation() call
                        SourceSection sourceLocation = value.getSourceLocation();
                        if (sourceLocation != null) {
                            Assert.assertSame("Sources differ", expectedSource, sourceLocation.getSource());
                        }
                    }
                }
                if (!found) {
                    Assert.fail("DebugValue named '" + name + "' not found.");
                }
            }
            run.removeFirst().run();
        });
    }

}
