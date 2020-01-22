/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
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
import com.oracle.truffle.api.debug.SuspendAnchor;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.debug.SuspensionFilter;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.data.RPromise.EagerPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.test.generate.FastRSession;
import com.oracle.truffle.tck.DebuggerTester;

public class FastRDebugTest {
    private Debugger debugger;
    private DebuggerSession debuggerSession;
    private final LinkedList<Runnable> run = new LinkedList<>();
    private SuspendedEvent suspendedEvent;
    private Throwable ex;
    private Context context;
    protected final ByteArrayOutputStream out = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Before
    public void before() {
        suspendedEvent = null;

        context = FastRSession.getContextBuilder("R", "llvm").in(System.in).out(out).err(err).build();
        debugger = context.getEngine().getInstruments().get("debugger").lookup(Debugger.class);
        debuggerSession = debugger.startSession(event -> {
            suspendedEvent = event;
            performWork();
            suspendedEvent = null;
        });

        run.clear();
    }

    @After
    public void dispose() {
        context.close();
        debuggerSession.close();
    }

    private static Source sourceFromText(String code, String name) throws IOException {
        return Source.newBuilder("R", code, name).interactive(true).build();
    }

    private static Source createFactorial() throws IOException {
        return sourceFromText("main <- function() {\n" +
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
                        "factorial.r");
    }

