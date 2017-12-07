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
public class TestBuiltin_formatinfo extends TestBase {

    @Test
    public void testformatinfo1() {
        assertEval("argv <- list(c(0.099999994, 0.2), 7L, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo2() {
        assertEval("argv <- list(c(0.099999994, 0.2), 6L, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo3() {
        assertEval("argv <- list(c(Inf, -Inf), NULL, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo4() {
        assertEval("argv <- list(FALSE, NULL, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo5() {
        assertEval("argv <- list(3.14159265358979e-10, NULL, 8); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo6() {
        assertEval("argv <- list(1e+08, NULL, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo7() {
        assertEval("argv <- list(1e+222, NULL, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo8() {
        assertEval("argv <- list(31.4159265358979, NULL, 8); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo9() {
        assertEval("argv <- list(712L, NULL, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo10() {
        assertEval("argv <- list(structure(c(2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 3, 0, 6, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), .Names = c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y')), NULL, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo11() {
        assertEval("argv <- list(structure(c(-3.14159265358979e-05, 3.14159265358979e-05, -0.000314159265358979, 0.000314159265358979, -0.00314159265358979, 0.00314159265358979, -0.0314159265358979, 0.0314159265358979, -0.314159265358979, 0.314159265358979, -3.14159265358979, 3.14159265358979, -31.4159265358979, 31.4159265358979, -314.159265358979, 314.159265358979, -3141.59265358979, 3141.59265358979, -31415.9265358979, 31415.9265358979, -314159.265358979, 314159.265358979, -1e-05, 1e-05, -1e-04, 1e-04, -0.001, 0.001, -0.01, 0.01, -0.1, 0.1), .Dim = c(2L, 16L)), NULL, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo12() {
        assertEval("argv <- list(c(NaN, NA), NULL, 0L); .Internal(format.info(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testformatinfo14() {
        assertEval("argv <- structure(list(x = complex(real = Inf, imaginary = Inf)),     .Names = 'x');do.call('format.info', argv)");
    }

    @Test
    public void testformatinfo15() {
        assertEval("argv <- structure(list(x = c(complex(real = NaN, imaginary = NaN),     NA)), .Names = 'x');do.call('format.info', argv)");
    }

    @Test
    public void testformatinfo16() {
        assertEval(".Internal(format.info(NA, 1, 1))");
        assertEval(".Internal(format.info(1, NA, 1))");
        assertEval(".Internal(format.info(1, 2, NA))");
        assertEval(".Internal(format.info(NULL, 1, 1))");
        assertEval(".Internal(format.info(1, NULL, 1))");
        assertEval(".Internal(format.info(1, 2, NULL))");
        assertEval(".Internal(format.info(1+2i, NULL, 1))");
        assertEval(".Internal(format.info(paste, NULL, 1))");
        assertEval(".Internal(format.info(environment(), NULL, 1))");
        assertEval(".Internal(format.info(c(1L,NA,3L) , NULL, 1)); .Internal(format.info(c(1L, 3L) , NULL, 1))");
        assertEval(".Internal(format.info(c(1,NA,3) , NULL, NULL))");
        assertEval(".Internal(format.info(c('a',NA) , NULL, 1))");
    }

}
