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
public class TestBuiltin_log10 extends TestBase {

    @Test
    public void testlog101() {
        assertEval("argv <- list(c(0.047, 0.013, 0.002, 1e-04, 2.3e-05, 4.5e-06));log10(argv[[1]]);");
    }

    @Test
    public void testlog102() {
        assertEval("argv <- list(1.529e+302);log10(argv[[1]]);");
    }

    @Test
    public void testlog103() {
        assertEval("argv <- list(structure(7.94649180820227e-05, .Names = 'value'));log10(argv[[1]]);");
    }

    @Test
    public void testlog104() {
        assertEval("argv <- list(c(0.0654707112145738, 0.999999999999999));log10(argv[[1]]);");
    }

    @Test
    public void testlog105() {
        assertEval("argv <- list(structure(c(160.1, 129.7, 84.8, 120.1, 160.1, 124.9, 84.8, 116.9, 169.7, 140.9, 89.7, 123.3, 187.3, 144.1, 92.9, 120.1, 176.1, 147.3, 89.7, 123.3, 185.7, 155.3, 99.3, 131.3, 200.1, 161.7, 102.5, 136.1, 204.9, 176.1, 112.1, 140.9, 227.3, 195.3, 115.3, 142.5, 244.9, 214.5, 118.5, 153.7, 244.9, 216.1, 188.9, 142.5, 301, 196.9, 136.1, 267.3, 317, 230.5, 152.1, 336.2, 371.4, 240.1, 158.5, 355.4, 449.9, 286.6, 179.3, 403.4, 491.5, 321.8, 177.7, 409.8, 593.9, 329.8, 176.1, 483.5, 584.3, 395.4, 187.3, 485.1, 669.2, 421, 216.1, 509.1, 827.7, 467.5, 209.7, 542.7, 840.5, 414.6, 217.7, 670.8, 848.5, 437, 209.7, 701.2, 925.3, 443.4, 214.5, 683.6, 917.3, 515.5, 224.1, 694.8, 989.4, 477.1, 233.7, 730, 1087, 534.7, 281.8, 787.6, 1163.9, 613.1, 347.4, 782.8), .Tsp = c(1960, 1986.75, 4), class = 'ts'));log10(argv[[1]]);");
    }

    @Test
    public void testlog106() {
        assertEval("argv <- list(c(10, 100, 1000, 10000, 1e+05));log10(argv[[1]]);");
    }

    @Test
    public void testlog107() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(numeric(0), .Dim = c(20L, 0L), .Dimnames = list(c('ant', 'bee', 'cat', 'cpl', 'chi', 'cow', 'duc', 'eag', 'ele', 'fly', 'fro', 'her', 'lio', 'liz', 'lob', 'man', 'rab', 'sal', 'spi', 'wha'), NULL)));log10(argv[[1]]);");
    }

    @Test
    public void testLog10() {
        assertEval("{ log10(1) } ");
        assertEval("{ log10(0) }");
        assertEval("{ log10(c(0,1)) }");

        assertEval("{ log10(10) } ");
        assertEval("{ log10(100) } ");
        assertEval("{ as.integer(log10(200)*100000) } ");

        assertEval(Ignored.Unknown, "{ m <- matrix(1:4, nrow=2) ; round( log10(m), digits=5 )  }");
        assertEval(Ignored.Unknown, "{ x <- c(a=1, b=10) ; round( c(log(x), log10(x), log2(x)), digits=5 ) }");
    }
}
