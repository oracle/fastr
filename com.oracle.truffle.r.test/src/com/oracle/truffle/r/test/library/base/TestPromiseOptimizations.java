/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

public class TestPromiseOptimizations extends TestBase {

    @Test
    public void testDeoptimization() {
        // Return RFunction: DEOPT
        assertEval("{ f <- function(x) { function() {x} } ; a <- 1 ; b <- f( a ) ; a <- 10 ; b() }");

        // Return REnvironment: DEOPT
        assertEval("{ f <- function(x) { sys.frame(sys.nframe()) } ; a <- 1 ; e <- f( a ) ; a <- 10 ; e$x }");

        // <<- RFunction: DEOPT
        assertEval("{ f <- function(x) { b <<- function() {x} } ; a <- 1 ; f( a ) ; a <- 10 ; b() }");

        // <<- REnvironment: DEOPT
        assertEval("{ f <- function(x) { e <<- sys.frame(sys.nframe()) } ; a <- 1 ; f( a ) ; a <- 10 ; e$x }");

        // "substitute": DEOPT

        // "assign" REnvironment: DEOPT
        assertEval("{ f <- function(x) { assign('e', sys.frame(sys.nframe()), parent.frame()) } ; a <- 1 ; f( a ) ; a <- 10 ; e$x }");

        // "assign" RFunction: DEOPT
        assertEval("{ f <- function(x) { assign('e', function() {x}, parent.frame()) } ; a <- 1 ; f( a ) ; a <- 10 ; e() }");

        // "delayedAssign": DEOPT (the SUPPLIED eval environment!)
        assertEval("{ f <- function(x) { delayedAssign('v', x, sys.frame(sys.nframe()), parent.frame()) } ; a <- 1 ; v <- 0; f( a ) ; a <- 10 ; v }");

        // "delayedAssign" REnvironment: (Special case of but handled by: delayedAssign)
        assertEval("{ f <- function(x) { le <- sys.frame(sys.nframe()); delayedAssign('e', le, le, parent.frame()) } ; a <- 1 ; f( a ) ; a <- 10 ; e$x }");

        // "delayedAssign" RPromise: (Special case of but handled by: delayedAssign)
        assertEval("{ f <- function(x) { delayedAssign('v', x, sys.frame(sys.nframe()), parent.frame()); } ; a <- 1 ; v <- 0; f( a ) ; a <- 10 ; v }");

        // "delayedAssign" RFunction: (Special case of but handled by: delayedAssign)
        assertEval("{ f <- function(x) { delayedAssign('b', function() {x}, sys.frame(sys.nframe()), parent.frame()); } ; a <- 1 ; f( a ) ; a <- 10 ; b() }");

        // the value of 'pi' promise may look like temporary vector, but the arithmetic operation
        // must not re-use it for the result
        assertEval("{ pi/180; pi }");
        // similar situation as above
        assertEval("{ delayedAssign('x', c(1,2,3)); x/180; x }");
    }
}
