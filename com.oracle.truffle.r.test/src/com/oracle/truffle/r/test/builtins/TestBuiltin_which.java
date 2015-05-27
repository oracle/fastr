/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_which extends TestBase {

    @Test
    public void testwhich1() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE)); .Internal(which(argv[[1]]))");
    }

    @Test
    public void testwhich2() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE), .Dim = c(2L, 2L), .Dimnames = list(c('A', 'B'), c('A', 'B')))); .Internal(which(argv[[1]]))");
    }

    @Test
    public void testwhich3() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE)); .Internal(which(argv[[1]]))");
    }

    @Test
    public void testwhich4() {
        assertEval("argv <- list(structure(TRUE, .Names = 'V1')); .Internal(which(argv[[1]]))");
    }

    @Test
    public void testwhich5() {
        assertEval("argv <- list(logical(0)); .Internal(which(argv[[1]]))");
    }

    @Test
    public void testwhich6() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE), .Dim = c(2L, 2L))); .Internal(which(argv[[1]]))");
    }

    @Test
    public void testwhich7() {
        assertEval("argv <- list(c(FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE)); .Internal(which(argv[[1]]))");
    }

    @Test
    public void testwhich8() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE), .Dim = 12L)); .Internal(which(argv[[1]]))");
    }

    @Test
    public void testwhich9() {
        assertEval("argv <- list(structure(FALSE, .Names = 'signature-class.Rd')); .Internal(which(argv[[1]]))");
    }
}
