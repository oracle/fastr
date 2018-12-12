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
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_exists extends TestBase {

    @Test
    public void testexists1() {
        assertEval("argv <- structure(list(x = '.Device'), .Names = 'x');do.call('exists', argv)");
        assertEval("exists('somethingthatdoesnotexist123456789')");
        assertEval("exists('.Device', inherit=FALSE)");
        assertEval("x <- 42; exists('x', mode='numeric')");
        assertEval("x <- '42'; exists('x', mode='numeric')");
        assertEval("exists('foo', mode='helloworld')");
        assertEval("{ foo <- 42L; exists('foo', mode='helloworld') }");
    }

    private final String[] VALUES = new String[]{"1L", "T", "3.4", "c(1L,3L)", "c(1.2, 3.3)", "as.raw(c(1,3))", "c(T,F)", "c(3+i, 1+i)", "as.symbol('a')", "quote(a+3)", "expression(a+3)",
                    "function() 42", "as.pairlist(1)", "NULL", "environment", "stats:::C_acf$address"};
    private final String[] MODES = new String[]{"integer", "double", "numeric", "raw", "complex", "logical", "function", "name", "symbol", "language", "character", "list", "expression", "pairlist",
                    "NULL", "new.env()", "externalptr"};

    @Test
    public void testExistsWithMode() {
        assertEval(template("{ foo <- %0; exists('foo', mode='%1'); }", VALUES, MODES));
    }
}
