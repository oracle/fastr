/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Tests that challenge the parser.
 */
public class TestSimpleParsing extends TestBase {

    @Test
    public void testIncorrectInput() {
        assertEval("/");
    }

    @Test
    public void testUnaryNotParsing() {
        assertEval("x <- 1; y <- TRUE; x - !y");
        assertEval("x <- 1; y <- TRUE; !x - !y");
        assertEval("x <- 1; y <- TRUE; (!x) - !y");
        assertEval("x <- FALSE; y <- TRUE; !x && !y");
        assertEval("x <- FALSE; y <- TRUE; !x && y");
        assertEval("x <- FALSE; y <- TRUE; (!x) && y");
        assertEval("x <- 1; y <- 2; !x < y");
        assertEval("x <- 1; y <- 2; !(x < y)");
        assertEval("x <- 1; y <- 2; !(x) < y");
    }

    @Test
    public void testEscapeSequences() {
        assertEval(" \"\\a\\b\\f \\v \\t \\r \\n\\' \\\"\\`\\011\\013\\036\" ");
        assertEval(" \"\\a\\b\\f \\v \\t \\r \" ");
        assertEval(" \"\\' \\\"\\`\" ");
        assertEval(" \"\\011\\013\\036\" ");
        assertEval(" \"\\111\\413\\36f \7 \" ");
        assertEval(" '\\a\\b\\f \\v \\t \\r \\n\\' \\\"\\`\\011\\013\\036' ");
    }

    @Test
    public void testSemicolons() {
        assertEval("{;}");
        assertEval("{;;;;;}");
        assertEval("{1;;;;;}");
        assertEval("{invisible(1);;;;;}");
        assertEval("{;;4;;;}");
        assertEval(";");
        assertEval(";1");
        assertEval(";1;;");
    }

    @Test
    public void testNumbers() {
        assertEval("1234L");
        assertEval("1234.0L");
        assertEval("1234.1L > 0");
        assertEval("-1234.1L > 0");
        assertEval("12340000000000L > 0");
        assertEval("12340000000000.0L > 0");
        assertEval("12340000000000.1L > 0");
        assertEval("-12340000000000.1L > 0");
    }

    @Test
    public void testFieldsAndSlots() {
        assertEval("a <- list(a=3, b=9); list(a$a, a$b)");
        assertEval("a <- list(a=3, b=9); list(a$'a', a$\"b\")");
        assertEval("setClass('Foo', representation(x='numeric')); a <- new('Foo'); a@x");
        assertEval("setClass('Foo', representation(x='numeric')); a <- new('Foo'); a@'x'");
    }
}
