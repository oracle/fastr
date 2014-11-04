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

// Checkstyle: stop line length check
public class TestrGenBuiltinmerge extends TestBase {

    @Test
    @Ignore
    public void testmerge1() {
        assertEval("argv <- list(c(0L, 0L, 0L, 0L, 0L), 0L, FALSE, TRUE); .Internal(merge(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testmerge2() {
        assertEval("argv <- list(c(0L, 0L, 0L, 0L, 0L), 0L, TRUE, FALSE); .Internal(merge(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    @Ignore
    public void testmerge3() {
        assertEval("argv <- list(c(0L, 0L, 0L, 3L, 4L), c(0L, 0L, 0L, 3L, 4L), FALSE, FALSE); .Internal(merge(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }
}
