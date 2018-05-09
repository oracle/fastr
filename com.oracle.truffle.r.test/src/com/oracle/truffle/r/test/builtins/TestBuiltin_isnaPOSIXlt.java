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

public class TestBuiltin_isnaPOSIXlt extends TestBase {

    @Test
    public void testisnaPOSIXlt1() {
        assertEval("argv <- structure(list(x = structure(list(sec = 0, min = 0L,     hour = 0L, mday = 11L, mon = 7L, year = 3L, wday = 2L, yday = 222L,     isdst = 0L, zone = 'EST', gmtoff = NA_integer_), .Names = c('sec',     'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst',     'zone', 'gmtoff'), class = c('POSIXlt', 'POSIXt'), tzone = c('EST5EDT',     'EST', 'EDT'))), .Names = 'x');" +
                        "do.call('is.na.POSIXlt', argv)");
    }
}
