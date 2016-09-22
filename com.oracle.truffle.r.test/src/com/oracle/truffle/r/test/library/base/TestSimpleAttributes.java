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

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// NOTE: some tests relating to attributes are also in TestSimpleBuiltins.testAttributes
public class TestSimpleAttributes extends TestBase {

    @Test
    public void testDefinition() {
        assertEval("{ x <- as.raw(10) ; attr(x, \"hi\") <- 2 ;  x }");
        assertEval("{ x <- TRUE ; attr(x, \"hi\") <- 2 ;  x }");
        assertEval("{ x <- 1L ; attr(x, \"hi\") <- 2 ;  x }");
        assertEval("{ x <- 1 ; attr(x, \"hi\") <- 2 ;  x }");
        assertEval("{ x <- 1+1i ; attr(x, \"hi\") <- 2 ;  x }");
        assertEval("{ x <- \"s\" ; attr(x, \"hi\") <- 2 ;  x }");
        assertEval("{ x <- c(1L, 2L) ; attr(x, \"hi\") <- 2; x }");
        assertEval("{ x <- c(1, 2) ; attr(x, \"hi\") <- 2; x }");
        assertEval("{ x <- c(1L, 2L) ; attr(x, \"hi\") <- 2; attr(x, \"hello\") <- 1:2 ;  x }");

        assertEval("{ x <- c(hello=9) ; attr(x, \"hi\") <- 2 ;  y <- x ; y }");

        assertEval("{ x <- c(hello=1) ; attr(x, \"hi\") <- 2 ;  attr(x,\"names\") <- \"HELLO\" ; x }");

        assertEval("{ x<-1; dim(x)<-1; y<-(attr(x, \"dimnames\")<-list(1)); y }");

        assertEval("{ x<-1; dim(x)<-1; y<-list(a=\"1\"); z<-(attr(x, \"dimnames\")<-y); z }");
        assertEval("{ x<-1; dim(x)<-1; y<-list(a=\"1\"); attr(y, \"foo\")<-\"foo\"; z<-(attr(x, \"dimnames\")<-y); z }");

        assertEval("{ x<-array(1:4, c(2,2), list(c(1,2), c(3,4))); attributes(x) }");
        assertEval("{ x<-1:4; attributes(x)<-list(dim=c(2,2), dimnames=list(c(1,2), c(3,4))); attributes(x) }");

        assertEval("{ attributes(NULL) }");

        assertEval("{ x<-function() 42; attr(x, \"foo\")<-\"foo\"; y<-x; attr(y, \"foo\")<-NULL; x }");

        assertEval("{ f<-function(y) attr(y, \"foo\")<-NULL; x<-function() 42; attr(x, \"foo\")<-\"foo\"; s<-\"bar\"; switch(s, f(x)); x }");

        assertEval("{ setClass(\"foo\", representation(j=\"numeric\")); x<-new(\"foo\", j=42); attr(x, \"foo\")<-\"foo\"; y<-x; attributes(y)<-NULL; x }");

        assertEval("{ x<-42; attributes(x)<-list(.Environment=globalenv()); environment(x) }");
        assertEval("{ x<-42; attributes(x)<-list(.Environment=globalenv()); attributes(x) }");
        assertEval("{ x<-42; attributes(x)<-list(.Environment=globalenv()); attributes(x)<-NULL; environment(x) }");
        assertEval("{ x<-42; attr(x, '.Environment')<-globalenv(); environment(x) }");
        assertEval("{ x<-42; attr(x, '.Environment')<-globalenv(); attr(x, '.Environment') }");
        assertEval("{ x<-42; attr(x, '.Environment')<-globalenv(); attr(x, '.Environment')<-NULL; environment(x) }");
        assertEval("{ f<-function() 42; attributes(f)<-list(.Environment=baseenv()); environment(f) }");
        assertEval("{ f<-function() 42; attributes(f)<-list(.Environment=baseenv()); attributes(f) }");
        assertEval("{ f<-function() 42; attr(f, '.Environment')<-baseenv(); environment(f) }");
        assertEval("{ f<-function() 42; attr(f, '.Environment')<-baseenv(); attr(f, '.Environment') }");
    }

