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

public class TestSimpleFunctions extends TestBase {

    @Test
    public void testDefinitionsWorking() {
        assertEval("{ x<-function(z){z} ; x(TRUE) }");
        assertEval("{ x<-1 ; f<-function(){x} ; x<-2 ; f() }");
        assertEval("{ x<-1 ; f<-function(x){x} ; f(TRUE) }");
        assertEval("{ x<-1 ; f<-function(x){a<-1;b<-2;x} ; f(TRUE) }");
        assertEval("{ f<-function(x){g<-function(x) {x} ; g(x) } ; f(TRUE) }");
        assertEval("{ x<-1 ; f<-function(x){a<-1; b<-2; g<-function(x) {b<-3;x} ; g(b) } ; f(TRUE) }");
        assertEval("{ x<-1 ; f<-function(z) { if (z) { x<-2 } ; x } ; x<-3 ; f(FALSE) }");
        assertEval("{ f<-function() {z} ; z<-2 ; f() }");
        assertEval("{ x<-1 ; g<-function() { x<-12 ; f<-function(z) { if (z) { x<-2 } ; x } ; x<-3 ; f(FALSE) } ; g() }");
        assertEval("{ x<-function() { z<-211 ; function(a) { if (a) { z } else { 200 } } } ; f<-x() ; z<-1000 ; f(TRUE) }");
    }

    @Test
    public void testReturn() {
        assertEval("{ f<-function() { return() } ; f() }");
        assertEval("{ f<-function() { return(2) ; 3 } ; f() }");
    }

