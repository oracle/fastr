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
public class TestBuiltin_floor extends TestBase {

    @Test
    public void testfloor1() {
        assertEval("argv <- list(c(2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8, 9.9, 11));floor(argv[[1]]);");
    }

    @Test
    public void testfloor2() {
        assertEval("argv <- list(structure(c(12784, 13149, 13514, 13879, 14245, 14610), tzone = 'UTC'));floor(argv[[1]]);");
    }

    @Test
    public void testfloor3() {
        assertEval("argv <- list(c(-0.783587745879035, -0.739712343519063, -0.314304892261569));floor(argv[[1]]);");
    }

    @Test
    public void testfloor4() {
        assertEval("argv <- list(structure(c(1920.5, 1920.5833, 1920.6667, 1920.75, 1920.8333, 1920.9167, 1921, 1921.0833, 1921.1667, 1921.25), .Tsp = c(1920.5, 1921.25, 12), class = 'ts'));floor(argv[[1]]);");
    }

    @Test
    public void testfloor5() {
        assertEval("argv <- list(c(-1.94786705265839, 0.813844117537122));floor(argv[[1]]);");
    }

    @Test
    public void testfloor6() {
        assertEval("argv <- list(structure(c(0.555857947411444, 2.74659181662125, NA, 6.01634386021798, 3.26975214359673, 2.19073396920981, 2.74659181662125, NA, 0.555857947411444, 3.82560999100817, 0.555857947411444, 2.19073396920981, 2.74659181662125, 2.74659181662125, 1.07901827438692, 4.38146783841962, 1.11171579482289, 1e-07, 7.09536203460491, 11.9999901, 4.93732568583106, 5.46048601280654), .Dim = c(22L, 1L)));floor(argv[[1]]);");
    }

    @Test
    public void testfloor7() {
        assertEval("argv <- list(structure(c(3.08577921002324, 0.531033162063639, 1.47434325842442, 5.64214292692797, 6.21994378924106, 2.27200744902353, 11.9999901, 0.424434048635841, 0.549397569660826, 0.973929660925175, 1e-07, 3.54172739357752, 11.9999901, 2.27200744902353, 4.47284349010678, 6.43648940805496, 7.50963843787849, 7.11757579203344, 11.9999901, 3.54172739357752, 6.21994378924106, 5.1224060214714, 6.89175397596987, 6.52603528890926, 11.9999901, 7.11757579203344, 1e-07, 5.64214292692797, 6.00414304408873, 9.63018799510384, 11.9999901, 6.52603528890926, 7.50963843787849, 0.973929660925175, 1.47434325842442, 4.2100341139702, 11.9999901, 9.63018799510384, 6.89175397596987, 6.43648940805496, 0.549397569660826, 0.531033162063639, 11.9999901, 4.2100341139702, 6.00414304408873, 5.1224060214714, 4.47284349010678, 0.424434048635841, 3.08577921002324), .Dim = c(7L, 7L), .Dimnames = list(c('privileges', 'rating', 'complaints', 'learning', 'raises', 'critical', 'advance'), c('advance', 'critical', 'raises', 'learning', 'complaints', 'rating', 'privileges'))));floor(argv[[1]]);");
    }

    @Test
    public void testfloor8() {
        assertEval("argv <- list(structure(c(1976, 1976.0833, 1976.1667, 1976.25, 1976.3333, 1976.4167, 1976.5, 1976.5833, 1976.6667, 1976.75, 1976.8333, 1976.9167, 1977, 1977.0833, 1977.1667, 1977.25, 1977.3333, 1977.4167, 1977.5, 1977.5833, 1977.6667, 1977.75, 1977.8333, 1977.9167, 1978), .Tsp = c(1976, 1978, 12), class = 'ts'));floor(argv[[1]]);");
    }

    @Test
    public void testfloor9() {
        assertEval("argv <- list(logical(0));floor(argv[[1]]);");
    }

    @Test
    public void testFloor() {
        assertEval("{ floor(c(0.2,-3.4,NA,0/0,1/0)) }");
        assertEval("{ typeof(floor(42L)); }");
        assertEval("{ typeof(floor(TRUE)); }");
        assertEval("{ trunc(1+1i); }");
        assertEval("{ trunc(\"aaa\"); }");
    }
}