    @Test
    public void testArithmeticPropagation() {
        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ;  x+1:4 }");
        assertEval("{ x <- 1+1i;  attr(x, \"hi\") <- 1+2 ; y <- 2:3 ;  x+y }");
        assertEval("{ x <- 1:2 ;  attr(x, \"hi\") <- 2 ;  !x  }");
        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ;  x & x }");
        assertEval("{ x <- as.raw(1:2);  attr(x, \"hi\") <- 2 ;  x & x }");

        assertEval("{ x <- c(1+1i,2+2i);  attr(x, \"hi\") <- 3 ; y <- 2:3 ; attr(y,\"zz\") <- 2; x+y }");
        assertEval("{ x <- 1+1i;  attr(x, \"hi\") <- 1+2 ; y <- 2:3 ; attr(y,\"zz\") <- 2; x+y }");
        assertEval("{ x <- c(1+1i, 2+2i) ;  attr(x, \"hi\") <- 3 ; attr(x, \"hihi\") <- 10 ; y <- c(2+2i, 3+3i) ; attr(y,\"zz\") <- 2; attr(y,\"hi\") <-3; attr(y,\"bye\") <- 4 ; x+y }");
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 2; 2+x }");

        assertEval("{ x <- c(a=1) ; y <- c(b=2,c=3) ; x + y }");
        assertEval("{ x <- c(a=1) ; y <- c(b=2,c=3) ; y + x }");

        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ;  x+1 }");
        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ; y <- 2:3 ; attr(y,\"hello\") <- 3; x+y }");
        assertEval("{ x <- 1;  attr(x, \"hi\") <- 1+2 ; y <- 2:3 ; attr(y, \"zz\") <- 2; x+y }");
        assertEval("{ x <- 1:2 ;  attr(x, \"hi\") <- 3 ; attr(x, \"hihi\") <- 10 ; y <- 2:3 ; attr(y,\"zz\") <- 2; attr(y,\"hi\") <-3; attr(y,\"bye\") <- 4 ; x+y }");

        assertEval("{ x <- c(a=1,b=2) ;  attr(x, \"hi\") <- 2 ;  -x  }");

        assertEval("{ x <- c(1+1i,2+2i);  names(x)<-c(\"a\", \"b\"); attr(x, \"hi\") <- 3 ; y <- 2:3 ; attr(y,\"zz\") <- 2; attributes(x+y) }");
        assertEval("{ x <- c(1+1i,2+2i,3+3i,4+4i);  dim(x)<-c(2,2); names(x)<-c(\"a\", \"b\"); attr(x, \"hi\") <- 3 ; y <- 2:5 ; attr(y,\"zz\") <- 2; attributes(x+y) }");
        assertEval("{ x <- c(1+1i,2+2i,3+3i,4+4i);  dim(x)<-c(2,2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); attr(x, \"hi\") <- 3 ; y <- 2:5 ; attr(y,\"zz\") <- 2; attributes(x+y) }");

        assertEval("{ x <- c(a=FALSE,b=TRUE) ;  attr(x, \"hi\") <- 2 ;  !x  }");
    }

    @Test
    public void testCasts() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; as.character(x) }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; as.double(x) }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; as.integer(x) }");
    }

    @Test
    public void testArrayPropagation() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; x[c(1,1)] }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; x[\"a\"] <- 2 ; x }");
        assertEval("{ x <- c(a=TRUE, b=FALSE) ; attr(x, \"myatt\") <- 1; x[2] <- 2 ; x }");
        assertEval("{ x <- TRUE ; attr(x, \"myatt\") <- 1; x[2] <- 2 ; x }");
        assertEval("{ x <- TRUE ; attr(x, \"myatt\") <- 1; x[1] <- 2 ; x }");
        assertEval("{ m <- matrix(rep(1,4), nrow=2) ; attr(m, \"a\") <- 1 ;  m[2,2] <- 1+1i ; m }");
        assertEval("{ a <- array(c(1,1), dim=c(1,2)) ; attr(a, \"a\") <- 1 ;  a[1,1] <- 1+1i ; a }");
    }

    @Test
    public void testBuiltinPropagation() {
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; matrix(x) }");
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; x:x }");
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; c(x, x, x) }");
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; cumsum(c(x, x, x)) }");
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; min(x) }");
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; x%o%x }");
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; rep(x,2) }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; order(x) }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; sum(x) }");
        // propagate names (or dimnames) and dimensions
        assertEval("{ x<-1:8; dim(x)<-c(2, 2, 2); names(x)<-101:108; attr(x, \"dimnames\")<-list(c(\"201\", \"202\"), c(\"203\", \"204\"), c(\"205\", \"206\")); attr(x, \"foo\")<-\"foo\"; y<-x; attributes(x>y) }");
        // convert elements of dimnames list to string vectors
        assertEval("{ x<-1:8; dim(x)<-c(2, 2, 2); names(x)<-101:108; attr(x, \"dimnames\")<-list(201:202, 203:204, 205:206); attr(x, \"foo\")<-\"foo\"; y<-x; attributes(x>y) }");
        assertEval("{ m <- 1:3 ; attr(m,\"a\") <- 1 ;  t(m) }");

        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; abs(x) }");

        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; array(x) }");
        assertEval("{ x <- \"a\" ; attr(x, \"myatt\") <- 1; toupper(x) }");
        assertEval("{ x <- \"a\" ; attr(x, \"myatt\") <- 1; tolower(x) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; attr(m,\"a\") <- 1 ;  diag(m) <- c(1,1) ; m }");
        assertEval("{ x <- c(a=1) ; attr(x, \"myatt\") <- 1; log10(x) }");
        assertEval("{ x <- c(a=1) ; attr(x, \"myatt\") <- 1; nchar(x) }"); // specific to
        assertEval("{ m <- matrix(rep(1,4), nrow=2) ; attr(m,\"a\") <- 1 ;  upper.tri(m) }");
        // FAST-R debugging format
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; rev(x) }");
        assertEval("{ x <- c(hello=1, hi=9) ; attr(x, \"hi\") <- 2 ;  sqrt(x) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; attr(m,\"a\") <- 1 ;  t(m) }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; unlist(x) }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; unlist(list(x,x)) }");

        assertEval("{ x <- c(a=1) ; attr(x, \"myatt\") <- 1 ; lapply(1:2, function(z) {x}) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; attr(m,\"a\") <- 1 ; mm <- aperm(m) ; dim(mm) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; attr(m,\"a\") <- 1 ;  aperm(m) }");

        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1 ; sapply(1:2, function(z) {x}) }");
        assertEval("{ x <- 1 ; attr(x, \"myatt\") <- 1; round(exp(x), digits=5) }");
        assertEval("{ x <- c(a=1,b=2) ; attr(x, \"myatt\") <- 1; round(exp(x), digits=5) }");
        assertEval("{ x <- c(1,2) ; dim(x)<-c(1,2); attr(x, \"myatt\") <- 1; round(exp(x), digits=5) }");
        assertEval("{ x <- c(a=TRUE) ; attr(x, \"myatt\") <- 1; rep(x,2) }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 1; seq(x) }");

        assertEval("{ a <- c(1,2,3,4); attr(a, \"x\") <- \"attrib\"; dim(a) <- c(2,2); a }");
        assertEval("{ a <- c(1,2,3,4); attr(a, \"x\") <- \"attrib\"; dim(a) <- NULL; a }");
    }

    @Test
    public void testBuiltinPropagationIgnore() {
        // eigen implementation problem
        assertEval(Ignored.Unknown, "{ m <- matrix(c(1,1,1,1), nrow=2) ; attr(m,\"a\") <- 1 ;  r <- eigen(m) ; r$vectors <- round(r$vectors, digits=5) ; r  }");
    }

    @Test
    public void testOtherPropagation() {
        assertEval("{ x <- 1:2;  attr(x, \"hi\") <- 2 ;  x == x }");

        assertEval("{ xx<-c(Package=\"digest\", Version=\"0.6.4\"); db<-list(xx); db <- do.call(\"rbind\", db); attributes(db) }");
        assertEval("{ xx<-c(Package=\"digest\", Version=\"0.6.4\"); db<-list(xx); db <- rbind(db); attributes(db) }");

        assertEval("{ x<-matrix(1, ncol=1); y<-c(1,2,3,4); x*y }");

        assertEval("{ x<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-x; attributes(y)<-NULL; x }");

        assertEval("{ gen<-function(object) 0; setGeneric(\"gen\"); x<-gen; attr(x, \"valueClass\")<-character(); res<-print(isS4(x)); removeGeneric(\"gen\"); res }");
    }

    @Test
    public void testLanguage() {
        assertEval("e <- quote(x(y)); e[[1]]; typeof(e[[1]])");
    }
}
