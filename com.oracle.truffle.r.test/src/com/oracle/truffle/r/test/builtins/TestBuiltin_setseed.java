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
public class TestBuiltin_setseed extends TestBase {

    // Note: all the unimplemented tests fail on unimplemented RNG of given kind (2nd argument)

    @Test
    public void testsetseed1() {
        assertEval(Ignored.Unimplemented, "argv <- list(1000, 0L, NULL); .Internal(set.seed(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsetseed2() {
        assertEval(Ignored.Unimplemented, "argv <- list(77, 2L, NULL); .Internal(set.seed(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsetseed3() {
        assertEval(Ignored.Unimplemented, "argv <- list(123, 6L, NULL); .Internal(set.seed(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsetseed4() {
        assertEval(Ignored.Unimplemented, "argv <- list(77, 4L, NULL); .Internal(set.seed(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsetseed5() {
        assertEval("argv <- list(1000, 1L, NULL); .Internal(set.seed(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsetseed6() {
        assertEval("argv <- list(0, NULL, NULL); .Internal(set.seed(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsetseed7() {
        assertEval(Ignored.Unimplemented, "argv <- list(123, 7L, NULL); .Internal(set.seed(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testsetseed8() {
        assertEval("argv <- list(NULL, NULL, NULL); .Internal(set.seed(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testArgCasts() {
        assertEval(Output.IgnoreErrorMessage, "set.seed('hello world')");
        assertEval("set.seed('1234'); runif(1)");
        assertEval("set.seed(FALSE)");
    }
}
