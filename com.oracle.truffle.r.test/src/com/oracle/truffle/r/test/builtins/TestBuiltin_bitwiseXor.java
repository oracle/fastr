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
public class TestBuiltin_bitwiseXor extends TestBase {

    @Test
    public void testbitwiseXor1() {
        assertEval("argv <- list(-1L, 1L); .Internal(bitwiseXor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testBitwiseFunctions() {
        assertEval("{ bitwXor(c(10,11,12,13,14,15), c(1,1,1,1,1,1)) }");
        assertEval("{ bitwXor(c(25,57,66), c(10,20,30,40,50,60)) }");
        assertEval("{ bitwXor(20,30) }");

        assertEval(Output.IgnoreErrorMessage, "{ bitwXor(c(\"r\"), c(16,17)) }");
    }
}
