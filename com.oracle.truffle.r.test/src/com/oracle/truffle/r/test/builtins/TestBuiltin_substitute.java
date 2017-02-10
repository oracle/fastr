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

public class TestBuiltin_substitute extends TestBase {

    @Test
    public void testSubstitute() {
        assertEval("{ f <- function(expr) { substitute(expr) } ; f(a * b) }");
        assertEval("{ f <- function() { delayedAssign(\"expr\", a * b) ; substitute(expr) } ; f() }");
        assertEval("{ f <- function() { delayedAssign(\"expr\", a * b) ; substitute(dummy) } ; f() }");
        assertEval("{ delayedAssign(\"expr\", a * b) ; substitute(expr) }");
        assertEval("{ f <- function(expr) { expr ; substitute(expr) } ; a <- 10; b <- 2; f(a * b) }");
        assertEval("{ f <- function(y) { substitute(y) } ; typeof(f()) }");
        assertEval("{ f <- function(y) { as.character(substitute(y)) } ; f(\"a\") }");
        assertEval("{ f <- function(x) { g <- function() { substitute(x) } ; g() } ;  f(a * b) }");
        assertEval("{ substitute(a, list(a = quote(x + y), x = 1)) }");
        assertEval("{ f <- function(x = y, y = x) { substitute(x) } ; f() }");
        assertEval("{ f <- function(a, b=a, c=b, d=c) { substitute(d) } ; f(x + y) }");
        assertEval("{ f <- function(x) { substitute(x, list(a=1,b=2)) } ; f(a + b) }");

        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...)) } ; f(x + z, a * b) }");
        assertEval("{ f <- function(...) { substitute(list(...)) } ; f(x + z, a * b) }");

        assertEval("{ f<-function(...) { substitute(list(...)) }; is.language(f(c(1,2))) }");
        // language is a list (of sorts)
        assertEval("{ f<-function(...) { substitute(list(...)) }; length(f(c(1,2))) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; is.symbol(f(c(x=1,2))[[1]]) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; is.language(f(c(x=1,2))[[2]]) }");

        assertEval("{ f<-function(...) { substitute(list(...)) }; typeof(f(c(1,2))) }");

        assertEval("{ g<-function() { f<-function() { 42 }; substitute(f()) } ; typeof(g()[[1]]) }");

        assertEval("{ f<-function(...) { substitute(list(...)) }; is.symbol(f(c(x=1,2))[[2]][[1]]) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; is.double(f(c(x=1,2))[[2]][[2]]) }");

        assertEval("{ f <- function() { substitute(list(a=1,b=2,...,3,...)) } ; f() }");
        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...)) } ; f() }");
        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...,c=8)) } ; f() }");

        assertEval("{ f<-function(...) { substitute(list(...)) }; f(c(1,2)) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; f(c(x=1, 2)) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; f(c(x=1, 2, z=3)) }");
        assertEval("{ env <- new.env() ; z <- 0 ; delayedAssign(\"var\", z+2, assign.env=env) ; substitute(var, env=env) }");
        assertEval("{ env <- new.env() ; z <- 0 ; delayedAssign(\"var\", z+2, assign.env=env) ; z <- 10 ; substitute(var, env=env) }");

        assertEval("{ substitute(if(a) { x } else { x * a }, list(a = quote(x + y), x = 1)) }");
        assertEval("{ f <- function() { substitute(x(1:10), list(x=quote(sum))) } ; f() }");
        assertEval("{ substitute(x + y, list(x=1)) }");
        assertEval("{ f <- function(expra, exprb) { substitute(expra + exprb) } ; f(a * b, a + b) }");

        assertEval("{ f <- function(z) { g <- function(y) { substitute(y)  } ; g(z) } ; f(a + d) }");
        assertEval("{ substitute(a[x], list(a = quote(x + y), x = 1)) }");
        assertEval("{ substitute(x <- x + 1, list(x = 1)) }");

        assertEval("{ f <- function(y) { substitute(y) } ; f() }");
        assertEval(Output.IgnoreWhitespace, "{ substitute(function(x, a) { x + a }, list(a = quote(x + y), x = 1)) }");

        // GNU R generates warning here, but the test has been included nevertheless to make sure
        // that FastR does not crash here
        assertEval("f<-function(..., list=character()) { substitute(list(...))[-1L] }; as.character(f(\"config\"))");

        assertEval("{ substitute({class(y) <- x; y}, list(x=42)) }");

        assertEval("f<-function(...) { print(typeof(get('...'))); environment() }; e <- f(c(1,2), b=15, c=44); substitute(foo2({...}), e)");

        assertEval("f<-function(x,name) substitute(x$name); f(foo, bar)");
        assertEval("f<-function(x,name) substitute(x@name); f(foo, bar)");
        assertEval("f<-function(x,name) substitute(x$name<-1); f(foo, bar)");
        assertEval("f<-function(x,name) substitute(x@name<-2); f(foo, bar)");
        assertEval("f<-function(x,name) substitute(x$name); f(foo, bar); foo <- new.env(); foo$bar <- 1; eval(f(foo,bar))");
        assertEval("f<-function(x,name) substitute(x$name <- 5); f(foo, bar); foo <- new.env(); eval(f(foo,bar)); foo$bar");
        assertEval("f<-function(x,name) substitute(x@name); f(foo, bar); setClass('cl', representation(bar='numeric')); foo <- new('cl'); foo@bar <- 1; eval(f(foo,bar))");
        assertEval("f<-function(x,name) substitute(x@name <- 5); f(foo, bar); setClass('cl', representation(bar='numeric')); foo <- new('cl'); eval(f(foo,bar)); foo@bar");

        assertEval("substitute(1, 1)");
        assertEval("substitute(1, 1, 1)");
        assertEval("substitute(expr=1, env=1)");

        assertEval("substitute(1, NULL)");
        assertEval("substitute(1, NA)");
        assertEval("substitute(1, c(list(1)))");
        assertEval("substitute(1, list(c(list(1))))");
        assertEval("substitute(1, list(list(1)))");

        assertEval("a<-substitute(quote(x+1), NULL); a");
        assertEval("a<-substitute(quote(x+1), NA); a");
        assertEval("a<-substitute(quote(x+1), list(1)); a");
        assertEval("a<-substitute(quote(x+1), list(x=1)); a");
        assertEval("a<-substitute(quote(x+1), list(y=1)); a");
        assertEval("a<-substitute(quote(x+1), c(list(x=1), 'breakme')); a");
        assertEval("a<-substitute(quote(x+1), c(c(list(x=1)))); a");
        assertEval("a<-substitute(quote(x+1), list(c(c(list(x=1))))); a");
        assertEval("a<-substitute(quote(x+1), list(list(x=1))); a");
        assertEval("a<-substitute(quote(x+1), c(list(x=1, 1))); a");
        assertEval("a<-substitute(quote(x+y), c(list(x=1), list(y=1))); a");
        assertEval("substitute(quote(x+1), environment())");
        assertEval("f<-function() {}; substitute(quote(x+1), f)");
        assertEval("substitute(quote(x+1), setClass('a'))");
    }
}
