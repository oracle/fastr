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
public class TestBuiltin_bitwiseAnd extends TestBase {

    @Test
    public void testbitwiseAnd1() {
        assertEval("argv <- list(structure(c(420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 493L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L), class = 'octmode'), structure(256L, class = 'octmode')); .Internal(bitwiseAnd(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testbitwiseAnd2() {
        assertEval("argv <- list(structure(integer(0), class = 'hexmode'), structure(integer(0), class = 'hexmode')); .Internal(bitwiseAnd(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testBitwiseFunctions() {
        assertEval("{ bitwAnd(c(10,11,12,13,14,15), c(1,1,1,1,1,1)) }");
        assertEval("{ bitwAnd(c(25,57,66), c(10,20,30,40,50,60)) }");
        assertEval("{ bitwAnd(c(10L,20L,30L,40L), c(3,5,7)) }");
        assertEval("{ bitwAnd(c(10.5,11.6,17.8), c(5L,7L,8L)) }");

        assertEval(Output.IgnoreErrorContext, Output.IgnoreErrorMessage, "{ bitwAnd(NULL, NULL) }");
        assertEval(Output.IgnoreErrorContext, Output.IgnoreErrorMessage, "{ bitwAnd(c(), c(1,2,3)) }");
        // Error message mismatch
        assertEval(Output.IgnoreErrorMessage, "{ bitwAnd(c(1,2,3,4), c(TRUE)) }");
    }
}
