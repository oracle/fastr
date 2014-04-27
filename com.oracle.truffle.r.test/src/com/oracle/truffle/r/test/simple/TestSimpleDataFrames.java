/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.simple;

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
        // list turned data frame is still a list (and a vector)
        assertEval("{ x<-list(c(7,42),c(1+1i, 2+2i)); class(x)<-\"data.frame\"; is.vector(x) }");
        assertEval("{ x<-list(c(7,42),c(1+1i, 2+2i)); class(x)<-\"data.frame\"; is.list(x) }");
        assertEval("{ x<-list(c(7,42),c(1+1i, 2+2i)); class(x)<-c(\"foo\", \"data.frame\", \"bar\"); is.data.frame(x) }");

        assertEval("{ x<-c(7,42); class(x)<-\"data.frame\"; is.data.frame(x) }");
        // vector turned data frame is not a list (but it's still a vector)
        assertEval("{ x<-c(7,42); class(x)<-\"data.frame\"; is.vector(x) }");
        assertEval("{ x<-c(7,42); class(x)<-\"data.frame\"; is.list(x) }");
        assertEval("{ x<-c(7,42); class(x)<-c(\"foo\", \"data.frame\", \"bar\"); is.data.frame(x) }");
    }
}
