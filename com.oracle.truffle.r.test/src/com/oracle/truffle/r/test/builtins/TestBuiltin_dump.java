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
public class TestBuiltin_dump extends TestBase {

    private static final String TEMPLATE = "{ %s; .Internal(dump(%s, stdout(), environment(), .deparseOpts('all'), TRUE))}";

    @Test
    public void testDumpFunctions() {
        assertEval(String.format(TEMPLATE, "fun <- function() print('Hello, World!')", "'fun'"));
        assertEval(String.format(TEMPLATE, "foo <- function() cat('Hello'); bar <- function() cat('World')", "c('foo', 'bar')"));
    }

    @Test
    public void testDumpData() {
        assertEval(String.format(TEMPLATE, "x <- 10", "'x'"));
        assertEval(String.format(TEMPLATE, "x <- list(a=10,b=20)", "'x'"));
        assertEval(String.format(TEMPLATE, "x <- c(10,20,25)", "'x'"));
        assertEval(String.format(TEMPLATE, "x <- 1:10", "'x'"));
    }

    @Test
    public void testDumpLanguage() {
        assertEval(String.format(TEMPLATE, "x <- quote(2 + 3)", "'x'"));
    }

}
