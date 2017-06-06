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
public class TestBuiltin_gregexpr extends TestBase {

    @Test
    public void testgregexpr1() {
        assertEval("argv <- list('', 'abc', FALSE, FALSE, FALSE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr2() {
        assertEval("argv <- list('[^\\\\.\\\\w:?$@[\\\\]]+', 'version$m', FALSE, TRUE, FALSE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr3() {
        assertEval("argv <- list('$', 'version$m', FALSE, FALSE, TRUE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr4() {
        // FIXME FastR ignores useBytes argument value (last argument)
        assertEval(Ignored.ImplementationError,
                        "argv <- list('éè', '«Latin-1 accented chars»: éè øØ å<Å æ<Æ é éè', FALSE, FALSE, TRUE, TRUE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr5() {
        assertEval("argv <- list('', 'abc', FALSE, TRUE, FALSE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr6() {
        assertEval("argv <- list('', 'abc', FALSE, FALSE, TRUE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr7() {
        // FIXME FastR ignores useBytes argument value (last argument)
        assertEval(Ignored.ImplementationError,
                        "argv <- list('éè', '«Latin-1 accented chars»: éè øØ å<Å æ<Æ é éè', TRUE, FALSE, FALSE, TRUE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr8() {
        assertEval("argv <- list('[[:space:]]?(,|,?[[:space:]]and)[[:space:]]+', character(0), FALSE, FALSE, FALSE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr9() {
        assertEval("argv <- list('\\\\[[^]]*\\\\]', 'FALSE', FALSE, FALSE, FALSE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr10() {
        assertEval("argv <- list('(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)', c('  Ben Franklin and Jefferson Davis', '\\tMillard Fillmore'), FALSE, TRUE, FALSE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr11() {
        assertEval("argv <- list('?', 'utils::data', FALSE, FALSE, TRUE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr12() {
        assertEval("argv <- list('[[', 'utils:::.show_help_on_topic_', FALSE, FALSE, TRUE, FALSE); .Internal(gregexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testgregexpr14() {
        assertEval("argv <- structure(list(pattern = '', text = 'abc', fixed = TRUE),     .Names = c('pattern', 'text', 'fixed'));do.call('gregexpr', argv)");
    }

    @Test
    public void testgregexpr15() {
        assertEval("argv <- structure(list(pattern = '', text = 'abc'), .Names = c('pattern',     'text'));do.call('gregexpr', argv)");
    }

    @Test
    public void testgregexpr16() {
        assertEval("argv <- structure(list(pattern = '', text = 'abc', perl = TRUE),     .Names = c('pattern', 'text', 'perl'));do.call('gregexpr', argv)");
    }

    @Test
    public void testRegExpr() {
        assertEval("gregexpr(\"e\",c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"))");
        // NOTE: this is without attributes
        // gregexpr help does not comment x == NA and GnuR returns NA
        // while FastR returns -1
        // Using ImplementationError for now.
        assertEval(Ignored.ImplementationError, "gregexpr(\"(a)[^a]\\\\1\", c(\"andrea apart\", \"amadeus\", NA))");

        assertEval("{ x<-gregexpr(\"foo\", c(\"bar foo foo\", \"foo\"), fixed=T); as.integer(c(x[[1]], x[[2]])) }");
        assertEval("{ x<-gregexpr(\"foo\", c(\"bar foo foo\", \"foo\"), fixed=F); as.integer(c(x[[1]], x[[2]])) }");
        assertEval("{ x<-gregexpr(\"foo\", c(\"bar foo foo\", \"foo\"), fixed=T); list(attr(x[[1]], \"match.length\"), attr(x[[2]], \"match.length\")) }");
        assertEval("{ x<-gregexpr(\"foo\", c(\"bar foo foo\", \"foo\"), fixed=F); list(attr(x[[1]], \"match.length\"), attr(x[[2]], \"match.length\")) }");

        assertEval("{ .Internal(gregexpr(7, \"42\", F, F, F, F)) }");
        assertEval("{ .Internal(gregexpr(character(), \"42\", F, F, F, F)) }");
        assertEval("{ .Internal(gregexpr(\"7\", 42, F, F, F, F)) }");

        assertEval("{ argv <- structure(list(pattern = '', text = c('abc', 'defg'), perl = TRUE),     .Names = c('pattern', 'text', 'perl'));do.call('gregexpr', argv) }");
        assertEval("{ x<-c(\"Aaa Bbb Aaa bbb\", \"Aaa Bbb Aaa Bbb\", \"Aaa bbb Aaa bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; gregexpr(p, x, perl=TRUE) }");
        assertEval("{ x<-c(\"Aaa bbb Aaa bbb\", \"Aaa Bbb Aaa Bbb\", \"Aaa Bbb Aaa bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; gregexpr(p, x, perl=TRUE) }");
        assertEval("{ x<-c(\"Aaa bbb Aaa Bbb\", \"Aaa bbb Aaa bbb\", \"Aaa bbb Aaa Bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; gregexpr(p, x, perl=TRUE) }");
        assertEval("{ x<-c(\"Aaa bbb Aaa bbb\", \"Aaa Bbb Aaa Bbb\", \"Aaa bbb Aaa bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; gregexpr(p, x, perl=TRUE) }");

    }
}
