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
}
