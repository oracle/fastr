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
}
