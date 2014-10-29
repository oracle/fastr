/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

public class TestConnections extends TestBase {
    @Test
    public void testTextReadConnection() {
        assertEval("{ con <- textConnection(c(\"1\", \"2\", \"3\",\"4\")); readLines(con) }");
        assertEval("{ con <- textConnection(c(\"1\", \"2\", \"3\",\"4\")); readLines(con, 2) }");
        assertEval("{ con <- textConnection(c(\"1\", \"2\", \"3\",\"4\")); readLines(con, 2); readLines(con, 2) }");
        assertEval("{ con <- textConnection(c(\"1\", \"2\", \"3\",\"4\")); readLines(con, 2); readLines(con, 2); readLines(con, 2) }");
    }

    @Test
    public void testPushBack() {
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBackLength(con) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(\"G\", con); pushBackLength(con) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(\"G\", con); clearPushBack(con); pushBackLength(con) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(\"G\", con); readLines(con, 1) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(\"G\", con); readLines(con, 2) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(\"G\", con, newLine=FALSE); readLines(con, 1) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(\"G\", con, newLine=FALSE); readLines(con, 2) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\", \"H\"), con); pushBackLength(con) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\", \"H\"), con); readLines(con, 1) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\", \"H\"), con); readLines(con, 2) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\", \"H\"), con, newLine=FALSE); pushBackLength(con) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\", \"H\"), con, newLine=FALSE); readLines(con, 1) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\", \"H\"), con, newLine=FALSE); readLines(con, 2) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\\nH\"), con); pushBackLength(con) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\\nH\"), con); readLines(con, 1) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\\nH\"), con); readLines(con, 2) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\\nH\"), con, newLine=FALSE); pushBackLength(con) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\\nH\"), con, newLine=FALSE); readLines(con, 1) }");
        assertEval("{ con<-textConnection(c(\"a\",\"b\",\"c\",\"d\")); pushBack(c(\"G\\nH\"), con, newLine=FALSE); readLines(con, 2) }");
    }
}
