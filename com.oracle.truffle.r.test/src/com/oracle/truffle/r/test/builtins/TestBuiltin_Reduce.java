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

public class TestBuiltin_Reduce extends TestBase {

    @Test
    public void testReduce1() {
        assertEval("argv <- structure(list(f = '+', x = 1:7, accumulate = TRUE),     .Names = c('f', 'x', 'accumulate'));do.call('Reduce', argv)");
    }

    @Test
    public void testReduce2() {
        assertEval("argv <- structure(list(f = function(f, ...) f(...), x = list(.Primitive('log'),     .Primitive('exp'), .Primitive('acos'), .Primitive('cos')),     init = 0, right = TRUE), .Names = c('f', 'x', 'init', 'right'));" +
                        "do.call('Reduce', argv)");
    }
}
