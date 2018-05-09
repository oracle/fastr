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
public class TestBuiltin_args extends TestBase {

    @Test
    public void testargs1() {
        assertEval("argv <- list(NULL); .Internal(args(argv[[1]]))");
    }

    @Test
    public void testargs2() {
        assertEval("argv <- list(character(0)); .Internal(args(argv[[1]]))");
    }

    @Test
    public void testargs3() {
        assertEval("argv <- list(.Primitive(':')); .Internal(args(argv[[1]]))");
    }

    @Test
    public void testargs4() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame')); .Internal(args(argv[[1]]))");
    }

    @Test
    public void testargs5() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(args(argv[[1]]))");
    }

    @Test
    public void testArgs() {
        assertEval("{ f <- function(a) {}; fa <- args(f); }");
        assertEval("{ f <- function(a, b) {}; fa <- args(f); }");
        assertEval("{ sa <- args(sum); }");
        assertEval(Output.IgnoreWhitespace, "{  f <- function(x=1, y) x + y; args(f); }");
    }
}
