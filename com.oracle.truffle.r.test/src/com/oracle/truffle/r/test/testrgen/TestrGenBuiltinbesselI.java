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

public class TestrGenBuiltinbesselI extends TestBase {

    @Test
    public void testbesselI1() {
        assertEval("argv <- list(FALSE, FALSE, 1); .Internal(besselI(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testbesselI2() {
        assertEval("argv <- list(logical(0), logical(0), 1); .Internal(besselI(argv[[1]], argv[[2]], argv[[3]]))");
    }
}