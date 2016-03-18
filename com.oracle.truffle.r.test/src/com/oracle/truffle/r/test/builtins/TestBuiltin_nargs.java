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
public class TestBuiltin_nargs extends TestBase {

    @Test
    public void testnargs1() {
        assertEval("nargs( );");
    }

    @Test
    public void testNargs() {
        assertEval("{  f <- function (a, b, c) { nargs() }; f() }");
        assertEval("{  f <- function (a, b, c) { nargs() }; f(1, 2) }");
        assertEval("{  f <- function (a, b=TRUE, c=FALSE) { nargs() }; f(1) }");
        assertEval("{  f <- function (a, b=TRUE, c=FALSE) { nargs() }; f(1, FALSE) }");
        assertEval("{  f<-function(x, ..., y=TRUE) { nargs() }; f(1, 2, 3) }");

        assertEval("{  f <- function (a, b, c) { nargs() }; f(,,a) }");
    }
}
