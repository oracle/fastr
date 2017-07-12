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

public class TestBuiltin_docall extends TestBase {

    @Test
    public void testDoCall() {
        assertEval("{ x<-list(c(1,2)); do.call(\"as.matrix\", x) }");
        assertEval("{ do.call(quote, list(quote(1)))}");
        assertEval("{ do.call(quote, list(quote(x)))}");
        assertEval("{ do.call(quote, list(quote(x+1)))}");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x) x; do.call(f, list(quote(y)))}");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x) x; do.call(f, list(quote(y + 1)))}");
        assertEval("{ do.call(\"+\", list(quote(1), 2))}");
        assertEval("v1 <- as.numeric_version('3.0.0'); v2 <- as.numeric_version('3.1.0'); do.call('<', list(v1, v2))");
        assertEval("v1 <- as.numeric_version('3.0.0'); v2 <- as.numeric_version('3.1.0'); do.call('<', list(quote(v1), quote(v2)))");
        assertEval(Output.IgnoreErrorContext, "typeof(do.call(function(x) x, list(as.symbol('foo'))))");
        assertEval("typeof(do.call(function(x) x, list(as.symbol('foo')), quote=TRUE))");

        assertEval("{ foo <- function() ls(parent.frame()); bar <- function(a,b,c) do.call('foo', list()); bar(a=1,b=2,c=3); }");
        assertEval("{ foo <- function() ls(parent.frame()); bar <- function(a,b,c) do.call('foo', list(), envir=globalenv()); bar(a=1,b=2,c=3) }");
        assertEval("{ foo <- function(a,b)  list(a=a,b=b); e <- new.env(); assign('a', 2); assign('b', 3); a<-0; b<-1; do.call('foo', list(a,b), env=e); }");
        assertEval("{ foo <- function(a,b)  list(a=a,b=b); e <- new.env(); assign('a', 2); assign('b', 3); a<-0; b<-1; do.call('foo', list(a=as.name('a'),as.name('b')), env=e) }");
        assertEval("{ foo <- function(a,b,c) { cat('foo called.'); list(a=a,b=b,c=c); }; side <- function() { cat('side effect!'); 42 }; do.call('foo', list(parse(text='side()')[[1]], 0, 0)); }");

        assertEval("{ e <- new.env(); assign('a', 42, e); a <- 1; foo <- function(x) force(x); do.call('foo', list(as.name('a')), envir=e); }");
        assertEval("{ e <- new.env(); assign('a', 42, e); a <- 1; foo <- function(x) force(x); do.call('foo', list(as.name('a')), envir=e, quote=T); }");
        assertEval("{ e <- new.env(); assign('foo', function() 42, e); foo <- function(x) 1; do.call('foo', list(), envir=e); }");
        assertEval("{ e <- new.env(); assign('foo', 42, e); foo <- function(x) 1; do.call('foo', list(), envir=e); }");
        assertEval("{ do.call('+', list(data.frame(1), data.frame(2)), envir = new.env()); do.call('assign', list('a',2,new.env()), envir = new.env()); }");

        assertEval("{ boo <- function(c) ls(parent.frame(2)); foo <- function(a,b) boo(a); bar <- function(x,z) do.call('foo', list(1,2)); bar() }");
        assertEval("{ boo <- function(c) ls(parent.frame(2)); foo <- function(a,b) boo(a); bar <- function(x,z) do.call('foo', list(parse(text='goo()'),2)); bar() }");
        assertEval("{ boo <- function(c) ls(parent.frame(3)); foo <- function(a,b) boo(a); bar <- function(x,z) do.call('foo', list(parse(text='goo()'),2)); baz <- function(bazX) bar(bazX,1); baz(); }");
        assertEval("{ f1 <- function(a) ls(parent.frame(2)); f2 <- function(b) f1(b); f3 <- function(c) f2(c); f4 <- function(d) do.call('f3', list(d)); f4(42); }");
    }
}
