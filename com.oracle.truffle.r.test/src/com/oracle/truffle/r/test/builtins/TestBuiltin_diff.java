/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_diff extends TestBase {

    @Test
    public void testdiff1() {
        assertEval("argv <- structure(list(x = c(0.467590032349108, 0.560407538764412)),     .Names = 'x');do.call('diff', argv)");
    }

    @Test
    public void testDiff() {
        assertEval("{ diff(1:10, 2) }");
        assertEval("{ diff(1:10, 2, 2) }");
        assertEval("{ x <- cumsum(cumsum(1:10)) ; diff(x, lag = 2) }");
        assertEval("{ x <- cumsum(cumsum(1:10)) ; diff(x, differences = 2) }");
    }
}
