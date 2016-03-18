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

public class TestBuiltin_isTRUE extends TestBase {

    @Test
    public void testisTRUE1() {
        assertEval("argv <- structure(list(x = TRUE), .Names = 'x');do.call('isTRUE', argv)");
    }

    @Test
    public void testIsTRUE() {
        assertEval("{ isTRUE(NULL) }");
        assertEval("{ isTRUE(TRUE) }");
        assertEval("{ isTRUE(FALSE) }");
        assertEval("{ isTRUE(NA) }");
        assertEval("{ isTRUE(1) }");
        assertEval("{ isTRUE(as.vector(TRUE)) }");
        assertEval("{ isTRUE(as.vector(FALSE)) }");
        assertEval("{ isTRUE(as.vector(1)) }");
        assertEval("{ file.path(\"a\", \"b\", c(\"d\",\"e\",\"f\")) }");
        assertEval("{ file.path() }");
    }
}
