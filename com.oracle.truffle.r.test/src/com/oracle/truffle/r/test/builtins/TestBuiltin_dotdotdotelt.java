/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_dotdotdotelt extends TestBase {

    @Test
    public void testdotdotdotelt1() {
        assertEval("tst <- function(n,...) ...elt(n); tst(1, 1, 2, 3)");
        assertEval(Ignored.NewRVersionMigration, "tst <- function(n,...) ...elt(n); tst(c(3,2,1), 1, 2, 3)");
        assertEval("tst <- function(n,...) ...elt(n); tst(1.6, 1, 2, 3)");
        // TODO: error with conversion of string to interger
        assertEval(Ignored.ImplementationError, "tst <- function(n,...) ...elt(n); tst('1.6', 1, 2, 3)");
        assertEval("tst <- function(n,...) ...elt(n); a <- 1; tst(a, 1, 2, 3)");
        assertEval("tst <- function(n,...) ...elt(n); f <- function() {print('hello')}; tst(1, f(), f(), 3)");
        assertEval("tst <- function(n,...) ...elt(n); tst(c(1), c(1,2,3))");
        assertEval("tst <- function(n,...) ...elt(n); tst(1, NA)");
        assertEval("tst <- function(n,...) ...elt(n);  b <- function() {print('b')}; tst(1, b())");
        assertEval("tst <- function(n,...) {print('a'); ...elt(n); print('c')};  b <- function() {print('b')}; tst(1, b())");
    }

    @Test
    public void testdotdotdoteltError() {
        assertEval("tst <- function(n,...) ...elt(n); tst(-1, 1)");
        assertEval("tst <- function(n,...) ...elt(n); tst(1)");
        assertEval("tst <- function(n,...) ...elt(n); tst(0, 1)");
        assertEval("tst <- function(n,...) ...elt(n); tst(5, 1, 2, 3)");
        // TODO: different error reporting for primitives
        assertEval(Output.IgnoreErrorMessage, "tst <- function(n,...) ...elt(); tst(c(1), c(1,2,3))");
        // TODO: error with conversion of string to interger
        assertEval(Ignored.ReferenceError, "tst <- function(n,...) ...elt(n); tst(' ', 1, 2)");
        assertEval("tst <- function(n,...) ...elt(n); tst(NA, 1, 2, 3)");
    }

}
