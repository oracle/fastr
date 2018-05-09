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
public class TestBuiltin_isinteger extends TestBase {

    @Test
    public void testisinteger1() {
        assertEval("argv <- list(2.74035772634541);is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger2() {
        assertEval("argv <- list(c(NA, 9, 3, 3));is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger3() {
        assertEval("argv <- list(c(NA, 0L));is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger4() {
        assertEval("argv <- list(c(NA, -4.19095158576965e-09));is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger5() {
        assertEval("argv <- list(structure(list(`character(0)` = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'character(0)', row.names = character(0), class = 'data.frame'));is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger6() {
        assertEval("argv <- list(c(-0.6, -0.6, -0.6, -0.6, -0.6, -0.6, -0.6, -0.6, -0.3, -0.3, -0.3, -0.3, -0.3, -0.3, -0.3, -0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6));is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger7() {
        assertEval("argv <- list(c(NA, 1L));is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger8() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger9() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1), .Dim = 1:3));is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger11() {
        assertEval("argv <- list(c(1L, 0L, NA, 1L));do.call('is.integer', argv)");
    }

    @Test
    public void testIsInteger() {
        assertEval("{ is.integer(seq(1,2)) }");
        assertEval("{ is.integer(seq(1L,2L)) }");
    }
}
