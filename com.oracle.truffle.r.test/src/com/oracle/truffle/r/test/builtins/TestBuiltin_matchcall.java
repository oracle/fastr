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

        // TODO add tests that pass "definition" and "call" explicitly
    }
}
