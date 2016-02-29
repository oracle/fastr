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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_isunsorted extends TestBase {

    @Test
    public void testisunsorted1() {
        assertEval("argv <- list(c(1L, 2L, 4L), FALSE); .Internal(is.unsorted(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisunsorted2() {
        assertEval("argv <- list(c(10.7041467781967, 11.5754756379084, 12.524991240374, 13.5975143137056, 14.4688431734172, 15.4183587758829, 16.4908818492144, 17.7566218541999, 19.1425780866377, 20.5285343190754, 22.0685075746448, 23.9825281292691, 26.4455166737415, 29.7592803351446, 34.4380365011698, 41.4254228764895, 44.7391865378926, 49.4179427039178, 56.4053290792375), FALSE); .Internal(is.unsorted(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisunsorted3() {
        assertEval("argv <- list(c(-19, -12.21, -7.07, -6.14, -4.56, -4.09, -3.8, -2.22, -1.97, -1.95, -1.83, -1.82, -1.77, -1.56, -1.48, -1.42, -1.19, -1.12, -1.09, -1.04, -0.96, -0.89, -0.87, -0.87, -0.78, -0.77, -0.77, -0.74, -0.71, -0.63, -0.61, -0.59, -0.54, -0.51, -0.5, -0.5, -0.44, -0.4, -0.4, -0.37, -0.33, -0.28, -0.21, -0.2, -0.16, -0.16, -0.12, -0.1, -0.05, -0.01, -0.01, 0.04, 0.11, 0.13, 0.14, 0.15, 0.15, 0.25, 0.25, 0.26, 0.34, 0.42, 0.44, 0.46, 0.48, 0.48, 0.49, 0.49, 0.51, 0.57, 0.58, 0.64, 0.66, 0.7, 0.74, 0.8, 0.83, 0.94, 0.94, 1.02, 1.09, 1.12, 1.15, 1.18, 1.19, 1.63, 1.86, 1.92, 2.11, 2.17, 2.21, 2.22, 2.25, 2.64, 2.75, 4.18, 4.6, 5.74, 22.42, 44.32), FALSE); .Internal(is.unsorted(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisunsorted4() {
        assertEval("argv <- list(c('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'), FALSE); .Internal(is.unsorted(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisunsorted5() {
        assertEval("argv <- list(c(8, 6, 9, 4, 3, 7, 1, 5, 2), TRUE); .Internal(is.unsorted(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisunsorted6() {
        assertEval("argv <- list(c(1, 2, 3, 2), FALSE); .Internal(is.unsorted(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisunsorted7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')), FALSE); .Internal(is.unsorted(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisunsorted8() {
        assertEval("argv <- list(c(2L, 1L, 0L, 3L), FALSE); .Internal(is.unsorted(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisunsorted9() {
        assertEval("argv <- list(c(1L, 3L, 2L, 4L), TRUE); .Internal(is.unsorted(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testisunsorted11() {
        assertEval("argv <- structure(list(x = c('A', 'B', 'C', 'D', 'E', 'F', 'G',     'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',     'T', 'U', 'V', 'W', 'X', 'Y', 'Z')), .Names = 'x');"
                        + "do.call('is.unsorted', argv)");
    }

    @Test
    public void testisunsorted12() {
        assertEval("argv <- structure(list(x = c(NA, 1, 2, 3, 2), na.rm = TRUE),     .Names = c('x', 'na.rm'));do.call('is.unsorted', argv)");
    }

    @Test
    public void testisunsorted13() {
        assertEval("argv <- structure(list(x = c(1L, 2L, 3L, 5L, 5L, 6L, 6L, 7L,     7L, 7L, 7L, 7L, 8L, 8L, 9L, 9L, 10L, 12L, 12L, 12L, 12L,     13L, 15L, 20L, 28L)), .Names = 'x');"
                        + "do.call('is.unsorted', argv)");
    }

    @Test
    public void testisunsorted14() {
        assertEval(Ignored.Unknown, "argv <- structure(list(x = structure(list(x = 3:4, y = 1:2),     .Names = c('x', 'y'), row.names = c(NA, -2L), class = 'data.frame')),     .Names = 'x');"
                        + "do.call('is.unsorted', argv)");
    }

    @Test
    public void testisunsorted15() {
        assertEval("argv <- structure(list(x = structure(list(x = c(2L, 1L)), .Names = 'x',     row.names = c(NA, -2L), class = 'data.frame')), .Names = 'x');do.call('is.unsorted', argv)");
    }

    @Test
    public void testIsUnsorted() {
        assertEval("{ is.unsorted(c(1,2,3,4)) }");
        assertEval("{ is.unsorted(c(1,2,6,4)) }");
    }
}
