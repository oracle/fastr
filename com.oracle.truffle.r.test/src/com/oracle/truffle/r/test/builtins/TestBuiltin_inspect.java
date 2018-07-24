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
public class TestBuiltin_inspect extends TestBase {

    @Test
    public void testInspect() {
        // There are many differences in an internal representation of objects
        // so we only test that the bultin does not crash for particular object.
        String prefix = "out <- capture.output(.Internal(inspect(";
        String suffix = "))); TRUE";
        String expected = "TRUE";
        assertEvalFastR(prefix + "1" + suffix, expected);
        assertEvalFastR(prefix + "1L" + suffix, expected);
        assertEvalFastR(prefix + "1:5" + suffix, expected);
        assertEvalFastR(prefix + "c(1,3,42)" + suffix, expected);
        assertEvalFastR(prefix + "c(1,NA,5)" + suffix, expected);
        assertEvalFastR(prefix + "TRUE" + suffix, expected);
        assertEvalFastR(prefix + "as.logical(c(TRUE,NA,FALSE))" + suffix, expected);
        assertEvalFastR(prefix + "as.raw(c(1,NA,255,129,16))" + suffix, expected);
        assertEvalFastR(prefix + "1+2i" + suffix, expected);
        assertEvalFastR(prefix + "c(1+3i,42-55i)" + suffix, expected);
        assertEvalFastR(prefix + "'abc'" + suffix, expected);
        assertEvalFastR(prefix + "c('abc', 'def')" + suffix, expected);
        assertEvalFastR(prefix + "list(a='b', b='c')" + suffix, expected);
        assertEvalFastR(prefix + "environment()" + suffix, expected);
    }

}
