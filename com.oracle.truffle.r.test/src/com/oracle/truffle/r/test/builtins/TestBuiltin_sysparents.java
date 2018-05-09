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
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import static com.oracle.truffle.r.test.builtins.TestBuiltin_sysparent.SYS_PARENT_SETUP;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_sysparents extends TestBase {
    @Test
    public void testSysParents() {
        assertEval("{ sys.parents() }");
        assertEval("{ f <- function() sys.parents() ; f() }");
        assertEval("{ f <- function() sys.parents() ; g <- function() f() ; g() }");
        assertEval("{ f <- function() sys.parents() ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x=sys.parents()) x ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=sys.parents()) g(z) ; h() }");

        assertEval("{ f4 <- function() sys.parents(); f3 <- function(y) y; f2 <- function(x) x; f1 <- function() f2(f3(f4())); f1(); }");

        // FIXME OptForcedEagerPromiseNode causes the promise to be evaluated in different context
        // than it should be, yielding different results when introspecting the stack
        assertEval(Ignored.ImplementationError, "{ u <- function() sys.parents(); g <- function(y) y; h <- function(z=u()) g(z); h(); }");
        assertEval(Ignored.ImplementationError, "{ u <- function() sys.parents(); g <- function(y) y; h <- function(z) g(z); h(u()); }");

        assertEval(Ignored.ImplementationError, "{ u <- function() sys.parents() ; f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=u()) g(z) ; h() }");
    }

    @Test
    public void frameAccessCommonTest() {
        assertEval("{ foo <- function(x) sys.parents();" + SYS_PARENT_SETUP + "}");
    }
}
