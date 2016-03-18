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
public class TestBuiltin_comment extends TestBase {

    @Test
    public void testcomment1() {
        assertEval(Ignored.Unknown, "argv <- list(NULL); .Internal(comment(argv[[1]]))");
    }

    @Test
    public void testcomment2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0'))); .Internal(comment(argv[[1]]))");
    }

    @Test
    public void testcomment3() {
        assertEval(Ignored.Unknown, "argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(comment(argv[[1]]))");
    }

    @Test
    public void testcomment4() {
        assertEval(Ignored.Unknown, "argv <- list(structure(1:12, .Dim = 3:4, comment = c('This is my very important data from experiment #0234', 'Jun 5, 1998'))); .Internal(comment(argv[[1]]))");
    }
}
