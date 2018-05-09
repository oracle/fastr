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
public class TestBuiltin_det extends TestBase {

    @Test
    public void testdet1() {
        assertEval("argv <- list(structure(c(FALSE, TRUE, TRUE, FALSE), .Dim = c(2L, 2L), .Dimnames = list(c('A', 'B'), c('A', 'B'))), TRUE); .Internal(det_ge_real(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testdet2() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE), .Dim = c(2L, 2L), .Dimnames = list(c('A', 'B'), c('A', 'B'))), TRUE); .Internal(det_ge_real(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testdet3() {
        assertEval("argv <- list(structure(c(2, 1, 1, 2), .Dim = c(2L, 2L), .Dimnames = list(c('A', 'B'), c('A', 'B'))), TRUE); .Internal(det_ge_real(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testdet5() {
        assertEval("argv <- structure(list(x = structure(c(0, 0, 0, 0, 0, 0, NA,     0, 0, NA, NA, 0, 0, 0, 0, 1), .Dim = c(4L, 4L))), .Names = 'x');do.call('det', argv)");
    }

    @Test
    public void testDet() {
        assertEval("{ det(matrix(c(1,2,4,5),nrow=2)) }");
        assertEval("{ det(matrix(c(1,-3,4,-5),nrow=2)) }");
        assertEval("{ det(matrix(c(1,0,4,NA),nrow=2)) }");
    }
}
