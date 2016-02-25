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

public class TestSimpleComparison extends TestBase {

    @Test
    public void testAttributes() {
        assertEval("{ x<-1:4; names(x)<-101:104; x < 7 }");
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:24; names(y)<-121:124; x < y }");
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; names(y)<-121:128; x < y }");
        assertEval("{ x<-1:4; names(x)<-101:104; attr(x, \"foo\")<-\"foo\"; attributes(x < 7) }");
        assertEval("{ x<-1:4; names(x)<-101:104; attr(x, \"foo\")<-\"foo\"; y<-21:24; names(y)<-121:124; attributes(x < y) }");
        assertEval("{ x<-1:4; y<-21:24; names(y)<-121:124; attributes(x > y) }");
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; names(y)<-121:128;  attributes(y > x) }");
        assertEval("{ x<-1:4; names(x)<-101:104; y<-21:28; attributes(x > y) }");
        assertEval(Output.ContainsError, "{ x<-1:4; dim(x)<-c(2,2); y<-21:28; x > y }");
        assertEval("{ x<-factor(c(a=1)); y<-factor(c(b=1)); x==y }");
    }

    @Test
    public void testScalars() {
        assertEval("{ 1==1 }");
        assertEval("{ 2==1 }");
        assertEval("{ 1L<=1 }");
        assertEval("{ 1<=0L }");
        assertEval("{ x<-2; f<-function(z=x) { if (z<=x) {z} else {x} } ; f(1.4)}");
        assertEval("{ 1L==1 }");
        assertEval("{ TRUE==1 }");
        assertEval("{ TRUE==1L }");
        assertEval("{ 2L==TRUE }");
        assertEval("{ TRUE==FALSE }");
        assertEval("{ FALSE<=TRUE }");
        assertEval("{ FALSE<TRUE }");
        assertEval("{ TRUE>FALSE }");
        assertEval("{ TRUE>=FALSE }");
        assertEval("{ TRUE!=FALSE }");
        assertEval("{ 2L==NA }");
        assertEval("{ NA==2L }");

        assertEval("{ 1==NULL }");
        assertEval("{ 2L==as.double(NA) }");
        assertEval("{ as.double(NA)==2L }");

        assertEval("{ 0/0 <= 2 }");
        assertEval("{ 1+1i == TRUE }");
        assertEval("{ 1+1i == 1 }");
        assertEval("{ 1+0i == 1 }");

        assertEval("{ \"-1+1i\" > \"1+1i\" }");
        assertEval("{ \"-1+1i\" > 1+1i }");
        assertEval("{ \"+1+1i\" > 1+1i }");
        assertEval("{ \"1+2i\" > 1+1i }");
        assertEval("{ \"1+1.1i\" == 1+1.1i }");
        assertEval("{ \"1+1.100i\" == 1+1.100i }");

        assertEval("{ x<-1+1i; x > FALSE }");
        assertEval("{ z <- TRUE; dim(z) <- c(1) ; dim(z == TRUE) }");
        assertEval("{ z <- TRUE; dim(z) <- c(1) ; u <- 1:3 ; dim(u) <- 3 ; u == z }");
    }

    @Test
    public void testScalarsStrings() {
        assertEval("\"hello\" < \"hi\"");
        assertEval("\"hello\" > \"hi\"");
        assertEval("\"hi\" <= \"hello\"");
        assertEval("\"hi\" >= \"hello\"");
        assertEval("\"hi\" < \"hello\"");
        assertEval("\"hi\" > \"hello\"");
        assertEval("\"hi\" == \"hello\"");
        assertEval("\"hi\" != \"hello\"");
        assertEval("\"hello\" <= \"hi\"");
        assertEval("\"hello\" >= \"hi\"");
        assertEval("\"hello\" == \"hello\"");
        assertEval("\"hello\" != \"hello\"");
        assertEval("{ \"a\" <= \"b\" }");
        assertEval("{ \"a\" > \"b\" }");
        assertEval("{ \"2.0\" == 2 }");
    }

    @Test
    public void testScalarsRaw() {
        assertEval("{ as.raw(15) > as.raw(10) }");
        assertEval("{ as.raw(15) < as.raw(10) }");
        assertEval("{ as.raw(15) >= as.raw(10) }");
        assertEval("{ as.raw(15) <= as.raw(10) }");
        assertEval("{ as.raw(10) >= as.raw(15) }");
        assertEval("{ as.raw(10) <= as.raw(15) }");
        assertEval("{ as.raw(15) == as.raw(10) }");
        assertEval("{ as.raw(15) != as.raw(10) }");
        assertEval("{ as.raw(15) == as.raw(15) }");
        assertEval("{ as.raw(15) != as.raw(15) }");
        assertEval("{ a <- as.raw(1) ; b <- as.raw(2) ; a < b }");
        assertEval("{ a <- as.raw(1) ; b <- as.raw(2) ; a > b }");
        assertEval("{ a <- as.raw(1) ; b <- as.raw(2) ; a == b }");
        assertEval("{ a <- as.raw(1) ; b <- as.raw(200) ; a < b }");
        assertEval("{ a <- as.raw(200) ; b <- as.raw(255) ; a < b }");
    }

    @Test
    public void testScalarsNA() {
        assertEval("{ a <- 1L ; b <- a[2] ; a == b }");
        assertEval("{ a <- 1L ; b <- a[2] ; b > a }");
        assertEval("{ a <- 1L ; b <- 1[2] ; a == b }");
        assertEval("{ a <- 1L[2] ; b <- 1 ; a == b }");
        assertEval("{ a <- 1L[2] ; b <- 1 ; b > a }");
        assertEval("{ a <- 1 ; b <- 1L[2] ; a == b }");
        assertEval("{ a <- 1[2] ; b <- 1L ; b > a }");
        assertEval("{ a <- 1L[2] ; b <- TRUE ; a != b }");
        assertEval("{ a <- TRUE ; b <- 1L[2] ; a > b }");
        assertEval("{ a <- 1 ; b <- a[2] ; a == b }");
        assertEval("{ a <- 1 ; b <- a[2] ; b > a }");
        assertEval("{ a <- 1L ; b <- TRUE[2] ; a == b }");
        assertEval("{ a <- TRUE[2] ; b <- 1L ; a == b }");
    }

    @Test
    public void testScalarsAsFunction() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1,2L) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1L,2L) ; f(1,2) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1L,2L) ; f(1L,2) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1L,2) ; f(1,2) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1L,2) ; f(1L,2L) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2L) ; f(1,2) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2L) ; f(1L,2L) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(TRUE,FALSE) ; f(TRUE,2) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(TRUE,FALSE) ; f(1L,2L) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(0L,TRUE) ; f(FALSE,2) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(0L,TRUE) ; f(0L,2L) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(0L,TRUE) ; f(2L,TRUE) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(TRUE,2L) ; f(FALSE,2) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(TRUE,2L) ; f(0L,2L) }");
    }

    @Test
    public void testScalarsNAAsFunction() {
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2, 1L[2]) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2[2], 1L) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2L, 1[2]) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2L[2], 1) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2L, 1L[2]) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2L[2], 1L) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2, 1[2]) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(2[2], 1) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(\"hello\", \"hi\"[2]) }");
        assertEval("{ f <- function(a,b) { a > b } ; f(1,2) ; f(1L,2) ; f(\"hello\"[2], \"hi\") }");
    }

    @Test
    public void testScalarsComplex() {
        assertEval("{ 1+1i == 1-1i }");
        assertEval("{ 1+1i == 1+1i }");
        assertEval("{ 1+1i == 2+1i }");
        assertEval("{ 1+1i != 1+1i }");
        assertEval("{ 1+1i != 1-1i }");
        assertEval("{ 1+1i != 2+1i }");

    }

    @Test
    public void testVectors() {
        assertEval("{ x<-c(1,2,3,4);y<-2.5; x<=y }");
        assertEval("{ x<-c(1L,2L,3L,4L);y<-1.5; x<=y }");
        assertEval("{ c(1:3,4,5)==1:5 }");
        assertEval("{ 3 != 1:2 }");
        assertEval("{ b <- 1:3 ; z <- FALSE ; b[2==2] }");

        assertEval("{ x<-1+1i; y<-2+2i; x > y }");
        assertEval("{ x<-1+1i; y<-2+2i; x < y }");
        assertEval("{ x<-1+1i; y<-2+2i; x >= y }");
        assertEval("{ x<-1+1i; y<-2+2i; x <= y }");

        assertEval("{ c(1,2,NA,4) != 2 }");
        assertEval("{ c(1,2,NA,4) == 2 }");

        assertEval("{ x<-c(FALSE,TRUE);y<-c(TRUE,FALSE); x<y }");
        assertEval("{ x<-c(FALSE,TRUE, FALSE, FALSE);y<-c(TRUE,FALSE); x<y }");
        assertEval("{ x<-c(\"0\",\"1\");y<-c(\"a\",\"-1\"); x<y }");
        assertEval("{ x<-c(\"0\",\"1\",\"-1\", \"2\");y<-c(\"a\",\"-1\", \"0\", \"2\"); x<y }");
        assertEval("{ x<-c(10,3);y<-c(10,2); x<=y }");
        assertEval("{ x<-c(10L,3L);y<-c(10L,2L); x<=y }");
        assertEval("{ x<-c(10L,3L);y<-c(10,2); x<=y }");
        assertEval("{ x<-c(10,3);y<-c(10L,2L); x<=y }");
        assertEval("{ x<-c(1,2,3,4);y<-c(10,2); x<=y }");
        assertEval("{ x<-c(1,2,3,4);y<-c(2.5+NA,2.5); x<=y }");
        assertEval("{ x<-c(1L,2L,3L,4L);y<-c(2.5+NA,2.5); x<=y }");

        assertEval("{ x<-c(10,1,3);y<-4:6; x<=y }");
        assertEval("{ x<-5;y<-4:6; x<=y }");

        assertEval("{ x<-c(1L,2L,3L,4L);y<-c(TRUE,FALSE); x<=y }");
        assertEval("{ 0/0 == c(1,2,3,4) }");

        assertEval("{ 1:3 == TRUE }");
        assertEval("{ TRUE == 1:3 }");

        assertEval(Output.ContainsWarning, "{ c(1,2) < c(2,1,4) }");
        assertEval(Output.ContainsWarning, "{ c(2,1,4) < c(1,2) }");
        assertEval(Output.ContainsWarning, "{ c(1L,2L) < c(2L,1L,4L) }");
        assertEval(Output.ContainsWarning, "{ c(2L,1L,4L) < c(1L,2L) }");
        assertEval(Output.ContainsWarning, "{ c(TRUE,FALSE,FALSE) < c(TRUE,TRUE) }");
        assertEval(Output.ContainsWarning, "{ c(TRUE,TRUE) == c(TRUE,FALSE,FALSE) }");
        assertEval(Output.ContainsWarning, "{ as.raw(c(1,2)) < as.raw(c(2,1,4)) }");
        assertEval(Output.ContainsWarning, "{ as.raw(c(2,1,4)) < as.raw(c(1,2)) }");
        assertEval(Output.ContainsWarning, "{ c(\"hi\",\"hello\",\"bye\") > c(\"cau\", \"ahoj\") }");
        assertEval(Output.ContainsWarning, "{ c(\"cau\", \"ahoj\") != c(\"hi\",\"hello\",\"bye\") }");
        assertEval(Output.ContainsWarning, "{ c(1+1i,2+2i) == c(2+1i,1+2i,1+1i) }");
        assertEval(Output.ContainsWarning, "{ c(2+1i,1+2i,1+1i) == c(1+1i, 2+2i) }");

        assertEval("{ as.raw(c(2,1,4)) < raw() }");
        assertEval("{ raw() < as.raw(c(2,1,4)) }");
        assertEval("{ 1:3 < integer() }");
        assertEval("{ integer() < 1:3 }");
        assertEval("{ c(1,2,3) < double() }");
        assertEval("{ double() == c(1,2,3) }");
        assertEval("{ c(TRUE,FALSE) < logical() }");
        assertEval("{ logical() == c(FALSE, FALSE) }");
        assertEval("{ c(1+2i, 3+4i) == (1+2i)[0] }");
        assertEval("{ (1+2i)[0] == c(2+3i, 4+1i) }");
        assertEval("{ c(\"hello\", \"hi\") == character() }");
        assertEval("{ character() > c(\"hello\", \"hi\") }");

        assertEval("{ integer() == 2L }");

        assertEval("{ c(1,2,3,4) != c(1,NA) }");
        assertEval("{ 2 != c(1,2,NA,4) }");
        assertEval("{ 2 == c(1,2,NA,4) }");
        assertEval("{ c(\"hello\", NA) < c(\"hi\", NA) }");
        assertEval("{ c(\"hello\", NA) >= \"hi\" }");
        assertEval("{ \"hi\" > c(\"hello\", NA)  }");
        assertEval("{ c(\"hello\", NA) > c(NA, \"hi\") }");
        assertEval("{ c(1L, NA) > c(NA, 2L) }");
        assertEval("{ c(TRUE, NA) > c(NA, FALSE) }");
        assertEval("{ \"hi\" > c(\"hello\", \"hi\")  }");
        assertEval("{ NA > c(\"hello\", \"hi\") }");
        assertEval("{ c(\"hello\", \"hi\") < NA }");
        assertEval("{ 1:3 < NA }");
        assertEval("{ NA > 1:3 }");
        assertEval("{ 2L > c(1L,NA,2L) }");
        assertEval("{ c(1L,NA,2L) < 2L }");
        assertEval("{ c(0/0+1i,2+1i) == c(1+1i,2+1i) }");
        assertEval("{ c(1+1i,2+1i) == c(0/0+1i,2+1i) }");

        assertEval(Output.ContainsError, "{ m <- matrix(nrow=2, ncol=2, 1:4) ; m == 1:16 }");
    }

    @Test
    public void testMatrices() {
        assertEval("{ matrix(1) > matrix(2) }");
        assertEval("{ matrix(1) > NA }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m > c(1,2,3) }");
    }

    @Test
    public void testOther() {
        assertEval("{ stdin() == 0L }");
    }
}
