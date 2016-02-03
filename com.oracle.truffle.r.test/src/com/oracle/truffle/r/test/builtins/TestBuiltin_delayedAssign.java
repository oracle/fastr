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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_delayedAssign extends TestBase {

    @Test
    public void testDelayedAssign() {
        assertEval("{ delayedAssign(\"x\", y); y <- 10; x }");
        assertEval("{ delayedAssign(\"x\", a+b); a <- 1 ; b <- 3 ; x }");
        assertEval("{ f <- function() { delayedAssign(\"x\", y); y <- 10; x  } ; f() }");
        assertEval("{ delayedAssign(\"x\", y); delayedAssign(\"y\", x) ; x }");
        assertEval("{ f <- function() { delayedAssign(\"x\", y); delayedAssign(\"y\", x) ; x } ; f() }");
        assertEval("{ f <- function() { delayedAssign(\"x\", 3); delayedAssign(\"x\", 2); x } ; f() }");
        assertEval("{ f <- function(...) { delayedAssign(\"x\", ..1) ; y <<- x } ; f(10) ; y }");
        assertEval("{ f <- function() print (\"outer\");  g <- function() { delayedAssign(\"f\", 1); f() }; g()}");
        assertEval(Output.ContainsError, "{ f <- function() { delayedAssign(\"x\",y); delayedAssign(\"y\",x); g(x, y)}; g <- function(x, y) { x + y }; f() }");
        assertEval("{ f <- function() { delayedAssign(\"x\",y); delayedAssign(\"y\",x); list(x, y)}; f() }");
        assertEval(Output.ContainsError, "{ f <- function() { delayedAssign(\"x\",y); delayedAssign(\"y\",x); paste(x, y)}; f() }");
        assertEval("{ f <- function() { delayedAssign(\"x\",y); delayedAssign(\"y\",x); print(x, y)}; f() }");
        assertEval("{ f <- function() { p <- 0; for (i in 1:10) { if (i %% 2 == 0) { delayedAssign(\"a\", p + 1); } else { a <- p + 1; }; p <- a; }; p }; f() }");
        assertEval("{ f <- function() { x <- 4 ; delayedAssign(\"x\", y); y <- 10; x  } ; f() }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; delayedAssign(\"x\", y, h, h) ; assign(\"y\", 2, h) ; get(\"x\", h) }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; delayedAssign(\"x\", y, h, h) ; assign(\"y\", 2, h) ; get(\"x\", h) }");
    }
}
