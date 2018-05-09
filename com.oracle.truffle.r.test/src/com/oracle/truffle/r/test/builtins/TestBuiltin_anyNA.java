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

public class TestBuiltin_anyNA extends TestBase {

    @Test
    public void testanyNA1() {
        assertEval("argv <- list(c(1.81566026854212e-304, 0, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0));do.call('anyNA', argv)");
    }

    @Test
    public void testanyNA2() {
        assertEval("anyNA(list(list(c(NA)),c(1)), recursive=TRUE)");
        assertEval("anyNA(list(list(c(NA)),c(1)), recursive=FALSE)");
        assertEval("anyNA(list(list(c(NA)),c(1)), recursive=c(FALSE,TRUE))");
        assertEval("anyNA(list(list(4,5,NA), 3), recursive=TRUE)");
    }

    @Test
    public void testanyNA3() {
        assertEval("anyNA(c(1, 2, 3))");
        assertEval("anyNA(c(1, NA, 3))");
        assertEval("anyNA(c(1, NA, 3), recursive = TRUE)");
        assertEval("anyNA(list(a = c(1, 2, 3), b = 'a'))");
        assertEval("anyNA(list(a = c(1, NA, 3), b = 'a'))");
        assertEval("anyNA(list(a = c('asdf', NA), b = 'a'))");
        assertEval("anyNA(list(a = c(NA, 3), b = 'a'))");
        assertEval("anyNA(list(a = NA, b = 'a'))");
        assertEval("anyNA(list(a = NA))");
        assertEval("anyNA(list(1, NA))");
        assertEval("anyNA(list(a = c('asdf', NA), b = 'a'), recursive = TRUE)");
        assertEval("anyNA(list(a = c(NA, 3), b = 'a'), recursive = TRUE)");
        assertEval("anyNA(list(a = NA, b = 'a'), recursive = TRUE)");
        assertEval("anyNA(list(a = NA), recursive = TRUE)");
        assertEval("anyNA(list(1, NA), recursive = TRUE)");
        assertEval("anyNA(list(a = c(1, 2, 3), b = 'a'), recursive = TRUE)");
        assertEval("anyNA(list(a = c(1, NA, 3), b = 'a'), recursive = TRUE)");
        assertEval("anyNA(list(a = c(1, 2, 3), b = list(NA, 'a')), recursive = TRUE)");
    }
}
