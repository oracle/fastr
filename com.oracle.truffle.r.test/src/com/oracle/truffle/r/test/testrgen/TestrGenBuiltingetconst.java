/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltingetconst extends TestBase {

    @Test
    @Ignore
    public void testgetconst1() {
        assertEval("argv <- list(list(list(), NULL), 1); .Internal(getconst(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void testgetconst2() {
        assertEval("argv <- list(list(FALSE), 1); .Internal(getconst(argv[[1]], argv[[2]]))");
    }
}
