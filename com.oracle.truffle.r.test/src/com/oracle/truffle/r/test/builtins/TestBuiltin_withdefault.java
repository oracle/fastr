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

public class TestBuiltin_withdefault extends TestBase {

    @Test
    public void testwithdefault1() {
        assertEval("argv <- structure(list(data = structure(list(X = 22.1693750707316,     Y = -0.652127930273561, Z = 1.03034043827436, a = -2.66666666666667,     b = -10, c = 28), .Names = c('X', 'Y', 'Z', 'a', 'b', 'c')),     expr = expression({        dX <- a * X + Y * Z;        dY <- b * (Y - Z);        dZ <- -X * Y + c * Y - Z;        list(c(dX, dY, dZ))    })), .Names = c('data', 'expr'));" +
                        "do.call('with.default', argv)");
    }
}
