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

public class TestBuiltin_allequalPOSIXt extends TestBase {

    @Test
    public void testallequalPOSIXt1() {
        assertEval("argv <- structure(list(target = structure(1412833061.16639, class = c('POSIXct',     'POSIXt')), current = structure(1412833061.16839, class = c('POSIXct',     'POSIXt'))), .Names = c('target', 'current'));" +
                        "do.call('all.equal.POSIXt', argv)");
    }

    @Test
    public void testallequalPOSIXt2() {
        assertEval("argv <- structure(list(target = structure(1412833061.16639, class = c('POSIXct',     'POSIXt')), current = structure(list(sec = 41.1663863658905,     min = 37L, hour = 1L, mday = 9L, mon = 9L, year = 114L, wday = 4L,     yday = 281L, isdst = 1L, zone = 'EDT', gmtoff = -14400L),     .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday',         'yday', 'isdst', 'zone', 'gmtoff'), class = c('POSIXlt',         'POSIXt'), tzone = c('', 'EST', 'EDT'))), .Names = c('target',     'current'));" +
                        "do.call('all.equal.POSIXt', argv)");
    }
}
