/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.base;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestSimpleDataFrames extends TestBase {

    @Test
    public void testIsDataFrame() {
        assertEval("{ is.data.frame(1) }");
        assertEval("{ is.data.frame(NULL) }");
        assertEval("{ x<-c(1,2); is.data.frame(x) }");
        assertEval("{ x<-list(1,2); is.data.frame(x) }");

        assertEval("{ x<-list(c(7,42),c(1+1i, 2+2i)); class(x)<-\"data.frame\"; is.data.frame(x) }");
        // list turned data frame is still a list
        assertEval("{ x<-list(c(7,42),c(1+1i, 2+2i)); class(x)<-\"data.frame\"; is.vector(x) }");
        assertEval("{ x<-list(c(7,42),c(1+1i, 2+2i)); class(x)<-\"data.frame\"; is.list(x) }");
        assertEval("{ x<-list(c(7,42),c(1+1i, 2+2i)); class(x)<-c(\"foo\", \"data.frame\", \"bar\"); is.data.frame(x) }");

        assertEval("{ x<-c(7,42); class(x)<-\"data.frame\"; is.data.frame(x) }");
        // vector turned data frame is not a list
        assertEval("{ x<-c(7,42); class(x)<-\"data.frame\"; is.vector(x) }");
        assertEval("{ x<-c(7,42); class(x)<-\"data.frame\"; is.list(x) }");
        assertEval("{ x<-c(7,42); class(x)<-c(\"foo\", \"data.frame\", \"bar\"); is.data.frame(x) }");

        // data frame turned (back) into a vector
        assertEval("{ x<-c(7,42); class(x)<-\"data.frame\"; class(x)<-NULL; is.vector(x) }");
        // vector obtained from data frame retains data frame's attributes
        assertEval("{ x<-c(7,42); class(x)<-\"data.frame\"; attr(x, \"foo\")<-\"foo\"; class(x)<-NULL;  attributes(x) }");
    }

    @Test
    public void testRowNames() {
        // testing row.names
        assertEval(Output.ContainsError, "{ x<-c(1,2); row.names(x)<-c(7, 42); attributes(x) }");
        assertEval("{ x<-c(1,2); row.names(x)<-NULL; attributes(x) }");
        assertEval(Output.ContainsError, "{ x<-c(1,2); row.names(x)<-logical(); attributes(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-2; row.names(x)<-c(7, 42); attributes(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-2; row.names(x)<-c(7, 42); row.names(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-2; row.names(x)<-NULL; attributes(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-2; row.names(x)<-NULL; row.names(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-2; row.names(x)<-logical(); attributes(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-2; row.names(x)<-logical(); row.names(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(1.1, c(2.2, 3.3)); row.names(x)<-7; attributes(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(1.1, c(2.2, 3.3)); row.names(x)<-7; row.names(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(1.1, c(2.2, 3.3)); row.names(x)<-NULL; attributes(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(1.1, c(2.2, 3.3)); row.names(x)<-NULL; row.names(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(1.1, c(2.2, 3.3)); row.names(x)<-logical(); attributes(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(1.1, c(2.2, 3.3)); row.names(x)<-logical(); row.names(x) }");
        assertEval(Output.ContainsError, "{ x<-c(1,2); dim(x)<-c(2,1); dimnames(x)<-list(c(2.2, 3.3), 1.1); row.names(x)<-7; attributess(x) }");
        assertEval(Output.ContainsError, "{ x<-c(1,2); dim(x)<-c(2,1); dimnames(x)<-list(c(2.2, 3.3), 1.1); row.names(x)<-7; row.names(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(2,1); dimnames(x)<-list(c(2.2, 3.3), 1.1); row.names(x)<-c(7, 42); attributes(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(2,1); dimnames(x)<-list(c(2.2, 3.3), 1.1); row.names(x)<-c(7, 42); row.names(x) }");

        assertEval("{ x<-c(1,2,3); y<-c(4,5); z<-list(x, y); class(z)<-\"data.frame\"; row.names(z)<-NULL; attributes(z) }");
        assertEval("{ x<-c(1,2,3); y<-c(4,5); z<-list(x, y); class(z)<-\"data.frame\"; row.names(z)<-c(\"a\", \"b\"); row.names(z)<-NULL; attributes(z) }");
        assertEval("{ x<-c(1,2,3); y<-c(4,5); z<-list(x, y); class(z)<-\"data.frame\"; row.names(z)<-c(\"a\", \"b\", \"c\"); row.names(z)<-NULL; attributes(z) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(1.1, c(2.2, 3.3)); class(x)<-\"data.frame\"; row.names(x) }");
        assertEval("{ x<-c(1,2); dim(x)<-c(1,2); dimnames(x)<-list(1.1, c(2.2, 3.3)); class(x)<-\"data.frame\"; row.names(x)<-\"r1\"; row.names(x) }");
    }

    @Test
    public void testAsDataFrame() {
        assertEval("{ x<-list(1,2); class(x)<-\"data.frame\"; row.names(x)<-\"r1\"; y<-as.data.frame(x, \"r2\"); attributes(x) }");
        assertEval("{ x<-list(1,2); class(x)<-\"data.frame\"; row.names(x)<-\"r1\"; y<-as.data.frame(x, \"r2\"); attributes(y) }");
        assertEval(Output.ContainsError, "{ x<-list(1,2); class(x)<-\"data.frame\"; row.names(x)<-\"r1\"; y<-as.data.frame(x, c(\"r1\", \"r2\")); attributes(y) }");
        assertEval("{ x<-c(7L,42L); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); attributes(y); }");
        assertEval("{ x<-as.double(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); attributes(y); }");
        assertEval("{ x<-as.logical(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); attributes(y); }");
        assertEval("{ x<-as.character(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); attributes(y); }");
        assertEval("{ x<-as.complex(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); attributes(y); }");
        assertEval("{ x<-as.raw(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); attributes(y); }");
        assertEval("{ x<-c(7L,42L); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); is.data.frame(y); }");
        assertEval("{ x<-as.double(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); is.data.frame(y); }");
        assertEval("{ x<-as.logical(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); is.data.frame(y); }");
        assertEval("{ x<-as.character(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); is.data.frame(y); }");
        assertEval("{ x<-as.complex(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); is.data.frame(y); }");
        assertEval("{ x<-as.raw(c(7L,42L)); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); is.data.frame(y); }");
        assertEval(Output.ContainsWarning, "{ x<-c(7L,42L); y<-as.data.frame(x, row.names=\"r1\", nm=\"x\"); attributes(y); }");
        assertEval("{ x<-c(7L,42L); y<-as.data.frame(x, row.names=c(\"r1\", \"r2\"), nm=\"x\"); attributes(y); }");
        assertEval(Output.ContainsWarning, "{ x<-c(7L,42L); y<-as.data.frame(x, row.names=c(\"r1\", \"r2\", \"r3\"), nm=\"x\"); attributes(y); }");
        assertEval("{ x<-matrix(c(1,2,3,4), nrow=2); y<-as.data.frame(x, row.names=NULL, optional=FALSE); attributes(y); }");
        assertEval("{ x<-matrix(c(1,2,3,4), nrow=2); y<-as.data.frame(x, row.names=\"r1\", optional=FALSE); attributes(y); }");
        assertEval(Output.ContainsError, "{ x<-1; class(x)<-\"foo\"; y<-as.data.frame(x) }");
    }

    @Test
    public void testAccess() {
        assertEval("{ x<-list(7,42); class(x)<-\"data.frame\"; row.names(x)<-\"r1\"; x[[1]] }");
        assertEval("{ x<-c(7,42); y<-as.data.frame(x, row.names=NULL, nm=\"x\"); y[[1]] }");

        assertEval("{ x<-data.frame(a=c(1,2), b=c(11,12)); x[[1,2]] }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(11,12)); x[1,2] }");
        assertEval("{ x<-data.frame(a=list(1,2), b=list(11,12)); x[[1,2]] }");
        assertEval("{ x<-data.frame(a=list(1,2), b=list(11,12)); x[1,2] }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(11,12)); x[c(1,2),2] }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(11,12)); x[[c(1,2),2]] }");

        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); attr(x, \"foo\")<-\"foo\"; x[1, c(1,2)] }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); attr(x, \"foo\")<-\"foo\"; attributes(x[1, c(1,2)]) }");

        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); x[,\"b\"] }");

        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); x[NULL, \"a\"] }");
        assertEval("{ x<-data.frame(a=factor(c(\"y\", \"z\", \"y\")), b=c(3,4,5)); x[NULL, \"a\"] }");

        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); x[logical(), \"b\"] }");
        assertEval("{ x<-data.frame(a=c(1L,2L), b=c(3L,4L)); x[logical(), \"b\"] }");
        assertEval("{ x<-data.frame(a=1, b=2); x[logical(), \"b\"] }");
        assertEval("{ x<-data.frame(a=1L, b=2L); x[logical(), \"b\"] }");

        assertEval("{ x<-data.frame(a=1:2, b=3:4, c=5:6); x[1, c(1,3)] }");
        assertEval("{ x<-data.frame(c(1,2), c(3,4)); x[1] }");

        assertEval("{ x<-data.frame(a=c(1,2), b=c(3,4)); x[\"a\"] }");
    }

    @Test
    public void testUpdate() {
        assertEval("{ n = c(2, 3, 5); s = c(\"aa\", \"bb\", \"cc\"); df = data.frame(n, s); df[[1]] <- c(22,33,55); df }");

        assertEval("{ x<-data.frame(c(1,2), c(3,4)); x[c(1,2)]<-list(c(11,12), c(13,14)); x }");
        assertEval("{ x<-data.frame(c(1,2), c(3,4), c(5,6)); x[c(1,2, 3)]<-list(c(11,12), c(13,14), c(15,16)); x }");
    }

    @Test
    public void testPrint() {
        assertEval("{x<-c(1,2); class(x)<-\"data.frame\"; x}");
        assertEval("{ x<-integer(); class(x)<-\"data.frame\"; x }");
        assertEval("{ x<-c(1,2); class(x)<-\"data.frame\"; row.names(x)<-integer(); x }");

        assertEval("{ x<-c(1,2); y<-data.frame(x); y }");
        assertEval("{ x<-c(7,42); y<-data.frame(x); y }");
        assertEval("{ n = c(2, 3, 5); s = c(TRUE, FALSE, TRUE); df = data.frame(n, s); df }");
        assertEval("{ x<-data.frame(n=c(\"2\", \"3\", \"5\"), s=c(\"TRUE\", \"FALSE\", \"TRUE\"), check.names=FALSE, row.names=c(\"1\", \"2\", \"3\")); x }");
    }

    @Test
    public void testDataFrame() {
        assertEval("{ x<-c(7,42); y<-data.frame(x); is.data.frame(y) }");
        assertEval("{ data.frame(c(1,2)) }");
        assertEval("{ data.frame(c(1,2), c(11,12)) }");
    }

    @Test
    public void testLapply() {
        assertEval("{ x <- c(1, 2, 3); xa <- as.data.frame(x); lapply(xa, function(x) x > 1) }");
    }

    @Test
    public void testMisc() {
        assertEval("{ y<-data.frame(7); as.logical(y) }");
        assertEval("{ y<-data.frame(integer()); as.logical(y) }");
        assertEval(Output.ContainsError, "{ y<-data.frame(c(1,2,3)); as.logical(y) }");

        assertEval("{ y<-data.frame(c(1,2,3)); length(y) }");

    }

}
