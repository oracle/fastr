/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltinisinteger extends TestBase {

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
        assertEval("argv <- list(structure(list(`character(0)` = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'character(0)\', row.names = character(0), class = \'data.frame\'));is.integer(argv[[1]]);");
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
        assertEval("argv <- list(structure(3.14159265358979, class = structure(\'3.14159265358979\', class = \'testit\')));is.integer(argv[[1]]);");
    }

    @Test
    public void testisinteger9() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1), .Dim = 1:3));is.integer(argv[[1]]);");
    }
}