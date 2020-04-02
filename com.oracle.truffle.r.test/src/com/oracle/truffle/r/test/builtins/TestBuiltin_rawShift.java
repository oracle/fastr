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
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_rawShift extends TestBase {

    @Test
    public void testrawShift1() {
        assertEval("argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = -10L), .Names = c('x', 'n'));do.call('rawShift', argv)");
        assertEval("argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = -9), .Names = c('x', 'n'));do.call('rawShift', argv)");
        for (int i = -8; i <= 8; i++) {
            assertEval("argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = " + i + "L), .Names = c('x', 'n'));do.call('rawShift', argv)");
            assertEval("argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = " + i + ".1), .Names = c('x', 'n'));do.call('rawShift', argv)");
        }
        assertEval("argv <- structure(list(x = as.raw(c(0, 1, 32, 127, 128, 255, 123)), n = 9), .Names = c('x', 'n'));do.call('rawShift', argv)");

        // as.raw(c(1,2,3))[1] returns RRaw
        assertEval("rawShift( as.raw(c(1,2,3))[1], 1)");
    }
}
