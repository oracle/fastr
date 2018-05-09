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
public class TestBuiltin_environmentassign extends TestBase {

    @Test
    public void testenvironmentassign1() {
        assertEval("{ e1 <- new.env(); environment(e1) <- NULL }");
        assertEval("{ e1 <- new.env(); e2 <- new.env(); environment(e1) <- e2 }");
        assertEval("{ e1 <- new.env(); environment(e1) <- 3 }");

        assertEval("{ e1 <- 1; environment(e1) <- NULL }");
        assertEval("{ e1 <- 1; e2 <- new.env(); environment(e1) <- e2 }");
        assertEval("{ e1 <- 1; environment(e1) <- 3 }");

        assertEval("{ f <- function() {}; e1 <- new.env(); environment(f) <- e1 }");
        assertEval("{ f <- function() {}; environment(f) <- NULL }");
        assertEval("{ f <- function() x; f2 <- f; e <- new.env(); assign('x', 2, envir=e); x <- 1; environment(f) <- e; c(f(), f2())}");

        assertEval("{ f <- NULL; environment(f) <- new.env() }");

        // changing the environment must not change metadata
        assertEval("{ f <- asS4(function(x) x+1); r <- isS4(f); environment(f) <- new.env(); r && isS4(f) }");
        assertEval("{ jh <- function(x) x+1; attributes(jh) <- list(myMetaData ='hello'); environment(jh) <- new.env(); attr(jh, 'myMetaData') }");
    }
}
