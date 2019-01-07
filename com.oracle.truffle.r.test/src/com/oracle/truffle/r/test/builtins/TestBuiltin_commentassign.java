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
public class TestBuiltin_commentassign extends TestBase {

    @Test
    public void testcommentassign1() {
        assertEval("argv <- list(structure(1:12, .Dim = 3:4, comment = c('This is my very important data from experiment #0234', 'Jun 5, 1998')), c('This is my very important data from experiment #0234', 'Jun 5, 1998')); .Internal(`comment<-`(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcommentassign2() {
        assertEval("argv <- list(character(0), NULL); .Internal(`comment<-`(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcommentassign3() {
        assertEval("argv <- list(logical(0), NULL); .Internal(`comment<-`(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testCommentAssign() {
        assertEval("{ x <- matrix(1:12, 3, 4); comment(x) <- c('This is my very important data from experiment #0234', 'Jun 5, 1998'); print(x); comment(x) }");
    }

    @Test
    public void testCommentAssignS4() {
        assertEval("{ setClass('CommentAssignS4Test', representation(f='numeric')); x <- new('CommentAssignS4Test'); comment(x) <- 'comment CDE'; attr(x, 'comment'); }");
    }
}
