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

public class TestrGenBuiltinlchoose extends TestBase {

    @Test
    @Ignore
    public void testlchoose1() {
        assertEval("argv <- list(FALSE, FALSE); .Internal(lchoose(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void testlchoose2() {
        assertEval("argv <- list(50L, 0:48); .Internal(lchoose(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void testlchoose3() {
        assertEval("argv <- list(0.5, 1:9); .Internal(lchoose(argv[[1]], argv[[2]]))");
    }
}
