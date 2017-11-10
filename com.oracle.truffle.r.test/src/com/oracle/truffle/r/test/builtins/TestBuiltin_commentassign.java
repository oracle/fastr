/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
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
}
