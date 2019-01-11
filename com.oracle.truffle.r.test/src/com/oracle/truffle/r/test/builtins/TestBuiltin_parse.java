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
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_parse extends TestBase {

    @Test
    public void testSource() {
        // FIXME
        // FastR repeats file name twice in the warning in this way:
        // cannot open file 'test/r/simple/data/tree2/setx.r': test/r/simple/data/tree2/setx.r (No
        // such file or directory)
        assertEval(Output.IgnoreWarningMessage, "{ source(\"test/r/simple/data/tree2/setx.r\") ; x }");
        assertEval(Output.IgnoreWarningMessage, "{ source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x }");
        assertEval(Output.IgnoreWarningMessage, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x } ; c(f(), x) }");
        assertEval(Output.IgnoreWarningMessage, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=FALSE) ; x } ; c(f(), x) }");
        assertEval(Output.IgnoreWarningMessage, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=FALSE) ; x } ; c(f(), x) }");
        assertEval(Output.IgnoreWarningMessage, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=TRUE) ; x } ; c(f(), x) }");
    }

    @Test
    public void testParseVector() {
        assertEval("parse(text=deparse(c(1, 2, 3)))");
        assertEval("{ parse(text=c(\"for (i in 1:10) {\", \"    x[i] <- i\", \"}\")) }");
    }

    @Test
    public void testParseDataFrame() {
        assertEval("eval(parse(text=deparse(data.frame(x=c(1)))))");
    }

    @Test
    public void test() {
        assertEval("{ typeof(parse(text = \"foo\", keep.source = FALSE, srcfile = NULL)[[1]]) }");
        assertEval("{ parse(text=\"NULL\") }");
    }

    @Test
    public void testParseIdentifier() {
        assertEval("parse(text='is.null')");
        assertEval(Ignored.ImplementationError, "attributes(parse(text='is.null'))");
        assertEval("parse(text='somethingthatdoesnotexist')");
        assertEval(Ignored.ImplementationError, "attributes(parse(text='somethingthatdoesnotexist'))");
    }

    @Test
    public void testArgumentsCasts() {
        assertEval(".Internal(parse(stdin(), c(1,2), c('expr1', 'expr2'), '?', '<weird-text', 'unknown'))");
    }

    @Test
    public void testSrcfile() {
        assertEval("parse(text='', srcfile=srcfile(system.file('testfile')))");
    }

    // TODO: correct parsing metadata for string constants with escape sequences and similar
    private final String[] PARSE_DATA_TESTS = {
                    "x + 1",
                    "foo(1,g,b=a)",
                    "foo %myop% 42",
                    "{ symbol }",
                    "x = x + 1",
                    "x = 1",
                    "x     = 1",
                    "x <- x + 1",
                    "attr(x,\"mya\") <- bar()",
                    "foo[[bar=xyz %myop% 42]]"};

    @Test
    public void testParseData() {

        // This R code normalizes the IDs to be always from 1:nrow
        // We also need to overwrite the rownames to normalized value
        // However, getParseData should already order the elements according to the position in the
        // source and that order should be the same in FastR and GNU-R regardless of the internals
        // of the implementation
        String testTemplate = "{ tmp <- getParseData(parse(text='%0', keep.source=T)); " +
                        "tmp$parent <- match(tmp$parent, tmp$id, nomatch=0); " +
                        "tmp$id <- 1:nrow(tmp); " +
                        "rownames(tmp) <- 1:nrow(tmp); tmp }";
        assertEval(template(testTemplate, PARSE_DATA_TESTS));
    }
}
