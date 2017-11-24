/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_findInterval extends TestBase {

    @Test
    public void testfindInterval1() {
        assertEval("argv <- list(c(1, 2, 3, 4, 5, 6, 7, 8, 9), c(3, 3.25, 3.5, 3.75, 4, 4.25, 4.5, 4.75, 5, 5.25, 5.5, 5.75, 6), FALSE, FALSE); .Internal(findInterval(argv[[1]], argv[[2]], argv[[3]], argv[[4]], T))");
        assertEval("argv <- list(c(1, 2, 3, 4, 5, 6, 7, 8, 9), c(3, 3.25, 3.5, 3.75, 4, 4.25, 4.5, 4.75, 5, 5.25, 5.5, 5.75, 6), FALSE, FALSE); .Internal(findInterval(argv[[1]], argv[[2]], argv[[3]], argv[[4]], F))");
    }

    @Test
    public void testfindInterval2() {
        assertEval("argv <- list(NA_real_, NA_real_, FALSE, FALSE); .Internal(findInterval(argv[[1]], argv[[2]], argv[[3]], argv[[4]], T))");
        assertEval("argv <- list(NA_real_, NA_real_, FALSE, FALSE); .Internal(findInterval(argv[[1]], argv[[2]], argv[[3]], argv[[4]], F))");
    }

    @Test
    public void testfindInterval3() {
        assertEval("argv <- list(numeric(0), numeric(0), FALSE, FALSE); .Internal(findInterval(argv[[1]], argv[[2]], argv[[3]], argv[[4]], T))");
        assertEval("argv <- list(numeric(0), numeric(0), FALSE, FALSE); .Internal(findInterval(argv[[1]], argv[[2]], argv[[3]], argv[[4]], F))");
    }

    @Test
    public void testfindInterval4() {
        assertEval("argv <- list(c(5, 10, 15), c(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18), FALSE, FALSE); .Internal(findInterval(argv[[1]], argv[[2]], argv[[3]], argv[[4]], T))");
        assertEval("argv <- list(c(5, 10, 15), c(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18), FALSE, FALSE); .Internal(findInterval(argv[[1]], argv[[2]], argv[[3]], argv[[4]], F))");
    }
}
