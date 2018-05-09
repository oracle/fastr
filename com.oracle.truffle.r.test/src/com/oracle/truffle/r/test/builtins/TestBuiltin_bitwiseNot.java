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
public class TestBuiltin_bitwiseNot extends TestBase {

    @Test
    public void testbitwiseNot1() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(bitwiseNot(argv[[1]]))");
    }

    @Test
    public void testBitwiseFunctions() {
        assertEval("{ bitwNot(c(17,24,34,48,51,66,72,99)) }");
        assertEval("{ bitwNot(c(0,100,200,50,70,20)) }");
        assertEval("{ bitwNot(as.integer(NA)) }");

        assertEval("{ bitwNot(TRUE) }");
    }
}
