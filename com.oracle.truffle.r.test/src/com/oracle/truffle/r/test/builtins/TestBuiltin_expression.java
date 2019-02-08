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
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_expression extends TestBase {

    @Test
    public void testExpression() {
        assertEval("{ f <- function(z) {z}; e<-c(expression(f), 7); eval(e) }");
        assertEval("{ f <- function(z) {z}; e<-expression(f); e2<-c(e, 7); eval(e2) }");

        assertEval("{ x<-expression(1); y<-c(x,2); typeof(y[[2]]) }");
        assertEval("{ class(expression(1)) }");

        assertEval("{ x<-expression(1); typeof(x[[1]]) }");
        assertEval("{ x<-expression(a); typeof(x[[1]]) }");
        assertEval("{ x<-expression(1); y<-c(x,2); typeof(y[[1]]) }");

        assertEval(Ignored.OutputFormatting, "{ expression(a=1, 2, b=3) }");
        assertEval("{ names(expression(a=1, 2, b=3)) }");
    }
}
