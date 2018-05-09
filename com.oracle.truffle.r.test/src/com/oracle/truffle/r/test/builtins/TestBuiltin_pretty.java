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
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_pretty extends TestBase {

    @Test
    public void testpretty1() {
        assertEval("argv <- list(0L, 3L, 5, 1, 0.75, c(1.5, 2.75), 0); .Internal(pretty(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testpretty2() {
        assertEval("argv <- list(-0.03, 1.11, 5, 1, 0.75, c(1.5, 2.75), 0); .Internal(pretty(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testpretty3() {
        assertEval("argv <- list(-6.64448090063514e-06, 6.64454021993011e-06, 1, 0, 0.75, c(1.5, 2.75), 0); .Internal(pretty(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }

    @Test
    public void testpretty4() {
        assertEval("argv <- list(1.234e+100, 1.234e+100, 5, 1, 0.75, c(1.5, 2.75), 0); .Internal(pretty(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]]))");
    }
}
