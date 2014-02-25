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

public class TestSimpleArrays extends TestBase {

    @Test
    public void testAccessScalarIndex() {
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); x[1,1,1,1] }");
        assertEvalError("{ x<-1:8; dim(x)<-c(2,2,2); x[42,1,1] }");
        assertEval("{ x<-1:8; dim(x)<-c(1,2,4); dim(x[1,0,3]) }");
        assertEval("{ x<-1:8; dim(x)<-c(1,2,4); dim(x[1,0,-1]) }");
        assertEval("{ x<-1:8; dim(x)<-c(1,2,4); dim(x[0,1,-1]) }");
        assertEval("{ x<-1:16; dim(x)<-c(2,2,4); dim(x[0,-1,-1]) }");
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); dim(x[0,-1,-1]) }");
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); dim(x[0,1,-1]) }");
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); dim(x[1,0,-1]) }");
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, 0,-1]) }");
        assertEval("{ x<-1:256; dim(x)<-c(4,4,4,4); dim(x[1,1, 0,-1]) }");
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,0, 1,-1]) }");
        assertEval("{ x<-1:256; dim(x)<-c(4,4,4,4); dim(x[1,0, 1,-1]) }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[1,0,1]) }");
        assertEval("{ x<-1:8; dim(x)<-c(1,2,4); dim(x[0,,-1]) }");

        assertEval("{ x<-1:16; dim(x)<-c(2,2,4); x[,1,1] }");
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); x[-1,-1,1] }");
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); x[-1,1,3] }");
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); x[1,1,3] }");
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dim(x[-1,1,1]) }");
        assertEval("{ x<-1:16; dim(x)<-c(4,1,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), NULL, c(\"e\", \"f\", \"g\", \"h\")); x[-1,1,1] }");
        assertEval("{ x<-1:16; dim(x)<-c(4,1,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), NULL, c(\"e\", \"f\", \"g\", \"h\")); x[-1,1,-1] }");
        assertEval("{ x<-1:16; dim(x)<-c(4,1,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), \"z\", c(\"e\", \"f\", \"g\", \"h\")); dimnames(x[-1,1,-1]) }");
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), c(\"x\", \"y\"), c(\"e\", \"f\", \"g\", \"h\")); x[-1,,-1] }");
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), c(\"x\", \"y\"), c(\"e\", \"f\", \"g\", \"h\")); x[1,1,1] }");
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dimnames(x)<-list(c(\"a\", \"b\", \"c\", \"d\"), NULL, c(\"e\", \"f\", \"g\", \"h\")); x[-1,,-1] }");
        assertEval("{ x<-1:32; dim(x)<-c(4,2,4); dimnames(x)<-list(NULL, c(\"x\", \"y\"), c(\"e\", \"f\", \"g\", \"h\")); x[-1,1,1] }");
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")) ;x[1,1,] }");
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")) ;x[,0,] }");

        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); x[1,1,NA] }");
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); x[1,1,c(1,NA,1)] }");
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); x[NA,1,c(1,NA,1)] }");
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")) ;x[1,1,NA] }");
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")); x[1,1,c(1,NA,1)] }");
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\"), c(\"e\", \"f\")); x[NA,1,c(1,NA,1)] }");

        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); dim(x[0,0,0]) }");
        assertEval("{ x<-(1:8); dim(x)<-c(2, 2, 2); x[0,0,1] }");
    }

    @Test
    @Ignore
    public void testArrayBuiltin() {
        // array with no arguments produces array of length 1
        assertEval("{ a = array(); length(a) }");

        // empty arg has first element NA
        assertEval("{ a = array(); is.na(a[1]) }");

        // dimnames not implemented yet
        // dimension names of empty array are null
        // assertTrue("{ a = array(); is.null(dimnames(a)); }");

        // empty array has single dimension that is 1
        assertEval("{ a <- array(); dim(a) }");

        // wrapping in arrays work even when prohibited by help
        assertEval("{ a = array(1:10, dim = c(2,6)); length(a) }");

        // negative length vectors are not allowed is the error reported by gnu-r
        // negative dims not allowed by R, special GNU message
        assertEvalError("{ array(dim=c(-2,2)); }");

        // negative dims not allowed
        assertEvalError("{ array(dim=c(-2,-2)); }");

        // zero dimension array has length 0
        assertEval("{ length(array(dim=c(1,0,2,3))) }");

        // double dimensions work and are rounded down always
        assertEval("{ dim(array(dim=c(2.1,2.9,3.1,4.7))) }");
    }

    @Test
    public void testMatrixBuiltin() {
        // empty matrix length is 1
        assertEval("{ length(matrix()) }");
    }

    @Test
    @Ignore
    public void testArraySimpleRead() {
        // simple read
        assertEval("{ a = array(1:27,c(3,3,3)); c(a[1,1,1],a[3,3,3],a[1,2,3],a[3,2,1]) }");

        // empty selectors reads the whole array
        assertEval("{ a = array(1:27, c(3,3,3)); b = a[,,]; d = dim(b); c(d[1],d[2],d[3]) }");

        // dimensions of 1 are dropped
        assertEval("{ a = array(1,c(3,3,3)); a = dim(a[,1,]); c(length(a),a[1],a[2]) }");

        // when all dimensions are dropped, dim is null
        assertEval("{ a = array(1,c(3,3,3)); is.null(dim(a[1,1,1])) }");

        // last dimension is dropped
        assertEval("{ a = array(1,c(3,3,3)); is.null(dim(a[1,1,])) } ");

        // dimensions of 1 are not dropped when requested (with subset)
        assertEval("{ a = array(1,c(3,3,3)); a = dim(a[1,1,1, drop = FALSE]); c(length(a),a[1],a[2],a[3]) }");

        // with subscript, dimensions are always dropped
        assertEval("{ m <- array(1:4, dim=c(4,1,1)) ; x <- m[[2,1,1,drop=FALSE]] ; is.null(dim(x)) }");

        // fallback to one dimensional read
        assertEval("{ a = array(1:27, c(3,3,3)); c(a[1],a[27],a[22],a[6]) }");

        // error when different dimensions given
        assertEvalError("{ a = array(1,c(3,3,3)); a[2,2]; }");

        // calculating result dimensions
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- m[1:2,1,1] ; c(x[1],x[2]) }");
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- dim(m[1:2,1,1]) ; is.null(x) }");
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- dim(m[1:2,1,1,drop=FALSE]) ; c(x[1],x[2],x[3]) }");
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- m[1:2,1,integer()] ; d <- dim(x) ; length(x) }");
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- m[1:2,1,integer()] ; d <- dim(x) ; c(d[1],d[2]) }");
    }

    @Test
    @Ignore
    public void testArraySubsetAndSelection() {
        // subset operator works for arrays
        assertEval("{ array(1,c(3,3,3))[1,1,1] }");

        // selection operator works for arrays
        assertEval("{ array(1,c(3,3,3))[[1,1,1]] }");

        // selection on multiple elements fails in arrays
        assertEvalError("{ array(1,c(3,3,3))[[,,]]; }");

        // selection on multiple elements fails in arrays
        assertEvalError("{ array(1,c(3,3,3))[[c(1,2),1,1]]; }");

        // last column
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; m[,,2] }");
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; m[,,2,drop=FALSE] }");
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; f <- function(i) { m[,,i] } ; f(1) ; f(2) ; dim(f(1:2)) }");
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; f <- function(i) { m[,,i] } ; f(1[2]) ; f(3) }");
    }

    @Test
    public void testMatrixSubsetAndSelection() {
        // subset operator works for matrices
        assertEval("{ matrix(1,3,3)[1,1] }");

        // selection operator works for arrays
        assertEval("{ matrix(1,3,3)[[1,1]] }");
    }

    @Test
    @Ignore
    public void testMatrixSubsetAndSelectionIgnore() {
        // selection on multiple elements fails in matrices with empty selector
        assertEvalError("{ matrix(1,3,3)[[,]]; }");

        // selection on multiple elements fails in matrices
        assertEvalError("{ matrix(1,3,3)[[c(1,2),1]]; }");

        assertEval("{  m <- matrix(1:6, nrow=2) ;  m[1,NULL] }");
    }

    @Test
    public void testArrayUpdate() {
        // update to matrix works
        assertEval("{ a = matrix(1,2,2); a[1,2] = 3; a[1,2] == 3; }");
    }

    @Test
    @Ignore
    public void testArrayUpdateIgnore() {
        // update to an array works
        assertEval("{ a = array(1,c(3,3,3)); c(a[1,2,3],a[1,2,3]) }");

        // update returns the rhs
        assertEval("{ a = array(1,c(3,3,3)); (a[1,2,3] = 3) }");

        // update of shared object does the copy
        assertEval("{ a = array(1,c(3,3,3)); b = a; b[1,2,3] = 3; c(a[1,2,3],b[1,2,3]) }");

        // update where rhs depends on the lhs
        assertEval("{ x <- array(c(1,2,3), dim=c(3,1,1)) ; x[1:2,1,1] <- sqrt(x[2:1]) ; c(x[1] == sqrt(2), x[2], x[3]) }");
    }

    @Test
    @Ignore
    public void testLhsCopy() {
        // lhs gets upgraded to int
        assertEval("{ a = array(TRUE,c(3,3,3)); a[1,2,3] = 8L; typeof(a[1,2,3]) }");

        // lhs logical gets upgraded to double
        assertEval("{ a = array(TRUE,c(3,3,3)); a[1,2,3] = 8.1; typeof(a[1,2,3]) }");

        // lhs integer gets upgraded to double
        assertEval("{ a = array(1L,c(3,3,3)); a[1,2,3] = 8.1; typeof(a[1,2,3]) }");

        // lhs logical gets upgraded to complex
        assertEval("{ a = array(TRUE,c(3,3,3)); a[1,2,3] = 2+3i; typeof(a[1,2,3]) }");

        // lhs integer gets upgraded to complex
        assertEval("{ a = array(1L,c(3,3,3)); a[1,2,3] = 2+3i; typeof(a[1,2,3]) }");

        // lhs double gets upgraded to complex
        assertEval("{ a = array(1.3,c(3,3,3)); a[1,2,3] = 2+3i; typeof(a[1,2,3]) }");

        // lhs logical gets upgraded to string
        assertEval("{ a = array(TRUE,c(3,3,3)); a[1,2,3] = \"2+3i\"; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");

        // lhs integer gets upgraded to string
        assertEval("{ a = array(1L,c(3,3,3)); a[1,2,3] = \"2+3i\"; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");

        // lhs double gets upgraded to string
        assertEval("{ a = array(1.5,c(3,3,3)); a[1,2,3] = \"2+3i\"; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    @Ignore
    public void testRhsCopy() {
        // rhs logical gets upgraded to int
        assertEval("{ a = array(7L,c(3,3,3)); b = TRUE; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");

        // rhs logical gets upgraded to double
        assertEval("{ a = array(1.7,c(3,3,3)); b = TRUE; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");

        // rhs logical gets upgraded to complex
        assertEval("{ a = array(3+2i,c(3,3,3)); b = TRUE; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");

        // rhs logical gets upgraded to string
        assertEval("{ a = array(\"3+2i\",c(3,3,3)); b = TRUE; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");

        // rhs int gets upgraded to double
        assertEval("{ a = array(1.7,c(3,3,3)); b = 3L; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");

        // rhs int gets upgraded to complex
        assertEval("{ a = array(3+2i,c(3,3,3)); b = 4L; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
        assertEval("{ m <- array(c(1+1i,2+2i,3+3i), dim=c(3,1,1)) ; m[1:2,1,1] <- c(100L,101L) ; m ; c(typeof(m[1,1,1]),typeof(m[2,1,1])) }");

        // rhs logical gets upgraded to string
        assertEval("{ a = array(\"3+2i\",c(3,3,3)); b = 7L; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");

        // rhs double gets upgraded to complex
        assertEval("{ a = array(3+2i,c(3,3,3)); b = 4.2; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");

        // rhs complex gets upgraded to string
        assertEval("{ a = array(\"3+2i\",c(3,3,3)); b = 2+3i; a[1,2,3] = b; c(typeof(a[1,2,3]),typeof(a[1,1,1])) }");
    }

    @Test
    @Ignore
    public void testMultiDimensionalUpdate() {
        // update matrix by vector, rows
        assertEval("{ a = matrix(1,3,3); a[1,] = c(3,4,5); c(a[1,1],a[1,2],a[1,3]) }");

        // update matrix by vector, cols
        assertEval("{ a = matrix(1,3,3); a[,1] = c(3,4,5); c(a[1,1],a[2,1],a[3,1]) }");

        // update array by vector, dim 3
        assertEval("{ a = array(1,c(3,3,3)); a[1,1,] = c(3,4,5); c(a[1,1,1],a[1,1,2],a[1,1,3]) }");

        // update array by vector, dim 2
        assertEval("{ a = array(1,c(3,3,3)); a[1,,1] = c(3,4,5); c(a[1,1,1],a[1,2,1],a[1,3,1]) }");

        // update array by vector, dim 1
        assertEval("{ a = array(1,c(3,3,3)); a[,1,1] = c(3,4,5); c(a[1,1,1],a[2,1,1],a[3,1,1]) }");

        // update array by matrix
        assertEval("{ a = array(1,c(3,3,3)); a[1,,] = matrix(1:9,3,3); c(a[1,1,1],a[1,3,1],a[1,3,3]) }");

    }

    @Test
    @Ignore
    public void testBugIfiniteLoopInGeneralizedRewriting() {
        assertEval("{ m <- array(1:3, dim=c(3,1,1)) ; f <- function(x,v) { x[1:2,1,1] <- v ; x } ; f(m,10L) ; f(m,10) ; f(m,c(11L,12L)); c(m[1,1,1],m[2,1,1],m[3,1,1]) }");
    }

    @Test
    public void testMatrixSimpleRead() {
        // last dimension is dropped
        assertEval("{ a = matrix(1,3,3); is.null(dim(a[1,])); }");
    }

    @Test
    public void testDefinitions() {
    }

    @Test
    @Ignore
    public void testDefinitionsIgnore() {
        assertEval("{ matrix( as.raw(101:106), nrow=2 ) }");
        assertEval("{ m <- matrix(1:6, ncol=3, byrow=TRUE) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2, byrow=TRUE) ; m }");
        assertEval("{ m <- matrix() ; m }");
        assertEval("{ matrix( (1:6) * (1+3i), nrow=2 ) }");
        assertEval("{ m <- matrix(1:6, nrow=2, ncol=3, byrow=TRUE) ; m }");
    }

    @Test
    public void testSelection() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=3) ; m[0] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[upper.tri(m)] }");
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[0] }");
    }

    @Test
    public void testUpdate() {
        // proper update in place
        assertEval("{ m <- matrix(1,2,2); m[1,1] = 6; m }");

        // element deletion
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,4,6)] <- NULL ; m }");
    }

    @Test
    @Ignore
    public void testUpdateIgnore() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; m[2] <- list(100) ; m }");

        // proper update in place
        assertEval("{ m <- matrix(1,2,2) ; m[,1] = 7 ; m }");
        assertEval("{ m <- matrix(1,2,2) ; m[1,] = 7 ; m }");
        assertEval("{ m <- matrix(1,2,2) ; m[,1] = c(10,11) ; m }");
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[2] <- list(100) ; m }");
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[[2]] <- list(100) ; m }");

        // error in lengths
        assertEvalError("{ m <- matrix(1,2,2) ; m[,1] = c(1,2,3,4) ; m }");

        // column update
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,2] <- 10:11 ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,2:3] <- 10:11 ; m }");
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; m[,,4] <- 10:15 ; m[,,4] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,integer()] <- integer() ; m }");
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[,2] <- integer() }");

        // subscript with rewriting
        assertEval("{  m <- array(1:3, dim=c(3,1,1)) ; f <- function(x,v) { x[[2,1,1]] <- v ; x } ; f(m,10L) ; f(m,10) ; x <- f(m,11L) ; c(x[1],x[2],x[3]) }");

        // error reporting
        assertEvalError("{ a <- 1:9 ; a[,,1] <- 10L }");
        assertEvalError("{ a <- 1:9 ; a[,1] <- 10L }");
        assertEvalError("{ a <- 1:9 ; a[1,1] <- 10L }");
        assertEvalError("{ a <- 1:9 ; a[1,1,1] <- 10L }");
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[[1:2,1]] <- 1 }");
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[[integer(),1]] <- 1 }");
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[[1,1]] <- integer() }");
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[[1:2,1]] <- integer() }");
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[1,2] <- integer() }");
        assertEvalError("{ m <- matrix(1:6, nrow=2) ; m[1,2] <- 1:3 }");

        // pushback child of a selector node
        assertEval("{ m <- matrix(1:100, nrow=10) ; z <- 1; s <- 0 ; for(i in 1:3) { m[z <- z + 1,z <- z + 1] <- z * z * 1000 } ; sum(m) }");

        // recovery from scalar selection update
        assertEval("{ m <- matrix(as.double(1:6), nrow=2) ; mi <- matrix(1:6, nrow=2) ; f <- function(v,i,j) { v[i,j] <- 100 ; v[i,j] * i * j } ; f(m, 1L, 2L) ; f(m,1L,TRUE)  }");
        assertEval("{ m <- matrix(as.double(1:6), nrow=2) ; mi <- matrix(1:6, nrow=2) ; f <- function(v,i,j) { v[i,j] <- 100 ; v[i,j] * i * j } ; f(m, 1L, 2L) ; f(m,1L,-1)  }");

        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1,-1) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1, c(-1,-10)) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1,c(-1,-10)) ; m <- f(1,-1) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1,c(-1,-10)) ; m <- f(-1,2) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(2,1:3) ; m <- f(1,-2) ; m }");
        assertEval("{ x <- array(c(1,2,3), dim=c(3,1)) ; x[1:2,1] <- 2:1 ; x }");
    }

    @Test
    @Ignore
    public void testDynamic() {
        assertEval("{ l <- quote(x[1,1] <- 10) ; f <- function() { eval(l) } ; x <- matrix(1:4,nrow=2) ; f() ; x }");
    }
}