    private static Source createRStatements() throws IOException {
        return sourceFromText("foo <- function(a) {\n" +
                        "  x = 2L * a\n" +
                        "}\n" +
                        "foo(1)\n" +
                        "x <- 1:100\n" +
                        "print(foo(x))\n" +
                        "y <- sin(x/10)\n" +
                        "print(foo(y))\n" +
                        "z <- cos(x^1.3/(runif(1)*5+10))\n" +
                        "print(foo(z))\n",
                        "statements.r");
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
            Breakpoint breakpoint = Breakpoint.newBuilder(DebuggerTester.getSourceImpl(factorial)).lineIs(9).build();
            debuggerSession.install(breakpoint);
        });
        // Init before eval:
        performWork();
        context.eval(factorial);
        assertExecutedOK();

        assertLocation(9, "nMinusOne = n - 1",
                        "n", 2);
        continueExecution();

        final Source evalSrc = sourceFromText("main()\n", "test.r");
        final Value value = context.eval(evalSrc);
        assertExecutedOK();
        Assert.assertEquals("[1] 2\n", getOut());
        final int i = value.asInt();
        assertEquals("Factorial computed OK", 2, i);
    }

    @Test
    public void testConditionalBreakpoint() throws Throwable {
        final Source source = sourceFromText("main <- function() { res <- 0;\n" +
                        "  for(i in seq(10)) {\n" +
                        "    res <- res + i\n" +
                        "  }\n" +
                        "  res\n" +
                        "}\n",
                        "test.r");
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            Breakpoint breakpoint = Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(3).build();
            breakpoint.setCondition("i == 5");
            debuggerSession.install(breakpoint);
        });

        // Init before eval:
        performWork();
        context.eval(source);
        assertExecutedOK();

        assertLocation(3, "res <- res + i", "i", 5);
        continueExecution();

        final Source evalSrc = sourceFromText("main()\n", "test2.r");
        Value result = context.eval(evalSrc);
        assertExecutedOK();
        assertEquals("result is correct", 55, result.asInt());
    }

    @Test
    public void stepInStepOver() throws Throwable {
        final Source factorial = createFactorial();
        context.eval(factorial);

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
                        "n", 2);
        stepOver(1);
        assertLocation(10, "nMOFact = Recall(n - 1)",
                        "n", 2,
                        "nMinusOne", 1);
        stepOver(1);
        assertLocation(11, "res = n * nMOFact",
                        "n", 2, "nMinusOne", 1,
                        "nMOFact", 1);
        assertMetaObjectsOrStringValues(factorial, true, "n", "double", "nMOFact", "double", "nMinusOne", "double");
        stepOver(1);
        assertLocation(12, "res",
                        "n", 2,
                        "nMinusOne", 1,
                        "nMOFact", 1,
                        "res", 2);
        stepOver(1);
        assertLocation(2, "fac(2)");
        stepOver(1);
        assertLocation(3, "res", "res", 2);
        stepOut();

        // Init before eval:
        performWork();
        final Source evalSource = sourceFromText("main()\n", "evaltest.r");
        final Value value = context.eval(evalSource);
        assertExecutedOK();
        Assert.assertEquals("[1] 2\n", getOut());
        final int i = value.asInt();
        assertEquals("Factorial computed OK", 2, i);
    }

    @Test
    public void testFindMetaObjectAndSourceLocation() throws Throwable {
        final Source source = sourceFromText("main <- function() {\n" +
                        " i = 3L\n" +
                        " n = 15\n" +
                        " str = 'hello'\n" +
                        " i <- i + 1L\n" +
                        " i\n" +
                        "}\n",
                        "test.r");
        context.eval(source);

        // @formatter:on
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        stepInto(1);
        stepOver(3);
        assertLocation(5, "i <- i + 1L", "i", 3, "n", 15, "str", "[1] \"hello\"");
        assertMetaObjectsOrStringValues(source, true, "i", "integer", "n", "double", "str", "character");
        stepOut();
        performWork();

        final Source evalSource = sourceFromText("main()\n", "evaltest.r");
        context.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testScopeFunction() throws Throwable {
        final Source srcFunMain = sourceFromText("function () {\n" +
                        "    i = 3L\n" +
                        "    n = 15L\n" +
                        "    str = \"hello\"\n" +
                        "    i <- i + 1L\n" +
                        "    ab <<- i\n" +
                        "    i\n" +
                        "}", "testFunc.r");
        final Source source = sourceFromText("x <- 10L\n" +
                        "makeActiveBinding('ab', function(v) { if(missing(v)) x else x <<- v }, .GlobalEnv)\n" +
                        "main <- " + srcFunMain.getCharacters() + "\n",
                        "test.r");
        context.eval(source);

        // @formatter:on
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        assertLocation(1, "main()", "x", 10, "ab", 10, "main", srcFunMain.getCharacters());
        stepInto(1);
        assertLocation(4, "i = 3L");
        stepOver(1);
        assertLocation(5, "n = 15L", "i", 3);
        stepOver(1);
        assertLocation(6, "str = \"hello\"", "i", 3, "n", 15);
        stepOver(1);
        assertLocation(7, "i <- i + 1L", "i", 3, "n", 15, "str", "[1] \"hello\"");
        stepOver(1);
        assertLocation(8, "ab <<- i", "i", 4, "n", 15, "str", "[1] \"hello\"");
        stepOver(1);
        assertScope(9, "i", true, false, "ab", 4, "x", 4);
        stepOut();
        assertLocation(1, "main()", "x", 4, "ab", 4, "main", srcFunMain.getCharacters());
        performWork();

        final Source evalSource = sourceFromText("main()\n", "evaltest.r");
        context.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testScopePromise() throws Throwable {
        final Source source = sourceFromText("main <- function(e) {\n" +
                        " x <- 10L\n" +
                        " e()\n" +
                        " x\n" +
                        "}\n" +
                        "closure <- function() {\n" +
                        " x <<- 123L\n" +
                        " x\n" +
                        "}\n",

                        "test.r");
        context.eval(source);

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

        final Source evalSource = sourceFromText("x <- 0L\nmain(closure)\n", "evaltest.r");
        context.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testScopeArguments() throws Throwable {
        final Source source = sourceFromText("main <- function(a, b, c, d) {\n" +
                        " x <- 10L\n" +
                        "}\n" +
                        "closure <- function() {\n" +
                        " x <<- 123L\n" +
                        " x\n" +
                        "}\n",

                        "test.r");
        context.eval(source);

        // @formatter:on
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        assertArguments(1, "main(1, 2, 3, 4)");

        stepInto(1);
        assertArguments(2, "x <- 10L", "a", 1, "b", 2, "c", 3, "d", 4);
        continueExecution();
        performWork();

        final Source evalSource = sourceFromText("main(1, 2, 3, 4)\n", "evaltest.r");
        context.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testChangedScopeChain() throws Throwable {
        final Source source = sourceFromText("main <- function(e) {\n" +
                        " x <- 10L\n" +
                        " environment(e) <- environment()\n" +
                        " e()\n" +
                        " x\n" +
                        "}\n" +
                        "closure <- function() {\n" +
                        " x <<- 123L\n" +
                        " x\n" +
                        "}\n",
                        "test.r");
        context.eval(source);

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

        final Source evalSource = sourceFromText("x <- 0L\nmain(closure)\n", "evaltest.r");
        context.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testStepOverStatements() throws Throwable {
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.setSteppingFilter(SuspensionFilter.newBuilder().ignoreLanguageContextInitialization(true).build());
            debuggerSession.suspendNextExecution();
        });
        assertLocation(1, "foo <- function(a) {");
        stepOver(1);
        assertLocation(4, "foo(1)");
        stepOver(1);
        assertLocation(5, "x <- 1:100");
        stepOver(1);
        assertLocation(6, "print(foo(x))");
        stepOver(1);
        assertLocation(7, "y <- sin(x/10)");
        stepOver(1);
        assertLocation(8, "print(foo(y))");
        stepOver(1);
        assertLocation(9, "z <- cos(x^1.3/(runif(1)*5+10))");
        stepOver(1);
        assertLocation(10, "print(foo(z))");
        continueExecution();
        performWork();
        context.eval(createRStatements());
        assertExecutedOK();
    }

    @Test
    public void testValueToString() throws Throwable {
        Source source = createFactorial();
        context.eval(source);

        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.suspendNextExecution();
        });

        stepInto(1);
        assertLocation(2, "res = fac(2)");
        stepInto(1);
        assertLocation(6, "if (n <= 1) {");
        assertMetaObjectsOrStringValues(source, true, "n", "promise");
        assertMetaObjectsOrStringValues(source, false, "n", "2");

        continueExecution();
        performWork();

        final Source evalSource = sourceFromText("main()\n", "evaltest.r");
        context.eval(evalSource);

        assertExecutedOK();
    }

    @Test
    public void testReenterArgumentsAndValues() throws Throwable {
        // Test that after a re-enter, arguments are kept and variables are cleared.
        final Source source = sourceFromText("" +
                        "main <- function () {\n" +
                        "  i <- 10\n" +
                        "  fnc(i <- i + 1, 20)\n" +
                        "}\n" +
                        "fnc <- function(n, m) {\n" +
                        "  x <- n + m\n" +
                        "  n <- m - n\n" +
                        "  m <- m / 2\n" +
                        "  x <- x + n * m\n" +
                        "  x\n" +
                        "}\n" +
                        "main()\n", "testReenterArgsAndVals.r");

        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.install(Breakpoint.newBuilder(DebuggerTester.getSourceImpl(source)).lineIs(6).build());
        });

        assertArguments(6, "x <- n + m", "n", "<unevaluated>", "m", 20);
        assertScope(6, "x <- n + m", false, true, "n", "<unevaluated>", "m", 20);
        stepOver(4);
        assertArguments(10, "x", "n", 9, "m", 10);
        assertScope(10, "x", false, true, "n", 9, "m", 10, "x", 121);
        run.addLast(() -> suspendedEvent.prepareUnwindFrame(suspendedEvent.getTopStackFrame()));
        assertArguments(3, "return fnc(i <- i + 1, 20)");
        assertScope(3, "return fnc(i <- i + 1, 20)", false, true, "i", 11);
        continueExecution();
        assertArguments(6, "x <- n + m", "n", 11, "m", 20);
        assertScope(6, "x <- n + m", false, true, "n", 11, "m", 20);
        continueExecution();

        performWork();
        Value ret = context.eval(source);
        assertEquals(121, ret.asInt());
        assertExecutedOK();
    }

    @Test
    public void testActiveBinding() throws Throwable {
        final Source source = sourceFromText("" +
                        "makeActiveBinding(\"bar\", function(x) { if (missing(x)) { 42; } else { cat(\"setting \", x, \"\\n\"); } }, .GlobalEnv)\n" +
                        "x <- bar\n" +
                        "bar <- 24\n", "activeBindingTest.r");
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.setSteppingFilter(SuspensionFilter.newBuilder().ignoreLanguageContextInitialization(true).build());
            debuggerSession.suspendNextExecution();
        });
        stepOver(1);
        assertLocation(2, 1, SuspendAnchor.BEFORE, "x <- bar", false, true);
        run.addLast(() -> {
            DebugValue bar = suspendedEvent.getSession().getTopScope("R").getDeclaredValue("bar");
            assertTrue(bar.isReadable());
            assertTrue(bar.hasReadSideEffects());
            assertTrue(bar.hasWriteSideEffects());
            if (!run.isEmpty()) {
                run.removeFirst().run();
            }
        });
        stepInto(1);
        assertLocation(1, 40, SuspendAnchor.BEFORE, "if (missing(x)) { 42; } else { cat(\"setting \", x, \"\\n\"); }", false, true, "x", "missing");
        stepOver(1);
        assertLocation(1, 58, SuspendAnchor.BEFORE, "42", false, true, "x", "missing");
        stepOver(1);
        // Bug: Location is 1, 1, ""; the node is com.oracle.truffle.r.nodes.function.RCallNodeGen
        // The node has CallTag and SourceSection(source=internal, index=0, length=0, characters=)
        // Expected: assertLocation(2, 9, SuspendAnchor.AFTER, "x <- bar", false, true);
        stepOver(1);
        assertLocation(3, 1, SuspendAnchor.BEFORE, "bar <- 24", false, true);
        run.addLast(() -> {
            DebugValue x = suspendedEvent.getSession().getTopScope("R").getDeclaredValue("x");
            assertTrue(x.isReadable());
            assertFalse(x.hasReadSideEffects());
            assertFalse(x.hasWriteSideEffects());
            if (!run.isEmpty()) {
                run.removeFirst().run();
            }
        });
        stepInto(1);
        assertLocation(1, 40, SuspendAnchor.BEFORE, "if (missing(x)) { 42; } else { cat(\"setting \", x, \"\\n\"); }", false, true, "x", 24);
        stepOver(1);
        assertLocation(1, 71, SuspendAnchor.BEFORE, "cat(\"setting \", x, \"\\n\")", false, true, "x", 24);
        stepOver(1);
        // Expected: assertLocation(3, 10, SuspendAnchor.AFTER, "bar <- 24", false, true, "x", 42);
        continueExecution();

        performWork();
        context.eval(source);
        assertExecutedOK();
    }

    @Test
    public void testStepOut() throws Throwable {
        final Source source = sourceFromText("fnc <- function() {\n" +
                        "  x <- 10L\n" +
                        "  return(x + 10)\n" +
                        "}\n" +
                        "fnc() + fnc()\n",
                        "test.r");
        context.eval(source);
        run.addLast(() -> {
            assertNull(suspendedEvent);
            assertNotNull(debuggerSession);
            debuggerSession.setSteppingFilter(SuspensionFilter.newBuilder().ignoreLanguageContextInitialization(true).build());
            debuggerSession.suspendNextExecution();
        });
        assertLocation(1, "fnc <- function() {");
        stepOver(1);
        assertLocation(5, "fnc() + fnc()");
        stepInto(1);
        assertLocation(2, "x <- 10L");
        stepOver(1);
        assertLocation(3, "return(x + 10)");
        stepOut();
        assertLocation(5, "fnc()");
        stepInto(1);
        assertLocation(2, "x <- 10L");
        continueExecution();
        performWork();
        context.eval(source);
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
        assertLocation(line, -1, null, code, false, false, expectedFrame);
    }

    private void assertLocation(final int line, final int column, final SuspendAnchor anchor, final String code, boolean includeAncestors, boolean completeMatch, final Object... expectedFrame) {
        final RuntimeException trace = new RuntimeException();
        run.addLast(() -> {
            try {
                assertNotNull(suspendedEvent);
                if (anchor != null) {
                    assertEquals(anchor, suspendedEvent.getSuspendAnchor());
                }
                SourceSection sourceSection = suspendedEvent.getSourceSection();
                final int currentLine;
                final int currentColumn;
                if (anchor == null || anchor == SuspendAnchor.BEFORE) {
                    currentLine = sourceSection.getStartLine();
                    currentColumn = sourceSection.getStartColumn();
                } else {
                    currentLine = sourceSection.getEndLine();
                    currentColumn = sourceSection.getEndColumn();
                }
                assertEquals(line, currentLine);
                if (column != -1) {
                    assertEquals(column, currentColumn);
                }
                String currentCode = sourceSection.getCharacters().toString();
                // Trim extra lines in currentCode
                int nl = currentCode.indexOf('\n');
                if (nl >= 0) {
                    currentCode = currentCode.substring(0, nl);
                }
                currentCode = currentCode.trim();
                assertEquals(code, currentCode);
                compareScope(line, code, includeAncestors, completeMatch, expectedFrame);
            } catch (RuntimeException | Error e) {

                final DebugStackFrame frame = suspendedEvent.getTopStackFrame();
                frame.getScope().getDeclaredValues().forEach(var -> {
                    System.out.println(var);
                });
                trace.printStackTrace();
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
        final RuntimeException trace = new RuntimeException();
        run.addLast(() -> {
            try {
                compareScope(line, code, includeAncestors, completeMatch, expectedFrame);
            } catch (RuntimeException | Error e) {

                final DebugStackFrame frame = suspendedEvent.getTopStackFrame();
                frame.getScope().getDeclaredValues().forEach(var -> {
                    System.out.println(var);
                });
                trace.printStackTrace();
                throw e;
            }
        });
    }

    private void assertArguments(final int line, final String code, final Object... expectedArgs) {
        final RuntimeException trace = new RuntimeException();
        run.addLast(() -> {
            final DebugStackFrame frame = suspendedEvent.getTopStackFrame();
            DebugScope scope = frame.getScope();
            if (scope == null) {
                scope = suspendedEvent.getSession().getTopScope("R");
            }
            try {
                int n = expectedArgs.length / 2;
                List<DebugValue> actualValues = new ArrayList<>(n);
                scope.getArguments().forEach((x) -> actualValues.add(x));

                assertEquals(line + ": " + code, n, actualValues.size());

                for (int i = 0; i < n; i++) {
                    int i2 = i << 1;
                    assertEquals(expectedArgs[i2], actualValues.get(i).getName());
                    Object expectedVal = expectedArgs[i2 + 1];
                    expectedVal = expectedToString(expectedVal);
                    assertEquals(expectedVal, actualValues.get(i).as(String.class));
                }

                if (!run.isEmpty()) {
                    run.removeFirst().run();
                }
            } catch (RuntimeException | Error e) {
                e.printStackTrace(System.err);
                scope.getDeclaredValues().forEach(var -> {
                    System.out.println(var);
                });
                trace.printStackTrace();
                throw e;
            }
        });
    }

    private static Object expectedToString(Object val) {
        if (val instanceof Integer || val instanceof Double) {
            return "[1] " + val;
        }
        return val;
    }

    private void compareScope(final int line, final String code, boolean includeAncestors, boolean completeMatch, final Object[] expectedFrame) {
        final DebugStackFrame frame = suspendedEvent.getTopStackFrame();

        final AtomicInteger numFrameVars = new AtomicInteger(0);
        DebugScope scope = frame.getScope();
        if (scope != null) {
            scope.getDeclaredValues().forEach(var -> {
                // skip synthetic slots
                for (RFrameSlot slot : RFrameSlot.values()) {
                    if (slot.toString().equals(var.getName())) {
                        return;
                    }
                }
                numFrameVars.incrementAndGet();
            });
        }
        if (completeMatch) {
            assertEquals(line + ": " + code, expectedFrame.length / 2, numFrameVars.get());
        }

        for (int i = 0; i < expectedFrame.length; i = i + 2) {
            String expectedIdentifier = (String) expectedFrame[i];
            Object expectedValue = expectedFrame[i + 1];
            expectedValue = expectedToString(expectedValue);
            String expectedValueStr = (expectedValue != null) ? expectedValue.toString() : null;
            scope = frame.getScope();
            DebugValue value = null;
            if (scope != null) {
                do {
                    value = scope.getDeclaredValue(expectedIdentifier);
                    scope = scope.getParent();
                } while (includeAncestors && value == null && scope != null && !REnvironment.baseEnv().getName().equals(scope.getName()));
            }
            if (value == null) {
                // Ask the top scope:
                scope = suspendedEvent.getSession().getTopScope("R");
                do {
                    value = scope.getDeclaredValue(expectedIdentifier);
                    scope = scope.getParent();
                } while (includeAncestors && value == null && scope != null && !REnvironment.baseEnv().getName().equals(scope.getName()));
            }
            assertNotNull("identifier \"" + expectedIdentifier + "\" not found", value);
            String valueStr = value.as(String.class);
            assertEquals(line + ": " + code + "; identifier: '" + expectedIdentifier + "'", expectedValueStr, valueStr);
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

    /**
     * Assert either meta object or string value for a set of variables.
     *
     * @param expectedSource expected source in which the values should be examined.
     * @param metaObjects <code>true</code> for checking metaObject or <code>false</code> for
     *            checking <code>value.as(String.class)</code>.
     * @param nameAndValuePairs name followed by value (arbitrary number of times).
     */
    private void assertMetaObjectsOrStringValues(final Source expectedSource, boolean metaObjects, final String... nameAndValuePairs) {
        final RuntimeException trace = new RuntimeException();
        run.addLast((Runnable) () -> {
            try {
                DebugStackFrame frame = suspendedEvent.getTopStackFrame();
                for (int i = 0; i < nameAndValuePairs.length;) {
                    String name = nameAndValuePairs[i++];
                    String expectedValue = nameAndValuePairs[i++];
                    boolean found = false;
                    for (DebugValue value : frame.getScope().getDeclaredValues()) {
                        if (name.equals(value.getName())) {
                            if (metaObjects) {
                                DebugValue moDV = value.getMetaObject();
                                if (moDV != null || expectedValue != null) {
                                    String mo = moDV.as(String.class);
                                    Assert.assertEquals("MetaObjects of '" + name + "' differ:", expectedValue, mo);
                                }
                            } else { // Check as(String.class) value
                                String valAsString = value.as(String.class);
                                Assert.assertEquals("Unexpected " + name + "toString():", expectedValue, valAsString);
                            }
                            found = true;
                            // Trigger findSourceLocation() call
                            SourceSection sourceLocation = value.getSourceLocation();
                            if (sourceLocation != null) {
                                Assert.assertEquals("Sources differ", DebuggerTester.getSourceImpl(expectedSource), sourceLocation.getSource());
                            }
                        }
                    }
                    if (!found) {
                        Assert.fail("DebugValue named '" + name + "' not found.");
                    }
                }
                if (!run.isEmpty()) {
                    run.removeFirst().run();
                }
            } catch (RuntimeException | Error e) {
                final DebugStackFrame frame = suspendedEvent.getTopStackFrame();
                frame.getScope().getDeclaredValues().forEach(var -> {
                    System.out.println(var);
                });
                trace.printStackTrace();
                throw e;
            }
        });
    }

}
