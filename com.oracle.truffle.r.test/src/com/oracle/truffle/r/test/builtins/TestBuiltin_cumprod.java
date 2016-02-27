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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_cumprod extends TestBase {

    @Test
    public void testcumprod1() {
        assertEval("argv <- list(structure(c(1, 60, 60, 24, 7), .Names = c('secs', 'mins', 'hours', 'days', 'weeks')));cumprod(argv[[1]]);");
    }

    @Test
    public void testcumprod2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i, 0.2853725+0.3927816i));cumprod(argv[[1]]);");
    }

    @Test
    public void testcumprod3() {
        assertEval("argv <- list(c(1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49));cumprod(argv[[1]]);");
    }

    @Test
    public void testcumprod4() {
        assertEval("argv <- list(structure(0L, .Names = 'l0'));cumprod(argv[[1]]);");
    }

    @Test
    public void testcumprod5() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')));cumprod(argv[[1]]);");
    }

    @Test
    public void testcumprod6() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));cumprod(argv[[1]]);");
    }

    @Test
    public void testcumprod7() {
        assertEval(Ignored.Unknown, "argv <- list(structure(list(), .Names = character(0)));cumprod(argv[[1]]);");
    }

    @Test
    public void testcumprod8() {
        assertEval(Ignored.Unknown, "argv <- list(NULL);cumprod(argv[[1]]);");
    }

    @Test
    public void testcumprod9() {
        assertEval("argv <- list(character(0));cumprod(argv[[1]]);");
    }

    @Test
    public void testcumprod10() {
        assertEval("argv <- list(c(0.982149602642989, 0.91866776738084, 0.859369083800704, 0.921182928974104));cumprod(argv[[1]]);");
    }
}
