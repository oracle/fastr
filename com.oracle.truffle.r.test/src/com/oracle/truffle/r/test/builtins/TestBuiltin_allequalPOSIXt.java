/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
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
