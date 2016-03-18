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
    }
}
