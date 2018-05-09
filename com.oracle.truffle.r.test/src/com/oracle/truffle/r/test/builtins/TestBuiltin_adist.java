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
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
