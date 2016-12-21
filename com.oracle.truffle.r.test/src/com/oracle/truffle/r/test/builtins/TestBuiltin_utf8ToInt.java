/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_utf8ToInt extends TestBase {

    @Test
    public void testUtf8ToInt() {
        assertEval("utf8ToInt('a')");
        assertEval("utf8ToInt('Hello')");
        assertEval("utf8ToInt('')");
        assertEval("utf8ToInt(5)");
        assertEval("utf8ToInt(character(0))");
        assertEval("utf8ToInt(numeric(0))");
        assertEval("utf8ToInt(NULL)");
        assertEval("utf8ToInt(NA)");
        assertEval(Output.IgnoreWhitespace, "utf8ToInt(c('a', 'b'))"); // no extra newline in warning msg
    }

    @Test
    public void testutf8ToInt1() {
        assertEval("argv <- list('lasy'); .Internal(utf8ToInt(argv[[1]]))");
    }

    @Test
    public void testutf8ToInt3() {
        assertEval("argv <- structure(list(x = NA_character_), .Names = 'x');do.call('utf8ToInt', argv)");
    }
}
