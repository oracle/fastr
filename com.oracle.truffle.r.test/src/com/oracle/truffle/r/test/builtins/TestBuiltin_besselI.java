/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
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
