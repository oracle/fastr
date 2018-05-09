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
        assertEval("{ x <- 1:3; names(x) <- c('a', 'b'); t(x) }");

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

    @Test
    public void testTransposeSquare() {
        // test square matrices
        assertEval("{ m <- matrix(1:64, 8, 8) ; sum(m * t(m)) }");
        assertEval("{ m <- matrix(seq(0.01,0.64,0.01), 8, 8) ; sum(m * t(m)) }");
        assertEval("{ m <- matrix(c(T, T, F, F), 2, 2); t(m) }");
        assertEval("{ m <- matrix(c('1', '2', '3', '4'), 2, 2); t(m) }");
        assertEval("{ m <- matrix(as.raw(c(1,2,3,4)), 2, 2); t(m) }");
        assertEval("{ m <- matrix(list(a=1,b=2,c=3,d=4), 2, 2); t(m) }");
    }

    @Test
    public void testTransposeNonSquare() {
        // test square matrices
        assertEval("{ m <- matrix(1:8, 2, 4) ; t(m) }");
        assertEval("{ m <- matrix(seq(0.1,0.8,0.1), 2, 4) ; t(m) }");
        assertEval("{ m <- matrix(c(T, F, F, F, T, F, F, T), 2, 4); t(m) }");
        assertEval("{ m <- matrix(c('1', '2', '3', '4', '5', '6', '7', '8'), 2, 4); t(m) }");
        assertEval("{ m <- matrix(as.raw(c(1:8)), 2, 4); t(m) }");
        assertEval("{ m <- matrix(list(a=1,b=2,c=3,d=4,e=5,f=6,g=7,h=8), 2, 4); t(m) }");
    }
}
