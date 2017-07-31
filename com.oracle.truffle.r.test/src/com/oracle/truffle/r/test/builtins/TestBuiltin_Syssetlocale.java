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
public class TestBuiltin_Syssetlocale extends TestBase {

    @Test
    public void testSyssetlocale1() {
        assertEval("argv <- list(3L, 'C'); Sys.setlocale(argv[[1]], argv[[2]])");
    }

    @Test
    public void testSyssetlocale3() {
        assertEval("argv <- structure(list(category = 'LC_TIME', locale = 'C'), .Names = c('category',     'locale'));do.call('Sys.setlocale', argv)");
        assertEval("{ Sys.setenv(LC_CTYPE=\"en_US.UTF-8\"); Sys.getlocale(\"LC_CTYPE\"); }");
    }

    @Test
    public void testSyssetlocaleInvalidArgs() {
        assertEval("Sys.setlocale(4, c('more', 'elements'))");
        assertEval("Sys.setlocale(4, 42)");
        assertEval("Sys.setlocale('3L', 'C')");
    }
}
