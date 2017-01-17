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
package com.oracle.truffle.r.test.library.utils;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestTrace extends TestBase {
    private static final String FASTR_TRACE = "(if (exists('.fastr.trace')) .fastr.trace else trace)";
    private static final String PLAIN_TRACE = "trace";
    private static final String[] TRACE_VARIANTS = new String[]{PLAIN_TRACE, FASTR_TRACE};

    @Test
    public void testSimple() {
        assertEval(template("f <- function(x) {}; %0(f); f()", TRACE_VARIANTS));
    }

    @Test
    public void testSimpleTrace() {
        assertEval(template("f <- function(x) {}; %0(f, tracer=quote(print(x))); f(100)", TRACE_VARIANTS));
    }

    @Test
    public void testMultiTrace() {
        assertEval(template("f <- function(x) {}; %0(f, tracer=quote(print(x))); g <- function() for (i in 1:10) f(i); g()", TRACE_VARIANTS));
    }

    @Test
    public void testCondTrace() {
        assertEval(template("f <- function(x) {}; %0(f, tracer=quote(if (x == 3 || x == 7) print(x))); g <- function() for (i in 1:10) f(i); g()", TRACE_VARIANTS));
    }
}
