/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_sysparent extends TestBase {

    /**
     * This is common test case used to test all the frame access related built-ins.
     */
    public static final String SYS_PARENT_SETUP = "bar <- function(ba) do.call(foo, list(ba));" +
                    "boo <- function(bo) bar(bo);" +
                    "callboo <- function(cb) do.call('boo', list(cb));" +
                    "fun <- function(f) callboo(f);" +
                    "fun(42);";

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
        // f is not on the stack because z=u() is being evaluated eagerly and not inside f
        assertEval(Ignored.ImplementationError, "{ v <- function() sys.parent() ; u<- function() v(); f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=u()) g(z) ; h() }");
        assertEval("{ f <- function(x) { print(sys.parent()); x }; g <- function(x) f(x); m <- function() g(g(sys.parent())); callm <- function() m(); callm() }");
    }

    @Test
    public void frameAccessCommonTest() {
        assertEval("{ foo <- function(x) sapply(1:7, function(i) sys.parent(i)); " + SYS_PARENT_SETUP + "}");
    }
}
