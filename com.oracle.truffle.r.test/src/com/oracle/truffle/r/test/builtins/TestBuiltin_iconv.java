/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_iconv extends TestBase {

    @Test
    public void testiconv1() {
        assertEval("argv <- list('Report Information on C Stack Size and Usage', 'UTF-8', '', 'byte', FALSE, FALSE); .Internal(iconv(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testiconv2() {
        // FIXME ç is not an ASCII char (although it's an extended ASCII char wit code==135)
        // and NA_character_ replacement leads to whole output to become NA
        assertEval(Ignored.ImplementationError,
                        "argv <- list('façile'   , 'latin1', 'ASCII', NA_character_, TRUE, FALSE); .Internal(iconv(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testiconv3() {
        assertEval("argv <- list(c('% This file is part of the foreign package for R', '% It is distributed under the GPL version 2 or later', '', '\\\\name{S3 read functions}', '\\\\alias{data.restore}', '\\\\alias{read.S}', '\\\\title{Read an S3 Binary or data.dump File}', '\\\\description{', '  Reads binary data files or \\\\code{data.dump} files that were produced', '  in S version 3.', '}', '\\\\usage{', '  data.restore(file, print = FALSE, verbose = FALSE, env = .GlobalEnv)', '  read.S(file)', '}', '\\\\arguments{', '  \\\\item{file}{the filename of the S-PLUS \\\\code{data.dump} or binary', '    file.}', '  \\\\item{print}{whether to print the name of each object as read from the', '    file.}', '  \\\\item{verbose}{whether to print the name of every subitem within each', '    object.}', '  \\\\item{env}{environment within which to create the restored object(s).}', '}', '\\\\value{', '  For \\\\code{read.S}, an R version of the S3 object.', '', '  For \\\\code{data.restore}, the name of the file.', '}', '\\\\details{', '  \\\\code{read.S} can read the binary files produced in some older', '  versions of S-PLUS on either Windows (versions 3.x, 4.x, 2000) or Unix', '  (version 3.x with 4 byte integers).  It automatically detects whether', '  the file was produced on a big- or little-endian machine and adapts', '  itself accordingly.', '', '  \\\\code{data.restore} can read a similar range of files produced by', '  \\\\code{data.dump} and for newer versions of S-PLUS, those from', '  \\\\code{data.dump(....., oldStyle=TRUE)}.', '', '  Not all S3 objects can be handled in the current version.  The most', '  frequently encountered exceptions are functions and expressions; you', '  will also have trouble with objects that contain model formulas.  In', '  particular, comments will be lost from function bodies, and the', '  argument lists of functions will often be changed.', '}', '\\\\author{', '  Duncan Murdoch', '}', '\\\\examples{', '\\\\dontrun{read.S(file.path(\\\'_Data\\\', \\\'myobj\\\'))', 'data.restore(\\\'dumpdata\\\', print = TRUE)', '}}', '\\\\keyword{data}', '\\\\keyword{file}'), '', 'ASCII', NA_character_, TRUE, FALSE); .Internal(iconv(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testiconv4() {
        assertEval("argv <- list(c('', 'Compute a Survival Curve for Censored Data'), 'UTF-8', '', 'byte', FALSE, FALSE); .Internal(iconv(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testiconv5() {
        assertEval("argv <- list(character(0), 'latin1', 'ASCII', NA_character_, TRUE, FALSE); .Internal(iconv(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testiconv6() {
        // FIXME According to GnuR docs x parm can be a list with NULLs or raws
        // btw current impl of IConv does not honor 'sub', 'mark' and 'toRaw' params at all
        assertEval(Ignored.ImplementationError,
                        "argv <- list(list(), 'latin1', 'ASCII', NA_character_, TRUE, FALSE); .Internal(iconv(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testiconv7() {
        // FIXME ç is not an ASCII char (although it's an extended ASCII char wit code==135)
        // so GnuR elimination of ç character is correct.
        assertEval(Ignored.ImplementationError, "argv <- list('façile'   , 'latin1', 'ASCII', '', TRUE, FALSE); .Internal(iconv(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testiconv8() {
        assertEval("argv <- list(structure('Prediction matrix for soap film smooth', Rd_tag = 'TEXT'), 'UTF-8', 'ASCII', NA_character_, FALSE, FALSE); .Internal(iconv(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testiconv9() {
        assertEval("argv <- list(structure(c('Q.1 Opinion of presidents job performance', 'Q.1 Opinion of presidents job performance', 'Q.1 Opinion of presidents job performance', 'Q.1 Opinion of presidents job performance', 'Q.1 Opinion of presidents job performance', 'Q.1 Opinion of presidents job performance', 'Q.1 Opinion of presidents job performance', 'Q.1 Opinion of presidents job performance', 'Q.1 Opinion of presidents job performance', 'Q.1 Opinion of presidents job performance'), .Names = c('Q1_MISSING_NONE', 'Q1_MISSING_1', 'Q1_MISSING_2', 'Q1_MISSING_3', 'Q1_MISSING_RANGE', 'Q1_MISSING_LOW', 'Q1_MISSING_HIGH', 'Q1_MISSING_RANGE_1', 'Q1_MISSING_LOW_1', 'Q1_MISSING_HIGH_1')), 'latin1', '', NA_character_, TRUE, FALSE); .Internal(iconv(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
    }

    @Test
    public void testIconv() {
        assertEval("{ .Internal(iconv(7, \"latin1\", \"ASCII\", \"42\", T, F)) }");
        assertEval("{ .Internal(iconv(\"7\", character(), \"ASCII\", \"42\", T, F)) }");
        assertEval("{ .Internal(iconv(\"7\", c(\"latin1\", \"latin1\"), \"ASCII\", \"42\", T, F)) }");
        assertEval("{ .Internal(iconv(\"7\", \"latin1\", c(\"ASCII\", \"ASCII\"), \"42\", T, F)) }");
        assertEval("{ .Internal(iconv(\"7\", \"latin1\", character(), \"42\", T, F)) }");
        assertEval("{ .Internal(iconv(\"7\", \"latin1\", \"ASCII\", 42, T, F)) }");
        assertEval("{ .Internal(iconv(\"7\", \"latin1\", \"ASCII\", character(), T, F)) }");
        assertEval("Sys.setlocale('LC_CTYPE', 'C'); iconv(c('²a²²','b')); Sys.setlocale('LC_CTYPE', 'UTF-8'); iconv(c('²a²²','b'))");
    }
}
