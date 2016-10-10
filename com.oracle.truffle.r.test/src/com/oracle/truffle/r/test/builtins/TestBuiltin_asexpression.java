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
    }
}
