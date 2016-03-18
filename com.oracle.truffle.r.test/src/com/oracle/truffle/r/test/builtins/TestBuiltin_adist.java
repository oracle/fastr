/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_adist extends TestBase {

    @Test
    public void testadist1() {
        assertEval(Ignored.MissingBuiltin,
                        "argv <- list(list(c(107L, 105L, 116L, 116L, 101L, 110L)), list(c(115L, 105L, 116L, 116L, 105L, 110L, 103L)), structure(c(1, 1, 1), .Names = c('insertions', 'deletions', 'substitutions')), FALSE, TRUE, FALSE, FALSE, FALSE); .Internal(adist(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testadist2() {
        assertEval(Ignored.MissingBuiltin,
                        "argv <- list(list(c(107L, 105L, 116L, 116L, 101L, 110L), c(115L, 105L, 116L, 116L, 105L, 110L, 103L)), list(c(107L, 105L, 116L, 116L, 101L, 110L), c(115L, 105L, 116L, 116L, 105L, 110L, 103L)), structure(c(1, 1, 1), .Names = c('insertions', 'deletions', 'substitutions')), TRUE, TRUE, FALSE, FALSE, FALSE); .Internal(adist(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testadist3() {
        assertEval(Ignored.MissingBuiltin,
                        "argv <- list('lasy', '1 lazy 2', c(1L, 1L, 1L), FALSE, TRUE, TRUE, FALSE, FALSE); .Internal(adist(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testadist4() {
        assertEval(Ignored.MissingBuiltin,
                        "argv <- list(list(), list(), structure(c(1, 1, 1), .Names = c('insertions', 'deletions', 'substitutions')), FALSE, TRUE, FALSE, FALSE, FALSE); .Internal(adist(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }
}
