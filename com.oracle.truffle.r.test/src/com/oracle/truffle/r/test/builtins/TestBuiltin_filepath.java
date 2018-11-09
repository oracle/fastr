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
public class TestBuiltin_filepath extends TestBase {

    @Test
    public void testfilepath1() {
        assertEval("argv <- list(list('/home/lzhao/hg/r-instrumented/tests/Packages/rpart/R', 'summary.rpart.R'), '/'); .Internal(file.path(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testfilepath2() {
        assertEval("argv <- list(list('/home/lzhao/hg/r-instrumented/src/library/parallel/R/unix', c('forkCluster.R', 'mcfork.R', 'mclapply.R', 'mcmapply.R', 'mcparallel.R', 'pvec.R')), '/'); .Internal(file.path(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testfilepath3() {
        assertEval("argv <- list(list('/home/lzhao/hg/r-instrumented/tests/tcltk.Rcheck', structure('tcltk', .Names = 'Package'), 'help'), '/'); .Internal(file.path(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testfilepath4() {
        assertEval("argv <- list(list(character(0), 'DESCRIPTION'), '/'); .Internal(file.path(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testfilepath5() {
        assertEval("argv <- list(list(structure(character(0), .Dim = c(0L, 0L))), '/'); .Internal(file.path(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testfilepath7() {
        assertEval("argv <- structure(list('.', 'Pkgs'), .Names = c('', ''));do.call('file.path', argv)");
    }

    @Test
    public void testfilepath() {
        assertEval("file.path('xyzqwrtyerta', NULL)");
    }
}
