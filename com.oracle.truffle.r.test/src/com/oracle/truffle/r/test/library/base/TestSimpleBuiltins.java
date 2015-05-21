/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.library.base;

import java.util.*;

import org.junit.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.test.*;

public class TestSimpleBuiltins extends TestBase {

    @Test
    public void testBincode() {
        assertEval("{ x <- c(0, 0.01, 0.5, 0.99, 1); b <- c(0, 0, 1, 1); .bincode(x, b, TRUE) }");
        assertEval("{ x <- c(0, 0.01, 0.5, 0.99, 1); b <- c(0, 0, 1, 1); .bincode(x, b, FALSE) }");
        assertEval("{ x <- c(0, 0.01, 0.5, 0.99, 1); b <- c(0, 0, 1, 1); .bincode(x, b, TRUE, TRUE) }");
        assertEval("{ x <- c(0, 0.01, 0.5, 0.99, 1); b <- c(0, 0, 1, 1); .bincode(x, b, FALSE, TRUE) }");
    }

    @Test
    public void testCopyDDFattr() {
        assertEval("{ x<-7; attr(x, \"foo\")<-\"foo\"; y<-42; z<-.Internal(copyDFattr(x, y)); attributes(z) }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(11,12)); y<-7; z<-.Internal(copyDFattr(y, x)); attributes(z) }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(11,12)); y<-7; attr(y, \"foo\")<-\"foo\"; z<-.Internal(copyDFattr(y, x)); attributes(z) }");
        assertEval("{ x<-data.frame(c(1,2), c(11,12)); attr(x, \"dim\")<-c(1,2); attr(x, \"dimnames\")<-list(\"a\", c(\"b\", \"c\")); y<-c(7, 42); z<-.Internal(copyDFattr(x, y)); attributes(z) }");
    }

    @Test
    public void testUnique() {
        assertEval("{x<-factor(c(\"a\", \"b\", \"a\")); unique(x) }");
    }

    @Test
    public void testIsType() {
        assertEval("{ is.integer(seq(1,2)) }");
        assertEval("{ is.integer(seq(1L,2L)) }");
    }

    @Test
    public void testAllEqual() {
        assertEval("{ all.equal(data.frame(list(1,2,3)), data.frame(list(1,2,3))) }");
    }

    @Test
    public void testModeSet() {
        assertEval("{  x<-c(1,2); mode(x)<-\"character\"; x }");
    }

    @Test
    public void testTable() {
        assertEval("{ a<-c(\"a\", \"b\", \"c\");  t<-table(a, sample(a)); dimnames(t) }");
    }

