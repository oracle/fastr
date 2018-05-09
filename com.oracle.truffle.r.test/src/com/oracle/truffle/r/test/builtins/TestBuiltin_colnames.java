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

public class TestBuiltin_colnames extends TestBase {

    @Test
    public void testcolnames1() {
        assertEval("argv <- structure(list(x = structure(c(1.00000000000001, 2, 3,     4, 5, 6, 7, 8, 9, 10, 0.999999999999998, 4, 9, 16, 25, 36,     49, 64, 81, 100, 5.39416105805496e-14, 2, 6, 12, 20, 30,     42, 56, 72, 90, 1, 0.999999999999999, 1, 1, 1, 1, 1, 1, 1,     1), .Dim = c(10L, 4L), .Dimnames = list(NULL, c('', 'B',     'C', 'D')))), .Names = 'x');" +
                        "do.call('colnames', argv)");
    }

    @Test
    public void testcolnames2() {
        assertEval("argv <- structure(list(x = structure(list(x = 1:6, CC = 11:16,     f = structure(c(1L, 1L, 2L, 2L, 3L, 3L), .Label = c('1',         '2', '3'), class = 'factor')), .Names = c('x', 'CC',     'f'), row.names = c(NA, -6L), class = 'data.frame')), .Names = 'x');" +
                        "do.call('colnames', argv)");
    }
}
