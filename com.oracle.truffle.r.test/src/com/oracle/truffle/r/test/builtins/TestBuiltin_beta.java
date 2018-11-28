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
public class TestBuiltin_beta extends TestBase {

    @Test
    public void testbeta1() {
        assertEval("argv <- list(FALSE, FALSE); .Internal(beta(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testbeta2() {
        assertEval("argv <- list(logical(0), logical(0)); .Internal(beta(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testbeta4() {
        assertEval("argv <- structure(list(a = 0.01, b = 171), .Names = c('a', 'b'));do.call('beta', argv)");
    }

    @Test
    public void testbeta5() {
        assertEval("argv <- structure(list(a = 1e-200, b = 1e-200), .Names = c('a',     'b'));do.call('beta', argv)");
    }

    @Test
    public void testbetabuiltin() {
        assertEval("{ .Internal(beta(NA, 1)) }");
        assertEval("{ .Internal(beta(1, NA)) }");
        assertEval("{ .Internal(beta(NULL, 1)) }");
        assertEval("{ .Internal(beta(1, NULL)) }");
        assertEval("{ .Internal(beta(FALSE, FALSE)) }");
        assertEval("{ .Internal(beta(logical(0), logical(0))) }");
        assertEval("{ .Internal(beta(2, 2.2)) }");
        assertEval("{ .Internal(beta(0:2, 2.2)) }");
        assertEval("{ .Internal(beta(c(2.2, 3.3), 2)) }");
        assertEval("{ .Internal(beta(c(2.2, 3.3), c(2,3,4))) }");
        assertEval("{ .Internal(beta(c(2.2, 3.3, 4.4), c(2,3))) }");
        assertEval(".Internal(beta(structure(array(21:24, dim=c(2,2)), dimnames=list(a=c('a1','a2'),b=c('b1','b2'))), 2))");
        assertEval(".Internal(beta(47, structure(array(21:24, dim=c(2,2)), dimnames=list(a=c('a1','a2'),b=c('b1','b2')))))");
        assertEval(".Internal(beta(structure(47, myattr='hello'), 2))");
    }
}
