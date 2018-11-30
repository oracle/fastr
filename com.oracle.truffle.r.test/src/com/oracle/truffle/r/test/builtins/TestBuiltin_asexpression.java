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

public class TestBuiltin_asexpression extends TestBase {

    @Test
    public void testasexpression1() {
        assertEval("argv <- structure(list(x = 1), .Names = 'x');do.call('as.expression', argv)");
    }

    @Test
    public void testAsExpression() {
        assertEval("{ as.expression(\"name\") }");
        assertEval("{ as.expression(NULL) }");
        assertEval("{ as.expression(123) }");
        assertEval("{ as.expression(as.symbol(123)) }");
        assertEval("{ as.expression(c(1,2)) }");
        assertEval("{ as.expression(list(1,2)) }");
        assertEval("{ as.expression(list(\"x\" = 1, \"y\" = 2)) }");
        assertEval(Output.IgnoreErrorContext, "{ as.expression(sum) }");
        assertEval(Output.IgnoreErrorContext, "{ as.expression(function() {}) }");

        assertEval("{ as.expression(as.raw(1)) }");
        assertEval(Output.IgnoreWhitespace, "{ as.expression(as.raw(c(0, 1, 2, 127, 128, 255))) }");
    }

    @Test
    public void noCopyCheck() {
        assertEvalFastR("{ x <- as.expression(quote(x+2)); .fastr.identity(x) == .fastr.identity(as.expression(x)); }", "[1] TRUE");
    }
}
