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

import com.oracle.truffle.r.test.ArithmeticWhiteList;
import com.oracle.truffle.r.test.TestBase;

public class TestSimpleArithmetic extends TestBase {

    @Test
    public void testScalarsReal() {
        assertEval("{ 1L+1 }");
        assertEval("{ 1L+1L }");
        assertEval("{ ( 1+1)*(3+2) }");
        assertEval("{ 1+TRUE }");
        assertEval("{ 1L+TRUE }");
        assertEval("{ 1+FALSE<=0 }");
        assertEval("{ 1L+FALSE<=0 }");
        assertEval("{ TRUE+TRUE+TRUE*TRUE+FALSE+4 }");
        assertEval("{ 1L*NA }");
        assertEval("{ 1+NA }");
        assertEval("{ 2L^10L }");
        assertEval("{ 0x10 + 0x10L + 1.28 }");

        assertEval("{ 1000000000*100000000000 }");
        assertEval("{ 1000000000L*1000000000 }");
        assertEval(Output.MissingWarning, "{ 1000000000L*1000000000L }"); // FIXME missing warning
    }

    @Test
    public void testIntegerDivision() {
        assertEval("{ 3 %/% 2 }");
        assertEval("{ 3L %/% 2L }");
        assertEval("{ 3L %/% -2L }");
        assertEval("{ 3 %/% -2 }");
        assertEval("{ 3 %/% 0 }");
    }

    @Test
    public void testModulo() {
        assertEval("{ 3 %% 2 }");
        assertEval("{ 3L %% 2L }");
        assertEval("{ 3L %% -2L }");
        assertEval("{ 3 %% -2 }");
        assertEval("{ 3 %% 0 }");
    }

    @Test
    public void testExponentiation() {
        assertEval("{ 1^(1/0) }");
        assertEval("{ (-2)^(1/0) }");
        assertEval("{ (-2)^(-1/0) }");
        assertEval("{ (1)^(-1/0) }");
        assertEval("{ 0^(-1/0) }");
        assertEval("{ 0^(1/0) }");
        assertEval("{ 0^(0/0) }");
        assertEval("{ 1^(0/0) }");
        assertEval("{ (-1)^(0/0) }");
        assertEval("{ (-1/0)^(0/0) }");
        assertEval("{ (1/0)^(0/0) }");
        assertEval("{ (0/0)^(1/0) }");
        assertEval("{ (-1/0)^3 }");
        assertEval("{ (1/0)^(-4) }");
        assertEval("{(-1/0)^(-4) }");
    }

    @Test
    public void testVectorsEmptyResult() {
        assertEval("{ integer()+1 }");
        assertEval("{ 1+integer() }");
    }

    @Test
    public void testVectorsNA() {
        assertEval("{ 1 + c(1L, NA, 3L) }");
        assertEval("{ NA + c(1, 2, 3) }");
        assertEval("{ c(1, 2, 3) + NA }");
        assertEval("{ NA+1:3 }");
        assertEval("{ 1:3+NA }");
        assertEval("{ NA+c(1L, 2L, 3L) }");
        assertEval("{ c(1L, 2L, 3L)+NA }");
        assertEval("{ c(NA,NA,NA)+1:3 }");
        assertEval("{ 1:3+c(NA, NA, NA) }");
        assertEval("{ c(NA,NA,NA)+c(1L,2L,3L) }");
        assertEval("{ c(1L,2L,3L)+c(NA, NA, NA) }");
        assertEval("{ c(NA,NA)+1:4 }");
        assertEval("{ 1:4+c(NA, NA) }");
        assertEval("{ c(NA,NA,NA,NA)+1:2 }");
        assertEval("{ 1:2+c(NA,NA,NA,NA) }");
        assertEval("{ c(NA,NA)+c(1L,2L,3L,4L) }");
        assertEval("{ c(1L,2L,3L,4L)+c(NA, NA) }");
        assertEval("{ c(NA,NA,NA,NA)+c(1L,2L) }");
        assertEval("{ c(1L,2L)+c(NA,NA,NA,NA) }");
        assertEval("{ c(1L,NA)+1 }");
        assertEval("{ c(1L,NA) + c(2,3) }");
        assertEval("{ c(2,3) + c(1L,NA)}");
    }

    @Test
    public void testScalarsComplexIgnore() {
        assertEval("{ (1+2i)^(-2) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ ((1+0i)/(0+0i)) ^ (-3) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ ((1+1i)/(0+0i)) ^ (-3) }");
    }

