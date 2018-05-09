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

    @Test
    public void testWhich() {
        assertEval("{ which(c(TRUE, FALSE, NA, TRUE)) }");
        assertEval("{ which(logical()) }");
        assertEval("{ which(TRUE) }");
        assertEval("{ which(NA) }");
        assertEval("{ x<-c(1,2); names(x)<-c(11, 12); attributes(which (x > 1)) }");
        assertEval("{ which(c(a=TRUE,b=FALSE,c=TRUE)) }");
    }
}
