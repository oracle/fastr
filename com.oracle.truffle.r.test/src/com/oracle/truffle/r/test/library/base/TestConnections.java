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

import java.nio.file.*;

import org.junit.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.test.*;

public class TestConnections extends TestBase {
    private static final class TestDir {
        private final Path testDirPath;

        TestDir() {
            Path rpackages = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test");
            testDirPath = TestBase.relativize(rpackages.resolve("library.base.conn"));
            if (!testDirPath.toFile().exists()) {
                testDirPath.toFile().mkdir();
            }
        }

        String[] subDir(String p) {
            return new String[]{testDirPath.resolve(p).toString()};
        }
    }

    private static TestDir testDir;

    @BeforeClass
    public static void setupTestDir() {
        testDir = new TestDir();
    }

    @AfterClass
    public static void teardownTestDir() {
        if (!deleteDir(testDir.testDirPath)) {
            throw new AssertionError();
        }
    }

    @Test
    public void testFileWriteReadLines() {
        assertEval(TestBase.template("{ writeLines(c(\"line1\", \"line2\"), file(\"%0\")) }", testDir.subDir("wl1")));
        assertEval(TestBase.template("{ readLines(file(\"%0\"), 2) }", testDir.subDir("wl1")));
        assertEval(TestBase.template("{ con <- file(\"%0\"); writeLines(c(\"line1\", \"line2\"), con) }", testDir.subDir("wl2")));
        assertEval(TestBase.template("{ con <- file(\"%0\"); readLines(con, 2) }", testDir.subDir("wl2")));
    }

    @Test
    public void testFileWriteReadChar() {
        assertEval(TestBase.template("{ writeChar(\"abc\", file(\"%0\")) }", testDir.subDir("wc1")));
        assertEval(TestBase.template("{ readChar(file(\"%0\"), 3) }", testDir.subDir("wc1")));
    }

    @Test
    public void testFileWriteReadBin() {
        assertEval(TestBase.template("{ writeBin(\"abc\", file(\"%0\", open=\"wb\")) }", testDir.subDir("wb1")));
        assertEval(TestBase.template("{ readBin(file(\"%0\", \"rb\"), 3) }", testDir.subDir("wb1")));
    }

    @Test
    public void testWriteTextReadConnection() {
        assertEval(Output.ContainsError, "{ writeChar(\"x\", textConnection(\"abc\")) }");
    }

    @Test
    public void testTextReadConnection() {
        assertEval("{ con <- textConnection(c(\"1\", \"2\", \"3\",\"4\")); readLines(con) }");
        assertEval("{ con <- textConnection(c(\"1\", \"2\", \"3\",\"4\")); readLines(con, 2) }");
        assertEval("{ con <- textConnection(c(\"1\", \"2\", \"3\",\"4\")); readLines(con, 2); readLines(con, 2) }");
        assertEval("{ con <- textConnection(c(\"1\", \"2\", \"3\",\"4\")); readLines(con, 2); readLines(con, 2); readLines(con, 2) }");
    }

    @Test
    public void testPushBackTextConnection() {
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

    @Test
    public void testWriteConnection() {
        assertEval("{ con <- textConnection(\"tcval\", open=\"w\"); writeLines(\"a\", con); tcval; close(con) }");
        assertEval("{ con <- textConnection(\"tcval\", open=\"w\"); writeLines(\"a\", con); writeLines(c(\"a\", \"b\"), con, sep=\".\"); tcval; close(con) }");
        assertEval("{ con <- textConnection(\"tcval\", open=\"w\"); writeLines(\"a\", con); writeLines(c(\"a\", \"b\"), con, sep=\".\"); writeLines(\"\", con); tcval; close(con) }");
        assertEval("{ con <- textConnection(\"tcval\", open=\"w\"); writeLines(\"a\\nb\", con); tcval; close(con) }");
        assertEval(Ignored.Unimplemented, "c <- textConnection('out', 'w'); cat('testtext', file=c); isIncomplete(c); cat('testtext2\\n', file=c); isIncomplete(c); close(c); out");

        assertEval("{ d<-data.frame(c(1,2), c(10, 20)); buf<-character(); c<-textConnection(\"buf\", open=\"w\", local=T); write.table(d, c); buf }");
    }
}
