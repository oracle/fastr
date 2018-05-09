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
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_besselI extends TestBase {

    @Test
    public void testbesselI1() {
        assertEval("besselI(1,c(NA,1))");
        assertEval("besselI(c(1,2),1)");
        assertEval("besselI(c(1,2,3),c(1,2))");
        assertEval("besselI(c(1,2,3),c(1,2),c(3,4,5,6,7,8))");
        assertEval("besselI(c(1,NA),1)");
        assertEval("besselI(c(1,2,3),c(NA,2))");
        assertEval("besselI(c(1,2,3),c(1,2),c(3,4,NA,6,7,8))");
        assertEval("argv <- list(FALSE, FALSE, 1); .Internal(besselI(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testbesselI2() {
        assertEval("argv <- list(logical(0), logical(0), 1); .Internal(besselI(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
