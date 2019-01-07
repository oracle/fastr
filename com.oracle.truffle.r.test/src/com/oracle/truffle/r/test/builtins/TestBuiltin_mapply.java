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
public class TestBuiltin_mapply extends TestBase {

    @Test
    public void testmapply1() {
        assertEval("argv <- list(.Primitive('c'), list(list(), list(), list()), NULL); .Internal(mapply(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testmapply() {
        assertEval("mapply(rep, 1:4, 4:1)");
        assertEval("mapply(function(x, y) seq_len(x) + y, c(a =  1, b = 2, c = 3),  c(A = 10, B = 0, C = -10))");
        assertEval("mapply(rep, times = 1:4, MoreArgs = list(x = 42))");
        assertEval("word <- function(C, k) paste(rep.int(C, k), collapse = \"\"); utils::str(mapply(word, LETTERS[1:6], 6:1, SIMPLIFY = FALSE))");
        assertEval("{ mapply(rep.int, 42, MoreArgs = list(4)) }");
        assertEval("{ mapply(rep, times = 1:4, x = 4:1) }");
        assertEval("{ mapply(rep, times = 1:4, MoreArgs = list(x = 42)) }");
        assertEval("mapply(function(...) 42)");
    }
}
