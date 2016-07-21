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

public class TestBuiltin_chol extends TestBase {

    @Test
    public void testchol1() {
        assertEval("argv <- structure(list(x = structure(c(1.66666666666667, -1.33333333333333,     1, -0.666666666666667, 0.333333333333333, -1.33333333333333,     2.66666666666667, -2, 1.33333333333333, -0.666666666666667,     1, -2, 3, -2, 1, -0.666666666666667, 1.33333333333333, -2,     2.66666666666667, -1.33333333333333, 0.333333333333333, -0.666666666666667,     1, -1.33333333333333, 1.66666666666667), .Dim = c(5L, 5L))),     .Names = 'x');" +
                        "do.call('chol', argv)");
    }

    @Test
    public void testChol() {
        assertEval("{ chol(1) }");
        assertEval("{ round( chol(10), digits=5) }");
        assertEval("{ m <- matrix(c(5,1,1,3),2) ; round( chol(m), digits=5 ) }");
        assertEval(Ignored.Unknown, Output.IgnoreErrorContext, "{ m <- matrix(c(5,-5,-5,3),2,2) ; chol(m) }");
    }
}
