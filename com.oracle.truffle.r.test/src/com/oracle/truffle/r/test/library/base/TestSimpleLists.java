/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.base;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestSimpleLists extends TestBase {

    @Test
    public void testListCreation() {
        assertEval("{ list() }");
        assertEval("{ list(list(),list()) }");
        assertEval("{ list(1,NULL,list()) }");
    }

    @Test
    public void testListAccess() {
        assertEval("{ l <- list(c(1,2,3),\"eep\") ; l[[1]] }");
        assertEval("{ l <- list(c(1,2,3),\"eep\") ; l[[2]] }");

        assertEval("{ l <- list(1,2,3) ; l[5] }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[5]) }");
        assertEval("{ l <- list(1,2,3) ; l[[5]] }");

        assertEval("{ l <- list(1,2,3) ; l[0] }");
        assertEval("{ l <- list(1,2,3) ; l[[0]] }");

        assertEval("{ l <- list(1,2,3) ; l[[NA]] }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[[NA]]) }");

        assertEval("{ l <- list(1,2,3) ; l[NA] }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[NA]) }");

        assertEval("{ l <- list(1,2,3) ; l[-2] }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[-2]) }");

        assertEval("{ l <- list(1,2,3) ; l[[-2]] }");

        assertEval("{ l <- list(1,2,3) ; l[-5] }");
        assertEval("{ l <- list(1,2,3) ; l[[-5]] }");

        assertEval("{ a <- list(1,NULL,list()) ; a[3] }");
        assertEval("{ a <- list(1,NULL,list()) ; a[[3]] }");
        assertEval("{ a <- list(1,NULL,list()) ; typeof(a[3]) }");
        assertEval("{ a <- list(1,NULL,list()) ; typeof(a[[3]]) }");

        assertEval("{ a <- list(1,2,3) ; x <- integer() ; a[x] }");
        assertEval("{ a <- list(1,2,3) ; x <- integer() ; a[[x]] }");
    }

    @Test
    public void testListUpdate() {
        assertEval("{ l <- list(c(1,2,3),c(4,5,6)) ; l[[1]] <- c(7,8,9) ; l[[1]] }");
    }

    @Test
    public void testListCombine() {
        assertEval("{ a <- c(list(1)) ; typeof(a) }");
        assertEval("{ a <- c(list(1)) ; typeof(a[1]) }");
        assertEval("{ a <- c(list(1)) ; typeof(a[[1]]) }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; a }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; typeof(a) }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; a[3] }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; typeof(a[3]) }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; a[[3]] }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; typeof(a[[3]]) }");
    }

    @Test
    public void testListArgumentEvaluation() {
        assertEval("{ a <- c(0,0,0) ; f <- function() { g <- function() { a[2] <<- 9 } ; g() } ; u <- function() { a <- c(1,1,1) ; f() ; a } ; list(a,u()) }");
    }
}
