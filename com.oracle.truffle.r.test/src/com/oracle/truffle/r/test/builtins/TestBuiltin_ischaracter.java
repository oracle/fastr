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
public class TestBuiltin_ischaracter extends TestBase {

    @Test
    public void testischaracter1() {
        assertEval("argv <- list('pch');is.character(argv[[1]]);");
    }

    @Test
    public void testischaracter2() {
        assertEval("argv <- list(structure(list(mai = c(0.51, 0.41, 0.41, 0.21), mar = c(5.1, 4.1, 4.1, 2.1), cex = 1, yaxs = 'r'), .Names = c('mai', 'mar', 'cex', 'yaxs')));is.character(argv[[1]]);");
    }

    @Test
    public void testischaracter3() {
        assertEval("argv <- list(structure(list(usr = c(-0.04, 1.04, -0.04, 1.04), mgp = c(3, 1, 0)), .Names = c('usr', 'mgp')));is.character(argv[[1]]);");
    }

    @Test
    public void testischaracter4() {
        assertEval("argv <- list(c(-1, 1, -1, 1));is.character(argv[[1]]);");
    }

    @Test
    public void testischaracter5() {
        assertEval("argv <- list(structure(list(usr = c(-4.82721591443179, -1.44459960821772, -4.82721591443179, -1.44459960821772)), .Names = 'usr'));is.character(argv[[1]]);");
    }

    @Test
    public void testischaracter6() {
        assertEval("argv <- list(structure(list(`character(0)` = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'character(0)', row.names = character(0), class = 'data.frame'));is.character(argv[[1]]);");
    }

    @Test
    public void testischaracter7() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.character(argv[[1]]);");
    }

    @Test
    public void testischaracter8() {
        assertEval("argv <- list(structure(c(238L, 154L, 73L), .Dim = c(3L, 1L), .Dimnames = list(c('red', 'green', 'blue'), NULL)));is.character(argv[[1]]);");
    }

    @Test
    public void testischaracter10() {
        assertEval("argv <- list('\\'class\\' is a reserved slot name and cannot be redefined');do.call('is.character', argv)");
    }
}
