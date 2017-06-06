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
public class TestBuiltin_sqrt extends TestBase {

    @Test
    public void testsqrt1() {
        assertEval("argv <- list(12.8025995273675);sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt2() {
        assertEval("argv <- list(numeric(0));sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt3() {
        assertEval("argv <- list(-17+0i);sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt4() {
        assertEval("argv <- list(1e+07);sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt5() {
        assertEval("argv <- list(1);sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt6() {
        assertEval("argv <- list(structure(c(0.0101832147522745, 0.0107298799092166, 0.0605795647466432, 7.03601392438852e-05), .Names = c('ar1', 'ar2', 'intercept', 'trend')));sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt7() {
        // FIXME GnuR outputs
        // attr(,"class")attr(,"package")
        // while FastR outputs just
        // attr(,"package")
        assertEval(Ignored.OutputFormatting, "argv <- list(structure(1:10, id = 'test 1', class = structure('withId', package = '.GlobalEnv')));sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt8() {
        assertEval("argv <- list(structure(c(660, 543, 711, 500, 410, 309, 546, 351, 269, 203, 370, 193, 181, 117, 243, 136, 117, 87, 154, 84), .Dim = 4:5, .Dimnames = list(c('Rural Male', 'Rural Female', 'Urban Male', 'Urban Female'), c('70-74', '65-69', '60-64', '55-59', '50-54'))));sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt9() {
        assertEval("argv <- list(c(6L, 5L, 4L, 3L, 2L, 1L, 0L, NA, NA, NA, NA));sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt10() {
        // FIXME missing warning about NaNs being produced
        assertEval(Output.MissingWarning, "argv <- list(c(6L, 5L, 4L, 3L, 2L, 1L, 0L, -1L, -2L, -3L, -4L));sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt11() {
        assertEval("argv <- list(0+1i);sqrt(argv[[1]]);");
    }

    @Test
    public void testsqrt12() {
        assertEval("argv <- list(c(TRUE, FALSE));sqrt(argv[[1]]);");
    }

    @Test
    public void testSqrt() {
        assertEval("{ sqrt(9) }");
        assertEval("{ sqrt(9L) }");
        assertEval("{ sqrt(NA) }");
        assertEval("{ sqrt(c(1,4,9,16)) }");
        assertEval("{ sqrt(c(1,4,NA,16)) }");
        assertEval("{ sqrt(c(a=9,b=81)) }");
        assertEval("{ sqrt(1:5) }");

        assertEval(Output.MissingWarning, "{ sqrt(-1L) }"); // FIXME warning
        assertEval(Output.MissingWarning, "{ sqrt(-1) }"); // FIXME warning
    }
}
