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
package com.oracle.truffle.r.test.library.base;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestSimpleSequences extends TestBase {

    @Test
    public void testSequenceConstruction() {
        assertEval("{ 1:3 }");
        assertEval("{ 1.1:3.1 }");
        assertEval("{ 3:1 }");
        assertEval("{ 3.1:1 }");
        assertEval("{ 1:NA }");
        assertEval("{ NA:1 }");
        assertEval("{ NA:NA }");
        assertEval("{ }");
        assertEval("{ 5L:10L }");
        assertEval("{ 5L:(0L-5L) }");
        assertEval("{ 1:10 }");
        assertEval("{ 1:(0-10) }");
        assertEval("{ 1L:(0-10) }");
        assertEval("{ 1:(0L-10L) }");
        assertEval("{ (0-12):1.5 }");
        assertEval("{ 1.5:(0-12) }");
        assertEval("{ (0-1.5):(0-12) }");
        assertEval("{ 10:1 }");
        assertEval("{ (0-5):(0-9) }");
        assertEval("{ 1.1:5.1 }");

        assertEval("{ (1:3):3 }");
        assertEval("{ 1:(1:3) }");
        assertEval("{ (1:3):(1:3) }");
    }

    @Test
    public void testInteractiveSequences() {
        assertEval("1;2;3");
        assertEval("1;invisible(2);3");
    }
}
