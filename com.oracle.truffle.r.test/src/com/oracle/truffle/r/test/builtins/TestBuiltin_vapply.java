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

public class TestBuiltin_vapply extends TestBase {

    @Test
    public void testVapply() {
        assertEval("{ vapply(c(1L, 2L, 3L, 4L), function(x) x+5L, c(1L)) }");
        assertEval("{ iv <- integer(1); iv[[1]] = 1L; vapply(c(1L, 2L, 3L, 4L), function(x) x+5L, iv) }");
        assertEval("{ vapply(c(10, 20, 30, 40), function(x) x/2, c(1)) }");
        assertEval("{ vapply(c(\"hello\", \"goodbye\", \"up\", \"down\"), function(x) x, c(\"a\"), USE.NAMES = FALSE) }");
        assertEval("{ vapply(c(\"hello\", \"goodbye\", \"up\", \"down\"), function(x) x, c(\"a\")) }");
        assertEval("{ vapply(c(3+2i, 7-4i, 8+6i), function(x) x+(3+2i), c(1+1i)) }");
        assertEval("{ vapply(c(TRUE, FALSE, TRUE), function(x) x, c(TRUE)) }");
        assertEval("{ vapply(c(TRUE, FALSE, TRUE), function(x) FALSE, c(TRUE)) }");
        assertEval("{ vapply(c(1L, 2L, 3L, 4L), function(x, y) x+5L, c(1L), 10) }");

        assertEval("{ f<-function(x) as.integer(x + 1) ; y<-vapply(list(1:3, 6:8), f, rep(7, 3)); y }");
        assertEval("{ f<-function(x) x + 1 ; y<-vapply(list(1:3, 6:8), f, rep(as.double(NA), 3)); y }");
        assertEval("{ f<-function(x) x + 1 ; y<-vapply(list(1:3), f, rep(as.double(NA), 3)); y }");

        assertEval("{ f <- function(a, ...) vapply(a, function(.) identical(a, T, ...), NA); v <- c(1,2,3); f(v) }");
        assertEval("{ f<-function(x,y) x-y; w<-c(42, 42); z<-7; vapply(w, f, as.double(NA), x=z) }");

        assertEval("{ vapply(c(\"foo\", \"bar\"), 42, c(TRUE)) }");
        assertEval(Output.IgnoreErrorContext, "{ vapply(c(\"foo\", \"bar\"), function(x) FALSE, function() 42) }");
        assertEval(Output.IgnoreErrorContext, "{ vapply(c(\"foo\", \"bar\"), function(x) FALSE, c(TRUE), USE.NAMES=logical()) }");
        assertEval(Output.IgnoreErrorContext, "{ vapply(c(\"foo\", \"bar\"), function(x) FALSE, c(TRUE), USE.NAMES=\"42\") }");
        assertEval("{ vapply(c(\"foo\", \"bar\"), function(x) FALSE, c(TRUE), USE.NAMES=42) }");
    }

    @Test
    public void testApply() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6),2) ; apply(m,1,sum) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6),2) ; apply(m,2,sum) }");
    }
}
