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
public class TestBuiltin_log2 extends TestBase {

    @Test
    public void testlog21() {
        assertEval("argv <- list(48L);log2(argv[[1]]);");
    }

    @Test
    public void testlog22() {
        assertEval("argv <- list(FALSE);log2(argv[[1]]);");
    }

    @Test
    public void testlog23() {
        assertEval("argv <- list(2.2250738585072e-308);log2(argv[[1]]);");
    }

    @Test
    public void testlog25() {
        assertEval("argv <- list(2.2250738585072e-308);do.call('log2', argv)");
    }

    @Test
    public void testLog2() {
        assertEval("{ log2(1) } ");
        assertEval("{ log2(0) }");
        assertEval("{ log2(c(0,1)) }");

        assertEval("{ log2(2) } ");
        assertEval("{ log2(4) } ");
        assertEval("{ as.integer(log2(6)*1000000) } ");

        assertEval("{ log2(c(1+1i, -1-1i)) }");

        assertEval("{ log2(NaN) }");
        assertEval("{ log2(NA) }");
    }
}
