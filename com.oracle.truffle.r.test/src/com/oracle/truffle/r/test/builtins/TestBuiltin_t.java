/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_t extends TestBase {

    @Test
    public void testt1() {
        assertEval("argv <- structure(list(x = c(-2.13777446721376, 1.17045456767922,     5.85180137819007)), .Names = 'x');do.call('t', argv)");
    }

    @Test
    public void testTranspose() {
        assertEval("{ m <- matrix(1:49, nrow=7) ; sum(m * t(m)) }");
        assertEval("{ m <- matrix(1:81, nrow=9) ; sum(m * t(m)) }");
        assertEval("{ m <- matrix(-5000:4999, nrow=100) ; sum(m * t(m)) }");
        assertEval("{ m <- matrix(c(rep(1:10,100200),100L), nrow=1001) ; sum(m * t(m)) }");

        assertEval("{ m <- double() ; dim(m) <- c(0,4) ; t(m) }");

        assertEval("{ t(1:3) }");
        assertEval("{ t(t(t(1:3))) }");
        assertEval("{ t(matrix(1:6, nrow=2)) }");
        assertEval("{ t(t(matrix(1:6, nrow=2))) }");
        assertEval("{ t(matrix(1:4, nrow=2)) }");
        assertEval("{ t(t(matrix(1:4, nrow=2))) }");

        assertEval("{ x<-matrix(1:2, ncol=2, dimnames=list(\"a\", c(\"b\", \"c\"))); t(x) }");

        assertEval("t(new.env())");
        assertEval("v <- as.complex(1:50); dim(v) <- c(5,10); dimnames(v) <- list(as.character(40:44), as.character(10:19)); t(v)");
        assertEval("t(1)");
        assertEval("t(TRUE)");
        assertEval("t(as.raw(c(1,2,3,4)))");
        assertEval("t(matrix(1:6, 3, 2, dimnames=list(x=c(\"x1\",\"x2\",\"x3\"),y=c(\"y1\",\"y2\"))))");
    }
}
