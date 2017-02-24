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
public class TestBuiltin_bitwiseNot extends TestBase {

    @Test
    public void testbitwiseNot1() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(bitwiseNot(argv[[1]]))");
    }

    @Test
    public void testBitwiseFunctions() {
        assertEval("{ bitwNot(c(17,24,34,48,51,66,72,99)) }");
        assertEval("{ bitwNot(c(0,100,200,50,70,20)) }");

        assertEval("{ bitwNot(TRUE) }");
    }
}
