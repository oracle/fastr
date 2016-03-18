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

public class TestBuiltin_missing extends TestBase {

    @Test
    public void testMissing() {
        assertEval("{ f <- function(a) { g(a) } ;  g <- function(b) { missing(b) } ; f() }");
        assertEval("{ f <- function(a = 2) { g(a) } ; g <- function(b) { missing(b) } ; f() }");
        assertEval("{ f <- function(a,b,c) { missing(b) } ; f(1,,2) }");
        assertEval("{ g <- function(a, b, c) { b } ; f <- function(a,b,c) { g(a,b=2,c) } ; f(1,,2) }"); // not
        // really the builtin, but somewhat related
        assertEval("{ f <- function(x) {print(missing(x)); g(x)}; g <- function(y=2) {print(missing(y)); y}; f(1) }");
        assertEval("{ k <- function(x=2,y) { xx <- x; yy <- y; print(missing(x)); print(missing(xx)); print(missing(yy)); print(missing(yy))}; k(y=1) }");
        assertEval("{ f <- function(a = 2 + 3) { missing(a) } ; f() }");
        assertEval("{ f <- function(a = z) { missing(a) } ; f() }");
        assertEval("{ f <- function(a = 2 + 3) { a;  missing(a) } ; f() }");
        assertEval("{ f <- function(a = z) {  g(a) } ; g <- function(b) { missing(b) } ; f() }");
        assertEval("{ f <- function(x) { missing(x) } ; f(a) }");
        assertEval("{ f <- function(a) { g <- function(b) { before <- missing(b) ; a <<- 2 ; after <- missing(b) ; c(before, after) } ; g(a) } ; f() }");
        assertEval("{ f <- function(...) { g(...) } ;  g <- function(b=2) { missing(b) } ; f() }");

        assertEval("{ f <- function(x) { print(missing(x)); g(x) }; g <- function(y=3) { print(missing(y)); k(y) }; k <- function(l=4) { print(missing(l)); l }; f(1) }");
        assertEval("{ k <- function(x=2,y) { xx <- x; yy <- y; print(missing(x)); print(missing(xx)); print(missing(yy)); print(missing(yy))}; k() }");

        assertEval("{ f <- function(a = z, z) {  g(a) } ; g <- function(b) { missing(b) } ; f() }");
        assertEval("{ f <- function(a) { g(a) } ; g <- function(b=2) { missing(b) } ; f() }");
        assertEval("{ f <- function(x = y, y = x) { g(x, y) } ; g <- function(x, y) { missing(x) } ; f() }");
        assertEval("{ f <- function(...) { missing(..2) } ; f(x + z, a * b) }");

        assertEval("{ f <- function(x) {print(missing(x)); g(x)}; g <- function(y=2) {print(missing(y)); y}; f() }");
        assertEval("{ f <- function(x) { print(missing(x)); g(x) }; g <- function(y=3) { print(missing(y)); k(y) }; k <- function(l=4) { print(missing(l)); l }; f() }");
        assertEval("{ f <- function(x) { print(missing(x)) ; g(x) } ; g <- function(y=1) { print(missing(y)) ; h(y) } ; h <- function(z) { print(missing(z)) ; z } ; f() }");
        assertEval("{ f <- function(a,b,c,d,e,env) (length(objects(env, all.names = TRUE, pattern = \"^[.]__[CTA]_\"))); f2 <- function(env) (length(objects(env, all.names = TRUE, pattern = \"^[.]__[CTA]_\"))); f(); f2() }");
    }
}
