/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestRBase;

// Checkstyle: stop line length check
public class TestBuiltin_rawConnection extends TestRBase {

    @Override
    protected String getTestDir() {
        return "builtins/connection";
    }

    @Test
    public void testReadAppendText() {

        assertEval("{ rc <- rawConnection(raw(0), \"a+\"); close(rc); write(charToRaw(\"A\"), rc) }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"a+\"); writeChar(\", World\", rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"a+\"); writeChar(\", World\", rc); res <- rawToChar(rawConnectionValue(rc)); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"a+\"); write(charToRaw(\", World\"), rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"a+\"); write(charToRaw(\", World\"), rc); res <- rawToChar(rawConnectionValue(rc)); close(rc); res }");
    }

    @Test
    public void testReadWriteText() {

        assertEval("{ rc <- rawConnection(raw(0), \"r+\"); close(rc); write(charToRaw(\"A\"), rc) }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"r+\"); writeChar(\", World\", rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"r+\"); writeChar(\", World\", rc); res <- rawToChar(rawConnectionValue(rc)); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"r+\"); write(charToRaw(\", World\"), rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"r+\"); write(charToRaw(\", World\"), rc); res <- rawToChar(rawConnectionValue(rc)); close(rc); res }");
    }

    @Test
    public void testWriteText() {

        assertEval("{ s <- \"äöüß\"; rc <- rawConnection(raw(0), \"w\"); writeChar(s, rc); rawConnectionValue(rc) }");
        assertEval("{ rc <- rawConnection(raw(0), \"w\"); writeChar(\"Hello\", rc); writeChar(\", World\", rc); res <- rawConnectionValue(rc); close(rc); res }");
    }

    @Test
    public void testWriteBinary() {
        // this test is currently ignored, since 'charToRaw' is not compliant
        assertEval(Ignored.Unknown, "{ s <- \"äöüß\"; rc <- rawConnection(raw(0), \"wb\"); write(charToRaw(s), rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval("{ zz <- rawConnection(raw(0), \"wb\"); x <- c(\"a\", \"this will be truncated\", \"abc\"); nc <- c(3, 10, 3); writeChar(x, zz, nc, eos = NULL); writeChar(x, zz, eos = \"\\r\\n\"); res <- rawConnectionValue(zz); close(zz); res }");
    }

}
