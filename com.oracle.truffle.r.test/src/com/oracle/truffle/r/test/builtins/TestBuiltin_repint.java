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
public class TestBuiltin_repint extends TestBase {

    @Test
    public void testrepint1() {
        assertEval("argv <- list(1, 6); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint2() {
        assertEval("argv <- list(NA_integer_, 1L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint3() {
        assertEval("argv <- list(1L, 4L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint4() {
        assertEval("argv <- list(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), 1); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint5() {
        assertEval("argv <- list(FALSE, 0L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint6() {
        assertEval("argv <- list('', 2L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint7() {
        assertEval("argv <- list(TRUE, 1L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint8() {
        assertEval("argv <- list('   ', 8L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint9() {
        assertEval("argv <- list(c(-4L, 11L, 23L, -3L, -2L, -1L, -6L, 0L, 8L, -13L, 6L, -32L, -8L, NA, 0L), c(10L, 9L, 11L, 17L, 9L, 18L, 8L, 11L, 8L, 15L, 4L, 12L, 12L, 1L, 34L)); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint10() {
        assertEval("argv <- list(c(1L, 1L, 2L, 2L), 6); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint11() {
        assertEval("argv <- list(NA_character_, 3L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint12() {
        assertEval("argv <- list(NA_character_, 5L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint13() {
        assertEval(Ignored.Unknown, "argv <- list(structure(1:4, .Label = c('A', 'B', 'C', 'D'), class = 'factor', .Names = c('a', 'b', 'c', 'd')), 2); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint14() {
        assertEval("argv <- list(2e-08, 9); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint15() {
        assertEval("argv <- list(c('A', 'B'), structure(list(A = 2L, B = 1L), .Names = c('A', 'B'))); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint16() {
        assertEval("argv <- list(0.8625, 2); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint17() {
        assertEval("argv <- list(FALSE, FALSE); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint18() {
        assertEval("argv <- list(c(-1.74520963996789, -1.58308930128988, NA), 100L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint19() {
        assertEval("argv <- list(structure(c(1974, 1974.08333333333, 1974.16666666667, 1974.25, 1974.33333333333, 1974.41666666667, 1974.5, 1974.58333333333, 1974.66666666667, 1974.75, 1974.83333333333, 1974.91666666667, 1975, 1975.08333333333, 1975.16666666667, 1975.25, 1975.33333333333, 1975.41666666667, 1975.5, 1975.58333333333, 1975.66666666667, 1975.75, 1975.83333333333, 1975.91666666667, 1976, 1976.08333333333, 1976.16666666667, 1976.25, 1976.33333333333, 1976.41666666667, 1976.5, 1976.58333333333, 1976.66666666667, 1976.75, 1976.83333333333, 1976.91666666667, 1977, 1977.08333333333, 1977.16666666667, 1977.25, 1977.33333333333, 1977.41666666667, 1977.5, 1977.58333333333, 1977.66666666667, 1977.75, 1977.83333333333, 1977.91666666667, 1978, 1978.08333333333, 1978.16666666667, 1978.25, 1978.33333333333, 1978.41666666667, 1978.5, 1978.58333333333, 1978.66666666667, 1978.75, 1978.83333333333, 1978.91666666667, 1979, 1979.08333333333, 1979.16666666667, 1979.25, 1979.33333333333, 1979.41666666667, 1979.5, 1979.58333333333, 1979.66666666667, 1979.75, 1979.83333333333, 1979.91666666667), .Tsp = c(1974, 1979.91666666667, 12), class = 'ts'), 3L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint20() {
        assertEval("argv <- list(NA, 10L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint21() {
        assertEval("argv <- list(c('C', 'A', 'B'), structure(list(C = 1L, A = 1L, B = 1L), .Names = c('C', 'A', 'B'))); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint22() {
        assertEval("argv <- list(NA_real_, 4L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint23() {
        assertEval("argv <- list(0.26784, 49); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint24() {
        assertEval("argv <- list(3.1e-06, 49); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint25() {
        assertEval("argv <- list(NA, 5L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint26() {
        assertEval("argv <- list(TRUE, 6L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testrepint27() {
        assertEval("argv <- list(structure(c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101), .Tsp = c(1, 101, 1), class = 'ts'), 3L); .Internal(rep.int(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testRepInt() {
        assertEval("{ rep.int(1,3) }");
        assertEval("{ rep.int(1:3,2) }");
        assertEval("{ rep.int(c(1,2),0) }");
        assertEval("{ rep.int(c(1,2),2) }");
        assertEval("{ rep.int(as.raw(14), 4) }");
        assertEval("{ rep.int(1L,3L) }");
        assertEval("{ rep.int(\"a\",3) }");
        assertEval("{ rep.int(c(1,2,3),c(2,8,3)) }");
        assertEval("{ rep.int(seq_len(2), rep.int(8, 2)) }");

        assertEval(Output.IgnoreErrorContext, "{ rep.int(c(1,2,3),c(2,8)) }");

        assertEval(Output.IgnoreErrorContext, "{ rep.int(function() 42, 7) }");
        assertEval("{ rep.int(7, character()) }");
        assertEval("{ rep.int(7, NULL) }");
        assertEval("{ rep.int(7, \"7\") }");
        assertEval(Output.IgnoreErrorContext, "{ rep.int(7, c(7, 42)) }");
        assertEval("{ rep_int(7, function() 42) }");
        assertEval(Output.IgnoreErrorContext, "{ rep.int(7, NA)  }");
    }
}
