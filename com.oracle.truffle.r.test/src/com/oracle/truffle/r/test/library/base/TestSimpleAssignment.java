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
package com.oracle.truffle.r.test.library.base;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestSimpleAssignment extends TestBase {

    @Test
    public void testAssign1() {
        assertEval("{ a<-1; a }");
        assertEval("{ a<-1; a<-a+1; a }");
    }

    @Test
    public void testAssign2() {
        assertEval("a <- 42; f <- function() { a <- 13; a <<- 37; }; f(); a;");
    }

    @Test
    public void testAssignShadowBuiltin1() {
        assertEval("f <- function(b) { c <- function(x,y) 42; c(1,1); }; f(0); f(1)");
        assertEval("f <- function(b) { if (b) c <- function(x,y) 42; c(1,1); }; f(0); f(1)");
    }

    @Test
    public void testAssignFunctionLookup1() {
        assertEval("f <- function(b) { c <- 42; c(1,1); }; f(0); f(1)");
        assertEval("f <- function(b) { if (b) c <- 42; c(1,1); }; f(0); f(1)");
    }

    @Test
    public void testAssignPoly1() {
        assertEval("test <- function(b) { if (b) f <- function() { 42 }; g <- function() { if (!b) f <- function() { 43 }; f() }; g() }; c(test(FALSE), test(TRUE))");
    }

    @Test
    public void testAssign() {
        assertEval("{ a<-1 }");
        assertEval("{ a<-FALSE ; b<-a }");
        assertEval("{ x = if (FALSE) 1 }");
    }

    @Test
    public void testSuperAssign() {
        assertEval("{ f <- function() { x <<- 2 } ; f() ; x }");
        assertEval("{ x <- 10 ; f <- function() { x <<- 2 } ; f() ; x }");
        assertEval("{ x <- 10 ; f <- function() { x <<- 2 ; x } ; c(f(), f()) }");
        assertEval("{ x <- 10 ; f <- function() { x <- x ; x <<- 2 ; x } ; c(f(), f()) }");
        assertEval("{ x <- 10 ; g <- function() { f <- function() { x <- x ; x <<- 2 ; x } ; c(f(), f()) } ; g() }");
        assertEval("{ x <- 10 ; g <- function() { x ; f <- function() { x <- x ; x <<- 2 ; x } ; c(f(), f()) } ; g() }");
        assertEval("{ x <- 10 ; g <- function() { x <- 100 ; f <- function() { x <- x ; x <<- 2 ; x } ; c(f(), f()) } ; g() }");
        assertEval("{ h <- function() { x <- 10 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { x <<- 3 ; x } ; f() } ; g() } ; h() }");

        assertEval("{ b <- 2 ; f <- function() { b <- 4 } ; f() ; b }");
        assertEval("{ b <- 2 ; f <- function() { b <<- 4 } ; f() ; b }");
        assertEval("{ b <- 2 ; f <- function() { b[2] <- 4 } ; f() ; b }");
        assertEval("{ b <- 2 ; f <- function() { b[2] <<- 4 } ; f() ; b }");
        assertEval("{ a <- c(1,2,3) ; f <- function() { a[2] <<- 4 } ; f() ; a }");
        assertEval("{ f <- function(a) { g <- function(x,y) { a[x] <<- y } ; g(2,4) ; a } ; u <- c(1,2,3) ; k <- f(u) ; u <- c(3,2,1) ; l <- f(u) ; list(k,l) }");
        assertEval("{ b <- c(1,1) ; f <- function(v,x) { g <- function(y) { v[y] <<- 2 } ; g(x) ; v } ; k <- f(b,1) ; l <- f(b,2) ; list(k,l,b) }");
        assertEval("{ a <- c(0,0,0) ; u <- function() { a <- c(1,1,1) ; f <- function() { g <- function() { a[2] <<- 9 } ; g() } ; f() ; a } ; list(a,u()) }");
        assertEval("{ a <- c(0,0,0) ; f <- function() { g <- function() { a[2] <<- 9 } ; g() } ; u <- function() { a <- c(1,1,1) ; f() ; a } ; r <- a ; s <- u() ; t <- a ; list(r,s,t) }");

        assertEval("{ answer <<- 42 }");

        assertEval("{ x <<- 1 }");
        assertEval("{ x <<- 1 ; x }");
        assertEval("{ a <- c(1,2,3) ; f <- function() { a[2] <- 4 } ; list(f(),a) }");
    }

    @Test
    public void testEmptyName() {
        assertEval("{ \"\" <- 123 }");
        assertEval("{ '' <- 123 }");
        assertEval("{ `` <- 123 }");
        assertEval("{ `` + 123 }");
        assertEval("{ 123 + `` }");
    }

    @Test
    public void testAssignBuiltin() {
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; h <- function() { assign(\"z\", 5) ; g <- function() { x <<- 10 ; x } ; g() } ; h() } ; f() ; x }");
    }

    @Test
    public void testDynamic() {
        assertEval("{ l <- quote(x <- 1) ; f <- function() { eval(l) } ; x <- 10 ; f() ; x }");
        assertEval("{ l <- quote(x <- 1) ; f <- function() { eval(l) ; x <<- 10 ; get(\"x\") } ; f() }");
    }

    @Test
    public void testMisc() {
        // some tests are just for corner cases of lookup, not necessarily with assignment
        assertEval("{ nonexistent }");
        assertEval("{ f <- function(i) { if (i==1) { x <- 1 } ; x } ; f(1) ; f(2) }");
        assertEval("{ f <- function(i) { if (i==1) { c <- 1 } ; c } ; f(1) ; typeof(f(2)) }");
        assertEval("{ f <- function(i) { if (i==1) { x <- 1 } ; x } ; f(1) ; f(1) ; f(2) }");
        assertEval("{ f <- function(i) { if (i==1) { c <- 1 ; x <- 1 } ; if (i!=2) { x } else { c }} ; f(1) ; f(1) ; typeof(f(2)) }");
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; g <- function() { assign(\"y\", 3) ; hh <- function() { assign(\"z\", 6) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x } ; h() } ; hh() } ; g()  } ; f() }");
        assertEval("{ f <- function() { if (FALSE) { x <- 1 } ; g <- function() { x } ; g() } ; f() }");
        assertEval("{ f <- function() { if (FALSE) { c <- 1 } ; g <- function() { c } ; g() } ; typeof(f()) }");
        assertEval("{ `f<-` <- function(x, y=42, value) { x[1]<-value+y; x }; y<-1:10; f(y)<-7; y }");

        assertEval("proto <- c(11,11); f <- function() { for (i in 1:2) proto[[i]] <- i; proto }; f()");
        assertEval("proto <- c(11,11); f <- function() { for (i in 1:2) print(proto[[i]] <- i); proto }; f()");
        assertEval("proto <- c(11,11); f <- function() { proto <- c(4,5); for (i in 1:2) proto[[i]] <- i; proto }; f()");
        assertEval("proto <- c(11,11); f <- function() { proto <- c(4,5); for (i in 1:2) print(proto[[i]] <- i); proto }; f()");
        assertEval("proto <- c(11,11); f <- function() { for (i in 1:2) proto[[i]] <<- i; proto }; f()");
        assertEval("proto <- c(11,11); f <- function() { for (i in 1:2) print(proto[[i]] <<- i); proto }; f()");
        assertEval("proto <- c(11,11); f <- function() { proto <- c(4,5); for (i in 1:2) proto[[i]] <<- i; proto }; f()");
        assertEval("proto <- c(11,11); f <- function() { proto <- c(4,5); for (i in 1:2) print(proto[[i]] <<- i); proto }; f()");
    }
}
