/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_sweep extends TestBase {

    @Test
    public void testsweep1() {
        // FIXME FastR does not honor .Dim attr in output:
        // Expected output:
        // [1,]
        // [2,]
        // [3,]
        // [4,]
        // [5,]
        // FastR output: integer(0)
        assertEval(Ignored.OutputFormatting,
                        "argv <- structure(list(x = structure(integer(0), .Dim = c(5L,     0L)), MARGIN = 2, STATS = integer(0)), .Names = c('x', 'MARGIN',     'STATS'));" + "do.call('sweep', argv)");
    }

    @Test
    public void testSweep() {
        assertEval("{ sweep(array(1:24, dim = 4:2), 1, 5) }");
        assertEval("{ sweep(array(1:24, dim = 4:2), 1, 1:4) }");
        assertEval("{ sweep(array(1:24, dim = 4:2), 1:2, 5) }");

        assertEval("{ A <- matrix(1:15, ncol=5); sweep(A, 2, colSums(A), \"/\") }");

        // FIXME results are the same but GnuR outputs the following warning:
        // Warning message:
        // In matrix(1:50, nrow = 4) :
        // data length [50] is not a sub-multiple or multiple of the number of rows [4]
        assertEval(Output.MissingWarning, "{ A <- matrix(1:50, nrow=4); sweep(A, 1, 5, '-') }");
        // Similar problem like on the line above
        assertEval(Output.MissingWarning, "{ A <- matrix(7:1, nrow=5); sweep(A, 1, -1, '*') }");
    }
}
