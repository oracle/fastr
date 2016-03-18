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
public class TestBuiltin_doTrace extends TestBase {

    @Test
    public void testdoTrace1() {
        assertEval("argv <- list(c(1, 1, 2));.doTrace(argv[[1]]);");
    }

    @Test
    public void testdoTrace3() {
        assertEval("argv <- structure(list(expr = expression(quote(x <- c(1, x)))),     .Names = 'expr');do.call('.doTrace', argv)");
    }
}