    @Test
    public void testScan() {
        // from scan's documentation
        assertEval("{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, skip = 1, quiet = TRUE) }");
        assertEval("{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, skip = 1) }");
        assertEval("{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, skip = 1, nlines = 1) }");
        assertEval(Output.ContainsWarning, "{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, what = list(\"\",\"\",\"\")) }");
        assertEval("{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, what = list(\"\",\"\",\"\"), flush=TRUE) }");

        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, skip = 1) }");
        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, skip = 1, quiet=TRUE) }");
        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, skip = 1, nlines = 1) }");
        assertEval(Output.ContainsWarning, "{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, what = list(\"\",\"\",\"\")) }");

        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, what = list(\"\",\"\",\"\"), fill=TRUE) }");
        assertEval(Output.ContainsError, "{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, what = list(\"\",\"\",\"\"), multi.line=FALSE) }");
        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, what = list(\"\",\"\",\"\"), fill=TRUE, multi.line=FALSE) }");

        assertEval("{ con<-textConnection(c(\"\\\"2\\\"\", \"\\\"11\\\"\")); scan(con, what=list(\"\")) }");
        assertEval("{ con<-textConnection(c(\"2 3 5\", \"\", \"11 13 17\")); scan(con, what=list(\"\")) }");
        assertEval("{ con<-textConnection(c(\"2 3 5\", \"\", \"11 13 17\")); scan(con, what=list(\"\"), blank.lines.skip=FALSE) }");
        assertEval("{ con<-textConnection(c(\"2 3 5\", \"\", \"11 13 17\")); scan(con, what=list(integer()), blank.lines.skip=FALSE) }");

        assertEval("{ con<-textConnection(c(\"foo faz\", \"\\\"bar\\\" \\\"baz\\\"\")); scan(con, what=list(\"\", \"\")) }");
        assertEval("{ con<-textConnection(c(\"foo faz\", \"bar \\\"baz\\\"\")); scan(con, what=list(\"\", \"\")) }");
        assertEval("{ con<-textConnection(c(\"foo, faz\", \"bar, baz\")); scan(con, what=list(\"\", \"\"), sep=\",\") }");

    }

    @Test
    public void testPMax() {
        assertEval("{ pmax(c(1L, 7L), c(42L, 1L)) }");
        assertEval("{ pmax(c(1L, 7L), integer()) }");
        assertEval(Output.ContainsWarning, "{ pmax(c(1L, 7L, 8L), c(1L), c(42L, 1L)) }");
        assertEval("{ pmax(c(1L, 7L), c(42L, as.integer(NA))) }");
        assertEval("{ pmax(c(1L, 7L), c(42L, as.integer(NA)), na.rm=TRUE) }");

        assertEval("{ pmax(c(1, 7), c(42, 1)) }");
        assertEval("{ pmax(c(1, 7), double()) }");
        assertEval(Output.ContainsWarning, "{ pmax(c(1, 7, 8), c(1), c(42, 1)) }");
        assertEval("{ pmax(c(1, 7), c(42, as.double(NA))) }");
        assertEval("{ pmax(c(1, 7), c(42, as.double(NA)), na.rm=TRUE) }");

        assertEval("{ pmax(c(\"1\", \"7\"), c(\"42\", \"1\")) }");
        assertEval("{ pmax(c(\"1\", \"7\"), character()) }");
        assertEval(Output.ContainsWarning, "{ pmax(c(\"1\", \"7\", \"8\"), c(\"1\"), c(\"42\", \"1\")) }");
        assertEval("{ pmax(c(\"1\", \"7\"), c(\"42\", as.character(NA))) }");
        assertEval("{ pmax(c(\"1\", \"7\"), c(\"42\", as.character(NA)), na.rm=TRUE) }");
        assertEval("{ pmax(c(\"1\", as.character(NA)), c(\"42\", \"1\"), na.rm=TRUE) }");
        assertEval("{ pmax(c(\"1\", as.character(NA)), c(as.character(NA), as.character(NA)), c(\"42\", \"1\"), na.rm=TRUE) }");

        assertEval("{ pmax(c(FALSE, TRUE), c(TRUE, FALSE)) }");
        assertEval("{ pmax(c(FALSE, TRUE), logical()) }");
        assertEval("{ pmax(c(FALSE, TRUE), c(FALSE, NA)) }");

        assertEval(Output.ContainsError, "{ pmax(as.raw(42)) }");
        assertEval(Output.ContainsError, "{ pmax(7+42i) }");
    }

    @Test
    public void testPMin() {
        assertEval("{ pmin(c(1L, 7L), c(42L, 1L)) }");
        assertEval("{ pmin(c(1L, 7L), integer()) }");
        assertEval(Output.ContainsWarning, "{ pmin(c(1L, 7L, 8L), c(1L), c(42L, 1L)) }");
        assertEval("{ pmin(c(1L, 7L), c(42L, as.integer(NA))) }");
        assertEval("{ pmin(c(1L, 7L), c(42L, as.integer(NA)), na.rm=TRUE) }");

        assertEval("{ pmin(c(1, 7), c(42, 1)) }");
        assertEval("{ pmin(c(1, 7), double()) }");
        assertEval(Output.ContainsWarning, "{ pmin(c(1, 7, 8), c(1), c(42, 1)) }");
        assertEval("{ pmin(c(1, 7), c(42, as.double(NA))) }");
        assertEval("{ pmin(c(1, 7), c(42, as.double(NA)), na.rm=TRUE) }");

        assertEval("{ pmin(c(\"1\", \"7\"), c(\"42\", \"1\")) }");
        assertEval("{ pmin(c(\"1\", \"7\"), character()) }");
        assertEval(Output.ContainsWarning, "{ pmin(c(\"1\", \"7\", \"8\"), c(\"1\"), c(\"42\", \"1\")) }");
        assertEval("{ pmin(c(\"1\", \"7\"), c(\"42\", as.character(NA))) }");
        assertEval("{ pmin(c(\"1\", \"7\"), c(\"42\", as.character(NA)), na.rm=TRUE) }");
        assertEval("{ pmin(c(\"1\", as.character(NA)), c(\"42\", \"1\"), na.rm=TRUE) }");
        assertEval("{ pmin(c(\"1\", as.character(NA)), c(as.character(NA), as.character(NA)), c(\"42\", \"1\"), na.rm=TRUE) }");

        assertEval("{ pmin(c(FALSE, TRUE), c(TRUE, FALSE)) }");
        assertEval("{ pmin(c(FALSE, TRUE), logical()) }");
        assertEval("{ pmin(c(FALSE, TRUE), c(FALSE, NA)) }");

        assertEval(Output.ContainsError, "{ pmin(as.raw(42)) }");
        assertEval(Output.ContainsError, "{ pmin(7+42i) }");
    }

    @Test
    public void testMakeNames() {
        assertEval("{ make.names(7) }");
        assertEval("{ make.names(\"a_a\") }");
        assertEval("{ make.names(\"a a\") }");
        assertEval("{ make.names(\"a_a\", allow_=FALSE) }");
        assertEval("{ make.names(\"a_a\", allow_=7) }");
        assertEval("{ make.names(\"a_a\", allow_=c(7,42)) }");
        assertEval("{ make.names(\"...7\") }");
        assertEval("{ make.names(\"..7\") }");
        assertEval("{ make.names(\".7\") }");
        assertEval("{ make.names(\"7\") }");
        assertEval("{ make.names(\"$\") }");
        assertEval("{ make.names(\"$_\", allow_=FALSE) }");
        assertEval("{ make.names(\"else\")}");
        assertEval("{ make.names(\"NA_integer_\", allow_=FALSE) }");

        assertEval(Output.ContainsError, "{ make.names(\"a_a\", allow_=\"a\") }");
        assertEval(Output.ContainsError, "{ make.names(\"a_a\", allow_=logical()) }");
        assertEval(Output.ContainsError, "{ make.names(\"a_a\", allow_=NULL) }");
    }

    @Test
    public void testMakeUnique() {
        assertEval("{ make.unique(\"a\") }");
        assertEval("{ make.unique(character()) }");
        assertEval("{ make.unique(c(\"a\", \"a\")) }");
        assertEval("{ make.unique(c(\"a\", \"a\", \"a\")) }");
        assertEval("{ make.unique(c(\"a\", \"a\"), \"_\") }");
        assertEval(Output.ContainsError, "{ make.unique(1) }");
        assertEval(Output.ContainsError, "{ make.unique(\"a\", 1) }");
        assertEval(Output.ContainsError, "{ make.unique(\"a\", character()) }");
    }

    @Test
    public void testSetAttr() {
        assertEval("{ x <- 1 ; levels(x)<-NULL; levels(notx)}");
        assertEval(Ignored.Unknown, "{ x <- NULL; levels(x)<-\"dog\"; levels(x)}");
    }

    @Test
    public void testSequence() {
        assertEval("{ 5L:10L }");
        assertEval("{ 5L:(0L-5L) }");
        assertEval("{ 1:10 }");
        assertEval("{ 1:(0-10) }");
        assertEval("{ 1L:(0-10) }");
        assertEval("{ 1:(0L-10L) }");
        assertEval("{ (0-12):1.5 }");
        assertEval("{ 1.5:(0-12) }");
        assertEval("{ (0-1.5):(0-12) }");
        assertEval("{ 10:1 }");
        assertEval("{ (0-5):(0-9) }");
        assertEval("{ 1.1:5.1 }");
    }

    @Test
    public void testSequenceStatement() {
        assertEval("{ seq(1L,10L) }");
        assertEval("{ seq(10L,1L) }");
        assertEval("{ seq(1L,4L,2L) }");
        assertEval("{ seq(1,-4,-2) }");
        assertEval("{ seq(0,0,0) }");
        assertEval("{ seq(0,0) }");
        assertEval("{ seq(0L,0L,0L) }");
        assertEval("{ seq(0L,0L) }");
        assertEval("{ seq(0,0,1i) }");
        assertEval(Output.ContainsError, "{ seq(integer(), 7) }");
        assertEval(Output.ContainsError, "{ seq(c(1,2), 7) }");
        assertEval(Output.ContainsError, "{ seq(7, integer()) }");
        assertEval(Output.ContainsError, "{ seq(7, c(41,42)) }");
        assertEval("{ seq(integer()) }");
        assertEval("{ seq(double()) }");
        assertEval("{ seq(from=3L, length.out=3L) }");
        assertEval("{ seq(to=10L, by=1) }");
        assertEval("{ seq(to=10L, by=1.1) }");

        assertEval("{ typeof(seq(1L, 3L)) }");
        assertEval("{ typeof(seq(1, 3)) }");
        assertEval("{ typeof(seq(1L, 3L, by=2)) }");
        assertEval("{ typeof(seq(1L, 3L, by=2L)) }");
        assertEval("{ typeof(seq(1L, 3L, length.out=2)) }");
        assertEval("{ typeof(seq(1L, 3L, length.out=2L)) }");
        assertEval("{ typeof(seq(FALSE, TRUE)) }");
        assertEval("{ typeof(seq(TRUE, FALSE, length.out=5)) }");
        assertEval("{ typeof(seq(TRUE, FALSE, length.out=5L)) }");
        assertEval("{ typeof(seq(1L, 3)) }");
        assertEval("{ typeof(seq(1L, 3, by=2)) }");
        assertEval("{ typeof(seq(1L, 3, by=2L)) }");
        assertEval("{ typeof(seq(1L, 3, length.out=5)) }");
        assertEval("{ typeof(seq(1L, 3, length.out=5L)) }");
        assertEval("{ typeof(seq(1, 3L)) }");
        assertEval("{ typeof(seq(1, 3L, by=2)) }");
        assertEval("{ typeof(seq(1, 3L, by=2L)) }");
        assertEval("{ typeof(seq(1, 3L, length.out=5)) }");
        assertEval("{ typeof(seq(1, 3L, length.out=5L)) }");
        assertEval("{ typeof(seq(to=3L, length.out=2)) }");
        assertEval("{ typeof(seq(to=3L, length.out=2L)) }");
        assertEval("{ typeof(seq(to=3L, by=5)) }");
        assertEval("{ typeof(seq(to=3L, by=5L)) }");
        assertEval("{ typeof(seq(along.with=c(1,2))) }");
        assertEval("{ typeof(seq(1, length.out=0)) }");
        assertEval("{ typeof(seq(1, length.out=0L)) }");
        assertEval("{ typeof(seq(1, along.with=double())) }");
        assertEval("{ typeof(seq(1L, along.with=double())) }");

        // seq does not work properly (added tests for vector accesses that paste correct seq's
        // result in TestSimpleVectors)
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(c(1,3,10), seq(2L,4L,2L),c(TRUE,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.double(1:5), seq(7L,1L,-3L),c(TRUE,FALSE,NA)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.logical(-3:3),seq(1L,7L,3L),c(TRUE,NA,FALSE)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(as.character(-3:3),seq(1L,7L,3L),c(\"A\",\"a\",\"XX\")) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; f(1:8, seq(1L,7L,3L), c(10,100,1000)) }");
        assertEval("{ f <- function(b, i, v) { b[i] <- v ; b } ; f(1:2,1:2,3:4); f(1:2,1:2,c(3,4)) ; z <- f(1:8, seq(1L,7L,3L), list(10,100,1000)) ; sum(as.double(z)) }");
    }

    @Test
    public void testSequenceStatementNamedParams() {
        assertEval("{ seq(from=1,to=3) }");
        assertEval("{ seq(length.out=1) }");
        assertEval("{ seq(from=1.4) }");
        assertEval("{ seq(from=1.7) }");
        assertEval("{ seq(from=1,to=3,by=1) }");
        assertEval("{ seq(from=-10,to=-5,by=2) }");

        assertEval("{ seq(length.out=0) }");

        assertEval("{ seq(to=-1,from=-10) }");
        assertEval("{ seq(length.out=13.4) }");
        assertEval("{ seq(along.with=10) }");
        assertEval("{ seq(along.with=NA) }");
        assertEval("{ seq(along.with=1:10) }");
        assertEval("{ seq(along.with=-3:-5) }");
        assertEval("{ seq(from=10:12) }");
        assertEval("{ seq(from=c(TRUE, FALSE)) }");
        assertEval("{ seq(from=TRUE, to=TRUE, length.out=0) }");
        assertEval("{ round(seq(from=10.5, to=15.4, length.out=4), digits=5) }");
        assertEval("{ seq(from=11, to=12, length.out=2) }");
        assertEval("{ seq(from=-10.4,to=-5.8,by=2.1) }");
        assertEval("{ round(seq(from=3L,to=-2L,by=-4.2), digits=5) }");
        assertEval("{ seq(along=c(10,11,12)) }"); // test partial name match
    }

    @Test
    public void testSeqLen() {
        assertEval("{ seq_len(10) }");
        assertEval("{ seq_len(5L) }");
        assertEval("{ seq_len(1:2) }");
        assertEval("{ seq_len(integer()) }");
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
    public void testVectorConstructor() {
        assertEval("{ vector() }");
        assertEval("{ vector(\"integer\") }");
        assertEval("{ vector(\"numeric\") }");
        assertEval("{ vector(\"numeric\", length=4) }");
        assertEval("{ vector(length=3) }");
        assertEval("{ x<-as.vector(3); y<-vector(length=x) }");
    }

    @Test
    public void testMaximum() {
        assertEval("{ max((-1):100) }");
        assertEval("{ max(2L, 4L) }");
        assertEval(Output.ContainsWarning, "{ max() }");
        assertEval("{ max(1:10, 100:200, c(4.0, 5.0)) }");
        assertEval("{ max(NA, 1.1) }");
        assertEval("{ max(0/0, 1.1) }");
        assertEval("{ max(0/0, 1.1, NA) }");
        assertEval("{ max(c(as.character(NA), \"foo\")) }");
        assertEval(Output.ContainsWarning, "{ max(character(0)) }");
        assertEval(Output.ContainsWarning, "{ max(character()) }");
        assertEval(Output.ContainsWarning, "{ max(double(0)) }");
        assertEval(Output.ContainsWarning, "{ max(double()) }");
        assertEval(Output.ContainsWarning, "{ max(NULL) }");

        assertEval("{ max(1:10, 100:200, c(4.0, 5.0), c(TRUE,FALSE,NA)) }");
        assertEval("{ max(c(\"hi\",\"abbey\",\"hello\")) }");
        assertEval("{ max(\"hi\",\"abbey\",\"hello\") }");

        assertEval("{ is.logical(max(TRUE, FALSE)) }");
        assertEval("{ is.logical(max(TRUE)) }");
        assertEval(Output.ContainsError, "{ max(as.raw(42), as.raw(7)) }");
        assertEval(Output.ContainsError, "{ max(42+42i, 7+7i) }");
        assertEval("{ max(\"42\", \"7\") }");

        assertEval("{ max(as.double(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsWarning, "{ max(as.double(NA), as.double(NA), na.rm=TRUE) }");
        assertEval("{ max(as.double(NA), as.double(NA), na.rm=FALSE) }");
        assertEval("{ max(as.integer(NA), na.rm=FALSE) }");
        assertEval("{ max(as.integer(NA), as.integer(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsWarning, "{ max(as.character(NA), na.rm=TRUE) }");
        assertEval("{ max(as.character(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsWarning, "{ max(as.character(NA), as.character(NA), na.rm=TRUE) }");
        assertEval("{ max(as.character(NA), as.character(NA), na.rm=FALSE) }");
        assertEval("{ max(42L, as.integer(NA), na.rm=TRUE) }");
        assertEval("{ max(42L, as.integer(NA), na.rm=FALSE) }");
        assertEval("{ max(42, as.double(NA), na.rm=TRUE) }");
        assertEval("{ max(42, as.double(NA), na.rm=FALSE) }");
        assertEval("{ max(\"42\", as.character(NA), na.rm=TRUE) }");
        assertEval("{ max(\"42\", as.character(NA), na.rm=FALSE) }");
        assertEval("{ max(42L, as.integer(NA), 7L, na.rm=TRUE) }");
        assertEval("{ max(42L, as.integer(NA), 7L, na.rm=FALSE) }");
        assertEval("{ max(42, as.double(NA), 7, na.rm=TRUE) }");
        assertEval("{ max(42, as.double(NA), 7, na.rm=FALSE) }");
        assertEval("{ max(\"42\", as.character(NA), \"7\", na.rm=TRUE) }");
        assertEval("{ max(\"42\", as.character(NA), \"7\", na.rm=FALSE) }");

        assertEval("{ max(as.character(NA), as.character(NA), \"42\", na.rm=TRUE) }");
        assertEval("{ max(as.character(NA), as.character(NA), \"42\", \"7\", na.rm=TRUE) }");

        assertEval("{ max(123, NA, TRUE, 12, FALSE, na.rm=TRUE) }");
        assertEval("{ max(123, NA, TRUE, 12, FALSE, na.rm=FALSE) }");
        assertEval("{ max(123, NA, TRUE, 12, FALSE) }");

        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(integer(0)) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(integer()) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(as.double(NA), na.rm=TRUE) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(as.integer(NA), na.rm=TRUE) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ max(as.integer(NA), as.integer(NA), na.rm=TRUE) }");
    }

    @Test
    public void testMinimum() {
        assertEval("{ min((-1):100) }");
        assertEval("{ min(2L, 4L) }");
        assertEval(Output.ContainsWarning, "{ min() }");
        assertEval("{ min(c(1,2,0/0)) }");
        assertEval("{ max(c(1,2,0/0)) }");
        assertEval("{ min(1:10, 100:200, c(4.0, -5.0)) }");
        assertEval("{ min(NA, 1.1) }");
        assertEval("{ min(0/0, 1.1) }");
        assertEval("{ min(0/0, 1.1, NA) }");
        assertEval("{ min(c(as.character(NA), \"foo\")) }");
        assertEval(Output.ContainsWarning, "{ min(character(0)) }");
        assertEval(Output.ContainsWarning, "{ min(character()) }");
        assertEval(Output.ContainsWarning, "{ min(double(0)) }");
        assertEval(Output.ContainsWarning, "{ min(double()) }");
        assertEval(Output.ContainsWarning, "{ min(NULL) }");

        assertEval("{ min(1:10, 100:200, c(4.0, 5.0), c(TRUE,FALSE,NA)) }");
        assertEval("{ min(c(\"hi\",\"abbey\",\"hello\")) }");
        assertEval("{ min(\"hi\",\"abbey\",\"hello\") }");
        assertEval("{ min(\"hi\",100) }");

        assertEval("{ is.logical(min(TRUE, FALSE)) }");
        assertEval("{ is.logical(min(TRUE)) }");
        assertEval(Output.ContainsError, "{ min(as.raw(42), as.raw(7)) }");
        assertEval(Output.ContainsError, "{ min(42+42i, 7+7i) }");
        assertEval("{ min(\"42\", \"7\") }");

        assertEval("{ min(as.double(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsWarning, "{ min(as.double(NA), as.double(NA), na.rm=TRUE) }");
        assertEval("{ min(as.double(NA), as.double(NA), na.rm=FALSE) }");
        assertEval("{ min(as.integer(NA), na.rm=FALSE) }");
        assertEval("{ min(as.integer(NA), as.integer(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsWarning, "{ min(as.character(NA), na.rm=TRUE) }");
        assertEval("{ min(as.character(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsWarning, "{ min(as.character(NA), as.character(NA), na.rm=TRUE) }");
        assertEval("{ min(as.character(NA), as.character(NA), na.rm=FALSE) }");
        assertEval("{ min(42L, as.integer(NA), na.rm=TRUE) }");
        assertEval("{ min(42L, as.integer(NA), na.rm=FALSE) }");
        assertEval("{ min(42, as.double(NA), na.rm=TRUE) }");
        assertEval("{ min(42, as.double(NA), na.rm=FALSE) }");
        assertEval("{ min(\"42\", as.character(NA), na.rm=TRUE) }");
        assertEval("{ min(\"42\", as.character(NA), na.rm=FALSE) }");
        assertEval("{ min(42L, as.integer(NA), 7L, na.rm=TRUE) }");
        assertEval("{ min(42L, as.integer(NA), 7L, na.rm=FALSE) }");
        assertEval("{ min(42, as.double(NA), 7, na.rm=TRUE) }");
        assertEval("{ min(42, as.double(NA), 7, na.rm=FALSE) }");
        assertEval("{ min(\"42\", as.character(NA), \"7\", na.rm=TRUE) }");
        assertEval("{ min(\"42\", as.character(NA), \"7\", na.rm=FALSE) }");

        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ min(integer(0)) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ min(integer()) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ min(as.double(NA), na.rm=TRUE) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ min(as.integer(NA), na.rm=TRUE) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ min(as.integer(NA), as.integer(NA), na.rm=TRUE) }");
    }

    @Test
    public void testRep() {
        assertEval("{ rep(1,3) }");
        assertEval("{ rep(1:3,2) }");
        assertEval("{ rep(c(1,2),0) }");
        assertEval("{ rep(as.raw(14), 4) }");
        assertEval("{ rep(1:3, length.out=4) }");
        assertEval("{ rep(\"hello\", 3) }");
        assertEval("{ rep(c(1,2),c(3,3)) }");
        assertEval("{ rep(NA,8) }");
        assertEval("{ rep(TRUE,8) }");
        assertEval("{ rep(1:3, length.out=NA) }");

        assertEval("{ x <- as.raw(11) ; names(x) <- c(\"X\") ; rep(x, 3) }");
        assertEval("{ x <- as.raw(c(11,12)) ; names(x) <- c(\"X\",\"Y\") ; rep(x, 2) }");
        assertEval("{ x <- c(TRUE,NA) ; names(x) <- c(\"X\",NA) ; rep(x, length.out=3) }");
        assertEval("{ x <- 1L ; names(x) <- c(\"X\") ; rep(x, times=2) } ");
        assertEval("{ x <- 1 ; names(x) <- c(\"X\") ; rep(x, times=0) }");
        assertEval("{ x <- 1+1i ; names(x) <- c(\"X\") ; rep(x, times=2) }");
        assertEval("{ x <- c(1+1i,1+2i) ; names(x) <- c(\"X\") ; rep(x, times=2) }");
        assertEval("{ x <- c(\"A\",\"B\") ; names(x) <- c(\"X\") ; rep(x, length.out=3) }");

        assertEval("{ x<-c(1,2); names(x)<-c(\"X\", \"Y\"); rep(x, c(3,2)) }");

        assertEval("{ rep(c(1, 2), each = 2) }");
        assertEval("{ rep(c(1, 2), each = 2, length.out = 5) }");
        assertEval("{ rep(c(1, 2), each = 2, length.out = 3) }");
        assertEval("{ rep(c(1, 2), times = 3) }");
        assertEval("{ rep(c(1, 2), times = c(2, 3)) }");
        assertEval("{ rep(c(1, 2), times = c(1, 2, 3)) }");
        assertEval("{ rep(c(1, 2), times = c(2, 3), each = 2) }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); rep(x, times=3) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); rep(x, length=5) }");
    }

    @Test
    public void testRepInt() {
        assertEval("{ rep.int(1,3) }");
        assertEval("{ rep.int(1:3,2) }");
        assertEval("{ rep.int(c(1,2),0) }");
        assertEval("{ rep.int(c(1,2),2) }");
        assertEval("{ rep.int(as.raw(14), 4) }");
        assertEval("{ rep.int(1L,3L) }");
        assertEval("{ rep.int(\"a\",3) }");
        assertEval("{ rep.int(c(1,2,3),c(2,8,3)) }");
        assertEval("{ rep.int(seq_len(2), rep.int(8, 2)) }");

        assertEval(Output.ContainsError, "{ rep.int(c(1,2,3),c(2,8)) }");
    }

    @Test
    public void testRepLen() {
        assertEval("{ rep_len(1, 2) }");
        assertEval("{ rep_len(3.14159, 3) }");
        assertEval("{ rep_len(\"RepeatTest\", 5) }");
        assertEval("{ rep_len(2+6i, 4) }");
        assertEval("{ rep_len(TRUE, 2) }");
        assertEval("{ x<-as.raw(16); rep_len(x, 2) }");

        assertEval("{ rep_len(1:4, 10) }");
        assertEval("{ rep_len(1:4, 3) }");
        assertEval("{ rep_len(1:4, 4) }");
        assertEval("{ rep_len(c(3.1415, 0.8), 1) }");
        assertEval("{ rep_len(c(2i+3, 4+2i), 4) }");
        assertEval("{ x<-as.raw(16); y<-as.raw(5); rep_len(c(x, y), 5) }");
        // cases with named arguments:
        assertEval("{rep_len(x=1:2, length.out=4)}");
        assertEval("{rep_len(length.out=4, x=1:2)}");
        assertEval("{rep_len(length.out=4, \"text\")}");
        assertEval("{rep_len(4, x=\"text\")}");
        assertEval("{x<-\"text\"; length.out<-4; rep_len(x=x, length.out=length.out)}");
        assertEval("{x<-\"text\"; length.out<-4; rep_len(length.out=length.out, x=x)}");
        // test string vector argument
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 7)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 14)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 8)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 0)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 1)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 2)}");
    }

    @Test
    public void testCombine() {
        assertEval("{ c(\"1.2\",\"3.4\") }");
        assertEval("{ c(\"a\",\"b\",\"c\") }");
        assertEval("{ c(\"1\",\"b\") }");
        assertEval("{ c(\"1.00\",\"2.00\") }");
        assertEval("{ c(\"1.00\",\"b\") }");

        assertEval("{ c(1.0,1L) }");
        assertEval("{ c(1L,1.0) }");
        assertEval("{ c( 1:3 ) }");
        assertEval("{ c( 1L:3L ) }");
        assertEval("{ c( 100, 1:3, 200 ) }");
        assertEval("{ c( 1:3, 7:9 ) }");
        assertEval("{ c( 1:3, 5, 7:9 ) }");

        assertEval("{ c() }");
        assertEval("{ c(NULL) }");
        assertEval("{ c(NULL,NULL) }");
        assertEval("{ c(NULL,1,2,3) }");

        assertEval("{ c(1+1i,2-3i,4+5i) }");

        assertEval("{ c(\"hello\", \"hi\") }");

        assertEval("{ c(1+1i, as.raw(10)) }");
        assertEval("{ c(as.raw(10), as.raw(20)) }");
        assertEval("{ c(as.raw(10),  \"test\") }");

        assertEval("{ f <- function(x,y) { c(x,y) } ; f(1,1) ; f(1, TRUE) }");
        assertEval("{ f <- function(x,y) { c(x,y) } ; f(1,1) ; f(1, TRUE) ; f(NULL, NULL) }");

        testCombine(new String[]{"1", "2", "3"});
        testCombine(new String[]{"1L", "2L", "3L"});
        testCombine(new String[]{"TRUE", "FALSE", "FALSE"});
        testCombine(new String[]{"\"a\"", "\"b\"", "\"c\""});
        testCombine(new String[]{"\"d\"", "2L", "\"f\""});
        testCombine(new String[]{"\"g\"", "2", "2"});
        testCombine(new String[]{"\"j\"", "TRUE", "TRUE"});

        // test propagation of the "names" attribute
        assertEval("{ x<-1:2; names(x)<-7:8; y<-3:4; names(y)<-9:10; z<-c(x, y); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; y<-3:4; z<-c(x, y); z }");
        assertEval("{ x<-1:2; names(x)<-7:8;  z<-c(x, integer()); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(x, 3L); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(3L, x); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; y<-double(0);  z<-c(x, y); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(x, 3); z }");
        assertEval("{ x<-1:2; names(x)<-7:8; z<-c(x, 3); attributes(z) }");

        assertEval("{ c(a=42) }");
        assertEval("{ c(a=FALSE) }");
        assertEval("{ c(a=as.raw(7)) }");
        assertEval("{ c(a=\"foo\") }");
        assertEval("{ c(a=7i) }");

        assertEval("{ c(a=1, b=2) }");
        assertEval("{ c(a=FALSE, b=TRUE) }");
        assertEval("{ c(a=as.raw(1), b=as.raw(2)) }");
        assertEval("{ c(a=\"bar\", b=\"baz\") }");
        assertEval("{ c(a=1, 2) }");
        assertEval("{ c(1, b=2) }");

        assertEval("{ c(a=1i, b=2i) }");
        assertEval("{ c(a=7i, a=1:2) }");
        assertEval("{ c(a=1:2, 42) }");
        assertEval("{ c(a=1:2, b=c(42)) }");
        assertEval("{ c(a=1:2, b=double()) }");
        assertEval("{ c(a=c(z=1), 42) }");
        assertEval("{ x<-c(z=1); names(x)=c(\"\"); c(a=x, 42) }");
        assertEval("{ x<-c(y=1, z=2); names(x)=c(\"\", \"\"); c(a=x, 42) }");
        assertEval("{ x<-c(y=1, z=2);  c(a=x, 42) }");
        assertEval("{ x<-c(y=1);  c(x, 42) }");
        assertEval("{ x<-c(1);  c(z=x, 42) }");
        assertEval("{ x<-c(y=1, 2);  c(a=x, 42) }");

        assertEval("{ c(TRUE,1L,1.0,list(3,4)) }");
        assertEval("{ c(TRUE,1L,1.0,list(3,list(4,5))) }");

        assertEval("{ c(x=1,y=2) }");
        assertEval("{ c(x=1,2) }");
        assertEval("{ x <- 1:2 ; names(x) <- c(\"A\",NA) ; c(x,test=x) }");
        assertEval("{ c(a=1,b=2:3,list(x=FALSE))  }");
        assertEval("{ c(1,z=list(1,b=22,3)) }");

        assertEval("{ is.matrix(c(matrix(1:4,2))) }");

        assertEval("{ x<-expression(1); c(x) }");
        assertEval("{ x<-expression(1); c(x,2) }");

        // print output for a function in a list doesn't match GnuR,
        // which seems to invoke deparse, so we just check the c didn't fail.
        assertEval("{ f <- function() { }; length(c(f, 2)) == 2 }");
    }

    @Test
    public void testList() {
        assertEval("{ list(a=1, b=2) }");
        assertEval("{ list(a=1, 2) }");
        assertEval("{ list(1, b=2) }");
        assertEval("{ x<-c(y=1, 2);  list(a=x, 42) }");
        assertEval("{ x<-list(y=1, 2);  c(a=x, 42) }");
        assertEval("{ x<-list(y=1, 2);  c(42, a=x) }");
        assertEval("{ x<-list(y=1, 2);  c(a=x, c(z=7,42)) }");
        assertEval("{ x<-list(y=1, 2);  c(a=x, c(y=7,z=42)) }");
    }

    public void testCombine(String[] testValues) {
        genTest("{ c(%s) }", 1, testValues);
        genTest("{ c(%s, %s) }", 2, testValues);
        genTest("{ c(c(%s,%s), %s) }", 3, testValues);
        genTest("{ c(%s, c(%s,%s)) }", 3, testValues);
        genTest("{ c(NULL, c(%s,%s)) }", 2, testValues);
        genTest("{ c(c(%s,%s), NULL) }", 2, testValues);
        genTest("{ c(NULL, %s, NULL) }", 1, testValues);
        genTest("{ c(NULL, %s) }", 1, testValues);
        genTest("{ c(%s, NULL) }", 1, testValues);

        genTest("{ c(c(%s,%s), c(%s,%s)) }", 4, testValues);
        genTest("{ c(c(%s,%s), %s, c(%s,%s)) }", 5, testValues);
        genTest("{ c(c(%s,%s), c(%s,%s), c(%s,%s)) }", 6, testValues);
    }

    private void genTest(String test, int size, String[] testValues) {
        String genTest = String.format(test, (Object[]) repeat(size, testValues));
        assertEval(genTest);
    }

    private static String[] repeat(int size, String[] array) {
        if (size == array.length) {
            return array;
        } else if (size <= array.length) {
            return Arrays.copyOf(array, size);
        } else {
            String[] result = new String[size];
            for (int i = 0; i < size; i++) {
                result[i] = array[i % array.length];
            }
            return result;
        }
    }

    @Test
    public void testCombineBroken() {
        assertEval(Ignored.Unknown, "{ c(1i,0/0) }"); // yes, this is done by GNU-R, note
        // inconsistency with as.complex(0/0)
    }

    @Test
    public void testIsNAAssign() {
        assertEval("{ x <- c(0:4); is.na(x) <- c(2, 4); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); is.na(x)<-1; x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); is.na(x)<-2; x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); is.na(x)<-c(1, 3); x }");
    }

    @Test
    public void testIsNA() {
        assertEval("{ is.na(NA) }");
        assertEval("{ is.na(NaN) }");
        assertEval("{ is.na(c(NA)) }");
        assertEval("{ is.na(c(1,2,3,4)) }");
        assertEval("{ is.na(c(1,2,NA,4)) }");
        assertEval("{ is.na(1[10]) }");
        assertEval("{ is.na(c(1[10],2[10],3)) }");
    }

    @Test
    public void testIsNABroken() {
        assertEval("{ is.na(list(1[10],1L[10],list(),integer())) }");
    }

    @Test
    public void testAsInteger() {
        assertEval("{ as.integer(\"1\") }");
        assertEval("{ as.integer(c(\"1\",\"2\")) }");
        assertEval("{ as.integer(c(1,2,3)) }");
        assertEval("{ as.integer(c(1.0,2.5,3.9)) }");
        assertEval("{ as.integer(0/0) }");
        assertEval("{ as.integer(-0/0) }");
        assertEval("{ as.integer(as.raw(c(1,2,3,4))) }");
        assertEval(Output.ContainsWarning, "{ as.integer(10+2i) }");
        assertEval(Output.ContainsWarning, "{ as.integer(c(3+3i, 4+4i)) }");
        assertEval(Output.ContainsWarning, "{ as.integer(10000000000000) }");
        assertEval("{ as.integer(list(c(1),2,3)) }");
        assertEval("{ as.integer(list(integer(),2,3)) }");
        assertEval("{ as.integer(list(list(1),2,3)) }");
        assertEval("{ as.integer(list(1,2,3,list())) }");
        assertEval(Output.ContainsWarning, "{ as.integer(10000000000) }");
        assertEval(Output.ContainsWarning, "{ as.integer(-10000000000) }");
        assertEval(Output.ContainsWarning, "{ as.integer(c(\"1\",\"hello\")) }");
        assertEval(Output.ContainsWarning, "{ as.integer(\"TRUE\") }");
        assertEval("{ as.integer(as.raw(1)) }");
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.integer(x); attributes(y) }");
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.integer(x); attributes(y) }");
        assertEval("{ as.integer(1.1:5.1) }");
        assertEval("{ as.integer(NULL) }");
        assertEval("{ as.integer(\"\") }");
        assertEval("{ as.integer(as.character(NA)) }");
        assertEval("{ as.integer(\"1\", as.character(NA)) }");
    }

    @Test
    public void testAsCharacter() {
        assertEval("{ as.character(1) }");
        assertEval("{ as.character(1L) }");
        assertEval("{ as.character(TRUE) }");
        assertEval("{ as.character(1:3) }");
        assertEval("{ as.character(NULL) }");

        assertEval(Ignored.Unknown, "{ as.character(list(1,2,3)) }");
        assertEval(Ignored.Unknown, "{ as.character(list(c(\"hello\", \"hi\"))) }");
        assertEval(Ignored.Unknown, "{ as.character(list(list(c(\"hello\", \"hi\")))) }");
        assertEval(Ignored.Unknown, "{ as.character(list(c(2L, 3L))) }");
        assertEval(Ignored.Unknown, "{ as.character(list(c(2L, 3L, 5L))) }");

        assertEval("{ x<-as.character(Sys.time()) }");
        assertEval("{ f<-function(x) { sys.call() }; as.character(f(7)) }");
    }

    @Test
    public void testAsDouble() {
        assertEval("{ as.double(\"1.27\") }");
        assertEval("{ as.double(1L) }");
        assertEval("{ as.double(as.raw(1)) }");
        assertEval(Output.ContainsWarning, "{ as.double(c(\"1\",\"hello\")) }");
        assertEval(Output.ContainsWarning, "{ as.double(\"TRUE\") }");
        assertEval(Output.ContainsWarning, "{ as.double(10+2i) }");
        assertEval(Output.ContainsWarning, "{ as.double(c(3+3i, 4+4i)) }");
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.double(x); attributes(y) }");
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.double(x); attributes(y) }");
        assertEval("{ as.double(NULL) }");
    }

    @Test
    public void testAsLogical() {
        assertEval("{ as.logical(1) }");
        assertEval("{ as.logical(\"false\") }");
        assertEval("{ as.logical(\"dummy\") }"); // no warning produced (as it should be)
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.logical(x); attributes(y) }");
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.logical(x); attributes(y) }");
        assertEval("{ as.logical(c(\"1\",\"hello\")) }");
        assertEval("{ as.logical(\"TRUE\") }");
        assertEval("{ as.logical(10+2i) }");
        assertEval("{ as.logical(c(3+3i, 4+4i)) }");
        assertEval("{ as.logical(NULL) }");
    }

    @Test
    public void testAsComplex() {
        assertEval("{ as.complex(0) }");
        assertEval("{ as.complex(TRUE) }");
        assertEval("{ as.complex(\"1+5i\") }");
        assertEval("{ as.complex(\"-1+5i\") }");
        assertEval("{ as.complex(\"-1-5i\") }");
        assertEval("{ as.complex(0/0) }");
        assertEval("{ as.complex(c(0/0, 0/0)) }");
        assertEval(Output.ContainsWarning, "{ as.complex(c(\"1\",\"hello\")) }");
        assertEval(Output.ContainsWarning, "{ as.complex(\"TRUE\") }");
        assertEval("{ x<-c(a=1.1, b=2.2); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.complex(x); attributes(y) }");
        assertEval("{ x<-c(a=1L, b=2L); dim(x)<-c(1,2); attr(x, \"foo\")<-\"foo\"; y<-as.complex(x); attributes(y) }");
        assertEval("{ as.complex(\"Inf\") }");
        assertEval("{ as.complex(\"NaN\") }");
        assertEval("{ as.complex(\"0x42\") }");
        assertEval("{ as.complex(NULL) }");

        assertEval(Ignored.Unknown, "{ as.complex(\"1e10+5i\") }");
        assertEval(Ignored.Unknown, "{ as.complex(\"-.1e10+5i\") }");
        assertEval(Ignored.Unknown, "{ as.complex(\"1e-2+3i\") }");
        assertEval(Ignored.Unknown, "{ as.complex(\"+.1e+2-3i\") }");
    }

    @Test
    public void testAsRaw() {
        assertEval("{ as.raw(NULL) }");
        assertEval("{ as.raw(1) }");
        assertEval("{ as.raw(1L) }");
        assertEval("{ as.raw(1.1) }");
        assertEval("{ as.raw(c(1, 2, 3)) }");
        assertEval("{ as.raw(c(1L, 2L, 3L)) }");
        assertEval("{ as.raw(list(1,2,3)) }");
        assertEval("{ as.raw(list(\"1\", 2L, 3.4)) }");

        assertEval(Output.ContainsWarning, "{ as.raw(1+1i) }");
        assertEval(Output.ContainsWarning, "{ as.raw(-1) }");
        assertEval(Output.ContainsWarning, "{ as.raw(-1L) }");
        assertEval(Output.ContainsWarning, "{ as.raw(NA) }");
        assertEval(Output.ContainsWarning, "{ as.raw(\"test\") }");
        assertEval(Output.ContainsWarning, "{ as.raw(c(1+3i, -2-1i, NA)) }");
        assertEval(Output.ContainsWarning, "{ as.raw(c(1, -2, 3)) }");
        assertEval(Output.ContainsWarning, "{ as.raw(c(1,1000,NA)) }");
        assertEval(Output.ContainsWarning, "{ as.raw(c(1L, -2L, 3L)) }");
        assertEval(Output.ContainsWarning, "{ as.raw(c(1L, -2L, NA)) }");
    }

    @Test
    public void testAsVector() {
        assertEval(Output.ContainsWarning, "{ as.vector(\"foo\", \"integer\") }");
        assertEval(Output.ContainsWarning, "{ as.vector(\"foo\", \"double\") }");
        assertEval(Output.ContainsWarning, "{ as.vector(\"foo\", \"numeric\") }");
        assertEval("{ as.vector(\"foo\", \"logical\") }");
        assertEval(Output.ContainsWarning, "{ as.vector(\"foo\", \"raw\") }");
        assertEval("{ as.vector(\"foo\", \"character\") }");
        assertEval("{ as.vector(\"foo\", \"list\") }");
        assertEval("{ as.vector(\"foo\") }");
        assertEval("{ as.vector(\"foo\", \"bar\") }");
        assertEval(Output.ContainsWarning, "{ as.vector(c(\"foo\", \"bar\"), \"raw\") }");
        assertEval("x<-c(a=1.1, b=2.2); as.vector(x, \"raw\")");
        assertEval("x<-c(a=1L, b=2L); as.vector(x, \"complex\")");
        assertEval("{ x<-c(a=FALSE, b=TRUE); attr(x, \"foo\")<-\"foo\"; y<-as.vector(x); attributes(y) }");
        assertEval("{ x<-c(a=1, b=2); as.vector(x, \"list\") }");
        assertEval("{ x<-c(a=FALSE, b=TRUE); attr(x, \"foo\")<-\"foo\"; y<-as.vector(x, \"list\"); attributes(y) }");
        assertEval("{ x<-1:4; dim(x)<-c(2, 2); dimnames(x)<-list(c(\"a\", \"b\"), c(\"c\", \"d\")); y<-as.vector(x, \"list\"); y }");

        assertEval("{ as.vector(NULL, \"list\") }");
        assertEval("{ as.vector(NULL) }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); as.vector(x) }");
    }

    @Test
    public void testAsSymbol() {
        assertEval("{ as.symbol(\"name\") }");
        assertEval("{ as.symbol(123) }");
        assertEval("{ as.symbol(as.symbol(123)) }");
    }

    @Test
    public void testAsExpression() {
        assertEval("{ as.expression(\"name\") }");
        assertEval("{ as.expression(NULL) }");
        assertEval("{ as.expression(123) }");
        assertEval("{ as.expression(as.symbol(123)) }");
        assertEval("{ as.expression(c(1,2)) }");
        assertEval("{ as.expression(list(1,2)) }");
        assertEval("{ as.expression(list(\"x\" = 1, \"y\" = 2)) }");
        assertEval(Output.ContainsError, "{ as.expression(sum) }");
        assertEval(Output.ContainsError, "{ as.expression(function()) }");
    }

    @Test
    public void testMatrix() {
        assertEval("{ matrix(c(1,2,3,4),2,2) }");
        assertEval("{ matrix(as.double(NA),2,2) }");
        assertEval("{ matrix(\"a\",10,10) }");
        assertEval("{ matrix(c(\"a\",NA),10,10) }");
        assertEval("{ matrix(1:4, nrow=2) }");
        assertEval("{ matrix(c(1,2,3,4), nrow=2) }");
        assertEval("{ matrix(c(1+1i,2+2i,3+3i,4+4i),2) }");
        assertEval("{ matrix(nrow=2,ncol=2) }");
        assertEval("{ matrix(1:4,2,2) }");
        assertEval("{ matrix(1i,10,10) }");
        assertEval("{ matrix(c(1i,NA),10,10) }");
        assertEval("{ matrix(c(10+10i,5+5i,6+6i,20-20i),2) }");
        assertEval("{ matrix(c(1i,100i),10,10) }");
        assertEval("{ matrix(1:6, nrow=3,byrow=TRUE)}");
        assertEval("{ matrix(1:6, nrow=3,byrow=1)}");
        assertEval("{ matrix(1:6, nrow=c(3,4,5),byrow=TRUE)}");
        assertEval("{ matrix(1:6)}");
        assertEval("{ matrix(1:6, ncol=3:5,byrow=TRUE)}");

        assertEval("{ matrix(TRUE,FALSE,FALSE,TRUE)}");

        assertEval(Ignored.Unknown, "{ matrix(c(NaN,4+5i,2+0i,5+10i)} ");
        // FIXME missing warning
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ matrix(c(1,2,3,4),3,2) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ matrix(1:4,3,2) }");

        assertEval("{ x<-matrix(integer(), ncol=2) }");
        assertEval("{ x<-matrix(integer(), nrow=2) }");
        assertEval("{ x<-matrix(integer(), ncol=2, nrow=3) }");
        assertEval("{ x<-matrix(integer()) }");

    }

    @Test
    public void testCasts() {
        // shortcuts in views (only some combinations)
        assertEval("{ as.complex(as.character(c(1+1i,1+1i))) }");
        assertEval("{ as.complex(as.integer(c(1+1i,1+1i))) }");
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

        assertEval("{ as.complex(as.double(c(1+1i,1+1i))) }"); // FIXME missing warning
        assertEval("{ as.complex(as.raw(c(1+1i,1+1i))) }"); // FIXME missing warning
    }

    @Test
    public void testDrop() {
        assertEval("{ x <- array(1:12, dim = c(1,3,1,1,2,1,2)); drop(x) }");
    }

    @Test
    public void testSum() {
        assertEval("{ sum() }");
        assertEval("{ sum(0, 1, 2, 3) }");
        assertEval("{ sum(c(0, 1, 2, 3)) }");
        assertEval("{ sum(c(0, 1, 2, 3), 4) }");
        assertEval("{ sum(1:6, 3, 4) }");
        assertEval("{ sum(1:6, 3L, TRUE) }");
        assertEval("{ `sum`(1:10) }");
        assertEval("{ x<-c(FALSE, FALSE); is.double(sum(x)) }");
        assertEval("{ x<-c(FALSE, FALSE); is.integer(sum(x)) }");

        assertEval("{ is.logical(sum(TRUE, FALSE)) }");
        assertEval("{ is.logical(sum(TRUE)) }");
        assertEval(Output.ContainsError, "{ sum(as.raw(42), as.raw(7)) }");
        assertEval("{ sum(42+42i, 7+7i) }");
        assertEval(Output.ContainsError, "{ sum(\"42\", \"7\") }");

        assertEval("{ sum(as.double(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.double(NA), na.rm=FALSE) }");
        assertEval("{ sum(as.double(NA), as.double(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.double(NA), as.double(NA), na.rm=FALSE) }");
        assertEval("{ sum(as.integer(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.integer(NA), na.rm=FALSE) }");
        assertEval("{ sum(as.integer(NA), as.integer(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.integer(NA), as.integer(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsError, "{ sum(as.character(NA), na.rm=TRUE) }");
        assertEval(Output.ContainsError, "{ sum(as.character(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsError, "{ sum(as.character(NA), as.character(NA), na.rm=TRUE) }");
        assertEval(Output.ContainsError, "{ sum(as.character(NA), as.character(NA), na.rm=FALSE) }");
        assertEval("{ sum(42L, as.integer(NA), na.rm=TRUE) }");
        assertEval("{ sum(42L, as.integer(NA), na.rm=FALSE) }");
        assertEval("{ sum(42, as.double(NA), na.rm=TRUE) }");
        assertEval("{ sum(42, as.double(NA), na.rm=FALSE) }");
        assertEval(Output.ContainsError, "{ sum(\"42\", as.character(NA), na.rm=TRUE) }");
        assertEval(Output.ContainsError, "{ sum(\"42\", as.character(NA), na.rm=FALSE) }");
        assertEval("{ sum(42L, as.integer(NA), 7L, na.rm=TRUE) }");
        assertEval("{ sum(42L, as.integer(NA), 7L, na.rm=FALSE) }");
        assertEval("{ sum(42, as.double(NA), 7, na.rm=TRUE) }");
        assertEval("{ sum(42, as.double(NA), 7, na.rm=FALSE) }");
        assertEval(Output.ContainsError, "{ sum(\"42\", as.character(NA), \"7\", na.rm=TRUE) }");
        assertEval(Output.ContainsError, "{ sum(\"42\", as.character(NA), \"7\", na.rm=FALSE) }");

        assertEval("{ sum(0, 1[3]) }");
        assertEval("{ sum(na.rm=FALSE, 0, 1[3]) }");
        assertEval("{ sum(0, na.rm=FALSE, 1[3]) }");
        assertEval("{ sum(0, 1[3], na.rm=FALSE) }");
        assertEval("{ sum(0, 1[3], na.rm=TRUE) }");
        assertEval("{ sum(1+1i,2,NA, na.rm=TRUE) }");
    }

    @Test
    public void testMatchFun() {
        assertEval("{ f <- match.fun(length) ; f(c(1,2,3)) }");
        assertEval("{ f <- match.fun(\"length\") ; f(c(1,2,3)) }");
        assertEval("{ f <- function(x) { y <- match.fun(x) ; y(c(1,2,3)) } ; c(f(\"sum\"),f(\"cumsum\")) }");
        assertEval("{ f <- function(x) { y <- match.fun(x) ; y(3,4) } ; c(f(\"+\"),f(\"*\")) }");
    }

    @Test
    public void testSapply() {
        assertEval("{ f <- function() { sapply(1:3,function(x){x*2L}) }; f() + f() }");
        assertEval("{ f <- function() { sapply(c(1,2,3),function(x){x*2}) }; f() + f() }");

        assertEval("{ h <- new.env() ; assign(\"a\",1,h) ; assign(\"b\",2,h) ; sa <- sapply(ls(h), function(k) get(k,h,inherits=FALSE)) ; names(sa) }");

        assertEval("{ sapply(1:3, function(x) { if (x==1) { list(1) } else if (x==2) { list(NULL) } else { list() } }) }");
        assertEval("{ f<-function(g) { sapply(1:3, g) } ; f(function(x) { x*2 }) }");
        assertEval("{ f<-function() { x<-2 ; sapply(1, function(i) { x }) } ; f() }");

        assertEval("{ sapply(1:3,function(x){x*2L}) }");
        assertEval("{ sapply(c(1,2,3),function(x){x*2}) }");
        assertEval("{ sapply(1:3, length) }");
        assertEval("{ f<-length; sapply(1:3, f) }");
        assertEval("{ sapply(list(1,2,3),function(x){x*2}) }");

        assertEval("{ sapply(1:3, function(x) { if (x==1) { 1 } else if (x==2) { integer() } else { TRUE } }) }");

        assertEval("{ f<-function(g) { sapply(1:3, g) } ; f(function(x) { x*2 }) ; f(function(x) { TRUE }) }");
        assertEval("{ sapply(1:2, function(i) { if (i==1) { as.raw(0) } else { 5+10i } }) }");
        assertEval("{ sapply(1:2, function(i) { if (i==1) { as.raw(0) } else { as.raw(10) } }) }");
        assertEval("{ sapply(1:2, function(i) { if (i==1) { as.raw(0) } else { \"hello\" }} ) } ");
        assertEval("{ sapply(1:3, function(x) { if (x==1) { list(1) } else if (x==2) { list(NULL) } else { list(2) } }) }");

        assertEval("{ sapply(1:3, `-`, 2) }");
        assertEval("{ sapply(1:3, \"-\", 2) }");

        // matrix support
        assertEval("{ sapply(1:3, function(i) { list(1,2) }) }");
        assertEval("{ sapply(1:3, function(i) { if (i < 3) { list(1,2) } else { c(11,12) } }) }");
        assertEval("{ sapply(1:3, function(i) { if (i < 3) { c(1+1i,2) } else { c(11,12) } }) }");

        // names
        assertEval("{ (sapply(1:3, function(i) { if (i < 3) { list(xxx=1) } else {list(zzz=2)} })) }");
        assertEval("{ (sapply(1:3, function(i) { list(xxx=1:i) } )) }");
        assertEval("{ sapply(1:3, function(i) { if (i < 3) { list(xxx=1) } else {list(2)} }) }");
        assertEval("{ (sapply(1:3, function(i) { if (i < 3) { c(xxx=1) } else {c(2)} })) }");
        assertEval("{ f <- function() { sapply(c(1,2), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ f <- function() { sapply(c(X=1,Y=2), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ f <- function() { sapply(c(\"a\",\"b\"), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ f <- function() { sapply(c(X=\"a\",Y=\"b\"), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ sapply(c(\"a\",\"b\",\"c\"), function(x) { x }) }");

        assertEval("{ f <- function(v) { sapply(1:3, function(k) v)}; f(1); f(2) }");
    }

    @Test
    public void testApply() {
        assertEval("{ m <- matrix(c(1,2,3,4,5,6),2) ; apply(m,1,sum) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6),2) ; apply(m,2,sum) }");
    }

    @Test
    public void testCat() {
        assertEval("{ cat() }");
        assertEval("{ cat(1) }");
        assertEval("{ cat(1, sep=\"\\n\") }");
        assertEval("{ cat(1,2,3) }");
        assertEval("{ cat(\"a\") }");
        assertEval("{ cat(\"a\", \"b\") }");
        assertEval("{ cat(1, \"a\") }");
        assertEval("{ cat(c(1,2,3)) }");
        assertEval("{ cat(c(\"a\",\"b\")) }");
        assertEval("{ cat(c(1,2,3),c(\"a\",\"b\")) }");
        assertEval("{ cat(TRUE) }");
        assertEval("{ cat(TRUE, c(1,2,3), FALSE, 7, c(\"a\",\"b\"), \"x\") }");
        assertEval("{ cat(1:3) }");
        assertEval("{ cat(\"hi\",1:3,\"hello\") }");
        assertEval("{ cat(2.3) }");
        assertEval("{ cat(1.2,3.4) }");
        assertEval("{ cat(c(1.2,3.4),5.6) }");
        assertEval("{ cat(c(TRUE,FALSE), TRUE) }");
        assertEval("{ cat(NULL) }");
        assertEval("{ cat(1L) }");
        assertEval("{ cat(1L, 2L, 3L) }");
        assertEval("{ cat(c(1L, 2L, 3L)) }");
        assertEval("{ cat(1,2,sep=\".\") }");
        assertEval("{ cat(\"hi\",1[2],\"hello\",sep=\"-\") }");
        assertEval("{ cat(\"hi\",1[2],\"hello\",sep=\"-\\n\") }");
        assertEval("{ m <- matrix(as.character(1:6), nrow=2) ; cat(m) }");
        assertEval("{ cat(sep=\" \", \"hello\") }");
        assertEval("{ cat(rep(NA, 8), \"Hey\",\"Hey\",\"Goodbye\",\"\\n\") }");
        assertEval("{ cat(\"hi\",NULL,\"hello\",sep=\"-\") }");
        assertEval("{ cat(\"hi\",integer(0),\"hello\",sep=\"-\") }");
        assertEval("{ cat(\"a\", \"b\", \"c\", sep=c(\"-\", \"+\")) }");

        assertEval(Ignored.Unknown, "{ cat(c(\"a\", \"b\", \"c\"), \"d\", sep=c(\"-\", \"+\")) }");
    }

    @Test
    public void testCatVarargs() {
        assertEval("{ f <- function(...) {cat(...,sep=\"-\")}; f(\"a\") }");
        assertEval("{ f <- function(...) {cat(...,sep=\"-\\n\")}; f(\"a\") }");
        assertEval("{ f <- function(...) {cat(...,sep=\"-\")}; f(\"a\", \"b\") }");
        assertEval("{ f <- function(...) {cat(...,sep=\"-\\n\")}; f(\"a\", \"b\") }");
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
    public void testOperators() {
        assertEval("{ `+`(1,2) }");
        assertEval("{ `-`(1,2) }");
        assertEval("{ `*`(1,2) }");
        assertEval("{ `/`(1,2) }");
        assertEval("{ `%/%`(1,2) }");
        assertEval("{ `%%`(1,2) }");
        assertEval("{ `^`(1,2) }");
        assertEval("{ `!`(TRUE) }");
        assertEval("{ `%o%`(3,5) }");
        assertEval("{ x <- `+` ; x(2,3) }");
        assertEval("{ x <- `+` ; f <- function() { x <- 1 ; x(2,3) } ; f() }");
        assertEval("{ `||`(TRUE, FALSE) }");
        assertEval("{ `&&`(TRUE, FALSE) }");
        assertEval("{ `|`(TRUE, FALSE) }");
        assertEval("{ `&`(TRUE, FALSE) }");
        assertEval("{ `%*%`(3,5) }");
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

    @Test
    public void testDiagonal() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; diag(m) }");
        assertEval("{ m <- matrix(1:9, nrow=3) ; diag(m) }");

        assertEval("{ diag(1, 7) }");
        assertEval("{ diag(1, 7, 2) }");
        assertEval("{ diag(1, 2, 7) }");
    }

    @Test
    public void testUpdateDiagonal() {
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) <- c(1,2) ; m }");
        assertEval("{ m <- matrix(1:6, nrow=3); y<-m+42; diag(y) <- c(1,2); y }");
        assertEval("{ m <- matrix(1:6, nrow=3) ;  attr(m, \"foo\")<-\"foo\"; diag(m) <- c(1,2); attributes(m) }");
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) <- c(1.1,2.2); m }");
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) <- c(1.1,2); m }");
        assertEval("{ m <- matrix(1:6, nrow=3) ; diag(m) <- c(1,2.2); m }");
        assertEval("{ m <- matrix(1:6, nrow=3) ;  attr(m, \"foo\")<-\"foo\"; diag(m) <- c(1.1,2.2); attributes(m) }");
        assertEval("{ x <- (m <- matrix(1:6, nrow=3)) ; diag(m) <- c(1,2) ; x }");
        assertEval("{ m <- matrix(1:6, nrow=3) ; f <- function() { diag(m) <- c(100,200) } ; f() ; m }");
    }

    @Test
    public void testDimensions() {
        assertEval("{ dim(1) }");
        assertEval("{ dim(1:3) }");
        assertEval("{ m <- matrix(1:6, nrow=3) ; dim(m) }");

        assertEval("{ nrow(1) }");
        assertEval("{ nrow(1:3) }");
        assertEval("{ NROW(1) }");
        assertEval("{ NROW(1:3) }");
        assertEval("{ ncol(1) }");
        assertEval("{ ncol(1:3) }");
        assertEval("{ NCOL(1) }");
        assertEval("{ NCOL(1:3) }");

        assertEval("{ m <- matrix(1:6, nrow=3) ; nrow(m) }");
        assertEval("{ m <- matrix(1:6, nrow=3) ; ncol(m) }");

        assertEval("{ z <- 1 ; dim(z) <- c(1,1) ; dim(z) <- NULL ; z }");

        assertEval("{ x <- 1:4 ; f <- function() { x <- 1:4 ; dim(x) <<- c(2,2) } ; f() ; dim(x) }");

        assertEval("{ x<-1:12; dim(x)<-c(12); x }");
        assertEval(Output.ContainsWarning, "{ x<-1:12; dim(x)<-c(12+10i); x }");
        assertEval("{ x<-1:12; dim(x)<-c(as.raw(12)); x }");
        assertEval("{ x<-1:12; dim(x)<-c(\"12\"); x }");
        assertEval("{ x<-1:1; dim(x)<-c(TRUE); x }");

        assertEval("{ x<-1:12; dim(x)<-c(3, 4); attr(x, \"dim\") }");
        assertEval("{ x<-1:12; attr(x, \"dim\")<-c(3, 4); dim(x) }");
        assertEval("{ x<-1:4; names(x)<-c(21:24); attr(x, \"foo\")<-\"foo\"; x }");
        assertEval("{ x<-list(1,2,3); names(x)<-c(21:23); attr(x, \"foo\")<-\"foo\"; x }");

        assertEval(Output.ContainsError, "{ x <- 1:2 ; dim(x) <- c(1, 3) ; x }");
        assertEval(Output.ContainsError, "{ x <- 1:2 ; dim(x) <- c(1, NA) ; x }");
        assertEval(Output.ContainsError, "{ x <- 1:2 ; dim(x) <- c(1, -1) ; x }");
        assertEval(Output.ContainsError, "{ x <- 1:2 ; dim(x) <- integer() ; x }");
        assertEval("{ b <- c(a=1+2i,b=3+4i) ; attr(b,\"my\") <- 211 ; dim(b) <- c(2,1) ; names(b) }");

        assertEval("{ x<-1:4; dim(x)<-c(4); y<-101:104; dim(y)<-c(4); x > y }");
        assertEval("{ x<-1:4; y<-101:104; dim(y)<-c(4); x > y }");
        assertEval("{ x<-1:4; dim(x)<-c(4); y<-101:104; x > y }");
        assertEval(Output.ContainsError, "{ x<-1:4; dim(x)<-c(4); y<-101:104; dim(y)<-c(2,2); x > y }");
        assertEval(Output.ContainsError, "{ x<-1:4; dim(x)<-c(4); y<-101:108; dim(y)<-c(8); x > y }");

        assertEval("{ x<-c(1); dim(x)<-1; names(x)<-c(\"b\"); attributes(x) }");
        assertEval("{ x<-c(1); dim(x)<-1; attr(x, \"dimnames\")<-list(\"b\"); attributes(x) }");
        assertEval("{ x<-c(1); dim(x)<-1; attr(x, \"dimnames\")<-list(a=\"b\"); attributes(x) }");
        // reset all attributes
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; dim(x)<-NULL; attributes(x) }");
        // reset dimensions and dimnames
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; attr(x, \"foo\")<-\"foo\"; attr(x, \"dim\")<-NULL; attributes(x) }");
        // second names() invocation sets "dimnames"
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; attributes(x) }");
        // third names() invocation resets "names" (and not "dimnames")
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; names(x)<-NULL; attributes(x) }");
        // both names and dimnames are set and then re-set
        assertEval("{ x<-c(42); names(x)<-\"a\"; attr(x, \"dim\")<-1; names(x)<-\"z\"; names(x)<-NULL; attr(x, \"dimnames\")<-NULL; attributes(x) }");
        assertEval(Output.ContainsError, "{ x<-1:4; attr(x, \"dimnames\") <- list(101, 102, 103, 104) }");
        // assigning an "invisible" list returned by "attr(y, dimnames)<-" as dimnames attribute for
        // x
        assertEval("{ x<-c(1); y<-c(1); dim(x)<-1; dim(y)<-1; attr(x, \"dimnames\")<-(attr(y, \"dimnames\")<-list(\"b\")); attributes(x) }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(NULL); attributes(x) }");
        assertEval(Output.ContainsError, "{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\")); x }");
        assertEval(Output.ContainsError, "{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\", \"b\"), NULL, c(\"d\")); x }");
        assertEval(Output.ContainsError, "{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\", \"b\"), 42, c(\"d\", \"e\", \"f\")); attributes(x) }");
        assertEval(Output.ContainsError, "{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x) <- list(c(\"a\", \"b\"), \"c\", c(\"d\", \"e\"), 7); attributes(x) }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(c(\"a\", \"b\"), \"c\", c(\"d\", \"e\")); attributes(x) }");
        assertEval("{ x<-1:4; dim(x)<-c(2,1,2); dimnames(x)<-list(c(\"a\", \"b\"), 42, c(\"d\", \"e\")); attributes(x) }");

        // there should be no output
        assertEval("{ x<-42; y<-(dim(x)<-1); }");
        assertEval("{ x<-42; y<-(dim(x)<-1); y }");
        // dim vector should not be shared
        assertEval("{ x<-1:4; y<-c(2, 2); dim(x)<-y; y[1]=4; dim(x) }");
        assertEval("{ x<-1; dim(x)=1; attr(x, \"foo\")<-\"foo\"; dim(x)<-NULL; attributes(x) }");
        assertEval("{ x<-1; dim(x)=1; attr(x, \"names\")<-\"a\"; dim(x)<-NULL; attributes(x) }");
        assertEval("{ x<-1; dim(x)=1; names(x)<-\"a\"; dim(x)<-NULL; attributes(x) }");
        assertEval("{ x<-1:2; dim(x)=c(1,2); names(x)<-c(\"a\", \"b\"); attr(x, \"foo\")<-\"foo\"; dim(x)<-NULL; attributes(x) }");
        assertEval("{ x<-1:4; names(x)<-c(21:24); attr(x, \"dim\")<-c(4); attr(x, \"foo\")<-\"foo\"; x }");
        assertEval("{ x<-list(1,2,3); names(x)<-c(21:23); attr(x, \"dim\")<-c(3); attr(x, \"foo\")<-\"foo\"; x }");

        assertEval("{ x<-1; dimnames(x) }");
        assertEval("{ dimnames(1) }");
        assertEval("{ dimnames(NULL) }");
        assertEval(Output.ContainsError, "{ x<-1; dim(x)<-1; dimnames(x) <- 1; dimnames(x) }");
        assertEval(Output.ContainsError, "{ x<-1; dim(x)<-1; attr(x, \"dimnames\") <- 1 }");
        assertEval("{ x<-1; dim(x)<-1; dimnames(x)<-list() }");
        assertEval("{ x<-1; dim(x)<-1; dimnames(x)<-list(0) }");
        assertEval("{ x<-1; dim(x)<-1; dimnames(x)<-list(\"a\"); dimnames(x); dimnames(x)<-list(); dimnames(x) }");

        assertEval("{ x <- 1:2 ; dim(x) <- c(1,2) ; x }");
        assertEval("{ x <- 1:2 ; attr(x, \"dim\") <- c(2,1) ; x }");

        assertEval("{ n <- 17 ; fac <- factor(rep(1:3, length = n), levels = 1:5) ; y<-tapply(1:n, fac, sum); attributes(y) }");
    }

    @Test
    public void testCumulativeSum() {
        assertEval("{ cumsum(1:10) }");
        assertEval("{ cumsum(c(1,2,3)) }");
        assertEval("{ cumsum(NA) }");
        assertEval("{ cumsum(c(2000000000L, NA, 2000000000L)) }");
        assertEval("{ cumsum(c(TRUE,FALSE,TRUE)) }");
        assertEval("{ cumsum(c(TRUE,FALSE,NA,TRUE)) }");
        assertEval("{ cumsum(c(1+1i,2-3i,4+5i)) }");
        assertEval("{ cumsum(c(1+1i, NA, 2+3i)) }");
        assertEval("{ cumsum(as.logical(-2:2)) }");

        assertEval(Ignored.Unknown, "{ cumsum(c(1,2,3,0/0,5)) }");
        assertEval(Ignored.Unknown, "{ cumsum(c(1,0/0,5+1i)) }");
        assertEval(Ignored.Unknown, "{ cumsum(as.raw(1:6)) }");
        assertEval(Ignored.Unknown, "{ cumsum(rep(1e308, 3) ) }"); // FIXME 1e+308
        assertEval(Ignored.Unknown, "{ cumsum(c(1e308, 1e308, NA, 1, 2)) }"); // FIXME 1e+308

        assertEval(Ignored.Unknown, "{ cumsum(c(2000000000L, 2000000000L)) }"); // FIXME missing
// warning
        assertEval(Ignored.Unknown, "{ cumsum(c(-2147483647L, -1L)) }"); // FIXME missing warning
        assertEval(Ignored.Unknown, "{ cumsum((1:6)*(1+1i)) }"); // FIXME print formatting

    }

    @Test
    public void testWhich() {
        assertEval("{ which(c(TRUE, FALSE, NA, TRUE)) }");
        assertEval("{ which(logical()) }");
        assertEval("{ which(TRUE) }");
        assertEval("{ which(NA) }");
        assertEval("{ x<-c(1,2); names(x)<-c(11, 12); attributes(which (x > 1)) }");
        assertEval("{ which(c(a=TRUE,b=FALSE,c=TRUE)) }");
    }

    @Test
    public void testColumnsRowsStat() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; colSums(na.rm = FALSE, x = m) }");

        assertEval("{ m <- matrix(1:6, nrow=2) ; colMeans(m) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; rowMeans(x = m, na.rm = TRUE) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; rowSums(x = m) }");

        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; colMeans(m) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; rowMeans(m) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; colSums(m) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; rowSums(m) }");

        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowSums(m) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowSums(m, na.rm = TRUE) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowMeans(m, na.rm = TRUE) }");

        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colSums(m) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colSums(na.rm = TRUE, m) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colMeans(m) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; colMeans(m, na.rm = TRUE) }");

        assertEval("{ colMeans(matrix(as.complex(1:6), nrow=2)) }");
        assertEval("{ colMeans(matrix((1:6)*(1+1i), nrow=2)) }");
        assertEval("{ rowSums(matrix(as.complex(1:6), nrow=2)) }");
        assertEval("{ rowSums(matrix((1:6)*(1+1i), nrow=2)) }");
        assertEval("{ rowMeans(matrix(as.complex(1:6), nrow=2)) }");
        assertEval("{ rowMeans(matrix((1:6)*(1+1i), nrow=2)) }");
        assertEval("{ colSums(matrix(as.complex(1:6), nrow=2)) }");
        assertEval("{ colSums(matrix((1:6)*(1+1i), nrow=2)) }");

        assertEval("{ o <- outer(1:3, 1:4, \"<\") ; colSums(o) }");
    }

    @Test
    public void testNChar() {
        assertEval("{ nchar(c(\"hello\", \"hi\")) }");
        assertEval("{ nchar(c(\"hello\", \"hi\", 10, 130)) }");
        assertEval("{ nchar(c(10,130)) }");
    }

    @Test
    public void testStrSplit() {
        assertEval("{ strsplit(\"helloh\", \"h\", fixed=TRUE) }");
        assertEval("{ strsplit( c(\"helloh\", \"hi\"), c(\"h\",\"\"), fixed=TRUE) }");
        assertEval("{ strsplit(\"helloh\", \"\", fixed=TRUE) }");
        assertEval("{ strsplit(\"helloh\", \"h\") }");
        assertEval("{ strsplit( c(\"helloh\", \"hi\"), c(\"h\",\"\")) }");
        assertEval("{ strsplit(\"ahoj\", split=\"\") [[c(1,2)]] }");
        assertEval("{ strsplit(\"a,h,o,j\", split=\",\") }");
    }

    @Test
    public void testPaste() {
        assertEval("{ paste() }");
        assertEval("{ a <- as.raw(200) ; b <- as.raw(255) ; paste(a, b) }");
        assertEval("{ paste(character(0),31415) }");
        assertEval("{ paste(1:2, 1:3, FALSE, collapse=NULL) }");
        assertEval("{ paste(sep=\"\") }");
        assertEval("{ paste(1:2, 1:3, FALSE, collapse=\"-\", sep=\"+\") }");
    }

    @Test
    public void testIsTRUE() {
        assertEval("{ isTRUE(NULL) }");
        assertEval("{ isTRUE(TRUE) }");
        assertEval("{ isTRUE(FALSE) }");
        assertEval("{ isTRUE(NA) }");
        assertEval("{ isTRUE(1) }");
        assertEval("{ isTRUE(as.vector(TRUE)) }");
        assertEval("{ isTRUE(as.vector(FALSE)) }");
        assertEval("{ isTRUE(as.vector(1)) }");
        assertEval("{ file.path(\"a\", \"b\", c(\"d\",\"e\",\"f\")) }");
        assertEval("{ file.path() }");
    }

    @Test
    public void testSubstring() {
        assertEval("{ substr(\"123456\", 2L, 4L) }");
        assertEval("{ substr(\"123456\", 2, 4) }");
        assertEval("{ substr(\"123456\", 4, 2) }");
        assertEval("{ substr(\"123456\", 7, 8) }");
        assertEval("{ substr(\"123456\", 4, 8) }");
        assertEval("{ substr(\"123456\", -1, 3) }");
        assertEval("{ substr(\"123456\", -5, -1) }");
        assertEval("{ substr(\"123456\", -20, -100) }");
        assertEval("{ substr(\"123456\", 2.8, 4) }");
        assertEval("{ substr(c(\"hello\", \"bye\"), 1, 2) }");
        assertEval("{ substr(c(\"hello\", \"bye\"), c(1,2,3), 4) }");
        assertEval("{ substr(c(\"hello\", \"bye\"), 1, c(1,2,3)) }");
        assertEval("{ substr(c(\"hello\", \"bye\"), c(1,2), c(2,3)) }");
        assertEval("{ substr(1234L,2,3) }");
        assertEval("{ substr(1234,2,3) }");
        assertEval("{ substr(\"abcdef\",c(1,2),c(3L,5L)) }");
        assertEval(Output.ContainsError, "{ substr(c(\"abcdef\", \"aa\"), integer(), 2) }");
        assertEval(Output.ContainsError, "{ substr(c(\"abcdef\", \"aa\"), 2, integer()) }");
        assertEval("{ substr(character(), integer(), integer()) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), NA, 4) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), 3, NA) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), c(NA,8), 4) }");
        assertEval("{ substr(c(\"abcdef\", \"aa\"), c(1,NA), 4) }");

        assertEval("{ substr(NA,1,2) }");
        assertEval("{ substr(\"fastr\", NA, 2) }");
        assertEval("{ substr(\"fastr\", 1, NA) }");

        assertEval("{ x<-\"abcdef\"; substr(x,1,4)<-\"0000\"; x }");
        assertEval("{ x<-\"abcdef\"; substr(x,1,3)<-\"0000\"; x }");
        assertEval("{ x<-\"abcdef\"; substr(x,1,3)<-\"0\"; x }");
        assertEval("{ x<-\"abcdef\"; substr(x,NA,3)<-\"0\"; x }");
        assertEval("{ x<-\"abcdef\"; substr(x,1,NA)<-\"0\"; x }");
        assertEval("{ x<-character(); substr(x,1,3)<-\"0\"; x }");
        assertEval("{ x<-c(\"abcdef\", \"ghijklm\"); substr(x, c(1,NA), 4)<-\"0\"; x }");
        assertEval(Output.ContainsError, "{ x<-\"abcdef\"; substr(x,3,1)<-0; x }");
        assertEval(Output.ContainsError, "{ x<-\"abcdef\"; substr(x,1,3)<-character(); x }");
        assertEval(Output.ContainsError, "{ x<-\"abcdef\"; substr(x,1,3)<-NULL; x }");
        assertEval(Output.ContainsError, "{ x<-\"abcdef\"; substr(x,integer(),3)<-NULL; x }");
        assertEval("{ x<-character(); substr(x,1,3)<-0; x }");
        assertEval("{ x<-character(); substr(x,1,3)<-NULL; x }");
        assertEval("{ x<-character(); substr(x,integer(),3)<-NULL; x }");

        assertEval("{ x<-c(\"abcdef\"); substr(x[1], 2, 3)<-\"0\"; x }");

        assertEval("{ substring(\"123456\", first=2, last=4) }");
        assertEval("{ substring(\"123456\", first=2.8, last=4) }");
        assertEval("{ substring(c(\"hello\", \"bye\"), first=c(1,2,3), last=4) }");
        assertEval("{ substring(\"fastr\", first=NA, last=2) }");
    }

    @Test
    public void testOrder() {
        assertEval("{ order(c(\"a\",\"c\",\"b\",\"d\",\"e\",\"f\")) }");
        assertEval("{ order(c(5,2,2,1,7,4)) }");
        assertEval("{ order(c(5,2,2,1,7,4),c(\"a\",\"c\",\"b\",\"d\",\"e\",\"f\")) }");
        assertEval("{ order(c(1,1,1,1),c(4,3,2,1)) }");
        assertEval("{ order(c(1,1,1,1),c(\"d\",\"c\",\"b\",\"a\")) }");
        assertEval("{ order(c(1i,2i,3i)) }");
        assertEval("{ order(c(3i,1i,2i)) }");
        assertEval("{ order(c(3+1i,2+2i,1+3i)) }");
        assertEval("{ order(c(3+1i,2+3i,2+2i,1+3i)) }");

        assertEval("{ order(7) }");
        assertEval("{ order(FALSE) }");
        assertEval("{ order(character()) }");

        assertEval("{ order(1:3) }");
        assertEval("{ order(3:1) }");
        assertEval("{ order(c(1,1,1), 3:1) }");
        assertEval("{ order(c(1,1,1), 3:1, decreasing=FALSE) }");
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=TRUE) }");
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=NA) }");
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=FALSE) }");
        assertEval("{ order() }");
        assertEval("{ order(c(NA,NA,1), c(2,1,3)) }"); // infinite loop
        assertEval("{ order(c(NA,NA,1), c(1,2,3)) }"); // infinite loop
        assertEval("{ order(c(1,2,3,NA)) }"); // infinite loop
        assertEval("{ order(c(1,2,3,NA), na.last=FALSE) }");
        assertEval("{ order(c(1,2,3,NA), na.last=FALSE, decreasing=TRUE) }");
        assertEval("{ order(c(0/0, -1/0, 2)) }");

        assertEval("{ x<-c(40, 40,  1, 40,  1, 20, 40, 10, 40, 10, 16, 40, 10, 26, 40, 10, 39, 40, 11, 40, 12, 40, 12, 20); order(x, decreasing=TRUE) }");
        assertEval("{ x<-c(40, 40,  1, 40,  1, 20, 40, 10, 40, 10, 16, 40, 10, 26, 40, 10, 39, 40, 11, 40, 12, 40, 12, 20); order(x, decreasing=FALSE) }");

        assertEval("{ order(c(-1480,  -974, -1576,  -970), c(\"a\", \"b\", \"c\", \"d\")) }");

        assertEval("{ order(c(0/0, -1/0, 2), na.last=NA) }");
    }

    @Test
    public void testTrigExp() {
        assertEval("{ sin(1.2) }");
        assertEval("{ cos(1.2) }");
        assertEval("{ tan(1.2) }");
        assertEval("{ asin(0.4) }");
        assertEval("{ acos(0.4) }");
        assertEval("{ atan(0.4) }");
        assertEval("{ atan2(0.4, 0.8) }");
        assertEval("{ exp(1) }");
        assertEval("{ expm1(2) }");
        assertEval("{ sin(c(0.3,0.6,0.9)) }");
        assertEval("{ cos(c(0.3,0.6,0.9)) }");
        assertEval("{ tan(c(0.3,0.6,0.9)) }");
        assertEval("{ asin(c(0.3,0.6,0.9)) }");
        assertEval("{ acos(c(0.3,0.6,0.9)) }");
        assertEval("{ atan(c(0.3,0.6,0.9)) }");
        assertEval("{ atan2(c(0.3,0.6,0.9), 0.4) }");
        assertEval("{ atan2(0.4, c(0.3,0.6,0.9)) }");
        assertEval("{ atan2(c(0.3,0.6,0.9), c(0.4, 0.3)) }");
        assertEval("{ exp(c(1,2,3)) }");
        assertEval("{ expm1(c(1,2,3)) }");
        assertEval(Output.ContainsError, "{ sin() }");
        assertEval(Output.ContainsError, "{ cos() }");
        assertEval(Output.ContainsError, "{ tan() }");
        assertEval(Output.ContainsError, "{ asin() }");
        assertEval(Output.ContainsError, "{ acos() }");
        assertEval(Output.ContainsError, "{ atan() }");
        assertEval(Output.ContainsError, "{ atan2() }");
        assertEval(Output.ContainsError, "{ atan2(0.7) }");
        assertEval(Output.ContainsError, "{ exp() }");
        assertEval(Output.ContainsError, "{ expm1() }");
    }

    @Test
    public void testLog() {
        assertEval("{ log(1) } ");
        assertEval("{ log(0) }");
        assertEval("{ log(c(0,1)) }");
        assertEval("{ round( log(10,), digits = 5 ) }");
        assertEval("{ round( log(10,2), digits = 5 ) }");
        assertEval("{ round( log(10,10), digits = 5 ) }");
    }

    @Test
    public void testLog2() {
        assertEval("{ log2(1) } ");
        assertEval("{ log2(0) }");
        assertEval("{ log2(c(0,1)) }");

        assertEval("{ log2(2) } ");
        assertEval("{ log2(4) } ");
        assertEval("{ as.integer(log2(6)*1000000) } ");
    }

    @Test
    public void testLog10() {
        assertEval("{ log10(1) } ");
        assertEval("{ log10(0) }");
        assertEval("{ log10(c(0,1)) }");

        assertEval("{ log10(10) } ");
        assertEval("{ log10(100) } ");
        assertEval("{ as.integer(log10(200)*100000) } ");

        assertEval(Ignored.Unknown, "{ m <- matrix(1:4, nrow=2) ; round( log10(m), digits=5 )  }");
        assertEval(Ignored.Unknown, "{ x <- c(a=1, b=10) ; round( c(log(x), log10(x), log2(x)), digits=5 ) }");
    }

    @Test
    public void testSqrt() {
        assertEval("{ sqrt(9) }");
        assertEval("{ sqrt(9L) }");
        assertEval("{ sqrt(NA) }");
        assertEval("{ sqrt(c(1,4,9,16)) }");
        assertEval("{ sqrt(c(1,4,NA,16)) }");
        assertEval("{ sqrt(c(a=9,b=81)) }");
        assertEval("{ sqrt(1:5) }");

        assertEval(Ignored.Unknown, "{ sqrt(-1L) }"); // FIXME warning
        assertEval(Ignored.Unknown, "{ sqrt(-1) }"); // FIXME warning
    }

    @Test
    public void testExp() {
        assertEval("{ round( exp(c(1+1i,-2-3i)), digits=5 ) }");
        assertEval("{ round( exp(1+2i), digits=5 ) }");
    }

    @Test
    public void testAbs() {
        assertEval("{ abs(0) }");
        assertEval("{ abs(100) }");
        assertEval("{ abs(-100) }");
        assertEval("{ abs(0/0) }");
        assertEval("{ abs((1:2)[3]) }");
        assertEval("{ abs((1/0)*(1-0i)) }");
        assertEval("{ abs(1) }");
        assertEval("{ abs(1L) }");
        assertEval("{ abs(-1) }");
        assertEval("{ abs(-1L) }");
        assertEval("{ abs(NA) }");
        assertEval("{ abs(c(1, 2, 3)) }");
        assertEval("{ abs(c(1L, 2L, 3L)) }");
        assertEval("{ abs(c(1, -2, 3)) }");
        assertEval("{ abs(c(1L, -2L, 3L)) }");
        assertEval("{ abs(c(1L, -2L, NA)) }");
        assertEval("{ abs((-1-0i)/(0+0i)) }");
        assertEval("{ abs((-0-1i)/(0+0i)) }");
        assertEval("{ abs(NA+0.1) }");
        assertEval("{ abs((0+0i)/0) }");
        assertEval("{ abs(c(1, -2, NA)) }");
        assertEval(Output.ContainsError, "{ abs(NULL) }");

        assertEval(Ignored.Unknown, "{ abs(c(0/0,1i)) }");
        assertEval(Ignored.Unknown, "{ abs(1:3) }");
        assertEval(Ignored.Unknown, "{ abs(-1:-3) }");
    }

    @Test
    public void testFloor() {
        assertEval("{ floor(c(0.2,-3.4,NA,0/0,1/0)) }");
    }

    @Test
    public void testCeiling() {
        assertEval("{ ceiling(c(0.2,-3.4,NA,0/0,1/0)) }");
    }

    @Test
    public void testCharUtils() {
        assertEval("{ toupper(c(\"hello\",\"bye\")) }");
        assertEval("{ tolower(c(\"Hello\",\"ByE\")) }");
        assertEval("{ toupper(c()) }");
        assertEval("{ tolower(c()) }");
        assertEval("{ toupper(NA) }");

        assertEval(Ignored.Unknown, "{ tolower(1E100) }");
        assertEval(Ignored.Unknown, "{ toupper(1E100) }");
        assertEval(Ignored.Unknown, "{ m <- matrix(\"hi\") ; toupper(m) }");
        assertEval(Ignored.Unknown, "{ toupper(c(a=\"hi\", \"hello\")) }");
        assertEval(Ignored.Unknown, "{ tolower(c(a=\"HI\", \"HELlo\")) }");
        assertEval(Ignored.Unknown, "{ tolower(NA) }");
    }

    @Test
    public void testTypeOf() {
        assertEval("{ typeof(1) }");
        assertEval("{ typeof(1L) }");
        assertEval("{ typeof(function(){}) }");
        assertEval("{ typeof(\"hi\") }");
        assertEval("{ typeof(sum) }");
        assertEval("{ typeof(NULL) }");
        assertEval("{ typeof(TRUE) }");
        assertEval("{ typeof(\"test\") }");
        assertEval("{ typeof(c(1, 2, 3)) }");
        assertEval("{ typeof(c(1L, 2L, 3L)) }");
        assertEval("{ typeof(1:3) }");
        assertEval("{ typeof(c(TRUE, TRUE, FALSE)) }");
        assertEval("{ typeof(typeof(NULL)) }");
        assertEval("{ length(typeof(NULL)) }");
        assertEval("{ typeof(length(typeof(NULL))) }");

        assertEval("{ f <- function(...) typeof(...); f(1)}");
        assertEval("{ f <- function(...) typeof(...); f(1, 2)}");
        assertEval("{ f <- function(...) typeof(...); f(1, 2, 3)}");
        assertEval("{ f <- function(...) typeof(...); f(1, 2, 3, 4)}");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); typeof(x) }");
        assertEval("{ x<-data.frame(c(\"a\", \"b\", \"a\")); typeof(x) }");

        assertEval("{ f <- function(...) typeof(...); f()}");

        assertEval("{  typeof(seq(1,2)) }");
    }

    @Test
    public void testSub() {
        assertEval("{ gsub(\"a\",\"aa\", \"prague alley\") }");
        assertEval("{ sub(\"a\",\"aa\", \"prague alley\") }");
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\") }");
        assertEval("{ gsub(\"a\",\"aa\", \"prague alley\", fixed=TRUE) }");
        assertEval("{ sub(\"a\",\"aa\", \"prague alley\", fixed=TRUE) }");
        assertEval("{ gsub(\"a\",\"aa\", \"prAgue alley\", fixed=TRUE) }");
        assertEval(Output.ContainsWarning, "{ gsub(\"a\",\"aa\", \"prAgue alley\", fixed=TRUE, ignore.case=TRUE) }");
        assertEval("{ gsub(\"([a-e])\",\"\\\\1\\\\1\", \"prague alley\") }");
        assertEval("{ gsub(\"h\",\"\", c(\"hello\", \"hi\", \"bye\")) }");
        assertEval("{ gsub(\"h\",\"\", c(\"hello\", \"hi\", \"bye\"), fixed=TRUE) }");
        assertEval(Ignored.Unknown, "{ gsub(\"a\",\"aa\", \"prAgue alley\", ignore.case=TRUE) }");

        assertEval("{ sub('[[:space:]]+$', '', 'R (>= 3.0.3)  ') }");
        assertEval("{ sub('^[[:space:]]*(.*)', '\\\\1', 'R (>= 3.0.3)') }");
        assertEval("{ sub('^([[:alnum:].]+).*$', '\\\\1', 'R (>= 3.0.3)') }");

        assertEval("{ sub('^([1[:alpha:].]+).*$', '\\\\1', '1R.ff (>= 3.0.3)') }");

        assertEval("{ sub('^([[:alnum:]]*).*$', '\\\\1', 'aZ45j%$  ') }");
        assertEval("{ sub('^([[:alpha:]]*).*$', '\\\\1', 'aZ45j%$  ') }");
        assertEval("{ sub('^([[:blank:]]*).*$', '\\\\1', '  \\ta45j%$  ') }");
        assertEval("{ sub('^([[:cntrl:]]*).*$', '\\\\1', '\\ta45j%$  ') }");
        assertEval("{ sub('^([[:digit:]]*).*$', '\\\\1', '12a45j%$  ') }");
        assertEval("{ sub('^([[:graph:]]*).*$', '\\\\1', 'a45j%$  ') }");
        assertEval("{ sub('^([[:lower:]]*).*$', '\\\\1', 'a45j%$  ') }");
        assertEval("{ sub('^([[:print:]]*).*$', '\\\\1', 'a45j%$  ') }");
        assertEval("{ sub('^([[:punct:]]*).*$', '\\\\1', '.,/a45j%$  ') }");
        assertEval("{ sub('^([[:space:]]*).*$', '\\\\1', '   a45j%$  ') }");
        assertEval("{ sub('^([[:upper:]]*).*$', '\\\\1', 'AASDFAa45j%$  ') }");
        assertEval("{ sub('^([[:xdigit:]]*).*$', '\\\\1', '1234abABhxa45j%$  ') }");
    }

    @Test
    public void testGrep() {
        assertEval("{ txt<-c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"); grep(\"foo\", txt) }");
        assertEval("{ txt<-c(\"is\", \"intended\", \"to\", \"guarantee\", \"your\", \"freedom\"); grep(\"[gu]\", txt) }");
        assertEval("{ txt<-c(\"1+1i\", \"7\", \"42.1\", \"7+42i\"); grep(\"[0-9].*[-+][0-9].*i$\", txt) }");
        assertEval("{ txt<-c(\"rai\", \"ira\", \"iri\"); grep(\"i$\", txt) }");
        assertEval("{ txt<-c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"); grepl(\"foo\", txt) }");
    }

    @Test
    public void testRegExpr() {
        // FIXME: missing attributes
        assertEval(Ignored.Unknown, "regexpr(\"e\",c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"))");
        // FIXME: missing attributes
        assertEval(Ignored.Unknown, "gregexpr(\"e\",c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"))");
    }

    @Test
    public void testRegExprComplex() {
        // NOTE: this is without attributes
        assertEval(Ignored.Unknown, "gregexpr(\"(a)[^a]\\\\1\", c(\"andrea apart\", \"amadeus\", NA))");
        // NOTE: this is without attributes
        assertEval(Ignored.Unknown, "regexpr(\"(a)[^a]\\\\1\", c(\"andrea apart\", \"amadeus\", NA))");
    }

    @Test
    public void testLsRegExp() {
        assertEval("{ abc <- 1; ls(pattern=\"a.*\")}");
        assertEval("{ .abc <- 1; ls(pattern=\"\\\\.a.*\")}");
        assertEval("{ .abc <- 1; ls(all.names=TRUE, pattern=\"\\\\.a.*\")}");
        assertEval("{ abc <- 1; ls(pattern=\"[[:alpha:]]*\")}");
        assertEval("{ f <- function(abc) { ls(pattern=\"[a-z]*\") }; f(1) }");
    }

    @Test
    public void testLengthUpdate() {
        assertEval("{ x<-c(a=1, b=2); length(x)<-1; x }");
        assertEval("{ x<-c(a=1, b=2); length(x)<-4; x }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); length(x)<-1; x }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); length(x)<-4; x }");
        assertEval("{ x<-data.frame(a=1,b=2); length(x)<-1; attributes(x) }");
        assertEval("{ x<-data.frame(a=1,b=2); length(x)<-4; attributes(x) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); length(x)<-1; x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); length(x)<-4; x }");
    }

    @Test
    public void testLength() {
        assertEval("{ x <- 1:4 ; length(x) <- 2 ; x }");
        assertEval("{ x <- 1:2 ; length(x) <- 4 ; x }");
        assertEval("{ length(c(z=1:4)) }");
        assertEval("{ x <- 1 ; f <- function() { length(x) <<- 2 } ; f() ; x }");
        assertEval("{ length(1) }");
        assertEval("{ length(NULL) }");
        assertEval("{ length(NA) }");
        assertEval("{ length(TRUE) }");
        assertEval("{ length(1L) }");
        assertEval("{ length(1+1i) }");
        assertEval("{ length(d<-dim(1:3)) }");
        assertEval("{ length(1:3) }");
        assertEval("{ x <- 1:2 ; z <- (length(x) <- 4) ; z }");
        assertEval("{ x<-c(a=7, b=42); length(x)<-4; x }");
        assertEval("{ x<-c(a=7, b=42); length(x)<-1; x }");
    }

    @Test
    public void testNames() {
        assertEval("{ x<-c(1,2,3); dim(x)<-3; dimnames(x)<-list(c(11,12,13)); names(x) }");
    }

    @Test
    public void testUpdateNames() {
        assertEval("{ x <- c(1,2) ; names(x) <- c(\"hello\", \"hi\"); names(x) } ");

        assertEval("{ x <- 1:2 ; names(x) <- c(\"hello\", \"hi\"); names(x) } ");
        assertEval("{ x<-c(1, 2); attr(x, \"names\")<-c(\"a\", \"b\"); x }");
        assertEval("{ x<-c(1, 2); attr(x, \"names\")<-c(\"a\", \"b\"); names(x)<-NULL; attributes(x) }");
        assertEval("{ x<-c(1, 2); names(x)<-c(\"a\", \"b\"); attr(x, \"names\")<-NULL; x }");
        assertEval("{ x<-c(1, 2); names(x)<-42; x }");
        assertEval("{ x<-c(1, 2); names(x)<-c(TRUE, FALSE); x }");
        assertEval(Output.ContainsError, "{ x<-c(1,2); names(x) <- 42:44; x }");
        assertEval(Output.ContainsError, "{ x<-c(1,2); attr(x, \"names\") <- 42:45; x }");
        assertEval("{ x<-list(1,2); names(x)<-c(\"a\",NA); x }");
        assertEval("{ x<-list(1,2); names(x)<-c(\"a\",\"$\"); x }");
        assertEval("{ x<-list(1,2); names(x)<-c(\"a\",\"b\"); x }");
        assertEval("{ x<-list(1,2); names(x)<-42:43; x }");
        assertEval("{ x<-7; attr(x, \"foo\")<-\"a\"; attr(x, \"bar\")<-42; attributes(x) }");
        assertEval("{ x<-c(\"a\", \"\", \"bbb\", \"\", \"c\"); names(x)<-1:4; x }");

        assertEval("{ x <- c(1,2); names(x) <- c(\"hello\", \"hi\") ; x }");
        assertEval("{ x <- 1:2; names(x) <- c(\"hello\", \"hi\") ; x }");

        assertEval("{ x <- c(1,9); names(x) <- c(\"hello\",\"hi\") ; sqrt(x) }");
        assertEval("{ x <- c(1,9); names(x) <- c(\"hello\",\"hi\") ; is.na(x) }");
        assertEval("{ x <- c(1,NA); names(x) <- c(\"hello\",\"hi\") ; cumsum(x) }");
        assertEval("{ x <- c(1,NA); names(x) <- c(NA,\"hi\") ; cumsum(x) }");

        assertEval("{ x <- 1:2; names(x) <- c(\"A\", \"B\") ; abs(x) }");
        assertEval("{ z <- c(a=1, b=2) ; names(z) <- NULL ; z }");
        assertEval("{ x <- c(1,2) ; names(x) <- c(\"hello\"); names(x) }");
        assertEval("{ x <- 1:2 ; names(x) <- c(\"hello\"); names(x) }");

        assertEval("{ x <- c(1,2); names(x) <- c(\"A\", \"B\") ; x + 1 }");
        assertEval("{ x <- 1:2; names(x) <- c(\"A\", \"B\") ; y <- c(1,2,3,4) ; names(y) <- c(\"X\", \"Y\", \"Z\") ; x + y }");
    }

    @Test
    public void testRev() {
        assertEval("{ rev(1:3) }");
        assertEval("{ rev(c(1+1i, 2+2i)) }");
    }

    @Test
    public void testLookup() {
        assertEval("{ f <- function() { assign(\"x\", 1) ; x } ; f() }");
        assertEval("{ f <- function() { x <- 2 ; g <- function() { x <- 3 ; assign(\"x\", 1, inherits=FALSE) ; x } ; g() } ; f() }");
        assertEval("{ f <- function() { x <- 2 ; g <- function() { assign(\"x\", 1, inherits=FALSE) } ; g() ; x } ; f() }");
        assertEval("{ f <- function() { x <- 2 ; g <- function() { assign(\"x\", 1, inherits=TRUE) } ; g() ; x } ; f() }");
        assertEval("{ f <- function() {  g <- function() { assign(\"x\", 1, inherits=TRUE) } ; g() } ; f() ; x }");
        assertEval("{ x <- 3 ; g <- function() { x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 2) ; g <- function() { x } ; g() } ; f() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; h() }");
        assertEval("{ h <- function() { x <- 3  ; f <- function() { assign(\"x\", 2) ; g <- function() { x } ; g() } ; f() }  ; h() }");
        assertEval("{ x <- 3 ; h <- function() { g <- function() { x } ; f <- function() { assign(\"x\", 2, inherits=TRUE) } ; f() ; g() }  ; h() }");
        assertEval("{ x <- 3 ; h <- function(s) { if (s == 2) { assign(\"x\", 2) } ; x }  ; h(1) ; h(2) }");
        assertEval("{ x <- 3 ; h <- function(s) { y <- x ; if (s == 2) { assign(\"x\", 2) } ; c(y,x) }  ; c(h(1),h(2)) }");
        assertEval("{ g <- function() { x <- 2 ; f <- function() { x ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { f <- function() { if (FALSE) { x } ; assign(\"x\", 1) ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; assign(\"x\", 2) ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ h <- function() { g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
        assertEval("{ f <- function(z) { exists(\"z\") } ; f() }");
        assertEval("{ f <- function() { x <- 3 ; exists(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ f <- function() { z <- 3 ; exists(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ f <- function() { if (FALSE) { x <- 3 } ; exists(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 2) ; exists(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ g <- function() { x <- 2 ; f <- function() { if (FALSE) { x <- 3 } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { x <- 2 ; f <- function() { x <- 5 ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { f <- function() { assign(\"x\", 3) ; if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { f <- function() { assign(\"z\", 3) ; if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ h <- function() { assign(\"x\", 1) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
        assertEval("{ h <- function() { assign(\"z\", 1) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");

        assertEval("{ x <- 3 ; f <- function() { exists(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { exists(\"x\", inherits=FALSE) } ; f() }");

        assertEval("{ x <- 2 ; y <- 3 ; rm(\"y\") ; ls() }");
        assertEval(Output.ContainsError, "{ x <- 2 ; rm(\"x\") ; get(\"x\") }");
        assertEval(Output.ContainsError, "{ get(\"x\") }");

        assertEval("{ f <- function() { assign(\"x\", 1) ; y <- 2 ; ls() } ; sort(f()) }");
        assertEval("{ f <- function() { x <- 1 ; y <- 2 ; ls() } ; sort(f()) }");
        assertEval("{ f <- function() { assign(\"x\", 1) ; y <- 2 ; if (FALSE) { z <- 3 } ; ls() } ; sort(f()) }");
        assertEval("{ f <- function() { if (FALSE) { x <- 1 } ; y <- 2 ; ls() } ; f() }");
        // the actual elements are formatted differently from GNU-R, also in different order
        assertEval("{ f <- function() { for (i in rev(1:10)) { assign(as.character(i), i) } ; ls() } ; length(f()) }");

        // lookup
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x } ; h() } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; g <- function() { assign(\"y\", 3) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x } ; h() } ; g()  } ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 2, inherits=TRUE) ; assign(\"x\", 1) ; h <- function() { x } ; h() } ; f() }");
        assertEval("{ x <- 3 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x } ; h() } ; f() } ; g() }");
        assertEval("{ x <- 3 ; gg <- function() {  g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x } ; h() } ; f() } ; g() } ; gg() }");
        assertEval("{ h <- function() { x <- 2 ; f <- function() { if (FALSE) { x <- 1 } ; g <- function() { x } ; g() } ; f() } ; h() }");
        assertEval("{ f <- function() { assign(\"x\", 3) ; g <- function() { x } ; g() } ; x <- 10 ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 3) ; h <- function() { assign(\"z\", 4) ; g <- function() { x } ; g() } ; h() } ; x <- 10 ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 3) ; h <- function() { g <- function() { x } ; g() } ; h() } ; x <- 10 ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 1) ; g <- function() { assign(\"z\", 2) ; x } ; g() } ; f() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { assign(\"z\", 2) ; x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; h() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { assign(\"x\", 5) ; x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; h() }");
        assertEval("{ x <- 10 ; g <- function() { x <- 100 ; z <- 2 ; f <- function() { assign(\"z\", 1); x <- x ; x } ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 ; y <- 3} ; f <- function() { if (FALSE) { x } ; assign(\"y\", 2) ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; h <- function() { exists(\"x\") } ; h() }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"y\", 2) ; h <- function() { exists(\"x\") } ; h() }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; gg <- function() { h <- function() { exists(\"x\") } ; h() } ; gg() } ; f() } ; g() }");
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function() { x } } ; f1 <- f(1) ; f2 <- f(2) ; f1() }");
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function() { x } } ; f1 <- f(1) ; f2 <- f(2) ; f2() ; f1() }");
        assertEval("{ f <- function() { x <- 2 ; g <- function() { if (FALSE) { x <- 2 } ; assign(\"x\", 1, inherits=TRUE) } ; g() ; x } ; f() }");
        assertEval("{ h <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; g <- function() { assign(\"z\", 3) ; if (FALSE) { x <- 4 } ;  f <- function() { exists(\"x\") } ; f() } ; g() } ; h() }");
        assertEval("{ f <- function(x) { assign(x, 23) ; exists(x) } ; c(f(\"a\"),f(\"b\")) }");
        assertEval("{ f <- function() { x <- 2 ; get(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { get(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { x <- 2 ; get(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { x <- 2; h <- function() {  get(\"x\") }  ; h() } ; f() }");
        assertEval("{ f <- function() { g <- function() { get(\"x\", inherits=TRUE) } ; g() } ; x <- 3 ; f() }");
        assertEval("{ f <- function() { assign(\"z\", 2) ; g <- function() { get(\"x\", inherits=TRUE) } ; g() } ; x <- 3 ; f() }");
        assertEval("{ f <- function() { x <- 22 ; get(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ x <- 33 ; f <- function() { assign(\"x\", 44) ; get(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; gg <- function() { h <- function() { get(\"x\") } ; h() } ; gg() } ; f() } ; g() }");

        // lookup with function matching
        assertEval("{ x <- function(){3} ; f <- function() { assign(\"x\", function(){4}) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x() } ; h() } ; f() }");
        assertEval("{ f <- function() { assign(\"x\", function(){2}, inherits=TRUE) ; assign(\"x\", function(){1}) ; h <- function() { x() } ; h() } ; f() }");
        assertEval("{ x <- function(){3} ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() }");
        assertEval("{ x <- function(){3} ; gg <- function() {  g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() } ; gg() }");
        assertEval("{ h <- function() { x <- function(){2} ; f <- function() { if (FALSE) { x <- 1 } ; g <- function() { x } ; g() } ; f() } ; z <- h() ; z() }");
        assertEval("{ h <- function() { g <- function() {4} ; f <- function() { if (FALSE) { g <- 4 } ; g() } ; f() } ; h() }");
        assertEval("{ h <- function() { assign(\"f\", function() {4}) ; f() } ; h() }");
        assertEval("{ f <- function() { 4 } ; h <- function() { assign(\"f\", 5) ; f() } ; h() }");
        assertEval("{ f <- function() { 4 } ; h <- function() { assign(\"z\", 5) ; f() } ; h() }");
        assertEval("{ gg <- function() {  assign(\"x\", function(){11}) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() } ; gg() }");
        assertEval("{ x <- function(){3} ; gg <- function() { assign(\"x\", 4) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() } ; gg() }");
        assertEval("{ h <- function() { x <- function() {3} ; g <- function() { assign(\"z\", 2) ; x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; z <- h() ; z() }");
        assertEval("{ h <- function() { x <- function() {3} ; g <- function() { assign(\"x\", function() {5} ) ; x() } ; g() } ; h() }");
        assertEval("{ h <- function() { z <- 3 ; x <- function() {3} ; g <- function() { x <- 1 ; assign(\"z\", 5) ; x() } ; g() } ; h() }");
        assertEval("{ h <- function() { x <- function() {3} ; gg <- function() { assign(\"x\", 5) ; g <- function() { x() } ; g() } ; gg() } ; h() }");
        assertEval("{ h <- function() { z <- 2 ; x <- function() {3} ; gg <- function() { assign(\"z\", 5) ; g <- function() { x() } ; g() } ; gg() } ; h() }");
        assertEval("{ h <- function() { x <- function() {3} ; g <- function() { assign(\"x\", function() {4}) ; x() } ; g() } ; h() }");
        assertEval("{ h <- function() { z <- 2 ; x <- function() {3} ; g <- function() { assign(\"z\", 1) ; x() } ; g() } ; h() }");
        assertEval("{ x <- function() { 3 } ; h <- function() { if (FALSE) { x <- 2 } ;  z <- 2  ; g <- function() { assign(\"z\", 1) ; x() } ; g() } ; h() }");
        assertEval("{ x <- function() { 3 } ; h <- function() { g <- function() { f <- function() { x <- 1 ; x() } ; f() } ; g() } ; h() }");
        assertEval("{ h <- function() { myfunc <- function(i) { sum(i) } ; g <- function() { myfunc <- 2 ; f <- function() { myfunc(2) } ; f() } ; g() } ; h() }");
        assertEval("{ x <- function() {11} ; g <- function() { f <- function() { assign(\"x\", 2) ; x() } ; f() } ; g() }");
        assertEval("{ x <- function() {3} ; f <- function(i) { if (i == 1) { assign(\"x\", function() {4}) } ; function() { x() } } ; f1 <- f(1) ; f2 <- f(2) ; f1() }");
        assertEval("{ x <- function() {3} ; f <- function(i) { if (i == 1) { assign(\"x\", function() {4}) } ; function() { x() } } ; f1 <- f(1) ; f2 <- f(2) ; f2() ; f1() }");
        assertEval("{ x <- function() {3} ; f <- function(i) { if (i == 1) { assign(\"x\", function() {4}) } ; function() { x() } } ; f1 <- f(1) ; f2 <- f(2) ; f1() ; f2() }");

        // lookup with super assignment
        assertEval("{ fu <- function() { uu <<- 23 } ; fu() ; sort(ls(globalenv())) }");
        assertEval("{ x <- 3 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x ; hh <- function() { x <<- 4 } ; hh() } ; h() } ; f() } ; g() ; x }");
        assertEval("{ f <- function() { x <- 1 ; g <- function() { h <- function() { x <<- 2 } ; h() } ; g() ; x } ; f() }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 } ; f <- function() { assign(\"x\", 4) ; x <<- 3 } ; f() } ; g() ; x }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; h <- function() { if (FALSE) { x <- 1 } ; assign(\"z\", 10) ; f <- function() { assign(\"x\", 4) ; x <<- 3 } ; f() } ; h() } ; g() ; x }");
        assertEval("{ gg <- function() { assign(\"x\", 100) ; g <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; "
                        + "h <- function() { if (FALSE) { x <- 1 } ; assign(\"z\", 10) ; f <- function() { assign(\"x\", 4) ; x <<- 3 } ; f() } ; h() } ; g() } ; x <- 10 ; gg() ; x }");
        assertEval("{ gg <- function() { if (FALSE) { x <- 100 } ; g <- function() { if (FALSE) { x <- 100 } ; h <- function() { f <- function() { x <<- 3 } ; f() } ; h() } ; g() } ; x <- 10 ; gg() ; x }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; h <- function() { assign(\"z\", 10) ; f <- function() { x <<- 3 } ; f() } ; h() } ; g() ; x }");
        assertEval("{ g <- function() { x <- 2 ; z <- 3 ; hh <- function() { assign(\"z\", 2) ; h <- function() { f <- function() { x <<- 3 } ; f() } ; h() } ; hh() } ; x <- 10 ; g() ; x }");
        assertEval("{ g <- function() { x <- 2 ; z <- 3 ; hh <- function() { assign(\"z\", 2) ; h <- function() { assign(\"x\", 1); f <- function() { x <<- 3 } ; f() } ; h() } ; hh() ; x } ; x <- 10 ; g() }");
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function(v) { x <<- v} } ; f1 <- f(1) ; f2 <- f(2) ; f1(10) ; f2(11) ; x }");
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function(v) { x <<- v} } ; f1 <- f(1) ; f2 <- f(2) ; f2(10) ; f1(11) ; x }");
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x <<- 6 } ; h() ; get(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; hh <- function() { if (FALSE) { x <- 100 } ; h <- function() { x <<- 6 } ; h() } ; hh() ; get(\"x\") } ; f() }");

        assertEval("{ assign(\"z\", 10, inherits=TRUE) ; z }");

        // top-level lookups
        assertEval("{ exists(\"sum\", inherits = FALSE) }");
        assertEval("{ x <- 1; exists(\"x\", inherits = FALSE) }");

        assertEval("{ f <- function(z) { exists(\"z\") } ; f(a) }"); // requires promises

        assertEval("{ exists(\"sum\") }");

        // lookup with function matching
        // require lapply
        assertEval(Ignored.Unknown, "{ g <- function() { assign(\"myfunc\", function(i) { sum(i) });  f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
        assertEval("{ myfunc <- function(i) { sum(i) } ; g <- function() { assign(\"z\", 1);  f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
        assertEval(Ignored.Unknown, "{ g <- function() { f <- function() { assign(\"myfunc\", function(i) { sum(i) }); lapply(2, \"myfunc\") } ; f() } ; g() }");
        assertEval(Ignored.Unknown, "{ g <- function() { myfunc <- function(i) { i+i } ; f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");

    }

    @Test
    public void testEnvironment() {
        assertEval("{ h <- new.env() ; assign(\"abc\", \"yes\", h) ; exists(c(\"abc\", \"def\"), h) }");
        assertEval("{ h <- new.env() ; assign(\"abc\", \"yes\", h) ; exists(c(\"def\", \"abc\"), h) }");
        assertEval("{ h <- new.env() ; assign(c(\"a\"), 1, h) ; ls(h) }");
        assertEval("{ h <- new.env() ; assign(c(\"a\"), 1L, h) ; ls(h) }");

        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; assign(\"y\", 2, h) ; ls(h) }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"y\", 1, h) ; assign(\"x\", 2, h) ; sort(ls(h)) }");

        assertEval("{ hh <- new.env() ; assign(\"z\", 3, hh) ; h <- new.env(parent=hh) ; assign(\"y\", 2, h) ; get(\"z\", h) }");

        // hashmaps
        assertEval("{ e<-new.env() ; ls(e) }");
        assertEval("{ e<-new.env() ; assign(\"x\",1,e) ; ls(e) }");
        assertEval("{ e<-new.env() ; assign(\"x\",1,e) ; get(\"x\",e) }");
        assertEval("{ h <- new.env() ; assign(\"x\", 1, h) ; assign(\"x\", 1, h) ; get(\"x\", h) }");
        assertEval("{ h <- new.env() ; assign(\"x\", 1, h) ; assign(\"x\", 2, h) ; get(\"x\", h) }");
        assertEval("{ h <- new.env() ; u <- 1 ; assign(\"x\", u, h) ; assign(\"x\", u, h) ; get(\"x\", h) }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; exists(\"x\", h) }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; exists(\"xx\", h) }");
        assertEval("{ hh <- new.env() ; assign(\"z\", 3, hh) ; h <- new.env(parent=hh) ; assign(\"y\", 2, h) ; exists(\"z\", h) }");
        assertEval("{ ph <- new.env() ; h <- new.env(parent=ph) ; assign(\"x\", 2, ph) ; assign(\"x\", 10, h, inherits=TRUE) ; get(\"x\", ph)}");

        // env queries
        assertEval("{ globalenv() }");
        assertEval("{ emptyenv() }");
        assertEval("{ baseenv() }");

        // ls
        assertEval("{ ls() }");
        assertEval("{ f <- function(x, y) { ls() }; f(1, 2) }");
        assertEval("{ x <- 1; ls(globalenv()) }");
        assertEval("{ x <- 1; .y <- 2; ls(globalenv()) }");

        assertEval("{ is.environment(globalenv()) }");
        assertEval("{ is.environment(1) }");

        // as.environment
        assertEval(Output.ContainsError, "{ as.environment(-1) }");
        assertEval("{ f <- function()  { as.environment(-1) } ; f() }");
        assertEval(Output.ContainsError, "{ as.environment(0) }");
        assertEval("{ as.environment(1) }");
        // GnuR has more packages loaded so can't test in the middle
        assertEval("{ as.environment(length(search())) }");
        assertEval("{ as.environment(length(search()) + 1) }");
        assertEval(Output.ContainsError, "{ as.environment(length(search()) + 2) }");

        assertEval("{ as.environment(\".GlobalEnv\") }");
        assertEval("{ as.environment(\"package:base\") }");

        // parent.env
        assertEval("{ identical(parent.env(baseenv()), emptyenv()) }");
        assertEval("{ e <- new.env(); `parent.env<-`(e, emptyenv()); identical(parent.env(e), emptyenv()) }");

        // environment
        assertEval("{ environment() }");
        assertEval("{ environment(environment) }");

        // environmentName
        assertEval("{ environmentName(baseenv()) }");
        assertEval("{ environmentName(globalenv()) }");
        assertEval("{ environmentName(emptyenv()) }");
        assertEval("{ environmentName(1) }");

        // locking
        assertEval("{ e<-new.env(); environmentIsLocked(e) }");
        assertEval("{ e<-new.env(); lockEnvironment(e); environmentIsLocked(e) }");
        assertEval(Output.ContainsError, "{ e<-new.env(); lockEnvironment(e); assign(\"a\", 1, e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e); assign(\"a\", 2, e) }");
        assertEval(Output.ContainsError, "{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e, TRUE); assign(\"a\", 2, e) }");
        assertEval(Output.ContainsError, "{ e<-new.env(); assign(\"a\", 1, e); lockBinding(\"a\", e); assign(\"a\", 2, e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e, TRUE); unlockBinding(\"a\", e); assign(\"a\", 2, e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e); bindingIsLocked(\"a\", e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e); lockBinding(\"a\", e); bindingIsLocked(\"a\", e) }");

        // rm
        assertEval(Output.ContainsError, "{ rm(\"foo\", envir = baseenv()) }");
        assertEval(Output.ContainsError, "{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e); rm(\"a\",envir = e); }");
        // ok to removed a locked binding
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockBinding(\"a\", e); rm(\"a\",envir = e); ls() }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; rm(\"a\",envir = e); ls() }");

        // get
        assertEval(Output.ContainsError, "{ e<-new.env(); get(\"x\", e) }");
        assertEval("{ e<-new.env(); x<-1; get(\"x\", e) }");
        assertEval("{ e<-new.env(); assign(\"x\", 1, e); get(\"x\", e) }");
        assertEval(Output.ContainsError, "{ e<-new.env(); x<-1; get(\"x\", e, inherits=FALSE) }");
        assertEval(Output.ContainsError, "{ e<-new.env(parent=emptyenv()); x<-1; get(\"x\", e) }");

        // misc
        assertEval(Output.ContainsError, "{ h <- new.env(parent=emptyenv()) ; assign(\"y\", 2, h) ; get(\"z\", h) }");
        assertEval("{ plus <- function(x) { function(y) x + y } ; plus_one <- plus(1) ; ls(environment(plus_one)) }");
        assertEval("{ ls(.GlobalEnv) }");
        assertEval("{ x <- 1 ; ls(.GlobalEnv) }");
        assertEval(Output.ContainsError, "{ ph <- new.env(parent=emptyenv()) ; h <- new.env(parent=ph) ; assign(\"x\", 10, h, inherits=TRUE) ; get(\"x\", ph)}");
        assertEval(Output.ContainsError, "{ ph <- new.env() ; h <- new.env(parent=ph) ; assign(\"x\", 2, h) ; assign(\"x\", 10, h, inherits=TRUE) ; get(\"x\", ph)}");
        assertEval("{ h <- new.env(parent=globalenv()) ; assign(\"x\", 10, h, inherits=TRUE) ; x }");
        assertEval("{ ph <- new.env() ; h <- new.env(parent=ph) ; assign(\"x\", 10, h, inherits=TRUE) ; x }");

        // Requires generic specialization
        assertEval(Ignored.Unknown, Output.ContainsError, "{ as.environment(as.environment) }");
    }

    @Test
    public void testEnvironmentAssignLocked() {
        assertEval(Ignored.Unknown, Output.ContainsError, "{ x <- 1; lockBinding(\"x\", globalenv()); x <- 1 }");
    }

    @Test
    public void testFrames() {
        assertEval("{ t1 <- function() {  aa <- 1; t2 <- function() { cat(\"current frame is\", sys.nframe(), \"; \"); cat(\"parents are frame numbers\", sys.parents(), \"; \"); print(ls(envir = sys.frame(-1))) };  t2() }; t1() }");
    }

    @Test
    public void testAttach() {
        // tests must leave the search path unmodified
        assertEval("{ e <- new.env(); assign(\"x\", 1, e); attach(e, name = \"mine\"); r <- x; detach(\"mine\"); r }");
        assertEval("{ e <- new.env(); assign(\"x\", \"abc\", e); attach(e, 2); r <- x; detach(2); r }");
        assertEval("{ attach(.Platform, 2); r <- file.sep; detach(2); r }");
        assertEval("{ e <- new.env(); assign(\"x\", 1, e); attach(e, 2); r <- x; detach(2); r }");
        assertEval(Output.ContainsError, "{ e <- new.env(); assign(\"x\", 1, e); attach(e, 2); x; detach(2); x }");
        assertEval(Output.ContainsError, "{ detach(\"missing\"); x }");
    }

    @Test
    public void testTranspose() {
        assertEval("{ m <- matrix(1:49, nrow=7) ; sum(m * t(m)) }");
        assertEval("{ m <- matrix(1:81, nrow=9) ; sum(m * t(m)) }");
        assertEval("{ m <- matrix(-5000:4999, nrow=100) ; sum(m * t(m)) }");
        assertEval("{ m <- matrix(c(rep(1:10,100200),100L), nrow=1001) ; sum(m * t(m)) }");

        assertEval("{ m <- double() ; dim(m) <- c(0,4) ; t(m) }");

        assertEval("{ t(1:3) }");
        assertEval("{ t(t(t(1:3))) }");
        assertEval("{ t(matrix(1:6, nrow=2)) }");
        assertEval("{ t(t(matrix(1:6, nrow=2))) }");
        assertEval("{ t(matrix(1:4, nrow=2)) }");
        assertEval("{ t(t(matrix(1:4, nrow=2))) }");

        assertEval("{ x<-matrix(1:2, ncol=2, dimnames=list(\"a\", c(\"b\", \"c\"))); t(x) }");
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
    public void testAttributes() {
        assertEval("{ x <- 1; attributes(x) }");
        assertEval("{ x <- 1; names(x) <- \"hello\" ; attributes(x) }");
        assertEval("{ x <- 1:3 ; attr(x, \"myatt\") <- 2:4 ; attributes(x) }");
        assertEval("{ x <- 1:3 ; attr(x, \"myatt\") <- 2:4 ; attr(x, \"myatt1\") <- \"hello\" ; attributes(x) }");
        assertEval("{ x <- 1:3 ; attr(x, \"myatt\") <- 2:4 ; y <- x; attr(x, \"myatt1\") <- \"hello\" ; attributes(y) }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"myatt\") <- 2:4 ; y <- x; attr(x, \"myatt1\") <- \"hello\" ; attributes(y) }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"names\") }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"na\") }");
        assertEval("{ x <- c(a=1, b=2) ; attr(x, \"mya\") <- 1; attr(x, \"b\") <- 2; attr(x, \"m\") }");
        assertEval("{ x <- 1:2; attr(x, \"aa\") <- 1 ; attr(x, \"ab\") <- 2; attr(x, \"bb\") <- 3; attr(x, \"b\") }");
        assertEval("{ z <- 1; attr(z,\"a\") <- 1; attr(z,\"b\") <- 2; attr(z,\"c\") <- 3 ; attr(z,\"b\") <- NULL ; z }");

        assertEval("{ x <- 1 ; attributes(x) <- list(hi=3, hello=2) ; x }");
        assertEval("{ x <- 1 ; attributes(x) <- list(hi=3, names=\"name\") ; x }");
        assertEval("{ x <- c(hello=1) ; attributes(x) <- list(names=NULL) ; x }");
        assertEval(Output.ContainsError, "{ x <- c(hello=1) ; attributes(x) <- list(hi = 1, 2) ; x }");
        assertEval(Output.ContainsError, "{ x <- c(hello=1) ; attributes(x) <- list(1, hi = 2) ; x }");
        assertEval(Output.ContainsError, "{ x <- c(hello=1) ; attributes(x) <- list(ho = 1, 2, 3) ; x }");
        assertEval(Output.ContainsError, "{ x <- c(hello=1) ; attributes(x) <- list(1, hi = 2, 3) ; x }");
        assertEval(Output.ContainsError, "{ x <- c(hello=1) ; y<-list(1,2); names(y)<-c(\"hi\", \"\"); attributes(x)<-y; x }");
        assertEval("{ x <- 1; attributes(x) <- list(my = 1) ; y <- x; attributes(y) <- list(his = 2) ; x }");
        assertEval("{ x <- c(hello=1) ; attributes(x) <- list(hi=1) ;  attributes(x) <- NULL ; x }");
        assertEval("{ x <- c(hello=1) ; attributes(x) <- list(hi=1, names=NULL, hello=3, hi=2, hello=NULL) ; x }");
        // dimensions are set first even though they are set to NULL (names are preserved even
        // though they are second on the list)
        assertEval("{ x<-1; attributes(x)<-list(names=\"c\", dim=NULL); attributes(x) }");

        assertEval("{ e <- new.env(); attributes(e) <- list(a=1); attributes(e) }");
        assertEval("{ e <- new.env(); attributes(e) <- list(class=\"srcfile\"); attributes(e) }");
    }

    @Test
    public void testUnlist() {
        assertEval("{ unlist(list(\"hello\", \"hi\")) }");

        assertEval("{ unlist(list(a=\"hello\", b=\"hi\")) }");
        assertEval("{ x <- list(a=1,b=2:3,list(x=FALSE)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ x <- list(1,z=list(1,b=22,3)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ x <- list(1,z=list(1,b=22,3)) ; unlist(x, recursive=FALSE, use.names=FALSE) }");

        assertEval("{ x <- list(a=1,b=c(x=2, z=3),list(x=FALSE)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ y<-c(2, 3); names(y)<-c(\"z\", NA); x <- list(a=1,b=y,list(x=FALSE)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ x <- list(a=1,b=c(x=2, 3),list(x=FALSE)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ unlist(list(a=1, c(b=2,c=3))) }");
        assertEval("{ unlist(list(a=1, c(2,3))) }");
        assertEval("{ unlist(list(a=1, c(2,3), d=4)) }");
        assertEval("{ unlist(list(a=1, c(2,3), 4)) }");
        assertEval("{ unlist(list(1+1i, c(7+7i,42+42i))) }");
        assertEval("{ unlist(list(1+1i, list(7+7i,42+42i)), recursive=FALSE) }");
        assertEval("{ unlist(list(1+1i, c(7,42))) }");
        assertEval("{ unlist(list(1+1i, list(7,42)), recursive=FALSE) }");

        assertEval("{ unlist(list(a=1,b=2, c=list(d=3,e=list(f=7))), recursive=TRUE) }");
        assertEval("{ unlist(list(a=1,b=2, c=list(d=3,list(f=7)))) }");
        assertEval("{ x <- list(list(\"1\",\"2\",b=\"3\",\"4\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",list(\"3\", \"4\"),\"5\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\"))) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\", \"4\"))) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\", \"4\"),\"5\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=c(\"3\", \"4\"),\"5\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\", list(\"10\"), \"4\"),\"5\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\", list(\"10\", \"11\"), \"4\"),\"5\")) ; unlist(x) }");

        assertEval("{ names(unlist(list(list(list(\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(list(\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(list(\"1\",\"2\"))))) }");

        assertEval("{ unlist(list(a=list(\"0\", list(\"1\")))) }");
        assertEval("{ unlist(list(a=list(b=list(\"1\")))) }");

        assertEval("{ unlist(list(a=list(\"0\", b=list(\"1\")))) }");
        assertEval("{ unlist(list(a=list(b=list(\"1\"), \"2\"))) }");

        assertEval("{ unlist(list(a=list(\"0\", b=list(\"1\"), \"2\"))) }");
        assertEval("{ unlist(list(a=list(\"0\", list(b=list(\"1\"))))) }");

        assertEval("{ unlist(list(a=list(\"-1\", \"0\", b=list(\"1\")))) }");
        assertEval("{ unlist(list(a=list(b=list(\"1\"), \"2\", \"3\"))) }");

        assertEval("{ names(unlist(list(list(b=list(\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(b=list(\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(b=list(\"1\", \"2\"))))) }");

        assertEval("{ names(unlist(list(list(list(c=\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(list(c=\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(list(c=\"1\", d=\"2\"))))) }");

        assertEval("{ unlist(list()) }");

        assertEval("{ x <- list(\"a\", c(\"b\", \"c\"), list(\"d\", list(\"e\"))) ; unlist(x) }");
        assertEval("{ x <- list(NULL, list(\"d\", list(), character())) ; unlist(x) }");

        assertEval("{ x <- list(a=list(\"1\",\"2\",b=\"3\",\"4\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(1,FALSE,b=list(2:4))) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",FALSE,b=list(2:4))) ; unlist(x) }");

        assertEval("{ x <- list(1,list(2,3),4) ; z <- list(x,x) ; u <- list(z,z) ; u[[c(2,2,3)]] <- 6 ; unlist(u) }");
    }

    @Test
    public void testOther() {
        assertEval("{ rev.mine <- function(x) { if (length(x)) x[length(x):1L] else x } ; rev.mine(1:3) }");
    }

    @Test
    public void testAperm() {
        // default argument for permutation is transpose
        assertEval("{ a = array(1:4,c(2,2)); b = aperm(a); c(a[1,1] == b[1,1], a[1,2] == b[2,1], a[2,1] == b[1,2], a[2,2] == b[2,2]) }");

        // default for resize is true
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a); c(dim(b)[1],dim(b)[2],dim(b)[3]) }");

        // no resize does not change the dimensions
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a, c(3,2,1), resize=FALSE); c(dim(b)[1],dim(b)[2],dim(b)[3]) }");

        // correct structure with resize
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a, c(2,3,1)); a[1,2,3] == b[2,3,1] }");

        // correct structure on cubic array
        assertEval("{ a = array(1:24,c(3,3,3)); b = aperm(a, c(2,3,1)); c(a[1,2,3] == b[2,3,1], a[2,3,1] == b[3,1,2], a[3,1,2] == b[1,2,3]) }");

        // correct structure on cubic array with no resize
        assertEval("{ a = array(1:24,c(3,3,3)); b = aperm(a, c(2,3,1), resize = FALSE); c(a[1,2,3] == b[2,3,1], a[2,3,1] == b[3,1,2], a[3,1,2] == b[1,2,3]) }");

        // correct structure without resize
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a, c(2,3,1), resize = FALSE); a[1,2,3] == b[2,1,2] }");

        // no resize does not change the dimensions
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a,, resize=FALSE); c(dim(b)[1],dim(b)[2],dim(b)[3]) }");

        assertEval("{ aperm(array(c(TRUE, FALSE, TRUE, TRUE, FALSE), c(2, 5, 2))) }");
        assertEval("{ aperm(array(c('FASTR', 'IS', 'SO', 'FAST'), c(3,1,2))) }");

        // perm specified in complex numbers produces warning
        assertEval(Output.ContainsWarning, "{ aperm(array(1:27,c(3,3,3)), c(1+1i,3+3i,2+2i))[1,2,3] == array(1:27,c(3,3,3))[1,3,2]; }");

        // perm is not a permutation vector
        assertEval(Output.ContainsError, "{ aperm(array(1,c( 3,3,3)), c(1,2,1)); }");

        // perm value out of bounds
        assertEval(Output.ContainsError, "{ aperm(array(1,c(3,3,3)), c(1,2,0)); }");

        // first argument not an array
        assertEval(Output.ContainsError, "{ aperm(c(1,2,3)); }");

        // Invalid first argument, not array
        assertEval(Output.ContainsError, "{ aperm(c(c(2,3), c(4,5), c(6,7)), c(3,4)) }");

        // invalid perm length
        assertEval(Output.ContainsError, "{ aperm(array(1,c(3,3,3)), c(1,2)); }");

        // Complex Vector
        assertEval("{ aperm(array(c(3+2i, 5+0i, 1+3i, 5-3i), c(2,2,2))) }");
    }

    @Test
    public void testRecall() {
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*Recall(i-1) } ; f(10) }");
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*Recall(i-1) } ; g <- f ; f <- sum ; g(10) }");
        assertEval("{ f<-function(i) { if (i==1) { 1 } else if (i==2) { 1 } else { Recall(i-1) + Recall(i-2) } } ; f(10) }");
        assertEval(Output.ContainsError, "{ Recall(10) }");
        // Recall with more then 1 argument
        assertEval("{ f <- function(tarDepth,curDepth) { if (tarDepth == curDepth) {curDepth} else {Recall(tarDepth,curDepth+1)}}; f(3,0) }");
    }

    @Test
    public void testCrossprod() {
        assertEval("{ x <- matrix(c(0.368962955428, 0.977400955511, 0.5002433417831, 0.0664379808586, 0.6384031679481, 0.4481831840239), nrow=2); crossprod(x) }");
        assertEval("{ x <- 1:6 ; crossprod(x) }");
        assertEval("{ x <- 1:2 ; crossprod(t(x)) }");
        assertEval("{ crossprod(1:3, matrix(1:6, ncol=2)) }");
        assertEval("{ crossprod(t(1:2), 5) }");
        assertEval("{ crossprod(c(1,NA,2), matrix(1:6, ncol=2)) }");
        assertEval("{ x <- matrix(c(NaN,2,3,4,5,NA), nrow=3); crossprod(x) }");

        assertEval(Ignored.Unknown, "{ x <- matrix(c(NaN,2+3i,3,4+1i,5,NA), nrow=3); crossprod(x) }");
    }

    @Test
    public void testSort() {
        assertEval("{ sort(c(1L,10L,2L)) }");
        assertEval("{ sort(c(3,10,2)) }");
        assertEval("{ sort(c(1,2,0/0,NA)) }");
        assertEval("{ sort(c(2,1,0/0,NA), na.last=NA) }");
        assertEval("{ sort(c(3,NA,0/0,2), na.last=FALSE) }");
        assertEval("{ sort(c(a=NA,b=NA,c=3,d=1),na.last=TRUE, decreasing=TRUE) }");
        assertEval("{ sort(c(a=NA,b=NA,c=3,d=1),na.last=FALSE, decreasing=FALSE) }");
        assertEval("{ sort(c(a=0/0,b=1/0,c=3,d=NA),na.last=TRUE, decreasing=FALSE) }");
        assertEval("{ sort(c(a=NA,b=NA,c=3L,d=-1L),na.last=TRUE, decreasing=FALSE) }");
        assertEval("{ sort(c(3,NA,1,d=10), decreasing=FALSE, index.return=TRUE) }");
        assertEval("{ sort(3:1, index.return=TRUE) }");
        assertEval("{ sort(c(1L,10L,2L), method=\"quick\") }");
        assertEval("{ sort(c(\"abc\", \"aba\", \"aa\")) }");
    }

    @Test
    public void testCbind() {
        assertEval("{ cbind() }");
        assertEval("{ cbind(1:3,2) }");
        assertEval("{ cbind(1:3,1:3) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; cbind(11:12, m) }");

        assertEval("{ cbind(c(1,2)) }");
        assertEval("{ cbind(a=c(b=1,c=2)) }");
        assertEval("{ cbind(c(b=1,c=2)) }");
        assertEval("{ cbind(c(1,c=2)) }");
        assertEval("{ v<-c(b=1, c=2); cbind(v) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y')))) }");

        assertEval("{ cbind(a=c(1,2), b=c(3,4)) }");
        assertEval("{ cbind(a=c(x=1,y=2), b=c(3,4)) }");
        assertEval("{ cbind(a=c(1,2), b=c(x=3,y=4)) }");
        assertEval("{ cbind(a=c(x=1,2), b=c(3,y=4)) }");
        assertEval("{ cbind(a=c(1,2), b=c(3,y=4)) }");
        assertEval("{ cbind(a=c(1,x=2), b=c(y=3,4,5,6)) }");
        assertEval("{ cbind(a=c(1,x=2), b=c(3,4,5,6)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y'))), z=c(8,9)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y'))), c(8,9)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), NULL)), z=c(8,9)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(NULL, c('x', 'y'))), c(m=8,n=9)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2), z=c(m=8,n=9)) }");

        assertEval("{ cbind(list(1,2), TRUE, \"a\") }");
        assertEval(Output.ContainsWarning, "{ cbind(1:3,1:2) }");
        assertEval("{ cbind(2,3, complex(3,3,2));}");
        assertEval("{ cbind(2,3, c(1,1,1)) }");
        assertEval("{ cbind(2.1:10,32.2) }");

        assertEval("{ x<-list(a=7, b=NULL, c=42); y<-as.data.frame(do.call(cbind,x)); y }");
    }

    @Test
    public void testRbind() {
        assertEval("{ rbind(1.1:3.3,1.1:3.3) }");
        assertEval("{ rbind() }");
        assertEval("{ rbind(1:3,2) }");
        assertEval("{ rbind(1:3,1:3) }");
        assertEval("{ m <- matrix(1:6, ncol=2) ; rbind(m, 11:12) }");
        assertEval("{ m <- matrix(1:6, ncol=2) ; rbind(11:12, m) }");
        assertEval(Output.ContainsWarning, "{ m <- matrix(1:6, nrow=2) ; rbind(11:12, m) }");

        assertEval("{ rbind(c(1,2)) }");
        assertEval("{ rbind(a=c(b=1,c=2)) }");
        assertEval("{ rbind(c(b=1,c=2)) }");
        assertEval("{ rbind(c(1,c=2)) }");
        assertEval("{ v<-c(b=1, c=2); rbind(v) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y')))) }");

        assertEval("{ rbind(a=c(1,2), b=c(3,4)) }");
        assertEval("{ rbind(a=c(x=1,y=2), b=c(3,4)) }");
        assertEval("{ rbind(a=c(1,2), b=c(x=3,y=4)) }");
        assertEval("{ rbind(a=c(x=1,2), b=c(3,y=4)) }");
        assertEval("{ rbind(a=c(1,2), b=c(3,y=4)) }");
        assertEval("{ rbind(a=c(1,x=2), b=c(y=3,4,5,6)) }");
        assertEval("{ rbind(a=c(1,x=2), b=c(3,4,5,6)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y'))), z=c(8,9)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y'))), c(8,9)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), NULL)), z=c(8,9)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(NULL, c('x', 'y'))), c(m=8,n=9)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2), z=c(m=8,n=9)) }");

        assertEval("{ info <- c(\"print\", \"AES\", \"print.AES\") ; ns <- integer(0) ; rbind(info, ns) }");

        assertEval("{ x<-list(a=7, b=NULL, c=42); y<-as.data.frame(do.call(rbind,x)); y }");
    }

    @Test
    public void testRank() {
        assertEval(Ignored.Unknown, "{ rank(c(10,100,100,1000)) }");
        assertEval(Ignored.Unknown, "{ rank(c(1000,100,100,100, 10)) }");
        assertEval(Ignored.Unknown, "{ rank(c(a=2,b=1,c=3,40)) }");
        assertEval(Ignored.Unknown, "{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=NA) }");
        assertEval(Ignored.Unknown, "{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=\"keep\") }");
        assertEval(Ignored.Unknown, "{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=TRUE) }");
        assertEval(Ignored.Unknown, "{ rank(c(a=2,b=1,c=3,d=NA,e=40), na.last=FALSE) }");
        assertEval(Ignored.Unknown, "{ rank(c(a=1,b=1,c=3,d=NA,e=3), na.last=FALSE, ties.method=\"max\") }");
        assertEval(Ignored.Unknown, "{ rank(c(a=1,b=1,c=3,d=NA,e=3), na.last=NA, ties.method=\"min\") }");
        assertEval("{ rank(c(1000, 100, 100, NA, 1, 20), ties.method=\"first\") }");
    }

    @Test
    public void testDet() {
        assertEval("{ det(matrix(c(1,2,4,5),nrow=2)) }");
        assertEval("{ det(matrix(c(1,-3,4,-5),nrow=2)) }");
        assertEval(Ignored.Unknown, "{ det(matrix(c(1,0,4,NA),nrow=2)) }");
    }

    @Test
    public void testChol() {
        assertEval("{ chol(1) }");
        assertEval("{ round( chol(10), digits=5) }");
        assertEval("{ m <- matrix(c(5,1,1,3),2) ; round( chol(m), digits=5 ) }");
        assertEval(Ignored.Unknown, Output.ContainsError, "{ m <- matrix(c(5,-5,-5,3),2,2) ; chol(m) }");
    }

    @Test
    public void testQr() {
        assertEval("{ qr(matrix(1:6,nrow=2), LAPACK=FALSE)$pivot }");
        assertEval("{ qr(matrix(1:6,nrow=2), LAPACK=FALSE)$rank }");
        assertEval("{ round( qr(matrix(1:6,nrow=2), LAPACK=FALSE)$qraux, digits=5 ) }");
        assertEval("{ round( qr(matrix(c(3,2,-3,-4),nrow=2), LAPACK=FALSE)$qr, digits=5 ) }");

        assertEval("{ m <- matrix(c(1,0,0,0,1,0,0,0,1),nrow=3) ; x <- qr(m, LAPACK=FALSE) ; qr.coef(x, 1:3) }");
        assertEval("{ x <- qr(cbind(1:3,2:4), LAPACK=FALSE) ; round( qr.coef(x, 1:3), digits=5 ) }");

        assertEval(Ignored.Unknown, "{ x <- qr(t(cbind(1:10,2:11)), LAPACK=TRUE) ; qr.coef(x, 1:2) }");
        assertEval("{ qr(10, LAPACK=TRUE) }");
        assertEval(Ignored.Unknown, "{ round( qr(matrix(1:6,nrow=2), LAPACK=TRUE)$qr, digits=5) }");

        // qr.coef
        assertEval(Ignored.Unknown, Output.ContainsError, "{ x <- qr(cbind(1:10,2:11), LAPACK=TRUE) ; qr.coef(x, 1:2) }");
        assertEval(Ignored.Unknown, " { x <- qr(cbind(1:10,2:11), LAPACK=TRUE) ; round( qr.coef(x, 1:10), digits=5 ) }");
        assertEval(Ignored.Unknown, "{ x <- qr(c(3,1,2), LAPACK=TRUE) ; round( qr.coef(x, c(1,3,2)), digits=5 ) }");
        // FIXME: GNU-R will print negative zero as zero
        assertEval("{ x <- qr(t(cbind(1:10,2:11)), LAPACK=FALSE) ; qr.coef(x, 1:2) }");
        assertEval(Ignored.Unknown, "{ x <- qr(c(3,1,2), LAPACK=FALSE) ; round( qr.coef(x, c(1,3,2)), digits=5 ) }");

        // qr.solve
        assertEval(Ignored.Unknown, "{ round( qr.solve(qr(c(1,3,4,2)), c(1,2,3,4)), digits=5 ) }");
        assertEval(Ignored.Unknown, "{ round( qr.solve(c(1,3,4,2), c(1,2,3,4)), digits=5) }");
    }

    @Test
    public void testComplex() {
        assertEval("{ complex(real=1,imaginary=2) }");
        assertEval("{ complex(real=1,imag=2) }");
        assertEval("{ complex(3) }");
        assertEval("{ complex(3, c(1,2,3), c(4,5,6)) }");
        assertEval("{ complex(3, c(1,2,3), c(4,5)) }");
        assertEval("{ complex(3, c(1,2), c(4,5,6)) }");

        assertEval(Ignored.Unknown, "{ x <- 1:2 ; attr(x,\"my\") <- 2 ; Im(x) }");
        assertEval(Ignored.Unknown, "{ x <- c(1+2i,3-4i) ; attr(x,\"my\") <- 2 ; Im(x) }");
        assertEval(Ignored.Unknown, "{ x <- 1:2 ; attr(x,\"my\") <- 2 ; Re(x) }");
        assertEval(Ignored.Unknown, "{ x <- c(1+2i,3-4i) ; attr(x,\"my\") <- 2 ; Re(x) }");
    }

    @Test
    public void testReIm() {
        assertEval("{ Re(1+1i) }");
        assertEval("{ Im(1+1i) }");
        assertEval("{ Re(1) }");
        assertEval("{ Im(1) }");
        assertEval("{ Re(c(1+1i,2-2i)) }");
        assertEval("{ Im(c(1+1i,2-2i)) }");
        assertEval("{ Re(c(1,2)) }");
        assertEval("{ Im(c(1,2)) }");
        assertEval("{ Re(as.double(NA)) }");
        assertEval("{ Im(as.double(NA)) }");
        assertEval("{ Re(c(1,NA,2)) }");
        assertEval("{ Im(c(1,NA,2)) }");
        assertEval("{ Re(NA+2i) }");
        assertEval("{ Im(NA+2i) }");
    }

    @Test
    public void testMod() {
        assertEval("{ round(Mod(1+1i)*10000) }");
    }

    @Test
    public void testRound() {
        assertEval("{ round(0.4) }");
        assertEval("{ round(0.5) }");
        assertEval("{ round(0.6) }");
        assertEval("{ round(1.5) }");
        assertEval("{ round(-1.5) }");
        assertEval("{ round(1L) }");
        assertEval("{ round(1/0) }");
        assertEval("{ round(c(0,0.2,0.4,0.6,0.8,1)) }");

        assertEval(Ignored.Unknown, "{ round(1.123456,digit=2.8) }");
    }

    @Test
    public void testSignif() {
        assertEval("{ signif(0.555, 2) }");
        assertEval("{ signif(0.5549, 2) }");
        assertEval("{ signif(0.5551, 2) }");
        assertEval("{ signif(0.555, 0) }");
        assertEval("{ signif(0.555, -1) }");
        assertEval("{ signif(0.0005551, 2) }");
    }

    @Test
    public void testLgamma() {
        assertEval("{ lgamma(1) }");
        assertEval("{ lgamma(100) }");
        assertEval("{ lgamma(7.42) }");
        assertEval("{ lgamma(as.double(NA)) }");
        assertEval("{ lgamma(c(100, 2.2)) }");
        assertEval("{ lgamma(FALSE) }");
        assertEval(Output.ContainsError, "{ lgamma(as.raw(1)) }");
        assertEval(Output.ContainsError, "{ lgamma(1+1i) }");
    }

    @Test
    public void testDiGamma() {
        assertEval("{ digamma(1) }");
        assertEval("{ digamma(100) }");
        assertEval("{ digamma(7.42) }");
        assertEval("{ digamma(as.double(NA)) }");
        assertEval("{ digamma(c(100, 2.2)) }");
        assertEval(Output.ContainsWarning, "{ digamma(FALSE) }");
        assertEval(Output.ContainsError, "{ digamma(as.raw(1)) }");
        assertEval(Output.ContainsError, "{ digamma(1+1i) }");
    }

    @Test
    public void testDelayedAssign() {
        assertEval("{ delayedAssign(\"x\", y); y <- 10; x }");
        assertEval("{ delayedAssign(\"x\", a+b); a <- 1 ; b <- 3 ; x }");
        assertEval("{ f <- function() { delayedAssign(\"x\", y); y <- 10; x  } ; f() }");
        assertEval("{ delayedAssign(\"x\", y); delayedAssign(\"y\", x) ; x }");
        assertEval("{ f <- function() { delayedAssign(\"x\", y); delayedAssign(\"y\", x) ; x } ; f() }");
        assertEval("{ f <- function() { delayedAssign(\"x\", 3); delayedAssign(\"x\", 2); x } ; f() }");
        assertEval("{ f <- function(...) { delayedAssign(\"x\", ..1) ; y <<- x } ; f(10) ; y }");
        assertEval("{ f <- function() print (\"outer\");  g <- function() { delayedAssign(\"f\", 1); f() }; g()}");
        assertEval("{ f <- function() { delayedAssign(\"x\",y); delayedAssign(\"y\",x); g(x, y)}; g <- function(x, y) { x + y }; f() }");
        assertEval("{ f <- function() { delayedAssign(\"x\",y); delayedAssign(\"y\",x); list(x, y)}; f() }");
        assertEval("{ f <- function() { delayedAssign(\"x\",y); delayedAssign(\"y\",x); paste(x, y)}; f() }");
        assertEval("{ f <- function() { delayedAssign(\"x\",y); delayedAssign(\"y\",x); print(x, y)}; f() }");
        assertEval("{ f <- function() { p <- 0; for (i in 1:10) { if (i %% 2 == 0) { delayedAssign(\"a\", p + 1); } else { a <- p + 1; }; p <- a; }; p }; f() }");
        assertEval("{ f <- function() { x <- 4 ; delayedAssign(\"x\", y); y <- 10; x  } ; f() }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; delayedAssign(\"x\", y, h, h) ; assign(\"y\", 2, h) ; get(\"x\", h) }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; delayedAssign(\"x\", y, h, h) ; assign(\"y\", 2, h) ; get(\"x\", h) }");
    }

    @Test
    public void testMissing() {
        assertEval("{ f <- function(a) { g(a) } ;  g <- function(b) { missing(b) } ; f() }");
        assertEval("{ f <- function(a = 2) { g(a) } ; g <- function(b) { missing(b) } ; f() }");
        assertEval("{ f <- function(a,b,c) { missing(b) } ; f(1,,2) }");
        assertEval("{ g <- function(a, b, c) { b } ; f <- function(a,b,c) { g(a,b=2,c) } ; f(1,,2) }"); // not
        // really the builtin, but somewhat related
        assertEval("{ f <- function(x) {print(missing(x)); g(x)}; g <- function(y=2) {print(missing(y)); y}; f(1) }");
        assertEval("{ k <- function(x=2,y) { xx <- x; yy <- y; print(missing(x)); print(missing(xx)); print(missing(yy)); print(missing(yy))}; k(y=1) }");
        assertEval("{ f <- function(a = 2 + 3) { missing(a) } ; f() }");
        assertEval("{ f <- function(a = z) { missing(a) } ; f() }");
        assertEval("{ f <- function(a = 2 + 3) { a;  missing(a) } ; f() }");
        assertEval("{ f <- function(a = z) {  g(a) } ; g <- function(b) { missing(b) } ; f() }");
        assertEval("{ f <- function(x) { missing(x) } ; f(a) }");
        assertEval("{ f <- function(a) { g <- function(b) { before <- missing(b) ; a <<- 2 ; after <- missing(b) ; c(before, after) } ; g(a) } ; f() }");
        assertEval("{ f <- function(...) { g(...) } ;  g <- function(b=2) { missing(b) } ; f() }");

        assertEval("{ f <- function(x) { print(missing(x)); g(x) }; g <- function(y=3) { print(missing(y)); k(y) }; k <- function(l=4) { print(missing(l)); l }; f(1) }");
        assertEval("{ k <- function(x=2,y) { xx <- x; yy <- y; print(missing(x)); print(missing(xx)); print(missing(yy)); print(missing(yy))}; k() }");

        assertEval("{ f <- function(a = z, z) {  g(a) } ; g <- function(b) { missing(b) } ; f() }");
        assertEval("{ f <- function(a) { g(a) } ; g <- function(b=2) { missing(b) } ; f() }");
        assertEval("{ f <- function(x = y, y = x) { g(x, y) } ; g <- function(x, y) { missing(x) } ; f() }");
        assertEval("{ f <- function(...) { missing(..2) } ; f(x + z, a * b) }");

        assertEval("{ f <- function(x) {print(missing(x)); g(x)}; g <- function(y=2) {print(missing(y)); y}; f() }");
        assertEval("{ f <- function(x) { print(missing(x)); g(x) }; g <- function(y=3) { print(missing(y)); k(y) }; k <- function(l=4) { print(missing(l)); l }; f() }");
        assertEval("{ f <- function(x) { print(missing(x)) ; g(x) } ; g <- function(y=1) { print(missing(y)) ; h(y) } ; h <- function(z) { print(missing(z)) ; z } ; f() }");
        assertEval("{ f <- function(a,b,c,d,e,env) (length(objects(env, all.names = TRUE, pattern = \"^[.]__[CTA]_\"))); f2 <- function(env) (length(objects(env, all.names = TRUE, pattern = \"^[.]__[CTA]_\"))); f(); f2() }");
    }

    @Test
    public void testExpression() {
        assertEval("{ f <- function(z) {z}; e<-c(expression(f), 7); eval(e) }");
        assertEval("{ f <- function(z) {z}; e<-expression(f); e2<-c(e, 7); eval(e2) }");

        assertEval("{ x<-expression(1); y<-c(x,2); typeof(y[[2]]) }");
        assertEval("{ class(expression(1)) }");

        assertEval("{ x<-expression(1); typeof(x[[1]]) }");
        assertEval("{ x<-expression(a); typeof(x[[1]]) }");
        assertEval("{ x<-expression(1); y<-c(x,2); typeof(y[[1]]) }");
    }

    @Test
    public void testQuote() {
        assertEval("{ quote(1:3) }");
        assertEval("{ quote(list(1, 2)) }");
        assertEval("{ typeof(quote(1)) }");
        assertEval("{ typeof(quote(x + y)) }");
        assertEval("{ class(quote(x + y)) }");
        assertEval("{ mode(quote(x + y)) }");
        assertEval("{ is.call(quote(x + y)) }");
        assertEval("{ quote(x <- x + 1) }");
        assertEval("{ typeof(quote(x)) }");

        assertEval(Output.ContainsError, "{ l <- quote(a[3] <- 4) ; f <- function() { eval(l) } ; f() }");
        assertEval(Output.ContainsError, "{ l <- quote(a[3] <- 4) ; eval(l) ; f() }");

        assertEval("{ l <- quote(x[1,1] <- 10) ; f <- function() { eval(l) } ; x <- matrix(1:4,nrow=2) ; f() ; x }");
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) } ; x <- 10 ; f() ; x }");
        assertEval("{ l <- quote(x[1] <- 1) ; f <- function() { eval(l) ; x <<- 10 ; get(\"x\") } ; x <- 20 ; f() }");
    }

    @Test
    public void testSubstitute() {
        assertEval("{ f <- function(expr) { substitute(expr) } ; f(a * b) }");
        assertEval("{ f <- function() { delayedAssign(\"expr\", a * b) ; substitute(expr) } ; f() }");
        assertEval("{ f <- function() { delayedAssign(\"expr\", a * b) ; substitute(dummy) } ; f() }");
        assertEval("{ delayedAssign(\"expr\", a * b) ; substitute(expr) }");
        assertEval("{ f <- function(expr) { expr ; substitute(expr) } ; a <- 10; b <- 2; f(a * b) }");
        assertEval("{ f <- function(y) { substitute(y) } ; typeof(f()) }");
        assertEval("{ f <- function(y) { as.character(substitute(y)) } ; f(\"a\") }");
        assertEval("{ f <- function(x) { g <- function() { substitute(x) } ; g() } ;  f(a * b) }");
        assertEval("{ substitute(a, list(a = quote(x + y), x = 1)) }");
        assertEval("{ f <- function(x = y, y = x) { substitute(x) } ; f() }");
        assertEval("{ f <- function(a, b=a, c=b, d=c) { substitute(d) } ; f(x + y) }");
        assertEval("{ f <- function(x) { substitute(x, list(a=1,b=2)) } ; f(a + b) }");

        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...)) } ; f(x + z, a * b) }");
        assertEval("{ f <- function(...) { substitute(list(...)) } ; f(x + z, a * b) }");

        assertEval("{ f<-function(...) { substitute(list(...)) }; is.language(f(c(1,2))) }");
        // language is a list (of sorts)
        assertEval("{ f<-function(...) { substitute(list(...)) }; length(f(c(1,2))) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; is.symbol(f(c(x=1,2))[[1]]) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; is.language(f(c(x=1,2))[[2]]) }");

        assertEval("{ f<-function(...) { substitute(list(...)) }; typeof(f(c(1,2))) }");

        assertEval("{ g<-function() { f<-function() { 42 }; substitute(f()) } ; typeof(g()[[1]]) }");

        assertEval("{ f<-function(...) { substitute(list(...)) }; is.symbol(f(c(x=1,2))[[2]][[1]]) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; is.double(f(c(x=1,2))[[2]][[2]]) }");

        assertEval("{ f <- function() { substitute(list(a=1,b=2,...,3,...)) } ; f() }");
        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...)) } ; f() }");
        assertEval("{ f <- function(...) { substitute(list(a=1,b=2,...,3,...,c=8)) } ; f() }");

        assertEval("{ f<-function(...) { substitute(list(...)) }; f(c(1,2)) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; f(c(x=1, 2)) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; f(c(x=1, 2, z=3)) }");
        assertEval("{ env <- new.env() ; z <- 0 ; delayedAssign(\"var\", z+2, assign.env=env) ; substitute(var, env=env) }");
        assertEval("{ env <- new.env() ; z <- 0 ; delayedAssign(\"var\", z+2, assign.env=env) ; z <- 10 ; substitute(var, env=env) }");

        assertEval("{ substitute(if(a) { x } else { x * a }, list(a = quote(x + y), x = 1)) }");
        assertEval("{ f <- function() { substitute(x(1:10), list(x=quote(sum))) } ; f() }");
        assertEval("{ substitute(x + y, list(x=1)) }");
        assertEval("{ f <- function(expra, exprb) { substitute(expra + exprb) } ; f(a * b, a + b) }");

        assertEval("{ f <- function(z) { g <- function(y) { substitute(y)  } ; g(z) } ; f(a + d) }");
        assertEval("{ substitute(a[x], list(a = quote(x + y), x = 1)) }");

        assertEval(Ignored.Unknown, "{ f <- function(y) { substitute(y) } ; f() }");
        assertEval(Ignored.Unknown, "{ substitute(function(x, a) { x + a }, list(a = quote(x + y), x = 1)) }");
        assertEval(Ignored.Unknown, "{ substitute(x <- x + 1, list(x = 1) }");

        // GNU R generates warning here, but the test has been included nevertheless to make sure
        // that FastR does not crash here
        assertEval("f<-function(..., list=character()) { substitute(list(...))[-1L] }; as.character(f(\"config\"))");
    }

    @Test
    public void testInvocation() {
        assertEval("{ g <- function(...) { max(...) } ; g(1,2) }");
        assertEval("{ f <- function(a, ...) { list(...) } ; f(1) }");

        assertEval(Output.ContainsError, "{ rnorm(n = 1, n = 2) }");
        assertEval(Output.ContainsError, "{ rnorm(s = 1, s = 1) }");
        assertEval(Output.ContainsError, "{ matrix(1:4, n = 2) }");

        assertEval("{ matrix(da=1:3,1) }");

        assertEval("{ f <- function(...) { l <- list(...) ; l[[1]] <- 10; ..1 } ; f(11,12,13) }");
        assertEval("{ g <- function(...) { length(list(...)) } ; f <- function(...) { g(..., ...) } ; f(z = 1, g = 31) }");
        assertEval("{ g <- function(...) { `-`(...) } ; g(1,2) }");
        assertEval("{ f <- function(...) { list(a=1,...) } ; f(b=2,3) }");
        assertEval("{ f <- function(...) { substitute(...) } ; f(x + z) } ");
        assertEval("{ p <- function(prefix, ...) { cat(prefix, ..., \"\\n\") } ; p(\"INFO\", \"msg:\", \"Hello\", 42) }");

        assertEval("{ f <- function(...) { g <- function() { list(...)$a } ; g() } ; f(a=1) }");
        assertEval("{ f <- function(...) { args <- list(...) ; args$name } ; f(name = 42) }");

        assertEval(Ignored.Unknown, Output.ContainsError, "{ matrix(x=1) }");
        assertEval(Ignored.Unknown, "{ round( rnorm(1,), digits = 5 ) }");
        assertEval(Ignored.Unknown, Output.ContainsError, "{ max(1,2,) }");
    }

    @Test
    public void testEval() {
        assertEval("{ eval(2 ^ 2 ^ 3)}");
        assertEval("{ a <- 1; eval(a) }");
        assertEval("{ a <- 1; eval(a + 1) }");
        assertEval("{ a <- 1; eval(expression(a + 1)) }");
        assertEval("{ f <- function(x) { eval(x) }; f(1) }");
        assertEval("{ eval(x <- 1); ls() }");
        assertEval("{ ne <- new.env(); eval(x <- 1, ne); ls() }");
        assertEval("{ ne <- new.env(); evalq(x <- 1, ne); ls(ne) }");
        assertEval("{ ne <- new.env(); evalq(envir=ne, expr=x <- 1); ls(ne) }");
        assertEval("{ e1 <- new.env(); assign(\"x\", 100, e1); e2 <- new.env(parent = e1); evalq(x, e2) }");

        assertEval("{ f <- function(z) {z}; e<-as.call(c(expression(f), 7)); eval(e) }");

        assertEval("{ f<-function(...) { substitute(list(...)) }; eval(f(c(1,2))) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; x<-1; eval(f(c(x,2))) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; eval(f(c(x=1,2))) }");

        assertEval("{ g<-function() { f<-function() { 42 }; substitute(f()) } ; eval(g()) }");
        assertEval("{ g<-function(y) { f<-function(x) { x }; substitute(f(y)) } ; eval(g(42)) }");
        assertEval("{ eval({ xx <- pi; xx^2}) ; xx }");

        // should print two values, xx^2 and xx
        assertEval(Ignored.Unknown, "eval({ xx <- pi; xx^2}) ; xx");
    }

    @Test
    public void testFormals() {
        assertEval("{ f <- function(a) {}; formals(f) }");
        assertEval("{ f <- function(a, b) {}; formals(f) }");
        assertEval("{ f <- function(a, b = c(1, 2)) {}; formals(f) }");
    }

    @Test
    public void testArgs() {
        // Printing doesn't match GnuR, so make the call (should return NULL)
        assertEval("{ f <- function(a) {}; fa <- args(f); fa() }");
        assertEval("{ f <- function(a, b) {}; fa <- args(f); fa() }");
        assertEval("{ sa <- args(sum); fa() }");
    }

    @Test
    public void testLocal() {
        assertEval("{ kk <- local({k <- function(x) {x*2}}); kk(8)}");
        assertEval("{ ne <- new.env(); local(a <- 1, ne); ls(ne) }");
    }

    @Test
    public void testDeparse() {
        assertEval("{ deparse(TRUE) }");
        assertEval("{ deparse(c(T, F)) }");
        assertEval("{ k <- 2 ; deparse(k) }");
        assertEval("{ deparse(round) }");
        assertEval("{ x<-expression(1); deparse(x) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; deparse(f(c(1,2))) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; deparse(f(c(x=1,2))) }");
        assertEval("{ f <- function(x) { deparse(substitute(x)) } ; f(a + b * (c - d)) }");
        assertEval("{ f<-function(x) { deparse(x) }; l<-list(7); f(l) }");
        assertEval("{ f<-function(x) { deparse(x) }; l<-list(7, 42); f(l) }");
        assertEval("{ f<-function(x) { deparse(x) }; l<-list(7, list(42)); f(l) }");
        assertEval("{ deparse(expression(a+b, c+d)) }");

        assertEval(Ignored.Unknown, "{ f <- function() 23 ; deparse(f) }");
        assertEval(Ignored.Unknown, "{ deparse(nrow) }");
        // should deparse as structure(...
        assertEval("{ e <- new.env(); assign(\"a\", 1, e); assign(\"b\", 2, e); le <- as.list(e); deparse(le)}");
    }

    @Test
    public void testDiagnostics() {
        assertEval(Output.ContainsError, "{ f <- function() { stop(\"hello\",\"world\") } ; f() }");
    }

    @Test
    public void testSprintf() {
        assertEval("{ sprintf(\"0x%x\",1L) }");
        assertEval("{ sprintf(\"0x%x\",10L) }");
        assertEval("{ sprintf(\"%d%d\",1L,2L) }");
        assertEval("{ sprintf(\"0x%x\",1) }");
        assertEval("{ sprintf(\"0x%x\",10) }");
        assertEval("{ sprintf(\"%d\", 10) }");
        assertEval("{ sprintf(\"%7.3f\", 10.1) }");
        assertEval("{ sprintf(\"%03d\", 1:3) }");
        assertEval("{ sprintf(\"%3d\", 1:3) }");
        assertEval("{ sprintf(\"%4X\", 26) }");
        assertEval("{ sprintf(\"%04X\", 26) }");
        assertEval("{ sprintf(\"Hello %*d\", 3, 2) }");
        assertEval("{ sprintf(\"Hello %*2$d\", 3, 2) }");
        assertEval("{ sprintf(\"Hello %2$*2$d\", 3, 2) }");
    }

    @Test
    public void testIdentical() {
        assertEval("{ identical(1,1) }");
        assertEval("{ identical(1L,1) }");
        assertEval("{ identical(1:3, c(1L,2L,3L)) }");
        assertEval("{ x <- 1 ; attr(x, \"my\") <- 10; y <- 1 ; attr(y, \"my\") <- 10 ; identical(x,y) }");
        assertEval("{ x <- 1 ; attr(x, \"hello\") <- 2 ; attr(x, \"my\") <- 10;  attr(x, \"hello\") <- NULL ; y <- 1 ; attr(y, \"my\") <- 10 ; identical(x,y) }");

        assertEval(Ignored.Unknown, "{ identical(0/0,1[2]) }");
        assertEval(Ignored.Unknown, "{ identical(list(1, list(2)), list(list(1), 1)) }");
        assertEval(Ignored.Unknown, "{ identical(list(1, list(2)), list(1, list(2))) }");
        assertEval(Ignored.Unknown, "{ x <- 1 ; attr(x, \"my\") <- 10; identical(x, 1) }");
        assertEval(Ignored.Unknown, "{ x <- 1 ; attr(x, \"my\") <- 10; y <- 1 ; attr(y, \"my\") <- 11 ; identical(x,y) }");
    }

    @Test
    public void testWorkingDirectory() {
        assertEval("{ cur <- getwd(); cur1 <- setwd(getwd()) ; cur2 <- getwd() ; cur == cur1 && cur == cur2 }");
        assertEval(Output.ContainsError, "{ setwd(1) }");
        assertEval(Output.ContainsError, "{ setwd(character()) }");
        assertEval("{ cur <- getwd(); cur1 <- setwd(c(cur, \"dummy\")) ; cur2 <- getwd() ; cur == cur1  }");
    }

    @Test
    public void testFileListing() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\") }");
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE) }");
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE, pattern=\".*dummy.*\") }");
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE, pattern=\"dummy\") }");

        assertEval("{ list.files(\"test/r/simple/data/tree1\", pattern=\"*.tx\") }");
    }

    @Test
    public void testAll() {
        assertEval("{ all(TRUE) }");
        assertEval("{ all(TRUE, TRUE, TRUE) }");
        assertEval("{ all() }");
        assertEval("{ all(logical(0)) }");

        assertEval("{ all(TRUE, FALSE) }");
        assertEval("{ all(FALSE) }");

        assertEval("{ all(TRUE, TRUE, NA) }");

        assertEval("{ v <- c(\"abc\", \"def\") ; w <- c(\"abc\", \"def\") ; all(v == w) }");

        assertEval("{ all(TRUE, FALSE, NA,  na.rm=FALSE) }");
        assertEval("{ all(TRUE, FALSE, NA,  na.rm=TRUE) }");
        assertEval("{ all(TRUE, TRUE, NA,  na.rm=FALSE) }");

        assertEval(Ignored.Unknown, "{ all(TRUE, TRUE, NA,  na.rm=TRUE) }");
        // FIXME coercion warning missing
        assertEval(Ignored.Unknown, "{ all(1) }");
        assertEval(Ignored.Unknown, "{ all(0) }");
        assertEval(Ignored.Unknown, "{ all(TRUE,c(TRUE,TRUE),1) }");
        assertEval(Ignored.Unknown, "{ all(TRUE,c(TRUE,TRUE),1,0) }");
    }

    @Test
    public void testAny() {
        assertEval("{ any(TRUE) }");
        assertEval("{ any(TRUE, TRUE, TRUE) }");
        assertEval("{ any(TRUE, FALSE) }");
        assertEval("{ any(TRUE, TRUE, NA) }");

        assertEval("{ any() }");
        assertEval("{ any(logical(0)) }");
        assertEval("{ any(FALSE) }");

        assertEval("{ any(NA, NA, NA) }");
        assertEval("{ any(NA) }");

        assertEval("{ any(TRUE, TRUE, NA,  na.rm=TRUE) }");
        assertEval("{ any(TRUE, FALSE, NA,  na.rm=TRUE) }");
        assertEval("{ any(FALSE, NA,  na.rm=FALSE) }");

        assertEval("{ any(NULL); }");

        assertEval(Ignored.Unknown, "{ any(FALSE, NA,  na.rm=TRUE) }");
        // FIXME coercion warning missing
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ any(1) }");
        // FIXME coercion warning missing
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ any(0) }");
    }

    @Test
    public void testSource() {
        assertEval(Ignored.Unknown, "{ source(\"test/r/simple/data/tree2/setx.r\") ; x }");
        assertEval(Ignored.Unknown, "{ source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x }");
        assertEval(Ignored.Unknown, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x } ; c(f(), x) }");
        assertEval(Ignored.Unknown, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=FALSE) ; x } ; c(f(), x) }");
        assertEval(Ignored.Unknown, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=FALSE) ; x } ; c(f(), x) }");
        assertEval(Ignored.Unknown, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=TRUE) ; x } ; c(f(), x) }");
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
    public void testIsCall() {
        assertEval("{ cl <- call(\"f\") ; is.call(cl) }");
        assertEval("{ cl <- call(\"f\", 2, 3) ; is.call(cl) }");
        assertEval("{ cl <- list(f, 2, 3) ; is.call(cl) }");
        assertEval("{ is.call(call) }");
    }

    @Test
    public void testAsCall() {
        assertEval("{ l <- list(f) ; as.call(l) }");
        assertEval("{ l <- list(f, 2, 3) ; as.call(l) }");
        assertEval("{ g <- function() 23 ; l <- list(f, g()) ; as.call(l) }");
        assertEval("{ f <- round ; g <- as.call(list(f, quote(A))) }");
        assertEval("{ f <- function() 23 ; l <- list(f) ; cl <- as.call(l) ; eval(cl) }");
        assertEval("{ f <- function(a,b) a+b ; l <- list(f,2,3) ; cl <- as.call(l) ; eval(cl) }");
        assertEval("{ f <- function(x) x+19 ; g <- function() 23 ; l <- list(f, g()) ; cl <- as.call(l) ; eval(cl) }");

        assertEval("{ f <- function(x) x ; l <- list(f, 42) ; cl <- as.call(l); typeof(cl[[1]]) }");
        assertEval("{ f <- function(x) x ; l <- list(f, 42) ; cl <- as.call(l); typeof(cl[[2]]) }");
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

        // TODO these fail because the argument name "x" is wrongly always output
        assertEval(Ignored.Unknown, "{ f <- function(x) sys.call() ; f(2) }");
        assertEval(Ignored.Unknown, "{ f <- function(x) sys.call() ; g <- function() 23 ; f(g()) }");
        // fails because can't parse out the "name"
        assertEval(Ignored.Unknown, "{ (function() sys.call())() }");
    }

    @Test
    public void testSysParent() {
        assertEval("{ sys.parent() }");
        assertEval("{ f <- function() sys.parent() ; f() }");
        assertEval("{ f <- function() sys.parent() ; g <- function() f() ; g() }");
        assertEval("{ f <- function() sys.parent() ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x=sys.parent()) x ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=sys.parent()) g(z) ; h() }");
        assertEval("{ u <- function() sys.parent() ; f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=u()) g(z) ; h() }");
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
    public void testMatchCall() {
        assertEval("{ f <- function() match.call() ; f() }");
        assertEval("{ f <- function(x) match.call() ; f(2) }");
        assertEval("{ f <- function(x) match.call() ; f(x=2) }");
        assertEval("{ f <- function(...) match.call() ; f(2) }");
        assertEval("{ f <- function(...) match.call() ; f(x=2) }");
        assertEval("{ f <- function(...) match.call() ; f(x=2, y=3) }");
        assertEval("{ f <- function(...) match.call() ; f(x=2, 3) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(2) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(x=2) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(2, 3) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(x=2, y=3) }");
        assertEval("{ f <- function(...) match.call(expand.dots=FALSE) ; f(x=2, 3) }");
    }

    @Test
    public void testDoCall() {
        assertEval("{ x<-list(c(1,2)); do.call(\"as.matrix\", x) }");
        assertEval("{ do.call(quote, list(quote(1)))}");
        assertEval("{ do.call(quote, list(quote(x)))}");
        assertEval("{ do.call(quote, list(quote(x+1)))}");
        assertEval(Output.ContainsError, "{ f <- function(x) x; do.call(f, list(quote(y)))}");
        assertEval(Output.ContainsError, "{ f <- function(x) x; do.call(f, list(quote(y + 1)))}");
        assertEval("{ do.call(\"+\", list(quote(1), 2))}");
        assertEval("v1 <- as.numeric_version('3.0.0'); v2 <- as.numeric_version('3.1.0'); do.call('<', list(v1, v2))");
        assertEval("v1 <- as.numeric_version('3.0.0'); v2 <- as.numeric_version('3.1.0'); do.call('<', list(quote(v1), quote(v2)))");
    }

    @Test
    public void testIfelse() {
        assertEval("{ ifelse(TRUE,1,0) }");
        assertEval("{ ifelse(FALSE,1,0) }");
        assertEval("{ ifelse(NA,1,0) }");
    }

    @Test
    public void testIsObject() {
        assertEval("{ is.object(1) }");
        assertEval("{ is.object(1L) }");
        assertEval("{ is.object(1:3) }");
        assertEval("{ is.object(c(1,2,3)) }");
        assertEval("{ is.object(NA) }");
        assertEval("{ is.object(NULL) }");
    }

    @Test
    public void testIsAtomic() {
        assertEval("{ is.atomic(1) }");
        assertEval("{ is.atomic(1L) }");
        assertEval("{ is.atomic(1:3) }");
        assertEval("{ is.atomic(c(1,2,3)) }");
        assertEval("{ is.atomic(NA) }");
        assertEval("{ is.atomic(NULL) }");
        assertEval("{ is.atomic(TRUE) }");
        assertEval("{ !is.atomic(list()) }");
        assertEval("{ !is.atomic(function() {}) }");
    }

    @Test
    public void testIsRecursive() {
        assertEval("{ is.recursive(1) }");
        assertEval("{ is.recursive(1L) }");
        assertEval("{ is.recursive(1:3) }");
        assertEval("{ is.recursive(c(1,2,3)) }");
        assertEval("{ is.recursive(NA) }");
        assertEval("{ is.recursive(NULL) }");
        assertEval("{ is.recursive(TRUE) }");
        assertEval("{ !is.recursive(list()) }");
        assertEval("{ !is.recursive(function() {}) }");
    }

    @Test
    public void testMatMult() {
        assertEval("{ matrix(c(1,2,3,4), 2) %*% matrix(c(5,6,7,8), 2) }");
        assertEval("{ matrix(c(3,1,2,0,1,2), 2) %*% matrix(c(1,0,4,2,1,0), 3) }");
        assertEval("{ c(1,2,3) %*% c(4,5,6) }");
        assertEval("{ matrix(c(3,1,2,0,1,2),2) %*% c(1,0,4) }");
        assertEval("{ c(1,0,4) %*% matrix(c(3,1,2,0,1,2),3) }");
        assertEval("{ as.vector(c(1,2,3)) %*% t(as.vector(c(1,2))) }");

        assertEval("{ matrix(c(1+1i,2-2i,3+3i,4-4i), 2) %*% matrix(c(5+5i,6-6i,7+7i,8-8i), 2) }");
        assertEval("{ matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i), 2) %*% matrix(c(1+1i,0-0i,4+4i,2-2i,1+1i,0-0i), 3) }");
        assertEval("{ c(1+1i,2-2i,3+3i) %*% c(4-4i,5+5i,6-6i) }");
        assertEval("{ matrix(c(1+1i,0-0i,4+4i),3) %*% matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i),1) }");

        assertEval(Ignored.Unknown, "{ matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i),2) %*% c(1+1i,0-0i,4+4i) }");
        assertEval(Ignored.Unknown, "{ c(1+1i,0-0i,4+4i) %*% matrix(c(3+3i,1-1i,2+2i,0-0i,1+1i,2-2i),3) }");
    }

    @Test
    public void testMatch() {
        assertEval("{ match(2,c(1,2,3)) }");
        assertEval("{ match(c(1,2,3,4,5),c(1,2,1,2)) }");
        assertEval("{ match(\"hello\",c(\"I\", \"say\", \"hello\", \"world\")) }");
        assertEval("{ match(c(\"hello\", \"say\"),c(\"I\", \"say\", \"hello\", \"world\")) }");
        assertEval("{ match(\"abc\", c(\"xyz\")) }");
        assertEval("{ match(\"abc\", c(\"xyz\"), nomatch=-1) }");

        assertEval("{ match(c(1,2,3,\"NA\",NA), c(NA,\"NA\",1,2,3,4,5,6,7,8,9,10)) }");
        assertEval("{ match(c(1L,2L,3L,1L,NA), c(NA,1L,1L,2L,3L,4L,5L,6L,7L,8L,9L,10L)) }");
        assertEval("{ match(c(1,2,3,NaN,NA,1), c(1,NA,NaN,1,2,3,4,5,6,7,8,9,10)) }");
        assertEval("{ match(c(0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,\"NA\",NA), c(NA,\"NA\",1,2,3,4,5,6,7,8,9,10,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9)) }");
        assertEval("{ match(c(0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,1L,NA), c(NA,1L,1L,2L,3L,4L,5L,6L,7L,8L,9L,10L,0L,1L,1L,2L,3L,4L,5L,6L,7L,8L,9L,10L,0L,1L,1L,2L,3L,4L,5L,6L)) }");
        assertEval("{ match(c(0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,NaN,NA,1), c(1,NA,NaN,1,2,3,4,5,6,7,8,9,10,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9)) }");

        assertEval("{ match(factor(c(\"a\", \"b\")), factor(c(\"c\", \"b\", \"a\", \"b\", \"c\", \"a\"))) }");
    }

    @Test
    public void testIn() {
        assertEval("{ 2 %in% c(1,2,3) }");
        assertEval("{ c(1,2,3,4,5) %in% c(1,2,1,2) }");
        assertEval("{ \"hello\" %in% c(\"I\", \"say\", \"hello\", \"world\") }");
        assertEval("{ c(\"hello\", \"say\") %in% c(\"I\", \"say\", \"hello\", \"world\") }");
        assertEval("{ `%in%`(2,c(1,2,3)) }");
    }

    @Test
    public void testIsUnsorted() {
        assertEval("{ is.unsorted(c(1,2,3,4)) }");
        assertEval("{ is.unsorted(c(1,2,6,4)) }");
    }

    @Test
    public void testPrint() {
        assertEval("{ print(23) }");
        assertEval("{ print(1:3,quote=TRUE) }");
        assertEval("{ print(list(1,2,3),quote=TRUE) }");
        assertEval("{ x<-c(1,2); names(x)=c(\"a\", \"b\"); print(x,quote=TRUE) }");
        assertEval("{ x<-c(1, 2:20, 21); n<-\"a\"; n[21]=\"b\"; names(x)<-n; print(x,quote=TRUE) }");
        assertEval("{ x<-c(10000000, 10000:10007, 21000000); n<-\"a\"; n[10]=\"b\"; names(x)<-n; print(x,quote=TRUE) }");
        assertEval("{ x<-c(\"11\", \"7\", \"2222\", \"7\", \"33\"); print(x,quote=TRUE) }");
        assertEval("{  x<-c(11, 7, 2222, 7, 33); print(x,quote=TRUE) }");
        assertEval("{ x<-c(\"11\", \"7\", \"2222\", \"7\", \"33\"); names(x)<-1:5; print(x,quote=TRUE) }");
        assertEval("{ x<-c(11, 7, 2222, 7, 33); names(x)<-1:5; print(x,quote=TRUE) }");
        assertEval("{ print(list(list(list(1,2),list(3)),list(list(4),list(5,6))),quote=TRUE) }");
        assertEval("{ print(c(1.1,2.34567),quote=TRUE) }");
        assertEval("{ print(c(1,2.34567),quote=TRUE) }");
        assertEval("{ print(c(11.1,2.34567),quote=TRUE) }");
        assertEval("{ nql <- noquote(letters); print(nql)}");
        assertEval("{ nql <- noquote(letters); nql[1:4] <- \"oh\"; print(nql)}");
        assertEval("{ print(c(\"foo\"),quote=FALSE)}");
        assertEval("{ x<-matrix(c(\"a\",\"b\",\"c\",\"d\"),nrow=2);print(x,quote=FALSE)}");
        assertEval("{ y<-c(\"a\",\"b\",\"c\",\"d\");dim(y)<-c(1,2,2);print(y,quote=FALSE)}");
        assertEval("{ n <- 17 ; fac <- factor(rep(1:3, length = n), levels = 1:5) ; y<-tapply(1:n, fac, sum); y }");
        assertEval("{ nql <- noquote(letters); nql}");
    }

    @Test
    public void testInvisible() {
        assertEval("{ f <- function() { invisible(23) } ; f() }");
        assertEval("{ f <- function() { invisible(23) } ; toString(f()) }");
        assertEval("{ f <- function(x, r) { if (x) invisible(r) else r }; f(FALSE, 1) }");
        assertEval("{ f <- function(x, r) { if (x) invisible(r) else r }; f(TRUE, 1) }");
        assertEval("{ f <- function(x, r) { if (x) return(invisible(r)) else return(r) }; f(FALSE, 1) }");
        assertEval("{ f <- function(x, r) { if (x) return(invisible(r)) else return(r) }; f(TRUE, 1) }");
    }

    @Test
    public void testSimpleRm() {
        assertEval(Output.ContainsError, "{ x <- 200 ; rm(\"x\") ; x }");
        assertEval(Output.ContainsWarning, "{ rm(\"ieps\") }");
        assertEval("{ x <- 200 ; rm(\"x\") }");
        assertEval(Output.ContainsError, "{ x<-200; y<-100; rm(\"x\", \"y\"); x }");
        assertEval(Output.ContainsError, "{ x<-200; y<-100; rm(\"x\", \"y\"); y }");
    }

    @Test
    public void testVapply() {
        assertEval("{ vapply(c(1L, 2L, 3L, 4L), function(x) x+5L, c(1L)) }");
        assertEval("{ iv <- integer(1); iv[[1]] = 1L; vapply(c(1L, 2L, 3L, 4L), function(x) x+5L, iv) }");
        assertEval("{ vapply(c(10, 20, 30, 40), function(x) x/2, c(1)) }");
        assertEval("{ vapply(c(\"hello\", \"goodbye\", \"up\", \"down\"), function(x) x, c(\"a\"), USE.NAMES = FALSE) }");
        assertEval("{ vapply(c(\"hello\", \"goodbye\", \"up\", \"down\"), function(x) x, c(\"a\")) }");
        assertEval("{ vapply(c(3+2i, 7-4i, 8+6i), function(x) x+(3+2i), c(1+1i)) }");
        assertEval("{ vapply(c(TRUE, FALSE, TRUE), function(x) x, c(TRUE)) }");
        assertEval("{ vapply(c(TRUE, FALSE, TRUE), function(x) FALSE, c(TRUE)) }");
        assertEval("{ vapply(c(1L, 2L, 3L, 4L), function(x, y) x+5L, c(1L), 10) }");

        assertEval("{ f<-function(x) as.integer(x + 1) ; y<-vapply(list(1:3, 6:8), f, rep(7, 3)); y }");
        assertEval("{ f<-function(x) x + 1 ; y<-vapply(list(1:3, 6:8), f, rep(as.double(NA), 3)); y }");
        assertEval("{ f<-function(x) x + 1 ; y<-vapply(list(1:3), f, rep(as.double(NA), 3)); y }");
    }

    @Test
    public void testUseMethodSimple() {
        // Basic UseMethod
        assertEval("{f <- function(x){ UseMethod(\"f\",x); };f.first <- function(x){cat(\"f first\",x)};f.second <- function(x){cat(\"f second\",x)};obj <-1;"
                        + "attr(obj,\"class\")  <- \"first\";f(obj);attr(obj,\"class\")  <- \"second\";}");
        assertEval("{f<-function(x){UseMethod(\"f\")};f.logical<-function(x){print(\"logical\")};f(TRUE)}");
    }

    @Test
    public void testUseMethodOneArg() {
        // If only one argument is passed to UseMethod(), the call should
        // be resolved based on first argument to enclosing function.
        assertEval("{f <- function(x){ UseMethod(\"f\"); };f.first <- function(x){cat(\"f first\",x)}; f.second <- function(x){cat(\"f second\",x)}; obj <-1; attr(obj,\"class\")  <- \"first\"; f(obj); attr(obj,\"class\")  <- \"second\";}");
    }

    @Test
    public void testUseMethodLocalVars() {
        // The variables defined before call to UseMethod should be
        // accessible to target function.
        assertEval(Ignored.Unknown,
                        "{f <- function(x){ y<-2;locFun <- function(){cat(\"local\")}; UseMethod(\"f\"); }; f.second <- function(x){cat(\"f second\",x);locFun();}; obj <-1; attr(obj,\"class\")  <- \"second\"; f(obj);}");
    }

    @Test
    public void testUseMethodNested() {
        // The UseMethod call can be nested deep compared to where target is
        // defined.
        assertEval("{f <- function(x){g<- function(x){ h<- function(x){ UseMethod(\"f\");}; h(x)}; g(x) }; f.second <- function(x){cat(\"f second\",x);}; obj <-1; attr(obj,\"class\")  <- \"second\"; f(obj);}");
    }

    @Test
    public void testUseMethodEnclFuncArgs() {
        // All the argument passed to the caller of UseMethod() should be
        // accessible to the target method.
        assertEval("{f <- function(x,y,z){ UseMethod(\"f\"); }; f.second <- function(x,y,z){cat(\"f second\",x,y,z)}; obj <-1; attr(obj,\"class\") <- \"second\"; arg2=2; arg3=3; f(obj,arg2,arg3);}");

    }

    @Test
    public void testUseMethodReturn() {
        // All the statements after UseMethod() call should get ignored.
        assertEval("{f <- function(x){ UseMethod(\"f\");cat(\"This should not be executed\"); }; f.second <- function(x){cat(\"f second\",x);}; obj <-1; attr(obj,\"class\")  <- \"second\"; f(obj);}");
    }

    @Test
    public void testUpdateClass() {
        assertEval("{x=1; class(x)<-\"first\"; x;}");

        assertEval("{ x=1;class(x)<-\"character\"; x}");

        assertEval("{x<-1; class(x)<-\"logical\"; x;  class(x)<-c(1,2,3); x; class(x)<-NULL; x;}");

        assertEval("{x<-1;class(x)<-c(1,2,3);class(x)<-c(); x;}");

        assertEval("{x<-1;class(x)<-c(1,2,3); x;}");

        assertEval("{x<-1;class(x)<-c(TRUE,FALSE); x;}");

        assertEval("{x<-1;class(x)<-c(2+3i,4+5i); x;}");

        assertEval("{x<-1;class(x)<-c(1,2,3);class(x)<-NULL; x;}");

        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2); class(x)<-\"array\"; x; class(x)<-\"matrix\"; x;}");

        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2); class(x)}");

        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2); class(x);dim(x)<-c(2,2,1);class(x)}");

        assertEval("{x<-c(1,2,3,4); dim(x)<-c(2,2,1); class(x)}");

        assertEval("{x<-1;class(x)<-c(1,2,3);y<-unclass(x);x;y}");

        assertEval("{x<-1;class(x)<-\"a\";x}");

        assertEval("{x<-1;class(x)<-\"a\";class(x)<-\"numeric\";x;}");

        assertEval("{x<-TRUE;class(x)<-\"a\";class(x)<-\"logical\";x;}");

        assertEval("{x<-2+3i;class(x)<-\"a\";class(x)<-\"complex\";x;}");

        assertEval("{x<-c(1,2);class(x)<-\"a\";class(x)<-\"list\";x;}");

        assertEval("{x<-\"abc\";class(x)<-\"a\";class(x)<-\"character\";x;}");

        assertEval("{x<-c(2+3i,4+5i);class(x)<-\"a\";class(x)<-\"complex\";x;}");

        assertEval("{x<-1;attr(x,\"class\")<-c(\"a\",\"b\");x;}");

        // Doesn't remove the class attribute unlike class(x)<-"numeric".
        assertEval("{x<-1;attr(x,\"class\")<-c(\"a\",\"b\");attr(x,\"class\")<-\"numeric\";x}");

        assertEval("{x<-1;attr(x,\"class\")<-\"b\";x;}");

        assertEval("{x<-1;y<-\"b\";attr(x,\"class\")<-y;x;}");

        // oldClass
        assertEval("{ x<-1; oldClass(x)<-\"foo\"; class(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"foo\"; oldClass(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"integer\"; class(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"integer\"; oldClass(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"integer\"; class(x)<-\"integer\"; class(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"integer\"; class(x)<-\"integer\"; oldClass(x) }");

        // test setting class on other types
        assertEval("{ x <- new.env(); class(x); class(x)<-\"abc\"; class(x); class(x)<-NULL; class(x) }");
        assertEval("{ x <- new.env(); class(x); class(x)<-c(\"abc\", \"xyz\"); class(x); class(x)<-NULL; class(x) }");

        assertEval("{ x <- function() { }; class(x); class(x)<-\"abc\"; class(x); class(x)<-NULL; class(x) }");
        assertEval("{ x <- function() { }; class(x); class(x)<-c(\"abc\", \"xyz\"); class(x); class(x)<-NULL; class(x) }");

        assertEval(Output.ContainsError, "{x<-c(1,2,3,4); class(x)<-\"array\"; class(x)<-\"matrix\";}");
        assertEval(Output.ContainsError, "{x<-1;attr(x,\"class\")<-c(1,2,3);}");
    }

    @Test
    public void testGetClass() {
        assertEval("{x<-1L;class(x)}");

        assertEval("{x<-c(1L,2L,3L);class(x)}");

        assertEval("{x<-seq(1L,10L);class(x)}");

        assertEval("{x<-seq(1.1,10.1);class(x)}");

        assertEval("{x<-1;class(x)}");

        assertEval("{x<-c(1,2,3);class(x)}");

        assertEval("{ x<-1; oldClass(x) }");

        assertEval("{x<-seq(1,10);class(x)}");
    }

    @Test
    public void testInherits() {
        assertEval("{x <- 10; inherits(x, \"a\") ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\"); inherits(x,\"a\") ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", TRUE) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, c(\"a\", \"b\", \"c\"), TRUE) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\");inherits(x, c(\"a\", \"b\", \"a\"), TRUE) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, c(\"c\", \"q\", \"b\"), TRUE) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, c(\"c\", \"q\", \"b\")) ;}");
        assertEval("{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", c(TRUE)) ;}");
        assertEval("{ inherits(NULL, \"try-error\") }");
        assertEval("{ inherits(new.env(), \"try-error\") }");

        assertEval("{ x<-data.frame(c(1,2)); inherits(x, \"data.frame\") }");
        assertEval("{ x<-factor(\"a\", \"b\", \"a\"); inherits(x, \"factor\") }");
        assertEval("{ inherits(textConnection(\"abc\"), \"connection\") }");

        assertEval("{ e <- new.env(); inherits(e, \"environment\") }");
        assertEval("{ e <- new.env(); inherits(e, \"abc\") }");
        assertEval("{ e <- new.env(); class(e)<-\"abc\"; inherits(e, \"abc\") }");
        assertEval("{ f <- function() { }; inherits(f, \"function\") }");
        assertEval("{ f <- function() { }; inherits(f, \"abc\") }");
        assertEval("{ f <- function() { }; class(f)<-\"abc\"; inherits(f, \"abc\") }");

        // Fails because of exact string matching in error message.
        assertEval(Ignored.Unknown, "{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, 2, c(TRUE)) ;}");
        assertEval(Ignored.Unknown, "{x <- 10;class(x) <- c(\"a\", \"b\");inherits(x, \"a\", 1) ;}");
    }

    @Test
    public void testGet() {
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"integer\")};y();}");
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"closure\")};y();}");
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"integer\",inherits=FALSE);get(\"y\",mode=\"integer\",inherits=FALSE)};y();}");
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"double\")};y();}");
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"double\",inherits=FALSE)};y();}");
        assertEval(Output.ContainsError, "{ get(\"dummy\") }");
        assertEval(Output.ContainsError, "{ x <- 33 ; f <- function() { if (FALSE) { x <- 22  } ; get(\"x\", inherits = FALSE) } ; f() }");
        assertEval(Output.ContainsError, "{ x <- 33 ; f <- function() { get(\"x\", inherits = FALSE) } ; f() }");
        assertEval("{ get(\".Platform\", globalenv())$endian }");
        assertEval("{ get(\".Platform\")$endian }");
        assertEval(Output.ContainsError, "{y<-function(){y<-2;get(\"y\",mode=\"closure\",inherits=FALSE);};y();}");
    }

    @Test
    public void testMGet() {
        assertEval("{ a<- 1; b <- 2; mget(c(\"a\", \"b\")) }");
        assertEval("{ a<- 1; b <- 2; f <- function() { mget(c(\"a\", \"b\"), inherits=TRUE)}; f() }");
        assertEval("{ a<- 1; mget(c(\"a\", \"b\"), ifnotfound=list(100)) }");
        assertEval("{ b <- 2; f <- function() { mget(c(\"a\", \"b\"), ifnotfound=list(100), inherits=TRUE)}; f() }");
        assertEval("{ mget(c(\"a\", \"b\"), ifnotfound=list(100, 200)) }");
        assertEval("{ a<- 1; b <- 2; mget(c(\"a\", \"b\"), mode=\"numeric\") }");
        assertEval("{ a<- 1; b <- \"2\"; mget(c(\"a\", \"b\"), mode=c(\"numeric\", \"character\")) }");
    }

    @Test
    public void testNextMethod() {
        assertEval("{ g<-function(){ x<-1; class(x)<-c(\"a\",\"b\",\"c\"); f<-function(x){UseMethod(\"f\")}; f.a<-function(x){cat(\"a\");NextMethod(\"f\",x)}; f.b<-function(x){cat(\"b\")}; f(x); }; g() }");
        assertEval("{ g<-function(){ x<-1; class(x)<-c(\"a\",\"b\",\"c\"); f<-function(x){UseMethod(\"f\")}; f.a<-function(x){cat(\"a\");NextMethod(\"f\",x, 42)}; f.b<-function(x, y=7){cat(\"b\", y)}; f(x); }; g(); }");
        assertEval("{ g<-function(){ x<-1; class(x)<-c(\"a\",\"b\",\"c\"); f<-function(x){UseMethod(\"f\")}; f.a<-function(x){cat(\"a\");NextMethod(\"f\",x,\"m\",\"n\")}; f.b<-function(x, y=\"h\", z=\"i\"){cat(\"b\", y, z)}; f(x); }; g() }");
        assertEval("{ g<-function(){ x<-1; class(x)<-c(\"a\",\"b\",\"c\"); f<-function(x){UseMethod(\"f\")}; f.a<-function(x){cat(\"a\");NextMethod(\"f\",x,z=\"m\",y=\"n\")}; f.b<-function(x, y=\"h\", z=\"i\"){cat(\"b\", y, z)}; f(x); }; g() }");
        assertEval("{ foo <- function(x,y) UseMethod('foo'); foo.bar <- function(x, y) { y <- 10; NextMethod() }; foo.default <- function(x,y) cat(x,y); v <- c(1,2,3); class(v) <- 'bar'; foo(v,5) }");
        assertEval("{ f.default<-function(x, a=7) a; f.foo<-function(x, a=v) { b<-NextMethod(\"f\"); v=42; c(a,b) }; x<-1; class(x)<-\"foo\"; f<-function(x) UseMethod(\"f\"); f(x) }");
    }

    @Test
    public void testSummaryGroupDispatch() {
        assertEval("{x<-c(1,2,3);class(x)<-\"foo\";Summary.foo<-function(x,...){\"summary\"};max(x)}");
        assertEval("{x<-c(1,2,3);class(x)<-\"foo\";Summary.foo<-function(x,...){\"summary\"};min(x)}");
        assertEval("{x<-c(1,2,3);class(x)<-\"foo\";min.foo<-function(x,...){\"summary\"};min(x)}");
    }

    @Test
    public void testOpsGroupDispatch() {
        assertEval("{x<-1;y<-7;class(x)<-\"foo\";class(y)<-\"foo\";\"*.foo\"<-function(e1,e2){min(e1,e2)};x*y}");
        assertEval("{x<-1;y<-7;class(x)<-\"foo\";class(y)<-\"fooX\";\"*.foo\"<-function(e1,e2){min(e1,e2)};x*y}");
        assertEval("{x<-1;y<-7;class(x)<-\"fooX\";class(y)<-\"foo\";\"*.foo\"<-function(e1,e2){min(e1,e2)};x*y}");
        assertEval("{x<-1;y<-7;class(x)<-\"fooX\";class(y)<-\"fooX\";\"*.foo\"<-function(e1,e2){min(e1,e2)};x*y}");

        assertEval("{x<-1;y<-7;class(x)<-\"foo\";class(y)<-\"foo\";\"^.foo\"<-function(e1,e2){e1+e2};x^y}");

        assertEval("{x<-1;class(x)<-\"foo\";\"!.foo\"<-function(e1,e2){x};!x}");
    }

    @Test
    public void testOpsGroupDispatchLs() {
        assertEval("{x<-1;y<-7;class(x)<-\"foo\";class(y)<-\"foo\";\"*.foo\"<-function(e1,e2){min(e1,e2)}; ls()}");
    }

    @Test
    public void testMathGroupDispatch() {
        assertEval("{x<--7;class(x)<-\"foo\";Math.foo<-function(z){x};abs(x);}");
        assertEval("{x<--7;class(x)<-\"foo\";Math.foo<-function(z){-z;};log(x);}");
    }

    @Test
    public void testComplexGroupDispatch() {
        assertEval("{x<--7+2i;class(x)<-\"foo\";Complex.foo<-function(z){1;};Im(x);}");
    }

    @Test
    public void testSwitch() {
        assertEval("{ test1 <- function(type) { switch(type, mean = 1, median = 2, trimmed = 3) };test1(\"median\")}");
        assertEval("{switch(3,1,2,3)}");
        assertEval("{switch(4,1,2,3)}");
        assertEval("{switch(4,1,2,z)}");
        assertEval("{ test1 <- function(type) { switch(type, mean = mean(c(1,2,3,4)), median = 2, trimmed = 3) };test1(\"mean\")}");
        assertEval("{ u <- \"uiui\" ; switch(u, \"iuiu\" = \"ieps\", \"uiui\" = \"miep\") }");
        assertEval("{ answer<-\"no\";switch(as.character(answer), yes=, YES=1, no=, NO=2,3) }");
        assertEval("{ x <- \"<\"; v <- switch(x, \"<=\" =, \"<\" =, \">\" = TRUE, FALSE); v }");
        assertEval("{ x <- \"<\"; switch(x, \"<=\" =, \"<\" =, \">\" = TRUE, FALSE) }");
        assertEval("{ x <- \"<\"; switch(x, \"<=\" =, \"<\" =, \">\" =, FALSE) }");
        assertEval("{ a <- NULL ; switch(mode(a), NULL=\"naught\") }");
        assertEval("{ a <- NULL ; switch(mode(a), NULL=) }");
        assertEval(Output.ContainsError, "{ x <- \"!\"; v <- switch(x, v77, \"<=\" =, \"<\" =, \">\" = 99, v55)}");
        assertEval(Output.ContainsError, "{ x <- \"!\"; v <- switch(x, \"\"=v77, \"<=\" =, \"<\" =, \">\" = 99, v55)}");
    }

    @Test
    public void testDefaultArgs() {
        assertEval(Output.ContainsError, "{ array(dim=c(-2,2)); }");
        assertEval(Output.ContainsError, "{ array(dim=c(-2,-2)); }");
        assertEval("{ length(array(dim=c(1,0,2,3))) }");
        assertEval("{ dim(array(dim=c(2.1,2.9,3.1,4.7))) }");
    }

    @Test
    public void testStorageMode() {
        assertEval("{storage.mode(1)}");
        assertEval("{storage.mode(c)}");
        assertEval("{storage.mode(f<-function(){1})}");
        assertEval("{storage.mode(c(1,2,3))}");
        assertEval("{x<-1;storage.mode(x)<-\"character\"}");
        assertEval("{x<-1;storage.mode(x)<-\"logical\";x}");
    }

    @Test
    public void testUpdateStorageMode() {
        assertEval("{ x <- c(1L, 2L); storage.mode(x) <- \"double\"}");
        assertEval(Output.ContainsError, "{ x <- c(1L, 2L); storage.mode(x) <- \"not.double\"}");
        assertEval("{ x <- c(1L, 2L); dim(x)<-c(1,2); storage.mode(x) <- \"double\"; x}");
    }

    @Test
    public void testIsFactor() {
        assertEval("{x<-1;class(x)<-\"foo\";is.factor(x)}");
        assertEval("{is.factor(1)}");
        assertEval("{is.factor(c)}");

        assertEval(Output.ContainsError, "{x<-1;class(x)<-\"factor\";is.factor(x)}");
    }

    @Test
    public void testIsFinite() {
        assertEval("{ is.finite(1:100) }");
        assertEval("{ is.finite(1:1) }");
        assertEval("{ is.finite(c(1,2,3)) }");
        assertEval("{ is.finite(c(1,2,Inf)) }");
        assertEval("{ is.finite(c(1,2,-Inf)) }");
        assertEval("{ is.finite(c(Inf,2,-Inf)) }");
        assertEval("{ is.finite(c(Inf,NA,-Inf)) }");
        assertEval("{ is.finite(1) }");
        assertEval("{ is.finite(c(1L,2L,3L)) }");
        assertEval("{ is.finite(c(1L,2L,NA)) }");
        assertEval("{ is.finite(1L) }");
        assertEval("{ is.finite(as.raw(c(1L,2L,3L))) }");
        assertEval("{ is.finite(c(TRUE, FALSE)) }");
        assertEval("{ is.finite(c(TRUE, FALSE, NA)) }");
    }

    @Test
    public void testParen() {
        assertEval(Ignored.Unknown, "{ a = array(1,c(3,3,3)); (a[1,2,3] = 3) }");
    }

    @Test
    public void testIsVector() {
        assertEval("{ is.vector(1) }");
        assertEval("{ is.vector(1:3) }");
        assertEval("{ is.vector(NULL) }");
        assertEval("{ x<-c(1,3); is.vector(x, \"double\"); }");
        assertEval("{ x<-c(1,3); is.vector(x, \"integer\"); }");
        assertEval("{ x<-c(1:3); is.vector(x, \"double\"); }");
        assertEval("{ x<-c(1:3); is.vector(x, \"integer\"); }");
        assertEval("{ x<-c(1,3); is.vector(x, \"d\"); }");
        assertEval("{ x<-list(1,3); }");
        assertEval("{ x<-c(1); attr(x, \"foo\")<-\"foo\"; is.vector(x) }");
        assertEval("{ x<-list(1); attr(x, \"foo\")<-\"foo\"; is.vector(x) }");
        assertEval("{is.vector(c(TRUE,FALSE),\"numeric\");}");
        assertEval("{is.vector(c(TRUE,FALSE),\"logical\");}");
        assertEval("{x<-1;class(x)<-\"a\";is.vector(x);}");
        assertEval("{x<-1;names(x)<-\"a\";is.vector(x);}");
    }

    @Test
    public void testLapply() {
        assertEval("{ lapply(1:3, function(x) { 2*x }) }");
        assertEval("{ lapply(1:3, function(x,y) { x*y }, 2) }");
        assertEval("{ x<-c(1,3,4);attr(x,\"names\")<-c(\"a\",\"b\",\"c\");lapply(x, function(x,y) { as.character(x*y) }, 2) }");
        assertEval("{ f <- function() { lapply(c(X=\"a\",Y=\"b\"), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ lapply(1:3, function(x,y,z) { as.character(x*y+z) }, 2,7) }");
        assertEval("{ f <- function(x) 2 * x ; lapply(1:3, f) }");
        assertEval("{ f <- function(x, y) x * y ; lapply(1:3, f, 2) }");
        assertEval("{ lapply(1:3, sum) }");
        assertEval("{ lapply(1:3, sum, 2) }");
        assertEval("{ x <- list(a=1:10, b=1:20) ; lapply(x, sum) }");
        assertEval("{ l <- list(list(1),list(2),list(3)); f <- function(a) { lapply(a, function(x) lapply(x, function(y) print(y))) }; f(l)}");
    }

    @Test
    public void testNgettext() {
        assertEval("{ ngettext(1, \"a\", \"b\") }");
        assertEval("{ ngettext(0, \"a\", \"b\") }");
        assertEval("{ ngettext(42, \"a\", \"b\") }");
        assertEval("{ ngettext(1, c(\"a\"), \"b\") }");
        assertEval("{ ngettext(1, \"a\", c(\"b\")) }");
        assertEval("{ ngettext(c(1), \"a\", \"b\") }");
        assertEval("{ ngettext(c(1,2), \"a\", \"b\") }");
        assertEval(Output.ContainsWarning, "{ ngettext(1+1i, \"a\", \"b\") }");
        assertEval(Output.ContainsError, "{ ngettext(1, NULL, \"b\") }");
        assertEval(Output.ContainsError, "{ ngettext(1, \"a\", NULL) }");
        assertEval(Output.ContainsError, "{ ngettext(1, NULL, NULL) }");
        assertEval(Output.ContainsError, "{ ngettext(1, c(\"a\", \"c\"), \"b\") }");
        assertEval(Output.ContainsError, "{ ngettext(1, \"a\", c(\"b\", \"c\")) }");
        assertEval(Output.ContainsError, "{ ngettext(1, c(1), \"b\") }");
        assertEval(Output.ContainsError, "{ ngettext(1, \"a\", c(1)) }");
        assertEval(Output.ContainsError, "{ ngettext(-1, \"a\", \"b\") }");
    }

    @Test
    public void testDate() {
        // Date at real time differs by milliseconds.
        assertEval(Ignored.Unknown, "{date()}");
    }

    public void testFormat() {
        assertEval("{ format(7) }");
        assertEval("{ format(7.42) }");
        assertEval("{ format(c(7,42)) }");
        assertEval("{ format(c(7.42,42.7)) }");
        assertEval("{ format(c(7.42,42.7,NA)) }");
    }

    @Test
    public void testProd() {
        assertEval("{prod(c(2,4))}");
        assertEval("{prod(c(2,4,3))}");
        assertEval("{prod(c(1,2,3,4,5))}");
        assertEval("{prod(c(1+2i))}");
        assertEval("{prod(c(1+2i, 2+3i))}");
        assertEval("{prod(c(1+2i,1+3i,1+45i))}");
        assertEval("{prod(c(TRUE, TRUE))}");
        assertEval("{prod(c(TRUE, FALSE))}");
    }

    @Test
    public void testProdNa() {
        assertEval(Ignored.Unknown, "{prod(c(2,4,NA))}");
        assertEval(Ignored.Unknown, "{prod(c(2,4,3,NA),TRUE)}");
        assertEval(Ignored.Unknown, "{prod(c(1,2,3,4,5,NA),FALSE)}");
    }

    @Test
    public void testMean() {
        assertEval("{ mean(c(5,5,5,5,5)) }");
        assertEval("{ mean(c(1,2,3,4,5)) }");
        assertEval("{ mean(c(2,4))}");
        assertEval("{ mean(c(2L,4L,3L))}");
        assertEval("{ mean(c(1,2,3,4,5))}");
        assertEval("{ mean(c(1+2i))}");
        assertEval("{ mean(c(1+2i, 2+3i))}");
        assertEval("{ mean(c(1+2i,1+3i,1+45i))}");
        assertEval("{ mean(c(TRUE, TRUE))}");
        assertEval("{ mean(c(TRUE, FALSE))}");
    }

    @Test
    public void testWhichMin() {
        assertEval("{ which.min(c(5,5,5,5,5)) }");
        assertEval("{ which.min(c(1,2,3,4,5)) }");
        assertEval("{ which.min(c(2,4))}");
        assertEval("{ which.min(c(2L,4L,3L))}");
        assertEval("{ which.min(c(1,2,3,4,5))}");
        assertEval("{ which.min(c(TRUE, TRUE))}");
        assertEval("{ which.min(c(TRUE, FALSE))}");
        assertEval("{ which.min(c(1:5))}");
        assertEval("{ which.min(c(5:1))}");
        assertEval("{ which.min(c(1:10000))}");
    }

    @Test
    public void testWhichMax() {
        assertEval("{ which.max(c(5,5,5,5,5)) }");
        assertEval("{ which.max(c(1,2,3,4,5)) }");
        assertEval("{ which.max(c(2,4))}");
        assertEval("{ which.max(c(2L,4L,3L))}");
        assertEval("{ which.max(c(1,2,3,4,5))}");
        assertEval("{ which.max(c(TRUE, TRUE))}");
        assertEval("{ which.max(c(TRUE, FALSE))}");
        assertEval("{ which.max(c(1:5))}");
        assertEval("{ which.max(c(5:1))}");
        assertEval("{ which.max(c(1:10000))}");
    }

    @Test
    public void testSample() {
        assertEval("{  set.seed(4357, \"default\"); x <- 5 ; sample(x, 5, TRUE, NULL) ;}");
        assertEval("{  set.seed(4357, \"default\"); x <- 5 ; sample(x, 5, FALSE, NULL) ;}");

        assertEval("{ set.seed(4357, \"default\");  x <- c(5, \"cat\"); sample(x, 2, TRUE, NULL) ;}");
        assertEval("{ set.seed(4357, \"default\"); x <- c(5, \"cat\"); sample(x, 2, FALSE, NULL) ;}");
        assertEval("{ set.seed(4357, \"default\"); x <- c(5, \"cat\"); sample(x, 3, TRUE, NULL) ;}");

        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5; sample(x, 5, TRUE, NULL) ;}");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5; sample(x, 5, FALSE, NULL) ;}");

        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- c(5, \"cat\") ; sample(x, 2, TRUE, NULL) ;}");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- c(5, \"cat\") ; sample(x, 2, FALSE, NULL) ;}");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- c(5, \"cat\") ; sample(x, 3, TRUE, NULL) ;}");

        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5 ; prob <- c(.1, .2, .3, .2, .1) ; sample(x, 10, TRUE, prob) ; }");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5 ; prob <- c(.5, .5, .5, .5, .5) ; sample(x, 5, FALSE, prob) ; }");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5 ; prob <- c(.2, .2, .2, .2, .2 ) ; sample(x, 5, FALSE, prob) ; }");

        assertEval("{ set.seed(4357, \"default\"); x <- c(\"Heads\", \"Tails\"); prob <- c(.3, .7) ; sample(x, 10, TRUE, prob) ; }");
        assertEval("{ set.seed(4357, \"default\"); x <- 5 ; prob <- c(.1, .2, .3, .2, .1); sample(x, 10, TRUE, prob) ; }");
        assertEval("{ set.seed(4357, \"default\"); x <- 5 ; prob <- c(.5, .5, .5, .5, .5); sample(x, 5, FALSE, prob) ; }");
        assertEval("{ set.seed(4357, \"default\"); x <- 5 ; prob <- c(.2, .2, .2, .2, .2 ); sample(x, 5, FALSE, prob) ; }");

        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\");x <- c(\"Heads\", \"Tails\") ; prob <- c(.3, .7) ; sample(x, 10, TRUE, prob) ; }");

        assertEval(Ignored.Unknown, "{ set.seed(9567, \"Marsaglia-Multicarry\");x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, TRUE, prob) ; }");
        assertEval(Ignored.Unknown, "{ set.seed(9567, \"Marsaglia-Multicarry\");x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, FALSE, prob) ; }");
        assertEval(Ignored.Unknown, "{ set.seed(4357, \"default\"); x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, TRUE, prob) ; }");
        assertEval(Ignored.Unknown, "{ set.seed(4357, \"default\"); x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, FALSE, prob) ; }");

        // Fails because of error message mismatch.
        assertEval(Ignored.Unknown, "{ set.seed(4357, \"default\"); x <- 5 ; sample(x, 6, FALSE, NULL) ;}");
        assertEval(Ignored.Unknown, "{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5 ; sample(x, 6, FALSE, NULL) ;}");
    }

    @Test
    public void testLevels() {
        assertEval("{ x <- 1 ; levels(x)<-\"a\"; levels(x);}");
        assertEval("{ x <- 5 ; levels(x)<-\"catdog\"; levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-NULL; levels(x)}");
        assertEval("{ x <- 1 ; levels(x)<-1; levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-4.5; levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-c(1); levels(x);}");
        assertEval("{ x <- 5 ; levels(x)<-c(1,2,3); levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-c(\"cat\", \"dog\"); levels(x)}");
        assertEval("{ x <- 1 ; levels(x)<-c(3, \"cat\"); levels(x);}");
        assertEval("{ x <- 1 ; levels(x)<-c(1, \"cat\", 4.5, \"3\"); levels(x);}");
    }

    private static final String[] DUPLICATED_FUNCTIONS = new String[]{"anyDuplicated", "duplicated"};

    @Test
    public void testAnyDuplicated() {
        assertEval(template("{ %0(c(1L, 2L, 3L, 4L, 2L, 3L)) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(1L, 2L, 3L, 4L, 2L, 3L), incomparables = TRUE )}", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(1L, 2L, 3L, 4L, 2L, 3L), fromLast = TRUE) }", DUPLICATED_FUNCTIONS));

        // strings
        assertEval(template("{%0(c(\"abc\"))}", DUPLICATED_FUNCTIONS));
        assertEval(template("{%0(c(\"abc\", \"def\", \"abc\"))}", DUPLICATED_FUNCTIONS));
        assertEval(template("{%0(c(\"abc\", \"def\", \"ghi\", \"jkl\"))}", DUPLICATED_FUNCTIONS));

        // boolean
        assertEval(template("{%0(c(FALSE))}", DUPLICATED_FUNCTIONS));
        assertEval(template("{%0(c(FALSE, TRUE))}", DUPLICATED_FUNCTIONS));
        assertEval(template("{%0(c(FALSE, TRUE, FALSE))}", DUPLICATED_FUNCTIONS));

        // complex
        assertEval(template("{%0(c(2+2i)) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{%0(c(2+2i, 3+3i, 2+2i)) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{%0(c(2+2i, 3+3i, 4+4i, 5+5i)) }", DUPLICATED_FUNCTIONS));

        // Double Vector
        assertEval(template("{ %0(c(27.2, 68.4, 94.3, 22.2)) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(1, 1, 4, 5, 4), TRUE, TRUE) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(1,2,1)) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(1)) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(1,2,3,4)) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(list(76.5, 5L, 5L, 76.5, 5, 5), incomparables = c(5L, 76.5)) }", DUPLICATED_FUNCTIONS));

        // Logical Vector
        assertEval(template("{ %0(c(TRUE, FALSE, TRUE), TRUE) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(TRUE, FALSE, TRUE), TRUE, fromLast = 1) }", DUPLICATED_FUNCTIONS));

        // String Vector
        assertEval(template("{ %0(c(\"abc\", \"good\", \"hello\", \"hello\", \"abc\")) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(\"TRUE\", \"TRUE\", \"FALSE\", \"FALSE\"), FALSE) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(\"TRUE\", \"TRUE\", \"FALSE\", \"FALSE\"), TRUE) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(\"TRUE\", \"TRUE\", \"FALSE\", \"FALSE\"), 1) }", DUPLICATED_FUNCTIONS));

        // Complex Vector
        assertEval(template("{ %0(c(1+0i, 6+7i, 1+0i), TRUE)}", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(1+1i, 4-6i, 4-6i, 6+7i)) }", DUPLICATED_FUNCTIONS));
        assertEval(template("{ %0(c(1, 4+6i, 7+7i, 1), incomparables = c(1, 2)) }", DUPLICATED_FUNCTIONS));

        assertEval(Output.ContainsWarning, template("{ %0(c(1L, 2L, 1L, 1L, 3L, 2L), incomparables = \"cat\") }", DUPLICATED_FUNCTIONS));
        assertEval(Output.ContainsWarning, template("{ %0(c(1,2,3,2), incomparables = c(2+6i)) }", DUPLICATED_FUNCTIONS));
    }

    @Test
    public void testSweep() {
        assertEval("{ sweep(array(1:24, dim = 4:2), 1, 5) }");
        assertEval("{ sweep(array(1:24, dim = 4:2), 1, 1:4) }");
        assertEval("{ sweep(array(1:24, dim = 4:2), 1:2, 5) }");

        assertEval("{ A <- matrix(1:15, ncol=5); sweep(A, 2, colSums(A), \"/\") }");

        // Correct output but warnings
        assertEval(Ignored.Unknown, "{ A <- matrix(1:50, nrow=4); sweep(A, 1, 5, '-') }");
        assertEval(Ignored.Unknown, "{ A <- matrix(7:1, nrow=5); sweep(A, 1, -1, '*') }");

        assertEval("{rowMeans(matrix(c(NaN,4+5i,2+0i,5+10i),nrow=2,ncol=2), na.rm = FALSE)}");
    }

    @Test
    public void testRowMeans() {
        assertEval("{rowMeans(matrix(c(3,4,2,5)))}");
        assertEval("{rowMeans(matrix(c(3L,4L,2L,5L)))}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,TRUE)))}");
        assertEval("{rowMeans(matrix(c(3+2i,4+5i,2+0i,5+10i)))}");
        assertEval("{rowMeans(matrix(c(3,4,NaN,5),ncol=2,nrow=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(3,4,NaN,5),ncol=2,nrow=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(3L,NaN,2L,5L),ncol=2,nrow=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(3L,NA,2L,5L),ncol=2,nrow=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(3L,NaN,2L,5L),ncol=2,nrow=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(3L,NA,2L,5L),ncol=2,nrow=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = FALSE)}");
        assertEval("{rowMeans(matrix(c(NaN,4+5i,2+0i,5+10i),nrow=2,ncol=2), na.rm = TRUE)}");
        // Whichever value(NA or NaN) is first in the row will be returned for that row.
        assertEval("{rowMeans(matrix(c(NA,NaN,NaN,NA),ncol=2,nrow=2))}");

        // Error message mismatch
        assertEval(Ignored.Unknown, "{rowMeans(matrix(NA,NA,NA),TRUE)}");
        assertEval(Output.ContainsError, "{x<-matrix(c(\"1\",\"2\",\"3\",\"4\"),ncol=2);rowMeans(x)}");

    }

    @Test
    public void testRowSums() {
        assertEval("{x<-cbind(1:3, 4:6, 7:9); rowSums(x)}");
        assertEval("{x<-cbind(1:3, NA, 7:9); rowSums(x)}");
        assertEval("{x<-cbind(1:3, NaN, 7:9); rowSums(x)}");
        assertEval("{x<-cbind(1:3, NaN, 7:9, 10:12); rowSums(x, na.rm=TRUE)}");
        assertEval("{x<-cbind(1:4, NA, NaN, 9:12); rowSums(x, na.rm=TRUE)}");
        assertEval("{x<-cbind(2L:10L,3L); rowSums(x)}");
        assertEval("{rowSums(matrix(c(3+2i,4+5i,2+0i,5+10i)))}");
        assertEval("{rowSums(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{rowSums(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{rowSums(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = FALSE)}");
        assertEval("{rowSums(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = FALSE)}");
        assertEval("{rowSums(matrix(c(NaN,4+5i,2+0i,5+10i),nrow=2,ncol=2), na.rm = TRUE)}");

        // Whichever value(NA or NaN) is first in the row will be returned for that row.
        assertEval("{rowSums(matrix(c(NA,NaN,NaN,NA),ncol=2,nrow=2))}");

        // rowSums on matrix drop dimension
        assertEval("{ a = rowSums(matrix(1:12,3,4)); is.null(dim(a)) }");

        // rowSums on matrix have correct length
        assertEval("{ a = rowSums(matrix(1:12,3,4)); length(a) }");

        // rowSums on matrix have correct values
        assertEval("{ a = rowSums(matrix(1:12,3,4)); c(a[1],a[2],a[3]) }");

        // rowSums on array have no dimension
        assertEval("{ a = rowSums(array(1:24,c(2,3,4))); is.null(dim(a)) }");

        // row on array have correct length
        assertEval("{ a = rowSums(array(1:24,c(2,3,4))); length(a) }");

        // rowSums on array have correct values
        assertEval("{ a = rowSums(array(1:24,c(2,3,4))); c(a[1],a[2]) }");

        assertEval(Output.ContainsError, "{x<-matrix(c(\"1\",\"2\",\"3\",\"4\"),ncol=2);rowSums(x)}");
    }

    @Test
    public void testColSums() {
        // colSums on matrix drop dimension
        assertEval("{ a = colSums(matrix(1:12,3,4)); dim(a) }");

        // colSums on matrix have correct length
        assertEval("{ a = colSums(matrix(1:12,3,4)); length(a) }");

        // colSums on matrix have correct values
        assertEval("{ colSums(matrix(1:12,3,4)) }");

        // colSums on array have correct dimension
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); d = dim(a); c(d[1],d[2]) }");

        // colSums on array have correct length
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); length(a) }");

        // colSums on array have correct values
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); c(a[1,1],a[2,2],a[3,3],a[3,4]) }");
    }

    @Test
    public void testColMeans() {
        assertEval("{colMeans(matrix(c(3,4,2,5)))}");
        assertEval("{colMeans(matrix(c(3L,4L,2L,5L)))}");
        assertEval("{colMeans(matrix(c(TRUE,FALSE,FALSE,TRUE)))}");
        assertEval("{colMeans(matrix(c(3+2i,4+5i,2+0i,5+10i)))}");
        assertEval("{colMeans(matrix(c(3,4,NaN,5),ncol=2,nrow=2), na.rm = TRUE)}");
        assertEval("{colMeans(matrix(c(3,4,NaN,5),ncol=2,nrow=2), na.rm = FALSE)}");
        assertEval("{colMeans(matrix(c(3L,NaN,2L,5L),ncol=2,nrow=2), na.rm = TRUE)}");
        assertEval("{colMeans(matrix(c(3L,NA,2L,5L),ncol=2,nrow=2), na.rm = TRUE)}");
        assertEval("{colMeans(matrix(c(3L,NaN,2L,5L),ncol=2,nrow=2), na.rm = FALSE)}");
        assertEval("{colMeans(matrix(c(3L,NA,2L,5L),ncol=2,nrow=2), na.rm = FALSE)}");
        assertEval("{colMeans(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{colMeans(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{colMeans(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = FALSE)}");
        assertEval("{colMeans(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = FALSE)}");
        // Whichever value(NA or NaN) is first in the row will be returned for that row.
        assertEval("{colMeans(matrix(c(NA,NaN,NaN,NA),ncol=2,nrow=2))}");
        assertEval("{ a = colSums(array(1:24,c(2,3,4))); colMeans(a)}");

        assertEval(Ignored.Unknown, "{colMeans(matrix(c(NaN,4+5i,2+0i,5+10i),nrow=2,ncol=2), na.rm = TRUE)}");
    }

    @Test
    public void testCumulativeMax() {
        assertEval("{ cummax(c(1,2,3)) }");
        assertEval("{ cummax(NA) }");
        assertEval("{ cummax(c(2000000000L, NA, 2000000000L)) }");
        assertEval("{ cummax(1:10) }");
        assertEval("{ cummax(c(TRUE,FALSE,TRUE)) }");
        assertEval("{ cummax(c(TRUE,FALSE,NA,TRUE)) }");
        assertEval("{ cummax(as.logical(-2:2)) }");

        assertEval(Ignored.Unknown, "{ cummax(c(1+1i,2-3i,4+5i)) }");
        assertEval(Ignored.Unknown, "{ cummax(c(1+1i, NA, 2+3i)) }");
    }

    @Test
    public void testCumulativeMin() {
        assertEval("{ cummin(c(1,2,3)) }");
        assertEval("{ cummin(NA) }");
        assertEval("{ cummin(1:10) }");
        assertEval("{ cummin(c(2000000000L, NA, 2000000000L)) }");
        assertEval("{ cummin(c(TRUE,FALSE,TRUE)) }");
        assertEval("{ cummin(c(TRUE,FALSE,NA,TRUE)) }");
        assertEval("{ cummin(as.logical(-2:2)) }");

        // Error message mismatch.
        assertEval(Ignored.Unknown, "{ cummin(c(1+1i,2-3i,4+5i)) }");
        assertEval(Ignored.Unknown, "{ cummin(c(1+1i, NA, 2+3i)) }");
    }

    @Test
    public void testEncodeString() {
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = NA);}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = NA, justify = \"centre\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = NA, justify = \"right\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = NA, quote = \"'\", justify = \"right\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = 0, quote = \"'\", justify = \"right\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = 0, quote = \"\", justify = \"centre\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = 3, quote = \"'\", justify = \"centre\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = NA, quote = \"'\", na.encode=FALSE, justify = \"right\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = 0, quote = \"'\", na.encode=FALSE, justify = \"right\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = 3, quote = \"'\", na.encode=FALSE, justify = \"centre\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = 2, quote = \"'\", na.encode=FALSE, justify = \"centre\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = 7, quote = \"\", na.encode=FALSE, justify = \"centre\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = 7, quote = \"\", na.encode=TRUE, justify = \"centre\");}");
        assertEval("{x <- c(\"a\", \"ab\", \"abcde\", NA); encodeString(x, width = 3, quote = \"\", na.encode=TRUE, justify = \"centre\");}");
    }

    @Test
    public void testFactor() {

        assertEval("{data = c(1,2,2,3,1,2,3,3,1,2,3,3,1);fdata<-factor(data);fdata}");
        assertEval("{data = c(1,2,2,3,1,2,3,3,1,2,3,3,1);rdata = factor(data,labels=c(\"I\",\"II\",\"III\"));rdata;}");
        assertEval("{data = c(1,2,2,3,1,2,3,3,1,2,3,3,1);fdata<-factor(data);levels(fdata) = c('I','II','III');fdata;}");
        assertEval("{set.seed(124);l1 = factor(sample(letters,size=10,replace=TRUE));set.seed(124);l2 = factor(sample(letters,size=10,replace=TRUE));l12 = factor(c(levels(l1)[l1],levels(l2)[l2]));l12;}");
        assertEval("{set.seed(124); schtyp <- sample(0:1, 20, replace = TRUE);schtyp.f <- factor(schtyp, labels = c(\"private\", \"public\")); schtyp.f;}");
        // Checkstyle: stop line length check
        assertEval("{ses <- c(\"low\", \"middle\", \"low\", \"low\", \"low\", \"low\", \"middle\", \"low\", \"middle\", \"middle\", \"middle\", \"middle\", \"middle\", \"high\", \"high\", \"low\", \"middle\", \"middle\", \"low\", \"high\"); ses.f.bad.order <- factor(ses); is.factor(ses.f.bad.order);levels(ses.f.bad.order);ses.f <- factor(ses, levels = c(\"low\", \"middle\", \"high\"));ses.order <- ordered(ses, levels = c(\"low\", \"middle\", \"high\"));ses.order; } ");
        // Checkstyle: resume line length check

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-NULL; as.character(x) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-character(); as.character(x) }");
        assertEval(Output.ContainsError, "{ x<-c(1,2,3); class(x)<-\"factor\"; x }");
        assertEval(Output.ContainsError, "{ x<-c(\"1\",\"2\",\"3\"); class(x)<-\"factor\"; x }");
        assertEval(Output.ContainsError, "{ x<-c(1L,2L,3L); class(x)<-\"factor\"; x }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7L, 42L); x  }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7, 42); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(FALSE, TRUE); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7+7i, 42+42i); x }");
        assertEval(Output.ContainsError, "{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(as.raw(7), as.raw(42)); x }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x == \"a\" }");
        // these would fail if comparison was performed on strings
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(+7L, +42L); x == 7 }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(+7, +42); x == 7 }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(+7+7i, +42+42i); x == 7+7i }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(as.raw(7), as.raw(42)); x == as.raw(7) }");

        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x == c(\"a\", \"b\") }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\", \"c\")); x == c(\"a\", \"b\") }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\"), ordered=TRUE); x > \"a\" }");

        assertEval("{ x<-c(1L, 2L, 1L); class(x)<-c(\"ordered\", \"factor\"); levels(x)<-c(\"a\", \"b\"); x > \"a\" }");

        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"c\", \"b\", \"a\", \"c\")); y<-list(1); y[1]<-x; y }");
        assertEval(Output.ContainsWarning, "{ x<-factor(c(\"c\", \"b\", \"a\", \"c\")); y<-c(1); y[1]<-x; y }");
        assertEval("{ x<-factor(c(\"c\", \"b\", \"a\", \"c\")); y<-list(1); y[[1]]<-x; y }");
        assertEval(Output.ContainsError, "{ x<-factor(c(\"c\", \"b\", \"a\", \"c\")); y<-c(1); y[[1]]<-x; y }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[1] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[[1]] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[2] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[[2]] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[c(1,2)] }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); x[c(1,2,3,4)] }");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); is.atomic(x) }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\"), ordered=TRUE); is.atomic(x) }");

        assertEval("{ as.logical(factor(c(\"a\", \"b\", \"a\"))) }");
        assertEval("{ as.logical(factor(integer())) }");

        assertEval("{ x<-structure(c(1.1,2.2,1.1), .Label=c(\"a\", \"b\"), class = c('factor')); attributes(x) }");
        assertEval("{ x<-structure(c(1.1,2.2,1.1), .Label=c(\"a\", \"b\"), class = c('factor')); x }");
        assertEval("{ x<-structure(c(1.2,2.2,1.1), .Label=c(\"a\", \"b\"), class = c('factor')); x }");
        assertEval("{ x<-structure(c(2.2,3.2,2.1), .Label=c(\"a\", \"b\"), class = c('factor')); as.integer(x) }");

        assertEval("{ x<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); y<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); x==y }");

        assertEval("{ x<-structure(factor(c(\"a\",\"b\",\"c\")), class=NULL); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); levels(x)<-c(7,42); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); levels(x)<-c(7,42); is.character(levels(x)) }");

        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7L, 42L); is.integer(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7, 42); is.double(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(FALSE, TRUE); is.logical(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); levels(x)<-c(7, 42); is.character(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7+7i, 42+42i); is.complex(levels(x)) }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(as.raw(7), as.raw(42)); is.raw(levels(x)) }");

        assertEval("{ z=factor(c(\"a\", \"b\", \"a\")); z[1] = \"b\"; z }");

        assertEval("{ x<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); names(x) }");

        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x > \"a\" }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x + \"a\" }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x > c(\"a\", \"b\") }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\")); x + c(\"a\", \"b\") }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ x<-factor(c(\"a\", \"b\", \"a\"), ordered=TRUE); x + \"a\" }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ x<-c(1L, 2L, 1L); class(x)<-c(\"factor\", \"ordered\"); levels(x)<-c(\"a\", \"b\"); x > \"a\" }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ x<-c(1L, 2L, 1L); class(x)<-c(\"ordered\", \"factor\"); levels(x)<-c(\"a\", \"b\"); x + \"a\" }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ x<-c(1L, 2L, 1L); class(x)<-c(\"factor\", \"ordered\"); levels(x)<-c(\"a\", \"b\"); x + \"a\" }");
        assertEval(Ignored.Unknown,
                        Output.ContainsWarning,
                        "{ x<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); y<-structure(c(1,2,1), .Label=c(\"a\", \"b\"), class = c('factor'), .Names=c(\"111\",\"112\",\"113\")); x+y }");
    }

    @Test
    public void testCharMatch() {
        assertEval("{charmatch(\"abc\", \"deeee\",c(\"3\",\"4\"))}");
        assertEval("{charmatch(\"abc\", \"deeee\")}");
        assertEval("{charmatch(\"abc\", \"deeeec\",c(\"3\",\"4\"))}");
        assertEval("{charmatch(\"\", \"\")}");
        assertEval("{charmatch(\"m\",   c(\"mean\", \"median\", \"mode\"))}");
        assertEval("{charmatch(\"med\", c(\"mean\", \"median\", \"mode\"))}");
        assertEval("{charmatch(matrix(c(9,3,1,6),2,2,byrow=T), \"hello\")}");
        assertEval("{charmatch(matrix(c('h',3,'e',6),2,2,byrow=T), \"hello\")}");
        assertEval("{charmatch(c(\"ole\",\"ab\"),c(\"ole\",\"ab\"))}");
        assertEval("{charmatch(c(\"ole\",\"ab\"),c(\"ole\",\"ole\"))}");
        assertEval("{charmatch(matrix(c('h','l','e',6),2,2,byrow=T), \"hello\")}");
    }

    @Test
    public void testOnExit() {
        assertEval("n = function() { on.exit(print(\"test\")); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\", TRUE)); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\")); on.exit(); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\")); on.exit(print(\"test2\", TRUE)); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\")); on.exit(print(\"test2\")); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\", TRUE)); on.exit(print(\"test2\")); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\")); on.exit(print(\"test2\")); print(\"some\"); on.exit() }; n()");
        assertEval("n = function() { on.exit() }; n()");
    }

    @Test
    public void testTabulate() {
        assertEval("{tabulate(c(2,3,5))}");
        assertEval("{tabulate(c(2,3,3,5), nbins = 10)}");
        assertEval("{tabulate(c(-2,0,2,3,3,5))}");
        assertEval("{tabulate(c(-2,0,2,3,3,5), nbins = 3)}");
        assertEval("{tabulate(factor(letters[1:10]))}");
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
    public void testDiff() {
        assertEval("{ diff(1:10, 2) }");
        assertEval("{ diff(1:10, 2, 2) }");
        assertEval("{ x <- cumsum(cumsum(1:10)) ; diff(x, lag = 2) }");
        assertEval("{ x <- cumsum(cumsum(1:10)) ; diff(x, differences = 2) }");
    }

    @Test
    public void testInteraction() {
        assertEval("{ a <- gl(2, 4, 8) ; b <- gl(2, 2, 8, labels = c(\"ctrl\", \"treat\")) ; interaction(a, b) }");
        assertEval("{ a <- gl(2, 4, 8) ; b <- gl(2, 2, 8, labels = c(\"ctrl\", \"treat\")) ; s <- gl(2, 1, 8, labels = c(\"M\", \"F\")) ; interaction(a, b, s, sep = \":\") }");
    }

    @Test
    public void testSplit() {
        assertEval("{ split(1:10, 1:2) }");
        assertEval("{ ma <- cbind(x = 1:10, y = (-4:5)^2) ; split(ma, col(ma)) }");
        assertEval("{ fu <- c(1,2,2,1,2,2,1,2,2,1,2,2,1,2,2,1,2,2,1,1) ; split(1:20,fu) }");
        assertEval("{ fu <- c(\"a\",\"b\") ; split(1:8,fu) }");
        assertEval("{ g <- factor(round(c(0.4,1.3,0.6,1.8,2.5,4.1,2.2,1.0))) ; x <- c(0.1,3.2,1,0.6,1.9,3.3,1.6,1.7) + sqrt(as.numeric(g)) ; xg <- split(x, g) ; xg }");
        assertEval("{ x <- factor(c(\"a\", \"b\", \"a\")); attr(x, \"levels\")<-c(7L, 42L) ; split(1:3, x) }");
    }

    @Test
    public void testCol() {
        assertEval("{ ma <- matrix(1:12, 3, 4) ; col(ma) }");
        assertEval("{ ma <- cbind(x = 1:10, y = (-4:5)^2) ; col(ma) }");

        assertEval(Output.ContainsError, "{ col(c(1,2,3)) }");
    }

    @Test
    public void testTapply() {
        assertEval("{ ind <- list(c(1, 2, 2), c(\"A\", \"A\", \"B\")) ; tapply(1:3, ind) }");
        assertEval("{ n <- 17 ; fac <- factor(rep(1:3, length = n), levels = 1:5) ; tapply(1:n, fac, sum) }");
        assertEval("{ ind <- list(c(1, 2, 2), c(\"A\", \"A\", \"B\")) ; tapply(1:3, ind, sum) }");
    }

    @Test
    public void testNargs() {
        assertEval("{  f <- function (a, b, c) { nargs() }; f() }");
        assertEval("{  f <- function (a, b, c) { nargs() }; f(1, 2) }");
        assertEval("{  f <- function (a, b=TRUE, c=FALSE) { nargs() }; f(1) }");
        assertEval("{  f <- function (a, b=TRUE, c=FALSE) { nargs() }; f(1, FALSE) }");
        assertEval("{  f<-function(x, ..., y=TRUE) { nargs() }; f(1, 2, 3) }");

        assertEval("{  f <- function (a, b, c) { nargs() }; f(,,a) }");
    }

    @Test
    public void testEnvVars() {
        assertEval(Output.ContainsError, "{ Sys.setenv(\"a\") } ");
        assertEval("{ Sys.setenv(FASTR_A=\"a\"); Sys.getenv(\"FASTR_A\"); } ");
        REnvVars.unset("FASTR_A");
        assertEval("{ Sys.setenv(FASTR_A=\"a\", FASTR_B=\"b\"); Sys.getenv(c(\"FASTR_A\", \"FASTR_B\"));  } ");
        REnvVars.unset("FASTR_A");
        REnvVars.unset("FASTR_B");
        assertEval("{ Sys.getenv(\"FASTR_A\") } ");
        assertEval("{ Sys.getenv(\"FASTR_A\", unset=\"UNSET\") } ");
    }

    @Test
    public void testBitwiseFunctions() {
        assertEval("{ bitwAnd(c(10,11,12,13,14,15), c(1,1,1,1,1,1)) }");
        assertEval("{ bitwAnd(c(25,57,66), c(10,20,30,40,50,60)) }");
        assertEval("{ bitwAnd(c(10L,20L,30L,40L), c(3,5,7)) }");
        assertEval("{ bitwAnd(c(10.5,11.6,17.8), c(5L,7L,8L)) }");
        assertEval("{ bitwOr(c(10,11,12,13,14,15), c(1,1,1,1,1,1)) }");
        assertEval("{ bitwOr(c(25,57,66), c(10,20,30,40,50,60)) }");
        assertEval("{ bitwXor(c(10,11,12,13,14,15), c(1,1,1,1,1,1)) }");
        assertEval("{ bitwXor(c(25,57,66), c(10,20,30,40,50,60)) }");
        assertEval("{ bitwXor(20,30) }");
        assertEval("{ bitwShiftR(c(10,11,12,13,14,15), c(1,1,1,1,1,1)) }");
        assertEval("{ bitwShiftR(c(100,200,300), 1) }");
        assertEval("{ bitwShiftR(c(25,57,66), c(10,20,30,40,50,60)) }");
        assertEval("{ bitwShiftL(c(10,11,12,13,14,15), c(1,1,1,1,1,1)) }");
        assertEval("{ bitwShiftL(c(100,200,300), 1) }");
        assertEval("{ bitwShiftL(c(25,57,66), c(10,20,30,40,50,60)) }");
        assertEval("{ bitwNot(c(17,24,34,48,51,66,72,99)) }");
        assertEval("{ bitwNot(c(0,100,200,50,70,20)) }");

        assertEval(Output.ContainsError, "{ bitwXor(c(\"r\"), c(16,17)) }");
        assertEval(Output.ContainsError, "{ bitwShiftL(TRUE, c(TRUE, FALSE)) }");
        assertEval(Output.ContainsError, "{ bitwNot(TRUE) }");
        assertEval(Ignored.Unknown, Output.ContainsWarning, "{ bitwShiftR(c(3,2,4), c(3+3i)) }");

        assertEval(Ignored.Unknown, "{ bitwAnd(NULL, NULL) }");
        assertEval(Ignored.Unknown, "{ bitwAnd(c(), c(1,2,3)) }");
        // Error message mismatch
        assertEval(Output.ContainsError, "{ bitwAnd(c(1,2,3,4), c(TRUE)) }");
        assertEval(Output.ContainsError, "{ bitwOr(c(1,2,3,4), c(3+3i)) }");
        assertEval(Ignored.Unknown, Output.ContainsError, "{ bitwShiftL(c(3+3i), c(3,2,4)) }");
        // Warning message mismatch
        assertEval(Ignored.Unknown, "{ bitwShiftL(c(3,2,4), c(3+3i)) }");
        // No warning message printed for NAs produced by coercion
        assertEval(Ignored.Unknown, "{ bitwShiftR(c(1,2,3,4), c(\"Hello\")) }");
        assertEval(Ignored.Unknown, "{ bitwShiftL(c(1,2,3,4), c(\"a\")) }");
    }
}
