/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.runtime.context.RContext.ContextKind;
import com.oracle.truffle.r.test.generate.FastRContext;
import com.oracle.truffle.r.test.generate.FastRSession;

/**
 * Tests various aspects of the Java embedding interface.
 */
public class JavaEmbeddingTest {
    private FastRContext context;
    protected final ByteArrayOutputStream out = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Before
    public void before() {
        context = FastRSession.create().createContext(ContextKind.SHARE_PARENT_RW, true);
    }

    @After
    public void dispose() {
        context.close();
    }

    @Test
    public void testToString() {
        assertEquals("[1] 1", context.eval("R", "1").toString());
        assertEquals("[1] TRUE", context.eval("R", "TRUE").toString());
        assertEquals("[1] NA", context.eval("R", "NA").toString());
        // @formatter:off
        String dataFrameExpected =
                "  x y\n" +
                "1 1 a\n" +
                "2 2 b\n" +
                "3 3 c";
        // @formatter:on
        assertEquals(dataFrameExpected, context.eval("R", "data.frame(x = 1:3, y = c('a', 'b', 'c'))").toString());
    }

    @Test
    public void testConvertingPrimitivePolyglotValuesFromR() {
        assertEquals(1, context.eval("R", "1").asInt());
        assertEquals(1.0, context.eval("R", "1").asDouble(), 0.0);
        assertTrue(context.eval("R", "TRUE").asBoolean());
    }

    @Test
    public void testAccessingVectorElementsOfPolyglotValuesFromR() {
        assertEquals(4, context.eval("R", "c(1L, 2L, 4L)").getArrayElement(2).asInt());
        assertEquals(3, context.eval("R", "c(1L, 2L, 4L)").getArraySize());
        assertEquals("John", context.eval("R", "list(name = 'John')").getMember("name").asString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNotWriteable() {
        context.eval("R", "c(1L, 2L, 4L)").setArrayElement(0, 123);
        context.eval("R", "list(1L, 2L, 4L)").setArrayElement(0, 123);
        context.eval("R", "pairlist(1L, 2L, 4L)").setArrayElement(0, 123);
        context.eval("R", "expression(a=1L, b=2L, c=4L)").setArrayElement(0, 123);
    }

    @Test
    public void testAccessingNAAsPolyglotValue() {
        assertTrue(context.eval("R", "NA").getArrayElement(0).isNull());
        assertTrue(context.eval("R", "c(1L, NA)").getArrayElement(1).isNull());
        assertFalse(context.eval("R", "c(1L, NA)").getArrayElement(0).isNull());
        Value isName = context.eval("R", "is.na");
        assertTrue(isName.execute(context.eval("R", "NA")).asBoolean());
    }
}
