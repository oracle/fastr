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

public class TestrGenBuiltincol extends TestBase {

    @Test
    @Ignore
    public void testcol1() {
        assertEval("argv <- list(c(2L, 2L)); .Internal(col(argv[[1]]))");
    }

    @Test
    @Ignore
    public void testcol3() {
        assertEval("argv <- list(c(1L, 0L)); .Internal(col(argv[[1]]))");
    }
}
