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
import java.io.IOException;

// Checkstyle: stop line length check

public class TestBuiltin_parse extends TestBase {

    @Test
    public void testSource() {
        assertEval(Ignored.Unknown, "{ source(\"test/r/simple/data/tree2/setx.r\") ; x }");
        assertEval(Ignored.Unknown, "{ source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x }");
        assertEval(Ignored.Unknown, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=TRUE) ; x } ; c(f(), x) }");
        assertEval(Ignored.Unknown, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/setx.r\", local=FALSE) ; x } ; c(f(), x) }");
        assertEval(Ignored.Unknown, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=FALSE) ; x } ; c(f(), x) }");
        assertEval(Ignored.Unknown, "{ x <- 1; f <- function() { source(\"test/r/simple/data/tree2/incx.r\", local=TRUE) ; x } ; c(f(), x) }");
    }

    @Test
    public void testParseVector() {
        assertEval("parse(text=deparse(c(1, 2, 3)))");
        assertEval("{ parse(text=c(\"for (i in 1:10) {\", \"    x[i] <- i\", \"}\")) }");
    }

    @Test
    public void testParseDataFrame() {
        assertEval(Ignored.ImplementationError, "eval(parse(text=deparse(data.frame(x=c(1)))))");
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
    public void testSrcfile() throws IOException {
        assertEval("parse(text='', srcfile=srcfile(system.file('testfile')))");
    }

}
