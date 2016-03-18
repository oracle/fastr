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
public class TestBuiltin_bincode extends TestBase {

    @Test
    public void testbincode1() {
        assertEval("argv <- list(c(-1, -1, -1, -1, -1), c(-1.001, -1, -0.999), TRUE, FALSE); .Internal(bincode(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testbincode2() {
        assertEval("argv <- list(c(8.70599232813489e-06, 7.24187268717448e-10, 7.84878459581784e-14), c(0, 0.001, 0.01, 0.05, 0.1, 1), TRUE, TRUE); .Internal(bincode(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testbincode3() {
        assertEval("argv <- list(c(0.00316901674455053, 0.000313731190323184, 2.12051012154177e-05, 0.000158772845963692), c(0, 0.001, 0.01, 0.05, 0.1, 1), TRUE, TRUE); .Internal(bincode(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testbincode4() {
        assertEval("argv <- list(c(NA, 0.0654707112145736, 0.999999999999999), c(0, 0.001, 0.01, 0.05, 0.1, 1), TRUE, TRUE); .Internal(bincode(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testbincode5() {
        assertEval("argv <- list(structure(c(3.00863041155973, 0.411191886071604, -0.690490615408105, -4.08011169113016, -1.74096111020938, 0.149871643848704, -0.617403399223794, 1.77225991336381, 0.70873696276922, 0.831384833755618, 0.760421822835713, -1.43576852408133, -1.06684579764157, 0.112230570314199, 1.7784773190779, 0.601241755061942, 3.2084694607557, 1.30812378137844, 0.548795030131126, -4.19457515085108, -2.57754314942853, -2.82255910587143, -1.79648698551886, 8.02186933983834, -1.32562596449111, 1.07620193452922, 0.454145574974766, -1.70834615344098, -1.67960999025708, -0.580565722061994, 1.46027792034151, 0.274978829340024), class = 'table', .Dim = c(4L, 4L, 2L), .Dimnames = structure(list(Hair = c('Black', 'Brown', 'Red', 'Blond'), Eye = c('Brown', 'Blue', 'Hazel', 'Green'), Sex = c('Male', 'Female')), .Names = c('Hair', 'Eye', 'Sex'))), c(-Inf, -4, -2, 0, 2, 4, Inf), TRUE, FALSE); .Internal(bincode(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testbincode6() {
        assertEval("argv <- list(c(4L, 8L, 7L, 4L, 8L, 7L, NA, 7L, 6L, 7L, 7L, 8L, 8L, 6L, 7L, 8L, 5L, 4L, 8L, 7L, 5L, 6L, 5L, 6L, 5L, 6L, 7L, 6L, 5L, 7L, 4L, 6L, 6L, 5L, 7L, 5L, 5L, 6L, 6L, 6L, 5L, 7L, 5L, 7L, 5L, 3L, 7L, 6L, 5L, 5L, 6L, 5L, 5L, 6L, 10L, 10L, 6L, 3L, 5L, 8L, 7L, 5L, 6L, 5L, 5L, 5L, 6L, 5L, 6L, 5L, 5L, 6L, 7L, 7L, 6L, 7L, 7L, 8L, 9L, 7L, 8L, 6L, 4L, 7L, 7L, 6L, NA, 8L, 5L, 7L, 6L, 5L, NA, 7L, 6L, 7L, 7L, 9L, 5L, 8L, 6L, 8L, 9L, 6L, 6L, 7L, 8L, 8L, 8L, 7L, 8L, 7L, 6L, 6L, 9L, 7L, 6L, 8L, 5L, 7L, 8L, 8L, 7L, 7L, 7L, 8L, 5L, 6L, 6L, 5L, 7L, 5L, 7L, 7L, 4L, 5L, 8L, 5L, 5L, 6L, 7L, 5L, 9L, 5L, 6L, 7L), c(2, 5.5, 10), TRUE, FALSE); .Internal(bincode(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testBincode() {
        assertEval("{ x <- c(0, 0.01, 0.5, 0.99, 1); b <- c(0, 0, 1, 1); .bincode(x, b, TRUE) }");
        assertEval("{ x <- c(0, 0.01, 0.5, 0.99, 1); b <- c(0, 0, 1, 1); .bincode(x, b, FALSE) }");
        assertEval("{ x <- c(0, 0.01, 0.5, 0.99, 1); b <- c(0, 0, 1, 1); .bincode(x, b, TRUE, TRUE) }");
        assertEval("{ x <- c(0, 0.01, 0.5, 0.99, 1); b <- c(0, 0, 1, 1); .bincode(x, b, FALSE, TRUE) }");
    }
}
