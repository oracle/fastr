/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_listfiles extends TestBase {

    @Test
    public void testlistfiles1() {
        assertEval("argv <- list('.', 'myTst_.*tar\\\\.gz$', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testlistfiles2() {
        assertEval("argv <- list('./myTst/data', NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testlistfiles3() {
        // FastR bug; not recursing in to "."
        assertEval(Output.IgnoreWhitespace,
                        "print('Work dir: ', getwd()); argv <- list('.', '^CITATION.*', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE); sort(.Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]])))");
    }

    @Test
    public void testlistfiles4() {
        assertEval("argv <- list('mgcv', NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testFileListing() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\") }");
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE) }");
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE, pattern=\".*dummy.*\") }");
        assertEval("{ list.files(\"test/r/simple/data/tree1\", recursive=TRUE, pattern=\"dummy\") }");

        // TODO Why does GnuR not require the leading "." when Java does?
        assertEval("{ list.files(\"test/r/simple/data/tree1\", pattern=\".*.tx\") }");
    }

    @Test
    public void testFileListingUsingFilePatterns() {
        assertEval("{ list.files(\"test/r/simple/data/tree1\", pattern=\"*.txt\") }");
        assertEval("{ list.files(\"test/r/simple/data/tree1\", pattern=\"$$$.txt\") }");
    }

}
