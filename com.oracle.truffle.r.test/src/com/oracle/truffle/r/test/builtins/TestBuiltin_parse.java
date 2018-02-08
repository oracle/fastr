/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
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

    @Test
    public void testParseData() {
        assertEvalFastR("p <- parse(text = 'x = 1', keep.source = TRUE); attr(p, 'srcfile')$parseData",
                        "structure(c(1L, 1L, 1L, 1L, 1L, 263L, 0L, 0L), text = \"x\", tokens = \"SYMBOL\", class = \"parseData\", .Dim = c(8L, 1L))");
        assertEvalFastR("p <- parse(text = 'x = x + 1; rnorm(1, std = z); f2 <- function(a=1) a', keep.source = TRUE); attr(p, 'srcfile')$parseData",
                        "structure(c(1L, 1L, 1L, 1L, 1L, 263L, 0L, 0L, 1L, 5L, 1L, 5L, 1L, 263L, 1L, 0L, 1L, 12L, 1L, 28L, 1L, 296L, 2L, 0L, 1L, 27L, 1L, 27L, 1L, 263L, 3L, 0L, 1L, 31L, 1L, 32L, 1L, 263L, 4L, 0L, 1L, 51L, 1L, 51L, 1L, 263L, 5L, 0L), text = c(\"x\", \"x\", \"rnorm\", \"z\", \"f2\", \"a\"), tokens = c(\"SYMBOL\", \"SYMBOL\", \"SYMBOL_FUNCTION_CALL\", \"SYMBOL\", \"SYMBOL\", \"SYMBOL\"), class = \"parseData\", .Dim = c(8L, 6L))");
    }

}
