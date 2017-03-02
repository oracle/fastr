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

public class TestBuiltin_scan extends TestBase {

    @Test
    public void testScan() {
        // from scan's documentation
        assertEval("{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, skip = 1, quiet = TRUE) }");
        assertEval("{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, skip = 1) }");
        assertEval("{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, skip = 1, nlines = 1) }");
        assertEval("{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, what = list(\"\",\"\",\"\")) }");
        assertEval("{ con<-textConnection(c(\"TITLE extra line\", \"2 3 5 7\", \"11 13 17\")); scan(con, what = list(\"\",\"\",\"\"), flush=TRUE) }");

        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, skip = 1) }");
        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, skip = 1, quiet=TRUE) }");
        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, skip = 1, nlines = 1) }");
        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, what = list(\"\",\"\",\"\")) }");

        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, what = list(\"\",\"\",\"\"), fill=TRUE) }");
        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, what = list(\"\",\"\",\"\"), multi.line=FALSE) }");
        assertEval("{ con<-textConnection(c(\"HEADER\", \"7 2 3\", \"4 5 42\")); scan(con, what = list(\"\",\"\",\"\"), fill=TRUE, multi.line=FALSE) }");

        assertEval("{ con<-textConnection(c(\"\\\"2\\\"\", \"\\\"11\\\"\")); scan(con, what=list(\"\")) }");
        assertEval("{ con<-textConnection(c(\"2 3 5\", \"\", \"11 13 17\")); scan(con, what=list(\"\")) }");
        assertEval("{ con<-textConnection(c(\"2 3 5\", \"\", \"11 13 17\")); scan(con, what=list(\"\"), blank.lines.skip=FALSE) }");
        assertEval("{ con<-textConnection(c(\"2 3 5\", \"\", \"11 13 17\")); scan(con, what=list(integer()), blank.lines.skip=FALSE) }");

        assertEval("{ con<-textConnection(c(\"foo faz\", \"\\\"bar\\\" \\\"baz\\\"\")); scan(con, what=list(\"\", \"\")) }");
        assertEval("{ con<-textConnection(c(\"foo faz\", \"bar \\\"baz\\\"\")); scan(con, what=list(\"\", \"\")) }");
        assertEval("{ con<-textConnection(c(\"foo, faz\", \"bar, baz\")); scan(con, what=list(\"\", \"\"), sep=\",\") }");

        assertEval("{ con<-textConnection(c(\"bar'foo'\")); scan(con, what=list(\"\")) }");
        assertEval("{ con<-textConnection(c(\"'foo'\")); scan(con, what=list(\"\")) }");
        assertEval("{ con<-textConnection(c(\"bar 'foo'\")); scan(con, what=list(\"\")) }");
    }

    @Test
    public void testArgsCasts() {
        // Empty 2nd 'what' parameter
        assertEval(Output.IgnoreErrorContext, "{ con<-textConnection(c(\"1 2 3\", \"4 5 6\")); .Internal(scan(con, , 2, ' ', '.', '\"', 0, 3, \"NA\", F, F, F, T, T, '', '#', T, 'utf8', F)) }");
        // NULL 2nd 'what' parameter
        assertEval("{ con<-textConnection(c(\"1 2 3\", \"4 5 6\")); .Internal(scan(con, NULL, 2, ' ', '.', '\"', 0, 3, \"NA\", F, F, F, T, T, '', '#', T, 'utf8', F)) }");
        // function passed as 2nd 'what' parameter
        assertEval("{ con<-textConnection(c(\"1 2 3\", \"4 5 6\")); .Internal(scan(con, print, 2, ' ', '.', '\"', 0, 3, \"NA\", F, F, F, T, T, '', '#', T, 'utf8', F)) }");
        // NULL 4th 'sep' parameter
        assertEval("{ con<-textConnection(c(\"1 2 3\", \"4 5 6\")); .Internal(scan(con, 1L, 2, NULL, '.', '\"', 0, 3, \"NA\", F, F, F, T, T, '', '#', T, 'utf8', F)) }");
        // NULL 5th 'dec' parameter
        assertEval("{ con<-textConnection(c(\"1.5 2.89 3\", \"4 5 6\")); .Internal(scan(con, 1.2, 2, ' ', NULL, '\"', 0, 3, \"NA\", F, F, F, T, T, '', '#', T, 'utf8', F)) }");

    }
}
