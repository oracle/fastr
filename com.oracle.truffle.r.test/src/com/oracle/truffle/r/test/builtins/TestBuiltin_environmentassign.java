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
public class TestBuiltin_environmentassign extends TestBase {

    @Test
    public void testenvironmentassign1() {
        assertEval("{ e1 <- new.env(); environment(e1) <- NULL }");
        assertEval("{ e1 <- new.env(); e2 <- new.env(); environment(e1) <- e2 }");
        assertEval("{ e1 <- new.env(); environment(e1) <- 3 }");

        assertEval("{ f <- function() {}; e1 <- new.env(); environment(f) <- e1 }");
        assertEval("{ f <- function() x; f2 <- f; e <- new.env(); assign('x', 2, envir=e); x <- 1; environment(f) <- e; c(f(), f2())}");
    }
}
