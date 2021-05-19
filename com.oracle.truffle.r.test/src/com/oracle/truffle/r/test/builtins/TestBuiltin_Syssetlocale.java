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
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates
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
        // FastR returns "en_US.UTF-8", while GNUR cuts the prefix and returns "UTF-8"
        assertEval(Ignored.Unstable, "{ Sys.setenv(LC_CTYPE=\"en_US.UTF-8\"); Sys.getlocale(\"LC_CTYPE\"); }");
    }

    @Test
    public void testSyssetlocaleInvalidArgs() {
        assertEval("Sys.setlocale(4, c('more', 'elements'))");
        assertEval("Sys.setlocale(4, 42)");
        assertEval("Sys.setlocale('3L', 'C')");
    }

    @Test
    public void testSyslocaleconv() {
        assertEval("names(Sys.localeconv())");
    }

}