    @Test
    public void testScalarsComplex() {
        assertEval("{ (1+2i)*(3+4i) }");
        assertEval("{ x <- 1+2i; y <- 3+4i; x*y }");
        assertEval("{ x <- 1+2i; y <- 3+4i; x-y }");
        assertEval("{ x <- c(-1.5-1i,-1.3-1i) ; y <- c(0+0i, 0+0i) ; y*y+x }");
        assertEval("{ x <- c(-1.5-1i,-1.3-1i) ; y <- c(0+0i, 0+0i) ; y-x }");
        assertEval("{ x <- 1+2i; y <- 3+4i; x/y }");
        assertEval("{ x <- c(-1-2i,3+10i) ; y <- c(3+1i, -4+5i) ; y-x }");
        assertEval("{ (1+2i)^2 }");
        assertEval("{ (1+2i)^0 }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ 1/((1+0i)/(0+0i)) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(1+2i, 3+4i) ; f(1, 2) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(2, 3+4i) ; f(1, 2) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(1+2i, 3) ; f(1, 2) }");
        assertEval("{ f <- function(b) { b / 4i } ; f(1) ; f(1L) }");
        assertEval("{ f <- function(b) { 1i / b } ; f(1) ; f(1L) ; f(4) }");
        assertEval("{ f <- function(b) { 1i / b } ; f(1+1i) ; f(1L) }");
        assertEval("{ f <- function(b) { 1i / b } ; f(1) ; f(1L) }");
        assertEval("{ f <- function(b) { 1i / b } ; f(TRUE) ; f(1L) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1+2i) ; f(TRUE, 2)  }");
        assertEval("{ f <- function(b) { 1 / b } ; f(1+1i) ; f(1L)  }");
        assertEval("{ f <- function(b) { b / 2 } ; f(1+1i) ; f(1L)  }");
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2+1i,(1:2)[3]) }");
        assertEval("{ (0+2i)^0 }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ (1+2i) / ((0-1i)/(0+0i)) }");
        assertEval("{ (3+2i)^2 }");
        assertEval("{ x <- 1+2i; y <- 3+4i; round(x*x*y/(x+y), digits=5) }");
        assertEval("{ round( (1+2i)^(3+4i), digits=5 ) }");
        assertEval("{ round( ((1+1i)/(0+1i)) ^ (-3.54), digits=5) }");
        assertEval("{ c(1+2i,1.1+2.1i) }");
        assertEval("{ c(1+2i,11.1+2.1i) }");
        assertEval("{ c(1+2i,1.1+12.1i) }");
        assertEval("{ c(11+2i,1.1+2.1i) }");
        assertEval("{ c(1+12i,1.1+2.1i) }");
        assertEval("{ c(-1+2i,1.1+2.1i) }");
        assertEval("{ c(1-2i,1+22i) }");
        assertEval("{ x <- c(-1-2i,3+10i) ; y <- c(3+1i, -4+5i) ; round(y/x, digits=5) }");
        assertEval("{ x <- c(-1-2i,3+10i) ; y <- c(3+1i, -4+5i) ; y+x }");
        assertEval("{ x <- c(-1-2i,3+10i) ; y <- c(3+1i, -4+5i) ; y*x }");
    }

    @Test
    public void testComplexNaNInfinity() {
        assertEval("{ 0^(-1+1i) }");
        assertEval("{ (0+0i)/(0+0i) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ (1+0i)/(0+0i) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ (0+1i)/(0+0i) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ (1+1i)/(0+0i) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ (-1+0i)/(0+0i) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ (-1-1i)/(0+0i) }");
        assertEval("{ (1+2i) / ((0-0i)/(0+0i)) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ ((0+1i)/0) * ((0+1i)/0) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ ((0-1i)/0) * ((0+1i)/0) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ ((0-1i)/0) * ((0-1i)/0) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ ((0-1i)/0) * ((1-1i)/0) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ ((0-1i)/0) * ((-1-1i)/0) }");
        assertEval("{ 0/0 - 4i }");
        assertEval("{ 4i + 0/0  }");
        assertEval("{ a <- 1 + 2i; b <- 0/0 - 4i; a + b }");
    }

    @Test
    public void testScalars() {
        assertEval("{ 1L / 2L }");
        assertEval("{ f <- function(a, b) { a / b } ; f(1L, 2L) ; f(1, 2) }");
        assertEval("{ (1:2)[3] / 2L }");
        assertEval("{ 2L / (1:2)[3] }");
        assertEval("{ a <- (1:2)[3] ; b <- 2L ; a / b }");
        assertEval("{ a <- 2L ; b <- (1:2)[3] ; a / b }");
        assertEval("{ (1:2)[3] + 2L }");
        assertEval("{ 2L + (1:2)[3] }");
        assertEval("{ a <- (1:2)[3] ; b <- 2L ; a + b }");
        assertEval("{ a <- 2L ; b <- (1:2)[3] ; a + b }");
        assertEval("{ a <- (1:2)[3] ; b <- 2 ; a + b }");
        assertEval("{ a <- 2 ; b <- (1:2)[3] ; a + b }");

        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2L,4) }");
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2L,4L) }");
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2L,(1:2)[3]) }");
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f((1:2)[3], 2L) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1L) ; f(2L,4) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1L) ; f(2L,4L) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1L) ; f(2L,(1:2)[3]) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(1,1) ; f(1,1L) ; f((1:2)[3], 2L) }");
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f(2,(1:2)[3]) }");
        assertEval("{ f <- function(a, b) { a / b } ; f(1,1) ; f(1,1L) ; f((1:2)[3],2) }");

        assertEval("{ f <- function(b) { 1 / b } ; f(1) ; f(1L) ; f(4) }");
        assertEval("{ f <- function(b) { 1 / b } ; f(1) ; f(1L) }");
        assertEval("{ f <- function(b) { 1 / b } ; f(1L) ; f(1) }");
        assertEval("{ f <- function(b) { 1 / b } ; f(TRUE) ; f(1L) }");
        assertEval("{ f <- function(b) { b / 1 } ; f(1) ; f(1L) ; f(4) }");
        assertEval("{ f <- function(b) { b / 2 } ; f(1) ; f(1L) }");
        assertEval("{ f <- function(b) { b / 4 } ; f(1L) ; f(1) }");
        assertEval("{ f <- function(b) { 4L / b } ; f(1L) ; f(2) }");
        assertEval("{ f <- function(b) { 4L + b } ; f(1L) ; f(2) }");
        assertEval("{ f <- function(b) { b / 2L } ; f(1L) ; f(2) }");

        assertEval("{ f <- function(b) { 4L / b } ; f(1L) ; f(2) ; f(TRUE) }");
        assertEval("{ f <- function(b) { 4L + b } ; f(1L) ; f(2) ; f(TRUE) }");
        assertEval("{ f <- function(b) { 4L + b } ; f(1L) ; f(2) ; f((1:2)[3]) }");
        assertEval("{ f <- function(b) { 4L / b } ; f(1L) ; f(2) ; f((1:2)[3]) }");
        assertEval("{ f <- function(b) { (1:2)[3] + b } ; f(1L) ; f(2) }");
        assertEval("{ f <- function(b) { (1:2)[3] + b } ; f(1) ; f(2L) }");
        assertEval("{ f <- function(b) { b + 4L } ; f(1L) ; f(2) ; f(TRUE) }");
        assertEval("{ f <- function(b) { b + 4L } ; f(1L) ; f(2) ; f((1:2)[3]) }");
        assertEval("{ f <- function(b) { b / 4L } ; f(1L) ; f(2) ; f(TRUE) }");
        assertEval("{ f <- function(b) { b / 4L } ; f(1L) ; f(2) ; f((1:2)[3]) }");
        assertEval("{ f <- function(b) { 1 + b } ; f(1L) ; f(TRUE) }");
        assertEval("{ f <- function(b) { FALSE + b } ; f(1L) ; f(2) }");
        assertEval("{ f <- function(b) { b + 1 } ; f(1L) ; f(TRUE) }");
        assertEval("{ f <- function(b) { b + FALSE } ; f(1L) ; f(2) }");
    }

    @Test
    public void testScalarsRange() {
        assertEval("{ f <- function(a, b) { a + b } ; f(c(1,2), c(3,4)) ; f(c(1,2), 3:4) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(1:2, c(3,4)) ; f(c(1,2), 3:4) }");
        assertEval("{ f <- function(a, b) { a + b } ; f(1:2, 3:4) ; f(c(1,2), 3:4) }");
    }

    @Test
    public void testVectors() {
        assertEval("{ x<-c(1,2,3);x }");
        assertEval("{ x<-c(1,2,3);x*2 }");
        assertEval("{ x<-c(1,2,3);x+2 }");
        assertEval("{ x<-c(1,2,3);x+FALSE }");
        assertEval("{ x<-c(1,2,3);x+TRUE }");
        assertEval("{ x<-c(1,2,3);x*x+x }");
        assertEval("{ x<-c(1,2);y<-c(3,4,5,6);x+y }");
        assertEval("{ x<-c(1,2);y<-c(3,4,5,6);x*y }");
        assertEval("{ x<-c(1,2);z<-c();x==z }");
        assertEval("{ x<-1+NA; c(1,2,3,4)+c(x,10) }");
        assertEval("{ c(1L,2L,3L)+TRUE }");
        assertEval("{ c(1L,2L,3L)*c(10L) }");
        assertEval("{ c(1L,2L,3L)*c(10,11,12) }");
        assertEval("{ c(1L,2L,3L,4L)-c(TRUE,FALSE) }");
        assertEval("{ ia<-c(1L,2L);ib<-c(3L,4L);d<-c(5,6);ia+ib+d }");
    }

    @Test
    public void testVectorsRanges() {
        assertEval("{ 1L + 1:2 }");
        assertEval("{ 4:3 + 2L }");
        assertEval("{ 1:2 + 3:4 }");
        assertEval("{ 1:2 + c(1L, 2L) }");
        assertEval("{ c(1L, 2L) + 1:4 }");
        assertEval("{ 1:4 + c(1L, 2L) }");
        assertEval("{ 2L + 1:2 }");
        assertEval("{ 1:2 + 2L }");
        assertEval("{ c(1L, 2L) + 2L }");
        assertEval("{ 2L + c(1L, 2L) }");
        assertEval("{ 1 + 1:2 }");
        assertEval("{ c(1,2) + 1:2 }");
        assertEval("{ c(1,2,3,4) + 1:2 }");
        assertEval("{ c(1,2,3,4) + c(1L,2L) }");
        assertEval("{ 1:2 + 1 }");
        assertEval("{ 1:2 + c(1,2) }");
        assertEval("{ 1:2 + c(1,2,3,4) }");
        assertEval("{ c(1L,2L) + c(1,2,3,4) }");
        assertEval("{ 1L + c(1,2) }");
        assertEval("{ 1:4+c(1,2) }");
        assertEval("{ c(1,2)+1:4 }");
    }

    @Test
    public void testVectorsOperations() {
        assertEval("{ a <- c(1,3) ; b <- c(2,4) ; a ^ b }");
        assertEval("{ a <- c(1,3) ; a ^ 3 }");
        assertEval("{ c(1,3) - 4 }");
        assertEval("{ c(1,3) %/% c(2,4) }");
        assertEval("{ c(1,3) / c(2,4) }");
    }

    @Test
    public void testVectorsOperationsComplex() {
        assertEval("{ a <- c(1+1i,3+2i) ; a - (4+3i) }");
        assertEval("{ c(1+1i,3+2i) * c(1,2) }");
        assertEval("{ z <- c(1+1i,3+2i) ; z * c(1,2) }");
        assertEval("{ round(c(1+1i,2+3i)^c(1+1i,3+4i), digits = 5) }");
        assertEval("{ round( 3^c(1,2,3+1i), digits=5 ) }");
        assertEval("{ z <- c(-1.5-1i,10) ; (z * z)[1] }");
        assertEval("{ c(1+1i,3+2i) / 2 }");
        assertEval("{ c(1,2,3+1i)^3 }");
    }

    @Test
    public void testVectorsComplex() {
        assertEval("{ 1:4+c(1,2+2i) }");
        assertEval("{ c(1,2+2i)+1:4 }");
        assertEval(ArithmeticWhiteList.WHITELIST, "x <- c(NaN, 3+2i); xre <- Re(x); xim <- (0+1i) * Im(x); xre + xim");
    }

    @Test
    public void testVectorsModulo() {
        assertEval("{ c(3,4) %% 2 }");
        assertEval("{ c(3,4) %% c(2,5) }");
    }

    @Test
    public void testVectorsIntegerDivision() {
        assertEval("{ c(3,4) %/% 2 }");
    }

    @Test
    public void testVectorsLengthWarning() {
        assertEval("{ 1:2+1:3 }");
        assertEval("{ 1:3*1:2 }");
        assertEval("{ 1:3+c(1,2+2i) }");
        assertEval("{ c(1,2+2i)+1:3 }");
    }

    @Test
    public void testVectorsNonConformable() {
        assertEval("{ x <- 1:2 ; dim(x) <- 1:2 ; y <- 2:3 ; dim(y) <- 2:1 ; x + y }");
        assertEval("{ x <- 1:2 ; dim(x) <- 1:2 ; y <- 2:3 ; dim(y) <- c(1,1,2) ; x + y }");
    }

    @Test
    public void testVectorsMatrixDimsDontMatch() {
        assertEval(Output.IgnoreErrorContext, "{ m <- matrix(nrow=2, ncol=2, 1:4) ; m + 1:16 }");
    }

    @Test
    public void testUnaryNot() {
        assertEval("{ !TRUE }");
        assertEval("{ !FALSE }");
        assertEval("{ !NA }");
    }

    @Test
    public void testUnaryNotVector() {
        assertEval("{ !c(TRUE,TRUE,FALSE,NA) }");
        assertEval("{ !c(1,2,3,4,0,0,NA) }");
        assertEval("{ !((0-3):3) }");
    }

    @Test
    public void testUnaryNotRaw() {
        assertEval("{ f <- function(arg) { !arg } ; f(as.raw(10)) ; f(as.raw(1:3)) }");
        assertEval("{ a <- as.raw(201) ; !a }");
        assertEval("{ a <- as.raw(12) ; !a }");
        assertEval("{ f <- function(arg) { !arg } ; f(as.raw(10)) ; f(as.raw(c(a=1,b=2))) }");
        assertEval("{ l <- list(); !l }");
        assertEval("{ f <- function(arg) { !arg } ; f(as.raw(10)) ; f(matrix(as.raw(1:4),nrow=2 )) }");
        assertEval("{ f <- function(arg) { !arg } ; f(as.raw(10)) ; x <- as.raw(10:11) ; attr(x, \"my\") <- 1 ; f(x) }");
    }

    @Test
    public void testUnaryNotError() {
        assertEval("{ l <- c(\"hello\", \"hi\") ; !l }");
        assertEval("{ l <- function(){1} ; !l }");
        assertEval("{ l <- list(1); !l }");
        assertEval("{ x<-1:4; dim(x)<-c(2, 2); names(x)<-101:104; attr(x, \"dimnames\")<-list(c(\"201\", \"202\"), c(\"203\", \"204\")); attr(x, \"foo\")<-\"foo\"; y<-!x; attributes(y) }");
    }

    @Test
    public void testUnaryNotPropagate() {
        // list of sequences should be converted to list of string vectors
        assertEval("{ x<-1:4; dim(x)<-c(2, 2); names(x)<-101:104; attr(x, \"dimnames\")<-list(201:202, 203:204); attr(x, \"foo\")<-\"foo\"; y<-!x; attributes(y) }");
    }

    @Test
    public void testUnaryNotDimensions() {
        assertEval("{ xx <- double(0); dim(xx) <- c(0,0); dim(!xx) }");
        assertEval("{ xx <- double(1); dim(xx) <- c(1,1); dim(!xx) }");
    }

    @Test
    public void testUnaryMinus() {
        assertEval("{ -3 }");
        assertEval("{ --3 }");
        assertEval("{ ---3 }");
        assertEval("{ ----3 }");
        assertEval("{ -(0/0) }");
        assertEval("{ -(1/0) }");
    }

    @Test
    public void testUnaryMinusVector() {
        assertEval("{ -(1[2]) }");
    }

    @Test
    public void testUnaryMinusComplex() {
        assertEval("{ -(2+1i)  }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ -((0+1i)/0)  }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ -((1+0i)/0)  }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ -c((1+0i)/0,2) }");
    }

    @Test
    public void testUnaryMinusAsFunction() {
        assertEval("{ f <- function(z) { -z } ; f(TRUE) ; f(1L) }");
        assertEval("{ f <- function(z) { -z } ; f(1L) ; f(1) }");
        assertEval("{ f <- function(z) { -z } ; f(1) ; f(1L) }");
        assertEval("{ f <- function(z) { -z } ; f(1L) ; f(TRUE) }");
        assertEval("{ z <- logical() ; -z }");
        assertEval("{ z <- integer() ; -z }");
        assertEval("{ z <- double() ; -z }");
        assertEval("{ f <- function(z) { -z } ; f(1:3) ; f(1L) }");
        assertEval("{ f <- function(z) { -z } ; f(1:3) ; f(TRUE) }");
    }

    @Test
    public void testUnaryMinusAsFunctionComplex() {
        assertEval("{ f <- function(z) { -z } ; f(1L) ; f(1+1i) }");
        assertEval("{ f <- function(z) { -z } ; f(1+1i) ; f(1L) }");
        assertEval("{ z <- (1+1i)[0] ; -z }");
        assertEval("{ f <- function(z) { -z } ; f(1:3) ; f(c((0+0i)/0,1+1i)) }");
    }

    @Test
    public void testUnaryMinusErrors() {
        assertEval("{ z <- \"hello\" ; -z }");
        assertEval("{ z <- c(\"hello\",\"hi\") ; -z }");
        assertEval("{ f <- function(z) { -z } ; f(1:3) ; f(\"hello\") }");
    }

    @Test
    public void testUnaryMinusDimensions() {
        assertEval("{ xx <- double(0); dim(xx) <- c(0,0); dim(-xx) }");
        assertEval("{ xx <- double(1); dim(xx) <- c(1,1); dim(-xx) }");
    }

    @Test
    public void testMatrices() {
        assertEval("{ m <- matrix(1:6, nrow=2, ncol=3, byrow=TRUE) ; m-1 }");
        assertEval("{ z<-matrix(12)+1 ; z }");
        assertEval("{ m <- matrix(1:6, nrow=2, ncol=3, byrow=TRUE) ; m+1L }");
        assertEval("{ m <- matrix(1:6, nrow=2, ncol=3, byrow=TRUE) ; m+m }");
    }

    @Test
    public void testMatricesProduct() {
        assertEval("{ double() %*% double() }");
        assertEval("{ m <- double() ; dim(m) <- c(0,4) ; m %*% t(m) }");
        assertEval("{ m <- double() ; dim(m) <- c(0,4) ; t(m) %*% m }");
        assertEval("{ m <- double() ; dim(m) <- c(0,4) ; n <- matrix(1:4,4) ; m %*% n }");
        assertEval("{ m <- double() ; dim(m) <- c(4,0) ; n <- matrix(1:4,ncol=4) ; n %*% m }");
        assertEval("{ x <- 1:3 %*% 9:11 ; x[1] }");
        assertEval("{ m<-matrix(1:3, nrow=1) ; 1:2 %*% m }");
        assertEval("{ m<-matrix(1:6, nrow=2) ; 1:2 %*% m }");
        assertEval("{ m<-matrix(1:6, nrow=2) ; m %*% 1:3 }");
        assertEval("{ m<-matrix(1:3, ncol=1) ; m %*% 1:2 }");
        assertEval("{ a<-matrix(1:6, ncol=2) ; b<-matrix(11:16, nrow=2) ; a %*% b }");
        assertEval("{ a <- array(1:9, dim=c(3,1,3)) ;  a %*% 1:9 }");
        assertEval("{ matrix(2,nrow=2,ncol=3) %*% matrix(4,nrow=1,ncol=5) }");
        assertEval("{ 1:3 %*% matrix(4,nrow=2,ncol=5) }");
        assertEval("{ matrix(4,nrow=2,ncol=5) %*% 1:4 }");
        assertEval("{ m <- matrix(c(1,2,3,0/0), nrow=4) ; m %*% 1:4 }");
        assertEval("{ m <- matrix(c(NA,1,0/0,2), nrow=2) ; 1:2 %*% m }");
        assertEval("{ m <- double() ; dim(m) <- c(0,0) ; m %*% m }");
        assertEval("{ m <- matrix(c(NA,1,4,2), nrow=2) ; t(m) %*% m }");
        assertEval("{ matrix(c(3,1,0/0,2), nrow=2) %*% matrix(1:6,nrow=2) }");
        assertEval("{ as.raw(1:3) %*% 1:3 }");
        assertEval("{ options(matprod = 'blas'); matrix(c(NaN,1,7,2,4,NA), nrow=3) %*% matrix(c(3,1,NA,2,NaN,5,6,7), nrow=2) }");
        // NaN vs. NA issue
        assertEval(Ignored.Unknown, "{ c(1,2,NA,NaN) %*% c(1,3,3,4) }");
        assertEval(Ignored.Unknown, "{ c(1,2,NaN,NA) %*% c(1,3,3,4) }");
        assertEval(Ignored.Unknown, "{ c(1,2,2,3) %*% c(1,3,NA,NaN) }");
        assertEval("{ c(1,2,2,3) %*% c(1,3,NaN,NA) }");
    }

    @Test
    public void testMatricesOuterProduct() {
        assertEval("{ 1:3 %o% 1:2 }");
        assertEval("{ 1:4 %*% 1:3 }");
        assertEval("{ 1:3 %*% as.raw(c(1,2,3)) }");
        assertEval("{ 1:3 %*% c(TRUE,FALSE,TRUE) }");
        assertEval("{ as.raw(1:3) %o% 1:3 }");
    }

    @Test
    public void testMatricesPrecedence() {
        assertEval("{ 10 / 1:3 %*% 3:1 }");
        assertEval("{ x <- 1:2 ; dim(x) <- c(1,1,2) ; y <- 2:3 ; dim(y) <- c(1,1,2) ; x + y }");
    }

    @Test
    public void testNonvectorizedLogicalOr() {
        assertEval("{ 1.1 || 3.15 }");
        assertEval("{ 0 || 0 }");
        assertEval("{ 1 || 0 }");
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; TRUE || f(FALSE) ; x } ");

        assertEval("{ NA || 1 }");
        assertEval("{ 0 || NA }");
        assertEval("{ NA || 0 }");
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; NA || f(NA) ; x }");
    }

    public void testNonvectorizedLogicalOrAsFunction() {
        assertEval("{ f <- function(a,b) { a || b } ; f(1,2) ; f(1,2) ; f(1L,2L) }");
        assertEval("{ f <- function(a,b) { a || b } ; f(1L,2L) ; f(1L,2L) ; f(0,FALSE) }");
    }

    @Test
    public void testNonvectorizedLogicalAnd() {
        assertEval("{ TRUE && FALSE }");
        assertEval("{ FALSE && FALSE }");
        assertEval("{ FALSE && TRUE }");
        assertEval("{ TRUE && TRUE }");
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; FALSE && f(FALSE) ; x } ");
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; c(FALSE, TRUE) && f(FALSE) ; x } ");
        assertEval("{ TRUE && NA }");
        assertEval("{ FALSE && NA }");
        assertEval("{ NA && TRUE }");
        assertEval("{ NA && FALSE }");
        assertEval("{ NA && NA }");
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; NA && f(NA) ; x } ");

        assertEval("{ TRUE && c(TRUE, FALSE) }");
        assertEval("{ c(TRUE, FALSE) && c(TRUE, FALSE) }");
        assertEval("{ c(TRUE, FALSE) && c(TRUE, FALSE, FALSE) }");
        assertEval("{ c(1.0, 0.0) && 1.0 }");
        assertEval("{ c(1, 0) && 1 }");
        assertEval("{ c(1.1, 0.0) && c(TRUE, FALSE) }");
        assertEval("{ c(1, 0) && 1+1i }");
        assertEval("{ c(1+1i, 0+0i) && 1 }");
        assertEval("{ 1.0 && c(1+1i, 0+0i) }");
        assertEval("{ c(1+1i, 0+0i) && c(1+1i, 0+0i) }");
        assertEval("{ c(\"1\", \"0\") && TRUE }");
        assertEval("{ c(1, 0) && \"1\" }");
        assertEval("{ \"1\" && c(1, 0) }");
        assertEval("{ as.raw(c(1, 0)) && TRUE }");
    }

    @Test
    public void testNonvectorizedLogicalAndAsFunction() {
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), TRUE) }");
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) }");
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(1,2) }");
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(double(),2) }");
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(integer(),2) }");
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(2+3i,1/0) }");
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(2+3i,logical()) }");
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(1:3,4:10) ; f(1,2) ; f(logical(),4) }");
        assertEval("{ f <- function(a,b) { a && b } ;  f(c(TRUE, FALSE), logical()) ; f(TRUE, c(TRUE,TRUE,FALSE)) ; f(1,2) }");
    }

    @Test
    public void testNonvectorizedLogicalSpecialChecks() {
        assertEval("{ FALSE && \"hello\" }");
        assertEval("{ TRUE || \"hello\" }");
        assertEval("{ \"hello\" || TRUE }");
        assertEval("{ FALSE || \"hello\" }");

        assertEval("{ 0 && \"hello\" }");
        assertEval("{ 0.0 && \"hello\" }");
        assertEval("{ 1+2i && 0 }");
        assertEval("{ 1+2i && TRUE }");
        assertEval("{ TRUE && 0+0i}");
        assertEval("{ 1.0 && 0+0i}");
        assertEval("{ 1 && \"hello\" }");
        assertEval("{ 0.1 && \"hello\" }");
        assertEval("{ TRUE && \"hello\" }");
        assertEval("{ \"hello\" && TRUE }");
        assertEval("{ \"hello\" && 1 }");
        assertEval("{ \"hello\" && 1L }");
        assertEval("{ NULL && 1 }");
        assertEval("{ 0.1 && NULL }");
        assertEval("{ as.raw(1) && 1 }");
        assertEval("{ 0.1 && as.raw(1) }");
        assertEval("{ logical(0) && logical(0) }");
        assertEval("{ logical(0) && TRUE }");
        assertEval("{ logical(0) && FALSE }");
        assertEval("{ character(0) && FALSE }");
        assertEval("{ character(0) && TRUE }");

        assertEval("{ 1 || \"hello\" }");
        assertEval("{ FALSE || 1+2i }");
        assertEval("{ 0+0i || FALSE}");
        assertEval("{ 1.1 || \"hello\" }");
        assertEval("{ 1+2i || 0 }");
        assertEval("{ 1+2i || 1.0 }");
        assertEval("{ 0 || \"hello\" }");
        assertEval("{ 0L || \"hello\" }");
        assertEval("{ \"hello\" || FALSE }");
        assertEval("{ \"hello\" || 1 }");
        assertEval("{ \"hello\" || 1L }");
        assertEval("{ NULL || 1 }");
        assertEval("{ 0 || NULL }");
        assertEval("{ as.raw(1) || 1 }");
        assertEval("{ 0 || as.raw(1) }");
        assertEval("{ as.raw(10) && \"hi\" }");
        assertEval("{ c(TRUE,FALSE) | logical() }");
        assertEval("{ logical() | c(TRUE,FALSE) }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ as.raw(c(1,4)) | raw() }");
        assertEval(ArithmeticWhiteList.WHITELIST, "{ raw() | as.raw(c(1,4))}");
        assertEval("{ logical(0) || logical(0) }");
        assertEval("{ logical(0) || TRUE }");
        assertEval("{ logical(0) || FALSE }");
        assertEval("{ character(0) || FALSE }");
        assertEval("{ character(0) || TRUE }");
    }

    @Test
    public void testNonvectorizedLogicalLengthChecks() {
        assertEval("{ as.raw(c(1,4)) | as.raw(c(1,5,4)) }");
        assertEval("{ as.raw(c(1,5,4)) | as.raw(c(1,4)) }");
        assertEval("{ c(TRUE, FALSE, FALSE) & c(TRUE,TRUE) }");
        assertEval("{ c(TRUE, TRUE) & c(TRUE, FALSE, FALSE) }");
        assertEval("{ c(a=TRUE, TRUE) | c(TRUE, b=FALSE, FALSE) }");
    }

    @Test
    public void testVectorizedLogicalOr() {
        assertEval("{ 1.1 | 3.15 }");
        assertEval("{ 0 | 0 }");
        assertEval("{ 1 | 0 }");
        assertEval("{ NA | 1 }");
        assertEval("{ NA | 0 }");
        assertEval("{ 0 | NA }");
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; NA | f(NA) ; x }");
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; TRUE | f(FALSE) ; x }");

        assertEval("{ a <- as.raw(200) ; b <- as.raw(255) ; a | b }");
        assertEval("{ a <- as.raw(200) ; b <- as.raw(1) ; a | b }");

        assertEval("c(TRUE, FALSE) | c(NA, NA)");
    }

    @Test
    public void testVectorizedLogicalOrAsFunction() {
        assertEval("{ f <- function(a,b) { a | b } ; f(c(TRUE, FALSE), FALSE) ; f(1L, 3+4i) }");
        assertEval("{ f <- function(a,b) { a | b } ; f(c(TRUE, FALSE), FALSE) ; f(c(FALSE,FALSE), 3+4i) }");
        assertEval("{ f <- function(a,b) { a | b } ; f(as.raw(c(1,4)), as.raw(3)) ; f(4, FALSE) }");
    }

    @Test
    public void testVectorizedLogicalAnd() {
        assertEval("{ TRUE & FALSE }");
        assertEval("{ FALSE & FALSE }");
        assertEval("{ FALSE & TRUE }");
        assertEval("{ TRUE & TRUE }");
        assertEval("{ TRUE & NA }");
        assertEval("{ FALSE & NA }");
        assertEval("{ NA & TRUE }");
        assertEval("{ NA & FALSE }");
        assertEval("{ NA & NA }");
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; NA & f(NA) ; x }");
        assertEval("{ x <- 1 ; f <- function(r) { x <<- 2; r } ; FALSE & f(FALSE) ; x }");

        assertEval("{ 1:4 & c(FALSE,TRUE) }");
        assertEval("{ a <- as.raw(201) ; b <- as.raw(1) ; a & b }");

        assertEval("{ c(FALSE, NA) & c(NA, NA) }");
    }

    @Test
    public void testVectorizedLogicalAndAsFunction() {
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(FALSE, FALSE) }");
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(as.raw(10), as.raw(11)) }");
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(1L, 0L) }");
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(1L, 0) }");
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(1L, TRUE) }");
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(1L, 3+4i) }");
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, FALSE) ; f(1L, 3+4i) }");
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, FALSE) ; f(TRUE, 3+4i) }");
    }

    @Test
    public void testVectorizedLogicalComplex() {
        assertEval("{ 1+2i | 0 }");
        assertEval("{ 1+2i & 0 }");
    }

    @Test
    public void testVectorizedLogicalTypeCheck() {
        assertEval("{ TRUE | \"hello\" }");
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(as.raw(10), 12) }");
        assertEval("{ f <- function(a,b) { a & b } ; f(TRUE, 1L) ; f(FALSE, as.raw(10)) }");
        assertEval("{ f <- function(a,b) { a | b } ; f(as.raw(c(1,4)), as.raw(3)) ; f(as.raw(4), FALSE) }");
        assertEval("{ f <- function(a,b) { a | b } ; f(as.raw(c(1,4)), as.raw(3)) ; f(FALSE, as.raw(4)) }");
        assertEval("{ f <- function(a,b) { a | b } ; f(as.raw(c(1,4)), 3) }");
        assertEval("{ f <- function(a,b) { a | b } ; f(3, as.raw(c(1,4))) }");
    }

    @Test
    public void testVectorizedLogicalAttributes() {
        assertEval("{ x<-1:4; names(x)<-101:104; x | TRUE }");
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:24; names(y)<-121:124; x | y }");
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; names(y)<-121:128; x | y }");
        assertEval("{ x<-1:4; names(x)<-101:104; attr(x, \"foo\")<-\"foo\"; attributes(x | TRUE) }");
        assertEval("{ x<-1:4; names(x)<-101:104; attr(x, \"foo\")<-\"foo\"; y<-21:24; names(y)<-121:124; attributes(x | y) }");
        // A misalignment error similar to those in TestSimpleVector (testIgnored1-3)
        assertEval(Ignored.ReferenceError, "{ x<-as.raw(1:4); names(x)<-101:104; y<-as.raw(21:24); names(y)<-121:124; x | y }");
        assertEval("{ x<-1:4; y<-21:24; names(y)<-121:124; attributes(x | y) }");
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; names(y)<-121:128;  attributes(y | x) }");
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; attributes(x | y) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); y<-21:28; x | y }");
    }

    @Test
    public void testIntegerOverflow() {
        assertEval(Output.MissingWarning, "{ x <- 2147483647L ; x + 1L }");
        assertEval(Output.MissingWarning, "{ x <- 2147483647L ; x * x }");
        assertEval(Output.MissingWarning, "{ x <- -2147483647L ; x - 2L }");
        assertEval(Output.MissingWarning, "{ x <- -2147483647L ; x - 1L }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ 2147483647L + 1:3 }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ 2147483647L + c(1L,2L,3L) }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ 1:3 + 2147483647L }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ c(1L,2L,3L) + 2147483647L }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ 1:3 + c(2147483647L,2147483647L,2147483647L) }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ c(2147483647L,2147483647L,2147483647L) + 1:3 }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ c(1L,2L,3L) + c(2147483647L,2147483647L,2147483647L) }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ c(2147483647L,2147483647L,2147483647L) + c(1L,2L,3L) }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ 1:4 + c(2147483647L,2147483647L) }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ c(2147483647L,2147483647L) + 1:4 }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ c(1L,2L,3L,4L) + c(2147483647L,2147483647L) }");
        assertEval(Ignored.ImplementationError, Output.IgnoreWarningContext, "{ c(2147483647L,2147483647L) + c(1L,2L,3L,4L) }");
    }

    @Test
    public void testIntegerOverflowNoWarning() {
        assertEval("{ 3L %/% 0L }");
        assertEval("{ 3L %% 0L }");
        assertEval("{ c(3L,3L) %/% 0L }");
        assertEval("{ c(3L,3L) %% 0L }");
    }

    @Test
    public void testArithmeticUpdate() {
        assertEval("{ x <- 3 ; f <- function(z) { if (z) { x <- 1 } ; x <- x + 1L ; x } ; f(FALSE) }");
        assertEval("{ x <- 3 ; f <- function(z) { if (z) { x <- 1 } ; x <- 1L + x ; x } ; f(FALSE) }");
        assertEval("{ x <- 3 ; f <- function(z) { if (z) { x <- 1 } ; x <- x - 1L ; x } ; f(FALSE) }");
    }

    @Test
    public void testXor() {
        assertEval(" xor(TRUE, TRUE) ");
        assertEval(" xor(FALSE, TRUE) ");
        assertEval(" xor(TRUE, FALSE) ");
        assertEval(" xor(FALSE, FALSE) ");
        assertEval("{ xor(7, 42) }");
        assertEval("{ xor(0:2, 2:4) }");
        assertEval("{ xor(0:2, 2:7) }");
    }
}
