/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_intToUtf8 extends TestBase {

    @Test
    public void testintToUtf81() {
        assertEval("argv <- list(NULL, FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testintToUtf82() {
        assertEval("argv <- list(list(), FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testintToUtf83() {
        assertEval("argv <- list(FALSE, FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testintToUtf85() {
        assertEval("argv <- structure(list(x = NA_integer_, multiple = TRUE), .Names = c('x',     'multiple'));do.call('intToUtf8', argv)");
    }

    @Test
    public void testintToUtf86() {
        assertEval("argv <- structure(list(x = NA_integer_), .Names = 'x');do.call('intToUtf8', argv)");
    }

    @Test
    public void testintToUtf8() {
        assertEval("intToUtf8(0)");
        assertEval(Output.IgnoreErrorMessage, "intToUtf8(-100)");
        assertEval("intToUtf8(1)");
        assertEval("intToUtf8(c(100,101,0,102))");
        assertEval("intToUtf8(c(100,101,0,102), TRUE)");
        assertEval("nchar(intToUtf8(c(100,101,0,102)))");
        assertEval("nchar(intToUtf8(c(100,101,0,102), TRUE))");
        assertEval("intToUtf8(32)");
        assertEval("intToUtf8(55)");
        assertEval("intToUtf8(55.5)");
        assertEval("intToUtf8(200L)");
        // it's not clear why GNUR does not print these characters
        assertEval(Ignored.ReferenceError, "intToUtf8(2000)");
        assertEval("intToUtf8(65535)");
        assertEval("intToUtf8(65536)");
        assertEval("intToUtf8(200000)");
        assertEval("intToUtf8(1:100)");
        assertEval("intToUtf8(1:100, FALSE)");
        assertEval("intToUtf8(1:100, TRUE)");
    }
}
