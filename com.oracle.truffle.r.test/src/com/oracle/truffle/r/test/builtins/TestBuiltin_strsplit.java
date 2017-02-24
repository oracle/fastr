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
public class TestBuiltin_strsplit extends TestBase {

    @Test
    public void teststrsplit1() {
        assertEval("argv <- list('exNSS4', '_', TRUE, FALSE, FALSE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit2() {
        assertEval("argv <- list(structure('x, row.names = NULL, ', Rd_tag = 'RCODE'), '', FALSE, FALSE, FALSE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit3() {
        assertEval("argv <- list('  \\036  isSeekable() now returns FALSE on connections       which have non-default encoding.  Although documented to       record if ‘in principle’ the connection supports seeking,       it seems safer to report FALSE when it may not work.', '[ \\t\\n]', FALSE, TRUE, FALSE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit4() {
        assertEval("argv <- list('Keywords:  device ', '[ \\t\\n]', FALSE, TRUE, TRUE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit5() {
        assertEval("argv <- list('R CMD check now gives an error if the R code in a vignette fails to\\nrun, unless this is caused by a missing package.\\n\\n\\036R CMD check now unpacks tarballs in the same way as R CMD INSTALL,\\nincluding making use of the environment variable R_INSTALL_TAR to\\noverride the default behaviour.\\n\\n\\036R CMD check performs additional code analysis of package startup\\nfunctions, and notifies about incorrect argument lists and\\n(incorrect) calls to functions which modify the search path or\\ninappropriately generate messages.\\n\\n\\036R CMD check now also checks compiled code for symbols corresponding\\nto functions which might terminate R or write to stdout/stderr\\ninstead of the console.\\n\\n\\036R CMD check now uses a pdf() device when checking examples (rather\\nthan postscript()).\\n\\n\\036R CMD check now checks line-endings of makefiles and C/C++/Fortran\\nsources in subdirectories of src as well as in src itself.\\n\\n\\036R CMD check now reports as a NOTE what look like methods documented\\nwith their full names even if there is a namespace and they are\\nexported.  In almost all cases they are intended to be used only as\\nmethods and should use the \\\\method markup.  In the other rare cases\\nthe recommended form is to use a function such as coefHclust which\\nwould not get confused with a method, document that and register it\\nin the NAMESPACE file by s3method(coef, hclust, coefHclust).\\n\\n\\036The default for the environment variable _R_CHECK_COMPACT_DATA2_ is\\nnow true: thus if using the newer forms of compression introduced\\nin R 2.10.0 would be beneficial is now checked (by default).\\n\\n\\036Reference output for a vignette can be supplied when checking a\\npackage by R CMD check: see ‘Writing R Extensions’.\\n\\n\\036R CMD Rd2dvi allows the use of LaTeX package inputenx rather than\\ninputenc: the value of the environment variable RD2DVI_INPUTENC is\\nused.  (LaTeX package inputenx is an optional install which\\nprovides greater coverage of the UTF-8 encoding.)\\n\\n\\036Rscript on a Unix-alike now accepts file names containing spaces\\n(provided these are escaped or quoted in the shell).\\n\\n\\036R CMD build on a Unix-alike (only) now tries to preserve dates on\\nfiles it copies from its input directory.  (This was the\\nundocumented behaviour prior to R 2.13.0.)', '\\n\\036', TRUE, FALSE, FALSE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit6() {
        assertEval("argv <- list(structure('Formal Methods and Classes', .Names = 'Title'), '\\n\\n', TRUE, FALSE, TRUE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit7() {
        assertEval("argv <- list('', '', FALSE, FALSE, FALSE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit8() {
        assertEval("argv <- list('The \\\\usage entries for S3 methods should use the \\\\method markup and not their full name.\\n', '\\n', FALSE, FALSE, TRUE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit9() {
        assertEval("argv <- list('M.user:Temp', ':', FALSE, FALSE, FALSE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit10() {
        assertEval("argv <- list('A shell of class documentation has been written to the file \\'./myTst2/man/DocLink-class.Rd\\'.\\n', '[ \\t\\n]', FALSE, TRUE, TRUE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit11() {
        assertEval("argv <- list(structure('pkgB', .Names = 'name'), '_', TRUE, FALSE, FALSE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit12() {
        assertEval("argv <- list('Keywords:  utilities ', '\\n[ \\t\\n]*\\n', FALSE, TRUE, TRUE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit13() {
        assertEval("argv <- list('x', '', FALSE, FALSE, FALSE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit14() {
        assertEval("argv <- list(c('* Edit the help file skeletons in \\'man\\', possibly combining help files for multiple functions.', '* Edit the exports in \\'NAMESPACE\\', and add necessary imports.', '* Put any C/C++/Fortran code in \\'src\\'.', '* If you have compiled code, add a useDynLib() directive to \\'NAMESPACE\\'.', '* Run R CMD build to build the package tarball.', '* Run R CMD check to check the package tarball.', '', 'Read \\\'Writing R Extensions\\\' for more information.'), '\\n[ \\t\\n]*\\n', FALSE, TRUE, TRUE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit15() {
        assertEval("argv <- list('  \\036  Complex arithmetic sometimes warned incorrectly about       producing NAs when there were NaNs in the input.', '[ \\t\\n]', FALSE, TRUE, TRUE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void teststrsplit16() {
        assertEval("argv <- list(structure(c('1', '2', '3', '4', '5', '1', '2', '3', '4', '5'), .Dim = 10L), '.', TRUE, FALSE, FALSE); .Internal(strsplit(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testStrSplit() {
        assertEval("{ strsplit(\"helloh\", \"h\", fixed=TRUE) }");
        assertEval("{ strsplit( c(\"helloh\", \"hi\"), c(\"h\",\"\"), fixed=TRUE) }");
        assertEval("{ strsplit(\"helloh\", \"\", fixed=TRUE) }");
        assertEval("{ strsplit(\"helloh\", \"h\") }");
        assertEval("{ strsplit( c(\"helloh\", \"hi\"), c(\"h\",\"\")) }");
        assertEval("{ strsplit(\"ahoj\", split=\"\") [[c(1,2)]] }");
        assertEval("{ strsplit(\"a,h,o,j\", split=\",\") }");
        assertEval("{ strsplit(\"abc\", \".\", fixed = TRUE, perl=FALSE) }");
        assertEval("{ strsplit(\"abc\", \".\", fixed = TRUE, perl=TRUE) }");
        assertEval("{ strsplit(\"abc\", \".\", fixed = FALSE, perl=FALSE) }");
        assertEval("{ strsplit(\"abc\", \".\", fixed = FALSE, perl=TRUE) }");

        assertEval("{ .Internal(strsplit(7, \"42\", F, F, F)) }");
        assertEval("{ .Internal(strsplit(\"7\", 42, F, F, F)) }");

        assertEval("strsplit('foo bar baz', '[f z]', perl=TRUE)");
        assertEval("strsplit('oo bar baz', '[f z]', perl=TRUE)");
        assertEval("strsplit('foo \u1010ÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄbar baz ', '[f z]', perl=TRUE)");
        assertEval("strsplit('Ä Ä', '[ ]', perl=TRUE)");

        assertEval("strsplit('1', '1', fixed=TRUE)");
        assertEval("strsplit('11', '11', fixed=TRUE)");
        assertEval("strsplit(c('1', '11'), c('1', '11'), fixed=TRUE)");
        assertEval("strsplit('Ä', 'Ä', fixed=TRUE)");
        assertEval("strsplit('ÄÄ', 'Ä', fixed=TRUE)");

        assertEval("strsplit('1', '1', fixed=FALSE)");
        assertEval("strsplit('11', '11', fixed=FALSE)");
        assertEval("strsplit(c('1', '11'), c('1', '11'), fixed=FALSE)");
        assertEval("strsplit('Ä', 'Ä', fixed=FALSE)");
        assertEval("strsplit('ÄÄ', 'Ä', fixed=FALSE)");

        assertEval("strsplit(c('111', '1'), c('111', '1'), fixed=TRUE)");
        assertEval("strsplit(c('1', ''), c('1', ''), fixed=TRUE)");
        assertEval("strsplit(c('1', 'b'), c('1', 'b'), fixed=TRUE)");
        assertEval("strsplit(c('a1a', 'a1b'), c('1', '1'), fixed=TRUE)");
        assertEval("strsplit(c('a1a', 'a1b'), '1', fixed=TRUE)");

        assertEval("strsplit(c('111', '1'), c('111', '1'), fixed=FALSE)");
        assertEval("strsplit(c('1', ''), c('1', ''), fixed=FALSE)");
        assertEval("strsplit(c('1', 'b'), c('1', 'b'), fixed=FALSE)");
        assertEval("strsplit(c('a1a', 'a1b'), c('1', '1'), fixed=FALSE)");
        assertEval("strsplit(c('a1a', 'a1b'), '1', fixed=FALSE)");

    }
}
