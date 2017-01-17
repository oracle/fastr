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

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestSimpleVectors extends TestBase {

    @Test
    public void testDirectAccess() {
        assertEval("{ x<-c(7,42); `[`(x, 2); }");
        assertEval("{ x<-c(7,42); `[[`(x, 2); }");
        assertEval("{ x<-c(7,42); `[`(x); }");
        assertEval("{ x<-c(7,42); `[[`(x); }");
        assertEval("{ x<-matrix(c(7,42), ncol=2); `[`(x, 1, 2) }");
        assertEval("{ x<-matrix(c(7,42), ncol=2); `[`(x, 1, 2, drop=FALSE) }");
        assertEval("{ x<-matrix(c(7,42), ncol=2); `[`(x, 1, 2, drop=TRUE) }");
        assertEval("{ x<-c(7,aa=42); `[[`(x, \"a\") }");
        assertEval("{ x<-c(7,aa=42); `[[`(x, \"a\", exact=FALSE) }");
        assertEval("{ x<-c(7,aa=42); `[[`(x, \"a\", exact=TRUE) }");

        assertEval("{ x<-list(a=7); `$`(x, \"a\") }");
    }

    @Test
    public void testDirectUpdate() {
        assertEval("{ x<-c(7,42); `[<-`(x, 1, 7); }");
        assertEval("{ x<-c(7,42); `[[<-`(x, 1, 7); }");
        assertEval("{ x<-c(7,42); `[<-`(x, 1); }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(7,42); `[[<-`(x, 1); }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); `[<-`(x, 3, value=\"b\") }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); `[[<-`(x, 3, value=\"b\") }");

        assertEval("{ x<-data.frame(1,2); `[<-.data.frame`(x, 2, value=7) }");
        assertEval("{ x<-data.frame(1,2); `[[<-.data.frame`(x, 2, value=7) }");
        assertEval("{ x<-data.frame(c(1,2), c(3,4)); `[<-.data.frame`(x, 2, 1, 7) }");
        assertEval("{ x<-data.frame(c(1,2), c(3,4)); `[[<-.data.frame`(x, 2, 1, 7) }");

        assertEval("{ x<-c(a=7); class(x)<-\"foo\"; `$<-.foo`<-function(x, name, value) x[name]<-value; `$<-.foo`(x, \"a\", 42); x }");
        assertEval("{ x<-c(a=7); class(x)<-\"foo\"; `$<-.foo`<-function(x, name, value) x[name]<-value; `$<-`(x, \"a\", 42); x }");
        assertEval("{ x<-data.frame(a=7, b=42); `$<-`(x, \"a\", 1); x }");

    }

    @Test
    public void testObject() {
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[3] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[3, drop=FALSE] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[3, drop=TRUE] }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[[3]] }");
        assertEval("{ x<-factor(c(\"a\", zz=\"b\", \"a\")); x[[\"z\", exact=FALSE]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-factor(c(\"a\", zz=\"b\", \"a\")); x[[\"z\", exact=TRUE]] }");

        assertEval("{ x<-data.frame(1,2); x[2]<-7; x }");
        assertEval("{ x<-data.frame(1,2); x[[2]]<-7; x }");
        assertEval("{ x<-data.frame(c(1,2), c(3,4)); x[2,1]<-7; x }");
        assertEval("{ x<-data.frame(c(1,2), c(3,4)); x[[2,1]]<-7; x }");

        assertEval("{ `[.foo` <- function(x, i, ...) structure(NextMethod(\"[\"), class = class(x)); x<-c(1,2); class(x)<-\"foo\"; x[1] }");

        assertEval("{ x<-c(a=6); class(x)<-\"foo\"; `$.foo`<-function(x, name) x[name]+1; x$a }");
        assertEval("{ x<-data.frame(a=7, b=42); x$a }");
        assertEval("{ x<-data.frame(a=7, b=42); x$a<-1; x }");
    }

    @Test
    public void testObjectDirectAccess() {
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); `[.factor`(x, 1) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); `[.factor`(x, 1, drop=TRUE) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); `[.factor`(x, 1, drop=FALSE) }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); `[[.factor`(x, 1) }");
        assertEval("{ x<-factor(c(\"a\", z=\"b\", \"a\")); `[[.factor`(x, \"z\") }");
        assertEval(Output.IgnoreErrorContext, "{ x<-factor(c(\"a\", zz=\"b\", \"a\")); `[[.factor`(x, \"z\", exact=TRUE) }");
        assertEval("{ x<-factor(c(\"a\", zz=\"b\", \"a\")); `[[.factor`(x, \"z\", exact=FALSE) }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); `[`(x, 1) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); `[`(x, 1, drop=FALSE) }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); `[[`(x, 1) }");
        assertEval("{ x<-factor(c(\"a\", zz=\"b\", \"a\")); `[[`(x, \"z\", exact=FALSE) }");

        assertEval("{ x<-c(a=6); class(x)<-\"foo\"; `$.foo`<-function(x, name) x[name]+1; `$.foo`(x, \"a\") }");
        assertEval("{ x<-c(a=6); class(x)<-\"foo\"; `$.foo`<-function(x, name) x[name]+1; `$`(x, \"a\") }");
        assertEval("{ x<-data.frame(a=7, b=42); `$`(x, \"a\") }");
    }

    @Test
    public void testObjectDefaultAccess() {
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); .subset(x, 1) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); .subset2(x, 1) }");
    }

    @Test
    public void testFunctionAccess() {
        assertEval("{ x<-matrix(1:4, ncol=2); y<-`[`(x); y }");
        assertEval("{ x<-matrix(1:4, ncol=2); y<-`[`(x, drop=TRUE); y }");
        assertEval("{ x<-matrix(1:4, ncol=2); y<-`[`(x, drop=FALSE); y }");
        assertEval("{ x<-matrix(1:4, ncol=2); y<-`[`(x, 2); y }");
        assertEval("{ x<-matrix(1:4, ncol=2); y<-`[`(x, 1, 2); y }");
        assertEval("{ x<-matrix(1:4, ncol=2); y<-`[`(x, 1, 2, drop=FALSE); y }");
        assertEval("{ x<-matrix(1:4, ncol=2); y<-`[`(x, drop=FALSE, 1, 2); y }");
        assertEval("{ x<-matrix(1:4, ncol=2); y<-`[[`(x); y }");
        assertEval("{ x<-matrix(1:4, ncol=2); y<-`[[`(x, exact=FALSE); y }");
        assertEval("{ x<-matrix(1:4, ncol=2, dimnames=list(c(\"a\", \"b\"), c(\"c\", \"dd\"))); `[[`(x, \"a\", \"dd\") }");
        assertEval("{ x<-matrix(1:4, ncol=2, dimnames=list(c(\"a\", \"b\"), c(\"c\", \"dd\"))); `[[`(x, \"a\", \"d\") }");
        assertEval("{ x<-matrix(1:4, ncol=2, dimnames=list(c(\"a\", \"b\"), c(\"c\", \"dd\"))); `[[`(x, \"a\", \"d\", exact=FALSE) }");
        assertEval("{ x<-matrix(1:4, ncol=2, dimnames=list(c(\"a\", \"b\"), c(\"c\", \"dd\"))); `[[`(x, exact=FALSE, \"a\", \"d\") }");
        assertEval("{ x<-list(7, list(1, 42)); y<-`[[`(x, c(2,2)); y }");

        assertEval("{ x<-matrix(1:4, ncol=2); y<-.subset(x); y }");
        assertEval("{ x<-matrix(1:4, ncol=2); y<-.subset2(x); y }");
    }

    @Test
    public void testScalarIntIndexScalarValueUpdateOnVector() {
        // Update logical vector
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- FALSE; x }");
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- 100L; x }");
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- 100; x }");
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- \"hello\"; x }");

        // Update int vector
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- FALSE; x }");
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- 100L; x }");
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- 100; x }");
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- \"hello\"; x }");

        // Update double vector
        assertEval("{ x<-c(1,2,3); x[2.3] <- FALSE; x }");
        assertEval("{ x<-c(1,2,3); x[2.3] <- 100; x }");
        assertEval("{ x<-c(1,2,3); x[2.3] <- \"hello\"; x }");

        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2.3] <- 100i; x }");
        assertEval("{ x<-c(1L,2L,3L); x[2.3] <- 100i; x }");
        assertEval("{ x<-c(1,2,3); x[2.3] <- 100i; x }");
    }

    @Test
    public void testScalarIntIndexOnVector() {
        assertEval("{ x<-c(1,2,3); x[1L] }");
        assertEval("{ x<-c(1,2,3); x[2L] }");
        assertEval("{ x<-c(1,2,3); x[3L] }");
        assertEval("{ x<-c(1L,2L,3L); x[1L] }");
        assertEval("{ x<-c(1L,2L,3L); x[2L] }");
        assertEval("{ x<-c(1L,2L,3L); x[3L] }");
        assertEval("{ x<-1:3; x[1L] }");
        assertEval("{ x<-1:3; x[2L] }");
        assertEval("{ x<-1:3; x[3L] }");
        assertEval("{ x<-3:1; x[1L] }");
        assertEval("{ x<-3:1; x[2L] }");
        assertEval("{ x<-3:1; x[3L] }");
    }

    @Test
    public void testSequenceIntIndexOnVector() {
        assertEval("{ x<-c(1L,2L,3L); x[1:3] }");
        assertEval("{ x<-c(1L,2L,3L); x[1:2] }");
        assertEval("{ x<-c(1L,2L,3L); x[2:3] }");
        assertEval("{ x<-c(1L,2L,3L); x[1:1] }");
        assertEval("{ x<-c(1L,2L,3L); x[0:3] }");
        assertEval("{ x<-c(1,2,3); x[1:3] }");
        assertEval("{ x<-c(1,2,3); x[1:2] }");
        assertEval("{ x<-c(1,2,3); x[2:3] }");
        assertEval("{ x<-c(1,2,3); x[1:1] }");
        assertEval("{ x<-c(1,2,3); x[0:3] }");
    }

    @Test
    public void testScalarDoubleIndexOnVector() {
        assertEval("{ x<-c(1,2,3); x[1.1] }");
        assertEval("{ x<-c(1,2,3); x[2.1] }");
        assertEval("{ x<-c(1,2,3); x[3.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[1.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[2.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[3.1] }");
        assertEval("{ x<-1:3; x[1.1] }");
        assertEval("{ x<-1:3; x[2.1] }");
        assertEval("{ x<-1:3; x[3.1] }");
        assertEval("{ x<-3:1; x[1.1] }");
        assertEval("{ x<-3:1; x[2.1] }");
        assertEval("{ x<-3:1; x[3.1] }");
    }

    @Test
    public void testScalarIntNegativeIndexOnVector() {
        assertEval("{ x<-c(1,2,3); x[-1L] }");
        assertEval("{ x<-c(1,2,3); x[-2L] }");
        assertEval("{ x<-c(1,2,3); x[-3L] }");
        assertEval("{ x<-c(1L,2L,3L); x[-1L] }");
        assertEval("{ x<-c(1L,2L,3L); x[-2L] }");
        assertEval("{ x<-c(1L,2L,3L); x[-3L] }");
        assertEval("{ x<-1:3; x[-1L] }");
        assertEval("{ x<-1:3; x[-2L] }");
        assertEval("{ x<-1:3; x[-3L] }");
    }

    @Test
    public void testScalarDoubleNegativeIndexOnVector() {
        assertEval("{ x<-c(1,2,3); x[-1.1] }");
        assertEval("{ x<-c(1,2,3); x[-2.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[-1.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[-2.1] }");
        assertEval("{ x<-1:3; x[-1.1] }");
        assertEval("{ x<-1:3; x[-2.1] }");
        assertEval("{ x<-c(1,2,3); x[-3.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[-3.1] }");
        assertEval("{ x<-1:3; x[-3.1] }");
    }

    @Test
    public void testMoreVectors() {
        assertEval("{ x<-NULL; x[1L] }");
        assertEval("{ x<-NULL; x[2L] }");
        assertEval("{ x<-NULL; x[3L] }");
        assertEval("{ x<-1.1:3.1; x[1L] }");
        assertEval("{ x<-1.1:3.1; x[2L] }");
        assertEval("{ x<-1.1:3.1; x[3L] }");
        assertEval("{ x<-3.1:1; x[1L] }");
        assertEval("{ x<-3.1:1; x[2L] }");
        assertEval("{ x<-3.1:1; x[3L] }");
    }

    @Test
    public void testScalarIntAsVector() {
        assertEval("{ x<-1L; x[1L] }");
        assertEval("{ x<-1L; x[0L] }");
        assertEval("{ x<-1L; x[2L] }");
        assertEval("{ x<-1L; x[-1L] }");
        assertEval("{ x<-1L; x[-2L] }");
        assertEval("{ x<-1L; x[TRUE] }");
        assertEval("{ x<-1L; x[FALSE] }");
        assertEval("{ x<-1L; x[NA] }");
    }

    @Test
    public void testAccessSequence() {
        assertEval("{ x<-c(1L,2L,3L,4L,5L); x[1:4][1:3][1:2][1:1] }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L); x[2:5][2:4][2:3][2:2] }");
        assertEval("{ x<-c(1L,2L,3L,4L,5L); x[1:5][2:5][2:4][2:2] }");
    }

    @Test
    public void testScalarDoubleAsVector() {
        assertEval("{ x<-1; x[1L] }");
        assertEval("{ x<-1; x[0L] }");
        assertEval("{ x<-1; x[2L] }");
        assertEval("{ x<-1; x[-1L] }");
        assertEval("{ x<-1; x[-2L] }");
        assertEval("{ x<-1; x[TRUE] }");
        assertEval("{ x<-1; x[FALSE] }");
        assertEval("{ x<-1; x[NA] }");
    }

    @Test
    public void testMoreVectorsOther() {

        assertEval("{ x<-matrix(1:4, ncol=2, dimnames=list(c(\"a\", \"b\"), c(\"c\", \"d\"))); dimnames(x)[[1]][1]<-\"z\"; x }");

        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[1L] }");
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[2L] }");
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[3L] }");

        assertEval("{ v<-c(a=\"foo\", aa=\"me\"); v[\"a\"] }");
        assertEval("{ x<-c(1,2); x[c(\"a\")] }");
        assertEval("{ x<-c(1,2); x[[c(\"a\")]] }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); x[c(\"a\")] }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); x[[c(\"a\")]] }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); x[\"a\", \"b\"] }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); x[[\"a\", \"b\"]] }");
        assertEval("{ x<-c(1,2); x[c(\"a\", \"b\")] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2); x[[c(\"a\", \"b\")]] }");
        assertEval("{ x<-1:2; x[c(TRUE, TRUE)] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:2; x[[c(TRUE, TRUE)]] }");
        assertEval("{ x<-1:2; dim(x)<-c(1,2); x[2+2i, 2+2i] }");
        assertEval("{ x<-1:2; dim(x)<-c(1,2); u<-2+2i; x[[u, u]] }");
        assertEval("{ x<-2; x[NULL] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-2; x[[NULL]] }");
        assertEval("{ x<-1; x[] }");
        assertEval("{ x<-1; x[[]] }");
        assertEval("{ x<-1:2; dim(x)=c(1,2); x[,] }");
        assertEval("{ x<-1:2; dim(x)=c(1,2); x[[, ]] }");
        assertEval("{ x<-1:2; dim(x)=c(1,2); x[,1] }");
        assertEval("{ x<-1:2; dim(x)=c(1,2); x[[, 1]] }");
        assertEval("{ x<-1:2; dim(x)=c(1,2); x[[1, ]] }");
        // Checkstyle: stop
        assertEval("{ x<-1:2; dim(x)=c(1,1,2); x[[1, , 1]] }");
        assertEval("{ x<-1:2; dim(x)=c(1,1,2); x[[, , 1]] }");
        assertEval("{ x<-1:2; dim(x)=c(1,1,2); x[[1, , ]] }");
        // Checkstyle: resume
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(\"a\", c(\"b\", \"c\")); x[\"z\", \"x\"] }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(\"a\", c(\"b\", \"c\")); x[[\"z\", \"x\"]] }");
        assertEval("{ x<-c(a=1, b=2); x[c(\"z\", \"x\")] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(a=1, b=2); x[[c(\"z\", \"x\")]] }");
        assertEval("{ x<-as.integer(1:4); x[[as.integer(NA)]] }");
        assertEval("{ x<-as.integer(1:4); dim(x)<-c(2,2); x[[as.integer(NA)]] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[0,0]<-integer(); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[0, 0]] <- integer(); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[-1, 0]] <- integer(); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[0, -1]] <- integer(); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[-5, 0]] <- integer(); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[0, -5]] <- integer(); x }");

        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-1,0]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[0,-1]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-5,0]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[0,-5]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-1] }");
        assertEval("{ x<-list(1); x[[c(1, 1)]] }");
        assertEval("{ x<-list(1); x[[c(1, 2)]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1; x[[c(1, 1)]] }");
        assertEval("{ x<-list(list(1,42)); x[[c(1, 2)]] }");
        assertEval("{ x<-list(list(1,list(42))); x[[c(1, 2)]] }");
        assertEval("{ x<-list(list(1,list(42,list(143)))); x[[c(1, 2, 2)]] }");
        assertEval("{ x<-list(1); x[[c(1, 1, 1)]] }");
        assertEval("{ x<-list(1); x[[c(1, 2, 0)]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1); x[[c(1, 0)]] }");
        assertEval("{ x<-list(1); x[[c(1, NA)]] }");
        assertEval("{ x<-list(1); x[[c(TRUE, TRUE)]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1); x[[c(TRUE, FALSE)]] }");
        assertEval("{ x<-list(1); x[[NA]] }");
        assertEval("{ x<-list(1); x[[c(NA)]] }");
        assertEval("{ x<-c(1); x[[NA]] }");
        assertEval("{ x<-list(1); x[[as.integer(NA)]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1); x[[NULL]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1); x[[c(NULL)]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1); x[[0]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1); x[[c(0)]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1); x[[-1]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3); x[[-1]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3); x[[-5]] }");
        assertEval("{ x<-list(42,2,3); x[[c(NA, 1)]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(42,2,3); x[[c(0, 1)]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(42,2,3); x[[c(1, -1)]] }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(42,2,3); x[[c(-1, 1)]] }");
        assertEval("{ x<-list(42,2,3); x[[c(NULL,1)]] }");
        assertEval("{ x<-list(42,2,3); x[[c(NULL, NULL,1)]] }");
        assertEval("{ x<-list(42,2,3); x[[c(1, NULL, 2)]] }");
        assertEval("{ x<-list(42,2,3); x[[c(1, 2)]] }");
        assertEval("{ x<-list(42,2,3); x[[c(NULL, 2,1)]] }");
        assertEval("{ x<-list(42,2,3); x[[c(NULL, 2, 1, 3)]] }");
        assertEval("{ x<-list(42,2,3); x[[c(NULL, 2, NULL, 1, 3)]] }");
        assertEval("{ x<-list(42,2,3); x[[c(2, 1, 3)]] }");
        assertEval("{ x<-c(a=1, b=2); x[1] }");
        assertEval("{ x<-c(a=1, b=2); x[[1]] }");
        // GnuR says "attempt to select less than one element", FastR says "... more ..."
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:8; dim(x)<-c(2,2,2); x[[-3, 1, 1]] }");

        assertEval("{ x<-list(1); x[\"y\"] }");
        assertEval("{ x<-list(1); x[[\"y\"]] }");
        assertEval("{ x<-list(a=42); x[\"b\"] }");
        assertEval("{ x<-list(a=42); x[[\"b\"]] }");
        assertEval("{ x<-list(a=42); x[\"a\"] }");
        assertEval("{ x<-list(a=42); x[[\"a\"]] }");
        assertEval("{ x<-list(a=42); x[[c(\"a\", \"y\")]] }");
        assertEval("{ x<-list(a=list(42)); x[[c(\"a\", \"y\")]] }");
        assertEval("{ x<-list(a=list(b=42)); x[[c(\"a\", \"b\")]] }");

        assertEval(Output.IgnoreErrorMessage, "{ l<-list(1,2,3,4); l[[c(NA,1)]]<-c(1); l }");
        assertEval(Output.IgnoreErrorContext, "{ l<-list(1,2,3,4); l[[c(1,NA)]]<-c(1); l }");
        assertEval(Output.IgnoreErrorContext, "{ l<-list(1,2,3,4); l[[c(7,1)]]<-c(1); l }");
        assertEval(Output.IgnoreErrorContext, "{ l<-list(1,2,3,4); l[[c(NA)]]<-c(1); l }");
        assertEval(Output.IgnoreErrorMessage, "{ l<-list(1,2,3,4); l[[c(NA,1)]]<-c(-1); l }");
        assertEval(Output.IgnoreErrorContext, "{ l<-list(1,2,3,4); l[[c(-1)]]<-c(1); l }");
        assertEval(Output.IgnoreErrorContext, "{ l<-list(1,2,3,4); l[[c(-1,1)]]<-c(1); l }");
        assertEval(Output.IgnoreErrorContext, "{ l<-list(1,2,3,4); l[[c(0)]]<-c(1); l }");
        assertEval(Output.IgnoreErrorContext, "{ l<-list(1,2,3,4); l[[c(0,1)]]<-c(1); l }");
        assertEval(Output.IgnoreErrorContext, "{ l<-list(1,2,3,4); l[[c(1,1,1)]]<-c(1); l }");
        assertEval(Output.IgnoreErrorContext, "{ l<-list(list(1),2,3,4); l[[c(1,1,NA)]]<-c(1); l }");
        assertEval("{ l<-list(1,2,3,4); l[[c(2,1)]]<-7; l }");
        assertEval("{ l<-list(1,2,3,4); l[c(2,1)]<-7; l }");

        assertEval(Output.IgnoreWarningContext, "{ x<-1:4; x[1]<-c(1,1); x }");
        assertEval("{ x<-1:4; x[[1]]<-c(1,1); x }");
        assertEval("{ x<-1; x[0]<-integer(); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1; x[[0]]<-integer(); x }");
        assertEval("{ x<-1; x[1]<-integer(); x }");
        assertEval("{ x<-1; x[]<-42; x }");
        assertEval("{ x<-1; x[[]]<-42; x }");
        assertEval("{ x<-7; x[NA]<-42; x }");
        assertEval("{ x<-7; x[NA]<-c(42, 7); x }");
        assertEval("{ x<-7; x[NULL]<-42; x }");
        assertEval("{ x<-7; x[0]<-42; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-7; x[[NA]]<-42; x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-7; x[[NA]]<-c(42, 7); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-7; x[[NULL]]<-42; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-7; x[[0]]<-42; x }");
        assertEval("{ x<-1:4;  x[c(1, 0)]<-42; x }");
        assertEval("{ x<-1:4;  x[c(0, 1)]<-42; x }");
        assertEval(Output.IgnoreWarningContext, "{ x<-1:4;  x[c(1, 0)]<-c(7, 42); x }");
        assertEval(Output.IgnoreWarningContext, "{ x<-1:4;  x[c(0, 1)]<-c(7, 42); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[NULL]<-42; x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[NA]<-42; x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[0]<-42; x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1,4)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[[c(1,4)]]<-c(42, 43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1,NA)]<-c(42, 43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(NA,1)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[[c(1,0)]]<-c(42, 43); x }");
        assertEval(Output.IgnoreWarningContext, "{ x<-1:4; dim(x)<-c(2,2); x[c(1,0)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[[c(1,0,0)]]<-c(42, 43); x }");
        assertEval(Output.IgnoreWarningContext, "{ x<-1:4; dim(x)<-c(2,2); x[c(1,0,0)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[[c(1,1,0)]]<-c(42, 43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1,1,0)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(0,1)]]<-c(42, 43); x }");
        assertEval(Output.IgnoreWarningContext, "{ x<-1:4; dim(x)<-c(2,2); x[c(0,1)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(0,0,1)]]<-c(42, 43); x }");
        assertEval(Output.IgnoreWarningContext, "{ x<-1:4; dim(x)<-c(2,2); x[c(0,0,1)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(0,1,1)]]<-c(42, 43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(0,1,1)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(0,0)]]<-c(42, 43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(0,0)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(0,0,0)]]<-c(42, 43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(0,0,0)]<-c(42, 43); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[[c(FALSE,TRUE)]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[[c(FALSE,TRUE,TRUE)]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[complex()]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(1+1i)]]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[[c(1+1i)]]<-c(42); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(1+1i)]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(1+1i,42+7i,3+3i)]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(0,42+7i)]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[c(1+1i)]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1+1i)]<-c(42); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[complex()]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1+1i)]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1+1i,42+7i,3+3i)]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(0,42+7i)]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[[c(0,0,42+71)]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(integer()))]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42))]]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42))]]<-c(43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42))]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42), as.raw(7))]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[c(as.raw(42), as.raw(7), as.raw(1))]]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(integer()))]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42))]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42))]<-c(43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42))]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42), as.raw(7))]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(as.raw(42), as.raw(7), as.raw(1))]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list()]]<-integer(); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list()]]<-c(42); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list()]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1)]]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[[list(1)]]<-c(42); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1)]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2)]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2,3)]]<-c(42,43); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[list()]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list()]<-c(42); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list()]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list(1)]<-integer(); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list(1)]<-c(42); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list(1)]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2)]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2,3)]<-c(42,43); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-1]<-42; x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[-5]<-42; x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(-1, -2)]<-42; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[[c(-1, -2)]]<-42; x }");
        assertEval("{ l <- list(1,2,3) ; l[c(1,3)] <- NULL ; l }");
        assertEval("{ l <- list(1,2,3) ; l[c(1,3)] <- c(NULL, 42) ; l }");
        assertEval("{ l <- list(1,2,3) ; l[c(1,3)] <- c(NULL, NULL) ; l }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1,1]<-NULL; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1,1]]<-NULL; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1]<-NULL; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1]]<-NULL; x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1,1]<-NULL; x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1]<-NULL; x }");
        assertEval("{ x<-1:4; x[1]<-NULL; x }");
        assertEval("{ x<-1:4; x[0]<-NULL; x }");
        assertEval("{ n<-1; n[7]<-42; n }");
        assertEval("{ n<-1; n[[7]]<-42; n }");
        assertEval("{ n<-1; n[c(7,8)]<-c(42,43); n }");
        assertEval(Output.IgnoreErrorContext, "{ n<-1; n[[c(7,8)]]<-c(42,43); n }");
        assertEval("{ x<-NULL; x[1]<-42; x }");
        assertEval("{ x<-NULL; x[1]<-42+7i; x }");
        assertEval("{ x<-NULL; x[7]<-42; x }");
        assertEval("{ x<-NULL; x[1,1]<-NULL }");
        assertEval("{ x<-NULL; x[1,1]<-42; x }");
        assertEval("{ x<-NULL; x[[1,1]]<-42; x }");
        assertEval("{ x<-NULL; x[1,1,1]<-42; x }");
        assertEval("{ x<-NULL; x[[1,1,1]]<-42; x }");
        assertEval("{ x<-1; x[1,1]<-42; x }");
        assertEval("{ x<-1; x[[1,1]]<-42; x }");
        assertEval("{ x<-1; x[1,1,1]<-42; x }");
        assertEval("{ x<-1; x[[1,1,1]]<-42; x }");
        assertEval("{ x<-c(a=1); x[\"b\"]<-2; x }");
        assertEval("{ x<-c(a=1); x[c(\"a\",\"b\")]<-c(7,42); x }");
        assertEval("{ x<-c(a=1); x[c(\"a\",\"b\",\"b\")]<-c(7,42,100); x }");
        assertEval("{ x<-NULL; x[c(\"a\", \"b\")]<-42L; x }");
        assertEval("{ x<-c(1,2); dim(x)<-2; attr(x, \"foo\")<-\"foo\"; x[\"a\"]<-42; attributes(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-2; attr(x, \"foo\")<-\"foo\"; x[1]<-42; attributes(x) }");
        assertEval("{ x <- NULL; x[c(\"a\", as.character(NA))] <- 7; x }");
        assertEval("{ x <- NULL; x[c(\"a\", as.character(NA), as.character(NA))] <- 7; x }");
        assertEval("{ b<-3:5; dim(b) <- c(1,3) ; b[] <- NULL ; b }");
        assertEval("{ b<-3:5; dim(b) <- c(1,3) ; b[c(FALSE, FALSE, FALSE)] <- NULL ; b }");
        assertEval(Output.IgnoreErrorContext, "{ b<-3:5; dim(b) <- c(1,3) ; b[[c(1,2)]] <- NULL ; b }");
        assertEval("{ b<-3:5; dim(b) <- c(1,3) ; b[c(1,2)] <- NULL ; b }");
        assertEval("{ b<-3:5; dim(b) <- c(1,3) ; b[c(1)] <- NULL ; b }");
        assertEval("{ b<-3:5; dim(b) <- c(1,3) ; b[0] <- NULL ; b }");

        assertEval("{ l<-list(1,2,3,4); l[1]<-NULL; l }");
        assertEval("{ l<-list(1,2,3,4); l[4]<-NULL; l }");
        assertEval("{ l<-list(1,2,3,4); l[5]<-NULL; l }");
        assertEval("{ l<-list(1,2,3,4); l[7]<-NULL; l }");
        assertEval("{ l<-list(1); l[1]<-NULL; l }");
        assertEval("{ x<-list(list(1,list(42))); x[[c(1, 2)]]<-7; x }");
        assertEval("{ x<-list(list(1,list(42,list(143)))); x[[c(1, 2, 2)]]<-7; x }");
        assertEval("{ x<-list(list(1,list(42,list(143)))); x[[c(1, 2, 7)]]<-7; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(list(1,list(42,list(143)))); x[[c(1, 2, 7, 7)]]<-7; x }");
        assertEval("{ x<-list(list(1,list(42,list(list(143))))); x[[c(1, 2, 2, 1)]]<-7; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(list(1,list(42,list(list(143))))); x[[c(1, NA, 2, 1)]]<-7; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(list(1,list(42,list(list(143))))); x[[c(1, 2, 2, NA)]]<-7; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1, list(42)); x[[c(-3, 1)]]<-7; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1, 2, list(42)); x[[c(-1, 1)]]<-7; x }");
        assertEval("{ x<-list(1, list(42, 1)); x[[c(-1, -2)]]<-7; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1, list(42, 1)); x[[c(-1, -3)]]<-7; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1, list(42, 1, 2)); x[[c(-1, -2)]]<-7; x }");
        assertEval("{ x<-list(1, list(42)); x[[c(-1, 1)]]<-7; x }");
        assertEval("{ x<-list(1, list(42)); x[[c(2, 5)]]<-7; x }");
        assertEval("{ x<-list(1, list(42)); x[c(2, 5)]<-7; x }");
        assertEval("{ x<-list(1, list(42)); dim(x)<-c(1,2); x[[c(2, 5)]]<-7; x }");
        assertEval("{ x<-list(1, list(42)); dim(x)<-c(1,2); x[c(2, 5)]<-7; x }");
        assertEval("{ y<-list(42,7); dim(y)<-c(1:2); attr(y, \"foo\")<-\"foo\"; x<-list(1, y); dim(x)<-c(1,2); x[[c(2, 1)]]<-7; x[2] }");
        assertEval("{ l<-list(1,2,3,4); l[c(1,3)]<-list(NULL); l }");
        assertEval("{ x<-list(1, list(42)); x[[c(2, 1)]]<-NULL; x }");
        assertEval("{ x<-list(1, list(42)); x[[c(2, 5)]]<-NULL; x }");
        assertEval("{ x<-list(1, list(42)); x[[c(-1, 1)]]<-NULL; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list() ; x[[NA]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(1) ; x[[NA]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(1,2) ; x[[NA]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(1,2,3) ; x[[NA]] <- NULL ; x }");
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,6,7)] <- NULL ; m }");
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,6,8)] <- NULL ; m }");
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,7)] <- NULL ; m }");
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,8)] <- NULL ; m }");
        assertEval("{ m <- matrix(list(1,2,3,4,5,6), nrow=3) ; m[c(2,3,6,8,9)] <- NULL ; m }");
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[] <- NULL ; b }");
        assertEval("{ b <- as.list(3:5) ; dim(b) <- c(1,3) ; b[NULL] <- NULL ; b }");
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[c(1,2)] <- NULL ; b }");
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[c(1)] <- NULL ; b }");
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[[c(1)]] <- NULL ; b }");
        assertEval(Output.IgnoreErrorContext, "{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[[0]] <- NULL ; b }");
        assertEval("{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[0] <- NULL ; b }");
        assertEval("{ l <- list(a=1,b=2) ; attr(l, \"foo\")<-\"foo\"; l[1] <- NULL ; l }");
        assertEval("{ l <- list(a=1,b=2) ; attr(l, \"foo\")<-\"foo\"; l[[1]] <- NULL ; l }");
        assertEval("{ l <- matrix(list(a=1,b=2,c=3,d=4)) ; attr(l, \"foo\")<-\"foo\"; l[1] <- NULL ; attributes(l) }");
        assertEval("{ l <- matrix(list(a=1,b=2,c=3,d=4)) ; attr(l, \"foo\")<-\"foo\"; l[[1]] <- NULL ; attributes(l) }");
        assertEval("{ l <- list(1,2) ; names(l)<-c(\"a\", \"b\"); attr(l, \"foo\")<-\"foo\"; l[1] <- NULL ; attributes(l) }");
        assertEval("{ l <- list(1,2) ;  attr(l, \"foo\")<-\"foo\"; names(l)<-c(\"a\", \"b\"); l[1] <- NULL ; attributes(l) }");
        assertEval("{ l <- c(1,2) ; names(l)<-c(\"a\", \"b\"); attr(l, \"foo\")<-\"foo\"; l[1] <- 7 ; attributes(l) }");
        assertEval("{ l <- c(1,2) ;  attr(l, \"foo\")<-\"foo\"; names(l)<-c(\"a\", \"b\"); l[1] <- 7 ; attributes(l) }");

        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1, NA), 2]<-7; x }");
        assertEval("{ x<-1:4; x[c(1, NA)]<-7; x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(1, NA)]<-7; x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2);  x[NA, NA]<-7; x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(NA, NA),1]<-7; x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(NA, 1),1]<-7; x }");
        assertEval("{ x<-1:4; x[c(1, NA)]<-c(7, 42); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[c(NA, 1),1]<-c(7, 42); x }");

        assertEval(Output.IgnoreErrorContext, "{ x<-c(1); x[[-4]]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1); x[[-4]]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3); x[[-4]]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3); x[[-4]]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3); x[[-1]]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3); x[[-1]]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1); x[[-4]]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3); x[[-4]]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3); x[[-1]]<-NULL }");

        assertEval("{ x<-c(5,10); names(x)<-c(101, 102); names(x)[1]<-42; x }");

        assertEval("{ x<-c(\"a\", \"b\"); dim(x)<-c(2,1); dimnames(x)<-list(c(\"Z\", \"X\"), NULL); x[, \"Z\"] }");
        assertEval("{ x<-c(1, 2); dim(x)<-c(2,1); dimnames(x)<-list(c(\"b\", \"c\"), NULL); x[1,] }");
        assertEval("{ x<-c(1, 2); dim(x)<-c(2,1); dimnames(x)<-list(c(\"b\", \"c\"), \"d\"); x[1,] }");
        assertEval("{ x<-c(1, 2); dim(x)<-c(1,2); dimnames(x)<-list(NULL, c(\"b\", \"c\")); x[,1] }");
        assertEval("{ x<-c(1, 2); dim(x)<-c(1,2); dimnames(x)<-list(\"a\", c(\"b\", \"c\")); x[,1] }");
        assertEval("{ x<-c(1, 2); dim(x)<-c(2,1,1); dimnames(x)<-list(c(\"b\", \"c\"), \"a\", NULL); x[1,,] }");
        assertEval("{ x<-c(1, 2); dim(x)<-c(2,1,1); dimnames(x)<-list(c(\"b\", \"c\"), NULL, \"a\"); x[1,,] }");
        assertEval("{ x<-c(1, 2); dim(x)<-c(2,1,1); dimnames(x)<-list(c(\"b\", \"c\"), NULL, NULL); x[1,,] }");

        assertEval("{ x <- c(\"a\", \"b\"); y<-NULL; y[integer()]<-x[integer()]; y }");
        assertEval("{ x <- c(\"a\", \"b\"); y<-c(\"c\",\"d\"); y[integer()]<-x[integer()]; y}");

        assertEval("{ x<-c(1,2); y<-list(a=x); names(y[1])<-\"c\"; names(y[1]) }");
        assertEval("{ x<-c(1,2); y<-list(a=x); names(y[1])<-\"c\"; names(y[[1]]) }");
        assertEval("{ x<-c(1,2); y<-list(a=x); names(y[[1]])<-\"c\"; names(y[1]) }");
        assertEval("{ x<-c(1,2); y<-list(a=x); names(y[[1]])<-\"c\"; names(y[[1]]) }");
        assertEval("{ x<-c(1,2); y<-list(a=x); names(y$a)<-\"c\"; names(y$a) }");

        assertEval("{ v <- c(1,2,3,4,5,6,7,8,9); f <- function(k) v[k]; f(2:5); f(-1:-2) }");

        assertEval("{ x <- NULL;  x[[\"names\", exact=TRUE]] }");
        assertEval("{ x<-list(aa=1, ba=2); x[[\"a\", exact=FALSE]] }");
        assertEval("{ x<-list(aa=1, ba=2); x[[exact=FALSE, \"a\"]] }");
        assertEval("{ x<-list(aa=1, ba=2); x[[\"a\", exact=TRUE]] }");
        assertEval("{ x<-list(aa=1, ba=2); x[[exact=TRUE], \"a\"] }");
        assertEval("{ x<-list(ab=1, ac=2); x[[\"a\", exact=FALSE]] }");

        assertEval("{ x<-matrix(1:4, ncol=2, dimnames=list(c(m=\"a\", \"b\"), c(\"c\", \"d\"))); dimnames(x)[[1]]$m<-\"z\"; x }");

        assertEval("{ x<-matrix(1:4, ncol=2, dimnames=list(m=c(\"a\", \"b\"), n=c(\"c\", \"d\"))); dimnames(x)$m[1]<-\"z\"; x }");

        assertEval(Output.IgnoreErrorContext, "{ x<-c(aa=1, b=2); dim(x)<-c(1,2); x[\"a\", exact=FALSE]<-7; x }");

        assertEval("{ f<-function(v, ...) v[...]; x<-matrix(1:4, ncol=2); f(x, 1, 2) }");
        assertEval("{ f<-function(v, i, ...) v[i, ...]; x<-matrix(1:4, ncol=2); f(x, 1, 2) }");
        assertEval("{ f<-function(v, val, ...) { v[...]<-val; v; } ; x<-matrix(1:4, ncol=2); f(x, 7, 1, 2) }");
        assertEval("{ f<-function(v, val, i, ...) { v[i, ...]<-val; v; } ; x<-matrix(1:4, ncol=2); f(x, 7, 1, 2) }");

        assertEval("{ x<-matrix(1:4, ncol=2); x[] }");
        assertEval("{ x<-matrix(1:4, ncol=2); x[alist(a=)[[1]]] }");
        assertEval("{ x<-matrix(1:4, ncol=2); x[1,alist(a=)[[1]]] }");
        assertEval("{ z<-1; s<-substitute(z); x<-matrix(1:4, ncol=2); x[s] }");
        assertEval("{ z<-1; s<-substitute(z); x<-matrix(1:4, ncol=2); x[s]<-1; }");

        assertEval("{ x<-list(data=list(matrix(1:4, ncol=2))); x$data[[1]][2,2]<-42; x }");

        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; x[[1]]<-NULL; x }");

        assertEval("{ f<-function(x,y) sys.call(); x<-f(7, 42); x[c(1,2)] }");
        assertEval("{ f<-function(x,y) sys.call(); x<-f(7, 42); typeof(x[c(1,2)]) }");
        assertEval("{ f<-function(x,y) sys.call(); x<-f(7, 42); typeof(x[c(1,2)][[1]]) }");
        assertEval("{ f<-function(x,y) sys.call(); x<-f(7, 42); typeof(x[c(1,2)][[2]]) }");
        assertEval("{ f<-function(x,y) sys.call(); x<-f(7, 42); x[c(2,3)] }");
        assertEval("{ f<-function(x,y) sys.call(); x<-f(7, 42); x[-1]}");

        /*
         * These tests, which should generate errors, appear to be non-deterministic in GnuR in the
         * error message produced. It is either "more elements supplied than there are to replace"
         * or some type-error, e.g. "incompatible types (from NULL to double) in [[ assignment". We
         * could address this with a whitelist.
         */
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1,1]]<-NULL; x }");
        assertEval(Output.IgnoreErrorMessage, "{ b<-as.list(3:5); dim(b) <- c(1,3) ; b[[c(1,2)]] <- NULL ; b }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3); x[[-4]]<-NULL }");
        // this came from testRawIndex

        // weird problems with fluctuating error messages in GNU R
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; x[[0]]<-NULL; x }");
        assertEval(Output.IgnoreErrorMessage, "{ b<-3:5; dim(b) <- c(1,3) ; b[[c(1)]] <- NULL ; b }");
        assertEval(Output.IgnoreErrorMessage, "{ b<-3:5; dim(b) <- c(1,3) ; b[[0]] <- NULL ; b }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- integer() ; x[[NA]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- c(1) ; x[[NA]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- c(1,2) ; x[[NA]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- c(1,2,3) ; x[[NA]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1]]<-NULL; x }");

        // inconsistent error messages in expected output and shell
        assertEval(Output.IgnoreErrorMessage, "{ x <- c(1); x[[-4]] <- NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- c(1,2,3); x[[-1]] <- NULL }");

        assertEval("{ x<-emptyenv(); y<-list(); y[[1]]<-x; y[[1]] }");
        assertEval("{ f<-function() 42; y<-list(); y[[1]]<-f; y[[1]] }");
        assertEval("{ x<-as.symbol(\"foo\"); y<-list(); y[[1]]<-x; y[[1]] }");
        assertEval("{ x<-getClass(\"ClassUnionRepresentation\"); y<-list(); y[[1]]<-x; y[[1]]@virtual }");

        assertEval("{ sigList<-list(object=c(\"foo\", \"\")); fcall<-do.call(\"call\", c(\"fun\", sigList)); x<-fcall[-1]; x[[1]] }");

        assertEval("{ m <- 1:4; dim(m) <- c(2,2); i <- 1:2; dim(i) <- c(1,2); m[i] }");
        assertEval("{ m <- 1:4; dim(m) <- c(2,2); i <- 1:2; dim(i) <- c(1,2); m[i] <- 42; m }");
        assertEval("{ m <- 1:4; dim(m) <- c(2,2); i <- as.double(1:2); dim(i) <- c(1,2); m[i] <- 42; m }");
        assertEval("{ m <- 1:4; dim(m) <- c(2,2); i <- as.double(1:2); dim(i) <- c(1,2); m[i] }");

        assertEval("{ f<-function(x, i, v) { x[[i]]<-v; x }; y<-list(a=47); f(y, \"a\", NULL); z<-list(a=47, b=7); f(z, \"a\", NULL) }");
    }

    @Test
    public void testLanguageIndex() {
        assertEval("{ e <- quote(f(x=a, b)); names(e) }");
        assertEval("{ e <- quote(f(x=a, y=b)); names(e) }");
        assertEval("{ e <- quote(f(x=a, y=b)); names(e[-1]) }");

        assertEval("{ x<-quote(function(x, y) 42); x[[2]] }");
        assertEval("{ x<-quote(function(x, y) 42); typeof(x[[2]][[1]]) }");
        assertEval("{ x<-quote(function(x, y) 42); names(x[[2]]) }");
        assertEval("{ x<-quote(function(x, y=7) 42); x[[2]] }");

        assertEval("{ f<-quote(function(x=42) x); f[[2]]$x<-7; eval(f)() }");
    }

    @Test
    public void testNAIndex() {
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA] }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA]] }");

        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA]] }");

        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, NA] }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1, NA] }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, 1] }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]] }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]] }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]] }");

        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, NA] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1, NA] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, 1] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]] }");

        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA]<-7; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-7; x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA]<-c(7,42); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-c(7, 42); x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA]<-c(7, 42, 1); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-c(7, 42, 1); x }");

        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA]<-7; x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-7; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA]<-c(7,42); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-c(7, 42); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA]<-c(7, 42, 1); x }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA]]<-c(7, 42, 1); x }");

        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-7; x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-7; x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-7; x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-c(7, 42); x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-c(7, 42); x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-c(7, 42); x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-7; x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-7; x }");
        assertEval("{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-7; x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-c(7, 42); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-c(7, 42); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-c(7, 42); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-c(7, 42, 1); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-c(7, 42, 1); x }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-c(7, 42, 1); x }");

        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-7; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-7; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-7; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-c(7, 42); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-c(7, 42); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-c(7, 42); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, NA]<-c(7, 42, 1); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1, NA]<-c(7, 42, 1); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[NA, 1]<-c(7, 42, 1); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-7; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-7; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-7; x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-c(7, 42); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-c(7, 42); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-c(7, 42); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, NA]]<-c(7, 42, 1); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1, NA]]<-c(7, 42, 1); x }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[NA, 1]]<-c(7, 42, 1); x }");
    }

    @Test
    public void testComplexIndex() {
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); x[[1+1i]]<-integer() }");
        assertEval("{ x<-c(1,2,3,4); x[[1+1i]]<-c(1) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); x[[1+1i]]<-c(1,2) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); x[[1+1i]]<-c(1,2,3) }");
        assertEval("{ x<-c(1,2,3,4); x[1+1i]<-NULL }");
        assertEval("{ x<-c(1,2,3,4); x[1+1i]<-integer() }");
        assertEval("{ x<-c(1,2,3,4); x[1+1i]<-c(1) }");
        assertEval("{ x<-c(1,2,3,4); x[1+1i]<-c(1,2) }");
        assertEval("{ x<-c(1,2,3,4); x[1+1i]<-c(1,2,3) }");

        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-7 }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-c(7,42) }");

        assertEval("{ x<-list(1,2,3,4); x[[1+1i]]<-NULL }");
        assertEval("{ x<-list(1,2,3,4); x[[1+1i]]<-integer() }");
        assertEval("{ x<-list(1,2,3,4); x[[1+1i]]<-c(1) }");
        assertEval("{ x<-list(1,2,3,4); x[[1+1i]]<-c(1,2) }");
        assertEval("{ x<-list(1,2,3,4); x[[1+1i]]<-c(1,2,3) }");
        assertEval("{ x<-list(1,2,3,4); x[1+1i]<-NULL }");
        assertEval("{ x<-list(1,2,3,4); x[1+1i]<-integer() }");
        assertEval("{ x<-list(1,2,3,4); x[1+1i]<-c(1) }");
        assertEval("{ x<-list(1,2,3,4); x[1+1i]<-c(1,2) }");
        assertEval("{ x<-list(1,2,3,4); x[1+1i]<-c(1,2,3) }");

        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-c(7,42) }");

        // weird fluctuating error messages in GNUR
        assertEval(Ignored.Unstable, Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); x[[1+1i]]<-NULL }");
        assertEval(Ignored.Unstable, Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[1+1i, 1]<-NULL }");
        assertEval(Ignored.Unstable, Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[1+1i, 1]]<-NULL }");
    }

    @Test
    public void testRawIndex() {
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); x[[as.raw(1)]]<-NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); x[[as.raw(1)]]<-integer() }");
        assertEval("{ x<-c(1,2,3,4); x[[as.raw(1)]]<-c(1) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); x[[as.raw(1)]]<-c(1,2) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); x[[as.raw(1)]]<-c(1,2,3) }");
        assertEval("{ x<-c(1,2,3,4); x[as.raw(1)]<-NULL }");
        assertEval("{ x<-c(1,2,3,4); x[as.raw(1)]<-integer() }");
        assertEval("{ x<-c(1,2,3,4); x[as.raw(1)]<-c(1) }");
        assertEval("{ x<-c(1,2,3,4); x[as.raw(1)]<-c(1,2) }");
        assertEval("{ x<-c(1,2,3,4); x[as.raw(1)]<-c(1,2,3) }");

        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-7 }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-c(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-c(7,42) }");

        assertEval("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-NULL }");
        assertEval("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-integer() }");
        assertEval("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-c(1) }");
        assertEval("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-c(1,2) }");
        assertEval("{ x<-list(1,2,3,4); x[[as.raw(1)]]<-c(1,2,3) }");
        assertEval("{ x<-list(1,2,3,4); x[as.raw(1)]<-NULL }");
        assertEval("{ x<-list(1,2,3,4); x[as.raw(1)]<-integer() }");
        assertEval("{ x<-list(1,2,3,4); x[as.raw(1)]<-c(1) }");
        assertEval("{ x<-list(1,2,3,4); x[as.raw(1)]<-c(1,2) }");
        assertEval("{ x<-list(1,2,3,4); x[as.raw(1)]<-c(1,2,3) }");

        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[as.raw(1), 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[as.raw(1), 1]<-c(7,42) }");

    }

    @Test
    public void testListIndex() {
        assertEval("{ z<-1:4; z[list()]<-42 }");
        assertEval("{ z<-1:4; z[list(1)]<-42 }");
        assertEval("{ z<-1:4; z[list(1,2)]<-42 }");
        assertEval("{ z<-1:4; z[list(1,2,3)]<-42 }");
        assertEval("{ z<-1:4; z[list()]<-integer() }");
        assertEval("{ z<-1:4; z[list(1)]<-integer() }");
        assertEval("{ z<-1:4; z[list(1,2)]<-integer() }");
        assertEval("{ z<-1:4; z[list(1,2,3)]<-integer() }");
        assertEval("{ z<-1:4; z[list()]<-NULL }");
        assertEval("{ z<-1:4; z[list(1)]<-NULL }");
        assertEval("{ z<-1:4; z[list(1,2)]<-NULL }");
        assertEval("{ z<-1:4; z[list(1,2,3)]<-NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list()]]<-42 }");
        assertEval("{ z<-1:4; z[[list(1)]]<-42 }");
        assertEval("{ z<-1:4; z[[list(1,2)]]<-42 }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list(1,2,3)]]<-42 }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list()]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list(1)]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list(1,2)]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list(1,2,3)]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list(1)]]<-NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list(1,2)]]<-NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list(1,2,3)]]<-NULL }");

        assertEval("{ z<-list(1,2,3,4); z[list()]<-42 }");
        assertEval("{ z<-list(1,2,3,4); z[list(1)]<-42 }");
        assertEval("{ z<-list(1,2,3,4); z[list(1,2)]<-42 }");
        assertEval("{ z<-list(1,2,3,4); z[list(1,2,3)]<-42 }");
        assertEval("{ z<-list(1,2,3,4); z[list()]<-integer() }");
        assertEval("{ z<-list(1,2,3,4); z[list(1)]<-integer() }");
        assertEval("{ z<-list(1,2,3,4); z[list(1,2)]<-integer() }");
        assertEval("{ z<-list(1,2,3,4); z[list(1,2,3)]<-integer() }");
        assertEval("{ z<-list(1,2,3,4); z[list()]<-NULL }");
        assertEval("{ z<-list(1,2,3,4); z[list(1)]<-NULL }");
        assertEval("{ z<-list(1,2,3,4); z[list(1,2)]<-NULL }");
        assertEval("{ z<-list(1,2,3,4); z[list(1,2,3)]<-NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-list(1,2,3,4); z[[list()]]<-42 }");
        assertEval("{ z<-list(1,2,3,4); z[[list(1)]]<-42 }");
        assertEval("{ z<-list(1,2,3,4); z[[list(1,2)]]<-42 }");
        assertEval(Output.IgnoreErrorContext, "{ z<-list(1,2,3,4); z[[list(1,2,3)]]<-42 }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-list(1,2,3,4); z[[list()]]<-integer() }");
        assertEval("{ z<-list(1,2,3,4); z[[list(1)]]<-integer() }");
        assertEval("{ z<-list(1,2,3,4); z[[list(1,2)]]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ z<-list(1,2,3,4); z[[list(1,2,3)]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-list(1,2,3,4); z[[list()]]<-NULL }");
        assertEval("{ z<-list(1,2,3,4); z[[list(1)]]<-NULL }");
        assertEval("{ z<-list(1,2,3,4); z[[list(1,2)]]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ z<-list(1,2,3,4); z[[list(1,2,3)]]<-NULL }");

        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(), 1]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(1), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(1), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(1), 1]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(1,2), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(1,2), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(1,2), 1]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(1,2,3), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(1,2,3), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[list(1,2,3), 1]<-c(7,42) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(), 1]]<-7 }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(), 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1), 1]]<-7 }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1), 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2), 1]]<-7 }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2), 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-7 }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-c(7,42) }");

        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1]<-c(7,42) }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2,3), 1]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2,3), 1]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2,3), 1]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2,3), 1]<-c(7,42) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]]<-NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]]<-7 }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]]<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]]<-7 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]]<-NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]]<-7 }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]]<-c(7,42) }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-NULL }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-integer() }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-7 }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2,3), 1]]<-c(7,42) }");

        assertEval("{ z<-1:4; z[list()] }");
        assertEval("{ z<-1:4; z[list(1)] }");
        assertEval("{ z<-1:4; z[list(1,2)] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list(), 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list(1), 1] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[list(1,2), 1] }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list()]] }");
        assertEval("{ z<-1:4; z[[list(1)]] }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-1:4; z[[list(1,2)]] }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(), 1]] }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2); x[[list(1), 1]] }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-1:4; dim(x)<-c(2,2); x[[list(1,2), 1]] }");

        assertEval("{ z<-list(1,2,3,4); z[list()] }");
        assertEval("{ z<-list(1,2,3,4); z[list(1)] }");
        assertEval("{ z<-list(1,2,3,4); z[list(1,2)] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(), 1] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1), 1] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[list(1,2), 1] }");
        assertEval(Output.IgnoreErrorMessage, "{ z<-list(1,2,3,4); z[[list()]] }");
        assertEval("{ z<-list(1,2,3,4); z[[list(1)]] }");
        assertEval("{ z<-list(1,2,3,4); z[[list(1,2)]] }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(), 1]] }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1), 1]] }");
        assertEval(Output.IgnoreErrorMessage, "{ x<-list(1,2,3,4); dim(x)<-c(2,2); x[[list(1,2), 1]] }");

        assertEval(Ignored.Unknown, Output.IgnoreErrorContext, "{ z<-1:4; z[[list()]]<-NULL }");
    }

    @Test
    public void testScalarOutOfBoundsOnVector() {

        // Positive above length.
        assertEval("{ x<-c(1,2,3); x[4L] }");
        assertEval("{ x<-c(1L,2L,3L); x[4L] }");
        assertEval("{ x<-1:3; x[4L] }");
        assertEval("{ x<-c(1,2,3); x[4.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[4.1] }");
        assertEval("{ x<-1:3; x[4.1] }");

        // Negative above length.
        assertEval("{ x<-c(1,2,3); x[-4L] }");
        assertEval("{ x<-c(1L,2L,3L); x[-4L] }");
        assertEval("{ x<-1:3; x[-4L] }");
        assertEval("{ x<-c(1,2,3); x[-4.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[-4.1] }");
        assertEval("{ x<-1:3; x[-4.1] }");

        // Around zero.
        assertEval("{ x<-c(1,2,3); x[0L] }");
        assertEval("{ x<-c(1L,2L,3L); x[0L] }");
        assertEval("{ x<-1:3; x[0L] }");
        assertEval("{ x<-c(1,2,3); x[0] }");
        assertEval("{ x<-c(1L,2L,3L); x[0] }");
        assertEval("{ x<-1:3; x[0] }");
        assertEval("{ x<-c(1,2,3); x[0.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[0.1] }");
        assertEval("{ x<-1:3; x[0.1] }");

        // TODO(tw): Test accesses with various forms of NA and NaN.
        assertEval("{ x<-c(1,2,3); x[NA] }");
        assertEval("{ x<-c(1L,2L,3L); x[NA] }");
        assertEval("{ x<-1:3; x[NA] }");
        assertEval("{ x<-c(1,2,3); typeof(x[NA]) }");
        assertEval("{ x<-c(1L,2L,3L); typeof(x[NA]) }");
        assertEval("{ x<-1:3; typeof(x[NA]) }");

        assertEval("{ x<-c(1,2,3); x[-0.1] }");
        assertEval("{ x<-c(1L,2L,3L); x[-0.1] }");
        assertEval("{ x<-1:3; x[-0.1] }");
    }

    @Test
    public void testScalarLogicOnVector() {
        assertEval("{ x<-c(1,2,3); x[FALSE] }");
        assertEval("{ x<-c(1L,2L,3L); x[FALSE] }");
        assertEval("{ x<-1:3; x[FALSE] }");
        assertEval("{ x<-c(1,2,3); x[TRUE] }");
        assertEval("{ x<-c(1L,2L,3L); x[TRUE] }");
        assertEval("{ x<-1:3; x[TRUE] }");
    }

    private static final String TEST_IGNORED1_EXPECTED_VAL = "<NA>\n  00\n";
    private static final String TEST_IGNORED2_EXPECTED_VAL = "<NA>\n  00\n";
    private static final String TEST_IGNORED3_EXPECTED_VAL = "<NA> <NA>\n  00   00\n";

    @Test
    public void testIgnored1() {
        Assert.assertEquals(TEST_IGNORED1_EXPECTED_VAL,
                        fastREval("{ x <- c(a=as.raw(10), b=as.raw(11), c=as.raw(12)) ; x[10] }", null, false));
    }

    @Test
    public void testIgnored2() {
        Assert.assertEquals(TEST_IGNORED2_EXPECTED_VAL,
                        fastREval("{ x <- c(a=as.raw(10),b=as.raw(11),c=as.raw(12),d=as.raw(13)) ; f <- function(s) { x[s] } ; f(TRUE) ; f(1L) ; f(as.character(NA)) }", null, false));
    }

    @Test
    public void testIgnored3() {
        Assert.assertEquals(TEST_IGNORED3_EXPECTED_VAL,
                        fastREval("{ x <- c(a=as.raw(10),b=as.raw(11),c=as.raw(12),d=as.raw(13)) ; f <- function(s) { x[c(s,s)] } ; f(TRUE) ; f(1L) ; f(as.character(NA)) }", null, false));
    }

    @Test
    public void testScalarIndex() {
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[[2]] }");

        assertEval("{ x<-5:1 ; y <- 6L;  x[y] }");
        assertEval("{ x<-5:1 ; y <- 2L;  x[[y]] }");
        assertEval("{ x <- c(1,4) ; y <- -1L ; x[y] }");
        assertEval("{ x <- c(1,4) ; y <- 10L ; x[y] }");
        assertEval("{ x <- c(1,4) ; y <- -1 ; x[y] }");
        assertEval("{ x <- c(1,4) ; y <- 10 ; x[y] }");
        assertEval("{ x <- 1:4 ; y <- -1 ; x[y] }");
        assertEval("{ x <- 1:4 ; y <- 10 ; x[y] }");
        assertEval("{ x <- list(1,2,3,4) ; y <- 3 ; x[[y]] }");

        assertEval("{ x <- c(as.raw(10), as.raw(11), as.raw(12)) ; x[-2] }");

        assertEval("{ x<-as.list(5:1) ; y <- 2L;  x[[y]] }");
        assertEval("{ x <- as.list(1:2) ; f <- function(i) { x[i] <- NULL ; x } ; f(1) ; f(NULL) }");

        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[0-2] }");
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[2] }");
        assertEval("{ x <- c(a=\"A\", b=\"B\", c=\"C\") ; x[-2] }");
        assertEval("{ x <- c(a=1+2i, b=2+3i, c=3) ; x[-2] }");
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[-2] }");
        assertEval("{ x <- c(a=1L, b=2L, c=3L) ; x[-2] }");
        assertEval("{ x <- c(a=TRUE, b=FALSE, c=NA) ; x[-2] }");
        assertEval("{ x <- c(a=as.raw(10), b=as.raw(11), c=as.raw(12)) ; x[-2] }");

        assertEval("{ x <- c(a=1L, b=2L, c=3L) ; x[0] }");
        assertEval("{ x <- c(a=1L, b=2L, c=3L) ; x[10] }");
        assertEval("{ x <- c(a=TRUE, b=FALSE, c=NA) ; x[0] }");
        assertEval("{ x <- c(TRUE, FALSE, NA) ; x[0] }");
        assertEval("{ x <- list(1L, 2L, 3L) ; x[10] }");
        assertEval("{ x <- list(a=1L, b=2L, c=3L) ; x[0] }");
        assertEval("{ x <- c(a=\"A\", b=\"B\", c=\"C\") ; x[10] }");
        assertEval("{ x <- c(a=\"A\", b=\"B\", c=\"C\") ; x[0] }");
        assertEval("{ x <- c(a=1+1i, b=2+2i, c=3+3i) ; x[10] }");
        assertEval("{ x <- c(a=1+1i, b=2+2i, c=3+3i) ; x[0] }");
        // See testIgnored1
        assertEval(Ignored.ReferenceError, "{ x <- c(a=as.raw(10), b=as.raw(11), c=as.raw(12)) ; x[10] }");
        assertEval("{ x <- c(a=as.raw(10), b=as.raw(11), c=as.raw(12)) ; x[0] }");
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[10] }");
        assertEval("{ x <- c(a=1, b=2, c=3) ; x[0] }");
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; x[\"b\"] }");
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; x[\"d\"] }");

        assertEval("{ x <- 1 ; attr(x, \"hi\") <- 2; x[2] <- 2; attr(x, \"hi\") }");

        assertEval("{ x<-function() {1} ; y <- 2;  x[y] }");
        assertEval("{ x<-function() {1} ; y <- 2;  y[x] }");
        assertEval("{ x<-5:1 ; y <- -1L;  x[y] }");
        assertEval("{ x<-as.list(5:1) ; y <- 1:2;  x[[y]] }");
        assertEval("{ x<-function() {1} ; x[2L] }");
        assertEval("{ x <- c(a=1,b=2) ; y <- 2L ; x[y] }");
        assertEval("{ x <- c(a=1,b=2) ; y <- 2 ; x[y] }");
        assertEval("{ x <- list(1,2,3,4) ; y <- 3 ; x[y] }");
        assertEval("{ x <- function(){3} ; y <- 3 ; x[[y]] }");

        assertEval("{ x <- list(1,4) ; y <- -1 ; x[y] }");
        assertEval("{ x <- list(1,4) ; y <- 4 ; x[y] }");
        assertEval("{ x <- list(a=1,b=4) ; y <- 2 ; x[y] }");
        assertEval("{ f <- function(x,i) { x[i] } ; x <- c(a=1,b=2) ; f(x,\"a\") }");
        assertEval("{ f <- function(x,i) { x[i] } ; x <- c(a=1,b=2) ; f(x,\"a\") ; f(x,2) }");

        assertEval(Output.IgnoreErrorContext, "{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),FALSE) }");
        assertEval("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),TRUE) }");
        assertEval("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),1+0i) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(), NA) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(integer(), NA) }");
        assertEval("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,4) }");
        assertEval("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,NA) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,-1) }");
        assertEval("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-1) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(2,-2) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(2,-3) }"); // like
        // GNU-R, but is it a bug?
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:4,-3) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-3) }");
        assertEval("{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-2) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,NA) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=1L,b=2L),0) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,0) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:2,-2) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),NA) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),-4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),0) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=TRUE,b=FALSE),0) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),-2) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(TRUE,FALSE),4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=TRUE,b=FALSE),4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),-4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(1,2),4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(list(a=1,b=2),4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(\"a\",\"b\"),4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(\"a\",\"b\"),NA) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(\"a\",\"b\"),-4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(\"a\",\"b\"),0) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=\"a\",b=\"b\"),0) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(1+2i,3+4i),NA) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(1+2i,3+4i),-4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(1+2i,3+4i),4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(c(a=1+2i,b=3+4i),4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(as.raw(c(10,11)),-4) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(as.raw(c(10,11)),0) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(as.raw(c(10,11)),4) }");

        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(1+2i,3+4i) ; attr(z, \"my\") <- 1 ; f(z,-10) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(1,3) ; attr(z, \"my\") <- 1 ; f(z,-10) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(1L,3L) ; attr(z, \"my\") <- 1 ; f(z,-10) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(TRUE,FALSE) ; attr(z, \"my\") <- 1 ; f(z,-10) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(a=\"a\",b=\"b\") ; attr(z, \"my\") <- 1 ; f(z,-10) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; z <- c(a=as.raw(10),b=as.raw(11)) ; attr(z, \"my\") <- 1 ; f(z,-10) }");

        assertEval(Output.IgnoreErrorContext, "{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,c(TRUE,FALSE)) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,c(TRUE,FALSE)) }");
        assertEval("{ f <- function(x,i) { x[i] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,c(1,2)) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(x,i) { x[[i]] } ; f(1:4, 2L) ; f(c(a=1), \"a\") ; f(1:3,c(3,3)) }");
        assertEval(Output.IgnoreErrorContext, " { x <- 1:3 ; x[[NULL]] }");
        assertEval("{ x <- as.list(1:2) ; f <- function(i) { x[[i]] <- NULL ; x } ; f(1) ; f(as.raw(10)) }");

        assertEval("{ x <- 1:3 ; x[2] <- integer() }");
        assertEval("{ x <- 1:3 ; x[[TRUE]] <- 1:2 }");
        assertEval("{ x <- 1:3 ; x[TRUE] <- 10 ; x }");
        assertEval("{ x <- 1:3 ; x[[TRUE]] <- 10 ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- 1:3 ; x[[FALSE]] <- 10 ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- 1:3 ; x[[NA]] <- 10 ; x }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, list(1), 3) }");
        assertEval(Output.IgnoreErrorMessage, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, list(), 3) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, 1+2i, 3) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, 1, 3:4) }");
        assertEval(Output.IgnoreErrorMessage, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:3, 1L, 10) ; f(c(1,2), \"hello\", TRUE) ; f(1:2, as.integer(NA), 3:4) }");
        assertEval("{ x <- 1:2 ; x[as.integer(NA)] <- 3:4 }");

        assertEval("{ b <- c(1+2i,3+4i) ; dim(b) <- c(2,1) ; b[1] <- 3+1i ; b }");
        assertEval("{ b <- list(1+2i,3+4i) ; dim(b) <- c(2,1) ; b[\"hello\"] <- NULL ; b }");

        assertEval("{ x<-1:4; x[c(-1.5)] }");
        assertEval("{ x<-1:4; x[c(1.4,1.8)] }");

        assertEval("{ f <- function(x,i) { x[[i]]} ; f(list(1,2,3,4), 3); f(f,2) }");
        assertEval("{ f <- function(x,i) { x[i] } ; x <- c(a=1,b=2) ; f(x,\"a\") ; f(function(){3},\"b\") }");

        assertEval("{ x<-1:4; x[c(-0.5)] }");
    }

    @Test
    public void testMultiDimScalarIndex() {
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[1,0,]) }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[,0,]) }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[-1,0,] }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x[-1,-1, 0] }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[0,2,0]) }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[0,-1,0]) }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dim(x[0,3,0]) }");
    }

    @Test
    public void testVectorIndex() {
        assertEval("{ x<-1:5 ; x[3:4] }");
        assertEval("{ x<-1:5 ; x[4:3] }");
        assertEval("{ x<-c(1,2,3,4,5) ; x[4:3] }");
        assertEval("{ (1:5)[3:4] }");
        assertEval("{ x<-(1:5)[2:4] ; x[2:1] }");
        assertEval("{ x<-1:5;x[c(0-2,0-3)] }");
        assertEval("{ x<-1:5;x[c(0-2,0-3,0,0,0)] }");
        assertEval("{ x<-1:5;x[c(2,5,4,3,3,3,0)] }");
        assertEval("{ x<-1:5;x[c(2L,5L,4L,3L,3L,3L,0L)] }");
        assertEval("{ f<-function(x, i) { x[i] } ; f(1:3,3:1) ; f(1:5,c(0,0,0,0-2)) }");
        assertEval("{ f<-function(x, i) { x[i] } ; f(1:3,0-3) ; f(1:5,c(0,0,0,0-2)) }");
        assertEval("{ f<-function(x, i) { x[i] } ; f(1:3,0L-3L) ; f(1:5,c(0,0,0,0-2)) }");
        assertEval("{ x<-1:5 ; x[c(TRUE,FALSE)] }");
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(1) ; f(1L) ; f(TRUE) }");
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(1) ; f(TRUE) ; f(1L)  }");
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(1) ; f(TRUE) ; f(c(3,2))  }");
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(1)  ; f(3:4) }");
        assertEval("{ f<-function(i) { x<-1:5 ; x[i] } ; f(c(TRUE,FALSE))  ; f(3:4) }");

        assertEval("{ x <- 1;  y<-c(1,1) ; x[y] }");
        assertEval("{ x <- 1L;  y<-c(1,1) ; x[y] }");

        assertEval("{ x <- c(1,2,3,2) ; x[x==2] }");
        assertEval("{ x <- c(1,2,3,2) ; x[c(3,4,2)==2] }");
        assertEval("{ x <- c(as.double(1:2000)) ; x[c(1,3,3,3,1:1996)==3] }");
        assertEval("{ x <- c(as.double(1:2000)) ; sum(x[rep(3, 2000)==3]) }");
        assertEval("{ x <- c(1,2,3,2) ; x[c(3,4,2,NA)==2] }");

        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,TRUE)) ; f(1:3,3:1) }");

        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3,c(2,1)) ; f(1:3,c(TRUE,FALSE)) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3,c(2,1)) ; f(1:3,NULL) }");

        assertEval("{ x <- \"hi\";  y<-c(1,1) ; x[y] }");
        assertEval("{ l <- list(1,function(){3}) ; f <- function(i) { l[[i]] } ; f(c(2)) }");

        assertEval("{ a <- c(1,2,3) ; x <- integer() ; a[x] }");
        assertEval(Output.IgnoreErrorContext, "{ a <- c(1,2,3) ; x <- integer() ; a[[x]] }");

        assertEval("{ f <- function(i) { l[[i]] } ; l <- list(1, as.list(1:3)) ; f(c(2,NA)) }");

        assertEval("{ x<-1:5 ; x[c(TRUE,TRUE,TRUE,NA)] }");
        assertEval("{ x<-1:5 ; x[c(TRUE,TRUE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,NA)] }");
        assertEval("{ x<-as.complex(c(1,2,3,4)) ; x[2:4] }");
        assertEval("{ x<-as.raw(c(1,2,3,4)) ; x[2:4] }");

        assertEval("{ x<-c(1,2,3,4) ; names(x) <- c(\"a\",\"b\",\"c\",\"d\") ; x[c(10,2,3,0)] }");
        assertEval("{ x<-c(1,2,3,4) ; names(x) <- c(\"a\",\"b\",\"c\",\"d\") ; x[c(10,2,3)] }");
        assertEval("{ x<-c(1,2,3,4) ; names(x) <- c(\"a\",\"b\",\"c\",\"d\") ; x[c(-2,-4,0)] }");
        assertEval("{ x<-c(1,2) ; names(x) <- c(\"a\",\"b\") ; x[c(FALSE,TRUE,NA,FALSE)] }");
        assertEval("{ x<-c(1,2) ; names(x) <- c(\"a\",\"b\") ; x[c(FALSE,TRUE)] }");

        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; x[character()] }");
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; x[c(\"b\",\"b\",\"d\",\"a\",\"a\")] }");
        // See testIgnored2
        assertEval(Ignored.ReferenceError, "{ x <- c(a=as.raw(10),b=as.raw(11),c=as.raw(12),d=as.raw(13)) ; f <- function(s) { x[s] } ; f(TRUE) ; f(1L) ; f(as.character(NA)) }");
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; f <- function(s) { x[s] } ; f(TRUE) ; f(1L) ; f(\"b\") }");
        // See testIgnored3
        assertEval(Ignored.ReferenceError, "{ x <- c(a=as.raw(10),b=as.raw(11),c=as.raw(12),d=as.raw(13)) ; f <- function(s) { x[c(s,s)] } ; f(TRUE) ; f(1L) ; f(as.character(NA)) }");
        assertEval("{ x <- c(a=1,b=2,c=3,d=4) ; f <- function(s) { x[c(s,s)] } ; f(TRUE) ; f(1L) ; f(\"b\") }");

        assertEval("{ x <- TRUE;  y<-c(1,1) ; x[y] }");
        assertEval("{ x <- 1+2i;  y<-c(1,2) ; x[y] }");

        // logical equality selection
        assertEval("{ f<-function(x,l) { x[l == 3] } ; f(c(1,2,3), c(1,2,3)) ; f(c(1,2,3), 1:3) ; f(1:3, c(3,3,2)) }");
        assertEval("{ f<-function(x,l) { x[l == 3] <- 4 } ; f(c(1,2,3), c(1,2,3)) ; f(c(1,2,3), 1:3) ; f(1:3, c(3,3,2)) }");

        assertEval("{ x <- function(){3} ; x[3:2] }");
        assertEval("{ x <- c(1,2,3) ; x[-1:2] }");
        assertEval("{ x <- c(TRUE,FALSE,TRUE) ; x[2:3] }");
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[2:3] }");
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(2,3,NA)] }");
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,3,NA)] }");
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,-3,NA)] }");
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,-3,-4,-5)] }");
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,-3,-4,-5,-5)] }");
        assertEval("{ x <- c(1+2i,3+4i,5+6i) ; x[c(-2,-3,-4,-5,-2)] }");
        assertEval("{ f <- function(b,i) { b[i] } ; x <- c(1+2i,3+4i,5+6i) ; f(x,c(1,2)) ; f(x,c(1+2i)) }");
        assertEval("{ x <- c(TRUE,FALSE,TRUE) ; x[integer()] }");

        assertEval("{ x <- c(a=1,x=2,b=3,y=2) ; x[c(3,4,2)==2] }");
        assertEval("{ x <- c(a=1,x=2,b=3,y=2) ; x[c(3,4,2,1)==2] }");
        assertEval("{ x <- c(as.double(1:2000)) ; x[c(NA,3,3,NA,1:1996)==3] }");

        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,TRUE)) ; f(c(a=1,b=2,c=3),3:1) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,NA)) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,NA,NA,NA)) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(c(a=1,b=2,c=3), c(TRUE,NA,FALSE,FALSE,TRUE)) }");
        assertEval(" { f <- function(b,i) { b[i] } ; f(c(a=1,b=2,c=3), c(TRUE,NA)) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, logical()) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(c(a=1L,b=2L,c=3L), logical()) }");

        assertEval("{ f <- function(b,i) { b[i] } ; f(c(a=1,b=2,c=3), character()) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(c(1,2,3), character()) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(c(1,2,3), c(\"hello\",\"hi\")) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(\"h\",\"hi\")) ; f(1:3,TRUE) }");

        assertEval("{ x <- list(1,2,list(3)) ; x[[c(3,1)]] }");
        assertEval("{ x <- list(1,2,list(3)) ; x[[c(4,1)]] }");
        assertEval("{ x <- list(1,2,list(3)) ; x[[c(3,NA)]] }");
        assertEval("{ x <- list(1,2,list(3)) ; x[[c(NA,1)]] }");
        assertEval("{ x <- list(1,list(3)) ; x[[c(-1,1)]] }");
        assertEval(Output.IgnoreErrorContext, "{ l <- list(1,list(2)) ; l[[integer()]] }");
        assertEval("{ l <- list(1,list(2)) ; f <- function(i) { l[[i]] } ; f(c(2,1)) ; f(1) }");
        assertEval("{ l <- list(1,NULL) ; f <- function(i) { l[[i]] } ; f(c(2,1)) }");
        assertEval("{ f <- function(i) { l[[i]] } ; l <- list(1, 1:3) ; f(c(2,NA)) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(i) { l[[i]] } ; l <- list(1, 1:3) ; f(c(2,-4)) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(i) { l[[i]] } ; l <- list(1, 2) ; f(c(2,-1)) }");
        assertEval("{ f <- function(i) { l[[i]] } ; l <- list(1, c(2,3)) ; f(c(2,-1)) }");
        assertEval("{ f <- function(i) { l[[i]] } ; l <- list(1, c(2,3)) ; f(c(2,-2)) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(i) { l[[i]] } ; l <- list(1, c(2,3)) ; f(c(2,-4)) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(i) { l[[i]] } ; l <- list(1, c(2,3)) ; f(c(2,0)) }");

        assertEval("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(\"d\",\"x\")]] }");
        assertEval("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(\"z\",\"x\")]] }");
        assertEval("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(\"z\",NA)]] }");
        assertEval("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(\"d\",NA)]] }");
        assertEval("{ x <- list(a=1,b=2,d=list(x=3)) ; x[[c(NA,\"x\")]] }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(a=1,b=2,d=list(x=3)) ; x[[character()]] }");
        assertEval("{ x <- list(a=1,b=2,d=list(x=3)) ; f <- function(i) { x[[i]] } ; f(c(\"d\",\"x\")) ; f(\"b\") }");
        assertEval(Output.IgnoreErrorContext, "{ x <- c(a=1,b=2) ; x[[c(\"a\",\"a\")]] }");
        assertEval("{ x <- list(1,2) ; x[[c(\"a\",\"a\")]] }");
        assertEval("{ x <- list(a=1,b=1:3) ; x[[c(\"b\",\"a\")]] }");
        assertEval("{ x <- list(a=1,b=1:3) ; x[[2+3i]] }");
        assertEval("{ x <- list(a=1,b=1:3) ; f <- function(i) { x[[i]] } ; f(c(2,2)) ; f(2+3i) }");
        assertEval("{ x <- 1:3; x[list(2,3)] }");
        assertEval("{ x <- 1:3; x[function(){3}] }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- 1:2; x[[list()]] }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- 1:2; x[[list(-0,-1)]] }");
        assertEval("{ x <- 1:2; x[[list(0)]] }");
        assertEval("{ f <- function(b,i) { b[[i]] } ; f(list(1,list(2)),c(2,1)) ; f(1:3,list(1)) }");

        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3,c(2,1)) ; f(1:3,as.raw(c(10,11))) }");

        assertEval("{ l <- list(1,2) ; l[[c(1,1,2,3,4,3)]] }");
        assertEval("{ l <- list(list(1,2),2) ; l[[c(1,1,2,3,4,3)]] }");

        assertEval("{ f <- function(b) { b[integer()] } ; f(c(TRUE,FALSE,TRUE)) ; f(f) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE,TRUE)) ; f(function(){2},3:1) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(TRUE,FALSE)) ; f(f, c(TRUE,NA)) }");
        assertEval("{ f <- function(b,i) { b[i] } ; f(1:3, c(\"h\",\"hi\")) ; f(function(){3},\"hi\") }");
        assertEval(Output.IgnoreErrorMessage, "{ f <- function(i) { l[[i]] } ; l <- list(1, f) ; f(c(2,1)) }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- list(a=1,b=function(){3},d=list(x=3)) ; x[[c(2,10)]] }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- list(a=1,b=function(){3},d=list(x=3)) ; x[[c(2,-3)]] }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- list(a=1,b=function(){3},d=list(x=3)) ; f <- function(i) { x[[i]] } ; f(c(\"d\",\"x\")) ; f(c(\"b\",\"z\")) }");
        assertEval("{ x <- list(a=1,b=1:3) ; f <- function(i) { x[[i]] } ; f(c(2,2)) ; x <- f ; f(2+3i) }");
    }

    @Test
    public void testScalarUpdate() {
        assertEval("{ x<-1:3; x[1]<-100L; x }");
        assertEval("{ x<-c(1,2,3); x[2L]<-100L; x }");
        assertEval("{ x<-c(1,2,3); x[2L]<-100; x }");
        assertEval("{ x<-c(1,2,3); x[2]<-FALSE; x }");
        assertEval("{ x<-1:5; x[2]<-1000; x[3] <- TRUE; x[8]<-3L; x }");
        assertEval("{ f<-function(x,i,v) { x<-1:5; x[i]<-v; x} ; f(c(1L,2L),1,3L) ; f(c(1L,2L),2,3) }");
        assertEval("{ f<-function(x,i,v) { x<-1:5; x[i]<-v; x} ; f(c(1L,2L),1,3L) ; f(c(1L,2L),8,3L) }");
        assertEval("{ f<-function(x,i,v) { x<-1:5; x[i]<-v; x} ; f(c(1L,2L),1,FALSE) ; f(c(1L,2L),2,3) }");
        assertEval("{ f<-function(x,i,v) { x<-1:5; x[i]<-v; x} ; f(c(1L,2L),1,FALSE) ; f(c(1L,2L),8,TRUE) }");

        assertEval("{ a <- c(1L,2L,3L); a <- 1:5; a[3] <- TRUE; a }");
        assertEval("{ x <- 1:3 ; x[2] <- \"hi\"; x }");
        assertEval("{ x <- c(1,2,3) ; x[2] <- \"hi\"; x }");
        assertEval("{ x <- c(TRUE,FALSE,FALSE) ; x[2] <- \"hi\"; x }");
        assertEval("{ x <- c(2,3,4) ; x[1] <- 3+4i ; x  }");

        assertEval("{ b <- c(1,2) ; x <- b ; b[2L] <- 3 ; b }");
        assertEval("{ b <- c(1,2) ; b[0L] <- 3 ; b }");
        assertEval("{ b <- c(1,2) ; b[0] <- 1+2i ; b }");
        assertEval(Output.IgnoreErrorContext, "{ x[3] <<- 10 }");
        assertEval("{ b <- c(1,2) ; b[5L] <- 3 ; b }");
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1L,2L),10L) ; f(1,3) }");
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1L,2L),10L) ; f(1L,3) }");
        assertEval("{ b <- c(1L,2L) ; b[3] <- 13L ; b }");
        assertEval("{ b <- c(1L,2L) ; b[0] <- 13L ; b }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; b <- c(10L,2L) ; b[0] <- TRUE ; b }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; b <- c(10L,2L) ; b[3] <- TRUE ; b }");
        assertEval("{ b <- c(1L,2L) ; b[2] <- FALSE ; b }");
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1L,2L),TRUE) ; f(1L,3) }");
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1L,2L),TRUE) ; f(10,3) }");
        assertEval("{ b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10) ; f(b,13L) }");
        assertEval(" { b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10) ; f(1:3,13L) }");
        assertEval("{ b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10) ; f(c(1,2),10) }");
        assertEval("{ b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10L) ; f(1:3,13L) }");
        assertEval("{ b <- c(1,2) ; x <- b ; f <- function(b,v) { b[2L] <- v ; b } ; f(b,10L) ; f(b,13) }");
        assertEval("{ b <- c(1,2) ; z <- b ; b[3L] <- 3L ; b }");
        assertEval("{ b <- c(1,2) ; z <- b ; b[3L] <- FALSE ; b }");
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1,2),FALSE) ; f(10L,3) }");
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(1,2),FALSE) ; f(10,3) }");
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(TRUE,NA),FALSE) ; f(c(FALSE,TRUE),3) }");
        assertEval("{ f <- function(b,v) { b[2] <- v ; b } ; f(c(TRUE,NA),FALSE) ; f(3,3) }");
        assertEval("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(c(\"a\",\"b\"),\"d\") ; f(1:3,\"x\") }");
        assertEval("{ b <- c(\"a\",\"b\") ; z <- b ; b[[3L]] <- \"xx\" ; b }");

        assertEval("{ x <- as.list(1:2) ; x[[\"z\"]] <- NULL ; x }");
        assertEval("{ e < new.env(); e[[\"abc\"]] <- 3}");
        assertEval("{ e < new.env(); e[[\"abc\"]] <- NULL}");

        assertEval("{ x<-5:1; x[0-2]<-1000; x }");
        assertEval("{ x<-c(); x[[TRUE]] <- 2; x }");
        assertEval("{ x<-1:2; x[[0-2]]<-100; x }");

        assertEval(Output.IgnoreErrorContext, "{ f <- function() { a[3] <- 4 } ; f() }");
        assertEval(Output.IgnoreWarningContext, "{ b <- c(1,2) ; z <- c(10,11) ; attr(z,\"my\") <- 4 ; b[2] <- z ; b }");
        assertEval("{ b <- c(1,2) ; z <- b ; b[-2] <- 3L ; b }");
        assertEval("{ b <- c(1,2) ; z <- b ; b[-10L] <- FALSE ; b }");
        assertEval("{ b <- c(TRUE,NA) ; z <- b ; b[-10L] <- FALSE ; b }");
        assertEval("{ b <- c(TRUE,NA) ; z <- b ; b[4L] <- FALSE ; b }");
        assertEval("{ b <- list(TRUE,NA) ; z <- b ; b[[4L]] <- FALSE ; b }");
        assertEval("{ b <- list(TRUE,NA) ; z <- b ; b[[-1L]] <- FALSE ; b }");
        assertEval("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(list(TRUE,NA),FALSE) ; f(3,3) }");
        assertEval("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(list(TRUE,NA),FALSE) ; f(list(3),NULL) }");
        assertEval("{ f <- function(b,v) { b[[2]] <- v ; b } ; f(list(TRUE,NA),FALSE) ; f(list(),NULL) }");
        assertEval("{ b <- c(\"a\",\"b\") ; z <- b ; b[[-1L]] <- \"xx\" ; b }");
        assertEval("{ b <- c(1,2) ; b[3] <- 2+3i ; b }");
        assertEval("{ b <- c(1+2i,3+4i) ; b[3] <- 2 ; b }");
        assertEval("{ b <- c(TRUE,NA) ; b[3] <- FALSE ; b }");
        assertEval("{ b <- as.raw(c(1,2)) ; b[3] <- 3 ; b }");
        assertEval("{ b <- c(1,2) ; b[3] <- as.raw(13) ; b }");
        assertEval("{ b <- as.raw(c(1,2)) ; b[3] <- as.raw(13) ; b }");
        assertEval("{ b <- as.raw(c(1,2)) ; b[as.double(NA)] <- as.raw(13) ; b }");
        assertEval("{ b <- as.raw(c(1,2)) ; b[[-2]] <- as.raw(13) ; b }");
        assertEval("{ b <- as.raw(c(1,2)) ; b[[-1]] <- as.raw(13) ; b }");
        assertEval(Output.IgnoreErrorContext, "{ b <- as.raw(c(1,2)) ; b[[-3]] <- as.raw(13) ; b }");
        assertEval(Output.IgnoreErrorContext, "{ b <- as.raw(1) ; b[[-3]] <- as.raw(13) ; b }");
        assertEval(Output.IgnoreErrorContext, "{ b <- as.raw(c(1,2,3)) ; b[[-2]] <- as.raw(13) ; b }");
        assertEval("{ f <- function(b,i) { b[i] <- 1 } ; f(1:3,2) ; f(f, 3) }");
        assertEval("{ f <- function(b,i) { b[i] <- 1 } ; f(1:3,2) ; f(1:2, f) }");
        assertEval("{ f <- function(b,v) { b[2] <- v } ; f(1:3,2) ; f(1:2, f) }");

        assertEval("{ x <- c(a=1+2i, b=3+4i) ; x[\"a\"] <- 10 ; x }");
        assertEval(" { x <- c(a=1+2i, b=3+4i) ; x[\"a\"] <- as.raw(13) ; x }");
        assertEval("{ x <- as.raw(c(10,11)) ; x[\"a\"] <- as.raw(13) ; x }");
        assertEval(" { x <- as.raw(c(10,11)) ; x[\"a\"] <- NA ; x }");
        assertEval("{ x <- 1:2 ; x[\"a\"] <- 10+3i ; x }");
        assertEval("{ x <- c(a=1+2i, b=3+4i) ; x[\"a\"] <- \"hi\" ; x }");
        assertEval("{ x <- 1:2 ; x[\"a\"] <- 10 ; x }");
        assertEval("{ x <- c(a=1,a=2) ; x[\"a\"] <- 10L ; x }");
        assertEval("{ x <- 1:2 ; x[\"a\"] <- FALSE ; x }");
        assertEval("{ x <- c(aa=TRUE,b=FALSE) ; x[\"a\"] <- 2L ; x }");
        assertEval("{ x <- c(aa=TRUE) ; x[[\"a\"]] <- list(2L) ; x }");
        assertEval("{ x <- c(aa=TRUE) ; x[\"a\"] <- list(2L) ; x }");
        assertEval("{ x <- c(b=2,a=3) ; z <- x ; x[\"a\"] <- 1 ; x }");

        assertEval("{ x <- list(1,2) ; dim(x) <- c(2,1) ; x[[3]] <- NULL ; x }");
        assertEval("{ x <- list(1,2) ; dim(x) <- c(2,1) ; x[3] <- NULL ; x }");
        assertEval("{ x <- list(1,2) ; dim(x) <- c(2,1) ; x[2] <- NULL ; x }");
        assertEval("{ x <- list(1,2) ; dim(x) <- c(2,1) ; x[[2]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(1,2) ; x[[0]] <- NULL ; x }");
        assertEval("{ x <- list(1,2) ; x[0] <- NULL ; x }");
        assertEval("{ x <- list(1,2) ; x[NA] <- NULL ; x }");
        assertEval("{ x <- list(1,2) ; x[as.integer(NA)] <- NULL ; x }");
        assertEval("{ x <- list(1,2) ; x[-1] <- NULL ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(1,2,3) ; x[[-1]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(1,2,3) ; x[[-5]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(1) ; x[[-2]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(1) ; x[[-1]] <- NULL ; x }");
        assertEval("{ x <- list(3,4) ; x[[-1]] <- NULL ; x }");
        assertEval("{ x <- list(3,4) ; x[[-2]] <- NULL ; x }");
        assertEval(Output.IgnoreErrorContext, "{ x <- list(3,4) ; x[[-10]] <- NULL ; x }");
        assertEval("{ x <- list(a=3,b=4) ; x[[\"a\"]] <- NULL ; x }");
        assertEval("{ x <- list(a=3,b=4) ; x[\"z\"] <- NULL ; x }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,-2,10) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,2,10) ; f(1:2,as.integer(NA), 10) }");
        assertEval(Output.IgnoreErrorContext, "{ x <- 1:2; x[[as.integer(NA)]] <- 10 ; x }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b,i,v) { b[[i]] <- v ; v } ; f(1:2,\"hi\",3L) ; f(1:2,c(2),10) ; f(1:2,as.integer(NA), 10) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,c(2),10) ; f(1:2,2, 10) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,c(2),10) ; f(1:2,0, 10) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,2,10) ; f(1:2,1:3, 10) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,2,10) ; f(as.list(1:2),1:3, 10) }");

        assertEval("{ b <- list(1+2i,3+4i) ; dim(b) <- c(2,1) ; b[3] <- NULL ; b }");

        // weird problems with fluctuating error messages in GNU R
        assertEval(Ignored.Unstable, "{ f <- function(b,v) { b[[2]] <- v ; b } ; f(c(\"a\",\"b\"),\"d\") ; f(c(\"a\",\"b\"),NULL) }");
        assertEval(Ignored.Unstable, Output.IgnoreErrorContext, "{ x <- 4:10 ; x[[\"z\"]] <- NULL ; x }");
    }

    @Test
    public void testVectorUpdate() {
        assertEval("{ a <- c(1,2,3) ; b <- a; a[1] <- 4L; a }");
        assertEval("{ a <- c(1,2,3) ; b <- a; a[2] <- 4L; a }");
        assertEval("{ a <- c(1,2,3) ; b <- a; a[3] <- 4L; a }");
        // logical value inserted to double vector
        // logical value inserted into logical vector
        assertEval("{ a <- c(TRUE,TRUE,TRUE); b <- a; a[[1]] <- FALSE; a }");
        assertEval("{ a <- c(TRUE,TRUE,TRUE); b <- a; a[[2]] <- FALSE; a }");
        assertEval("{ a <- c(TRUE,TRUE,TRUE); b <- a; a[[3]] <- FALSE; a }");

        assertEval("{ f<-function(i,v) { x<-1:5 ; x[i]<-v ; x } ; f(1,1) ; f(1L,TRUE) ; f(2,TRUE) }");
        assertEval("{ f<-function(i,v) { x<-1:5 ; x[[i]]<-v ; x } ; f(1,1) ; f(1L,TRUE) ; f(2,TRUE) }");

        assertEval("{ buf <- integer() ; buf[[1]] <- 4L ; buf }");
        assertEval("{ buf <- double() ; buf[[1]] <- 23 ; buf }");

        assertEval("{ inds <- 1:4 ; m <- 2:3 ; inds[m] <- inds[m] + 1L ; inds }");
        assertEval("{ inds <- 1:4 ; m <- c(2L,3L) ; inds[m] <- inds[m] + 1L ; inds }");
        assertEval("{ inds <- 1:4 ; m <- 2:3 ; inds[m] <- inds[m] + 1L ; m <- 1:2 ; inds[m] <- inds[m] + 1L ; inds }");
        assertEval("{ inds <- 1:4 ; m <- 2L ; inds[m] <- inds[m] + 1L ; m <- c(1L,2L) ; inds[m] <- inds[m] + 1L ; inds }");

        assertEval("{ x <- c(1) ; f <- function() { x[[1]] <<- x[[1]] + 1 ; x } ; a <- f() ; b <- f() ; c(a,b) }");

        assertEval("{ x<-c(1,2,3,4,5); x[3:4]<-c(300,400); x }");
        assertEval("{ x<-c(1,2,3,4,5); x[4:3]<-c(300L,400L); x }");
        assertEval("{ x<-1:5; x[4:3]<-c(300L,400L); x }");
        assertEval("{ x<-5:1; x[3:4]<-c(300L,400L); x }");
        assertEval("{ x<-5:1; x[3:4]<-c(300,400); x }");
        assertEval("{ x<-1:5; x[c(4,2,3)]<-c(256L,257L,258L); x }");

        assertEval("{ f<-function(i,v) { x<-1:5 ; x[i]<-v ; x } ; f(3:2,1) ; f(1L,TRUE) ; f(2:4,4:2) }");
        assertEval("{ f<-function(i,v) { x<-1:5 ; x[i]<-v ; x } ; f(c(3,2),1) ; f(1L,TRUE) ; f(2:4,c(4,3,2)) }");

        assertEval("{ b <- 1:3 ; b[integer()] <- 3:5 ; b }");

        assertEval("{ x <- (0:4); x[c(NA, NA, NA)] <- c(200L, 300L); x }");
        assertEval("{ x <- c(1L, 2L, 3L, 4L, 5L); x[c(NA, 2, 10)] <- c(400L, 500L, 600L); x }");
        assertEval("{ x <- c(1L, 2L, 3L, 4L, 5L); x[c(NA, 0, NA)] <- c(400L, 500L, 600L); x }");

        assertEval("{ b <- as.list(3:6) ; dim(b) <- c(4,1) ; b[c(TRUE,FALSE)] <- NULL ; b }");
        assertEval("{ b <- as.list(3:6) ; names(b) <- c(\"X\",\"Y\",\"Z\",\"Q\") ; b[c(TRUE,FALSE)] <- NULL ; b }");
        assertEval("{ b <- as.list(3:6) ; names(b) <- c(\"X\",\"Y\",\"Z\",\"Q\") ; b[c(FALSE,FALSE)] <- NULL ; b }");
        assertEval("{ b <- as.list(3:6) ; dim(b) <- c(1,4) ; b[c(FALSE,FALSE)] <- NULL ; b }");
        assertEval("{ b <- as.list(3:6) ; dim(b) <- c(1,4) ; b[c(FALSE,FALSE,TRUE)] <- NULL ; b }");
        assertEval("{ b <- as.list(3:5) ; dim(b) <- c(1,3) ; b[c(FALSE,FALSE,FALSE)] <- NULL ; b }");
        assertEval("{ b <- as.list(3:5) ; dim(b) <- c(1,3) ; b[c(FALSE,TRUE,NA)] <- NULL ; b }");

        assertEval("{ x<-1:5; x[c(0-2,0-3,0-3,0-100,0)]<-256; x }");
        assertEval("{ x<-c(1,2,3,4,5); x[c(TRUE,FALSE)] <- 1000; x }");
        assertEval("{ x<-c(1,2,3,4,5,6); x[c(TRUE,TRUE,FALSE)] <- c(1000L,2000L) ; x }");
        assertEval("{ x<-c(1,2,3,4,5); x[c(TRUE,FALSE,TRUE,TRUE,FALSE)] <- c(1000,2000,3000); x }");
        assertEval("{ x<-c(1,2,3,4,5); x[c(TRUE,FALSE,TRUE,TRUE,0)] <- c(1000,2000,3000); x }");
        assertEval("{ x<-1:3; x[c(TRUE, FALSE, TRUE)] <- c(TRUE,FALSE); x }");
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[c(TRUE, FALSE, TRUE)] <- c(FALSE,TRUE); x }");
        assertEval("{ x<-c(TRUE,TRUE,FALSE); x[c(TRUE, FALSE, TRUE)] <- c(1000,2000); x }");
        assertEval("{ x<-11:9 ; x[c(TRUE, FALSE, TRUE)] <- c(1000,2000); x }");
        assertEval("{ l <- double() ; l[c(TRUE,TRUE)] <-2 ; l}");
        assertEval("{ l <- double() ; l[c(FALSE,TRUE)] <-2 ; l}");

        assertEval("{ a<- c('a','b','c','d'); a[3:4] <- c(4,5); a}");
        assertEval("{ a<- c('a','b','c','d'); a[3:4] <- c(4L,5L); a}");
        assertEval("{ a<- c('a','b','c','d'); a[3:4] <- c(TRUE,FALSE); a}");

        assertEval("{ f<-function(b,i,v) { b[i]<-v ; b } ; f(1:4,4:1,TRUE) ; f(c(3,2,1),8,10) }");
        assertEval("{ f<-function(b,i,v) { b[i]<-v ; b } ; f(1:4,4:1,TRUE) ; f(c(3,2,1),8,10) ; f(c(TRUE,FALSE),TRUE,FALSE) }");
        assertEval("{ x<-c(TRUE,TRUE,FALSE,TRUE) ; x[3:2] <- TRUE; x }");

        assertEval("{ x<-1:3 ; y<-(x[2]<-100) ; y }");
        assertEval("{ x<-1:5 ; x[3] <- (x[4]<-100) ; x }");
        assertEval("{ x<-5:1 ; x[x[2]<-2] <- (x[3]<-50) ; x }");

        assertEval("{ v<-1:3 ; v[TRUE] <- 100 ; v }");
        assertEval("{ v<-1:3 ; v[-1] <- c(100,101) ; v }");
        assertEval("{ v<-1:3 ; v[TRUE] <- c(100,101,102) ; v }");

        assertEval("{ x <- c(a=1,b=2,c=3) ; x[2]<-10; x }");
        assertEval("{ x <- c(a=1,b=2,c=3) ; x[2:3]<-10; x }");
        assertEval("{ x <- c(a=1,b=2,c=3) ; x[c(2,3)]<-10; x }");
        assertEval("{ x <- c(a=1,b=2,c=3) ; x[c(TRUE,TRUE,FALSE)]<-10; x }");
        assertEval("{ x <- c(a=1,b=2) ; x[2:3]<-10; x }");
        assertEval("{ x <- c(a=1,b=2) ; x[c(2,3)]<-10; x }");
        assertEval("{ x <- c(a=1,b=2) ; x[3]<-10; x }");
        assertEval("{ x <- matrix(1:2) ; x[c(FALSE,FALSE,TRUE)]<-10; x }");
        assertEval("{ x <- 1:2 ; x[c(FALSE,FALSE,TRUE)]<-10; x }");
        assertEval("{ x <- c(a=1,b=2) ; x[c(FALSE,FALSE,TRUE)]<-10; x }");

        assertEval("{ x<-c(a=1,b=2,c=3) ; x[[\"b\"]]<-200; x }");
        assertEval("{ x<-c(a=1,b=2,c=3) ; x[[\"d\"]]<-200; x }");
        assertEval("{ x<-c() ; x[c(\"a\",\"b\",\"c\",\"d\")]<-c(1,2); x }");
        assertEval("{ x<-c(a=1,b=2,c=3) ; x[\"d\"]<-4 ; x }");
        assertEval("{ x<-c(a=1,b=2,c=3) ; x[c(\"d\",\"e\")]<-c(4,5) ; x }");
        assertEval("{ x<-c(a=1,b=2,c=3) ; x[c(\"d\",\"a\",\"d\",\"a\")]<-c(4,5) ; x }");

        assertEval("{ a = c(1, 2); a[['a']] = 67; a; }");
        assertEval("{ a = c(a=1,2,3); a[['x']] = 67; a; }");

        assertEval("{ x <- c(TRUE,TRUE,TRUE,TRUE); x[2:3] <- c(FALSE,FALSE); x }");
        assertEval("{ x <- c(TRUE,TRUE,TRUE,TRUE); x[3:2] <- c(FALSE,TRUE); x }");

        assertEval("{ x <- c('a','b','c','d'); x[2:3] <- 'x'; x}");
        assertEval("{ x <- c('a','b','c','d'); x[2:3] <- c('x','y'); x}");
        assertEval("{ x <- c('a','b','c','d'); x[3:2] <- c('x','y'); x}");

        assertEval("{ x <- c('a','b','c','d'); x[c(TRUE,FALSE,TRUE)] <- c('x','y','z'); x }");

        assertEval("{ x <- c(TRUE,TRUE,TRUE,TRUE); x[c(TRUE,TRUE,FALSE)] <- c(10L,20L,30L); x }");
        assertEval("{ x <- c(1L,1L,1L,1L); x[c(TRUE,TRUE,FALSE)] <- c('a','b','c'); x}");
        assertEval("{ x <- c(TRUE,TRUE,TRUE,TRUE); x[c(TRUE,TRUE,FALSE)] <- list(10L,20L,30L); x }");

        assertEval("{ x <- c(); x[c('a','b')] <- c(1L,2L); x }");
        assertEval("{ x <- c(); x[c('a','b')] <- c(TRUE,FALSE); x }");
        assertEval("{ x <- c(); x[c('a','b')] <- c('a','b'); x }");
        assertEval("{ x <- list(); x[c('a','b')] <- c('a','b'); x }");
        assertEval("{ x <- list(); x[c('a','b')] <- list('a','b'); x }");

        // negative tests
        assertEval(Output.IgnoreWarningContext, "{ x = c(1,2,3,4); x[x %% 2 == 0] <- c(1,2,3,4); }");
        assertEval("{ x <- 1:3 ; x[c(-2, 1)] <- 10 }");

        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, 10) ; f(1:2, 1:2, 11) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, TRUE) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, 11L) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, TRUE) ;  f(list(1,2), 1:2, as.raw(10))}");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(list(1,2), 1:2, c(1+2i,3+4i))}");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(1:2, 1:2, c(10,5))}");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(1:2, c(0,0), as.raw(c(11,23)))}");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, TRUE) ;  f(list(1,2), -1:1, c(2,10,5)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(list(1,2), 1:3, c(2,10,5)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(list(1,2), -10:10, 1:3) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2,3,4,5), 4:3, c(TRUE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2,3,4), seq(1L,4L,2L), c(TRUE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2),1:2,3:4) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2),1:2,c(4,3)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2),1:2,c(1+2i,3+2i)) }");

        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,1+2i) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(3,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(3L,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(TRUE,FALSE)) ; f(c(10,4), 2:1, as.raw(10)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),1:2,c(TRUE,FALSE)) ; f(c(10L,4L), 2:1, 1+2i) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10),-1:0,c(TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10), c(2L,4L),c(TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.double(1:5), seq(1L,6L,2L),c(TRUE,FALSE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.double(1:5), c(7L,4L,1L),c(TRUE,FALSE,NA)) }");

        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),2:1,1+2i) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),2:1,c(3,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),2:1,c(3L,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),1:2,c(TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),1:2,c(TRUE,FALSE)) ; f(c(10L,4L), 2:1, as.raw(10)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1L,3L,10L),1:2,c(TRUE,FALSE)) ; f(c(10,4), 2:1, 1+2i) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:5, seq(1L,6L,2L),c(TRUE,FALSE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2, seq(1L,6L,2L),c(TRUE,FALSE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2, seq(1L,-8L,-2L),c(TRUE,FALSE,NA)) }");

        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,FALSE,NA),2:1,1+2i) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,NA,FALSE),2:1,c(TRUE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,NA,FALSE),2:0,c(TRUE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,NA,FALSE),3:4,c(TRUE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.logical(-3:3),c(1L,4L,7L),c(TRUE,NA,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,FALSE),2:1,c(NA,NA)) ; f(c(TRUE,FALSE),1:2,3:4) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(TRUE,FALSE),2:1,c(NA,NA)) ; f(10:11,1:2,c(NA,FALSE)) }");

        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"a\",\"b\"),2:1,1+2i) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.character(-3:3),c(1L,4L,7L),c(\"A\",\"a\",\"XX\")) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"hello\",\"hi\",\"X\"), -1:-2, \"ZZ\") }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"hello\",\"hi\",\"X\"), 3:4, \"ZZ\") }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"hello\",\"hi\",\"X\"), 1:2, c(\"ZZ\",\"xx\")) ; f(1:4,1:2,NA) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(\"hello\",\"hi\",\"X\"), 1:2, c(\"ZZ\",\"xx\")) ; f(as.character(1:2),1:2,NA) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1+2i,2+3i), 1:2, c(10+1i,2+4i)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.raw(1:3), 1:2, as.raw(40:41)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1+2i,2+3i), 1:2, as.raw(10:11)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.raw(10:11), 1:2, c(10+1i, 11)) }");

        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(list(1,2), 1:2, c(TRUE,NA)) ;  f(1:2, c(0,0), c(1+2i,3+4i))}");
        assertEval(" { f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3, 1:2, f) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3, 1:2, 3:4); f(c(TRUE,FALSE), 2:1, 1:2) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3, 1:2, 3:4); f(3:4, 2:1, c(NA,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(f, 1:2, 1:3) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(TRUE,FALSE,NA), 1:2, c(FALSE,TRUE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4), 1:2, c(NA,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4), 1:2, c(\"hello\",\"hi\")) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4,8), 1:2, list(3,TRUE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); l <- list(3,5L) ; dim(l) <- c(2,1) ; f(5:6,1:2,c(3,4)) ; f(l, 1:2, list(3,TRUE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); l <- list(3,5L) ; dim(l) <- c(2,1) ; f(5:6,1:2,c(3,4)) ; f(list(3,TRUE), 1:2, l) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); l <- c(3,5L) ; dim(l) <- c(2,1) ; f(5:6,1:2,c(3,4)) ; f(l, 1:2, c(3,TRUE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); l <- list(3,5L) ; dim(l) <- c(2,1) ; f(5:6,1:2,c(3,4)) ; m <- c(3,TRUE) ; dim(m) <- c(1,2) ; f(m, 1:2, l) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4,8), -1:-2, 10) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(c(3,4,8), 3:4, 10) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(1:8, c(1L,4L,7L), c(10,100,1000)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; z <- f(1:8, c(1L,4L,7L), list(10,100,1000)) ; sum(as.double(z)) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; x <- list(1,2) ; attr(x,\"my\") <- 10 ; f(x, 1:2, c(10,11)) }");

        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10), seq(2L,4L,2L),c(TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.double(1:5), seq(7L,1L,-3L),c(TRUE,FALSE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.logical(-3:3),seq(1L,7L,3L),c(TRUE,NA,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.character(-3:3),seq(1L,7L,3L),c(\"A\",\"a\",\"XX\")) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(1:8, seq(1L,7L,3L), c(10,100,1000)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; z <- f(1:8, seq(1L,7L,3L), list(10,100,1000)) ; sum(as.double(z)) }");

        assertEval("{ b <- 1:3 ; b[c(3,2)] <- list(TRUE,10) ; b }");
        assertEval("{ b <- as.raw(11:13) ; b[c(3,2)] <- list(2) ; b }");
        assertEval("{ b <- as.raw(11:13) ; b[c(3,2)] <- as.raw(2) ; b }");
        assertEval("{ b <- as.raw(11:13) ; b[c(3,2)] <- 2 ; b }");
        assertEval("{ b <- c(TRUE,NA,FALSE) ; b[c(3,2)] <- FALSE ; b }");
        assertEval("{ b <- 1:4 ; b[c(3,2)] <- c(NA,NA) ; b }");
        assertEval("{ b <- c(TRUE,FALSE) ; b[c(3,2)] <- 5:6 ; b }");
        assertEval("{ b <- c(1+2i,3+4i) ; b[c(3,2)] <- 5:6 ; b }");
        assertEval("{ b <- 3:4 ; b[c(3,2)] <- c(1+2i,3+4i) ; b }");
        assertEval("{ b <- c(\"hello\",\"hi\") ; b[c(3,2)] <- c(2,3) ; b }");
        assertEval("{ b <- 3:4 ; b[c(3,2)] <- c(\"X\",\"xx\") ; b }");
        assertEval("{ b <- 3:4 ; b[c(NA)] <- c(2,7) ; b }");
        assertEval("{ b <- 3:4 ; b[c(NA,1)] <- c(2,10) ; b }");
        assertEval(Output.IgnoreErrorContext, "{ b <- 3:4 ; b[[c(NA,1)]] <- c(2,10) ; b }");
        assertEval(Output.IgnoreWarningContext, "{ b <- 3:4 ; b[c(0,1)] <- c(2,10,11) ; b }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(3:4, c(1,2), c(10,11)) ; f(4:5, as.integer(NA), 2) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(3:4, c(1,2), c(10,11)) ; f(4:5, c(1,-1), 2) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(3:4, c(1,2), c(10,11)) ; f(4:5, c(NA,-1), 2) }");

        assertEval("{ b <- c(1,4,5) ; x <- c(2,8,2) ; b[x==2] <- c(10,11) ; b }");
        assertEval("{ b <- c(1,4,5) ; z <- b ; x <- c(2,8,2) ; b[x==2] <- c(10,11) ; b }");
        assertEval(Output.IgnoreWarningContext, "{ b <- c(1,4,5) ;  x <- c(2,2) ; b[x==2] <- c(10,11) ; b }");
        assertEval("{ b <- c(1,2,5) ;  x <- c(2,2,NA) ; b[x==2] <- c(10,11,3) ; b }");
        assertEval("{ b <- c(1,2,5) ;  x <- as.double(NA) ; attr(x,\"my\") <- 2 ; b[c(1,NA,2)==2] <- x ; b }");
        assertEval(Output.IgnoreWarningContext, "{ b <- c(1,2,5) ;  x <- c(2,2,-1) ; b[x==2] <- c(10,11,5) ; b }");

        assertEval("{ b <- c(1,2,5) ; b[integer()] <- NULL ; b }");
        assertEval("{ b <- c(1,2,5) ; b[c(1)] <- NULL ; b }");
        assertEval("{ b <- c(1,2,5) ; attr(b,\"my\") <- 10 ; b[integer()] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; b[c(1,1,5)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; b[c(-1,-4,-5,-1,-5)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; b[c(1,1,0,NA,5,5,7)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; b[c(0,-1)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; b[c(1,NA)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; b[c(-1,NA)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; b[c(-1,1)] <- NULL ; b }");
        assertEval("{ b <- list(x=1,y=2,z=5) ; b[c(0,-1)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(0,-1)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(0,0)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(-10,-20,0)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(0,0,-1,-2,-3)] <- NULL ; b }");
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(0,3,5)] <- NULL ; b }");
        assertEval("{ b <- c(1,2,5) ; b[c(0,3,5)] <- NULL ; b }");

        assertEval("{ b <- c(1,2,5) ; b[c(TRUE,FALSE,FALSE)] <- NULL ; b }");
        assertEval("{ b <- c(1,2,5) ; b[logical()] <- NULL ; b }");
        assertEval("{ b <- c(1,2,5) ; b[c(TRUE,NA,TRUE)] <- list(TRUE,1+2i) ; b }");
        assertEval("{ b <- c(1,2,5) ; b[c(TRUE,FALSE,TRUE)] <- list(TRUE,1+2i) ; b }");
        assertEval("{ b <- list(1,2,5) ; dim(b) <- c(1,3) ; b[c(TRUE,FALSE,TRUE)] <- list(TRUE,1+2i) ; b }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; x <- list(1,2,5) ; dim(x) <- c(1,3) ; f(x, c(FALSE,TRUE,TRUE), list(TRUE,1+2i)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; x <- as.raw(10:12) ; dim(x) <- c(1,3) ; f(x, c(FALSE,TRUE,TRUE), as.raw(21:22)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; x <- as.raw(10:12) ; dim(x) <- c(1,3) ; f(x, c(FALSE,TRUE,TRUE), 21:22) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; x <- 10:12 ; dim(x) <- c(1,3) ; f(x, c(FALSE,TRUE,TRUE), as.raw(21:22)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(\"a\",\"XX\",\"b\"), c(FALSE,TRUE,TRUE), 21:22) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(10,12,3), c(FALSE,TRUE,TRUE), c(\"hi\",NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(10,12,3), c(FALSE,TRUE,TRUE), c(1+2i,10)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(3+4i,5+6i), c(FALSE,TRUE,TRUE), c(\"hi\",NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(3+4i,5+6i), c(FALSE,TRUE,TRUE), c(NA,1+10i)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(TRUE,FALSE), c(FALSE,TRUE,TRUE), c(NA,2L)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,TRUE,TRUE), c(NA,FALSE)) }");
        assertEval(Output.IgnoreWarningContext, "{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,TRUE,TRUE), 4:6) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,c(TRUE,FALSE,TRUE),5:6) ; f(c(TRUE,TRUE,FALSE), c(FALSE,TRUE,TRUE), c(TRUE,NA)) }");
        assertEval(" { f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,TRUE,TRUE), c(NA,FALSE)) }");
        assertEval(Output.IgnoreWarningContext, "{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,TRUE,TRUE), 4:6) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,NA), 4) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,NA), 4:5) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b, i, v) { b[[i]] <- v ; b } ; f(c(1,2,3),c(TRUE,FALSE,TRUE),5:6) ; f(3:5, c(FALSE,NA), 4:5) }");

        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(1:2, c(TRUE,FALSE), list(TRUE)) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(as.list(1:2), c(TRUE,FALSE), TRUE) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(as.list(1:2), c(TRUE,FALSE), 1+2i) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(as.list(1:2), c(TRUE,FALSE), 10) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,FALSE), list(1+2i)) ; f(as.list(1:2), c(TRUE,FALSE), 10L) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,NA), list(1+2i)) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,NA), 10) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(list(1,2), c(TRUE,NA), c(10,11)) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; x <- list(1,2) ; z <- x ; f(x, c(TRUE,NA), c(10,11)) }");
        assertEval(Output.IgnoreWarningContext, "{ x <- list(1,2) ; attr(x,\"my\") <- 10; x[c(TRUE,TRUE)] <- c(10,11,12); x }");
        assertEval(Output.IgnoreWarningContext, "{ x <- list(1,0) ; x[as.logical(x)] <- c(10,11); x }");
        assertEval("{ x <- list(1,0) ; x[is.na(x)] <- c(10,11); x }");
        assertEval(Output.IgnoreWarningContext, "{ x <- list(1,0) ; x[c(TRUE,FALSE)] <- x[2:1] ; x }");
        assertEval(Output.IgnoreWarningContext, "{ x <- list(1,0) ; attr(x,\"my\") <- 20 ; x[c(TRUE,FALSE)] <- c(11,12) ; x }");
        assertEval("{ x <- list(1,0) ; x[is.na(x)] <- c(10L,11L); x }");
        assertEval("{ x <- list(1,0) ; x[c(TRUE,TRUE)] <- c(TRUE,NA); x }");
        assertEval("{ x <- list(1,0) ; x[logical()] <- c(TRUE,NA); x }");

        assertEval("{ x <- c(1,0) ; x[c(TRUE,TRUE)] <- c(TRUE,NA); x }");
        assertEval("{ x <- c(1,0) ; x[c(TRUE,TRUE)] <- 3:4; x }");
        assertEval("{ x <- c(1,0) ; x[logical()] <- 3:4; x }");
        assertEval("{ x <- c(1,0) ; attr(x,\"my\") <- 1 ; x[c(TRUE,TRUE)] <- c(NA,TRUE); x }");
        assertEval("{ x <- c(1,0) ; x[c(NA,TRUE)] <- c(NA,TRUE); x }");
        assertEval("{ x <- c(1,0) ; z <- x ; x[c(NA,TRUE)] <- c(NA,TRUE); x }");
        assertEval("{ x <- c(1,0) ; z <- x ; x[c(NA,TRUE)] <- TRUE; x }");
        assertEval("{ x <- c(1,0)  ; x[is.na(x)] <- TRUE; x }");
        assertEval("{ x <- c(1,0)  ; x[c(TRUE,TRUE)] <- rev(x) ; x }");
        assertEval("{ x <- c(1,0) ; f <- function(v) { x[c(TRUE,TRUE)] <- v ; x } ; f(1:2) ; f(c(1,2)) }");
        assertEval("{ x <- c(1,0) ; f <- function(v) { x[c(TRUE,TRUE)] <- v ; x } ; f(1:2) ; f(1+2i) }");

        assertEval("{ b <- list(1,2,3) ; attr(b,\"my\") <- 12; b[2] <- NULL ; b }");
        assertEval("{ b <- list(1,2,3) ; attr(b,\"my\") <- 12; b[2:3] <- NULL ; b }");

        assertEval("{ x <- 1:2 ; x[c(TRUE,FALSE,FALSE,TRUE)] <- 3:4 ; x }");
        assertEval("{ x <- 1:2 ; x[c(TRUE,FALSE,FALSE,NA)] <- 3:4 ; x }");
        assertEval("{ x <- 1:2 ; x[c(TRUE,FALSE,FALSE,NA)] <- 3L ; x }");
        assertEval("{ x <- 1:2 ; x[c(TRUE,NA)] <- 3L ; x }");
        assertEval("{ x <- 1:2 ; x[c(TRUE,NA)] <- 2:3 ; x }");
        assertEval("{ x <- c(1L,2L) ; x[c(TRUE,FALSE)] <- 3L ; x }");
        assertEval("{ x <- c(1L,2L) ; x[c(TRUE,NA)] <- 3L ; x }");
        assertEval("{ x <- c(1L,2L) ; x[TRUE] <- 3L ; x }");
        assertEval("{ x <- c(1L,2L,3L,4L) ; x[c(TRUE,FALSE)] <- 5:6 ; x }");
        assertEval("{ x <- c(1L,2L,3L,4L) ; attr(x,\"my\") <- 0 ;  x[c(TRUE,FALSE)] <- 5:6 ; x }");
        assertEval("{ x <- c(1L,2L,3L,4L) ;  x[is.na(x)] <- 5:6 ; x }");
        assertEval(Output.IgnoreWarningContext, "{ x <- c(1L,2L,3L,4L) ; x[c(TRUE,FALSE)] <- rev(x) ; x }");
        assertEval("{ x <- c(1L,2L) ; x[logical()] <- 3L ; x }");

        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,FALSE)] <- c(FALSE,NA) ; b }");
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,FALSE,FALSE)] <- c(FALSE,NA) ; b }");
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,NA)] <- c(FALSE,NA) ; b }");
        assertEval(Output.IgnoreWarningContext, "{ b <- c(TRUE,NA,FALSE) ; b[c(TRUE,TRUE)] <- c(FALSE,NA) ; b }");
        assertEval("{ b <- c(TRUE,NA,FALSE) ; b[c(TRUE,FALSE,TRUE,TRUE)] <- c(FALSE,NA,NA) ; b }");
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,FALSE,TRUE,NA)] <- FALSE ; b }");
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; z <- b ; b[c(TRUE,FALSE,TRUE,NA)] <- FALSE ; b }");
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; attr(b,\"my\") <- 10 ; b[c(TRUE,FALSE,TRUE,NA)] <- FALSE ; b }");
        assertEval(Output.IgnoreWarningContext, "{ b <- c(TRUE,NA,FALSE,TRUE) ; b[c(TRUE,FALSE,TRUE,FALSE)] <- b ; b }");
        assertEval("{ b <- c(TRUE,FALSE,FALSE,TRUE) ; b[b] <- c(TRUE,FALSE) ; b }");
        assertEval(Output.IgnoreWarningContext, "{ f <- function(b,i,v) { b[b] <- b ; b } ; f(c(TRUE,FALSE,FALSE,TRUE)) ; f(1:3) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(TRUE,FALSE,FALSE,TRUE),c(TRUE,FALSE), NA) ; f(1:4, c(TRUE,TRUE), NA) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(TRUE,FALSE,FALSE,TRUE),c(TRUE,FALSE), NA) ; f(c(FALSE,FALSE,TRUE), c(TRUE,TRUE), c(1,2,3)) }");
        assertEval("{ b <- c(TRUE,NA,FALSE,TRUE) ; b[logical()] <- c(FALSE,NA) ; b }");

        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE)] <- \"X\" ; b }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE,TRUE,TRUE)] <- \"X\" ; b }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE,TRUE,NA)] <- \"X\" ; b }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE,NA)] <- \"X\" ; b }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[logical()] <- \"X\" ; b }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(FALSE,NA,NA)] <- c(\"X\",\"y\") ; b }");
        assertEval(Output.IgnoreWarningContext, "{ b <- c(\"a\",\"b\",\"c\") ; b[c(FALSE,TRUE,TRUE)] <- c(\"X\",\"y\",\"z\") ; b }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; x <- b ; b[c(FALSE,TRUE,TRUE)] <- c(\"X\",\"z\") ; b } ");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; x <- b ; b[c(FALSE,TRUE,NA)] <- c(\"X\",\"z\") ; b }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[is.na(b)] <- c(\"X\",\"z\") ; b }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; attr(b,\"my\") <- 211 ; b[c(FALSE,TRUE)] <- c(\"X\") ; b }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,TRUE,TRUE)] <- rev(as.character(b)) ; b }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(\"a\",\"b\",\"c\"),c(TRUE,FALSE),c(\"A\",\"X\")) ; f(1:3,c(TRUE,FALSE),4) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(\"a\",\"b\",\"c\"),c(TRUE,FALSE),c(\"A\",\"X\")) ; f(c(\"A\",\"X\"),c(TRUE,FALSE),4) }");
        assertEval("{ b <- c(\"a\",\"b\",\"c\") ; b[c(TRUE,FALSE,TRUE)] <- c(1+2i,3+4i) ; b }");
        assertEval("{ b <- as.raw(1:5) ; b[c(TRUE,FALSE,TRUE)] <- c(1+2i,3+4i) ; b }");

        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(\"a\",\"b\",\"c\"),c(TRUE,FALSE),c(\"A\",\"X\")) ; f(f,c(TRUE,FALSE),4) }");
        assertEval("{ f <- function(b,i,v) { b[i] <- v ; b } ; f(c(\"a\",\"b\",\"c\"),c(TRUE,FALSE),c(\"A\",\"X\")) ; f(c(\"A\",\"X\"),c(TRUE,FALSE),f) }");

        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(1:2,\"hi\",3L) ; f(1:2,c(2),10) ; f(1:2, -1, 10) }");
        assertEval("{ x <- c(); f <- function(i, v) { x[i] <- v ; x } ; f(1:2,3:4); f(c(1,2),c(TRUE,FALSE)) }");
        assertEval("{ x <- c(); f <- function(i, v) { x[i] <- v ; x } ; f(1:2,3:4); f(c(\"a\",\"b\"),c(TRUE,FALSE)) }");

        assertEval("{ a <- c(2.1,2.2,2.3); b <- a; a[[2]] <- TRUE; a }");
        assertEval("{ a <- c(2.1,2.2,2.3); b <- a; a[[3]] <- TRUE; a }");
        assertEval("{ buf <- character() ; buf[[1]] <- \"hello\" ; buf[[3]] <- \"world\" ; buf }");
        assertEval("{ b <- 1:3 ; dim(b) <- c(1,3) ;  b[integer()] <- 3:5 ; b }");

        // lazy evaluation...
        assertEval("{ x<-1:5 ; x[x[4]<-2] <- (x[4]<-100) ; x }");
        assertEval("{ x<-5:1 ; x[x[2]<-2] }");

        /*
         * this case depends on the order of evaluation: it expects the receiver of the replacement
         * to be evaluated after the index. we currently evaluate the receiver before the index, and
         * it is unclear whether (a) this is a real problem and (b) how complex it is to change
         * this.
         */
        assertEval(Ignored.Unimplemented, "{ f <- function(a) { a }; x<-1:5 ; x[x[4]<-2] <- ({x[4]<-100; f(x)[4]}) ; x }");
    }

    @Test
    public void testListDefinitions() {
        assertEval("{ list(1:4) }");
        assertEval("{ list(1,list(2,list(3,4))) }");

        assertEval("{ list(1,b=list(2,3)) }");
        assertEval("{ list(1,b=list(c=2,3)) }");
        assertEval("{ list(list(c=2)) }");
    }

    @Test
    public void testListAccess() {
        // indexing
        assertEval("{ l<-list(1,2L,TRUE) ; l[[2]] }");

        // indexing
        assertEval("{ l<-list(1,2L,TRUE) ; l[c(FALSE,FALSE,TRUE)] }");
        assertEval("{ l<-list(1,2L,TRUE) ; l[FALSE] }");
        assertEval("{ l<-list(1,2L,TRUE) ; l[-2] }");
        assertEval("{ l<-list(1,2L,TRUE) ; l[NA] }");
        assertEval("{ l<-list(1,2,3) ; l[c(1,2)] }");
        assertEval("{ l<-list(1,2,3) ; l[c(2)] }");
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[2:4] }");
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[4:2] }");
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[c(-2,-3)] }");
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[c(-2,-3,-4,0,0,0)] }");
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[c(2,5,4,3,3,3,0)] }");
        assertEval("{ x<-list(1,2L,TRUE,FALSE,5) ; x[c(2L,5L,4L,3L,3L,3L,0L)] }");
        assertEval("{ m<-list(1,2) ; m[NULL] }");

        // indexing with rewriting
        assertEval("{ f<-function(x, i) { x[i] } ; f(list(1,2,3),3:1) ; f(list(1L,2L,3L,4L,5L),c(0,0,0,0-2)) }");
        assertEval("{ x<-list(1,2,3,4,5) ; x[c(TRUE,TRUE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,NA)] }");
        assertEval("{ f<-function(i) { x<-list(1,2,3,4,5) ; x[i] } ; f(1) ; f(1L) ; f(TRUE) }");
        assertEval("{ f<-function(i) { x<-list(1,2,3,4,5) ; x[i] } ; f(1) ; f(TRUE) ; f(1L)  }");
        assertEval("{ f<-function(i) { x<-list(1L,2L,3L,4L,5L) ; x[i] } ; f(1) ; f(TRUE) ; f(c(3,2))  }");
        assertEval("{ f<-function(i) { x<-list(1,2,3,4,5) ; x[i] } ; f(1)  ; f(3:4) }");
        assertEval("{ f<-function(i) { x<-list(1,2,3,4,5) ; x[i] } ; f(c(TRUE,FALSE))  ; f(3:4) }");

        // recursive indexing
        assertEval("{ l<-(list(list(1,2),list(3,4))); l[[c(1,2)]] }");
        assertEval("{ l<-(list(list(1,2),list(3,4))); l[[c(1,-2)]] }");
        assertEval("{ l<-(list(list(1,2),list(3,4))); l[[c(1,-1)]] }");
        assertEval("{ l<-(list(list(1,2),list(3,4))); l[[c(1,TRUE)]] }");
        assertEval("{ l<-(list(list(1,2),c(3,4))); l[[c(2,1)]] }");
        assertEval("{ l <- list(a=1,b=2,c=list(d=3,e=list(f=4))) ; l[[c(3,2)]] }");
        assertEval("{ l <- list(a=1,b=2,c=list(d=3,e=list(f=4))) ; l[[c(3,1)]] }");

        assertEval("{ l <- list(c=list(d=3,e=c(f=4)), b=2, a=3) ; l[[c(\"c\",\"e\")]] }");
        assertEval("{ l <- list(c=list(d=3,e=c(f=4)), b=2, a=3) ; l[[c(\"c\",\"e\", \"f\")]] }");
        assertEval("{ l <- list(c=list(d=3,e=c(f=4)), b=2, a=3) ; l[[c(\"c\")]] }");
        assertEval("{ f <- function(b, i, v) { b[[i]] <- v ; b } ; f(1:3,2,2) ; f(1:3,\"X\",2) ; f(list(1,list(2)),c(2,1),4) }");
    }

    @Test
    public void testListUpdate() {
        // copying
        assertEval("{ x<-c(1,2,3) ; y<-x ; x[2]<-100 ; y }");
        assertEval("{ x <-2L ; y <- x; x[1] <- 211L ; y }");

        // element deletion
        assertEval("{ l <- matrix(list(1,2)) ; l[3] <- NULL ; l }");
        assertEval("{ l <- matrix(list(1,2)) ; l[4] <- NULL ; l }");

        // copying
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=list(x=3)),character(),10) }");

        // scalar update
        assertEval("{ l<-list(1,2L,TRUE) ; l[[2]]<-100 ; l }");
        assertEval("{ l<-list(1,2L,TRUE) ; l[[5]]<-100 ; l }");
        assertEval("{ l<-list(1,2L,TRUE) ; l[[3]]<-list(100) ; l }");
        assertEval("{ v<-1:3 ; v[2] <- list(100) ; v }");
        assertEval("{ v<-1:3 ; v[[2]] <- list(100) ; v }");
        assertEval("{ l <- list() ; l[[1]] <-2 ; l}");
        assertEval("{ l<-list() ; x <- 1:3 ; l[[1]] <- x  ; l }");
        assertEval("{ l <- list(1,2,3) ; l[2] <- list(100) ; l[2] }");
        assertEval("{ l <- list(1,2,3) ; l[[2]] <- list(100) ; l[2] }");

        // element deletion
        assertEval("{ m<-list(1,2) ; m[TRUE] <- NULL ; m }");
        assertEval("{ m<-list(1,2) ; m[[TRUE]] <- NULL ; m }");
        assertEval("{ m<-list(1,2) ; m[[1]] <- NULL ; m }");
        assertEval("{ m<-list(1,2) ; m[[-1]] <- NULL ; m }");
        assertEval("{ m<-list(1,2) ; m[[-2]] <- NULL ; m }");
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[1] <- NULL ; l }");
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[3] <- NULL ; l }");

        assertEval("{ l <- list(a=1,b=2,c=3) ; l[5] <- NULL ; l}");
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[4] <- NULL ; l}");
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[[5]] <- NULL ; l}");
        assertEval("{ l <- list(a=1,b=2,c=3) ; l[[4]] <- NULL ; l}");

        assertEval("{ l <- list(1,2); l[0] <- NULL; l}");
        assertEval(Output.IgnoreErrorContext, "{ l <- list(1,2); l[[0]] }");

        // vector update
        assertEval("{ l <- list(1,2,3) ; l[c(2,3)] <- c(20,30) ; l }");
        assertEval("{ l <- list(1,2,3) ; l[c(2:3)] <- c(20,30) ; l }");
        assertEval("{ l <- list(1,2,3) ; l[-1] <- c(20,30) ; l }");
        assertEval("{ l <- list(1,2,3) ; l[-1L] <- c(20,30) ; l }");
        assertEval("{ l <- list(1,2,3) ; l[c(FALSE,TRUE,TRUE)] <- c(20,30) ; l }");
        assertEval("{ l <- list() ; l[c(TRUE,TRUE)] <-2 ; l }");
        assertEval("{ x <- 1:3 ; l <- list(1) ; l[[TRUE]] <- x ; l[[1]] } ");

        assertEval("{ x<-list(1,2,3,4,5); x[3:4]<-c(300L,400L); x }");
        assertEval("{ x<-list(1,2,3,4,5); x[4:3]<-c(300L,400L); x }");
        assertEval("{ x<-list(1,2L,TRUE,TRUE,FALSE); x[c(-2,-3,-3,-100,0)]<-256; x }");
        assertEval("{ x<-list(1,2L,list(3,list(4)),list(5)) ; x[c(4,2,3)]<-list(256L,257L,258L); x }");
        assertEval("{ x<-list(FALSE,NULL,3L,4L,5.5); x[c(TRUE,FALSE)] <- 1000; x }");
        assertEval("{ x<-list(11,10,9) ; x[c(TRUE, FALSE, TRUE)] <- c(1000,2000); x }");
        assertEval("{ l <- list(1,2,3) ; x <- list(100) ; y <- x; l[1:1] <- x ; l[[1]] }");
        assertEval("{ l <- list(1,2,3) ; x <- list(100) ; y <- x; l[[1:1]] <- x ; l[[1]] }");

        // vector element deletion
        assertEval("{ v<-list(1,2,3) ; v[c(2,3,NA,7,0)] <- NULL ; v }");
        assertEval("{ v<-list(1,2,3) ; v[c(2,3,4)] <- NULL ; v }");
        assertEval("{ v<-list(1,2,3) ; v[c(-1,-2,-6)] <- NULL ; v }");
        assertEval("{ v<-list(1,2,3) ; v[c(TRUE,FALSE,TRUE)] <- NULL ; v }");
        assertEval("{ v<-list(1,2,3) ; v[c()] <- NULL ; v }");
        assertEval("{ v<-list(1,2,3) ; v[integer()] <- NULL ; v }");
        assertEval("{ v<-list(1,2,3) ; v[double()] <- NULL ; v }");
        assertEval("{ v<-list(1,2,3) ; v[logical()] <- NULL ; v }");
        assertEval("{ v<-list(1,2,3) ; v[c(TRUE,FALSE)] <- NULL ; v }");
        assertEval("{ v<-list(1,2,3) ; v[c(TRUE,FALSE,FALSE,FALSE,FALSE,TRUE)] <- NULL ; v }");

        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(-1,-3)] <- NULL ; l}");
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(-1,-10)] <- NULL ; l}");
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(2,3)] <- NULL ; l}");
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(2,3,5)] <- NULL ; l}");
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(2,3,6)] <- NULL ; l}");
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(TRUE,TRUE,FALSE,TRUE)] <- NULL ; l}");
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(TRUE,FALSE)] <- NULL ; l}");
        assertEval("{ l<-list(a=1,b=2,c=3,d=4); l[c(TRUE,FALSE,FALSE,TRUE,FALSE,NA,TRUE,TRUE)] <- NULL ; l}");

        assertEval("{ l <- list(a=1,b=2,c=3) ; l[[\"b\"]] <- NULL ; l }");

        // recursive indexing
        assertEval("{ l <- list(1,list(2,c(3))) ; l[[c(2,2)]] <- NULL ; l }");
        assertEval("{ l <- list(1,list(2,c(3))) ; l[[c(2,2)]] <- 4 ; l }");
        assertEval("{ l <- list(1,list(2,list(3))) ; l[[1]] <- NULL ; l }");
        assertEval("{ l <- list(1,list(2,list(3))) ; l[[1]] <- 5 ; l }");

        assertEval("{ l<-list(a=1,b=2,list(c=3,d=4,list(e=5:6,f=100))) ; l[[c(3,3,1)]] <- NULL ; l }");
        assertEval("{ l<-list(a=1,b=2,c=list(d=1,e=2,f=c(x=1,y=2,z=3))) ; l[[c(\"c\",\"f\",\"zz\")]] <- 100 ; l }");
        assertEval("{ l<-list(a=1,b=2,c=list(d=1,e=2,f=c(x=1,y=2,z=3))) ; l[[c(\"c\",\"f\",\"z\")]] <- 100 ; l }");
        assertEval("{ l<-list(a=1,b=2,c=list(d=1,e=2,f=c(x=1,y=2,z=3))) ; l[[c(\"c\",\"f\")]] <- NULL ; l }");
        assertEval("{ l<-list(a=1,b=2,c=3) ; l[c(\"a\",\"a\",\"a\",\"c\")] <- NULL ; l }");
        assertEval("{ l<-list(a=1L,b=2L,c=list(d=1L,e=2L,f=c(x=1L,y=2L,z=3L))) ; l[[c(\"c\",\"f\",\"zz\")]] <- 100L ; l }");
        assertEval("{ l<-list(a=TRUE,b=FALSE,c=list(d=TRUE,e=FALSE,f=c(x=TRUE,y=FALSE,z=TRUE))) ; l[[c(\"c\",\"f\",\"zz\")]] <- TRUE ; l }");
        assertEval("{ l<-list(a=\"a\",b=\"b\",c=list(d=\"cd\",e=\"ce\",f=c(x=\"cfx\",y=\"cfy\",z=\"cfz\"))) ; l[[c(\"c\",\"f\",\"zz\")]] <- \"cfzz\" ; l }");

        assertEval("{ l<-list(a=1,b=2,c=list(d=1,e=2,f=c(x=1,y=2,z=3))) ; l[[c(\"c\",\"f\",\"zz\")]] <- list(100) ; l }");
        assertEval("{ l<-list(a=1L,b=2L,c=list(d=1L,e=2L,f=c(x=1L,y=2L,z=3L))) ; l[[c(\"c\",\"f\")]] <- 100L ; l }");
        assertEval("{ l<-list(a=1L,b=2L,c=list(d=1L,e=2L,f=c(x=1L,y=2L,z=3L))) ; l[[c(\"c\",\"f\")]] <- list(haha=\"gaga\") ; l }");

        assertEval(Output.IgnoreErrorContext, "{ l <- list(list(1,2),2) ; l[[c(1,1,2,3,4,3)]] <- 10 ; l }");
        assertEval(Output.IgnoreErrorContext, "{ l <- list(1,2) ; l[[c(1,1,2,3,4,3)]] <- 10 ; l }");

        // copying
        assertEval("{ l<-list() ; x <- 1:3 ; l[[1]] <- x; x[2] <- 100L; l[[1]] }");
        assertEval("{ l <- list(1, list(2)) ;  m <- l ; l[[c(2,1)]] <- 3 ; m[[2]][[1]] }");
        assertEval("{ l <- list(1, list(2,3,4)) ;  m <- l ; l[[c(2,1)]] <- 3 ; m[[2]][[1]] }");
        assertEval("{ x <- c(1L,2L,3L) ; l <- list(1) ; l[[1]] <- x ; x[2] <- 100L ; l[[1]] }");
        assertEval("{ l <- list(100) ; f <- function() { l[[1]] <- 2 } ; f() ; l }");
        assertEval("{ l <- list(100,200,300,400,500) ; f <- function() { l[[3]] <- 2 } ; f() ; l }");
        assertEval("{ f <- function() { l[1:2] <- x ; x[1] <- 211L  ; l[1] } ; l <- 1:3 ; x <- 10L ; f() }");

        assertEval(Output.IgnoreErrorContext, "{ l <- as.list(1:3) ; l[[0]] <- 2 }");
        assertEval(Output.IgnoreErrorContext, "{ x <- as.list(1:3) ; x[[integer()]] <- 3 }");
        assertEval("{ x <- list(1,list(2,3),4) ; x[[c(2,3)]] <- 3 ; x }");
        assertEval("{ x <- list(1,list(2,3),4) ; z <- x[[2]] ; x[[c(2,3)]] <- 3 ; z }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(list(1,2,3), 2L, NULL) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(list(1,2,3), 2L, 3) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(list(f,f), c(1,1), 3) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(c(1,2,3), 2L, 1:2) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(c(1,2,3), f, 2) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(c(1,2,3), \"hello\", 2) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=list(x=3)),c(\"b\",\"x\"),10) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=c(x=3)),c(\"b\",\"x\"),10) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(c(1,2,b=c(x=3)),c(\"b\",\"x\"),10) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(c(1,2,b=c(x=3)),c(\"b\"),10) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2, list(3)),c(\"b\",\"x\"),10) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=list(3)),c(\"a\",\"x\"),10) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=f),c(\"b\",\"x\"),3) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,b=list(a=list(x=1,y=2),3),4),c(\"b\",\"a\",\"x\"),10) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=2),\"b\",NULL) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=list(2)),\"b\",double()) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=c(a=2)),c(\"b\",\"a\"),1:3) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=c(a=2)),1+2i,1:3) }");
        assertEval(" { f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(list(a=1,b=c(a=2)),c(TRUE,TRUE),3) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(f,TRUE,3) }");
        assertEval(Ignored.Unstable, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(c(a=1,b=2),\"b\",NULL) }");
        assertEval("{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(c(a=1,b=2),\"b\",as.raw(12)) }");
        assertEval(Output.IgnoreErrorMessage, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ;  f(list(1,2,b=list(a=1)),c(\"b\",\"a\"),10) ; f(c(a=1,b=2),c(1+2i,3+4i),as.raw(12)) }");
        assertEval("{ l <- list(a=1,b=2,cd=list(c=3,d=4)) ; x <- list(l,xy=list(x=l,y=l)) ; x[[c(2,2,3,2)]] <- 10 ; l }");
        assertEval("{ l <- list(a=1,b=2,cd=list(c=3,d=4)) ; x <- list(l,xy=list(x=l,y=l)) ; x[[c(\"xy\",\"y\",\"cd\",\"d\")]] <- 10 ; l }");

        assertEval("{ l <- matrix(list(1,2)) ; l[[3]] <- NULL ; l }");
        assertEval("{ l <- matrix(list(1,2)) ; l[[4]] <- NULL ; l }");

        assertEval(Ignored.Unstable, "{ f <- function(b,i,v) { b[[i]] <- v ; b } ; f(list(1,2,list(3)), c(3,1), 4) ; f(c(1,2,3), 2L, NULL) }");
    }

    @Test
    public void testStringUpdate() {
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(1:3,\"a\",4) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(NULL,\"a\",4) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(NULL,c(\"a\",\"X\"),4:5) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(double(),c(\"a\",\"X\"),4:5) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(double(),c(\"a\",\"X\"),list(3,TRUE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.raw(11:13),c(\"a\",\"X\"),list(3,TRUE)) }");
        assertEval("{ b <- c(11,12) ; b[\"\"] <- 100 ; b }"); // note
        // the whitespace
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(1,a=2),c(\"a\",\"X\",\"a\"),list(3,TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"a\",\"X\",\"a\"),list(3,TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.raw(c(13,14)),c(\"a\",\"X\",\"a\"),c(3,TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.complex(c(13,14)),as.character(NA),as.complex(23)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.complex(c(13,14)),character(),as.complex(23)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.complex(c(13,14)),c(\"\",\"\",\"\"),as.complex(23)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.complex(c(13,14)),c(\"\",\"\",NA),as.complex(23)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(as.raw(c(13,14)),c(\"a\",\"X\",\"a\"),as.raw(23)) }");
        assertEval(Output.IgnoreWarningContext, "{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"a\",\"X\",\"a\",\"b\"),list(3,TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),list(3,TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),as.raw(10)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),as.complex(10)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:3,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),1:3) }");
        assertEval(Output.IgnoreWarningContext, "{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; f(c(X=1,a=2),c(\"X\",\"b\",NA),c(TRUE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; f(c(X=1L,a=2L),c(\"X\",\"b\",NA),c(TRUE,NA,FALSE)) }");
        assertEval(Output.IgnoreErrorContext, "{ f <- function(b, i, v) { b[[i]] <- v ; b } ; f(1+2i,3:1,4:6) ; f(c(X=1L,a=2L),c(\"X\",\"b\",NA),NULL) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; f(list(X=1L,a=2L),c(\"X\",\"b\",NA),NULL) }");

        assertEval("{ b <- c(a=1+2i,b=3+4i) ; dim(b) <- c(2,1) ; b[c(\"a\",\"b\")] <- 3+1i ; b }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; b <- list(1L,2L) ; attr(b,\"my\") <- 21 ; f(b,c(\"X\",\"b\",NA),NULL) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; b <- list(b=1L,2L) ; attr(b,\"my\") <- 21 ; f(b,c(\"X\",\"b\",NA),NULL) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1+2i,3:1,4:6) ; b <- list(b=1L,2L) ; attr(b,\"my\") <- 21 ; f(b,c(\"ZZ\",\"ZZ\",NA),NULL) }");

        assertEval("{ b <- list(1+2i,3+4i) ; dim(b) <- c(2,1) ; b[c(\"hello\",\"hi\")] <- NULL ; b }");

        // FIXME print format
        assertEval("{ a <- 'hello'; a[[5]] <- 'done'; a[[3]] <- 'muhuhu'; a; }");
        assertEval("{ a <- 'hello'; a[[5]] <- 'done'; b <- a; b[[3]] <- 'muhuhu'; b; }");
    }

    @Test
    public void testGenericUpdate() {
        assertEval("{ a <- TRUE; a[[2]] <- FALSE; a; }");
    }

    @Test
    public void testNullUpdate() {
        assertEval("{ x <- NULL; x[[1]] <- c(5); x; }");
        assertEval("{ x <- NULL; x[1] <- c(5); x; }");
        assertEval("{ x <- NULL; x[c(1,2)] <- c(5); x; }");
        assertEval("{ x <- NULL; x[c(1,2)] <- c(1,5); x; }");

        assertEval("{ x <- NULL; x[[0]] <- c(); x; }");
        assertEval("{ x <- NULL; x[[1]] <- c(); x; }");
        assertEval("{ x <- NULL; x[[c(1,0)]] <- c(); x; }");
        assertEval("{ x <- NULL; x[[c(1,2)]] <- c(); x; }");
        assertEval("{ x <- NULL; x[[c(0,1)]] <- c(); x; }");
        assertEval("{ x <- NULL; x[[c(0,2)]] <- c(); x; }");
        assertEval(Output.IgnoreErrorContext, "{ x <- NULL; x[[0]] <- c(5); x; }");
        assertEval(Output.IgnoreErrorContext, "{ x <- NULL; x[[c(1,0)]] <- c(5); x; }");
        assertEval(Output.IgnoreErrorContext, "{ x <- NULL; x[[c(1,2)]] <- c(5); x; }");
        assertEval(Output.IgnoreErrorContext, "{ x <- NULL; x[[c(0,1)]] <- c(5); x; }");
        assertEval(Output.IgnoreErrorContext, "{ x <- NULL; x[[c(0,2)]] <- c(5); x; }");
        assertEval(Output.IgnoreErrorContext, "{ x <- NULL; x[[0]] <- c(1,5); x; }");
        assertEval(Output.IgnoreErrorContext, "{ x <- NULL; x[[c(0,1)]] <- c(1,5); x; }");
        assertEval(Output.IgnoreErrorContext, "{ x <- NULL; x[[c(0,2)]] <- c(1,5); x; }");
        assertEval("{ x <- NULL; x[0] <- c(); x; }");
        assertEval("{ x <- NULL; x[1] <- c(); x; }");
        assertEval("{ x <- NULL; x[c(1,0)] <- c(); x; }");
        assertEval("{ x <- NULL; x[c(1,2)] <- c(); x; }");
        assertEval("{ x <- NULL; x[c(0,1)] <- c(); x; }");
        assertEval("{ x <- NULL; x[c(0,2)] <- c(); x; }");
        assertEval("{ x <- NULL; x[0] <- c(5); x; }");
        assertEval("{ x <- NULL; x[c(1,0)] <- c(5); x; }");
        assertEval("{ x <- NULL; x[c(0,1)] <- c(5); x; }");
        assertEval("{ x <- NULL; x[c(0,2)] <- c(5); x; }");
        assertEval("{ x <- NULL; x[0] <- c(1,5); x; }");
        assertEval(Output.IgnoreWarningContext, "{ x <- NULL; x[1] <- c(1,5); x; }");
        assertEval(Output.IgnoreWarningContext, "{ x <- NULL; x[c(1,0)] <- c(1,5); x; }");
        assertEval(Output.IgnoreWarningContext, "{ x <- NULL; x[c(0,1)] <- c(1,5); x; }");
        assertEval(Output.IgnoreWarningContext, "{ x <- NULL; x[c(0,2)] <- c(1,5); x; }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- NULL; x[[c(1,0)]] <- c(1,5); x; }");
        assertEval(Output.IgnoreErrorMessage, "{ x <- NULL; x[[c(1,2)]] <- c(1,5); x; }");

        assertEval(Ignored.Unknown, "{ x <- NULL; x[[1]] <- c(1,5); x; }");
    }

    @Test
    public void testSuperUpdate() {
        assertEval("{ x <- 1:3 ; f <- function() { x[2] <<- 100 } ; f() ; x }");
        assertEval("{ x <- 1:3 ; f <- function() { x[2] <- 10 ; x[2] <<- 100 ; x[2] <- 1000 } ; f() ; x }");
    }

    @Test
    public void testMatrixIndex() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1,2] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1,] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,1] }");

        assertEval("{ m <- matrix(1:6, nrow=3) ; f <- function(i,j) { m[i,j] } ; f(1,c(1,2)) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] } ; f(1,1) }");
        assertEval("{ m <- matrix(1:6, nrow=3) ; f <- function(i,j) { m[i,j] } ; f(1,c(-1,0,-1,-10)) }");

        assertEval("{ m <- matrix(1:6, nrow=2) ; x<-2 ; m[[1,x]] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[[1,2]] }");

        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1:2,2:3] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1:2,-1] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,c(-1,0,0,-1)] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,c(1,NA,1,NA)] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,c(NA,1,0)] }");

        assertEval("{ m <- matrix(1:6, nrow=3) ; f <- function(i,j) { m[i,j] } ; f(c(TRUE),c(FALSE,TRUE)) }");

        assertEval("{ m <- matrix(1:6, nrow=2) ; f <- function(i,j) { m[i,j] } ; f(1,1:3) }");

        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1:2,0:1] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1:2,0:1] ; m[1:2,1:1] }");

        // FIXME print regression
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,-1] }");

        // non-index argument not supported
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[1,,drop=FALSE] }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; m[,1[2],drop=FALSE] }");
        assertEval("{ m <- matrix(1:16, nrow=8) ; m[c(TRUE,FALSE,FALSE),c(FALSE,NA), drop=FALSE]}");
        assertEval("{ m <- matrix(1:16, nrow=8) ; m[c(TRUE,FALSE),c(FALSE,TRUE), drop=TRUE]}");
        assertEval("{ m <- matrix(1:16, nrow=8) ; m[c(TRUE,FALSE,FALSE),c(FALSE,TRUE), drop=TRUE]}");
        assertEval("{ m <- matrix(1:4, nrow=2) ; m[[2,1,drop=FALSE]] }");
    }

    @Test
    public void testIn() {
        assertEval("{ 1:3 %in% 1:10 }");
        assertEval("{ 1 %in% 1:10 }");
        assertEval("{ c(\"1L\",\"hello\") %in% 1:10 }");
        assertEval("{ (1 + 2i) %in% c(1+10i, 1+4i, 2+2i, 1+2i) }");
        assertEval("{ as.logical(-1:1) %in% TRUE }");
        assertEval(Output.IgnoreErrorContext, "{ x <- function(){1} ; x %in% TRUE }");
    }

    @Test
    public void testEmptyUpdate() {
        assertEval("{ a <- list(); a$a = 6; a; }");
        assertEval("{ a <- list(); a[['b']] = 6; a; }");
    }

    @Test
    public void testFieldAccess() {
        assertEval("{ a <- list(a = 1, b = 2); a$a; }");
        assertEval("{ a <- list(a = 1, b = 2); a$b; }");
        assertEval("{ a <- list(a = 1, b = 2); a$c; }");
        assertEval("{ a <- list(a = 1, b = 2); a$a <- 67; a; }");
        assertEval("{ a <- list(a = 1, b = 2); a$b <- 67; a; }");
        assertEval("{ a <- list(a = 1, b = 2); a$c <- 67; a; }");
        assertEval("{ v <- list(xb=1, b=2, aa=3, aa=4) ; v$aa }");
        assertEval("{ x <- list(1, 2) ; x$b }");
        assertEval("{ x <- list(a=1, b=2) ; f <- function(x) { x$b } ; f(x) ; f(1:3) }");
        assertEval("{ x <- list(a=1, b=2) ; f <- function(x) { x$b } ; f(x) ; f(x) }");
        assertEval("{ x <- list(a=1, b=2) ; f <- function(x) { x$b } ; f(x) ; x <- list(c=2,b=10) ; f(x) }");

        // partial matching
        assertEval("{ v <- list(xb=1, b=2, aa=3, aa=4) ; v$x }");
        assertEval("{ v <- list(xb=1, b=2, aa=3, aa=4) ; v$a }");
        assertEval("{ f <- function(v) { v$x } ; f(list(xa=1, xb=2, hello=3)) ; f(list(y=2,x=3)) }");

        // rewriting
        assertEval("{ f <- function(v) { v$x } ; f(list(xa=1, xb=2, hello=3)) ; l <- list(y=2,x=3) ; f(l) ; l[[2]] <- 4 ; f(l) }");

        // make sure that dollar only works for lists
        assertEval("{ a <- c(a=1,b=2); a$a; }");
        // make sure that coercion returns warning
        assertEval("{ a <- c(1,2); a$a = 3; a; }");

        assertEval("{ x<-data.frame(a=list(1,2)); y<-list(bb=x, c=NULL); y$b$a.1 }");
        assertEval("{ x<-data.frame(a=list(1,2)); y<-list(bb=x, c=NULL); y$b$a.2 }");

        assertEval("{ x<-list(list(a=7), NULL); x[[1]]$a<-42; x }");

        assertEval("{ x<-list(a=list(b=7)); x$a$b<-42; x }");
        assertEval("{ x<-list(a=list(b=7)); x[[\"a\"]]$b<-42; x }");
        assertEval("{ x<-list(a=list(b=7)); x$a[[\"b\"]]<-42; x }");

        assertEval("{ e <- list(a=2) ; e$a }");
        assertEval("{ e <- list(a=2) ; e$\"a\" }");

        assertEval("{ x<-NULL; x$a }");

        assertEval("{ x<-list(a=7, 42); x$a<-NULL; x }");
        assertEval("{ x<-list(1, a=7, 42); x$a<-NULL; x }");
        assertEval("{ x<-c(a=7, 42); x$a<-NULL; x }");
    }

    @Test
    public void testLengthUpdate() {
        assertEval("{ k <- c(1,2,3) ; length(k) <- 5 ; k }");
        assertEval("{ k <- c(1,2,3,4,5,6,7,8,9) ; length(k) <- 4 ; k }");
    }

    private static final String[] TESTED_4L_VECTORS = new String[]{"(1:4)", "c(1.1, 2.2, 3.3, 4.4)", "c(1+1i, 2+2i, 3+3i, 4+4i)", "c(\"a\", \"b\", \"c\", \"d\")", "c(TRUE, FALSE, TRUE, FALSE)",
                    "c(as.raw(1),as.raw(2),as.raw(3),as.raw(4))", "list(TRUE, \"a\", 42, 1.1)"};

    @Test
    public void testPrint() {
        assertEval("{ x<-1:8; dim(x)<-c(2, 4); x }");
        assertEval("{ x<-c(1,2); y<-list(x, 1, 2, 3); dim(y)<-c(2, 2); y }");
        assertEval("{ x<-integer(0); y<-list(x, 1, 2, 3); dim(y)<-c(2, 2); y }");
        assertEval("{ x<-character(0); y<-list(x, 1+1i, 2+2i, 3+3i); dim(y)<-c(2, 2); y }");
        assertEval("{ x<-list(1,2,3,4); dim(x)<-c(2, 2); y<-list(x, 1, 2, 3); dim(y)<-c(2, 2); y }");
        assertEval("{ z<-list(1,2,3,4); dim(z)<-c(2,2); x<-list(z,2,3,42); dim(x)<-c(2, 2); y<-list(x, 1, 2, 3); dim(y)<-c(2, 2); y }");
        assertEval("{ x<-1:8; dim(x)<-c(2, 4); toString(x) }");
        assertEval("{ x<-list(1, 2, 3, 4); dim(x)<-c(2, 2); toString(x) }");

        // multiple dimensions
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); x }");
        assertEval("{ x<-list(1,2,3,4,5,6,7,8); dim(x)<-c(2,2,2); x }");
        assertEval("{ x<-1:16; dim(x)<-c(2,2,2,2); x }");
        assertEval("{ x<-1:32; dim(x)<-c(2,2,2,2,2); x }");
        assertEval("{ x<-1:64; dim(x)<-c(2,2,2,2,2,2); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), NULL, NULL); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), c(103, 104), NULL); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), c(103, 104), c(105, 106)); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), c(105, 106)); x }");
        assertEval("{ x<-1:8; dim(x)<-c(2,2,2); dimnames(x)<-list(c(101, 102), NULL, c(105, 106)); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(c(\"a\", \"b\"), \"c\", c(\"d\", \"e\")); x }");
        assertEval("{ x<-101:108; dim(x)<-c(2,2,2); dimnames(x)<-list(c(1, 2), c(3, 4), c(5, 6)); x }");
        assertEval("{ x<-10001:10008; dim(x)<-c(2,2,2); x }");
        assertEval("{ x<-c(1:2, 100003:100004,10005:10008); dim(x)<-c(2,2,2); x }");
        assertEval("{ x<-c(1:4,10005:10008); dim(x)<-c(2,2,2); x }");
        assertEval("{ x<-1:16; dim(x)<-c(2,4,2); x }");
        assertEval("{ x<-1:32; dim(x)<-c(4,2,2,2); x }");
        assertEval("{ x<-1:32; dim(x)<-c(2,4,2,2); x }");
        assertEval("{ x<-1:32; dim(x)<-c(2,2,4,2); x }");
        assertEval("{ x<-1:32; dim(x)<-c(2,2,2,4); x }");
        assertEval("{ x<-1:64; dim(x)<-c(4,4,4); x }");
        assertEval("{ x<-1:256; dim(x)<-c(4,4,4,4); x }");
        assertEval("{ x<-1:64; dim(x)<-c(2,2,2,4,2); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2,1); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,2,1,1); x }");
        assertEval("{ x<-1:4; dim(x)<-c(1,2,2); x }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(0, 1); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(0, 3); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(3, 0); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(0, 0); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 2); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(0, 0, 2, 2, 2); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0); dimnames(x)<-list(\"a\"); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(0, 1); dimnames(x)<-list(NULL, \"a\"); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 2, 2, 2); dimnames(x)<-list(\"a\", NULL, c(\"b\", \"c\"), c(\"d\", \"e\"), c(\"f\", \"g\")); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(0, 4); dimnames(x)<-list(NULL, c(\"a\", \"bbbbbbbbbbbb\", \"c\", \"d\")); x }");
        assertEval("{ x<-c(3+2i, 5+0i, 1+3i, 5+3i, 2-4i, 5-2i, 6-7i, 5-0i); dim(x)<-c(2,2,2); x }");

        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"a\", \"b\", \"c\", \"d\")); x[0, c(1,1,1,1)] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"aaa\", \"b\", \"c\", \"d\")); x[0, c(1,1,1,1)] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"aaa\", \"b\", \"c\", \"d\")); x[0, c(1,2,3,4)] }", TESTED_4L_VECTORS));
        assertEval(template("{ x<-%0; dim(x)<-c(1,4); dimnames(x)<-list(\"z\", c(\"aaa\", \"b\", \"cc\", \"d\")); x[0, c(1,2,3,4)] }", TESTED_4L_VECTORS));

        assertEval("{ mp<-getOption(\"max.print\"); options(max.print=3); x<-c(1,2,3,4,5); print(x); options(max.print=mp) }");
        assertEval("{ mp<-getOption(\"max.print\"); options(max.print=3); x<-c(1,2,3,4,5); attr(x, \"foo\")<-\"foo\"; print(x); options(max.print=mp) }");

        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 0); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 0, 2); x }");
        assertEval("{ x<-integer(0); dim(x)<-c(1, 0, 2, 0, 2); x }");
    }

    @Test
    public void testQuotes() {
        assertEval("{ e <- quote(x(y=z)); e[[2]] }");
        assertEval("{ e <- quote(x(y=z)); e[2] }");
        assertEval("{ e <- quote(x(y=z)); names(e[2]) }");
        assertEval("{ e <- quote(x(y=z)); typeof(e[2]) }");
        assertEval("{ e <- quote(x(y=z)); typeof(e[[2]]) }");
    }

    @Test
    public void testUpdateOther() {
        assertEval("{ a <- c(TRUE, FALSE); b <- c(a=3, b=4); a[b] <- c(TRUE, FALSE); a }");
        assertEval("{ f <- function(a, i1, i2) {a[i1, i2]}; a <- rep(c('1'),14); dim(a) <- c(2,7); dimnames(a) <- list(c('a','b'), rep('c',7)); temp <- f(a,,1); dimnames(a) <- list(NULL, rep('c',7)); f(a,,1) }");
        assertEval("{ x<-c(1,2); f<-function() { x<-c(100, 200); x[1]<-4; print(x) } ; f(); x }");
        assertEval("{ x<-c(1,2); f<-function() { x<-c(100, 200); x[1]<<-4; print(x) } ; f(); x }");

        assertEval("{ x<-quote(foo(42)); x[character(0)]<-list(); typeof(x) }");
        assertEval("{ x<-as.pairlist(list(7,42)); x[character(0)]<-list(); typeof(x) }");
        assertEval("{ x<-expression(y, z, 7 + 42); x[character(0)]<-list(); typeof(x) }");
    }

    @Test
    public void testDimnamesNames() {
        assertEval("v <- 1:8; dim(v) <- c(2,2,2); dimnames(v) <- list(foo=c('a','b'), bar=c('x','y'), baz=c('u','v')); v[,,1]");
        assertEval("v <- 1:8; dim(v) <- c(2,2,2); dimnames(v) <- list(foo=c('a','b'), bar=c('x','y'), baz=c('u','v')); v[,,2,drop=FALSE]");
        assertEval("v <- 1:8; dim(v) <- c(2,2,2); dimnames(v) <- list(foo=c('a','b'), bar=c('x','y'), baz=c('u','v')); v[,,1,drop=TRUE]");
    }
}
