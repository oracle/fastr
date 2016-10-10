/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_anyNA extends TestBase {

    @Test
    public void testanyNA1() {
        assertEval("argv <- list(c(1.81566026854212e-304, 0, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0));do.call('anyNA', argv)");
    }

    @Test
    public void testanyNA2() {
        assertEval("anyNA(list(list(c(NA)),c(1)), recursive=TRUE)");
        assertEval("anyNA(list(list(c(NA)),c(1)), recursive=FALSE)");
        assertEval("anyNA(list(list(c(NA)),c(1)), recursive=c(FALSE,TRUE))");
        assertEval("anyNA(list(list(4,5,NA), 3), recursive=TRUE)");
    }

}
