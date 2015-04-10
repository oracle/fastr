/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestrGenBuiltinasinteger extends TestBase {

    @Test
    public void testasinteger1() {
        assertEval("argv <- list(structure(c(4L, 5L, 3L, 2L, 2L, 1L, 6L), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables', 'R Core'), class = 'factor'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c('   33', '   34', '   35', '   36', '   37', '   38', '   18', '   19', '   20', '   21', '   22', '   23', '   36', '   37', '   38', '   39'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(-Inf, -8.5, -2.83333333333333, -1.41666666666667, -0.85, -0.566666666666666, -0.404761904761905, -0.303571428571428, -0.236111111111111, -0.188888888888889));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger4() {
        assertEval("argv <- list(c(0, 1, NA, NA, 1, 1, -1, 1, 3, -2, -2, 7, -1, -1, -1, -1, -1, -1, -1, -1, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger6() {
        assertEval("argv <- list(2e+05);as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger7() {
        assertEval("argv <- list(NULL);as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger8() {
        assertEval("argv <- list(list(7L, 20, 0L, 1));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger9() {
        assertEval("argv <- list('-1');as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger10() {
        assertEval(Ignored.Unknown, "argv <- list(c('1', NA, '0'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger11() {
        assertEval(Ignored.Unknown, "argv <- list(c('3', '14159265358979'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger12() {
        assertEval("argv <- list(TRUE);as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger13() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0), .Dim = c(13L, 1L), .Dimnames = list(c('59', '115', '156', '268', '329', '431', '448', '477', '638', '803', '855', '1040', '1106'), NULL)));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger14() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger15() {
        assertEval("argv <- list(character(0));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger16() {
        assertEval("argv <- list(4999.0000000001);as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger17() {
        assertEval(Ignored.Unknown, Output.ContainsWarning,
                        "argv <- list(structure(c(100, -1e-13, Inf, -Inf, NaN, 3.14159265358979, NA), .Names = c(' 100', '-1e-13', ' Inf', '-Inf', ' NaN', '3.14', '  NA')));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger18() {
        assertEval("argv <- list(structure(c(1L, 2L, 3L, 2L), .Label = c('1', '2', NA), class = 'factor'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger19() {
        assertEval("argv <- list(structure(c(NA, 1L, NA, 2L, 1L, NA, NA, 1L, 4L, 1L, NA, 4L, 1L, 3L, NA, 4L, 2L, 2L, NA, 4L, 4L, 2L, 4L, 4L, 2L, 1L, 4L, 4L, 3L, 1L, 1L, 4L, 1L, 4L, NA, 1L, 4L, 4L, 2L, 2L, 4L, 4L, 3L, 4L, 2L, 2L, 3L, 3L, 4L, 1L, 1L, 1L, 4L, 1L, 4L, 4L, 4L, 4L, NA, 4L, 4L, 4L, NA, 1L, 2L, 3L, 4L, 3L, 4L, 2L, 4L, 4L, 1L, 4L, 1L, 4L, NA, 4L, 2L, 1L, 4L, 1L, 1L, 1L, 4L, 4L, 2L, 4L, 1L, 1L, 1L, 4L, 1L, 1L, 1L, 4L, 3L, 1L, 4L, 3L, 2L, 4L, 3L, 1L, 4L, 2L, 4L, NA, 4L, 4L, 4L, 2L, 1L, 4L, 4L, NA, 2L, 4L, 4L, 1L, 1L, 1L, 1L, 4L, 1L, 2L, 3L, 2L, 1L, 4L, 4L, 4L, 1L, NA, 4L, 2L, 2L, 2L, 4L, 4L, 3L, 3L, 4L, 2L, 4L, 3L, 1L, 1L, 4L, 2L, 4L, 3L, 1L, 4L, 3L, 4L, 4L, 1L, 1L, 4L, 4L, 3L, 1L, 1L, 2L, 1L, 3L, 4L, 2L, 2L, 2L, 4L, 4L, 3L, 2L, 1L, 1L, 4L, 1L, 1L, 2L, NA, 2L, 3L, 3L, 2L, 1L, 1L, 1L, 1L, 4L, 4L, 4L, 4L, 4L, 4L, 2L, 2L, 1L, 4L, 1L, 4L, 3L, 4L, 2L, 3L, 1L, 3L, 1L, 4L, 1L, 4L, 1L, 4L, 3L, 3L, 4L, 4L, 1L, NA, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 3L, 4L, 3L, 4L, 2L, 4L, 4L, 1L, 2L, NA, 4L, 4L, 4L, 4L, 1L, 2L, 1L, 1L, 2L, 1L, 4L, 2L, 3L, 1L, 4L, 4L, 4L, 1L, 2L, 1L, 4L, 2L, 1L, 3L, 1L, 2L, 2L, 1L, 2L, 1L, NA, 3L, 2L, 2L, 4L, 1L, 4L, 4L, 2L, 4L, 4L, 4L, 2L, 1L, 4L, 2L, 4L, 4L, 4L, 4L, 4L, 1L, 3L, 4L, 3L, 4L, 1L, NA, 4L, NA, 1L, 1L, 1L, 4L, 4L, 4L, 4L, 2L, 4L, 3L, 2L, NA, 1L, 4L, 4L, 3L, 4L, 4L, 4L, 2L, 4L, 2L, 1L, 4L, 4L, NA, 4L, 4L, 3L, 3L, 4L, 2L, 2L, 4L, 1L, 4L, 4L, 4L, 3L, 4L, 4L, 4L, 3L, 2L, 1L, 3L, 1L, 4L, 1L, 4L, 2L, NA, 1L, 4L, 4L, 3L, 1L, 4L, 1L, 4L, 1L, 4L, 4L, 1L, 2L, 2L, 1L, 4L, 1L, 1L, 4L, NA, 4L, NA, 4L, 4L, 4L, 1L, 4L, 2L, 1L, 2L, 2L, 2L, 2L, 1L, 1L, 2L, 1L, 4L, 2L, 3L, 3L, 1L, 3L, 1L, 4L, 1L, 3L, 2L, 2L, 4L, 1L, NA, 3L, 4L, 2L, 4L, 4L, 4L, 4L, 4L, 4L, 3L, 4L, 4L, 3L, 2L, 1L, 4L, 4L, 2L, 4L, 2L, 1L, 2L, 1L, 1L, 1L, 1L, 4L, 4L, 1L, 1L, 4L, 1L, 4L, 4L, 4L, 1L, 1L, NA, 3L, 2L, 4L, 4L, 4L, 4L, 2L, 3L, 3L, 2L, NA, 4L, 2L, 4L, 4L, 1L, 1L, 4L, 4L, 1L, 1L, 4L, 1L, 2L, 2L, 2L, 2L, 1L, 4L, 4L, 1L, 2L, 2L, 2L, 3L, 4L, 4L, 3L, 4L, 1L, 1L, 4L, 4L, NA, 4L, 1L, 4L, 4L, 4L, 1L, 4L, 4L, 1L, 2L, 4L, 4L, 4L, 4L, 1L, 2L, 4L, 4L, 2L, 1L, 4L, 2L, 4L, 2L, 2L, 4L, 1L, 3L, 3L, 2L, 4L, 1L, 4L, 4L, 4L, 1L, NA, 4L, 4L, 2L, 4L, 4L, 4L, 4L, 4L, 2L, NA, 4L, 2L, 4L, 3L, 1L, 4L, 4L, 3L, 4L, 2L, 4L, 4L, 1L, 2L, 1L, 4L, 1L, 3L, 3L, 1L, 4L, 4L, 2L, 4L, 4L, 4L, 4L, 3L, 2L, 3L, 3L, 2L, NA, 3L, 4L, 4L, 3L, 3L, 4L, 4L, 4L, 1L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 2L, 4L, 2L, 3L, 4L, 1L, 3L, 1L, NA, 4L, 1L, 2L, 2L, 1L, 4L, 3L, 3L, 4L, 1L, 1L, 3L), .Label = c('(1) Approve STRONGLY', '(2) Approve SOMEWHAT', '(3) Disapprove SOMEWHAT', '(4) Disapprove STRONGLY'), class = 'factor'));as.integer(argv[[1]]);");
    }

    @Test
    public void testasinteger20() {
        assertEval("argv <- list(39);as.integer(argv[[1]]);");
    }
}
