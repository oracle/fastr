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
public class TestBuiltin_strtrim extends TestBase {

    @Test
    public void teststrtrim1() {
        assertEval("argv <- list(c('\\\'time\\\'', '\\\'status\\\''), 128); .Internal(strtrim(argv[[1]], argv[[2]]))");
    }

    @Test
    public void teststrtrim2() {
        assertEval("argv <- list('2014-03-17 14:47:20', 8); .Internal(strtrim(argv[[1]], argv[[2]]))");
    }

    @Test
    public void teststrtrim3() {
        assertEval("argv <- list(c('\\\'1\\\'', '\\\'2\\\'', NA), 128); .Internal(strtrim(argv[[1]], argv[[2]]))");
    }

    @Test
    public void teststrtrim4() {
        assertEval("argv <- list(c('\\\'gray17\\\'', '\\\'grey17\\\''), 128); .Internal(strtrim(argv[[1]], argv[[2]]))");
    }

    @Test
    public void teststrtrim5() {
        assertEval("argv <- list(structure('\\\'@CRAN@\\\'', .Names = 'CRAN'), 128); .Internal(strtrim(argv[[1]], argv[[2]]))");
    }

    @Test
    public void teststrtrim6() {
        assertEval("argv <- list('FALSE', FALSE); .Internal(strtrim(argv[[1]], argv[[2]]))");
    }

    @Test
    public void teststrtrim8() {
        assertEval("argv <- list(character(0), 40L); .Internal(strtrim(argv[[1]], argv[[2]]))");
    }

    @Test
    public void teststrtrim() {
        assertEval("v <- c('a', 'fooooo', 'bbbbbb', 'cccccccccc', 'dd', NA); names(v) <- as.character(1:6); strtrim(v, c(2L, 5L))");
        assertEval("v <- c('a', 'fooooo', 'bbbbbb', 'cccccccccc', 'dd', NA); names(v) <- as.character(1:6); strtrim(v, c(2, 5))");
    }
}
