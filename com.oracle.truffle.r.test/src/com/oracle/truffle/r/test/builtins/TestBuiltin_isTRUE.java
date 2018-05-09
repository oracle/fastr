/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
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
