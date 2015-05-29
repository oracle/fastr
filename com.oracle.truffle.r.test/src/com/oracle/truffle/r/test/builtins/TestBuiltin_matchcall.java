/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_matchcall extends TestBase {

    @Test
    public void testMatchCall() {
        assertEval("{ f <- function() match.call() ; f() }");
        assertEval("{ f <- function(x) match.call() ; f(2) }");
        assertEval("{ f <- function(x) match.call() ; f(x=2) }");
        assertEval("{ f <- function(...) match.call() ; f(2) }");
        assertEval("{ f <- function(...) match.call() ; f(x=2) }");
        assertEval("{ f <- function(...) match.call() ; f(x=2, y=3) }");
        assertEval("{ f <- function(...) match.call() ; f(x=2, 3) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(2) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(x=2) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(2, 3) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(x=2, y=3) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(x=2, 3) }");

        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); f2(\"a\") }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); f2(c(\"a\")) }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); f2(c(\"a\"), \"b\") }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); f2(c(\"a\"), c(\"b\")) }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); typeof(f2(\"a\")[[1]]) }");
        assertEval("{ f1<-function(...) { dots <- match.call(expand.dots = FALSE)$...; dots }; f2<-function(...) f1(...); typeof(f2(c(\"a\"))[[1]]) }");
    }
}
