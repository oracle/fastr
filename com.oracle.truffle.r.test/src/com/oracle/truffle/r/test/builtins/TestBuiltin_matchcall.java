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

public class TestBuiltin_matchcall extends TestBase {

    @Test
    public void testMatchCall() {
        assertEval("{ fxy <- function(x, y) match.call(); fxy() }");
        assertEval("{ fxy <- function(x, y) match.call(); fxy(1, 2) }");
        assertEval("{ fxy <- function(x, y) match.call(); fxy(x=1, y=2) }");
        assertEval("{ fxy <- function(x, y) match.call(); fxy(y=2, x=1) }");

        assertEval("{ fd <- function(...) match.call(); fd() }");
        assertEval("{ fd <- function(...) match.call(); fd(1, 2) }");
        assertEval("{ fd <- function(...) match.call(); fd(x=1,y=2) }");
        assertEval("{ fd <- function(...) match.call(); fd(y=2, x=1) }");

        assertEval("{ fdx <- function(x, ...) match.call(); fdx() }");
        assertEval("{ fdx <- function(x, ...) match.call(); fdx(1, 2) }");
        assertEval("{ fdx <- function(x, ...) match.call(); fdx(x=1,y=2) }");
        assertEval("{ fdx <- function(x, ...) match.call(); fdx(y=2, x=1) }");

        assertEval("{ fdf <- function(...) match.call(expand.dots=F); fdf() }");
        assertEval("{ fdf <- function(...) match.call(expand.dots=F); fdf(1, 2) }");
        assertEval("{ fdf <- function(...) match.call(expand.dots=F); fdf(x=1,y=2) }");
        assertEval("{ fdf <- function(...) match.call(expand.dots=F); fdf(y=2, x=1) }");

        assertEval("{ fdxf <- function(x, ...) match.call(expand.dots=F); fdxf() }");
        assertEval("{ fdxf <- function(x, ...) match.call(expand.dots=F); fdxf(1, 2) }");
        assertEval("{ fdxf <- function(x, ...) match.call(expand.dots=F); fdxf(x=1,y=2) }");
        assertEval("{ fdxf <- function(x, ...) match.call(expand.dots=F); fdxf(y=2, x=1) }");

        assertEval("{ fdxy <- function(x, ..., y) match.call(); fdxy() }");
        assertEval("{ fdxy <- function(x, ..., y) match.call(); fdxy(1, 2, 3) }");
        assertEval("{ fdxy <- function(x, ..., y) match.call(); fdxy(2, 3, x=1) }");
        assertEval("{ fdxy <- function(x, ..., y) match.call(); fdxy(y=4, 1, 2, 3) }");
        assertEval("{ fdxy <- function(x, ..., y) match.call(); fdxy(y=4, 2, 3, x=1) }");

        assertEval("{ fdxyf <- function(x, ..., y) match.call(expand.dots=F); fdxyf() }");
        assertEval("{ fdxyf <- function(x, ..., y) match.call(expand.dots=F); fdxyf(1, 2, 3) }");
        assertEval("{ fdxyf <- function(x, ..., y) match.call(expand.dots=F); fdxyf(2, 3, x=1) }");
        assertEval("{ fdxyf <- function(x, ..., y) match.call(expand.dots=F); fdxyf(y=4, 1, 2, 3) }");
        assertEval("{ fdxyf <- function(x, ..., y) match.call(expand.dots=F); fdxyf(y=4, 2, 3, x=1) }");

        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); f2(\"a\") }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); f2(c(\"a\")) }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); f2(c(\"a\"), b=\"b\") }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); f2(c(\"a\"), c(\"b\")) }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); typeof(f2(\"a\")[[1]]) }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); typeof(f2(c(\"a\"))[[1]]) }");

        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(x, ...) f1(x, ...); f2(\"a\") }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(x, ...) f1(x, ...); f2(c(\"a\")) }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(x, ...) f1(x, ...); f2(c(\"a\"), x=\"b\") }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(x, ...) f1(x, ...); f2(c(\"a\"), c(\"b\")) }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(x, ...) f1(x, ...); typeof(f2(\"a\")[[1]]) }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(x, ...) f1(x, ...); typeof(f2(c(\"a\"))[[1]]) }");

        assertEval("{ f1<-function(...) { match.call(expand.dots=FALSE)$... }; f1() }");

        assertEval("fn3 <- function(...) { (function(...) match.call(cat, call(\"cat\", \"abc\", p=3,as.symbol(\"...\")), expand.dots = FALSE))(...) }; fn3(sep=x,lab=\"b\",fill=13)");
        assertEval("fn3 <- function(...) { (function(...) match.call(cat, call(\"cat\", \"abc\", p=3,as.symbol(\"...\")), expand.dots = TRUE))(...) }; fn3(sep=x,lab=\"b\",fill=13)");

        assertEval("{ foo<-function(...) match.call(expand.dots=F); bar<-function(x) x; y<-42; foo(bar(y), 7) }");

        assertEval("{ f <- function(a, ...) { UseMethod('f1', a) };" +
                        "f1.default <- function(a, b=2, c=3, d=4, e=5, ...) { match.call() };" +
                        // fill up signature cache
                        "f(a=1); f(a=1, b=2); f(a=1, b=2, c=3);f(a=1, b=2, d=4);" +
                        // this should be ok as well
                        "f(a=1, c=3, d=4, e=5) }");

        assertEval("{ f <- function(a, b, c, d, e) { UseMethod('f1', a) };" +
                        "f1.default <- function(a, b=2, c=3, d=4, e=5) { match.call() };" +
                        // fill up signature cache
                        "f(a=1); f(a=1, b=2); f(a=1, b=2, c=3);f(a=1, b=2, d=4);" +
                        // this should be ok as well
                        "f(a=1, c=3, d=4, e=5) }");

        // TODO add tests that pass "definition" and "call" explicitly
    }

    @Test
    public void testS3DispatchAndMatchcall() {
        assertEval("{foo.default<-function(a,b,...,c)match.call(); foo<-function(x,...)UseMethod('foo'); foo(2,3,c=4);}");
    }
}
