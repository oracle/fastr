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

public class TestBuiltin_sysparents extends TestBase {
    @Test
    public void testSysParents() {
        assertEval("{ sys.parents() }");
        assertEval("{ f <- function() sys.parents() ; f() }");
        assertEval("{ f <- function() sys.parents() ; g <- function() f() ; g() }");
        assertEval("{ f <- function() sys.parents() ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x=sys.parents()) x ; g <- function() f() ; h <- function() g() ; h() }");
        assertEval("{ f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=sys.parents()) g(z) ; h() }");

        assertEval(Ignored.ImplementationError, "{ u <- function() sys.parents() ; f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=u()) g(z) ; h() }");
    }

}
