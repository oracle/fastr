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
public class TestBuiltin_bitwiseShiftR extends TestBase {

    @Test
    public void testbitwiseShiftR1() {
        assertEval("argv <- list(-1, 1:31); .Internal(bitwiseShiftR(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testBitwiseFunctions() {
        assertEval("{ bitwShiftR(c(10,11,12,13,14,15), c(1,1,1,1,1,1)) }");
        assertEval("{ bitwShiftR(c(100,200,300), 1) }");
        assertEval("{ bitwShiftR(c(25,57,66), c(10,20,30,40,50,60)) }");

        assertEval("{ bitwShiftR(c(3,2,4), c(3+3i)) }");

        // No warning message printed for NAs produced by coercion
        assertEval(Ignored.Unknown, "{ bitwShiftR(c(1,2,3,4), c(\"Hello\")) }");
    }
}
