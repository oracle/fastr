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
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_lchoose extends TestBase {

    @Test
    public void testlchoose1() {
        assertEval("argv <- list(FALSE, FALSE); .Internal(lchoose(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testlchoose2() {
        assertEval("argv <- list(50L, 0:48); .Internal(lchoose(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testlchoose3() {
        assertEval("argv <- list(0.5, 1:9); .Internal(lchoose(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testlchoose4() {
        assertEval("{ .Internal(lchoose(NA, 1)) }");
        assertEval("{ .Internal(lchoose(1, NA)) }");
        assertEval("{ .Internal(lchoose(NULL, 1)) }");
        assertEval("{ .Internal(lchoose(1, NULL)) }");
        assertEval("{ .Internal(lchoose(logical(0), logical(0))) }");
        // Minor diff in warning msg: extra newline; number formatting
        assertEval(Output.IgnoreWarningMessage, "{ .Internal(lchoose(2, 2.2)) }");
        assertEval(Output.IgnoreWarningMessage, "{ .Internal(lchoose(0:2, 2.2)) }");

        assertEval("{ .Internal(lchoose(c(2.2, 3.3), 2)) }");
        assertEval("{ .Internal(lchoose(c(2.2, 3.3), c(2,3,4))) }");
        assertEval("{ .Internal(lchoose(c(2.2, 3.3, 4.4), c(2,3))) }");
        assertEval(".Internal(lchoose(structure(array(21:24, dim=c(2,2)), dimnames=list(a=c('a1','a2'),b=c('b1','b2'))), 2))");
        assertEval(".Internal(lchoose(47, structure(array(21:24, dim=c(2,2)), dimnames=list(a=c('a1','a2'),b=c('b1','b2')))))");
        assertEval(".Internal(lchoose(structure(47, myattr='hello'), 2))");
    }
}
