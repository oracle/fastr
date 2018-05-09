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
public class TestBuiltin_merge extends TestBase {

    @Test
    public void testmerge1() {
        assertEval("argv <- list(c(0L, 0L, 0L, 0L, 0L), 0L, FALSE, TRUE); .Internal(merge(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmerge2() {
        assertEval("argv <- list(c(0L, 0L, 0L, 0L, 0L), 0L, TRUE, FALSE); .Internal(merge(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmerge3() {
        assertEval("argv <- list(c(0L, 0L, 0L, 3L, 4L), c(0L, 0L, 0L, 3L, 4L), FALSE, FALSE); .Internal(merge(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testmerge5() {
        assertEval("argv <- structure(list(x = structure(list(gender = structure(c(1L,     1L, 2L), .Label = c('F', 'M'), class = 'factor'), age = c(20,     30, 40), filename = structure(1:3, .Label = c('q1.csv', 'q2.csv',     'q3.csv'), class = 'factor')), .Names = c('gender', 'age',     'filename'), row.names = c(NA, -3L), class = 'data.frame'),     y = structure(list(effsize = c(3.5, 2, 1.7), constraint = c(0.40625,         0.5, 0.882), outdegree = c(4, 2, 2), indegree = c(4,         2, 3), efficiency = c(0.625, 0.5, 0.444444444444444),         hierarchy = c(0, 0, 0.333333333333333), centralization = c(0.833333333333333,             1, 0.333333333333333), gden = c(0.5, 0.666666666666667,             0.666666666666667), ego.gden = c(0.166666666666667,             0, 0.5), filename = structure(1:3, .Label = c('q1.csv',             'q2.csv', 'q3.csv'), class = 'factor')), .Names = c('effsize',         'constraint', 'outdegree', 'indegree', 'efficiency',         'hierarchy', 'centralization', 'gden', 'ego.gden', 'filename'),         row.names = c('q1.csv', 'q2.csv', 'q3.csv'), class = 'data.frame'),     by = 'filename'), .Names = c('x', 'y', 'by'));" +
                        "do.call('merge', argv)");
    }

    @Test
    public void testExamplesFromHelp() {
        String init = "x <- data.frame(k1 = c(NA,NA,3,4,5), k2 = c(1,NA,NA,4,5), data = 1:5);" +
                        "y <- data.frame(k1 = c(NA,2,NA,4,5), k2 = c(NA,NA,3,4,5), data = 1:5);";
        assertEval(init + "merge(x, y, by = c('k1','k2'))");
        assertEval(init + "merge(x, y, by = 'k1')");
        assertEval(Ignored.ImplementationError, init + "merge(x, y, by = 'k2', incomparables = NA)");
    }
}
