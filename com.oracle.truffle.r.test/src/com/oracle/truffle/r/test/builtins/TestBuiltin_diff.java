/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
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
