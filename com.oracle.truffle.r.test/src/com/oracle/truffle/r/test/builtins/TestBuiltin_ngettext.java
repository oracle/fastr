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
public class TestBuiltin_ngettext extends TestBase {

    @Test
    public void testngettext1() {
        assertEval("argv <- list(1L, '%s is not TRUE', '%s are not all TRUE', NULL); .Internal(ngettext(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testngettext2() {
        assertEval("argv <- list(2L, '%s is not TRUE', '%s are not all TRUE', NULL); .Internal(ngettext(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testNgettext() {
        assertEval("{ ngettext(1, \"a\", \"b\") }");
        assertEval("{ ngettext(0, \"a\", \"b\") }");
        assertEval("{ ngettext(42, \"a\", \"b\") }");
        assertEval("{ ngettext(1, c(\"a\"), \"b\") }");
        assertEval("{ ngettext(1, \"a\", c(\"b\")) }");
        assertEval("{ ngettext(c(1), \"a\", \"b\") }");
        assertEval("{ ngettext(c(1,2), \"a\", \"b\") }");
        assertEval("{ ngettext(1+1i, \"a\", \"b\") }");
        assertEval("{ ngettext(1, NULL, \"b\") }");
        assertEval("{ ngettext(1, \"a\", NULL) }");
        assertEval("{ ngettext(1, NULL, NULL) }");
        assertEval("{ ngettext(1, c(\"a\", \"c\"), \"b\") }");
        assertEval("{ ngettext(1, \"a\", c(\"b\", \"c\")) }");
        assertEval("{ ngettext(1, c(1), \"b\") }");
        assertEval("{ ngettext(1, \"a\", c(1)) }");
        assertEval("{ ngettext(-1, \"a\", \"b\") }");
    }
}