    @Test
    public void testDefinitionsNamedAndDefault() {
        assertEval("{ f<-function(a=1,b=2,c=3) {TRUE} ; f(,,) }");

        assertEval("{ f<-function(x=2) {x} ; f() } ");
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(1,2,c=4,d=4) }");
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(1,2,d=8,c=1) }");
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(1,d=8,2,c=1) }");
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(d=8,1,2,c=1) }");
        assertEval("{ f<-function(a,b,c=2,d) {c} ; f(d=8,c=1,2,3) }");
        assertEval("{ f<-function(a=10,b,c=20,d=20) {c} ; f(4,3,5,1) }");

        assertEval("{ x<-1 ; z<-TRUE ; f<-function(y=x,a=z,b) { if (z) {y} else {z}} ; f(b=2) }");
        assertEval("{ x<-1 ; z<-TRUE ; f<-function(y=x,a=z,b) { if (z) {y} else {z}} ; f(2) }");
        assertEval("{ x<-1 ; f<-function(x=x) { x } ; f(x=x) }");
        assertEval("{ f<-function(z, x=if (z) 2 else 3) {x} ; f(FALSE) }");

        assertEval("{f<-function(a,b,c=2,d) {c} ; g <- function() f(d=8,c=1,2,3) ; g() ; g() }");

        assertEval("{ x <- function(y) { sum(y) } ; f <- function() { x <- 1 ; x(1:10) } ; f() }");
        assertEval("{ f <- sum ; f(1:10) }");
    }

    @Test
    public void testDefinitions() {
        assertEval("{ \"%plus%\" <- function(a,b) a+b ; 3 %plus% 4 }");
        assertEval("{ \"-\"(1) }");
        assertEval("x<-function(){1};x");

        // replacement function
        assertEval("{ 'my<-' <- function(x, value) { attr(x, \"myattr\") <- value ; x } ; z <- 1; my(z) <- \"hello\" ; z }");
    }

    @Test
    @Ignore
    public void testDefinitionsIgnore() {
        // function matching, builtins
        assertEval("{ x <- function(a,b) { a^b } ; f <- function() { x <- \"sum\" ; sapply(1, x, 2) } ; f() }");
        assertEval("{ x <- function(a,b) { a^b } ; g <- function() { x <- \"sum\" ; f <- function() { sapply(1, x, 2) } ; f() }  ; g() }");
        assertEval("{ x <- function(a,b) { a^b } ; f <- function() { x <- 211 ; sapply(1, x, 2) } ; f() }");
        assertEval("{ x <- function(a,b) { a^b } ; dummy <- sum ; f <- function() { x <- \"dummy\" ; sapply(1, x, 2) } ; f() }");
        assertEval("{ x <- function(a,b) { a^b } ; dummy <- sum ; f <- function() { x <- \"dummy\" ; dummy <- 200 ; sapply(1, x, 2) } ; f() }");
        assertEval("{ foo <- function (x) { x } ; foo() }");
        assertEval("{ foo <- function (x) { x } ; foo(1,2,3) }");
    }

    @Test
    @Ignore
    public void testErrors() {
        assertEvalError("{ x<-function(){1} ; x(y=1) }");
        assertEvalError("{ x<-function(y, b){1} ; x(y=1, 2, 3, z = 5) }");
        assertEvalError("{ x<-function(){1} ; x(1) }");
        assertEvalError("{ x<-function(a){1} ; x(1,) }");
        assertEvalError("{ x<-function(){1} ; x(y=sum(1:10)) }");
        assertEvalError("{ f <- function(x) { x } ; f() }");
        assertEvalError("{ x<-function(y,b){1} ; x(y=1,y=3,4) }");
        assertEvalError("{ x<-function(foo,bar){foo*bar} ; x(fo=10,f=1,2) }");

        assertEvalError("{ f <- function(a,a) {1} }"); // note
        // exactly GNU-R message
        assertEvalError("{ f <- function(a,b,c,d) { a + b } ; f(1,x=1,2,3,4) }");
    }

    @Test
    public void testBinding() {
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; myapp(function(x,y) { x + y }, 1, 2) ; myapp(sum, 1, 2) }");
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; myapp(f = function(x,y) { x + y }, y = 1, x = 2) ; myapp(f = sum, x = 1, y = 2) }");
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; myapp(f = function(x,y) { x + y }, y = 1, x = 2) ; myapp(f = sum, x = 1, y = 2) ; myapp(f = c, y = 10, x = 3) }");
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; myapp(f = function(x,y) { x + y }, y = 1, x = 2) ; myapp(f = sum, x = 1, y = 2) ; myapp(f = function(x,y) { x - y }, y = 10, x = 3) }");
        assertEval("{ myapp <- function(f, x, y) { f(x,y) } ; g <- function(x,y) { x + y } ; myapp(f = g, y = 1, x = 2) ; myapp(f = sum, x = 1, y = 2) ; myapp(f = g, y = 10, x = 3) ;  myapp(f = g, y = 11, x = 2) }");
        assertEval("{ f <- function(i) { if (i==2) { c <- sum }; c(1,2) } ; f(1) ; f(2) }");
        assertEval("{ f <- function(i) { if (i==2) { assign(\"c\", sum) }; c(1,2) } ; f(1) ; f(2) }");
        assertEval("{ f <- function(func, arg) { func(arg) } ; f(sum, c(3,2)) ; f(length, 1:4) }");
        assertEval("{ f <- function(func, arg) { func(arg) } ; f(sum, c(3,2)) ; f(length, 1:4) ; f(length,1:3) }");
        assertEval("{ f <- function(func, arg) { func(arg) } ; f(sum, c(3,2)) ; f(length, 1:4) ; f(function(i) {3}, 1) ; f(length,1:3) }");
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; f(function(x) {TRUE}, 5) ; f(is.na, 4) }");
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 3) }");
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; h <- function(x) { x == x } ; f(h, 3) }");
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 3) ; f(is.na, 10) }");
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 3) ; f(c, 10) }");
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 3) ; f(function(x) { 3+4i }, 10) }");
        assertEval("{ f <- function(func, a) { if (func(a)) { 1 } else { 2 } } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(is.na, 10) }");
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4)  }");
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(is.na, 10) }");
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(length, 10) }");
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(g, 10) ; f(is.na,5) }");
        assertEval("{ f <- function(func, a) { func(a) && TRUE } ; g <- function(x) {TRUE} ; f(g, 5) ; f(is.na, 4) ; f(function(x) { x + x }, 10) }");

        assertEval("{ f <- function(i) { c(1,2) } ; f(1) ; c <- sum ; f(2) }");
    }

    @Test
    public void testRecursion() {
        assertEval("{ f<-function(i) { if(i==1) { 1 } else { j<-i-1 ; f(j) } } ; f(10) }");
        assertEval("{ f<-function(i) { if(i==1) { 1 } else { f(i-1) } } ; f(10) }");
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*f(i-1) } ; f(10) }"); // factorial
        assertEval("{ f<-function(i) { if(i<=1L) 1L else i*f(i-1L) } ; f(10L) }"); // factorial
        // 100 times calculate factorial of 120
        // the GNU R outputs 6.689503e+198
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*f(i-1) } ; g<-function(n, f, a) { if (n==1) { f(a) } else { f(a) ; g(n-1, f, a) } } ; g(100,f,120) }");

        // Fibonacci numbers
        assertEval("{ f<-function(i) { if (i==1) { 1 } else if (i==2) { 1 } else { f(i-1) + f(i-2) } } ; f(10) }");
        assertEval("{ f<-function(i) { if (i==1L) { 1L } else if (i==2L) { 1L } else { f(i-1L) + f(i-2L) } } ; f(10L) }");
    }

    @Test
    public void testPromises() {
        assertEval("{ z <- 1 ; f <- function(c = z) { c(1,2) ; z <- z + 1 ; c  } ; f() }");
    }

    @Test
    @Ignore
    public void testPromisesIgnore() {
        assertEval("{ f <- function(x = z) { z = 1 ; x } ; f() }");
        assertEval("{ z <- 1 ; f <- function(c = z) {  z <- z + 1 ; c  } ; f() }");
        assertEval("{ f <- function(a) { g <- function(b) { x <<- 2; b } ; g(a) } ; x <- 1 ; f(x) }");
        assertEval("{ f <- function(a) { g <- function(b) { a <<- 3; b } ; g(a) } ; x <- 1 ; f(x) }");
        assertEval("{ f <- function(x) { function() {x} } ; a <- 1 ; b <- f(a) ; a <- 10 ; b() }");
        assertEvalError("{ f <- function(x = y, y = x) { y } ; f() }");
    }

    @Test
    public void testMatching() {
        assertEval("{ x<-function(foo,bar){foo*bar} ; x(f=10,2) }");
        assertEval("{ x<-function(foo,bar){foo*bar} ; x(fo=10, bar=2) }");
    }

    @Test
    @Ignore
    public void testMatchingIgnore() {
        assertEvalError("{ f <- function(hello, hi) { hello + hi } ; f(h = 1) }");
        assertEvalError("{ f <- function(hello, hi) { hello + hi } ; f(hello = 1, bye = 3) }");
        assertEvalError("{ f <- function(a) { a } ; f(1,2) }");
    }

    @Test
    public void testDots() {
        assertEval("{ f <- function(a, b) { a - b } ; g <- function(...) { f(1, ...) } ; g(b = 2) }");
        assertEval("{ f <- function(...) { g(...) } ;  g <- function(b=2) { b } ; f() }");
        assertEval("{ f <- function(a, barg) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(b=2) }");
        assertEval("{ f <- function(a, b) { a * b } ; g <- function(...) { f(...,...) } ; g(3) }");
        assertEval("{ g <- function(...) { c(...,...) } ; g(3) }");

        assertEval("{ f <- function(...) cat(..., \"\\n\") ; f(\"Hello\", \"world\") }");
    }

    @Test
    @Ignore
    public void testDotsIgnore() {
        assertEval("{ f <- function(...) { ..1 } ;  f(10) }");
        assertEval("{ f <- function(...) { x <<- 10 ; ..1 } ; x <- 1 ; f(x) }");
        assertEval("{ f <- function(...) { ..1 ; x <<- 10 ; ..1 } ; x <- 1 ; f(x) }");
        assertEval("{ f <- function(...) { ..1 ; x <<- 10 ; ..2 } ; x <- 1 ; f(100,x) }");
        assertEval("{ f <- function(...) { ..2 ; x <<- 10 ; ..1 } ; x <- 1 ; f(x,100) }");
        assertEval("{ g <- function(...) { 0 } ; f <- function(...) { g(...) ; x <<- 10 ; ..1 } ; x <- 1 ; f(x) }");
        assertEval("{ f <- function(...) { substitute(..1) } ;  f(x+y) }");
        assertEval("{ f <- function(...) { g <- function() { ..1 } ; g() } ; f(a=2) }");
        assertEval("{ f <- function(...) { ..1 <- 2 ; ..1 } ; f(z = 1) }");

        assertEval("{ g <- function(a,b) { a + b } ; f <- function(...) { g(...) }  ; f(1,2) }");
        assertEval("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(...,x=4) }  ; f(b=1,a=2) }");
        assertEval("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(x=4, ...) }  ; f(b=1,a=2) }");
        assertEval("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(x=4, ..., 10) }  ; f(b=1) }");
        assertEvalError("{ g <- function(a,b,x) { a + b * x } ; f <- function(...) { g(x=4, ..., 10) }  ; f(b=1,a=2) }");
        assertEval("{ g <- function(a,b,aa,bb) { a ; x <<- 10 ; aa ; c(a, aa) } ; f <- function(...) {  g(..., ...) } ; x <- 1; y <- 2; f(x, y) }");
        assertEval("{ f <- function(a, b) { a - b } ; g <- function(...) { f(1, ...) } ; g(a = 2) }");

        assertEvalError("{ f <- function(...) { ..3 } ; f(1,2) }");
        assertEvalError("{ f <- function() { dummy() } ; f() }"); // note:
        // GNU-R has slightly different error code formatting
        assertEvalError("{ f <- function() { if (FALSE) { dummy <- 2 } ; dummy() } ; f() }");
        assertEvalError("{ f <- function() { if (FALSE) { dummy <- 2 } ; g <- function() { dummy() } ; g() } ; f() }");
        assertEvalError("{ f <- function() { dummy <- 2 ; g <- function() { dummy() } ; g() } ; f() }");
        assertEvalError("{ f <- function() { dummy() } ; dummy <- 2 ; f() }");
        assertEvalError("{ dummy <- 2 ; dummy() }");
        assertEvalError("{ lapply(1:3, \"dummy\") }");

        assertEvalError("{ f <- function(a, b) { a + b } ; g <- function(...) { f(a=1, ...) } ; g(a=2) }");
        assertEval("{ f <- function(a, barg, ...) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(b=2,3) }");
        assertEvalError("{ f <- function(a, barg, bextra) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(b=2,3) }");
        assertEval("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(be=2,du=3, 3) }");
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(be=2,bex=3, 3) }");
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ..., x=2) } ; g(1) }");
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ..., x=2,z=3) } ; g(1) }");
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ..., xxx=2) } ; g(1) }");
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, xxx=2, ...) } ; g(1) }");
        assertEval("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...) } ; g(1,2,3) }");
        assertEvalError("{ f <- function(a, barg, bextra, dummy) { a + barg } ; g <- function(...) { f(a=1, ...,,,) } ; g(1) }");
        assertEval("{ f <- function(...,d) { ..1 + ..2 } ; f(1,d=4,2) }");
        assertEval("{ f <- function(...,d) { ..1 + ..2 } ; f(1,2,d=4) }");

        assertEvalError("{ f <- function(...) { ..2 + ..2 } ; f(1,,2) }");
        assertEvalError("{ f <- function(...) { ..1 + ..2 } ; f(1,,3) }");
    }

}
