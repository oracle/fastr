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

import org.junit.*;

import com.oracle.truffle.r.test.*;

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

        assertEval("{ log10(c(1+1i, -1-1i)) }");
    }
}
