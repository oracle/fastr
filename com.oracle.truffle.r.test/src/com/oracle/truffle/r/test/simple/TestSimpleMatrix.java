/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.simple;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestSimpleMatrix extends TestBase {

    @Test
    public void testAccessDim() {
        assertEval("{ x<-1:10; dim(x) }");
        assertEval("{ x<-FALSE; dim(x) }");
        assertEval("{ x<-TRUE; dim(x) }");
        assertEval("{ x<-1; dim(x) }");
        assertEval("{ x<-1L; dim(x) }");
        assertEval("{ x<-c(1L, 2L, 3L); dim(x) }");
        assertEval("{ x<-c(1, 2, 3); dim(x) }");
    }

    @Test
    public void testUpdateDim() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2.1,3.9); dim(x) }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2L,3L); dim(x) }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2L,3L); dim(x) <- NULL; dim(x) }");
        assertEval("{ x<-c(1,2,3,4,5,6); dim(x) <- c(2L,3L); dim(x) }");
        assertEval("{ x<-c(1,2,3,4,5,6); dim(x) <- c(2L,3L); dim(x) <- NULL; dim(x) }");
    }

    @Test
    public void testAccessScalarIndex() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2,3); x[1,2] }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L); dim(x) <- c(2,5); x[2,4] }");
    }

    @Test
    public void testUpdateScalarIndex() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L); dim(x) <- c(2,3); x[1,2] <- 100L; x[1,2] }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L); dim(x) <- c(2,5); x[2,4] <- 100L; x[2,4] }");
    }

    @Test
    public void testMatrixAccessWithScalarAndVector() {
        assertEval("{ i <- c(1L,3L,5L) ; m <- 1:10 ; dim(m) <- c(2,5) ; m[2,i] }");
        assertEval("{ i <- c(1L,3L,5L) ; m <- c(\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\") ; dim(m) <- c(2,5) ; m[2,i] }");
    }

}
