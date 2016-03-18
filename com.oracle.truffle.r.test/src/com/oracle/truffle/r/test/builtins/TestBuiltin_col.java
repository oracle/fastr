/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_col extends TestBase {

    @Test
    public void testcol1() {
        assertEval("argv <- list(c(2L, 2L)); .Internal(col(argv[[1]]))");
    }

    @Test
    public void testcol3() {
        assertEval("argv <- list(c(1L, 0L)); .Internal(col(argv[[1]]))");
    }

    @Test
    public void testCol() {
        assertEval("{ ma <- matrix(1:12, 3, 4) ; col(ma) }");
        assertEval("{ ma <- cbind(x = 1:10, y = (-4:5)^2) ; col(ma) }");

        assertEval(Output.ContainsError, "{ col(c(1,2,3)) }");
    }
}
