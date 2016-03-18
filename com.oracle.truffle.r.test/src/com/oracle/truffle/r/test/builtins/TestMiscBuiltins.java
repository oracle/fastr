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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestMiscBuiltins extends TestBase {

    @Test
    public void testModeSet() {
        assertEval("{  x<-c(1,2); mode(x)<-\"character\"; x }");
    }

    @Test
    public void testTable() {
        assertEval("{ a<-c(\"a\", \"b\", \"c\");  t<-table(a, sample(a)); dimnames(t) }");
    }

    @Test
    public void testArrayConstructors() {
        assertEval("{ integer() }");
        assertEval("{ double() }");
        assertEval("{ logical() }");
        assertEval("{ double(3) }");
        assertEval("{ logical(3L) }");
        assertEval("{ character(1L) }");
        assertEval("{ raw() }");
    }

    @Test
    public void testCasts() {
        // shortcuts in views (only some combinations)
        assertEval("{ as.complex(as.character(c(1+1i,1+1i))) }");
        assertEval(Output.ContainsWarning, "{ as.complex(as.integer(c(1+1i,1+1i))) }");
        assertEval("{ as.complex(as.logical(c(1+1i,1+1i))) }");

        assertEval("{ as.double(as.logical(c(10,10))) }");
        assertEval("{ as.integer(as.logical(-1:1)) }");
        assertEval("{ as.raw(as.logical(as.raw(c(1,2)))) }");
        assertEval("{ as.character(as.double(1:5)) }");
        assertEval("{ as.character(as.complex(1:2)) }");

        assertEval("{ m<-matrix(1:6, nrow=3) ; as.integer(m) }");
        assertEval("{ m<-matrix(1:6, nrow=3) ; as.vector(m, \"any\") }");
        assertEval("{ m<-matrix(1:6, nrow=3) ; as.vector(mode = \"integer\", x=m) }");
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"double\") }");
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"numeric\") }");
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m) }");
        assertEval("{ m<-matrix(c(TRUE,FALSE,FALSE,TRUE), nrow=2) ; as.vector(m) }");
        assertEval("{ m<-matrix(c(1+1i,2+2i,3-3i,4-4i), nrow=2) ; as.vector(m) }");
        assertEval("{ m<-matrix(c(\"a\",\"b\",\"c\",\"d\"), nrow=2) ; as.vector(m) }");
        assertEval("{ m<-matrix(as.raw(c(1,2,3,4)), nrow=2) ; as.vector(m) }");

        // dropping dimensions
        assertEval("{ m <- matrix(1:6, nrow=2) ; as.double(m) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; as.integer(m) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; as.logical(m) }");

        // dropping names
        assertEval("{ x <- c(0,2); names(x) <- c(\"hello\",\"hi\") ; as.logical(x) }");
        assertEval("{ x <- 1:2; names(x) <- c(\"hello\",\"hi\") ; as.double(x) }");
        assertEval("{ x <- c(1,2); names(x) <- c(\"hello\",\"hi\") ; as.integer(x) }");

        assertEval("{ m<-matrix(c(1,0,1,0), nrow=2) ; as.vector(m, mode = \"logical\") }");
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"complex\") }");
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"character\") }");
        assertEval("{ m<-matrix(c(1,2,3,4), nrow=2) ; as.vector(m, mode = \"raw\") }");

        assertEval("{ as.vector(list(1,2,3), mode=\"integer\") }");

        // as.list
        assertEval("{ k <- as.list(3:6) ; l <- as.list(1) ; list(k,l) }");
        assertEval("{ as.list(list(1,2,\"eep\")) }");
        assertEval("{ as.list(c(1,2,3,2,1)) }");
        assertEval("{ as.list(3:6) }");
        assertEval("{ l <- list(1) ; attr(l, \"my\") <- 1; as.list(l) }");
        assertEval("{ l <- 1 ; attr(l, \"my\") <- 1; as.list(l) }");
        assertEval("{ l <- c(x=1) ; as.list(l) }");
        assertEval("{ x<-7; as.list(environment()) }");
        assertEval("{ x<-7; .y<-42; as.list(environment()) }");
        // not sorted so can't compare list print
        assertEval("{ x<-7; .y<-42; length(as.list(environment(), all.names=TRUE)) }");
        assertEval("{ x<-7; f<-function() x<<-42; f_copy<-as.list(environment())[[\"f\"]]; f_copy(); x }");

        // as.matrix
        assertEval("{ as.matrix(1) }");
        assertEval("{ as.matrix(1:3) }");
        assertEval("{ x <- 1:3; z <- as.matrix(x); x }");
        assertEval("{ x <- 1:3 ; attr(x,\"my\") <- 10 ; attributes(as.matrix(x)) }");

        assertEval(Output.ContainsWarning, "{ as.complex(as.double(c(1+1i,1+1i))) }");
        assertEval(Output.ContainsWarning, "{ as.complex(as.raw(c(1+1i,1+1i))) }");
    }

    @Test
    public void testOuter() {
        assertEval("{ outer(c(1,2,3),c(1,2),\"-\") }");
        assertEval("{ outer(c(1,2,3),c(1,2),\"*\") }");
        assertEval("{ outer(1, 3, \"-\") }");

        assertEval("{ foo <- function (x,y) { x + y * 1i } ; outer(3,3,foo) }");
        assertEval("{ foo <- function (x,y) { x + y * 1i } ; outer(1:3,1:3,foo) }");
        assertEval("{ outer(c(1,2,3),c(1,2),\"+\") }");
        assertEval("{ outer(1:3,1:2) }");
        assertEval("{ outer(1:3,1:2,\"*\") }");
        assertEval("{ outer(1:3,1:2, function(x,y,z) { x*y*z }, 10) }");
        assertEval("{ outer(1:2, 1:3, \"<\") }");
        assertEval("{ outer(1:2, 1:3, '<') }");
        assertEval("{ foo <- function (x,y) { x + y * 1i } ; outer(3,3,\"foo\") }");
    }

    @Test
    public void testUpperTriangular() {
        assertEval("{ m <- matrix(1:6, nrow=2) ;  upper.tri(m, diag=TRUE) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ;  upper.tri(m, diag=FALSE) }");
        assertEval("{ upper.tri(1:3, diag=TRUE) }");
        assertEval("{ upper.tri(1:3, diag=FALSE) }");
    }

    @Test
    public void testLowerTriangular() {
        assertEval(Ignored.Unknown, "{ m <- matrix(1:6, nrow=2) ;  lower.tri(m, diag=TRUE) }");
        assertEval(Ignored.Unknown, "{ m <- matrix(1:6, nrow=2) ;  lower.tri(m, diag=FALSE) }");

        assertEval(Ignored.Unknown, "{ lower.tri(1:3, diag=TRUE) }");
        assertEval(Ignored.Unknown, "{ lower.tri(1:3, diag=FALSE) }");
    }

    @Test
    public void testTriangular() {
        assertEval(Ignored.Unknown, "{ m <- { matrix( as.character(1:6), nrow=2 ) } ; diag(m) <- c(1,2) ; m }");
        assertEval(Ignored.Unknown, "{ m <- { matrix( (1:6) * (1+3i), nrow=2 ) } ; diag(m) <- c(1,2) ; m }");
        assertEval(Ignored.Unknown, "{ m <- { matrix( as.raw(11:16), nrow=2 ) } ; diag(m) <- c(as.raw(1),as.raw(2)) ; m }");
    }

    //@formatter:off
    private static final String[] BASIC_TYPES = new String[]{
        "call", "character", "complex", "double", "expression", "function", "integer", "list",
        "logical", "name", "symbol", "null", "pairlist", "raw",
    };

    private static final String[] BASIC_TYPE_VALUES = new String[]{
        "call(\"foo\")", "\"1\"", "1i", "1", "expression(x + 1)", "function() { }",
        "1L", "list()", "TRUE", "quote(x)", "NULL", "pairlist()", "raw()"
    };

    //@formatter:on

    @Test
    public void testBasicTypes() {
        // cross-product of all basic types and values
        assertEval(template("is.%0(%1)", BASIC_TYPES, BASIC_TYPE_VALUES));
    }

    @Test
    public void testArrayTypeCheck() {
        assertEval(template("is.array(%0)", BASIC_TYPE_VALUES));
        assertEval("{ is.array(as.array(1)) }");
    }

    @Test
    public void testAtomicTypeCheck() {
        assertEval(template("is.atomic(%0)", BASIC_TYPE_VALUES));
        assertEval("{ is.atomic(integer()) }");
    }

    @Test
    public void testDataFrameTypeCheck() {
        assertEval(template("is.data.frame(%0)", BASIC_TYPE_VALUES));
        assertEval("{ is.data.frame(as.data.frame(1)) }");
    }

    @Test
    public void testLanguageTypeCheck() {
        assertEval(template("is.language(%0)", BASIC_TYPE_VALUES));
    }

    @Test
    public void testMatrixTypeCheck() {
        assertEval(template("is.matrix(%0)", BASIC_TYPE_VALUES));
        assertEval("{ is.matrix(as.matrix(1)) }");
    }

    @Test
    public void testObjectTypeCheck() {
        assertEval(template("is.object(%0)", BASIC_TYPE_VALUES));
        assertEval("{ e <- expression(x + 1); class(e) <- \"foo\"; is.object(e) }");
    }

    @Test
    public void testNumericTypeCheck() {
        assertEval(template("is.numeric(%0)", BASIC_TYPE_VALUES));
        assertEval("{ is.numeric(1:6) }");
    }

    @Test
    public void testOverride() {
        assertEval("{ sub <- function(x,y) { x - y }; sub(10,5) }");
    }

    @Test
    public void testEigen() {
        // symmetric real input
        assertEval(Ignored.Unknown, "{ r <- eigen(matrix(rep(1,4), nrow=2), only.values=FALSE) ; round( r$vectors, digits=5 ) }");
        assertEval(Ignored.Unknown, "{ r <- eigen(matrix(rep(1,4), nrow=2), only.values=FALSE) ; round( r$values, digits=5 ) }");
        assertEval(Ignored.Unknown, "{ eigen(10, only.values=FALSE) }");

        // non-symmetric real input, real output
        assertEval(Ignored.Unknown, "{ r <- eigen(matrix(c(1,2,2,3), nrow=2), only.values=FALSE); round( r$vectors, digits=5 ) }");
        assertEval(Ignored.Unknown, "{ r <- eigen(matrix(c(1,2,2,3), nrow=2), only.values=FALSE); round( r$values, digits=5 ) }");
        assertEval(Ignored.Unknown, "{ r <- eigen(matrix(c(1,2,3,4), nrow=2), only.values=FALSE); round( r$vectors, digits=5 ) }");
        assertEval(Ignored.Unknown, "{ r <- eigen(matrix(c(1,2,3,4), nrow=2), only.values=FALSE); round( r$values, digits=5 ) }");

        // non-symmetric real input, complex output
        // FIXME: GNUR is won't print the minus sign for negative zero
        assertEval(Ignored.Unknown, "{ r <- eigen(matrix(c(3,-2,4,-1), nrow=2), only.values=FALSE); round( r$vectors, digits=5 ) }");
        assertEval(Ignored.Unknown, "{ r <- eigen(matrix(c(3,-2,4,-1), nrow=2), only.values=FALSE); round( r$values, digits=5 ) }");
    }

    @Test
    public void testOther() {
        assertEval("{ rev.mine <- function(x) { if (length(x)) x[length(x):1L] else x } ; rev.mine(1:3) }");
    }

    @Test
    public void testLocal() {
        assertEval("{ kk <- local({k <- function(x) {x*2}}); kk(8)}");
        assertEval("{ ne <- new.env(); local(a <- 1, ne); ls(ne) }");
    }

    @Test
    public void testDiagnostics() {
        assertEval(Output.ContainsError, "{ f <- function() { stop(\"hello\",\"world\") } ; f() }");
    }

    @Test
    public void testWorkingDirectory() {
        assertEval("{ cur <- getwd(); cur1 <- setwd(getwd()) ; cur2 <- getwd() ; cur == cur1 && cur == cur2 }");
        assertEval(Output.ContainsError, "{ setwd(1) }");
        assertEval(Output.ContainsError, "{ setwd(character()) }");
        assertEval("{ cur <- getwd(); cur1 <- setwd(c(cur, \"dummy\")) ; cur2 <- getwd() ; cur == cur1  }");
    }

    @Test
    public void testCall() {
        assertEval("{ call(\"f\") }");
        assertEval("{ call(\"f\", 2, 3) }");
        assertEval("{ call(\"f\", quote(A)) }");
        assertEval("{ f <- \"f\" ; call(f, quote(A)) }");
        assertEval("{ f <- round ; call(f, quote(A)) }");
        assertEval("{ f <- function() 23 ; cl <- call(\"f\") ; eval(cl) }");
        assertEval("{ f <- function(a, b) { a + b } ; l <- call(\"f\", 2, 3) ; eval(l) }");
        assertEval("{ f <- function(a, b) { a + b } ; x <- 1 ; y <- 2 ; l <- call(\"f\", x, y) ; x <- 10 ; eval(l) }");
        assertEval("{ cl <- call(\"f\") ; typeof(cl) }");
        assertEval("{ cl <- call(\"f\") ; class(cl) }");
    }

    @Test
    public void testSysCall() {
        assertEval("{ f <- function() sys.call() ; f() }");
        assertEval("{ f <- function(x) sys.call() ; f(x = 2) }");
        assertEval("{ f <- function() sys.call(1) ; g <- function() f() ; g() }");
        assertEval("{ f <- function() sys.call(2) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call(1) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call(-1) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call(-2) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call() ; g <- function() f() ; h <- function() g() ; h() }");

        assertEval("{ f <- function() sys.call() ; typeof(f()[[1]]) }");
        assertEval("{ f <- function(x) sys.call() ; typeof(f(x = 2)[[1]]) }");
        assertEval("{ f <- function(x) sys.call() ; typeof(f(x = 2)[[2]]) }");

        assertEval("{ f <- function(x) sys.call() ; f(2) }");
        assertEval("{ f <- function(x) sys.call() ; g <- function() 23 ; f(g()) }");

        assertEval("{ f <- function(x, y) sys.call() ; f(1, 2) }");
        assertEval("{ f <- function(x, y) sys.call() ; f(x=1, 2) }");
        assertEval("{ f <- function(x, y) sys.call() ; f(1, y=2) }");
        assertEval("{ f <- function(x, y) sys.call() ; f(y=1, 2) }");
        assertEval("{ f <- function(x, y) sys.call() ; f(y=1, x=2) }");

        // fails because can't parse out the "name"
        assertEval(Ignored.Unknown, "{ (function() sys.call())() }");
    }

    @Test
    public void testSysParents() {
        assertEval("{ sys.parents() }");
        assertEval("{ f <- function() sys.parents() ; f() }");
        assertEval("{ f <- function() sys.parents() ; g <- function() f() ; g() }");
        assertEval("{ f <- function() sys.parents() ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x=sys.parents()) x ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=sys.parents()) g(z) ; h() }");

        assertEval(Ignored.Unknown, "{ u <- function() sys.parents() ; f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=u()) g(z) ; h() }");
    }

    @Test
    public void testSysNFrame() {
        assertEval("{ sys.nframe() }");
        assertEval("{ f <- function() sys.nframe() ; f() }");
        assertEval("{ f <- function() sys.nframe() ; g <- function() f() ; g() }");
        assertEval("{ f <- function() sys.nframe() ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x=sys.nframe()) x ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=sys.nframe()) g(z) ; h() }");

        assertEval(Ignored.Unknown, "{ u <- function() sys.nframe() ; f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=u()) g(z) ; h() }");
    }

    @Test
    public void testSimpleRm() {
        assertEval("{ x <- 200 ; rm(\"x\") ; x }");
        assertEval(Output.ContainsWarning, "{ rm(\"ieps\") }");
        assertEval("{ x <- 200 ; rm(\"x\") }");
        assertEval("{ x<-200; y<-100; rm(\"x\", \"y\"); x }");
        assertEval("{ x<-200; y<-100; rm(\"x\", \"y\"); y }");
    }

    @Test
    public void testParen() {
        assertEval(Ignored.Unknown, "{ a = array(1,c(3,3,3)); (a[1,2,3] = 3) }");
    }

    @Test
    public void testGL() {
        assertEval("{x<-gl(2, 8, labels = c(\"Control\", \"Treat\")); print(x)}");
        assertEval("{x<-gl(2, 1, 20); print(x)}");
        assertEval("{x<-gl(2, 2, 20); print(x)}");
        assertEval("{ a <- gl(2, 4, 8) ; print(a) }");
        assertEval("{ b <- gl(2, 2, 8, labels = c(\"ctrl\", \"treat\")) ; print(b) }");
    }

    @Test
    public void testTypeConvert() {
        assertEval("{ x<-as.character(list(a=\"0\", b=\"0\", c=\"0.3\")); type.convert(x, as.is=FALSE) }");
    }
}
