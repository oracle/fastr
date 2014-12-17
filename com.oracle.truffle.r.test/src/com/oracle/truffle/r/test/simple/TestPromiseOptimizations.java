/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.simple;

import org.junit.*;

import com.oracle.truffle.r.test.*;

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
    }
}
