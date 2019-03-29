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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.generate.FastRSession;

public class TruffleRLanguageTest extends TestBase {

    private org.graalvm.polyglot.Context context;

    @Before
    public void before() {
        context = FastRSession.getContextBuilder("R").build();
    }

    @After
    public void dispose() {
        context.close();
    }

    @Test
    public void testToString() {
        context.getEngine().getInstruments().get(ToStringTesterInstrument.ID).lookup(ToStringTesterInstrument.Initialize.class);
        context.eval("R", "1+1");   // to trigger the instrument
        assertEquals("42", ToStringTesterInstrument.intAsString);
        assertEquals("42", ToStringTesterInstrument.byteAsString);
        assertEquals("42.5", ToStringTesterInstrument.doubleAsString);
        assertEquals("Hello", ToStringTesterInstrument.stringAsString);
        assertEquals("true", ToStringTesterInstrument.booleanAsString);
    }

}
