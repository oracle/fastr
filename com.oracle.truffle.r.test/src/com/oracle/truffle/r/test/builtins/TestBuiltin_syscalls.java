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

public class TestBuiltin_syscalls extends TestBase {

    @Test
    public void testSysCalls() {
        assertEval("sys.calls()");
        assertEval("{ f <- function(x) sys.calls(); g <- function() f(x); g() }");
        // Avoid deparse issues in the output of the try code by comparing length
        assertEval("{ f <- function(x) sys.calls(); g <- function() f(x); length(try(g())) }");
    }

    @Test
    public void testSysCallsPromises() {
        assertEval("{ f <- function(x) x; g <- function() f(sys.calls()); g() }");
        // similar eager promise problem as below
        assertEval(Ignored.ImplementationError, "{ f <- function(x) x; g <- function() f(sys.calls()); length(try(g())) }");
        // f is not on the stack because z=u() is being evaluated eagerly and not inside f
        assertEval(Ignored.ImplementationError, "{ v <- function() sys.calls() ; u<- function() v(); f <- function(x) x ; g <- function(y) f(y) ; h <- function(z=u()) g(z) ; h() }");
    }
}
