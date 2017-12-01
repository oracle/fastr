/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_choose extends TestBase {

    @Test
    public void testchoose1() {
        assertEval(".Internal(choose(-1, 3))");
    }

    @Test
    public void testchoose2() {
        assertEval(".Internal(choose(9L, c(-14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)))");
        assertEval(".Internal(choose(-9L, c(-14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)))");
    }

    @Test
    public void testchooseWithLogical() {
        assertEval(".Internal(choose(logical(0), logical(0)))");
        assertEval(".Internal(choose(FALSE, FALSE))");
        assertEval(".Internal(choose(NA, 1))");
        assertEval(".Internal(choose(1, NA))");
    }

    @Test
    public void testchoose4() {
        assertEval(".Internal(choose(0.5, 0:10))");
        // Minor diff in warning msg: extra newline; number formatting
        assertEval(Output.IgnoreWarningMessage, ".Internal(choose(2, 1.2))");
        assertEval(Output.IgnoreWarningMessage, ".Internal(choose(0:2, 1.2))");
        assertEval(".Internal(choose(structure(array(21:24, dim=c(2,2)), dimnames=list(a=c('a1','a2'),b=c('b1','b2'))), 2))");
        assertEval(".Internal(choose(47, structure(array(21:24, dim=c(2,2)), dimnames=list(a=c('a1','a2'),b=c('b1','b2')))))");
        assertEval(".Internal(choose(structure(47, myattr='hello'), 2))");
    }

    @Test
    public void testWithNonNumericArgs() {
        // GnuR choose error message does not show args evaluated
        assertEval(".Internal(choose('hello', 3))");
        assertEval(".Internal(choose(3, 'hello'))");
    }
}
