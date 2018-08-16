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
public class TestBuiltin_regexpr extends TestBase {

    @Test
    public void testregexpr1() {
        assertEval(Ignored.NewRVersionMigration, "argv <- list('package:', 'exNSS4', FALSE, FALSE, TRUE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr2() {
        // FIXME FastR ignores useBytes == TRUE (then resulting index must be expressed in bytes)
        assertEval(Ignored.ImplementationError,
                        "argv <- list('éè', '«Latin-1 accented chars»: éè øØ å<Å æ<Æ é éè', FALSE, FALSE, TRUE, TRUE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr3() {
        assertEval(Ignored.NewRVersionMigration,
                        "argv <- list('package:', 'graphics', FALSE, FALSE, TRUE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr4() {
        assertEval(Ignored.NewRVersionMigration,
                        "argv <- list('^.*\\\\{n', 'my(ugly[file{name', FALSE, FALSE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr5() {
        assertEval(Ignored.NewRVersionMigration,
                        "argv <- list('(\\\\\\\\S4method\\\\{([._[:alnum:]]*|\\\\$|\\\\[\\\\[?|\\\\+|\\\\-|\\\\*|\\\\/|\\\\^|<=?|>=?|!=?|==|\\\\&|\\\\||\\\\%[[:alnum:][:punct:]]*\\\\%)\\\\}\\\\{((([._[:alnum:]]+|`[^`]+`),)*([._[:alnum:]]+|`[^`]+`))\\\\})', '\\nread.00Index(file)\\n', FALSE, FALSE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr6() {
        assertEval(Ignored.NewRVersionMigration,
                        "argv <- list('\\\\.([[:alnum:]]+)$', character(0), FALSE, FALSE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr7() {
        assertEval(Ignored.NewRVersionMigration,
                        "argv <- list('(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)', c('  Ben Franklin and Jefferson Davis', '\\tMillard Fillmore'), FALSE, TRUE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr8() {
        assertEval(Ignored.NewRVersionMigration,
                        "argv <- list('^[[:space:]]*@(?i)attribute', '% 4. Relevant Information Paragraph:', FALSE, TRUE, FALSE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr9() {
        assertEval(Ignored.NewRVersionMigration, "argv <- list('package:', 'dummy', FALSE, FALSE, TRUE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testregexpr10() {
        assertEval(Ignored.NewRVersionMigration,
                        "argv <- list('package:', 'environmental', FALSE, FALSE, TRUE, FALSE); .Internal(regexpr(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
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
        assertEval(Ignored.NewRVersionMigration, "regexpr(\"e\",c(\"arm\",\"foot\",\"lefroo\", \"bafoobar\"))");
        // NOTE: this is without attributes
        // FIXME NA should be the match for NA (not -1)
        // Expected output: [1] 6 1 NA
        // FastR output: [1] 6 1 -1
        assertEval(Ignored.ImplementationError, "regexpr(\"(a)[^a]\\\\1\", c(\"andrea apart\", \"amadeus\", NA))");

        assertEval(Ignored.NewRVersionMigration, "{ regexpr(\"aaa\", \"bbbaaaccc\", fixed=TRUE)  }");
        assertEval(Ignored.NewRVersionMigration, "{ regexpr(\"aaa\", c(\"bbbaaaccc\", \"haaah\"), fixed=TRUE) }");
        assertEval(Ignored.NewRVersionMigration, "{ regexpr(\"aaa\", c(\"bbbaaaccc\", \"hah\"), fixed=TRUE) }");

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

        assertEval(Ignored.NewRVersionMigration, "{ argv <- structure(list(pattern = '', text = c('abc', 'defg'), perl = TRUE),     .Names = c('pattern', 'text', 'perl'));do.call('regexpr', argv) }");
        assertEval(Ignored.NewRVersionMigration,
                        "{ x<-c(\"Aaa Bbb Aaa Bbb\", \"Aaa bbb Aaa bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; regexpr(p, x, perl=TRUE) }");
        assertEval(Ignored.NewRVersionMigration,
                        "{ x<-c(\"Aaa bbb Aaa bbb\", \"Aaa Bbb Aaa Bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; regexpr(p, x, perl=TRUE) }");
        assertEval(Ignored.NewRVersionMigration,
                        "{ x<-c(\"Aaa bbb Aaa bbb\", \"Aaa Bbb Aaa Bbb\", \"Aaa bbb Aaa bbb\"); p<-\"(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)\"; regexpr(p, x, perl=TRUE) }");

        assertEval(Ignored.NewRVersionMigration, "regexpr(')', 'abc()', fixed = TRUE)");
        assertEval(Ignored.NewRVersionMigration, "regexpr('(', 'abc()', fixed = TRUE)");
        assertEval(Ignored.NewRVersionMigration, "regexpr(')', 'abc()', fixed = FALSE)");
        assertEval(Ignored.NewRVersionMigration, "regexpr('\\\\)', 'abc()', fixed = FALSE)");
        assertEval(Output.IgnoreErrorMessage, "regexpr('(', 'abc()', fixed = FALSE)");
        assertEval(Ignored.NewRVersionMigration, "regexpr('\\\\(', 'abc()', fixed = FALSE)");
    }
}
