/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_nchar extends TestBase {

    @Test
    public void testnchar1() {
        assertEval("argv <- list('DtTmCl> format(.leap.seconds)         # all 24 leap seconds in your timezone', 'c', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar2() {
        assertEval("argv <- list(c('\\\'a\\\'', '\\\'b\\\'', NA, NA, NA, '\\\'f\\\'', '\\\'g\\\'', '\\\'h\\\'', '\\\'i\\\'', '\\\'j\\\'', '\\\'k\\\'', '\\\'l\\\''), 'w', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar3() {
        assertEval("argv <- list('\\\'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz\\\'', 'w', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar4() {
        assertEval("argv <- list(structure(c('1', '2', '3', '4', '5', '1', '2', '3', '4', '5'), .Dim = 10L), 'c', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar5() {
        assertEval("argv <- list(c('Var1', 'Var2'), 'bytes', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar6() {
        assertEval("argv <- list(c('0.0470', '0.0130', '0.0020', '0.0001', '2.3e-05', '4.5e-06'), 'w', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar7() {
        assertEval("argv <- list(c('x1', 'x.2', 'x3'), 'bytes', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar9() {
        assertEval("argv <- list(c('\\\'1\\\'', '\\\'2\\\'', NA), 'w', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar10() {
        assertEval("argv <- list(FALSE, 'chars', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar11() {
        assertEval("argv <- list('> contour(x, y, volcano, levels = lev, col=\\\'yellow\\\', lty=\\\'solid\\\', add=TRUE)', 'c', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar12() {
        assertEval("argv <- list(character(0), 'c', FALSE, FALSE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testnchar13() {
        assertEval("argv <- list(structure(c('rpart', 'recommended', '4.1-1', '2013-03-20', 'c(person(\\\'Terry\\\', \\\'Therneau\\\', role = \\\'aut\\\',\\n\\t             email = \\\'therneau@mayo.edu\\\'),\\n             person(\\\'Beth\\\', \\\'Atkinson\\\', role = \\\'aut\\\',\\t\\n\\t             email = \\\'atkinson@mayo.edu\\\'),\\n             person(\\\'Brian\\\', \\\'Ripley\\\', role = c(\\\'aut\\\', \\\'trl\\\', \\\'cre\\\'),\\n                    email = \\\'ripley@stats.ox.ac.uk\\\',\\n\\t\\t   comment = \\\'author of R port\\\'))', 'Recursive partitioning and regression trees', 'Recursive Partitioning', 'R (>= 2.14.0), graphics, stats, grDevices', 'survival', 'GPL-2 | GPL-3', 'yes', 'yes', 'Maintainers are not available to give advice on using a package\\nthey did not author.', '2013-03-20 07:27:05 UTC; ripley', 'Terry Therneau [aut],\\n  Beth Atkinson [aut],\\n  Brian Ripley [aut, trl, cre] (author of R port)', 'Brian Ripley <ripley@stats.ox.ac.uk>'), .Names = c('Package', 'Priority', 'Version', 'Date', 'Authors@R', 'Description', 'Title', 'Depends', 'Suggests', 'License', 'LazyData', 'ByteCompile', 'Note', 'Packaged', 'Author', 'Maintainer')), 'c', TRUE); .Internal(nchar(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testNChar() {
        assertEval("{ nchar(c(\"hello\", \"hi\")) }");
        assertEval("{ nchar(c(\"hello\", \"hi\", 10, 130)) }");
        assertEval("{ nchar(c(10,130)) }");
        assertEval("{ .Internal(nchar(c(10,130), 'chars', FALSE)) }");
        assertEval("{ .Internal(nchar('ff', 'chars', FALSE)) }");
    }
}
