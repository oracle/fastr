/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_formatC extends TestBase {

    @Test
    public void testformatC1() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(3.14159265358979, 3.1415926535898, 1), 'double', 10, 4L, 'g', '', c(12L, 12L, 12L)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC2() {
        assertEval(Ignored.Unknown, "argv <- list(1, 'double', 8, 5, 'g', '-', 13); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC3() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(1.5, 13.3414265412268, 1e-15, 8, 1, 500, 28), .Dim = c(7L, 1L), .Dimnames = list(c('m.ship.expon.', 'objective', 'tolerance', 'iterations', 'converged', 'maxit', 'n'), ' ')), 'double', 8L, 7L, 'g', '', c(15L, 15L, 15L, 15L, 15L, 15L, 15L)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC4() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(1000, 1e+07, 1), 'double', 5, 4L, 'g', '', c(12L, 12L, 12L)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC5() {
        assertEval("argv <- list(c(-3, -2, -1, 0, 1, 2, 3), 'double', 1L, 4L, 'g', '', c(12L, 12L, 12L, 12L, 12L, 12L, 12L)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC6() {
        assertEval(Ignored.Unknown, "argv <- list(3L, 'integer', 3, 2L, 'd', '0', 10L); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC7() {
        assertEval("argv <- list(c(0, 25, 50, 75, 100), 'double', 1, 7L, 'fg', '', c(16L, 15L, 15L, 15L, 15L)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC8() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(48.4333681840033, .Names = 'value'), 'double', 5L, 4L, 'g', '', 12L); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC9() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(0.0599, 0.00599, 0.000599, 5.99e-05, 5.99e-06, 5.99e-07), 'double', 3, -2, 'fg', '#', c(10, 11, 12, 13, 14, 15)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC10() {
        assertEval("argv <- list(c(20, 30, 40, 50, 60, 70, 80, 90, 100), 'double', 1, 7L, 'fg', '', c(15L, 15L, 15L, 15L, 15L, 15L, 15L, 15L, 15L)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC11() {
        assertEval("argv <- list(c(0, 25, 50, 75, 100), 'double', 1, 6L, 'fg', '', c(14L, 13L, 13L, 13L, 13L)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC12() {
        assertEval(Ignored.Unknown, "argv <- list(5L, 'integer', 2, 2L, 'd', '', 10L); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC13() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(3.14159265358979e-05, 0.000314159265358979, 0.00314159265358979, 0.0314159265358979, 0.314159265358979, 3.14159265358979, 31.4159265358979, 314.159265358979, 3141.59265358979, 31415.9265358979), 'double', 5, 4, 'fg', '', c(15, 14, 13, 12, 11, 10, 9, 9, 9, 9)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testformatC14() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(1962.25, 1962.5, 1962.75, 1963, 1963.25, 1963.5, 1963.75, 1964, 1964.25, 1964.5, 1964.75, 1965, 1965.25, 1965.5, 1965.75, 1966, 1966.25, 1966.5, 1966.75, 1967, 1967.25, 1967.5, 1967.75, 1968, 1968.25, 1968.5, 1968.75, 1969, 1969.25, 1969.5, 1969.75, 1970, 1970.25, 1970.5, 1970.75, 1971, 1971.25, 1971.5, 1971.75), .Tsp = c(1962.25, 1971.75, 4), class = 'ts'), 'double', 1, 4L, 'g', '', c(12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L)); .Internal(formatC(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }
}
