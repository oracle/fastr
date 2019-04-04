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
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates
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
    public void testcommentassign4() {
        assertEval(Output.IgnoreErrorContext, "x<-42; comment(x) <- 1");
        assertEval(Output.IgnoreErrorContext, "x<-42; comment(x) <- c(1, 2)");
        assertEval("x<-42; comment(x) <- 'a'");
        assertEval("x<-42; comment(x) <- c('a', 'b')");
        assertEval("x<-42; comment(x) <- c('a', NA)");
        assertEval("x<-42; comment(x) <- c(NA, 'a', NA)");
        assertEval(Output.IgnoreErrorContext, "x<-42; comment(x) <- c(NA)");
        assertEval(Output.IgnoreErrorContext, "x<-42; comment(x) <- c(NA, NA)");
        assertEval("x<-42; comment(x) <- NULL");
        assertEval(Output.IgnoreErrorContext, "x<-42; comment(x) <- NA");
        assertEval(Output.IgnoreErrorContext, "x<-c(); comment(x) <- 'a'");
        assertEval(Output.IgnoreErrorContext, "x<-NULL; comment(x) <- 'a'");
        assertEval("x<-NA; comment(x) <- 'a'");
        assertEval("x <- c(1,2); y <- x; comment(x) <- 'hello'; comment(x); comment(y)");
        assertEval("x <- c(1,2); comment(x) <- 'hello'; y <- x; comment(x) <- NULL; comment(x); comment(y)");
        assertEval("foo <- function(x) 3*x; bar <- foo; comment(foo) <- 'hello'; comment(foo); comment(bar)");
        assertEval("foo <- function(x) 3*x; comment(foo) <- 'hello'; bar <- foo; comment(foo) <- NULL; comment(foo); comment(bar)");
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
