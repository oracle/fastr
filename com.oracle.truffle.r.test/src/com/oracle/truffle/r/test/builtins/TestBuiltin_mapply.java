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
public class TestBuiltin_mapply extends TestBase {

    @Test
    public void testmapply1() {
        assertEval("argv <- list(.Primitive('c'), list(list(), list(), list()), NULL); .Internal(mapply(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testmapply() {
        assertEval("mapply(rep, 1:4, 4:1)");
        assertEval("mapply(function(x, y) seq_len(x) + y, c(a =  1, b = 2, c = 3),  c(A = 10, B = 0, C = -10))");
        assertEval(Ignored.Unimplemented, "mapply(rep, times = 1:4, MoreArgs = list(x = 42))");
        assertEval(Ignored.Unimplemented, "word <- function(C, k) paste(rep.int(C, k), collapse = \"\"); utils::str(mapply(word, LETTERS[1:6], 6:1, SIMPLIFY = FALSE))");
        assertEval("{ mapply(rep.int, 42, MoreArgs = list(4)) }");
        assertEval("{ mapply(rep, times = 1:4, x = 4:1) }");
        assertEval("{ mapply(rep, times = 1:4, MoreArgs = list(x = 42)) }");
    }
}
