/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.LinkedList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.ExecutionEvent;
import com.oracle.truffle.api.debug.SuspendedEvent;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.LineLocation;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.EventConsumer;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;
import com.oracle.truffle.r.runtime.data.RPromise.EagerPromise;

public class FastRDebugTest {
    private Debugger debugger;
    private final LinkedList<Runnable> run = new LinkedList<>();
    private SuspendedEvent suspendedEvent;
    private Throwable ex;
    private ExecutionEvent executionEvent;
    protected PolyglotEngine engine;
    protected final ByteArrayOutputStream out = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Before
    public void before() {
        suspendedEvent = null;
        executionEvent = null;
        engine = PolyglotEngine.newBuilder().setOut(out).setErr(err).onEvent(new EventConsumer<ExecutionEvent>(ExecutionEvent.class) {
            @Override
            protected void on(ExecutionEvent event) {
                executionEvent = event;
                debugger = executionEvent.getDebugger();
                performWork();
                executionEvent = null;
            }
        }).onEvent(new EventConsumer<SuspendedEvent>(SuspendedEvent.class) {
            @Override
            protected void on(SuspendedEvent event) {
                suspendedEvent = event;
                performWork();
                suspendedEvent = null;
            }
        }).build();
        run.clear();
    }

    @After
    public void dispose() {
        if (engine != null) {
            engine.dispose();
        }
    }

    private static Source createFactorial() {
        return Source.fromText("main <- function() {\n" +
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
                        "factorial.r").withMimeType(
                                        "application/x-r");
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

        run.addLast(new Runnable() {
            @Override
            public void run() {
                try {
                    assertNull(suspendedEvent);
                    assertNotNull(executionEvent);
                    LineLocation nMinusOne = factorial.createLineLocation(9);
                    debugger.setLineBreakpoint(0, nMinusOne, false);
                    executionEvent.prepareContinue();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        engine.eval(factorial);
        assertExecutedOK();

        run.addLast(new Runnable() {
            @Override
            public void run() {
                // the breakpoint should hit instead
            }
        });
        assertLocation(9, "nMinusOne = n - 1",
                        "n", 2.0);
        continueExecution();

        final Source evalSrc = Source.fromText("main()\n", "debugtest.r").withMimeType("application/x-r");
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
        run.addLast(new Runnable() {
            @Override
            public void run() {
                assertNull(suspendedEvent);
                assertNotNull(executionEvent);
                executionEvent.prepareStepInto();
            }
        });

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

        final Source evalSource = Source.fromText("main()\n", "evaltest.r").withMimeType("application/x-r");
        final Value value = engine.eval(evalSource);
        assertExecutedOK();
        Assert.assertEquals("[1] 2\n", getOut());
        final Number n = value.as(Number.class);
        assertNotNull(n);
        assertEquals("Factorial computed OK", 2, n.intValue());
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
        run.addLast(new Runnable() {
            @Override
            public void run() {
                suspendedEvent.prepareStepOver(size);
            }
        });
    }

    private void stepOut() {
        run.addLast(new Runnable() {
            @Override
            public void run() {
                suspendedEvent.prepareStepOut();
            }
        });
    }

    private void continueExecution() {
        run.addLast(new Runnable() {
            @Override
            public void run() {
                suspendedEvent.prepareContinue();
            }
        });
    }

    private void stepInto(final int size) {
        run.addLast(new Runnable() {
            @Override
            public void run() {
                suspendedEvent.prepareStepInto(size);
            }
        });
    }

    private void assertLocation(final int line, final String code, final Object... expectedFrame) {
        run.addLast(new Runnable() {
            @Override
            public void run() {
                assertNotNull(suspendedEvent);
                final int actualLine = suspendedEvent.getNode().getSourceSection().getLineLocation().getLineNumber();
                Assert.assertEquals(line, actualLine);
                final String actualCode = suspendedEvent.getNode().getSourceSection().getCode();
                Assert.assertEquals(code, actualCode);
                final MaterializedFrame frame = suspendedEvent.getFrame();

                Assert.assertEquals(expectedFrame.length / 2, frame.getFrameDescriptor().getSize());

                for (int i = 0; i < expectedFrame.length; i = i + 2) {
                    final String expectedIdentifier = (String) expectedFrame[i];
                    final FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(expectedIdentifier);
                    Assert.assertNotNull(slot);
                    final Object expectedValue = expectedFrame[i + 1];
                    final Object actualValue = getRValue(frame.getValue(slot));
                    Assert.assertEquals(expectedValue, actualValue);
                }
                run.removeFirst().run();
            }
        });
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
}
