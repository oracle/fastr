/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_syscall extends TestBase {
    @Test
    public void testSysCall() {
        assertEval("{ f <- function() sys.call() ; f() }");
        assertEval("{ f <- function(x) sys.call() ; f(x = 2) }");
        assertEval("{ f <- function() sys.call(1) ; g <- function() f() ; g() }");
        assertEval("{ f <- function() sys.call(2) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call(1) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call(-1) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call(-2) ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function() sys.call() ; g <- function() f() ; h <- function() g() ; h() }");

        assertEval("{ f <- function() sys.call() ; typeof(f()[[1]]) }");
        assertEval("{ f <- function(x) sys.call() ; typeof(f(x = 2)[[1]]) }");
        assertEval("{ f <- function(x) sys.call() ; typeof(f(x = 2)[[2]]) }");

        assertEval("{ f <- function(x) sys.call() ; f(2) }");
        assertEval("{ f <- function(x) sys.call() ; g <- function() 23 ; f(g()) }");

        assertEval("{ f <- function(x, y) sys.call() ; f(1, 2) }");
        assertEval("{ f <- function(x, y) sys.call() ; f(x=1, 2) }");
        assertEval("{ f <- function(x, y) sys.call() ; f(1, y=2) }");
        assertEval("{ f <- function(x, y) sys.call() ; f(y=1, 2) }");
        assertEval("{ f <- function(x, y) sys.call() ; f(y=1, x=2) }");

        // fails because can't parse out the "name"
        assertEval(Ignored.Unknown, "{ (function() sys.call())() }");
    }

}
