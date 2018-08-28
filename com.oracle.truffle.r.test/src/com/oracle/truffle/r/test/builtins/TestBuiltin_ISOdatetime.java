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

public class TestBuiltin_ISOdatetime extends TestBase {

    @Test
    public void testISOdatetime1() {
        assertEval("argv <- structure(list(year = 1970, month = 1, day = 1, hour = 0,     min = 0, sec = 0, tz = 'GMT'), .Names = c('year', 'month',     'day', 'hour', 'min', 'sec', 'tz'));" +
                        "do.call('ISOdatetime', argv)");
    }

    @Test
    public void testISOdatetime2() {
        assertEval("argv <- structure(list(year = 2002, month = 6, day = 24, hour = 0,     min = 0, sec = 10), .Names = c('year', 'month', 'day', 'hour',     'min', 'sec'));" +
                        "do.call('ISOdatetime', argv)");
    }
}
