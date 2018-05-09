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
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_atan2 extends TestBase {

    @Test
    public void testatan21() {
        assertEval("argv <- list(structure(0.142857142857143, .Names = 'Var2'), structure(1.75510204081633, .Names = 'Var1')); .Internal(atan2(argv[[1]], argv[[2]]))");
        assertEval("argv <- list(structure(0.142857142857143, .Names = 'Var2'), 1.75510204081633); .Internal(atan2(argv[[1]], argv[[2]]))");
        assertEval(".Internal(atan2(structure(c(1,2), .Names = c('a','b')), 1));");
        assertEval(".Internal(atan2(structure(1:2, .Names = c('a','b')), 1));");
        assertEval(".Internal(atan2(structure(1:6, dim = c(2,3)), 1));");
        assertEval("x <- 1:4; class(x) <- 'asdfasdf'; attr(x, 'f') <- 'fff'; names(x) <- c('a','b','c','d'); y <- 1:2; class(y) <- 'fds'; attr(y, 'a') <- 'Asdf'; names(y) <- c('v','y'); .Internal(atan2(x,1)); .Internal(atan2(x,x)); .Internal(atan2(x,y)); .Internal(atan2(y,x)); .Internal(atan2(y,1))");
    }

    @Test
    public void testatan22() {
        assertEval("argv <- list(structure(-0.224489795918367, .Names = 'Var2'), structure(-0.816326530612245, .Names = 'Var1')); .Internal(atan2(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testatan23() {
        assertEval("argv <- list(c(-1.95681154249341, -2.88854075894443, -2.84850921846233, -2.14635417317387, -1.72790445779804, -0.92649412488672, -0.261537463816701, 0.948205247045638, 1.0990096500205, 2.09024037060933, 2.90928417418961, 4.00425294069879, 1.70515935701163), c(-3.2406391957027, -2.61163262017643, -0.21977838696678, 1.24931893031091, 1.6032898858835, 2.16902716372255, 2.15792786802985, 2.10075226013806, 2.04989923648162, 1.49269068253165, 0.515893014329757, -2.61745072267338, -4.64929811590859)); .Internal(atan2(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testatan24() {
        // FIXME atan2 not implemented for complex values
        assertEval(Ignored.Unimplemented, "argv <- list(0+1i, 0+0i); .Internal(atan2(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testatan25() {
        assertEval("argv <- list(2.43782895752771e-05, 0.999996523206508); .Internal(atan2(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testatan26() {
        assertEval("argv <- list(logical(0), logical(0)); .Internal(atan2(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testTrigExp() {
        assertEval("{ atan2(0.4, 0.8) }");
        assertEval("{ atan2(c(0.3,0.6,0.9), 0.4) }");
        assertEval("{ atan2(0.4, c(0.3,0.6,0.9)) }");
        assertEval("{ atan2(c(0.3,0.6,0.9), c(0.4, 0.3)) }");
        assertEval("{ atan2() }");
        assertEval("{ atan2(0.7) }");

        assertEval("{ atan2(NULL, 1) }");
        assertEval("{ atan2(2, new.env()) }");
        assertEval("{ atan2(2, as.symbol('45')) }");
    }
}
