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
