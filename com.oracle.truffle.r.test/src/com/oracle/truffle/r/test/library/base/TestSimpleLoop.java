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
package com.oracle.truffle.r.test.library.base;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestSimpleLoop extends TestBase {

    @Test
    public void testLoops1() {
        assertEval("{ x<-210 ; repeat { x <- x + 1 ; break } ; x }");
        assertEval("{ x<-1 ; repeat { x <- x + 1 ; if (x > 11) { break } } ; x }");
        assertEval("{ x<-1 ; repeat { x <- x + 1 ; if (x <= 11) { next } else { break } ; x <- 1024 } ; x }");
        assertEval("{ x<-1 ; while(TRUE) { x <- x + 1 ; if (x > 11) { break } } ; x }");
        assertEval("{ x<-1 ; while(x <= 10) { x<-x+1 } ; x }");
        assertEval("{ x<-1 ; for(i in 1:10) { x<-x+1 } ; x }");
        assertEval("{ for(i in c(1,2)) { x <- i } ; x }");
        assertEval("{ f<-function(r) { x<-0 ; for(i in r) { x<-x+i } ; x } ; f(1:10) ; f(c(1,2,3,4,5)) }");
        assertEval("{ f<-function(r) { x<-0 ; for(i in r) { x<-x+i } ; x } ; f(c(1,2,3,4,5)) ; f(1:10) }");
        assertEval("{ r <- \"\" ; for (s in c(\"Hello\", \"world\")) r <- paste(r, s) ; r }");
    }

    @Test
    public void testOneIterationLoops() {
        assertEval("{ for (a in 1) cat(a) }");
        assertEval("{ for (a in 1L) cat(a) }");
        assertEval("{ for (a in \"xyz\") cat(a) }");
    }

    @Test
    public void testFactorial() {
        assertEval("{ f<-function(i) { if (i<=1) {1} else {r<-i; for(j in 2:(i-1)) {r=r*j}; r} }; f(10) }");
    }

    @Test
    public void testFibonacci() {
        assertEval("{ f<-function(i) { x<-integer(i); x[1]<-1; x[2]<-1; if (i>2) { for(j in 3:i) { x[j]<-x[j-1]+x[j-2] } }; x[i] } ; f(32) }");
    }

    @Test
    public void testLoopsErrors() {
        assertEval(Output.ContainsError, "{ while (1 < NA) { 1 } }");

        assertEval("{ break; }");
        assertEval("{ next; }");
        assertEval("{ break(); }");
        assertEval("{ next(); }");
        assertEval("{ break(1,2,3); }");
        assertEval("{ next(1,2,$$); }");
    }

    @Test
    public void testLoopsErrorsIgnore() {
        assertEval(Ignored.Unknown, Output.ContainsError, "{ l <- quote(for(i in s) { x <- i }) ; s <- 1:3 ; eval(l) ; s <- function(){} ; eval(l) ; x }");
        assertEval(Ignored.Unknown, Output.ContainsError, "{ l <- function(s) { for(i in s) { x <- i } ; x } ; l(1:3) ; s <- function(){} ; l(s) ; x }");
        assertEval(Ignored.Unknown, Output.ContainsError, "{ l <- quote({ for(i in s) { x <- i } ; x }) ; f <- function(s) { eval(l) } ; f(1:3) ; s <- function(){} ; f(s) ; x }");
    }

    @Test
    public void testLoopsBreakNext() {
        assertEval("{ for(i in c(1,2,3,4)) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }");
        assertEval("{ f <- function() { for(i in c(1,2,3,4)) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x } ; f()  }");
        assertEval("{ f <- function() { for(i in 1:4) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x } ; f() }");
        assertEval("{ for(i in 1:4) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }");
        assertEval("{ i <- 0L ; while(i < 3L) { i <- i + 1 ; if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }");
        assertEval("{ f <- function(s) { for(i in s) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x } ; f(2:1) ; f(c(1,2,3,4)) }");
        assertEval("{ x <- repeat tryCatch({break}, handler = function(e) NULL) }");
    }

    @Test
    public void testForSequenceDescending() {
        assertEval("{ sum <- 0; for (i in 3:1) { sum <- sum + i; }; sum; }");
    }

    @Test
    public void testLoops3() {
        assertEval("{ l <- quote({for(i in c(1,2)) { x <- i } ; x }) ; f <- function() { eval(l) } ; f() }");
        assertEval("{ l <- quote(for(i in s) { x <- i }) ; s <- 1:3 ; eval(l) ; s <- 2:1 ; eval(l) ; x }");
        assertEval("{ l <- quote({for(i in c(2,1)) { x <- i } ; x }) ; f <- function() { if (FALSE) i <- 2 ; eval(l) } ; f() }");

        assertEval("{ l <- quote({ for(i in c(1,2,3,4)) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }) ; f <- function() { eval(l) } ; f()  }");
        assertEval("{ l <- quote({ for(i in 1:4) { if (i == 1) { next } ; if (i==3) { break } ; x <- i ; if (i==4) { x <- 10 } } ; x }) ; f <- function() { eval(l) } ; f()  }");
        assertEval("{ l <- quote(for(i in s) { x <- i }) ; s <- 1:3 ; eval(l) ; s <- NULL ; eval(l) ; x }");
    }

    @Test
    public void testDynamic() {
        assertEval("{ l <- quote({x <- 0 ; for(i in 1:10) { x <- x + i } ; x}) ; f <- function() { eval(l) } ; x <<- 10 ; f() }");
    }
}
