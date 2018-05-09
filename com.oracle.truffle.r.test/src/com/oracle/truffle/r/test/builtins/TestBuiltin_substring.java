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
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_substring extends TestBase {

    @Test
    public void testsubstring1() {
        assertEval("argv <- structure(list(text = c('NA', NA, 'BANANA'), first = 1,     last = 1), .Names = c('text', 'first', 'last'));do.call('substring', argv)");
    }

    @Test
    public void testsubstring2() {
        assertEval("argv <- structure(list(text = 'abcdef', first = 1:6, last = 1:6),     .Names = c('text', 'first', 'last'));do.call('substring', argv)");
    }

    @Test
    public void testSubstring() {
        assertEval("{ substring(\"123456\", first=2, last=4) }");
        assertEval("{ substring(\"123456\", first=2.8, last=4) }");
        assertEval("{ substring(c(\"hello\", \"bye\"), first=c(1,2,3), last=4) }");
        assertEval("{ substring(\"fastr\", first=NA, last=2) }");
    }
}
