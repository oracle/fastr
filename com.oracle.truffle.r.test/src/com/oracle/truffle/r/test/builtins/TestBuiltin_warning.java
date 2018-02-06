/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_warning extends TestBase {

    @Test
    public void testwarning() {
        assertEval("warning('foo')");
        assertEval("f <- function() warning('foo'); f()");
        assertEval("options(warn=1); f <- function() warning('foo'); f()");
        assertEval("f <- function() warning('foo'); f2 <- function() f(); f2()");
        assertEval("options(warn=1); f <- function() warning('foo'); f2 <- function() f(); f2()");

        // options(warn = 2)
        assertEval("op.warn <- getOption('warn'); options(warn = 2); f <- function() warning('foo'); tryCatch(f(), finally={options(warn = op.warn)})");
        assertEval("op.warn <- getOption('warn'); options(warn = 2); f <- function() warning('foo'); f2 <- function() f(); tryCatch(f2(), finally={options(warn = op.warn)})");
    }

}
