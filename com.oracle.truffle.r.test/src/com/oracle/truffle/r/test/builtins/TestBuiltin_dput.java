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
public class TestBuiltin_dput extends TestBase {

    @Test
    public void testdput1() {
        assertEval("argv <- list(logical(0), structure(1L, class = c('terminal', 'connection')), 69); .Internal(dput(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdput2() {
        assertEval(Ignored.Unimplemented, "argv <- list(structure(1, .Dim = 1L), structure(1L, class = c('terminal', 'connection')), 95); .Internal(dput(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdput3() {
        assertEval("argv <- list(character(0), structure(1L, class = c('terminal', 'connection')), 69); .Internal(dput(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdput4() {
        assertEval(Ignored.Unimplemented,
                        "argv <- list(structure(numeric(0), .Dim = c(0L, 0L)), structure(1L, class = c('terminal', 'connection')), 69); .Internal(dput(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdput5() {
        assertEval("argv <- list(NULL, structure(1L, class = c('terminal', 'connection')), 69); .Internal(dput(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdput6() {
        assertEval(Ignored.Unimplemented,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')), structure(1L, class = c('terminal', 'connection')), 69); .Internal(dput(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdput7() {
        assertEval("argv <- list(FALSE, structure(1L, class = c('terminal', 'connection')), 69); .Internal(dput(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testdput8() {
        assertEval("argv <- list(c(0.00508571428571428, 0.876285714285715), structure(1L, class = c('terminal', 'connection')), 69); .Internal(dput(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
