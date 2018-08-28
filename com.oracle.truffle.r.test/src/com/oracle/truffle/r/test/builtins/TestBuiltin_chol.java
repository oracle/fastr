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
        // Error messages differ
        // Expected output: Error in chol.default(m) :
        // the leading minor of order 2 is not positive definite
        // FastR output: Error in chol.default(m) : error code 2 from Lapack routine 'dpotrf'
        assertEval(Output.IgnoreErrorMessage, "{ m <- matrix(c(5,-5,-5,3),2,2) ; chol(m) }");
        assertEval("chol(matrix(c(4,2,2,3), ncol=2), pivot=TRUE)");
    }
}
