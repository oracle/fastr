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

public class TestBuiltin_storagemode extends TestBase {

    @Test
    public void teststoragemode1() {
        assertEval("argv <- structure(list(x = structure(c(1, 0.666666666666667,     0.333333333333333, 0, -0.333333333333333, -0.666666666666667,     -1, -1.33333333333333, -1.66666666666667, 1.5, 1, 0.5, 0,     -0.5, -1, -1.5, -2, -2.5, 3, 2, 1, 0, -1, -2, -3, -4, -5,     -Inf, -Inf, -Inf, NaN, Inf, Inf, Inf, Inf, Inf, -3, -2, -1,     0, 1, 2, 3, 4, 5, -1.5, -1, -0.5, 0, 0.5, 1, 1.5, 2, 2.5,     -1, -0.666666666666667, -0.333333333333333, 0, 0.333333333333333,     0.666666666666667, 1, 1.33333333333333, 1.66666666666667,     -0.75, -0.5, -0.25, 0, 0.25, 0.5, 0.75, 1, 1.25, -0.6, -0.4,     -0.2, 0, 0.2, 0.4, 0.6, 0.8, 1), .Dim = c(9L, 9L))), .Names = 'x');" +
                        "do.call('storage.mode', argv)");
    }

    @Test
    public void testStorageMode() {
        assertEval("{storage.mode(1)}");
        assertEval("{storage.mode(c)}");
        assertEval("{storage.mode(f<-function(){1})}");
        assertEval("{storage.mode(c(1,2,3))}");
        assertEval("{x<-1;storage.mode(x)<-\"character\"}");
        assertEval("{x<-1;storage.mode(x)<-\"logical\";x}");
    }
}
