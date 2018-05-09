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
public class TestBuiltin_pmatch extends TestBase {

    @Test
    public void testpmatch1() {
        assertEval("argv <- list('kendall', c('pearson', 'kendall', 'spearman'), 0L, TRUE); .Internal(pmatch(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testpmatch2() {
        assertEval("argv <- list('month', c('secs', 'mins', 'hours', 'days', 'weeks', 'months', 'years', 'DSTdays'), NA_integer_, FALSE); .Internal(pmatch(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testpmatch3() {
        assertEval("argv <- list(c(NA_character_, NA_character_, NA_character_, NA_character_), 'NA', NA_integer_, TRUE); .Internal(pmatch(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testpmatch4() {
        assertEval("argv <- list('maximum', 'euclidian', NA_integer_, FALSE); .Internal(pmatch(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testpmatch5() {
        assertEval("argv <- list('fanny.object.', 'fanny.object', 0L, FALSE); .Internal(pmatch(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testpmatch6() {
        assertEval("argv <- list(c('alpha', 'col', 'border', 'lty', 'lwd'), c('col', 'border', 'alpha', 'size', 'height', 'angle', 'density'), NA_integer_, TRUE); .Internal(pmatch(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testpmatch7() {
        assertEval("argv <- list('unique.', 'unique.array', 0L, FALSE); .Internal(pmatch(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testpmatch8() {
        assertEval("argv <- list(character(0), c('labels', 'col', 'alpha', 'adj', 'cex', 'lineheight', 'font'), NA_integer_, TRUE); .Internal(pmatch(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testArgumentsCasts() {
        assertEval("pmatch(NULL, 1)");
        assertEval("pmatch(1, NULL)");
        assertEval("pmatch(x=NULL, table=NULL)");
        assertEval("pmatch(x=1)");
        assertEval("pmatch(table=1)");
        assertEval("pmatch(1:5, c(1,3), nomatch=NULL)");
        assertEval("pmatch(1:5, c(1,3), nomatch='str')");
        assertEval("pmatch(1:5, c(1,3), duplicates.ok=42)");
    }
}
