/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_gctorture2 extends TestBase {

    @Test
    public void testgctorture() {
        assertEval("argv <- list(FALSE); .Internal(gctorture(argv[[1]]))");
        assertEval("argv <- list(NULL); .Internal(gctorture(argv[[1]]))");
    }

    @Test
    public void testgctorture21() {
        assertEval("argv <- list(NULL, NULL, FALSE); .Internal(gctorture2(argv[[1]], argv[[2]], argv[[3]]))");
        assertEval("argv <- list(NULL, NULL, NULL); .Internal(gctorture2(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testgctorture22() {
        assertEval("argv <- list(FALSE, FALSE, FALSE); .Internal(gctorture2(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
