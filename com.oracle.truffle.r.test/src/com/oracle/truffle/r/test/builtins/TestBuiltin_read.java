/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

/**
 * Tests of read* builtins for example read.dcf.
 */
public class TestBuiltin_read extends TestBase {

    private static final String DCF_CONTENT = "Authors@R: c(\n" +
                    "  person('John', 'Doe'))\n" +
                    "Collate: 'assertions.R' 'built.R'\n" +
                    // Whitespaces are at the beginning of the line and at the end of the line on
                    // purpose
                    "    'collate.R' 'constants.R  '\n";

    private static final String[] KEEP_WHITE = new String[]{"'Authors@R'", "'Collate'", "NULL", "c('Authors@R', 'Collate')"};

    @Test
    public void testReadDCF() {
        assertEval("{ f <- tempfile(); write('hi', file=f); read.dcf(f); unlink(f); }");
    }

    /**
     * We have to keep white spaces correctly in .Internal(readDCF) because desc package expects
     * that.
     */
    @Test
    public void testReadDCFKeepWhiteSpaces() {
        assertEval(template("{ f <- tempfile(); write(\"%0\", file=f); print(read.dcf(f)); unlink(f); }", new String[]{DCF_CONTENT}));
        assertEval(template("{ f <- tempfile(); write(\"%0\", file=f); print(read.dcf(f, keep.white=%1, all=FALSE)); unlink(f); }", new String[]{DCF_CONTENT}, KEEP_WHITE));
        assertEval(template("{ f <- tempfile(); write(\"%0\", file=f); print(read.dcf(f, keep.white=%1, all=TRUE)); unlink(f); }", new String[]{DCF_CONTENT}, KEEP_WHITE));
    }

}
