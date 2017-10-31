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

public class TestSimpleArrays extends TestBase {

    @Test
    public void testAccess() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[1, 1, 1, 1] }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[42,1,1] }");
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
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[[, 1, 1]] }");

        assertEval("{ v<-c(\"a\", \"b\"); dim(v)<-c(1,2); dimnames(v)<-list(\"x\", c(\"y\", \"z\")); v[1, c(1,2), drop=FALSE] }");

        assertEval("{ e <- new.env(); assign(\"a\", 1, e); e[[\"a\"]] }");
        assertEval("{ e <- new.env(); assign(\"a\", 1, e); e[\"a\"] }");
        assertEval("{ e <- new.env(); assign(\"a\", 1, e); e[[1]] }");

        assertEval("{ x <- c(1, 2); names(x) <- c(\"A\", \"A2\"); x[\"A\"] }");

    }

    @Test
    public void testArrayBuiltin() {
        // array with no arguments produces array of length 1
        assertEval("{ a = array(); length(a) }");

        // empty array has first element NA
        assertEval("{ a = array(); is.na(a[1]) }");

        // dimension names of empty array are null
        assertEval("{ a = array(); is.null(dimnames(a)); }");

        // empty array has single dimension that is 1
        assertEval("{ a <- array(); dim(a) }");

        // wrapping in arrays work even when prohibited by help
        assertEval("{ a = array(1:10, dim = c(2,6)); length(a) }");

        // negative length vectors are not allowed is the error reported by gnu-r
        // negative dims not allowed by R, special GNU message
        assertEval("{ array(NA, dim=c(-2,2)); }");

        // negative dims not allowed
        assertEval("{ array(NA, dim=c(-2,-2)); }");

        // zero dimension array has length 0
        assertEval("{ length(array(NA, dim=c(1,0,2,3))) }");

        // double dimensions work and are rounded down always
        assertEval("{ dim(array(NA, dim=c(2.1,2.9,3.1,4.7))) }");

        // complex size check
        assertEval("{ c <- array(c(3+2i, 5+0i, 1+3i, 5-3i), c(2,2,2)); length(c); dim(c) <- c(2,2,2); }");
    }

    @Test
    public void testMatrixBuiltin() {
        // empty matrix length is 1
        assertEval("{ length(matrix()) }");

        assertEval("{ matrix(1:4, dimnames=list(c(\"b\", \"c\", \"d\", \"e\"), \"a\")) }");
        assertEval("{ matrix(1:4, dimnames=list(101:104, 42)) }");
    }

    @Test
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
        assertEval("{ a = array(1,c(3,3,3)); a[2,2]; }");

        // calculating result dimensions
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- m[1:2,1,1] ; c(x[1],x[2]) }");
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- dim(m[1:2,1,1]) ; is.null(x) }");
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- dim(m[1:2,1,1,drop=FALSE]) ; c(x[1],x[2],x[3]) }");
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- m[1:2,1,integer()] ; d <- dim(x) ; length(x) }");
        assertEval("{ m <- array(c(1,2,3), dim=c(3,1,1)) ; x <- m[1:2,1,integer()] ; d <- dim(x) ; c(d[1],d[2]) }");

        // "drop" argument does not have to be last on the list
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=FALSE, 0, -1]) }");
        // "drop" argument is converted to boolean vector whose first elemen is taken
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=c(0,2), 0, -1]) }");
        // "drop" argument is converted to boolean
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=0, 0, -1]) }");
        // "drop" argument is the same as TRUE if it's an empty vector
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=integer(), 0, -1]) }");
        // second "drop" argument is considered an index
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,drop=FALSE, 1, drop=TRUE, -1]) }");
        // cannot specify multiple drop arguments if overall exceeding number of dimensions
        assertEval("{ x<-1:64; dim(x)<-c(4,4,2,2); dim(x[1,1, drop=FALSE, 0, drop=TRUE, -1]) }");
    }

    @Test
    public void testArraySubsetAndSelection() {
        // subset operator works for arrays
        assertEval("{ array(1,c(3,3,3))[1,1,1] }");

        // selection operator works for arrays
        assertEval("{ array(1,c(3,3,3))[[1,1,1]] }");

        // selection on multiple elements fails in arrays
        assertEval("{ array(1,c(3,3,3))[[,,]]; }");

        // selection on multiple elements fails in arrays
        assertEval(Output.IgnoreErrorContext, "{ array(1,c(3,3,3))[[c(1,2),1,1]]; }");

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

        // selection on multiple elements fails in matrices with empty selector
        assertEval("{ matrix(1,3,3)[[,]]; }");

        // selection on multiple elements fails in matrices
        assertEval(Output.IgnoreErrorContext, "{ matrix(1,3,3)[[c(1,2),1]]; }");

        assertEval("{  m <- matrix(1:6, nrow=2) ;  m[1,NULL] }");
    }

    @Test
    public void testArrayUpdate() {
        // update to matrix works
        assertEval("{ a = matrix(1,2,2); a[1,2] = 3; a[1,2] == 3; }");

        // update to an array works
        assertEval("{ a = array(1,c(3,3,3)); c(a[1,2,3],a[1,2,3]) }");

        // update returns the rhs
        assertEval("{ a = array(1,c(3,3,3)); a[1,2,3] = 3; a }");

        // update of shared object does the copy
        assertEval("{ a = array(1,c(3,3,3)); b = a; b[1,2,3] = 3; c(a[1,2,3],b[1,2,3]) }");

        // update where rhs depends on the lhs
        assertEval("{ x <- array(c(1,2,3), dim=c(3,1,1)) ; x[1:2,1,1] <- sqrt(x[2:1]) ; c(x[1] == sqrt(2), x[2], x[3]) }");

        // matrix update should preserve dimnames
        assertEval("{ ansmat <- array(dim=c(2,2),dimnames=list(c(\"1\",\"2\"),c(\"A\",\"B\"))) ; ansmat }");
        assertEval("{ ansmat <- array(dim=c(2,2),dimnames=list(c(\"1\",\"2\"),c(\"A\",\"B\"))) ; ansmat[c(1,2,4)] <- c(1,2,3) ; ansmat }");
    }

    @Test
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

        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); x[1:2,1:2,1]<-y; x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); x[1, 1] <- y; x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:102); z<-(x[1:2,c(1,2,0),1]<-y); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:120); z<-(x[1:2, c(1, 2, 0), 1] <- y); x }");
        assertEval("{ x<-1:16; dim(x)<-c(2,2,2,2); y<-c(101:108); dim(y)<-c(2,4); x[1:2, 1:2, 1] <- y; x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,c(1,1),1]<-y); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,c(1,2,0),1]<-y); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2, c(1, 2, 1), 1] <- y); x }");
        assertEval("{ x<-as.double(1:8); dim(x)<-c(2,2,2); x[1,1,1]<-42L; x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,1:2,0]<-y); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,1:2,c(0,0)]<-y); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[0,5,1] <- y); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2, c(1, NA), 1] <- y); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x }");
        assertEval("{ x<-1.1:8.8; dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x }");
        assertEval("({ x<-1:8; dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
        assertEval("({ x<-as.double(1:8); dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
        assertEval("({ x<-as.logical(1:8); dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
        assertEval("({ x<-as.character(1:8); dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
        assertEval("({ x<-as.complex(1:8); dim(x)<-c(2,2,2); x[1, 1, 1] = as.raw(42); x })");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); z<-(x[1,1,1]<-42); z }");

        // proper update in place
        assertEval("{ m <- matrix(1,2,2) ; m[,1] = 7 ; m }");
        assertEval("{ m <- matrix(1,2,2) ; m[1,] = 7 ; m }");
        assertEval("{ m <- matrix(1,2,2) ; m[,1] = c(10,11) ; m }");
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[2] <- list(100) ; m }");

        // error in lengths
        assertEval("{ m <- matrix(1,2,2) ; m[, 1] = c(1, 2, 3, 4) ; m }");

        // column update
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,2] <- 10:11 ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,2:3] <- 10:11 ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,integer()] <- integer() ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[, 2] <- integer() }");

        // error reporting
        // Checkstyle: stop
        assertEval("{ a <- 1:9 ; a[, , 1] <- 10L }");
        // Checkstyle: resume
        assertEval("{ a <- 1:9 ; a[, 1] <- 10L }");
        assertEval("{ a <- 1:9 ; a[1, 1] <- 10L }");
        assertEval("{ a <- 1:9 ; a[1, 1, 1] <- 10L }");

        assertEval("{ m <- matrix(1:6, nrow=2) ; m[[1, 1]] <- integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ m <- matrix(1:6, nrow=2) ; m[[1:2, 1]] <- integer() }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1, 2] <- integer() }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1, 2] <- 1:3 }");

        // pushback child of a selector node
        // It's a bug in GNUR introduced in 3.4.0
        assertEval(Ignored.ReferenceError, "{ m <- matrix(1:100, nrow=10) ; z <- 1; s <- 0 ; for(i in 1:3) { m[z <- z + 1,z <- z + 1] <- z * z * 1000 } ; sum(m) }");

        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1,-1) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1, c(-1,-10)) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1,c(-1,-10)) ; m <- f(1,-1) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(1,c(-1,-10)) ; m <- f(-1,2) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] <- 10 ; m } ; m <- f(2,1:3) ; m <- f(1,-2) ; m }");

        assertEval(Output.IgnoreErrorMessage, "{ x<-1:8; dim(x)<-c(2,2,2); y<-c(101:104); dim(y)<-c(2,2); z<-(x[1:2,c(1,2,NA),1]<-y); x }");

        assertEval("{ m <- matrix(1:6, nrow=3) ; m[2] <- list(100) ; m }");

        // proper update in place
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[[2]] <- list(100) ; m }");

        // column update
        assertEval("{ m <- array(1:24, dim=c(2,3,4)) ; m[,,4] <- 10:15 ; m[,,4] }");

        // subscript with rewriting
        assertEval("{  m <- array(1:3, dim=c(3,1,1)) ; f <- function(x,v) { x[[2,1,1]] <- v ; x } ; f(m,10L) ; f(m,10) ; x <- f(m,11L) ; c(x[1],x[2],x[3]) }");

        // error reporting
        assertEval(Output.IgnoreErrorContext, "{ m <- matrix(1:6, nrow=2) ; m[[1:2,1]] <- 1 }");
        assertEval(Output.IgnoreErrorContext, "{ m <- matrix(1:6, nrow=2) ; m[[integer(),1]] <- 1 }");

        // recovery from scalar selection update
        assertEval("{ m <- matrix(as.double(1:6), nrow=2) ; mi <- matrix(1:6, nrow=2) ; f <- function(v,i,j) { v[i,j] <- 100 ; v[i,j] * i * j } ; f(m, 1L, 2L) ; f(m,1L,TRUE)  }");
        assertEval("{ m <- matrix(as.double(1:6), nrow=2) ; mi <- matrix(1:6, nrow=2) ; f <- function(v,i,j) { v[i,j] <- 100 ; v[i,j] * i * j } ; f(m, 1L, 2L) ; f(m,1L,-1)  }");

        assertEval("{ x <- array(c(1,2,3), dim=c(3,1)) ; x[1:2,1] <- 2:1 ; x }");

        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[] = 42; x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[] = c(42,7); x }");

        assertEval("{ z<-1:4; y<-((z[1]<-42) >  1) }");
        assertEval("{ z<-1:4; y<-((names(z)<-101:104) >  1) }");
    }
}
