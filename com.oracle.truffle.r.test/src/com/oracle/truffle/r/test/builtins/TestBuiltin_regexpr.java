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
public class TestBuiltin_regexpr extends TestBase {

    @Test
    public void testregexpr1() {
        assertEval("argv <- list('package:', 'exNSS4', FALSE, FALSE, TRUE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr2() {
        // FIXME FastR ignores useBytes == TRUE (then resulting index must be expressed in bytes)
        assertEval(Ignored.ImplementationError,
                        "argv <- list('éè', '«Latin-1 accented chars»: éè øØ å<Å æ<Æ é éè', FALSE, FALSE, TRUE, TRUE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr3() {
        assertEval("argv <- list('package:', 'graphics', FALSE, FALSE, TRUE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr4() {
        assertEval("argv <- list('^.*\\\\{n', 'my(ugly[file{name', FALSE, FALSE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr5() {
        assertEval("argv <- list('(\\\\\\\\S4method\\\\{([._[:alnum:]]*|\\\\$|\\\\[\\\\[?|\\\\+|\\\\-|\\\\*|\\\\/|\\\\^|<=?|>=?|!=?|==|\\\\&|\\\\||\\\\%[[:alnum:][:punct:]]*\\\\%)\\\\}\\\\{((([._[:alnum:]]+|`[^`]+`),)*([._[:alnum:]]+|`[^`]+`))\\\\})', '\\nread.00Index(file)\\n', FALSE, FALSE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr6() {
        assertEval("argv <- list('\\\\.([[:alnum:]]+)$', character(0), FALSE, FALSE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr7() {
        assertEval("argv <- list('(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)', c('  Ben Franklin and Jefferson Davis', '\\tMillard Fillmore'), FALSE, TRUE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr8() {
        assertEval("argv <- list('^[[:space:]]*@(?i)attribute', '% 4. Relevant Information Paragraph:', FALSE, TRUE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr9() {
        assertEval("argv <- list('package:', 'dummy', FALSE, FALSE, TRUE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr10() {
        assertEval("argv <- list('package:', 'environmental', FALSE, FALSE, TRUE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr12() {
        // Expected output: Error: '\d' is an unrecognized escape in character string starting "'\d"
        // FastR output: Error: unexpected 'd' in "argv <- structure(list(pattern = '\d"
        assertEval(Output.IgnoreErrorMessage, "argv <- structure(list(pattern = '\\d', text = c('1', 'B', '3')),     .Names = c('pattern', 'text'));do.call('regexpr', argv)");
    }

    @Test
    public void testregexpr13() {
        // FIXME according to ?regexpr the GnuR is correct and NA should be returned
        // Expected output: [1] NA
        // FastR output: [1] -1
        assertEval(Ignored.ImplementationError, "regexpr('a', NA)");
        assertEval(Ignored.ImplementationError, "argv <- structure(list(pattern = '[a-z]', text = NA), .Names = c('pattern',     'text'));do.call('regexpr', argv)");
    }

    @Test
    public void testRegExpr() {
        assertEval("regexpr(\"e\",c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"))");
        // NOTE: this is without attributes
        // FIXME NA should be the match for NA (not -1)
        // Expected output: [1] 6 1 NA
        // FastR output: [1] 6 1 -1
        assertEval(Ignored.ImplementationError, "regexpr(\"(a)[^a]\\\\1\", c(\"andrea apart\", \"amadeus\", NA))");

        assertEval("{ regexpr(\"aaa\", \"bbbaaaccc\", fixed=TRUE)  }");
        assertEval("{ regexpr(\"aaa\", c(\"bbbaaaccc\", \"haaah\"), fixed=TRUE) }");
        assertEval("{ regexpr(\"aaa\", c(\"bbbaaaccc\", \"hah\"), fixed=TRUE) }");

        assertEval("{ x<-regexpr(\"aaa\", \"bbbaaaccc\", fixed=TRUE); c(x[1])  }");
        assertEval("{ x<-regexpr(\"aaa\", c(\"bbbaaaccc\", \"haaah\"), fixed=TRUE); c(x[1], x[2]) }");
        assertEval("{ x<-regexpr(\"aaa\", c(\"bbbaaaccc\", \"hah\"), fixed=TRUE); c(x[1], x[2]) }");

        assertEval("{ x <- \"methods.html\"; pos <- regexpr(\"\\\\.([[:alnum:]]+)$\", x); substring(x, pos + 1L) }");

        assertEval("{ as.integer(regexpr(\"foo\", c(\"bar foo foo\", \"foo\"), fixed=T)) }");
        assertEval("{ as.integer(regexpr(\"foo\", c(\"bar foo foo\", \"foo\"), fixed=F)) }");
        assertEval("{ x<-regexpr(\"foo\", c(\"bar foo foo\", \"foo\"), fixed=T); attr(x, \"match.length\") }");
        assertEval("{ x<-regexpr(\"foo\", c(\"bar foo foo\", \"foo\")); attr(x, \"match.length\") }");

        assertEval("{ .Internal(regexpr(7, \"42\", F, F, F, F)) }");
        assertEval("{ .Internal(regexpr(character(), \"42\", F, F, F, F)) }");
        assertEval("{ .Internal(regexpr(\"7\", 42, F, F, F, F)) }");

        assertEval("{ argv <- structure(list(pattern = '', text = c('abc', 'defg'), perl = TRUE),     .Names = c('pattern', 'text', 'perl'));do.call('regexpr', argv) }");
        assertEval("{ x<-c(\"Aaa Bbb Aaa Bbb\", \"Aaa bbb Aaa bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; regexpr(p, x, perl=TRUE) }");
        assertEval("{ x<-c(\"Aaa bbb Aaa bbb\", \"Aaa Bbb Aaa Bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; regexpr(p, x, perl=TRUE) }");
        assertEval("{ x<-c(\"Aaa bbb Aaa bbb\", \"Aaa Bbb Aaa Bbb\", \"Aaa bbb Aaa bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; regexpr(p, x, perl=TRUE) }");

        assertEval("regexpr(')', 'abc()', fixed = TRUE)");
        assertEval("regexpr('(', 'abc()', fixed = TRUE)");
        assertEval("regexpr(')', 'abc()', fixed = FALSE)");
        assertEval("regexpr('\\\\)', 'abc()', fixed = FALSE)");
        assertEval(Output.IgnoreErrorMessage, "regexpr('(', 'abc()', fixed = FALSE)");
        assertEval("regexpr('\\\\(', 'abc()', fixed = FALSE)");
    }
}
