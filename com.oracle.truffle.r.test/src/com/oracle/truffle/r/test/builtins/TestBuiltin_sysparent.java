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

// Checkstyle: stop line length check
public class TestBuiltin_sysparent extends TestBase {

    @Test
    public void testsysparent1() {
        assertEval("argv <- list(2); .Internal(sys.parent(argv[[1]]))");
    }

    @Test
    public void testSysParent() {
        assertEval("{ sys.parent() }");
        assertEval("{ f <- function() sys.parent() ; f() }");
        assertEval("{ f <- function() sys.parent() ; g <- function() f() ; g() }");
        assertEval("{ f <- function() sys.parent() ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x) sys.parent(x); f(0) }");
        assertEval("{ f <- function(x) sys.parent(x); f(4) }");
        assertEval(Ignored.ImplementationError, "{ f <- function(x) sys.parent(x); f(-4) }");
    }

    @Test
    public void testSysParentPromises() {
        assertEval("{ f <- function(x) x; g <- function() f(sys.parent()); h <- function() g(); h() }");
        assertEval("{ f <- function(x=sys.parent()) x ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=sys.parent()) g(z) ; h() }");
        assertEval("{ u <- function() sys.parent() ; f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=u()) g(z) ; h() }");
        assertEval(Ignored.ImplementationError, "{ f <- function(x) { print(sys.parent()); x }; g <- function(x) f(x); m <- function() g(g(sys.parent())); callm <- function() m(); callm() }");
    }

}
